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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoneAll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.Order
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import kotlinx.coroutines.launch

private data class OrderFilter(val key: String, val labelRes: Int)

private data class SafetyOption(val key: String, val labelRes: Int)

private val ORDER_FILTERS =
    listOf(
        OrderFilter("all", R.string.orders_filter_all),
        OrderFilter("pending_payment", R.string.orders_filter_pending),
        OrderFilter("paid", R.string.orders_filter_paid),
        OrderFilter("shipped", R.string.orders_filter_shipped),
        OrderFilter("completed", R.string.orders_filter_completed),
    )

private val REVIEW_TAGS =
    listOf(
        R.string.safety_review_tag_fast_reply,
        R.string.safety_review_tag_accurate,
        R.string.safety_review_tag_pickup,
        R.string.safety_review_tag_quality,
        R.string.safety_review_tag_friendly,
    )

private val REPORT_REASONS =
    listOf(
        SafetyOption("item", R.string.safety_report_reason_item),
        SafetyOption("unresponsive", R.string.safety_report_reason_unresponsive),
        SafetyOption("pickup", R.string.safety_report_reason_pickup),
        SafetyOption("payment", R.string.safety_report_reason_payment),
        SafetyOption("other", R.string.safety_report_reason_other),
    )

private enum class OrderSafetyRoute {
    LeaveReview,
    SellerReviews,
    ReportIssue,
    OpenDispute,
    DisputeStatus,
}

