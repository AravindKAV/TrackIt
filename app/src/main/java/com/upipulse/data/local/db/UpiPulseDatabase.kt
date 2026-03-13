package com.upipulse.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.upipulse.data.local.dao.AccountDao
import com.upipulse.data.local.dao.CategoryDao
import com.upipulse.data.local.dao.TransactionDao
import com.upipulse.data.local.entity.AccountEntity
import com.upipulse.data.local.entity.CategoryEntity
import com.upipulse.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UpiPulseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
}
