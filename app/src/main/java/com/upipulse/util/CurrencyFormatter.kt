package com.upipulse.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

fun formatInr(amount: Double): String = inrFormat.format(abs(amount))
