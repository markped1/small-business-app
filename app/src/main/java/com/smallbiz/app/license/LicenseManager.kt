package com.smallbiz.app.license

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * ToMega POS License Manager
 *
 * Trial:   3 days from first install, fully functional
 * License: Perpetual per-device key tied to a business name
 *
 * Key format:  TMPOS-XXXX-XXXX-XXXX-XXXX
 * Generation:  SHA-256(businessName + secret + deviceSalt) → formatted hex
 *
 * The secret is embedded here. For production, move key generation to a
 * server so the secret is never in the APK.
 */
object LicenseManager {

    private const val PREFS_NAME       = "tomega_license"
    private const val KEY_INSTALL_TIME = "install_time"
    private const val KEY_LICENSE_KEY  = "license_key"
    private const val KEY_BIZ_NAME     = "licensed_biz"
    private const val KEY_ACTIVATED    = "activated"
    private const val TRIAL_DAYS       = 3L

    // ── Internal secret — change this before distributing ────────────────────
    private const val SECRET = "ToMegaPOS@2026#Secure"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── First-run: record install timestamp ───────────────────────────────────
    fun recordInstallIfNew(ctx: Context) {
        val p = prefs(ctx)
        if (!p.contains(KEY_INSTALL_TIME)) {
            p.edit().putLong(KEY_INSTALL_TIME, System.currentTimeMillis()).apply()
        }
    }

    // ── Trial status ──────────────────────────────────────────────────────────
    fun getTrialDaysRemaining(ctx: Context): Long {
        val installTime = prefs(ctx).getLong(KEY_INSTALL_TIME, System.currentTimeMillis())
        val elapsed = System.currentTimeMillis() - installTime
        val daysUsed = TimeUnit.MILLISECONDS.toDays(elapsed)
        return maxOf(0L, TRIAL_DAYS - daysUsed)
    }

    fun isTrialActive(ctx: Context): Boolean = getTrialDaysRemaining(ctx) > 0

    // ── License check ─────────────────────────────────────────────────────────
    fun isActivated(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ACTIVATED, false)

    fun isAppUsable(ctx: Context): Boolean = isActivated(ctx) || isTrialActive(ctx)

    fun getLicensedBusinessName(ctx: Context): String =
        prefs(ctx).getString(KEY_BIZ_NAME, "") ?: ""

    fun getSavedKey(ctx: Context): String =
        prefs(ctx).getString(KEY_LICENSE_KEY, "") ?: ""

    // ── Activate with a key ───────────────────────────────────────────────────
    fun activate(ctx: Context, businessName: String, key: String): ActivationResult {
        val trimmedName = businessName.trim()
        val trimmedKey  = key.trim().uppercase()

        if (trimmedName.isEmpty()) return ActivationResult.INVALID_NAME
        if (!isValidFormat(trimmedKey)) return ActivationResult.INVALID_FORMAT

        val expected = generateKey(trimmedName)
        return if (expected == trimmedKey) {
            prefs(ctx).edit()
                .putString(KEY_LICENSE_KEY, trimmedKey)
                .putString(KEY_BIZ_NAME, trimmedName)
                .putBoolean(KEY_ACTIVATED, true)
                .apply()
            ActivationResult.SUCCESS
        } else {
            ActivationResult.INVALID_KEY
        }
    }

    // ── Key generation (run this on your PC/server to create keys) ────────────
    fun generateKey(businessName: String): String {
        val input = "${businessName.trim().uppercase()}|$SECRET"
        val hash  = sha256(input)
        // Take 16 hex chars, split into 4 groups of 4
        val hex = hash.take(16).uppercase()
        return "TMPOS-${hex.substring(0,4)}-${hex.substring(4,8)}-${hex.substring(8,12)}-${hex.substring(12,16)}"
    }

    private fun isValidFormat(key: String): Boolean {
        val regex = Regex("^TMPOS-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}$")
        return regex.matches(key)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    enum class ActivationResult {
        SUCCESS,
        INVALID_NAME,
        INVALID_FORMAT,
        INVALID_KEY
    }
}
