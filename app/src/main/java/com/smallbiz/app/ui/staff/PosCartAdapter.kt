package com.smallbiz.app.ui.staff

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smallbiz.app.R
import com.smallbiz.app.data.model.CartItem
import com.smallbiz.app.databinding.ItemPosCartBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PosCartAdapter(
    private val onIncrement: (Long) -> Unit,
    private val onDecrement: (Long) -> Unit,
    private val onRemove: (Long) -> Unit
) : ListAdapter<CartItem, PosCartAdapter.VH>(DIFF) {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPosCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemPosCartBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: CartItem) {
            b.tvCartPosName.text = item.product.name
            b.tvCartPosPrice.text = CurrencyFormatter.format(item.product.sellingPrice)
            b.tvCartPosQty.text = item.quantity.toString()
            b.tvCartPosSubtotal.text = CurrencyFormatter.format(item.subtotal)
            b.tvCartPosTime.text = timeFmt.format(Date())

            // Image
            if (!item.product.imagePath.isNullOrEmpty()) {
                val file = File(item.product.imagePath)
                if (file.exists()) {
                    Glide.with(b.ivCartPosItem)
                        .load(file).centerCrop()
                        .placeholder(R.drawable.ic_product_placeholder)
                        .into(b.ivCartPosItem)
                } else {
                    b.ivCartPosItem.setImageResource(R.drawable.ic_product_placeholder)
                }
            } else {
                b.ivCartPosItem.setImageResource(R.drawable.ic_product_placeholder)
            }

            b.btnCartPosIncrease.setOnClickListener { onIncrement(item.product.id) }
            b.btnCartPosDecrease.setOnClickListener { onDecrement(item.product.id) }
            b.btnCartPosRemove.setOnClickListener { onRemove(item.product.id) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CartItem>() {
            override fun areItemsTheSame(a: CartItem, b: CartItem) = a.product.id == b.product.id
            override fun areContentsTheSame(a: CartItem, b: CartItem) =
                a.quantity == b.quantity && a.product == b.product
        }
    }
}
