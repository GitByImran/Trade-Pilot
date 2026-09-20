package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BullishGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SignalsHistoryScreen(
    viewModel: TradePilotViewModel,
    onSelectSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val signalsHistory by viewModel.signalsHistory.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(signalsHistory, selectedFilter) {
        when (selectedFilter) {
            "BUY" -> signalsHistory.filter { it.signalType == SignalType.BUY_SETUP }
            "SELL" -> signalsHistory.filter { it.signalType == SignalType.SELL_EXIT_SETUP }
            "HIGH_SCORE" -> signalsHistory.filter { it.score >= 7.5 }
            else -> signalsHistory
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("signals_history_screen")
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Historical Signals & Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "All algorithmic setups recorded by the deterministic engine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Filter Pills
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf("ALL", "BUY", "SELL", "HIGH_SCORE")
                items(filters) { f ->
                    val isSel = selectedFilter == f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) BinanceGold else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedFilter = f }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (f) {
                                "ALL" -> "All Signals"
                                "BUY" -> "Buy Setups"
                                "SELL" -> "Sell / Exit"
                                "HIGH_SCORE" -> "Score ≥ 7.5"
                                else -> f
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
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
                            text = "No recorded signals matching filter. As live Binance market ticks are monitored, qualified signals are archived here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredList) { signal ->
                SignalCard(
                    signal = signal,
                    onPaperTradeClick = {
                        viewModel.executePaperTrade(signal)
                    },
                    onAiExplainClick = {
                        viewModel.selectSymbol(signal.symbol)
                        viewModel.analyzeSetupWithAi(signal)
                        onSelectSymbol(signal.symbol)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
