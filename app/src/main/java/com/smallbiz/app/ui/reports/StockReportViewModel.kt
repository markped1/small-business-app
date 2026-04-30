package com.smallbiz.app.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smallbiz.app.data.model.StockReportItem
import com.smallbiz.app.data.repository.AppRepository
import kotlinx.coroutines.launch

class StockReportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _stockItems = MutableLiveData<List<StockReportItem>>()
    val stockItems: LiveData<List<StockReportItem>> = _stockItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Totals derived from the list
    private val _totalOpeningStock = MutableLiveData(0)
    val totalOpeningStock: LiveData<Int> = _totalOpeningStock

    private val _totalSoldToday = MutableLiveData(0)
    val totalSoldToday: LiveData<Int> = _totalSoldToday

    private val _totalRemaining = MutableLiveData(0)
    val totalRemaining: LiveData<Int> = _totalRemaining

    private val _totalPurchaseValueRemaining = MutableLiveData(0.0)
    val totalPurchaseValueRemaining: LiveData<Double> = _totalPurchaseValueRemaining

    private val _totalSalesValueRemaining = MutableLiveData(0.0)
    val totalSalesValueRemaining: LiveData<Double> = _totalSalesValueRemaining

    private val _totalProfitValueRemaining = MutableLiveData(0.0)
    val totalProfitValueRemaining: LiveData<Double> = _totalProfitValueRemaining

    private val _totalTodaySales = MutableLiveData(0.0)
    val totalTodaySales: LiveData<Double> = _totalTodaySales

    private val _totalTodayProfit = MutableLiveData(0.0)
    val totalTodayProfit: LiveData<Double> = _totalTodayProfit

    fun loadStockReport() {
        viewModelScope.launch {
            _isLoading.value = true
            val items = repository.getDailyStockReport()
            _stockItems.value = items

            // Compute totals
            _totalOpeningStock.value = items.sumOf { it.openingStock }
            _totalSoldToday.value = items.sumOf { it.qtySoldToday }
            _totalRemaining.value = items.sumOf { it.remainingStock }
            _totalPurchaseValueRemaining.value = items.sumOf { it.purchaseValueRemaining }
            _totalSalesValueRemaining.value = items.sumOf { it.salesValueRemaining }
            _totalProfitValueRemaining.value = items.sumOf { it.profitValueRemaining }
            _totalTodaySales.value = items.sumOf { it.todaySalesAmount }
            _totalTodayProfit.value = items.sumOf { it.todayProfit }

            _isLoading.value = false
        }
    }
}
