package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketRegime
import com.example.data.model.SignalType
import com.example.data.model.TradingSignal
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BearishRedSubtle
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BinanceGoldSubtle
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.BullishGreenSubtle
import com.example.ui.theme.NeutralBlue

@Composable
fun SignalCard(
    signal: TradingSignal,
    onPaperTradeClick: () -> Unit,
    onAiExplainClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val scoreColor = when {
        signal.score >= 8.5 -> BullishGreen
        signal.score >= 7.0 -> BullishGreen
        signal.score >= 5.0 -> BinanceGold
        signal.score >= 3.0 -> Color(0xFFFF9800)
        else -> BearishRed
    }

    val signalBgColor = when (signal.signalType) {
        SignalType.BUY_SETUP -> BullishGreenSubtle
        SignalType.SELL_EXIT_SETUP -> BearishRedSubtle
        SignalType.WAIT -> BinanceGoldSubtle
        SignalType.NO_TRADE -> Color(0x159E9E9E)
    }

    val regimeColor = when (signal.marketRegime) {
        MarketRegime.BULLISH -> BullishGreen
        MarketRegime.BEARISH -> BearishRed
        MarketRegime.SIDEWAYS -> NeutralBlue
        MarketRegime.HIGH_VOLATILITY -> Color(0xFFFF9800)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Symbol + Score Badge + Signal Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = signal.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(regimeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = signal.marketRegime.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = regimeColor
                        )
                    }
                }

                // Score pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(scoreColor.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Score: ${signal.score}/10",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }

            // Signal Type Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(signalBgColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = signal.signalType.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "R:R 1:${signal.riskRewardRatio}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Entry, Stop Loss, Target 1, Target 2 Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Entry Zone", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$${String.format("%.2f", signal.suggestedEntryMin)}-$${String.format("%.2f", signal.suggestedEntryMax)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text("Stop Loss", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$${String.format("%.2f", signal.stopLoss)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BearishRed
                    )
                }
                Column {
                    Text("Take Profit 1", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$${String.format("%.2f", signal.takeProfit1)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BullishGreen
                    )
                }
                Column {
                    Text("Take Profit 2", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$${String.format("%.2f", signal.takeProfit2)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BullishGreen
                    )
                }
            }

            // Quick Reasons Preview
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                signal.reasons.take(2).forEach { reason ->
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BullishGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = reason,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (signal.warnings.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = BinanceGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = signal.warnings.first(),
                            fontSize = 11.sp,
                            color = BinanceGold
                        )
                    }
                }
            }

            // Expandable details for full breakdown
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Itemized Score Confluences (0 to 10):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    signal.scoreBreakdown.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${item.category}: ${item.explanation}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${item.pointsAwarded}/${item.maxPoints}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.pointsAwarded > 0) BullishGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (signal.reasons.size > 2) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Additional Reasons:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        signal.reasons.drop(2).forEach { reason ->
                            Text(
                                text = "✓ $reason",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }

                    if (signal.warnings.size > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Risk Warnings:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BinanceGold
                        )
                        signal.warnings.drop(1).forEach { warning ->
                            Text(
                                text = "⚠ $warning",
                                fontSize = 11.sp,
                                color = BinanceGold,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Expand Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Show Less" else "View Score Breakdown (${signal.scoreBreakdown.size} Factors)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAiExplainClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BinanceGold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Explain", fontSize = 12.sp)
                }

                Button(
                    onClick = onPaperTradeClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (signal.score >= 7.0) BullishGreen else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paper Trade", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
