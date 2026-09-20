package com.example.domain.advisor

import com.example.data.model.BinanceAccountAsset
import com.example.data.model.BinanceConnectedAccount
import com.example.data.model.PortfolioTradeAdvisory
import com.example.data.model.SignalType
import com.example.data.model.TickerData
import com.example.data.model.TradingSignal
import com.example.data.model.UserSettings
import kotlin.math.max
import kotlin.math.min

object PortfolioAdvisorEngine {

    fun generatePortfolioAdvisories(
        signals: List<TradingSignal>,
        tickers: Map<String, TickerData>,
        account: BinanceConnectedAccount,
        settings: UserSettings
    ): List<PortfolioTradeAdvisory> {
        val advisories = mutableListOf<PortfolioTradeAdvisory>()
        val effectiveCapital = if (account.isConnected && account.freeUsdt > 5.0) {
            account.freeUsdt
        } else {
            settings.paperBalanceUsdt
        }

        // Map user's existing asset balances
        val assetMap = mutableMapOf<String, BinanceAccountAsset>()
        if (account.isConnected) {
            for (a in account.assets) {
                assetMap[a.asset.uppercase()] = a
            }
        }

        for (signal in signals) {
            val ticker = tickers[signal.symbol]
            val currentPrice = ticker?.lastPrice ?: signal.price
            if (currentPrice <= 0.0) continue

            // Determine base coin from symbol e.g. "BTC" from "BTCUSDT"
            val baseAsset = signal.symbol.replace("USDT", "").uppercase()
            val existingHolding = assetMap[baseAsset]
            val hasInventory = existingHolding != null && existingHolding.free > 0.0001
            val heldQty = existingHolding?.free ?: 0.0

            val isScoreHigh = signal.score >= settings.signalScoreThreshold
            val isBuySignal = signal.signalType == SignalType.BUY_SETUP && isScoreHigh
            val isSellSignal = signal.signalType == SignalType.SELL_EXIT_SETUP || (hasInventory && signal.score < 4.0)

            val action: String
            val urgency: String
            val headline: String
            val rationale: String

            // Dynamic allocation calculation based on user's live free capital
            val riskPercentage = settings.riskPerTradePercent // e.g. 1.0% or 2.0%
            val riskAmountUsdt = effectiveCapital * (riskPercentage / 100.0)
            val distanceToStop = kotlin.math.abs(currentPrice - signal.stopLoss)
            val distanceToStopPercent = if (currentPrice > 0) (distanceToStop / currentPrice) * 100.0 else 1.0

            val rawAllocationUsdt = if (distanceToStopPercent > 0.1) {
                riskAmountUsdt / (distanceToStopPercent / 100.0)
            } else {
                effectiveCapital * 0.05
            }
            val maxCapUsdt = effectiveCapital * (settings.maxPositionSizePercent / 100.0)
            val recommendedAllocUsdt = min(maxCapUsdt, min(rawAllocationUsdt, effectiveCapital * 0.90))
            val recommendedQuantity = if (currentPrice > 0) recommendedAllocUsdt / currentPrice else 0.0

            if (isBuySignal) {
                action = if (hasInventory) "ACCUMULATE / ADD" else "BUY / ENTER LONG"
                urgency = if (signal.score >= 8.5) "HIGH PRIORITY" else "LIMIT ENTRY"
                headline = "High Conviction Long Setup on ${signal.symbol}"
                rationale = "Signal Score is ${String.format("%.1f", signal.score)}/10 in a ${signal.marketRegime} regime. " +
                        "Allocate $${String.format("%,.1f", recommendedAllocUsdt)} USDT (~${String.format("%.1f", (recommendedAllocUsdt / max(1.0, effectiveCapital)) * 100.0)}% of your available USDT) " +
                        "with Stop-Loss strictly at $${String.format("%.2f", signal.stopLoss)}."
            } else if (isSellSignal && hasInventory) {
                action = "SELL / TAKE PROFIT"
                urgency = "IMMEDIATE EXIT"
                headline = "Protect Capital: Sell $baseAsset Holding"
                rationale = "You hold ${String.format("%.4f", heldQty)} $baseAsset. Momentum has deteriorated to ${signal.marketRegime} regime (Score ${String.format("%.1f", signal.score)}). Take profit or cut loss before further drawdown."
            } else if (hasInventory) {
                action = "HOLD CURRENT ASSET"
                urgency = "MONITOR"
                headline = "Holding $baseAsset in Range"
                rationale = "You hold ${String.format("%.4f", heldQty)} $baseAsset. Regime is ${signal.marketRegime}. Current stop-loss cushion is $${String.format("%.2f", signal.stopLoss)}."
            } else if (signal.score >= 6.0) {
                action = "WATCHLIST ONLY"
                urgency = "WAIT FOR PULLBACK"
                headline = "Setup Brewing: Wait for Clearer Breakout"
                rationale = "Setup score (${String.format("%.1f", signal.score)}/10) is approaching trigger threshold (${settings.signalScoreThreshold}). Keep cash ready; do not rush market orders."
            } else {
                continue // Skip noisy neutral coins to keep advisor focused
            }

            advisories.add(
                PortfolioTradeAdvisory(
                    symbol = signal.symbol,
                    action = action,
                    urgency = urgency,
                    confidenceScore = signal.score,
                    reasonHeadline = headline,
                    rationale = rationale,
                    entryPrice = currentPrice,
                    stopLoss = signal.stopLoss,
                    takeProfit1 = signal.takeProfit1,
                    takeProfit2 = signal.takeProfit2,
                    riskRewardRatio = signal.riskRewardRatio,
                    userFreeCapitalUsdt = effectiveCapital,
                    recommendedAllocationUsdt = recommendedAllocUsdt,
                    recommendedQuantity = recommendedQuantity,
                    portfolioRiskPercent = riskPercentage,
                    isHoldingAsset = hasInventory,
                    heldQuantity = heldQty
                )
            )
        }

        return advisories.sortedWith(
            compareByDescending<PortfolioTradeAdvisory> { it.isHoldingAsset && it.action.startsWith("SELL") }
                .thenByDescending { it.confidenceScore }
        )
    }
}
