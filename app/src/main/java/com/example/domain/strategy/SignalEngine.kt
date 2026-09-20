package com.example.domain.strategy

import com.example.data.model.Candle
import com.example.data.model.IndicatorSnapshot
import com.example.data.model.MarketRegime
import com.example.data.model.OrderBookDepth
import com.example.data.model.ScoreItem
import com.example.data.model.SignalType
import com.example.data.model.TimeFrame
import com.example.data.model.TradingSignal
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SignalEngine {

    /**
     * Evaluates market data, technical indicators, order book, and multi-timeframe confirmation
     * to produce a transparent TradingSignal.
     */
    fun evaluateSignal(
        symbol: String,
        timeframe: TimeFrame,
        candles: List<Candle>,
        indicators: IndicatorSnapshot,
        higherTimeframeIndicators: IndicatorSnapshot? = null,
        orderBook: OrderBookDepth? = null
    ): TradingSignal {
        val price = indicators.currentPrice
        val reasons = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val scoreBreakdown = mutableListOf<ScoreItem>()

        if (price <= 0.0 || candles.size < 20) {
            return TradingSignal(
                id = UUID.randomUUID().toString(),
                symbol = symbol,
                timeframe = timeframe,
                timestamp = System.currentTimeMillis(),
                price = price,
                signalType = SignalType.NO_TRADE,
                score = 0.0,
                scoreBreakdown = emptyList(),
                marketRegime = MarketRegime.SIDEWAYS,
                suggestedEntryMin = price,
                suggestedEntryMax = price,
                stopLoss = price * 0.98,
                takeProfit1 = price * 1.03,
                takeProfit2 = price * 1.05,
                riskRewardRatio = 1.5,
                reasons = listOf("Insufficient candle history to calculate indicators"),
                warnings = listOf("Need at least 20 historical candles"),
                dataFreshnessSeconds = 0
            )
        }

        val regime = MarketRegimeClassifier.classify(indicators)

        // 1. Trend Alignment (0 to 2 points)
        var trendPoints = 0.0
        val ema20 = indicators.ema20
        val ema50 = indicators.ema50
        val ema200 = indicators.ema200

        if (ema20 != null && ema50 != null) {
            if (ema20 > ema50 && price > ema20) {
                trendPoints += 1.0
                reasons.add("Price is above EMA20 and EMA20 is above EMA50")
                if (ema200 != null && ema50 > ema200) {
                    trendPoints += 1.0
                    reasons.add("Golden alignment: EMA20 > EMA50 > EMA200 (Major Bullish Trend)")
                } else if (ema200 != null && price > ema200) {
                    trendPoints += 0.5
                    reasons.add("Price trading above long-term EMA200")
                }
            } else if (ema20 < ema50 && price < ema20) {
                warnings.add("Short-term EMA20 is below EMA50 (Bearish pressure)")
            } else {
                trendPoints += 0.5
                reasons.add("Mixed EMA structure - consolidation / pullback zone")
            }
        }
        trendPoints = min(2.0, trendPoints)
        scoreBreakdown.add(
            ScoreItem(
                category = "Trend Alignment",
                pointsAwarded = trendPoints,
                maxPoints = 2.0,
                explanation = "EMA 20/50/200 structure and price position"
            )
        )

        // 2. Momentum (0 to 2 points)
        var momentumPoints = 0.0
        val macdHist = indicators.macdHistogram ?: 0.0
        val macdLine = indicators.macdLine ?: 0.0
        val macdSig = indicators.macdSignal ?: 0.0

        if (macdHist > 0) {
            momentumPoints += 1.0
            reasons.add("MACD histogram positive (Bullish momentum expansion)")
            if (macdLine > macdSig && macdLine > 0) {
                momentumPoints += 1.0
                reasons.add("MACD line above signal line and above centerline")
            } else if (macdLine > macdSig) {
                momentumPoints += 0.5
                reasons.add("MACD bullish crossover developing below centerline")
            }
        } else {
            warnings.add("MACD histogram is negative or contracting")
        }
        momentumPoints = min(2.0, momentumPoints)
        scoreBreakdown.add(
            ScoreItem(
                category = "Momentum",
                pointsAwarded = momentumPoints,
                maxPoints = 2.0,
                explanation = "MACD trend velocity and histogram expansion"
            )
        )

        // 3. RSI Quality (0 to 1 point)
        var rsiPoints = 0.0
        val rsi = indicators.rsi14
        if (rsi != null) {
            when {
                rsi in 45.0..62.0 -> {
                    rsiPoints = 1.0
                    reasons.add("RSI (14) at ${String.format("%.1f", rsi)}: Optimal bullish continuation zone (not overbought)")
                }
                rsi in 62.0..70.0 -> {
                    rsiPoints = 0.7
                    reasons.add("RSI (14) strong at ${String.format("%.1f", rsi)}")
                }
                rsi in 35.0..45.0 -> {
                    rsiPoints = 0.5
                    reasons.add("RSI pulling back into neutral support (${String.format("%.1f", rsi)})")
                }
                rsi > 70.0 -> {
                    rsiPoints = 0.2
                    warnings.add("RSI overbought (${String.format("%.1f", rsi)}) - Pullback risk elevated")
                }
                else -> {
                    warnings.add("RSI oversold / weak (${String.format("%.1f", rsi)})")
                }
            }
        }
        scoreBreakdown.add(
            ScoreItem(
                category = "RSI (14)",
                pointsAwarded = rsiPoints,
                maxPoints = 1.0,
                explanation = "RSI sweet spot between 45 and 65"
            )
        )

        // 4. MACD Direction (0 to 1 point)
        var macdDirectionPoints = 0.0
        if (macdLine > macdSig) {
            macdDirectionPoints = 1.0
            reasons.add("MACD Line is leading Signal Line")
        } else {
            warnings.add("MACD Line trailing below Signal Line")
        }
        scoreBreakdown.add(
            ScoreItem(
                category = "MACD Cross",
                pointsAwarded = macdDirectionPoints,
                maxPoints = 1.0,
                explanation = "Signal line relationship"
            )
        )

        // 5. Volume Confirmation (0 to 1 point)
        var volumePoints = 0.0
        val volPct = indicators.volumeVsAvgPercent
        if (volPct != null) {
            when {
                volPct >= 15.0 -> {
                    volumePoints = 1.0
                    reasons.add("Volume surge: +${String.format("%.1f", volPct)}% vs 20-period average")
                }
                volPct >= -10.0 -> {
                    volumePoints = 0.6
                    reasons.add("Volume in line with average (${String.format("%.1f", volPct)}%)")
                }
                else -> {
                    volumePoints = 0.2
                    warnings.add("Low volume (${String.format("%.1f", volPct)}% vs average) - lower conviction")
                }
            }
        } else {
            volumePoints = 0.5
        }
        scoreBreakdown.add(
            ScoreItem(
                category = "Volume Confirmation",
                pointsAwarded = volumePoints,
                maxPoints = 1.0,
                explanation = "Comparison against 20-period Volume MA"
            )
        )

        // 6. Price Structure & Support/Resistance (0 to 1 point)
        var structurePoints = 0.0
        val support = indicators.supportLevel
        val resistance = indicators.resistanceLevel
        if (support != null && support < price) {
            val distToSupport = (price - support) / price
            val distToRes = if (resistance != null && resistance > price) (resistance - price) / price else 0.05
            if (distToRes > distToSupport * 1.2) {
                structurePoints = 1.0
                reasons.add("Favorable price structure: closer to support ($${String.format("%.2f", support)}) than resistance ($${String.format("%.2f", resistance ?: (price * 1.05))})")
            } else {
                structurePoints = 0.5
                reasons.add("Support at $${String.format("%.2f", support)}")
            }
        } else {
            structurePoints = 0.4
        }
        scoreBreakdown.add(
            ScoreItem(
                category = "Price Structure",
                pointsAwarded = structurePoints,
                maxPoints = 1.0,
                explanation = "Key support and overhead resistance proximity"
            )
        )

        // 7. Order Book Depth Pressure (0 to 1 point)
        var obPoints = 0.0
        if (orderBook != null) {
            val buyPressure = orderBook.buyPressureRatio
            when {
                buyPressure >= 0.60 -> {
                    obPoints = 1.0
                    reasons.add("Order book buy wall: ${(buyPressure * 100).toInt()}% bid volume dominance")
                }
                buyPressure in 0.48..0.60 -> {
                    obPoints = 0.6
                    reasons.add("Balanced order book (${(buyPressure * 100).toInt()}% bids)")
                }
                else -> {
                    obPoints = 0.1
                    warnings.add("Order book selling pressure: ${((1 - buyPressure) * 100).toInt()}% ask volume dominance")
                }
            }
        } else {
            obPoints = 0.5
        }
        scoreBreakdown.add(
            ScoreItem(
                category = "Order Book Pressure",
                pointsAwarded = obPoints,
                maxPoints = 1.0,
                explanation = "Top 15 bid vs ask depth liquidity"
            )
        )

        // 8. Higher Timeframe Confirmation (0 to 1 point)
        var htfPoints = 0.0
        if (higherTimeframeIndicators != null) {
            val htfEma20 = higherTimeframeIndicators.ema20
            val htfEma50 = higherTimeframeIndicators.ema50
            val htfPrice = higherTimeframeIndicators.currentPrice
            val htfRsi = higherTimeframeIndicators.rsi14 ?: 50.0

            if (htfEma20 != null && htfEma50 != null && htfEma20 > htfEma50 && htfPrice > htfEma20 && htfRsi > 45.0) {
                htfPoints = 1.0
                reasons.add("Higher timeframe confirms dominant bullish trend")
            } else if (htfEma20 != null && htfEma50 != null && htfEma20 < htfEma50) {
                warnings.add("Higher timeframe trend is bearish - counter-trend caution")
                htfPoints = 0.0
            } else {
                htfPoints = 0.5
                reasons.add("Higher timeframe neutral/consolidating")
            }
        } else {
            htfPoints = 0.5 // Neutral if not available yet
        }
        scoreBreakdown.add(
            ScoreItem(
                category = "Higher Timeframe",
                pointsAwarded = htfPoints,
                maxPoints = 1.0,
                explanation = "Trend confluence on 1h/4h chart"
            )
        )

        // Total score calculation (0.0 to 10.0)
        val rawTotal = scoreBreakdown.sumOf { it.pointsAwarded }
        val totalScore = String.format("%.1f", min(10.0, max(0.0, rawTotal))).toDouble()

        // Map to signal type:
        // 0-2: NO TRADE
        // 3-4: WEAK / WAIT
        // 5-6: WATCH
        // 7-8: BUY / SELL SETUP (Spot: BUY SETUP or SELL / EXIT SETUP)
        // 9-10: STRONG SETUP
        val signalType: SignalType = when {
            regime == MarketRegime.HIGH_VOLATILITY && (indicators.atr14 ?: 0.0) / price > 0.04 -> {
                warnings.add("Extreme market volatility detected - Trading disabled for capital safety")
                SignalType.NO_TRADE
            }
            totalScore >= 8.5 -> SignalType.BUY_SETUP
            totalScore >= 7.0 -> SignalType.BUY_SETUP
            totalScore in 5.0..6.9 -> SignalType.WAIT
            totalScore in 3.0..4.9 -> {
                if (regime == MarketRegime.BEARISH && (indicators.rsi14 ?: 50.0) < 40) {
                    SignalType.SELL_EXIT_SETUP
                } else {
                    SignalType.WAIT
                }
            }
            else -> SignalType.NO_TRADE
        }

        // Calculate Entry, Stop Loss, and Targets using Market Structure and ATR
        val atr = indicators.atr14 ?: (price * 0.015)
        val suggestedStop = if (support != null && support < price && (price - support) < (2.5 * atr)) {
            support - (0.2 * atr)
        } else {
            price - (1.5 * atr)
        }

        val riskDistance = max(price * 0.005, price - suggestedStop)
        val entryMin = max(price - (0.3 * atr), price * 0.998)
        val entryMax = price

        val tp1 = price + (1.5 * riskDistance)
        val tp2 = if (resistance != null && resistance > price && resistance < (price + 3.0 * riskDistance)) {
            resistance
        } else {
            price + (2.5 * riskDistance)
        }

        val rewardDistance = tp1 - price
        val rr = if (riskDistance > 0) rewardDistance / riskDistance else 1.5

        if (resistance != null && resistance > price && (resistance - price) < (0.8 * riskDistance)) {
            warnings.add("Strong resistance near $${String.format("%.2f", resistance)} - tight headroom")
        }

        return TradingSignal(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            timeframe = timeframe,
            timestamp = System.currentTimeMillis(),
            price = price,
            signalType = signalType,
            score = totalScore,
            scoreBreakdown = scoreBreakdown,
            marketRegime = regime,
            suggestedEntryMin = entryMin,
            suggestedEntryMax = entryMax,
            stopLoss = suggestedStop,
            takeProfit1 = tp1,
            takeProfit2 = tp2,
            riskRewardRatio = String.format("%.2f", rr).toDouble(),
            reasons = reasons,
            warnings = warnings,
            dataFreshnessSeconds = (System.currentTimeMillis() - indicators.timestamp) / 1000
        )
    }
}
