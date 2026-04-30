package com.smallbiz.app.license

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * ToMega POS — Batch License Manager
 *
 * ── Key types ────────────────────────────────────────────────────────────────
 *
 * SINGLE key  (old style, still supported):
 *   TMPOS-XXXX-XXXX-XXXX-XXXX
 *   Tied to one specific business name. Offline validation.
 *
 * BATCH key (new):
 *   TMBAT-{batchId}-{seats}-{checksum}
 *   e.g.  TMBAT-A3F9-005-B2
 *   One key shared by up to N businesses.
 *   Each business sets their own name + PIN.
 *   Firebase tracks seat usage.
 *
 * ── Trial ────────────────────────────────────────────────────────────────────
 *   3 days from first install, fully functional, no key needed.
 *
 * ── Per-business isolation ───────────────────────────────────────────────────
 *   Each activated business has its own Room DB (same file, different data).
 *   The PIN is set during business setup — only people with the PIN can access
 *   the admin panel. Sales staff never need a PIN.
 */
object LicenseManager {

    private const val PREFS_NAME        = "tomega_license"
    private const val KEY_INSTALL_TIME  = "install_time"
    private const val KEY_LICENSE_KEY   = "license_key"
    private const val KEY_BIZ_NAME      = "licensed_biz"
    private const val KEY_ACTIVATED     = "activated"
    private const val KEY_LICENSE_TYPE  = "license_type"   // "single" | "batch"
    private const val KEY_BATCH_ID      = "batch_id"
    private const val KEY_SEATS         = "seats"
    private const val TRIAL_DAYS        = 3L

    // ── Secret — must match keygen.py ────────────────────────────────────────
    private const val SECRET = "ToMegaPOS@2026#Secure"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Device fingerprint (stable per device) ────────────────────────────────
    private fun deviceId(ctx: Context): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    // ── First-run install timestamp ───────────────────────────────────────────
    fun recordInstallIfNew(ctx: Context) {
        val p = prefs(ctx)
        if (!p.contains(KEY_INSTALL_TIME)) {
            p.edit().putLong(KEY_INSTALL_TIME, System.currentTimeMillis()).apply()
        }
    }

