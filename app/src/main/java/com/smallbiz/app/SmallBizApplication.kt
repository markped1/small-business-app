package com.smallbiz.app

import android.app.Application
import com.smallbiz.app.sync.FirebaseSyncManager
import com.smallbiz.app.utils.PrefsManager

class SmallBizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = PrefsManager(this)

        // Restore the admin's chosen currency into CurrencyFormatter at startup
        prefs.applyStoredCurrency()

        // Init Firebase sync with this business's ID
        if (prefs.isBusinessSetup()) {
            FirebaseSyncManager.init(prefs.getBusinessName(), prefs.getAdminPin())
        }

        // If this phone is a viewer connected to a remote business, restore that ID
        prefs.getRemoteBusinessId()?.let { remoteId ->
            if (!prefs.isBusinessSetup()) {
                FirebaseSyncManager.initWithId(remoteId)
            }
        }
    }
}
