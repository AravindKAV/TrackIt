package com.upipulse.ui.components

import java.util.Locale

private const val INR_SYMBOL = "\u20B9"
private val inrLocale = Locale("en", "IN")

fun formatInr(amount: Double, includeSymbol: Boolean = true): String {
    val formatted = String.format(inrLocale, "%,.0f", amount)
    return if (includeSymbol) INR_SYMBOL + formatted else formatted
}

fun formatInrLabel(prefix: String, amount: Double): String =
    "$prefix ${formatInr(amount)}"