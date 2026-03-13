package com.upipulse.data.sample

import com.upipulse.domain.model.Account
import com.upipulse.domain.model.AccountSpending
import com.upipulse.domain.model.AccountSummary
import com.upipulse.domain.model.Category
import com.upipulse.domain.model.CategoryBreakdown
import com.upipulse.domain.model.DashboardAnalytics
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.model.TransactionSource
import com.upipulse.domain.model.WeeklySpendingPoint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

object SampleDataSource {
    fun categories(): List<Category> = listOf(
        Category(id = 0, name = "Food", icon = "ic_food"),
        Category(id = 0, name = "Transport", icon = "ic_transport"),
        Category(id = 0, name = "Shopping", icon = "ic_shopping"),
        Category(id = 0, name = "Bills", icon = "ic_bills"),
        Category(id = 0, name = "Entertainment", icon = "ic_entertainment"),
        Category(id = 0, name = "Groceries", icon = "ic_groceries"),
        Category(id = 0, name = "Others", icon = "ic_others")
    )

    fun accounts(): List<Account> = listOf(
        Account(
            id = 1,
            name = "Axis Savings",
            bankName = "Axis Bank",
            numberSuffix = "1234",
            colorHex = 0xFF4C1D95,
            balance = 25_000.0
        ),
        Account(
            id = 2,
            name = "HDFC Millennial",
            bankName = "HDFC Bank",
            numberSuffix = "9988",
            colorHex = 0xFF0EA5E9,
            balance = 18_000.0
        )
    )

    fun drafts(): List<SampleTransactionDraft> {
        val today = LocalDate.now()
        return listOf(
            SampleTransactionDraft(645.0, "Swiggy", "Food", "UPI", today.minusDays(1), "Axis Savings"),
            SampleTransactionDraft(320.0, "Uber", "Transport", "UPI", today.minusDays(2), "Axis Savings"),
            SampleTransactionDraft(1599.0, "Amazon", "Shopping", "Credit Card", today.minusDays(3), "HDFC Millennial"),
            SampleTransactionDraft(780.0, "Reliance Fresh", "Groceries", "UPI", today.minusDays(4), "Axis Savings"),
            SampleTransactionDraft(1450.0, "Electricity Board", "Bills", "Net Banking", today.minusDays(5), "HDFC Millennial"),
            SampleTransactionDraft(299.0, "Netflix", "Entertainment", "UPI", today.minusDays(6), "Axis Savings"),
            SampleTransactionDraft(120.0, "Tea Shop", "Others", "Cash", today, "Axis Savings")
        )
    }

    fun sampleTransactions(accountPool: List<Account> = accounts()): List<Transaction> {
        val zone = ZoneId.systemDefault()
        val accountLookup = accountPool.associateBy { it.name }
        return drafts().mapIndexed { index, row ->
            val account = accountLookup[row.accountName] ?: accountLookup.values.first()
            Transaction(
                id = index + 1L,
                amount = row.amount,
                merchant = row.merchant,
                category = row.category,
                paymentMethod = row.paymentMethod,
                date = row.date.atStartOfDay(zone).toInstant(),
                notes = "Sample data",
                source = TransactionSource.MANUAL,
                account = account.toSummary()
            )
        }
    }

    fun sampleAnalytics(): DashboardAnalytics {
        val accountPool = accounts()
        val transactions = sampleTransactions(accountPool)
        val monthlyTotal = transactions.sumOf { it.amount }
        val categoryBreakdown = transactions
            .groupBy { it.category }
            .map { (category, values) -> CategoryBreakdown(category, values.sumOf { it.amount }) }
        val weeklyTrend = DayOfWeek.values().map { day ->
            WeeklySpendingPoint(
                day = day,
                amount = transactions.filter { it.date.atZone(ZoneId.systemDefault()).dayOfWeek == day }
                    .sumOf { it.amount }
            )
        }
        val summaryLookup = accountPool.associateBy { it.toSummary() }
        val accountSpending = transactions
            .groupBy { it.account }
            .map { (account, values) ->
                AccountSpending(
                    account = account,
                    amount = values.sumOf { it.amount },
                    balance = summaryLookup[account]?.balance ?: 0.0
                )
            }
        val recent = transactions.sortedByDescending { it.date }.take(5)
        return DashboardAnalytics(
            monthlyTotal = monthlyTotal,
            categoryBreakdown = categoryBreakdown,
            weeklyTrend = weeklyTrend,
            recentTransactions = recent,
            accountSpending = accountSpending
        )
    }

    data class SampleTransactionDraft(
        val amount: Double,
        val merchant: String,
        val category: String,
        val paymentMethod: String,
        val date: LocalDate,
        val accountName: String
    )
}
