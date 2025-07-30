package com.example.bundlemaker.model

data class Product(
    val product_serial: String,
    var robot_serial: String? = null,
    var control_serial: String? = null,
    val sales_id: String? = null,
    val created_at: Long? = null,
    val updated_at: Long? = null,
    var isLocalOnly: Boolean = true
) {
    fun isComplete(): Boolean =
        !product_serial.isNullOrBlank() &&
        !robot_serial.isNullOrBlank() &&
        !control_serial.isNullOrBlank()

    fun isValidProductSerial(): Boolean =
        product_serial.length in 10..20 && product_serial.matches(Regex("^[A-Za-z0-9]+$"))

    fun isValidSerial(serial: String?): Boolean =
        !serial.isNullOrBlank() && serial.length in 8..15 && serial.matches(Regex("^[A-Za-z0-9]+$"))
}
