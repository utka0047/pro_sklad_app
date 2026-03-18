package com.tornus.pro_sklad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
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
)
