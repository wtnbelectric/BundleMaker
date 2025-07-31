package com.example.bundlemaker.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bundlemaker.R
import com.example.bundlemaker.model.Product

class ProductAdapter(
    private val onRowSelected: (Int) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {
    private var products = mutableListOf<Product>()
    var selectedPosition: Int = -1

    fun setProducts(list: List<Product>) {
        products.clear()
        products.addAll(list)
        notifyDataSetChanged()
    }

    fun addProduct(product: Product) {
        products.add(product)
        notifyItemInserted(products.size - 1)
    }

    fun updateProductAt(position: Int, product: Product) {
        if (position in products.indices) {
            products[position] = product
            notifyItemChanged(position)
        }
    }

    fun getProductAt(position: Int): Product? =
        if (position in products.indices) products[position] else null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.productSerial.text = product.product_serial
        holder.robotSerial.text = product.robot_serial ?: ""
        holder.controlSerial.text = product.control_serial ?: ""
        // 商談No.や時刻は非表示または未実装
        holder.itemView.setBackgroundColor(
            if (position == selectedPosition) Color.parseColor("#D0E8FF")
            else Color.TRANSPARENT
        )
        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onRowSelected(position)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productSerial: TextView = itemView.findViewById(R.id.product_serial)
        val robotSerial: TextView = itemView.findViewById(R.id.robot_serial)
        val controlSerial: TextView = itemView.findViewById(R.id.control_serial)
    }
}
