package com.smallbiz.app.update

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.smallbiz.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * ToMega POS — GitHub Releases Auto-Updater
 *
 * ── How it works ─────────────────────────────────────────────────────────────
 * 1. On app start, calls GitHub Releases API (no Firebase needed):
 *    GET https://api.github.com/repos/markped1/small-business-app/releases/latest
 *
 * 2. Parses the response:
 *    - tag_name  → version string (e.g. "v1.1" or "1.1")
 *    - assets[0].browser_download_url → direct APK download URL
 *    - body → release notes / update message
 *
 * 3. Compares tag version to BuildConfig.VERSION_NAME
 *    - If remote version > current → shows update dialog
 *    - User can tap "Update Now" or "Later"
 *
 * 4. Downloads APK silently via DownloadManager (shows notification)
 *
 * 5. When download completes → prompts to install
 *
 * ── How to release a new version ─────────────────────────────────────────────
 * 1. Bump versionCode and versionName in app/build.gradle
 *    e.g.  versionCode 2  /  versionName "1.1"
 *
 * 2. Build the APK:  ./gradlew assembleRelease  (or assembleDebug for testing)
 *
 * 3. Go to https://github.com/markped1/small-business-app/releases
 *    → "Draft a new release"
 *    → Tag: v1.1  (must match versionName with optional "v" prefix)
 *    → Title: "ToMega POS v1.1"
 *    → Description: what changed (shown to users in the update dialog)
 *    → Attach the APK file
 *    → Publish release
 *
 * 4. All installed apps will see the update on next launch.
 *
 * ── Version comparison ────────────────────────────────────────────────────────
 * Tags like "v1.2.3", "1.2.3", "v2.0" are all supported.
 * Comparison is done numerically per segment: 1.10 > 1.9
 */
object UpdateManager {

    private const val TAG = "ToMegaUpdate"
    private const val APK_FILE_NAME = "tomega_pos_update.apk"

    // GitHub Releases API — always returns the latest published release
    private const val RELEASES_API_URL =
        "https://api.github.com/repos/markped1/small-business-app/releases"

