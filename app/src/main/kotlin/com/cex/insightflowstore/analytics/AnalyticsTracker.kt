package com.cex.insightflowstore.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.model.Order
import com.cex.insightflowstore.model.Product
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SimulationContext(
    val simRunId: String,
    val scenarioCode: String,
    val targetClass: Int
)

data class AnalyticsLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val eventName: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val simRunId: String? = null,
    val scenarioCode: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

interface AnalyticsTracker {
    val eventLogs: StateFlow<List<AnalyticsLogEntry>>
    fun clearLogs()
    fun setSimulationContext(simRunId: String?, scenarioCode: String?, targetClass: Int?)
    fun logViewItemList(category: String, items: List<Product>)
    fun logViewItem(product: Product)
    fun logAddToCart(product: Product, quantity: Int = 1)
    fun logRemoveFromCart(product: Product, quantity: Int = 1)
    fun logViewCart(items: List<CartItem>, total: Double)
    fun logBeginCheckout(items: List<CartItem>, total: Double)
    fun logAddShippingInfo(items: List<CartItem>, total: Double)
    fun logAddPaymentInfo(items: List<CartItem>, total: Double, paymentType: String = "Credit Card")
    fun logPurchase(order: Order)
}

class AppAnalyticsTracker(private val context: Context) : AnalyticsTracker {

    private val firebase: FirebaseAnalytics? by lazy {
        try {
            FirebaseAnalytics.getInstance(context)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics not available (google-services.json may not be configured yet): ${e.message}")
            null
        }
    }

    private val _eventLogs = MutableStateFlow<List<AnalyticsLogEntry>>(emptyList())
    override val eventLogs: StateFlow<List<AnalyticsLogEntry>> = _eventLogs.asStateFlow()

    @Volatile
    private var activeSimContext: SimulationContext? = null

    override fun setSimulationContext(simRunId: String?, scenarioCode: String?, targetClass: Int?) {
        if (scenarioCode != null && targetClass != null) {
            activeSimContext = SimulationContext(simRunId ?: "", scenarioCode, targetClass)
            firebase?.apply {
                setUserProperty(EcommerceEvents.PARAM_SCENARIO_CODE, scenarioCode)
                setUserProperty(EcommerceEvents.PARAM_IS_SYNTHETIC, "true")
            }
        } else {
            activeSimContext = null
            firebase?.apply {
                setUserProperty(EcommerceEvents.PARAM_SCENARIO_CODE, null)
                setUserProperty(EcommerceEvents.PARAM_IS_SYNTHETIC, "false")
            }
        }
    }

    override fun clearLogs() {
        _eventLogs.value = emptyList()
    }

