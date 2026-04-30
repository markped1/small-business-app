package com.smallbiz.app.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

    fun format(amount: Double): String {
        return try {
            formatter.format(amount)
        } catch (e: Exception) {
            "%.2f".format(amount)
        }
    }
}
