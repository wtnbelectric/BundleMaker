package com.example.bundlemaker.model

import android.os.Parcel
import android.os.Parcelable

data class Product(
    val product_serial: String,
    var robot_serial: String? = null,
    var control_serial: String? = null,
    val sales_id: String? = null,
    val created_at: Long? = null,
    val updated_at: Long? = null,
    var isLocalOnly: Boolean = true
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readValue(Long::class.java.classLoader) as? Long,
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(product_serial)
        parcel.writeString(robot_serial)
        parcel.writeString(control_serial)
        parcel.writeString(sales_id)
        parcel.writeValue(created_at)
        parcel.writeValue(updated_at)
        parcel.writeByte(if (isLocalOnly) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(parcel: Parcel): Product {
            return Product(parcel)
        }

        override fun newArray(size: Int): Array<Product?> {
            return arrayOfNulls(size)
        }
    }
}