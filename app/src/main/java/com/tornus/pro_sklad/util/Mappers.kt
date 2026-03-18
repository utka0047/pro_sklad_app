package com.tornus.pro_sklad.util

import com.tornus.pro_sklad.data.local.entity.MovementEntity
import com.tornus.pro_sklad.data.local.entity.ProductEntity
import com.tornus.pro_sklad.data.remote.dto.MovementOutDto
import com.tornus.pro_sklad.data.remote.dto.ProductOutDto
import com.tornus.pro_sklad.domain.model.Movement
import com.tornus.pro_sklad.domain.model.MovementType
import com.tornus.pro_sklad.domain.model.Product

// DTO → Entity
fun ProductOutDto.toEntity() = ProductEntity(
    id = id, name = name, sku = sku, category = category,
    unit = unit, price = price, description = description,
    minStock = minStock, currentStock = currentStock,
    createdAt = createdAt, updatedAt = updatedAt
)

fun MovementOutDto.toEntity() = MovementEntity(
    id = id, productId = productId, productName = productName,
    productSku = productSku, movementType = movementType,
    quantity = quantity, quantityBefore = quantityBefore,
    quantityAfter = quantityAfter, comment = comment, createdAt = createdAt
)

// Entity → Domain
fun ProductEntity.toDomain() = Product(
    id = id, name = name, sku = sku, category = category,
    unit = unit, price = price, description = description,
    minStock = minStock, currentStock = currentStock,
    createdAt = createdAt, updatedAt = updatedAt
)

fun MovementEntity.toDomain() = Movement(
    id = id, productId = productId, productName = productName,
    productSku = productSku,
    movementType = MovementType.valueOf(movementType),
    quantity = quantity, quantityBefore = quantityBefore,
    quantityAfter = quantityAfter, comment = comment, createdAt = createdAt
)

// DTO → Domain
fun ProductOutDto.toDomain() = Product(
    id = id, name = name, sku = sku, category = category,
    unit = unit, price = price, description = description,
    minStock = minStock, currentStock = currentStock,
    createdAt = createdAt, updatedAt = updatedAt
)

fun MovementOutDto.toDomain() = Movement(
    id = id, productId = productId, productName = productName,
    productSku = productSku,
    movementType = MovementType.valueOf(movementType),
    quantity = quantity, quantityBefore = quantityBefore,
    quantityAfter = quantityAfter, comment = comment, createdAt = createdAt
)
