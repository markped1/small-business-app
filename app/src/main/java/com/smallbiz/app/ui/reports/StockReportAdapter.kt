package com.smallbiz.app.ui.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.smallbiz.app.R
import com.smallbiz.app.data.model.StockReportItem
import com.smallbiz.app.databinding.ItemStockReportBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.io.File

class StockReportAdapter :
    ListAdapter<StockReportItem, StockReportAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemStockReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StockReportItem) {
            val ctx = binding.root.context

            binding.tvStockProductName.text = item.product.name
            binding.tvStockCategory.text = item.product.category

            // Quantities
            binding.tvOpeningStock.text = item.openingStock.toString()
            binding.tvQtySold.text = item.qtySoldToday.toString()
            binding.tvRemainingStock.text = item.remainingStock.toString()

            // Colour remaining stock: red if low (≤ 20% of opening), green otherwise
            val remainingColor = when {
                item.openingStock == 0 -> R.color.text_secondary
                item.remainingStock == 0 -> R.color.red_400
                item.remainingStock <= (item.openingStock * 0.2) -> R.color.orange_500
                else -> R.color.green_500
            }
            binding.tvRemainingStock.setTextColor(ContextCompat.getColor(ctx, remainingColor))

            // Values
            binding.tvPurchaseValueRemaining.text =
                CurrencyFormatter.format(item.purchaseValueRemaining)
            binding.tvSalesValueRemaining.text =
                CurrencyFormatter.format(item.salesValueRemaining)
            binding.tvProfitValueRemaining.text =
                CurrencyFormatter.format(item.profitValueRemaining)

            // Today's activity
            binding.tvTodaySales.text = CurrencyFormatter.format(item.todaySalesAmount)
            binding.tvTodayCost.text = CurrencyFormatter.format(item.todayCostAmount)
            binding.tvTodayProfit.text = CurrencyFormatter.format(item.todayProfit)

            val profitColor = if (item.todayProfit >= 0) R.color.green_500 else R.color.red_400
            binding.tvTodayProfit.setTextColor(ContextCompat.getColor(ctx, profitColor))
            binding.tvProfitValueRemaining.setTextColor(
                ContextCompat.getColor(ctx, if (item.profitValueRemaining >= 0) R.color.green_500 else R.color.red_400)
            )

            // Product image
            if (!item.product.imagePath.isNullOrEmpty()) {
                val file = File(item.product.imagePath)
                if (file.exists()) {
                    Glide.with(binding.ivStockProduct)
                        .load(file)
                        .centerCrop()
                        .placeholder(R.drawable.ic_product_placeholder)
                        .into(binding.ivStockProduct)
                } else {
                    binding.ivStockProduct.setImageResource(R.drawable.ic_product_placeholder)
                }
            } else {
                binding.ivStockProduct.setImageResource(R.drawable.ic_product_placeholder)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<StockReportItem>() {
            override fun areItemsTheSame(old: StockReportItem, new: StockReportItem) =
                old.product.id == new.product.id
            override fun areContentsTheSame(old: StockReportItem, new: StockReportItem) =
                old == new
        }
    }
}
