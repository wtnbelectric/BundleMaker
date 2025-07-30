package com.example.bundlemaker.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// データ取得用
interface ProductApiService {
    @GET("/data")
    suspend fun getAllProducts(): Response<List<ProductResponse>>

    @GET("/data/incomplete")
    suspend fun getIncompleteProducts(): Response<List<ProductResponse>>

    @POST("/products")
    suspend fun postProduct(@Body body: ProductRequest): Response<ApiResult>
}

// レスポンス用データクラス
// 必要に応じてフィールド追加

data class ProductResponse(
    val product_serial: String,
    val robot_serial: String?,
    val control_serial: String?,
    val sales_id: String?,
    val created_at: Long?,
    val updated_at: Long?
)

data class ProductRequest(
    val product_serial: String,
    val robot_serial: String?,
    val control_serial: String?,
    val sales_id: String? = null
)

data class ApiResult(
    val message: String?,
    val error: String? = null
)

