package com.cex.insightflowstore.simulator

import com.cex.insightflowstore.analytics.AnalyticsTracker
import com.cex.insightflowstore.data.SampleProducts
import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.model.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

data class SimulationState(
    val isRunning: Boolean = false,
    val simRunId: String? = null,
    val seed: Long? = null,
    val currentScenario: AnomalyScenario? = null,
    val completedSessions: Int = 0,
    val totalSessions: Int = 0,
    val statusMessage: String = "Idle"
)

class TrafficSimulationEngine(
    private val tracker: AnalyticsTracker,
    val manifestManager: SimulationManifestManager? = null,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SimulationState())
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private var activeJob: Job? = null

    @Volatile
    private var isCancelling: Boolean = false

    fun startSimulation(
        scenario: AnomalyScenario,
        sessionCount: Int = 20,
        delayMsBetweenEvents: Long = 100L,
        seed: Long? = null
    ) {
        if (_state.value.isRunning || isCancelling || (activeJob != null && activeJob?.isActive == true)) {
            return
        }

        val runId = "RUN-" + UUID.randomUUID().toString().take(8).uppercase()
        val effectiveSeed = seed ?: System.currentTimeMillis()
        val random = Random(effectiveSeed)
        val startTime = System.currentTimeMillis()

        var currentRecord = SimulationRunRecord(
            runId = runId,
            scenarioCode = scenario.code,
            targetClassIndex = scenario.targetClassIndex,
            seed = effectiveSeed,
            requestedSessions = sessionCount,
            completedSessions = 0,
            startTimestamp = startTime,
            endTimestamp = null,
            status = SimulationRunStatus.RUNNING
        )
        manifestManager?.saveRun(currentRecord)

        activeJob = scope.launch(Dispatchers.Default) {
            _state.value = SimulationState(
                isRunning = true,
                simRunId = runId,
                seed = effectiveSeed,
                currentScenario = scenario,
                completedSessions = 0,
                totalSessions = sessionCount,
                statusMessage = "Starting ${scenario.title} ($sessionCount sessions)..."
            )

            tracker.setSimulationContext(runId, scenario.code, scenario.targetClassIndex)

            var completedCount = 0
            try {
                for (i in 1..sessionCount) {
                    _state.value = _state.value.copy(
                        completedSessions = completedCount,
                        statusMessage = "Simulating session $i / $sessionCount (${scenario.code} | Run: ${runId.take(8)})"
                    )

                    simulateSingleSession(scenario, delayMsBetweenEvents, random)

                    completedCount = i
                    currentRecord = currentRecord.copy(completedSessions = completedCount)
                    manifestManager?.saveRun(currentRecord)

                    _state.value = _state.value.copy(
                        completedSessions = completedCount
                    )

                    delay(delayMsBetweenEvents)
                }

                currentRecord = currentRecord.copy(
                    completedSessions = completedCount,
                    endTimestamp = System.currentTimeMillis(),
                    status = SimulationRunStatus.COMPLETED
                )
                manifestManager?.saveRun(currentRecord)

                _state.value = _state.value.copy(
                    isRunning = false,
                    completedSessions = completedCount,
                    statusMessage = "Completed $completedCount / $sessionCount sessions for ${scenario.title} (Run: $runId, Seed: $effectiveSeed)."
                )
            } catch (_: CancellationException) {
                currentRecord = currentRecord.copy(
                    completedSessions = completedCount,
                    endTimestamp = System.currentTimeMillis(),
                    status = SimulationRunStatus.CANCELLED
                )
                manifestManager?.saveRun(currentRecord)

                _state.value = _state.value.copy(
                    isRunning = false,
                    completedSessions = completedCount,
                    statusMessage = "Simulation cancelled ($completedCount / $sessionCount sessions completed)."
                )
            } catch (e: Exception) {
                currentRecord = currentRecord.copy(
                    completedSessions = completedCount,
                    endTimestamp = System.currentTimeMillis(),
                    status = SimulationRunStatus.FAILED
                )
                manifestManager?.saveRun(currentRecord)

                _state.value = _state.value.copy(
                    isRunning = false,
                    completedSessions = completedCount,
                    statusMessage = "Error during simulation: ${e.message}"
                )
            } finally {
                isCancelling = false
                tracker.setSimulationContext(null, null, null)
            }
        }
    }

    fun stopSimulation() {
        if (!_state.value.isRunning) return
        isCancelling = true
        _state.value = _state.value.copy(
            statusMessage = "Cancelling simulation cleanup in progress..."
        )
        activeJob?.cancel()
    }

    private suspend fun simulateSingleSession(
        scenario: AnomalyScenario,
        stepDelay: Long,
        random: Random
    ) {
        val products = SampleProducts.items
        val categories = products.map { it.category }.distinct()

        // Step 1: Browse category / view list
        val selectedCategory = categories.random(random)
        val categoryProducts = products.filter { it.category == selectedCategory }
        tracker.logViewItemList(selectedCategory, categoryProducts)
        delay(stepDelay)

        // Adjust candidate products based on AOV_DECLINE scenario
        val pool = when (scenario) {
            AnomalyScenario.AOV_DECLINE -> products.filter { it.price < 60.0 }.ifEmpty { products }
            else -> products
        }

        // Step 2: View 1 to 3 items
        val viewCount = when (scenario) {
            AnomalyScenario.TRAFFIC_DECLINE -> 1
            else -> random.nextInt(1, 4)
        }
        val viewedItems = pool.shuffled(random).take(viewCount)
        for (item in viewedItems) {
            tracker.logViewItem(item)
            delay(stepDelay)
        }

        // Step 3: Add to cart probability
        val addToCartProb = when (scenario) {
            AnomalyScenario.NORMAL -> 0.25
            AnomalyScenario.TRAFFIC_DECLINE -> 0.25
            AnomalyScenario.CONVERSION_REGRESSION -> 0.35
            AnomalyScenario.AOV_DECLINE -> 0.30
            AnomalyScenario.CHECKOUT_REGRESSION -> 0.30
            AnomalyScenario.GLOBAL_ANOMALY -> 0.10
        }

        if (random.nextDouble() > addToCartProb) {
            // User bounced after browsing
            return
        }

        val cartProducts = viewedItems.shuffled(random).take(random.nextInt(1, viewedItems.size + 1))
        val cartItems = cartProducts.map {
            CartItem(
                product = it,
                quantity = if (scenario == AnomalyScenario.AOV_DECLINE) 1 else random.nextInt(1, 3)
            )
        }

        for (item in cartItems) {
            tracker.logAddToCart(item.product, item.quantity)
            delay(stepDelay)
        }

        val cartSubtotal = cartItems.sumOf { it.subtotal }
        tracker.logViewCart(cartItems, cartSubtotal)
        delay(stepDelay)

        // Step 4: Begin Checkout probability
        val beginCheckoutProb = when (scenario) {
            AnomalyScenario.NORMAL -> 0.40
            AnomalyScenario.TRAFFIC_DECLINE -> 0.35
            AnomalyScenario.CONVERSION_REGRESSION -> 0.10
            AnomalyScenario.AOV_DECLINE -> 0.50
            AnomalyScenario.CHECKOUT_REGRESSION -> 0.50
            AnomalyScenario.GLOBAL_ANOMALY -> 0.10
        }

        if (random.nextDouble() > beginCheckoutProb) {
            // Cart abandoned
            return
        }

        tracker.logBeginCheckout(cartItems, cartSubtotal)
        delay(stepDelay)

        tracker.logAddShippingInfo(cartItems, cartSubtotal)
        delay(stepDelay)

        val paymentType = listOf("Credit Card", "Google Pay", "PayPal").random(random)
        tracker.logAddPaymentInfo(cartItems, cartSubtotal, paymentType)
        delay(stepDelay)

        // Step 5: Final Purchase probability
        val purchaseProb = when (scenario) {
            AnomalyScenario.NORMAL -> 0.30
            AnomalyScenario.TRAFFIC_DECLINE -> 0.30
            AnomalyScenario.CONVERSION_REGRESSION -> 0.30
            AnomalyScenario.AOV_DECLINE -> 0.40
            AnomalyScenario.CHECKOUT_REGRESSION -> 0.04
            AnomalyScenario.GLOBAL_ANOMALY -> 0.10
        }

        if (random.nextDouble() > purchaseProb) {
            // Checkout failed / aborted
            return
        }

        val discount = if (scenario == AnomalyScenario.AOV_DECLINE) cartSubtotal * 0.50 else 0.0
        val couponCode = if (scenario == AnomalyScenario.AOV_DECLINE) "HALFPRICE" else null
        val finalTotal = (cartSubtotal - discount).coerceAtLeast(0.0)

        val randomId = (1000..9999).random(random)
        val order = Order(
            orderId = "SIM-$randomId",
            items = cartItems,
            totalAmount = finalTotal,
            discountAmount = discount,
            timestamp = System.currentTimeMillis(),
            couponCode = couponCode
        )
        tracker.logPurchase(order)
    }
}
