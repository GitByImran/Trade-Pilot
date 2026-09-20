package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SignalEntity::class,
        PaperPositionEntity::class,
        PaperTradeEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TradePilotDatabase : RoomDatabase() {
    abstract fun signalDao(): SignalDao
    abstract fun paperTradingDao(): PaperTradingDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: TradePilotDatabase? = null

        fun getInstance(context: Context): TradePilotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TradePilotDatabase::class.java,
                    "tradepilot_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
