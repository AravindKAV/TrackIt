package com.upipulse.service.parser

import com.upipulse.domain.model.AccountSummary
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.model.TransactionSource
import java.time.Instant
import java.util.Locale

class UpiDetectionParser {
    
    private val amountRegex = Regex("""(?i)(?:rs\.?|inr|₹|paid|spent)\s*([0-9,.]+)""")
    private val cardSuffixRegex = Regex("""(?i)(?:card no\.|a/c|ending in|no\s*xx|a/c no\s*xx)\s*(\d{4})""")
    private val upiRefRegex = Regex("""(?i)(?:upi ref no|ref no|rrn|txn id)\s*([A-Z0-9]+)""")
    
    // Keywords indicating a credit (income)
    private val creditKeywords = listOf("credited", "received", "added to", "deposited", "refund")
    // Keywords indicating a debit (expense)
    private val debitKeywords = listOf("debited", "sent", "paid", "spent", "transfer to", "payment to")
    
    // Signals that strongly indicate this is a real completed transaction.
    private val transactionalSignals = listOf(
        "debited",
        "credited",
        "sent",
        "received",
        "paid",
        "spent",
        "payment successful",
        "txn successful",
        "transaction successful",
        "upi ref",
        "rrn",
        "ref no",
        "a/c",
        "available balance",
        "avl bal",
        "avl limit"
    )

    // Marketing and request patterns that should not create transactions.
    private val promotionalNoiseKeywords = listOf(
        "offer",
        "discount",
        "cashback up to",
        "apply now",
        "eligible for",
        "pre-approved",
        "loan offer",
        "instant loan",
        "personal loan",
        "credit line",
        "invite",
        "remind",
        "reminder",
        "split bill",
        "split request",
        "collect request",
        "payment request",
        "pay request"
    )

    private val completionOverrideKeywords = listOf(
        "debited",
        "credited",
        "payment successful",
        "transaction successful",
        "txn successful",
        "sent",
        "received",
        "paid to"
    )

    fun parse(
        body: String,
        source: TransactionSource,
        timestamp: Instant = Instant.now()
    ): Transaction? {
        val lowerBody = body.lowercase(Locale.getDefault())
        
        if (isLikelyPromotionalOrRequest(lowerBody)) return null
        if (!hasTransactionalSignal(lowerBody)) return null

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
        
        // Improved deduplication logic: 
        // 1. If upiRef is found, use it as part of the key.
        // 2. Otherwise, use amount + suffix + timestamp rounded to 10 minutes to allow for multiple SMS of same txn.
        val dedupeKey = upiRef ?: "${amount}_${cardSuffix ?: "NOSUFFIX"}_${timestamp.toEpochMilli() / 600000}"
        val externalId = "AUTO_$dedupeKey"
        
        return Transaction(
            id = 0,
            amount = amount,
            merchant = merchant,
            category = category,
            paymentMethod = paymentMethod,
            date = timestamp,
            notes = cardSuffix?.let { "Suffix:$it|$body" } ?: body.take(140),
            source = source,
            account = AccountSummary(id = -1, bankName = "Unassigned"),
            externalId = externalId
        )
    }

    private fun hasTransactionalSignal(lowerBody: String): Boolean {
        return transactionalSignals.any { lowerBody.contains(it) }
    }

    private fun isLikelyPromotionalOrRequest(lowerBody: String): Boolean {
        val hasNoiseKeyword = promotionalNoiseKeywords.any { lowerBody.contains(it) }
        val hasGenericRequest = lowerBody.contains("request") && !lowerBody.contains("request completed")
        val hasGenericLoanPromo =
            lowerBody.contains("loan") &&
                (lowerBody.contains("eligible") || lowerBody.contains("pre-approved") || lowerBody.contains("apply"))

        if (!hasNoiseKeyword && !hasGenericRequest && !hasGenericLoanPromo) return false

        val isCompletedTransaction = completionOverrideKeywords.any { lowerBody.contains(it) }
        val cashbackCredit = lowerBody.contains("cashback") &&
            (lowerBody.contains("credited") || lowerBody.contains("received"))

        return !isCompletedTransaction && !cashbackCredit
    }

