package com.example.bundlemaker.model

import java.io.Serializable

data class Product(
    val product_serial: String,
    var robot_serial: String? = null,
    var control_serial: String? = null,
    val sales_id: String? = null,
    val created_at: Long? = null,
    var updated_at: Long? = null,
    var isLocalOnly: Boolean = true
) : Serializable

fun Product.toLocalProduct(): LocalProduct {
    return LocalProduct(
        product_serial = this.product_serial,
        robot_serial = this.robot_serial,
        control_serial = this.control_serial,
        sales_id = this.sales_id,
        created_at = this.created_at!!,
        updated_at = this.updated_at
        // LocalProductに必要なフィールドをすべて記述
    )
}