package com.upipulse.domain.usecase

import javax.inject.Inject

class ResetSampleDataUseCase @Inject constructor(
    private val seedSampleDataUseCase: SeedSampleDataUseCase
) {
    suspend operator fun invoke() {
        seedSampleDataUseCase(force = true)
    }
}
