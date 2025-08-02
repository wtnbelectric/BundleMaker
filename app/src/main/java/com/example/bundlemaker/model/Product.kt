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
