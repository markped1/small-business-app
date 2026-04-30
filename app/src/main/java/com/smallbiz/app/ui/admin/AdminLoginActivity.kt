package com.smallbiz.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.databinding.ActivityAdminLoginBinding
import com.smallbiz.app.utils.PrefsManager

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminLoginBinding
    private lateinit var prefs: PrefsManager
    private var attempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        binding.btnLogin.setOnClickListener {
            val pin = binding.etPin.text.toString()
            if (prefs.verifyPin(pin)) {
                startActivity(Intent(this, AdminActivity::class.java))
                finish()
            } else {
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

        binding.btnCancel.setOnClickListener { finish() }
    }
}
