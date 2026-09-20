package com.example.data.repository

import com.example.data.local.PaperPositionEntity
import com.example.data.local.PaperTradeEntity
import com.example.data.local.SignalEntity
import com.example.data.local.TradePilotDatabase
import com.example.data.local.UserSettingsEntity
import com.example.data.model.MarketRegime
import com.example.data.model.PaperPosition
import com.example.data.model.PaperTrade
import com.example.data.model.PositionStatus
import com.example.data.model.SignalType
import com.example.data.model.TimeFrame
import com.example.data.model.TradingSignal
import com.example.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class TradePilotRepository(private val db: TradePilotDatabase) {

    val allSignals: Flow<List<TradingSignal>> = db.signalDao().getAllSignals().map { list ->
        list.map { it.toDomain() }
    }

    val openPositions: Flow<List<PaperPosition>> = db.paperTradingDao().getOpenPositions().map { list ->
        list.map { it.toDomain() }
    }

    val allTrades: Flow<List<PaperTrade>> = db.paperTradingDao().getAllTrades().map { list ->
        list.map { it.toDomain() }
    }

    val userSettings: Flow<UserSettings> = db.settingsDao().getSettings().map { entity ->
        entity?.toDomain() ?: UserSettings()
    }

    suspend fun saveSignal(signal: TradingSignal) {
        db.signalDao().insertSignal(signal.toEntity())
    }

    suspend fun updateSignalStatus(id: String, status: String) {
        db.signalDao().updateStatus(id, status)
    }

    suspend fun savePosition(position: PaperPosition) {
        db.paperTradingDao().insertPosition(position.toEntity())
    }

    suspend fun updatePosition(position: PaperPosition) {
        db.paperTradingDao().updatePosition(position.toEntity())
    }

    suspend fun saveTrade(trade: PaperTrade) {
        db.paperTradingDao().insertTrade(trade.toEntity())
    }

    suspend fun saveSettings(settings: UserSettings) {
        db.settingsDao().saveSettings(settings.toEntity())
    }

    suspend fun resetPaperData() {
        db.paperTradingDao().clearPositions()
        db.paperTradingDao().clearTrades()
    }

    // Mappers
    private fun SignalEntity.toDomain(): TradingSignal {
        val reasonsList = parseJsonArray(reasonsJson)
        val warningsList = parseJsonArray(warningsJson)
        return TradingSignal(
            id = id,
            symbol = symbol,
            timeframe = TimeFrame.fromApiValue(timeframe),
            timestamp = timestamp,
            price = price,
            signalType = try { SignalType.valueOf(signalType) } catch (e: Exception) { SignalType.WAIT },
            score = score,
            scoreBreakdown = emptyList(),
            marketRegime = try { MarketRegime.valueOf(marketRegime) } catch (e: Exception) { MarketRegime.SIDEWAYS },
            suggestedEntryMin = suggestedEntryMin,
            suggestedEntryMax = suggestedEntryMax,
            stopLoss = stopLoss,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            riskRewardRatio = riskRewardRatio,
            reasons = reasonsList,
            warnings = warningsList,
            dataFreshnessSeconds = (System.currentTimeMillis() - timestamp) / 1000,
            status = status
        )
    }

    private fun TradingSignal.toEntity(): SignalEntity {
        return SignalEntity(
            id = id,
            symbol = symbol,
            timeframe = timeframe.apiValue,
            timestamp = timestamp,
            price = price,
            signalType = signalType.name,
            score = score,
            marketRegime = marketRegime.name,
            suggestedEntryMin = suggestedEntryMin,
            suggestedEntryMax = suggestedEntryMax,
            stopLoss = stopLoss,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            riskRewardRatio = riskRewardRatio,
            reasonsJson = JSONArray(reasons).toString(),
            warningsJson = JSONArray(warnings).toString(),
            status = status
        )
    }

    private fun PaperPositionEntity.toDomain(): PaperPosition {
        return PaperPosition(
            id = id,
            symbol = symbol,
            entryPrice = entryPrice,
            currentPrice = currentPrice,
            quantity = quantity,
            investedUsdt = investedUsdt,
            stopLoss = stopLoss,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            openedAt = openedAt,
            status = try { PositionStatus.valueOf(status) } catch (e: Exception) { PositionStatus.OPEN },
            entryFee = entryFee,
            exitFee = exitFee,
            closedAt = closedAt,
            closePrice = closePrice,
            realizedPnl = realizedPnl,
            realizedPnlPercent = realizedPnlPercent
        )
    }

    private fun PaperPosition.toEntity(): PaperPositionEntity {
        return PaperPositionEntity(
            id = id,
            symbol = symbol,
            entryPrice = entryPrice,
            currentPrice = currentPrice,
            quantity = quantity,
            investedUsdt = investedUsdt,
            stopLoss = stopLoss,
            takeProfit1 = takeProfit1,
            takeProfit2 = takeProfit2,
            openedAt = openedAt,
            status = status.name,
            entryFee = entryFee,
            exitFee = exitFee,
            closedAt = closedAt,
            closePrice = closePrice,
            realizedPnl = realizedPnl,
            realizedPnlPercent = realizedPnlPercent
        )
    }

    private fun PaperTradeEntity.toDomain(): PaperTrade {
        return PaperTrade(
            id = id,
            positionId = positionId,
            symbol = symbol,
            side = side,
            price = price,
            quantity = quantity,
            usdtAmount = usdtAmount,
            fee = fee,
            pnl = pnl,
            pnlPercent = pnlPercent,
            reason = reason,
            timestamp = timestamp
        )
    }

    private fun PaperTrade.toEntity(): PaperTradeEntity {
        return PaperTradeEntity(
            id = id,
            positionId = positionId,
            symbol = symbol,
            side = side,
            price = price,
            quantity = quantity,
            usdtAmount = usdtAmount,
            fee = fee,
            pnl = pnl,
            pnlPercent = pnlPercent,
            reason = reason,
            timestamp = timestamp
        )
    }

    private fun UserSettingsEntity.toDomain(): UserSettings {
        return UserSettings(
            watchedSymbols = watchedSymbolsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            defaultTimeframe = TimeFrame.fromApiValue(defaultTimeframe),
            signalScoreThreshold = signalScoreThreshold,
            paperBalanceUsdt = paperBalanceUsdt,
            riskPerTradePercent = riskPerTradePercent,
            maxOpenTrades = maxOpenTrades,
            maxDailyLossPercent = maxDailyLossPercent,
            minRiskRewardRatio = minRiskRewardRatio,
            maxPositionSizePercent = maxPositionSizePercent,
            binanceFeePercent = binanceFeePercent,
            slippagePercent = slippagePercent,
            notificationsEnabled = notificationsEnabled,
            telegramEnabled = telegramEnabled,
            telegramBotToken = telegramBotToken,
            telegramChatId = telegramChatId,
            binanceApiKey = binanceApiKey,
            binanceApiSecret = binanceApiSecret,
            isLiveTradingEnabled = false
        )
    }

    private fun UserSettings.toEntity(): UserSettingsEntity {
        return UserSettingsEntity(
            id = 1,
            watchedSymbolsCsv = watchedSymbols.joinToString(","),
            defaultTimeframe = defaultTimeframe.apiValue,
            signalScoreThreshold = signalScoreThreshold,
            paperBalanceUsdt = paperBalanceUsdt,
            riskPerTradePercent = riskPerTradePercent,
            maxOpenTrades = maxOpenTrades,
            maxDailyLossPercent = maxDailyLossPercent,
            minRiskRewardRatio = minRiskRewardRatio,
            maxPositionSizePercent = maxPositionSizePercent,
            binanceFeePercent = binanceFeePercent,
            slippagePercent = slippagePercent,
            notificationsEnabled = notificationsEnabled,
            telegramEnabled = telegramEnabled,
            telegramBotToken = telegramBotToken,
            telegramChatId = telegramChatId,
            binanceApiKey = binanceApiKey,
            binanceApiSecret = binanceApiSecret
        )
    }

    private fun parseJsonArray(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
