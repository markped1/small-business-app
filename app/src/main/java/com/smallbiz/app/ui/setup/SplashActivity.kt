package com.smallbiz.app.ui.setup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.R
import com.smallbiz.app.license.LicenseManager
import com.smallbiz.app.ui.license.LicenseActivity
import com.smallbiz.app.ui.sales.SalesActivity
import com.smallbiz.app.update.UpdateManager
import com.smallbiz.app.utils.PrefsManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Record install date on very first launch
        LicenseManager.recordInstallIfNew(this)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = PrefsManager(this)

            val dest: Class<*> = when {
                !LicenseManager.isAppUsable(this) -> LicenseActivity::class.java
                prefs.isBusinessSetup()            -> SalesActivity::class.java
                else                               -> BusinessSetupActivity::class.java
            }
            startActivity(Intent(this, dest))
            finish()
        }, 2000)
    }

    override fun onStart() {
        super.onStart()
        // Check for updates silently in the background — does NOT block the UI
        // Only shows a dialog if a newer version is available on Firebase Remote Config
        UpdateManager.checkForUpdate(this)
    }
}
