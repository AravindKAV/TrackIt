package com.upipulse.domain.model

import java.time.Instant

data class Transaction(
    val id: Long = 0L,
    val amount: Double,
    val merchant: String,
    val category: String,
    val paymentMethod: String,
    val date: Instant,
    val notes: String?,
    val source: TransactionSource,
    val account: AccountSummary
)

enum class TransactionSource { MANUAL, SMS, NOTIFICATION }
