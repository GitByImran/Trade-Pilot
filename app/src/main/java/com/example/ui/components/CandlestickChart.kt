package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Candle
import com.example.domain.indicators.TechnicalIndicators
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.IndicatorBollinger
import com.example.ui.theme.IndicatorEma20
import com.example.ui.theme.IndicatorEma200
import com.example.ui.theme.IndicatorEma50
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    showEma20: Boolean = true,
    showEma50: Boolean = true,
    showEma200: Boolean = true,
    showBollingerBands: Boolean = true
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier.height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Loading Binance candles...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        return
    }

    // Display the last N visible candles (e.g., 40-50 for crisp mobile resolution)
    val displayCandles = remember(candles) { candles.takeLast(45) }
    val closePrices = remember(candles) { candles.map { it.close } }
    val volumes = remember(candles) { candles.map { it.volume } }

    val ema20List = remember(closePrices) { TechnicalIndicators.calculateEma(closePrices, 20).takeLast(displayCandles.size) }
    val ema50List = remember(closePrices) { TechnicalIndicators.calculateEma(closePrices, 50).takeLast(displayCandles.size) }
    val ema200List = remember(closePrices) { TechnicalIndicators.calculateEma(closePrices, 200).takeLast(displayCandles.size) }
    val bb = remember(closePrices) {
        val fullBb = TechnicalIndicators.calculateBollingerBands(closePrices, 20)
        Triple(
            fullBb.upper.takeLast(displayCandles.size),
            fullBb.middle.takeLast(displayCandles.size),
            fullBb.lower.takeLast(displayCandles.size)
        )
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        // Scrubber / Info header
        val activeCandle = selectedIndex?.let { displayCandles.getOrNull(it) } ?: displayCandles.lastOrNull()
        if (activeCandle != null) {
            val dateStr = remember(activeCandle.closeTime) {
                SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(activeCandle.closeTime))
            }
            val change = activeCandle.close - activeCandle.open
            val changePct = (change / activeCandle.open) * 100.0
            val isBullish = activeCandle.close >= activeCandle.open
            val priceColor = if (isBullish) BullishGreen else BearishRed

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateStr,
                            style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "O: ${String.format("%.2f", activeCandle.open)}",
                                style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = "H: ${String.format("%.2f", activeCandle.high)}",
                                style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = "L: ${String.format("%.2f", activeCandle.low)}",
                                style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = "C: ${String.format("%.2f", activeCandle.close)}",
                                style = TextStyle(fontSize = 11.sp, color = priceColor)
                            )
                        }
                    }

                    // Legend indicator dots
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${if (change >= 0) "+" else ""}${String.format("%.2f", changePct)}%",
                            style = TextStyle(fontSize = 11.sp, color = priceColor)
                        )
                        if (showEma20) {
                            Text("EMA20", style = TextStyle(fontSize = 10.sp, color = IndicatorEma20))
                        }
                        if (showEma50) {
                            Text("EMA50", style = TextStyle(fontSize = 10.sp, color = IndicatorEma50))
                        }
                        if (showEma200) {
                            Text("EMA200", style = TextStyle(fontSize = 10.sp, color = IndicatorEma200))
                        }
                        if (showBollingerBands) {
                            Text("BOLL", style = TextStyle(fontSize = 10.sp, color = IndicatorBollinger))
                        }
                    }
                }
            }
        }

        // Canvas for Candlesticks + Indicators + Volume
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .pointerInput(displayCandles) {
                    detectTapGestures(
                        onTap = { offset ->
                            val candleWidth = size.width / displayCandles.size
                            val idx = (offset.x / candleWidth).toInt().coerceIn(0, displayCandles.lastIndex)
                            selectedIndex = idx
                        }
                    )
                }
                .pointerInput(displayCandles) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            val candleWidth = size.width / displayCandles.size
                            val idx = (change.position.x / candleWidth).toInt().coerceIn(0, displayCandles.lastIndex)
                            selectedIndex = idx
                        },
                        onDragEnd = {
                            // keep selected or reset
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = displayCandles.size
                if (count == 0) return@Canvas

                val chartHeight = size.height * 0.75f // Top 75% for candles
                val volumeHeight = size.height * 0.22f // Bottom 22% for volume
                val volumeTop = size.height * 0.78f

                // Find global min and max price across visible candles and indicators
                var minPrice = displayCandles.minOf { it.low }
                var maxPrice = displayCandles.maxOf { it.high }

                bb.first.filterNotNull().forEach { maxPrice = max(maxPrice, it) }
                bb.third.filterNotNull().forEach { minPrice = min(minPrice, it) }

                val pricePadding = (maxPrice - minPrice) * 0.05
                val effectiveMin = max(0.0, minPrice - pricePadding)
                val effectiveMax = maxPrice + pricePadding
                val priceRange = max(0.000001, effectiveMax - effectiveMin)

                val maxVolume = displayCandles.maxOfOrNull { it.volume } ?: 1.0

                val candleSlotWidth = size.width / count
                val candleBodyWidth = max(2f, candleSlotWidth * 0.65f)

                // 1. Draw horizontal grid lines and price labels
                val gridSteps = 4
                for (step in 0..gridSteps) {
                    val y = chartHeight * (step.toFloat() / gridSteps)
                    val priceVal = effectiveMax - (step.toDouble() / gridSteps) * priceRange

                    drawLine(
                        color = BorderSubtle.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )

                    val textLayout = textMeasurer.measure(
                        text = String.format("%.2f", priceVal),
                        style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(size.width - textLayout.size.width - 4.dp.toPx(), y - textLayout.size.height)
                    )
                }

                // 2. Draw Bollinger Bands (fill and upper/lower lines)
                if (showBollingerBands) {
                    val upperPath = Path()
                    val lowerPath = Path()
                    var upperStarted = false
                    var lowerStarted = false

                    for (i in 0 until count) {
                        val centerX = (i + 0.5f) * candleSlotWidth
                        val up = bb.first.getOrNull(i)
                        val low = bb.third.getOrNull(i)

                        if (up != null) {
                            val upY = (chartHeight * (1.0 - (up - effectiveMin) / priceRange)).toFloat()
                            if (!upperStarted) {
                                upperPath.moveTo(centerX, upY)
                                upperStarted = true
                            } else {
                                upperPath.lineTo(centerX, upY)
                            }
                        }

                        if (low != null) {
                            val lowY = (chartHeight * (1.0 - (low - effectiveMin) / priceRange)).toFloat()
                            if (!lowerStarted) {
                                lowerPath.moveTo(centerX, lowY)
                                lowerStarted = true
                            } else {
                                lowerPath.lineTo(centerX, lowY)
                            }
                        }
                    }

                    if (upperStarted) {
                        drawPath(
                            path = upperPath,
                            color = IndicatorBollinger.copy(alpha = 0.8f),
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                        )
                    }
                    if (lowerStarted) {
                        drawPath(
                            path = lowerPath,
                            color = IndicatorBollinger.copy(alpha = 0.8f),
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                        )
                    }
                }

                // 3. Draw EMA Lines
                fun drawIndicatorLine(values: List<Double?>, color: Color, strokeWidth: Float = 2f) {
                    val path = Path()
                    var started = false
                    for (i in 0 until count) {
                        val v = values.getOrNull(i) ?: continue
                        val x = (i + 0.5f) * candleSlotWidth
                        val y = (chartHeight * (1.0 - (v - effectiveMin) / priceRange)).toFloat()
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    if (started) {
                        drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
                    }
                }

                if (showEma20) drawIndicatorLine(ema20List, IndicatorEma20, 2f)
                if (showEma50) drawIndicatorLine(ema50List, IndicatorEma50, 2f)
                if (showEma200) drawIndicatorLine(ema200List, IndicatorEma200, 2.5f)

                // 4. Draw Candlesticks & Volume Bars
                for (i in 0 until count) {
                    val c = displayCandles[i]
                    val isBullish = c.close >= c.open
                    val color = if (isBullish) BullishGreen else BearishRed

                    val centerX = (i + 0.5f) * candleSlotWidth
                    val highY = (chartHeight * (1.0 - (c.high - effectiveMin) / priceRange)).toFloat()
                    val lowY = (chartHeight * (1.0 - (c.low - effectiveMin) / priceRange)).toFloat()
                    val openY = (chartHeight * (1.0 - (c.open - effectiveMin) / priceRange)).toFloat()
                    val closeY = (chartHeight * (1.0 - (c.close - effectiveMin) / priceRange)).toFloat()

                    val bodyTop = min(openY, closeY)
                    val bodyHeight = max(2f, kotlin.math.abs(openY - closeY))

                    // Wick
                    drawLine(
                        color = color,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = 1.2f
                    )

                    // Candle Body
                    drawRect(
                        color = color,
                        topLeft = Offset(centerX - candleBodyWidth / 2f, bodyTop),
                        size = Size(candleBodyWidth, bodyHeight)
                    )

                    // Volume Bar in bottom panel
                    val volBarHeight = if (maxVolume > 0) ((c.volume / maxVolume) * volumeHeight).toFloat() else 0f
                    drawRect(
                        color = color.copy(alpha = 0.45f),
                        topLeft = Offset(centerX - candleBodyWidth / 2f, size.height - volBarHeight),
                        size = Size(candleBodyWidth, volBarHeight)
                    )
                }

                // 5. Draw Crosshair if an index is selected by touch/drag
                selectedIndex?.let { idx ->
                    if (idx in 0 until count) {
                        val c = displayCandles[idx]
                        val x = (idx + 0.5f) * candleSlotWidth
                        val y = (chartHeight * (1.0 - (c.close - effectiveMin) / priceRange)).toFloat()

                        // Vertical Crosshair
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )

                        // Horizontal Crosshair
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )

                        // Crosshair dot on candle close
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
