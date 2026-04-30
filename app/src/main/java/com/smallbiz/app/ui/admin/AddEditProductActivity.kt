package com.smallbiz.app.ui.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.smallbiz.app.R
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.databinding.ActivityAddEditProductBinding
import com.smallbiz.app.utils.CurrencyFormatter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AddEditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditProductBinding
    private val viewModel: AdminViewModel by viewModels()
    private var editingProduct: Product? = null
    private var selectedImagePath: String? = null
    private var cameraImageUri: Uri? = null

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                saveImageToInternalStorage(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                saveImageToInternalStorage(uri)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openGallery() else
            Toast.makeText(this, "Permission needed to pick images", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Apply the admin's chosen currency symbol as prefix on price fields
        val symbol = CurrencyFormatter.getCurrencySymbol()
        binding.tilSellingPrice.prefixText = symbol
        binding.tilCostPrice.prefixText = symbol

        val productId = intent.getLongExtra(EXTRA_PRODUCT_ID, -1L)
        if (productId != -1L) {
            loadProduct(productId)
            supportActionBar?.title = "Edit Product"
        } else {
            supportActionBar?.title = "Add Product"
        }

        binding.ivProductImage.setOnClickListener { showImagePickerDialog() }
        binding.btnPickImage.setOnClickListener { showImagePickerDialog() }

        binding.btnSaveProduct.setOnClickListener { saveProduct() }
    }

    private fun loadProduct(id: Long) {
        lifecycleScope.launch {
            // Observe once
            viewModel.allProducts.observe(this@AddEditProductActivity) { products ->
                val product = products.find { it.id == id }
                product?.let {
                    editingProduct = it
                    binding.etProductName.setText(it.name)
                    binding.etSellingPrice.setText(it.sellingPrice.toString())
                    binding.etCostPrice.setText(it.costPrice.toString())
                    binding.etCategory.setText(it.category)
                    binding.etStockQuantity.setText(it.stockQuantity.toString())
                    selectedImagePath = it.imagePath
                    if (!it.imagePath.isNullOrEmpty()) {
                        val file = File(it.imagePath)
                        if (file.exists()) {
                            Glide.with(this@AddEditProductActivity)
                                .load(file)
                                .centerCrop()
                                .into(binding.ivProductImage)
                        }
                    }
                }
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Image")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Product Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> checkPermissionAndOpenGallery()
                    2 -> {
                        selectedImagePath = null
                        binding.ivProductImage.setImageResource(R.drawable.ic_product_placeholder)
                    }
                }
            }.show()
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun openCamera() {
        val imageFile = File(filesDir, "product_${UUID.randomUUID()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        cameraLauncher.launch(cameraImageUri)
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            val fileName = "product_${UUID.randomUUID()}.jpg"
            val destFile = File(filesDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            selectedImagePath = destFile.absolutePath
            Glide.with(this)
                .load(destFile)
                .centerCrop()
                .into(binding.ivProductImage)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProduct() {
        val name = binding.etProductName.text.toString().trim()
        val sellingPriceStr = binding.etSellingPrice.text.toString().trim()
        val costPriceStr = binding.etCostPrice.text.toString().trim()
        val category = binding.etCategory.text.toString().trim().ifEmpty { "General" }
        val stockQtyStr = binding.etStockQuantity.text.toString().trim()

        when {
            name.isEmpty() -> { binding.etProductName.error = "Product name required"; return }
            sellingPriceStr.isEmpty() -> { binding.etSellingPrice.error = "Selling price required"; return }
            costPriceStr.isEmpty() -> { binding.etCostPrice.error = "Cost price required"; return }
            stockQtyStr.isEmpty() -> { binding.etStockQuantity.error = "Stock quantity required"; return }
        }

        val sellingPrice = sellingPriceStr.toDoubleOrNull()
        val costPrice = costPriceStr.toDoubleOrNull()
        val stockQty = stockQtyStr.toIntOrNull()

        if (sellingPrice == null || sellingPrice <= 0) {
            binding.etSellingPrice.error = "Enter a valid price"; return
        }
        if (costPrice == null || costPrice < 0) {
            binding.etCostPrice.error = "Enter a valid cost"; return
        }
        if (stockQty == null || stockQty < 0) {
            binding.etStockQuantity.error = "Enter a valid quantity"; return
        }

        val product = Product(
            id = editingProduct?.id ?: 0,
            name = name,
            sellingPrice = sellingPrice,
            costPrice = costPrice,
            imagePath = selectedImagePath,
            category = category,
            isActive = editingProduct?.isActive ?: true,
            stockQuantity = stockQty
        )

        if (editingProduct != null) {
            viewModel.updateProduct(product)
            Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertProduct(product)
            Toast.makeText(this, "Product added!", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
