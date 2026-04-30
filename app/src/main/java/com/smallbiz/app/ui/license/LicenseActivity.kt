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

        val daysLeft    = LicenseManager.getTrialDaysRemaining(this)
        val isActivated = LicenseManager.isActivated(this)

        when {
            isActivated  -> showActivatedState()
            daysLeft > 0 -> showTrialState(daysLeft)
            else         -> showExpiredState()
        }

        // Pre-fill fields
        val prefs = PrefsManager(this)
        if (prefs.isBusinessSetup()) {
            binding.etLicenseBusinessName.setText(prefs.getBusinessName())
        }
        val savedKey = LicenseManager.getSavedKey(this)
        if (savedKey.isNotEmpty()) binding.etLicenseKey.setText(savedKey)

        binding.btnActivate.setOnClickListener { attemptActivation() }
        binding.btnContinueTrial.setOnClickListener { proceedToApp() }
        binding.btnPasteKey.setOnClickListener {
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cb.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (text.isNotEmpty()) binding.etLicenseKey.setText(text)
            else Toast.makeText(this, "Nothing in clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTrialState(daysLeft: Long) {
        val word = if (daysLeft == 1L) "day" else "days"
        binding.tvLicenseStatus.text = "⏳ Free Trial"
        binding.tvLicenseMessage.text =
            "You have $daysLeft $word remaining in your free trial.\n" +
            "Activate now to use ToMega POS forever."
        binding.tvLicenseStatus.setTextColor(getColor(com.smallbiz.app.R.color.orange_500))
        binding.btnContinueTrial.visibility = View.VISIBLE
        binding.btnContinueTrial.text = "Continue Trial ($daysLeft $word left)"
        binding.cardActivation.visibility = View.VISIBLE
        binding.tvKeyTypeHint.visibility = View.VISIBLE
    }

    private fun showExpiredState() {
        binding.tvLicenseStatus.text = "🔒 Trial Expired"
        binding.tvLicenseMessage.text =
            "Your 3-day free trial has ended.\n" +
            "Enter your license key to continue using ToMega POS."
        binding.tvLicenseStatus.setTextColor(getColor(com.smallbiz.app.R.color.red_400))
        binding.btnContinueTrial.visibility = View.GONE
        binding.cardActivation.visibility = View.VISIBLE
        binding.tvKeyTypeHint.visibility = View.VISIBLE
    }

    private fun showActivatedState() {
        val bizName = LicenseManager.getLicensedBusinessName(this)
        val type    = LicenseManager.getLicenseType(this)
        val seats   = LicenseManager.getBatchSeats(this)
        val typeLabel = if (type == "batch") "Batch License ($seats seats)" else "Single License"
        binding.tvLicenseStatus.text = "✅ Licensed"
        binding.tvLicenseMessage.text = "ToMega POS is fully activated.\n$bizName\n$typeLabel"
        binding.tvLicenseStatus.setTextColor(getColor(com.smallbiz.app.R.color.green_500))
        binding.btnContinueTrial.visibility = View.VISIBLE
        binding.btnContinueTrial.text = "Continue to App"
        binding.cardActivation.visibility = View.GONE
        binding.tvKeyTypeHint.visibility = View.GONE
    }

    private fun attemptActivation() {
        val name = binding.etLicenseBusinessName.text.toString().trim()
        val key  = binding.etLicenseKey.text.toString().trim()

        if (name.isEmpty()) { binding.etLicenseBusinessName.error = "Enter your business name"; return }
        if (key.isEmpty())  { binding.etLicenseKey.error = "Enter your license key"; return }

        setLoading(true)

        LicenseManager.activate(this, name, key) { result ->
            runOnUiThread {
                setLoading(false)
                handleResult(result)
            }
        }
    }

    private fun handleResult(result: LicenseManager.ActivationResult) {
        when (result) {
            LicenseManager.ActivationResult.SUCCESS -> {
                Toast.makeText(this, "✅ Activated! Welcome to ToMega POS.", Toast.LENGTH_LONG).show()
                showActivatedState()
                binding.root.postDelayed({ proceedToApp() }, 1500)
            }
            LicenseManager.ActivationResult.INVALID_KEY -> {
                binding.etLicenseKey.error = "Invalid key for this business name"
                Toast.makeText(this, "❌ License key is incorrect", Toast.LENGTH_LONG).show()
            }
            LicenseManager.ActivationResult.INVALID_FORMAT -> {
                binding.etLicenseKey.error = "Invalid key format.\nSingle: TMPOS-XXXX-XXXX-XXXX-XXXX\nBatch:  TMBAT-XXXX-NNN-XX"
            }
            LicenseManager.ActivationResult.INVALID_NAME -> {
                binding.etLicenseBusinessName.error = "Business name cannot be empty"
            }
            LicenseManager.ActivationResult.SEATS_EXHAUSTED -> {
                Toast.makeText(
                    this,
                    "❌ This batch key has reached its maximum number of activations.\nContact ToMega for a new key.",
                    Toast.LENGTH_LONG
                ).show()
                binding.etLicenseKey.error = "All seats used — key is full"
            }
            LicenseManager.ActivationResult.NETWORK_ERROR -> {
                Toast.makeText(
                    this,
                    "⚠️ Could not reach the server. Check your internet connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnActivate.isEnabled = !loading
        binding.progressActivation.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.btnActivate.text = "Verifying…"
        } else {
            binding.btnActivate.text = "🔑  Activate Now"
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
