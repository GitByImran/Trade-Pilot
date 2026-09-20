package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BacktestConfig
import com.example.data.model.BacktestResult
import com.example.data.model.TimeFrame
import com.example.ui.TradePilotViewModel
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BullishGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun BacktestScreen(
    viewModel: TradePilotViewModel,
    modifier: Modifier = Modifier
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val backtestResult by viewModel.backtestResult.collectAsState()
    val isBacktesting by viewModel.isBacktesting.collectAsState()

    var selectedSymbol by remember { mutableStateOf("BTCUSDT") }
    var selectedTimeframe by remember { mutableStateOf(TimeFrame.M15) }
    var minScore by remember { mutableDoubleStateOf(7.0) }
    var riskPct by remember { mutableDoubleStateOf(1.0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("backtest_screen")
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Deterministic Strategy Backtesting",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Runs the identical 0-10 signal engine historically with strict look-ahead bias prevention, 0.1% Binance fees, and slippage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("backtest_config_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Select Symbol", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(userSettings.watchedSymbols) { sym ->
                            val isSel = selectedSymbol == sym
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) BinanceGold else MaterialTheme.colorScheme.surface)
                                    .clickable { selectedSymbol = sym }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sym,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select Timeframe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(listOf(TimeFrame.M5, TimeFrame.M15, TimeFrame.H1, TimeFrame.H4)) { tf ->
                            val isSel = selectedTimeframe == tf
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) BinanceGold else MaterialTheme.colorScheme.surface)
                                    .clickable { selectedTimeframe = tf }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tf.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Min Score to Trade: ${String.format("%.1f", minScore)}/10", fontSize = 11.sp)
                        Text("Risk Per Trade: ${String.format("%.1f", riskPct)}%", fontSize = 11.sp)
                    }
                    Slider(
                        value = minScore.toFloat(),
                        onValueChange = { minScore = it.toDouble() },
                        valueRange = 5.0f..9.0f,
                        steps = 7
                    )

                    Button(
                        onClick = {
                            viewModel.runBacktest(
                                BacktestConfig(
                                    symbol = selectedSymbol,
                                    timeframe = selectedTimeframe,
                                    startingBalance = 10000.0,
                                    riskPerTradePercent = riskPct,
                                    minScoreToTrade = minScore,
                                    feePercent = 0.1,
                                    slippagePercent = 0.05
                                )
                            )
                        },
                        enabled = !isBacktesting,
                        modifier = Modifier.fillMaxWidth().testTag("run_backtest_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BinanceGold)
                    ) {
                        if (isBacktesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulating Historical Candles...", color = Color.Black)
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Strategy Simulation", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Backtest Results
        if (backtestResult != null) {
            val result = backtestResult!!
            val isNetProfit = result.netPnlUsdt >= 0.0
            val pnlColor = if (isNetProfit) BullishGreen else BearishRed

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("backtest_results_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simulation Results",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${result.totalCandlesAnalyzed} Candles Analyzed",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // KPI Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Net P&L", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${if (isNetProfit) "+" else ""}$${String.format("%.2f", result.netPnlUsdt)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pnlColor
                                )
                                Text(
                                    "${if (isNetProfit) "+" else ""}${String.format("%.2f", result.netPnlPercent)}%",
                                    fontSize = 11.sp,
                                    color = pnlColor
                                )
                            }
                            Column {
                                Text("Win Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", result.winRatePercent)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                                Text("${result.winningTrades}W / ${result.losingTrades}L", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column {
                                Text("Profit Factor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.2f", result.profitFactor)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Max Drawdown", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("-${String.format("%.2f", result.maxDrawdownPercent)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BearishRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Trades: ${result.totalTrades}", fontSize = 11.sp)
                            Text("Gross Profit: +$${String.format("%.2f", result.grossProfit)}", fontSize = 11.sp, color = BullishGreen)
                            Text("Total Fees: -$${String.format("%.2f", result.totalFeesUsdt)}", fontSize = 11.sp, color = BearishRed)
                        }

                        // Equity Curve Canvas
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Simulated Equity Curve", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 6.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val pts = result.equityCurve
                                if (pts.size < 2) return@Canvas

                                val minEq = pts.minOf { it.equity } * 0.99
                                val maxEq = pts.maxOf { it.equity } * 1.01
                                val range = max(1.0, maxEq - minEq)

                                val path = Path()
                                pts.forEachIndexed { idx, pt ->
                                    val x = (idx.toFloat() / (pts.size - 1)) * size.width
                                    val y = (size.height * (1.0 - (pt.equity - minEq) / range)).toFloat()
                                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }

                                drawPath(
                                    path = path,
                                    color = if (isNetProfit) BullishGreen else BearishRed,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // Trade Execution Log
            item {
                Text(
                    text = "Backtest Trade Log (${result.tradeLog.filter { it.side == "SELL" }.size} closed trades)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(result.tradeLog.filter { it.side == "SELL" }) { trade ->
                val isWin = trade.pnl > 0.0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(trade.symbol, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(trade.reason, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isWin) "+" else ""}$${String.format("%.2f", trade.pnl)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isWin) BullishGreen else BearishRed
                            )
                            Text(
                                text = "${if (isWin) "+" else ""}${String.format("%.2f", trade.pnlPercent)}%",
                                fontSize = 10.sp,
                                color = if (isWin) BullishGreen else BearishRed
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
}
