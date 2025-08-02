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
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.bundlemaker.ConfirmActivity
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

    private lateinit var productSerialSearchBtn: Button
    private lateinit var robotSerialEnterBtn: Button
    private lateinit var controllerSerialEnterBtn: Button
    private lateinit var commitBtn: Button
    private lateinit var syncBtn: ImageButton
    private lateinit var confirmBtn: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter

    private var currentProducts: MutableList<Product> = mutableListOf()
    private var selectedRowIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        productSerialSearchBtn = findViewById(R.id.product_serial_search_button)
        robotSerialEnterBtn = findViewById(R.id.robot_serial_enter_button)
        controllerSerialEnterBtn = findViewById(R.id.controller_serial_enter_button)
        commitBtn = findViewById(R.id.commit_button)
        syncBtn = findViewById(R.id.sync_button)
        confirmBtn = findViewById(R.id.confirm_button)
        recyclerView = findViewById(R.id.product_table)

        adapter = ProductAdapter(currentProducts)
        recyclerView.adapter = adapter

        // ボタン初期状態
        updateButtonState()

        // 製品シリアル検索
        productSerialSearchBtn.setOnClickListener {
            showInputDialog("製造番号を入力してください") { serial ->
                if (serial.isNotBlank()) {
                    lifecycleScope.launch {
                        val db = Room.databaseBuilder(
                            applicationContext,
                            AppDatabase::class.java,
                            "local_products"
                        ).build()
                        val localProduct = withContext(Dispatchers.IO) {
                            db.localProductDao().getProductBySerial(serial)
                        }
                        val product = localProduct?.let { it.toProduct() } ?: Product(product_serial = serial)
                        if (localProduct != null) {
                            selectedRowIndex = currentProducts.indexOfFirst { p -> p.product_serial == serial }
                            if (selectedRowIndex == -1) {
                                currentProducts.add(product)
                                selectedRowIndex = currentProducts.size - 1
                            }
                            adapter.selectedPosition = selectedRowIndex
                            adapter.notifyItemChanged(selectedRowIndex)
                            Toast.makeText(this@MainActivity, "製造番号が見つかりました", Toast.LENGTH_SHORT).show()
                        } else {
                            currentProducts.add(product)
                            selectedRowIndex = currentProducts.size - 1
                            adapter.selectedPosition = selectedRowIndex
                            adapter.notifyItemInserted(selectedRowIndex)
                            Toast.makeText(this@MainActivity, "新規製造番号を追加しました", Toast.LENGTH_SHORT).show()
                        }
                        updateButtonState()
                    }
                }
            }
        }

        robotSerialEnterBtn.setOnClickListener {
            if (selectedRowIndex >= 0) {
                showInputDialog("ロボットシリアル番号を入力してください") { robotSerial ->
                    if (robotSerial.isNotBlank()) {
                        val product = currentProducts[selectedRowIndex]
                        product.robot_serial = robotSerial
                        adapter.notifyItemChanged(selectedRowIndex)
                        lifecycleScope.launch {
                            val db = Room.databaseBuilder(
                                applicationContext,
                                AppDatabase::class.java,
                                "local_products"
                            ).build()
                            withContext(Dispatchers.IO) {
                                val localProduct = db.localProductDao().getProductBySerial(product.product_serial) ?: LocalProduct(
                                    product_serial = product.product_serial,
                                    robot_serial = robotSerial,
                                    created_at = System.currentTimeMillis(),
                                    updated_at = System.currentTimeMillis()
                                )
                                db.localProductDao().insert(localProduct)
                            }
                        }
                        updateButtonState()
                    }
                }
            }
        }

        controllerSerialEnterBtn.setOnClickListener {
            if (selectedRowIndex >= 0) {
                showInputDialog("コントローラシリアル番号を入力してください") { controlSerial ->
                    if (controlSerial.isNotBlank()) {
                        val product = currentProducts[selectedRowIndex]
                        product.control_serial = controlSerial
                        adapter.notifyItemChanged(selectedRowIndex)
                        lifecycleScope.launch {
                            val db = Room.databaseBuilder(
                                applicationContext,
                                AppDatabase::class.java,
                                "local_products"
                            ).build()
                            withContext(Dispatchers.IO) {
                                val localProduct = db.localProductDao().getProductBySerial(product.product_serial) ?: LocalProduct(
                                    product_serial = product.product_serial,
                                    control_serial = controlSerial,
                                    created_at = System.currentTimeMillis(),
                                    updated_at = System.currentTimeMillis()
                                )
                                db.localProductDao().insert(localProduct)
                            }
                        }
                        updateButtonState()
                    }
                }
            }
        }

        commitBtn.setOnClickListener {
            if (selectedRowIndex >= 0) {
                val product = currentProducts[selectedRowIndex]
                if (product.product_serial.isNotBlank() &&
                    !product.robot_serial.isNullOrBlank() &&
                    !product.control_serial.isNullOrBlank()
                ) {
                    product.updated_at = System.currentTimeMillis()
                    adapter.notifyItemChanged(selectedRowIndex)
                    lifecycleScope.launch {
                        val db = Room.databaseBuilder(
                            applicationContext,
                            AppDatabase::class.java,
                            "local_products"
                        ).build()
                        withContext(Dispatchers.IO) {
                            val localProduct = LocalProduct(
                                product_serial = product.product_serial,
                                robot_serial = product.robot_serial ?: "",
                                control_serial = product.control_serial ?: "",
                                created_at = System.currentTimeMillis(),
                                updated_at = System.currentTimeMillis(),
                                sync_status = 1 // Mark as completed
                            )
                            db.localProductDao().insert(localProduct)
                        }
                    }
                    Toast.makeText(this, "レコードを確定しました", Toast.LENGTH_SHORT).show()
                    // 新しい行の準備
                    selectedRowIndex = -1
                    adapter.selectedPosition = -1
                    updateButtonState()
                }
            }
        }

        // 同期ボタン
        syncBtn.setOnClickListener {
            lifecycleScope.launch {
                sendCompletedRecordsToServer()
                fetchIncompleteRecordsFromServer()
                refreshProductList()
            }
        }

        // 確認ボタン
        confirmBtn.setOnClickListener {
            lifecycleScope.launch {
                val products = fetchAllRecordsFromServer()
                val intent = Intent(this@MainActivity, ConfirmActivity::class.java)
                intent.putExtra("products", ArrayList(products))
                startActivity(intent)
            }
        }
    }

    private fun updateButtonState() {
        productSerialSearchBtn.isEnabled = true
        robotSerialEnterBtn.isEnabled = selectedRowIndex >= 0 && currentProducts[selectedRowIndex].product_serial.isNotBlank()
        controllerSerialEnterBtn.isEnabled = selectedRowIndex >= 0 && !currentProducts[selectedRowIndex].robot_serial.isNullOrBlank()
        commitBtn.isEnabled = selectedRowIndex >= 0 &&
                !currentProducts[selectedRowIndex].product_serial.isNullOrBlank() &&
                !currentProducts[selectedRowIndex].robot_serial.isNullOrBlank() &&
                !currentProducts[selectedRowIndex].control_serial.isNullOrBlank()
    }

    private fun showInputDialog(title: String, callback: (String) -> Unit) {
        val editText = EditText(this)
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                callback(editText.text.toString())
            }
            .setNegativeButton("キャンセル", null)
            .create()
        dialog.show()
    }

    private suspend fun sendCompletedRecordsToServer() {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "local_products"
        ).build()
        val completed = withContext(Dispatchers.IO) {
            db.localProductDao().getPending() // Get all pending sync records
        }
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.5.72:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ProductApiService::class.java)

        for (localProduct in completed) {
            try {
                val request = ProductRequest(
                    product_serial = localProduct.product_serial,
                    robot_serial = localProduct.robot_serial,
                    control_serial = localProduct.control_serial
                )
                val response = withContext(Dispatchers.IO) {
                    try {
                        api.postProduct(request)
                    } catch (e: Exception) {
                        Log.e("API", "API呼び出しエラー: ${e.message}")
                        null
                    }
                }
                
                response?.let { res ->
                    if (res.isSuccessful) {
                        withContext(Dispatchers.IO) {
                            db.localProductDao().updateSyncStatus(localProduct.product_serial, 1)
                        }
                    } else {
                        val errorBody = res.errorBody()?.string()
                        Log.e("API", "送信失敗: $errorBody")
                    }
                } ?: run {
                    Log.e("API", "API呼び出しに失敗しました")
                }
            } catch (e: Exception) {
                Log.e("API", "送信例外: ${e.message}")
            }
        }
    }

    private suspend fun fetchIncompleteRecordsFromServer() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://your-server-url/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ProductApiService::class.java)
        try {
            val response = withContext(Dispatchers.IO) { api.getIncompleteProducts() }
            if (response.isSuccessful) {
                val incompleteList = response.body() ?: emptyList()
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "local_products"
                ).build()
                withContext(Dispatchers.IO) {
                    for (prod in incompleteList) {
                        db.localProductDao().insert(
                            LocalProduct(
                                product_serial = prod.product_serial,
                                robot_serial = prod.robot_serial,
                                control_serial = prod.control_serial,
                                sales_id = prod.sales_id,
                                created_at = prod.created_at ?: System.currentTimeMillis(),
                                updated_at = prod.updated_at,
                                sync_status = 0
                            )
                        )
                    }
                }
            } else {
                Log.e("API", "未完成レコード取得失敗: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("API", "未完成レコード取得例外: ${e.message}")
        }
    }

    private suspend fun fetchAllRecordsFromServer(): List<Product> {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.5.72:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ProductApiService::class.java)
        return try {
            val response = withContext(Dispatchers.IO) { api.getAllProducts() }
            if (response.isSuccessful) {
                response.body()?.map { product ->
                    Product(
                        product_serial = product.product_serial,
                        robot_serial = product.robot_serial,
                        control_serial = product.control_serial,
                        sales_id = product.sales_id,
                        created_at = product.created_at,
                        updated_at = product.updated_at,
                        isLocalOnly = false
                    )
                } ?: emptyList()
            } else {
                Log.e("API", "全レコード取得失敗: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("API", "全レコード取得例外: ${e.message}")
            emptyList()
        }
    }

    private suspend fun refreshProductList() {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "local_products"
        ).build()
        val products = withContext(Dispatchers.IO) {
            db.localProductDao().getAll().map { localProduct: LocalProduct -> localProduct.toProduct() }
        }
        currentProducts.clear()
        currentProducts.addAll(products)
        adapter.notifyDataSetChanged()
    }

    // LocalProduct拡張関数
    private fun LocalProduct.toProduct(): Product {
        return Product(
            product_serial = this.product_serial,
            robot_serial = this.robot_serial,
            control_serial = this.control_serial,
            sales_id = this.sales_id,
            created_at = this.created_at,
            updated_at = this.updated_at,
            isLocalOnly = this.sync_status == 0
        )
    }
}
