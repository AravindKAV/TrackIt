package com.upipulse.data.local.dao

import androidx.room.*
import com.upipulse.data.local.entity.MandateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MandateDao {
    @Query("SELECT * FROM mandates WHERE isActive = 1")
    fun observeAllActive(): Flow<List<MandateEntity>>

    @Query("SELECT * FROM mandates")
    fun observeAll(): Flow<List<MandateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mandate: MandateEntity): Long

    @Update
    suspend fun update(mandate: MandateEntity)

    @Delete
    suspend fun delete(mandate: MandateEntity)

    @Query("UPDATE mandates SET lastPaidMonth = :month WHERE id = :id")
    suspend fun updateLastPaidMonth(id: Long, month: String)
}
