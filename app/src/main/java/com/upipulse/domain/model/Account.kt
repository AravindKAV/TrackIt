package com.upipulse.domain.model

data class Account(
    val id: Long = 0L,
    val name: String,
    val bankName: String,
    val numberSuffix: String? = null,
    val colorHex: Long = ACCOUNT_COLORS.random(),
    val balance: Double = 0.0
) {
    fun toSummary(): AccountSummary = AccountSummary(id = id, name = name)

    companion object {
        private val ACCOUNT_COLORS = listOf(
            0xFF4C1D95,
            0xFF0EA5E9,
            0xFF16A34A,
            0xFFEA580C,
            0xFF9333EA
        ).map(Long::toLong)
    }
}

data class AccountSummary(
    val id: Long,
    val name: String
)

data class AccountSpending(
    val account: AccountSummary,
    val amount: Double,
    val balance: Double
)
