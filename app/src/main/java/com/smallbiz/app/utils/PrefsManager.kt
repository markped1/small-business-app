package com.smallbiz.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.smallbiz.app.data.model.StaffMember
import org.json.JSONArray
import org.json.JSONObject

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smallbiz_prefs", Context.MODE_PRIVATE)

    // ── Business info ─────────────────────────────────────────────────────────
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
    fun getBusinessName(): String  = prefs.getString(KEY_BUSINESS_NAME, "My Business") ?: "My Business"
    fun getBusinessAddress(): String = prefs.getString(KEY_BUSINESS_ADDRESS, "") ?: ""
    fun getAdminPin(): String      = prefs.getString(KEY_ADMIN_PIN, "1234") ?: "1234"
    fun getCurrencyCode(): String  = prefs.getString(KEY_CURRENCY_CODE, "NGN") ?: "NGN"
    fun verifyPin(pin: String): Boolean = pin == getAdminPin()
    fun verifyAdminPin(pin: String): Boolean = pin == getAdminPin()

    // ── Gmail backup ──────────────────────────────────────────────────────────
    fun saveAdminGmail(email: String) {
        prefs.edit().putString(KEY_ADMIN_GMAIL, email).apply()
    }

    fun getAdminGmail(): String = prefs.getString(KEY_ADMIN_GMAIL, "") ?: ""
    fun hasAdminGmail(): Boolean = getAdminGmail().isNotEmpty()

    // ── Named Staff (up to 5) ─────────────────────────────────────────────────

    fun saveStaffList(staffList: List<StaffMember>) {
        val array = JSONArray()
        staffList.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_STAFF_LIST, array.toString()).apply()
    }

    fun getStaffList(): List<StaffMember> {
        val json = prefs.getString(KEY_STAFF_LIST, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { StaffMember.fromJson(array.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addStaff(member: StaffMember): Boolean {
        val list = getStaffList().toMutableList()
        if (list.size >= StaffMember.MAX_STAFF) return false
        // Ensure PIN is unique
        if (list.any { it.pin == member.pin } || member.pin == getAdminPin()) return false
        list.add(member)
        saveStaffList(list)
        return true
    }

    fun updateStaff(member: StaffMember) {
        val list = getStaffList().toMutableList()
        val idx = list.indexOfFirst { it.id == member.id }
        if (idx >= 0) {
            list[idx] = member
            saveStaffList(list)
        }
    }

    fun removeStaff(staffId: String) {
        val list = getStaffList().filter { it.id != staffId }
        saveStaffList(list)
    }

    fun hasAnyStaff(): Boolean = getStaffList().isNotEmpty()

    // Legacy single staff PIN — kept for backward compat, delegates to list
    fun saveStaffPin(pin: String) {
        // No-op if staff list already has entries
        if (getStaffList().isEmpty() && pin.isNotEmpty()) {
            val member = StaffMember(
                id   = java.util.UUID.randomUUID().toString(),
                name = "Staff",
                pin  = pin
            )
            addStaff(member)
        }
    }

    fun getStaffPin(): String = getStaffList().firstOrNull()?.pin ?: ""
    fun hasStaffPin(): Boolean = hasAnyStaff()

    /**
     * Returns the role for a given PIN:
     *   "admin"        → admin PIN
     *   "staff:<name>" → matched staff member name
     *   null           → no match
     */
    fun getRoleForPin(pin: String): String? {
        if (verifyAdminPin(pin)) return "admin"
        val staff = getStaffList().find { it.pin == pin }
        return if (staff != null) "staff:${staff.name}" else null
    }

    fun getStaffNameForPin(pin: String): String? =
        getStaffList().find { it.pin == pin }?.name

    /** Call at app start to restore the saved currency into CurrencyFormatter. */
    fun applyStoredCurrency() {
        CurrencyFormatter.init(getCurrencyCode())
    }

    // ── Remote business ID ────────────────────────────────────────────────────
    fun saveRemoteBusinessId(id: String) {
        prefs.edit().putString(KEY_REMOTE_BIZ_ID, id).apply()
    }

    fun getRemoteBusinessId(): String? = prefs.getString(KEY_REMOTE_BIZ_ID, null)

    // ── Clock-out tracking ────────────────────────────────────────────────────
    fun recordClockOut(staffName: String = "Staff") {
        prefs.edit()
            .putLong(KEY_LAST_CLOCK_OUT, System.currentTimeMillis())
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
        private const val KEY_ADMIN_GMAIL        = "admin_gmail"
        private const val KEY_STAFF_LIST         = "staff_list"
        private const val KEY_IS_SETUP           = "is_setup"
        private const val KEY_CURRENCY_CODE      = "currency_code"
        private const val KEY_REMOTE_BIZ_ID      = "remote_biz_id"
        private const val KEY_LAST_CLOCK_OUT     = "last_clock_out"
        private const val KEY_LAST_CLOCK_OUT_BY  = "last_clock_out_by"
        private const val KEY_RESTOCK_ALERTS     = "restock_alerts"
    }
}
