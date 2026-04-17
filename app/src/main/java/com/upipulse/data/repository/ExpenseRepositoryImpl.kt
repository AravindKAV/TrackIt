package com.upipulse.data.repository

import com.upipulse.data.local.dao.AccountDao
import com.upipulse.data.local.dao.CategoryDao
import com.upipulse.data.local.dao.MandateDao
import com.upipulse.data.local.dao.TransactionDao
import com.upipulse.data.local.dao.TransactionDao.TransactionWithAccountProjection
import com.upipulse.data.local.entity.AccountEntity
import com.upipulse.data.local.entity.CategoryEntity
import com.upipulse.data.local.entity.MandateEntity
import com.upipulse.data.local.entity.TransactionEntity
import com.upipulse.di.IoDispatcher
import com.upipulse.domain.model.*
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
    private val mandateDao: MandateDao,
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

    override fun observeMandates(): Flow<List<Mandate>> =
        mandateDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun upsertMandate(mandate: Mandate): Long =
        withContext(ioDispatcher) {
            mandateDao.insert(mandate.toEntity())
        }

    override suspend fun deleteMandate(mandate: Mandate) {
        withContext(ioDispatcher) {
            mandateDao.delete(mandate.toEntity())
        }
    }

    override suspend fun ensureCategories(categories: List<Category>) {
        withContext(ioDispatcher) {
            val entities = categories.map { it.toEntity() }
            categoryDao.insertAll(entities)
        }
    }

    override suspend fun upsertCategory(category: Category): Category =
        withContext(ioDispatcher) {
            val entity = category.toEntity()
            categoryDao.insertAll(listOf(entity))
            category
        }

    override suspend fun deleteCategory(category: Category) {
        withContext(ioDispatcher) {
            categoryDao.delete(category.toEntity())
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
            accountDao.delete(account.toEntity())
        }
    }

    override suspend fun getDefaultAccount(): Account = withContext(ioDispatcher) {
        val existing = accountDao.first()
        if (existing != null) {
            return@withContext existing.toDomain()
        }
        throw IllegalStateException("No accounts found. Please add one in Settings.")
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

    private fun buildDashboard(transactions: List<Transaction>, accounts: List<Account>): DashboardAnalytics {
        val monthRange = DateUtils.currentMonthRange()
        val weekRange = DateUtils.currentWeekRange()
        val monthlyTransactions = transactions.filter { it.date.isWithin(monthRange) }
        val weeklyTransactions = transactions.filter { it.date.isWithin(weekRange) }
        
        val monthlyTotalSpent = abs(monthlyTransactions.filter { it.amount < 0 }.sumOf { it.amount })
        val monthlyTotalEarned = monthlyTransactions.filter { it.amount > 0 }.sumOf { it.amount }
        
        val categoryBreakdown = monthlyTransactions
            .filter { it.amount < 0 }
            .groupBy { it.category }
            .map { (category, values) ->
                CategoryBreakdown(category = category, total = abs(values.sumOf { it.amount }))
            }
            .sortedByDescending { it.total }

        val incomeCategoryBreakdown = monthlyTransactions
            .filter { it.amount > 0 }
            .groupBy { it.category }
            .map { (category, values) ->
                CategoryBreakdown(category = category, total = values.sumOf { it.amount })
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
            .groupBy { it.account.id }
            .map { (accountId, values) ->
                val account = accountLookup[accountId] ?: Account(id = accountId, name = "Unknown", bankName = "Unknown")
                val balance = account.balance
                AccountSpending(
                    account = account.toSummary(),
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
            monthlyIncome = monthlyTotalEarned,
            categoryBreakdown = categoryBreakdown,
            incomeCategoryBreakdown = incomeCategoryBreakdown,
            weeklyTrend = weeklyTrend,
            recentTransactions = recent,
            accountSpending = accountTotals
        )
    }

    private fun TransactionWithAccountProjection.toDomain(): Transaction {
        return Transaction(
            id = transaction.id,
            amount = transaction.amount,
            merchant = transaction.merchant,
            category = transaction.category,
            paymentMethod = transaction.paymentMethod,
            date = transaction.date,
            notes = transaction.notes,
            source = transaction.source,
            account = AccountSummary(
                id = transaction.accountId, 
                bankName = accountBankName ?: "Bank #${transaction.accountId}",
                nickname = accountNickname
            ),
            externalId = transaction.externalId
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
        accountId = account.id,
        externalId = externalId
    )

    private fun CategoryEntity.toDomain(): Category = Category(id = id, name = name, icon = icon, type = type)

    private fun Category.toEntity(): CategoryEntity = CategoryEntity(id = id, name = name, icon = icon, type = type)

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

    private fun MandateEntity.toDomain(): Mandate = Mandate(
        id = id,
        name = name,
        amount = amount,
        dueDay = dueDay,
        type = type,
        category = category,
        startDate = startDate,
        endDate = endDate,
        isActive = isActive,
        lastPaidMonth = lastPaidMonth
    )

    private fun Mandate.toEntity(): MandateEntity = MandateEntity(
        id = id,
        name = name,
        amount = amount,
        dueDay = dueDay,
        type = type,
        category = category,
        startDate = startDate,
        endDate = endDate,
        isActive = isActive,
        lastPaidMonth = lastPaidMonth
    )
}
