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
        bindRow(binding.rowDailySales.root,    "Gross Sales",    s.totalSales,    R.color.text_primary)
        bindRow(binding.rowDailyCost.root,     "Cost of Goods",  s.totalCost,     R.color.text_secondary)
        bindRow(binding.rowDailyGross.root,    "Gross Profit",   s.grossProfit,   R.color.green_500)
        bindRow(binding.rowDailyExpenses.root, "Expenses",       s.totalExpenses, R.color.red_400)
        binding.tvDailyNetProfit.text = CurrencyFormatter.format(s.netProfit)
        colorize(binding.tvDailyNetProfit, s.netProfit)
    }

    private fun bindWeekly(s: ReportSummary) {
        binding.tvWeeklyPeriod.text = s.period
        binding.tvWeeklyTransactions.text = "${s.transactionCount} transactions"
        bindRow(binding.rowWeeklySales.root,    "Gross Sales",    s.totalSales,    R.color.text_primary)
        bindRow(binding.rowWeeklyCost.root,     "Cost of Goods",  s.totalCost,     R.color.text_secondary)
        bindRow(binding.rowWeeklyGross.root,    "Gross Profit",   s.grossProfit,   R.color.green_500)
        bindRow(binding.rowWeeklyExpenses.root, "Expenses",       s.totalExpenses, R.color.red_400)
        binding.tvWeeklyNetProfit.text = CurrencyFormatter.format(s.netProfit)
        colorize(binding.tvWeeklyNetProfit, s.netProfit)
    }

    private fun bindMonthly(s: ReportSummary) {
        binding.tvMonthlyPeriod.text = s.period
        binding.tvMonthlyTransactions.text = "${s.transactionCount} transactions"
        bindRow(binding.rowMonthlySales.root,    "Gross Sales",    s.totalSales,    R.color.text_primary)
        bindRow(binding.rowMonthlyCost.root,     "Cost of Goods",  s.totalCost,     R.color.text_secondary)
        bindRow(binding.rowMonthlyGross.root,    "Gross Profit",   s.grossProfit,   R.color.green_500)
        bindRow(binding.rowMonthlyExpenses.root, "Expenses",       s.totalExpenses, R.color.red_400)
        binding.tvMonthlyNetProfit.text = CurrencyFormatter.format(s.netProfit)
        colorize(binding.tvMonthlyNetProfit, s.netProfit)
    }

    private fun bindRow(view: View, label: String, value: Double, colorRes: Int) {
        view.findViewById<TextView>(R.id.tvRowLabel).text = label
        view.findViewById<TextView>(R.id.tvRowValue).apply {
            text = CurrencyFormatter.format(value)
            setTextColor(getColor(colorRes))
        }
    }

    private fun colorize(view: TextView, value: Double) {
        view.setTextColor(getColor(if (value >= 0) R.color.green_500 else R.color.red_400))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
