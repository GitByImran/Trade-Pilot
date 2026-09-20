package com.example.data.model

enum class PositionSide {
    LONG
}

enum class PositionStatus {
    OPEN,
    CLOSED_TP1,
    CLOSED_TP2,
    CLOSED_STOP_LOSS,
    CLOSED_MANUAL,
    CLOSED_SIGNAL_EXIT
}

data class PaperPosition(
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
    val status: PositionStatus = PositionStatus.OPEN,
    val entryFee: Double = 0.0,
    val exitFee: Double = 0.0,
    val closedAt: Long? = null,
    val closePrice: Double? = null,
    val realizedPnl: Double = 0.0,
    val realizedPnlPercent: Double = 0.0
) {
    val unrealizedPnl: Double
        get() = (currentPrice - entryPrice) * quantity - entryFee
    val unrealizedPnlPercent: Double
        get() = if (investedUsdt > 0.0) (unrealizedPnl / investedUsdt) * 100.0 else 0.0
}

data class PaperTrade(
    val id: String,
    val positionId: String,
    val symbol: String,
    val side: String, // "BUY" or "SELL"
    val price: Double,
    val quantity: Double,
    val usdtAmount: Double,
    val fee: Double,
    val pnl: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val reason: String,
    val timestamp: Long
)

data class PaperPortfolio(
    val startingBalance: Double = 10000.0,
    val availableUsdt: Double = 10000.0,
    val openPositions: List<PaperPosition> = emptyList(),
    val closedTrades: List<PaperTrade> = emptyList()
) {
    val totalInvestedInPositions: Double
        get() = openPositions.sumOf { it.investedUsdt }

    val totalUnrealizedPnl: Double
        get() = openPositions.sumOf { it.unrealizedPnl }

    val totalRealizedPnl: Double
        get() = closedTrades.filter { it.side == "SELL" }.sumOf { it.pnl }

    val totalEquity: Double
        get() = availableUsdt + totalInvestedInPositions + totalUnrealizedPnl

    val totalClosedTradesCount: Int
        get() = closedTrades.count { it.side == "SELL" }

    val winningTradesCount: Int
        get() = closedTrades.count { it.side == "SELL" && it.pnl > 0.0 }

    val losingTradesCount: Int
        get() = closedTrades.count { it.side == "SELL" && it.pnl < 0.0 }

    val winRatePercent: Double
        get() = if (totalClosedTradesCount > 0) (winningTradesCount.toDouble() / totalClosedTradesCount) * 100.0 else 0.0

    val grossProfit: Double
        get() = closedTrades.filter { it.side == "SELL" && it.pnl > 0.0 }.sumOf { it.pnl }

    val grossLoss: Double
        get() = closedTrades.filter { it.side == "SELL" && it.pnl < 0.0 }.sumOf { kotlin.math.abs(it.pnl) }

    val profitFactor: Double
        get() = if (grossLoss > 0.0) grossProfit / grossLoss else if (grossProfit > 0.0) 99.9 else 0.0
}

data class RiskCalculation(
    val isAllowed: Boolean,
    val rejectionReason: String? = null,
    val balance: Double,
    val riskPercentage: Double,
    val riskAmountUsdt: Double,
    val entryPrice: Double,
    val stopLossPrice: Double,
    val takeProfit1Price: Double,
    val takeProfit2Price: Double,
    val distanceToStopPercent: Double,
    val suggestedPositionUsdt: Double,
    val suggestedQuantity: Double,
    val estimatedLossUsdt: Double,
    val estimatedProfitUsdt: Double,
    val riskRewardRatio: Double
)

data class BacktestConfig(
    val symbol: String = "BTCUSDT",
    val timeframe: TimeFrame = TimeFrame.M15,
    val startingBalance: Double = 10000.0,
    val riskPerTradePercent: Double = 1.0,
    val minScoreToTrade: Double = 7.0,
    val feePercent: Double = 0.1, // 0.1% Binance fee
    val slippagePercent: Double = 0.05
)

data class EquityPoint(
    val timestamp: Long,
    val equity: Double,
    val label: String
)

data class BacktestResult(
    val config: BacktestConfig,
    val totalCandlesAnalyzed: Int,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRatePercent: Double,
    val startingBalance: Double,
    val finalBalance: Double,
    val netPnlUsdt: Double,
    val netPnlPercent: Double,
    val totalFeesUsdt: Double,
    val grossProfit: Double,
    val grossLoss: Double,
    val profitFactor: Double,
    val maxDrawdownPercent: Double,
    val averageTradePnl: Double,
    val largestWinUsdt: Double,
    val largestLossUsdt: Double,
    val equityCurve: List<EquityPoint>,
    val tradeLog: List<PaperTrade>
)

data class UserSettings(
    val watchedSymbols: List<String> = listOf(
        "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
        "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "LINKUSDT", "TRXUSDT"
    ),
    val defaultTimeframe: TimeFrame = TimeFrame.M15,
    val signalScoreThreshold: Double = 7.0,
    val paperBalanceUsdt: Double = 10000.0,
    val riskPerTradePercent: Double = 1.0,
    val maxOpenTrades: Int = 3,
    val maxDailyLossPercent: Double = 3.0,
    val minRiskRewardRatio: Double = 1.5,
    val maxPositionSizePercent: Double = 25.0,
    val binanceFeePercent: Double = 0.1,
    val slippagePercent: Double = 0.05,
    val notificationsEnabled: Boolean = true,
    val telegramEnabled: Boolean = false,
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val binanceApiKey: String = "",
    val binanceApiSecret: String = "",
    val isLiveTradingEnabled: Boolean = false // Strictly false in version 1
)
