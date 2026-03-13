package com.upipulse.data.local.db

import androidx.room.TypeConverter
import com.upipulse.domain.model.TransactionSource
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromSource(value: TransactionSource?): String? = value?.name

    @TypeConverter
    fun toSource(value: String?): TransactionSource? = value?.let(TransactionSource::valueOf)
}
