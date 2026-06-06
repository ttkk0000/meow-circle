package com.ttkk0000.meowcircle.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.Listing
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.desktop.LocalDesktopStitchColors
import com.ttkk0000.meowcircle.desktop.MndApiClient
import com.ttkk0000.meowcircle.desktop.MndDesktopShape
import kotlinx.coroutines.launch

@Composable
fun MarketScreen(
    sdk: MeowCircleSdk,
    apiClient: MndApiClient,
    onNavigateToPublish: () -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf("") } // empty means "All"
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    fun refreshListings() {
        scope.launch {
            loading = true
            errorMsg = null
            sdk.listings()
                .onSuccess { listings = it }
                .onFailure { errorMsg = it.message ?: "Failed to load listings" }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshListings()
    }

    val filteredListings = if (selectedType.isBlank()) {
        listings
    } else {
        listings.filter { it.type.equals(selectedType, ignoreCase = true) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Market Header
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
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 16.dp)
                    ) {
                        Text(
                            text = "M&D Market",
                            color = colors.onSurface,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Social commerce marketplace, not cart-first ecommerce.",
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = onNavigateToPublish,
                        shape = MndDesktopShape.pill,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EC4A6), contentColor = Color.White)
                    ) {
                        Text("Publish Listing", fontWeight = FontWeight.Bold)
                    }
                }

                // Filter Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarketFilterTab("All", selectedType == "") { selectedType = "" }
                    MarketFilterTab("Products", selectedType == "product") { selectedType = "product" }
                    MarketFilterTab("Services", selectedType == "service") { selectedType = "service" }
                    MarketFilterTab("Adoption", selectedType == "adopt") { selectedType = "adopt" }
                }
            }
        }

        if (successMsg != null) {
            Snackbar(
                action = { TextButton(onClick = { successMsg = null }) { Text("Dismiss", color = colors.brand) } },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(successMsg.orEmpty())
            }
        }

        // Listings Grid
        if (loading && listings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2EC4A6))
            }
        } else if (errorMsg != null && listings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMsg.orEmpty(), color = colors.error, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { refreshListings() }, shape = MndDesktopShape.pill) {
                        Text("Retry")
                    }
                }
            }
        } else if (filteredListings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No items listed in this category.", color = colors.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 240.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredListings) { item ->
                    Card(
                        shape = MndDesktopShape.card,
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(MndDesktopShape.card)
                                    .background(colors.surfaceLow),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = when (item.type) {
                                            "adopt" -> "🐱"
                                            "service" -> "✂️"
                                            else -> "🎁"
                                        },
                                        fontSize = 36.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (item.type == "adopt") "Adoption Companion" else "M&D Listing Item",
                                        color = colors.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Type Badge at top-left
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clip(MndDesktopShape.pill)
                                        .background(
                                            when (item.type) {
                                                "adopt" -> Color(0xFFFFD166) // Gold for Adoption
                                                "service" -> Color(0xFF8B5CF6) // Purple for Services
                                                else -> Color(0xFF2EC4A6) // Mint/Green for Products
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.type.uppercase(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    color = colors.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$${String.format("%.2f", item.priceCents / 100.0)}",
                                    color = colors.brand,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(
                                text = item.description,
                                color = colors.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 2
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        apiClient.createOrder(item.id)
                                            .onSuccess {
                                                successMsg = "Order #${it.id} created! Go to Orders to complete payment."
                                            }
                                            .onFailure {
                                                errorMsg = it.message ?: "Failed to purchase"
                                            }
                                    }
                                },
                                shape = MndDesktopShape.pill,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EC4A6), contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Text("Buy Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketFilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    Box(
        modifier = Modifier
            .clip(MndDesktopShape.pill)
            .background(if (selected) Color(0xFFE7FAF5) else Color.Transparent)
            .border(1.dp, if (selected) Color(0xFF2EC4A6) else colors.outlineVariant, MndDesktopShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF2EC4A6) else colors.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
