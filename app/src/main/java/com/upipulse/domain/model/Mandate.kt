package com.upipulse.domain.model

import java.time.LocalDate

enum class MandateType {
    LOAN, SUBSCRIPTION
}

data class Mandate(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int, // Day of month (1-31)
    val type: MandateType,
    val category: String, // Linked category for auto-tracking
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null, // Null for ongoing subscriptions
    val isActive: Boolean = true,
    val lastPaidMonth: String? = null // Format: "YYYY-MM"
)

data class MandateSummary(
    val totalCommitment: Double,
    val totalPaid: Double,
    val pendingCount: Int,
    val upcomingMandates: List<Mandate>
)
