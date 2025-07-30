package com.example.bundlemaker.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "local_products",
    indices = [Index(value = ["product_serial"], unique = true)]
)
data class LocalProduct(
    @PrimaryKey val product_serial: String,
    val robot_serial: String? = null,
    val control_serial: String? = null,
    val sales_id: String? = null,
    val created_at: Long,
    val updated_at: Long? = null,
    val sync_status: Int = 0  // 0:未送信, 1:送信済み, 2:エラー
)
