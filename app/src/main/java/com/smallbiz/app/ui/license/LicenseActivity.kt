package com.smallbiz.app.ui.license

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.databinding.ActivityLicenseBinding
import com.smallbiz.app.license.LicenseManager
import com.smallbiz.app.ui.setup.BusinessSetupActivity
import com.smallbiz.app.ui.sales.SalesActivity
import com.smallbiz.app.utils.PrefsManager

class LicenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLicenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val daysLeft = LicenseManager.getTrialDaysRemaining(this)
        val isActivated = LicenseManager.isActivated(this)

        if (isActivated) {
            // Already licensed — show status and allow continuing
            showActivatedState()
        } else if (daysLeft > 0) {
            // Still in trial
            showTrialState(daysLeft)
        } else {
            // Trial expired
            showExpiredState()
        }

        // Pre-fill business name if already set
        val prefs = PrefsManager(this)
        if (prefs.isBusinessSetup()) {
            binding.etLicenseBusinessName.setText(prefs.getBusinessName())
        }

        // Pre-fill saved key if any
        val savedKey = LicenseManager.getSavedKey(this)
        if (savedKey.isNotEmpty()) {
            binding.etLicenseKey.setText(savedKey)
        }

        binding.btnActivate.setOnClickListener { attemptActivation() }

        binding.btnContinueTrial.setOnClickListener { proceedToApp() }

        binding.btnPasteKey.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (text.isNotEmpty()) {
                binding.etLicenseKey.setText(text)
            } else {
                Toast.makeText(this, "Nothing in clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTrialState(daysLeft: Long) {
        val dayWord = if (daysLeft == 1L) "day" else "days"
        binding.tvLicenseStatus.text = "⏳ Free Trial"
        binding.tvLicenseMessage.text =
            "You have $daysLeft $dayWord remaining in your free trial.\nActivate now to use ToMega POS forever."
        binding.tvLicenseStatus.setTextColor(getColor(com.smallbiz.app.R.color.orange_500))
        binding.btnContinueTrial.visibility = View.VISIBLE
        binding.btnContinueTrial.text = "Continue Trial ($daysLeft $dayWord left)"
        binding.cardActivation.visibility = View.VISIBLE
    }

    private fun showExpiredState() {
        binding.tvLicenseStatus.text = "🔒 Trial Expired"
        binding.tvLicenseMessage.text =
            "Your 3-day free trial has ended.\nEnter your license key to continue using ToMega POS."
        binding.tvLicenseStatus.setTextColor(getColor(com.smallbiz.app.R.color.red_400))
        binding.btnContinueTrial.visibility = View.GONE
        binding.cardActivation.visibility = View.VISIBLE
    }

    private fun showActivatedState() {
        val bizName = LicenseManager.getLicensedBusinessName(this)
        binding.tvLicenseStatus.text = "✅ Licensed"
        binding.tvLicenseMessage.text = "ToMega POS is fully activated for:\n$bizName"
        binding.tvLicenseStatus.setTextColor(getColor(com.smallbiz.app.R.color.green_500))
        binding.btnContinueTrial.visibility = View.VISIBLE
        binding.btnContinueTrial.text = "Continue to App"
        binding.cardActivation.visibility = View.GONE
    }

    private fun attemptActivation() {
        val name = binding.etLicenseBusinessName.text.toString().trim()
        val key  = binding.etLicenseKey.text.toString().trim()

        if (name.isEmpty()) {
            binding.etLicenseBusinessName.error = "Enter your business name"
            return
        }
        if (key.isEmpty()) {
            binding.etLicenseKey.error = "Enter your license key"
            return
        }

        binding.btnActivate.isEnabled = false
        binding.progressActivation.visibility = View.VISIBLE

        when (LicenseManager.activate(this, name, key)) {
            LicenseManager.ActivationResult.SUCCESS -> {
                binding.progressActivation.visibility = View.GONE
                Toast.makeText(this, "✅ Activated! Welcome to ToMega POS.", Toast.LENGTH_LONG).show()
                showActivatedState()
                // Small delay then proceed
                binding.root.postDelayed({ proceedToApp() }, 1500)
            }
            LicenseManager.ActivationResult.INVALID_KEY -> {
                binding.progressActivation.visibility = View.GONE
                binding.btnActivate.isEnabled = true
                binding.etLicenseKey.error = "Invalid key for this business name"
                Toast.makeText(this, "❌ License key is incorrect", Toast.LENGTH_LONG).show()
            }
            LicenseManager.ActivationResult.INVALID_FORMAT -> {
                binding.progressActivation.visibility = View.GONE
                binding.btnActivate.isEnabled = true
                binding.etLicenseKey.error = "Key must be in format: TMPOS-XXXX-XXXX-XXXX-XXXX"
            }
            LicenseManager.ActivationResult.INVALID_NAME -> {
                binding.progressActivation.visibility = View.GONE
                binding.btnActivate.isEnabled = true
                binding.etLicenseBusinessName.error = "Business name cannot be empty"
            }
        }
    }

    private fun proceedToApp() {
        val prefs = PrefsManager(this)
        val dest = if (prefs.isBusinessSetup()) SalesActivity::class.java
                   else BusinessSetupActivity::class.java
        startActivity(Intent(this, dest))
        finish()
    }
}
