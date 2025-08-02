package com.example.bundlemaker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bundlemaker.model.Product
import com.example.bundlemaker.R

class ProductAdapter(
    private val products: List<Product>
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    var selectedPosition: Int = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productSerialText: TextView = itemView.findViewById(R.id.product_serial_text)
        val robotSerialText: TextView = itemView.findViewById(R.id.robot_serial_text)
        val controlSerialText: TextView = itemView.findViewById(R.id.control_serial_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.productSerialText.text = product.product_serial
        holder.robotSerialText.text = product.robot_serial ?: ""
        holder.controlSerialText.text = product.control_serial ?: ""
        holder.itemView.isSelected = position == selectedPosition
    }
}
