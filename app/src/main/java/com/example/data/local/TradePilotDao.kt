package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {
    @Query("SELECT * FROM signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<SignalEntity>>

    @Query("SELECT * FROM signals WHERE symbol = :symbol ORDER BY timestamp DESC")
    fun getSignalsForSymbol(symbol: String): Flow<List<SignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: SignalEntity)

    @Update
    suspend fun updateSignal(signal: SignalEntity)

    @Query("UPDATE signals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM signals")
    suspend fun clearAllSignals()
}

@Dao
interface PaperTradingDao {
    @Query("SELECT * FROM paper_positions WHERE status = 'OPEN' ORDER BY openedAt DESC")
    fun getOpenPositions(): Flow<List<PaperPositionEntity>>

    @Query("SELECT * FROM paper_positions ORDER BY openedAt DESC")
    fun getAllPositions(): Flow<List<PaperPositionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PaperPositionEntity)

    @Update
    suspend fun updatePosition(position: PaperPositionEntity)

    @Query("SELECT * FROM paper_trades ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<PaperTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: PaperTradeEntity)

    @Query("DELETE FROM paper_positions")
    suspend fun clearPositions()

    @Query("DELETE FROM paper_trades")
    suspend fun clearTrades()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)
}
