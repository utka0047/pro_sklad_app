package com.tornus.pro_sklad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey val id: Int,
    val productId: Int,
    val productName: String?,
    val productSku: String?,
    val movementType: String,
    val quantity: String,
    val quantityBefore: String,
    val quantityAfter: String,
    val comment: String?,
    val createdAt: String
)
