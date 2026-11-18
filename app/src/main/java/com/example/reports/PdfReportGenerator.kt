package com.example.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.LowStockAlertEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StaffMemberEntity
import com.example.data.model.StockTransactionEntity
import com.example.data.model.StockTxType
import com.example.data.model.StoreTaskEntity
import com.example.data.model.TaskStatus
import com.example.data.preferences.StoreConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ReportType(val title: String, val subtitle: String, val filePrefix: String) {
    FINANCIAL_AUDIT(
        title = "Financial Audit Report",
        subtitle = "Revenue, COGS, Gross Margin & Transaction Ledger",
        filePrefix = "Store_Manager_Financial_Audit"
    ),
    INVENTORY_RECONCILIATION(
        title = "Inventory Reconciliation Sheet",
        subtitle = "System Stock Valuation, Low-Stock Flags & Physical Count Sheet",
        filePrefix = "Store_Manager_Inventory_Reconciliation"
    ),
    OPERATIONS_COMPLIANCE(
        title = "Daily Operations & Task Compliance",
        subtitle = "Shift Task Completion, Staff Accountability & Alert Log",
        filePrefix = "Store_Manager_Operations_Report"
    )
}

class PdfReportGenerator(private val context: Context) {
    private val pageWidth = 595 // A4 width at 72 dpi
    private val pageHeight = 842 // A4 height at 72 dpi
    private val margin = 36f
    private val contentWidth = pageWidth - margin * 2

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val shortDateFormatter = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    fun generateToCacheFile(
        type: ReportType,
        storeConfig: StoreConfig,
        auditor: StaffMemberEntity,
        products: List<ProductEntity>,
        transactions: List<StockTransactionEntity>,
        tasks: List<StoreTaskEntity>,
        alerts: List<LowStockAlertEntity>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(reportsDir, "${type.filePrefix}_$stamp.pdf")
        FileOutputStream(file).use { out ->
            writeReport(type, storeConfig, auditor, products, transactions, tasks, alerts, out)
        }
        return file
    }

    fun writeReport(
        type: ReportType,
        storeConfig: StoreConfig,
        auditor: StaffMemberEntity,
        products: List<ProductEntity>,
        transactions: List<StockTransactionEntity>,
        tasks: List<StoreTaskEntity>,
        alerts: List<LowStockAlertEntity>,
        outputStream: OutputStream
    ) {
        val pdf = PdfDocument()
        val auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
        val currency = storeConfig.currencySymbol.ifBlank { "₹" }

        try {
            when (type) {
                ReportType.FINANCIAL_AUDIT -> renderFinancialAudit(
                    pdf, storeConfig, auditor, auditId, currency, products, transactions
                )
                ReportType.INVENTORY_RECONCILIATION -> renderInventoryReconciliation(
                    pdf, storeConfig, auditor, auditId, currency, products
                )
                ReportType.OPERATIONS_COMPLIANCE -> renderOperationsReport(
                    pdf, storeConfig, auditor, auditId, tasks, alerts
                )
            }
            pdf.writeTo(outputStream)
        } finally {
            pdf.close()
        }
    }

