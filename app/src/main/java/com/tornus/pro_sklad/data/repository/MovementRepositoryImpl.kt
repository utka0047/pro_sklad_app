package com.tornus.pro_sklad.data.repository

import com.tornus.pro_sklad.data.local.dao.MovementDao
import com.tornus.pro_sklad.data.remote.ApiService
import com.tornus.pro_sklad.data.remote.dto.MovementCreateDto
import com.tornus.pro_sklad.domain.model.Movement
import com.tornus.pro_sklad.domain.model.MovementType
import com.tornus.pro_sklad.domain.model.WarehouseSummary
import com.tornus.pro_sklad.domain.repository.MovementRepository
import com.tornus.pro_sklad.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MovementRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val dao: MovementDao
) : MovementRepository {

    override fun getMovements(): Flow<List<Movement>> =
        dao.getAllMovements().map { entities -> entities.map { it.toDomain() } }

    override fun getMovementsForProduct(productId: Int): Flow<List<Movement>> =
        dao.getMovementsForProduct(productId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createMovement(
        productId: Int,
        type: MovementType,
        quantity: Double,
        comment: String?
    ): Result<Movement> = safeApiCall {
        val dto = MovementCreateDto(
            productId = productId,
            movementType = type.name,
            quantity = quantity,
            comment = comment
        )
        val response = api.createMovement(dto)
        if (response.isSuccessful) {
            val result = response.body()!!
            dao.insert(result.toEntity())
            result.toDomain()
        } else {
            throw Exception("Ошибка ${response.code()}: ${response.message()}")
        }
    }

    override suspend fun getSummary(): Result<WarehouseSummary> = safeApiCall {
        val response = api.getSummary()
        if (response.isSuccessful) {
            val dto = response.body()!!
            WarehouseSummary(
                totalProducts = dto.totalProducts,
                totalSkuCount = dto.totalSkuCount,
                lowStockCount = dto.lowStockCount,
                totalStockValue = dto.totalStockValue,
                movementsToday = dto.movementsToday,
                movementsThisMonth = dto.movementsThisMonth
            )
        } else {
            throw Exception("Ошибка ${response.code()}: ${response.message()}")
        }
    }

    override suspend fun syncMovements(): Result<Unit> = safeApiCall {
        val response = api.getMovements(limit = 500)
        if (response.isSuccessful) {
            val entities = response.body()!!.map { it.toEntity() }
            dao.deleteAll()
            dao.insertAll(entities)
        } else {
            throw Exception("Ошибка синхронизации: ${response.code()}")
        }
    }
}
