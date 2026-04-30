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
import com.smallbiz.app.utils.PrefsManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Record install date on very first launch
        LicenseManager.recordInstallIfNew(this)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = PrefsManager(this)

            when {
                // App is usable (trial active OR licensed) — go to normal flow
                LicenseManager.isAppUsable(this) -> {
                    val dest = if (prefs.isBusinessSetup()) SalesActivity::class.java
                               else BusinessSetupActivity::class.java
                    startActivity(Intent(this, dest))
                }
                // Trial expired and not licensed — go to license screen
                else -> {
                    startActivity(Intent(this, LicenseActivity::class.java))
                }
            }
            finish()
        }, 2000)
    }
}
