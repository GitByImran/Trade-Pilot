package com.example.data.model

import java.util.Locale

enum class TimeFrame(val apiValue: String, val displayName: String, val seconds: Long) {
    M1("1m", "1m", 60),
    M3("3m", "3m", 180),
    M5("5m", "5m", 300),
    M15("15m", "15m", 900),
    M30("30m", "30m", 1800),
    H1("1h", "1h", 3600),
    H4("4h", "4h", 14400),
    D1("1d", "1d", 86400);

    companion object {
        fun fromApiValue(value: String): TimeFrame =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: M15
    }
}

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long,
    val isClosed: Boolean = true
)

data class TickerData(
    val symbol: String,
    val lastPrice: Double,
    val priceChange: Double,
    val priceChangePercent: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volume: Double,
    val quoteVolume: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class OrderBookEntry(
    val price: Double,
    val quantity: Double
)

data class OrderBookDepth(
    val symbol: String,
    val bids: List<OrderBookEntry>, // Buy orders, highest price first
    val asks: List<OrderBookEntry>, // Sell orders, lowest price first
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalBidVolume: Double get() = bids.take(15).sumOf { it.quantity * it.price }
    val totalAskVolume: Double get() = asks.take(15).sumOf { it.quantity * it.price }
    val buyPressureRatio: Double
        get() {
            val total = totalBidVolume + totalAskVolume
            return if (total > 0.0) totalBidVolume / total else 0.5
        }
    val spread: Double
        get() {
            val bestBid = bids.firstOrNull()?.price ?: 0.0
            val bestAsk = asks.firstOrNull()?.price ?: 0.0
            return if (bestBid > 0.0 && bestAsk > 0.0) bestAsk - bestBid else 0.0
        }
    val spreadPercent: Double
        get() {
            val bestBid = bids.firstOrNull()?.price ?: 0.0
            return if (bestBid > 0.0) (spread / bestBid) * 100.0 else 0.0
        }
}

data class RecentTrade(
    val id: Long,
    val price: Double,
    val qty: Double,
    val time: Long,
    val isBuyerMaker: Boolean // true = sell taker, false = buy taker
)

enum class MarketRegime(val label: String, val description: String) {
    BULLISH("BULLISH", "Price trending above key EMAs with positive momentum"),
    BEARISH("BEARISH", "Price trending below key EMAs with negative momentum"),
    SIDEWAYS("SIDEWAYS", "Range-bound market without strong directional momentum"),
    HIGH_VOLATILITY("HIGH VOLATILITY", "Abnormal volatility range; risk elevated")
}

data class IndicatorSnapshot(
    val timestamp: Long,
    val currentPrice: Double,
    val ema20: Double?,
    val ema50: Double?,
    val ema200: Double?,
    val sma20: Double?,
    val rsi14: Double?,
    val macdLine: Double?,
    val macdSignal: Double?,
    val macdHistogram: Double?,
    val bbUpper: Double?,
    val bbMiddle: Double?,
    val bbLower: Double?,
    val bbWidthPercent: Double?,
    val atr14: Double?,
    val volumeMa20: Double?,
    val currentVolume: Double?,
    val volumeVsAvgPercent: Double?,
    val supportLevel: Double?,
    val resistanceLevel: Double?
)

enum class SignalType(val label: String) {
    BUY_SETUP("BUY SETUP"),
    SELL_EXIT_SETUP("SELL / EXIT SETUP"),
    WAIT("WAIT"),
    NO_TRADE("NO TRADE")
}

data class ScoreItem(
    val category: String,
    val pointsAwarded: Double,
    val maxPoints: Double,
    val explanation: String
)

data class TradingSignal(
    val id: String,
    val symbol: String,
    val timeframe: TimeFrame,
    val timestamp: Long,
    val price: Double,
    val signalType: SignalType,
    val score: Double, // 0.0 to 10.0
    val scoreBreakdown: List<ScoreItem>,
    val marketRegime: MarketRegime,
    val suggestedEntryMin: Double,
    val suggestedEntryMax: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val riskRewardRatio: Double,
    val reasons: List<String>,
    val warnings: List<String>,
    val dataFreshnessSeconds: Long,
    val status: String = "NEW" // NEW, WATCHING, TRIGGERED, EXPIRED, INVALIDATED, PAPER_TRADED
)
