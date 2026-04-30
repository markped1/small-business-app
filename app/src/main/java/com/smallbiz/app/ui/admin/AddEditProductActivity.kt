package com.smallbiz.app.ui.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.smallbiz.app.R
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.databinding.ActivityAddEditProductBinding
import com.smallbiz.app.utils.CurrencyFormatter
import com.smallbiz.app.utils.LanguageMapper
import com.smallbiz.app.utils.PrefsManager
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

    // Voice input
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var selectedLanguage: LanguageMapper.LanguageOption? = null

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    // ── Activity result launchers ─────────────────────────────────────────────

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri -> saveImageToInternalStorage(uri) }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraImageUri?.let { saveImageToInternalStorage(it) }
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openGallery()
        else Toast.makeText(this, "Permission needed to pick images", Toast.LENGTH_SHORT).show()
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput()
        else Toast.makeText(this, "Microphone permission needed for voice input", Toast.LENGTH_SHORT).show()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Currency symbol prefix
        val symbol = CurrencyFormatter.getCurrencySymbol()
        binding.tilSellingPrice.prefixText = symbol
        binding.tilCostPrice.prefixText = symbol

        // Set default language from selected currency
        val currencyCode = PrefsManager(this).getCurrencyCode()
        val languages = LanguageMapper.getLanguagesForCurrency(currencyCode)
        selectedLanguage = languages.first()
        updateLanguageButton()

        val productId = intent.getLongExtra(EXTRA_PRODUCT_ID, -1L)
        if (productId != -1L) {
            loadProduct(productId)
            supportActionBar?.title = "Edit Product"
        } else {
            supportActionBar?.title = "Add Product"
        }

        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        // Image: tap image or button
        binding.ivProductImage.setOnClickListener { showImagePickerDialog() }
        binding.btnPickImage.setOnClickListener { showImagePickerDialog() }

        // Direct camera shortcut button
        binding.btnTakePhoto.setOnClickListener { checkCameraAndOpen() }

        // Voice input button
        binding.btnVoiceInput.setOnClickListener { checkMicAndStart() }

        // Language selector
        binding.btnSelectLanguage.setOnClickListener { showLanguagePicker() }

        // Save
        binding.btnSaveProduct.setOnClickListener { saveProduct() }
    }

    // ── Language picker ───────────────────────────────────────────────────────

    private fun showLanguagePicker() {
        val currencyCode = PrefsManager(this).getCurrencyCode()
        val languages = LanguageMapper.getLanguagesForCurrency(currencyCode)
        val labels = languages.map { "${it.flag} ${it.displayName}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Input Language")
            .setItems(labels) { _, index ->
                selectedLanguage = languages[index]
                updateLanguageButton()
                Toast.makeText(
                    this,
                    "Language set to ${languages[index].displayName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun updateLanguageButton() {
        val lang = selectedLanguage ?: return
        binding.btnSelectLanguage.text = "${lang.flag} ${lang.displayName}"
    }

    // ── Voice input ───────────────────────────────────────────────────────────

    private fun checkMicAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                "Speech recognition not available on this device",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (isListening) {
            stopVoiceInput()
            return
        }

        val lang = selectedLanguage?.bcp47Tag ?: "en"

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isListening = true
                runOnUiThread {
                    binding.btnVoiceInput.text = "🔴 Listening…"
                    binding.btnVoiceInput.isSelected = true
                    binding.tvVoiceHint.visibility = View.VISIBLE
                    binding.tvVoiceHint.text = "Speak the product name in ${selectedLanguage?.displayName ?: "your language"}…"
                }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                runOnUiThread {
                    binding.btnVoiceInput.text = "🎤 Speak Product Name"
                    binding.btnVoiceInput.isSelected = false
                }
            }
            override fun onError(error: Int) {
                isListening = false
                runOnUiThread {
                    binding.btnVoiceInput.text = "🎤 Speak Product Name"
                    binding.btnVoiceInput.isSelected = false
                    binding.tvVoiceHint.visibility = View.GONE
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH       -> "Could not understand. Try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try again."
                        SpeechRecognizer.ERROR_NETWORK        -> "Network error. Check internet."
                        else -> "Voice input error. Try again."
                    }
                    Toast.makeText(this@AddEditProductActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                runOnUiThread {
                    binding.btnVoiceInput.text = "🎤 Speak Product Name"
                    binding.btnVoiceInput.isSelected = false
                    binding.tvVoiceHint.visibility = View.GONE
                }
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognized = matches[0]
                    runOnUiThread {
                        // Capitalize first letter
                        val formatted = recognized.replaceFirstChar { it.uppercase() }
                        binding.etProductName.setText(formatted)
                        binding.etProductName.setSelection(formatted.length)
                        Toast.makeText(
                            this@AddEditProductActivity,
                            "✓ \"$formatted\"",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                runOnUiThread {
                    binding.tvVoiceHint.text = "Hearing: \"$partial\"…"
                }
            }
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the product name in ${selectedLanguage?.displayName ?: "your language"}")
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopVoiceInput() {
        speechRecognizer?.stopListening()
        isListening = false
        binding.btnVoiceInput.text = "🎤 Speak Product Name"
        binding.btnVoiceInput.isSelected = false
        binding.tvVoiceHint.visibility = View.GONE
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private fun checkCameraAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("📷 Take Photo", "🖼 Choose from Gallery", "🗑 Remove Image")
        AlertDialog.Builder(this)
            .setTitle("Product Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraAndOpen()
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
            galleryPermissionLauncher.launch(permission)
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
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            selectedImagePath = destFile.absolutePath
            Glide.with(this).load(destFile).centerCrop().into(binding.ivProductImage)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Load existing product ─────────────────────────────────────────────────

    private fun loadProduct(id: Long) {
        lifecycleScope.launch {
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
                                .load(file).centerCrop().into(binding.ivProductImage)
                        }
                    }
                }
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private fun saveProduct() {
        val name          = binding.etProductName.text.toString().trim()
        val sellingPriceStr = binding.etSellingPrice.text.toString().trim()
        val costPriceStr  = binding.etCostPrice.text.toString().trim()
        val category      = binding.etCategory.text.toString().trim().ifEmpty { "General" }
        val stockQtyStr   = binding.etStockQuantity.text.toString().trim()

        when {
            name.isEmpty()          -> { binding.etProductName.error = "Product name required"; return }
            sellingPriceStr.isEmpty() -> { binding.etSellingPrice.error = "Selling price required"; return }
            costPriceStr.isEmpty()  -> { binding.etCostPrice.error = "Cost price required"; return }
            stockQtyStr.isEmpty()   -> { binding.etStockQuantity.error = "Stock quantity required"; return }
        }

        val sellingPrice = sellingPriceStr.toDoubleOrNull()
        val costPrice    = costPriceStr.toDoubleOrNull()
        val stockQty     = stockQtyStr.toIntOrNull()

        if (sellingPrice == null || sellingPrice <= 0) { binding.etSellingPrice.error = "Enter a valid price"; return }
        if (costPrice == null || costPrice < 0)        { binding.etCostPrice.error = "Enter a valid cost"; return }
        if (stockQty == null || stockQty < 0)          { binding.etStockQuantity.error = "Enter a valid quantity"; return }

        val product = Product(
            id           = editingProduct?.id ?: 0,
            name         = name,
            sellingPrice = sellingPrice,
            costPrice    = costPrice,
            imagePath    = selectedImagePath,
            category     = category,
            isActive     = editingProduct?.isActive ?: true,
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

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
