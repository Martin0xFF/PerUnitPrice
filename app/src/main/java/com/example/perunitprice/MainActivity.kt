package com.example.perunitprice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val TAG = "[PUP][UI]"

    // Load the native library
    init {
        System.loadLibrary("core_logic")
    }

    // Declare the native function
    private external fun calculatePerUnitPrice(price: Double, quantityStr: String): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "Activity created")

        val editPrice = findViewById<EditText>(R.id.editPrice)
        val editQuantity = findViewById<EditText>(R.id.editQuantity)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val textResult = findViewById<TextView>(R.id.textResult)

        btnCalculate.setOnClickListener {
            val priceStr = editPrice.text.toString()
            val quantityStr = editQuantity.text.toString()

            Log.d(TAG, "Calculate clicked: price=$priceStr, quantity=$quantityStr")

            val price = priceStr.toDoubleOrNull()
            if (price == null || price <= 0.0) {
                Log.w(TAG, "Invalid price input: $priceStr")
                textResult.text = "Please enter a valid price."
                return@setOnClickListener
            }

            // Call the native Rust function
            val result = calculatePerUnitPrice(price, quantityStr)
            Log.d(TAG, "Native calculation result: $result")
            textResult.text = "Price per unit: $result"
        }
    }
}
