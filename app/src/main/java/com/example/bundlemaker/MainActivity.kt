package com.example.bundlemaker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.bundlemaker.adapter.ProductAdapter
import com.example.bundlemaker.model.AppDatabase
import com.example.bundlemaker.model.LocalProduct
import com.example.bundlemaker.model.Product
import com.example.bundlemaker.network.ProductApiService
import com.example.bundlemaker.network.ProductRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var productAdapter: ProductAdapter
    private var currentProducts: MutableList<Product> = mutableListOf()
    private var selectedRowIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ボタン取得
        val productSerialBtn = findViewById<Button>(R.id.product_serial_search_button)
        val robotSerialBtn = findViewById<Button>(R.id.robot_serial_enter_button)
        val controllerSerialBtn = findViewById<Button>(R.id.controller_serial_enter_button)
        val commitBtn = findViewById<Button>(R.id.commit_button)
        val syncBtn = findViewById<ImageButton>(R.id.sync_button)
        val confirmBtn = findViewById<Button>(R.id.confirm_button)

        // RecyclerViewセットアップ
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.product_table)
        productAdapter = ProductAdapter { position ->
            selectedRowIndex = position
            updateButtonStates()
        }
        recyclerView.adapter = productAdapter
        productAdapter.setProducts(currentProducts)

        // ボタンリスナー
        productSerialBtn.setOnClickListener {
            // 製造番号入力ダイアログ表示
            val editText = EditText(this)
            editText.hint = "製造番号を入力"
            AlertDialog.Builder(this)
                .setTitle("製造番号入力")
                .setView(editText)
                .setPositiveButton("追加") { _, _ ->
                    val serial = editText.text.toString().trim()
                    if (serial.isNotEmpty()) {
                        // 重複チェック
                        if (currentProducts.any { it.product_serial == serial }) {
                            Toast.makeText(this, "同じ製造番号が既に存在します", Toast.LENGTH_SHORT).show()
                        } else {
                            currentProducts.add(Product(product_serial = serial))
                            productAdapter.setProducts(currentProducts)
                            selectedRowIndex = currentProducts.lastIndex
                            updateButtonStates()
                        }
                    } else {
                        Toast.makeText(this, "製造番号を入力してください", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        robotSerialBtn.setOnClickListener {
            // 選択行のrobot_serialを入力・更新
            if (selectedRowIndex in currentProducts.indices) {
                val editText = EditText(this)
                editText.hint = "ロボットシリアル番号を入力"
                AlertDialog.Builder(this)
                    .setTitle("ロボットシリアル番号入力")
                    .setView(editText)
                    .setPositiveButton("入力") { _, _ ->
                        val serial = editText.text.toString().trim()
                        if (serial.isNotEmpty()) {
                            currentProducts[selectedRowIndex].robot_serial = serial
                            productAdapter.notifyItemChanged(selectedRowIndex)
                            updateButtonStates()
                        } else {
                            Toast.makeText(this, "ロボットシリアル番号を入力してください", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
            }
        }

        controllerSerialBtn.setOnClickListener {
            // 選択行のcontrol_serialを入力・更新
            if (selectedRowIndex in currentProducts.indices) {
                val editText = EditText(this)
                editText.hint = "コントローラシリアル番号を入力"
                AlertDialog.Builder(this)
                    .setTitle("コントローラシリアル番号入力")
                    .setView(editText)
                    .setPositiveButton("入力") { _, _ ->
                        val serial = editText.text.toString().trim()
                        if (serial.isNotEmpty()) {
                            currentProducts[selectedRowIndex].control_serial = serial
                            productAdapter.notifyItemChanged(selectedRowIndex)
                            updateButtonStates()
                        } else {
                            Toast.makeText(this, "コントローラシリアル番号を入力してください", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
            }
        }

        commitBtn.setOnClickListener {
            // 選択行の全項目が入力済みならupdated_atを更新し確定
            if (selectedRowIndex in currentProducts.indices) {
                val p = currentProducts[selectedRowIndex]
                if (!p.product_serial.isNullOrBlank() && !p.robot_serial.isNullOrBlank() && !p.control_serial.isNullOrBlank()) {
                    currentProducts[selectedRowIndex] = p.copy(updated_at = System.currentTimeMillis())
                    productAdapter.setProducts(currentProducts)
                    selectedRowIndex = -1
                    updateButtonStates()
                }
            }
        }

        syncBtn.setOnClickListener {
            // サーバーへ完成レコード送信・未完成レコード取得
            lifecycleScope.launch {
                // 完成レコードのみ抽出
                val completed = currentProducts.filter {
                    !it.product_serial.isNullOrBlank() &&
                    !it.robot_serial.isNullOrBlank() &&
                    !it.control_serial.isNullOrBlank() &&
                    it.updated_at != null
                }
                val retrofit = Retrofit.Builder()
                    .baseUrl("http://your-server-url/") // サーバーURLを適宜修正
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(ProductApiService::class.java)
                var successCount = 0
                var failCount = 0

                // 完成レコード送信
                for (product in completed) {
                    val req = ProductRequest(
                        product_serial = product.product_serial,
                        robot_serial = product.robot_serial,
                        control_serial = product.control_serial,
                        sales_id = product.sales_id
                    )
                    val resp = withContext(Dispatchers.IO) {
                        try {
                            // ↓ここを正しいメソッド名に修正
                            val response = api.postProduct(req)
                            response.isSuccessful
                        } catch (e: Exception) {
                            false
                        }
                    }
                    if (resp) successCount++ else failCount++
                }

                // 未完成レコード取得
                val incomplete = withContext(Dispatchers.IO) {
                    try {
                        val response = api.getIncompleteProducts()
                        if (response.isSuccessful) {
                            // ↓ここでList<Product>にキャスト
                            response.body() as? List<Product> ?: emptyList()
                        } else emptyList()
                    } catch (e: Exception) {
                        emptyList<Product>()
                    }
                }
                // 画面に反映
                currentProducts.clear()
                currentProducts.addAll(incomplete)
                productAdapter.setProducts(currentProducts)
                selectedRowIndex = -1
                updateButtonStates()

                Toast.makeText(
                    this@MainActivity,
                    "送信: $successCount 件, 失敗: $failCount 件\n未完成データを再取得しました",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        confirmBtn.setOnClickListener {
            // APIで全てのレコードを取得し、確認画面で表示
            lifecycleScope.launch {
                val retrofit = Retrofit.Builder()
                    .baseUrl("http://your-server-url/") // サーバーURLを適宜修正
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(ProductApiService::class.java)
                val allProducts = withContext(Dispatchers.IO) {
                    try {
                        val response = api.getAllProducts()
                        if (response.isSuccessful) {
                            response.body() as? List<Product> ?: emptyList()
                        } else emptyList()
                    } catch (e: Exception) {
                        emptyList<Product>()
                    }
                }
                val intent = Intent(this@MainActivity, ConfirmActivity::class.java)
                intent.putExtra("all_products", ArrayList(allProducts))
                startActivity(intent)
            }
        }

        updateButtonStates()
    }

    private fun updateButtonStates() {
        val productSerialBtn = findViewById<Button>(R.id.product_serial_search_button)
        val robotSerialBtn = findViewById<Button>(R.id.robot_serial_enter_button)
        val controllerSerialBtn = findViewById<Button>(R.id.controller_serial_enter_button)
        val commitBtn = findViewById<Button>(R.id.commit_button)

        productSerialBtn.isEnabled = true

        val selected = selectedRowIndex in currentProducts.indices
        val selectedProduct = if (selected) currentProducts[selectedRowIndex] else null

        robotSerialBtn.isEnabled = selected && !selectedProduct?.product_serial.isNullOrBlank()
        controllerSerialBtn.isEnabled = selected &&
            !selectedProduct?.product_serial.isNullOrBlank() &&
            !selectedProduct?.robot_serial.isNullOrBlank()
        commitBtn.isEnabled = selected &&
            !selectedProduct?.product_serial.isNullOrBlank() &&
            !selectedProduct?.robot_serial.isNullOrBlank() &&
            !selectedProduct?.control_serial.isNullOrBlank()
    }
}