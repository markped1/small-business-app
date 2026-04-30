package com.smallbiz.app.analytics

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.smallbiz.app.BuildConfig

/**
 * ToMega POS — Business Analytics
 *
 * Tracks (anonymously) how many businesses are using the app.
 * Data stored in Firebase under:
 *
 *   analytics/
 *     businesses/
 *       {deviceId}/
 *         businessName: "Mama Ngozi Store"
 *         adminEmail:   "admin@gmail.com"  (only if provided)
 *         appVersion:   "1.0"
 *         firstSeen:    1234567890
 *         lastSeen:     1234567890
 *         platform:     "android"
 *
 * This lets the developer see:
 *   - Total number of active businesses
 *   - App version distribution
 *   - When businesses first installed and last used the app
 *
 * No sales data, no PINs, no financial data is ever sent here.
 */
object BusinessAnalytics {

    private const val TAG = "ToMegaAnalytics"

    fun registerBusiness(ctx: Context, businessName: String, adminEmail: String = "") {
        try {
            val deviceId = Settings.Secure.getString(
                ctx.contentResolver, Settings.Secure.ANDROID_ID
            ) ?: return

            val db  = FirebaseDatabase.getInstance()
            val ref = db.getReference("analytics/businesses/$deviceId")

            ref.get().addOnSuccessListener { snapshot ->
                val firstSeen = snapshot.child("firstSeen").getValue(Long::class.java)
                    ?: System.currentTimeMillis()

                val data = mutableMapOf<String, Any>(
                    "businessName" to businessName,
                    "appVersion"   to BuildConfig.VERSION_NAME,
                    "versionCode"  to BuildConfig.VERSION_CODE,
                    "firstSeen"    to firstSeen,
                    "lastSeen"     to System.currentTimeMillis(),
                    "platform"     to "android"
                )
                if (adminEmail.isNotEmpty()) data["adminEmail"] = adminEmail

                ref.setValue(data)
                    .addOnSuccessListener { Log.d(TAG, "Business registered: $businessName") }
                    .addOnFailureListener { Log.w(TAG, "Analytics write failed: ${it.message}") }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Analytics error: ${e.message}")
            // Silent failure — never block the user
        }
    }

    /** Call on every app launch to update lastSeen timestamp */
    fun heartbeat(ctx: Context, businessName: String) {
        try {
            val deviceId = Settings.Secure.getString(
                ctx.contentResolver, Settings.Secure.ANDROID_ID
            ) ?: return

            val db  = FirebaseDatabase.getInstance()
            val ref = db.getReference("analytics/businesses/$deviceId")

            ref.updateChildren(mapOf(
                "lastSeen"    to System.currentTimeMillis(),
                "businessName" to businessName,
                "appVersion"  to BuildConfig.VERSION_NAME,
                "versionCode" to BuildConfig.VERSION_CODE
            ))
        } catch (e: Exception) {
            // Silent
        }
    }
}
