package com.ttkk0000.meowcircle.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.desktop.LocalDesktopStitchColors
import com.ttkk0000.meowcircle.desktop.MndApiClient
import com.ttkk0000.meowcircle.desktop.MndDesktopShape
import kotlinx.coroutines.launch

@Composable
fun PublishScreen(
    sdk: MeowCircleSdk,
    apiClient: MndApiClient,
    onPublishSuccess: () -> Unit,
) {
    val colors = LocalDesktopStitchColors.current
    val scope = rememberCoroutineScope()

    var postCategory by remember { mutableStateOf("daily_share") }
    var postContent by remember { mutableStateOf("") }
    var postLoading by remember { mutableStateOf(false) }

    var productType by remember { mutableStateOf("product") }
    var productTitle by remember { mutableStateOf("") }
    var productDesc by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productLoading by remember { mutableStateOf(false) }

    var statusMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            shape = MndDesktopShape.panel,
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Publish Workspace", fontSize = 30.sp, fontWeight = FontWeight.Black, color = colors.onSurface)
                Text("Create a new social post or publish a marketplace listing.", fontSize = 14.sp, color = colors.onSurfaceVariant)
            }
        }

        if (statusMsg != null) {
            Snackbar(
                action = { TextButton(onClick = { statusMsg = null }) { Text("Dismiss") } }
            ) {
                Text(statusMsg.orEmpty())
            }
        }
        if (errorMsg != null) {
            Text(errorMsg.orEmpty(), color = colors.error, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Post Composer Card
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = MndDesktopShape.panel,
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Post Composer Entry", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.onSurface)

                    Text("Category", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterTab("Daily", postCategory == "daily_share") { postCategory = "daily_share" }
                        FilterTab("Help", postCategory == "help") { postCategory = "help" }
                        FilterTab("Activity", postCategory == "activity") { postCategory = "activity" }
                    }

                    OutlinedTextField(
                        value = postContent,
                        onValueChange = { postContent = it },
                        label = { Text("What's on your mind?") },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = MndDesktopShape.field
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                postLoading = true
                                errorMsg = null
                                sdk.createPost(postContent.take(60), postContent, postCategory)
                                    .onSuccess {
                                        postContent = ""
                                        statusMsg = "Post published successfully!"
                                        onPublishSuccess()
                                    }
                                    .onFailure { errorMsg = it.message ?: "Failed to post" }
                                postLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MndDesktopShape.pill,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.brand, contentColor = Color.White),
                        enabled = !postLoading && postContent.isNotBlank()
                    ) {
                        Text("Publish Post", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Listing Publisher Card (Mint Theme Styled)
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = MndDesktopShape.panel,
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, Color(0xFFCDEFE6))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Product Publish Entry", fontSize = 18.sp, fontWeight = FontWeight.Black, color = colors.onSurface)

                    Text("Listing Type", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MarketFilterTab("Sell", productType == "product") { productType = "product" }
                        MarketFilterTab("Service", productType == "service") { productType = "service" }
                        MarketFilterTab("Adoption", productType == "adopt") { productType = "adopt" }
                    }

                    OutlinedTextField(
                        value = productTitle,
                        onValueChange = { productTitle = it },
                        label = { Text("Listing Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MndDesktopShape.field
                    )

                    OutlinedTextField(
                        value = productDesc,
                        onValueChange = { productDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        shape = MndDesktopShape.field
                    )

                    OutlinedTextField(
                        value = productPrice,
                        onValueChange = { productPrice = it },
                        label = { Text("Price (USD)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MndDesktopShape.field
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                productLoading = true
                                errorMsg = null
                                val parsedPrice = (productPrice.toDoubleOrNull() ?: 0.0) * 100.0
                                apiClient.publishListing(productType, productTitle, productDesc, parsedPrice.toLong())
                                    .onSuccess {
                                        productTitle = ""
                                        productDesc = ""
                                        productPrice = ""
                                        statusMsg = "Market listing published!"
                                        onPublishSuccess()
                                    }
                                    .onFailure { errorMsg = it.message ?: "Failed to publish listing" }
                                productLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MndDesktopShape.pill,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EC4A6), contentColor = Color.White),
                        enabled = !productLoading && productTitle.isNotBlank()
                    ) {
                        Text("Publish Listing", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
