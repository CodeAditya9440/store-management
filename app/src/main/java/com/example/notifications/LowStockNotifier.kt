package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AlertSeverity

class LowStockNotifier(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "stockpulse_low_stock_alerts"
    }

    init {
        createChannelIfNeeded()
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_low_stock),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.notification_channel_low_stock_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun notifyLowStock(
        productId: String,
        productName: String,
        currentQty: Int,
        threshold: Int,
        severity: AlertSeverity
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val title = when (severity) {
            AlertSeverity.OUT_OF_STOCK -> "Out of Stock: $productName"
            AlertSeverity.CRITICAL -> "Critical Stock Alert: $productName"
            AlertSeverity.LOW -> "Low Stock Warning: $productName"
        }
        val body = if (currentQty <= 0) {
            "0 units remaining (Reorder threshold: $threshold). Immediate restock recommended."
        } else {
            "$currentQty units remaining (Threshold: $threshold). Schedule supplier reorder."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(productId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime; fail gracefully without crashing
        }
    }
}
