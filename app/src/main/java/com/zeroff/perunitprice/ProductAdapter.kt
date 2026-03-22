package com.zeroff.perunitprice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private val onAddProduct: (String, String, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_INPUT = 0
        private const val TYPE_PRODUCT = 1
    }

    private val products = mutableListOf<Product>()

    fun submitList(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_INPUT else TYPE_PRODUCT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_INPUT) {
            val view = inflater.inflate(R.layout.item_input, parent, false)
            InputViewHolder(view, onAddProduct)
        } else {
            val view = inflater.inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ProductViewHolder) {
            holder.bind(products[position - 1], position - 1)
        }
    }

    override fun getItemCount(): Int = products.size + 1

    class InputViewHolder(
        itemView: View,
        private val onAddProduct: (String, String, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val editName: EditText = itemView.findViewById(R.id.editName)
        private val editPrice: EditText = itemView.findViewById(R.id.editPrice)
        private val editQuantity: EditText = itemView.findViewById(R.id.editQuantity)
        private val btnAdd: Button = itemView.findViewById(R.id.btnAdd)

        init {
            btnAdd.setOnClickListener {
                tryAddProduct()
            }

            editQuantity.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    tryAddProduct()
                    true
                } else {
                    false
                }
            }
        }

        private fun tryAddProduct() {
            val name = editName.text.toString().ifEmpty { "Product" }
            val price = editPrice.text.toString()
            val quantity = editQuantity.text.toString()
            if (price.isNotEmpty()) {
                onAddProduct(name, price, quantity)
                editName.text.clear()
                editPrice.text.clear()
                editQuantity.text.clear()
                editName.requestFocus()
            }
        }
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textRank: TextView = itemView.findViewById(R.id.textRank)
        private val textName: TextView = itemView.findViewById(R.id.textName)
        private val textInputs: TextView = itemView.findViewById(R.id.textInputs)
        private val textResult: TextView = itemView.findViewById(R.id.textResult)

        fun bind(product: Product, position: Int) {
            val rank = position + 1
            textRank.text = "#$rank"
            textName.text = product.name
            textInputs.text = "Price: ${product.priceInput}, Qty: ${product.quantityInput}"
            textResult.text = product.formattedResult
        }
    }
}