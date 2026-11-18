package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LowStockAlertEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StaffMemberEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.StoreTaskEntity
import com.example.data.model.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    // --- Staff ---
    @Query("SELECT * FROM staff_members ORDER BY createdAt ASC")
    fun observeStaff(): Flow<List<StaffMemberEntity>>

    @Query("SELECT * FROM staff_members")
    suspend fun getAllStaffOnce(): List<StaffMemberEntity>

    @Query("SELECT * FROM staff_members WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: String): StaffMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStaff(staff: StaffMemberEntity)

    @Query("DELETE FROM staff_members WHERE id = :id")
    suspend fun deleteStaffById(id: String)

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE LOWER(barcode) = LOWER(:barcode) LIMIT 1")
    fun observeProductByBarcode(barcode: String): Flow<ProductEntity?>

    @Query("SELECT * FROM stock_transactions WHERE LOWER(barcode) = LOWER(:barcode) ORDER BY timestamp DESC LIMIT 10")
    fun observeTransactionsForBarcode(barcode: String): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    // --- Tasks ---
    @Query("SELECT * FROM store_tasks ORDER BY CASE statusName WHEN 'DONE' THEN 1 ELSE 0 END, createdAt DESC")
    fun observeTasks(): Flow<List<StoreTaskEntity>>

    @Query("SELECT * FROM store_tasks")
    suspend fun getAllTasksOnce(): List<StoreTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: StoreTaskEntity)

    @Query("DELETE FROM store_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    // --- Transactions ---
    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    fun observeTransactions(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsOnce(): List<StockTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: StockTransactionEntity)

    // --- Low Stock Alerts ---
    @Query("SELECT * FROM low_stock_alerts ORDER BY CASE statusName WHEN 'ACTIVE' THEN 0 WHEN 'ACKNOWLEDGED' THEN 1 ELSE 2 END, triggeredAt DESC")
    fun observeAlerts(): Flow<List<LowStockAlertEntity>>

    @Query("SELECT * FROM low_stock_alerts WHERE productId = :productId AND statusName != 'RESOLVED' LIMIT 1")
    suspend fun getOpenAlertForProduct(productId: String): LowStockAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlert(alert: LowStockAlertEntity)

    @Update
    suspend fun updateAlert(alert: LowStockAlertEntity)

    // --- Sync Queue ---
    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC LIMIT 100")
    fun observeSyncQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue")
    suspend fun getAllSyncQueueOnce(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncEvent(event: SyncQueueEntity)

    @Query("UPDATE sync_queue SET statusName = 'LOCAL_COMMITTED' WHERE statusName = 'OFFLINE_QUEUED'")
    suspend fun markAllSyncedLocally()

    @Query("DELETE FROM sync_queue WHERE statusName = 'LOCAL_COMMITTED'")
    suspend fun pruneSyncedEvents()
}
