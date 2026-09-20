package com.example.data.remote

import android.util.Log
import com.example.data.model.TradingSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TelegramService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun sendSignalAlert(
        botToken: String,
        chatId: String,
        signal: TradingSignal
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Telegram bot token or chat ID is not configured"))
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(signal.timestamp))
        val reasonsText = signal.reasons.take(3).joinToString("\n") { "• $it" }
        val warningsText = if (signal.warnings.isNotEmpty()) "\nWarnings:\n" + signal.warnings.take(2).joinToString("\n") { "⚠ $it" } else ""

        val message = """
            🚨 <b>TRADEPILOT ALERT</b> 🚨
            
            <b>Pair:</b> ${signal.symbol}
            <b>Signal:</b> ${signal.signalType.label}
            <b>Score:</b> ${signal.score}/10
            <b>Timeframe:</b> ${signal.timeframe.displayName}
            <b>Price:</b> $${String.format("%.2f", signal.price)}
            
            <b>Suggested Entry:</b> $${String.format("%.2f", signal.suggestedEntryMin)} - $${String.format("%.2f", signal.suggestedEntryMax)}
            <b>Stop Loss:</b> $${String.format("%.2f", signal.stopLoss)}
            <b>Target 1:</b> $${String.format("%.2f", signal.takeProfit1)}
            <b>Target 2:</b> $${String.format("%.2f", signal.takeProfit2)}
            <b>R:R:</b> 1:${signal.riskRewardRatio}
            <b>Market Regime:</b> ${signal.marketRegime.label}
            
            <b>Key Confluences:</b>
            $reasonsText$warningsText
            
            <i>Timestamp: $dateStr</i>
            <i>Paper Trading Simulation • Not Financial Advice</i>
        """.trimIndent()

        sendMessage(botToken, chatId, message)
    }

    suspend fun testConnection(botToken: String, chatId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val testMsg = "✅ <b>TradePilot Connection Test</b>\n\nYour Telegram bot notifications are configured correctly!"
        sendMessage(botToken, chatId, testMsg)
    }

    private fun sendMessage(botToken: String, chatId: String, text: String): Result<Boolean> {
        return try {
            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val formBody = FormBody.Builder()
                .add("chat_id", chatId)
                .add("text", text)
                .add("parse_mode", "HTML")
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val body = response.body?.string() ?: ""
                    Log.w("TelegramService", "Failed to send: ${response.code} $body")
                    Result.failure(Exception("Telegram API error (${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Log.e("TelegramService", "Exception sending telegram message", e)
            Result.failure(e)
        }
    }
}
