package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.LowStockAlertEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StaffMemberEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.StoreTaskEntity
import com.example.data.model.SyncQueueEntity

@Database(
    entities = [
        StaffMemberEntity::class,
        ProductEntity::class,
        StoreTaskEntity::class,
        StockTransactionEntity::class,
        LowStockAlertEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StockPulseDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: StockPulseDatabase? = null

        fun getInstance(context: Context): StockPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockPulseDatabase::class.java,
                    "stockpulse_vault.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
