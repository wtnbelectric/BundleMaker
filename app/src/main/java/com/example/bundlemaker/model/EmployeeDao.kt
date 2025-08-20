package com.example.bundlemaker.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM Employee WHERE employeeId = :id LIMIT 1")
    suspend fun getEmployeeById(id: String): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: Employee)

    @Query("SELECT * FROM employee")
    suspend fun getAllEmployees(): List<Employee>

    @Delete
    suspend fun delete(employee: Employee)
}