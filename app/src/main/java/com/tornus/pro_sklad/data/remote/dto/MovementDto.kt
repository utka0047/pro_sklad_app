package com.tornus.pro_sklad.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MovementOutDto(
    val id: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String?,
    @SerializedName("product_sku") val productSku: String?,
    @SerializedName("movement_type") val movementType: String,
    val quantity: String,
    @SerializedName("quantity_before") val quantityBefore: String,
    @SerializedName("quantity_after") val quantityAfter: String,
    val comment: String?,
    @SerializedName("created_at") val createdAt: String
)

data class MovementCreateDto(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("movement_type") val movementType: String,
    val quantity: Any,
    val comment: String? = null
)
