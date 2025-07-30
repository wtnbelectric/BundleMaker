package com.example.bundlemaker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
    private val currentProducts = mutableListOf<Product>()
    private var selectedRowIndex: Int = -1
    private var scanTargetType: String? = null

    private val qrScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val scanResult = data?.getStringExtra("scan_result") ?: ""
            val success = data?.getBooleanExtra("success", false) ?: false
            if (success && scanTargetType != null) {
                when (scanTargetType) {
                    "product" -> {
                        val newProduct = Product(product_serial = scanResult)
                        currentProducts.add(newProduct)
                        productAdapter.setProducts(currentProducts)
                        selectedRowIndex = currentProducts.size - 1
                    }
                    "robot" -> {
                        if (selectedRowIndex in currentProducts.indices) {
                            val p = currentProducts[selectedRowIndex]
                            currentProducts[selectedRowIndex] = p.copy(robot_serial = scanResult)
                            productAdapter.setProducts(currentProducts)
                        }
                    }
                    "controller" -> {
                        if (selectedRowIndex in currentProducts.indices) {
                            val p = currentProducts[selectedRowIndex]
                            currentProducts[selectedRowIndex] = p.copy(control_serial = scanResult)
                            productAdapter.setProducts(currentProducts)
                        }
                    }
                }
                updateButtonStates()
            }
        }
        scanTargetType = null
    }

    private val confirmLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val action = result.data?.getStringExtra("action")
            if (action == "register") {
                onCommit() // 登録処理
            } else if (action == "modify") {
                // 修正時は何もしない（画面に戻るだけ）
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // RecyclerViewとAdapterのセットアップ
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.product_table)
        productAdapter = ProductAdapter { position ->
            selectedRowIndex = position
            updateButtonStates()
        }
        recyclerView.adapter = productAdapter
        productAdapter.setProducts(currentProducts)

        // ボタン取得
        val productSerialBtn = findViewById<Button>(R.id.product_serial_search_button)
        val robotSerialBtn = findViewById<Button>(R.id.robot_serial_enter_button)
        val controllerSerialBtn = findViewById<Button>(R.id.controller_serial_enter_button)
        val commitBtn = findViewById<Button>(R.id.commit_button)

        // ボタンリスナー
        productSerialBtn.setOnClickListener { onProductSerialSearch() }
        robotSerialBtn.setOnClickListener { onRobotSerialEnter() }
        controllerSerialBtn.setOnClickListener { onControllerSerialEnter() }
        commitBtn.setOnClickListener { onCommit() }

        updateButtonStates()
    }

    private fun updateButtonStates() {
        val productSerialBtn = findViewById<Button>(R.id.product_serial_search_button)
        val robotSerialBtn = findViewById<Button>(R.id.robot_serial_enter_button)
        val controllerSerialBtn = findViewById<Button>(R.id.controller_serial_enter_button)
        val commitBtn = findViewById<Button>(R.id.commit_button)

        productSerialBtn.isEnabled = true
        val selected = selectedRowIndex.takeIf { it in currentProducts.indices }?.let { currentProducts[it] }
        robotSerialBtn.isEnabled = selected != null && !selected.product_serial.isNullOrBlank()
        controllerSerialBtn.isEnabled = selected != null && !selected.product_serial.isNullOrBlank()
        commitBtn.isEnabled = selected != null && selected.isComplete()
    }

    private fun onProductSerialSearch() {
        scanTargetType = "product"
        val intent = Intent(this, QRScanActivity::class.java)
        qrScanLauncher.launch(intent)
    }

    private fun onRobotSerialEnter() {
        scanTargetType = "robot"
        val intent = Intent(this, QRScanActivity::class.java)
        qrScanLauncher.launch(intent)
    }

    private fun onControllerSerialEnter() {
        scanTargetType = "controller"
        val intent = Intent(this, QRScanActivity::class.java)
        qrScanLauncher.launch(intent)
    }

    private fun onCommit() {
        val selected = selectedRowIndex.takeIf { it in currentProducts.indices }?.let { currentProducts[it] }
        if (selected == null) {
            Toast.makeText(this, "行が選択されていません。", Toast.LENGTH_SHORT).show()
            return
        }
        // バリデーション
        if (!selected.isValidProductSerial() ||
            !selected.isValidSerial(selected.robot_serial) ||
            !selected.isValidSerial(selected.control_serial)) {
            Toast.makeText(this, "入力された番号の形式が正しくありません。", Toast.LENGTH_SHORT).show()
            return
        }
        val now = System.currentTimeMillis()
        val localProduct = LocalProduct(
            product_serial = selected.product_serial,
            robot_serial = selected.robot_serial,
            control_serial = selected.control_serial,
            sales_id = selected.sales_id,
            created_at = selected.created_at ?: now,
            updated_at = now,
            sync_status = 0
        )
        lifecycleScope.launch {
            // Room保存
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "local_products.db"
            ).build()
            db.localProductDao().insert(localProduct)
            // API送信
            val retrofit = Retrofit.Builder()
                .baseUrl(getServerUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(ProductApiService::class.java)
            val req = ProductRequest(
                product_serial = selected.product_serial,
                robot_serial = selected.robot_serial,
                control_serial = selected.control_serial,
                sales_id = selected.sales_id
            )
            try {
                val resp = api.postProduct(req)
                if (resp.isSuccessful && resp.body()?.error == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "サーバーに登録しました", Toast.LENGTH_SHORT).show()
                        // 行を新規入力状態に
                        // 完了行はisLocalOnly=falseに
                        currentProducts[selectedRowIndex] = selected.copy(isLocalOnly = false)
                        productAdapter.setProducts(currentProducts)
                        updateButtonStates()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, resp.body()?.error ?: "サーバーエラー", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "サーバーとの通信に失敗しました。データはローカルに保存されました。", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getServerUrl(): String {
        // TODO: SharedPreferences等から取得。未設定時はデフォルト値
        return "http://10.0.2.2:5000/" // エミュレータ用
    }
}