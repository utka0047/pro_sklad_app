package com.tornus.pro_sklad.data.repository

import com.tornus.pro_sklad.data.local.dao.ProductDao
import com.tornus.pro_sklad.data.remote.ApiService
import com.tornus.pro_sklad.domain.model.Product
import com.tornus.pro_sklad.domain.repository.ProductRepository
import com.tornus.pro_sklad.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val dao: ProductDao
) : ProductRepository {

    override fun getProducts(): Flow<List<Product>> =
        dao.getAllProducts().map { entities -> entities.map { it.toDomain() } }

    override fun searchProducts(query: String): Flow<List<Product>> =
        dao.searchProducts(query).map { entities -> entities.map { it.toDomain() } }

    override fun getLowStockProducts(): Flow<List<Product>> =
        dao.getLowStockProducts().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getProductById(id: Int): Result<Product> = safeApiCall {
        val local = dao.getProductById(id)
        if (local != null) return@safeApiCall local.toDomain()
        val response = api.getProduct(id)
        if (response.isSuccessful) {
            val dto = response.body()!!
            dao.insert(dto.toEntity())
            dto.toDomain()
        } else {
            throw Exception("Ошибка ${response.code()}: ${response.message()}")
        }
    }

    override suspend fun getProductBySku(sku: String): Result<Product> = safeApiCall {
        val local = dao.getProductBySku(sku)
        if (local != null) return@safeApiCall local.toDomain()
        val response = api.getProducts()
        if (response.isSuccessful) {
            val products = response.body()!!
            dao.insertAll(products.map { it.toEntity() })
            products.find { it.sku == sku }?.toDomain()
                ?: throw Exception("Товар с SKU $sku не найден")
        } else {
            throw Exception("Ошибка ${response.code()}: ${response.message()}")
        }
    }

    override suspend fun syncProducts(): Result<Unit> = safeApiCall {
        val response = api.getProducts()
        if (response.isSuccessful) {
            val entities = response.body()!!.map { it.toEntity() }
            dao.deleteAll()
            dao.insertAll(entities)
        } else {
            throw Exception("Ошибка синхронизации: ${response.code()}")
        }
    }
}
