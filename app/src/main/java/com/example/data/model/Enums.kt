package com.example.data.model

enum class StaffRole(val displayName: String, val description: String) {
    OWNER(
        displayName = "Owner",
        description = "Full store vault, staff RBAC, encrypted cost & tax records, and financial audit PDFs"
    ),
    MANAGER(
        displayName = "Manager",
        description = "Inventory management, staff task assignment, stock audits, and reconciliation PDFs"
    ),
    CASHIER(
        displayName = "Staff / Cashier",
        description = "Assigned daily tasks, barcode sales scanning, and low-stock flagging"
    );

    fun canManageStaff(): Boolean = this == OWNER
    fun canViewSensitiveFinancials(): Boolean = this == OWNER || this == MANAGER
    fun canEditInventoryPricing(): Boolean = this == OWNER || this == MANAGER
    fun canDeleteInventory(): Boolean = this == OWNER
    fun canExportFinancialAuditPdf(): Boolean = this == OWNER || this == MANAGER
    fun canAssignTasks(): Boolean = this == OWNER || this == MANAGER

    companion object {
        fun fromName(raw: String): StaffRole =
            entries.firstOrNull { it.name == raw } ?: CASHIER
    }
}

enum class TaskPriority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        fun fromName(raw: String): TaskPriority =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class TaskCategory(val label: String) {
    OPENING("Opening"),
    RESTOCK("Restock"),
    AUDIT("Audit"),
    CUSTOMER("Floor"),
    CLOSING("Closing");

    companion object {
        fun fromName(raw: String): TaskCategory =
            entries.firstOrNull { it.name == raw } ?: RESTOCK
    }
}

enum class WorkShift(val label: String) {
    MORNING("Morning"),
    MIDDAY("Midday"),
    EVENING("Evening");

    companion object {
        fun fromName(raw: String): WorkShift =
            entries.firstOrNull { it.name == raw } ?: MORNING
    }
}

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    companion object {
        fun fromName(raw: String): TaskStatus =
            entries.firstOrNull { it.name == raw } ?: TODO
    }
}

enum class StockTxType(val label: String) {
    SALE("Sale"),
    RESTOCK("Restock"),
    AUDIT_ADJUSTMENT("Audit"),
    WASTE("Shrinkage");

    companion object {
        fun fromName(raw: String): StockTxType =
            entries.firstOrNull { it.name == raw } ?: SALE
    }
}

enum class AlertSeverity(val label: String) {
    OUT_OF_STOCK("Out of Stock"),
    CRITICAL("Critical"),
    LOW("Low Stock");

    companion object {
        fun fromName(raw: String): AlertSeverity =
            entries.firstOrNull { it.name == raw } ?: LOW
    }
}

enum class AlertStatus(val label: String) {
    ACTIVE("Active"),
    ACKNOWLEDGED("Acknowledged"),
    RESOLVED("Resolved");

    companion object {
        fun fromName(raw: String): AlertStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}
