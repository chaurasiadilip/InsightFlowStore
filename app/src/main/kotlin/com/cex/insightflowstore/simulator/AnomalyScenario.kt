package com.cex.insightflowstore.simulator

enum class AnomalyScenario(
    val code: String,
    val title: String,
    val description: String,
    val targetClassIndex: Int
) {
    NORMAL(
        code = "NORMAL",
        title = "Normal Baseline Traffic",
        description = "Organic user browsing with balanced ~2-4% end-to-end conversion.",
        targetClassIndex = 0
    ),
    TRAFFIC_DECLINE(
        code = "TRAFFIC_DECLINE",
        title = "Traffic Drop (-60%)",
        description = "Simulates sudden drop in visits (e.g. ad campaign stopped or SEO dip).",
        targetClassIndex = 1
    ),
    CONVERSION_REGRESSION(
        code = "CONVERSION_REGRESSION",
        title = "Conversion Drop (Cart Dropoff)",
        description = "Users browse and add to cart, but abandon heavily before starting checkout.",
        targetClassIndex = 2
    ),
    AOV_DECLINE(
        code = "AOV_DECLINE",
        title = "AOV Drop (Low Basket Value)",
        description = "Users buy exclusively low-cost accessories with high discount coupon codes.",
        targetClassIndex = 3
    ),
    CHECKOUT_REGRESSION(
        code = "CHECKOUT_REGRESSION",
        title = "Checkout Funnel Outage",
        description = "Users begin checkout and add payment info, but fail or abort before purchase.",
        targetClassIndex = 4
    ),
    GLOBAL_ANOMALY(
        code = "GLOBAL_ANOMALY",
        title = "Global Anomaly Spike",
        description = "Severe simultaneous decline across traffic, revenue, and checkout completion.",
        targetClassIndex = 5
    )
}
