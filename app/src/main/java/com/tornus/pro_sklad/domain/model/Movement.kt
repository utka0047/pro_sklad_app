package com.tornus.pro_sklad.domain.model

enum class MovementType(val label: String) {
    IN("Приход"),
    OUT("Расход"),
    TRANSFER("Перемещение"),
    INVENTORY("Инвентаризация")
}

data class Movement(
    val id: Int,
    val productId: Int,
    val productName: String?,
    val productSku: String?,
    val movementType: MovementType,
    val quantity: String,
    val quantityBefore: String,
    val quantityAfter: String,
    val comment: String?,
    val createdAt: String
)

data class WarehouseSummary(
    val totalProducts: Int,
    val totalSkuCount: Int,
    val lowStockCount: Int,
    val totalStockValue: String,
    val movementsToday: Int,
    val movementsThisMonth: Int
)
