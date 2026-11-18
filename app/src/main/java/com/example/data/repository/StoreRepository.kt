package com.example.data.repository

import com.example.data.local.StoreDao
import com.example.data.model.AlertSeverity
import com.example.data.model.AlertStatus
import com.example.data.model.LowStockAlertEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StaffMemberEntity
import com.example.data.model.StaffRole
import com.example.data.model.StockTransactionEntity
import com.example.data.model.StockTxType
import com.example.data.model.StoreTaskEntity
import com.example.data.model.SyncQueueEntity
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.WorkShift
import com.example.data.preferences.StoreConfig
import com.example.data.preferences.StorePreferences
import com.example.notifications.LowStockNotifier
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class StoreRepository(
    private val dao: StoreDao,
    private val preferences: StorePreferences,
    private val notifier: LowStockNotifier
) {
    val storeConfig: Flow<StoreConfig> = preferences.configFlow
    val staffStream: Flow<List<StaffMemberEntity>> = dao.observeStaff()
    val productsStream: Flow<List<ProductEntity>> = dao.observeProducts()
    val tasksStream: Flow<List<StoreTaskEntity>> = dao.observeTasks()
    val transactionsStream: Flow<List<StockTransactionEntity>> = dao.observeTransactions()
    val alertsStream: Flow<List<LowStockAlertEntity>> = dao.observeAlerts()
    val syncQueueStream: Flow<List<SyncQueueEntity>> = dao.observeSyncQueue()

    suspend fun initializeStoreVault(
        storeName: String,
        currencySymbol: String,
        taxId: String,
        ownerName: String,
        ownerPin: String,
        ownerContact: String,
        seedSampleData: Boolean
    ): Result<StaffMemberEntity> = runCatching {
        require(storeName.isNotBlank()) { "Store name is required" }
        require(ownerName.isNotBlank()) { "Owner name is required" }
        require(ownerPin.length >= 4) { "PIN must be at least 4 digits" }

        val owner = createStaffRecord(
            name = ownerName.trim(),
            role = StaffRole.OWNER,
            pin = ownerPin,
            contact = ownerContact.trim()
        )
        dao.upsertStaff(owner)
        preferences.saveStoreSetup(
            storeName = storeName,
            currencySymbol = currencySymbol,
            plainTaxId = taxId,
            activeStaffId = owner.id
        )
        enqueueSync("STAFF", owner.id, "UPSERT", "Vault initialized by Owner ${owner.name}")

        if (seedSampleData) {
            seedStarterRetailData(owner)
        }
        owner
    }

    private suspend fun seedStarterRetailData(owner: StaffMemberEntity) {
        val manager = createStaffRecord("Maya Lin", StaffRole.MANAGER, "2222", "maya@localstore.pos")
        val cashier = createStaffRecord("Liam Patel", StaffRole.CASHIER, "3333", "liam@localstore.pos")
        dao.upsertStaff(manager)
        dao.upsertStaff(cashier)

        val now = System.currentTimeMillis()
        val products = listOf(
            ProductEntity(
                barcode = "8901491101837",
                name = "Artisan Cold Brew 330ml",
                category = "Beverages",
                quantity = 28,
                lowStockThreshold = 10,
                costPriceCents = 11000L,
                retailPriceCents = 18000L,
                supplierName = "Nordic Roasters Co.",
                encryptedWholesaleNotes = CryptoManager.encrypt("Net-30 invoice; 12% bulk rebate over 5 cases"),
                lastAuditedAt = now - 86_400_000L
            ),
            ProductEntity(
                barcode = "4902430881234",
                name = "Organic Matcha Oat Latte",
                category = "Beverages",
                quantity = 4,
                lowStockThreshold = 8,
                costPriceCents = 14000L,
                retailPriceCents = 24000L,
                supplierName = "Kyoto Leaf Imports",
                encryptedWholesaleNotes = CryptoManager.encrypt("Chilled delivery Tuesday/Friday morning"),
                lastAuditedAt = now - 43_200_000L
            ),
            ProductEntity(
                barcode = "0041331021456",
                name = "Sourdough Sea Salt Crackers",
                category = "Pantry",
                quantity = 0,
                lowStockThreshold = 6,
                costPriceCents = 12000L,
                retailPriceCents = 19500L,
                supplierName = "Hearth & Grain Bakery",
                encryptedWholesaleNotes = CryptoManager.encrypt("Min order 12 packs; shelf life 90 days"),
                lastAuditedAt = now - 172_800_000L
            ),
            ProductEntity(
                barcode = "0078912345612",
                name = "Ceramic Pour-Over Dripper",
                category = "Supplies",
                quantity = 14,
                lowStockThreshold = 5,
                costPriceCents = 75000L,
                retailPriceCents = 129900L,
                supplierName = "Studio Kiln Supply",
                encryptedWholesaleNotes = CryptoManager.encrypt("Fragile packaging required; margin protected"),
                lastAuditedAt = now - 259_200_000L
            ),
            ProductEntity(
                barcode = "5060123450098",
                name = "Single-Origin Espresso Beans 500g",
                category = "Pantry",
                quantity = 6,
                lowStockThreshold = 6,
                costPriceCents = 42000L,
                retailPriceCents = 69900L,
                supplierName = "Nordic Roasters Co.",
                encryptedWholesaleNotes = CryptoManager.encrypt("Roast batch #412 verified"),
                lastAuditedAt = now - 21_600_000L
            )
        )

        products.forEach { product ->
            dao.upsertProduct(product)
            evaluateLowStockAlert(product, notifyUser = false, autoCreateTask = false)
        }

        val tasks = listOf(
            StoreTaskEntity(
                title = "Morning POS Cash Drawer & Safe Audit",
                description = "Verify opening float (₹5,000.00) and inspect encrypted safe seal.",
                categoryName = TaskCategory.OPENING.name,
                priorityName = TaskPriority.HIGH.name,
                shiftName = WorkShift.MORNING.name,
                statusName = TaskStatus.DONE.name,
                assignedStaffId = owner.id,
                assignedStaffName = owner.name,
                dueLabel = "08:30 AM",
                completedAt = now - 7_200_000L
            ),
            StoreTaskEntity(
                title = "Restock Organic Matcha Oat Latte Cooler",
                description = "Only 4 units left on front display. Pull backup case or submit PO.",
                categoryName = TaskCategory.RESTOCK.name,
                priorityName = TaskPriority.HIGH.name,
                shiftName = WorkShift.MORNING.name,
                statusName = TaskStatus.TODO.name,
                assignedStaffId = manager.id,
                assignedStaffName = manager.name,
                linkedBarcode = "4902430881234",
                dueLabel = "11:00 AM"
            ),
            StoreTaskEntity(
                title = "Cycle Count Pantry Shelving Aisle 2",
                description = "Scan barcodes on sourdough crackers & espresso bags for EOD reconciliation.",
                categoryName = TaskCategory.AUDIT.name,
                priorityName = TaskPriority.MEDIUM.name,
                shiftName = WorkShift.MIDDAY.name,
                statusName = TaskStatus.IN_PROGRESS.name,
                assignedStaffId = cashier.id,
                assignedStaffName = cashier.name,
                linkedBarcode = "0041331021456",
                dueLabel = "02:30 PM"
            ),
            StoreTaskEntity(
                title = "Export Daily Financial Audit PDF",
                description = "Review gross margin and archive signed PDF report before closing.",
                categoryName = TaskCategory.CLOSING.name,
                priorityName = TaskPriority.MEDIUM.name,
                shiftName = WorkShift.EVENING.name,
                statusName = TaskStatus.TODO.name,
                assignedStaffId = owner.id,
                assignedStaffName = owner.name,
                dueLabel = "08:00 PM"
            )
        )
        tasks.forEach { dao.upsertTask(it) }

        val txs = listOf(
            StockTransactionEntity(
                productId = products[0].id,
                productName = products[0].name,
                barcode = products[0].barcode,
                typeName = StockTxType.SALE.name,
                quantityDelta = -6,
                unitCostCents = products[0].costPriceCents,
                unitPriceCents = products[0].retailPriceCents,
                totalAmountCents = 6 * products[0].retailPriceCents,
                staffId = cashier.id,
                staffName = cashier.name,
                note = "Morning rush POS checkout",
                timestamp = now - 7_200_000L
            ),
            StockTransactionEntity(
                productId = products[3].id,
                productName = products[3].name,
                barcode = products[3].barcode,
                typeName = StockTxType.SALE.name,
                quantityDelta = -2,
                unitCostCents = products[3].costPriceCents,
                unitPriceCents = products[3].retailPriceCents,
                totalAmountCents = 2 * products[3].retailPriceCents,
                staffId = manager.id,
                staffName = manager.name,
                note = "Retail hardware sale",
                timestamp = now - 5_400_000L
            ),
            StockTransactionEntity(
                productId = products[4].id,
                productName = products[4].name,
                barcode = products[4].barcode,
                typeName = StockTxType.RESTOCK.name,
                quantityDelta = 12,
                unitCostCents = products[4].costPriceCents,
                unitPriceCents = products[4].retailPriceCents,
                totalAmountCents = 12 * products[4].costPriceCents,
                staffId = manager.id,
                staffName = manager.name,
                note = "Supplier invoice #NR-882",
                timestamp = now - 86_400_000L
            ),
            StockTransactionEntity(
                productId = products[4].id,
                productName = products[4].name,
                barcode = products[4].barcode,
                typeName = StockTxType.SALE.name,
                quantityDelta = -5,
                unitCostCents = products[4].costPriceCents,
                unitPriceCents = products[4].retailPriceCents,
                totalAmountCents = 5 * products[4].retailPriceCents,
                staffId = owner.id,
                staffName = owner.name,
                note = "Espresso bean counter sales",
                timestamp = now - 90_000_000L
            ),
            StockTransactionEntity(
                productId = products[1].id,
                productName = products[1].name,
                barcode = products[1].barcode,
                typeName = StockTxType.SALE.name,
                quantityDelta = -5,
                unitCostCents = products[1].costPriceCents,
                unitPriceCents = products[1].retailPriceCents,
                totalAmountCents = 5 * products[1].retailPriceCents,
                staffId = cashier.id,
                staffName = cashier.name,
                note = "Afternoon beverage sales",
                timestamp = now - 172_800_000L
            ),
            StockTransactionEntity(
                productId = products[0].id,
                productName = products[0].name,
                barcode = products[0].barcode,
                typeName = StockTxType.RESTOCK.name,
                quantityDelta = 24,
                unitCostCents = products[0].costPriceCents,
                unitPriceCents = products[0].retailPriceCents,
                totalAmountCents = 24 * products[0].costPriceCents,
                staffId = manager.id,
                staffName = manager.name,
                note = "Weekly cold brew stock delivery",
                timestamp = now - 259_200_000L
            ),
            StockTransactionEntity(
                productId = products[2].id,
                productName = products[2].name,
                barcode = products[2].barcode,
                typeName = StockTxType.WASTE.name,
                quantityDelta = -4,
                unitCostCents = products[2].costPriceCents,
                unitPriceCents = products[2].retailPriceCents,
                totalAmountCents = 4 * products[2].costPriceCents,
                staffId = manager.id,
                staffName = manager.name,
                note = "Damaged / expired shelf batch write-off",
                timestamp = now - 345_600_000L
            ),
            StockTransactionEntity(
                productId = products[2].id,
                productName = products[2].name,
                barcode = products[2].barcode,
                typeName = StockTxType.SALE.name,
                quantityDelta = -1,
                unitCostCents = products[2].costPriceCents,
                unitPriceCents = products[2].retailPriceCents,
                totalAmountCents = 1 * products[2].retailPriceCents,
                staffId = cashier.id,
                staffName = cashier.name,
                note = "Counter snack sale",
                timestamp = now - 349_200_000L
            ),
            StockTransactionEntity(
                productId = products[3].id,
                productName = products[3].name,
                barcode = products[3].barcode,
                typeName = StockTxType.RESTOCK.name,
                quantityDelta = 8,
                unitCostCents = products[3].costPriceCents,
                unitPriceCents = products[3].retailPriceCents,
                totalAmountCents = 8 * products[3].costPriceCents,
                staffId = owner.id,
                staffName = owner.name,
                note = "Ceramic dripper restock",
                timestamp = now - 432_000_000L
            )
        )
        txs.forEach { dao.insertTransaction(it) }
    }

    suspend fun authenticateStaff(staffId: String, pin: String): Result<StaffMemberEntity> = runCatching {
        val staff = dao.getStaffById(staffId) ?: error("Staff account not found")
        require(staff.isActive) { "This staff account is deactivated" }
        val ok = CryptoManager.verifyPin(pin, staff.pinSalt, staff.pinHash)
        require(ok) { "Incorrect PIN for ${staff.name}" }
        preferences.setActiveStaffId(staff.id)
        staff
    }

    suspend fun lockSession() {
        preferences.setActiveStaffId("")
    }

    suspend fun addStaffMember(
        actor: StaffMemberEntity,
        name: String,
        role: StaffRole,
        pin: String,
        contact: String
    ): Result<Unit> = runCatching {
        require(actor.role.canManageStaff()) { "Only Store Owners can add staff members" }
        require(name.isNotBlank()) { "Staff name is required" }
        require(pin.length in 4..8) { "PIN must be 4 to 8 digits" }
        val newStaff = createStaffRecord(name.trim(), role, pin, contact.trim())
        dao.upsertStaff(newStaff)
        enqueueSync("STAFF", newStaff.id, "UPSERT", "Added ${newStaff.name} (${role.displayName})")
    }

    suspend fun updateStaffRole(actor: StaffMemberEntity, targetId: String, newRole: StaffRole): Result<Unit> = runCatching {
        require(actor.role.canManageStaff()) { "Only Store Owners can update staff roles" }
        val target = dao.getStaffById(targetId) ?: error("Staff member not found")
        require(target.id != actor.id) { "Cannot demote the active Owner session" }
        dao.upsertStaff(target.copy(roleName = newRole.name))
        enqueueSync("STAFF", target.id, "UPSERT", "Role updated: ${target.name} -> ${newRole.displayName}")
    }

    suspend fun toggleStaffActive(actor: StaffMemberEntity, targetId: String): Result<Unit> = runCatching {
        require(actor.role.canManageStaff()) { "Only Store Owners can activate or deactivate staff" }
        val target = dao.getStaffById(targetId) ?: error("Staff member not found")
        require(target.id != actor.id) { "Cannot deactivate your own active session" }
        dao.upsertStaff(target.copy(isActive = !target.isActive))
        enqueueSync("STAFF", target.id, "UPSERT", "Toggled status for ${target.name}")
    }

    private fun createStaffRecord(
        name: String,
        role: StaffRole,
        pin: String,
        contact: String
    ): StaffMemberEntity {
        val salt = CryptoManager.generateSalt()
        val hash = CryptoManager.hashPin(pin, salt)
        return StaffMemberEntity(
            name = name,
            roleName = role.name,
            pinHash = hash,
            pinSalt = salt,
            encryptedContact = CryptoManager.encrypt(contact)
        )
    }

    suspend fun saveProduct(
        actor: StaffMemberEntity,
        existingId: String?,
        barcode: String,
        name: String,
        category: String,
        quantity: Int,
        lowStockThreshold: Int,
        costPriceCents: Long,
        retailPriceCents: Long,
        supplierName: String,
        plainWholesaleNotes: String
    ): Result<ProductEntity> = runCatching {
        require(actor.role.canEditInventoryPricing() || existingId == null) {
            "Insufficient permissions to edit product pricing"
        }
        require(name.isNotBlank()) { "Product name is required" }
        require(barcode.isNotBlank()) { "Barcode or SKU is required" }
        require(quantity >= 0) { "Quantity cannot be negative" }
        require(lowStockThreshold >= 0) { "Threshold cannot be negative" }

        val now = System.currentTimeMillis()
        val encryptedNotes = CryptoManager.encrypt(plainWholesaleNotes.trim())
        val product = if (existingId != null) {
            val prev = dao.getProductById(existingId) ?: error("Product not found")
            prev.copy(
                barcode = barcode.trim(),
                name = name.trim(),
                category = category.trim().ifBlank { "General" },
                quantity = quantity,
                lowStockThreshold = lowStockThreshold,
                costPriceCents = costPriceCents,
                retailPriceCents = retailPriceCents,
                supplierName = supplierName.trim(),
                encryptedWholesaleNotes = encryptedNotes,
                updatedAt = now
            )
        } else {
            ProductEntity(
                barcode = barcode.trim(),
                name = name.trim(),
                category = category.trim().ifBlank { "General" },
                quantity = quantity,
                lowStockThreshold = lowStockThreshold,
                costPriceCents = costPriceCents,
                retailPriceCents = retailPriceCents,
                supplierName = supplierName.trim(),
                encryptedWholesaleNotes = encryptedNotes,
                lastAuditedAt = now,
                updatedAt = now
            )
        }

        dao.upsertProduct(product)
        val config = storeConfig.first()
        evaluateLowStockAlert(product, config.lowStockNotificationsEnabled, config.autoCreateRestockTask)
        enqueueSync("PRODUCT", product.id, "UPSERT", "Saved SKU ${product.barcode} (${product.name})")
        product
    }

    suspend fun deleteProduct(actor: StaffMemberEntity, productId: String): Result<Unit> = runCatching {
        require(actor.role.canDeleteInventory()) { "Only Store Owners can delete SKUs" }
        dao.deleteProductById(productId)
        enqueueSync("PRODUCT", productId, "DELETE", "Deleted product $productId")
    }

    suspend fun adjustStock(
        actor: StaffMemberEntity,
        productId: String,
        txType: StockTxType,
        units: Int,
        note: String
    ): Result<ProductEntity> = runCatching {
        require(units > 0) { "Units must be greater than zero" }
        if (txType == StockTxType.RESTOCK || txType == StockTxType.AUDIT_ADJUSTMENT) {
            require(actor.role.canEditInventoryPricing() || txType == StockTxType.RESTOCK) {
                "Insufficient role permissions"
            }
        }
        val product = dao.getProductById(productId) ?: error("Product not found")
        val delta = when (txType) {
            StockTxType.SALE, StockTxType.WASTE -> -units
            StockTxType.RESTOCK -> units
            StockTxType.AUDIT_ADJUSTMENT -> units - product.quantity
        }
        val newQty = (product.quantity + delta).coerceAtLeast(0)
        val actualDelta = newQty - product.quantity
        val now = System.currentTimeMillis()
        val updated = product.copy(
            quantity = newQty,
            lastAuditedAt = if (txType == StockTxType.AUDIT_ADJUSTMENT) now else product.lastAuditedAt,
            updatedAt = now
        )
        dao.upsertProduct(updated)

        val unitAmount = when (txType) {
            StockTxType.SALE -> product.retailPriceCents
            StockTxType.RESTOCK, StockTxType.WASTE, StockTxType.AUDIT_ADJUSTMENT -> product.costPriceCents
        }
        val totalCents = kotlin.math.abs(actualDelta).toLong() * unitAmount

        val tx = StockTransactionEntity(
            productId = product.id,
            productName = product.name,
            barcode = product.barcode,
            typeName = txType.name,
            quantityDelta = actualDelta,
            unitCostCents = product.costPriceCents,
            unitPriceCents = product.retailPriceCents,
            totalAmountCents = totalCents,
            staffId = actor.id,
            staffName = actor.name,
            note = note.ifBlank { "${txType.label} by ${actor.name}" },
            timestamp = now
        )
        dao.insertTransaction(tx)

        val config = storeConfig.first()
        evaluateLowStockAlert(updated, config.lowStockNotificationsEnabled, config.autoCreateRestockTask)
        enqueueSync("TRANSACTION", tx.id, "UPSERT", "${txType.label}: ${product.name} ($actualDelta)")
        updated
    }

    private suspend fun evaluateLowStockAlert(
        product: ProductEntity,
        notifyUser: Boolean,
        autoCreateTask: Boolean
    ) {
        val openAlert = dao.getOpenAlertForProduct(product.id)
        if (product.isLowStock) {
            val severity = when {
                product.quantity <= 0 -> AlertSeverity.OUT_OF_STOCK
                product.quantity <= (product.lowStockThreshold / 2).coerceAtLeast(1) -> AlertSeverity.CRITICAL
                else -> AlertSeverity.LOW
            }
            val alert = openAlert?.copy(
                currentQty = product.quantity,
                threshold = product.lowStockThreshold,
                severityName = severity.name
            ) ?: LowStockAlertEntity(
                productId = product.id,
                productName = product.name,
                barcode = product.barcode,
                currentQty = product.quantity,
                threshold = product.lowStockThreshold,
                severityName = severity.name
            )
            dao.upsertAlert(alert)
            if (notifyUser) {
                notifier.notifyLowStock(product.id, product.name, product.quantity, product.lowStockThreshold, severity)
            }
            if (autoCreateTask && openAlert == null) {
                val existingTasks = dao.getAllTasksOnce()
                val alreadyHasRestockTask = existingTasks.any {
                    it.linkedBarcode == product.barcode && it.status != TaskStatus.DONE
                }
                if (!alreadyHasRestockTask) {
                    dao.upsertTask(
                        StoreTaskEntity(
                            title = "Reorder ${product.name}",
                            description = "Automated alert: ${product.quantity} units left (threshold ${product.lowStockThreshold}). Supplier: ${product.supplierName.ifBlank { "Unspecified" }}.",
                            categoryName = TaskCategory.RESTOCK.name,
                            priorityName = if (severity == AlertSeverity.OUT_OF_STOCK) TaskPriority.HIGH.name else TaskPriority.MEDIUM.name,
                            shiftName = WorkShift.MORNING.name,
                            statusName = TaskStatus.TODO.name,
                            assignedStaffName = "Manager / Owner",
                            linkedBarcode = product.barcode,
                            dueLabel = "Urgent"
                        )
                    )
                }
            }
        } else if (openAlert != null) {
            dao.updateAlert(
                openAlert.copy(
                    currentQty = product.quantity,
                    statusName = AlertStatus.RESOLVED.name,
                    resolvedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun acknowledgeAlert(alertId: String) {
        val alerts = dao.observeAlerts().first()
        val alert = alerts.firstOrNull { it.id == alertId } ?: return
        if (alert.status == AlertStatus.ACTIVE) {
            dao.updateAlert(alert.copy(statusName = AlertStatus.ACKNOWLEDGED.name))
        }
    }

    suspend fun saveTask(
        actor: StaffMemberEntity,
        existingId: String?,
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        shift: WorkShift,
        assignee: StaffMemberEntity?,
        linkedBarcode: String?,
        dueLabel: String
    ): Result<Unit> = runCatching {
        require(title.isNotBlank()) { "Task title is required" }
        val resolvedAssignee = if (actor.role.canAssignTasks()) assignee else actor
        val task = if (existingId != null) {
            val prev = dao.getAllTasksOnce().firstOrNull { it.id == existingId } ?: error("Task not found")
            prev.copy(
                title = title.trim(),
                description = description.trim(),
                categoryName = category.name,
                priorityName = priority.name,
                shiftName = shift.name,
                assignedStaffId = resolvedAssignee?.id ?: prev.assignedStaffId,
                assignedStaffName = resolvedAssignee?.name ?: prev.assignedStaffName,
                linkedBarcode = linkedBarcode?.trim()?.takeIf { it.isNotBlank() },
                dueLabel = dueLabel.trim().ifBlank { "Today" }
            )
        } else {
            StoreTaskEntity(
                title = title.trim(),
                description = description.trim(),
                categoryName = category.name,
                priorityName = priority.name,
                shiftName = shift.name,
                assignedStaffId = resolvedAssignee?.id ?: "",
                assignedStaffName = resolvedAssignee?.name ?: "Unassigned",
                linkedBarcode = linkedBarcode?.trim()?.takeIf { it.isNotBlank() },
                dueLabel = dueLabel.trim().ifBlank { "Today" }
            )
        }
        dao.upsertTask(task)
        enqueueSync("TASK", task.id, "UPSERT", "Task saved: ${task.title}")
    }

    suspend fun cycleTaskStatus(task: StoreTaskEntity): Result<Unit> = runCatching {
        val next = when (task.status) {
            TaskStatus.TODO -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.DONE
            TaskStatus.DONE -> TaskStatus.TODO
        }
        dao.upsertTask(
            task.copy(
                statusName = next.name,
                completedAt = if (next == TaskStatus.DONE) System.currentTimeMillis() else null
            )
        )
        enqueueSync("TASK", task.id, "UPSERT", "Task ${task.title} -> ${next.label}")
    }

    suspend fun deleteTask(actor: StaffMemberEntity, taskId: String): Result<Unit> = runCatching {
        require(actor.role.canAssignTasks()) { "Only Owners and Managers can delete tasks" }
        dao.deleteTaskById(taskId)
        enqueueSync("TASK", taskId, "DELETE", "Deleted task $taskId")
    }

    suspend fun findProductByBarcode(barcode: String): ProductEntity? =
        dao.getProductByBarcode(barcode.trim())

    fun observeScannedProduct(barcode: String): Flow<ProductEntity?> =
        dao.observeProductByBarcode(barcode.trim())

    fun observeScannedProductTransactions(barcode: String): Flow<List<StockTransactionEntity>> =
        dao.observeTransactionsForBarcode(barcode.trim())

    suspend fun updateAutomation(autoRestock: Boolean, notifications: Boolean) {
        preferences.updateAutomationSettings(autoRestock, notifications)
    }

    suspend fun updateTaxId(actor: StaffMemberEntity, plainTaxId: String): Result<Unit> = runCatching {
        require(actor.role == StaffRole.OWNER) { "Only the Store Owner can modify Tax ID records" }
        preferences.updateTaxId(plainTaxId)
    }

    suspend fun updateCurrency(actor: StaffMemberEntity, symbol: String): Result<Unit> = runCatching {
        require(actor.role == StaffRole.OWNER || actor.role == StaffRole.MANAGER) {
            "Only Owners or Managers can change store currency"
        }
        preferences.updateCurrencySymbol(symbol)
    }

    suspend fun commitOfflineQueueLocally(): Int {
        val pending = dao.getAllSyncQueueOnce().count { it.statusName == "OFFLINE_QUEUED" }
        dao.markAllSyncedLocally()
        preferences.updateLastBackupTimestamp(System.currentTimeMillis())
        return pending
    }

    suspend fun exportEncryptedBackupJson(): String {
        val config = storeConfig.first()
        val products = dao.getAllProductsOnce()
        val tasks = dao.getAllTasksOnce()
        val txs = dao.getAllTransactionsOnce()

        val payload = JSONObject().apply {
            put("storeName", config.storeName)
            put("currencySymbol", config.currencySymbol)
            put("exportedAt", System.currentTimeMillis())
            put("products", JSONArray().apply {
                products.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("barcode", p.barcode)
                        put("name", p.name)
                        put("category", p.category)
                        put("quantity", p.quantity)
                        put("lowStockThreshold", p.lowStockThreshold)
                        put("costPriceCents", p.costPriceCents)
                        put("retailPriceCents", p.retailPriceCents)
                        put("supplierName", p.supplierName)
                    })
                }
            })
            put("tasksCount", tasks.size)
            put("transactionsCount", txs.size)
        }.toString()

        val encrypted = CryptoManager.encrypt(payload)
        preferences.updateLastBackupTimestamp(System.currentTimeMillis())
        return JSONObject().apply {
            put("format", "StockPulse-AES256GCM-Backup-v1")
            put("cipherPayload", encrypted)
        }.toString(2)
    }

    private suspend fun enqueueSync(entityType: String, entityId: String, action: String, summary: String) {
        dao.insertSyncEvent(
            SyncQueueEntity(
                entityType = entityType,
                entityId = entityId,
                action = action,
                summary = summary,
                statusName = "OFFLINE_QUEUED"
            )
        )
    }
}
