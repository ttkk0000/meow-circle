package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.Order
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import kotlinx.coroutines.launch

private data class OrderFilter(val key: String, val labelRes: Int)

private val ORDER_FILTERS =
    listOf(
        OrderFilter("all", R.string.orders_filter_all),
        OrderFilter("pending_payment", R.string.orders_filter_pending),
        OrderFilter("paid", R.string.orders_filter_paid),
        OrderFilter("shipped", R.string.orders_filter_shipped),
        OrderFilter("completed", R.string.orders_filter_completed),
    )

@Composable
fun StitchOrdersScreen(
    sdk: MeowCircleSdk,
    onOpenMarket: () -> Unit = {},
    onOpenMessages: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var role by remember { mutableStateOf("buyer") }
    var filter by remember { mutableStateOf("all") }
    var orders by remember { mutableStateOf<List<Order>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    var mockMode by remember { mutableStateOf(false) }
    var refreshSignal by remember { mutableStateOf(0) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var showTracking by remember { mutableStateOf(false) }
    var infoText by remember { mutableStateOf<String?>(null) }
    var actionBusy by remember { mutableStateOf(false) }

    fun createMockOrders(): List<Order> =
        listOf(
            Order(
                id = 4824L,
                buyerId = 1L,
                sellerId = 2L,
                listingId = 1L,
                listingTitle = context.getString(R.string.orders_mock_title_collar),
                amountCents = 12800L,
                currency = "CNY",
                status = "shipped",
                createdAt = "2026-06-08T09:12:00Z",
                updatedAt = "2026-06-08T12:00:00Z",
                paidAt = "2026-06-08T09:15:00Z",
                shippedAt = "2026-06-08T12:00:00Z",
            ),
            Order(
                id = 9810L,
                buyerId = 1L,
                sellerId = 3L,
                listingId = 2L,
                listingTitle = context.getString(R.string.orders_mock_title_bowl),
                amountCents = 6800L,
                currency = "CNY",
                status = "paid",
                createdAt = "2026-06-07T10:00:00Z",
                updatedAt = "2026-06-07T10:05:00Z",
                paidAt = "2026-06-07T10:05:00Z",
            ),
            Order(
                id = 832L,
                buyerId = 4L,
                sellerId = 1L,
                listingId = 3L,
                listingTitle = context.getString(R.string.orders_mock_title_harness),
                amountCents = 4500L,
                currency = "CNY",
                status = "paid",
                createdAt = "2026-06-06T11:00:00Z",
                updatedAt = "2026-06-06T11:10:00Z",
                paidAt = "2026-06-06T11:10:00Z",
            ),
        )

    LaunchedEffect(role, refreshSignal, mockMode) {
        if (mockMode) {
            orders = createMockOrders().filter { if (role == "buyer") it.buyerId == 1L else it.sellerId == 1L }
            err = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        err = null
        orders =
            sdk.myOrders(role).fold(
                onSuccess = { it },
                onFailure = { e ->
                    err = (e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl)
                    null
                },
            )
        loading = false
    }

    fun updateOrder(action: String, order: Order) {
        if (mockMode) {
            infoText = context.getString(R.string.orders_demo_action, action)
            refreshSignal++
            return
        }
        actionBusy = true
        scope.launch {
            val result =
                when (action) {
                    "pay" -> sdk.payOrder(order.id)
                    "cancel" -> sdk.cancelOrder(order.id)
                    "ship" -> sdk.shipOrder(order.id)
                    "complete" -> sdk.completeOrder(order.id)
                    "refund" -> sdk.refundOrder(order.id)
                    else -> Result.failure(Exception("Unknown action"))
                }
            result.fold(
                onSuccess = {
                    selectedOrder = it
                    infoText = context.getString(R.string.orders_updated, it.id)
                    refreshSignal++
                },
                onFailure = { e ->
                    infoText = (e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl)
                },
            )
            actionBusy = false
        }
    }

    when {
        showTracking && selectedOrder != null ->
            TrackingScreen(
                order = selectedOrder!!,
                onBack = { showTracking = false },
                modifier = modifier,
            )
        selectedOrder != null ->
            OrderDetailScreen(
                order = selectedOrder!!,
                role = role,
                actionBusy = actionBusy,
                onBack = { selectedOrder = null },
                onTrack = { showTracking = true },
                onOpenMessages = onOpenMessages,
                onAction = { updateOrder(it, selectedOrder!!) },
                modifier = modifier,
            )
        else ->
            OrdersListScreen(
                role = role,
                filter = filter,
                orders = orders.orEmpty().filter { filter == "all" || it.status == filter },
                loading = loading,
                err = err,
                mockMode = mockMode,
                onRoleToggle = {
                    role = if (role == "buyer") "seller" else "buyer"
                    selectedOrder = null
                },
                onFilterChange = { filter = it },
                onOpenOrder = { selectedOrder = it },
                onEnableMock = { mockMode = true },
                onOpenMarket = onOpenMarket,
                onOpenMessages = onOpenMessages,
                modifier = modifier,
            )
    }

    if (infoText != null) {
        AlertDialog(
            onDismissRequest = { infoText = null },
            shape = StitchShape.dialog,
            title = { Text(stringResource(R.string.orders_title)) },
            text = { Text(infoText.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { infoText = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}

@Composable
private fun OrdersListScreen(
    role: String,
    filter: String,
    orders: List<Order>,
    loading: Boolean,
    err: String?,
    mockMode: Boolean,
    onRoleToggle: () -> Unit,
    onFilterChange: (String) -> Unit,
    onOpenOrder: (Order) -> Unit,
    onEnableMock: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(StitchPalette.Surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(44.dp))
            Text(
                stringResource(R.string.orders_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRoleToggle, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.orders_toggle_role), tint = StitchPalette.OnSurface)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = StitchShape.pill,
                color = StitchPalette.Surface,
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                modifier = Modifier.clickable(onClick = onRoleToggle),
            ) {
                Text(
                    if (role == "buyer") stringResource(R.string.orders_role_buying) else stringResource(R.string.orders_role_selling),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = StitchPalette.Brand,
                    fontWeight = FontWeight.Black,
                )
            }
            ORDER_FILTERS.forEach { item ->
                val selected = filter == item.key
                Surface(
                    shape = StitchShape.pill,
                    color = if (selected) StitchPalette.Brand else StitchPalette.Surface,
                    border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                    modifier = Modifier.clickable { onFilterChange(item.key) },
                ) {
                    Text(
                        stringResource(item.labelRes),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) Color.White else StitchPalette.OnSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StitchPalette.Brand)
                }
                err != null && !mockMode -> OrdersEmptyState(
                    title = stringResource(R.string.orders_unavailable_title),
                    body = err,
                    primary = stringResource(R.string.market_view_demo),
                    secondary = null,
                    onPrimary = onEnableMock,
                    onSecondary = null,
                )
                orders.isEmpty() -> OrdersEmptyState(
                    title = stringResource(R.string.orders_empty_title),
                    body = stringResource(R.string.orders_empty_body),
                    primary = stringResource(R.string.orders_explore_items),
                    secondary = stringResource(R.string.orders_view_saved),
                    onPrimary = onOpenMarket,
                    onSecondary = onOpenMessages,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderListCard(order = order, role = role, onClick = { onOpenOrder(order) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailScreen(
    order: Order,
    role: String,
    actionBusy: Boolean,
    onBack: () -> Unit,
    onTrack: () -> Unit,
    onOpenMessages: () -> Unit,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sellerMode = role == "seller"
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Header(title = if (sellerMode) stringResource(R.string.orders_seller_order) else stringResource(R.string.orders_order_number, order.id), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StatusBanner(order, sellerMode)
            OrderProductCard(order, sellerMode)
            if (sellerMode) {
                InfoCard(title = stringResource(R.string.orders_shipping_requirement)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = StitchPalette.Gold)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.orders_awaiting_shipment), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.orders_standard_shipping), style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                        }
                    }
                }
                InfoCard(title = stringResource(R.string.orders_buyer_information)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(StitchPalette.BrandMuted))
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("Alex Johnson", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("@alexj", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                        }
                        IconButton(onClick = onOpenMessages) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = stringResource(R.string.orders_contact_buyer), tint = StitchPalette.Brand)
                        }
                    }
                }
            } else {
                InfoCard(title = stringResource(R.string.orders_shipping_information)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = StitchPalette.Brand)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.orders_status_format, statusLabel(order.status)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.orders_arrival), style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                        }
                    }
                }
                InfoCard(title = stringResource(R.string.orders_payment_summary)) {
                    SummaryRow(stringResource(R.string.orders_item), formatOrderPrice(order.amountCents, order.currency))
                    SummaryRow(stringResource(R.string.orders_shipping_local), "$0.00")
                    HorizontalDivider(color = StitchPalette.BorderHairline, modifier = Modifier.padding(vertical = 8.dp))
                    SummaryRow(stringResource(R.string.orders_total), formatOrderPrice(order.amountCents, order.currency), total = true)
                }
            }
        }
        OrderActionBar(
            order = order,
            sellerMode = sellerMode,
            busy = actionBusy,
            onTrack = onTrack,
            onOpenMessages = onOpenMessages,
            onAction = onAction,
        )
    }
}

