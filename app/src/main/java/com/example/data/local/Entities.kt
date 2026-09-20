package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signals")
data class SignalEntity(
    @PrimaryKey
    val id: String,
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val price: Double,
    val signalType: String,
    val score: Double,
    val marketRegime: String,
    val suggestedEntryMin: Double,
    val suggestedEntryMax: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val riskRewardRatio: Double,
    val reasonsJson: String,
    val warningsJson: String,
    val status: String
)

@Entity(tableName = "paper_positions")
data class PaperPositionEntity(
    @PrimaryKey
    val id: String,
    val symbol: String,
    val entryPrice: Double,
    val currentPrice: Double,
    val quantity: Double,
    val investedUsdt: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val openedAt: Long,
    val status: String,
    val entryFee: Double,
    val exitFee: Double,
    val closedAt: Long?,
    val closePrice: Double?,
    val realizedPnl: Double,
    val realizedPnlPercent: Double
)

@Entity(tableName = "paper_trades")
data class PaperTradeEntity(
    @PrimaryKey
    val id: String,
    val positionId: String,
    val symbol: String,
    val side: String,
    val price: Double,
    val quantity: Double,
    val usdtAmount: Double,
    val fee: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val reason: String,
    val timestamp: Long
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val watchedSymbolsCsv: String,
    val defaultTimeframe: String,
    val signalScoreThreshold: Double,
    val paperBalanceUsdt: Double,
    val riskPerTradePercent: Double,
    val maxOpenTrades: Int,
    val maxDailyLossPercent: Double,
    val minRiskRewardRatio: Double,
    val maxPositionSizePercent: Double,
    val binanceFeePercent: Double,
    val slippagePercent: Double,
    val notificationsEnabled: Boolean,
    val telegramEnabled: Boolean,
    val telegramBotToken: String,
    val telegramChatId: String,
    val binanceApiKey: String = "",
    val binanceApiSecret: String = ""
)
