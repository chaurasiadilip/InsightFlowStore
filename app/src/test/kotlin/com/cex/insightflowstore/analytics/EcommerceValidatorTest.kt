package com.cex.insightflowstore.analytics

import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.model.Order
import com.cex.insightflowstore.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EcommerceValidatorTest {

    @Test
    fun testValidProductPassesValidation() {
        val validProduct = Product(
            id = "prod_100",
            name = "Test Wireless Earbuds",
            category = "Audio",
            price = 99.99,
            rating = 4.5,
            description = "High fidelity audio"
        )
        val result = EcommerceValidator.validateProduct(validProduct)
        assertTrue(result.isValid)
    }

    @Test
    fun testBlankProductIdFailsValidation() {
        val invalidProduct = Product(
            id = "",
            name = "Test Wireless Earbuds",
            category = "Audio",
            price = 99.99,
            rating = 4.5,
            description = "High fidelity audio"
        )
        val result = EcommerceValidator.validateProduct(invalidProduct)
        assertFalse(result.isValid)
        assertEquals("Product ID cannot be blank", result.errorMessage)
    }

    @Test
    fun testBlankProductNameFailsValidation() {
        val invalidProduct = Product(
            id = "prod_101",
            name = "   ",
            category = "Audio",
            price = 99.99,
            rating = 4.5,
            description = "High fidelity audio"
        )
        val result = EcommerceValidator.validateProduct(invalidProduct)
        assertFalse(result.isValid)
        assertEquals("Product name cannot be blank", result.errorMessage)
    }

    @Test
    fun testBlankProductCategoryFailsValidation() {
        val invalidProduct = Product(
            id = "prod_101",
            name = "Earbuds",
            category = "",
            price = 99.99,
            rating = 4.5,
            description = "High fidelity audio"
        )
        val result = EcommerceValidator.validateProduct(invalidProduct)
        assertFalse(result.isValid)
        assertEquals("Product category cannot be blank", result.errorMessage)
    }

    @Test
    fun testZeroOrNegativePriceFailsValidation() {
        val invalidProduct = Product(
            id = "prod_102",
            name = "Earbuds",
            category = "Audio",
            price = 0.0,
            rating = 4.5,
            description = "Free item"
        )
        val result = EcommerceValidator.validateProduct(invalidProduct)
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("must be greater than 0.0") == true)
    }

    @Test
    fun testInvalidCartItemQuantityFailsValidation() {
        val validProduct = Product("p1", "Item 1", "Cat 1", 50.0, 4.0, "Desc")
        val invalidCartItem = CartItem(product = validProduct, quantity = 0)
        val result = EcommerceValidator.validateCartItem(invalidCartItem)
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("quantity must be at least 1") == true)
    }

    @Test
    fun testValidOrderPassesValidation() {
        val product = Product("p1", "Item 1", "Cat 1", 100.0, 4.0, "Desc")
        val item = CartItem(product, quantity = 2)
        val order = Order(
            orderId = "ORD-TEST-001",
            items = listOf(item),
            totalAmount = 150.0,
            discountAmount = 50.0,
            timestamp = System.currentTimeMillis(),
            couponCode = "SAVE50"
        )
        val result = EcommerceValidator.validateOrder(order)
        assertTrue(result.isValid)
    }

    @Test
    fun testBlankOrderIdFailsValidation() {
        val product = Product("p1", "Item 1", "Cat 1", 100.0, 4.0, "Desc")
        val item = CartItem(product, quantity = 1)
        val order = Order(
            orderId = "",
            items = listOf(item),
            totalAmount = 100.0,
            discountAmount = 0.0,
            timestamp = System.currentTimeMillis()
        )
        val result = EcommerceValidator.validateOrder(order)
        assertFalse(result.isValid)
        assertEquals("Order transaction ID cannot be blank", result.errorMessage)
    }
}
