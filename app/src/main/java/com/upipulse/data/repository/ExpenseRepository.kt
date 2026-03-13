package com.upipulse.data.repository

import com.upipulse.domain.model.Account
import com.upipulse.domain.model.DashboardAnalytics
import com.upipulse.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeDashboard(): Flow<DashboardAnalytics>
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: Long): Flow<Transaction?>
    fun observeCategories(): Flow<List<com.upipulse.domain.model.Category>>
    fun observeAccounts(): Flow<List<Account>>
    suspend fun ensureCategories(categories: List<com.upipulse.domain.model.Category>)
    suspend fun upsertAccount(account: Account): Account
    suspend fun upsertAccounts(accounts: List<Account>): List<Account>
    suspend fun deleteAccount(account: Account)
    suspend fun getDefaultAccount(): Account
    suspend fun insert(transaction: Transaction)
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    suspend fun insertMany(transactions: List<Transaction>)
    suspend fun clearTransactions()
}
