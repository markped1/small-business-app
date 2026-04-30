package com.smallbiz.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.smallbiz.app.databinding.ActivityAdminBinding
import com.smallbiz.app.ui.setup.BusinessSetupActivity
import com.smallbiz.app.ui.reports.StockReportActivity
import com.smallbiz.app.utils.PrefsManager

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var productAdapter: AdminProductAdapter
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Admin Panel – ${prefs.getBusinessName()}"

        setupProductList()
        setupObservers()
        setupClickListeners()
    }

    private fun setupProductList() {
        productAdapter = AdminProductAdapter(
            onEdit = { product ->
                val intent = Intent(this, AddEditProductActivity::class.java)
                intent.putExtra(AddEditProductActivity.EXTRA_PRODUCT_ID, product.id)
                startActivity(intent)
            },
            onToggleActive = { product ->
                if (product.isActive) {
                    AlertDialog.Builder(this)
                        .setTitle("Deactivate Product")
                        .setMessage("Hide \"${product.name}\" from the sales screen?")
                        .setPositiveButton("Deactivate") { _, _ ->
                            viewModel.deactivateProduct(product.id)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    viewModel.updateProduct(product.copy(isActive = true))
                }
            },
            onDelete = { product ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Product")
                    .setMessage("Permanently delete \"${product.name}\"? This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteProduct(product)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvAdminProducts.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        viewModel.allProducts.observe(this) { products ->
            productAdapter.submitList(products)
            binding.tvProductCount.text = "${products.size} products"
        }
    }

    private fun setupClickListeners() {
        binding.fabAddProduct.setOnClickListener {
            startActivity(Intent(this, AddEditProductActivity::class.java))
        }

        binding.btnManageExpenses.setOnClickListener {
            startActivity(Intent(this, ExpensesActivity::class.java))
        }

        binding.btnBusinessSettings.setOnClickListener {
            startActivity(Intent(this, BusinessSetupActivity::class.java))
        }

        binding.btnManageStaff.setOnClickListener {
            startActivity(Intent(this, ManageStaffActivity::class.java))
        }

        binding.btnStockReport.setOnClickListener {
            startActivity(Intent(this, StockReportActivity::class.java))
        }

        binding.btnRemoteView.setOnClickListener {
            startActivity(Intent(this, com.smallbiz.app.ui.remote.ConnectBusinessActivity::class.java))
        }

        binding.btnManageLicense.setOnClickListener {
            startActivity(Intent(this, com.smallbiz.app.ui.license.LicenseActivity::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
