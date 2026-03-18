package com.upipulse.service.parser

import com.upipulse.domain.model.AccountSummary
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.model.TransactionSource
import java.time.Instant
import java.util.Locale

class UpiDetectionParser {
    
    private val amountRegex = Regex("""(?i)(?:rs\.?|inr|₹|paid|spent)\s*([0-9,.]+)""")
    private val merchantRegex = Regex("""(?i)(?:to|from|at)\s+([A-Za-z0-9 .@&_'-]+)""")
    private val cardSuffixRegex = Regex("""(?i)(?:card no\.|a/c|ending in|no\s*xx)\s*(?:xx|x+)?(\d{4})""")
    private val upiRefRegex = Regex("""(?i)(?:upi ref no|ref no|rrn)\s*(\d+)""")
    
    // Keywords indicating a credit (income)
    private val creditKeywords = listOf("credited", "received", "added to", "deposited", "refund")
    // Keywords indicating a debit (expense)
    private val debitKeywords = listOf("debited", "sent", "paid", "spent", "transfer to", "payment to")

    fun parse(
        body: String,
        source: TransactionSource,
        timestamp: Instant = Instant.now()
    ): Transaction? {
        val lowerBody = body.lowercase(Locale.getDefault())
        
        // Find amount
        val amountMatch = amountRegex.find(body) ?: Regex("""\d+\.\d{2}""").find(body)
        if (amountMatch == null) return null
        
        val amountStr = amountMatch.groupValues.getOrNull(1)?.replace(",", "") ?: amountMatch.value
        var amount = amountStr.toDoubleOrNull() ?: return null
        
        // Detect if it's a credit or debit
        val isCredit = creditKeywords.any { lowerBody.contains(it) }
        val isDebit = debitKeywords.any { lowerBody.contains(it) }
        
        if ((isDebit || lowerBody.contains("paid") || lowerBody.contains("spent")) && !isCredit) {
            amount = -amount
        } else if (!isCredit) {
            if (lowerBody.contains("to ")) amount = -amount
        }

        // Try to extract card/account suffix
        val cardSuffix = cardSuffixRegex.find(body)?.groupValues?.getOrNull(1)
        
        // Try to extract UPI Reference Number for deduplication
        val upiRef = upiRefRegex.find(body)?.groupValues?.getOrNull(1)

        val merchant = extractMerchant(body) ?: guessMerchant(body)
            
        val category = inferCategory(merchant, lowerBody)
        val paymentMethod = when {
            body.contains("upi", ignoreCase = true) -> "UPI"
            body.contains("card", ignoreCase = true) -> "Card"
            else -> "System"
        }
        
        // Create an externalId for deduplication. 
        // Preference: UPI Reference > (Amount + Suffix + Date approx)
        val externalId = upiRef ?: "MANUAL_${amount}_${cardSuffix ?: "NOSUFFIX"}_${timestamp.toEpochMilli() / 60000}"
        
        return Transaction(
            id = 0,
            amount = amount,
            merchant = merchant,
            category = category,
            paymentMethod = paymentMethod,
            date = timestamp,
            notes = cardSuffix?.let { "Suffix:$it|$body" } ?: body.take(140),
            source = source,
            account = AccountSummary(id = -1, name = "Unassigned"),
            externalId = externalId
        )
    }

    private fun extractMerchant(body: String): String? {
        val patterns = listOf(
            Regex("""(?i)to\s+([A-Za-z0-9 .@&_'-]+?)\s+(?:on|using|ref|txn)"""),
            Regex("""(?i)paid\s+(?:rs\.?|inr|₹)\s*[0-9,.]+\s+to\s+([A-Za-z0-9 .@&_'-]+)"""),
            Regex("""(?i)received\s+(?:rs\.?|inr|₹)\s*[0-9,.]+\s+from\s+([A-Za-z0-9 .@&_'-]+)""")
        )
        
        for (pattern in patterns) {
            pattern.find(body)?.let { return it.groupValues[1].trim() }
        }
        return null
    }

    private fun guessMerchant(body: String): String {
        val parts = body.split(Regex("(?i)to|from|at|using"))
        if (parts.size > 1) {
            val potential = parts[1].trim().split(" ").take(3).joinToString(" ")
            if (potential.isNotEmpty()) return potential
        }
        return "Manual Entry"
    }

    private fun inferCategory(merchant: String, body: String): String {
        val text = (merchant + " " + body).lowercase(Locale.getDefault())
        return when {
            text.contains("swiggy") || text.contains("zomato") || text.contains("restaurant") || text.contains("food") -> "Food & Dining"
            text.contains("uber") || text.contains("ola") || text.contains("metro") || text.contains("petrol") || text.contains("fuel") -> "Transport"
            text.contains("amazon") || text.contains("flipkart") || text.contains("myntra") || text.contains("shopping") -> "Shopping"
            text.contains("electric") || text.contains("bill") || text.contains("recharge") || text.contains("jio") || text.contains("airtel") || text.contains("utility") -> "Bills & Utilities"
            text.contains("netflix") || text.contains("hotstar") || text.contains("prime video") || text.contains("movie") -> "Entertainment"
            text.contains("mart") || text.contains("fresh") || text.contains("bigbasket") || text.contains("grocer") -> "Groceries"
            text.contains("salary") || text.contains("stipend") -> "Salary"
            else -> "Others"
        }
    }
}
