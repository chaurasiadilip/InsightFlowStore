package com.cex.insightflowstore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cex.insightflowstore.analytics.AnalyticsTracker
import com.cex.insightflowstore.data.CartManager
import com.cex.insightflowstore.model.CartItem
import com.cex.insightflowstore.ui.theme.DangerRed
import com.cex.insightflowstore.ui.theme.SuccessGreen
import com.cex.insightflowstore.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun CartScreen(
    cartManager: CartManager,
    tracker: AnalyticsTracker,
    onProceedToCheckout: () -> Unit
) {
    val items by cartManager.items.collectAsState()
    val discountPercent by cartManager.discountPercent.collectAsState()
    var couponText by remember { mutableStateOf("") }
    var couponMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            tracker.logViewCart(items, cartManager.total)
        }
    }

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TextSecondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your Cart is Empty",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explore the catalog and add products to start generating shopping telemetry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(items, key = { it.product.id }) { item ->
                CartItemRow(
                    item = item,
                    onIncrease = { cartManager.addToCart(item.product, 1) },
                    onDecrease = { cartManager.removeFromCart(item.product, 1) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Promo Code Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Promo Code (Try SAVE10, SAVE25, HALFPRICE)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = couponText,
                                onValueChange = { couponText = it },
                                placeholder = { Text("Enter code") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val success = cartManager.applyCoupon(couponText)
                                    couponMessage = if (success) "Coupon applied!" else "Invalid coupon code"
                                }
                            ) {
                                Text("Apply")
                            }
                        }
                        if (couponMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = couponMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (discountPercent > 0) SuccessGreen else DangerRed
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Order Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Order Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SummaryRow("Subtotal", "$${String.format(Locale.US, "%.2f", cartManager.subtotal)}")
                        if (discountPercent > 0) {
                            SummaryRow(
                                "Discount (${discountPercent.toInt()}%)",
                                "-$${String.format(Locale.US, "%.2f", cartManager.discountAmount)}",
                                color = SuccessGreen
                            )
                        }
                        SummaryRow("Estimated Shipping", "FREE")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SummaryRow(
                            "Total",
                            "$${String.format(Locale.US, "%.2f", cartManager.total)}",
                            isBold = true
                        )
                    }
                }
            }
        }

        // Checkout Button Bottom Bar
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        tracker.logBeginCheckout(items, cartManager.total)
                        onProceedToCheckout()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Proceed to Checkout • $${String.format(Locale.US, "%.2f", cartManager.total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${String.format(Locale.US, "%.2f", item.product.price)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (item.quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}
