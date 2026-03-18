package com.tornus.pro_sklad.data.remote

import com.tornus.pro_sklad.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Products
    @GET("products/")
    suspend fun getProducts(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 200,
        @Query("category") category: String? = null,
        @Query("low_stock_only") lowStockOnly: Boolean = false
    ): Response<List<ProductOutDto>>

    @GET("products/categories")
    suspend fun getCategories(): Response<List<String>>

    @GET("products/{product_id}")
    suspend fun getProduct(@Path("product_id") productId: Int): Response<ProductOutDto>

    @POST("products/")
    suspend fun createProduct(@Body product: ProductCreateDto): Response<ProductOutDto>

    @PUT("products/{product_id}")
    suspend fun updateProduct(
        @Path("product_id") productId: Int,
        @Body product: ProductUpdateDto
    ): Response<ProductOutDto>

    @DELETE("products/{product_id}")
    suspend fun deleteProduct(@Path("product_id") productId: Int): Response<Unit>

    // Movements
    @GET("movements/")
    suspend fun getMovements(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 200,
        @Query("product_id") productId: Int? = null,
        @Query("movement_type") movementType: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<List<MovementOutDto>>

    @GET("movements/{movement_id}")
    suspend fun getMovement(@Path("movement_id") movementId: Int): Response<MovementOutDto>

    @POST("movements/")
    suspend fun createMovement(@Body movement: MovementCreateDto): Response<MovementOutDto>

    // Analytics
    @GET("analytics/summary")
    suspend fun getSummary(): Response<WarehouseSummaryDto>

    @GET("analytics/low-stock")
    suspend fun getLowStock(): Response<List<LowStockItemDto>>

    @GET("analytics/movements-chart")
    suspend fun getMovementsChart(@Query("days") days: Int = 30): Response<List<MovementChartPointDto>>

    @GET("analytics/top-products")
    suspend fun getTopProducts(@Query("limit") limit: Int = 10): Response<List<TopProductDto>>
}
