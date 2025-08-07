package com.example.bundlemaker

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bundlemaker.adapter.ProductAdapter
import com.example.bundlemaker.model.AppDatabase
import com.example.bundlemaker.model.Product
import com.example.bundlemaker.model.toLocalProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfirmActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var registerBtn: Button
    private lateinit var modifyBtn: Button
    private lateinit var cancelBtn: Button
    private lateinit var adapter: ProductAdapter
    private lateinit var products: List<Product>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)

        // インテントからデータを取得
        // products = intent.getSerializableExtra("products") as? List<Product> ?: emptyList()

        recyclerView = findViewById(R.id.confirm_product_list)
        registerBtn = findViewById(R.id.confirm_register_button)
        modifyBtn = findViewById(R.id.confirm_modify_button)
        cancelBtn = findViewById(R.id.confirm_cancel_button)

        // IntentからProductリストを受け取る
        products = (intent.getSerializableExtra("products") as? ArrayList<Product>) ?: arrayListOf()

        // データが正しく取得できているかログ出力（デバッグ用）
        Log.d("ConfirmActivity", "Received products count: ${products.size}")

        // RecyclerViewの設定
        recyclerView.layoutManager = LinearLayoutManager(this)  // レイアウトマネージャーの設定を追加
        adapter = ProductAdapter(products.toMutableList())
        recyclerView.adapter = adapter

        registerBtn.setOnClickListener {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
        }

        modifyBtn.setOnClickListener {
            val selectedProduct = adapter.getSelectedProduct()
            if (selectedProduct != null) {
                // 選択されたProductの情報を未完成状態に戻す
                selectedProduct.robot_serial = null
                selectedProduct.control_serial = null
                selectedProduct.updated_at = null

                AlertDialog.Builder(this)
                    .setTitle("確認")
                    .setMessage("未完成に戻したレコードをサーバーに送信しますか？")
                    .setPositiveButton("Yes") { dialog, which ->
                        sendToServer(selectedProduct)
                        updateLocalDB(this, selectedProduct)
                        Toast.makeText(this, "サーバーに送信しました", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No") { dialog, which ->
                        updateLocalDB(this, selectedProduct)
                        Toast.makeText(this, "ローカルのみ修正しました", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            } else {
                Toast.makeText(this, "レコードを選択してください", Toast.LENGTH_SHORT).show()
            }
        }


        cancelBtn.setOnClickListener {
            finish() // 元の画面に戻る
        }
    }
}

// RoomDB更新処理
private fun updateLocalDB(context: Context, product: Product) {
    CoroutineScope(Dispatchers.IO).launch {
        val db = AppDatabase.getInstance(context)
        db.localProductDao().update(product.toLocalProduct())
    }
}

// サーバー送信処理（仮実装）
private fun sendToServer(product: Product) {
    // Retrofit等でAPI送信処理を記述
}