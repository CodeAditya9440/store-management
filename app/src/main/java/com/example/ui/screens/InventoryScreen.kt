package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProductEntity
import com.example.data.model.StaffMemberEntity
import com.example.data.model.StockTxType
import com.example.security.CryptoManager
import com.example.ui.components.formatCurrency
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.AmberTertiaryContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.OnAlertRedContainer
import java.util.Locale
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<ProductEntity>,
    currencySymbol: String,
    activeStaff: StaffMemberEntity,
    onOpenScanner: () -> Unit,
    onOpenStockGraphs: () -> Unit,
    onAdjustStock: (productId: String, type: StockTxType, units: Int, note: String) -> Unit,
    onSaveProduct: (
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
    ) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var lowStockOnly by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showEditorSheet by remember { mutableStateOf(false) }
    var auditProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val categories = remember(products) {
        products.map { it.category }.distinct().sorted()
    }

    val filtered = remember(products, search, lowStockOnly, selectedCategory) {
        products.filter { p ->
            val matchQuery = search.isBlank() ||
                p.name.contains(search, ignoreCase = true) ||
                p.barcode.contains(search, ignoreCase = true) ||
                p.supplierName.contains(search, ignoreCase = true)
            val matchLow = !lowStockOnly || p.isLowStock
            val matchCat = selectedCategory == null || p.category == selectedCategory
            matchQuery && matchLow && matchCat
        }
    }

    val canEditPricing = activeStaff.role.canEditInventoryPricing()
    val canDelete = activeStaff.role.canDeleteInventory()
    val canSeeSensitive = activeStaff.role.canViewSensitiveFinancials()

    Scaffold(
        floatingActionButton = {
            if (canEditPricing) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showEditorSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_add_item))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .animateContentSize(animationSpec = spring(dampingRatio = 0.85f)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search SKU, barcode, supplier...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inventory_search_input")
                    )
                    OutlinedButton(
                        onClick = onOpenStockGraphs,
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("inventory_graphs_button")
                    ) {
                        Icon(Icons.Rounded.BarChart, contentDescription = "Open stock graphs")
                    }
                    OutlinedButton(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("inventory_scan_button")
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = stringResource(R.string.cd_scan_barcode))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = lowStockOnly,
                        onClick = { lowStockOnly = !lowStockOnly },
                        label = { Text("Low / Out (${products.count { it.isLowStock }})") }
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_inventory),
                                contentDescription = stringResource(R.string.cd_empty_inventory),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Text(
                                text = "No inventory items match",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Scan a retail barcode or tap + to add products with automated low-stock thresholds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { product ->
                    ProductInventoryCard(
                        product = product,
                        currencySymbol = currencySymbol,
                        canSeeSensitive = canSeeSensitive,
                        canEdit = canEditPricing,
                        canDelete = canDelete,
                        onQuickSale = { onAdjustStock(product.id, StockTxType.SALE, 1, "Quick POS sale") },
                        onQuickRestock = { onAdjustStock(product.id, StockTxType.RESTOCK, 1, "Quick shelf restock") },
                        onAuditCount = { auditProduct = product },
                        onEdit = {
                            editingProduct = product
                            showEditorSheet = true
                        },
                        onDelete = { onDeleteProduct(product.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showEditorSheet) {
        ModalBottomSheet(onDismissRequest = { showEditorSheet = false }) {
            ProductEditorSheet(
                existing = editingProduct,
                initialBarcode = "",
                currencySymbol = currencySymbol,
                onDismiss = { showEditorSheet = false },
                onSave = { barcode, name, cat, qty, threshold, costCents, retailCents, supplier, notes ->
                    onSaveProduct(
                        editingProduct?.id,
                        barcode,
                        name,
                        cat,
                        qty,
                        threshold,
                        costCents,
                        retailCents,
                        supplier,
                        notes
                    )
                    showEditorSheet = false
                }
            )
        }
    }

    auditProduct?.let { target ->
        PhysicalAuditDialog(
            product = target,
            onDismiss = { auditProduct = null },
            onConfirmAudit = { physicalQty, note ->
                onAdjustStock(target.id, StockTxType.AUDIT_ADJUSTMENT, physicalQty, note)
                auditProduct = null
            }
        )
    }
}

@Composable
private fun ProductInventoryCard(
    product: ProductEntity,
    currencySymbol: String,
    canSeeSensitive: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    onQuickSale: () -> Unit,
    onQuickRestock: () -> Unit,
    onAuditCount: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val stockBadgeColors = when {
        product.isOutOfStock -> AlertRedContainer to OnAlertRedContainer
        product.isLowStock -> AmberTertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> EmeraldPrimaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    val decryptedNotes = remember(product.encryptedWholesaleNotes, canSeeSensitive) {
        if (canSeeSensitive) CryptoManager.decrypt(product.encryptedWholesaleNotes) else ""
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.barcode}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                    MaterialTheme.shapes.medium
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.barcode} • ${product.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(color = stockBadgeColors.first, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "${product.quantity} in stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = stockBadgeColors.second,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Retail: ${formatCurrency(product.retailPriceCents, currencySymbol)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (canSeeSensitive) {
                        Text(
                            text = "Cost: ${formatCurrency(product.costPriceCents, currencySymbol)} (${String.format(Locale.US, "%.0f", product.marginPercent)}% margin)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Min Alert: ${product.lowStockThreshold}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canSeeSensitive && (product.supplierName.isNotBlank() || decryptedNotes.isNotBlank())) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = "Encrypted wholesale record",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = buildString {
                                if (product.supplierName.isNotBlank()) append(product.supplierName)
                                if (decryptedNotes.isNotBlank()) {
                                    if (isNotEmpty()) append(" — ")
                                    append(decryptedNotes)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onQuickSale,
                        enabled = product.quantity > 0,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("sale_minus_${product.barcode}")
                    ) {
                        Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sale")
                    }
                    OutlinedButton(
                        onClick = onQuickRestock,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("restock_plus_${product.barcode}")
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restock")
                    }
                }

                Row {
                    if (canEdit) {
                        TextButton(onClick = onAuditCount) {
                            Text("Audit")
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit product")
                        }
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete product",
                                tint = AlertRed.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductEditorSheet(
    existing: ProductEntity?,
    initialBarcode: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        barcode: String,
        name: String,
        category: String,
        quantity: Int,
        lowStockThreshold: Int,
        costPriceCents: Long,
        retailPriceCents: Long,
        supplierName: String,
        plainWholesaleNotes: String
    ) -> Unit
) {
    var barcode by remember(existing, initialBarcode) {
        mutableStateOf(existing?.barcode ?: initialBarcode)
    }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var category by remember(existing) { mutableStateOf(existing?.category ?: "General") }
    var qtyStr by remember(existing) { mutableStateOf((existing?.quantity ?: 10).toString()) }
    var thresholdStr by remember(existing) { mutableStateOf((existing?.lowStockThreshold ?: 5).toString()) }
    var costStr by remember(existing) {
        mutableStateOf(
            if (existing != null) String.format(Locale.US, "%.2f", existing.costPriceCents / 100.0) else "100.00"
        )
    }
    var retailStr by remember(existing) {
        mutableStateOf(
            if (existing != null) String.format(Locale.US, "%.2f", existing.retailPriceCents / 100.0) else "150.00"
        )
    }
    var supplier by remember(existing) { mutableStateOf(existing?.supplierName ?: "") }
    var wholesaleNotes by remember(existing) {
        mutableStateOf(existing?.let { CryptoManager.decrypt(it.encryptedWholesaleNotes) } ?: "")
    }

    fun parseDecimalInput(raw: String): String {
        val cleaned = raw.replace(",", "").filter { it.isDigit() || it == '.' }
        val dotIndex = cleaned.indexOf('.')
        return if (dotIndex == -1) cleaned else {
            val whole = cleaned.substring(0, dotIndex + 1)
            val frac = cleaned.substring(dotIndex + 1).replace(".", "").take(2)
            whole + frac
        }
    }

    val parsedCostCents = ((costStr.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
    val parsedRetailCents = ((retailStr.toDoubleOrNull() ?: 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
    val profitPerUnitCents = parsedRetailCents - parsedCostCents
    val marginPct = if (parsedRetailCents > 0L) (profitPerUnitCents.toDouble() / parsedRetailCents) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (existing == null) "Add Product" else "Edit Product",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = { Text("Barcode / SKU") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_barcode_input")
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Product Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_name_input")
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = qtyStr,
                onValueChange = { if (it.all(Char::isDigit)) qtyStr = it },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = thresholdStr,
                onValueChange = { if (it.all(Char::isDigit)) thresholdStr = it },
                label = { Text("Low-Stock Alert") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = costStr,
                onValueChange = { costStr = parseDecimalInput(it) },
                label = { Text("Cost Price ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = retailStr,
                onValueChange = { retailStr = parseDecimalInput(it) },
                label = { Text("Selling Price ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Surface(
            color = if (profitPerUnitCents >= 0L) EmeraldPrimaryContainer.copy(alpha = 0.5f) else AlertRedContainer.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (profitPerUnitCents >= 0L) {
                    "Profit per unit: ${formatCurrency(profitPerUnitCents, currencySymbol)} (${String.format(Locale.US, "%.0f", marginPct)}% margin)"
                } else {
                    "Loss per unit: ${formatCurrency(profitPerUnitCents, currencySymbol)} (selling below cost)"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (profitPerUnitCents >= 0L) MaterialTheme.colorScheme.onPrimaryContainer else OnAlertRedContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        OutlinedTextField(
            value = supplier,
            onValueChange = { supplier = it },
            label = { Text("Supplier Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = wholesaleNotes,
            onValueChange = { wholesaleNotes = it },
            label = { Text("Supplier Notes (Encrypted)") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val qty = qtyStr.toIntOrNull() ?: 0
                    val thresh = thresholdStr.toIntOrNull() ?: 5
                    onSave(barcode, name, category, qty, thresh, parsedCostCents, parsedRetailCents, supplier, wholesaleNotes)
                },
                enabled = barcode.isNotBlank() && name.isNotBlank(),
                modifier = Modifier
                    .weight(2f)
                    .testTag("save_product_button")
            ) {
                Text("Save Product")
            }
        }
    }
}

@Composable
private fun PhysicalAuditDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirmAudit: (physicalQty: Int, note: String) -> Unit
) {
    var countText by remember { mutableStateOf(product.quantity.toString()) }
    var note by remember { mutableStateOf("Physical shelf count reconciliation") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audit Physical Count") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${product.name}\nSystem expects: ${product.quantity} units",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { if (it.all(Char::isDigit)) countText = it },
                    label = { Text("Actual Counted Units") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Audit Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmAudit(countText.toIntOrNull() ?: product.quantity, note)
                },
                enabled = countText.isNotBlank()
            ) {
                Text("Commit Audit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
