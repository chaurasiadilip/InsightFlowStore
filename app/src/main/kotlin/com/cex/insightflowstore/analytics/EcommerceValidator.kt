package com.cex.insightflowstore.analytics

import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.model.Order
import com.cex.insightflowstore.model.Product

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success(): ValidationResult = ValidationResult(true)
        fun failure(message: String): ValidationResult = ValidationResult(false, message)
    }
}

object EcommerceValidator {

    fun validateProduct(product: Product?): ValidationResult {
        if (product == null) {
            return ValidationResult.failure("Product cannot be null")
        }
        if (product.id.isBlank()) {
            return ValidationResult.failure("Product ID cannot be blank")
        }
        if (product.name.isBlank()) {
            return ValidationResult.failure("Product name cannot be blank")
        }
        if (product.category.isBlank()) {
            return ValidationResult.failure("Product category cannot be blank")
        }
        if (product.price <= 0.0) {
            return ValidationResult.failure("Product price must be greater than 0.0 (got ${product.price})")
        }
        return ValidationResult.success()
    }

    fun validateCartItem(item: CartItem?): ValidationResult {
        if (item == null) {
            return ValidationResult.failure("CartItem cannot be null")
        }
        val productResult = validateProduct(item.product)
        if (!productResult.isValid) {
            return productResult
        }
        if (item.quantity < 1) {
            return ValidationResult.failure("CartItem quantity must be at least 1 (got ${item.quantity})")
        }
        return ValidationResult.success()
    }

    fun validateCartItems(items: List<CartItem>?): ValidationResult {
        if (items.isNullOrEmpty()) {
            return ValidationResult.failure("Cart items list cannot be null or empty")
        }
        for ((index, item) in items.withIndex()) {
            val itemResult = validateCartItem(item)
            if (!itemResult.isValid) {
                return ValidationResult.failure("Invalid item at index $index: ${itemResult.errorMessage}")
            }
        }
        return ValidationResult.success()
    }

    fun validateOrder(order: Order?): ValidationResult {
        if (order == null) {
            return ValidationResult.failure("Order cannot be null")
        }
        if (order.orderId.isBlank()) {
            return ValidationResult.failure("Order transaction ID cannot be blank")
        }
        val itemsResult = validateCartItems(order.items)
        if (!itemsResult.isValid) {
            return itemsResult
        }
        val subtotal = order.items.sumOf { it.subtotal }
        if (order.discountAmount < 0.0 || order.discountAmount > subtotal + 0.01) {
            return ValidationResult.failure("Discount amount ($${order.discountAmount}) is invalid for subtotal ($${subtotal})")
        }
        val expectedTotal = (subtotal - order.discountAmount).coerceAtLeast(0.0)
        val diff = Math.abs(order.totalAmount - expectedTotal)
        if (diff > 0.01) {
            return ValidationResult.failure("Order total ($${order.totalAmount}) does not match reconciled subtotal - discount ($${expectedTotal})")
        }
        if (order.totalAmount < 0.0) {
            return ValidationResult.failure("Order total amount cannot be negative")
        }
        return ValidationResult.success()
    }

    /**
     * Allocates a total discount proportionally across a list of CartItems based on item subtotals.
     * Returns a list of allocated discount amounts for each item corresponding to the items list order.
     */
    fun allocateDiscounts(items: List<CartItem>, totalDiscount: Double): List<Double> {
        if (items.isEmpty() || totalDiscount <= 0.0) {
            return items.map { 0.0 }
        }
        val subtotal = items.sumOf { it.subtotal }
        if (subtotal <= 0.0) {
            return items.map { 0.0 }
        }
        val effectiveDiscount = totalDiscount.coerceAtMost(subtotal)
        var remainingDiscount = effectiveDiscount
        val allocated = mutableListOf<Double>()

        for ((index, item) in items.withIndex()) {
            if (index == items.lastIndex) {
                allocated.add(Math.round(remainingDiscount * 100.0) / 100.0)
            } else {
                val ratio = item.subtotal / subtotal
                val itemDiscount = Math.round(effectiveDiscount * ratio * 100.0) / 100.0
                allocated.add(itemDiscount)
                remainingDiscount -= itemDiscount
            }
        }
        return allocated
    }
}
