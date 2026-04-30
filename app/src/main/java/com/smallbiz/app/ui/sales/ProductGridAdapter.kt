package com.smallbiz.app.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smallbiz.app.R
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.databinding.ItemProductGridBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.io.File

class ProductGridAdapter(
    private val onAddToCart: (Product) -> Unit
) : ListAdapter<Product, ProductGridAdapter.ProductViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = CurrencyFormatter.format(product.sellingPrice)

            // Load product image
            if (!product.imagePath.isNullOrEmpty()) {
                val file = File(product.imagePath)
                if (file.exists()) {
                    Glide.with(binding.ivProduct)
                        .load(file)
                        .centerCrop()
                        .placeholder(R.drawable.ic_product_placeholder)
                        .into(binding.ivProduct)
                } else {
                    binding.ivProduct.setImageResource(R.drawable.ic_product_placeholder)
                }
            } else {
                binding.ivProduct.setImageResource(R.drawable.ic_product_placeholder)
            }

            binding.btnAddToCart.setOnClickListener {
                onAddToCart(product)
                // Quick visual feedback
                binding.btnAddToCart.animate()
                    .scaleX(0.85f).scaleY(0.85f).setDuration(80)
                    .withEndAction {
                        binding.btnAddToCart.animate()
                            .scaleX(1f).scaleY(1f).setDuration(80).start()
                    }.start()
            }

            binding.root.setOnClickListener { onAddToCart(product) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
            override fun areContentsTheSame(old: Product, new: Product) = old == new
        }
    }
}
