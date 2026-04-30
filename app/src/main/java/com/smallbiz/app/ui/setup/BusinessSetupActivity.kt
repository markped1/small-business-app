package com.smallbiz.app.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.databinding.ActivityBusinessSetupBinding
import com.smallbiz.app.ui.sales.SalesActivity
import com.smallbiz.app.utils.CurrencyItem
import com.smallbiz.app.utils.PrefsManager

class BusinessSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusinessSetupBinding
    private lateinit var prefs: PrefsManager
    private var selectedCurrencyCode: String = "NGN"
    private var selectedCurrencyLabel: String = "🇳🇬  NGN – Nigerian Naira (₦)"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusinessSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        // Pre-fill if editing
        if (prefs.isBusinessSetup()) {
            binding.etBusinessName.setText(prefs.getBusinessName())
            binding.etBusinessAddress.setText(prefs.getBusinessAddress())
            binding.etAdminPin.setText(prefs.getAdminPin())
            binding.etAdminGmail.setText(prefs.getAdminGmail())
            binding.btnSave.text = "Update Business Info"
            selectedCurrencyCode = prefs.getCurrencyCode()
            updateCurrencyDisplay(selectedCurrencyCode)
        } else {
            updateCurrencyDisplay("NGN")
        }

        // Open currency picker on tap
        binding.layoutCurrencyPicker.setOnClickListener {
            val dialog = CurrencyPickerDialog(selectedCurrencyCode) { item ->
                selectedCurrencyCode = item.code
                updateCurrencyDisplay(item.code)
            }
            dialog.show(supportFragmentManager, "currency_picker")
        }

        binding.btnSave.setOnClickListener {
            val name       = binding.etBusinessName.text.toString().trim()
            val address    = binding.etBusinessAddress.text.toString().trim()
            val pin        = binding.etAdminPin.text.toString().trim()
            val confirmPin = binding.etConfirmPin.text.toString().trim()
            val gmail      = binding.etAdminGmail.text.toString().trim()

            when {
                name.isEmpty()      -> binding.etBusinessName.error = "Business name is required"
                address.isEmpty()   -> binding.etBusinessAddress.error = "Address is required"
                pin.length < 4      -> binding.etAdminPin.error = "PIN must be at least 4 digits"
                pin != confirmPin   -> binding.etConfirmPin.error = "PINs do not match"
                gmail.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(gmail).matches() ->
                    binding.etAdminGmail.error = "Enter a valid Gmail address"
                else -> {
                    prefs.saveBusinessInfo(name, address, pin, selectedCurrencyCode)
                    if (gmail.isNotEmpty()) prefs.saveAdminGmail(gmail)
                    // Register business in Firebase analytics
                    com.smallbiz.app.analytics.BusinessAnalytics.registerBusiness(
                        this, name, gmail
                    )
                    Toast.makeText(this, "Business setup complete!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SalesActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun updateCurrencyDisplay(code: String) {
        val item = CurrencyItem.buildList().find { it.code == code }
        binding.tvSelectedCurrency.text = item?.displayLabel ?: "🌐  $code"
    }
}
