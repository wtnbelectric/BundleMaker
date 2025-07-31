package com.example.bundlemaker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bundlemaker.model.Product
import java.io.Serializable

class ConfirmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)

        // すべてのレコードを受け取る
        val allProducts = intent.getSerializableExtra("all_products") as? ArrayList<Product>

        val textView = findViewById<TextView>(R.id.confirm_title)
        if (allProducts != null && allProducts.isNotEmpty()) {
            val sb = StringBuilder()
            allProducts.forEachIndexed { idx, p ->
                sb.append("${idx + 1}: 製造No=${p.product_serial}, ロボNo=${p.robot_serial}, コンNo=${p.control_serial}\n")
            }
            textView.text = sb.toString()
        } else {
            textView.text = "データがありません"
        }

        val cancelBtn = findViewById<Button>(R.id.confirm_cancel_button)
        cancelBtn.setOnClickListener {
            finish()
        }
    }
}
