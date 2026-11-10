package com.cex.insightflowstore.model

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val rating: Double,
    val description: String,
    val inStock: Boolean = true
)