@Composable
private fun TrackingScreen(
    order: Order,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Header(title = stringResource(R.string.orders_tracking), onBack = onBack, close = true)
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoCard(title = stringResource(R.string.orders_tracking_number)) {
                Text("940123456", style = MaterialTheme.typography.titleLarge, color = StitchPalette.Brand, fontWeight = FontWeight.Black)
            }
            Card(
                shape = StitchShape.cardFeed,
                colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    TrackingStep(stringResource(R.string.orders_step_placed), order.createdAt.take(16).replace('T', ' '), true)
                    TrackingStep(stringResource(R.string.orders_step_paid), order.paidAt?.take(16)?.replace('T', ' ') ?: stringResource(R.string.orders_awaiting_payment), order.paidAt != null)
                    TrackingStep(stringResource(R.string.orders_step_shipped), stringResource(R.string.orders_in_transit), order.status in listOf("shipped", "completed"))
                    TrackingStep(stringResource(R.string.orders_step_completed), stringResource(R.string.orders_awaiting_delivery), order.status == "completed")
                }
            }
        }
    }
}

@Composable
private fun OrderListCard(
    order: Order,
    role: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${stringResource(R.string.orders_order_number, order.id)} · ${if (role == "buyer") "Jordan M." else "Sarah P."}",
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchPalette.OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(order.status)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(74.dp).clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = StitchPalette.Brand)
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(order.listingTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatOrderPrice(order.amountCents, order.currency), style = MaterialTheme.typography.titleMedium, color = StitchPalette.Brand, fontWeight = FontWeight.Black)
                }
            }
            when (order.status) {
                "shipped" -> ButtonLine(stringResource(R.string.orders_confirm_receipt))
                "paid" -> OutlinedButtonLine(stringResource(R.string.orders_contact_seller))
                "pending_payment" -> ButtonLine(stringResource(R.string.orders_pay_now))
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    onBack: () -> Unit,
    close: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(StitchPalette.Surface).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = if (close) stringResource(R.string.common_close) else stringResource(R.string.common_back), tint = StitchPalette.OnSurface)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(44.dp))
    }
}

