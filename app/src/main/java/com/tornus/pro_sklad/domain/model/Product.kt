package com.tornus.pro_sklad.domain.model

data class Product(
    val id: Int,
    val name: String,
    val sku: String,
    val category: String?,
    val unit: String,
    val price: String,
    val description: String?,
    val minStock: String,
    val currentStock: String,
    val createdAt: String,
    val updatedAt: String
) {
    val isLowStock: Boolean
        get() = minStock.toDoubleOrNull()?.let { min ->
            min > 0 && (currentStock.toDoubleOrNull() ?: 0.0) <= min
        } ?: false
}
