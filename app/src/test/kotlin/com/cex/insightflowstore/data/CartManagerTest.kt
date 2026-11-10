package com.cex.insightflowstore.data

import com.cex.insightflowstore.analytics.AnalyticsLogEntry
import com.cex.insightflowstore.analytics.AnalyticsTracker
import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.model.Order
import com.cex.insightflowstore.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockAnalyticsTracker : AnalyticsTracker {
    override val eventLogs: StateFlow<List<AnalyticsLogEntry>> = MutableStateFlow(emptyList())
    override fun clearLogs() {}
    override fun setSimulationContext(simRunId: String?, scenarioCode: String?, targetClass: Int?) {}
    override fun logViewItemList(category: String, items: List<Product>) {}
    override fun logViewItem(product: Product) {}
    override fun logAddToCart(product: Product, quantity: Int) {}
    override fun logRemoveFromCart(product: Product, quantity: Int) {}
    override fun logViewCart(items: List<CartItem>, total: Double) {}
    override fun logBeginCheckout(items: List<CartItem>, total: Double) {}
    override fun logAddShippingInfo(items: List<CartItem>, total: Double) {}
    override fun logAddPaymentInfo(items: List<CartItem>, total: Double, paymentType: String) {}
    override fun logPurchase(order: Order) {}
}

class CartManagerTest {

    private lateinit var cartManager: CartManager
    private val p1 = Product("p1", "Charger", "Accessories", 40.0, 4.5, "65W charger")
    private val p2 = Product("p2", "Smartwatch", "Wearables", 160.0, 4.7, "Smartwatch v3")

    @Before
    fun setUp() {
        cartManager = CartManager(MockAnalyticsTracker())
    }

    @Test
    fun testAddToCartAndSubtotalCalculation() {
        cartManager.addToCart(p1, 2)
        cartManager.addToCart(p2, 1)

        assertEquals(3, cartManager.itemCount)
        assertEquals(240.0, cartManager.subtotal, 0.001)
        assertEquals(240.0, cartManager.total, 0.001)
    }

    @Test
    fun testApplyCouponAndDiscountCalculation() {
        cartManager.addToCart(p1, 1)
        cartManager.addToCart(p2, 1)

        val applied = cartManager.applyCoupon("HALFPRICE")
        assertTrue(applied)
        assertEquals(50.0, cartManager.discountPercent.value, 0.001)
        assertEquals(100.0, cartManager.discountAmount, 0.001)
        assertEquals(100.0, cartManager.total, 0.001)
    }

    @Test
    fun testCompleteCheckoutGeneratesReconciledOrderAndClearsCart() {
        cartManager.addToCart(p1, 1)
        cartManager.applyCoupon("SAVE25")

        val order = cartManager.completeCheckout()
        assertEquals(1, order.items.size)
        assertEquals(10.0, order.discountAmount, 0.001)
        assertEquals(30.0, order.totalAmount, 0.001)
        assertEquals("SAVE25", order.couponCode)

        assertEquals(0, cartManager.itemCount)
        assertEquals(0.0, cartManager.subtotal, 0.001)
    }
}
