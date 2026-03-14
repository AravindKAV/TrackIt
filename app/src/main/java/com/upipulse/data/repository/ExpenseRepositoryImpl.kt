package com.upipulse.data.repository

import com.upipulse.data.local.dao.AccountDao
import com.upipulse.data.local.dao.CategoryDao
import com.upipulse.data.local.dao.TransactionDao
import com.upipulse.data.local.dao.TransactionDao.TransactionWithAccountProjection
import com.upipulse.data.local.entity.AccountEntity
import com.upipulse.data.local.entity.CategoryEntity
import com.upipulse.data.local.entity.TransactionEntity
import com.upipulse.di.IoDispatcher
import com.upipulse.domain.model.Account
import com.upipulse.domain.model.AccountSpending
import com.upipulse.domain.model.AccountSummary
import com.upipulse.domain.model.Category
import com.upipulse.domain.model.CategoryBreakdown
import com.upipulse.domain.model.DashboardAnalytics
import com.upipulse.domain.model.Transaction
import com.upipulse.domain.model.WeeklySpendingPoint
import com.upipulse.util.DateUtils
import com.upipulse.util.DateUtils.isWithin
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ExpenseRepository {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    override fun observeDashboard(): Flow<DashboardAnalytics> =
        combine(
            transactionDao.observeAllWithAccount(),
            accountDao.observeAccounts()
        ) { transactionProjections, accountEntities ->
            val transactions = transactionProjections.map { it.toDomain() }
            val accounts = accountEntities.map { it.toDomain() }
            buildDashboard(transactions, accounts)
        }.flowOn(ioDispatcher)

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAllWithAccount()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeTransaction(id: Long): Flow<Transaction?> =
        transactionDao.observeWithAccount(id)
            .map { projection -> projection?.toDomain() }
            .flowOn(ioDispatcher)

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAccounts()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun ensureCategories(categories: List<Category>) {
        withContext(ioDispatcher) {
            val entities = categories.map { it.toEntity() }
            categoryDao.insertAll(entities)
        }
    }

    override suspend fun upsertAccount(account: Account): Account =
        withContext(ioDispatcher) {
            val id = accountDao.upsert(account.toEntity())
            account.copy(id = if (account.id == 0L) id else account.id)
        }

    override suspend fun upsertAccounts(accounts: List<Account>): List<Account> =
        withContext(ioDispatcher) {
            val entities = accounts.map { it.toEntity() }
            val ids = accountDao.insertAll(entities)
            accounts.mapIndexed { index, account ->
                val id = if (account.id == 0L) ids.getOrNull(index) ?: account.id else account.id
                account.copy(id = id)
            }
        }

    override suspend fun deleteAccount(account: Account) {
        withContext(ioDispatcher) {
            val fallback = getDefaultAccountInternal(excludeId = account.id)
            transactionDao.reassignAccount(account.id, fallback.id)
            accountDao.delete(account.toEntity())
        }
    }

    override suspend fun getDefaultAccount(): Account = withContext(ioDispatcher) {
        getDefaultAccountInternal()
    }

    override suspend fun insert(transaction: Transaction) {
        withContext(ioDispatcher) {
            val entity = transaction.toEntity()
            transactionDao.insert(entity)
            accountDao.adjustBalance(transaction.account.id, transaction.amount)
        }
    }

    override suspend fun update(transaction: Transaction) {
        withContext(ioDispatcher) {
            val entity = transaction.toEntity()
            val existing = transactionDao.getByIdNow(transaction.id)
            if (existing != null) {
                if (existing.accountId != transaction.account.id) {
                    // Reverse old amount on old account
                    accountDao.adjustBalance(existing.accountId, -existing.amount)
                    // Apply new amount on new account
                    accountDao.adjustBalance(transaction.account.id, transaction.amount)
                } else {
                    val delta = transaction.amount - existing.amount
                    accountDao.adjustBalance(transaction.account.id, delta)
                }
            }
            transactionDao.update(entity)
        }
    }

    override suspend fun delete(transaction: Transaction) {
        withContext(ioDispatcher) {
            val entity = transaction.toEntity()
            transactionDao.delete(entity)
            accountDao.adjustBalance(transaction.account.id, -transaction.amount)
        }
    }

    override suspend fun insertMany(transactions: List<Transaction>) {
        withContext(ioDispatcher) {
            transactions.forEach { txn ->
                transactionDao.insert(txn.toEntity())
                accountDao.adjustBalance(txn.account.id, txn.amount)
            }
        }
    }

    override suspend fun clearTransactions() {
        withContext(ioDispatcher) { transactionDao.clearAll() }
    }

    private suspend fun getDefaultAccountInternal(excludeId: Long? = null): Account {
        val existing = accountDao.first()
        if (existing != null && existing.id != excludeId) {
            return existing.toDomain()
        }
        val seed = Account(
            name = "Primary UPI",
            bankName = "UPI",
            numberSuffix = null,
            colorHex = 0xFF4C1D95,
            balance = 0.0
        )
        val inserted = accountDao.upsert(seed.toEntity())
        return seed.copy(id = inserted)
    }

    private fun buildDashboard(transactions: List<Transaction>, accounts: List<Account>): DashboardAnalytics {
        val monthRange = DateUtils.currentMonthRange()
        val weekRange = DateUtils.currentWeekRange()
        val monthlyTransactions = transactions.filter { it.date.isWithin(monthRange) }
        val weeklyTransactions = transactions.filter { it.date.isWithin(weekRange) }
        
        // Total spent should be positive for display
        val monthlyTotalSpent = abs(monthlyTransactions.filter { it.amount < 0 }.sumOf { it.amount })
        
        val categoryBreakdown = monthlyTransactions
            .filter { it.amount < 0 } // Only breakdown spending
            .groupBy { it.category }
            .map { (category, values) ->
                CategoryBreakdown(category = category, total = abs(values.sumOf { it.amount }))
            }
            .sortedByDescending { it.total }
            
        val weeklyTrend = DateUtils.weekDays().map { day ->
            val total = weeklyTransactions
                .filter { it.date.atZone(zoneId).dayOfWeek == day && it.amount < 0 }
                .sumOf { it.amount }
            WeeklySpendingPoint(day = day, amount = abs(total))
        }
        
        val accountLookup = accounts.associateBy { it.id }
        val accountTotals = monthlyTransactions
            .groupBy { it.account }
            .map { (account, values) ->
                val balance = accountLookup[account.id]?.balance ?: 0.0
                AccountSpending(
                    account = account,
                    amount = values.sumOf { it.amount },
                    balance = balance
                )
            }
            .sortedByDescending { abs(it.amount) }
            
        val recent = transactions
            .sortedByDescending { it.date }
            .take(5)
            
        return DashboardAnalytics(
            monthlyTotal = monthlyTotalSpent,
            categoryBreakdown = categoryBreakdown,
            weeklyTrend = weeklyTrend,
            recentTransactions = recent,
            accountSpending = accountTotals
        )
    }

    private fun TransactionWithAccountProjection.toDomain(): Transaction {
        val accountName = accountName ?: "Account #${transaction.accountId}"
        return Transaction(
            id = transaction.id,
            amount = transaction.amount,
            merchant = transaction.merchant,
            category = transaction.category,
            paymentMethod = transaction.paymentMethod,
            date = transaction.date,
            notes = transaction.notes,
            source = transaction.source,
            account = AccountSummary(transaction.accountId, accountName)
        )
    }

    private fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
        id = id,
        amount = amount,
        merchant = merchant,
        category = category,
        paymentMethod = paymentMethod,
        date = date,
        notes = notes,
        source = source,
        accountId = account.id
    )

    private fun CategoryEntity.toDomain(): Category = Category(id = id, name = name, icon = icon)

    private fun Category.toEntity(): CategoryEntity = CategoryEntity(id = id, name = name, icon = icon)

    private fun AccountEntity.toDomain(): Account = Account(
        id = id,
        name = name,
        bankName = bankName,
        numberSuffix = numberSuffix,
        colorHex = colorHex,
        balance = balance
    )

    private fun Account.toEntity(): AccountEntity = AccountEntity(
        id = id,
        name = name,
        bankName = bankName,
        numberSuffix = numberSuffix,
        colorHex = colorHex,
        balance = balance
    )
}
