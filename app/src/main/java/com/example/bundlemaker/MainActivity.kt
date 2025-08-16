package com.example.bundlemaker


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.annotation.SuppressLint
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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

    private lateinit var productSerialSearchBtn: Button
    private lateinit var robotSerialEnterBtn: Button
    private lateinit var controllerSerialEnterBtn: Button
    private lateinit var commitBtn: Button
    private lateinit var syncBtn: ImageButton
    private lateinit var confirmBtn: Button
    private lateinit var logoutBtn: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter

    private var currentProducts: MutableList<Product> = mutableListOf()
    private var selectedRowIndex: Int = -1

    @SuppressLint("MissingInflatedId")
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
        logoutBtn = findViewById(R.id.logout_button)
        recyclerView = findViewById(R.id.product_table)

        adapter = ProductAdapter(currentProducts)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        val employeeId = intent.getStringExtra("employee_id")
        val nameText = findViewById<TextView>(R.id.employee_name_text)

        if (employeeId != null) {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val employee = db.employeeDao().getEmployeeById(employeeId)
                nameText.text = employee?.name ?: "不明な従業員"
            }
        }

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

                        if (localProduct != null) {
                            val product = localProduct.toProduct()
                            selectedRowIndex = currentProducts.indexOfFirst { p -> p.product_serial == serial }

                            if (selectedRowIndex != -1) {
                                // リストに既に存在する場合のみ更新
                                currentProducts[selectedRowIndex] = product
                                adapter.selectedPosition = selectedRowIndex
                                adapter.notifyItemChanged(selectedRowIndex)
                                recyclerView.scrollToPosition(selectedRowIndex)
                                Toast.makeText(this@MainActivity, "製造番号が見つかりました", Toast.LENGTH_SHORT).show()
                            } else {
                                // リストに存在しない場合は何も追加せずメッセージのみ表示
                                // Toast.makeText(this@MainActivity, "リストに存在しない製造番号です", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "製造番号が見つかりません", Toast.LENGTH_SHORT).show()
                        }
                        updateButtonState()
                    }
                }
            }
        }

        // ロボットシリアル番号入力後の処理
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
                                // 既存のレコードを取得して更新
                                val localProduct = db.localProductDao().getProductBySerial(product.product_serial)
                                if (localProduct != null) {
                                    localProduct.robot_serial = robotSerial
                                    localProduct.updated_at = System.currentTimeMillis()
                                    db.localProductDao().update(localProduct)
                                }
                            }
                            // リストを最新の状態に更新
                            refreshProductList()
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
                                // 既存のレコードを取得して更新
                                val localProduct = db.localProductDao().getProductBySerial(product.product_serial)
                                if (localProduct != null) {
                                    localProduct.control_serial = controlSerial
                                    localProduct.updated_at = System.currentTimeMillis()
                                    db.localProductDao().update(localProduct)
                                }
                            }
                            // リストを最新の状態に更新
                            refreshProductList()
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
        syncBtn.isEnabled = true
        confirmBtn.isEnabled = true

        syncBtn.setOnClickListener {
            lifecycleScope.launch {
                // 1. サーバーに完了レコードを送信
                sendCompletedRecordsToServer()

                // 2. サーバーから未完了レコードを取得してRoomDBに保存
                fetchAndSaveIncompleteRecords()

                // 3. RoomDBから最新のデータを取得して表示
                refreshProductList()

                Toast.makeText(this@MainActivity, "同期が完了しました", Toast.LENGTH_SHORT).show()
            }
        }

        confirmBtn.setOnClickListener {
            lifecycleScope.launch {
                val db = Room.databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    "local_products"
                ).build()

                val allProducts = withContext(Dispatchers.IO) {
                    db.localProductDao().getAll().map { it.toProduct() }
                }

                val intent = Intent(this@MainActivity, ConfirmActivity::class.java)
                intent.putExtra("products", ArrayList(allProducts))
                startActivity(intent)
            }
        }

        // ログアウトボタン
        logoutBtn.isEnabled = true

        logoutBtn.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("ログアウトしますか？")
                .setPositiveButton("はい") { _, _ ->
                    finishAffinity() // アプリ終了
                }
                .setNegativeButton("いいえ", null)
                .show()
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
        // editTextにフォーカスを設定
        editText.requestFocus()
        // ダイアログ表示時に自動的にキーボードを表示しない
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        dialog.show()
    }

    // サーバーから未完成レコード取得→RoomDBへ保存
    private suspend fun fetchAndSaveIncompleteRecords() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.5.94:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ProductApiService::class.java)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "local_products"
        ).build()
        val response = withContext(Dispatchers.IO) { api.getIncompleteProducts() }
        if (response.isSuccessful) {
            response.body()?.forEach { prod ->
                val localProduct = LocalProduct(
                    product_serial = prod.product_serial,
                    robot_serial = prod.robot_serial ?: "",
                    control_serial = prod.control_serial ?: "",
                    created_at = prod.created_at ?: System.currentTimeMillis(),
                    updated_at = prod.updated_at,
                    sync_status = 0 // 未完成
                )
                withContext(Dispatchers.IO) {
                    db.localProductDao().insert(localProduct)
                }
            }
        }
    }

    // RoomDBの完成レコードのみサーバー送信
    private suspend fun sendCompletedRecordsToServer() {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "local_products"
        ).build()
        val completed = withContext(Dispatchers.IO) {
            db.localProductDao().getCompleted()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.5.94:5000/")
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
                Log.d("API", "送信データ: $request")
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

    private suspend fun fetchIncompleteRecordsFromServer(): List<Product> {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.5.94:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(ProductApiService::class.java)
        return try {
            val response = withContext(Dispatchers.IO) { api.getIncompleteProducts() }
            if (response.isSuccessful) {
                response.body()?.map { prod ->
                    Product(
                        product_serial = prod.product_serial,
                        robot_serial = prod.robot_serial,
                        control_serial = prod.control_serial,
                        sales_id = prod.sales_id,
                        created_at = prod.created_at,
                        updated_at = prod.updated_at,
                        isLocalOnly = false
                    )
                } ?: emptyList()
            } else {
                Log.e("API", "未完成レコード取得失敗: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("API", "未完成レコード取得例外: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchAllRecordsFromServer(): List<Product> {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.5.94:5000/")
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

    // RecyclerViewはRoomDBの内容を表示
    private suspend fun refreshProductList() {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "local_products"
        ).build()

        // サーバーから取得した未完成レコードのみを表示
        val products = withContext(Dispatchers.IO) {
            db.localProductDao().getIncomplete().map { it.toProduct() }
        }

        // UIスレッドでアダプターを更新
        runOnUiThread {
            currentProducts.clear()
            currentProducts.addAll(products)
            adapter.updateProducts(products)
            updateButtonState()
        }
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
