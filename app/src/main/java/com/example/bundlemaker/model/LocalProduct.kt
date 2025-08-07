package com.example.bundlemaker.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_products")
data class LocalProduct(
    @PrimaryKey val product_serial: String,
    var robot_serial: String? = null,
    var control_serial: String? = null,
    val sales_id: String? = null,
    val created_at: Long,
    var updated_at: Long? = null,
    var sync_status: Int = 0  // 0:未送信, 1:送信済み, 2:エラー
)

fun LocalProduct.toProduct(): Product {
    return Product(
        product_serial = this.product_serial,
        robot_serial = this.robot_serial,
        control_serial = this.control_serial,
        sales_id = this.sales_id,
        created_at = this.created_at,
        updated_at = this.updated_at,
        isLocalOnly = this.sync_status == 0
    )
}