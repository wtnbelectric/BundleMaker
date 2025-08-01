package com.example.bundlemaker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
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
import com.example.bundlemaker.network.ProductResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var productAdapter: ProductAdapter
    private lateinit var progressBar: ProgressBar
    private var currentProducts: MutableList<Product> = mutableListOf()
    private var selectedRowIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val products = intent.getParcelableArrayListExtra<Product>("all_products") ?: emptyList()

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

        // ProgressBarの初期化
        progressBar = findViewById(R.id.progressBar)

        // RecyclerViewセットアップ
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.product_table)
        // レイアウトマネージャーを設定（これが抜けていました）
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        productAdapter = ProductAdapter { position ->
            selectedRowIndex = position
            updateButtonStates()
        }
        recyclerView.adapter = productAdapter
        productAdapter.setProducts(currentProducts)
        
        // デバッグ用：RecyclerViewの設定を確認
        Log.d("MainActivity", "RecyclerView setup complete. Has layout manager: ${recyclerView.layoutManager != null}")

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
                syncIncompleteProducts()
        }

        confirmBtn.setOnClickListener {
            lifecycleScope.launch {
                // ローディング表示
                progressBar.visibility = View.VISIBLE

                try {
                    // サーバーから全レコードを取得
                    val allProducts = withContext(Dispatchers.IO) {
                        try {
                            val retrofit = Retrofit.Builder()
                                .baseUrl("http://192.168.5.72:5000/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()

                            val api = retrofit.create(ProductApiService::class.java)
                            val response = api.getAllProducts()

                            if (response.isSuccessful) {
                                response.body()?.map { responseItem ->
                                    Product(
                                        product_serial = responseItem.product_serial,
                                        robot_serial = responseItem.robot_serial,
                                        control_serial = responseItem.control_serial,
                                        sales_id = responseItem.sales_id,
                                        created_at = responseItem.created_at,
                                        updated_at = responseItem.updated_at,
                                        isLocalOnly = false
                                    )
                                } ?: emptyList()
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            emptyList()
                        }
                    }

                    // 確認画面を表示
                    val intent = Intent(this@MainActivity, ConfirmActivity::class.java).apply {
                        putParcelableArrayListExtra("all_products", ArrayList(allProducts))
                    }
                    startActivity(intent)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@MainActivity,
                        "データの取得に失敗しました: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    // ローディングを非表示
                    progressBar.visibility = View.GONE
                }
            }
        }

        /*
        confirmBtn.setOnClickListener {
            try {
                // 現在のリストを新しいArrayListとしてコピー
                val productsToPass = ArrayList<Product>().apply {
                    addAll(currentProducts.map { 
                        Product(
                            product_serial = it.product_serial,
                            robot_serial = it.robot_serial,
                            control_serial = it.control_serial,
                            sales_id = it.sales_id,
                            created_at = it.created_at,
                            updated_at = it.updated_at,
                            isLocalOnly = it.isLocalOnly
                        )
                    })
                }
                
                val intent = Intent(this@MainActivity, ConfirmActivity::class.java).apply {
                    putParcelableArrayListExtra("all_products", productsToPass)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "確認画面を開けませんでした: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }*/

        updateButtonStates()
    }

    private fun syncIncompleteProducts() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            Log.d("MainActivity", "Starting to fetch incomplete products...")

            try {
                val incompleteProducts = withContext(Dispatchers.IO) {
                    try {
                        val retrofit = Retrofit.Builder()
                            .baseUrl("http://192.168.5.72:5000/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

                        val api = retrofit.create(ProductApiService::class.java)
                        Log.d("MainActivity", "Calling API to get incomplete products...")
                        val response = api.getIncompleteProducts()
                        Log.d("MainActivity", "API response code: ${response.code()}")
                        Log.d("MainActivity", "API response body: ${response.body()}")

                        if (response.isSuccessful) {
                            val products = response.body()?.map { productResponse ->
                                Product(
                                    product_serial = productResponse.product_serial,
                                    robot_serial = productResponse.robot_serial,
                                    control_serial = productResponse.control_serial,
                                    sales_id = productResponse.sales_id,
                                    created_at = productResponse.created_at,
                                    updated_at = productResponse.updated_at,
                                    isLocalOnly = false
                                )
                            } ?: emptyList()
                            Log.d("MainActivity", "Successfully mapped ${products.size} products")
                            products
                        } else {
                            Log.e("MainActivity", "API call failed with code: ${response.code()}")
                            emptyList()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }

                // Update the RecyclerView with the new data
                Log.d("MainActivity", "Updating UI with ${incompleteProducts.size} products")
                runOnUiThread {
                    Log.d("MainActivity", "Current products before update: ${currentProducts.size}")
                    currentProducts.clear()
                    currentProducts.addAll(incompleteProducts)
                    Log.d("MainActivity", "Current products after update: ${currentProducts.size}")
                    
                    // アダプタにデータを設定
                    productAdapter.setProducts(currentProducts)
                    Log.d("MainActivity", "Adapter item count: ${productAdapter.itemCount}")
                    
                    // Clear selection when updating the list
                    selectedRowIndex = -1
                    updateButtonStates()
                    
                    // Show a toast with the number of records fetched
                    val message = if (incompleteProducts.isEmpty()) {
                        "未完了のレコードはありません"
                    } else {
                        "${incompleteProducts.size}件の未完了レコードを取得しました"
                    }
                    Log.d("MainActivity", "Showing toast: $message")
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "データの取得に失敗しました: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
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