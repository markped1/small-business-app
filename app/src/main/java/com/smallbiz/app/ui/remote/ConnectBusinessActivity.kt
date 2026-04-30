package com.smallbiz.app.ui.remote

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.databinding.ActivityConnectBusinessBinding
import com.smallbiz.app.sync.FirebaseSyncManager
import com.smallbiz.app.utils.PrefsManager

class ConnectBusinessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConnectBusinessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBusinessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PrefsManager(this)

        // Show the current business ID so the admin can share it
        val currentId = FirebaseSyncManager.getBusinessId()
        if (currentId != "default") {
            binding.tvYourBusinessId.text = "Your Business ID:\n$currentId"
            binding.cardYourId.visibility = android.view.View.VISIBLE
        }

        binding.btnConnect.setOnClickListener {
            val id = binding.etBusinessId.text.toString().trim()
            if (id.isEmpty()) {
                binding.etBusinessId.error = "Enter a Business ID"
                return@setOnClickListener
            }
            // Save the remote business ID to prefs
            prefs.saveRemoteBusinessId(id)
            // Re-init FirebaseSyncManager with this ID
            FirebaseSyncManager.initWithId(id)
            Toast.makeText(this, "Connected! Opening live view…", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, RemoteViewActivity::class.java))
        }

        binding.btnViewMine.setOnClickListener {
            startActivity(Intent(this, RemoteViewActivity::class.java))
        }

        binding.btnCancel.setOnClickListener { finish() }
    }
}
