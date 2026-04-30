package com.smallbiz.app.data.model

data class ReportSummary(
    val period: String,
    val totalSales: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val transactionCount: Int
)
