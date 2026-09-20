package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ConnectionStatus
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BinanceGold
import com.example.ui.theme.BinanceGoldSubtle
import com.example.ui.theme.BullishGreen

@Composable
fun StatusHeader(
    connectionStatus: ConnectionStatus,
    lastUpdateTimestamp: Long,
    modifier: Modifier = Modifier
) {
    val isStale = (System.currentTimeMillis() - lastUpdateTimestamp) > 15_000L &&
            connectionStatus == ConnectionStatus.CONNECTED

    Column(modifier = modifier.fillMaxWidth()) {
        // Mode Banner: Mandatory prominent Paper Trading notice
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BinanceGoldSubtle,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Paper Trading Active",
                        tint = BinanceGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PAPER TRADING — NO REAL ORDERS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BinanceGold,
                        letterSpacing = 0.5.sp
                    )
                }

                // WebSocket Connection Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (dotColor, label) = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> Pair(BullishGreen, "Binance Live")
                        ConnectionStatus.CONNECTING -> Pair(BinanceGold, "Connecting...")
                        ConnectionStatus.RECONNECTING -> Pair(BinanceGold, "Reconnecting...")
                        ConnectionStatus.DISCONNECTED -> Pair(Color.Gray, "Offline")
                        ConnectionStatus.ERROR -> Pair(BearishRed, "Conn Error")
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Stale Data Warning
        AnimatedVisibility(visible = isStale) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                color = BearishRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = BearishRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Market data delayed. Awaiting live ticks from Binance streams...",
                        fontSize = 11.sp,
                        color = BearishRed
                    )
                }
            }
        }
    }
}
