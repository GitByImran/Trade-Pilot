package com.example.domain.indicators

import com.example.data.model.Candle
import com.example.data.model.IndicatorSnapshot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

object TechnicalIndicators {

    /**
     * Simple Moving Average (SMA)
     */
    fun calculateSma(prices: List<Double>, period: Int): List<Double?> {
        if (prices.size < period || period <= 0) {
            return List(prices.size) { null }
        }
        val result = MutableList<Double?>(prices.size) { null }
        var sum = 0.0
        for (i in 0 until period) {
            sum += prices[i]
        }
        result[period - 1] = sum / period

        for (i in period until prices.size) {
            sum += prices[i] - prices[i - period]
            result[i] = sum / period
        }
        return result
    }

    /**
     * Exponential Moving Average (EMA)
     */
    fun calculateEma(prices: List<Double>, period: Int): List<Double?> {
        if (prices.size < period || period <= 0) {
            return List(prices.size) { null }
        }
        val result = MutableList<Double?>(prices.size) { null }
        val multiplier = 2.0 / (period + 1)

        // Seed with SMA
        var sum = 0.0
        for (i in 0 until period) {
            sum += prices[i]
        }
        var prevEma = sum / period
        result[period - 1] = prevEma

        for (i in period until prices.size) {
            val currentEma = (prices[i] - prevEma) * multiplier + prevEma
            result[i] = currentEma
            prevEma = currentEma
        }
        return result
    }

