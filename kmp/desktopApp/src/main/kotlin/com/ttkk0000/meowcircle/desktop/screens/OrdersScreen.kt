package com.ttkk0000.meowcircle.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.desktop.LocalDesktopStitchColors
import com.ttkk0000.meowcircle.desktop.MndApiClient
import com.ttkk0000.meowcircle.desktop.MndDesktopShape
import com.ttkk0000.meowcircle.desktop.Order
import kotlinx.coroutines.launch

@Composable
fun OrdersScreen(
    sdk: MeowCircleSdk,
    apiClient: MndApiClient,
) {
    val colors = LocalDesktopStitchColors.current
    val scope = rememberCoroutineScope()

    var isBuyerRole by remember { mutableStateOf(true) } // true for Buyer, false for Seller
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    fun refreshOrders() {
        scope.launch {
            loading = true
            errorMsg = null
            val role = if (isBuyerRole) "buyer" else "seller"
            apiClient.fetchOrders(role)
                .onSuccess { orders = it }
                .onFailure { errorMsg = it.message ?: "Failed to load orders" }
            loading = false
        }
    }

    LaunchedEffect(isBuyerRole) {
        refreshOrders()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Orders Header
        Card(
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Orders Workspace",
                            color = colors.onSurface,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Manage your buyer and seller order statuses per official Stitch rules.",
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }

                    // Role Toggles
                    Row(
                        modifier = Modifier
                            .clip(MndDesktopShape.pill)
                            .background(colors.surfaceLow)
                            .border(1.dp, colors.outlineVariant, MndDesktopShape.pill)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(MndDesktopShape.pill)
                                .background(if (isBuyerRole) colors.brand else Color.Transparent)
                                .clickable { isBuyerRole = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Buyer Mode",
                                color = if (isBuyerRole) Color.White else colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(MndDesktopShape.pill)
                                .background(if (!isBuyerRole) colors.brand else Color.Transparent)
                                .clickable { isBuyerRole = false }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Seller Mode",
                                color = if (!isBuyerRole) Color.White else colors.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        if (statusMsg != null) {
            Snackbar(
                action = { TextButton(onClick = { statusMsg = null }) { Text("Dismiss", color = colors.brand) } }
            ) {
                Text(statusMsg.orEmpty())
            }
        }

        // Orders List
        if (loading && orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.brand)
            }
        } else if (errorMsg != null && orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMsg.orEmpty(), color = colors.error, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { refreshOrders() }, shape = MndDesktopShape.pill) {
                        Text("Retry")
                    }
                }
            }
        } else if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No orders placed yet.", color = colors.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { o ->
                    Card(
                        shape = MndDesktopShape.card,
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = o.listingTitle.ifBlank { "Order #${o.id}" },
                                    color = colors.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                val statusColor = when (o.status) {
                                    "completed" -> Color(0xFF10B981)
                                    "shipped" -> Color(0xFF3B82F6)
                                    "pending_payment" -> Color(0xFFF59E0B)
                                    else -> colors.onSurfaceVariant
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(MndDesktopShape.pill)
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = o.status.uppercase(),
                                        color = statusColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Text(
                                text = "Amount: $${String.format("%.2f", o.amountCents / 100.0)} ${o.currency}  ·  ID: ${o.id}",
                                color = colors.onSurfaceVariant,
                                fontSize = 13.sp
                            )

                            // Actions based on state + role
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isBuyerRole) {
                                    if (o.status == "pending_payment") {
                                        OrderActionButton("Pay Now", colors.brand) {
                                            scope.launch {
                                                apiClient.updateOrderStatus(o.id, "pay")
                                                    .onSuccess {
                                                        statusMsg = "Order Paid!"
                                                        refreshOrders()
                                                    }
                                            }
                                        }
                                        OrderActionButton("Cancel Order", colors.onSurfaceVariant) {
                                            scope.launch {
                                                apiClient.updateOrderStatus(o.id, "cancel")
                                                    .onSuccess {
                                                        statusMsg = "Order Cancelled."
                                                        refreshOrders()
                                                    }
                                            }
                                        }
                                    } else if (o.status == "shipped") {
                                        OrderActionButton("Confirm Receipt", Color(0xFF10B981)) {
                                            scope.launch {
                                                apiClient.updateOrderStatus(o.id, "complete")
                                                    .onSuccess {
                                                        statusMsg = "Order Completed!"
                                                        refreshOrders()
                                                    }
                                            }
                                        }
                                    }
                                } else {
                                    // Seller Role
                                    if (o.status == "paid") {
                                        OrderActionButton("Ship Item", Color(0xFF3B82F6)) {
                                            scope.launch {
                                                apiClient.updateOrderStatus(o.id, "ship")
                                                    .onSuccess {
                                                        statusMsg = "Order Shipped!"
                                                        refreshOrders()
                                                    }
                                            }
                                        }
                                        OrderActionButton("Refund Buyer", colors.error) {
                                            scope.launch {
                                                apiClient.updateOrderStatus(o.id, "refund")
                                                    .onSuccess {
                                                        statusMsg = "Order Refunded."
                                                        refreshOrders()
                                                    }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderActionButton(
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = MndDesktopShape.pill,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