    private fun renderFinancialAudit(
        pdf: PdfDocument,
        storeConfig: StoreConfig,
        auditor: StaffMemberEntity,
        auditId: String,
        currency: String,
        products: List<ProductEntity>,
        transactions: List<StockTransactionEntity>
    ) {
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = drawHeader(canvas, storeConfig, auditor, ReportType.FINANCIAL_AUDIT, auditId, pageNumber)

        val salesTx = transactions.filter { it.type == StockTxType.SALE }
        val restockTx = transactions.filter { it.type == StockTxType.RESTOCK }
        val wasteTx = transactions.filter { it.type == StockTxType.WASTE || it.type == StockTxType.AUDIT_ADJUSTMENT }

        val grossRevenueCents = salesTx.sumOf { it.totalAmountCents }
        val cogsCents = salesTx.sumOf { kotlin.math.abs(it.quantityDelta) * it.unitCostCents }
        val grossProfitCents = grossRevenueCents - cogsCents
        val marginPct = if (grossRevenueCents > 0) (grossProfitCents.toDouble() / grossRevenueCents) * 100.0 else 0.0
        val restockSpendCents = restockTx.sumOf { it.totalAmountCents }
        val inventoryCostValCents = products.sumOf { it.quantity.toLong() * it.costPriceCents }

        y = drawSectionTitle(canvas, "1. Executive Financial Summary", y)
        y = drawKpiGrid(
            canvas,
            y,
            listOf(
                "Gross Sales Revenue" to formatMoney(grossRevenueCents, currency),
                "Cost of Goods Sold" to formatMoney(cogsCents, currency),
                "Gross Profit (${String.format(Locale.US, "%.1f", marginPct)}%)" to formatMoney(grossProfitCents, currency),
                "Restock Procurement" to formatMoney(restockSpendCents, currency),
                "Stock Valuation (Cost)" to formatMoney(inventoryCostValCents, currency),
                "Shrinkage / Audit Entries" to "${wasteTx.size} events"
            )
        )

        y += 12f
        y = drawSectionTitle(canvas, "2. Audited Transaction Ledger (${transactions.size} Records)", y)
        y = drawTableHeader(
            canvas,
            y,
            listOf("Date" to 85f, "Item" to 165f, "Type" to 65f, "Qty" to 40f, "Amount" to 85f, "Staff" to 83f)
        )

        for (tx in transactions) {
            if (y > pageHeight - 75f) {
                drawFooter(canvas, auditId, pageNumber)
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = drawHeader(canvas, storeConfig, auditor, ReportType.FINANCIAL_AUDIT, auditId, pageNumber)
                y = drawTableHeader(
                    canvas,
                    y,
                    listOf("Date" to 85f, "Item" to 165f, "Type" to 65f, "Qty" to 40f, "Amount" to 85f, "Staff" to 83f)
                )
            }
            val qtyStr = if (tx.quantityDelta >= 0) "+${tx.quantityDelta}" else "${tx.quantityDelta}"
            y = drawTableRow(
                canvas,
                y,
                listOf(
                    shortDateFormatter.format(Date(tx.timestamp)) to 85f,
                    tx.productName.take(24) to 165f,
                    tx.type.label to 65f,
                    qtyStr to 40f,
                    formatMoney(tx.totalAmountCents, currency) to 85f,
                    tx.staffName.take(12) to 83f
                )
            )
        }

        if (transactions.isEmpty()) {
            y = drawEmptyNote(canvas, "No stock movement transactions recorded yet.", y)
        }

        y += 18f
        if (y > pageHeight - 95f) {
            drawFooter(canvas, auditId, pageNumber)
            pdf.finishPage(page)
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = drawHeader(canvas, storeConfig, auditor, ReportType.FINANCIAL_AUDIT, auditId, pageNumber)
        }
        drawAuditSignatureBlock(canvas, y, auditor, storeConfig.decryptedTaxId())
        drawFooter(canvas, auditId, pageNumber)
        pdf.finishPage(page)
    }

    private fun renderInventoryReconciliation(
        pdf: PdfDocument,
        storeConfig: StoreConfig,
        auditor: StaffMemberEntity,
        auditId: String,
        currency: String,
        products: List<ProductEntity>
    ) {
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = drawHeader(canvas, storeConfig, auditor, ReportType.INVENTORY_RECONCILIATION, auditId, pageNumber)

        val totalUnits = products.sumOf { it.quantity }
        val totalCostCents = products.sumOf { it.quantity.toLong() * it.costPriceCents }
        val totalRetailCents = products.sumOf { it.quantity.toLong() * it.retailPriceCents }
        val lowCount = products.count { it.isLowStock }

        y = drawSectionTitle(canvas, "1. Stock Valuation Overview", y)
        y = drawKpiGrid(
            canvas,
            y,
            listOf(
                "Total Active SKUs" to "${products.size}",
                "Total Units On Hand" to "$totalUnits",
                "Total Cost Valuation" to formatMoney(totalCostCents, currency),
                "Total Retail Valuation" to formatMoney(totalRetailCents, currency),
                "Low / Out-of-Stock SKUs" to "$lowCount",
                "Audit Verification" to "Physical Count Pending"
            )
        )

        y += 12f
        y = drawSectionTitle(canvas, "2. SKU Reconciliation Checklist", y)
        y = drawTableHeader(
            canvas,
            y,
            listOf("Barcode" to 95f, "Product Name" to 160f, "Sys Qty" to 50f, "Cost Val" to 85f, "Status" to 68f, "Phys Count" to 65f)
        )

        for (p in products) {
            if (y > pageHeight - 75f) {
                drawFooter(canvas, auditId, pageNumber)
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = drawHeader(canvas, storeConfig, auditor, ReportType.INVENTORY_RECONCILIATION, auditId, pageNumber)
                y = drawTableHeader(
                    canvas,
                    y,
                    listOf("Barcode" to 95f, "Product Name" to 160f, "Sys Qty" to 50f, "Cost Val" to 85f, "Status" to 68f, "Phys Count" to 65f)
                )
            }
            val status = when {
                p.isOutOfStock -> "OUT"
                p.isLowStock -> "LOW"
                else -> "OK"
            }
            y = drawTableRow(
                canvas,
                y,
                listOf(
                    p.barcode.take(14) to 95f,
                    p.name.take(24) to 160f,
                    "${p.quantity}" to 50f,
                    formatMoney(p.quantity.toLong() * p.costPriceCents, currency) to 85f,
                    status to 68f,
                    "[      ]" to 65f
                )
            )
        }

        if (products.isEmpty()) {
            y = drawEmptyNote(canvas, "No inventory SKUs found in store database.", y)
        }

        y += 18f
        if (y > pageHeight - 95f) {
            drawFooter(canvas, auditId, pageNumber)
            pdf.finishPage(page)
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = drawHeader(canvas, storeConfig, auditor, ReportType.INVENTORY_RECONCILIATION, auditId, pageNumber)
        }
        drawAuditSignatureBlock(canvas, y, auditor, storeConfig.decryptedTaxId())
        drawFooter(canvas, auditId, pageNumber)
        pdf.finishPage(page)
    }

