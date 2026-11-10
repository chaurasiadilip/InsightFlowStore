package com.cex.insightflowstore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.cex.insightflowstore.analytics.AppAnalyticsTracker
import com.cex.insightflowstore.data.CartManager
import com.cex.insightflowstore.simulator.SimulationManifestManager
import com.cex.insightflowstore.simulator.TrafficSimulationEngine
import com.cex.insightflowstore.ui.InsightStoreApp
import com.cex.insightflowstore.ui.theme.InsightFlowStoreTheme

class MainActivity : ComponentActivity() {

    private lateinit var tracker: AppAnalyticsTracker
    private lateinit var cartManager: CartManager
    private lateinit var manifestManager: SimulationManifestManager
    private lateinit var simulationEngine: TrafficSimulationEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tracker = AppAnalyticsTracker(applicationContext)
        cartManager = CartManager(tracker)
        manifestManager = SimulationManifestManager(applicationContext)
        simulationEngine = TrafficSimulationEngine(
            tracker = tracker,
            manifestManager = manifestManager,
            scope = lifecycleScope
        )

        setContent {
            InsightFlowStoreTheme {
                InsightStoreApp(
                    cartManager = cartManager,
                    tracker = tracker,
                    simulationEngine = simulationEngine
                )
            }
        }
    }
}
