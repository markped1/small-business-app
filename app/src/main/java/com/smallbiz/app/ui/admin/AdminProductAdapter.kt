package com.smallbiz.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smallbiz.app.R
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.databinding.ItemAdminProductBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.io.File

class AdminProductAdapter(
    private val onEdit: (Product) -> Unit,
    private val onToggleActive: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : ListAdapter<Product, AdminProductAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAdminProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvAdminProductName.text = product.name
            binding.tvAdminSellingPrice.text = "Sell: ${CurrencyFormatter.format(product.sellingPrice)}"
            binding.tvAdminCostPrice.text = "Cost: ${CurrencyFormatter.format(product.costPrice)}"
            val profit = product.sellingPrice - product.costPrice
            binding.tvAdminProfit.text = "Profit/unit: ${CurrencyFormatter.format(profit)}"
            binding.tvAdminCategory.text = product.category
            binding.tvAdminStockQty.text = "${product.stockQuantity} units"
            // Colour stock qty: red if 0, orange if low
            val stockColor = when {
                product.stockQuantity == 0 -> R.color.red_400
                product.stockQuantity <= 5 -> R.color.orange_500
                else -> R.color.primary
            }
            binding.tvAdminStockQty.setTextColor(
                ContextCompat.getColor(binding.root.context, stockColor)
            )

            // Status indicator
            if (product.isActive) {
                binding.tvStatus.text = "Active"
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.green_500)
                )
                binding.btnToggleActive.text = "Deactivate"
            } else {
                binding.tvStatus.text = "Inactive"
                binding.tvStatus.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.red_400)
                )
                binding.btnToggleActive.text = "Activate"
            }

            // Image
            if (!product.imagePath.isNullOrEmpty()) {
                val file = File(product.imagePath)
                if (file.exists()) {
                    Glide.with(binding.ivAdminProduct)
                        .load(file)
                        .centerCrop()
                        .placeholder(R.drawable.ic_product_placeholder)
                        .into(binding.ivAdminProduct)
                } else {
                    binding.ivAdminProduct.setImageResource(R.drawable.ic_product_placeholder)
                }
            } else {
                binding.ivAdminProduct.setImageResource(R.drawable.ic_product_placeholder)
            }

            binding.btnEditProduct.setOnClickListener { onEdit(product) }
            binding.btnToggleActive.setOnClickListener { onToggleActive(product) }
            binding.btnDeleteProduct.setOnClickListener { onDelete(product) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
            override fun areContentsTheSame(old: Product, new: Product) = old == new
        }
    }
}
