package com.smallbiz.app.ui.remote

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.smallbiz.app.databinding.ActivityRemoteViewBinding
import com.smallbiz.app.sync.FirebaseSyncManager
import com.smallbiz.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class RemoteViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemoteViewBinding
    private val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoteViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Live Sales View"

        binding.progressBar.visibility = View.VISIBLE

        // Listen for live sales from Firebase
        FirebaseSyncManager.listenForSales(
            onData = { salesData ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    updateSalesSummary(salesData)
                    updateSalesList(salesData)
                }
            },
            onError = { msg ->
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = "Error: $msg"
                    binding.tvStatus.visibility = View.VISIBLE
                }
            }
        )

        // Listen for live expenses
        FirebaseSyncManager.listenForExpenses(
            onData = { expensesData ->
                runOnUiThread { updateExpensesSummary(expensesData) }
            },
            onError = { /* silent */ }
        )
    }

    private fun updateSalesSummary(sales: List<Map<String, Any>>) {
        if (sales.isEmpty()) {
            binding.tvStatus.text = "No sales recorded yet"
            binding.tvStatus.visibility = View.VISIBLE
            binding.cardSummary.visibility = View.GONE
            return
        }

        binding.tvStatus.visibility = View.GONE
        binding.cardSummary.visibility = View.VISIBLE

        val totalRevenue = sales.sumOf { (it["totalAmount"] as? Number)?.toDouble() ?: 0.0 }
        val totalProfit  = sales.sumOf { (it["profit"]      as? Number)?.toDouble() ?: 0.0 }
        val totalCost    = sales.sumOf { (it["totalCost"]   as? Number)?.toDouble() ?: 0.0 }
        val txCount      = sales.map { it["transactionId"] as? String ?: "" }.toSet().size

        binding.tvLiveTotalSales.text    = CurrencyFormatter.format(totalRevenue)
        binding.tvLiveTotalCost.text     = CurrencyFormatter.format(totalCost)
        binding.tvLiveTotalProfit.text   = CurrencyFormatter.format(totalProfit)
        binding.tvLiveTransactions.text  = "$txCount transactions"
        binding.tvLastUpdated.text       = "Last updated: ${sdf.format(Date())}"

        binding.tvLiveTotalProfit.setTextColor(
            getColor(if (totalProfit >= 0) com.smallbiz.app.R.color.green_500 else com.smallbiz.app.R.color.red_400)
        )
    }

    private fun updateExpensesSummary(expenses: List<Map<String, Any>>) {
        val total = expenses.sumOf { (it["amount"] as? Number)?.toDouble() ?: 0.0 }
        binding.tvLiveTotalExpenses.text = CurrencyFormatter.format(total)
        val profit = (binding.tvLiveTotalProfit.text.toString()
            .replace(CurrencyFormatter.getCurrencySymbol(), "")
            .replace(",", "").toDoubleOrNull() ?: 0.0) - total
        binding.tvLiveNetProfit.text = CurrencyFormatter.format(profit)
        binding.tvLiveNetProfit.setTextColor(
            getColor(if (profit >= 0) com.smallbiz.app.R.color.green_500 else com.smallbiz.app.R.color.red_400)
        )
    }

    private fun updateSalesList(sales: List<Map<String, Any>>) {
        // Group by transactionId and show most recent first
        val sorted = sales.sortedByDescending { (it["saleDate"] as? Number)?.toLong() ?: 0L }
        val adapter = RemoteSalesAdapter(sorted)
        binding.rvRemoteSales.layoutManager = LinearLayoutManager(this)
        binding.rvRemoteSales.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
