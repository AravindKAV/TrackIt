package com.upipulse.data.local.db

import androidx.room.TypeConverter
import com.upipulse.domain.model.CategoryType
import com.upipulse.domain.model.MandateType
import com.upipulse.domain.model.TransactionSource
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromSource(value: TransactionSource?): String? = value?.name

    @TypeConverter
    fun toSource(value: String?): TransactionSource? = value?.let(TransactionSource::valueOf)

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? = value?.let(CategoryType::valueOf)

    @TypeConverter
    fun fromMandateType(value: MandateType?): String? = value?.name

    @TypeConverter
    fun toMandateType(value: String?): MandateType? = value?.let(MandateType::valueOf)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
}
