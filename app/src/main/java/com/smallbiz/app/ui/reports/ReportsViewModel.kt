package com.smallbiz.app.ui.reports

import android.app.Application
import androidx.lifecycle.*
import com.smallbiz.app.data.model.ReportSummary
import com.smallbiz.app.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    private val _dailySummary = MutableLiveData<ReportSummary>()
    val dailySummary: LiveData<ReportSummary> = _dailySummary

    private val _weeklySummary = MutableLiveData<ReportSummary>()
    val weeklySummary: LiveData<ReportSummary> = _weeklySummary

    private val _monthlySummary = MutableLiveData<ReportSummary>()
    val monthlySummary: LiveData<ReportSummary> = _monthlySummary

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadAllReports() {
        viewModelScope.launch {
            _isLoading.value = true
            loadDaily()
            loadWeekly()
            loadMonthly()
            _isLoading.value = false
        }
    }

    private suspend fun loadDaily() {
        val start = AppRepository.startOfDay()
        val end = AppRepository.endOfDay()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val label = "Today – ${sdf.format(Date())}"
        _dailySummary.value = repository.getReportSummary(start, end, label)
    }

    private suspend fun loadWeekly() {
        val start = AppRepository.startOfWeek()
        val end = AppRepository.endOfWeek()
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        val label = "This Week (${sdf.format(Date(start))} – ${sdf.format(Date(end))})"
        _weeklySummary.value = repository.getReportSummary(start, end, label)
    }

    private suspend fun loadMonthly() {
        val start = AppRepository.startOfMonth()
        val end = AppRepository.endOfMonth()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val label = sdf.format(Date())
        _monthlySummary.value = repository.getReportSummary(start, end, label)
    }
}
