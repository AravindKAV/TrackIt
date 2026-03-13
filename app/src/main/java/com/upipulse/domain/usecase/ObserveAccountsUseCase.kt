package com.upipulse.domain.usecase

import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.domain.model.Account
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAccountsUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<Account>> = repository.observeAccounts()
}
