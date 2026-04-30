package com.smallbiz.app.ui.sales

import android.app.Application
import androidx.lifecycle.*
import com.smallbiz.app.data.model.CartItem
import com.smallbiz.app.data.model.Product
import com.smallbiz.app.data.model.Sale
import com.smallbiz.app.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.util.UUID

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    val products: LiveData<List<Product>> = repository.allActiveProducts

    // Cart state
    private val _cart = MutableLiveData<MutableList<CartItem>>(mutableListOf())
    val cart: LiveData<MutableList<CartItem>> = _cart

    val cartTotal: LiveData<Double> = _cart.map { items ->
        items.sumOf { it.subtotal }
    }

    val cartItemCount: LiveData<Int> = _cart.map { items ->
        items.sumOf { it.quantity }
    }

    fun addToCart(product: Product) {
        val current = _cart.value ?: mutableListOf()
        val existing = current.find { it.product.id == product.id }
        if (existing != null) {
            existing.quantity++
        } else {
            current.add(CartItem(product, 1))
        }
        _cart.value = current
    }

    fun incrementItem(productId: Long) {
        val current = _cart.value ?: return
        current.find { it.product.id == productId }?.let { it.quantity++ }
        _cart.value = current
    }

    fun decrementItem(productId: Long) {
        val current = _cart.value ?: return
        val item = current.find { it.product.id == productId } ?: return
        if (item.quantity > 1) {
            item.quantity--
        } else {
            current.remove(item)
        }
        _cart.value = current
    }

    fun removeFromCart(productId: Long) {
        val current = _cart.value ?: return
        current.removeAll { it.product.id == productId }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = mutableListOf()
    }

    fun checkout(onComplete: () -> Unit) {
        val items = _cart.value ?: return
        if (items.isEmpty()) return

        val transactionId = UUID.randomUUID().toString()
        val sales = items.map { cartItem ->
            Sale(
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                quantity = cartItem.quantity,
                sellingPrice = cartItem.product.sellingPrice,
                costPrice = cartItem.product.costPrice,
                totalAmount = cartItem.subtotal,
                totalCost = cartItem.subtotalCost,
                profit = cartItem.subtotalProfit,
                transactionId = transactionId
            )
        }

        viewModelScope.launch {
            repository.insertSales(sales)
            clearCart()
            onComplete()
        }
    }
}