    /**
     * Relative Strength Index (RSI) using Wilder's smoothing
     */
    fun calculateRsi(prices: List<Double>, period: Int = 14): List<Double?> {
        if (prices.size <= period || period <= 0) {
            return List(prices.size) { null }
        }
        val result = MutableList<Double?>(prices.size) { null }

        var sumGain = 0.0
        var sumLoss = 0.0

        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change > 0) sumGain += change else sumLoss += abs(change)
        }

        var avgGain = sumGain / period
        var avgLoss = sumLoss / period

        val firstRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        result[period] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + firstRs))

        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            if (avgLoss == 0.0) {
                result[i] = 100.0
            } else {
                val rs = avgGain / avgLoss
                result[i] = 100.0 - (100.0 / (1.0 + rs))
            }
        }
        return result
    }

    data class MacdResult(
        val macdLine: List<Double?>,
        val signalLine: List<Double?>,
        val histogram: List<Double?>
    )

    /**
     * Moving Average Convergence Divergence (MACD: 12, 26, 9)
     */
    fun calculateMacd(
        prices: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MacdResult {
        val fastEma = calculateEma(prices, fastPeriod)
        val slowEma = calculateEma(prices, slowPeriod)

        val macdLine = MutableList<Double?>(prices.size) { null }
        for (i in prices.indices) {
            val fast = fastEma[i]
            val slow = slowEma[i]
            if (fast != null && slow != null) {
                macdLine[i] = fast - slow
            }
        }

        // Calculate EMA of the valid MACD line values
        val validMacdStartIndex = macdLine.indexOfFirst { it != null }
        val signalLine = MutableList<Double?>(prices.size) { null }
        val histogram = MutableList<Double?>(prices.size) { null }

        if (validMacdStartIndex != -1 && prices.size - validMacdStartIndex >= signalPeriod) {
            val validMacdValues = macdLine.subList(validMacdStartIndex, macdLine.size).map { it!! }
            val validSignals = calculateEma(validMacdValues, signalPeriod)

            for (j in validSignals.indices) {
                val originalIndex = validMacdStartIndex + j
                val sig = validSignals[j]
                signalLine[originalIndex] = sig
                val macd = macdLine[originalIndex]
                if (macd != null && sig != null) {
                    histogram[originalIndex] = macd - sig
                }
            }
        }

        return MacdResult(macdLine, signalLine, histogram)
    }

    data class BollingerBandsResult(
        val upper: List<Double?>,
        val middle: List<Double?>,
        val lower: List<Double?>,
        val bandwidthPercent: List<Double?>
    )

    /**
     * Bollinger Bands (20 period, 2 std deviations)
     */
    fun calculateBollingerBands(
        prices: List<Double>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0
    ): BollingerBandsResult {
        val sma = calculateSma(prices, period)
        val upper = MutableList<Double?>(prices.size) { null }
        val lower = MutableList<Double?>(prices.size) { null }
        val bandwidth = MutableList<Double?>(prices.size) { null }

        for (i in (period - 1) until prices.size) {
            val mean = sma[i] ?: continue
            var varianceSum = 0.0
            for (k in (i - period + 1)..i) {
                varianceSum += (prices[k] - mean).pow(2)
            }
            val stdDev = sqrt(varianceSum / period)
            val up = mean + (stdDevMultiplier * stdDev)
            val low = mean - (stdDevMultiplier * stdDev)
            upper[i] = up
            lower[i] = low
            bandwidth[i] = if (mean > 0) ((up - low) / mean) * 100.0 else 0.0
        }

        return BollingerBandsResult(upper, sma, lower, bandwidth)
    }

    /**
     * Average True Range (ATR: 14 period)
     */
    fun calculateAtr(candles: List<Candle>, period: Int = 14): List<Double?> {
        if (candles.size <= period || period <= 0) {
            return List(candles.size) { null }
        }
        val tr = MutableList(candles.size) { 0.0 }
        tr[0] = candles[0].high - candles[0].low

        for (i in 1 until candles.size) {
            val high = candles[i].high
            val low = candles[i].low
            val prevClose = candles[i - 1].close
            val tr1 = high - low
            val tr2 = abs(high - prevClose)
            val tr3 = abs(low - prevClose)
            tr[i] = max(tr1, max(tr2, tr3))
        }

        val result = MutableList<Double?>(candles.size) { null }
        var initialAtr = 0.0
        for (i in 1..period) {
            initialAtr += tr[i]
        }
        initialAtr /= period
        result[period] = initialAtr

        var prevAtr = initialAtr
        for (i in (period + 1) until candles.size) {
            val currentAtr = (prevAtr * (period - 1) + tr[i]) / period
            result[i] = currentAtr
            prevAtr = currentAtr
        }
        return result
    }

    /**
     * Identifies dynamic Support and Resistance levels from recent swing pivots (lookback 50 candles)
     */
    fun findSupportResistance(candles: List<Candle>, lookback: Int = 50): Pair<Double?, Double?> {
        if (candles.size < 10) return Pair(null, null)
        val slice = candles.takeLast(lookback)
        val currentPrice = candles.last().close

        // Swing highs (where candle high is higher than 2 candles before and after)
        val swingHighs = mutableListOf<Double>()
        val swingLows = mutableListOf<Double>()

        for (i in 2 until slice.size - 2) {
            val c = slice[i]
            if (c.high >= slice[i - 1].high && c.high >= slice[i - 2].high &&
                c.high >= slice[i + 1].high && c.high >= slice[i + 2].high
            ) {
                swingHighs.add(c.high)
            }
            if (c.low <= slice[i - 1].low && c.low <= slice[i - 2].low &&
                c.low <= slice[i + 1].low && c.low <= slice[i + 2].low
            ) {
                swingLows.add(c.low)
            }
        }

        // Resistance: nearest swing high above current price (or highest high in lookback)
        val resistancesAbove = swingHighs.filter { it > currentPrice * 1.002 }.sorted()
        val nearestResistance = resistancesAbove.firstOrNull() ?: slice.maxOfOrNull { it.high }

        // Support: nearest swing low below current price (or lowest low in lookback)
        val supportsBelow = swingLows.filter { it < currentPrice * 0.998 }.sortedDescending()
        val nearestSupport = supportsBelow.firstOrNull() ?: slice.minOfOrNull { it.low }

        return Pair(nearestSupport, nearestResistance)
    }

    /**
     * Compute full snapshot of all technical indicators for the latest candle
     */
    fun computeSnapshot(candles: List<Candle>): IndicatorSnapshot {
        if (candles.isEmpty()) {
            return IndicatorSnapshot(
                timestamp = System.currentTimeMillis(),
                currentPrice = 0.0,
                ema20 = null, ema50 = null, ema200 = null, sma20 = null,
                rsi14 = null, macdLine = null, macdSignal = null, macdHistogram = null,
                bbUpper = null, bbMiddle = null, bbLower = null, bbWidthPercent = null,
                atr14 = null, volumeMa20 = null, currentVolume = null, volumeVsAvgPercent = null,
                supportLevel = null, resistanceLevel = null
            )
        }

        val closePrices = candles.map { it.close }
        val volumes = candles.map { it.volume }
        val latest = candles.last()

        val ema20List = calculateEma(closePrices, 20)
        val ema50List = calculateEma(closePrices, 50)
        val ema200List = calculateEma(closePrices, 200)
        val sma20List = calculateSma(closePrices, 20)

        val rsiList = calculateRsi(closePrices, 14)
        val macd = calculateMacd(closePrices, 12, 26, 9)
        val bb = calculateBollingerBands(closePrices, 20, 2.0)
        val atrList = calculateAtr(candles, 14)
        val volMaList = calculateSma(volumes, 20)

        val (support, resistance) = findSupportResistance(candles)

        val lastIndex = candles.lastIndex
        val curVol = latest.volume
        val avgVol = volMaList.getOrNull(lastIndex)
        val volDiffPct = if (avgVol != null && avgVol > 0.0) {
            ((curVol - avgVol) / avgVol) * 100.0
        } else null

        return IndicatorSnapshot(
            timestamp = latest.closeTime,
            currentPrice = latest.close,
            ema20 = ema20List.getOrNull(lastIndex),
            ema50 = ema50List.getOrNull(lastIndex),
            ema200 = ema200List.getOrNull(lastIndex),
            sma20 = sma20List.getOrNull(lastIndex),
            rsi14 = rsiList.getOrNull(lastIndex),
            macdLine = macd.macdLine.getOrNull(lastIndex),
            macdSignal = macd.signalLine.getOrNull(lastIndex),
            macdHistogram = macd.histogram.getOrNull(lastIndex),
            bbUpper = bb.upper.getOrNull(lastIndex),
            bbMiddle = bb.middle.getOrNull(lastIndex),
            bbLower = bb.lower.getOrNull(lastIndex),
            bbWidthPercent = bb.bandwidthPercent.getOrNull(lastIndex),
            atr14 = atrList.getOrNull(lastIndex),
            volumeMa20 = avgVol,
            currentVolume = curVol,
            volumeVsAvgPercent = volDiffPct,
            supportLevel = support,
            resistanceLevel = resistance
        )
    }
}
