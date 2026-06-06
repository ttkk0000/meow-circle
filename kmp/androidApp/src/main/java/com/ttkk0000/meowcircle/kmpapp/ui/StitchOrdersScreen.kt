package com.ttkk0000.meowcircle.kmpapp.ui

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
import com.ttkk0000.meowcircle.Order
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import kotlinx.coroutines.launch

@Composable
fun StitchOrdersScreen(
    sdk: MeowCircleSdk,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isBuyerRole by remember { mutableStateOf(true) } // true for Buyer, false for Seller
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var mockMode by remember { mutableStateOf(false) }

    fun createMockOrders(): List<Order> {
        val currentUserId = sdk.cachedUser()?.id ?: 1L
        return listOf(
            Order(
                id = 101L,
                buyerId = currentUserId,
                sellerId = 5L,
                listingId = 1L,
                listingTitle = "未拆封猫罐头 6 罐组合",
                amountCents = 6800L,
                currency = "CNY",
                status = "pending_payment",
                createdAt = "2026-06-02T10:00:00Z",
                updatedAt = "2026-06-02T10:00:00Z"
            ),
            Order(
                id = 102L,
                buyerId = currentUserId,
                sellerId = 8L,
                listingId = 2L,
                listingTitle = "周末上门喂猫与铲砂",
                amountCents = 12000L,
                currency = "CNY",
                status = "shipped",
                createdAt = "2026-06-01T12:00:00Z",
                updatedAt = "2026-06-01T12:00:00Z"
            ),
            Order(
                id = 103L,
                buyerId = 4L,
                sellerId = currentUserId,
                listingId = 3L,
                listingTitle = "三个月橘猫领养专用笼/合同",
                amountCents = 2000L,
                currency = "CNY",
                status = "paid",
                createdAt = "2026-05-31T18:30:00Z",
                updatedAt = "2026-05-31T18:30:00Z"
            )
        )
    }

    fun refreshOrders() {
        if (mockMode) {
            val allMocks = createMockOrders()
            val currentUserId = sdk.cachedUser()?.id ?: 1L
            orders = if (isBuyerRole) {
                allMocks.filter { it.buyerId == currentUserId }
            } else {
                allMocks.filter { it.sellerId == currentUserId }
            }
            errorMsg = null
            loading = false
            return
        }
        scope.launch {
            loading = true
            errorMsg = null
            val role = if (isBuyerRole) "buyer" else "seller"
            sdk.fetchOrders(role)
                .onSuccess { orders = it }
                .onFailure { errorMsg = it.message ?: "无法加载订单列表" }
            loading = false
        }
    }

    LaunchedEffect(isBuyerRole, mockMode) {
        refreshOrders()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Orders Header Card
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
                            text = "订单工作台",
                            color = StitchPalette.OnSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "买家与卖家交易状态跟踪",
                            color = StitchPalette.OnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

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
                }

                // Role Toggles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(StitchShape.pill)
                        .background(StitchPalette.SurfaceLow)
                        .border(1.dp, StitchPalette.BorderHairline, StitchShape.pill)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(StitchShape.pill)
                            .background(if (isBuyerRole) StitchPalette.Brand else Color.Transparent)
                            .clickable { isBuyerRole = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "买家模式",
                            color = if (isBuyerRole) Color.White else StitchPalette.OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(StitchShape.pill)
                            .background(if (!isBuyerRole) StitchPalette.Brand else Color.Transparent)
                            .clickable { isBuyerRole = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "卖家模式",
                            color = if (!isBuyerRole) Color.White else StitchPalette.OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (successMsg != null) {
            Snackbar(
                action = { TextButton(onClick = { successMsg = null }) { Text("确认", color = StitchPalette.Brand) } }
            ) {
                Text(successMsg.orEmpty())
            }
        }

        // Orders List
        if (loading && orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StitchPalette.Brand)
            }
        } else if (errorMsg != null && orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMsg.orEmpty(), color = StitchPalette.Error, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { refreshOrders() }, shape = StitchShape.pill) {
                        Text("重试")
                    }
                }
            }
        } else if (orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无订单记录", color = StitchPalette.OnSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(orders, key = { it.id }) { o ->
                    Card(
                        shape = StitchShape.card,
                        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                        border = BorderStroke(1.dp, StitchPalette.BorderHairline)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = o.listingTitle.ifBlank { "订单 #${o.id}" },
                                    color = StitchPalette.OnSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                val statusColor = when (o.status) {
                                    "completed" -> Color(0xFF10B981) // Green
                                    "shipped" -> Color(0xFF3B82F6) // Blue
                                    "paid" -> Color(0xFF3B82F6) // Blue
                                    "pending_payment" -> Color(0xFFF59E0B) // Amber
                                    "cancelled" -> Color(0xFF6B7280) // Gray
                                    "refunded" -> Color(0xFFEF4444) // Red
                                    else -> StitchPalette.OnSurfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(StitchShape.pill)
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = when (o.status) {
                                            "completed" -> "已完成"
                                            "shipped" -> "已发货"
                                            "paid" -> "已支付"
                                            "pending_payment" -> "待支付"
                                            "cancelled" -> "已取消"
                                            "refunded" -> "已退款"
                                            else -> o.status
                                        },
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "交易金额: ¥${o.amountCents / 100.0}   ·   订单编号: ${o.id}",
                                color = StitchPalette.OnSurfaceVariant,
                                fontSize = 12.sp
                            )

                            // Actions based on state + role
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isBuyerRole) {
                                    if (o.status == "pending_payment") {
                                        OrderButton("立即支付", StitchPalette.Brand) {
                                            if (mockMode) {
                                                orders = orders.map { if (it.id == o.id) it.copy(status = "paid") else it }
                                                successMsg = "支付成功（模拟）！"
                                                return@OrderButton
                                            }
                                            scope.launch {
                                                sdk.payOrder(o.id, "mock")
                                                    .onSuccess {
                                                        successMsg = "订单支付成功！"
                                                        refreshOrders()
                                                    }
                                                    .onFailure {
                                                        errorMsg = it.message ?: "支付失败"
                                                    }
                                            }
                                        }
                                        OrderButton("取消订单", StitchPalette.OnSurfaceVariant) {
                                            if (mockMode) {
                                                orders = orders.map { if (it.id == o.id) it.copy(status = "cancelled") else it }
                                                successMsg = "订单已取消（模拟）"
                                                return@OrderButton
                                            }
                                            scope.launch {
                                                sdk.cancelOrder(o.id)
                                                    .onSuccess {
                                                        successMsg = "订单取消成功！"
                                                        refreshOrders()
                                                    }
                                            }
                                        }
                                    } else if (o.status == "shipped") {
                                        OrderButton("确认收货", Color(0xFF10B981)) {
                                            if (mockMode) {
                                                orders = orders.map { if (it.id == o.id) it.copy(status = "completed") else it }
                                                successMsg = "已确认收货（模拟），交易已完成！"
                                                return@OrderButton
                                            }
                                            scope.launch {
                                                sdk.completeOrder(o.id)
                                                    .onSuccess {
                                                        successMsg = "确认收货成功！交易完成。"
                                                        refreshOrders()
                                                    }
                                            }
                                        }
                                    }
                                } else {
                                    // Seller Role Actions
                                    if (o.status == "paid") {
                                        OrderButton("确认发货", Color(0xFF3B82F6)) {
                                            if (mockMode) {
                                                orders = orders.map { if (it.id == o.id) it.copy(status = "shipped") else it }
                                                successMsg = "商品已发货（模拟）"
                                                return@OrderButton
                                            }
                                            scope.launch {
                                                sdk.shipOrder(o.id)
                                                    .onSuccess {
                                                        successMsg = "商品发货成功！"
                                                        refreshOrders()
                                                    }
                                            }
                                        }
                                        OrderButton("退款给买家", StitchPalette.Error) {
                                            if (mockMode) {
                                                orders = orders.map { if (it.id == o.id) it.copy(status = "refunded") else it }
                                                successMsg = "已退款给买家（模拟）"
                                                return@OrderButton
                                            }
                                            scope.launch {
                                                sdk.refundOrder(o.id)
                                                    .onSuccess {
                                                        successMsg = "已退款，订单交易关闭。"
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
fun OrderButton(
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = StitchShape.pill,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
