package com.smallbiz.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.smallbiz.app.R
import com.smallbiz.app.data.model.StaffMember
import com.smallbiz.app.databinding.ActivityManageStaffBinding
import com.smallbiz.app.utils.PrefsManager
import java.util.UUID

class ManageStaffActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageStaffBinding
    private lateinit var prefs: PrefsManager
    private lateinit var adapter: StaffAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manage Staff"

        adapter = StaffAdapter(
            onEdit   = { staff -> showAddEditDialog(staff) },
            onDelete = { staff -> confirmDelete(staff) }
        )

        binding.rvStaff.apply {
            layoutManager = LinearLayoutManager(this@ManageStaffActivity)
            adapter = this@ManageStaffActivity.adapter
        }

        binding.fabAddStaff.setOnClickListener {
            if (prefs.getStaffList().size >= StaffMember.MAX_STAFF) {
                Toast.makeText(
                    this,
                    "Maximum ${StaffMember.MAX_STAFF} staff members allowed",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                showAddEditDialog(null)
            }
        }

        refreshList()
    }

    private fun refreshList() {
        val list = prefs.getStaffList()
        adapter.submitList(list)
        binding.tvStaffCount.text = "${list.size} / ${StaffMember.MAX_STAFF} staff"
        binding.tvNoStaff.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddEditDialog(existing: StaffMember?) {
        val view = layoutInflater.inflate(R.layout.dialog_add_staff, null)
        val etName = view.findViewById<TextInputEditText>(R.id.etStaffName)
        val etPin  = view.findViewById<TextInputEditText>(R.id.etStaffMemberPin)
        val tilPin = view.findViewById<TextInputLayout>(R.id.tilStaffMemberPin)

        existing?.let {
            etName.setText(it.name)
            etPin.setText(it.pin)
        }

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Add Staff Member" else "Edit Staff Member")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val pin  = etPin.text.toString().trim()

                when {
                    name.isEmpty() -> Toast.makeText(this, "Enter staff name", Toast.LENGTH_SHORT).show()
                    pin.length < 4 -> Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                    pin == prefs.getAdminPin() -> Toast.makeText(this, "PIN cannot match admin PIN", Toast.LENGTH_SHORT).show()
                    else -> {
                        if (existing == null) {
                            val member = StaffMember(
                                id   = UUID.randomUUID().toString(),
                                name = name,
                                pin  = pin
                            )
                            val added = prefs.addStaff(member)
                            if (added) {
                                Toast.makeText(this, "✓ $name added", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "PIN already in use or staff limit reached", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            prefs.updateStaff(existing.copy(name = name, pin = pin))
                            Toast.makeText(this, "✓ $name updated", Toast.LENGTH_SHORT).show()
                        }
                        refreshList()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(staff: StaffMember) {
        AlertDialog.Builder(this)
            .setTitle("Remove Staff")
            .setMessage("Remove \"${staff.name}\" from staff list?")
            .setPositiveButton("Remove") { _, _ ->
                prefs.removeStaff(staff.id)
                Toast.makeText(this, "${staff.name} removed", Toast.LENGTH_SHORT).show()
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}

// ── Staff list adapter ────────────────────────────────────────────────────────

class StaffAdapter(
    private val onEdit: (StaffMember) -> Unit,
    private val onDelete: (StaffMember) -> Unit
) : RecyclerView.Adapter<StaffAdapter.VH>() {

    private var list: List<StaffMember> = emptyList()

    fun submitList(newList: List<StaffMember>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_staff_member, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(list[position])
    override fun getItemCount() = list.size

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvName   = v.findViewById<TextView>(R.id.tvStaffMemberName)
        private val tvPin    = v.findViewById<TextView>(R.id.tvStaffMemberPin)
        private val tvNumber = v.findViewById<TextView>(R.id.tvStaffNumber)
        private val btnEdit  = v.findViewById<android.widget.Button>(R.id.btnEditStaff)
        private val btnDel   = v.findViewById<android.widget.Button>(R.id.btnDeleteStaff)

        fun bind(staff: StaffMember) {
            tvNumber.text = "#${adapterPosition + 1}"
            tvName.text   = staff.name
            tvPin.text    = "PIN: ${"•".repeat(staff.pin.length)}"
            btnEdit.setOnClickListener { onEdit(staff) }
            btnDel.setOnClickListener  { onDelete(staff) }
        }
    }
}
