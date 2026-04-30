package com.smallbiz.app.ui.admin

import android.app.Application
import androidx.lifecycle.*
import com.smallbiz.app.data.model.Expense
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.data.repository.AppRepository
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    val allProducts: LiveData<List<Product>> = repository.allProducts
    val allExpenses: LiveData<List<Expense>> = repository.allExpenses

    fun insertProduct(product: Product) = viewModelScope.launch {
        repository.insertProduct(product)
    }

    fun updateProduct(product: Product) = viewModelScope.launch {
        repository.updateProduct(product)
    }

    fun deactivateProduct(id: Long) = viewModelScope.launch {
        repository.deactivateProduct(id)
    }

    fun deleteProduct(product: Product) = viewModelScope.launch {
        repository.deleteProduct(product)
    }

    fun insertExpense(expense: Expense) = viewModelScope.launch {
        repository.insertExpense(expense)
    }

    fun updateExpense(expense: Expense) = viewModelScope.launch {
        repository.updateExpense(expense)
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch {
        repository.deleteExpense(expense)
    }
}
