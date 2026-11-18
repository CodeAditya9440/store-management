package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "staff_members")
data class StaffMemberEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val roleName: String,
    val pinHash: String,
    val pinSalt: String,
    val encryptedContact: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val role: StaffRole get() = StaffRole.fromName(roleName)
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val barcode: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val lowStockThreshold: Int,
    val costPriceCents: Long,
    val retailPriceCents: Long,
    val supplierName: String,
    val encryptedWholesaleNotes: String = "",
    val lastAuditedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isOutOfStock: Boolean get() = quantity <= 0
    val isLowStock: Boolean get() = quantity <= lowStockThreshold
    val marginPercent: Double
        get() = if (retailPriceCents <= 0) 0.0
        else ((retailPriceCents - costPriceCents).toDouble() / retailPriceCents.toDouble()) * 100.0
}

@Entity(tableName = "store_tasks")
data class StoreTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val categoryName: String = TaskCategory.RESTOCK.name,
    val priorityName: String = TaskPriority.MEDIUM.name,
    val shiftName: String = WorkShift.MORNING.name,
    val statusName: String = TaskStatus.TODO.name,
    val assignedStaffId: String = "",
    val assignedStaffName: String = "Unassigned",
    val linkedBarcode: String? = null,
    val dueLabel: String = "Today",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val category: TaskCategory get() = TaskCategory.fromName(categoryName)
    val priority: TaskPriority get() = TaskPriority.fromName(priorityName)
    val shift: WorkShift get() = WorkShift.fromName(shiftName)
    val status: TaskStatus get() = TaskStatus.fromName(statusName)
}

@Entity(tableName = "stock_transactions")
data class StockTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val barcode: String,
    val typeName: String,
    val quantityDelta: Int,
    val unitCostCents: Long,
    val unitPriceCents: Long,
    val totalAmountCents: Long,
    val staffId: String,
    val staffName: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val type: StockTxType get() = StockTxType.fromName(typeName)
}

@Entity(tableName = "low_stock_alerts")
data class LowStockAlertEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val barcode: String,
    val currentQty: Int,
    val threshold: Int,
    val severityName: String,
    val statusName: String = AlertStatus.ACTIVE.name,
    val triggeredAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
) {
    val severity: AlertSeverity get() = AlertSeverity.fromName(severityName)
    val status: AlertStatus get() = AlertStatus.fromName(statusName)
}

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val action: String,
    val summary: String,
    val statusName: String = "OFFLINE_QUEUED",
    val createdAt: Long = System.currentTimeMillis()
)