@Composable
fun StitchOrdersScreen(
    sdk: MeowCircleSdk,
    apiBase: String,
    mockMode: Boolean,
    onEnableMock: () -> Unit,
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
    var localMockMode by remember(mockMode) { mutableStateOf(mockMode) }
    var refreshSignal by remember { mutableStateOf(0) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var showTracking by remember { mutableStateOf(false) }
    var safetyRoute by remember { mutableStateOf<OrderSafetyRoute?>(null) }
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

    LaunchedEffect(role, refreshSignal, localMockMode) {
        if (localMockMode) {
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
        if (localMockMode) {
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
        safetyRoute != null && selectedOrder != null ->
            when (safetyRoute) {
                OrderSafetyRoute.LeaveReview ->
                    LeaveReviewScreen(
                        order = selectedOrder!!,
                        onBack = { safetyRoute = null },
                        onSellerReviews = { safetyRoute = OrderSafetyRoute.SellerReviews },
                        onSubmit = {
                            infoText = context.getString(R.string.safety_review_submitted)
                            safetyRoute = null
                        },
                        modifier = modifier,
                    )
                OrderSafetyRoute.SellerReviews ->
                    SellerReviewsScreen(
                        order = selectedOrder!!,
                        onBack = { safetyRoute = null },
                        modifier = modifier,
                    )
                OrderSafetyRoute.ReportIssue ->
                    ReportIssueScreen(
                        order = selectedOrder!!,
                        onBack = { safetyRoute = null },
                        onSubmit = {
                            infoText = context.getString(R.string.safety_report_submitted)
                            safetyRoute = null
                        },
                        modifier = modifier,
                    )
                OrderSafetyRoute.OpenDispute ->
                    OpenDisputeScreen(
                        order = selectedOrder!!,
                        onBack = { safetyRoute = null },
                        onSubmit = { safetyRoute = OrderSafetyRoute.DisputeStatus },
                        modifier = modifier,
                    )
                OrderSafetyRoute.DisputeStatus ->
                    DisputeStatusScreen(
                        order = selectedOrder!!,
                        onBack = { safetyRoute = null },
                        onOpenMessages = onOpenMessages,
                        onAddEvidence = {
                            infoText = context.getString(R.string.safety_evidence_added)
                        },
                        onCloseDispute = {
                            infoText = context.getString(R.string.safety_dispute_closed)
                            safetyRoute = null
                        },
                        modifier = modifier,
                    )
                null -> Unit
            }
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
                apiBase = apiBase,
                onBack = { selectedOrder = null },
                onTrack = { showTracking = true },
                onOpenMessages = onOpenMessages,
                onAction = { updateOrder(it, selectedOrder!!) },
                onLeaveReview = { safetyRoute = OrderSafetyRoute.LeaveReview },
                onSellerReviews = { safetyRoute = OrderSafetyRoute.SellerReviews },
                onReportIssue = { safetyRoute = OrderSafetyRoute.ReportIssue },
                onOpenDispute = { safetyRoute = OrderSafetyRoute.OpenDispute },
                modifier = modifier,
            )
        else ->
            OrdersListScreen(
                role = role,
                filter = filter,
                orders = orders.orEmpty().filter { filter == "all" || it.status == filter },
                loading = loading,
                err = err,
                mockMode = localMockMode,
                apiBase = apiBase,
                onRoleToggle = {
                    role = if (role == "buyer") "seller" else "buyer"
                    selectedOrder = null
                },
                onFilterChange = { filter = it },
                onOpenOrder = { selectedOrder = it },
                onEnableMock = {
                    localMockMode = true
                    onEnableMock()
                },
                onOpenMarket = onOpenMarket,
                onOpenMessages = onOpenMessages,
                onAction = { action, order -> updateOrder(action, order) },
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
    apiBase: String,
    onRoleToggle: () -> Unit,
    onFilterChange: (String) -> Unit,
    onOpenOrder: (Order) -> Unit,
    onEnableMock: () -> Unit,
    onOpenMarket: () -> Unit,
    onOpenMessages: () -> Unit,
    onAction: (String, Order) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StitchPalette.Surface)
                .statusBarsPadding()
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
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
                        OrderListCard(
                            order = order,
                            role = role,
                            apiBase = apiBase,
                            onClick = { onOpenOrder(order) },
                            onAction = { action -> onAction(action, order) },
                            onOpenMessages = onOpenMessages,
                        )
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
    apiBase: String,
    onBack: () -> Unit,
    onTrack: () -> Unit,
    onOpenMessages: () -> Unit,
    onAction: (String) -> Unit,
    onLeaveReview: () -> Unit,
    onSellerReviews: () -> Unit,
    onReportIssue: () -> Unit,
    onOpenDispute: () -> Unit,
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
            OrderProductCard(order, sellerMode, apiBase)
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
                InfoCard(title = stringResource(R.string.safety_feedback_title)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SafetyActionRow(stringResource(R.string.safety_leave_review), onLeaveReview)
                        SafetyActionRow(stringResource(R.string.safety_seller_reviews), onSellerReviews)
                        SafetyActionRow(stringResource(R.string.safety_report_issue), onReportIssue)
                        SafetyActionRow(stringResource(R.string.safety_open_dispute), onOpenDispute, danger = true)
                    }
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
private fun LeaveReviewScreen(
    order: Order,
    onBack: () -> Unit,
    onSellerReviews: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rating by remember { mutableStateOf(5) }
    var review by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf(REVIEW_TAGS[0], REVIEW_TAGS[1], REVIEW_TAGS[2], REVIEW_TAGS[3])) }
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Header(title = stringResource(R.string.safety_leave_review), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OrderReviewSummary(order)
            InfoCard(title = stringResource(R.string.safety_rating)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { index ->
                        Text(
                            text = "★",
                            modifier = Modifier.clickable { rating = index }.padding(2.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (index <= rating) StitchPalette.Brand else StitchPalette.OutlineVariant,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            InfoCard(title = stringResource(R.string.safety_write_review)) {
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    minLines = 4,
                    placeholder = { Text(stringResource(R.string.safety_review_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = StitchShape.field,
                    colors = orderTextFieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    REVIEW_TAGS.forEach { labelRes ->
                        val selected = labelRes in selectedTags
                        SafetyChip(
                            label = stringResource(labelRes),
                            selected = selected,
                            onClick = {
                                selectedTags = if (selected) selectedTags - labelRes else selectedTags + labelRes
                            },
                        )
                    }
                }
            }
            TextButton(onClick = onSellerReviews, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.safety_seller_reviews), color = StitchPalette.Brand, fontWeight = FontWeight.Bold)
            }
        }
        BottomTwoActions(
            secondary = stringResource(R.string.safety_save_draft),
            primary = stringResource(R.string.safety_submit_review),
            onSecondary = onBack,
            onPrimary = onSubmit,
        )
    }
}

@Composable
private fun SellerReviewsScreen(
    order: Order,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Header(title = stringResource(R.string.safety_seller_reviews), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(88.dp).clip(CircleShape).background(StitchPalette.BrandMuted), contentAlignment = Alignment.Center) {
                Text("J", style = MaterialTheme.typography.headlineMedium, color = StitchPalette.Brand, fontWeight = FontWeight.Black)
            }
            Text(stringResource(R.string.safety_seller_name), style = MaterialTheme.typography.headlineSmall, color = StitchPalette.PrimaryDark, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.market_verified_seller_rating), style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SellerMetric("4.8", stringResource(R.string.market_rating), Modifier.weight(1f))
                SellerMetric("124", stringResource(R.string.market_sales), Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SafetyChip(stringResource(R.string.safety_review_filter_product), selected = true, onClick = {})
                SafetyChip(stringResource(R.string.safety_review_filter_pickup), selected = false, onClick = {})
                SafetyChip(stringResource(R.string.safety_review_filter_communication), selected = false, onClick = {})
            }
            ReviewCard(stringResource(R.string.safety_review_author_luna), stringResource(R.string.safety_review_luna_body))
            ReviewCard(stringResource(R.string.safety_review_author_sarah), stringResource(R.string.safety_review_sarah_body))
            OrderReviewSummary(order)
        }
    }
}

@Composable
private fun ReportIssueScreen(
    order: Order,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reason by remember { mutableStateOf("pickup") }
    var details by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Header(title = stringResource(R.string.safety_report_issue), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OrderReviewSummary(order)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = StitchShape.cardFeed,
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0x55F59E0B)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.safety_urgent_title), style = MaterialTheme.typography.titleSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.safety_urgent_body), style = MaterialTheme.typography.bodySmall, color = Color(0xCCB45309))
                }
            }
            InfoCard(title = stringResource(R.string.safety_issue_question)) {
                REPORT_REASONS.forEach { option ->
                    SafetySelectRow(
                        label = stringResource(option.labelRes),
                        selected = reason == option.key,
                        onClick = { reason = option.key },
                    )
                }
            }
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                minLines = 4,
                placeholder = { Text(stringResource(R.string.safety_report_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = StitchShape.field,
                colors = orderTextFieldColors(),
            )
        }
        BottomTwoActions(
            secondary = stringResource(R.string.common_cancel),
            primary = stringResource(R.string.safety_submit_report),
            onSecondary = onBack,
            onPrimary = onSubmit,
        )
    }
}

