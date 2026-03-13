package com.upipulse.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.upipulse.data.local.entity.TransactionEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Query(
        "SELECT t.*, a.name AS accountName FROM transactions t " +
            "LEFT JOIN accounts a ON a.id = t.accountId ORDER BY date DESC"
    )
    fun observeAllWithAccount(): Flow<List<TransactionWithAccountProjection>>

    @Query(
        "SELECT t.*, a.name AS accountName FROM transactions t " +
            "LEFT JOIN accounts a ON a.id = t.accountId ORDER BY date DESC LIMIT :limit"
    ) 
    fun observeRecentWithAccount(limit: Int): Flow<List<TransactionWithAccountProjection>>

    @Query(
        "SELECT t.*, a.name AS accountName FROM transactions t " +
            "LEFT JOIN accounts a ON a.id = t.accountId WHERE t.id = :id LIMIT 1"
    )
    fun observeWithAccount(id: Long): Flow<TransactionWithAccountProjection?>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getByIdNow(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun observeBetween(start: Instant, end: Instant): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE date BETWEEN :start AND :end")
    fun observeTotalBetween(start: Instant, end: Instant): Flow<Double>

    @Query(
        "SELECT category AS category, SUM(amount) AS total FROM transactions " +
            "WHERE date BETWEEN :start AND :end GROUP BY category ORDER BY total DESC"
    )
    fun observeCategorySummary(start: Instant, end: Instant): Flow<List<CategorySummaryProjection>>

    @Query("UPDATE transactions SET accountId = :newAccountId WHERE accountId = :oldAccountId")
    suspend fun reassignAccount(oldAccountId: Long, newAccountId: Long)

    data class CategorySummaryProjection(
        val category: String,
        val total: Double
    )

    data class TransactionWithAccountProjection(
        @Embedded val transaction: TransactionEntity,
        val accountName: String?
    )
}
