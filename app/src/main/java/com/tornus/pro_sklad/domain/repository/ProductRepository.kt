package com.tornus.pro_sklad.domain.repository

import com.tornus.pro_sklad.domain.model.Product
import com.tornus.pro_sklad.util.Result
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
    fun searchProducts(query: String): Flow<List<Product>>
    fun getLowStockProducts(): Flow<List<Product>>
    suspend fun getProductById(id: Int): Result<Product>
    suspend fun getProductBySku(sku: String): Result<Product>
    suspend fun syncProducts(): Result<Unit>
}
