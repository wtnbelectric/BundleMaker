package com.example.bundlemaker.model

import androidx.room.*

@Dao
interface LocalProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: LocalProduct)

    @Query("SELECT * FROM local_products ORDER BY product_serial ASC")
    suspend fun getAll(): List<LocalProduct>

    @Query("SELECT * FROM local_products WHERE sync_status = 0 ORDER BY created_at DESC")
    suspend fun getPending(): List<LocalProduct>

    @Update
    suspend fun update(product: LocalProduct)

    @Delete
    suspend fun delete(product: LocalProduct)

    @Query("DELETE FROM local_products")
    suspend fun deleteAll()

    @Query("UPDATE local_products SET sync_status = :newStatus WHERE product_serial = :serial")
    suspend fun updateSyncStatus(serial: String, newStatus: Int)

    @Query("SELECT * FROM local_products WHERE product_serial = :serial LIMIT 1")
    suspend fun getProductBySerial(serial: String): LocalProduct?

    @Query("SELECT * FROM local_products WHERE sync_status = 1")
    suspend fun getCompleted(): List<LocalProduct>
}

