package com.smallbiz.app.ui.staff

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.databinding.ActivityStaffLoginBinding
import com.smallbiz.app.ui.admin.AdminActivity
import com.smallbiz.app.utils.PrefsManager

/**
 * Unified PIN login screen.
 * - Staff PIN  → StaffSalesActivity (sell only + today's total)
 * - Admin PIN  → AdminActivity (full access)
 * The user just enters their PIN — the app figures out the role automatically.
 */
class StaffLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffLoginBinding
    private lateinit var prefs: PrefsManager
    private var attempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        binding.tvBusinessName.text = prefs.getBusinessName()

        binding.btnLogin.setOnClickListener {
            val pin = binding.etPin.text.toString()
            val role = prefs.getRoleForPin(pin)

            when (role) {
                "admin" -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                    finish()
                }
                "staff" -> {
                    startActivity(Intent(this, StaffSalesActivity::class.java))
                    finish()
                }
                else -> {
                    attempts++
                    binding.etPin.text?.clear()
                    if (attempts >= 3) {
                        Toast.makeText(this, "Too many failed attempts", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        binding.tilPin.error = "Incorrect PIN (${3 - attempts} attempts left)"
                    }
                }
            }
        }

        binding.btnCancel.setOnClickListener { finish() }
    }
}
