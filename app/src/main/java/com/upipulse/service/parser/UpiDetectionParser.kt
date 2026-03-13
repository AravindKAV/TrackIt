package com.upipulse.service.parser

import com.upipulse.domain.model.AccountSummary
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.model.TransactionSource
import java.time.Instant
import java.util.Locale

class UpiDetectionParser {
    // Uses raw strings to correctly handle regex escape sequences
    private val amountRegex = Regex("""(?i)(?:rs\.?|inr|₹)\s*([0-9,.]+)""")
    private val merchantRegex = Regex("""(?i)(?:to|from)\s+([A-Za-z0-9 .@&_'-]+)""")

    fun parse(
        body: String,
        source: TransactionSource,
        timestamp: Instant = Instant.now()
    ): Transaction? {
        val amount = amountRegex.find(body)?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: return null
        val merchant = merchantRegex.find(body)?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null }
            ?: guessMerchant(body)
        val category = inferCategory(merchant)
        val paymentMethod = if (body.contains("upi", ignoreCase = true)) "UPI" else "Notification"
        return Transaction(
            id = 0,
            amount = amount,
            merchant = merchant,
            category = category,
            paymentMethod = paymentMethod,
            date = timestamp,
            notes = body.take(140),
            source = source,
            account = AccountSummary(id = -1, name = "Unassigned")
        )
    }

    private fun guessMerchant(body: String): String {
        val tokens = body.split(" ")
        return tokens.find { it.any(Char::isLetter) }?.trim() ?: "UPI Merchant"
    }

    private fun inferCategory(merchant: String): String {
        val normalized = merchant.lowercase(Locale.getDefault())
        return when {
            normalized.contains("swiggy") || normalized.contains("zomato") -> "Food"
            normalized.contains("uber") || normalized.contains("ola") -> "Transport"
            normalized.contains("amazon") || normalized.contains("flipkart") -> "Shopping"
            normalized.contains("electric") || normalized.contains("bill") -> "Bills"
            normalized.contains("netflix") || normalized.contains("hotstar") -> "Entertainment"
            normalized.contains("mart") || normalized.contains("fresh") -> "Groceries"
            else -> "Others"
        }
    }
}
