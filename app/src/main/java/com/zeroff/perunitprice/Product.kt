package com.zeroff.perunitprice

data class Product(
    val name: String,
    val priceInput: String,
    val quantityInput: String,
    val formattedResult: String,
    val rawPerUnitPrice: Double,
)
