package com.smallbiz.app.ui.reports

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.smallbiz.app.databinding.ActivityStockReportBinding
import com.smallbiz.app.utils.CurrencyFormatter
import com.smallbiz.app.utils.PrefsManager
import java.text.SimpleDateFormat
import java.util.*

class StockReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStockReportBinding
    private val viewModel: StockReportViewModel by viewModels()
    private lateinit var adapter: StockReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PrefsManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Daily Stock Report"

        // Date subtitle
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        binding.tvReportDate.text = sdf.format(Date())
        binding.tvBusinessName.text = prefs.getBusinessName()

        adapter = StockReportAdapter()
        binding.rvStockReport.apply {
            layoutManager = LinearLayoutManager(this@StockReportActivity)
            adapter = this@StockReportActivity.adapter
        }

        setupObservers()
        viewModel.loadStockReport()

        binding.btnRefresh.setOnClickListener {
            viewModel.loadStockReport()
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.rvStockReport.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.stockItems.observe(this) { items ->
            adapter.submitList(items)
            binding.tvNoStock.visibility =
                if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        // Summary totals
        viewModel.totalOpeningStock.observe(this) {
            binding.tvTotalOpeningStock.text = it.toString()
        }
        viewModel.totalSoldToday.observe(this) {
            binding.tvTotalSoldToday.text = it.toString()
        }
        viewModel.totalRemaining.observe(this) {
            binding.tvTotalRemaining.text = it.toString()
        }
        viewModel.totalPurchaseValueRemaining.observe(this) {
            binding.tvTotalPurchaseValue.text = CurrencyFormatter.format(it)
        }
        viewModel.totalSalesValueRemaining.observe(this) {
            binding.tvTotalSalesValue.text = CurrencyFormatter.format(it)
        }
        viewModel.totalProfitValueRemaining.observe(this) {
            binding.tvTotalProfitValue.text = CurrencyFormatter.format(it)
            binding.tvTotalProfitValue.setTextColor(
                getColor(if (it >= 0) com.smallbiz.app.R.color.green_500 else com.smallbiz.app.R.color.red_400)
            )
        }
        viewModel.totalTodaySales.observe(this) {
            binding.tvSummaryTodaySales.text = CurrencyFormatter.format(it)
        }
        viewModel.totalTodayProfit.observe(this) {
            binding.tvSummaryTodayProfit.text = CurrencyFormatter.format(it)
            binding.tvSummaryTodayProfit.setTextColor(
                getColor(if (it >= 0) com.smallbiz.app.R.color.green_500 else com.smallbiz.app.R.color.red_400)
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
