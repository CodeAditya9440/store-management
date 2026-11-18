package com.example.ui.state

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.StockPulseDatabase
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
import com.example.data.network.NetworkConnectivityMonitor
import com.example.data.preferences.StoreConfig
import com.example.data.preferences.StorePreferences
import com.example.data.repository.StoreRepository
import com.example.notifications.LowStockNotifier
import com.example.reports.PdfReportGenerator
import com.example.reports.ReportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyMetric(
    val dayLabel: String,
    val revenueCents: Long,
    val profitCents: Long,
    val purchaseCents: Long = 0L,
    val wasteCents: Long = 0L,
    val netProfitLossCents: Long = 0L,
    val unitsSold: Int = 0,
    val unitsPurchased: Int = 0
)

data class CategoryValuation(
    val category: String,
    val retailValueCents: Long,
    val skuCount: Int,
    val lowStockCount: Int
)

data class ProductStockPerformance(
    val productId: String,
    val name: String,
    val barcode: String,
    val category: String,
    val currentStockQty: Int,
    val lowStockThreshold: Int,
    val stockCostValueCents: Long,
    val stockRetailValueCents: Long,
    val salesCents: Long,
    val purchaseCents: Long,
    val wasteLossCents: Long,
    val profitLossCents: Long,
    val unitsSold: Int,
    val unitsPurchased: Int
)

data class AnalyticsSummary(
    val totalSkus: Int = 0,
    val totalUnits: Int = 0,
    val inventoryCostCents: Long = 0L,
    val inventoryRetailCents: Long = 0L,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val todayRevenueCents: Long = 0L,
    val todayProfitCents: Long = 0L,
    val todayPurchaseCents: Long = 0L,
    val totalRevenueCents: Long = 0L,
    val totalProfitCents: Long = 0L,
    val totalPurchaseCents: Long = 0L,
    val totalWasteLossCents: Long = 0L,
    val netProfitLossCents: Long = 0L,
    val unitsSoldToday: Int = 0,
    val totalUnitsSold: Int = 0,
    val totalUnitsPurchased: Int = 0,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0,
    val dailySeries: List<DailyMetric> = emptyList(),
    val categoryBreakdown: List<CategoryValuation> = emptyList(),
    val productPerformance: List<ProductStockPerformance> = emptyList()
) {
    val taskCompletionRate: Float
        get() = if (totalTasksCount == 0) 0f else completedTasksCount.toFloat() / totalTasksCount.toFloat()
}

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data object NeedsSetup : AuthSessionState
    data class Locked(val staff: List<StaffMemberEntity>) : AuthSessionState
    data class Authenticated(val activeStaff: StaffMemberEntity) : AuthSessionState
}

data class StockPulseUiState(
    val authState: AuthSessionState = AuthSessionState.Loading,
    val storeConfig: StoreConfig = StoreConfig(),
    val isOnline: Boolean = true,
    val staff: List<StaffMemberEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val tasks: List<StoreTaskEntity> = emptyList(),
    val transactions: List<StockTransactionEntity> = emptyList(),
    val alerts: List<LowStockAlertEntity> = emptyList(),
    val syncQueue: List<SyncQueueEntity> = emptyList(),
    val analytics: AnalyticsSummary = AnalyticsSummary(),
    val pendingSyncCount: Int = 0,
    val activeAlertCount: Int = 0
)

data class ScannedBarcodeRequest(
    val barcode: String,
    val formatLabel: String
)

data class ScannedItemState(
    val barcode: String,
    val formatLabel: String,
    val product: ProductEntity?,
    val recentTransactions: List<StockTransactionEntity>
)

