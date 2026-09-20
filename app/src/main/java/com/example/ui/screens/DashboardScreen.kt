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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.SignalType
import com.example.ui.TradePilotViewModel
import com.example.ui.components.SignalCard
import com.example.ui.components.StatusHeader
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.NeutralBlue

@Composable
fun DashboardScreen(
    viewModel: TradePilotViewModel,
    onNavigateToCoin: (String) -> Unit,
    onNavigateToPaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val lastUpdate by viewModel.lastMessageTimestamp.collectAsState()
    val tickers by viewModel.tickers.collectAsState()
    val signalsMap by viewModel.signalsMap.collectAsState()
    val portfolio by viewModel.paperPortfolio.collectAsState()
    val connectedAccount by viewModel.connectedAccount.collectAsState()
    val portfolioAdvisories by viewModel.portfolioAdvisories.collectAsState()
    val aiBriefing by viewModel.aiBriefing.collectAsState()
    val isGeneratingBriefing by viewModel.isGeneratingBriefing.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    var showBriefingModal by remember { mutableStateOf(false) }

    val topSetups = remember(signalsMap) {
        signalsMap.values.filter { it.score >= 6.5 }.sortedByDescending { it.score }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status & Mode banner
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusHeader(
                connectionStatus = connectionStatus,
                lastUpdateTimestamp = lastUpdate
            )
        }

        // AI Market Briefing Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("market_briefing_card"),
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
                                text = "AI Market Briefing",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.generateMarketBriefing()
                                showBriefingModal = true
                            },
                            enabled = !isGeneratingBriefing,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BinanceGold),
                            modifier = Modifier.testTag("generate_briefing_button")
                        ) {
                            if (isGeneratingBriefing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            } else {
                                Text("Briefing", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (aiBriefing != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiBriefing!!.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BinanceGold
                        )
                        Text(
                            text = aiBriefing!!.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bullish Lead: ${aiBriefing!!.strongestBullish}",
                                fontSize = 10.sp,
                                color = BullishGreen
                            )
                        }
                    } else {
                        Text(
                            text = "Generate a Gemini AI intelligence summary across all watched Binance Spot markets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Real Binance Account Connection Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("binance_status_bar"),
                colors = CardDefaults.cardColors(
                    containerColor = if (connectedAccount.isConnected)
                        MaterialTheme.colorScheme.surfaceVariant
                    else
                        BinanceGold.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = BinanceGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (connectedAccount.isConnected) "REAL BINANCE ACCOUNT SYNCED" else "REAL BINANCE: UNLINKED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (connectedAccount.isConnected) BullishGreen else BinanceGold
                            )
                            Text(
                                text = if (connectedAccount.isConnected)
                                    "Live Available: $${String.format("%,.2f", connectedAccount.freeUsdt)} USDT"
                                else
                                    "Link your API key in Settings to get tailored asset-specific trade sizing",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (connectedAccount.isConnected) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BullishGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BullishGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Portfolio-Aware Trade Advisor (When, What, How to trade based on holdings & available USDT)
        if (portfolioAdvisories.isNotEmpty()) {
            item {
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
                            text = "Portfolio Trade Advisor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Asset & Capital Aware",
                        style = MaterialTheme.typography.labelSmall,
                        color = BinanceGold
                    )
                }
            }

            items(portfolioAdvisories.take(3)) { advice ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToCoin(advice.symbol) }
                        .testTag("advisory_card_${advice.symbol}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = advice.symbol,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (advice.action.startsWith("BUY")) BullishGreen.copy(alpha = 0.2f)
                                    else if (advice.action.startsWith("SELL")) BearishRed.copy(alpha = 0.2f)
                                    else BinanceGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = advice.action,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (advice.action.startsWith("BUY")) BullishGreen
                                        else if (advice.action.startsWith("SELL")) BearishRed
                                        else BinanceGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Score: ${String.format("%.1f", advice.confidenceScore)}/10",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (advice.confidenceScore >= 7.5) BullishGreen else BinanceGold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = advice.rationale,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Suggested Alloc", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${String.format("%,.1f", advice.recommendedAllocationUsdt)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BinanceGold)
                            }
                            Column {
                                Text("Entry Limit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${String.format("%.2f", advice.entryPrice)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Stop Loss", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${String.format("%.2f", advice.stopLoss)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BearishRed)
                            }
                            Column {
                                Text("Take Profit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$${String.format("%.2f", advice.takeProfit1)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BullishGreen)
                            }
                        }
                    }
                }
            }
        }

        // Paper Portfolio Quick Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPaper() }
                    .testTag("portfolio_quick_bar"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PAPER PORTFOLIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$${String.format("%,.2f", portfolio.totalEquity)} USDT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Open Trades", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${portfolio.openPositions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Unrealized P&L", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val pnlColor = if (portfolio.totalUnrealizedPnl >= 0) BullishGreen else BearishRed
                            Text(
                                text = "${if (portfolio.totalUnrealizedPnl >= 0) "+" else ""}$${String.format("%.2f", portfolio.totalUnrealizedPnl)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = pnlColor
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "View Portfolio",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Top Opportunities Section
        if (topSetups.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "High-Conviction Setups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${topSetups.size} Qualified",
                        style = MaterialTheme.typography.labelSmall,
                        color = BullishGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(topSetups.take(2)) { signal ->
                SignalCard(
                    signal = signal,
                    onPaperTradeClick = { viewModel.executePaperTrade(signal) },
                    onAiExplainClick = {
                        viewModel.selectSymbol(signal.symbol)
                        viewModel.analyzeSetupWithAi(signal)
                        onNavigateToCoin(signal.symbol)
                    }
                )
            }
        }

        // Watchlist Table Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Binance Spot Watchlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${userSettings.watchedSymbols.size} Pairs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Watchlist Table Items
        items(userSettings.watchedSymbols) { symbol ->
            val ticker = tickers[symbol]
            val signal = signalsMap[symbol]
            val price = ticker?.lastPrice ?: 0.0
            val pct = ticker?.priceChangePercent ?: 0.0
            val isUp = pct >= 0.0
            val priceColor = if (isUp) BullishGreen else BearishRed

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCoin(symbol) }
                    .testTag("pair_card_$symbol"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Symbol + Signal badge
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = symbol,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (signal != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                val scoreColor = if (signal.score >= 7.0) BullishGreen else if (signal.score >= 5.0) BinanceGold else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(scoreColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${signal.score}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = scoreColor
                                    )
                                }
                            }
                        }
                        Text(
                            text = "24h Vol: $${String.format("%,.0f", (ticker?.quoteVolume ?: 0.0))}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Price + 24h % Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (price > 0) "$${String.format("%.2f", price)}" else "...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${if (isUp) "+" else ""}${String.format("%.2f", pct)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = priceColor
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Open chart",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
