package com.example

import com.example.data.model.StaffRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun rbac_permissions_enforced_by_role() {
        assertTrue(StaffRole.OWNER.canManageStaff())
        assertTrue(StaffRole.OWNER.canDeleteInventory())
        assertTrue(StaffRole.OWNER.canExportFinancialAuditPdf())

        assertFalse(StaffRole.MANAGER.canManageStaff())
        assertFalse(StaffRole.MANAGER.canDeleteInventory())
        assertTrue(StaffRole.MANAGER.canEditInventoryPricing())
        assertTrue(StaffRole.MANAGER.canExportFinancialAuditPdf())

        assertFalse(StaffRole.CASHIER.canManageStaff())
        assertFalse(StaffRole.CASHIER.canEditInventoryPricing())
        assertFalse(StaffRole.CASHIER.canExportFinancialAuditPdf())
    }
}
