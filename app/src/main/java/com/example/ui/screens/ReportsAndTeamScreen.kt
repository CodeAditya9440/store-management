package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.StaffMemberEntity
import com.example.data.model.StaffRole
import com.example.reports.ReportType
import com.example.security.CryptoManager
import com.example.ui.components.ConnectivityStatusPill
import com.example.ui.components.EncryptedVaultBanner
import com.example.ui.components.RoleBadge
import com.example.ui.components.formatTimestampShort
import com.example.ui.state.StockPulseUiState
import java.io.File

@Composable
fun ReportsAndTeamScreen(
    state: StockPulseUiState,
    activeStaff: StaffMemberEntity,
    onSharePdf: (ReportType, (File) -> Unit) -> Unit,
    onSavePdfToUri: (android.content.Context, android.net.Uri, ReportType) -> Unit,
    onSaveBackupToUri: (android.content.Context, android.net.Uri) -> Unit,
    onCommitOfflineQueue: () -> Unit,
    onAddStaff: (name: String, role: StaffRole, pin: String, contact: String) -> Unit,
    onToggleStaffActive: (String) -> Unit,
    onUpdateAutomation: (autoRestock: Boolean, notifications: Boolean) -> Unit,
    onUpdateTaxId: (String) -> Unit,
    onUpdateCurrency: (String) -> Unit,
    onLockSession: () -> Unit
) {
    val context = LocalContext.current
    var pendingSaveReport by remember { mutableStateOf<ReportType?>(null) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var showTaxIdDialog by remember { mutableStateOf(false) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val reportType = pendingSaveReport
        if (uri != null && reportType != null) {
            onSavePdfToUri(context, uri, reportType)
        }
        pendingSaveReport = null
    }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onSaveBackupToUri(context, uri)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun sharePdfFile(file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $title"))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EncryptedVaultBanner()
        }

        // PDF Export Center
        item {
            Text(
                text = "PDF Audit & Reconciliation Exports",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(ReportType.entries) { report ->
            val allowed = report != ReportType.FINANCIAL_AUDIT || activeStaff.role.canExportFinancialAuditPdf()
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = report.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!allowed) {
                        Text(
                            text = "Restricted: Requires Manager or Owner role for financial margin visibility.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    pendingSaveReport = report
                                    savePdfLauncher.launch("${report.filePrefix}.pdf")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_pdf_${report.name.lowercase()}")
                            ) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save PDF")
                            }
                            Button(
                                onClick = {
                                    onSharePdf(report) { file -> sharePdfFile(file, report.title) }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_pdf_${report.name.lowercase()}")
                            ) {
                                Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share PDF")
                            }
                        }
                    }
                }
            }
        }

        // Offline Reliability, Sync Queue & Encrypted Backup
        item {
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Offline Reliability & Backups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ConnectivityStatusPill(
                            isOnline = state.isOnline,
                            pendingCount = state.pendingSyncCount
                        )
                    }

                    Text(
                        text = "Local Room vault records (${state.pendingSyncCount} queued changes) persist offline reliably. Multi-device cloud syncing is disabled because Firebase Cloud setup was declined; use encrypted backups below to archive records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Last backup / vault commit: ${formatTimestampShort(state.storeConfig.lastBackupTimestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCommitOfflineQueue,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Verify Queue")
                        }
                        Button(
                            onClick = { saveBackupLauncher.launch("Store_Manager_Encrypted_Backup.json") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_encrypted_backup_button")
                        ) {
                            Icon(Icons.Rounded.Backup, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Encrypted JSON")
                        }
                    }
                }
            }
        }

        // Automation & Security Settings
        item {
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Automation & Vault Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-create Restock Task", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Generates urgent task when SKU hits low-stock alert",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.storeConfig.autoCreateRestockTask,
                            onCheckedChange = {
                                onUpdateAutomation(it, state.storeConfig.lowStockNotificationsEnabled)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Low-Stock System Notifications", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Triggers Android alerts when inventory drops below threshold",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.storeConfig.lowStockNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                onUpdateAutomation(state.storeConfig.autoCreateRestockTask, enabled)
                            }
                        )
                    }

                    if (activeStaff.role == StaffRole.OWNER || activeStaff.role == StaffRole.MANAGER) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Shop Currency", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("₹", "€", "£").forEach { sym ->
                                    FilterChip(
                                        selected = state.storeConfig.currencySymbol == sym,
                                        onClick = { onUpdateCurrency(sym) },
                                        label = { Text(if (sym == "₹") "₹ INR" else sym) }
                                    )
                                }
                            }
                        }
                    }

                    if (activeStaff.role == StaffRole.OWNER) {
                        OutlinedButton(
                            onClick = { showTaxIdDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            val tax = state.storeConfig.decryptedTaxId()
                            Text(
                                if (tax.isBlank()) "Set Encrypted GST / Tax ID"
                                else "Update Tax ID (••••${tax.takeLast(4)})"
                            )
                        }
                    }
                }
            }
        }

        // Staff RBAC Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Access Control (RBAC)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (activeStaff.role.canManageStaff()) {
                    Button(
                        onClick = { showAddStaffDialog = true },
                        modifier = Modifier.testTag("add_staff_button")
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Staff")
                    }
                }
            }
        }

        items(state.staff, key = { it.id }) { member ->
            val contact = remember(member.encryptedContact, activeStaff.role) {
                if (activeStaff.role.canManageStaff() || member.id == activeStaff.id) {
                    CryptoManager.decrypt(member.encryptedContact)
                } else ""
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (member.id == activeStaff.id) {
                                Icon(
                                    Icons.Rounded.VerifiedUser,
                                    contentDescription = "Active session",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = member.role.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (contact.isNotBlank()) {
                            Text(
                                text = "Encrypted Contact: $contact",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoleBadge(role = member.role)
                        if (activeStaff.role.canManageStaff() && member.id != activeStaff.id) {
                            TextButton(onClick = { onToggleStaffActive(member.id) }) {
                                Text(if (member.isActive) "Deactivate" else "Reactivate")
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onLockSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("lock_session_button")
            ) {
                Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out / Switch Staff")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddStaffDialog) {
        AddStaffDialog(
            onDismiss = { showAddStaffDialog = false },
            onAdd = { name, role, pin, contact ->
                onAddStaff(name, role, pin, contact)
                showAddStaffDialog = false
            }
        )
    }

    if (showTaxIdDialog) {
        var newTax by remember { mutableStateOf(state.storeConfig.decryptedTaxId()) }
        AlertDialog(
            onDismissRequest = { showTaxIdDialog = false },
            title = { Text("Encrypted Tax / Business ID") },
            text = {
                OutlinedTextField(
                    value = newTax,
                    onValueChange = { newTax = it },
                    label = { Text("Tax / Audit Registration ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateTaxId(newTax)
                    showTaxIdDialog = false
                }) {
                    Text("Encrypt & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTaxIdDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AddStaffDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, role: StaffRole, pin: String, contact: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.CASHIER) }
    var pin by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staff Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Staff Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Email / Phone (Encrypted)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    label = { Text("Security PIN (4–8 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Access Role", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(StaffRole.MANAGER, StaffRole.CASHIER, StaffRole.OWNER).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, role, pin, contact) },
                enabled = name.isNotBlank() && pin.length >= 4
            ) {
                Text("Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
