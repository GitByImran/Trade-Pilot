package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderBookDepth
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedSubtle
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenSubtle
import kotlin.math.max

@Composable
fun OrderBookView(
    orderBook: OrderBookDepth?,
    modifier: Modifier = Modifier
) {
    if (orderBook == null) return

    val bids = orderBook.bids.take(8)
    val asks = orderBook.asks.take(8)
    val maxBidQty = bids.maxOfOrNull { it.quantity } ?: 1.0
    val maxAskQty = asks.maxOfOrNull { it.quantity } ?: 1.0
    val maxQty = max(maxBidQty, maxAskQty)

    val buyPressure = orderBook.buyPressureRatio
    val buyPct = (buyPressure * 100).toInt()
    val sellPct = 100 - buyPct

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Book Depth",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Spread: ${String.format("%.2f", orderBook.spread)} (${String.format("%.3f", orderBook.spreadPercent)}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Buy / Sell Pressure Bar
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bids $buyPct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = BullishGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Asks $sellPct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = BearishRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BearishRed.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(buyPressure.toFloat().coerceIn(0.05f, 0.95f))
                            .fillMaxHeight()
                            .background(BullishGreen)
                    )
                }
            }

            // Column Titles
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bid Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Amount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Ask Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Bids & Asks rows side-by-side or stacked
            val rows = maxOf(bids.size, asks.size)
            for (i in 0 until rows) {
                val bid = bids.getOrNull(i)
                val ask = asks.getOrNull(i)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bid side
                    if (bid != null) {
                        val bidFill = (bid.quantity / maxQty).toFloat().coerceIn(0.05f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(BullishGreenSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bidFill)
                                    .fillMaxHeight()
                                    .background(BullishGreen.copy(alpha = 0.25f))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format("%.2f", bid.price),
                                    fontSize = 10.sp,
                                    color = BullishGreen,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = String.format("%.4f", bid.quantity),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                    // Ask side
                    if (ask != null) {
                        val askFill = (ask.quantity / maxQty).toFloat().coerceIn(0.05f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(BearishRedSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(askFill)
                                    .fillMaxHeight()
                                    .background(BearishRed.copy(alpha = 0.25f))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format("%.2f", ask.price),
                                    fontSize = 10.sp,
                                    color = BearishRed,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = String.format("%.4f", ask.quantity),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
