package com.smallbiz.app.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.databinding.ActivityBusinessSetupBinding
import com.smallbiz.app.ui.sales.SalesActivity
import com.smallbiz.app.utils.PrefsManager

class BusinessSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusinessSetupBinding
    private lateinit var prefs: PrefsManager

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
            binding.btnSave.text = "Update Business Info"
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etBusinessName.text.toString().trim()
            val address = binding.etBusinessAddress.text.toString().trim()
            val pin = binding.etAdminPin.text.toString().trim()
            val confirmPin = binding.etConfirmPin.text.toString().trim()

            when {
                name.isEmpty() -> binding.etBusinessName.error = "Business name is required"
                address.isEmpty() -> binding.etBusinessAddress.error = "Address is required"
                pin.length < 4 -> binding.etAdminPin.error = "PIN must be at least 4 digits"
                pin != confirmPin -> binding.etConfirmPin.error = "PINs do not match"
                else -> {
                    prefs.saveBusinessInfo(name, address, pin)
                    Toast.makeText(this, "Business setup complete!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SalesActivity::class.java))
                    finish()
                }
            }
        }
    }
}
