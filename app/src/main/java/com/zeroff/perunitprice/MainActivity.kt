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
    private var firstUnit: String? = null

    // Load the native library
    init {
        System.loadLibrary("core_logic")
    }

    // Declare the native functions
    private external fun calculatePerUnitPrice(price: Double, quantityStr: String): String
    private external fun calculateRawPerUnitPrice(price: Double, quantityStr: String): Double
    private external fun getUnit(quantityStr: String): String

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
            firstUnit = null
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

        val currentUnit = getUnit(quantityStr)
        val effectiveQuantityStr = if (currentUnit.isEmpty() && firstUnit != null) {
            if (quantityStr.isBlank()) "1$firstUnit" else "$quantityStr$firstUnit"
        } else {
            quantityStr
        }

        // Call the native Rust functions
        val formattedResult = calculatePerUnitPrice(price, effectiveQuantityStr)
        val rawPrice = calculateRawPerUnitPrice(price, effectiveQuantityStr)
        
        Log.d(TAG, "Native calculation result: $formattedResult (Raw: $rawPrice)")
        
        val newProduct = Product(name, priceStr, effectiveQuantityStr, formattedResult, rawPrice)
        productList.add(newProduct)
        
        if (firstUnit == null && currentUnit.isNotEmpty()) {
            firstUnit = currentUnit
            Log.d(TAG, "First unit set: $firstUnit")
        }
        
        // Sort list by raw price ascending
        productList.sortBy { it.rawPerUnitPrice }
        
        productAdapter.submitList(productList.toList())
    }
}