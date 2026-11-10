package com.cex.insightflowstore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cex.insightflowstore.analytics.AnalyticsTracker
import com.cex.insightflowstore.data.CartManager
import com.cex.insightflowstore.model.Order
import com.cex.insightflowstore.simulator.TrafficSimulationEngine
import com.cex.insightflowstore.ui.screens.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightStoreApp(
    cartManager: CartManager,
    tracker: AnalyticsTracker,
    simulationEngine: TrafficSimulationEngine
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSimulationSheetOpen by remember { mutableStateOf(false) }

    val cartItemCount = cartManager.itemCount
    var placedOrder by remember { mutableStateOf<Order?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "InsightFlow Store",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Open AI Telemetry Simulator
                    IconButton(onClick = { isSimulationSheetOpen = true }) {
                        BadgedBox(
                            badge = {
                                Badge { Text("AI") }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "AI Simulator",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Cart with Badge
                    IconButton(onClick = { navController.navigate("cart") }) {
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) {
                                    Badge { Text("$cartItemCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = "catalog"
            ) {
                composable("catalog") {
                    CatalogScreen(
                        cartManager = cartManager,
                        tracker = tracker,
                        onProductClick = { productId ->
                            navController.navigate("product/$productId")
                        }
                    )
                }

                composable(
                    route = "product/{productId}",
                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId") ?: ""
                    ProductDetailScreen(
                        productId = productId,
                        cartManager = cartManager,
                        tracker = tracker,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("cart") {
                    CartScreen(
                        cartManager = cartManager,
                        tracker = tracker,
                        onProceedToCheckout = {
                            navController.navigate("checkout")
                        }
                    )
                }

                composable("checkout") {
                    CheckoutScreen(
                        cartManager = cartManager,
                        tracker = tracker,
                        onBack = { navController.popBackStack() },
                        onOrderPlaced = { order ->
                            placedOrder = order
                            navController.navigate("order_success") {
                                popUpTo("catalog")
                            }
                        }
                    )
                }

                composable("order_success") {
                    val currentOrder = placedOrder
                    if (currentOrder != null) {
                        OrderSuccessScreen(
                            order = currentOrder,
                            onContinueShopping = {
                                navController.navigate("catalog") {
                                    popUpTo("catalog") { inclusive = true }
                                }
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate("catalog")
                        }
                    }
                }
            }
        }

        // Bottom Sheet for the Simulator
        if (isSimulationSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isSimulationSheetOpen = false },
                sheetState = sheetState
            ) {
                SimulationDrawer(
                    simulationEngine = simulationEngine,
                    tracker = tracker,
                    onClose = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            isSimulationSheetOpen = false
                        }
                    }
                )
            }
        }
    }
}
