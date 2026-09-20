package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.IndicatorSnapshot
import com.example.data.model.TradingSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class AiSetupAnalysis(
    val summary: String,
    val marketCondition: String,
    val setupExplanation: String,
    val keyRisks: String,
    val invalidationCondition: String,
    val recommendedAction: String
)

data class AiMarketBriefing(
    val title: String,
    val summary: String,
    val strongestBullish: String,
    val strongestBearish: String,
    val marketWideCondition: String,
    val majorRisks: String,
    val pairsToWatch: List<String>,
    val pairsToAvoid: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Cache responses for 5 minutes to prevent redundant API calls
    private val analysisCache = ConcurrentHashMap<String, Pair<Long, AiSetupAnalysis>>()
    private var cachedBriefing: Pair<Long, AiMarketBriefing>? = null

    suspend fun analyzeSetup(
        signal: TradingSignal,
        snapshot: IndicatorSnapshot
    ): Result<AiSetupAnalysis> = withContext(Dispatchers.IO) {
        val cacheKey = "${signal.symbol}_${signal.timeframe.apiValue}_${signal.score}"
        val now = System.currentTimeMillis()
        val cached = analysisCache[cacheKey]
        if (cached != null && (now - cached.first) < 300_000L) {
            return@withContext Result.success(cached.second)
        }

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback generation when API key is not yet set in AI Studio Secrets
            val fallback = AiSetupAnalysis(
                summary = "${signal.symbol} shows a ${signal.signalType.label} with a score of ${signal.score}/10.",
                marketCondition = "${signal.marketRegime.label} market regime with price hovering at $${String.format("%.2f", signal.price)}.",
                setupExplanation = signal.reasons.joinToString(". ") + ".",
                keyRisks = if (signal.warnings.isNotEmpty()) signal.warnings.joinToString(". ") + "." else "Normal market volatility risk applies.",
                invalidationCondition = "Setup invalidates if candle closes below Stop Loss at $${String.format("%.2f", signal.stopLoss)}.",
                recommendedAction = signal.signalType.name
            )
            return@withContext Result.success(fallback)
        }

        try {
            val prompt = """
                You are TradePilot's AI Market Analyst. The deterministic mathematical trading engine has already analyzed the Binance Spot market for ${signal.symbol}.
                DO NOT guess or invent numbers. Use the exact metrics provided below:
                
                Symbol: ${signal.symbol}
                Current Price: ${signal.price}
                Timeframe: ${signal.timeframe.displayName}
                Deterministic Signal: ${signal.signalType.label}
                Signal Score: ${signal.score}/10
                Market Regime: ${signal.marketRegime.label}
                EMA 20: ${snapshot.ema20 ?: "N/A"}
                EMA 50: ${snapshot.ema50 ?: "N/A"}
                EMA 200: ${snapshot.ema200 ?: "N/A"}
                RSI (14): ${snapshot.rsi14 ?: "N/A"}
                MACD Line: ${snapshot.macdLine ?: "N/A"}, Signal: ${snapshot.macdSignal ?: "N/A"}, Hist: ${snapshot.macdHistogram ?: "N/A"}
                ATR (14): ${snapshot.atr14 ?: "N/A"}
                Volume vs 20-MA: ${snapshot.volumeVsAvgPercent ?: 0.0}%
                Support Level: ${snapshot.supportLevel ?: "N/A"}
                Resistance Level: ${snapshot.resistanceLevel ?: "N/A"}
                Suggested Entry: ${signal.suggestedEntryMin} - ${signal.suggestedEntryMax}
                Stop Loss: ${signal.stopLoss}
                Target 1: ${signal.takeProfit1}
                Target 2: ${signal.takeProfit2}
                Risk/Reward: 1:${signal.riskRewardRatio}
                Identified Reasons: ${signal.reasons.joinToString("; ")}
                Identified Warnings: ${signal.warnings.joinToString("; ")}
                
                Provide a JSON response with the following format:
                {
                  "summary": "Brief 1-2 sentence executive overview",
                  "marketCondition": "Detailed market structure explanation",
                  "setupExplanation": "Why the technical indicators and volume create this setup",
                  "keyRisks": "Specific risks, nearby resistance, or volatility factors",
                  "invalidationCondition": "Exact conditions that invalidate the setup",
                  "recommendedAction": "${signal.signalType.name}"
                }
                Return ONLY valid JSON.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.w(TAG, "Gemini call failed (${response.code}): $errBody")
                    throw Exception("Gemini API error: ${response.code}")
                }

                val respStr = response.body?.string() ?: ""
                val root = JSONObject(respStr)
                val text = root.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val jsonClean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsed = JSONObject(jsonClean)

                val result = AiSetupAnalysis(
                    summary = parsed.optString("summary", "Analysis completed."),
                    marketCondition = parsed.optString("marketCondition", "${signal.marketRegime.label} market"),
                    setupExplanation = parsed.optString("setupExplanation", signal.reasons.joinToString(". ")),
                    keyRisks = parsed.optString("keyRisks", signal.warnings.joinToString(". ")),
                    invalidationCondition = parsed.optString("invalidationCondition", "Break below stop loss at $${signal.stopLoss}"),
                    recommendedAction = parsed.optString("recommendedAction", signal.signalType.name)
                )

                analysisCache[cacheKey] = Pair(System.currentTimeMillis(), result)
                Result.success(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze setup with Gemini", e)
            val fallback = AiSetupAnalysis(
                summary = "${signal.symbol}: ${signal.signalType.label} (Score: ${signal.score}/10)",
                marketCondition = "${signal.marketRegime.label} market conditions detected.",
                setupExplanation = signal.reasons.joinToString(". ") + ".",
                keyRisks = signal.warnings.joinToString(". ") + ".",
                invalidationCondition = "Price drops below stop loss at $${String.format("%.2f", signal.stopLoss)}.",
                recommendedAction = signal.signalType.name
            )
            Result.success(fallback)
        }
    }

    suspend fun generateMarketBriefing(
        signals: List<TradingSignal>
    ): Result<AiMarketBriefing> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedBriefing != null && (now - cachedBriefing!!.first) < 300_000L) {
            return@withContext Result.success(cachedBriefing!!.second)
        }

        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        val topSignals = signals.sortedByDescending { it.score }
        val strongestBullish = topSignals.firstOrNull { it.signalType.name.contains("BUY") }
        val strongestBearish = topSignals.firstOrNull { it.signalType.name.contains("SELL") }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val briefing = AiMarketBriefing(
                title = "Binance Spot Market Pulse",
                summary = "Monitored ${signals.size} spot pairs. Top opportunities currently led by ${strongestBullish?.symbol ?: "BTCUSDT"}.",
                strongestBullish = strongestBullish?.let { "${it.symbol} (${it.score}/10): ${it.reasons.firstOrNull() ?: "Favorable alignment"}" } ?: "None currently qualified",
                strongestBearish = strongestBearish?.let { "${it.symbol} (${it.score}/10): Bearish pressure / exit setup" } ?: "No heavy exit pressure identified",
                marketWideCondition = "Markets experiencing mixed consolidation with selective spot opportunities.",
                majorRisks = "Monitor BTC dominance and key resistance zones before opening paper trades.",
                pairsToWatch = topSignals.filter { it.score >= 6.5 }.map { it.symbol },
                pairsToAvoid = topSignals.filter { it.score < 4.0 }.map { it.symbol }
            )
            cachedBriefing = Pair(now, briefing)
            return@withContext Result.success(briefing)
        }

        try {
            val signalSummaryList = topSignals.take(8).map { s ->
                "${s.symbol}: ${s.signalType.label}, Score: ${s.score}/10, Regime: ${s.marketRegime.label}, Price: $${s.price}, Warnings: ${s.warnings.joinToString(",")}"
            }.joinToString("\n")

            val prompt = """
                You are TradePilot's Chief Market Strategist. Analyze this pre-calculated snapshot of Binance Spot pairs:
                $signalSummaryList
                
                Produce a structured JSON briefing:
                {
                  "title": "Short catchy market headline",
                  "summary": "2-3 sentences summarising current spot conditions",
                  "strongestBullish": "Name of best bullish setup and why",
                  "strongestBearish": "Name of weakest/bearish pair and why",
                  "marketWideCondition": "Overview of general crypto market regime",
                  "majorRisks": "Key risks to capital right now",
                  "pairsToWatch": ["SYMBOL1", "SYMBOL2"],
                  "pairsToAvoid": ["SYMBOL3", "SYMBOL4"]
                }
                Return ONLY valid JSON.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Gemini API error: ${response.code}")
                }
                val respStr = response.body?.string() ?: ""
                val root = JSONObject(respStr)
                val text = root.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val jsonClean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val parsed = JSONObject(jsonClean)

                val watchArr = parsed.optJSONArray("pairsToWatch") ?: JSONArray()
                val avoidArr = parsed.optJSONArray("pairsToAvoid") ?: JSONArray()
                val watchList = mutableListOf<String>()
                for (i in 0 until watchArr.length()) watchList.add(watchArr.getString(i))
                val avoidList = mutableListOf<String>()
                for (i in 0 until avoidArr.length()) avoidList.add(avoidArr.getString(i))

                val briefing = AiMarketBriefing(
                    title = parsed.optString("title", "Market Briefing"),
                    summary = parsed.optString("summary", ""),
                    strongestBullish = parsed.optString("strongestBullish", ""),
                    strongestBearish = parsed.optString("strongestBearish", ""),
                    marketWideCondition = parsed.optString("marketWideCondition", ""),
                    majorRisks = parsed.optString("majorRisks", ""),
                    pairsToWatch = watchList,
                    pairsToAvoid = avoidList
                )

                cachedBriefing = Pair(now, briefing)
                Result.success(briefing)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating briefing with Gemini", e)
            val briefing = AiMarketBriefing(
                title = "TradePilot Spot Market Briefing",
                summary = "Deterministic algorithms evaluated ${signals.size} pairs. Strongest setup: ${strongestBullish?.symbol ?: "N/A"}.",
                strongestBullish = strongestBullish?.let { "${it.symbol} (Score ${it.score}/10)" } ?: "None",
                strongestBearish = strongestBearish?.let { "${it.symbol} (Score ${it.score}/10)" } ?: "None",
                marketWideCondition = "Mixed market conditions. Follow disciplined position sizing.",
                majorRisks = "Watch for sudden volatility spikes and false breakouts.",
                pairsToWatch = topSignals.filter { it.score >= 6.5 }.map { it.symbol },
                pairsToAvoid = topSignals.filter { it.score < 4.0 }.map { it.symbol }
            )
            Result.success(briefing)
        }
    }
}