    private fun renderOperationsReport(
        pdf: PdfDocument,
        storeConfig: StoreConfig,
        auditor: StaffMemberEntity,
        auditId: String,
        tasks: List<StoreTaskEntity>,
        alerts: List<LowStockAlertEntity>
    ) {
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = drawHeader(canvas, storeConfig, auditor, ReportType.OPERATIONS_COMPLIANCE, auditId, pageNumber)

        val doneCount = tasks.count { it.status == TaskStatus.DONE }
        val completionPct = if (tasks.isEmpty()) 100 else (doneCount * 100) / tasks.size
        val openAlerts = alerts.count { it.statusName != "RESOLVED" }

        y = drawSectionTitle(canvas, "1. Operations Compliance Summary", y)
        y = drawKpiGrid(
            canvas,
            y,
            listOf(
                "Total Shift Tasks" to "${tasks.size}",
                "Completed Tasks" to "$doneCount ($completionPct%)",
                "Open Low-Stock Alerts" to "$openAlerts",
                "Total Alert Events" to "${alerts.size}"
            )
        )

        y += 12f
        y = drawSectionTitle(canvas, "2. Daily Task Checklist", y)
        y = drawTableHeader(
            canvas,
            y,
            listOf("Task Title" to 210f, "Shift" to 70f, "Priority" to 65f, "Assignee" to 105f, "Status" to 73f)
        )

        for (t in tasks) {
            if (y > pageHeight - 75f) {
                drawFooter(canvas, auditId, pageNumber)
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = drawHeader(canvas, storeConfig, auditor, ReportType.OPERATIONS_COMPLIANCE, auditId, pageNumber)
                y = drawTableHeader(
                    canvas,
                    y,
                    listOf("Task Title" to 210f, "Shift" to 70f, "Priority" to 65f, "Assignee" to 105f, "Status" to 73f)
                )
            }
            y = drawTableRow(
                canvas,
                y,
                listOf(
                    t.title.take(32) to 210f,
                    t.shift.label to 70f,
                    t.priority.label to 65f,
                    t.assignedStaffName.take(15) to 105f,
                    t.status.label to 73f
                )
            )
        }

        if (tasks.isEmpty()) {
            y = drawEmptyNote(canvas, "No shift tasks recorded.", y)
        }

        drawFooter(canvas, auditId, pageNumber)
        pdf.finishPage(page)
    }

