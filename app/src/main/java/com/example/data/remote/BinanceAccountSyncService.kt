package com.example.data.remote

import android.util.Log
import com.example.data.model.BinanceAccountAsset
import com.example.data.model.BinanceConnectedAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object BinanceAccountSyncService {

    suspend fun syncAccount(
        apiKey: String,
        apiSecret: String,
        apiService: BinanceApiService
    ): Result<BinanceConnectedAccount> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Binance API Key and Secret cannot be empty"))
        }

        try {
            val timestamp = System.currentTimeMillis()
            val recvWindow = 60000L
            val queryString = "recvWindow=$recvWindow&timestamp=$timestamp"
            val signature = hmacSha256(queryString, apiSecret.trim())

            val response = apiService.getAccountInformation(
                apiKey = apiKey.trim(),
                timestamp = timestamp,
                recvWindow = recvWindow,
                signature = signature
            )

            val body = response.string()
            val json = JSONObject(body)

            if (json.has("code") && json.getInt("code") != 200 && json.getInt("code") < 0) {
                val msg = json.optString("msg", "Binance API rejected request")
                return@withContext Result.failure(Exception("Binance error: $msg"))
            }

            val canTrade = json.optBoolean("canTrade", false)
            val balances = json.optJSONArray("balances")

            val assetList = mutableListOf<BinanceAccountAsset>()
            var freeUsdt = 0.0
            var totalEstUsdt = 0.0

            if (balances != null) {
                for (i in 0 until balances.length()) {
                    val item = balances.getJSONObject(i)
                    val asset = item.optString("asset")
                    val free = item.optString("free", "0.0").toDoubleOrNull() ?: 0.0
                    val locked = item.optString("locked", "0.0").toDoubleOrNull() ?: 0.0

                    if (free > 0.0 || locked > 0.0) {
                        if (asset.equals("USDT", ignoreCase = true)) {
                            freeUsdt = free
                            totalEstUsdt += (free + locked)
                        }
                        assetList.add(
                            BinanceAccountAsset(
                                asset = asset,
                                free = free,
                                locked = locked,
                                estimatedUsdtValue = if (asset == "USDT") free + locked else 0.0
                            )
                        )
                    }
                }
            }

            // Sort assets with highest free balance first, keeping USDT on top
            val sortedAssets = assetList.sortedWith(
                compareByDescending<BinanceAccountAsset> { it.asset.equals("USDT", ignoreCase = true) }
                    .thenByDescending { it.free + it.locked }
            )

            val account = BinanceConnectedAccount(
                isConnected = true,
                canTrade = canTrade,
                totalEstimatedUsdt = if (totalEstUsdt > 0) totalEstUsdt else freeUsdt,
                freeUsdt = freeUsdt,
                assets = sortedAssets,
                lastSyncTimestamp = System.currentTimeMillis(),
                errorMessage = null
            )

            Result.success(account)
        } catch (e: Exception) {
            Log.e("BinanceSync", "Failed to sync Binance account: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun hmacSha256(data: String, secret: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        sha256Hmac.init(secretKey)
        val hash = sha256Hmac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        return bytesToHex(hash)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val hexArray = "0123456789abcdef".toCharArray()
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
