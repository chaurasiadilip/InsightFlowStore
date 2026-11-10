package com.cex.insightflowstore.analytics

import android.os.Bundle
import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.model.Product

object EcommerceEvents {
    // GA4 Standard Event Names
    const val EVENT_VIEW_ITEM_LIST = "view_item_list"
    const val EVENT_VIEW_ITEM = "view_item"
    const val EVENT_ADD_TO_CART = "add_to_cart"
    const val EVENT_REMOVE_FROM_CART = "remove_from_cart"
    const val EVENT_VIEW_CART = "view_cart"
    const val EVENT_BEGIN_CHECKOUT = "begin_checkout"
    const val EVENT_ADD_SHIPPING_INFO = "add_shipping_info"
    const val EVENT_ADD_PAYMENT_INFO = "add_payment_info"
    const val EVENT_PURCHASE = "purchase"

    // GA4 Standard Parameter Names
    const val PARAM_ITEM_ID = "item_id"
    const val PARAM_ITEM_NAME = "item_name"
    const val PARAM_ITEM_CATEGORY = "item_category"
    const val PARAM_ITEM_LIST_ID = "item_list_id"
    const val PARAM_ITEM_LIST_NAME = "item_list_name"
    const val PARAM_PRICE = "price"
    const val PARAM_QUANTITY = "quantity"
    const val PARAM_CURRENCY = "currency"
    const val PARAM_VALUE = "value"
    const val PARAM_DISCOUNT = "discount"
    const val PARAM_COUPON = "coupon"
    const val PARAM_ITEMS = "items"
    const val PARAM_TRANSACTION_ID = "transaction_id"
    const val PARAM_PAYMENT_TYPE = "payment_type"

    // Simulation Provenance Parameters
    const val PARAM_SIM_RUN_ID = "sim_run_id"
    const val PARAM_SCENARIO_CODE = "scenario_code"
    const val PARAM_TARGET_CLASS = "target_class"
    const val PARAM_IS_SYNTHETIC = "is_synthetic"

    const val DEFAULT_CURRENCY = "USD"

    fun productToBundle(product: Product, quantity: Int = 1): Bundle {
        return Bundle().apply {
            putString(PARAM_ITEM_ID, product.id)
            putString(PARAM_ITEM_NAME, product.name)
            putString(PARAM_ITEM_CATEGORY, product.category)
            putDouble(PARAM_PRICE, product.price)
            putLong(PARAM_QUANTITY, quantity.toLong())
        }
    }

    fun cartItemToBundle(item: CartItem, itemDiscount: Double = 0.0): Bundle {
        return Bundle().apply {
            putString(PARAM_ITEM_ID, item.product.id)
            putString(PARAM_ITEM_NAME, item.product.name)
            putString(PARAM_ITEM_CATEGORY, item.product.category)
            putDouble(PARAM_PRICE, item.product.price)
            putLong(PARAM_QUANTITY, item.quantity.toLong())
            if (itemDiscount > 0.0) {
                putDouble(PARAM_DISCOUNT, itemDiscount)
            }
        }
    }
}
