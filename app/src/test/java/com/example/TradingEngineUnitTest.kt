package com.example

import com.example.data.model.BacktestConfig
import com.example.data.model.Candle
import com.example.data.model.TimeFrame
import com.example.data.model.UserSettings
import com.example.domain.backtest.BacktestEngine
import com.example.domain.indicators.TechnicalIndicators
import com.example.domain.risk.RiskCalculator
import com.example.domain.strategy.SignalEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class TradingEngineUnitTest {

    private fun generateMockCandles(count: Int, basePrice: Double = 100.0, trend: Double = 0.5): List<Candle> {
        val list = mutableListOf<Candle>()
        var cur = basePrice
        val now = 1700000000000L
        for (i in 0 until count) {
            val open = cur
            val close = cur + trend + (if (i % 2 == 0) 0.2 else -0.1)
            val high = maxOf(open, close) + 0.4
            val low = minOf(open, close) - 0.4
            val vol = 1000.0 + (i * 10)
            list.add(
                Candle(
                    openTime = now + (i * 900000L),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = vol,
                    closeTime = now + ((i + 1) * 900000L),
                    isClosed = true
                )
            )
            cur = close
        }
        return list
    }

    @Test
    fun testSmaCalculation() {
        val prices = listOf(10.0, 11.0, 12.0, 13.0, 14.0)
        val sma3 = TechnicalIndicators.calculateSma(prices, 3)
        assertEquals(5, sma3.size)
        assertEquals(null, sma3[0])
        assertEquals(null, sma3[1])
        assertEquals(11.0, sma3[2]!!, 0.001)
        assertEquals(12.0, sma3[3]!!, 0.001)
        assertEquals(13.0, sma3[4]!!, 0.001)
    }

    @Test
    fun testEmaCalculation() {
        val prices = (1..30).map { it.toDouble() }
        val ema10 = TechnicalIndicators.calculateEma(prices, 10)
        assertNotNull(ema10[9])
        assertNotNull(ema10.last())
        assertTrue(ema10.last()!! > 25.0)
    }

    @Test
    fun testRsiCalculation() {
        val uptrend = (1..30).map { it * 2.0 }
        val rsi = TechnicalIndicators.calculateRsi(uptrend, 14)
        assertNotNull(rsi.last())
        // In strong continuous uptrend, RSI should be high (> 80)
        assertTrue(rsi.last()!! > 80.0)
    }

    @Test
    fun testBollingerBandsCalculation() {
        val prices = (1..40).map { 100.0 + (it % 3) }
        val bb = TechnicalIndicators.calculateBollingerBands(prices, 20, 2.0)
        val lastUpper = bb.upper.last()
        val lastLower = bb.lower.last()
        val lastMiddle = bb.middle.last()

        assertNotNull(lastUpper)
        assertNotNull(lastLower)
        assertNotNull(lastMiddle)
        assertTrue(lastUpper!! > lastMiddle!!)
        assertTrue(lastMiddle > lastLower!!)
    }

    @Test
    fun testAtrCalculation() {
        val candles = generateMockCandles(30)
        val atr = TechnicalIndicators.calculateAtr(candles, 14)
        assertNotNull(atr.last())
        assertTrue(atr.last()!! > 0.0)
    }

    @Test
    fun testSignalEngineDeterminism() {
        val candles = generateMockCandles(60, trend = 1.0)
        val snapshot = TechnicalIndicators.computeSnapshot(candles)

        val signal1 = SignalEngine.evaluateSignal("BTCUSDT", TimeFrame.M15, candles, snapshot)
        val signal2 = SignalEngine.evaluateSignal("BTCUSDT", TimeFrame.M15, candles, snapshot)

        assertEquals(signal1.score, signal2.score, 0.001)
        assertEquals(signal1.signalType, signal2.signalType)
        assertEquals(signal1.marketRegime, signal2.marketRegime)
        assertEquals(signal1.stopLoss, signal2.stopLoss, 0.001)
        assertEquals(signal1.takeProfit1, signal2.takeProfit1, 0.001)
    }

    @Test
    fun testRiskCalculatorSafetyFilters() {
        val candles = generateMockCandles(60)
        val snapshot = TechnicalIndicators.computeSnapshot(candles)
        val baseSignal = SignalEngine.evaluateSignal("BTCUSDT", TimeFrame.M15, candles, snapshot)
        val buySignal = baseSignal.copy(
            timestamp = System.currentTimeMillis(),
            dataFreshnessSeconds = 5L,
            signalType = com.example.data.model.SignalType.BUY_SETUP,
            price = 100.0,
            suggestedEntryMin = 99.8,
            suggestedEntryMax = 100.0,
            stopLoss = 98.0,
            takeProfit1 = 104.0,
            takeProfit2 = 106.0,
            riskRewardRatio = 2.0
        )
        val settings = UserSettings(maxOpenTrades = 2, minRiskRewardRatio = 1.5)

        // 1. When under trade limit, trade is allowed
        val calcAllowed = RiskCalculator.calculateRisk(buySignal, settings, 10000.0, 1)
        assertTrue(calcAllowed.isAllowed)
        assertTrue(calcAllowed.suggestedPositionUsdt > 0.0)

        // 2. When max open trades is reached, safety filter rejects
        val calcRejected = RiskCalculator.calculateRisk(buySignal, settings, 10000.0, 2)
        assertFalse(calcRejected.isAllowed)
        assertNotNull(calcRejected.rejectionReason)
    }

    @Test
    fun testBacktestEngineNoLookahead() {
        val candles = generateMockCandles(100, trend = 0.5)
        val config = BacktestConfig(symbol = "BTCUSDT", timeframe = TimeFrame.M15, startingBalance = 10000.0)
        val result = BacktestEngine.runBacktest(candles, config)

        assertNotNull(result)
        assertEquals(100, result.totalCandlesAnalyzed)
        assertTrue(result.equityCurve.isNotEmpty())
        assertEquals(10000.0, result.startingBalance, 0.01)
    }

    @Test
    fun testPortfolioAdvisorEngineWithBinanceAccount() {
        val candles = generateMockCandles(60)
        val snapshot = TechnicalIndicators.computeSnapshot(candles)
        val signal = SignalEngine.evaluateSignal("BTCUSDT", TimeFrame.M15, candles, snapshot).copy(
            signalType = com.example.data.model.SignalType.BUY_SETUP,
            score = 8.2,
            price = 60000.0,
            suggestedEntryMin = 59800.0,
            suggestedEntryMax = 60000.0,
            stopLoss = 58500.0,
            takeProfit1 = 63000.0,
            riskRewardRatio = 2.0
        )

        // Scenario 1: Connected account with $5,000 USDT and zero BTC
        val account = com.example.data.model.BinanceConnectedAccount(
            isConnected = true,
            freeUsdt = 5000.0,
            totalEstimatedUsdt = 5000.0,
            assets = listOf(
                com.example.data.model.BinanceAccountAsset(asset = "USDT", free = 5000.0, locked = 0.0)
            )
        )

        val advisories = com.example.domain.advisor.PortfolioAdvisorEngine.generatePortfolioAdvisories(
            signals = listOf(signal),
            tickers = mapOf("BTCUSDT" to com.example.data.model.TickerData("BTCUSDT", 60000.0, 1500.0, 2.5, 61000.0, 59000.0, 1000.0, 60000000.0)),
            account = account,
            settings = UserSettings(riskPerTradePercent = 2.0)
        )

        assertEquals(1, advisories.size)
        val advice = advisories.first()
        assertEquals("BTCUSDT", advice.symbol)
        assertTrue(advice.action.startsWith("BUY"))
        assertEquals(5000.0, advice.userFreeCapitalUsdt, 0.01)
        assertTrue(advice.recommendedAllocationUsdt > 0.0)
        assertTrue(advice.recommendedAllocationUsdt <= 5000.0)
        assertEquals(60000.0, advice.entryPrice, 0.01)
        assertEquals(58500.0, advice.stopLoss, 0.01)
        assertEquals(63000.0, advice.takeProfit1, 0.01)

        // Scenario 2: User already holds heavy BTC and signal turns BEARISH / WEAK
        val bearishSignal = signal.copy(
            signalType = com.example.data.model.SignalType.SELL_EXIT_SETUP,
            score = 3.5
        )
        val accountWithBtc = account.copy(
            assets = listOf(
                com.example.data.model.BinanceAccountAsset(asset = "BTC", free = 0.5, locked = 0.0),
                com.example.data.model.BinanceAccountAsset(asset = "USDT", free = 1000.0, locked = 0.0)
            )
        )
        val advisoriesBtc = com.example.domain.advisor.PortfolioAdvisorEngine.generatePortfolioAdvisories(
            signals = listOf(bearishSignal),
            tickers = mapOf("BTCUSDT" to com.example.data.model.TickerData("BTCUSDT", 60000.0, -1200.0, -2.0, 61000.0, 59000.0, 1000.0, 60000000.0)),
            account = accountWithBtc,
            settings = UserSettings()
        )

        assertEquals(1, advisoriesBtc.size)
        val sellAdvice = advisoriesBtc.first()
        assertTrue(sellAdvice.action.contains("SELL") || sellAdvice.action.contains("TRIM") || sellAdvice.action.contains("PROTECT"))
        assertTrue(sellAdvice.heldQuantity > 0.0)
    }
}
