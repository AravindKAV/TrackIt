package com.upipulse.domain.usecase

import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.domain.model.Account
import javax.inject.Inject

class GetDefaultAccountUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(): Account = repository.getDefaultAccount()
}
