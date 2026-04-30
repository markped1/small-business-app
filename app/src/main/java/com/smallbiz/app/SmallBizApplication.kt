package com.smallbiz.app

import android.app.Application
import com.smallbiz.app.utils.PrefsManager

class SmallBizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Restore the admin's chosen currency into CurrencyFormatter at startup
        PrefsManager(this).applyStoredCurrency()
    }
}
