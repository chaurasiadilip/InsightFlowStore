package com.cex.insightflowstore.data

import com.cex.insightflowstore.analytics.AnalyticsTracker
import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.model.Order
import com.cex.insightflowstore.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class CartManager(private val tracker: AnalyticsTracker) {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    private val _discountPercent = MutableStateFlow(0.0)
    val discountPercent: StateFlow<Double> = _discountPercent.asStateFlow()

    private val _appliedCouponCode = MutableStateFlow<String?>(null)
    val appliedCouponCode: StateFlow<String?> = _appliedCouponCode.asStateFlow()

    val subtotal: Double
        get() = _items.value.sumOf { it.subtotal }

    val discountAmount: Double
        get() = subtotal * (_discountPercent.value / 100.0)

    val total: Double
        get() = (subtotal - discountAmount).coerceAtLeast(0.0)

    val itemCount: Int
        get() = _items.value.sumOf { it.quantity }

    fun addToCart(product: Product, quantity: Int = 1) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            current.add(CartItem(product = product, quantity = quantity))
        }
        _items.value = current
        tracker.logAddToCart(product, quantity)
    }

    fun removeFromCart(product: Product, quantity: Int = 1) {
        val current = _items.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            if (existing.quantity > quantity) {
                current[index] = existing.copy(quantity = existing.quantity - quantity)
                tracker.logRemoveFromCart(product, quantity)
            } else {
                current.removeAt(index)
                tracker.logRemoveFromCart(product, existing.quantity)
            }
            _items.value = current
        }
    }

    fun applyCoupon(code: String): Boolean {
        val trimmed = code.trim().uppercase()
        return when (trimmed) {
            "SAVE10" -> {
                _discountPercent.value = 10.0
                _appliedCouponCode.value = trimmed
                true
            }
            "SAVE25" -> {
                _discountPercent.value = 25.0
                _appliedCouponCode.value = trimmed
                true
            }
            "HALFPRICE" -> {
                _discountPercent.value = 50.0
                _appliedCouponCode.value = trimmed
                true
            }
            else -> false
        }
    }

    fun clearCart() {
        _items.value = emptyList()
        _discountPercent.value = 0.0
        _appliedCouponCode.value = null
    }

    fun completeCheckout(): Order {
        val currentItems = _items.value
        val order = Order(
            orderId = "ORD-" + UUID.randomUUID().toString().take(8).uppercase(),
            items = currentItems,
            totalAmount = total,
            discountAmount = discountAmount,
            timestamp = System.currentTimeMillis(),
            couponCode = _appliedCouponCode.value
        )
        tracker.logPurchase(order)
        clearCart()
        return order
    }
}
