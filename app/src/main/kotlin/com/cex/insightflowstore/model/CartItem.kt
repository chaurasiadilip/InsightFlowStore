package com.cex.insightflowstore.model

data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val subtotal: Double get() = product.price * quantity
}

data class Order(
    val orderId: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val discountAmount: Double,
    val timestamp: Long,
    val couponCode: String? = null
)
