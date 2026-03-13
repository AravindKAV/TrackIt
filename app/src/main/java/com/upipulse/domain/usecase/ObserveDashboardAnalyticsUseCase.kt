package com.upipulse.domain.usecase

import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.domain.model.DashboardAnalytics
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDashboardAnalyticsUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<DashboardAnalytics> = repository.observeDashboard()
}
