package com.upipulse.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.upipulse.domain.model.MandateType
import java.time.LocalDate

@Entity(tableName = "mandates")
data class MandateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val type: MandateType,
    val category: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
    val lastPaidMonth: String?
)
