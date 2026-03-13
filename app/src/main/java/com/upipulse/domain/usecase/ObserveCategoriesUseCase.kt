package com.upipulse.domain.usecase

import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.domain.model.Category
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveCategoriesUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<Category>> = repository.observeCategories()
}
