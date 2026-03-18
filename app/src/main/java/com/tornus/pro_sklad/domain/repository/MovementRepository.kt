package com.tornus.pro_sklad.domain.repository

import com.tornus.pro_sklad.domain.model.Movement
import com.tornus.pro_sklad.domain.model.MovementType
import com.tornus.pro_sklad.domain.model.WarehouseSummary
import com.tornus.pro_sklad.util.Result
import kotlinx.coroutines.flow.Flow

interface MovementRepository {
    fun getMovements(): Flow<List<Movement>>
    fun getMovementsForProduct(productId: Int): Flow<List<Movement>>
    suspend fun createMovement(
        productId: Int,
        type: MovementType,
        quantity: Double,
        comment: String?
    ): Result<Movement>
    suspend fun getSummary(): Result<WarehouseSummary>
    suspend fun syncMovements(): Result<Unit>
}
