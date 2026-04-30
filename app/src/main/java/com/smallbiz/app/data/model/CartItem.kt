package com.smallbiz.app.data.model

data class CartItem(
    val product: Product,
    var quantity: Int = 1
) {
    val subtotal: Double get() = product.sellingPrice * quantity
    val subtotalCost: Double get() = product.costPrice * quantity
    val subtotalProfit: Double get() = subtotal - subtotalCost
}
