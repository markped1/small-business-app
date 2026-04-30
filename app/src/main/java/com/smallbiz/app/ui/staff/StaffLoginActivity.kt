package com.smallbiz.app.ui.staff

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.databinding.ActivityStaffLoginBinding
import com.smallbiz.app.ui.admin.AdminActivity
import com.smallbiz.app.utils.PrefsManager

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

        // Show staff count hint
        val staffCount = prefs.getStaffList().size
        binding.tvLoginHint.text = if (staffCount > 0)
            "Admin PIN → Full access  |  Staff PIN → Sales only\n$staffCount staff member(s) registered"
        else
            "Enter Admin PIN to continue"

        binding.btnLogin.setOnClickListener {
            val pin  = binding.etPin.text.toString()
            val role = prefs.getRoleForPin(pin)

            when {
                role == "admin" -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                    finish()
                }
                role != null && role.startsWith("staff:") -> {
                    val staffName = role.removePrefix("staff:")
                    val intent = Intent(this, StaffSalesActivity::class.java)
                    intent.putExtra(StaffSalesActivity.EXTRA_STAFF_NAME, staffName)
                    startActivity(intent)
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
