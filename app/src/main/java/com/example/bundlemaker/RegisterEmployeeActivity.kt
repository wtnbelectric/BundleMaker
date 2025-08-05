package com.example.bundlemaker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.bundlemaker.model.AppDatabase
import com.example.bundlemaker.model.Employee
import kotlinx.coroutines.launch

class RegisterEmployeeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_employee)

        val idInput = findViewById<EditText>(R.id.employee_id_input)
        val nameInput = findViewById<EditText>(R.id.employee_name_input)
        val registerBtn = findViewById<Button>(R.id.register_button)

        registerBtn.setOnClickListener {
            val id = idInput.text.toString()
            val name = nameInput.text.toString()
            if (id.isNotBlank() && name.isNotBlank()) {
                lifecycleScope.launch {
                    val db = Room.databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java,
                        "local_products"
                    ).build()
                    db.employeeDao().insert(Employee(id, name))
                    Toast.makeText(
                        this@RegisterEmployeeActivity,
                        "登録しました",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }
}