package com.upipulse.domain.usecase

import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.domain.model.Transaction
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        repository.delete(transaction)
    }
}
