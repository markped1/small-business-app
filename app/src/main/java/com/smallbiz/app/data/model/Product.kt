package com.smallbiz.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sellingPrice: Double,   // Price shown to customer
    val costPrice: Double,      // Cost of purchase (for profit calculation)
    val imagePath: String?,     // Local file path or null for default
    val category: String = "General",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val stockQuantity: Int = 0  // Total units in stock (set by admin)
)
