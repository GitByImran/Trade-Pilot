package com.example.domain.backtest

import com.example.data.model.BacktestConfig
import com.example.data.model.BacktestResult
import com.example.data.model.Candle
import com.example.data.model.EquityPoint
import com.example.data.model.PaperTrade
import com.example.data.model.PositionStatus
import com.example.data.model.SignalType
import com.example.domain.indicators.TechnicalIndicators
import com.example.domain.strategy.SignalEngine
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object BacktestEngine {

    fun runBacktest(
        candles: List<Candle>,
        config: BacktestConfig
    ): BacktestResult {
        if (candles.size < 60) {
            return emptyResult(config, candles.size)
        }

        var balance = config.startingBalance
        var peakEquity = balance
        var maxDrawdownPct = 0.0

        val trades = mutableListOf<PaperTrade>()
        val equityCurve = mutableListOf<EquityPoint>()
        equityCurve.add(EquityPoint(candles.first().openTime, balance, "Start"))

        var activePosition: ActiveBacktestPosition? = null
        val minWarmup = 50 // candles required for 20/50 period indicators

        for (i in minWarmup until candles.size) {
            val currentCandle = candles[i]
            val prevCandles = candles.subList(0, i + 1) // Strictly past and current candle only (no look-ahead!)

            // 1. If we have an open position, check if current candle's high/low hits SL or TP
            if (activePosition != null) {
                val pos = activePosition!!
                var exitPrice: Double? = null
                var exitReason = PositionStatus.OPEN

                // Check stop loss first (pessimistic execution)
                if (currentCandle.low <= pos.stopLoss) {
                    val slippagePrice = pos.stopLoss * (1.0 - config.slippagePercent / 100.0)
                    exitPrice = slippagePrice
                    exitReason = PositionStatus.CLOSED_STOP_LOSS
                } else if (currentCandle.high >= pos.takeProfit1) {
                    val slippagePrice = pos.takeProfit1 * (1.0 - config.slippagePercent / 100.0)
                    exitPrice = slippagePrice
                    exitReason = PositionStatus.CLOSED_TP1
                }

                if (exitPrice != null) {
                    val sellFee = (pos.quantity * exitPrice) * (config.feePercent / 100.0)
                    val rawGrossPnl = (exitPrice - pos.entryPrice) * pos.quantity
                    val netPnl = rawGrossPnl - pos.entryFee - sellFee
                    val pnlPercent = (netPnl / pos.investedUsdt) * 100.0

                    balance += pos.investedUsdt + netPnl
                    peakEquity = max(peakEquity, balance)
                    val currentDd = if (peakEquity > 0) ((peakEquity - balance) / peakEquity) * 100.0 else 0.0
                    maxDrawdownPct = max(maxDrawdownPct, currentDd)

                    trades.add(
                        PaperTrade(
                            id = UUID.randomUUID().toString(),
                            positionId = pos.id,
                            symbol = config.symbol,
                            side = "SELL",
                            price = exitPrice,
                            quantity = pos.quantity,
                            usdtAmount = pos.quantity * exitPrice,
                            fee = sellFee,
                            pnl = netPnl,
                            pnlPercent = pnlPercent,
                            reason = exitReason.name,
                            timestamp = currentCandle.closeTime
                        )
                    )
                    equityCurve.add(EquityPoint(currentCandle.closeTime, balance, "Exit: $exitReason"))
                    activePosition = null
                }
            }

            // 2. If no position is open, check if signal engine triggers a BUY SETUP with score >= threshold
            if (activePosition == null && i < candles.size - 1) {
                val snapshot = TechnicalIndicators.computeSnapshot(prevCandles)
                val signal = SignalEngine.evaluateSignal(
                    symbol = config.symbol,
                    timeframe = config.timeframe,
                    candles = prevCandles,
                    indicators = snapshot
                )

                if (signal.signalType == SignalType.BUY_SETUP && signal.score >= config.minScoreToTrade) {
                    val nextCandle = candles[i + 1]
                    // Execute at next candle open with slippage
                    val executionPrice = nextCandle.open * (1.0 + config.slippagePercent / 100.0)
                    val riskAmount = balance * (config.riskPerTradePercent / 100.0)
                    val stopDistance = abs(executionPrice - signal.stopLoss)

                    if (stopDistance > 0 && executionPrice > signal.stopLoss) {
                        val stopDistancePct = stopDistance / executionPrice
                        val rawPositionUsdt = riskAmount / stopDistancePct
                        val maxPosition = balance * 0.25 // max 25% allocation
                        val positionUsdt = min(rawPositionUsdt, min(maxPosition, balance * 0.95))
                        val quantity = positionUsdt / executionPrice
                        val entryFee = positionUsdt * (config.feePercent / 100.0)

                        if (positionUsdt > 10.0 && balance >= positionUsdt + entryFee) {
                            balance -= (positionUsdt + entryFee)
                            val posId = UUID.randomUUID().toString()

                            activePosition = ActiveBacktestPosition(
                                id = posId,
                                entryPrice = executionPrice,
                                quantity = quantity,
                                investedUsdt = positionUsdt,
                                stopLoss = signal.stopLoss,
                                takeProfit1 = signal.takeProfit1,
                                takeProfit2 = signal.takeProfit2,
                                entryFee = entryFee,
                                entryTime = nextCandle.openTime
                            )

                            trades.add(
                                PaperTrade(
                                    id = UUID.randomUUID().toString(),
                                    positionId = posId,
                                    symbol = config.symbol,
                                    side = "BUY",
                                    price = executionPrice,
                                    quantity = quantity,
                                    usdtAmount = positionUsdt,
                                    fee = entryFee,
                                    pnl = 0.0,
                                    pnlPercent = 0.0,
                                    reason = "Signal Score ${signal.score}/10",
                                    timestamp = nextCandle.openTime
                                )
                            )
                        }
                    }
                }
            }
        }

        // Close remaining open position at last candle close
        if (activePosition != null) {
            val lastCandle = candles.last()
            val pos = activePosition!!
            val exitPrice = lastCandle.close
            val sellFee = (pos.quantity * exitPrice) * (config.feePercent / 100.0)
            val netPnl = (exitPrice - pos.entryPrice) * pos.quantity - pos.entryFee - sellFee
            val pnlPercent = (netPnl / pos.investedUsdt) * 100.0
            balance += pos.investedUsdt + netPnl

            trades.add(
                PaperTrade(
                    id = UUID.randomUUID().toString(),
                    positionId = pos.id,
                    symbol = config.symbol,
                    side = "SELL",
                    price = exitPrice,
                    quantity = pos.quantity,
                    usdtAmount = pos.quantity * exitPrice,
                    fee = sellFee,
                    pnl = netPnl,
                    pnlPercent = pnlPercent,
                    reason = "End of Backtest Period",
                    timestamp = lastCandle.closeTime
                )
            )
            equityCurve.add(EquityPoint(lastCandle.closeTime, balance, "Backtest End"))
        }

        val sellTrades = trades.filter { it.side == "SELL" }
        val winningTrades = sellTrades.count { it.pnl > 0.0 }
        val losingTrades = sellTrades.count { it.pnl <= 0.0 }
        val winRate = if (sellTrades.isNotEmpty()) (winningTrades.toDouble() / sellTrades.size) * 100.0 else 0.0

        val grossProfit = sellTrades.filter { it.pnl > 0.0 }.sumOf { it.pnl }
        val grossLoss = sellTrades.filter { it.pnl < 0.0 }.sumOf { abs(it.pnl) }
        val totalFees = trades.sumOf { it.fee }
        val netPnl = balance - config.startingBalance
        val netPnlPct = (netPnl / config.startingBalance) * 100.0
        val profitFactor = if (grossLoss > 0.0) grossProfit / grossLoss else if (grossProfit > 0.0) 99.9 else 0.0

        val largestWin = sellTrades.maxOfOrNull { it.pnl } ?: 0.0
        val largestLoss = sellTrades.minOfOrNull { it.pnl } ?: 0.0
        val avgTrade = if (sellTrades.isNotEmpty()) netPnl / sellTrades.size else 0.0

        return BacktestResult(
            config = config,
            totalCandlesAnalyzed = candles.size,
            totalTrades = sellTrades.size,
            winningTrades = winningTrades,
            losingTrades = losingTrades,
            winRatePercent = winRate,
            startingBalance = config.startingBalance,
            finalBalance = balance,
            netPnlUsdt = netPnl,
            netPnlPercent = netPnlPct,
            totalFeesUsdt = totalFees,
            grossProfit = grossProfit,
            grossLoss = grossLoss,
            profitFactor = profitFactor,
            maxDrawdownPercent = maxDrawdownPct,
            averageTradePnl = avgTrade,
            largestWinUsdt = largestWin,
            largestLossUsdt = largestLoss,
            equityCurve = equityCurve,
            tradeLog = trades
        )
    }

    private fun emptyResult(config: BacktestConfig, candleCount: Int): BacktestResult {
        return BacktestResult(
            config = config,
            totalCandlesAnalyzed = candleCount,
            totalTrades = 0,
            winningTrades = 0,
            losingTrades = 0,
            winRatePercent = 0.0,
            startingBalance = config.startingBalance,
            finalBalance = config.startingBalance,
            netPnlUsdt = 0.0,
            netPnlPercent = 0.0,
            totalFeesUsdt = 0.0,
            grossProfit = 0.0,
            grossLoss = 0.0,
            profitFactor = 0.0,
            maxDrawdownPercent = 0.0,
            averageTradePnl = 0.0,
            largestWinUsdt = 0.0,
            largestLossUsdt = 0.0,
            equityCurve = listOf(EquityPoint(System.currentTimeMillis(), config.startingBalance, "Start")),
            tradeLog = emptyList()
        )
    }

    private data class ActiveBacktestPosition(
        val id: String,
        val entryPrice: Double,
        val quantity: Double,
        val investedUsdt: Double,
        val stopLoss: Double,
        val takeProfit1: Double,
        val takeProfit2: Double,
        val entryFee: Double,
        val entryTime: Long
    )
}
