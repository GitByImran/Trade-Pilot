package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface BinanceApiService {

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100
    ): ResponseBody

    @GET("api/v3/ticker/24hr")
    suspend fun get24hrTicker(
        @Query("symbol") symbol: String
    ): ResponseBody

    @GET("api/v3/depth")
    suspend fun getOrderBookDepth(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 20
    ): ResponseBody

    @GET("api/v3/trades")
    suspend fun getRecentTrades(
        @Query("symbol") symbol: String,
        @Query("limit") limit: Int = 20
    ): ResponseBody

    @GET("api/v3/account")
    suspend fun getAccountInformation(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long = 60000L,
        @Query("signature") signature: String
    ): ResponseBody

    @GET("api/v3/openOrders")
    suspend fun getOpenOrders(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("recvWindow") recvWindow: Long = 60000L,
        @Query("signature") signature: String
    ): ResponseBody

    companion object {
        private const val BASE_URL = "https://api.binance.com/"

        fun create(): BinanceApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .build()
                .create(BinanceApiService::class.java)
        }
    }
}
