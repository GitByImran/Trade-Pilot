package com.example.domain.risk

import com.example.data.model.OrderBookDepth
import com.example.data.model.RiskCalculation
import com.example.data.model.TradingSignal
import com.example.data.model.UserSettings
import kotlin.math.abs
import kotlin.math.min

object RiskCalculator {

    fun calculateRisk(
        signal: TradingSignal,
        settings: UserSettings,
        currentBalance: Double,
        currentOpenTradesCount: Int,
        orderBook: OrderBookDepth? = null
    ): RiskCalculation {
        val entry = signal.suggestedEntryMax
        val stop = signal.stopLoss
        val tp1 = signal.takeProfit1
        val tp2 = signal.takeProfit2

        // Safety filter checks
        if (currentOpenTradesCount >= settings.maxOpenTrades) {
            return reject(
                "Maximum open trades limit reached (${currentOpenTradesCount}/${settings.maxOpenTrades})",
                currentBalance, settings, entry, stop, tp1, tp2
            )
        }

        if (signal.dataFreshnessSeconds > 120) {
            return reject(
                "Market data is stale (${signal.dataFreshnessSeconds}s old). Awaiting fresh live ticks.",
                currentBalance, settings, entry, stop, tp1, tp2
            )
        }

        if (orderBook != null && orderBook.spreadPercent > 0.25) {
            return reject(
                "Spread too wide (${String.format("%.2f", orderBook.spreadPercent)}% > 0.25% max)",
                currentBalance, settings, entry, stop, tp1, tp2
            )
        }

        val distanceToStop = abs(entry - stop)
        val distanceToStopPercent = if (entry > 0) (distanceToStop / entry) * 100.0 else 0.0

        if (distanceToStopPercent < 0.15) {
            return reject(
                "Stop loss too tight (${String.format("%.2f", distanceToStopPercent)}%) - high noise risk",
                currentBalance, settings, entry, stop, tp1, tp2
            )
        }

        if (distanceToStopPercent > 6.0) {
            return reject(
                "Stop loss distance too wide (${String.format("%.2f", distanceToStopPercent)}% > 6.0% max limit)",
                currentBalance, settings, entry, stop, tp1, tp2
            )
        }

        val distanceToTp = abs(tp1 - entry)
        val rr = if (distanceToStop > 0) distanceToTp / distanceToStop else 0.0

        if (rr < settings.minRiskRewardRatio) {
            return reject(
                "Risk/Reward ratio (${String.format("%.2f", rr)}) below required minimum (${settings.minRiskRewardRatio})",
                currentBalance, settings, entry, stop, tp1, tp2
            )
        }

        // Calculate sizing based on fixed risk per trade %
        val riskAmountUsdt = currentBalance * (settings.riskPerTradePercent / 100.0)
        // Position Size = Risk Amount / (Distance to Stop %)
        val rawPositionUsdt = if (distanceToStopPercent > 0) {
            riskAmountUsdt / (distanceToStopPercent / 100.0)
        } else 0.0

        // Cap position size at maxPositionSizePercent of balance and available balance
        val maxAllowedPositionUsdt = currentBalance * (settings.maxPositionSizePercent / 100.0)
        val finalPositionUsdt = min(rawPositionUsdt, min(maxAllowedPositionUsdt, currentBalance * 0.95))
        val finalQuantity = if (entry > 0) finalPositionUsdt / entry else 0.0

        val estimatedLossUsdt = finalQuantity * distanceToStop
        val estimatedProfitUsdt = finalQuantity * distanceToTp

        return RiskCalculation(
            isAllowed = true,
            rejectionReason = null,
            balance = currentBalance,
            riskPercentage = settings.riskPerTradePercent,
            riskAmountUsdt = riskAmountUsdt,
            entryPrice = entry,
            stopLossPrice = stop,
            takeProfit1Price = tp1,
            takeProfit2Price = tp2,
            distanceToStopPercent = distanceToStopPercent,
            suggestedPositionUsdt = finalPositionUsdt,
            suggestedQuantity = finalQuantity,
            estimatedLossUsdt = estimatedLossUsdt,
            estimatedProfitUsdt = estimatedProfitUsdt,
            riskRewardRatio = rr
        )
    }

    private fun reject(
        reason: String,
        balance: Double,
        settings: UserSettings,
        entry: Double,
        stop: Double,
        tp1: Double,
        tp2: Double
    ): RiskCalculation {
        return RiskCalculation(
            isAllowed = false,
            rejectionReason = reason,
            balance = balance,
            riskPercentage = settings.riskPerTradePercent,
            riskAmountUsdt = 0.0,
            entryPrice = entry,
            stopLossPrice = stop,
            takeProfit1Price = tp1,
            takeProfit2Price = tp2,
            distanceToStopPercent = if (entry > 0) (abs(entry - stop) / entry) * 100.0 else 0.0,
            suggestedPositionUsdt = 0.0,
            suggestedQuantity = 0.0,
            estimatedLossUsdt = 0.0,
            estimatedProfitUsdt = 0.0,
            riskRewardRatio = if (abs(entry - stop) > 0) abs(tp1 - entry) / abs(entry - stop) else 0.0
        )
    }
}
