package com.smallbiz.app.ui.reports

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smallbiz.app.R
import com.smallbiz.app.data.model.ReportSummary
import com.smallbiz.app.databinding.ActivityReportsBinding
import com.smallbiz.app.utils.CurrencyFormatter
import com.smallbiz.app.utils.PrefsManager

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private val viewModel: ReportsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PrefsManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sales Reports – ${prefs.getBusinessName()}"

        setupObservers()
        viewModel.loadAllReports()
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.dailySummary.observe(this) { bindDaily(it) }
        viewModel.weeklySummary.observe(this) { bindWeekly(it) }
        viewModel.monthlySummary.observe(this) { bindMonthly(it) }
    }

    private fun bindDaily(s: ReportSummary) {
        binding.tvDailyPeriod.text = s.period
        binding.tvDailyTransactions.text = "${s.transactionCount} transactions"
        binding.tvDailySales.text = CurrencyFormatter.format(s.totalSales)
        binding.tvDailyCost.text = CurrencyFormatter.format(s.totalCost)
        binding.tvDailyGrossProfit.text = CurrencyFormatter.format(s.grossProfit)
        binding.tvDailyExpenses.text = CurrencyFormatter.format(s.totalExpenses)
        binding.tvDailyNetProfit.text = CurrencyFormatter.format(s.netProfit)
        colorize(binding.tvDailyNetProfit, s.netProfit)
    }

    private fun bindWeekly(s: ReportSummary) {
        binding.tvWeeklyPeriod.text = s.period
        binding.tvWeeklyTransactions.text = "${s.transactionCount} transactions"
        binding.tvWeeklySales.text = CurrencyFormatter.format(s.totalSales)
        binding.tvWeeklyCost.text = CurrencyFormatter.format(s.totalCost)
        binding.tvWeeklyGrossProfit.text = CurrencyFormatter.format(s.grossProfit)
        binding.tvWeeklyExpenses.text = CurrencyFormatter.format(s.totalExpenses)
        binding.tvWeeklyNetProfit.text = CurrencyFormatter.format(s.netProfit)
        colorize(binding.tvWeeklyNetProfit, s.netProfit)
    }

    private fun bindMonthly(s: ReportSummary) {
        binding.tvMonthlyPeriod.text = s.period
        binding.tvMonthlyTransactions.text = "${s.transactionCount} transactions"
        binding.tvMonthlySales.text = CurrencyFormatter.format(s.totalSales)
        binding.tvMonthlyCost.text = CurrencyFormatter.format(s.totalCost)
        binding.tvMonthlyGrossProfit.text = CurrencyFormatter.format(s.grossProfit)
        binding.tvMonthlyExpenses.text = CurrencyFormatter.format(s.totalExpenses)
        binding.tvMonthlyNetProfit.text = CurrencyFormatter.format(s.netProfit)
        colorize(binding.tvMonthlyNetProfit, s.netProfit)
    }

    private fun colorize(view: TextView, value: Double) {
        view.setTextColor(getColor(if (value >= 0) R.color.green_500 else R.color.red_400))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
