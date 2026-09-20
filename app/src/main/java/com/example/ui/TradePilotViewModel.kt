package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TradePilotDatabase
import com.example.data.model.BacktestConfig
import com.example.data.model.BacktestResult
import com.example.data.model.BinanceConnectedAccount
import com.example.data.model.Candle
import com.example.data.model.IndicatorSnapshot
import com.example.data.model.OrderBookDepth
import com.example.data.model.PaperPortfolio
import com.example.data.model.PaperPosition
import com.example.data.model.PortfolioTradeAdvisory
import com.example.data.model.PositionStatus
import com.example.data.model.TickerData
import com.example.data.model.TimeFrame
import com.example.data.model.TradingSignal
import com.example.data.model.UserSettings
import com.example.data.remote.AiMarketBriefing
import com.example.data.remote.AiSetupAnalysis
import com.example.data.remote.BinanceAccountSyncService
import com.example.data.remote.BinanceApiService
import com.example.data.remote.BinanceWebSocketManager
import com.example.data.remote.ConnectionStatus
import com.example.data.remote.GeminiService
import com.example.data.remote.TelegramService
import com.example.data.repository.TradePilotRepository
import com.example.domain.advisor.PortfolioAdvisorEngine
import com.example.domain.alert.AlertManager
import com.example.domain.backtest.BacktestEngine
import com.example.domain.execution.PaperTradingExecutionService
import com.example.domain.indicators.TechnicalIndicators
import com.example.domain.risk.RiskCalculator
import com.example.domain.strategy.SignalEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class TradePilotViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TradePilotDatabase.getInstance(application)
    val repository = TradePilotRepository(db)
    val webSocketManager = BinanceWebSocketManager(viewModelScope)
    private val apiService = BinanceApiService.create()
    val paperService = PaperTradingExecutionService(repository)
    private val alertManager = AlertManager(application, viewModelScope)

    val connectionStatus: StateFlow<ConnectionStatus> = webSocketManager.connectionStatus
    val lastMessageTimestamp: StateFlow<Long> = webSocketManager.lastMessageTimestamp
    val tickers: StateFlow<Map<String, TickerData>> = webSocketManager.tickers
    val orderBookDepthMap: StateFlow<Map<String, OrderBookDepth>> = webSocketManager.orderBookDepth

    private val _selectedSymbol = MutableStateFlow("BTCUSDT")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(TimeFrame.M15)
    val selectedTimeframe: StateFlow<TimeFrame> = _selectedTimeframe.asStateFlow()

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _htfCandles = MutableStateFlow<List<Candle>>(emptyList())

    private val _indicatorSnapshot = MutableStateFlow<IndicatorSnapshot?>(null)
    val indicatorSnapshot: StateFlow<IndicatorSnapshot?> = _indicatorSnapshot.asStateFlow()

    private val _activeSignal = MutableStateFlow<TradingSignal?>(null)
    val activeSignal: StateFlow<TradingSignal?> = _activeSignal.asStateFlow()

    private val _signalsMap = MutableStateFlow<Map<String, TradingSignal>>(emptyMap())
    val signalsMap: StateFlow<Map<String, TradingSignal>> = _signalsMap.asStateFlow()

    val signalsHistory = repository.allSignals.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val userSettings = repository.userSettings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings()
    )

    // Paper Portfolio State: combines settings balance, open positions, and closed trades
    val paperPortfolio: StateFlow<PaperPortfolio> = combine(
        repository.userSettings,
        repository.openPositions,
        repository.allTrades
    ) { settings, positions, trades ->
        val investedInOpen = positions.sumOf { it.investedUsdt }
        val available = kotlin.math.max(0.0, settings.paperBalanceUsdt - investedInOpen)
        PaperPortfolio(
            startingBalance = 10000.0,
            availableUsdt = available,
            openPositions = positions,
            closedTrades = trades
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaperPortfolio())

    // AI States
    private val _aiAnalysis = MutableStateFlow<AiSetupAnalysis?>(null)
    val aiAnalysis: StateFlow<AiSetupAnalysis?> = _aiAnalysis.asStateFlow()

    private val _isAnalyzingAi = MutableStateFlow(false)
    val isAnalyzingAi: StateFlow<Boolean> = _isAnalyzingAi.asStateFlow()

    private val _aiBriefing = MutableStateFlow<AiMarketBriefing?>(null)
    val aiBriefing: StateFlow<AiMarketBriefing?> = _aiBriefing.asStateFlow()

    private val _isGeneratingBriefing = MutableStateFlow(false)
    val isGeneratingBriefing: StateFlow<Boolean> = _isGeneratingBriefing.asStateFlow()

    // Backtest State
    private val _backtestResult = MutableStateFlow<BacktestResult?>(null)
    val backtestResult: StateFlow<BacktestResult?> = _backtestResult.asStateFlow()

    private val _isBacktesting = MutableStateFlow(false)
    val isBacktesting: StateFlow<Boolean> = _isBacktesting.asStateFlow()

    // Real Binance Account Sync State
    private val _connectedAccount = MutableStateFlow(BinanceConnectedAccount())
    val connectedAccount: StateFlow<BinanceConnectedAccount> = _connectedAccount.asStateFlow()

    private val _isSyncingAccount = MutableStateFlow(false)
    val isSyncingAccount: StateFlow<Boolean> = _isSyncingAccount.asStateFlow()

    // Portfolio-Aware Trade Advisories (What, When, How to trade based on user's real balance & holdings)
    val portfolioAdvisories: StateFlow<List<PortfolioTradeAdvisory>> = combine(
        _signalsMap,
        tickers,
        _connectedAccount,
        userSettings
    ) { sigMap, tickerMap, account, settings ->
        PortfolioAdvisorEngine.generatePortfolioAdvisories(
            signals = sigMap.values.toList(),
            tickers = tickerMap,
            account = account,
            settings = settings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val candleCache = ConcurrentHashMap<String, MutableList<Candle>>()

    init {
        viewModelScope.launch {
            val settings = userSettings.value
            // If user previously saved Binance API keys, automatically sync account
            if (settings.binanceApiKey.isNotBlank() && settings.binanceApiSecret.isNotBlank()) {
                syncBinanceAccount(settings.binanceApiKey, settings.binanceApiSecret)
            }
            // Immediate REST ticker snapshot to ensure instant real Binance market prices
            loadInitialTickerSnapshot(settings.watchedSymbols)

            webSocketManager.connect(
                symbols = settings.watchedSymbols,
                selectedSymbol = _selectedSymbol.value,
                timeframe = _selectedTimeframe.value.apiValue
            )
            loadHistoricalKlines(_selectedSymbol.value, _selectedTimeframe.value)
            loadHigherTimeframeKlines(_selectedSymbol.value)
            preloadWatchlistSignals(settings.watchedSymbols)
        }

        // Collect live candle updates from WebSocket
        viewModelScope.launch {
            webSocketManager.liveCandle.collect { pair ->
                if (pair != null) {
                    val (symbol, liveCandle) = pair
                    if (symbol.equals(_selectedSymbol.value, ignoreCase = true)) {
                        updateLiveCandle(liveCandle)
                    }
                }
            }
        }

        // Monitor prices from tickers to update open paper positions and trigger TP / SL
        viewModelScope.launch {
            tickers.collect { tickerMap ->
                val positions = repository.openPositions.stateIn(viewModelScope).value
                for (pos in positions) {
                    val ticker = tickerMap[pos.symbol] ?: continue
                    val curPrice = ticker.lastPrice
                    if (curPrice <= 0) continue

                    // Update current price of open position
                    if (pos.currentPrice != curPrice) {
                        repository.updatePosition(pos.copy(currentPrice = curPrice))
                    }

                    // Check Stop Loss
                    if (curPrice <= pos.stopLoss) {
                        paperService.closePosition(
                            positionId = pos.id,
                            currentPrice = curPrice,
                            reason = PositionStatus.CLOSED_STOP_LOSS,
                            feePercent = userSettings.value.binanceFeePercent,
                            slippagePercent = userSettings.value.slippagePercent
                        )
                        alertManager.onPaperTradeExecuted(pos, PositionStatus.CLOSED_STOP_LOSS, userSettings.value)
                    }
                    // Check Take Profit 1
                    else if (curPrice >= pos.takeProfit1) {
                        paperService.closePosition(
                            positionId = pos.id,
                            currentPrice = curPrice,
                            reason = PositionStatus.CLOSED_TP1,
                            feePercent = userSettings.value.binanceFeePercent,
                            slippagePercent = userSettings.value.slippagePercent
                        )
                        alertManager.onPaperTradeExecuted(pos, PositionStatus.CLOSED_TP1, userSettings.value)
                    }
                }
            }
        }
    }

    fun selectSymbol(symbol: String) {
        if (_selectedSymbol.value == symbol) return
        _selectedSymbol.value = symbol
        webSocketManager.updateSelectedSymbol(symbol, _selectedTimeframe.value.apiValue)
        loadHistoricalKlines(symbol, _selectedTimeframe.value)
        loadHigherTimeframeKlines(symbol)
    }

    fun selectTimeframe(tf: TimeFrame) {
        if (_selectedTimeframe.value == tf) return
        _selectedTimeframe.value = tf
        webSocketManager.updateSelectedSymbol(_selectedSymbol.value, tf.apiValue)
        loadHistoricalKlines(_selectedSymbol.value, tf)
    }

    private fun loadHistoricalKlines(symbol: String, timeframe: TimeFrame) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getKlines(symbol, timeframe.apiValue, limit = 100)
                val bodyStr = response.string()
                val jsonArr = JSONArray(bodyStr)
                val list = mutableListOf<Candle>()

                for (i in 0 until jsonArr.length()) {
                    val k = jsonArr.getJSONArray(i)
                    list.add(
                        Candle(
                            openTime = k.getLong(0),
                            open = k.getString(1).toDouble(),
                            high = k.getString(2).toDouble(),
                            low = k.getString(3).toDouble(),
                            close = k.getString(4).toDouble(),
                            volume = k.getString(5).toDouble(),
                            closeTime = k.getLong(6),
                            isClosed = true
                        )
                    )
                }

                _candles.value = list
                candleCache[symbol] = list.toMutableList()
                recomputeIndicatorsAndSignal(symbol, timeframe, list)
            } catch (e: Exception) {
                Log.e("TradePilotVM", "Error loading klines for $symbol: ${e.message}")
            }
        }
    }

    private fun loadHigherTimeframeKlines(symbol: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getKlines(symbol, "1h", limit = 60)
                val bodyStr = response.string()
                val jsonArr = JSONArray(bodyStr)
                val list = mutableListOf<Candle>()
                for (i in 0 until jsonArr.length()) {
                    val k = jsonArr.getJSONArray(i)
                    list.add(
                        Candle(
                            openTime = k.getLong(0),
                            open = k.getString(1).toDouble(),
                            high = k.getString(2).toDouble(),
                            low = k.getString(3).toDouble(),
                            close = k.getString(4).toDouble(),
                            volume = k.getString(5).toDouble(),
                            closeTime = k.getLong(6),
                            isClosed = true
                        )
                    )
                }
                _htfCandles.value = list
            } catch (e: Exception) {
                Log.w("TradePilotVM", "HTF klines load failed: ${e.message}")
            }
        }
    }

    private fun loadInitialTickerSnapshot(symbols: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (sym in symbols) {
                try {
                    val response = apiService.get24hrTicker(sym)
                    val bodyStr = response.string()
                    val json = JSONObject(bodyStr)
                    val ticker = TickerData(
                        symbol = sym,
                        lastPrice = json.optString("lastPrice", "0.0").toDoubleOrNull() ?: 0.0,
                        priceChange = json.optString("priceChange", "0.0").toDoubleOrNull() ?: 0.0,
                        priceChangePercent = json.optString("priceChangePercent", "0.0").toDoubleOrNull() ?: 0.0,
                        highPrice = json.optString("highPrice", "0.0").toDoubleOrNull() ?: 0.0,
                        lowPrice = json.optString("lowPrice", "0.0").toDoubleOrNull() ?: 0.0,
                        volume = json.optString("volume", "0.0").toDoubleOrNull() ?: 0.0,
                        quoteVolume = json.optString("quoteVolume", "0.0").toDoubleOrNull() ?: 0.0,
                        timestamp = System.currentTimeMillis()
                    )
                    webSocketManager.updateTicker(ticker)
                } catch (e: Exception) {
                    Log.w("TradePilotVM", "Initial ticker load failed for $sym: ${e.message}")
                }
            }
        }
    }

    private fun preloadWatchlistSignals(symbols: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (sym in symbols) {
                try {
                    val response = apiService.getKlines(sym, "15m", limit = 50)
                    val bodyStr = response.string()
                    val jsonArr = JSONArray(bodyStr)
                    val list = mutableListOf<Candle>()
                    for (i in 0 until jsonArr.length()) {
                        val k = jsonArr.getJSONArray(i)
                        list.add(
                            Candle(
                                openTime = k.getLong(0),
                                open = k.getString(1).toDouble(),
                                high = k.getString(2).toDouble(),
                                low = k.getString(3).toDouble(),
                                close = k.getString(4).toDouble(),
                                volume = k.getString(5).toDouble(),
                                closeTime = k.getLong(6),
                                isClosed = true
                            )
                        )
                    }
                    if (list.isNotEmpty()) {
                        val snapshot = TechnicalIndicators.computeSnapshot(list)
                        val sig = SignalEngine.evaluateSignal(
                            symbol = sym,
                            timeframe = TimeFrame.M15,
                            candles = list,
                            indicators = snapshot
                        )
                        val currentMap = _signalsMap.value.toMutableMap()
                        currentMap[sym] = sig
                        _signalsMap.value = currentMap
                    }
                } catch (e: Exception) {
                    Log.w("TradePilotVM", "Preload signal failed for $sym: ${e.message}")
                }
                delay(100) // gentle rate-limit pacing
            }
        }
    }

    private fun updateLiveCandle(candle: Candle) {
        val current = _candles.value.toMutableList()
        if (current.isEmpty()) {
            current.add(candle)
        } else {
            val last = current.last()
            if (last.openTime == candle.openTime) {
                // Update the current in-progress candle
                current[current.lastIndex] = candle
            } else if (candle.openTime > last.openTime) {
                // New candle started
                current.add(candle)
                if (current.size > 120) current.removeAt(0)
            }
        }
        _candles.value = current
        recomputeIndicatorsAndSignal(_selectedSymbol.value, _selectedTimeframe.value, current)
    }

    private fun recomputeIndicatorsAndSignal(
        symbol: String,
        timeframe: TimeFrame,
        candlesList: List<Candle>
    ) {
        val snapshot = TechnicalIndicators.computeSnapshot(candlesList)
        _indicatorSnapshot.value = snapshot

        val htfSnapshot = if (_htfCandles.value.isNotEmpty()) {
            TechnicalIndicators.computeSnapshot(_htfCandles.value)
        } else null

        val orderBook = orderBookDepthMap.value[symbol]

        val signal = SignalEngine.evaluateSignal(
            symbol = symbol,
            timeframe = timeframe,
            candles = candlesList,
            indicators = snapshot,
            higherTimeframeIndicators = htfSnapshot,
            orderBook = orderBook
        )
        _activeSignal.value = signal

        val updatedMap = _signalsMap.value.toMutableMap()
        updatedMap[symbol] = signal
        _signalsMap.value = updatedMap

        // Persist high quality signals to Room and notify if threshold met
        if (signal.score >= userSettings.value.signalScoreThreshold) {
            viewModelScope.launch {
                repository.saveSignal(signal)
                alertManager.onSignalGenerated(signal, userSettings.value)
            }
        }
    }

    fun executePaperTrade(signal: TradingSignal) {
        viewModelScope.launch {
            val settings = userSettings.value
            val currentPortfolio = paperPortfolio.value
            val orderBook = orderBookDepthMap.value[signal.symbol]

            val riskCalc = RiskCalculator.calculateRisk(
                signal = signal,
                settings = settings,
                currentBalance = currentPortfolio.availableUsdt,
                currentOpenTradesCount = currentPortfolio.openPositions.size,
                orderBook = orderBook
            )

            if (!riskCalc.isAllowed) {
                Log.w("TradePilotVM", "Paper trade rejected: ${riskCalc.rejectionReason}")
                return@launch
            }

            paperService.placeBuyOrder(
                symbol = signal.symbol,
                price = signal.price,
                usdtAmount = riskCalc.suggestedPositionUsdt,
                stopLoss = signal.stopLoss,
                takeProfit1 = signal.takeProfit1,
                takeProfit2 = signal.takeProfit2,
                feePercent = settings.binanceFeePercent,
                slippagePercent = settings.slippagePercent
            )
        }
    }

    fun closePaperPositionManually(positionId: String, currentPrice: Double) {
        viewModelScope.launch {
            paperService.closePosition(
                positionId = positionId,
                currentPrice = currentPrice,
                reason = PositionStatus.CLOSED_MANUAL,
                feePercent = userSettings.value.binanceFeePercent,
                slippagePercent = userSettings.value.slippagePercent
            )
        }
    }

    fun analyzeSetupWithAi(signal: TradingSignal) {
        val snapshot = _indicatorSnapshot.value ?: return
        viewModelScope.launch {
            _isAnalyzingAi.value = true
            val result = GeminiService.analyzeSetup(signal, snapshot)
            result.onSuccess { _aiAnalysis.value = it }
            _isAnalyzingAi.value = false
        }
    }

    fun generateMarketBriefing() {
        viewModelScope.launch {
            _isGeneratingBriefing.value = true
            val currentSignals = _signalsMap.value.values.toList()
            val result = GeminiService.generateMarketBriefing(currentSignals)
            result.onSuccess { _aiBriefing.value = it }
            _isGeneratingBriefing.value = false
        }
    }

    fun runBacktest(config: BacktestConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBacktesting.value = true
            try {
                // Fetch 250 historical candles for backtesting
                val response = apiService.getKlines(config.symbol, config.timeframe.apiValue, limit = 250)
                val bodyStr = response.string()
                val jsonArr = JSONArray(bodyStr)
                val list = mutableListOf<Candle>()
                for (i in 0 until jsonArr.length()) {
                    val k = jsonArr.getJSONArray(i)
                    list.add(
                        Candle(
                            openTime = k.getLong(0),
                            open = k.getString(1).toDouble(),
                            high = k.getString(2).toDouble(),
                            low = k.getString(3).toDouble(),
                            close = k.getString(4).toDouble(),
                            volume = k.getString(5).toDouble(),
                            closeTime = k.getLong(6),
                            isClosed = true
                        )
                    )
                }

                val result = BacktestEngine.runBacktest(list, config)
                _backtestResult.value = result
            } catch (e: Exception) {
                Log.e("TradePilotVM", "Backtest failed: ${e.message}")
            } finally {
                _isBacktesting.value = false
            }
        }
    }

    fun saveSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            // Reconnect websocket if symbols changed
            webSocketManager.connect(
                symbols = settings.watchedSymbols,
                selectedSymbol = _selectedSymbol.value,
                timeframe = _selectedTimeframe.value.apiValue
            )
        }
    }

    fun resetPaperData() {
        viewModelScope.launch {
            repository.resetPaperData()
            val current = userSettings.value
            repository.saveSettings(current.copy(paperBalanceUsdt = 10000.0))
        }
    }

    suspend fun testTelegram(botToken: String, chatId: String): Result<Boolean> {
        return TelegramService.testConnection(botToken, chatId)
    }

    fun syncBinanceAccount(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            _isSyncingAccount.value = true
            val result = BinanceAccountSyncService.syncAccount(apiKey, apiSecret, apiService)
            result.onSuccess { account ->
                _connectedAccount.value = account
                // Automatically reflect synced live USDT balance in paper portfolio if desired
                val current = userSettings.value
                if (account.freeUsdt > 0.0) {
                    repository.saveSettings(current.copy(
                        paperBalanceUsdt = account.freeUsdt,
                        binanceApiKey = apiKey,
                        binanceApiSecret = apiSecret
                    ))
                } else {
                    repository.saveSettings(current.copy(
                        binanceApiKey = apiKey,
                        binanceApiSecret = apiSecret
                    ))
                }
            }.onFailure { err ->
                _connectedAccount.value = _connectedAccount.value.copy(
                    errorMessage = err.message ?: "Failed to connect to Binance"
                )
            }
            _isSyncingAccount.value = false
        }
    }

    fun disconnectBinanceAccount() {
        viewModelScope.launch {
            _connectedAccount.value = BinanceConnectedAccount()
            val current = userSettings.value
            repository.saveSettings(current.copy(binanceApiKey = "", binanceApiSecret = ""))
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
