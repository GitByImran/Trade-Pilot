package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaperPosition
import com.example.ui.TradePilotViewModel
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BinanceGoldSubtle
import com.example.ui.theme.BullishGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaperTradingScreen(
    viewModel: TradePilotViewModel,
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.paperPortfolio.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("paper_trading_screen")
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Notice banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BinanceGoldSubtle,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = BinanceGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIRTUAL ACCOUNT: All trades are simulated with real live Binance prices, 0.1% fees, and slippage.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BinanceGold
                    )
                }
            }
        }

        // Portfolio Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("portfolio_summary_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Portfolio Equity",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${String.format("%,.2f", portfolio.totalEquity)} USDT",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Account",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Available USDT", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%,.2f", portfolio.availableUsdt)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("In Positions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%,.2f", portfolio.totalInvestedInPositions)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Unrealized P&L", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val pnlColor = if (portfolio.totalUnrealizedPnl >= 0) BullishGreen else BearishRed
                            Text(
                                text = "${if (portfolio.totalUnrealizedPnl >= 0) "+" else ""}$${String.format("%.2f", portfolio.totalUnrealizedPnl)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = pnlColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Performance KPIs Grid
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Win Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format("%.1f", portfolio.winRatePercent)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                        }
                        Column {
                            Text("Profit Factor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format("%.2f", portfolio.profitFactor)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Realized P&L", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val rPnlColor = if (portfolio.totalRealizedPnl >= 0) BullishGreen else BearishRed
                            Text(
                                "${if (portfolio.totalRealizedPnl >= 0) "+" else ""}$${String.format("%.2f", portfolio.totalRealizedPnl)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = rPnlColor
                            )
                        }
                        Column {
                            Text("Trades Closed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${portfolio.totalClosedTradesCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Open Positions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Positions (${portfolio.openPositions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Open Positions List
        if (portfolio.openPositions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No open positions. Identify setups on the Dashboard or Coin details to execute paper trades.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(portfolio.openPositions) { pos ->
                OpenPositionCard(
                    position = pos,
                    onCloseClick = {
                        viewModel.closePaperPositionManually(pos.id, pos.currentPrice)
                    }
                )
            }
        }

        // Closed Trades Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Closed Trade History (${portfolio.closedTrades.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Closed Trades List
        if (portfolio.closedTrades.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No closed trades recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(portfolio.closedTrades.filter { it.side == "SELL" }) { trade ->
                val isProfit = trade.pnl >= 0.0
                val pnlColor = if (isProfit) BullishGreen else BearishRed
                val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(trade.timestamp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = trade.symbol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = trade.reason,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Exit: $${String.format("%.2f", trade.price)} | Qty: ${String.format("%.4f", trade.quantity)} | $dateStr",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isProfit) "+" else ""}$${String.format("%.2f", trade.pnl)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = pnlColor
                            )
                            Text(
                                text = "${if (isProfit) "+" else ""}${String.format("%.2f", trade.pnlPercent)}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pnlColor
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Paper Account") },
            text = { Text("This will close all open positions, clear trade logs, and reset your virtual balance to $10,000 USDT.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPaperData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OpenPositionCard(
    position: PaperPosition,
    onCloseClick: () -> Unit
) {
    val pnl = position.unrealizedPnl
    val pnlPct = position.unrealizedPnlPercent
    val isProfit = pnl >= 0.0
    val pnlColor = if (isProfit) BullishGreen else BearishRed

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = position.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BullishGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("SPOT LONG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isProfit) "+" else ""}$${String.format("%.2f", pnl)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                    Text(
                        text = "${if (isProfit) "+" else ""}${String.format("%.2f", pnlPct)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format("%.2f", position.entryPrice)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Current Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format("%.2f", position.currentPrice)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Stop Loss", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format("%.2f", position.stopLoss)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BearishRed)
                }
                Column {
                    Text("Take Profit 1", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${String.format("%.2f", position.takeProfit1)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BullishGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invested: $${String.format("%.2f", position.investedUsdt)} (${String.format("%.4f", position.quantity)} units)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onCloseClick,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Market Close", fontSize = 11.sp)
                }
            }
        }
    }
}
