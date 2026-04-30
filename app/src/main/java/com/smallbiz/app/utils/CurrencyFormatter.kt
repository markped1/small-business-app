package com.smallbiz.app.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    // Set once at app start (or whenever currency changes) via init()
    private var currencySymbol: String = "₦"
    private var currencyCode: String = "NGN"

    /**
     * Call this once in Application.onCreate() or after the admin saves settings.
     * [localeTag] is a BCP-47 locale tag stored in prefs, e.g. "en-NG" for Nigeria.
     * [code] is the ISO 4217 currency code, e.g. "NGN".
     */
    fun init(code: String) {
        currencyCode = code
        currencySymbol = try {
            Currency.getInstance(code).symbol
        } catch (e: Exception) {
            code
        }
    }

    fun format(amount: Double): String {
        return try {
            val nf = NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
            "$currencySymbol${nf.format(amount)}"
        } catch (e: Exception) {
            "$currencySymbol${"%.2f".format(amount)}"
        }
    }

    fun getCurrencyCode(): String = currencyCode
    fun getCurrencySymbol(): String = currencySymbol
}