    private fun extractMerchant(body: String): String? {
        // Try various patterns commonly found in Indian bank SMS
        val patterns = listOf(
            // "Paid Rs.100 to MERCHANT NAME"
            Regex("""(?i)paid\s+(?:rs\.?|inr|₹)\s*[0-9,.]+\s+to\s+([A-Za-z0-9 .@&_'-]+?)(?:\s+on|\s+using|\s+ref|\s+txn|\s+at|\.|$)"""),
            // "Transfer to MERCHANT NAME"
            Regex("""(?i)transfer\s+to\s+([A-Za-z0-9 .@&_'-]+?)(?:\s+on|\s+using|\s+ref|\s+txn|\s+at|\.|$)"""),
            // "at MERCHANT NAME" (often used in card/POS transactions)
            Regex("""(?i)at\s+([A-Za-z0-9 .@&_'-]+?)(?:\s+on|\s+using|\s+ref|\s+txn|\.|$)"""),
            // "Received from MERCHANT NAME"
            Regex("""(?i)received\s+(?:rs\.?|inr|₹)\s*[0-9,.]+\s+from\s+([A-Za-z0-9 .@&_'-]+?)(?:\s+on|\s+using|\s+ref|\s+txn|\s+at|\.|$)"""),
            // HDFC style: "to VPA merchant@vpa"
            Regex("""(?i)to\s+VPA\s+([A-Za-z0-9 .@&_'-]+)"""),
            // Google Pay style: "You paid MERCHANT NAME"
            Regex("""(?i)you\s+paid\s+([A-Za-z0-9 .@&_'-]+?)\s+(?:rs\.?|inr|₹)""")
        )
        
        for (pattern in patterns) {
            pattern.find(body)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && !isGenericWord(merchant)) {
                    return merchant
                }
            }
        }
        return null
    }

    private fun isGenericWord(word: String): Boolean {
        val generics = setOf("vpa", "upi", "account", "bank", "your", "the", "using")
        return generics.contains(word.lowercase())
    }

    private fun guessMerchant(body: String): String {
        // If everything fails, try to find any text after 'to' or 'from'
        val parts = body.split(Regex("(?i)\\bto\\b|\\bfrom\\b|\\bat\\b"))
        if (parts.size > 1) {
            val potential = parts[1].trim().split(Regex("\\s+")).take(3).joinToString(" ")
            if (potential.isNotEmpty() && !potential.contains(Regex("""\d{10,12}"""))) {
                return potential.replace(Regex("""[.,]$"""), "").trim()
            }
        }
        return "Unknown Merchant"
    }

    private fun inferCategory(merchant: String, body: String): String {
        val text = (merchant + " " + body).lowercase(Locale.getDefault())
        return when {
            text.contains("swiggy") || text.contains("zomato") || text.contains("restaurant") || text.contains("food") || text.contains("eat") -> "Food & Dining"
            text.contains("uber") || text.contains("ola") || text.contains("metro") || text.contains("petrol") || text.contains("fuel") || text.contains("transport") -> "Transport"
            text.contains("amazon") || text.contains("flipkart") || text.contains("myntra") || text.contains("shopping") || text.contains("nykaa") -> "Shopping"
            text.contains("electric") || text.contains("bill") || text.contains("recharge") || text.contains("jio") || text.contains("airtel") || text.contains("utility") || text.contains("bescom") -> "Bills & Utilities"
            text.contains("netflix") || text.contains("hotstar") || text.contains("prime video") || text.contains("movie") || text.contains("cinema") -> "Entertainment"
            text.contains("mart") || text.contains("fresh") || text.contains("bigbasket") || text.contains("grocer") || text.contains("blinkit") || text.contains("zepto") -> "Groceries"
            text.contains("salary") || text.contains("stipend") || text.contains("credited") -> "Salary"
            else -> "Others"
        }
    }
}
