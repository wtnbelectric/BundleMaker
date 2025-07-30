package com.example.bundlemaker.model

import androidx.room.*

@Dao
interface LocalProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: LocalProduct)

    @Query("SELECT * FROM local_products ORDER BY created_at DESC")
    suspend fun getAll(): List<LocalProduct>

    @Query("SELECT * FROM local_products WHERE sync_status = 0 ORDER BY created_at DESC")
    suspend fun getPending(): List<LocalProduct>

    @Update
    suspend fun update(product: LocalProduct)

    @Delete
    suspend fun delete(product: LocalProduct)

    @Query("DELETE FROM local_products")
    suspend fun deleteAll()
}

