package com.upipulse.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.upipulse.domain.model.TransactionSource
import java.time.Instant

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["externalId"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: String,
    val paymentMethod: String,
    val date: Instant,
    val notes: String?,
    val source: TransactionSource,
    val accountId: Long,
    val externalId: String?,
    val createdAt: Instant = Instant.now()
)
