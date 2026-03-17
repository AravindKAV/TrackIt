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
        // Debit Categories
        Category(id = 0, name = "Food & Dining", icon = "ic_food"),
        Category(id = 0, name = "Transport", icon = "ic_transport"),
        Category(id = 0, name = "Shopping", icon = "ic_shopping"),
        Category(id = 0, name = "Bills & Utilities", icon = "ic_bills"),
        Category(id = 0, name = "Entertainment", icon = "ic_entertainment"),
        Category(id = 0, name = "Groceries", icon = "ic_groceries"),
        Category(id = 0, name = "Health & Wellness", icon = "ic_health"),
        Category(id = 0, name = "Education", icon = "ic_education"),
        Category(id = 0, name = "Investment", icon = "ic_investment"),
        Category(id = 0, name = "Travel", icon = "ic_travel"),
        Category(id = 0, name = "Personal Care", icon = "ic_personal"),
        Category(id = 0, name = "Gift & Donations", icon = "ic_gift"),
        Category(id = 0, name = "Others", icon = "ic_others"),
        Category(id = 0, name = "Transfer", icon = "ic_transfer"),
        
        // Credit Categories
        Category(id = 0, name = "Salary", icon = "ic_salary"),
        Category(id = 0, name = "Refunds", icon = "ic_refunds"),
        Category(id = 0, name = "Cashback", icon = "ic_cashback"),
        Category(id = 0, name = "Interest", icon = "ic_interest"),
        Category(id = 0, name = "Gift Received", icon = "ic_gift_in"),
        Category(id = 0, name = "Rental Income", icon = "ic_rent")
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
        val lastMonth = today.minusMonths(1)
        val twoMonthsAgo = today.minusMonths(2)
        
        return listOf(
            // Current Month
            SampleTransactionDraft(-645.0, "Swiggy", "Food & Dining", "UPI", today.minusDays(1), "Axis Savings"),
            SampleTransactionDraft(-320.0, "Uber", "Transport", "UPI", today.minusDays(2), "Axis Savings"),
            SampleTransactionDraft(-1599.0, "Amazon", "Shopping", "Credit Card", today.minusDays(3), "HDFC Millennial"),
            SampleTransactionDraft(-780.0, "Reliance Fresh", "Groceries", "UPI", today.minusDays(4), "Axis Savings"),
            SampleTransactionDraft(-1450.0, "Electricity Board", "Bills & Utilities", "Net Banking", today.minusDays(5), "HDFC Millennial"),
            SampleTransactionDraft(50000.0, "Company Inc", "Salary", "IMPS", today.minusDays(15), "HDFC Millennial"),
            SampleTransactionDraft(150.0, "Google Pay", "Cashback", "UPI", today.minusDays(2), "Axis Savings"),
            
            // Last Month
            SampleTransactionDraft(-2500.0, "Supermarket", "Groceries", "Debit Card", lastMonth.withDayOfMonth(10), "Axis Savings"),
            SampleTransactionDraft(-1200.0, "Gas Station", "Transport", "Cash", lastMonth.withDayOfMonth(15), "Axis Savings"),
            SampleTransactionDraft(45000.0, "Company Inc", "Salary", "IMPS", lastMonth.withDayOfMonth(1), "HDFC Millennial"),
            SampleTransactionDraft(-5000.0, "Rent", "Bills & Utilities", "Net Banking", lastMonth.withDayOfMonth(5), "HDFC Millennial"),
            
            // Two Months Ago
            SampleTransactionDraft(-3000.0, "Hospital", "Health & Wellness", "Credit Card", twoMonthsAgo.withDayOfMonth(12), "HDFC Millennial"),
            SampleTransactionDraft(-800.0, "Movie Theater", "Entertainment", "UPI", twoMonthsAgo.withDayOfMonth(20), "Axis Savings"),
            SampleTransactionDraft(45000.0, "Company Inc", "Salary", "IMPS", twoMonthsAgo.withDayOfMonth(1), "HDFC Millennial")
        )
    }

    fun sampleTransactions(accountPool: List<Account> = accounts()): List<Transaction> {
        val zone = ZoneId.systemDefault()
        val accountLookup = accountPool.associateBy { it.name }
        return drafts().mapIndexed { index, row ->
            val account = accountLookup[row.accountName] ?: accountPool.first()
            Transaction(
                id = 0L, // Let DB assign ID
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
        val monthlyTotal = transactions.filter { it.amount < 0 }.sumOf { it.amount }
        val monthlyIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val categoryBreakdown = transactions.filter { it.amount < 0 }
            .groupBy { it.category }
            .map { (category, values) -> CategoryBreakdown(category, values.sumOf { it.amount }.absoluteValue) }
        val weeklyTrend = DayOfWeek.values().map { day ->
            WeeklySpendingPoint(
                day = day,
                amount = transactions.filter { it.amount < 0 && it.date.atZone(ZoneId.systemDefault()).dayOfWeek == day }
                    .sumOf { it.amount }.absoluteValue
            )
        }
        val summaryLookup = accountPool.associateBy { it.toSummary() }
        val accountSpending = transactions
            .groupBy { it.account }
            .map { (account, values) ->
                AccountSpending(
                    account = account,
                    amount = values.filter { it.amount < 0 }.sumOf { it.amount }.absoluteValue,
                    balance = summaryLookup[account]?.balance ?: 0.0
                )
            }
        val recent = transactions.sortedByDescending { it.date }.take(5)
        return DashboardAnalytics(
            monthlyTotal = monthlyTotal.absoluteValue,
            monthlyIncome = monthlyIncome,
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
    
    private val Double.absoluteValue: Double
        get() = if (this < 0) -this else this
}
