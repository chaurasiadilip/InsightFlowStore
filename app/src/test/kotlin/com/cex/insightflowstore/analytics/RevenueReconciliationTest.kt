package com.cex.insightflowstore.analytics

import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.model.Order
import com.cex.insightflowstore.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RevenueReconciliationTest {

    @Test
    fun testProportionalDiscountAllocationSingleItem() {
        val p1 = Product("p1", "Headphones", "Audio", 200.0, 4.5, "Desc")
        val item1 = CartItem(p1, quantity = 1)
        val items = listOf(item1)

        val totalDiscount = 50.0
        val allocated = EcommerceValidator.allocateDiscounts(items, totalDiscount)

        assertEquals(1, allocated.size)
        assertEquals(50.0, allocated[0], 0.001)
    }

    @Test
    fun testProportionalDiscountAllocationMultipleItems() {
        val p1 = Product("p1", "Item A", "Cat 1", 100.0, 4.5, "Desc")
        val p2 = Product("p2", "Item B", "Cat 2", 50.0, 4.0, "Desc")
        val item1 = CartItem(p1, quantity = 1)
        val item2 = CartItem(p2, quantity = 2)
        val items = listOf(item1, item2)

        val totalDiscount = 50.0
        val allocated = EcommerceValidator.allocateDiscounts(items, totalDiscount)

        assertEquals(2, allocated.size)
        assertEquals(25.0, allocated[0], 0.001)
        assertEquals(25.0, allocated[1], 0.001)

        val sumAllocated = allocated.sum()
        assertEquals(totalDiscount, sumAllocated, 0.001)
    }

    @Test
    fun testOrderSubtotalDiscountAndNetTotalReconciliation() {
        val p1 = Product("p1", "Laptop", "Computing", 1200.0, 4.9, "Desc")
        val p2 = Product("p2", "Mouse", "Accessories", 50.0, 4.2, "Desc")
        val item1 = CartItem(p1, quantity = 1)
        val item2 = CartItem(p2, quantity = 2)
        val items = listOf(item1, item2)

        val subtotal = items.sumOf { it.subtotal }
        val discountAmount = 130.0
        val totalAmount = subtotal - discountAmount

        val order = Order(
            orderId = "ORD-RECON-001",
            items = items,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            timestamp = System.currentTimeMillis(),
            couponCode = "SAVE10"
        )

        val validation = EcommerceValidator.validateOrder(order)
        assertTrue(validation.isValid)

        val allocated = EcommerceValidator.allocateDiscounts(items, discountAmount)
        val itemPostDiscountSum = items.zip(allocated).sumOf { (item, disc) ->
            item.subtotal - disc
        }
        assertEquals(order.totalAmount, itemPostDiscountSum, 0.001)
    }
}