    private fun drawHeader(
        canvas: Canvas,
        storeConfig: StoreConfig,
        auditor: StaffMemberEntity,
        type: ReportType,
        auditId: String,
        pageNumber: Int
    ): Float {
        val bgPaint = Paint().apply { color = Color.rgb(13, 59, 46) }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 92f, bgPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.rgb(167, 243, 208)
            textSize = 11f
            isAntiAlias = true
        }
        val metaPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            textSize = 9.5f
            isAntiAlias = true
        }

        val storeTitle = storeConfig.storeName.ifBlank { "Store Manager" }
        canvas.drawText("$storeTitle — ${type.title}", margin, 32f, titlePaint)
        canvas.drawText(type.subtitle, margin, 50f, subPaint)
        canvas.drawText(
            "Audit ID: $auditId  |  Auditor: ${auditor.name} (${auditor.role.displayName})  |  Generated: ${dateFormatter.format(Date())}  |  Page $pageNumber",
            margin,
            72f,
            metaPaint
        )
        return 114f
    }

    private fun drawSectionTitle(canvas: Canvas, title: String, y: Float): Float {
        val paint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title, margin, y, paint)
        return y + 14f
    }

    private fun drawKpiGrid(canvas: Canvas, startY: Float, metrics: List<Pair<String, String>>): Float {
        val boxPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.rgb(13, 59, 46)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cols = 3
        val gap = 8f
        val boxWidth = (contentWidth - gap * (cols - 1)) / cols
        val boxHeight = 42f
        var y = startY

        metrics.chunked(cols).forEach { row ->
            row.forEachIndexed { idx, (label, value) ->
                val left = margin + idx * (boxWidth + gap)
                canvas.drawRect(left, y, left + boxWidth, y + boxHeight, boxPaint)
                canvas.drawRect(left, y, left + boxWidth, y + boxHeight, borderPaint)
                canvas.drawText(label, left + 8f, y + 15f, labelPaint)
                canvas.drawText(value, left + 8f, y + 32f, valuePaint)
            }
            y += boxHeight + gap
        }
        return y + 4f
    }

    private fun drawTableHeader(canvas: Canvas, y: Float, columns: List<Pair<String, Float>>): Float {
        val bg = Paint().apply { color = Color.rgb(226, 232, 240) }
        canvas.drawRect(margin, y, margin + contentWidth, y + 20f, bg)
        val textPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        var x = margin + 4f
        columns.forEach { (title, width) ->
            canvas.drawText(title, x, y + 14f, textPaint)
            x += width
        }
        return y + 24f
    }

    private fun drawTableRow(canvas: Canvas, y: Float, cells: List<Pair<String, Float>>): Float {
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.7f
        }
        var x = margin + 4f
        cells.forEach { (text, width) ->
            canvas.drawText(text, x, y + 10f, textPaint)
            x += width
        }
        canvas.drawLine(margin, y + 15f, margin + contentWidth, y + 15f, linePaint)
        return y + 18f
    }

    private fun drawEmptyNote(canvas: Canvas, note: String, y: Float): Float {
        val paint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText(note, margin + 6f, y + 14f, paint)
        return y + 24f
    }

    private fun drawAuditSignatureBlock(
        canvas: Canvas,
        y: Float,
        auditor: StaffMemberEntity,
        taxId: String
    ) {
        val boxPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val strokePaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            isAntiAlias = true
        }
        canvas.drawRect(margin, y, margin + contentWidth, y + 54f, boxPaint)
        canvas.drawRect(margin, y, margin + contentWidth, y + 54f, strokePaint)
        val maskedTax = if (taxId.isBlank()) "N/A" else "••••${taxId.takeLast(4)}"
        canvas.drawText(
            "Certified by: ${auditor.name} (${auditor.role.displayName})   |   Encrypted Tax Ref: $maskedTax",
            margin + 10f,
            y + 18f,
            textPaint
        )
        canvas.drawText(
            "Auditor Signature: ___________________________     Date: ________________",
            margin + 10f,
            y + 40f,
            textPaint
        )
    }

    private fun drawFooter(canvas: Canvas, auditId: String, pageNumber: Int) {
        val paint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            isAntiAlias = true
        }
        canvas.drawText(
            "Store Manager Encrypted Business Records  •  AES-256-GCM Protected  •  Ref: $auditId  •  Page $pageNumber",
            margin,
            pageHeight - 20f,
            paint
        )
    }

    private fun formatMoney(cents: Long, currency: String): String {
        val absVal = kotlin.math.abs(cents) / 100.0
        val sign = if (cents < 0) "-" else ""
        val formatted = if (currency == "₹") {
            java.text.DecimalFormat("#,##,##0.00", java.text.DecimalFormatSymbols(Locale.US)).format(absVal)
        } else {
            String.format(Locale.US, "%.2f", absVal)
        }
        return "$sign$currency$formatted"
    }
}
