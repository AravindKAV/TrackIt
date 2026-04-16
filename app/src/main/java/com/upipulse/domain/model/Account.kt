package com.upipulse.domain.model

data class Account(
    val id: Long = 0L,
    val name: String, // This is the nickname
    val bankName: String,
    val numberSuffix: String? = null,
    val colorHex: Long = ACCOUNT_COLORS.random(),
    val balance: Double = 0.0
) {
    fun toSummary(): AccountSummary = AccountSummary(
        id = id, 
        bankName = bankName,
        nickname = name.takeIf { it != bankName }
    )

    companion object {
        private val ACCOUNT_COLORS = listOf(
            0xFF6366F1, // Indigo
            0xFF0EA5E9, // Sky
            0xFF10B981, // Emerald
            0xFFF59E0B, // Amber
            0xFFEF4444  // Red
        ).map { it.toLong() }
    }
}

data class AccountSummary(
    val id: Long,
    val bankName: String,
    val nickname: String? = null
) {
    val displayName: String get() = bankName
}

data class AccountSpending(
    val account: AccountSummary,
    val amount: Double,
    val balance: Double
)
