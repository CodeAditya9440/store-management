package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StaffRole
import com.example.ui.state.DailyMetric
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.AmberTertiaryContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryContainer
import com.example.ui.theme.OnAlertRedContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(cents: Long, symbol: String = "₹"): String {
    val absVal = kotlin.math.abs(cents) / 100.0
    val sign = if (cents < 0) "-" else ""
    val formatted = if (symbol == "₹") {
        val formatter = java.text.DecimalFormat("#,##,##0.00", java.text.DecimalFormatSymbols(Locale.US))
        formatter.format(absVal)
    } else {
        String.format(Locale.US, "%.2f", absVal)
    }
    return "$sign$symbol$formatted"
}

fun formatTimestampShort(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    return SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun RoleBadge(role: StaffRole, modifier: Modifier = Modifier) {
    val (bg, fg) = when (role) {
        StaffRole.OWNER -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        StaffRole.MANAGER -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        StaffRole.CASHIER -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = role.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ConnectivityStatusPill(
    isOnline: Boolean,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val bg = if (isOnline) EmeraldPrimaryContainer.copy(alpha = 0.7f) else AmberTertiaryContainer
    val fg = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Surface(
        modifier = modifier,
        color = bg,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Rounded.CloudQueue else Icons.Rounded.CloudOff,
                contentDescription = if (isOnline) "Online" else "Offline mode",
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isOnline) {
                    if (pendingCount > 0) "Saved Locally ($pendingCount)" else "Online • Saved"
                } else {
                    "Offline Mode"
                },
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun KpiMetricCard(
    label: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EncryptedVaultBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Shop protection active — wholesale costs, tax ID & staff PINs are encrypted offline on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun RevenueProfitBarChart(
    series: List<DailyMetric>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val maxVal = (series.maxOfOrNull { it.revenueCents } ?: 0L).coerceAtLeast(100L)
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650),
        label = "chart_anim"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val profitColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-Day Sales & Profit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(color = primaryColor, label = "Revenue")
                LegendDot(color = profitColor, label = "Profit")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
        ) {
            val w = size.width
            val h = size.height
            val chartBottom = h - 8f
            val chartTop = 10f
            val usableHeight = chartBottom - chartTop

            // Dashed horizontal grid lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = chartBottom - usableHeight * frac
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
            }

            if (series.isEmpty()) return@Canvas
            val slotWidth = w / series.size.toFloat()
            val barWidth = (slotWidth * 0.28f).coerceAtLeast(10f)

            series.forEachIndexed { index, item ->
                val centerX = slotWidth * index + slotWidth / 2f
                val revHeight = (item.revenueCents.toFloat() / maxVal.toFloat()) * usableHeight * animProgress
                val profHeight = (item.profitCents.toFloat() / maxVal.toFloat()) * usableHeight * animProgress

                // Revenue bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(primaryColor, primaryColor.copy(alpha = 0.65f))
                    ),
                    topLeft = Offset(centerX - barWidth - 2f, chartBottom - revHeight.coerceAtLeast(4f)),
                    size = Size(barWidth, revHeight.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Profit bar
                drawRoundRect(
                    color = profitColor,
                    topLeft = Offset(centerX + 2f, chartBottom - profHeight.coerceAtLeast(4f)),
                    size = Size(barWidth, profHeight.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            series.forEach { metric ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = metric.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (metric.revenueCents > 0L) {
                        Text(
                            text = formatCurrency(metric.revenueCents, currency),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
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

@Composable
fun StockHealthDonut(
    healthyCount: Int,
    lowCount: Int,
    outCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (healthyCount + lowCount + outCount).coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(92.dp)) {
                val stroke = 14f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                val healthySweep = (healthyCount.toFloat() / total) * 360f
                val lowSweep = (lowCount.toFloat() / total) * 360f
                val outSweep = (outCount.toFloat() / total) * 360f

                var start = -90f
                if (healthySweep > 0f) {
                    drawArc(
                        color = EmeraldPrimary,
                        startAngle = start,
                        sweepAngle = healthySweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    start += healthySweep
                }
                if (lowSweep > 0f) {
                    drawArc(
                        color = AmberTertiary,
                        startAngle = start,
                        sweepAngle = lowSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    start += lowSweep
                }
                if (outSweep > 0f) {
                    drawArc(
                        color = AlertRed,
                        startAngle = start,
                        sweepAngle = outSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val pct = if (healthyCount + lowCount + outCount == 0) 100 else (healthyCount * 100) / total
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Healthy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HealthRow("In Stock", healthyCount, EmeraldPrimary, EmeraldPrimaryContainer)
            HealthRow("Low Stock", lowCount, AmberTertiary, AmberTertiaryContainer)
            HealthRow("Out of Stock", outCount, AlertRed, AlertRedContainer, OnAlertRedContainer)
        }
    }
}

@Composable
private fun HealthRow(
    label: String,
    count: Int,
    dotColor: Color,
    pillBg: Color,
    pillText: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Surface(color = pillBg, shape = RoundedCornerShape(6.dp)) {
            Text(
                text = "$count SKUs",
                style = MaterialTheme.typography.labelSmall,
                color = pillText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
