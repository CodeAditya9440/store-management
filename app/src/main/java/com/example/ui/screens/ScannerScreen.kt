package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.model.ProductEntity
import com.example.data.model.StaffMemberEntity
import com.example.data.model.StockTxType
import com.example.security.CryptoManager
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatTimestampShort
import com.example.ui.state.ScannedBarcodeRequest
import com.example.ui.state.ScannedItemState
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.AmberTertiaryContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.OnAlertRedContainer
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.Locale
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    currencySymbol: String,
    activeStaff: StaffMemberEntity,
    scannedItemState: ScannedItemState?,
    recentScans: List<ScannedBarcodeRequest>,
    onBarcodeDetected: (barcode: String, formatLabel: String) -> Unit,
    onClearScanned: () -> Unit,
    onAdjustStock: (productId: String, type: StockTxType, units: Int, note: String) -> Unit,
    onSaveNewProduct: (
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
    ) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var manualBarcode by remember { mutableStateOf("") }
    var showCreateProductSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // CameraX + ML Kit Live Viewport Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MaterialTheme.shapes.large)
                .background(Color(0xFF0A1410))
        ) {
            if (hasCameraPermission) {
                BarcodeCameraPreview(
                    isPaused = scannedItemState != null || showCreateProductSheet,
                    onCameraBound = { camera -> cameraRef = camera },
                    onBarcodeScanned = { code, format ->
                        onBarcodeDetected(code, format)
                    }
                )
                ScannerReticleOverlay(modifier = Modifier.fillMaxSize())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (scannedItemState == null) "Point camera at product barcode"
                            else "Found in Shop Database",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    val hasFlash = cameraRef?.cameraInfo?.hasFlashUnit() == true
                    FilledTonalIconButton(
                        onClick = {
                            if (hasFlash) {
                                torchEnabled = !torchEnabled
                                cameraRef?.cameraControl?.enableTorch(torchEnabled)
                            }
                        },
                        enabled = hasFlash,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("scanner_torch_button")
                    ) {
                        Icon(
                            imageVector = if (torchEnabled && hasFlash) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                            contentDescription = stringResource(R.string.cd_toggle_torch)
                        )
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = "Supports EAN-13 • EAN-8 • UPC-A • UPC-E • Code-128 • QR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD1FAE5),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Camera Access Needed for Live Scanning",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Grant camera permission to scan retail barcodes with ML Kit, or enter a barcode manually below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("grant_camera_button")
                    ) {
                        Text("Enable Camera Scanner")
                    }
                }
            }
        }

        // Recent Scans Strip
        if (recentScans.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "Recent scans",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                recentScans.forEach { scan ->
                    AssistChip(
                        onClick = { onBarcodeDetected(scan.barcode, scan.formatLabel) },
                        label = { Text(scan.barcode, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Manual Barcode Entry Fallback
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        MaterialTheme.shapes.medium
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Manual Barcode / Local DB Lookup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcode,
                        onValueChange = { manualBarcode = it },
                        placeholder = { Text("Enter EAN / UPC / SKU...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_barcode_input")
                    )
                    Button(
                        onClick = {
                            if (manualBarcode.isNotBlank()) {
                                onBarcodeDetected(manualBarcode.trim(), "Manual Entry")
                            }
                        },
                        enabled = manualBarcode.isNotBlank(),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("manual_lookup_button")
                    ) {
                        Icon(Icons.Rounded.Search, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Lookup")
                    }
                }
            }
        }
    }

    // Scanned Item Details Sheet (Directly backed by Room Local Database)
    if (scannedItemState != null && !showCreateProductSheet) {
        ModalBottomSheet(onDismissRequest = onClearScanned) {
            ScannedProductDetailSheet(
                state = scannedItemState,
                currencySymbol = currencySymbol,
                activeStaff = activeStaff,
                onAdjustStock = onAdjustStock,
                onOpenCreateProduct = { showCreateProductSheet = true },
                onClose = onClearScanned
            )
        }
    }

    if (showCreateProductSheet && scannedItemState != null) {
        ModalBottomSheet(onDismissRequest = {
            showCreateProductSheet = false
            onClearScanned()
        }) {
            ProductEditorSheet(
                existing = null,
                initialBarcode = scannedItemState.barcode,
                currencySymbol = currencySymbol,
                onDismiss = {
                    showCreateProductSheet = false
                    onClearScanned()
                },
                onSave = { code, name, cat, qty, thresh, costCents, retailCents, supplier, notes ->
                    onSaveNewProduct(null, code, name, cat, qty, thresh, costCents, retailCents, supplier, notes)
                    showCreateProductSheet = false
                }
            )
        }
    }
}

@Composable
private fun ScannedProductDetailSheet(
    state: ScannedItemState,
    currencySymbol: String,
    activeStaff: StaffMemberEntity,
    onAdjustStock: (productId: String, type: StockTxType, units: Int, note: String) -> Unit,
    onOpenCreateProduct: () -> Unit,
    onClose: () -> Unit
) {
    val product = state.product
    val canSeeSensitive = activeStaff.role.canViewSensitiveFinancials()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "${state.formatLabel} • Local DB Match",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.barcode,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close details")
            }
        }

        if (product != null) {
            var unitsStr by remember(product.id) { mutableStateOf("1") }
            val units = (unitsStr.toIntOrNull() ?: 1).coerceAtLeast(1)

            val (badgeBg, badgeFg, badgeLabel) = when {
                product.isOutOfStock -> Triple(AlertRedContainer, OnAlertRedContainer, "Out of Stock")
                product.isLowStock -> Triple(AmberTertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, "Low Stock")
                else -> Triple(EmeraldPrimaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "In Stock")
            }

            val decryptedNotes = remember(product.encryptedWholesaleNotes, canSeeSensitive) {
                if (canSeeSensitive) CryptoManager.decrypt(product.encryptedWholesaleNotes) else ""
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Category: ${product.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeFg,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailStat("On Hand", "${product.quantity} units")
                        DetailStat("Alert Min", "${product.lowStockThreshold} units")
                        DetailStat("Retail Price", formatCurrency(product.retailPriceCents, currencySymbol))
                    }

                    if (canSeeSensitive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailStat("Cost Price", formatCurrency(product.costPriceCents, currencySymbol))
                            DetailStat(
                                "Gross Margin",
                                "${String.format(Locale.US, "%.1f", product.marginPercent)}%"
                            )
                            DetailStat("Last Audit", formatTimestampShort(product.lastAuditedAt))
                        }
                    }

                    if (product.supplierName.isNotBlank() || decryptedNotes.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (canSeeSensitive) Icons.Rounded.Lock else Icons.Rounded.Inventory2,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    if (product.supplierName.isNotBlank()) {
                                        Text(
                                            text = "Supplier: ${product.supplierName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (canSeeSensitive && decryptedNotes.isNotBlank()) {
                                        Text(
                                            text = decryptedNotes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Stock Adjustment Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = unitsStr,
                    onValueChange = { if (it.all(Char::isDigit)) unitsStr = it },
                    label = { Text("Units") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(96.dp)
                )
                Button(
                    onClick = {
                        onAdjustStock(product.id, StockTxType.SALE, units, "Barcode POS scan")
                    },
                    enabled = product.quantity > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("scanner_record_sale_button")
                ) {
                    Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sale")
                }
                if (activeStaff.role.canEditInventoryPricing()) {
                    OutlinedButton(
                        onClick = {
                            onAdjustStock(product.id, StockTxType.RESTOCK, units, "Barcode restock scan")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("scanner_restock_button")
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restock")
                    }
                }
            }

            // Recent Database Ledger for this Scanned Item
            if (state.recentTransactions.isNotEmpty()) {
                Text(
                    text = "Recent Ledger Activity (${state.recentTransactions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                state.recentTransactions.take(4).forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${tx.type.label} • ${tx.staffName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${formatTimestampShort(tx.timestamp)} — ${tx.note}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val delta = if (tx.quantityDelta >= 0) "+${tx.quantityDelta}" else "${tx.quantityDelta}"
                        Text(
                            text = "$delta units",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (tx.quantityDelta >= 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Not Found in Local Database",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No product with barcode ${state.barcode} exists in your local shop catalog yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                    Text("Scan Another")
                }
                if (activeStaff.role.canEditInventoryPricing()) {
                    Button(
                        onClick = onOpenCreateProduct,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("scanner_register_sku_button")
                    ) {
                        Text("Register New SKU")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun BarcodeCameraPreview(
    isPaused: Boolean,
    onCameraBound: (Camera) -> Unit,
    onBarcodeScanned: (barcode: String, formatLabel: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pausedState by rememberUpdatedState(isPaused)
    val scannedCallback by rememberUpdatedState(onBarcodeScanned)
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var lastHandledCode = ""
        var lastHandledAtMs = 0L

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    if (pausedState) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(inputImage)
                            .addOnSuccessListener(mainExecutor) { barcodes ->
                                if (pausedState) return@addOnSuccessListener
                                val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                val code = first?.rawValue?.trim()
                                if (!code.isNullOrBlank()) {
                                    val nowMs = System.currentTimeMillis()
                                    if (code != lastHandledCode || nowMs - lastHandledAtMs > 1200L) {
                                        lastHandledCode = code
                                        lastHandledAtMs = nowMs
                                        scannedCallback(code, barcodeFormatLabel(first.format))
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                onCameraBound(camera)
            } catch (e: Exception) {
                Log.w("ScannerScreen", "Camera binding failed: ${e.message}")
            }
        }, mainExecutor)
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

private fun barcodeFormatLabel(format: Int): String = when (format) {
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    Barcode.FORMAT_CODE_128 -> "Code-128"
    Barcode.FORMAT_CODE_39 -> "Code-39"
    Barcode.FORMAT_QR_CODE -> "QR Code"
    else -> "Barcode"
}

@Composable
private fun ScannerReticleOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "laser_scan")
    val laserFraction by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = modifier) {
        val boxW = size.width * 0.72f
        val boxH = size.height * 0.36f
        val left = (size.width - boxW) / 2f
        val top = (size.height - boxH) / 2f

        drawRoundRect(
            color = Color(0xFF34D399),
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 4f)
        )

        val laserY = top + boxH * laserFraction
        drawLine(
            color = Color(0xFF34D399).copy(alpha = 0.85f),
            start = Offset(left + 16f, laserY),
            end = Offset(left + boxW - 16f, laserY),
            strokeWidth = 3f
        )
    }
}
