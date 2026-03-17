package com.upipulse.service.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.upipulse.data.preferences.UserPreferencesDataSource
import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.domain.model.TransactionSource
import com.upipulse.domain.usecase.GetDefaultAccountUseCase
import com.upipulse.service.parser.UpiDetectionParser
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class SmsTransactionReceiver : BroadcastReceiver() {

    @Inject lateinit var parser: UpiDetectionParser
    @Inject lateinit var repository: ExpenseRepository
    @Inject lateinit var preferences: UserPreferencesDataSource
    @Inject lateinit var getDefaultAccountUseCase: GetDefaultAccountUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val body = messages.joinToString(separator = "\n") { it.displayMessageBody }
        val timestamp = messages.maxOfOrNull { it.timestampMillis }
            ?.takeIf { it > 0 }?.let(Instant::ofEpochMilli) ?: Instant.now()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val settings = preferences.settings.first()
            if (settings.smsDetectionEnabled) {
                val transaction = parser.parse(body, TransactionSource.SMS, timestamp)
                if (transaction != null) {
                    val accounts = repository.observeAccounts().first()
                    // Extract suffix from the hidden metadata in notes if present
                    val suffixMatch = Regex("""Suffix:(\d{4})\|""").find(transaction.notes ?: "")
                    val extractedSuffix = suffixMatch?.groupValues?.getOrNull(1)
                    
                    val targetAccount = if (extractedSuffix != null) {
                        accounts.find { it.numberSuffix == extractedSuffix } ?: getDefaultAccountUseCase()
                    } else {
                        getDefaultAccountUseCase()
                    }
                    
                    repository.insert(transaction.copy(
                        account = targetAccount.toSummary(),
                        // Clean up notes to remove suffix metadata
                        notes = transaction.notes?.substringAfter("|")?.ifBlank { null }
                    ))
                }
            }
            pendingResult.finish()
        }
    }
}
