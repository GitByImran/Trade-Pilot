package com.example.domain.execution

import com.example.data.model.PaperPosition
import com.example.data.model.PaperTrade
import com.example.data.model.PositionStatus
import com.example.data.repository.TradePilotRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

class PaperTradingExecutionService(
    private val repository: TradePilotRepository
) : TradingExecutionService {

    override val executionModeName: String = "Paper Trading (Simulation)"
    override val isLive: Boolean = false

    override suspend fun getAvailableBalance(): Double {
        val settings = repository.userSettings.first()
        val openPositions = repository.openPositions.first()
        val invested = openPositions.sumOf { it.investedUsdt }
        return kotlin.math.max(0.0, settings.paperBalanceUsdt - invested)
    }

    override suspend fun getOpenPositions(): List<PaperPosition> {
        return repository.openPositions.first()
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
        val currentBalance = getAvailableBalance()
        val totalNeeded = usdtAmount * (1.0 + feePercent / 100.0)

        if (currentBalance < totalNeeded) {
            return Result.failure(IllegalStateException("Insufficient paper USDT balance ($${String.format("%.2f", currentBalance)} < $${String.format("%.2f", totalNeeded)})"))
        }

        val executionPrice = price * (1.0 + slippagePercent / 100.0)
        val quantity = usdtAmount / executionPrice
        val fee = usdtAmount * (feePercent / 100.0)
        val posId = UUID.randomUUID().toString()

        val position = PaperPosition(
            id = posId,
            symbol = symbol,
            entryPrice = executionPrice,
            currentPrice = executionPrice,
            quantity = quantity,
            investedUsdt = usdtAmount,
            stopLoss = stopLoss,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            openedAt = System.currentTimeMillis(),
            status = PositionStatus.OPEN,
            entryFee = fee,
            exitFee = 0.0,
            closedAt = null,
            closePrice = null,
            realizedPnl = 0.0,
            realizedPnlPercent = 0.0
        )

        val trade = PaperTrade(
            id = UUID.randomUUID().toString(),
            positionId = posId,
            symbol = symbol,
            side = "BUY",
            price = executionPrice,
            quantity = quantity,
            usdtAmount = usdtAmount,
            fee = fee,
            pnl = 0.0,
            pnlPercent = 0.0,
            reason = "Paper Buy Order Executed",
            timestamp = System.currentTimeMillis()
        )

        repository.savePosition(position)
        repository.saveTrade(trade)

        return Result.success(position)
    }

    override suspend fun closePosition(
        positionId: String,
        currentPrice: Double,
        reason: PositionStatus,
        feePercent: Double,
        slippagePercent: Double
    ): Result<PaperTrade> {
        val openPositions = repository.openPositions.first()
        val position = openPositions.firstOrNull { it.id == positionId }
            ?: return Result.failure(NoSuchElementException("Position not found: $positionId"))

        val executionPrice = currentPrice * (1.0 - slippagePercent / 100.0)
        val grossAmount = position.quantity * executionPrice
        val exitFee = grossAmount * (feePercent / 100.0)
        val grossPnl = (executionPrice - position.entryPrice) * position.quantity
        val netPnl = grossPnl - position.entryFee - exitFee
        val pnlPercent = (netPnl / position.investedUsdt) * 100.0

        val closedPosition = position.copy(
            currentPrice = executionPrice,
            status = reason,
            exitFee = exitFee,
            closedAt = System.currentTimeMillis(),
            closePrice = executionPrice,
            realizedPnl = netPnl,
            realizedPnlPercent = pnlPercent
        )

        val trade = PaperTrade(
            id = UUID.randomUUID().toString(),
            positionId = positionId,
            symbol = position.symbol,
            side = "SELL",
            price = executionPrice,
            quantity = position.quantity,
            usdtAmount = grossAmount,
            fee = exitFee,
            pnl = netPnl,
            pnlPercent = pnlPercent,
            reason = reason.name,
            timestamp = System.currentTimeMillis()
        )

        repository.updatePosition(closedPosition)
        repository.saveTrade(trade)

        // Update paper balance in user settings with realized PnL
        val settings = repository.userSettings.first()
        repository.saveSettings(settings.copy(paperBalanceUsdt = settings.paperBalanceUsdt + netPnl))

        return Result.success(trade)
    }
}
