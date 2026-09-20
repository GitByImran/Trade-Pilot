package com.example.domain.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.model.PaperPosition
import com.example.data.model.PositionStatus
import com.example.data.model.TradingSignal
import com.example.data.model.UserSettings
import com.example.data.remote.TelegramService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AlertManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Symbol -> Last alert timestamp in ms
    private val alertCooldownMap = ConcurrentHashMap<String, Long>()
    private val COOLDOWN_MS = 20 * 60 * 1000L // 20 minutes cooldown per symbol

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TradePilot Trading Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High confidence signals and paper trading execution alerts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun onSignalGenerated(signal: TradingSignal, settings: UserSettings) {
        if (!settings.notificationsEnabled) return
        if (signal.score < settings.signalScoreThreshold) return

        val now = System.currentTimeMillis()
        val lastAlert = alertCooldownMap[signal.symbol] ?: 0L
        if (now - lastAlert < COOLDOWN_MS) {
            Log.d("AlertManager", "Alert for ${signal.symbol} suppressed due to active cooldown")
            return
        }
        alertCooldownMap[signal.symbol] = now

        // Android System Notification
        showSystemNotification(
            title = "${signal.symbol} ${signal.signalType.label} (Score: ${signal.score}/10)",
            body = "Entry: $${String.format("%.2f", signal.suggestedEntryMin)}-$${String.format("%.2f", signal.suggestedEntryMax)} | TP: $${String.format("%.2f", signal.takeProfit1)} | SL: $${String.format("%.2f", signal.stopLoss)}",
            notificationId = signal.symbol.hashCode()
        )

        // Telegram Notification if enabled
        if (settings.telegramEnabled && settings.telegramBotToken.isNotBlank() && settings.telegramChatId.isNotBlank()) {
            scope.launch {
                TelegramService.sendSignalAlert(
                    botToken = settings.telegramBotToken,
                    chatId = settings.telegramChatId,
                    signal = signal
                )
            }
        }
    }

    fun onPaperTradeExecuted(position: PaperPosition, status: PositionStatus, settings: UserSettings) {
        if (!settings.notificationsEnabled) return

        val title = when (status) {
            PositionStatus.CLOSED_TP1 -> "🎯 TAKE PROFIT HIT: ${position.symbol}"
            PositionStatus.CLOSED_TP2 -> "🎯🎯 TP2 HIT: ${position.symbol}"
            PositionStatus.CLOSED_STOP_LOSS -> "🛑 STOP LOSS HIT: ${position.symbol}"
            else -> "Paper Trade Updated: ${position.symbol}"
        }

        val pnlText = if (position.realizedPnl >= 0) "+$${String.format("%.2f", position.realizedPnl)}" else "-$${String.format("%.2f", kotlin.math.abs(position.realizedPnl))}"
        val body = "Result: $pnlText (${String.format("%.2f", position.realizedPnlPercent)}%) at $${String.format("%.2f", position.closePrice ?: 0.0)}"

        showSystemNotification(title, body, (position.id + status.name).hashCode())
    }

    private fun showSystemNotification(title: String, body: String, notificationId: Int) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Log.w("AlertManager", "Failed to display system notification: ${e.message}")
        }
    }

    companion object {
        private const val CHANNEL_ID = "tradepilot_alerts_channel"
    }
}
