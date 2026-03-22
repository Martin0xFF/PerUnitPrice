package com.zeroff.perunitprice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private val TAG = "[PUP][UI]"
    private lateinit var productAdapter: ProductAdapter
    private var firstUnit: String? = null
    
    private val PREFS_NAME = "pup_prefs"
    private val KEY_DARK_MODE = "dark_mode"

    // Load the native library
    init {
        System.loadLibrary("core_logic")
    }

    // Declare the native functions
    private external fun calculatePerUnitPrice(price: Double, quantityStr: String): String
    private external fun calculateRawPerUnitPrice(price: Double, quantityStr: String): Double
    private external fun getUnit(quantityStr: String): String
    
    // New storage management functions
    private external fun addProduct(name: String, priceStr: String, quantityStr: String)
    private external fun clearProducts()
    private external fun getProductCount(): Int
    private external fun getProductAt(index: Int): String

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        
        // Apply theme before super.onCreate and setContentView
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "Activity created")

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)

        // Set navigation icon based on current mode
        toolbar.setNavigationIcon(if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon)

        toolbar.setNavigationOnClickListener {
            val currentMode = prefs.getBoolean(KEY_DARK_MODE, false)
            val newMode = !currentMode
            prefs.edit().putBoolean(KEY_DARK_MODE, newMode).apply()
            
            if (newMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        btnReset.setOnClickListener {
            Log.d(TAG, "Reset All clicked")
            clearProducts()
            refreshList()
            firstUnit = null
            Toast.makeText(this, "All items cleared", Toast.LENGTH_SHORT).show()
        }

        productAdapter = ProductAdapter { name, priceStr, quantityStr ->
            addItem(name, priceStr, quantityStr)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productAdapter

        // Refresh list to show existing products (e.g. after theme switch)
        refreshList()
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

        // Call the native Rust function to add and sort
        addProduct(name, priceStr, effectiveQuantityStr)
        
        if (firstUnit == null && currentUnit.isNotEmpty()) {
            firstUnit = currentUnit
            Log.d(TAG, "First unit set: $firstUnit")
        }
        
        refreshList()
    }

    private fun refreshList() {
        val count = getProductCount()
        val products = mutableListOf<Product>()
        for (i in 0 until count) {
            val productStr = getProductAt(i)
            if (productStr.isNotEmpty()) {
                val p = parseProduct(productStr)
                if (p != null) products.add(p)
            }
        }
        productAdapter.submitList(products)
    }

    private fun parseProduct(s: String): Product? {
        val parts = s.split(";")
        if (parts.size < 5) return null
        return Product(
            name = parts[0],
            priceInput = parts[1],
            quantityInput = parts[2],
            formattedResult = parts[3],
            rawPerUnitPrice = parts[4].toDoubleOrNull() ?: 0.0
        )
    }
}