package com.example.data.remote

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.OrderBookDepth
import com.example.data.model.OrderBookEntry
import com.example.data.model.TickerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class ConnectionStatus {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    RECONNECTING,
    ERROR
}

class BinanceWebSocketManager(
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isClosedManually = AtomicBoolean(false)
    private var reconnectJob: Job? = null
    private var backoffDelayMs = 1000L

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _tickers = MutableStateFlow<Map<String, TickerData>>(emptyMap())
    val tickers: StateFlow<Map<String, TickerData>> = _tickers.asStateFlow()

    private val _liveCandle = MutableStateFlow<Pair<String, Candle>?>(null)
    val liveCandle: StateFlow<Pair<String, Candle>?> = _liveCandle.asStateFlow()

    private val _orderBookDepth = MutableStateFlow<Map<String, OrderBookDepth>>(emptyMap())
    val orderBookDepth: StateFlow<Map<String, OrderBookDepth>> = _orderBookDepth.asStateFlow()

    private val _lastMessageTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastMessageTimestamp: StateFlow<Long> = _lastMessageTimestamp.asStateFlow()

    private var currentSymbols: List<String> = emptyList()
    private var activeSelectedSymbol: String = "BTCUSDT"
    private var activeTimeframe: String = "15m"

    fun connect(symbols: List<String>, selectedSymbol: String = "BTCUSDT", timeframe: String = "15m") {
        currentSymbols = symbols.map { it.uppercase(Locale.US) }
        activeSelectedSymbol = selectedSymbol.uppercase(Locale.US)
        activeTimeframe = timeframe
        isClosedManually.set(false)
        startWebSocket()
    }

    fun updateSelectedSymbol(symbol: String, timeframe: String) {
        if (activeSelectedSymbol != symbol.uppercase(Locale.US) || activeTimeframe != timeframe) {
            activeSelectedSymbol = symbol.uppercase(Locale.US)
            activeTimeframe = timeframe
            // Reconnect to update dedicated kline & depth stream for focused pair
            startWebSocket()
        }
    }

    private fun startWebSocket() {
        webSocket?.cancel()
        reconnectJob?.cancel()

        _connectionStatus.value = ConnectionStatus.CONNECTING

        // Build combined streams URL
        // 1. Ticker streams for all watched symbols: <symbol>@ticker
        // 2. Active symbol kline and depth streams: <active>@kline_<timeframe> and <active>@depth20@100ms
        val streamNames = mutableListOf<String>()
        currentSymbols.forEach { sym ->
            streamNames.add("${sym.lowercase(Locale.US)}@ticker")
        }
        val activeLower = activeSelectedSymbol.lowercase(Locale.US)
        streamNames.add("${activeLower}@kline_${activeTimeframe}")
        streamNames.add("${activeLower}@depth20@100ms")

        val streamParam = streamNames.distinct().joinToString("/")
        val url = "wss://stream.binance.com:9443/stream?streams=$streamParam"

        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("BinanceWS", "WebSocket connected: $url")
                _connectionStatus.value = ConnectionStatus.CONNECTED
                backoffDelayMs = 1000L
                _lastMessageTimestamp.value = System.currentTimeMillis()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _lastMessageTimestamp.value = System.currentTimeMillis()
                try {
                    handleIncomingMessage(text)
                } catch (e: Exception) {
                    Log.e("BinanceWS", "Error handling message", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("BinanceWS", "WebSocket closed: $code / $reason")
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                if (!isClosedManually.get()) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("BinanceWS", "WebSocket failure: ${t.message}")
                _connectionStatus.value = ConnectionStatus.ERROR
                if (!isClosedManually.get()) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun handleIncomingMessage(jsonStr: String) {
        val root = JSONObject(jsonStr)
        if (!root.has("stream") || !root.has("data")) return

        val stream = root.getString("stream")
        val data = root.getJSONObject("data")

        when {
            stream.endsWith("@ticker") -> {
                parseTicker(data)
            }
            stream.contains("@kline_") -> {
                parseKline(data)
            }
            stream.contains("@depth20") -> {
                parseDepth(stream, data)
            }
        }
    }

    private fun parseTicker(data: JSONObject) {
        val sym = data.optString("s")
        if (sym.isEmpty()) return
        val lastPrice = data.optString("c", "0.0").toDoubleOrNull() ?: 0.0
        val priceChange = data.optString("p", "0.0").toDoubleOrNull() ?: 0.0
        val priceChangePct = data.optString("P", "0.0").toDoubleOrNull() ?: 0.0
        val high = data.optString("h", "0.0").toDoubleOrNull() ?: 0.0
        val low = data.optString("l", "0.0").toDoubleOrNull() ?: 0.0
        val volume = data.optString("v", "0.0").toDoubleOrNull() ?: 0.0
        val quoteVol = data.optString("q", "0.0").toDoubleOrNull() ?: 0.0

        val ticker = TickerData(
            symbol = sym,
            lastPrice = lastPrice,
            priceChange = priceChange,
            priceChangePercent = priceChangePct,
            highPrice = high,
            lowPrice = low,
            volume = volume,
            quoteVolume = quoteVol,
            timestamp = System.currentTimeMillis()
        )

        val updatedMap = _tickers.value.toMutableMap()
        updatedMap[sym] = ticker
        _tickers.value = updatedMap
    }

    fun updateTicker(ticker: TickerData) {
        val updatedMap = _tickers.value.toMutableMap()
        updatedMap[ticker.symbol] = ticker
        _tickers.value = updatedMap
    }

    private fun parseKline(data: JSONObject) {
        val sym = data.optString("s")
        val k = data.optJSONObject("k") ?: return

        val candle = Candle(
            openTime = k.optLong("t"),
            open = k.optString("o").toDoubleOrNull() ?: 0.0,
            high = k.optString("h").toDoubleOrNull() ?: 0.0,
            low = k.optString("l").toDoubleOrNull() ?: 0.0,
            close = k.optString("c").toDoubleOrNull() ?: 0.0,
            volume = k.optString("v").toDoubleOrNull() ?: 0.0,
            closeTime = k.optLong("T"),
            isClosed = k.optBoolean("x", false)
        )
        _liveCandle.value = Pair(sym, candle)
    }

    private fun parseDepth(stream: String, data: JSONObject) {
        val sym = activeSelectedSymbol
        val bidsArr = data.optJSONArray("bids") ?: JSONArray()
        val asksArr = data.optJSONArray("asks") ?: JSONArray()

        val bids = mutableListOf<OrderBookEntry>()
        for (i in 0 until bidsArr.length()) {
            val item = bidsArr.getJSONArray(i)
            bids.add(OrderBookEntry(item.getString(0).toDouble(), item.getString(1).toDouble()))
        }

        val asks = mutableListOf<OrderBookEntry>()
        for (i in 0 until asksArr.length()) {
            val item = asksArr.getJSONArray(i)
            asks.add(OrderBookEntry(item.getString(0).toDouble(), item.getString(1).toDouble()))
        }

        val depth = OrderBookDepth(
            symbol = sym,
            bids = bids,
            asks = asks,
            timestamp = System.currentTimeMillis()
        )

        val map = _orderBookDepth.value.toMutableMap()
        map[sym] = depth
        _orderBookDepth.value = map
    }

    private fun scheduleReconnect() {
        if (isClosedManually.get()) return
        reconnectJob?.cancel()
        _connectionStatus.value = ConnectionStatus.RECONNECTING

        reconnectJob = scope.launch(Dispatchers.IO) {
            Log.d("BinanceWS", "Reconnecting in ${backoffDelayMs}ms...")
            delay(backoffDelayMs)
            backoffDelayMs = minOf(backoffDelayMs * 2, 30000L) // Exponential backoff up to 30s
            if (isActive && !isClosedManually.get()) {
                startWebSocket()
            }
        }
    }

    fun disconnect() {
        isClosedManually.set(true)
        reconnectJob?.cancel()
        webSocket?.close(1000, "App closed")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }
}
