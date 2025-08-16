package com.example.bundlemaker.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LocalProduct::class, Employee::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localProductDao(): LocalProductDao

    abstract fun employeeDao(): EmployeeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            insertInitialData(context)
                        }
                    }
                ).build().also { INSTANCE = it }
            }
        }
        private fun insertInitialData(context: Context) {
            val db = getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                db.employeeDao().insert(Employee(employeeId = "d0000", name = "Dummy"))
                db.employeeDao().insert(Employee(employeeId = "admin", name = "管理者"))
                db.employeeDao().insert(Employee(employeeId = "j0419", name = "渡邊 竜矢"))
            }
        }
    }
}
