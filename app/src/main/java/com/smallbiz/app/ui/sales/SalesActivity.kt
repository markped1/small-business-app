package com.smallbiz.app.ui.sales

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.smallbiz.app.R
import com.smallbiz.app.databinding.ActivitySalesBinding
import com.smallbiz.app.ui.admin.AdminLoginActivity
import com.smallbiz.app.ui.reports.ReportsActivity
import com.smallbiz.app.utils.CurrencyFormatter
import com.smallbiz.app.utils.PrefsManager

class SalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesBinding
    private val viewModel: SalesViewModel by viewModels()
    private lateinit var productAdapter: ProductGridAdapter
    private lateinit var cartAdapter: CartAdapter
    private lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        setupToolbar()
        setupProductGrid()
        setupCart()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.tvBusinessName.text = prefs.getBusinessName()
        binding.tvBusinessAddress.text = prefs.getBusinessAddress()
    }

    private fun setupProductGrid() {
        productAdapter = ProductGridAdapter { product ->
            viewModel.addToCart(product)
        }
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(this@SalesActivity, 2)
            adapter = productAdapter
        }
    }

    private fun setupCart() {
        cartAdapter = CartAdapter(
            onIncrement = { productId -> viewModel.incrementItem(productId) },
            onDecrement = { productId -> viewModel.decrementItem(productId) },
            onRemove = { productId -> viewModel.removeFromCart(productId) }
        )
        binding.rvCart.adapter = cartAdapter
    }

    private fun setupObservers() {
        viewModel.products.observe(this) { products ->
            productAdapter.submitList(products)
            binding.tvNoProducts.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.cart.observe(this) { cartItems ->
            cartAdapter.submitList(cartItems.toList())
            val isEmpty = cartItems.isEmpty()
            binding.layoutCartEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvCart.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.btnCheckout.isEnabled = !isEmpty
        }

        viewModel.cartTotal.observe(this) { total ->
            binding.tvCartTotal.text = CurrencyFormatter.format(total)
        }

        viewModel.cartItemCount.observe(this) { count ->
            binding.tvCartBadge.text = count.toString()
            binding.tvCartBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnCheckout.setOnClickListener {
            showCheckoutDialog()
        }

        binding.btnClearCart.setOnClickListener {
            if ((viewModel.cart.value?.size ?: 0) > 0) {
                AlertDialog.Builder(this)
                    .setTitle("Clear Cart")
                    .setMessage("Remove all items from cart?")
                    .setPositiveButton("Clear") { _, _ -> viewModel.clearCart() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        binding.fabAdmin.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }

        binding.btnReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }
    }

    private fun showCheckoutDialog() {
        val total = viewModel.cartTotal.value ?: 0.0
        val itemCount = viewModel.cartItemCount.value ?: 0

        AlertDialog.Builder(this)
            .setTitle("Confirm Sale")
            .setMessage(
                "Items: $itemCount\n" +
                "Total: ${CurrencyFormatter.format(total)}\n\n" +
                "Complete this sale?"
            )
            .setPositiveButton("Confirm Sale") { _, _ ->
                viewModel.checkout {
                    runOnUiThread {
                        Toast.makeText(this, "Sale recorded successfully!", Toast.LENGTH_SHORT).show()
                        showSaleSuccessAnimation()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSaleSuccessAnimation() {
        binding.layoutSaleSuccess.visibility = View.VISIBLE
        binding.layoutSaleSuccess.animate()
            .alpha(1f)
            .setDuration(300)
            .withEndAction {
                binding.layoutSaleSuccess.postDelayed({
                    binding.layoutSaleSuccess.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            binding.layoutSaleSuccess.visibility = View.GONE
                        }.start()
                }, 1500)
            }.start()
    }
}
