package com.smallbiz.app.data.repository

import android.content.Context
import com.smallbiz.app.data.db.AppDatabase
import com.smallbiz.app.data.model.*
import java.util.*

class AppRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val productDao = db.productDao()
    private val saleDao = db.saleDao()
    private val expenseDao = db.expenseDao()

    // ─── Products ────────────────────────────────────────────────────────────
    val allActiveProducts = productDao.getAllActiveProducts()
    val allProducts = productDao.getAllProducts()

    suspend fun insertProduct(product: Product) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    suspend fun deactivateProduct(id: Long) = productDao.deactivateProduct(id)
    suspend fun getProductById(id: Long) = productDao.getProductById(id)

    // ─── Sales ───────────────────────────────────────────────────────────────
    val allSales = saleDao.getAllSales()

    suspend fun insertSales(sales: List<Sale>) = saleDao.insertSales(sales)

    fun getSalesByDay(startOfDay: Long, endOfDay: Long) =
        saleDao.getSalesByDay(startOfDay, endOfDay)

    suspend fun getReportSummary(start: Long, end: Long, periodLabel: String): ReportSummary {
        val totalSales = saleDao.getTotalSalesForPeriod(start, end)
        val totalCost = saleDao.getTotalCostForPeriod(start, end)
        val grossProfit = saleDao.getTotalProfitForPeriod(start, end)
        val totalExpenses = expenseDao.getTotalExpensesForPeriod(start, end)
        val netProfit = grossProfit - totalExpenses
        val txCount = saleDao.getTransactionCountForPeriod(start, end)
        return ReportSummary(
            period = periodLabel,
            totalSales = totalSales,
            totalCost = totalCost,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            transactionCount = txCount
        )
    }

    suspend fun getSalesByWeek(start: Long, end: Long) = saleDao.getSalesByWeek(start, end)
    suspend fun getSalesByMonth(start: Long, end: Long) = saleDao.getSalesByMonth(start, end)
    suspend fun getSalesByDaySync(start: Long, end: Long) = saleDao.getSalesByDaySync(start, end)

    // ─── Stock Report ─────────────────────────────────────────────────────────
    /**
     * Builds a per-product stock report for today.
     * For each active product:
     *   - openingStock  = product.stockQuantity (set by admin)
     *   - qtySoldToday  = sum of quantities sold today
     *   - remaining     = openingStock - qtySoldToday
     *   - purchaseValue = remaining * costPrice
     *   - salesValue    = remaining * sellingPrice
     *   - profitValue   = salesValue - purchaseValue
     */
    suspend fun getDailyStockReport(): List<StockReportItem> {
        val start = startOfDay()
        val end = endOfDay()
        val products = productDao.getAllActiveProductsSync()
        return products.map { product ->
            val qtySold = saleDao.getQtySoldForProduct(product.id, start, end)
            val todaySales = saleDao.getSalesAmountForProduct(product.id, start, end)
            val todayCost = saleDao.getCostAmountForProduct(product.id, start, end)
            val remaining = maxOf(0, product.stockQuantity - qtySold)
            StockReportItem(
                product = product,
                openingStock = product.stockQuantity,
                qtySoldToday = qtySold,
                remainingStock = remaining,
                purchaseValueRemaining = remaining * product.costPrice,
                salesValueRemaining = remaining * product.sellingPrice,
                profitValueRemaining = remaining * (product.sellingPrice - product.costPrice),
                todaySalesAmount = todaySales,
                todayCostAmount = todayCost,
                todayProfit = todaySales - todayCost
            )
        }
    }

    // ─── Expenses ────────────────────────────────────────────────────────────
    val allExpenses = expenseDao.getAllExpenses()

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    fun getExpensesByPeriod(start: Long, end: Long) = expenseDao.getExpensesByPeriod(start, end)
    suspend fun getExpensesByPeriodSync(start: Long, end: Long) =
        expenseDao.getExpensesByPeriodSync(start, end)

    // ─── Date helpers ────────────────────────────────────────────────────────
    companion object {
        fun startOfDay(date: Calendar = Calendar.getInstance()): Long {
            val c = date.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        fun endOfDay(date: Calendar = Calendar.getInstance()): Long {
            val c = date.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
            return c.timeInMillis
        }

        fun startOfWeek(): Long {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
            return startOfDay(c)
        }

        fun endOfWeek(): Long {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
            c.add(Calendar.DAY_OF_WEEK, 6)
            return endOfDay(c)
        }

        fun startOfMonth(): Long {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, 1)
            return startOfDay(c)
        }

        fun endOfMonth(): Long {
            val c = Calendar.getInstance()
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
            return endOfDay(c)
        }
    }
}