    private fun recordEvent(name: String, details: String, params: Bundle? = null) {
        val eventParams = (params ?: Bundle()).apply {
            activeSimContext?.let { sim ->
                putString(EcommerceEvents.PARAM_SCENARIO_CODE, sim.scenarioCode)
                putInt(EcommerceEvents.PARAM_TARGET_CLASS, sim.targetClass)
                putLong(EcommerceEvents.PARAM_IS_SYNTHETIC, 1L)
            }
        }

        val formattedDetails = activeSimContext?.let { sim ->
            "$details [Sim:${sim.scenarioCode}]"
        } ?: details

        Log.i(TAG, "[$name] $formattedDetails")
        firebase?.logEvent(name, eventParams)

        val entry = AnalyticsLogEntry(
            eventName = name,
            details = formattedDetails,
            simRunId = activeSimContext?.simRunId,
            scenarioCode = activeSimContext?.scenarioCode
        )
        val current = _eventLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 200) {
            _eventLogs.value = current.take(200)
        } else {
            _eventLogs.value = current
        }
    }

    private fun recordValidationError(eventName: String, message: String) {
        val warningDetails = "⚠️ SKIPPED: $message"
        Log.w(TAG, "[$eventName] $warningDetails")
        val entry = AnalyticsLogEntry(
            eventName = "$eventName (Invalid)",
            details = warningDetails,
            simRunId = activeSimContext?.simRunId,
            scenarioCode = activeSimContext?.scenarioCode
        )
        val current = _eventLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 200) {
            _eventLogs.value = current.take(200)
        } else {
            _eventLogs.value = current
        }
    }

    override fun logViewItemList(category: String, items: List<Product>) {
        if (category.isBlank() || items.isEmpty()) {
            recordValidationError(EcommerceEvents.EVENT_VIEW_ITEM_LIST, "Category cannot be blank and items list cannot be empty")
            return
        }
        for (product in items) {
            val validation = EcommerceValidator.validateProduct(product)
            if (!validation.isValid) {
                recordValidationError(EcommerceEvents.EVENT_VIEW_ITEM_LIST, validation.errorMessage ?: "Invalid product in list")
                return
            }
        }

        val itemBundles = items.map { EcommerceEvents.productToBundle(it) }.toTypedArray()
        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_ITEM_LIST_ID, category.lowercase(Locale.US))
            putString(EcommerceEvents.PARAM_ITEM_LIST_NAME, category)
            putString(EcommerceEvents.PARAM_ITEM_CATEGORY, category)
            putParcelableArray(EcommerceEvents.PARAM_ITEMS, itemBundles)
        }
        recordEvent(
            EcommerceEvents.EVENT_VIEW_ITEM_LIST,
            "Browsed category: $category (${items.size} items)",
            params
        )
    }

    override fun logViewItem(product: Product) {
        val validation = EcommerceValidator.validateProduct(product)
        if (!validation.isValid) {
            recordValidationError(EcommerceEvents.EVENT_VIEW_ITEM, validation.errorMessage ?: "Invalid product")
            return
        }

        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_CURRENCY, EcommerceEvents.DEFAULT_CURRENCY)
            putDouble(EcommerceEvents.PARAM_VALUE, product.price)
            putParcelableArray(
                EcommerceEvents.PARAM_ITEMS,
                arrayOf(EcommerceEvents.productToBundle(product))
            )
        }
        recordEvent(
            EcommerceEvents.EVENT_VIEW_ITEM,
            "Viewed '${product.name}' ($${product.price})",
            params
        )
    }

    override fun logAddToCart(product: Product, quantity: Int) {
        val cartItem = CartItem(product, quantity)
        val validation = EcommerceValidator.validateCartItem(cartItem)
        if (!validation.isValid) {
            recordValidationError(EcommerceEvents.EVENT_ADD_TO_CART, validation.errorMessage ?: "Invalid item or quantity")
            return
        }

        val total = product.price * quantity
        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_CURRENCY, EcommerceEvents.DEFAULT_CURRENCY)
            putDouble(EcommerceEvents.PARAM_VALUE, total)
            putParcelableArray(
                EcommerceEvents.PARAM_ITEMS,
                arrayOf(EcommerceEvents.productToBundle(product, quantity))
            )
        }
        recordEvent(
            EcommerceEvents.EVENT_ADD_TO_CART,
            "Added $quantity x '${product.name}' ($${total})",
            params
        )
    }

    override fun logRemoveFromCart(product: Product, quantity: Int) {
        val cartItem = CartItem(product, quantity)
        val validation = EcommerceValidator.validateCartItem(cartItem)
        if (!validation.isValid) {
            recordValidationError(EcommerceEvents.EVENT_REMOVE_FROM_CART, validation.errorMessage ?: "Invalid item or quantity")
            return
        }

        val total = product.price * quantity
        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_CURRENCY, EcommerceEvents.DEFAULT_CURRENCY)
            putDouble(EcommerceEvents.PARAM_VALUE, total)
            putParcelableArray(
                EcommerceEvents.PARAM_ITEMS,
                arrayOf(EcommerceEvents.productToBundle(product, quantity))
            )
        }
        recordEvent(
            EcommerceEvents.EVENT_REMOVE_FROM_CART,
            "Removed $quantity x '${product.name}' ($${total})",
            params
        )
    }

    override fun logViewCart(items: List<CartItem>, total: Double) {
        val validation = EcommerceValidator.validateCartItems(items)
        if (!validation.isValid) {
            recordValidationError(EcommerceEvents.EVENT_VIEW_CART, validation.errorMessage ?: "Invalid cart items")
            return
        }

        val bundles = items.map { EcommerceEvents.cartItemToBundle(it) }.toTypedArray()
        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_CURRENCY, EcommerceEvents.DEFAULT_CURRENCY)
            putDouble(EcommerceEvents.PARAM_VALUE, total)
            putParcelableArray(EcommerceEvents.PARAM_ITEMS, bundles)
        }
        recordEvent(
            EcommerceEvents.EVENT_VIEW_CART,
            "Viewed cart: ${items.size} unique items, Total=$${String.format(Locale.US, "%.2f", total)}",
            params
        )
    }

    override fun logBeginCheckout(items: List<CartItem>, total: Double) {
        val validation = EcommerceValidator.validateCartItems(items)
        if (!validation.isValid) {
            recordValidationError(EcommerceEvents.EVENT_BEGIN_CHECKOUT, validation.errorMessage ?: "Invalid cart items")
            return
        }

        val bundles = items.map { EcommerceEvents.cartItemToBundle(it) }.toTypedArray()
        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_CURRENCY, EcommerceEvents.DEFAULT_CURRENCY)
            putDouble(EcommerceEvents.PARAM_VALUE, total)
            putParcelableArray(EcommerceEvents.PARAM_ITEMS, bundles)
        }
        recordEvent(
            EcommerceEvents.EVENT_BEGIN_CHECKOUT,
            "Started checkout: ${items.size} items, Total=$${String.format(Locale.US, "%.2f", total)}",
            params
        )
    }

    override fun logAddShippingInfo(items: List<CartItem>, total: Double) {
        val validation = EcommerceValidator.validateCartItems(items)
        if (!validation.isValid) {
            recordValidationError(EcommerceEvents.EVENT_ADD_SHIPPING_INFO, validation.errorMessage ?: "Invalid cart items")
            return
        }

        val bundles = items.map { EcommerceEvents.cartItemToBundle(it) }.toTypedArray()
        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_CURRENCY, EcommerceEvents.DEFAULT_CURRENCY)
            putDouble(EcommerceEvents.PARAM_VALUE, total)
            putParcelableArray(EcommerceEvents.PARAM_ITEMS, bundles)
        }
        recordEvent(
            EcommerceEvents.EVENT_ADD_SHIPPING_INFO,
            "Added shipping details, Total=$${String.format(Locale.US, "%.2f", total)}",
            params
        )
    }

    override fun logAddPaymentInfo(items: List<CartItem>, total: Double, paymentType: String) {
        val validation = EcommerceValidator.validateCartItems(items)
        if (!validation.isValid) {
            recordValidationError(EcommerceEvents.EVENT_ADD_PAYMENT_INFO, validation.errorMessage ?: "Invalid cart items")
            return
        }

        val bundles = items.map { EcommerceEvents.cartItemToBundle(it) }.toTypedArray()
        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_CURRENCY, EcommerceEvents.DEFAULT_CURRENCY)
            putDouble(EcommerceEvents.PARAM_VALUE, total)
            putString(EcommerceEvents.PARAM_PAYMENT_TYPE, paymentType)
            putParcelableArray(EcommerceEvents.PARAM_ITEMS, bundles)
        }
        recordEvent(
            EcommerceEvents.EVENT_ADD_PAYMENT_INFO,
            "Added payment info ($paymentType), Total=$${String.format(Locale.US, "%.2f", total)}",
            params
        )
    }

    override fun logPurchase(order: Order) {
        val validation = EcommerceValidator.validateOrder(order)
        if (!validation.isValid) {
            recordValidationError(EcommerceEvents.EVENT_PURCHASE, validation.errorMessage ?: "Invalid order payload")
            return
        }

        val allocatedDiscounts = EcommerceValidator.allocateDiscounts(order.items, order.discountAmount)
        val bundles = order.items.mapIndexed { index, item ->
            EcommerceEvents.cartItemToBundle(item, allocatedDiscounts[index])
        }.toTypedArray()

        val params = Bundle().apply {
            putString(EcommerceEvents.PARAM_TRANSACTION_ID, order.orderId)
            putString(EcommerceEvents.PARAM_CURRENCY, EcommerceEvents.DEFAULT_CURRENCY)
            putDouble(EcommerceEvents.PARAM_VALUE, order.totalAmount)
            if (order.discountAmount > 0.0) {
                putDouble(EcommerceEvents.PARAM_DISCOUNT, order.discountAmount)
            }
            order.couponCode?.let {
                putString(EcommerceEvents.PARAM_COUPON, it)
            }
            putParcelableArray(EcommerceEvents.PARAM_ITEMS, bundles)
        }
        val discountText = if (order.discountAmount > 0.0) " (Discount=$${String.format(Locale.US, "%.2f", order.discountAmount)})" else ""
        recordEvent(
            EcommerceEvents.EVENT_PURCHASE,
            "Completed Purchase #${order.orderId}: Total=$${String.format(Locale.US, "%.2f", order.totalAmount)}$discountText",
            params
        )
    }

    companion object {
        private const val TAG = "GA4_ANALYTICS"
    }
}
