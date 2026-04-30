package com.smallbiz.app

import android.app.Application
import com.smallbiz.app.analytics.BusinessAnalytics
import com.smallbiz.app.sync.FirebaseSyncManager
import com.smallbiz.app.utils.PrefsManager

class SmallBizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = PrefsManager(this)

        // Restore the admin's chosen currency into CurrencyFormatter at startup
        prefs.applyStoredCurrency()

        if (prefs.isBusinessSetup()) {
            // Init Firebase sync
            FirebaseSyncManager.init(prefs.getBusinessName(), prefs.getAdminPin())

            // Analytics heartbeat — updates lastSeen so developer can track active businesses
            BusinessAnalytics.heartbeat(this, prefs.getBusinessName())
        }

        // If this phone is a viewer connected to a remote business, restore that ID
        prefs.getRemoteBusinessId()?.let { remoteId ->
            if (!prefs.isBusinessSetup()) {
                FirebaseSyncManager.initWithId(remoteId)
            }
        }
    }
}