class StockPulseViewModel(
    private val repository: StoreRepository,
    connectivityMonitor: NetworkConnectivityMonitor,
    private val pdfGenerator: PdfReportGenerator
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _scannedBarcodeRequest = MutableStateFlow<ScannedBarcodeRequest?>(null)
    private val _recentScans = MutableStateFlow<List<ScannedBarcodeRequest>>(emptyList())
    val recentScans: StateFlow<List<ScannedBarcodeRequest>> = _recentScans.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val scannedItemState: StateFlow<ScannedItemState?> = _scannedBarcodeRequest
        .flatMapLatest { req ->
            if (req == null) {
                flowOf(null)
            } else {
                combine(
                    repository.observeScannedProduct(req.barcode),
                    repository.observeScannedProductTransactions(req.barcode)
                ) { product, txs ->
                    ScannedItemState(
                        barcode = req.barcode,
                        formatLabel = req.formatLabel,
                        product = product,
                        recentTransactions = txs
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null
        )

    private val coreStreams = combine(
        repository.storeConfig,
        repository.staffStream,
        repository.productsStream,
        repository.tasksStream,
        repository.transactionsStream
    ) { config, staff, products, tasks, txs ->
        Tuple5(config, staff, products, tasks, txs)
    }

    private val auxiliaryStreams = combine(
        repository.alertsStream,
        repository.syncQueueStream,
        connectivityMonitor.observeOnlineState()
    ) { alerts, syncQueue, isOnline ->
        Triple(alerts, syncQueue, isOnline)
    }

    val uiState: StateFlow<StockPulseUiState> = combine(coreStreams, auxiliaryStreams) { core, aux ->
        val (config, staff, products, tasks, txs) = core
        val (alerts, syncQueue, isOnline) = aux

        val authState = when {
            !config.isVaultConfigured || staff.isEmpty() -> AuthSessionState.NeedsSetup
            config.activeStaffId.isBlank() -> AuthSessionState.Locked(staff)
            else -> {
                val active = staff.firstOrNull { it.id == config.activeStaffId && it.isActive }
                if (active != null) AuthSessionState.Authenticated(active)
                else AuthSessionState.Locked(staff)
            }
        }

        val analytics = computeAnalytics(products, tasks, txs)
        val pendingSync = syncQueue.count { it.statusName == "OFFLINE_QUEUED" }
        val activeAlerts = alerts.count { it.statusName != "RESOLVED" }

        StockPulseUiState(
            authState = authState,
            storeConfig = config,
            isOnline = isOnline,
            staff = staff,
            products = products,
            tasks = tasks,
            transactions = txs,
            alerts = alerts,
            syncQueue = syncQueue,
            analytics = analytics,
            pendingSyncCount = pendingSync,
            activeAlertCount = activeAlerts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = StockPulseUiState()
    )

    private fun computeAnalytics(
        products: List<ProductEntity>,
        tasks: List<StoreTaskEntity>,
        txs: List<StockTransactionEntity>
    ): AnalyticsSummary {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sales = txs.filter { it.type == StockTxType.SALE }
        val purchases = txs.filter { it.type == StockTxType.RESTOCK }
        val losses = txs.filter {
            it.type == StockTxType.WASTE ||
                (it.type == StockTxType.AUDIT_ADJUSTMENT && it.quantityDelta < 0)
        }

        val todaySales = sales.filter { it.timestamp >= startOfToday }
        val todayPurchases = purchases.filter { it.timestamp >= startOfToday }

        val todayRevenue = todaySales.sumOf { it.totalAmountCents }
        val todayCogs = todaySales.sumOf { kotlin.math.abs(it.quantityDelta).toLong() * it.unitCostCents }
        val todayPurchase = todayPurchases.sumOf { it.totalAmountCents }

        val totalRevenue = sales.sumOf { it.totalAmountCents }
        val totalCogs = sales.sumOf { kotlin.math.abs(it.quantityDelta).toLong() * it.unitCostCents }
        val totalPurchase = purchases.sumOf { it.totalAmountCents }
        val totalWasteLoss = losses.sumOf { kotlin.math.abs(it.quantityDelta).toLong() * it.unitCostCents }
        val totalGrossProfit = totalRevenue - totalCogs
        val netProfitLoss = totalGrossProfit - totalWasteLoss

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dailySeries = (6 downTo 0).map { offset ->
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val start = dayCal.timeInMillis
            val end = start + 86_400_000L
            val daySales = sales.filter { it.timestamp in start until end }
            val dayPurchases = purchases.filter { it.timestamp in start until end }
            val dayLosses = losses.filter { it.timestamp in start until end }

            val rev = daySales.sumOf { it.totalAmountCents }
            val cogs = daySales.sumOf { kotlin.math.abs(it.quantityDelta).toLong() * it.unitCostCents }
            val purch = dayPurchases.sumOf { it.totalAmountCents }
            val waste = dayLosses.sumOf { kotlin.math.abs(it.quantityDelta).toLong() * it.unitCostCents }
            val grossProfit = rev - cogs
            val netPl = grossProfit - waste

            DailyMetric(
                dayLabel = dayFormat.format(Date(start)),
                revenueCents = rev,
                profitCents = grossProfit.coerceAtLeast(0L),
                purchaseCents = purch,
                wasteCents = waste,
                netProfitLossCents = netPl,
                unitsSold = daySales.sumOf { kotlin.math.abs(it.quantityDelta) },
                unitsPurchased = dayPurchases.sumOf { kotlin.math.abs(it.quantityDelta) }
            )
        }

        val categoryBreakdown = products
            .groupBy { it.category.ifBlank { "General" } }
            .map { (cat, list) ->
                CategoryValuation(
                    category = cat,
                    retailValueCents = list.sumOf { it.quantity.toLong() * it.retailPriceCents },
                    skuCount = list.size,
                    lowStockCount = list.count { it.isLowStock }
                )
            }
            .sortedByDescending { it.retailValueCents }

        val txByProduct = txs.groupBy { it.productId }
        val productPerformance = products.map { p ->
            val itemTxs = txByProduct[p.id].orEmpty()
            val itemSales = itemTxs.filter { it.type == StockTxType.SALE }
            val itemPurchases = itemTxs.filter { it.type == StockTxType.RESTOCK }
            val itemLosses = itemTxs.filter {
                it.type == StockTxType.WASTE ||
                    (it.type == StockTxType.AUDIT_ADJUSTMENT && it.quantityDelta < 0)
            }
            val sCents = itemSales.sumOf { it.totalAmountCents }
            val sCogs = itemSales.sumOf { kotlin.math.abs(it.quantityDelta).toLong() * it.unitCostCents }
            val pCents = itemPurchases.sumOf { it.totalAmountCents }
            val wCents = itemLosses.sumOf { kotlin.math.abs(it.quantityDelta).toLong() * it.unitCostCents }
            ProductStockPerformance(
                productId = p.id,
                name = p.name,
                barcode = p.barcode,
                category = p.category,
                currentStockQty = p.quantity,
                lowStockThreshold = p.lowStockThreshold,
                stockCostValueCents = p.quantity.toLong() * p.costPriceCents,
                stockRetailValueCents = p.quantity.toLong() * p.retailPriceCents,
                salesCents = sCents,
                purchaseCents = pCents,
                wasteLossCents = wCents,
                profitLossCents = (sCents - sCogs) - wCents,
                unitsSold = itemSales.sumOf { kotlin.math.abs(it.quantityDelta) },
                unitsPurchased = itemPurchases.sumOf { kotlin.math.abs(it.quantityDelta) }
            )
        }.sortedByDescending { it.salesCents + it.purchaseCents + it.stockRetailValueCents }

        return AnalyticsSummary(
            totalSkus = products.size,
            totalUnits = products.sumOf { it.quantity },
            inventoryCostCents = products.sumOf { it.quantity.toLong() * it.costPriceCents },
            inventoryRetailCents = products.sumOf { it.quantity.toLong() * it.retailPriceCents },
            lowStockCount = products.count { it.isLowStock && !it.isOutOfStock },
            outOfStockCount = products.count { it.isOutOfStock },
            todayRevenueCents = todayRevenue,
            todayProfitCents = todayRevenue - todayCogs,
            todayPurchaseCents = todayPurchase,
            totalRevenueCents = totalRevenue,
            totalProfitCents = totalGrossProfit,
            totalPurchaseCents = totalPurchase,
            totalWasteLossCents = totalWasteLoss,
            netProfitLossCents = netProfitLoss,
            unitsSoldToday = todaySales.sumOf { kotlin.math.abs(it.quantityDelta) },
            totalUnitsSold = sales.sumOf { kotlin.math.abs(it.quantityDelta) },
            totalUnitsPurchased = purchases.sumOf { kotlin.math.abs(it.quantityDelta) },
            completedTasksCount = tasks.count { it.status == TaskStatus.DONE },
            totalTasksCount = tasks.size,
            dailySeries = dailySeries,
            categoryBreakdown = categoryBreakdown,
            productPerformance = productPerformance
        )
    }

    private fun activeActor(): StaffMemberEntity? =
        (uiState.value.authState as? AuthSessionState.Authenticated)?.activeStaff

    fun clearAuthError() {
        _authError.value = null
    }

    fun initializeVault(
        storeName: String,
        currencySymbol: String,
        taxId: String,
        ownerName: String,
        ownerPin: String,
        ownerContact: String,
        seedSampleData: Boolean
    ) {
        viewModelScope.launch {
            _authError.value = null
            repository.initializeStoreVault(
                storeName = storeName,
                currencySymbol = currencySymbol,
                taxId = taxId,
                ownerName = ownerName,
                ownerPin = ownerPin,
                ownerContact = ownerContact,
                seedSampleData = seedSampleData
            ).onSuccess {
                _messages.tryEmit("Welcome! Your shop is ready")
            }.onFailure {
                val msg = it.message ?: "Setup failed"
                _authError.value = msg
                _messages.tryEmit(msg)
            }
        }
    }

    fun signInWithPin(staffId: String, pin: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.authenticateStaff(staffId, pin)
                .onSuccess {
                    _messages.tryEmit("Welcome back, ${it.name} (${it.role.displayName})")
                }
                .onFailure {
                    val msg = it.message ?: "Incorrect PIN. Please try again."
                    _authError.value = msg
                    _messages.tryEmit(msg)
                }
        }
    }

    fun lockVault() {
        viewModelScope.launch {
            _authError.value = null
            repository.lockSession()
        }
    }

    fun addStaff(name: String, role: StaffRole, pin: String, contact: String) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.addStaffMember(actor, name, role, pin, contact)
                .onSuccess { _messages.tryEmit("Added $name (${role.displayName})") }
                .onFailure { _messages.tryEmit(it.message ?: "Could not add staff") }
        }
    }

    fun updateStaffRole(targetId: String, newRole: StaffRole) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.updateStaffRole(actor, targetId, newRole)
                .onSuccess { _messages.tryEmit("Updated role to ${newRole.displayName}") }
                .onFailure { _messages.tryEmit(it.message ?: "Could not update role") }
        }
    }

    fun toggleStaffActive(targetId: String) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.toggleStaffActive(actor, targetId)
                .onSuccess { _messages.tryEmit("Updated staff status") }
                .onFailure { _messages.tryEmit(it.message ?: "Could not update staff status") }
        }
    }

    fun saveProduct(
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
    ) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.saveProduct(
                actor = actor,
                existingId = existingId,
                barcode = barcode,
                name = name,
                category = category,
                quantity = quantity,
                lowStockThreshold = lowStockThreshold,
                costPriceCents = costPriceCents,
                retailPriceCents = retailPriceCents,
                supplierName = supplierName,
                plainWholesaleNotes = plainWholesaleNotes
            ).onSuccess {
                _messages.tryEmit("Saved ${it.name}")
            }.onFailure {
                _messages.tryEmit(it.message ?: "Failed to save product")
            }
        }
    }

    fun deleteProduct(productId: String) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.deleteProduct(actor, productId)
                .onSuccess { _messages.tryEmit("SKU removed") }
                .onFailure { _messages.tryEmit(it.message ?: "Delete denied") }
        }
    }

    fun adjustStock(productId: String, txType: StockTxType, units: Int, note: String = "") {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.adjustStock(actor, productId, txType, units, note)
                .onSuccess { updated ->
                    val statusMsg = when {
                        updated.isOutOfStock -> "${updated.name}: OUT OF STOCK!"
                        updated.isLowStock -> "${updated.name}: Low stock (${updated.quantity} left)"
                        else -> "${txType.label} recorded — ${updated.quantity} in stock"
                    }
                    _messages.tryEmit(statusMsg)
                }
                .onFailure {
                    _messages.tryEmit(it.message ?: "Stock adjustment failed")
                }
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId)
            _messages.tryEmit("Alert acknowledged")
        }
    }

    fun saveTask(
        existingId: String?,
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        shift: WorkShift,
        assignee: StaffMemberEntity?,
        linkedBarcode: String?,
        dueLabel: String
    ) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.saveTask(
                actor = actor,
                existingId = existingId,
                title = title,
                description = description,
                category = category,
                priority = priority,
                shift = shift,
                assignee = assignee,
                linkedBarcode = linkedBarcode,
                dueLabel = dueLabel
            ).onSuccess {
                _messages.tryEmit("Task saved")
            }.onFailure {
                _messages.tryEmit(it.message ?: "Failed to save task")
            }
        }
    }

    fun cycleTaskStatus(task: StoreTaskEntity) {
        viewModelScope.launch {
            repository.cycleTaskStatus(task)
        }
    }

    fun deleteTask(taskId: String) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.deleteTask(actor, taskId)
                .onSuccess { _messages.tryEmit("Task deleted") }
                .onFailure { _messages.tryEmit(it.message ?: "Delete failed") }
        }
    }

    fun onBarcodeDetected(barcode: String, formatLabel: String = "Barcode") {
        val clean = barcode.trim()
        if (clean.isBlank()) return
        val req = ScannedBarcodeRequest(clean, formatLabel)
        _scannedBarcodeRequest.value = req
        _recentScans.value = (listOf(req) + _recentScans.value.filterNot { it.barcode.equals(clean, ignoreCase = true) }).take(6)
    }

    fun clearScannedBarcode() {
        _scannedBarcodeRequest.value = null
    }

    fun updateAutomation(autoRestock: Boolean, notifications: Boolean) {
        viewModelScope.launch {
            repository.updateAutomation(autoRestock, notifications)
        }
    }

    fun updateTaxId(plainTaxId: String) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.updateTaxId(actor, plainTaxId)
                .onSuccess { _messages.tryEmit("Encrypted Tax ID updated") }
                .onFailure { _messages.tryEmit(it.message ?: "Permission denied") }
        }
    }

    fun updateCurrency(symbol: String) {
        val actor = activeActor() ?: return
        viewModelScope.launch {
            repository.updateCurrency(actor, symbol)
                .onSuccess { _messages.tryEmit("Shop currency updated to $symbol") }
                .onFailure { _messages.tryEmit(it.message ?: "Permission denied") }
        }
    }

    fun commitOfflineQueue() {
        viewModelScope.launch {
            val count = repository.commitOfflineQueueLocally()
            _messages.tryEmit(
                if (count > 0) "Verified & committed $count local records to offline vault"
                else "All local records are up to date"
            )
        }
    }

    fun generateShareablePdf(type: ReportType, onFileReady: (File) -> Unit) {
        val actor = activeActor() ?: return
        val state = uiState.value
        if (type == ReportType.FINANCIAL_AUDIT && !actor.role.canExportFinancialAuditPdf()) {
            _messages.tryEmit("Financial Audit PDFs require Manager or Owner access")
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    pdfGenerator.generateToCacheFile(
                        type = type,
                        storeConfig = state.storeConfig,
                        auditor = actor,
                        products = state.products,
                        transactions = state.transactions,
                        tasks = state.tasks,
                        alerts = state.alerts
                    )
                }
            }
            result.onSuccess(onFileReady)
                .onFailure { _messages.tryEmit("PDF generation failed: ${it.message}") }
        }
    }

    fun savePdfToUri(context: Context, uri: Uri, type: ReportType) {
        val actor = activeActor() ?: return
        val state = uiState.value
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        pdfGenerator.writeReport(
                            type = type,
                            storeConfig = state.storeConfig,
                            auditor = actor,
                            products = state.products,
                            transactions = state.transactions,
                            tasks = state.tasks,
                            alerts = state.alerts,
                            outputStream = out
                        )
                    } ?: error("Unable to open output stream")
                }
            }
            result.onSuccess { _messages.tryEmit("${type.title} saved as PDF") }
                .onFailure { _messages.tryEmit("Save failed: ${it.message}") }
        }
    }

    fun saveEncryptedBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val json = repository.exportEncryptedBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open backup destination")
                }
            }
            result.onSuccess { _messages.tryEmit("AES-256-GCM encrypted backup exported") }
                .onFailure { _messages.tryEmit("Backup failed: ${it.message}") }
        }
    }

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            val db = StockPulseDatabase.getInstance(appCtx)
            val prefs = StorePreferences(appCtx)
            val notifier = LowStockNotifier(appCtx)
            val repo = StoreRepository(db.storeDao(), prefs, notifier)
            val monitor = NetworkConnectivityMonitor(appCtx)
            val pdfGen = PdfReportGenerator(appCtx)
            return StockPulseViewModel(repo, monitor, pdfGen) as T
        }
    }
}
