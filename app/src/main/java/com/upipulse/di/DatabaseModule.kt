package com.upipulse.di

import android.content.Context
import androidx.room.Room
import com.upipulse.data.local.dao.AccountDao
import com.upipulse.data.local.dao.CategoryDao
import com.upipulse.data.local.dao.TransactionDao
import com.upipulse.data.local.db.UpiPulseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UpiPulseDatabase =
        Room.databaseBuilder(context, UpiPulseDatabase::class.java, "upi_pulse.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransactionDao(db: UpiPulseDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: UpiPulseDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideAccountDao(db: UpiPulseDatabase): AccountDao = db.accountDao()
}
