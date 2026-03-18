package com.tornus.pro_sklad.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WarehouseSummaryDto(
    @SerializedName("total_products") val totalProducts: Int,
    @SerializedName("total_sku_count") val totalSkuCount: Int,
    @SerializedName("low_stock_count") val lowStockCount: Int,
    @SerializedName("total_stock_value") val totalStockValue: String,
    @SerializedName("movements_today") val movementsToday: Int,
    @SerializedName("movements_this_month") val movementsThisMonth: Int
)

data class LowStockItemDto(
    val id: Int,
    val name: String,
    val sku: String,
    val category: String?,
    val unit: String,
    @SerializedName("current_stock") val currentStock: String,
    @SerializedName("min_stock") val minStock: String,
    val deficit: String
)

data class MovementChartPointDto(
    val date: String,
    @SerializedName("in_qty") val inQty: Double,
    @SerializedName("out_qty") val outQty: Double
)

data class TopProductDto(
    val id: Int,
    val name: String,
    val sku: String,
    val category: String?,
    val unit: String,
    @SerializedName("total_out") val totalOut: Double,
    @SerializedName("total_in") val totalIn: Double
)
