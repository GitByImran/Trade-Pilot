package com.example.data.model

data class BinanceAccountAsset(
    val asset: String,
    val free: Double,
    val locked: Double,
    val estimatedUsdtValue: Double = 0.0
)

data class BinanceConnectedAccount(
    val isConnected: Boolean = false,
    val canTrade: Boolean = false,
    val totalEstimatedUsdt: Double = 0.0,
    val freeUsdt: Double = 0.0,
    val assets: List<BinanceAccountAsset> = emptyList(),
    val lastSyncTimestamp: Long = 0L,
    val errorMessage: String? = null
)

data class PortfolioTradeAdvisory(
    val symbol: String,
    val action: String, // "BUY / ACCUMULATE", "SELL / TAKE PROFIT", "HOLD", "WAIT"
    val urgency: String, // "IMMEDIATE", "LIMIT ORDER", "WATCH"
    val confidenceScore: Double, // 1 to 10
    val reasonHeadline: String,
    val rationale: String,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val riskRewardRatio: Double,
    val userFreeCapitalUsdt: Double,
    val recommendedAllocationUsdt: Double,
    val recommendedQuantity: Double,
    val portfolioRiskPercent: Double,
    val isHoldingAsset: Boolean = false,
    val heldQuantity: Double = 0.0
)
