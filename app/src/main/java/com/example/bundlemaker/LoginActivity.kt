package com.example.bundlemaker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.bundlemaker.model.AppDatabase
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val idInput = findViewById<EditText>(R.id.employee_id_input)
        val loginBtn = findViewById<Button>(R.id.login_button)

        loginBtn.setOnClickListener {
            val id = idInput.text.toString()
            if (id.isNotBlank()) {
                lifecycleScope.launch {
                    val db = Room.databaseBuilder(
                        applicationContext,
                        AppDatabase::class.java,
                        "local_products"
                    ).build()
                    val employee = db.employeeDao().getEmployeeById(id)
                    if (employee != null) {
                        Toast.makeText(
                            this@LoginActivity,
                            "ようこそ、${employee.name}さん",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("employee_id", id)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "従業員IDが見つかりません",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}