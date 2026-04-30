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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.smallbiz.app.BuildConfig
import java.io.File

/**
 * ToMega POS — Silent Update Manager
 *
 * How it works:
 * 1. On app start, fetches Firebase Remote Config (cached for 1 hour)
 * 2. Compares remote `latest_version_code` with current BuildConfig.VERSION_CODE
 * 3. If newer version exists:
 *    - Downloads the APK silently in the background via DownloadManager
 *    - Shows a non-blocking dialog: "Update available — install now or later"
 *    - User can dismiss and keep working — update is NOT forced
 * 4. When download completes, prompts to install
 *
 * Firebase Remote Config keys to set in Firebase Console:
 *   latest_version_code  →  integer  (e.g. 2)
 *   latest_version_name  →  string   (e.g. "1.1")
 *   apk_download_url     →  string   (direct URL to the new APK)
 *   update_message       →  string   (e.g. "Bug fixes and performance improvements")
 *   force_update         →  boolean  (true = user cannot dismiss, use sparingly)
 */
object UpdateManager {

    private const val TAG = "ToMegaUpdate"
    private const val KEY_VERSION_CODE  = "latest_version_code"
    private const val KEY_VERSION_NAME  = "latest_version_name"
    private const val KEY_APK_URL       = "apk_download_url"
    private const val KEY_UPDATE_MSG    = "update_message"
    private const val KEY_FORCE_UPDATE  = "force_update"
    private const val APK_FILE_NAME     = "tomega_pos_update.apk"

    private var downloadId: Long = -1L

    /**
     * Call this from SplashActivity or MainActivity after the app is ready.
     * Runs silently — only shows UI if an update is available.
     */
    fun checkForUpdate(activity: Activity) {
        val remoteConfig = FirebaseRemoteConfig.getInstance()

        // Fetch interval: 1 hour in production, 0 in debug
        val fetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(fetchInterval)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)

        // Set safe defaults so the app works even if Firebase is unreachable
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_VERSION_CODE to BuildConfig.VERSION_CODE.toLong(),
                KEY_VERSION_NAME to BuildConfig.VERSION_NAME,
                KEY_APK_URL      to "",
                KEY_UPDATE_MSG   to "Bug fixes and improvements",
                KEY_FORCE_UPDATE to false
            )
        )

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d(TAG, "Remote config fetch failed — skipping update check")
                return@addOnCompleteListener
            }

            val remoteVersionCode = remoteConfig.getLong(KEY_VERSION_CODE).toInt()
            val remoteVersionName = remoteConfig.getString(KEY_VERSION_NAME)
            val apkUrl            = remoteConfig.getString(KEY_APK_URL)
            val updateMessage     = remoteConfig.getString(KEY_UPDATE_MSG)
            val forceUpdate       = remoteConfig.getBoolean(KEY_FORCE_UPDATE)

            Log.d(TAG, "Current: ${BuildConfig.VERSION_CODE}, Remote: $remoteVersionCode")

            if (remoteVersionCode > BuildConfig.VERSION_CODE && apkUrl.isNotEmpty()) {
                Log.d(TAG, "Update available: v$remoteVersionName")
                activity.runOnUiThread {
                    showUpdateDialog(activity, remoteVersionName, updateMessage, apkUrl, forceUpdate)
                }
            }
        }
    }

    private fun showUpdateDialog(
        activity: Activity,
        versionName: String,
        message: String,
        apkUrl: String,
        forceUpdate: Boolean
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val builder = android.app.AlertDialog.Builder(activity)
            .setTitle("Update Available — v$versionName")
            .setMessage("$message\n\nThe update will download in the background and install automatically.")
            .setPositiveButton("Update Now") { _, _ ->
                downloadUpdate(activity, apkUrl)
            }

        if (!forceUpdate) {
            // Non-forced: user can dismiss and keep working
            builder.setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
            builder.setCancelable(true)
        } else {
            // Force update: cannot dismiss (use only for critical security fixes)
            builder.setCancelable(false)
        }

        builder.show()
    }

    private fun downloadUpdate(activity: Activity, apkUrl: String) {
        try {
            // Delete any previous downloaded APK
            val destFile = File(
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                APK_FILE_NAME
            )
            if (destFile.exists()) destFile.delete()

            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("ToMega POS Update")
                .setDescription("Downloading update…")
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                .setDestinationUri(Uri.fromFile(destFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)

            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)

            // Listen for download completion
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        activity.unregisterReceiver(this)
                        installUpdate(activity, destFile)
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

            Log.d(TAG, "Download started, id=$downloadId")

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
        }
    }

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
        } catch (e: Exception) {
            Log.e(TAG, "Install failed: ${e.message}")
        }
    }
}