@Composable
private fun StatusBanner(order: Order, sellerMode: Boolean) {
    val tint = if (order.status in listOf("paid", "shipped")) Color(0xFF2F7FD5) else StitchPalette.Brand
    Surface(
        shape = StitchShape.field,
        color = tint.copy(alpha = 0.16f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (order.status == "shipped") Icons.Outlined.LocalShipping else Icons.Outlined.ReceiptLong, contentDescription = null, tint = tint)
            Spacer(Modifier.width(10.dp))
            Text(statusLabel(order.status), style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text(if (sellerMode) stringResource(R.string.orders_ship_by) else stringResource(R.string.orders_est_aug), style = MaterialTheme.typography.labelSmall, color = tint.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun OrderProductCard(order: Order, sellerMode: Boolean) {
    Card(
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.common_img), color = StitchPalette.OnSurfaceVariant)
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(stringResource(R.string.orders_order_number, order.id), style = MaterialTheme.typography.labelMedium, color = StitchPalette.OnSurfaceVariant)
                Text(order.listingTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(if (sellerMode) stringResource(R.string.orders_buyer_name) else stringResource(R.string.orders_seller_name), style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
            }
            Text(formatOrderPrice(order.amountCents, order.currency), style = MaterialTheme.typography.titleMedium, color = StitchPalette.Brand, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            content()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, total: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = if (total) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = if (total) StitchPalette.Brand else StitchPalette.OnSurface, fontWeight = if (total) FontWeight.Black else FontWeight.Medium)
    }
}

@Composable
private fun OrderActionBar(
    order: Order,
    sellerMode: Boolean,
    busy: Boolean,
    onTrack: () -> Unit,
    onOpenMessages: () -> Unit,
    onAction: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(StitchPalette.Surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            sellerMode && order.status == "paid" -> {
                Button(onClick = { onAction("ship") }, enabled = !busy, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand)) {
                    Text(stringResource(R.string.orders_ship_order), color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onOpenMessages, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(46.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                    Text(stringResource(R.string.orders_contact_buyer), color = StitchPalette.OnSurface)
                }
            }
            !sellerMode && order.status == "pending_payment" -> {
                Button(onClick = { onAction("pay") }, enabled = !busy, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand)) {
                    Text(stringResource(R.string.orders_pay_now), color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { onAction("cancel") }, enabled = !busy, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(46.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                    Text(stringResource(R.string.orders_cancel_order), color = StitchPalette.OnSurface)
                }
            }
            !sellerMode && order.status == "shipped" -> {
                Button(onClick = { onAction("complete") }, enabled = !busy, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand)) {
                    Text(stringResource(R.string.orders_confirm_receipt), color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onOpenMessages, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(46.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                    Text(stringResource(R.string.orders_contact_seller), color = StitchPalette.OnSurface)
                }
                TextButton(onClick = onTrack) { Text(stringResource(R.string.orders_view_tracking), color = StitchPalette.Brand) }
            }
            else -> {
                OutlinedButton(onClick = onOpenMessages, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(46.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                    Text(if (sellerMode) stringResource(R.string.orders_contact_buyer) else stringResource(R.string.orders_contact_seller), color = StitchPalette.OnSurface)
                }
                if (order.status == "shipped") {
                    TextButton(onClick = onTrack) { Text(stringResource(R.string.orders_view_tracking), color = StitchPalette.Brand) }
                }
            }
        }
    }
}

@Composable
private fun TrackingStep(title: String, body: String, done: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(if (done) StitchPalette.Brand else StitchPalette.SurfaceLow).border(1.dp, StitchPalette.BorderHairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (done) "✓" else "", color = Color.White, fontWeight = FontWeight.Black)
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (done) StitchPalette.Brand else StitchPalette.OnSurfaceVariant, fontWeight = FontWeight.Black)
            Text(body, style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
        }
    }
}

@Composable
private fun OrdersEmptyState(
    title: String,
    body: String,
    primary: String,
    secondary: String?,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?,
) {
    Column(
        Modifier.fillMaxSize().padding(30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(StitchPalette.BrandMuted), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(22.dp))
        Button(onClick = onPrimary, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand)) {
            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(primary, color = Color.White, fontWeight = FontWeight.Bold)
        }
        if (secondary != null && onSecondary != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onSecondary, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                Text(secondary, color = StitchPalette.OnSurface)
            }
        }
        Spacer(Modifier.height(22.dp))
        listOf(
            R.string.orders_starter_market,
            R.string.orders_starter_contact,
            R.string.orders_starter_messages,
        ).forEach {
            Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = StitchPalette.Brand, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = statusColor(status)
    Surface(shape = StitchShape.pill, color = color.copy(alpha = 0.15f)) {
        Text(
            statusLabel(status).uppercase(),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun ButtonLine(text: String) {
    Button(
        onClick = {},
        shape = StitchShape.field,
        colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
        modifier = Modifier.fillMaxWidth().height(38.dp),
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OutlinedButtonLine(text: String) {
    OutlinedButton(
        onClick = {},
        shape = StitchShape.field,
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
        modifier = Modifier.fillMaxWidth().height(38.dp),
    ) {
        Text(text, color = StitchPalette.OnSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun statusLabel(status: String): String =
    when (status) {
        "pending_payment" -> stringResource(R.string.orders_status_pending)
        "paid" -> stringResource(R.string.orders_status_paid)
        "shipped" -> stringResource(R.string.orders_status_shipped)
        "completed" -> stringResource(R.string.orders_status_completed)
        "cancelled" -> stringResource(R.string.orders_status_cancelled)
        "refunded" -> stringResource(R.string.orders_status_refunded)
        else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

@Composable
private fun statusColor(status: String): Color =
    when (status) {
        "pending_payment" -> Color(0xFFF59E0B)
        "paid" -> Color(0xFF2F7FD5)
        "shipped" -> Color(0xFF2F7FD5)
        "completed" -> Color(0xFF22C55E)
        "cancelled" -> StitchPalette.OnSurfaceVariant
        "refunded" -> StitchPalette.Error
        else -> StitchPalette.Brand
    }

private fun formatOrderPrice(cents: Long, currency: String): String {
    val symbol = if (currency.uppercase() == "CNY") "$" else currency.uppercase()
    val amount = cents / 100.0
    return "$symbol${amount.toString().trimEnd('0').trimEnd('.')}"
}