@Composable
private fun OpenDisputeScreen(
    order: Order,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var resolution by remember { mutableStateOf("refund") }
    var note by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Header(title = stringResource(R.string.safety_open_dispute), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OrderReviewSummary(order)
            Text(stringResource(R.string.safety_dispute_intro), style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
            InfoCard(title = stringResource(R.string.safety_dispute_reason)) {
                SafetySelectRow(stringResource(R.string.safety_report_reason_unresponsive), selected = true, onClick = {})
                SafetySelectRow(stringResource(R.string.safety_report_reason_pickup), selected = false, onClick = {})
            }
            InfoCard(title = stringResource(R.string.safety_requested_resolution)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    DisputeResolutionCard(stringResource(R.string.safety_resolution_refund), resolution == "refund", Modifier.weight(1f)) { resolution = "refund" }
                    DisputeResolutionCard(stringResource(R.string.safety_resolution_replace), resolution == "replace", Modifier.weight(1f)) { resolution = "replace" }
                }
                Spacer(Modifier.height(10.dp))
                SummaryRow(stringResource(R.string.orders_total), formatOrderPrice(order.amountCents, order.currency), total = true)
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                minLines = 3,
                placeholder = { Text(stringResource(R.string.safety_dispute_note_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                shape = StitchShape.field,
                colors = orderTextFieldColors(),
            )
        }
        BottomTwoActions(
            secondary = stringResource(R.string.safety_save_draft),
            primary = stringResource(R.string.safety_submit_dispute),
            onSecondary = onBack,
            onPrimary = onSubmit,
        )
    }
}

@Composable
private fun DisputeStatusScreen(
    order: Order,
    onBack: () -> Unit,
    onOpenMessages: () -> Unit,
    onAddEvidence: () -> Unit,
    onCloseDispute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Header(title = stringResource(R.string.safety_dispute_status), onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoCard(title = stringResource(R.string.safety_dispute_id, order.id)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.orders_order_number, order.id), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Surface(shape = StitchShape.pill, color = Color(0xFFFEF3C7)) {
                        Text(stringResource(R.string.safety_under_review), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = Color(0xFFF59E0B), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text(stringResource(R.string.safety_under_review_body), style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
            }
            InfoCard(title = stringResource(R.string.safety_timeline)) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    TrackingStep(
                        title = stringResource(R.string.safety_step_submitted),
                        body = stringResource(R.string.orders_step_placed),
                        state = StepState.COMPLETED,
                        icon = Icons.Outlined.Check,
                        connectorDone = true,
                    )
                    TrackingStep(
                        title = stringResource(R.string.safety_step_seller_notified),
                        body = stringResource(R.string.messages_today),
                        state = StepState.COMPLETED,
                        icon = Icons.Outlined.Check,
                        connectorDone = true,
                    )
                    TrackingStep(
                        title = stringResource(R.string.safety_step_under_review),
                        body = stringResource(R.string.safety_support_reviewing),
                        state = StepState.ACTIVE,
                        icon = Icons.Outlined.Check,
                        connectorDone = false,
                    )
                    TrackingStep(
                        title = stringResource(R.string.safety_step_resolution),
                        body = stringResource(R.string.safety_waiting_resolution),
                        state = StepState.FUTURE,
                        icon = Icons.Outlined.DoneAll,
                        connectorDone = false,
                        isLast = true,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenMessages, shape = StitchShape.field, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                    Text(stringResource(R.string.safety_message_seller), color = StitchPalette.OnSurface)
                }
                OutlinedButton(onClick = onAddEvidence, shape = StitchShape.field, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, StitchPalette.Brand)) {
                    Text(stringResource(R.string.safety_add_evidence), color = StitchPalette.Brand, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(onClick = onCloseDispute, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(48.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                Text(stringResource(R.string.safety_close_dispute), color = StitchPalette.OnSurfaceVariant)
            }
        }
    }
}

private enum class StepState {
    COMPLETED,
    ACTIVE,
    FUTURE
}

@Composable
private fun TrackingScreen(
    order: Order,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = order.status
    val isPaid = order.paidAt != null

    val step1State = StepState.COMPLETED
    val step2State = when {
        isPaid -> StepState.COMPLETED
        status == "pending_payment" || status == "placed" -> StepState.ACTIVE
        else -> StepState.FUTURE
    }
    val step3State = when {
        status == "completed" -> StepState.COMPLETED
        status == "shipped" -> StepState.ACTIVE
        else -> StepState.FUTURE
    }
    val step4State = when {
        status == "completed" -> StepState.ACTIVE
        else -> StepState.FUTURE
    }

    val connector1Done = isPaid
    val connector2Done = status in listOf("shipped", "completed")
    val connector3Done = status == "completed"

    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Header(title = stringResource(R.string.orders_tracking), onBack = onBack, close = true)
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                shape = StitchShape.cardFeed,
                colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.orders_tracking_number),
                        style = MaterialTheme.typography.labelMedium,
                        color = StitchPalette.OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        text = "9400123456",
                        style = MaterialTheme.typography.titleLarge,
                        color = StitchPalette.Brand,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Card(
                shape = StitchShape.cardFeed,
                colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    TrackingStep(
                        title = stringResource(R.string.orders_step_placed),
                        body = order.createdAt.take(16).replace('T', ' '),
                        state = step1State,
                        icon = Icons.Outlined.Check,
                        connectorDone = connector1Done,
                    )
                    TrackingStep(
                        title = stringResource(R.string.orders_step_paid),
                        body = order.paidAt?.take(16)?.replace('T', ' ') ?: stringResource(R.string.orders_awaiting_payment),
                        state = step2State,
                        icon = Icons.Outlined.ReceiptLong,
                        connectorDone = connector2Done,
                    )
                    TrackingStep(
                        title = stringResource(R.string.orders_step_shipped),
                        body = if (status in listOf("shipped", "completed")) stringResource(R.string.orders_in_transit) else stringResource(R.string.orders_step_shipped),
                        state = step3State,
                        icon = Icons.Outlined.LocalShipping,
                        connectorDone = connector3Done,
                    )
                    TrackingStep(
                        title = stringResource(R.string.orders_step_completed),
                        body = if (status == "completed") stringResource(R.string.orders_step_completed) else stringResource(R.string.orders_awaiting_delivery),
                        state = step4State,
                        icon = Icons.Outlined.DoneAll,
                        connectorDone = false,
                        isLast = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderListCard(
    order: Order,
    role: String,
    apiBase: String,
    onClick: () -> Unit,
    onAction: (String) -> Unit,
    onOpenMessages: () -> Unit,
) {
    val imageUrl = remember(order.id, order.listingTitle) {
        when {
            order.id == 4824L || order.listingTitle.contains("项圈") || order.listingTitle.contains("Collar") -> 
                "${apiBase.removeSuffix("/")}/mock-images/mock_image_14.png"
            order.id == 9810L || order.listingTitle.contains("碗") || order.listingTitle.contains("Bowl") -> 
                "${apiBase.removeSuffix("/")}/mock-images/mock_image_15.png"
            order.id == 832L || order.listingTitle.contains("胸背") || order.listingTitle.contains("Harness") -> 
                "${apiBase.removeSuffix("/")}/mock-images/mock_image_6.png"
            else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        R.string.orders_card_meta,
                        stringResource(R.string.orders_order_number, order.id),
                        if (role == "buyer") "Jordan M." else "Sarah P.",
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchPalette.OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(order.status)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(74.dp).clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = order.listingTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(order.listingTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatOrderPrice(order.amountCents, order.currency), style = MaterialTheme.typography.titleMedium, color = StitchPalette.Brand, fontWeight = FontWeight.Black)
                }
            }
            when (order.status) {
                "shipped" -> ButtonLine(stringResource(R.string.orders_confirm_receipt), onClick = { onAction("complete") })
                "paid" -> OutlinedButtonLine(
                    if (role == "seller") stringResource(R.string.orders_contact_buyer) else stringResource(R.string.orders_contact_seller),
                    onClick = onOpenMessages,
                )
                "pending_payment" -> ButtonLine(stringResource(R.string.orders_pay_now), onClick = { onAction("pay") })
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchPalette.Surface)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = if (close) stringResource(R.string.common_close) else stringResource(R.string.common_back), tint = StitchPalette.OnSurface)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(44.dp))
        }
        HorizontalDivider(color = StitchPalette.BorderHairline)
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
private fun OrderProductCard(order: Order, sellerMode: Boolean, apiBase: String) {
    val imageUrl = remember(order.id, order.listingTitle) {
        when {
            order.id == 4824L || order.listingTitle.contains("项圈") || order.listingTitle.contains("Collar") -> 
                "${apiBase.removeSuffix("/")}/mock-images/mock_image_14.png"
            order.id == 9810L || order.listingTitle.contains("碗") || order.listingTitle.contains("Bowl") -> 
                "${apiBase.removeSuffix("/")}/mock-images/mock_image_15.png"
            order.id == 832L || order.listingTitle.contains("胸背") || order.listingTitle.contains("Harness") -> 
                "${apiBase.removeSuffix("/")}/mock-images/mock_image_6.png"
            else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
        }
    }

    Card(
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = imageUrl ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png",
                    contentDescription = order.listingTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
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
private fun OrderReviewSummary(order: Order) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StitchShape.cardFeed,
        color = StitchPalette.Surface,
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(62.dp).clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ShoppingBag, contentDescription = null, tint = StitchPalette.Brand)
            }
            Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.orders_order_number, order.id), style = MaterialTheme.typography.labelMedium, color = StitchPalette.OnSurfaceVariant)
                Text(order.listingTitle, style = MaterialTheme.typography.titleSmall, color = StitchPalette.OnSurface, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.orders_seller_name), style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
            }
            Text(formatOrderPrice(order.amountCents, order.currency), style = MaterialTheme.typography.titleMedium, color = StitchPalette.Brand, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SafetyActionRow(
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = StitchShape.field,
        color = if (danger) StitchPalette.GoldWeak else StitchPalette.SurfaceLow,
        border = BorderStroke(1.dp, if (danger) StitchPalette.Gold else StitchPalette.BorderHairline),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (danger) StitchPalette.PrimaryDark else StitchPalette.OnSurface, fontWeight = FontWeight.SemiBold)
            Text("›", style = MaterialTheme.typography.titleLarge, color = StitchPalette.Outline)
        }
    }
}

