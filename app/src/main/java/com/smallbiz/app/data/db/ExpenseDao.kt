package com.smallbiz.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smallbiz.app.data.model.Expense

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses WHERE expenseDate BETWEEN :start AND :end ORDER BY expenseDate DESC")
    fun getExpensesByPeriod(start: Long, end: Long): LiveData<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE expenseDate BETWEEN :start AND :end")
    suspend fun getTotalExpensesForPeriod(start: Long, end: Long): Double

    @Query("SELECT * FROM expenses WHERE expenseDate BETWEEN :start AND :end ORDER BY expenseDate DESC")
    suspend fun getExpensesByPeriodSync(start: Long, end: Long): List<Expense>
}
