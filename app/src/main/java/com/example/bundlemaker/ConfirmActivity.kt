package com.example.bundlemaker

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.bundlemaker.adapter.ProductAdapter
import com.example.bundlemaker.model.Product

class ConfirmActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var registerBtn: Button
    private lateinit var modifyBtn: Button
    private lateinit var cancelBtn: Button
    private lateinit var adapter: ProductAdapter
    private var products: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)

        recyclerView = findViewById(R.id.confirm_product_list)
        registerBtn = findViewById(R.id.confirm_register_button)
        modifyBtn = findViewById(R.id.confirm_modify_button)
        cancelBtn = findViewById(R.id.confirm_cancel_button)

        // 非推奨APIを使わずに取得
        products = intent.getSerializableExtra("products") as? ArrayList<Product> ?: arrayListOf()

        adapter = ProductAdapter(products.toMutableList())
        recyclerView.adapter = adapter

        registerBtn.setOnClickListener {
            // 登録処理（必要に応じて実装）
        }

        modifyBtn.setOnClickListener {
            // 修正処理（必要に応じて実装）
        }

        cancelBtn.setOnClickListener {
            finish() // 元の画面に戻る
        }
    }
}