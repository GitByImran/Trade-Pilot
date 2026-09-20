package com.example.domain.execution

import com.example.data.model.PaperPosition
import com.example.data.model.PaperTrade
import com.example.data.model.PositionStatus
import java.util.UUID

interface TradingExecutionService {
    val executionModeName: String
    val isLive: Boolean

    suspend fun getAvailableBalance(): Double
    suspend fun getOpenPositions(): List<PaperPosition>
    suspend fun placeBuyOrder(
        symbol: String,
        price: Double,
        usdtAmount: Double,
        stopLoss: Double,
        takeProfit1: Double,
        takeProfit2: Double,
        feePercent: Double = 0.1,
        slippagePercent: Double = 0.05
    ): Result<PaperPosition>

    suspend fun closePosition(
        positionId: String,
        currentPrice: Double,
        reason: PositionStatus,
        feePercent: Double = 0.1,
        slippagePercent: Double = 0.05
    ): Result<PaperTrade>
}

/**
 * Disabled stub representing future live Binance order execution.
 * Kept completely disabled per Safety Rule 2 and Architecture Rule 21.
 */
class BinanceExecutionService : TradingExecutionService {
    override val executionModeName: String = "Binance Spot Live (DISABLED)"
    override val isLive: Boolean = false

    override suspend fun getAvailableBalance(): Double {
        throw UnsupportedOperationException("Live trading is strictly disabled in Version 1.0.")
    }

    override suspend fun getOpenPositions(): List<PaperPosition> {
        throw UnsupportedOperationException("Live trading is strictly disabled in Version 1.0.")
    }

    override suspend fun placeBuyOrder(
        symbol: String,
        price: Double,
        usdtAmount: Double,
        stopLoss: Double,
        takeProfit1: Double,
        takeProfit2: Double,
        feePercent: Double,
        slippagePercent: Double
    ): Result<PaperPosition> {
        return Result.failure(
            IllegalStateException("Safety Interlock: Real Binance order execution is disabled in this build.")
        )
    }

    override suspend fun closePosition(
        positionId: String,
        currentPrice: Double,
        reason: PositionStatus,
        feePercent: Double,
        slippagePercent: Double
    ): Result<PaperTrade> {
        return Result.failure(
            IllegalStateException("Safety Interlock: Real Binance order execution is disabled in this build.")
        )
    }
}
