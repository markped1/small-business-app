package com.smallbiz.app.ui.staff

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.data.model.Sale
import com.smallbiz.app.data.repository.AppRepository
import com.smallbiz.app.databinding.ActivityStaffSalesBinding
import com.smallbiz.app.ui.sales.SalesViewModel
import com.smallbiz.app.utils.CurrencyFormatter
import com.smallbiz.app.utils.PrefsManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StaffSalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffSalesBinding
    private val viewModel: SalesViewModel by viewModels()
    private lateinit var productAdapter: PosProductAdapter
    private lateinit var cartAdapter: PosCartAdapter
    private lateinit var prefs: PrefsManager
    private lateinit var repository: AppRepository

    private var allProducts: List<Product> = emptyList()
    private var isReportMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffSalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        repository = AppRepository(this)

        // Header
        binding.tvPosBusinessName.text = prefs.getBusinessName()
        binding.tvPosAddress.text = prefs.getBusinessAddress()

        setupProductList()
        setupCart()
        setupSearch()
        setupObservers()
        setupClickListeners()
        loadTodayStats()
    }

    // ── Product list (POS style, linear) ─────────────────────────────────────
    private fun setupProductList() {
        productAdapter = PosProductAdapter(
            onAddToCart = { product -> viewModel.addToCart(product) },
            onAlertRestock = { product -> showRestockAlert(product) }
        )
        binding.rvPosProducts.apply {
            layoutManager = LinearLayoutManager(this@StaffSalesActivity)
            adapter = productAdapter
        }
    }

    // ── Cart ──────────────────────────────────────────────────────────────────
    private fun setupCart() {
        cartAdapter = PosCartAdapter(
            onIncrement = { id -> viewModel.incrementItem(id) },
            onDecrement = { id -> viewModel.decrementItem(id) },
            onRemove    = { id -> viewModel.removeFromCart(id) }
        )
        binding.rvPosCart.apply {
            layoutManager = LinearLayoutManager(this@StaffSalesActivity)
            adapter = cartAdapter
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private fun setupSearch() {
        binding.etPosSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim().lowercase()
                val filtered = if (query.isEmpty()) allProducts
                else allProducts.filter {
                    it.name.lowercase().contains(query) ||
                    it.category.lowercase().contains(query)
                }
                productAdapter.submitList(filtered)
                binding.tvPosNoProducts.visibility =
                    if (filtered.isEmpty()) View.VISIBLE else View.GONE
                binding.tvPosNoProducts.text = if (query.isNotEmpty())
                    "No products found for \"$query\""
                else "No products available. Ask admin to add products."
            }
        })
    }

    // ── Observers ─────────────────────────────────────────────────────────────
    private fun setupObservers() {
        viewModel.products.observe(this) { products ->
            allProducts = products
            productAdapter.submitList(products)
            binding.tvPosNoProducts.visibility =
                if (products.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.cart.observe(this) { cartItems ->
            cartAdapter.submitList(cartItems.toList())
            val isEmpty = cartItems.isEmpty()
            binding.layoutPosCartEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvPosCart.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.btnPosCheckout.isEnabled = !isEmpty
            // Update cart badge
            val count = cartItems.sumOf { it.quantity }
            binding.tvPosCartBadge.text = count.toString()
            binding.tvPosCartBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        viewModel.cartTotal.observe(this) { total ->
            binding.tvPosCartTotal.text = CurrencyFormatter.format(total)
            binding.tvPosOrderTotalFooter.text = CurrencyFormatter.format(total)
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnPosCheckout.setOnClickListener { showCheckoutDialog() }

        binding.btnPosClearCart.setOnClickListener {
            if ((viewModel.cart.value?.size ?: 0) > 0) {
                AlertDialog.Builder(this)
                    .setTitle("Clear Order")
                    .setMessage("Remove all items from the current order?")
                    .setPositiveButton("Clear") { _, _ -> viewModel.clearCart() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        // Bottom bar buttons
        binding.btnBottomReport.setOnClickListener {
            isReportMode = !isReportMode
            if (isReportMode) showDailyReport() else showSellMode()
        }

        binding.btnBottomClockOut.setOnClickListener { showClockOutDialog() }

        binding.btnBottomLogout.setOnClickListener { finish() }
    }

    // ── Checkout ──────────────────────────────────────────────────────────────
    private fun showCheckoutDialog() {
        val total     = viewModel.cartTotal.value ?: 0.0
        val itemCount = viewModel.cart.value?.sumOf { it.quantity } ?: 0
        val sdf       = SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault())
        val now       = sdf.format(Date())

        AlertDialog.Builder(this)
            .setTitle("Confirm Sale")
            .setMessage(
                "Date/Time: $now\n" +
                "Items: $itemCount\n" +
                "Total: ${CurrencyFormatter.format(total)}\n\n" +
                "Complete this sale?"
            )
            .setPositiveButton("✓ Confirm Sale") { _, _ ->
                viewModel.checkout {
                    runOnUiThread {
                        Toast.makeText(this, "✓ Sale recorded at $now", Toast.LENGTH_SHORT).show()
                        loadTodayStats()
                        showSaleSuccess()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Restock alert ─────────────────────────────────────────────────────────
    private fun showRestockAlert(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Alert Management")
            .setMessage("Send a restock request for \"${product.name}\" to the admin?")
            .setPositiveButton("Send Alert") { _, _ ->
                prefs.saveRestockAlert(product.name)
                Toast.makeText(
                    this,
                    "✓ Restock alert sent for \"${product.name}\".\nAdmin will see it in the Admin Panel.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Clock out ─────────────────────────────────────────────────────────────
    private fun showClockOutDialog() {
        val sdf = SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.getDefault())
        val now = sdf.format(Date())

        AlertDialog.Builder(this)
            .setTitle("Clock Out")
            .setMessage(
                "Clock out and end your shift?\n\n" +
                "Time: $now\n\n" +
                "This will record that sales for today have ended."
            )
            .setPositiveButton("Clock Out") { _, _ ->
                prefs.recordClockOut()
                Toast.makeText(
                    this,
                    "✓ Clocked out at $now\nHave a good rest!",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Today's stats ─────────────────────────────────────────────────────────
    private fun loadTodayStats() {
        lifecycleScope.launch {
            val start = AppRepository.startOfDay()
            val end   = AppRepository.endOfDay()
            val sales = repository.getSalesByDaySync(start, end)
            val total = sales.sumOf { it.totalAmount }
            val count = sales.map { it.transactionId }.toSet().size
            runOnUiThread {
                binding.tvPosTodayTotal.text = CurrencyFormatter.format(total)
                binding.tvPosTodayCount.text = "$count"
            }
        }
    }

    // ── Daily Report ──────────────────────────────────────────────────────────
    private fun showDailyReport() {
        binding.btnBottomReport.text = "← Sales"
        binding.layoutPosSellMode.visibility = View.GONE
        binding.layoutPosReportMode.visibility = View.VISIBLE

        lifecycleScope.launch {
            val start = AppRepository.startOfDay()
            val end   = AppRepository.endOfDay()
            val sales = repository.getSalesByDaySync(start, end)
            runOnUiThread { bindDailyReport(sales) }
        }
    }

    private fun bindDailyReport(sales: List<Sale>) {
        val dateFmt = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        binding.tvPosReportDate.text = dateFmt.format(Date())

        val totalRevenue = sales.sumOf { it.totalAmount }
        val txCount      = sales.map { it.transactionId }.toSet().size
        val itemsSold    = sales.sumOf { it.quantity }

        binding.tvPosReportRevenue.text  = CurrencyFormatter.format(totalRevenue)
        binding.tvPosReportTxCount.text  = "$txCount"
        binding.tvPosReportItemsSold.text = "$itemsSold"

        // Product breakdown table
        val productMap = mutableMapOf<String, Pair<Int, Double>>()
        sales.forEach { sale ->
            val e = productMap[sale.productName] ?: Pair(0, 0.0)
            productMap[sale.productName] = Pair(e.first + sale.quantity, e.second + sale.totalAmount)
        }

        val sb = StringBuilder()
        sb.append(String.format("%-22s %5s  %12s\n", "Product", "Qty", "Amount"))
        sb.append("─".repeat(42) + "\n")
        productMap.entries.sortedByDescending { it.value.second }.forEach { (name, data) ->
            val truncName = if (name.length > 20) name.take(19) + "…" else name
            sb.append(String.format("%-22s %5d  %12s\n",
                truncName, data.first, CurrencyFormatter.format(data.second)))
        }
        sb.append("─".repeat(42) + "\n")
        sb.append(String.format("%-22s %5d  %12s\n", "TOTAL", itemsSold, CurrencyFormatter.format(totalRevenue)))
        binding.tvPosProductTable.text = if (productMap.isNotEmpty()) sb.toString() else "No sales recorded today"

        // Transaction log
        val txSb = StringBuilder()
        txSb.append(String.format("%-8s  %-20s  %10s\n", "Time", "Items", "Total"))
        txSb.append("─".repeat(42) + "\n")
        val txGroups = sales.groupBy { it.transactionId }
            .entries.sortedByDescending { it.value.first().saleDate }
        txGroups.forEach { (_, txSales) ->
            val time  = timeFmt.format(Date(txSales.first().saleDate))
            val items = txSales.joinToString(", ") { "${it.productName}×${it.quantity}" }
            val total = CurrencyFormatter.format(txSales.sumOf { it.totalAmount })
            val truncItems = if (items.length > 18) items.take(17) + "…" else items
            txSb.append(String.format("%-8s  %-20s  %10s\n", time, truncItems, total))
        }
        binding.tvPosTxLog.text = if (txGroups.isNotEmpty()) txSb.toString() else "No transactions yet"

        // Clock-out info
        val lastOut = prefs.getLastClockOut()
        if (lastOut > 0) {
            val outTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastOut))
            binding.tvPosClockOutInfo.text = "Last clock-out: $outTime"
            binding.tvPosClockOutInfo.visibility = View.VISIBLE
        } else {
            binding.tvPosClockOutInfo.visibility = View.GONE
        }
    }

    private fun showSellMode() {
        binding.btnBottomReport.text = "📋 Report"
        binding.layoutPosSellMode.visibility = View.VISIBLE
        binding.layoutPosReportMode.visibility = View.GONE
    }

    // ── Sale success ──────────────────────────────────────────────────────────
    private fun showSaleSuccess() {
        binding.layoutPosSaleSuccess.visibility = View.VISIBLE
        binding.layoutPosSaleSuccess.animate().alpha(1f).setDuration(300)
            .withEndAction {
                binding.layoutPosSaleSuccess.postDelayed({
                    binding.layoutPosSaleSuccess.animate().alpha(0f).setDuration(300)
                        .withEndAction { binding.layoutPosSaleSuccess.visibility = View.GONE }
                        .start()
                }, 1500)
            }.start()
    }
}
