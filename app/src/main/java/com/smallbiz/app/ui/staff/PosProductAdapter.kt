package com.smallbiz.app.ui.staff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smallbiz.app.R
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.databinding.ItemPosProductBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.io.File

class PosProductAdapter(
    private val onAddToCart: (Product) -> Unit,
    private val onAlertRestock: (Product) -> Unit
) : ListAdapter<Product, PosProductAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPosProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemPosProductBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(product: Product) {
            b.tvPosProductName.text = product.name
            b.tvPosProductPrice.text = CurrencyFormatter.format(product.sellingPrice)
            b.tvPosCategory.text = product.category

            // Stock status
            val inStock = product.stockQuantity > 0
            if (inStock) {
                b.tvPosStockStatus.text = "${product.stockQuantity} in stock"
                b.tvPosStockStatus.setTextColor(
                    ContextCompat.getColor(b.root.context, R.color.green_500)
                )
                b.btnPosAdd.isEnabled = true
                b.btnPosAdd.alpha = 1f
                b.btnAlertRestock.visibility = View.GONE
                b.root.alpha = 1f
            } else {
                b.tvPosStockStatus.text = "Out of stock"
                b.tvPosStockStatus.setTextColor(
                    ContextCompat.getColor(b.root.context, R.color.red_400)
                )
                b.btnPosAdd.isEnabled = false
                b.btnPosAdd.alpha = 0.4f
                b.btnAlertRestock.visibility = View.VISIBLE
                b.root.alpha = 0.7f
            }

            // Product image
            if (!product.imagePath.isNullOrEmpty()) {
                val file = File(product.imagePath)
                if (file.exists()) {
                    Glide.with(b.ivPosProduct)
                        .load(file)
                        .centerCrop()
                        .placeholder(R.drawable.ic_product_placeholder)
                        .into(b.ivPosProduct)
                } else {
                    b.ivPosProduct.setImageResource(R.drawable.ic_product_placeholder)
                }
            } else {
                b.ivPosProduct.setImageResource(R.drawable.ic_product_placeholder)
            }

            b.btnPosAdd.setOnClickListener {
                onAddToCart(product)
                // Bounce animation
                b.btnPosAdd.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80)
                    .withEndAction {
                        b.btnPosAdd.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    }.start()
            }

            b.btnAlertRestock.setOnClickListener { onAlertRestock(product) }

            // Tap row to add
            b.root.setOnClickListener { if (inStock) onAddToCart(product) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(a: Product, b: Product) = a.id == b.id
            override fun areContentsTheSame(a: Product, b: Product) = a == b
        }
    }
}
