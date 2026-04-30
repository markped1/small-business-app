package com.smallbiz.app.ui.setup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.R
import com.smallbiz.app.ui.sales.SalesActivity
import com.smallbiz.app.utils.PrefsManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = PrefsManager(this)
            if (prefs.isBusinessSetup()) {
                startActivity(Intent(this, SalesActivity::class.java))
            } else {
                startActivity(Intent(this, BusinessSetupActivity::class.java))
            }
            finish()
        }, 2000)
    }
}
