package com.smallbiz.app.sync

import com.smallbiz.app.data.model.Expense
import com.smallbiz.app.data.model.Sale

/**
 * Stub sync manager. Replace with real Firebase implementation by following
 * the setup guide in README.md → "Remote Viewing Setup".
 *
 * When Firebase is configured:
 *  1. Add google-services.json to app/
 *  2. Add Firebase BOM + firebase-database-ktx to app/build.gradle
 *  3. Add 'com.google.gms.google-services' plugin
 *  4. Uncomment the real implementation below
 */
object FirebaseSyncManager {

    private var businessId: String = "default"
    var isEnabled: Boolean = false   // set to true once google-services.json is added

    fun init(businessName: String, adminPin: String) {
        businessId = (businessName + adminPin).hashCode().toString().replace("-", "n")
    }

    fun initWithId(id: String) {
        businessId = id
    }

    fun getBusinessId(): String = businessId

    fun pushSales(sales: List<Sale>) {
        if (!isEnabled) return
        // TODO: implement with Firebase after adding google-services.json
    }

    fun pushExpense(expense: Expense) {
        if (!isEnabled) return
        // TODO: implement with Firebase after adding google-services.json
    }

    fun pushBusinessInfo(name: String, address: String, currency: String) {
        if (!isEnabled) return
        // TODO: implement with Firebase after adding google-services.json
    }

    fun listenForSales(
        onData: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isEnabled) {
            onError("Remote sync not configured. See README for setup instructions.")
            return
        }
        // TODO: implement with Firebase after adding google-services.json
    }

    fun listenForExpenses(
        onData: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isEnabled) return
        // TODO: implement with Firebase after adding google-services.json
    }
}
