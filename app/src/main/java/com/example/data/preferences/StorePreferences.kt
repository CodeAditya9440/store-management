package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "stockpulse_store_prefs")

data class StoreConfig(
    val storeName: String = "",
    val currencySymbol: String = "₹",
    val encryptedTaxId: String = "",
    val autoCreateRestockTask: Boolean = true,
    val lowStockNotificationsEnabled: Boolean = true,
    val activeStaffId: String = "",
    val lastBackupTimestamp: Long = 0L,
    val isVaultConfigured: Boolean = false
) {
    fun decryptedTaxId(): String = CryptoManager.decrypt(encryptedTaxId)
}

class StorePreferences(private val context: Context) {
    private object Keys {
        val STORE_NAME = stringPreferencesKey("store_name")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val ENCRYPTED_TAX_ID = stringPreferencesKey("encrypted_tax_id")
        val AUTO_RESTOCK_TASK = booleanPreferencesKey("auto_restock_task")
        val LOW_STOCK_NOTIF = booleanPreferencesKey("low_stock_notif")
        val ACTIVE_STAFF_ID = stringPreferencesKey("active_staff_id")
        val LAST_BACKUP_TS = longPreferencesKey("last_backup_ts")
        val IS_CONFIGURED = booleanPreferencesKey("is_configured")
    }

    val configFlow: Flow<StoreConfig> = context.dataStore.data.map { prefs ->
        val storedCurrency = prefs[Keys.CURRENCY_SYMBOL]
        val normalizedCurrency = if (storedCurrency.isNullOrBlank() || storedCurrency == "$") "₹" else storedCurrency
        StoreConfig(
            storeName = prefs[Keys.STORE_NAME] ?: "",
            currencySymbol = normalizedCurrency,
            encryptedTaxId = prefs[Keys.ENCRYPTED_TAX_ID] ?: "",
            autoCreateRestockTask = prefs[Keys.AUTO_RESTOCK_TASK] ?: true,
            lowStockNotificationsEnabled = prefs[Keys.LOW_STOCK_NOTIF] ?: true,
            activeStaffId = prefs[Keys.ACTIVE_STAFF_ID] ?: "",
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TS] ?: 0L,
            isVaultConfigured = prefs[Keys.IS_CONFIGURED] ?: false
        )
    }

    suspend fun saveStoreSetup(
        storeName: String,
        currencySymbol: String,
        plainTaxId: String,
        activeStaffId: String
    ) {
        val encTax = CryptoManager.encrypt(plainTaxId.trim())
        context.dataStore.edit { prefs ->
            prefs[Keys.STORE_NAME] = storeName.trim()
            prefs[Keys.CURRENCY_SYMBOL] = currencySymbol.takeIf { it.isNotBlank() && it != "$" } ?: "₹"
            prefs[Keys.ENCRYPTED_TAX_ID] = encTax
            prefs[Keys.ACTIVE_STAFF_ID] = activeStaffId
            prefs[Keys.IS_CONFIGURED] = true
        }
    }

    suspend fun setActiveStaffId(staffId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_STAFF_ID] = staffId
        }
    }

    suspend fun updateAutomationSettings(autoRestockTask: Boolean, notificationsEnabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_RESTOCK_TASK] = autoRestockTask
            prefs[Keys.LOW_STOCK_NOTIF] = notificationsEnabled
        }
    }

    suspend fun updateTaxId(plainTaxId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENCRYPTED_TAX_ID] = CryptoManager.encrypt(plainTaxId.trim())
        }
    }

    suspend fun updateCurrencySymbol(currencySymbol: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_SYMBOL] = currencySymbol.takeIf { it.isNotBlank() && it != "$" } ?: "₹"
        }
    }

    suspend fun updateLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TS] = timestamp
        }
    }
}