    // ── Trial ─────────────────────────────────────────────────────────────────
    fun getTrialDaysRemaining(ctx: Context): Long {
        val installTime = prefs(ctx).getLong(KEY_INSTALL_TIME, System.currentTimeMillis())
        val daysUsed = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installTime)
        return maxOf(0L, TRIAL_DAYS - daysUsed)
    }

    fun isTrialActive(ctx: Context): Boolean = getTrialDaysRemaining(ctx) > 0

    // ── License state ─────────────────────────────────────────────────────────
    fun isActivated(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ACTIVATED, false)

    fun isAppUsable(ctx: Context): Boolean = isActivated(ctx) || isTrialActive(ctx)

    fun getLicensedBusinessName(ctx: Context): String =
        prefs(ctx).getString(KEY_BIZ_NAME, "") ?: ""

    fun getSavedKey(ctx: Context): String =
        prefs(ctx).getString(KEY_LICENSE_KEY, "") ?: ""

    fun getLicenseType(ctx: Context): String =
        prefs(ctx).getString(KEY_LICENSE_TYPE, "single") ?: "single"

    fun getBatchSeats(ctx: Context): Int =
        prefs(ctx).getInt(KEY_SEATS, 0)

    // ── Key type detection ────────────────────────────────────────────────────
    fun isBatchKey(key: String): Boolean = key.trim().uppercase().startsWith("TMBAT-")

    fun isSingleKey(key: String): Boolean = key.trim().uppercase().startsWith("TMPOS-")

    // ═════════════════════════════════════════════════════════════════════════
    // SINGLE KEY ACTIVATION (offline)
    // ═════════════════════════════════════════════════════════════════════════
    fun activateSingle(ctx: Context, businessName: String, key: String): ActivationResult {
        val name = businessName.trim()
        val k    = key.trim().uppercase()
        if (name.isEmpty()) return ActivationResult.INVALID_NAME
        if (!isValidSingleFormat(k)) return ActivationResult.INVALID_FORMAT
        if (generateSingleKey(name) != k) return ActivationResult.INVALID_KEY

        prefs(ctx).edit()
            .putString(KEY_LICENSE_KEY, k)
            .putString(KEY_BIZ_NAME, name)
            .putString(KEY_LICENSE_TYPE, "single")
            .putBoolean(KEY_ACTIVATED, true)
            .apply()
        return ActivationResult.SUCCESS
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BATCH KEY ACTIVATION (requires internet — checks Firebase seat count)
    // ═════════════════════════════════════════════════════════════════════════
    fun activateBatch(
        ctx: Context,
        businessName: String,
        key: String,
        onResult: (ActivationResult) -> Unit
    ) {
        val name = businessName.trim()
        val k    = key.trim().uppercase()

        if (name.isEmpty()) { onResult(ActivationResult.INVALID_NAME); return }
        if (!isValidBatchFormat(k)) { onResult(ActivationResult.INVALID_FORMAT); return }

        // Parse batch key: TMBAT-{batchId}-{seats3digits}-{checksum2}
        val parts = k.split("-")
        if (parts.size != 4) { onResult(ActivationResult.INVALID_FORMAT); return }

        val batchId  = parts[1]
        val seats    = parts[2].toIntOrNull() ?: 0
        val checksum = parts[3]

        // Verify checksum
        val expectedCheck = batchChecksum(batchId, seats)
        if (checksum != expectedCheck) { onResult(ActivationResult.INVALID_KEY); return }

        val deviceId = deviceId(ctx)
        val db = FirebaseDatabase.getInstance()
        val batchRef = db.getReference("batches/$batchId")

        batchRef.get().addOnSuccessListener { snapshot ->
            val usedCount = (snapshot.child("used").value as? Long)?.toInt() ?: 0
            val maxSeats  = (snapshot.child("seats").value as? Long)?.toInt() ?: seats

            // Check if this device already activated with this batch
            val activations = snapshot.child("activations")
            var alreadyActivated = false
            for (child in activations.children) {
                if (child.child("deviceId").value == deviceId) {
                    alreadyActivated = true
                    break
                }
            }

            when {
                alreadyActivated -> {
                    // Re-activation on same device — just restore locally
                    saveActivation(ctx, name, k, "batch", batchId, maxSeats)
                    onResult(ActivationResult.SUCCESS)
                }
                usedCount >= maxSeats -> {
                    onResult(ActivationResult.SEATS_EXHAUSTED)
                }
                else -> {
                    // Register this activation in Firebase
                    val newUsed = usedCount + 1
                    val activationKey = batchRef.child("activations").push().key ?: deviceId
                    batchRef.child("used").setValue(newUsed)
                    batchRef.child("seats").setValue(maxSeats)
                    batchRef.child("activations").child(activationKey).setValue(
                        mapOf(
                            "deviceId"     to deviceId,
                            "businessName" to name,
                            "activatedAt"  to System.currentTimeMillis()
                        )
                    ).addOnSuccessListener {
                        saveActivation(ctx, name, k, "batch", batchId, maxSeats)
                        onResult(ActivationResult.SUCCESS)
                    }.addOnFailureListener {
                        onResult(ActivationResult.NETWORK_ERROR)
                    }
                }
            }
        }.addOnFailureListener {
            // Firebase not reachable — if key format is valid, allow offline grace
            onResult(ActivationResult.NETWORK_ERROR)
        }
    }

    private fun saveActivation(
        ctx: Context, name: String, key: String,
        type: String, batchId: String, seats: Int
    ) {
        prefs(ctx).edit()
            .putString(KEY_LICENSE_KEY, key)
            .putString(KEY_BIZ_NAME, name)
            .putString(KEY_LICENSE_TYPE, type)
            .putString(KEY_BATCH_ID, batchId)
            .putInt(KEY_SEATS, seats)
            .putBoolean(KEY_ACTIVATED, true)
            .apply()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // KEY GENERATION (used by keygen.py — mirrored here for reference)
    // ═════════════════════════════════════════════════════════════════════════

    /** Generate a single-business key tied to a business name */
    fun generateSingleKey(businessName: String): String {
        val input = "${businessName.trim().uppercase()}|$SECRET"
        val hex   = sha256(input).take(16).uppercase()
        return "TMPOS-${hex.substring(0,4)}-${hex.substring(4,8)}-${hex.substring(8,12)}-${hex.substring(12,16)}"
    }

    /** Generate a batch key for N seats. batchId is a random 4-char hex string. */
    fun generateBatchKey(batchId: String, seats: Int): String {
        val check = batchChecksum(batchId.uppercase(), seats)
        val seatsStr = seats.toString().padStart(3, '0')
        return "TMBAT-${batchId.uppercase()}-$seatsStr-$check"
    }

    /** 2-char checksum for batch key integrity */
    private fun batchChecksum(batchId: String, seats: Int): String {
        val input = "${batchId.uppercase()}|${seats}|$SECRET"
        return sha256(input).take(2).uppercase()
    }

    // ── Format validators ─────────────────────────────────────────────────────
    private fun isValidSingleFormat(key: String): Boolean =
        Regex("^TMPOS-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}$").matches(key)

    private fun isValidBatchFormat(key: String): Boolean =
        Regex("^TMBAT-[A-F0-9]{4}-\\d{3}-[A-F0-9]{2}$").matches(key)

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ── Unified activate entry point ──────────────────────────────────────────
    fun activate(
        ctx: Context,
        businessName: String,
        key: String,
        onResult: (ActivationResult) -> Unit
    ) {
        val k = key.trim().uppercase()
        when {
            isBatchKey(k)  -> activateBatch(ctx, businessName, k, onResult)
            isSingleKey(k) -> onResult(activateSingle(ctx, businessName, k))
            else           -> onResult(ActivationResult.INVALID_FORMAT)
        }
    }

    enum class ActivationResult {
        SUCCESS,
        INVALID_NAME,
        INVALID_FORMAT,
        INVALID_KEY,
        SEATS_EXHAUSTED,
        NETWORK_ERROR
    }
}
