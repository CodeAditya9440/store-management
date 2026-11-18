package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StaffMemberEntity
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StockHealthDonut
import com.example.ui.components.formatCurrency
import com.example.ui.state.DailyMetric
import com.example.ui.state.ProductStockPerformance
import com.example.ui.state.StockPulseUiState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.OnAlertRedContainer
import kotlin.math.abs

enum class StockGraphTab(val label: String) {
    ALL("All Overview"),
    SALE("Sale"),
    PURCHASE("Purchase"),
    STOCK("Stock"),
    PROFIT_LOSS("Profit & Loss")
}

private val PurchaseBlue = Color(0xFF2563EB)
private val StockAmber = AmberTertiary
private val SaleEmerald = EmeraldPrimary
private val LossRed = AlertRed

@Composable
fun StockGraphsScreen(
    state: StockPulseUiState,
    activeStaff: StaffMemberEntity
) {
    val currency = state.storeConfig.currencySymbol.ifBlank { "₹" }
    val analytics = state.analytics
    val canSeeFinancials = activeStaff.role.canViewSensitiveFinancials()

    var selectedTab by remember { mutableStateOf(StockGraphTab.ALL) }
    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var selectedDayIndex by remember { mutableIntStateOf((analytics.dailySeries.size - 1).coerceAtLeast(0)) }

    val selectedProduct = remember(selectedProductId, analytics.productPerformance) {
        analytics.productPerformance.firstOrNull { it.productId == selectedProductId }
    }
    val healthySkus = (analytics.totalSkus - analytics.lowStockCount - analytics.outOfStockCount).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Sale, Purchase, Stock Value & Profit/Loss",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Top 2x2 Summary KPI Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        label = if (canSeeFinancials) "Total Sales" else "Total Units Sold",
                        value = if (canSeeFinancials) {
                            formatCurrency(analytics.totalRevenueCents, currency)
                        } else {
                            "${analytics.totalUnitsSold} units"
                        },
                        subtext = "${analytics.totalUnitsSold} units sold",
                        icon = if (currency == "₹") Icons.Rounded.CurrencyRupee else Icons.Rounded.TrendingUp,
                        accentColor = SaleEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = if (canSeeFinancials) "Total Purchase" else "Units Restocked",
                        value = if (canSeeFinancials) {
                            formatCurrency(analytics.totalPurchaseCents, currency)
                        } else {
                            "${analytics.totalUnitsPurchased} units"
                        },
                        subtext = "${analytics.totalUnitsPurchased} units purchased",
                        icon = Icons.Rounded.ShoppingBag,
                        accentColor = PurchaseBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiMetricCard(
                        label = if (canSeeFinancials) "Stock Value" else "Current Stock",
                        value = if (canSeeFinancials) {
                            formatCurrency(analytics.inventoryRetailCents, currency)
                        } else {
                            "${analytics.totalUnits} units"
                        },
                        subtext = if (canSeeFinancials) {
                            "Cost ${formatCurrency(analytics.inventoryCostCents, currency)} • ${analytics.totalUnits} units"
                        } else {
                            "${analytics.totalSkus} active products"
                        },
                        icon = Icons.Rounded.Inventory2,
                        accentColor = StockAmber,
                        modifier = Modifier.weight(1f)
                    )
                    if (canSeeFinancials) {
                        val isProfit = analytics.netProfitLossCents >= 0L
                        KpiMetricCard(
                            label = if (isProfit) "Net Profit" else "Net Loss",
                            value = formatCurrency(analytics.netProfitLossCents, currency),
                            subtext = if (analytics.totalWasteLossCents > 0L) {
                                "Waste ${formatCurrency(analytics.totalWasteLossCents, currency)}"
                            } else {
                                "Sales minus COGS"
                            },
                            icon = if (isProfit) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                            accentColor = if (isProfit) SaleEmerald else LossRed,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        KpiMetricCard(
                            label = "Low / Out Stock",
                            value = "${analytics.lowStockCount + analytics.outOfStockCount}",
                            subtext = "${analytics.outOfStockCount} out • ${analytics.lowStockCount} low",
                            icon = Icons.Rounded.BarChart,
                            accentColor = if (analytics.outOfStockCount > 0) LossRed else StockAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (!canSeeFinancials) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Showing unit movement graphs. Profit, loss, and purchase amounts are restricted to Managers & Owners.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Metric Graph Filter Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StockGraphTab.entries.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("graph_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        // Section 1: 7-Day Multi-Series / Focused Daily Chart
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
                    when (selectedTab) {
                        StockGraphTab.ALL -> {
                            DailyComparisonGraphCard(
                                series = analytics.dailySeries,
                                currency = currency,
                                canSeeFinancials = canSeeFinancials,
                                selectedIndex = selectedDayIndex,
                                onSelectDay = { selectedDayIndex = it }
                            )
                        }
                        StockGraphTab.SALE -> {
                            SingleSeriesTrendChart(
                                title = if (canSeeFinancials) "7-Day Sales Graph" else "7-Day Units Sold Graph",
                                series = analytics.dailySeries,
                                barColor = SaleEmerald,
                                currency = currency,
                                valueExtractor = { m ->
                                    if (canSeeFinancials) m.revenueCents else m.unitsSold.toLong()
                                },
                                isMoney = canSeeFinancials,
                                selectedIndex = selectedDayIndex,
                                onSelectDay = { selectedDayIndex = it },
                                subtitleProvider = { m -> "${m.unitsSold} units sold" }
                            )
                        }
                        StockGraphTab.PURCHASE -> {
                            SingleSeriesTrendChart(
                                title = if (canSeeFinancials) "7-Day Stock Purchase Graph" else "7-Day Units Purchased Graph",
                                series = analytics.dailySeries,
                                barColor = PurchaseBlue,
                                currency = currency,
                                valueExtractor = { m ->
                                    if (canSeeFinancials) m.purchaseCents else m.unitsPurchased.toLong()
                                },
                                isMoney = canSeeFinancials,
                                selectedIndex = selectedDayIndex,
                                onSelectDay = { selectedDayIndex = it },
                                subtitleProvider = { m -> "${m.unitsPurchased} units purchased" }
                            )
                        }
                        StockGraphTab.STOCK -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Current Stock Health",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                StockHealthDonut(
                                    healthyCount = healthySkus,
                                    lowCount = analytics.lowStockCount,
                                    outCount = analytics.outOfStockCount
                                )
                            }
                        }
                        StockGraphTab.PROFIT_LOSS -> {
                            if (canSeeFinancials) {
                                ProfitAndLossDivergingChart(
                                    series = analytics.dailySeries,
                                    currency = currency,
                                    selectedIndex = selectedDayIndex,
                                    onSelectDay = { selectedDayIndex = it }
                                )
                            } else {
                                SingleSeriesTrendChart(
                                    title = "7-Day Units Sold vs Restocked",
                                    series = analytics.dailySeries,
                                    barColor = SaleEmerald,
                                    currency = currency,
                                    valueExtractor = { it.unitsSold.toLong() },
                                    isMoney = false,
                                    selectedIndex = selectedDayIndex,
                                    onSelectDay = { selectedDayIndex = it },
                                    subtitleProvider = { m -> "Sold ${m.unitsSold} • Restocked ${m.unitsPurchased}" }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Overall Store Summary Bar Comparison (Sale vs Purchase vs Stock vs Profit/Loss)
        if (selectedTab == StockGraphTab.ALL || selectedTab == StockGraphTab.PROFIT_LOSS) {
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
                            text = if (canSeeFinancials) {
                                "Shop Totals: Sale, Purchase, Stock & P&L"
                            } else {
                                "Shop Unit Totals: Sold, Purchased & On Hand"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (canSeeFinancials) {
                            val items = listOf(
                                GraphBarEntry("Sale", analytics.totalRevenueCents, SaleEmerald),
                                GraphBarEntry("Purchase", analytics.totalPurchaseCents, PurchaseBlue),
                                GraphBarEntry("Stock Value", analytics.inventoryRetailCents, StockAmber),
                                GraphBarEntry(
                                    label = if (analytics.netProfitLossCents >= 0L) "Net Profit" else "Net Loss",
                                    value = analytics.netProfitLossCents,
                                    color = if (analytics.netProfitLossCents >= 0L) SaleEmerald else LossRed
                                )
                            )
                            HorizontalComparisonBarGraph(entries = items, currency = currency, isMoney = true)
                        } else {
                            val items = listOf(
                                GraphBarEntry("Sold", analytics.totalUnitsSold.toLong(), SaleEmerald),
                                GraphBarEntry("Purchased", analytics.totalUnitsPurchased.toLong(), PurchaseBlue),
                                GraphBarEntry("Current Stock", analytics.totalUnits.toLong(), StockAmber)
                            )
                            HorizontalComparisonBarGraph(entries = items, currency = currency, isMoney = false)
                        }
                    }
                }
            }
        }

        // Section 3: Interactive Product Selector for Per-Item Stock Graphs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Product-by-Product Graphs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (analytics.productPerformance.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedProductId == null,
                            onClick = { selectedProductId = null },
                            label = { Text("All Products") }
                        )
                        analytics.productPerformance.forEach { prod ->
                            FilterChip(
                                selected = selectedProductId == prod.productId,
                                onClick = {
                                    selectedProductId = if (selectedProductId == prod.productId) null else prod.productId
                                },
                                label = { Text(prod.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
        }

        if (selectedProduct != null) {
            item(key = "selected_${selectedProduct.productId}") {
                ProductGraphCard(
                    perf = selectedProduct,
                    currency = currency,
                    canSeeFinancials = canSeeFinancials,
                    mode = selectedTab
                )
            }
        } else if (analytics.productPerformance.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Add products or scan items to see Sale, Purchase, Stock, and Profit & Loss graphs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(analytics.productPerformance, key = { it.productId }) { perf ->
                ProductGraphCard(
                    perf = perf,
                    currency = currency,
                    canSeeFinancials = canSeeFinancials,
                    mode = selectedTab
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private data class GraphBarEntry(
    val label: String,
    val value: Long,
    val color: Color
)

@Composable
private fun DailyComparisonGraphCard(
    series: List<DailyMetric>,
    currency: String,
    canSeeFinancials: Boolean,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val safeIndex = selectedIndex.coerceIn(0, (series.size - 1).coerceAtLeast(0))
    val selected = series.getOrNull(safeIndex)
    val maxMagnitude = series.maxOfOrNull { m ->
        if (canSeeFinancials) {
            maxOf(m.revenueCents, m.purchaseCents, abs(m.netProfitLossCents))
        } else {
            maxOf(m.unitsSold.toLong(), m.unitsPurchased.toLong(), 1L)
        }
    }?.coerceAtLeast(1L) ?: 1L

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "multi_bar_anim"
    )
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-Day Sale, Purchase & P&L",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GraphLegendDot(SaleEmerald, "Sale")
            GraphLegendDot(PurchaseBlue, "Purchase")
            if (canSeeFinancials) {
                GraphLegendDot(AmberTertiary, "P&L")
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            val w = size.width
            val h = size.height
            val bottom = h - 8f
            val top = 12f
            val usableH = bottom - top

            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = bottom - usableH * frac
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.4f, pathEffect = dash)
            }

            if (series.isEmpty()) return@Canvas
            val slotW = w / series.size.toFloat()
            val barCount = if (canSeeFinancials) 3 else 2
            val barW = (slotW * 0.2f).coerceIn(8f, 22f)
            val gap = 4f
            val groupW = barCount * barW + (barCount - 1) * gap

            series.forEachIndexed { idx, m ->
                if (idx == safeIndex) {
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(slotW * idx + 3f, 2f),
                        size = Size(slotW - 6f, h - 4f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }

                val startX = slotW * idx + (slotW - groupW) / 2f
                val saleVal = if (canSeeFinancials) m.revenueCents else m.unitsSold.toLong()
                val purchVal = if (canSeeFinancials) m.purchaseCents else m.unitsPurchased.toLong()
                val plVal = abs(m.netProfitLossCents)

                val saleH = ((saleVal.toFloat() / maxMagnitude.toFloat()) * usableH * animProgress).coerceAtLeast(4f)
                val purchH = ((purchVal.toFloat() / maxMagnitude.toFloat()) * usableH * animProgress).coerceAtLeast(4f)

                drawRoundRect(
                    color = SaleEmerald,
                    topLeft = Offset(startX, bottom - saleH),
                    size = Size(barW, saleH),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                drawRoundRect(
                    color = PurchaseBlue,
                    topLeft = Offset(startX + barW + gap, bottom - purchH),
                    size = Size(barW, purchH),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                if (canSeeFinancials) {
                    val plH = ((plVal.toFloat() / maxMagnitude.toFloat()) * usableH * animProgress).coerceAtLeast(4f)
                    val plColor = if (m.netProfitLossCents < 0L) LossRed else AmberTertiary
                    drawRoundRect(
                        color = plColor,
                        topLeft = Offset(startX + (barW + gap) * 2f, bottom - plH),
                        size = Size(barW, plH),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                }
            }
        }

        // Interactive day selector strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            series.forEachIndexed { idx, m ->
                val isSelected = idx == safeIndex
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectDay(idx) }
                ) {
                    Text(
                        text = m.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (selected != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canSeeFinancials) {
                        Column {
                            Text("Sale (${selected.dayLabel})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(selected.revenueCents, currency), style = MaterialTheme.typography.labelLarge, color = SaleEmerald, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Purchase", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(selected.purchaseCents, currency), style = MaterialTheme.typography.labelLarge, color = PurchaseBlue, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val isProf = selected.netProfitLossCents >= 0L
                            Text(if (isProf) "Profit" else "Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formatCurrency(selected.netProfitLossCents, currency),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isProf) SaleEmerald else LossRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text("${selected.dayLabel}: ${selected.unitsSold} units sold", style = MaterialTheme.typography.labelMedium, color = SaleEmerald, fontWeight = FontWeight.Bold)
                        Text("${selected.unitsPurchased} units restocked", style = MaterialTheme.typography.labelMedium, color = PurchaseBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleSeriesTrendChart(
    title: String,
    series: List<DailyMetric>,
    barColor: Color,
    currency: String,
    valueExtractor: (DailyMetric) -> Long,
    isMoney: Boolean,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    subtitleProvider: (DailyMetric) -> String
) {
    val safeIndex = selectedIndex.coerceIn(0, (series.size - 1).coerceAtLeast(0))
    val selected = series.getOrNull(safeIndex)
    val maxVal = (series.maxOfOrNull { valueExtractor(it) } ?: 0L).coerceAtLeast(1L)
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "trend_anim"
    )
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
        ) {
            val w = size.width
            val h = size.height
            val bottom = h - 10f
            val top = 16f
            val usableH = bottom - top

            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = bottom - usableH * frac
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.4f, pathEffect = dash)
            }

            if (series.isEmpty()) return@Canvas
            val slotW = w / series.size.toFloat()
            val barW = (slotW * 0.42f).coerceIn(14f, 34f)
            val linePath = Path()
            val points = mutableListOf<Offset>()

            series.forEachIndexed { idx, m ->
                val valNum = valueExtractor(m).coerceAtLeast(0L)
                val barH = ((valNum.toFloat() / maxVal.toFloat()) * usableH * animProgress).coerceAtLeast(4f)
                val cx = slotW * idx + slotW / 2f
                val topY = bottom - barH
                points.add(Offset(cx, topY))

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            if (idx == safeIndex) barColor else barColor.copy(alpha = 0.72f),
                            barColor.copy(alpha = 0.3f)
                        )
                    ),
                    topLeft = Offset(cx - barW / 2f, topY),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            points.forEachIndexed { idx, pt ->
                if (idx == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = linePath,
                color = barColor,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            points.forEachIndexed { idx, pt ->
                drawCircle(
                    color = if (idx == safeIndex) barColor else Color.White,
                    radius = if (idx == safeIndex) 7f else 4.5f,
                    center = pt
                )
                drawCircle(
                    color = barColor,
                    radius = if (idx == safeIndex) 7f else 4.5f,
                    center = pt,
                    style = Stroke(width = 2.5f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            series.forEachIndexed { idx, m ->
                val isSelected = idx == safeIndex
                Surface(
                    color = if (isSelected) barColor else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectDay(idx) }
                ) {
                    Text(
                        text = m.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (selected != null) {
            val v = valueExtractor(selected)
            Surface(
                color = barColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selected.dayLabel} • ${subtitleProvider(selected)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isMoney) formatCurrency(v, currency) else "$v units",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfitAndLossDivergingChart(
    series: List<DailyMetric>,
    currency: String,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    val safeIndex = selectedIndex.coerceIn(0, (series.size - 1).coerceAtLeast(0))
    val selected = series.getOrNull(safeIndex)
    val maxAbs = (series.maxOfOrNull { abs(it.netProfitLossCents) } ?: 0L).coerceAtLeast(100L)
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "pl_anim"
    )
    val zeroLineColor = MaterialTheme.colorScheme.outline

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-Day Profit & Loss Graph",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GraphLegendDot(SaleEmerald, "Profit")
                GraphLegendDot(LossRed, "Loss")
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val maxBarH = (h / 2f) - 14f

            drawLine(
                color = zeroLineColor,
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 2f
            )

            if (series.isEmpty()) return@Canvas
            val slotW = w / series.size.toFloat()
            val barW = (slotW * 0.42f).coerceIn(14f, 34f)

            series.forEachIndexed { idx, m ->
                val pl = m.netProfitLossCents
                val cx = slotW * idx + slotW / 2f
                val magH = ((abs(pl).toFloat() / maxAbs.toFloat()) * maxBarH * animProgress).coerceAtLeast(4f)
                val barColor = if (pl >= 0L) SaleEmerald else LossRed
                val topY = if (pl >= 0L) midY - magH else midY

                drawRoundRect(
                    color = if (idx == safeIndex) barColor else barColor.copy(alpha = 0.78f),
                    topLeft = Offset(cx - barW / 2f, topY),
                    size = Size(barW, magH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            series.forEachIndexed { idx, m ->
                val isSelected = idx == safeIndex
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectDay(idx) }
                ) {
                    Text(
                        text = m.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (selected != null) {
            val isProfit = selected.netProfitLossCents >= 0L
            Surface(
                color = if (isProfit) EmeraldPrimaryContainer.copy(alpha = 0.55f) else AlertRedContainer.copy(alpha = 0.65f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selected.dayLabel} • Sales ${formatCurrency(selected.revenueCents, currency)} • Waste ${formatCurrency(selected.wasteCents, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isProfit) MaterialTheme.colorScheme.onPrimaryContainer else OnAlertRedContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = (if (isProfit) "+" else "") + formatCurrency(selected.netProfitLossCents, currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfit) SaleEmerald else OnAlertRedContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalComparisonBarGraph(
    entries: List<GraphBarEntry>,
    currency: String,
    isMoney: Boolean
) {
    val maxMag = (entries.maxOfOrNull { abs(it.value) } ?: 0L).coerceAtLeast(1L)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEach { entry ->
            val ratio = (abs(entry.value).toFloat() / maxMag.toFloat()).coerceIn(0.03f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isMoney) formatCurrency(entry.value, currency) else "${entry.value} units",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = entry.color
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    drawRoundRect(
                        color = trackColor,
                        size = size,
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = entry.color,
                        size = Size(size.width * ratio, size.height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductGraphCard(
    perf: ProductStockPerformance,
    currency: String,
    canSeeFinancials: Boolean,
    mode: StockGraphTab
) {
    val entries = remember(perf, canSeeFinancials, mode) {
        if (canSeeFinancials) {
            val all = listOf(
                GraphBarEntry("Sale", perf.salesCents, SaleEmerald),
                GraphBarEntry("Purchase", perf.purchaseCents, PurchaseBlue),
                GraphBarEntry("Stock Value", perf.stockRetailValueCents, StockAmber),
                GraphBarEntry(
                    label = if (perf.profitLossCents >= 0L) "Profit" else "Loss",
                    value = perf.profitLossCents,
                    color = if (perf.profitLossCents >= 0L) SaleEmerald else LossRed
                )
            )
            when (mode) {
                StockGraphTab.ALL -> all
                StockGraphTab.SALE -> listOf(all[0])
                StockGraphTab.PURCHASE -> listOf(all[1])
                StockGraphTab.STOCK -> listOf(all[2])
                StockGraphTab.PROFIT_LOSS -> listOf(all[3])
            }
        } else {
            val allUnits = listOf(
                GraphBarEntry("Sold", perf.unitsSold.toLong(), SaleEmerald),
                GraphBarEntry("Purchased", perf.unitsPurchased.toLong(), PurchaseBlue),
                GraphBarEntry("In Stock", perf.currentStockQty.toLong(), StockAmber)
            )
            when (mode) {
                StockGraphTab.ALL, StockGraphTab.PROFIT_LOSS -> allUnits
                StockGraphTab.SALE -> listOf(allUnits[0])
                StockGraphTab.PURCHASE -> listOf(allUnits[1])
                StockGraphTab.STOCK -> listOf(allUnits[2])
            }
        }
    }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = perf.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${perf.category} • In Stock: ${perf.currentStockQty} units (Min ${perf.lowStockThreshold})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canSeeFinancials) {
                    val isProfit = perf.profitLossCents >= 0L
                    Surface(
                        color = if (isProfit) EmeraldPrimaryContainer else AlertRedContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = (if (isProfit) "Profit " else "Loss ") + formatCurrency(perf.profitLossCents, currency),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) MaterialTheme.colorScheme.onPrimaryContainer else OnAlertRedContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalComparisonBarGraph(
                entries = entries,
                currency = currency,
                isMoney = canSeeFinancials
            )
        }
    }
}

@Composable
private fun GraphLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
