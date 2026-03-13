package com.upipulse.domain.usecase

import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.domain.model.Transaction
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTransactionUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(id: Long): Flow<Transaction?> = repository.observeTransaction(id)
}
