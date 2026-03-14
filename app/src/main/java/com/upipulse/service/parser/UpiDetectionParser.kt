package com.upipulse.service.parser

import com.upipulse.domain.model.AccountSummary
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.model.TransactionSource
import java.time.Instant
import java.util.Locale

class UpiDetectionParser {
    
    private val amountRegex = Regex("""(?i)(?:rs\.?|inr|₹)\s*([0-9,.]+)""")
    private val merchantRegex = Regex("""(?i)(?:to|from|at)\s+([A-Za-z0-9 .@&_'-]+)""")
    
    // Keywords indicating a credit (income)
    private val creditKeywords = listOf("credited", "received", "added to", "deposited")
    // Keywords indicating a debit (expense)
    private val debitKeywords = listOf("debited", "sent", "paid", "spent", "transfer to")

    fun parse(
        body: String,
        source: TransactionSource,
        timestamp: Instant = Instant.now()
    ): Transaction? {
        val lowerBody = body.lowercase(Locale.getDefault())
        
        val amountMatch = amountRegex.find(body) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        var amount = amountStr.toDoubleOrNull() ?: return null
        
        // Detect if it's a credit or debit
        val isCredit = creditKeywords.any { lowerBody.contains(it) }
        val isDebit = debitKeywords.any { lowerBody.contains(it) }
        
        // If it's a debit (and not also a credit which would be confusing), make it negative
        if (isDebit && !isCredit) {
            amount = -amount
        } else if (!isCredit) {
            // Default to debit if no keywords found, but "to" is present
            if (lowerBody.contains("to ")) amount = -amount
        }

        val merchant = merchantRegex.find(body)?.groupValues?.getOrNull(1)?.trim()?.ifEmpty { null }
            ?: guessMerchant(body)
            
        val category = inferCategory(merchant)
        val paymentMethod = if (body.contains("upi", ignoreCase = true)) "UPI" else "System"
        
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
        // Try to find text after "to" or "from" first
        val parts = body.split(Regex("(?i)to|from|at"))
        if (parts.size > 1) {
            val potential = parts[1].trim().split(" ").take(3).joinToString(" ")
            if (potential.isNotEmpty()) return potential
        }
        
        val tokens = body.split(" ")
        return tokens.find { it.length > 2 && it.any(Char::isLetter) }?.trim() ?: "Unknown"
    }

    private fun inferCategory(merchant: String): String {
        val normalized = merchant.lowercase(Locale.getDefault())
        return when {
            normalized.contains("swiggy") || normalized.contains("zomato") || normalized.contains("restaurant") -> "Food"
            normalized.contains("uber") || normalized.contains("ola") || normalized.contains("metro") || normalized.contains("petrol") -> "Transport"
            normalized.contains("amazon") || normalized.contains("flipkart") || normalized.contains("myntra") -> "Shopping"
            normalized.contains("electric") || normalized.contains("bill") || normalized.contains("recharge") || normalized.contains("jio") || normalized.contains("airtel") -> "Bills"
            normalized.contains("netflix") || normalized.contains("hotstar") || normalized.contains("prime video") -> "Entertainment"
            normalized.contains("mart") || normalized.contains("fresh") || normalized.contains("bigbasket") || normalized.contains("grocer") -> "Groceries"
            else -> "Others"
        }
    }
}
