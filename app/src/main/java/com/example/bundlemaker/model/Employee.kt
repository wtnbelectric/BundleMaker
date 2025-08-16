package com.example.bundlemaker.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Employee(
    @PrimaryKey val employeeId: String,
    val name: String
)