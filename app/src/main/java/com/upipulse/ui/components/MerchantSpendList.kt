package com.upipulse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.upipulse.domain.model.MerchantSpend

@Composable
fun MerchantSpendList(
    merchants: List<MerchantSpend>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        merchants.forEach { merchantSpend ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = merchantSpend.merchant.name)
                        Text(
                            text = "${merchantSpend.transactions} txns",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(text = formatInr(merchantSpend.amount))
                }
            }
        }
    }
}