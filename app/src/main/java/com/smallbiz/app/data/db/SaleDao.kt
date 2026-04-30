package com.smallbiz.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smallbiz.app.data.model.Sale

@Dao
interface SaleDao {

    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Insert
    suspend fun insertSales(sales: List<Sale>)

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun getAllSales(): LiveData<List<Sale>>

    // Daily: sales for a specific day (start and end of day in millis)
    @Query("SELECT * FROM sales WHERE saleDate BETWEEN :startOfDay AND :endOfDay ORDER BY saleDate DESC")
    fun getSalesByDay(startOfDay: Long, endOfDay: Long): LiveData<List<Sale>>

    @Query("SELECT * FROM sales WHERE saleDate BETWEEN :startOfDay AND :endOfDay ORDER BY saleDate DESC")
    suspend fun getSalesByDaySync(startOfDay: Long, endOfDay: Long): List<Sale>

    // Weekly
    @Query("SELECT * FROM sales WHERE saleDate BETWEEN :startOfWeek AND :endOfWeek ORDER BY saleDate DESC")
    suspend fun getSalesByWeek(startOfWeek: Long, endOfWeek: Long): List<Sale>

    // Monthly
    @Query("SELECT * FROM sales WHERE saleDate BETWEEN :startOfMonth AND :endOfMonth ORDER BY saleDate DESC")
    suspend fun getSalesByMonth(startOfMonth: Long, endOfMonth: Long): List<Sale>

    // Totals for a period
    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE saleDate BETWEEN :start AND :end")
    suspend fun getTotalSalesForPeriod(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(totalCost), 0) FROM sales WHERE saleDate BETWEEN :start AND :end")
    suspend fun getTotalCostForPeriod(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(profit), 0) FROM sales WHERE saleDate BETWEEN :start AND :end")
    suspend fun getTotalProfitForPeriod(start: Long, end: Long): Double

    @Query("SELECT COUNT(DISTINCT transactionId) FROM sales WHERE saleDate BETWEEN :start AND :end")
    suspend fun getTransactionCountForPeriod(start: Long, end: Long): Int

    // Stock report: total qty sold for a specific product within a period
    @Query("SELECT COALESCE(SUM(quantity), 0) FROM sales WHERE productId = :productId AND saleDate BETWEEN :start AND :end")
    suspend fun getQtySoldForProduct(productId: Long, start: Long, end: Long): Int

    // Stock report: total sales amount for a specific product within a period
    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM sales WHERE productId = :productId AND saleDate BETWEEN :start AND :end")
    suspend fun getSalesAmountForProduct(productId: Long, start: Long, end: Long): Double

    // Stock report: total cost for a specific product within a period
    @Query("SELECT COALESCE(SUM(totalCost), 0) FROM sales WHERE productId = :productId AND saleDate BETWEEN :start AND :end")
    suspend fun getCostAmountForProduct(productId: Long, start: Long, end: Long): Double

    @Delete
    suspend fun deleteSale(sale: Sale)
}