@Composable
private fun SafetyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = StitchShape.pill,
        color = if (selected) StitchPalette.BrandMuted else StitchPalette.Surface,
        border = BorderStroke(1.dp, if (selected) StitchPalette.Brand else StitchPalette.BorderHairline),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SellerMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = StitchShape.field,
        color = StitchPalette.Surface,
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = StitchPalette.PrimaryDark, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = StitchPalette.OnSurfaceVariant)
        }
    }
}

@Composable
private fun ReviewCard(
    author: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StitchShape.cardFeed,
        color = StitchPalette.Surface,
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(author, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = StitchPalette.OnSurface, fontWeight = FontWeight.Bold)
                Text("★★★★★", color = StitchPalette.Brand, style = MaterialTheme.typography.labelLarge)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurface)
        }
    }
}

@Composable
private fun SafetySelectRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(StitchShape.field)
                .background(if (selected) StitchPalette.BrandMuted else StitchPalette.Surface)
                .border(1.dp, if (selected) StitchPalette.Brand else StitchPalette.BorderHairline, StitchShape.field)
                .clickable(onClick = onClick)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) StitchPalette.Brand else StitchPalette.Surface)
                .border(1.dp, if (selected) StitchPalette.Brand else StitchPalette.Outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
        }
        Text(label, modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurface)
    }
}

