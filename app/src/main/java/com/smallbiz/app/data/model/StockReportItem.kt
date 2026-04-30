package com.smallbiz.app.data.model

/**
 * Per-product stock snapshot for the daily stock report.
 *
 * @param product           The product this row is about
 * @param openingStock      Total units the admin recorded as in stock
 * @param qtySoldToday      Units sold today (from sales table)
 * @param remainingStock    openingStock - qtySoldToday
 * @param purchaseValueRemaining  remainingStock * costPrice
 * @param salesValueRemaining     remainingStock * sellingPrice
 * @param profitValueRemaining    salesValueRemaining - purchaseValueRemaining
 * @param todaySalesAmount        Revenue earned today for this product
 * @param todayCostAmount         Cost of goods sold today for this product
 * @param todayProfit             todaySalesAmount - todayCostAmount
 */
data class StockReportItem(
    val product: Product,
    val openingStock: Int,
    val qtySoldToday: Int,
    val remainingStock: Int,
    val purchaseValueRemaining: Double,
    val salesValueRemaining: Double,
    val profitValueRemaining: Double,
    val todaySalesAmount: Double,
    val todayCostAmount: Double,
    val todayProfit: Double
)
