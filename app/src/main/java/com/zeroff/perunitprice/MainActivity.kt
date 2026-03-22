package com.zeroff.perunitprice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val TAG = "[PUP][UI]"
    private val productList = mutableListOf<Product>()
    private lateinit var productAdapter: ProductAdapter

    // Load the native library
    init {
        System.loadLibrary("core_logic")
    }

    // Declare the native functions
    private external fun calculatePerUnitPrice(price: Double, quantityStr: String): String
    private external fun calculateRawPerUnitPrice(price: Double, quantityStr: String): Double

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "Activity created")

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val btnReset = findViewById<Button>(R.id.btnReset)

        productAdapter = ProductAdapter { name, priceStr, quantityStr ->
            addItem(name, priceStr, quantityStr)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productAdapter

        btnReset.setOnClickListener {
            Log.d(TAG, "Reset All clicked")
            productList.clear()
            productAdapter.submitList(emptyList())
            Toast.makeText(this, "All items cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addItem(name: String, priceStr: String, quantityStr: String) {
        Log.d(TAG, "Add Item triggered: name=$name, price=$priceStr, quantity=$quantityStr")

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            Log.w(TAG, "Invalid price input: $priceStr")
            Toast.makeText(this, "Please enter a valid price.", Toast.LENGTH_SHORT).show()
            return
        }

        // Call the native Rust functions
        val formattedResult = calculatePerUnitPrice(price, quantityStr)
        val rawPrice = calculateRawPerUnitPrice(price, quantityStr)
        
        Log.d(TAG, "Native calculation result: $formattedResult (Raw: $rawPrice)")
        
        val newProduct = Product(name, priceStr, quantityStr, formattedResult, rawPrice)
        productList.add(newProduct)
        
        // Sort list by raw price ascending
        productList.sortBy { it.rawPerUnitPrice }
        
        productAdapter.submitList(productList.toList())
    }
}