@Composable
private fun DisputeResolutionCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = StitchShape.field,
        color = if (selected) StitchPalette.BrandMuted else StitchPalette.Surface,
        border = BorderStroke(1.dp, if (selected) StitchPalette.Brand else StitchPalette.BorderHairline),
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(if (selected) StitchPalette.Brand else StitchPalette.SurfaceLow))
            Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = StitchPalette.OnSurface, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BottomTwoActions(
    secondary: String,
    primary: String,
    onSecondary: () -> Unit,
    onPrimary: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(StitchPalette.Surface).border(1.dp, StitchPalette.BorderHairline).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(onClick = onSecondary, shape = StitchShape.field, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
            Text(secondary, color = StitchPalette.OnSurface, fontWeight = FontWeight.Bold)
        }
        Button(onClick = onPrimary, shape = StitchShape.field, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand)) {
            Text(primary, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun orderTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = StitchPalette.Brand,
        unfocusedBorderColor = StitchPalette.BorderHairline,
        cursorColor = StitchPalette.Brand,
        focusedContainerColor = StitchPalette.Surface,
        unfocusedContainerColor = StitchPalette.Surface,
        focusedTextColor = StitchPalette.OnSurface,
        unfocusedTextColor = StitchPalette.OnSurface,
    )

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
private fun TrackingStep(
    title: String,
    body: String,
    state: StepState,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    connectorDone: Boolean = false,
    isLast: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            StepState.COMPLETED -> Color(0xFFFF8A3D)
                            StepState.ACTIVE -> Color.White
                            StepState.FUTURE -> Color.White
                        }
                    )
                    .border(
                        width = if (state == StepState.COMPLETED) 0.dp else 2.dp,
                        color = when (state) {
                            StepState.COMPLETED -> Color.Transparent
                            StepState.ACTIVE -> Color(0xFFFF8A3D)
                            StepState.FUTURE -> Color(0xFFD6C7BE)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (state) {
                        StepState.COMPLETED -> Color.White
                        StepState.ACTIVE -> Color(0xFFFF8A3D)
                        StepState.FUTURE -> Color(0xFFD6C7BE)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            if (connectorDone) Color(0xFFFF8A3D) else Color(0xFFF5E2D5)
                        )
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 4.dp, bottom = 24.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = when (state) {
                    StepState.COMPLETED -> Color(0xFF231F20)
                    StepState.ACTIVE -> Color(0xFFFF8A3D)
                    StepState.FUTURE -> Color(0xFF9A8A80)
                },
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = when (state) {
                    StepState.COMPLETED -> Color(0xFF6B5E57)
                    StepState.ACTIVE -> Color(0xFF6B5E57)
                    StepState.FUTURE -> Color(0xFF9A8A80)
                }
            )
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
private fun ButtonLine(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = StitchShape.field,
        colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
        modifier = Modifier.fillMaxWidth().height(38.dp),
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OutlinedButtonLine(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
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
