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
        // Update formatter immediately
        CurrencyFormatter.init(currencyCode)
    }

    fun isBusinessSetup(): Boolean = prefs.getBoolean(KEY_IS_SETUP, false)

    fun getBusinessName(): String = prefs.getString(KEY_BUSINESS_NAME, "My Business") ?: "My Business"

    fun getBusinessAddress(): String = prefs.getString(KEY_BUSINESS_ADDRESS, "") ?: ""

    fun getAdminPin(): String = prefs.getString(KEY_ADMIN_PIN, "1234") ?: "1234"

    fun getCurrencyCode(): String = prefs.getString(KEY_CURRENCY_CODE, "NGN") ?: "NGN"

    fun verifyPin(pin: String): Boolean = pin == getAdminPin()

    /** Call at app start to restore the saved currency into CurrencyFormatter. */
    fun applyStoredCurrency() {
        CurrencyFormatter.init(getCurrencyCode())
    }

    companion object {
        private const val KEY_BUSINESS_NAME    = "business_name"
        private const val KEY_BUSINESS_ADDRESS = "business_address"
        private const val KEY_ADMIN_PIN        = "admin_pin"
        private const val KEY_IS_SETUP         = "is_setup"
        private const val KEY_CURRENCY_CODE    = "currency_code"
    }
}
