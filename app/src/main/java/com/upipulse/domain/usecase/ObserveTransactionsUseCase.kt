package com.upipulse.domain.usecase

import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.domain.model.Transaction
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTransactionsUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.observeTransactions()
}
