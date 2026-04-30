package com.smallbiz.app.ui.sales

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smallbiz.app.R
import com.smallbiz.app.data.model.CartItem
import com.smallbiz.app.databinding.ItemCartBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.io.File

class CartAdapter(
    private val onIncrement: (Long) -> Unit,
    private val onDecrement: (Long) -> Unit,
    private val onRemove: (Long) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.tvCartItemName.text = item.product.name
            binding.tvCartItemPrice.text = CurrencyFormatter.format(item.product.sellingPrice)
            binding.tvCartItemQty.text = item.quantity.toString()
            binding.tvCartItemSubtotal.text = CurrencyFormatter.format(item.subtotal)

            // Load image
            if (!item.product.imagePath.isNullOrEmpty()) {
                val file = File(item.product.imagePath)
                if (file.exists()) {
                    Glide.with(binding.ivCartItem)
                        .load(file)
                        .centerCrop()
                        .placeholder(R.drawable.ic_product_placeholder)
                        .into(binding.ivCartItem)
                } else {
                    binding.ivCartItem.setImageResource(R.drawable.ic_product_placeholder)
                }
            } else {
                binding.ivCartItem.setImageResource(R.drawable.ic_product_placeholder)
            }

            binding.btnIncrease.setOnClickListener { onIncrement(item.product.id) }
            binding.btnDecrease.setOnClickListener { onDecrement(item.product.id) }
            binding.btnRemoveItem.setOnClickListener { onRemove(item.product.id) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CartItem>() {
            override fun areItemsTheSame(old: CartItem, new: CartItem) =
                old.product.id == new.product.id
            override fun areContentsTheSame(old: CartItem, new: CartItem) =
                old.quantity == new.quantity && old.product == new.product
        }
    }
}
