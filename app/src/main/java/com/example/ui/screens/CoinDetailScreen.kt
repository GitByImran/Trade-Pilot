package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.model.TimeFrame
import com.example.ui.TradePilotViewModel
import com.example.ui.components.CandlestickChart
import com.example.ui.components.OrderBookView
import com.example.ui.components.SignalCard
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.NeutralBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    symbol: String,
    viewModel: TradePilotViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tickers by viewModel.tickers.collectAsState()
    val ticker = tickers[symbol]
    val selectedTf by viewModel.selectedTimeframe.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val indicators by viewModel.indicatorSnapshot.collectAsState()
    val activeSignal by viewModel.activeSignal.collectAsState()
    val orderBookMap by viewModel.orderBookDepthMap.collectAsState()
    val orderBook = orderBookMap[symbol]
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAnalyzingAi by viewModel.isAnalyzingAi.collectAsState()
    val connectedAccount by viewModel.connectedAccount.collectAsState()
    val portfolioAdvisories by viewModel.portfolioAdvisories.collectAsState()
    val coinAdvisory = portfolioAdvisories.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }

    val currentPrice = ticker?.lastPrice ?: (candles.lastOrNull()?.close ?: 0.0)
    val pct = ticker?.priceChangePercent ?: 0.0
    val isUp = pct >= 0.0
    val priceColor = if (isUp) BullishGreen else BearishRed

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("coin_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = symbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Binance Spot",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = if (currentPrice > 0) "$${String.format("%.2f", currentPrice)}" else "...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = priceColor
                            )
                            Text(
                                text = "${if (isUp) "+" else ""}${String.format("%.2f", pct)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = priceColor
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Timeframe Selector Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(TimeFrame.entries) { tf ->
                        val isSelected = selectedTf == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BinanceGold else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.selectTimeframe(tf) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tf.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Real-Time Candlestick Chart
            item {
                CandlestickChart(
                    candles = candles,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 24h Stats Strip
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("24h High", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%.2f", ticker?.highPrice ?: 0.0)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("24h Low", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%.2f", ticker?.lowPrice ?: 0.0)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("24h Volume", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format("%,.1f", ticker?.volume ?: 0.0)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Quote Vol", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format("%,.0f", ticker?.quoteVolume ?: 0.0)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Technical Indicators Snapshot Strip
            if (indicators != null) {
                item {
                    val ind = indicators!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Technical Indicators Snapshot",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("RSI (14)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val rsiVal = ind.rsi14 ?: 0.0
                                    val rsiColor = if (rsiVal in 45.0..65.0) BullishGreen else if (rsiVal > 70) BearishRed else BinanceGold
                                    Text("${String.format("%.1f", rsiVal)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = rsiColor)
                                }
                                Column {
                                    Text("MACD Hist", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val hist = ind.macdHistogram ?: 0.0
                                    Text("${String.format("%.2f", hist)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (hist >= 0) BullishGreen else BearishRed)
                                }
                                Column {
                                    Text("ATR (14)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$${String.format("%.2f", ind.atr14 ?: 0.0)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Support", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$${String.format("%.2f", ind.supportLevel ?: 0.0)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BullishGreen)
                                }
                                Column {
                                    Text("Resistance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$${String.format("%.2f", ind.resistanceLevel ?: 0.0)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BearishRed)
                                }
                            }
                        }
                    }
                }
            }

            // Personalized Portfolio-Aware Trade Advisory (When, What, How to Trade)
            if (coinAdvisory != null) {
                item {
                    val adv = coinAdvisory!!
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("portfolio_trade_advisor_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (adv.action.startsWith("BUY"))
                                BullishGreen.copy(alpha = 0.12f)
                            else if (adv.action.startsWith("SELL"))
                                BearishRed.copy(alpha = 0.12f)
                            else
                                BinanceGold.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = BinanceGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Portfolio-Aware Advisory",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (adv.action.startsWith("BUY")) BullishGreen
                                    else if (adv.action.startsWith("SELL")) BearishRed
                                    else BinanceGold
                                ) {
                                    Text(
                                        text = adv.action,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = adv.reasonHeadline,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = adv.rationale,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = if (connectedAccount.isConnected) "Your Live Binance USDT" else "Paper Trading Capital",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$${String.format("%,.2f", adv.userFreeCapitalUsdt)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Recommended Sizing", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "$${String.format("%,.1f", adv.recommendedAllocationUsdt)} USDT",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = BinanceGold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Order Type", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("LIMIT / DCA", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column {
                                            Text("Recommended Qty", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${String.format("%.4f", adv.recommendedQuantity)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Risk on Portfolio", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${String.format("%.1f", adv.portfolioRiskPercent)}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BullishGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Signal Engine Decision Support Card
            if (activeSignal != null) {
                item {
                    Text(
                        text = "Algorithmic Setup Evaluation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SignalCard(
                        signal = activeSignal!!,
                        onPaperTradeClick = { viewModel.executePaperTrade(activeSignal!!) },
                        onAiExplainClick = { viewModel.analyzeSetupWithAi(activeSignal!!) }
                    )
                }
            }

            // AI Decision-Support Analysis (Gemini)
            item {
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
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BinanceGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini AI Analyst Explanation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (activeSignal != null) {
                                Button(
                                    onClick = { viewModel.analyzeSetupWithAi(activeSignal!!) },
                                    enabled = !isAnalyzingAi,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BinanceGold)
                                ) {
                                    if (isAnalyzingAi) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Black
                                        )
                                    } else {
                                        Text("Analyze", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (aiAnalysis != null) {
                            val ai = aiAnalysis!!
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = ai.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BinanceGold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Market Structure: ${ai.marketCondition}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Technical Rationale: ${ai.setupExplanation}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Key Risks: ${ai.keyRisks}",
                                fontSize = 11.sp,
                                color = BearishRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Invalidation: ${ai.invalidationCondition}",
                                fontSize = 11.sp,
                                color = NeutralBlue
                            )
                        } else {
                            Text(
                                text = "Tap 'Analyze' to invoke Gemini AI for a structured explanation grounded strictly in the calculated metrics.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // Order Book Depth
            item {
                OrderBookView(orderBook = orderBook)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
