package com.zeroff.perunitprice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val products = mutableListOf<Product>()

    fun submitList(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position], position)
    }

    override fun getItemCount(): Int = products.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textRank: TextView = itemView.findViewById(R.id.textRank)
        private val textInputs: TextView = itemView.findViewById(R.id.textInputs)
        private val textResult: TextView = itemView.findViewById(R.id.textResult)

        fun bind(product: Product, position: Int) {
            val rank = position + 1
            textRank.text = "#$rank"
            textInputs.text = "Price: ${product.priceInput}, Qty: ${product.quantityInput}"
            textResult.text = product.formattedResult
        }
    }
}