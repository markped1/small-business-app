package com.smallbiz.app.ui.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smallbiz.app.R
import com.smallbiz.app.data.model.Sale
import com.smallbiz.app.data.repository.AppRepository
import com.smallbiz.app.databinding.ActivityStaffSalesBinding
import com.smallbiz.app.ui.sales.CartAdapter
import com.smallbiz.app.ui.sales.ProductGridAdapter
import com.smallbiz.app.ui.sales.SalesViewModel
import com.smallbiz.app.utils.CurrencyFormatter
import com.smallbiz.app.utils.PrefsManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Staff-only sales screen.
 *
 * Staff CAN:
 *   ✅ Sell products (add to cart, +/- qty, confirm sale)
 *   ✅ See today's total revenue and transaction count
 *   ✅ See full list of today's sales (product, qty, amount per transaction)
 *   ✅ See which products were sold and how many units
 *
 * Staff CANNOT:
 *   ❌ See profit margins or cost prices
 *   ❌ See stock levels / stock report
 *   ❌ See weekly or monthly reports
 *   ❌ See expenses
 *   ❌ Add/edit/delete products
 *   ❌ Access business settings
 *   ❌ Access admin panel
 */
class StaffSalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffSalesBinding
    private val viewModel: SalesViewModel by viewModels()
    private lateinit var productAdapter: ProductGridAdapter
    private lateinit var cartAdapter: CartAdapter
    private lateinit var prefs: PrefsManager
    private lateinit var repository: AppRepository

    // Toggle between SELL mode and DAILY REPORT mode
    private var isReportMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffSalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        repository = AppRepository(this)

        binding.tvStaffBusinessName.text = prefs.getBusinessName()

        setupProductGrid()
        setupCart()
        setupObservers()
        setupClickListeners()
        loadTodayStats()
    }

    // ── Product grid ──────────────────────────────────────────────────────────
    private fun setupProductGrid() {
        productAdapter = ProductGridAdapter { product ->
            viewModel.addToCart(product)
        }
        binding.rvStaffProducts.apply {
            layoutManager = GridLayoutManager(this@StaffSalesActivity, 2)
            adapter = productAdapter
        }
    }

    // ── Cart ──────────────────────────────────────────────────────────────────
    private fun setupCart() {
        cartAdapter = CartAdapter(
            onIncrement = { productId -> viewModel.incrementItem(productId) },
            onDecrement = { productId -> viewModel.decrementItem(productId) },
            onRemove    = { productId -> viewModel.removeFromCart(productId) }
        )
        binding.rvStaffCart.adapter = cartAdapter
    }

    // ── LiveData observers ────────────────────────────────────────────────────
    private fun setupObservers() {
        viewModel.products.observe(this) { products ->
            productAdapter.submitList(products)
            binding.tvStaffNoProducts.visibility =
                if (products.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.cart.observe(this) { cartItems ->
            cartAdapter.submitList(cartItems.toList())
            val isEmpty = cartItems.isEmpty()
            binding.layoutStaffCartEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvStaffCart.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.btnStaffCheckout.isEnabled = !isEmpty
        }

        viewModel.cartTotal.observe(this) { total ->
            binding.tvStaffCartTotal.text = CurrencyFormatter.format(total)
        }

        viewModel.cartItemCount.observe(this) { count ->
            binding.tvStaffCartBadge.text = count.toString()
            binding.tvStaffCartBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnStaffCheckout.setOnClickListener { showCheckoutDialog() }

        binding.btnStaffClearCart.setOnClickListener {
            if ((viewModel.cart.value?.size ?: 0) > 0) {
                AlertDialog.Builder(this)
                    .setTitle("Clear Cart")
                    .setMessage("Remove all items from cart?")
                    .setPositiveButton("Clear") { _, _ -> viewModel.clearCart() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        // Toggle between Sell mode and Daily Report mode
        binding.btnDailyReport.setOnClickListener {
            isReportMode = !isReportMode
            if (isReportMode) {
                showDailyReport()
            } else {
                showSellMode()
            }
        }

        binding.btnStaffLogout.setOnClickListener { finish() }
    }

    // ── Checkout ──────────────────────────────────────────────────────────────
    private fun showCheckoutDialog() {
        val total     = viewModel.cartTotal.value ?: 0.0
        val itemCount = viewModel.cartItemCount.value ?: 0

        AlertDialog.Builder(this)
            .setTitle("Confirm Sale")
            .setMessage("Items: $itemCount\nTotal: ${CurrencyFormatter.format(total)}\n\nComplete this sale?")
            .setPositiveButton("Confirm Sale") { _, _ ->
                viewModel.checkout {
                    runOnUiThread {
                        Toast.makeText(this, "Sale recorded!", Toast.LENGTH_SHORT).show()
                        loadTodayStats()
                        showSaleSuccess()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Load today's stats (header bar) ──────────────────────────────────────
    private fun loadTodayStats() {
        lifecycleScope.launch {
            val start = AppRepository.startOfDay()
            val end   = AppRepository.endOfDay()
            val sales = repository.getSalesByDaySync(start, end)
            val total = sales.sumOf { it.totalAmount }
            val count = sales.map { it.transactionId }.toSet().size
            runOnUiThread {
                binding.tvTodayTotal.text = CurrencyFormatter.format(total)
                binding.tvTodayTransactions.text = "$count sales today"
            }
        }
    }

    // ── Daily Report mode ─────────────────────────────────────────────────────
    private fun showDailyReport() {
        binding.btnDailyReport.text = "← Back to Sales"
        binding.layoutSellMode.visibility = View.GONE
        binding.layoutReportMode.visibility = View.VISIBLE

        lifecycleScope.launch {
            val start = AppRepository.startOfDay()
            val end   = AppRepository.endOfDay()
            val sales = repository.getSalesByDaySync(start, end)

            runOnUiThread {
                bindDailyReport(sales)
            }
        }
    }

    private fun bindDailyReport(sales: List<Sale>) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        binding.tvReportDate.text = "Daily Report — ${sdf.format(Date())}"

        // Summary
        val totalRevenue = sales.sumOf { it.totalAmount }
        val txCount = sales.map { it.transactionId }.toSet().size
        val itemsSold = sales.sumOf { it.quantity }

        binding.tvReportTotalRevenue.text = CurrencyFormatter.format(totalRevenue)
        binding.tvReportTxCount.text = "$txCount transactions"
        binding.tvReportItemsSold.text = "$itemsSold items sold"

        // Product breakdown — group by product name, sum quantities and amounts
        val productMap = mutableMapOf<String, Pair<Int, Double>>() // name → (qty, amount)
        sales.forEach { sale ->
            val existing = productMap[sale.productName] ?: Pair(0, 0.0)
            productMap[sale.productName] = Pair(
                existing.first + sale.quantity,
                existing.second + sale.totalAmount
            )
        }

        // Build product breakdown text
        val breakdown = StringBuilder()
        productMap.entries.sortedByDescending { it.value.first }.forEach { (name, data) ->
            breakdown.append("• $name  ×${data.first}  —  ${CurrencyFormatter.format(data.second)}\n")
        }
        binding.tvProductBreakdown.text = if (breakdown.isNotEmpty())
            breakdown.toString().trimEnd()
        else
            "No sales recorded today"

        // Transaction list — group by transactionId, show time + total
        val txGroups = sales.groupBy { it.transactionId }
            .entries.sortedByDescending { it.value.first().saleDate }

        val txLines = StringBuilder()
        txGroups.forEachIndexed { index, (_, txSales) ->
            val time = timeFmt.format(Date(txSales.first().saleDate))
            val txTotal = txSales.sumOf { it.totalAmount }
            val items = txSales.joinToString(", ") { "${it.productName} ×${it.quantity}" }
            txLines.append("${index + 1}. $time  —  ${CurrencyFormatter.format(txTotal)}\n   $items\n\n")
        }
        binding.tvTransactionList.text = if (txLines.isNotEmpty())
            txLines.toString().trimEnd()
        else
            "No transactions yet"
    }

    private fun showSellMode() {
        binding.btnDailyReport.text = "📋 Daily Report"
        binding.layoutSellMode.visibility = View.VISIBLE
        binding.layoutReportMode.visibility = View.GONE
    }

    // ── Sale success animation ────────────────────────────────────────────────
    private fun showSaleSuccess() {
        binding.layoutStaffSaleSuccess.visibility = View.VISIBLE
        binding.layoutStaffSaleSuccess.animate().alpha(1f).setDuration(300)
            .withEndAction {
                binding.layoutStaffSaleSuccess.postDelayed({
                    binding.layoutStaffSaleSuccess.animate().alpha(0f).setDuration(300)
                        .withEndAction {
                            binding.layoutStaffSaleSuccess.visibility = View.GONE
                        }.start()
                }, 1500)
            }.start()
    }
}
