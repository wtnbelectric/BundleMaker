package com.example.bundlemaker

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bundlemaker.adapter.ProductAdapter
import com.example.bundlemaker.databinding.ActivityConfirmBinding
import com.example.bundlemaker.model.Product

class ConfirmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmBinding
    private lateinit var adapter: ProductAdapter
    private var products: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // インテントからデータを取得
        @Suppress("UNCHECKED_CAST")
        products = intent.getParcelableArrayListExtra<Product>("all_products") as? ArrayList<Product> ?: emptyList()

        if (products.isEmpty()) {
            showToast("表示する製品がありません")
            finish()
            return
        }

        setupRecyclerView()
        setupClickListeners()
        // 最初のアイテムを表示
        updateProductDetails(products.first())
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter { position ->
            // 行が選択されたら詳細を更新
            updateProductDetails(products[position])
            // スクroll位置を調整（オプション）
            binding.recyclerView.smoothScrollToPosition(position)
        }

        // RecyclerViewのセットアップ
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        
        // アニメーションを追加（オプション）
        binding.recyclerView.itemAnimator?.addDuration = 200

        // 製品リストをアダプターにセット
        adapter.setProducts(products)
    }

    private fun setupClickListeners() {
        // 登録ボタンの処理
        binding.buttonRegister.setOnClickListener {
            showToast("登録が完了しました")
            // 登録処理を実装
            finish()
        }

        // 修正ボタンの処理
        binding.buttonModify.setOnClickListener {
            // 修正処理を実装（前の画面に戻る）
            finish()
        }

        // キャンセルボタンの処理
        binding.buttonCancel.setOnClickListener {
            // キャンセル処理（前の画面に戻る）
            finish()
        }
    }

    private fun updateProductDetails(product: Product) {
        // 製品の詳細情報を表示
        binding.textProductSerial.text = product.product_serial
        binding.textRobotSerial.text = product.robot_serial ?: "未設定"
        binding.textControlSerial.text = product.control_serial ?: "未設定"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}