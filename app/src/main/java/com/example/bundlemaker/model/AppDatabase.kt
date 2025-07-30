package com.example.bundlemaker.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocalProduct::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localProductDao(): LocalProductDao
}
