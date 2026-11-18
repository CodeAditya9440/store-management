package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AlertSeverity
import com.example.data.model.AlertStatus
import com.example.data.model.StaffMemberEntity
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.RevenueProfitBarChart
import com.example.ui.components.RoleBadge
import com.example.ui.components.StockHealthDonut
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatTimestampShort
import com.example.ui.state.StockPulseUiState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.AmberTertiaryContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnAlertRedContainer

@Composable
fun DashboardScreen(
    state: StockPulseUiState,
    activeStaff: StaffMemberEntity,
    onOpenScanner: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenStockGraphs: () -> Unit,
    onAcknowledgeAlert: (String) -> Unit
) {
    val currency = state.storeConfig.currencySymbol.ifBlank { "₹" }
    val analytics = state.analytics
    val canSeeFinancials = activeStaff.role.canViewSensitiveFinancials()
    val openAlerts = state.alerts.filter { it.status != AlertStatus.RESOLVED }
    val healthySkus = (analytics.totalSkus - analytics.lowStockCount - analytics.outOfStockCount).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Store Operations Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_store_hero),
                    contentDescription = stringResource(id = R.string.cd_store_hero),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF0A2E23).copy(alpha = 0.92f),
                                    Color(0xFF0A2E23).copy(alpha = 0.55f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.storeConfig.storeName.ifBlank { "Store Manager" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Welcome, ${activeStaff.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD1FAE5)
                                )
                            }
                            RoleBadge(role = activeStaff.role)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenScanner,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF34D399),
                                    contentColor = Color(0xFF062E22)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_scan_button")
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Scan Item")
                            }
                            FilledTonalButton(
                                onClick = onOpenTasks,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_tasks_button")
                            ) {
                                Icon(Icons.Rounded.TaskAlt, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Daily Tasks")
                            }
                        }
                    }
                }
            }
        }

        // KPI Metric Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canSeeFinancials) {
                        KpiMetricCard(
                            label = "Today's Sales",
                            value = formatCurrency(analytics.todayRevenueCents, currency),
                            subtext = "Profit ${formatCurrency(analytics.todayProfitCents, currency)}",
                            icon = Icons.Rounded.CurrencyRupee,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            label = "Shop Stock Value",
                            value = formatCurrency(analytics.inventoryRetailCents, currency),
                            subtext = "Cost ${formatCurrency(analytics.inventoryCostCents, currency)}",
                            icon = Icons.Rounded.TrendingUp,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        KpiMetricCard(
                            label = "Units Sold Today",
                            value = "${analytics.unitsSoldToday}",
                            subtext = "POS Barcode Checkouts",
                            icon = Icons.Rounded.TrendingUp,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricCard(
                            label = "Active Catalog",
                            value = "${analytics.totalSkus} SKUs",
                            subtext = "${analytics.totalUnits} total units",
                            icon = Icons.Rounded.Inventory2,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        label = "Daily Tasks",
                        value = "${analytics.completedTasksCount}/${analytics.totalTasksCount}",
                        subtext = "${(analytics.taskCompletionRate * 100).toInt()}% shift complete",
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = "Stock Alerts",
                        value = "${analytics.lowStockCount + analytics.outOfStockCount}",
                        subtext = "${analytics.outOfStockCount} out • ${analytics.lowStockCount} low",
                        icon = Icons.Rounded.WarningAmber,
                        accentColor = if (analytics.outOfStockCount > 0) AlertRed else AmberTertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Stock Details in Graph Format Quick-Launch Card
        item {
            Card(
                onClick = onOpenStockGraphs,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_stock_graphs_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stock & Profit Graphs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "View Sale, Purchase, Stock & Profit/Loss charts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Open stock graphs",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Automated Low-Stock Alerts Section
        if (openAlerts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Automated Low-Stock Alerts (${openAlerts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = onOpenInventory) {
                        Text("Restock")
                    }
                }
            }

            items(openAlerts.take(4), key = { it.id }) { alert ->
                val isOut = alert.severity == AlertSeverity.OUT_OF_STOCK
                val container = if (isOut) AlertRedContainer.copy(alpha = 0.65f) else AmberTertiaryContainer.copy(alpha = 0.65f)
                val accent = if (isOut) OnAlertRedContainer else MaterialTheme.colorScheme.onTertiaryContainer

                Card(
                    colors = CardDefaults.cardColors(containerColor = container),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${alert.severity.label}: ${alert.productName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SKU ${alert.barcode} • ${alert.currentQty} left (Min ${alert.threshold})",
                                style = MaterialTheme.typography.bodySmall,
                                color = accent.copy(alpha = 0.85f)
                            )
                        }
                        if (alert.status == AlertStatus.ACTIVE) {
                            TextButton(
                                onClick = { onAcknowledgeAlert(alert.id) },
                                modifier = Modifier.testTag("ack_alert_${alert.id}")
                            ) {
                                Text("Acknowledge", color = accent, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(color = Color.White.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "Ack'd",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-Time Revenue & Margin Chart (Owner/Manager)
        if (canSeeFinancials) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    RevenueProfitBarChart(
                        series = analytics.dailySeries,
                        currency = currency,
                        modifier = Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                MaterialTheme.shapes.medium
                            )
                            .padding(16.dp)
                    )
                }
            }
        }

        // Stock Health Breakdown & Category Valuation
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Inventory Health Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    StockHealthDonut(
                        healthyCount = healthySkus,
                        lowCount = analytics.lowStockCount,
                        outCount = analytics.outOfStockCount
                    )

                    if (analytics.categoryBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Category Breakdown",
                            style = MaterialTheme.typography.labelLarge
                        )
                        val maxCatVal = analytics.categoryBreakdown.maxOf { it.retailValueCents }.coerceAtLeast(1L)
                        analytics.categoryBreakdown.take(4).forEach { cat ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${cat.category} (${cat.skuCount} SKUs)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = if (canSeeFinancials) formatCurrency(cat.retailValueCents, currency) else "${cat.lowStockCount} alerts",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (cat.retailValueCents.toFloat() / maxCatVal.toFloat()).coerceIn(0.05f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity & Quick Audit Export
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Shop Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = onOpenReports,
                            modifier = Modifier.testTag("dashboard_export_pdf_button")
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF Reports")
                        }
                    }

                    if (state.transactions.isEmpty()) {
                        Text(
                            text = "No stock movements yet. Scan a barcode or adjust inventory to log real-time analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.transactions.take(5).forEach { tx ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tx.type.label}: ${tx.productName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${tx.staffName} • ${formatTimestampShort(tx.timestamp)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val deltaText = if (tx.quantityDelta >= 0) "+${tx.quantityDelta}" else "${tx.quantityDelta}"
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$deltaText units",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (tx.quantityDelta >= 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (canSeeFinancials) {
                                        Text(
                                            text = formatCurrency(tx.totalAmountCents, currency),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
