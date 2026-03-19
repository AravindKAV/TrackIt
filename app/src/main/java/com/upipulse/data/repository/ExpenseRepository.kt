package com.upipulse.data.repository

import com.upipulse.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeDashboard(): Flow<DashboardAnalytics>
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: Long): Flow<Transaction?>
    fun observeCategories(): Flow<List<Category>>
    fun observeAccounts(): Flow<List<Account>>
    
    // Mandates
    fun observeMandates(): Flow<List<Mandate>>
    suspend fun upsertMandate(mandate: Mandate): Long
    suspend fun deleteMandate(mandate: Mandate)

    suspend fun ensureCategories(categories: List<Category>)
    suspend fun upsertCategory(category: Category): Category
    suspend fun deleteCategory(category: Category)
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
