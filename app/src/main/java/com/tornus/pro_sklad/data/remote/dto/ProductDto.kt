package com.tornus.pro_sklad.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductOutDto(
    val id: Int,
    val name: String,
    val sku: String,
    val category: String?,
    val unit: String,
    val price: String,
    val description: String?,
    @SerializedName("min_stock") val minStock: String,
    @SerializedName("current_stock") val currentStock: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class ProductCreateDto(
    val name: String,
    val sku: String,
    val category: String? = null,
    val unit: String = "шт",
    val price: Any = "0",
    val description: String? = null,
    @SerializedName("min_stock") val minStock: Any = "0"
)

data class ProductUpdateDto(
    val name: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val price: Any? = null,
    val description: String? = null,
    @SerializedName("min_stock") val minStock: Any? = null
)
