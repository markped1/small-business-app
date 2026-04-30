package com.smallbiz.app.ui.remote

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smallbiz.app.databinding.ItemRemoteSaleBinding
import com.smallbiz.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class RemoteSalesAdapter(
    private val sales: List<Map<String, Any>>
) : RecyclerView.Adapter<RemoteSalesAdapter.VH>() {

    private val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRemoteSaleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(sales[position])
    override fun getItemCount() = sales.size

    inner class VH(private val b: ItemRemoteSaleBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(sale: Map<String, Any>) {
            b.tvRemoteProductName.text = sale["productName"] as? String ?: "—"
            val qty = (sale["quantity"] as? Number)?.toInt() ?: 0
            val total = (sale["totalAmount"] as? Number)?.toDouble() ?: 0.0
            val profit = (sale["profit"] as? Number)?.toDouble() ?: 0.0
            val date = (sale["saleDate"] as? Number)?.toLong() ?: 0L
            b.tvRemoteQty.text = "×$qty"
            b.tvRemoteTotal.text = CurrencyFormatter.format(total)
            b.tvRemoteProfit.text = CurrencyFormatter.format(profit)
            b.tvRemoteDate.text = if (date > 0) sdf.format(Date(date)) else "—"
            b.tvRemoteProfit.setTextColor(
                b.root.context.getColor(
                    if (profit >= 0) com.smallbiz.app.R.color.green_500
                    else com.smallbiz.app.R.color.red_400
                )
            )
        }
    }
}
