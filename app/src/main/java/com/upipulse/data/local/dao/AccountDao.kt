package com.upipulse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.upipulse.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>): List<Long>

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts LIMIT 1")
    suspend fun first(): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("UPDATE accounts SET balance = balance + :delta WHERE id = :accountId")
    suspend fun adjustBalance(accountId: Long, delta: Double)

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AccountEntity?
}
