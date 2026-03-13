package com.upipulse.domain.model

import java.time.DayOfWeek

data class DashboardAnalytics(
    val monthlyTotal: Double,
    val categoryBreakdown: List<CategoryBreakdown>,
    val weeklyTrend: List<WeeklySpendingPoint>,
    val recentTransactions: List<Transaction>,
    val accountSpending: List<AccountSpending>
)

data class CategoryBreakdown(
    val category: String,
    val total: Double
)

data class WeeklySpendingPoint(
    val day: DayOfWeek,
    val amount: Double
)
