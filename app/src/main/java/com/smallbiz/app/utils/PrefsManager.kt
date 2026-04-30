package com.smallbiz.app.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smallbiz_prefs", Context.MODE_PRIVATE)

    fun saveBusinessInfo(name: String, address: String, pin: String, currencyCode: String) {
        prefs.edit()
            .putString(KEY_BUSINESS_NAME, name)
            .putString(KEY_BUSINESS_ADDRESS, address)
            .putString(KEY_ADMIN_PIN, pin)
            .putString(KEY_CURRENCY_CODE, currencyCode)
            .putBoolean(KEY_IS_SETUP, true)
            .apply()
        CurrencyFormatter.init(currencyCode)
    }

    fun isBusinessSetup(): Boolean = prefs.getBoolean(KEY_IS_SETUP, false)
    fun getBusinessName(): String = prefs.getString(KEY_BUSINESS_NAME, "My Business") ?: "My Business"
    fun getBusinessAddress(): String = prefs.getString(KEY_BUSINESS_ADDRESS, "") ?: ""
    fun getAdminPin(): String = prefs.getString(KEY_ADMIN_PIN, "1234") ?: "1234"
    fun getCurrencyCode(): String = prefs.getString(KEY_CURRENCY_CODE, "NGN") ?: "NGN"
    fun verifyPin(pin: String): Boolean = pin == getAdminPin()
    fun verifyAdminPin(pin: String): Boolean = pin == getAdminPin()

    // ── Staff PIN ─────────────────────────────────────────────────────────────
    fun saveStaffPin(pin: String) {
        prefs.edit().putString(KEY_STAFF_PIN, pin).apply()
    }

    fun getStaffPin(): String = prefs.getString(KEY_STAFF_PIN, "") ?: ""

    fun hasStaffPin(): Boolean = getStaffPin().isNotEmpty()

    fun verifyStaffPin(pin: String): Boolean = pin == getStaffPin()

    /** Returns true if pin matches either admin or staff PIN */
    fun verifyAnyPin(pin: String): Boolean = verifyAdminPin(pin) || verifyStaffPin(pin)

    /** Returns the role for a given PIN: "admin", "staff", or null if invalid */
    fun getRoleForPin(pin: String): String? = when {
        verifyAdminPin(pin) -> "admin"
        hasStaffPin() && verifyStaffPin(pin) -> "staff"
        else -> null
    }

    /** Call at app start to restore the saved currency into CurrencyFormatter. */
    fun applyStoredCurrency() {
        CurrencyFormatter.init(getCurrencyCode())
    }

    fun saveRemoteBusinessId(id: String) {
        prefs.edit().putString(KEY_REMOTE_BIZ_ID, id).apply()
    }

    fun getRemoteBusinessId(): String? = prefs.getString(KEY_REMOTE_BIZ_ID, null)

    // ── Clock-out tracking ────────────────────────────────────────────────────
    fun recordClockOut(staffName: String = "Staff") {
        val ts = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_CLOCK_OUT, ts)
            .putString(KEY_LAST_CLOCK_OUT_BY, staffName)
            .apply()
    }

    fun getLastClockOut(): Long = prefs.getLong(KEY_LAST_CLOCK_OUT, 0L)
    fun getLastClockOutBy(): String = prefs.getString(KEY_LAST_CLOCK_OUT_BY, "") ?: ""

    // ── Restock alerts ────────────────────────────────────────────────────────
    fun saveRestockAlert(productName: String) {
        val existing = getRestockAlerts().toMutableSet()
        existing.add(productName)
        prefs.edit().putStringSet(KEY_RESTOCK_ALERTS, existing).apply()
    }

    fun getRestockAlerts(): Set<String> =
        prefs.getStringSet(KEY_RESTOCK_ALERTS, emptySet()) ?: emptySet()

    fun clearRestockAlert(productName: String) {
        val existing = getRestockAlerts().toMutableSet()
        existing.remove(productName)
        prefs.edit().putStringSet(KEY_RESTOCK_ALERTS, existing).apply()
    }

    companion object {
        private const val KEY_BUSINESS_NAME      = "business_name"
        private const val KEY_BUSINESS_ADDRESS   = "business_address"
        private const val KEY_ADMIN_PIN          = "admin_pin"
        private const val KEY_STAFF_PIN          = "staff_pin"
        private const val KEY_IS_SETUP           = "is_setup"
        private const val KEY_CURRENCY_CODE      = "currency_code"
        private const val KEY_REMOTE_BIZ_ID      = "remote_biz_id"
        private const val KEY_LAST_CLOCK_OUT     = "last_clock_out"
        private const val KEY_LAST_CLOCK_OUT_BY  = "last_clock_out_by"
        private const val KEY_RESTOCK_ALERTS     = "restock_alerts"
    }
}
