package com.smallbiz.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val sellingPrice: Double,
    val costPrice: Double,
    val totalAmount: Double,        // quantity * sellingPrice
    val totalCost: Double,          // quantity * costPrice
    val profit: Double,             // totalAmount - totalCost
    val transactionId: String,      // Groups items in one customer transaction
    val saleDate: Long = System.currentTimeMillis()
)
