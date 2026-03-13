package com.upipulse.service.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpiNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var parser: UpiDetectionParser
    @Inject lateinit var repository: ExpenseRepository
    @Inject lateinit var preferences: UserPreferencesDataSource
    @Inject lateinit var getDefaultAccountUseCase: GetDefaultAccountUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val body = sbn.notification.extras?.let { extras ->
            listOf(
                extras.getCharSequence("android.text"),
                extras.getCharSequence("android.bigText"),
                extras.getCharSequence("android.subText"),
                extras.getCharSequence("android.infoText")
            ).joinToString("\n") { it?.toString().orEmpty() }.trim()
        } ?: ""
        if (body.isEmpty()) return
        if (!SUPPORTED_PACKAGES.contains(sbn.packageName)) return
        val postedAt = Instant.ofEpochMilli(sbn.postTime)
        serviceScope.launch {
            val settings = preferences.settings.first()
            if (settings.notificationDetectionEnabled) {
                val transaction = parser.parse(body, TransactionSource.NOTIFICATION, postedAt)
                if (transaction != null) {
                    val account = getDefaultAccountUseCase().toSummary()
                    repository.insert(transaction.copy(account = account))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private val SUPPORTED_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "in.amazon.mShop.android.shopping"
        )
    }
}
