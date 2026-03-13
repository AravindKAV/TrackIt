package com.upipulse.di

import com.upipulse.data.repository.ExpenseRepository
import com.upipulse.data.repository.ExpenseRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: ExpenseRepositoryImpl
    ): ExpenseRepository
}
