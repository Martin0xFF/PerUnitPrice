package com.example.perunitprice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    // Load the native library
    init {
        System.loadLibrary("core_logic")
    }

    // Declare the native function
    private external fun calculatePerUnitPrice(price: Double, quantityStr: String): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editPrice = findViewById<EditText>(R.id.editPrice)
        val editQuantity = findViewById<EditText>(R.id.editQuantity)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val textResult = findViewById<TextView>(R.id.textResult)

        btnCalculate.setOnClickListener {
            val priceStr = editPrice.text.toString()
            val quantityStr = editQuantity.text.toString()

            val price = priceStr.toDoubleOrNull()
            if (price == null || price <= 0.0) {
                textResult.text = "Please enter a valid price."
                return@setOnClickListener
            }

            // Call the native Rust function
            val result = calculatePerUnitPrice(price, quantityStr)
            textResult.text = "Price per unit: $result"
        }
    }
}
