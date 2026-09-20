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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TradePilotViewModel
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BinanceGoldSubtle
import com.example.ui.theme.BullishGreen
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: TradePilotViewModel,
    modifier: Modifier = Modifier
) {
    val currentSettings by viewModel.userSettings.collectAsState()
    val scope = rememberCoroutineScope()

    var scoreThreshold by remember(currentSettings) { mutableDoubleStateOf(currentSettings.signalScoreThreshold) }
    var riskPerTrade by remember(currentSettings) { mutableDoubleStateOf(currentSettings.riskPerTradePercent) }
    var maxOpenTrades by remember(currentSettings) { mutableIntStateOf(currentSettings.maxOpenTrades) }
    var minRr by remember(currentSettings) { mutableDoubleStateOf(currentSettings.minRiskRewardRatio) }
    var notificationsEnabled by remember(currentSettings) { mutableStateOf(currentSettings.notificationsEnabled) }

    var telegramEnabled by remember(currentSettings) { mutableStateOf(currentSettings.telegramEnabled) }
    var telegramToken by remember(currentSettings) { mutableStateOf(currentSettings.telegramBotToken) }
    var telegramChatId by remember(currentSettings) { mutableStateOf(currentSettings.telegramChatId) }

    var binanceApiKey by remember(currentSettings) { mutableStateOf(currentSettings.binanceApiKey) }
    var binanceApiSecret by remember(currentSettings) { mutableStateOf(currentSettings.binanceApiSecret) }

    val connectedAccount by viewModel.connectedAccount.collectAsState()
    val isSyncingAccount by viewModel.isSyncingAccount.collectAsState()

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTestingTelegram by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Application Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Configure risk management, signal thresholds, and alert channels.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Real Binance Account Connection & Synchronization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("binance_sync_card"),
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
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = BinanceGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Connect Real Binance Account",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Sync assets to get personalized When, What & How to trade advice",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (connectedAccount.isConnected) BullishGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = if (connectedAccount.isConnected) "CONNECTED" else "UNLINKED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (connectedAccount.isConnected) BullishGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (connectedAccount.isConnected) {
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
                                        Text("Available USDT (Free)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "$${String.format("%,.2f", connectedAccount.freeUsdt)} USDT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = BinanceGold
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Total Portfolio Est.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "$${String.format("%,.2f", connectedAccount.totalEstimatedUsdt)} USDT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (connectedAccount.assets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Held Assets: " + connectedAccount.assets.filter { it.free > 0.0001 }
                                            .joinToString(", ") { "${it.asset} (${String.format("%.3f", it.free)})" },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (binanceApiKey.isNotBlank() && binanceApiSecret.isNotBlank()) {
                                        viewModel.syncBinanceAccount(binanceApiKey, binanceApiSecret)
                                    }
                                },
                                enabled = !isSyncingAccount,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncingAccount) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh Balances", fontSize = 11.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.disconnectBinanceAccount()
                                    binanceApiKey = ""
                                    binanceApiSecret = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disconnect", fontSize = 11.sp, color = BearishRed)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = binanceApiKey,
                            onValueChange = { binanceApiKey = it },
                            label = { Text("Binance API Key") },
                            placeholder = { Text("Paste your API Key here") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = binanceApiSecret,
                            onValueChange = { binanceApiSecret = it },
                            label = { Text("Binance API Secret") },
                            placeholder = { Text("Paste your API Secret here") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = BullishGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Security: Read-Only keys recommended. Keys are stored locally on your device.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.syncBinanceAccount(binanceApiKey, binanceApiSecret)
                            },
                            enabled = !isSyncingAccount && binanceApiKey.isNotBlank() && binanceApiSecret.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = BinanceGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncingAccount) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection & Sync Assets", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        connectedAccount.errorMessage?.let { err ->
                            Text(
                                text = err,
                                fontSize = 11.sp,
                                color = BearishRed,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live Trading Interlock Status
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("live_trading_status_banner"),
                color = BearishRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Live Trading Disabled",
                        tint = BearishRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LIVE TRADING: STRICTLY DISABLED",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BearishRed
                        )
                        Text(
                            text = "Version 1.0 executes paper trades only. Real Binance Spot order execution is isolated in a disabled architecture adapter.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Signal Scoring Threshold
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Signal Engine Configuration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Signal Score Threshold", fontSize = 12.sp)
                        Text("${String.format("%.1f", scoreThreshold)} / 10", fontWeight = FontWeight.Bold, color = BinanceGold)
                    }
                    Slider(
                        value = scoreThreshold.toFloat(),
                        onValueChange = { scoreThreshold = it.toDouble() },
                        valueRange = 5.0f..9.0f,
                        steps = 7
                    )
                    Text(
                        text = "Setups scoring above this threshold trigger notifications and qualify for automated paper tracking.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Risk Management Rules
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("risk_settings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Risk Management Engine",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Risk Per Trade", fontSize = 12.sp)
                        Text("${String.format("%.1f", riskPerTrade)}% of balance", fontWeight = FontWeight.Bold, color = BullishGreen)
                    }
                    Slider(
                        value = riskPerTrade.toFloat(),
                        onValueChange = { riskPerTrade = it.toDouble() },
                        valueRange = 0.5f..5.0f,
                        steps = 8
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max Open Trades", fontSize = 12.sp)
                        Text("$maxOpenTrades positions", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = maxOpenTrades.toFloat(),
                        onValueChange = { maxOpenTrades = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Minimum Risk/Reward", fontSize = 12.sp)
                        Text("1:${String.format("%.1f", minRr)}", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = minRr.toFloat(),
                        onValueChange = { minRr = it.toDouble() },
                        valueRange = 1.0f..3.0f,
                        steps = 3
                    )
                }
            }
        }

        // Telegram Integration
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("telegram_settings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Telegram Bot Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Receive formatted alerts on Telegram", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = telegramEnabled,
                            onCheckedChange = { telegramEnabled = it }
                        )
                    }

                    if (telegramEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = telegramToken,
                            onValueChange = { telegramToken = it },
                            label = { Text("Telegram Bot Token") },
                            placeholder = { Text("123456:ABC-DEF...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = telegramChatId,
                            onValueChange = { telegramChatId = it },
                            label = { Text("Telegram Chat ID") },
                            placeholder = { Text("-100123456789") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingTelegram = true
                                    val res = viewModel.testTelegram(telegramToken, telegramChatId)
                                    testStatusMessage = if (res.isSuccess) {
                                        "Test message sent successfully!"
                                    } else {
                                        "Test failed: ${res.exceptionOrNull()?.message}"
                                    }
                                    isTestingTelegram = false
                                }
                            },
                            enabled = !isTestingTelegram && telegramToken.isNotBlank() && telegramChatId.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTestingTelegram) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp))
                            } else {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Test Telegram Alert", fontSize = 11.sp)
                            }
                        }

                        testStatusMessage?.let { msg ->
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = if (msg.contains("success")) BullishGreen else BearishRed,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Save Settings Action Button
        item {
            Button(
                onClick = {
                    viewModel.saveSettings(
                        currentSettings.copy(
                            signalScoreThreshold = scoreThreshold,
                            riskPerTradePercent = riskPerTrade,
                            maxOpenTrades = maxOpenTrades,
                            minRiskRewardRatio = minRr,
                            notificationsEnabled = notificationsEnabled,
                            telegramEnabled = telegramEnabled,
                            telegramBotToken = telegramToken,
                            telegramChatId = telegramChatId,
                            binanceApiKey = binanceApiKey,
                            binanceApiSecret = binanceApiSecret
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag("save_settings_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BinanceGold)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Settings", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
