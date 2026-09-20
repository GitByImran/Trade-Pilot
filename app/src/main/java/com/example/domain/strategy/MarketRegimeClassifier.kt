package com.example.domain.strategy

import com.example.data.model.IndicatorSnapshot
import com.example.data.model.MarketRegime

object MarketRegimeClassifier {

    /**
     * Classifies market condition based on indicator snapshot and volatility threshold
     */
    fun classify(
        snapshot: IndicatorSnapshot,
        highVolatilityAtrPctThreshold: Double = 2.5
    ): MarketRegime {
        val price = snapshot.currentPrice
        if (price <= 0.0) return MarketRegime.SIDEWAYS

        // Check High Volatility first (ATR as % of current price)
        val atr = snapshot.atr14
        if (atr != null) {
            val atrPercent = (atr / price) * 100.0
            if (atrPercent >= highVolatilityAtrPctThreshold) {
                return MarketRegime.HIGH_VOLATILITY
            }
        }

        val ema20 = snapshot.ema20
        val ema50 = snapshot.ema50
        val ema200 = snapshot.ema200
        val macdHist = snapshot.macdHistogram ?: 0.0

        if (ema20 != null && ema50 != null) {
            val bullishEma = ema20 > ema50 && price > ema20
            val bearishEma = ema20 < ema50 && price < ema20

            val longTermConfirmationBullish = ema200 == null || ema50 > ema200 || price > ema200
            val longTermConfirmationBearish = ema200 == null || ema50 < ema200 || price < ema200

            if (bullishEma && longTermConfirmationBullish && macdHist >= -0.0001) {
                return MarketRegime.BULLISH
            } else if (bearishEma && longTermConfirmationBearish && macdHist <= 0.0001) {
                return MarketRegime.BEARISH
            }
        }

        return MarketRegime.SIDEWAYS
    }
}
