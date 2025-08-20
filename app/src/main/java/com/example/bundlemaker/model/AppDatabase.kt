package com.example.bundlemaker.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.forEach

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
        fun insertInitialData(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = getInstance(context)
                val assetManager = context.assets
                assetManager.open("employees.csv").bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",")
                        if (parts.size >= 2) {
                            val employeeId = parts[0]
                            val name = parts[1]
                            // 既存チェック
                            val exists = db.employeeDao().getEmployeeById(employeeId) != null
                            if (!exists) {
                                db.employeeDao().insert(Employee(employeeId = employeeId, name = name))
                            }
                        }
                    }
                }
                // db.employeeDao().insert(Employee(employeeId = "d0000", name = "Dummy"))
                // db.employeeDao().insert(Employee(employeeId = "admin", name = "管理者"))
                // db.employeeDao().insert(Employee(employeeId = "j0419", name = "渡邊 竜矢"))
            }
        }
        fun deleteEmployeesNotInCsv(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = getInstance(context)
                val assetManager = context.assets
                val csvIds = assetManager.open("employees.csv").bufferedReader().useLines { lines ->
                    lines.drop(1).map { it.split(",")[0] }.toSet()
                }
                val allEmployees = db.employeeDao().getAllEmployees()
                val toDelete = allEmployees.filter { it.employeeId !in csvIds }
                toDelete.forEach { db.employeeDao().delete(it) }
            }
        }
    }
}
