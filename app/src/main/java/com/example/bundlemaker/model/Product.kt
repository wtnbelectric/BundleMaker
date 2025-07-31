package com.example.bundlemaker.model

data class Product(
    val product_serial: String,
    var robot_serial: String? = null,
    var control_serial: String? = null,
    val sales_id: String? = null,
    val created_at: Long? = null,
    val updated_at: Long? = null,
    var isLocalOnly: Boolean = true
) : java.io.Serializable
