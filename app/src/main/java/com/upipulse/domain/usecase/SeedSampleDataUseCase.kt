package com.upipulse.domain.usecase

import com.upipulse.data.preferences.UserPreferencesDataSource
import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.data.sample.SampleDataSource
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SeedSampleDataUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val preferences: UserPreferencesDataSource
) {
    suspend operator fun invoke(force: Boolean = false) {
        // Always ensure categories exist so new ones are added
        repository.ensureCategories(SampleDataSource.categories())
        
        val settings = preferences.settings.first()
        if (settings.sampleDataSeeded && !force) return

        if (force) {
            repository.clearTransactions()
        }

        val accounts = if (force || repository.observeAccounts().first().isEmpty()) {
            SampleDataSource.accounts().map { repository.upsertAccount(it) }
        } else {
            repository.observeAccounts().first()
        }
        
        // Insert sample transactions if we are forcing or if there are no transactions
        if (force || repository.observeTransactions().first().isEmpty()) {
            val transactions = SampleDataSource.sampleTransactions(accounts)
            repository.insertMany(transactions)
        }

        preferences.setSampleSeeded(true)
    }
}