    private var downloadId: Long = -1L

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Call from SplashActivity.onStart().
     * Runs entirely in the background — only shows UI if an update is found.
     */
    fun checkForUpdate(activity: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val releaseInfo = fetchLatestRelease() ?: return@launch
                Log.d(TAG, "Latest release: ${releaseInfo.tagName}, APK: ${releaseInfo.apkUrl}")

                if (isNewerVersion(releaseInfo.tagName, BuildConfig.VERSION_NAME)) {
                    Log.d(TAG, "Update available: ${releaseInfo.tagName}")
                    withContext(Dispatchers.Main) {
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            showUpdateDialog(activity, releaseInfo)
                        }
                    }
                } else {
                    Log.d(TAG, "App is up to date (current: ${BuildConfig.VERSION_NAME})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed: ${e.message}")
                // Silent failure — app continues normally
            }
        }
    }

    // ── GitHub API fetch ──────────────────────────────────────────────────────

    private data class ReleaseInfo(
        val tagName: String,
        val releaseName: String,
        val apkUrl: String,
        val releaseNotes: String,
        val apkSize: Long
    )

    private fun fetchLatestRelease(): ReleaseInfo? {
        val url = URL(RELEASES_API_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "ToMegaPOS-Android/${BuildConfig.VERSION_NAME}")
            connectTimeout = 10_000
            readTimeout    = 10_000
        }

        return try {
            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GitHub API returned $responseCode")
                return null
            }

            val json = conn.inputStream.bufferedReader().readText()
            parseRelease(json)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseRelease(json: String): ReleaseInfo? {
        return try {
            // Releases endpoint returns an array — take the first (most recent)
            val array = JSONArray(json)
            if (array.length() == 0) return null

            val release = array.getJSONObject(0)
            val tagName      = release.optString("tag_name", "")
            val releaseName  = release.optString("name", tagName)
            val releaseNotes = release.optString("body", "Bug fixes and improvements")

            // Find the APK asset
            val assets = release.optJSONArray("assets") ?: return null
            var apkUrl  = ""
            var apkSize = 0L

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val contentType = asset.optString("content_type", "")
                val name        = asset.optString("name", "")
                if (contentType == "application/vnd.android.package-archive" ||
                    name.endsWith(".apk")
                ) {
                    apkUrl  = asset.optString("browser_download_url", "")
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }

            if (apkUrl.isEmpty()) {
                Log.w(TAG, "No APK asset found in release $tagName")
                return null
            }

            ReleaseInfo(tagName, releaseName, apkUrl, releaseNotes, apkSize)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse release JSON: ${e.message}")
            null
        }
    }

    // ── Version comparison ────────────────────────────────────────────────────

    /**
     * Returns true if [remote] is strictly newer than [current].
     * Strips leading "v" or "V" before comparing.
     * Compares numerically segment by segment: "1.10" > "1.9"
     * Falls back to string comparison if parsing fails.
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        val r = remote.trimStart('v', 'V').trim()
        val c = current.trimStart('v', 'V').trim()

        // If tags are identical, no update needed
        if (r == c) return false

        // Try numeric comparison
        return try {
            val rParts = r.split(".").map { it.toInt() }
            val cParts = c.split(".").map { it.toInt() }
            val maxLen = maxOf(rParts.size, cParts.size)

            for (i in 0 until maxLen) {
                val rv = rParts.getOrElse(i) { 0 }
                val cv = cParts.getOrElse(i) { 0 }
                if (rv > cv) return true
                if (rv < cv) return false
            }
            false // equal
        } catch (e: NumberFormatException) {
            // Non-numeric tag (e.g. "smallbusiness") — treat as newer if different
            // This handles the initial "smallbusiness" tag gracefully
            Log.d(TAG, "Non-numeric version tag '$r' — skipping update")
            false
        }
    }

    // ── Update dialog ─────────────────────────────────────────────────────────

    private fun showUpdateDialog(activity: Activity, info: ReleaseInfo) {
        val sizeMb = if (info.apkSize > 0)
            " (${String.format("%.1f", info.apkSize / 1_048_576.0)} MB)"
        else ""

        val notes = if (info.releaseNotes.isNotBlank() && info.releaseNotes != "null")
            "\n\n${info.releaseNotes.take(200)}"
        else ""

        android.app.AlertDialog.Builder(activity)
            .setTitle("🔄 Update Available — ${info.tagName}")
            .setMessage(
                "A new version of ToMega POS is ready.$notes\n\n" +
                "Download size: $sizeMb\n" +
                "The update installs automatically when downloaded."
            )
            .setPositiveButton("Update Now") { _, _ ->
                downloadUpdate(activity, info)
            }
            .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }

    // ── Download ──────────────────────────────────────────────────────────────

    private fun downloadUpdate(activity: Activity, info: ReleaseInfo) {
        try {
            // Clean up any previous download
            val destFile = File(
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_FILE_NAME
            )
            if (destFile.exists()) destFile.delete()

            val request = DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle("ToMega POS ${info.tagName}")
                .setDescription("Downloading update…")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationUri(Uri.fromFile(destFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)

            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)

            Log.d(TAG, "Download started: id=$downloadId url=${info.apkUrl}")

            // Register receiver for download completion
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        activity.unregisterReceiver(this)
                        verifyAndInstall(activity, destFile, dm, id)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                activity.registerReceiver(
                    receiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            android.widget.Toast.makeText(
                activity,
                "Download failed. Check your internet connection.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    // ── Verify download then install ──────────────────────────────────────────

    private fun verifyAndInstall(
        activity: Activity,
        apkFile: File,
        dm: DownloadManager,
        id: Long
    ) {
        // Check download status
        val query  = DownloadManager.Query().setFilterById(id)
        val cursor = dm.query(query)
        var success = false

        if (cursor.moveToFirst()) {
            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusCol >= 0 && cursor.getInt(statusCol) == DownloadManager.STATUS_SUCCESSFUL) {
                success = true
            }
        }
        cursor.close()

        if (success && apkFile.exists()) {
            activity.runOnUiThread { installUpdate(activity, apkFile) }
        } else {
            Log.e(TAG, "Download verification failed")
            activity.runOnUiThread {
                android.widget.Toast.makeText(
                    activity,
                    "Download failed. Please try again.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ── Install ───────────────────────────────────────────────────────────────

    private fun installUpdate(activity: Activity, apkFile: File) {
        if (!apkFile.exists()) return
        try {
            val uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            Log.d(TAG, "Install prompt shown")
        } catch (e: Exception) {
            Log.e(TAG, "Install failed: ${e.message}")
        }
    }
}
