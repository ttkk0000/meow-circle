package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttkk0000.meowcircle.Listing
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchMarketScreen(
    sdk: MeowCircleSdk,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf("") } // empty means "All"
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var mockMode by remember { mutableStateOf(false) }
    var showPublishDialog by remember { mutableStateOf(false) }

    fun createMockListings(): List<Listing> {
        return listOf(
            Listing(
                id = 1L,
                sellerId = 2L,
                type = "product",
                title = "未拆封猫罐头 6 罐组合",
                description = "猫猫优先，适合换粮过渡；同城可自提。",
                priceCents = 6800L,
                currency = "CNY",
                createdAt = "2026-06-02T10:00:00Z",
            ),
            Listing(
                id = 2L,
                sellerId = 5L,
                type = "service",
                title = "周末上门喂猫与铲砂",
                description = "有基础照护记录，可同时帮 doggie 换水。",
                priceCents = 12000L,
                currency = "CNY",
                createdAt = "2026-06-01T12:00:00Z",
            ),
            Listing(
                id = 3L,
                sellerId = 8L,
                type = "adopt",
                title = "三个月橘猫找稳定家庭",
                description = "已驱虫，性格亲人，需要领养回访。",
                priceCents = 0L,
                currency = "CNY",
                createdAt = "2026-05-31T18:30:00Z",
            ),
        )
    }

    fun refreshListings() {
        if (mockMode) {
            listings = createMockListings()
            errorMsg = null
            loading = false
            return
        }
        scope.launch {
            loading = true
            errorMsg = null
            sdk.listings()
                .onSuccess { listings = it }
                .onFailure { errorMsg = it.message ?: "无法加载市集商品" }
            loading = false
        }
    }

    LaunchedEffect(mockMode) {
        refreshListings()
    }

    val filteredListings = if (selectedType.isBlank()) {
        listings
    } else {
        listings.filter { it.type.equals(selectedType, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Market Header Card
        Card(
            shape = StitchShape.card,
            colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
            border = BorderStroke(1.dp, StitchPalette.BorderHairline)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "M&D 市集",
                            color = StitchPalette.OnSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "猫犬好物交易与领养服务",
                            color = StitchPalette.OnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = { mockMode = !mockMode },
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = if (mockMode) StitchPalette.BrandMuted else StitchPalette.SurfaceLow,
                                contentColor = StitchPalette.Brand
                            ),
                            shape = StitchShape.pill,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (mockMode) "演示中" else "离线演示", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showPublishDialog = true },
                            shape = StitchShape.pill,
                            colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("发布好物", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarketFilterTab("全部", selectedType == "") { selectedType = "" }
                    MarketFilterTab("商品", selectedType == "product") { selectedType = "product" }
                    MarketFilterTab("服务", selectedType == "service") { selectedType = "service" }
                    MarketFilterTab("领养", selectedType == "adopt") { selectedType = "adopt" }
                }
            }
        }

        if (successMsg != null) {
            Snackbar(
                action = { TextButton(onClick = { successMsg = null }) { Text("确认", color = StitchPalette.Brand) } },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(successMsg.orEmpty())
            }
        }

        // Listings Grid
        if (loading && listings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StitchPalette.Brand)
            }
        } else if (errorMsg != null && listings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMsg.orEmpty(), color = StitchPalette.Error, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { refreshListings() }, shape = StitchShape.pill) {
                        Text("重试")
                    }
                }
            }
        } else if (filteredListings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("该分类下暂时没有好物发布", color = StitchPalette.OnSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredListings, key = { it.id }) { item ->
                    Card(
                        shape = StitchShape.card,
                        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                        border = BorderStroke(1.dp, StitchPalette.BorderHairline)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(StitchShape.card)
                                    .background(StitchPalette.SurfaceLow),
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
                                        fontSize = 28.sp
                                    )
                                }

                                // Type Badge at top-left
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .clip(StitchShape.pill)
                                        .background(
                                            when (item.type) {
                                                "adopt" -> Color(0xFFFFD166) // Yellow for Adoption
                                                "service" -> Color(0xFF8B5CF6) // Purple for Services
                                                else -> StitchPalette.Brand // Orange/Brand for Products
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (item.type) {
                                            "adopt" -> "领养"
                                            "service" -> "服务"
                                            else -> "商品"
                                        },
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = item.title,
                                color = StitchPalette.OnSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatPrice(item.priceCents, item.currency),
                                    color = StitchPalette.Brand,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = item.description,
                                color = StitchPalette.OnSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 2,
                                minLines = 2
                            )

                            Button(
                                onClick = {
                                    if (mockMode) {
                                        successMsg = "已创建订单！请前往「订单」Tab 进行支付。"
                                        return@Button
                                    }
                                    scope.launch {
                                        sdk.createOrder(item.id, "来自移动端的交易订单")
                                            .onSuccess {
                                                successMsg = "订单已发起 (ID: ${it.id})！请在「订单」Tab中查看。"
                                            }
                                            .onFailure {
                                                errorMsg = it.message ?: "购买失败，请重试"
                                            }
                                    }
                                },
                                shape = StitchShape.pill,
                                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = PaddingValues(vertical = 0.dp)
                            ) {
                                Text("立即购买", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPublishDialog) {
        PublishListingDialog(
            onDismiss = { showPublishDialog = false },
            onPublish = { type, title, description, price ->
                if (mockMode) {
                    val mockId = (listings.maxOfOrNull { it.id } ?: 0L) + 1L
                    val newMock = Listing(
                        id = mockId,
                        sellerId = 1L,
                        type = type,
                        title = title,
                        description = description,
                        priceCents = (price * 100).toLong(),
                        currency = "CNY",
                        createdAt = "2026-06-06T12:00:00Z"
                    )
                    listings = listOf(newMock) + listings
                    successMsg = "商品发布成功（模拟）"
                    showPublishDialog = false
                    return@PublishListingDialog
                }
                scope.launch {
                    val priceCents = (price * 100).toLong()
                    sdk.createListing(type, title, description, priceCents, "CNY")
                        .onSuccess {
                            successMsg = "商品发布成功！"
                            refreshListings()
                            showPublishDialog = false
                        }
                        .onFailure {
                            errorMsg = it.message ?: "商品发布失败"
                        }
                }
            }
        )
    }
}

@Composable
fun MarketFilterTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(StitchShape.pill)
            .background(if (selected) StitchPalette.BrandMuted else Color.Transparent)
            .border(1.dp, if (selected) StitchPalette.Brand else StitchPalette.BorderHairline, StitchShape.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun PublishListingDialog(
    onDismiss: () -> Unit,
    onPublish: (type: String, title: String, description: String, price: Double) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("product") } // product, service, adopt
    var priceStr by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发布新好物", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("详情描述") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("好物类别", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { type = "product" }
                        ) {
                            RadioButton(selected = type == "product", onClick = { type = "product" })
                            Text("商品", fontSize = 13.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { type = "service" }
                        ) {
                            RadioButton(selected = type == "service", onClick = { type = "service" })
                            Text("服务", fontSize = 13.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { type = "adopt" }
                        ) {
                            RadioButton(selected = type == "adopt", onClick = { type = "adopt" })
                            Text("领养", fontSize = 13.sp)
                        }
                    }
                }

                if (type != "adopt") {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("价格 (元)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorText != null) {
                    Text(errorText!!, color = StitchPalette.Error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorText = "标题不能为空"
                        return@Button
                    }
                    val price = if (type == "adopt") 0.0 else priceStr.toDoubleOrNull() ?: -1.0
                    if (type != "adopt" && price < 0) {
                        errorText = "请输入合理的价格"
                        return@Button
                    }
                    onPublish(type, title, description, price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand)
            ) {
                Text("发布", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = StitchPalette.OnSurfaceVariant)
            }
        }
    )
}

private fun formatPrice(cents: Long, currency: String): String {
    if (cents <= 0) return "免费"
    val symbol = if (currency.uppercase() == "CNY") "¥" else currency
    return "$symbol${cents / 100.0}"
}
