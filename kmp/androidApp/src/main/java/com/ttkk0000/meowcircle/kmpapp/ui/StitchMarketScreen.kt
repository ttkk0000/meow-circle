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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.Listing
import com.ttkk0000.meowcircle.ListingDetailData
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShadows
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBar
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBarLeading
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBarTrailing
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchSearchField
import com.ttkk0000.meowcircle.kmpapp.util.resolveMediaUrl
import kotlinx.coroutines.launch

private data class MarketOption(val key: String, val labelRes: Int)

private val MARKET_CATEGORIES =
    listOf(
        MarketOption("toys", R.string.market_category_toys),
        MarketOption("food", R.string.market_category_food),
        MarketOption("apparel", R.string.market_category_apparel),
        MarketOption("care", R.string.market_category_care),
    )

private val TRADE_TYPES =
    listOf(
        MarketOption("sell", R.string.market_trade_sell),
        MarketOption("trade", R.string.market_trade_trade),
        MarketOption("looking_for", R.string.market_trade_looking_for),
        MarketOption("free", R.string.market_trade_free),
    )

private val CONDITIONS =
    listOf(
        MarketOption("new", R.string.market_condition_new),
        MarketOption("lightly_used", R.string.market_condition_lightly_used),
        MarketOption("used", R.string.market_condition_used),
    )

@Composable
fun StitchMarketScreen(
    sdk: MeowCircleSdk,
    user: User,
    apiBase: String,
    listings: List<Listing>?,
    loading: Boolean,
    err: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    mockMode: Boolean,
    onEnableMock: () -> Unit,
    onAvatarPress: () -> Unit,
    onNotifyPress: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenMessages: () -> Unit,
    onChromeVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("toys") }
    var selectedTrade by remember { mutableStateOf("sell") }
    var selectedListing by remember { mutableStateOf<Listing?>(null) }
    var detail by remember { mutableStateOf<ListingDetailData?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var seller by remember { mutableStateOf<User?>(null) }
    var showOffer by remember { mutableStateOf(false) }
    var showPublish by remember { mutableStateOf(false) }
    var showSellerProfile by remember { mutableStateOf(false) }
    var infoText by remember { mutableStateOf<String?>(null) }
    var actionLoading by remember { mutableStateOf(false) }
    val chromeVisible = !showPublish && !showOffer && !showSellerProfile && selectedListing == null

    val visibleListings =
        remember(listings, query, selectedCategory, selectedTrade) {
            listings.orEmpty()
                .filter { listing ->
                    query.isBlank() ||
                        listing.title.contains(query, ignoreCase = true) ||
                        listing.description.contains(query, ignoreCase = true)
                }
                .filter { listing -> listing.matchesCategory(selectedCategory) }
                .filter { listing -> listing.matchesTradeType(selectedTrade) }
        }

    LaunchedEffect(chromeVisible) {
        onChromeVisibleChange(chromeVisible)
    }

    LaunchedEffect(selectedListing?.id, mockMode) {
        val listing = selectedListing ?: return@LaunchedEffect
        detail = ListingDetailData(listing = listing)
        seller = null
        if (mockMode) {
            seller = mockSeller(listing.sellerId)
            detailLoading = false
            return@LaunchedEffect
        }
        detailLoading = true
        detail =
            sdk.listingDetail(listing.id).getOrElse {
                ListingDetailData(listing = listing)
            }
        seller = sdk.publicUser(listing.sellerId).getOrNull()
        detailLoading = false
    }

    fun sendSellerMessage(listing: Listing, content: String, after: (() -> Unit)? = null) {
        if (listing.sellerId == user.id) {
            infoText = context.getString(R.string.market_seller_self)
            return
        }
        if (mockMode) {
            infoText = context.getString(R.string.market_message_sent)
            after?.invoke()
            return
        }
        actionLoading = true
        scope.launch {
            sdk.sendMessage(listing.sellerId, content).fold(
                onSuccess = {
                    infoText = context.getString(R.string.market_message_sent)
                    after?.invoke()
                },
                onFailure = { e ->
                    infoText = (e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl)
                },
            )
            actionLoading = false
        }
    }

    fun buyNow(listing: Listing) {
        if (listing.priceCents <= 0L) {
            sendSellerMessage(listing, context.getString(R.string.market_interest_message, listing.title))
            return
        }
        if (mockMode) {
            infoText = context.getString(R.string.market_order_created)
            onOpenOrders()
            return
        }
        actionLoading = true
        scope.launch {
            sdk.createOrder(listing.id, note = context.getString(R.string.market_order_note)).fold(
                onSuccess = {
                    infoText = context.getString(R.string.market_order_created_id, it.id)
                    onOpenOrders()
                },
                onFailure = { e ->
                    infoText = (e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl)
                },
            )
            actionLoading = false
        }
    }

    when {
        showPublish ->
            PublishProductScreen(
                sdk = sdk,
                mockMode = mockMode,
                onClose = { showPublish = false },
                onPublished = {
                    showPublish = false
                    infoText = context.getString(R.string.market_published)
                },
                onError = { infoText = it },
                modifier = modifier,
            )
        showOffer && selectedListing != null ->
            MakeOfferScreen(
                listing = selectedListing!!,
                apiBase = apiBase,
                busy = actionLoading,
                onDismiss = { showOffer = false },
                onSend = { offerCents, message ->
                    val listing = selectedListing ?: return@MakeOfferScreen
                    val offer = formatListingPrice(offerCents, listing.currency)
                    sendSellerMessage(
                        listing,
                        context.getString(R.string.market_offer_message_template, listing.title, offer, message).trim(),
                    )
                    showOffer = false
                },
                modifier = modifier,
            )
        showSellerProfile && selectedListing != null ->
            SellerProfileScreen(
                seller = seller ?: mockSeller(selectedListing!!.sellerId),
                listings = visibleListings.filter { it.sellerId == selectedListing!!.sellerId }.ifEmpty { visibleListings.take(2) },
                apiBase = apiBase,
                onBack = { showSellerProfile = false },
                onMessageSeller = {
                    selectedListing?.let { listing ->
                        sendSellerMessage(listing, context.getString(R.string.market_availability_message, listing.title)) {
                            onOpenMessages()
                        }
                    }
                },
                onSelectListing = {
                    selectedListing = it
                    showSellerProfile = false
                },
                modifier = modifier,
            )
        selectedListing != null ->
            ProductDetailScreen(
                apiBase = apiBase,
                detail = detail ?: ListingDetailData(selectedListing!!),
                seller = seller,
                loading = detailLoading,
                actionLoading = actionLoading,
                onBack = {
                    selectedListing = null
                    detail = null
                    seller = null
                },
                onContact = {
                    val listing = selectedListing ?: return@ProductDetailScreen
                    sendSellerMessage(listing, context.getString(R.string.market_availability_message, listing.title)) {
                        onOpenMessages()
                    }
                },
                onOffer = { showOffer = true },
                onSeller = { showSellerProfile = true },
                onSave = { infoText = context.getString(R.string.common_saved) },
                onShare = { infoText = context.getString(R.string.common_share_ready) },
                onBuyNow = {
                    selectedListing?.let(::buyNow)
                },
                modifier = modifier,
            )
        else ->
            MarketListScreen(
                user = user,
                listings = visibleListings,
                loading = loading,
                err = err,
                query = query,
                selectedCategory = selectedCategory,
                selectedTrade = selectedTrade,
                mockMode = mockMode,
                apiBase = apiBase,
                onQueryChange = onQueryChange,
                onCategoryChange = { selectedCategory = it },
                onTradeChange = { selectedTrade = it },
                onAvatarPress = onAvatarPress,
                onNotifyPress = onNotifyPress,
                onEnableMock = onEnableMock,
                onPublish = { showPublish = true },
                onSelectListing = { selectedListing = it },
                onBuyNow = ::buyNow,
                modifier = modifier,
            )
    }

    if (infoText != null) {
        AlertDialog(
            onDismissRequest = { infoText = null },
            shape = StitchShape.dialog,
            title = { Text(stringResource(R.string.market_title)) },
            text = { Text(infoText.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { infoText = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarketListScreen(
    user: User,
    listings: List<Listing>,
    loading: Boolean,
    err: String?,
    query: String,
    selectedCategory: String,
    selectedTrade: String,
    mockMode: Boolean,
    apiBase: String,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTradeChange: (String) -> Unit,
    onAvatarPress: () -> Unit,
    onNotifyPress: () -> Unit,
    onEnableMock: () -> Unit,
    onPublish: () -> Unit,
    onSelectListing: (Listing) -> Unit,
    onBuyNow: (Listing) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilters by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        StitchTopBar(
            apiBase = apiBase,
            user = user,
            title = "M&D",
            leading = StitchTopBarLeading.Paw,
            trailing = StitchTopBarTrailing.Bell,
            onAvatarPress = onAvatarPress,
            onNotifyPress = onNotifyPress,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StitchSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.market_search_placeholder),
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    modifier = Modifier.size(48.dp).clickable { showFilters = true },
                    shape = CircleShape,
                    color = StitchPalette.Surface,
                    border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.market_filter), tint = StitchPalette.OnSurface)
                    }
                }
            }
            if (query.isNotBlank()) {
                Text(
                    stringResource(R.string.market_search_results, query),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = StitchPalette.OnSurfaceVariant,
                )
            }
            OptionRow(
                options = MARKET_CATEGORIES,
                selected = selectedCategory,
                onSelect = onCategoryChange,
            )
        }
        Spacer(Modifier.height(26.dp))
        Box(Modifier.weight(1f)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StitchPalette.Brand)
                }
                err != null && !mockMode -> MarketEmptyState(
                    title = stringResource(R.string.market_unavailable_title),
                    body = err,
                    primary = stringResource(R.string.market_view_demo),
                    secondary = null,
                    onPrimary = onEnableMock,
                    onSecondary = null,
                )
                listings.isEmpty() -> MarketEmptyState(
                    title = stringResource(R.string.market_no_products_title),
                    body = stringResource(R.string.market_no_products_body),
                    primary = stringResource(R.string.market_clear_filters),
                    secondary = stringResource(R.string.market_browse_all),
                    onPrimary = {
                        onQueryChange("")
                        onTradeChange("sell")
                    },
                    onSecondary = { onCategoryChange("toys") },
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(listings, key = { it.id }) { listing ->
                        MarketHeroCard(
                            listing = listing,
                            featured = listing == listings.first(),
                            apiBase = apiBase,
                            mockMode = mockMode,
                            onClick = { onSelectListing(listing) },
                            onBuyNow = { onBuyNow(listing) },
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = filterSheetState,
            containerColor = StitchPalette.Surface,
            dragHandle = null,
        ) {
            MarketFilterSheet(
                selectedCategory = selectedCategory,
                selectedTrade = selectedTrade,
                onCategoryChange = onCategoryChange,
                onTradeChange = onTradeChange,
                onApply = { showFilters = false },
                onClear = {
                    onQueryChange("")
                    onCategoryChange("toys")
                    onTradeChange("sell")
                    showFilters = false
                },
            )
        }
    }
}

@Composable
private fun MarketFilterSheet(
    selectedCategory: String,
    selectedTrade: String,
    onCategoryChange: (String) -> Unit,
    onTradeChange: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.market_filters_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = StitchPalette.OnSurface,
            )
            IconButton(onClick = onApply, modifier = Modifier.size(42.dp).clip(CircleShape).background(StitchPalette.SurfaceLow)) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_close), tint = StitchPalette.OnSurface)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.market_field_category), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = StitchPalette.OnSurface)
            OptionRow(MARKET_CATEGORIES, selectedCategory, onSelect = onCategoryChange)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.market_field_trade_type), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = StitchPalette.OnSurface)
            OptionRow(TRADE_TYPES, selectedTrade, onSelect = onTradeChange)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onClear,
                shape = StitchShape.field,
                modifier = Modifier.weight(1f).height(50.dp),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
            ) {
                Text(stringResource(R.string.market_clear_filters), color = StitchPalette.OnSurface, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onApply,
                shape = StitchShape.field,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
            ) {
                Text(stringResource(R.string.market_apply_filters), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(
    apiBase: String,
    detail: ListingDetailData,
    seller: User?,
    loading: Boolean,
    actionLoading: Boolean,
    onBack: () -> Unit,
    onContact: () -> Unit,
    onOffer: () -> Unit,
    onSeller: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onBuyNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listing = detail.listing
    val mediaUrl = detail.media.firstOrNull()?.url ?: when (listing.id) {
        1L -> "mock-images/mock_image_4.png"
        2L -> "mock-images/mock_image_5.png"
        3L -> "mock-images/mock_image_2.png"
        else -> "mock-images/mock_image_3.png"
    }
    val imageUrl = resolveMediaUrl(apiBase, mediaUrl)
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        ProductDetailHeader(title = stringResource(R.string.market_product_detail), onBack = onBack, onSave = onSave, onShare = onShare)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .background(StitchPalette.Surface),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imageUrl ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png",
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .size(if (index == 0) 9.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0) StitchPalette.Brand else StitchPalette.OutlineVariant),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listing.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = StitchPalette.OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    MarketBadge(stringResource(tradeTypeLabelRes(listing.type)))
                }
                Text(
                    localizedListingPrice(listing.priceCents, listing.currency),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = StitchPalette.Brand,
                )
                Text(stringResource(R.string.market_listed_recently), style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    ProductPill(stringResource(R.string.market_condition_new))
                    ProductPill(stringResource(R.string.market_shipping_available), Icons.Outlined.LocalShipping)
                    ProductPill(stringResource(R.string.market_local_pickup))
                }
                SellerCard(seller = seller, sellerId = listing.sellerId, apiBase = apiBase, loading = loading, onClick = onSeller)
                Card(
                    shape = StitchShape.cardFeed,
                    colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                    border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.market_description), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            listing.description.ifBlank { stringResource(R.string.market_no_description) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = StitchPalette.OnSurfaceVariant,
                        )
                    }
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(StitchPalette.Surface)
                    .border(1.dp, StitchPalette.BorderHairline)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onContact,
                enabled = !actionLoading,
                shape = StitchShape.field,
                modifier = Modifier.weight(0.9f).height(48.dp),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.market_contact))
            }
            OutlinedButton(
                onClick = onOffer,
                enabled = !actionLoading,
                shape = StitchShape.field,
                modifier = Modifier.weight(0.8f).height(48.dp),
                border = BorderStroke(1.dp, StitchPalette.Brand),
            ) {
                Text(stringResource(R.string.market_offer), color = StitchPalette.Brand, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onBuyNow,
                enabled = !actionLoading,
                shape = StitchShape.field,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
            ) {
                Text(if (actionLoading) "..." else stringResource(R.string.market_buy_now), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProductDetailHeader(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(StitchPalette.Surface)
                .border(1.dp, StitchPalette.BorderHairline)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(StitchPalette.SurfaceLow)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = StitchPalette.OnSurface)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSave, modifier = Modifier.size(44.dp).clip(CircleShape).background(StitchPalette.SurfaceLow)) {
            Icon(Icons.Outlined.FavoriteBorder, contentDescription = stringResource(R.string.common_save), tint = StitchPalette.OnSurface)
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onShare, modifier = Modifier.size(44.dp).clip(CircleShape).background(StitchPalette.SurfaceLow)) {
            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.common_share), tint = StitchPalette.OnSurface)
        }
    }
}

@Composable
private fun PublishProductScreen(
    sdk: MeowCircleSdk,
    mockMode: Boolean,
    onClose: () -> Unit,
    onPublished: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var title by remember { mutableStateOf(context.getString(R.string.market_sample_title)) }
    var price by remember { mutableStateOf("128.00") }
    var description by remember { mutableStateOf(context.getString(R.string.market_sample_description)) }
    var category by remember { mutableStateOf("care") }
    var tradeType by remember { mutableStateOf("sell") }
    var condition by remember { mutableStateOf("new") }
    var busy by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(StitchPalette.Surface)) {
        DetailHeader(title = stringResource(R.string.market_publish_title), onBack = onClose, action = stringResource(R.string.market_drafts), close = true)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AddPhotoBox()
                Box(
                    modifier =
                        Modifier
                            .size(92.dp)
                            .clip(StitchShape.cardFeed)
                            .background(StitchPalette.SurfaceLow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = StitchPalette.OnSurfaceVariant)
                }
            }
            PublishField(stringResource(R.string.market_field_title), title, onValueChange = { title = it })
            PublishField(stringResource(R.string.market_field_price), price, onValueChange = { price = it })
            Text(stringResource(R.string.market_field_category), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            OptionRow(MARKET_CATEGORIES, category, onSelect = { category = it })
            Text(stringResource(R.string.market_field_trade_type), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            OptionRow(TRADE_TYPES, tradeType, onSelect = { tradeType = it })
            Text(stringResource(R.string.market_field_condition), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            OptionRow(CONDITIONS, condition, onSelect = { condition = it })
            PublishField(stringResource(R.string.market_field_description), description, onValueChange = { description = it }, minLines = 4)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onClose,
                shape = StitchShape.field,
                modifier = Modifier.weight(1f).height(48.dp),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
            ) {
                Text(stringResource(R.string.market_save_draft), color = StitchPalette.OnSurface)
            }
            Button(
                onClick = {
                    val cents = ((price.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    if (mockMode) {
                        onPublished()
                    } else {
                        busy = true
                        scope.launch {
                            sdk.createListing(
                                type = tradeType,
                                title = title,
                                description = "$description\n${context.getString(R.string.market_backend_meta, category, condition)}",
                                priceCents = cents,
                            ).fold(
                                onSuccess = { onPublished() },
                                onFailure = { e ->
                                    onError((e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl))
                                },
                            )
                            busy = false
                        }
                    }
                },
                enabled = !busy && title.isNotBlank(),
                shape = StitchShape.field,
                modifier = Modifier.weight(1.5f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
            ) {
                Text(if (busy) stringResource(R.string.market_publishing) else stringResource(R.string.market_publish), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MakeOfferScreen(
    listing: Listing,
    apiBase: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSend: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets =
        remember(listing.id) {
            listOf(0.78, 0.86, 0.94)
                .map { ((listing.priceCents * it).toLong() / 100L * 100L).coerceAtLeast(100L) }
                .distinct()
        }
    var offerCents by remember(listing.id) { mutableStateOf(presets.getOrNull(1) ?: listing.priceCents) }
    var delivery by remember(listing.id) { mutableStateOf("pickup") }
    val defaultOfferPrice = formatListingPrice(offerCents, listing.currency)
    val defaultOfferMessage = stringResource(R.string.market_default_offer_message, defaultOfferPrice)
    var message by remember(listing.id, defaultOfferMessage) { mutableStateOf(defaultOfferMessage) }

    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        DetailHeader(title = stringResource(R.string.market_make_offer), onBack = onDismiss, close = true)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Card(
                shape = StitchShape.cardFeed,
                colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    val imageUrl = remember(listing.id, apiBase) {
                        when (listing.id) {
                            1L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                            2L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                            3L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                            else -> {
                                when {
                                    listing.title.contains("罐头") || listing.title.contains("Can") -> 
                                        "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                                    listing.title.contains("上门") || listing.title.contains("铲砂") || listing.title.contains("Feed") -> 
                                        "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                                    listing.title.contains("橘猫") || listing.title.contains("领养") || listing.title.contains("Adopt") -> 
                                        "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                                    else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
                                }
                            }
                        }
                    }
                    Box(Modifier.size(64.dp).clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = listing.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(listing.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.market_listed_price, localizedListingPrice(listing.priceCents, listing.currency)), style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                    }
                }
            }
            Text(stringResource(R.string.market_your_offer), style = MaterialTheme.typography.labelLarge, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black)
            Text(
                formatListingPrice(offerCents, listing.currency),
                style = MaterialTheme.typography.displaySmall,
                color = StitchPalette.Brand,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                presets.forEach { preset ->
                    Surface(
                        shape = StitchShape.pill,
                        color = if (offerCents == preset) StitchPalette.Brand else StitchPalette.Surface,
                        border = BorderStroke(1.dp, if (offerCents == preset) StitchPalette.Brand else StitchPalette.BorderHairline),
                        modifier = Modifier.weight(1f).clickable { offerCents = preset },
                    ) {
                        Text(
                            formatListingPrice(preset, listing.currency),
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (offerCents == preset) Color.White else StitchPalette.OnSurface,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Text(stringResource(R.string.market_delivery_method), style = MaterialTheme.typography.labelLarge, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OfferSegment(
                    label = stringResource(R.string.market_local_pickup),
                    selected = delivery == "pickup",
                    onClick = { delivery = "pickup" },
                    modifier = Modifier.weight(1f),
                )
                OfferSegment(
                    label = stringResource(R.string.market_shipping),
                    selected = delivery == "shipping",
                    onClick = { delivery = "shipping" },
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text(stringResource(R.string.market_offer_message)) },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = StitchShape.field,
                colors = mintTextFieldColors(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(StitchPalette.Surface).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !busy,
                shape = StitchShape.field,
                modifier = Modifier.weight(1f).height(48.dp),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
            ) {
                Text(stringResource(R.string.common_cancel), color = StitchPalette.OnSurface, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onSend(offerCents, message) },
                enabled = !busy,
                shape = StitchShape.field,
                modifier = Modifier.weight(1.4f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
            ) {
                Text(if (busy) stringResource(R.string.market_sending) else stringResource(R.string.market_send_offer), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OfferSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = StitchShape.field,
        color = if (selected) StitchPalette.BrandMuted else StitchPalette.Surface,
        border = BorderStroke(1.dp, if (selected) StitchPalette.Brand else StitchPalette.BorderHairline),
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 13.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun MarketHeroCard(
    listing: Listing,
    featured: Boolean,
    apiBase: String,
    mockMode: Boolean,
    onClick: () -> Unit,
    onBuyNow: () -> Unit,
) {
    val imageUrl = remember(listing.id, mockMode, apiBase) {
        if (mockMode) {
            when (listing.id) {
                1L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                2L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                3L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
            }
        } else {
            when {
                listing.title.contains("罐头") || listing.title.contains("Can") -> 
                    "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                listing.title.contains("上门") || listing.title.contains("铲砂") || listing.title.contains("Feed") -> 
                    "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                listing.title.contains("橘猫") || listing.title.contains("领养") || listing.title.contains("Adopt") -> 
                    "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
        elevation = CardDefaults.cardElevation(defaultElevation = if (featured) StitchShadows.cardAmbientY else 0.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (featured) 1.28f else 1.18f)
                    .background(if (featured) StitchPalette.SurfaceLow else Color(0xFFCFCFCF)),
        ) {
            AsyncImage(
                model = imageUrl ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png",
                contentDescription = listing.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            MarketBadge(
                label = stringResource(tradeTypeLabelRes(listing.type)),
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                icon = true,
            )
            Surface(
                shape = StitchShape.pill,
                color = StitchPalette.Surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, StitchPalette.BorderHairline),
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            ) {
                Text(
                    stringResource(R.string.market_verified_seller),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = StitchPalette.PrimaryDark,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                listing.title,
                style = MaterialTheme.typography.headlineSmall,
                color = StitchPalette.PrimaryDark,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    localizedListingPrice(listing.priceCents, listing.currency),
                    style = MaterialTheme.typography.headlineSmall,
                    color = StitchPalette.PrimaryDark,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onBuyNow,
                    shape = StitchShape.field,
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
                    modifier = Modifier.height(56.dp).width(132.dp),
                ) {
                    Text(stringResource(R.string.market_buy_now), color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun FeaturedMarketCard(
    listing: Listing,
    apiBase: String,
    mockMode: Boolean,
    onClick: () -> Unit,
    onBuyNow: () -> Unit,
) {
    val imageUrl = remember(listing.id, mockMode, apiBase) {
        if (mockMode) {
            when (listing.id) {
                1L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                2L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                3L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
            }
        } else {
            when {
                listing.title.contains("罐头") || listing.title.contains("Can") -> 
                    "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                listing.title.contains("上门") || listing.title.contains("铲砂") || listing.title.contains("Feed") -> 
                    "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                listing.title.contains("橘猫") || listing.title.contains("领养") || listing.title.contains("Adopt") -> 
                    "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
        elevation = CardDefaults.cardElevation(defaultElevation = StitchShadows.cardAmbientY),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.82f).clip(StitchShape.cardFeed).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = imageUrl ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png",
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                MarketBadge(stringResource(tradeTypeLabelRes(listing.type)), Modifier.align(Alignment.TopStart).padding(10.dp))
            }
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = StitchPalette.OnSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    localizedListingPrice(listing.priceCents, listing.currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = StitchPalette.Brand,
                    fontWeight = FontWeight.Black,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(StitchPalette.BrandMuted), contentAlignment = Alignment.Center) {
                    Text("J", color = StitchPalette.Brand, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
                Text("Jordan M.", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(15.dp))
                Text(" ${stringResource(R.string.market_verified_seller)}", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
            }
            Button(
                onClick = onBuyNow,
                shape = StitchShape.field,
                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
                Text(stringResource(R.string.market_buy_now), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompactMarketCard(
    listing: Listing,
    apiBase: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val imageUrl = remember(listing.id, apiBase) {
                when (listing.id) {
                    1L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                    2L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                    3L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                    else -> {
                        when {
                            listing.title.contains("罐头") || listing.title.contains("Can") -> 
                                "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                            listing.title.contains("上门") || listing.title.contains("铲砂") || listing.title.contains("Feed") -> 
                                "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                            listing.title.contains("橘猫") || listing.title.contains("领养") || listing.title.contains("Adopt") -> 
                                "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                            else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
                        }
                    }
                }
            }
            Box(Modifier.size(92.dp).clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.padding(start = 14.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(listing.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(localizedListingPrice(listing.priceCents, listing.currency), style = MaterialTheme.typography.titleMedium, color = StitchPalette.Brand, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Verified, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(14.dp))
                    Text(" ${stringResource(R.string.market_verified_seller)}", style = MaterialTheme.typography.labelMedium, color = StitchPalette.Brand)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = stringResource(R.string.common_save), tint = StitchPalette.OnSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                MarketBadge(stringResource(tradeTypeLabelRes(listing.type)))
            }
        }
    }
}

@Composable
private fun DetailHeader(
    title: String,
    onBack: () -> Unit,
    action: String? = null,
    close: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(StitchPalette.Surface)
                .border(1.dp, StitchPalette.BorderHairline)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
            Icon(
                if (close) Icons.Outlined.Close else Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = if (close) stringResource(R.string.common_close) else stringResource(R.string.common_back),
                tint = StitchPalette.OnSurface,
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            Text(action, color = StitchPalette.Brand, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        } else {
            Spacer(Modifier.width(44.dp))
        }
    }
}

@Composable
private fun OptionRow(
    options: List<MarketOption>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val active = selected == option.key
            Surface(
                shape = StitchShape.pill,
                color = if (active) StitchPalette.Brand else StitchPalette.Surface,
                border = BorderStroke(1.dp, if (active) StitchPalette.Brand else StitchPalette.BorderHairline),
                modifier = Modifier.clickable { onSelect(option.key) },
            ) {
                Text(
                    stringResource(option.labelRes),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Color.White else StitchPalette.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MarketEmptyState(
    title: String,
    body: String,
    primary: String,
    secondary: String?,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?,
) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(82.dp).clip(CircleShape).background(StitchPalette.BrandMuted), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = StitchPalette.OnSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPrimary, shape = StitchShape.field, colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(primary, color = Color.White, fontWeight = FontWeight.Bold)
        }
        if (secondary != null && onSecondary != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onSecondary, shape = StitchShape.field, modifier = Modifier.fillMaxWidth().height(50.dp), border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                Text(secondary, color = StitchPalette.OnSurface)
            }
        }
    }
}

@Composable
private fun SellerProfileScreen(
    seller: User,
    listings: List<Listing>,
    apiBase: String,
    onBack: () -> Unit,
    onMessageSeller: () -> Unit,
    onSelectListing: (Listing) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = seller.nickname.ifBlank { seller.username.ifBlank { stringResource(R.string.market_seller_id, seller.id) } }
    Column(modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        DetailHeader(title = stringResource(R.string.market_seller_profile), onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val avatarUrl = resolveMediaUrl(apiBase, seller.avatarUrl.takeIf { it.isNotBlank() })
                        ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_1.png"
                    Box(
                        modifier =
                            Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(StitchPalette.BrandMuted)
                                .border(3.dp, StitchPalette.Brand, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(displayName, style = MaterialTheme.typography.headlineSmall, color = StitchPalette.PrimaryDark, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.Verified, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(20.dp))
                    }
                    Text(stringResource(R.string.market_seller_location), style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = StitchShape.pill, color = StitchPalette.BrandMuted) {
                        Text(
                            stringResource(R.string.market_verified_seller),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = StitchPalette.Brand,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        SellerStat(value = "124", label = stringResource(R.string.market_sales), modifier = Modifier.weight(1f))
                        SellerStat(value = "4.8", label = stringResource(R.string.market_rating), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onMessageSeller,
                        shape = StitchShape.field,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.market_message_seller), color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.market_active_listings), style = MaterialTheme.typography.titleMedium, color = StitchPalette.PrimaryDark, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    Surface(shape = StitchShape.pill, color = StitchPalette.Surface, border = BorderStroke(1.dp, StitchPalette.BorderHairline)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.FilterList, contentDescription = null, tint = StitchPalette.OnSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.market_filter), style = MaterialTheme.typography.labelMedium, color = StitchPalette.OnSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            listings.chunked(2).forEach { rowListings ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowListings.forEach { listing ->
                            SellerListingCard(
                                listing = listing,
                                apiBase = apiBase,
                                onClick = { onSelectListing(listing) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowListings.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = StitchShape.field,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = StitchPalette.PrimaryDark, fontWeight = FontWeight.Black)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = StitchPalette.OnSurfaceVariant, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SellerListingCard(
    listing: Listing,
    apiBase: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageUrl = remember(listing.id, apiBase) {
        when (listing.id) {
            1L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
            2L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
            3L -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
            else -> {
                when {
                    listing.title.contains("罐头") || listing.title.contains("Can") -> 
                        "${apiBase.removeSuffix("/")}/mock-images/mock_image_4.png"
                    listing.title.contains("上门") || listing.title.contains("铲砂") || listing.title.contains("Feed") -> 
                        "${apiBase.removeSuffix("/")}/mock-images/mock_image_5.png"
                    listing.title.contains("橘猫") || listing.title.contains("领养") || listing.title.contains("Adopt") -> 
                        "${apiBase.removeSuffix("/")}/mock-images/mock_image_2.png"
                    else -> "${apiBase.removeSuffix("/")}/mock-images/mock_image_3.png"
                }
            }
        }
    }
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(StitchPalette.SurfaceLow),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(listing.title, style = MaterialTheme.typography.labelLarge, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(localizedListingPrice(listing.priceCents, listing.currency), style = MaterialTheme.typography.titleSmall, color = StitchPalette.Brand, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SellerCard(
    seller: User?,
    sellerId: Long,
    apiBase: String,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val avatarUrl = remember(seller?.id, apiBase) {
        resolveMediaUrl(apiBase, seller?.avatarUrl?.takeIf { it.isNotBlank() })
            ?: "${apiBase.removeSuffix("/")}/mock-images/mock_image_1.png"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(StitchPalette.BrandMuted), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = seller?.nickname,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(seller?.nickname?.ifBlank { seller.username } ?: stringResource(R.string.market_seller_id, sellerId), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Verified, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(15.dp))
                Text(
                    if (loading) " ${stringResource(R.string.market_loading_seller)}" else " ${stringResource(R.string.market_verified_seller_rating)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchPalette.OnSurfaceVariant,
                )
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = StitchPalette.OnSurfaceVariant)
        }
    }
}

@Composable
private fun ProductPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        shape = StitchShape.pill,
        color = StitchPalette.Surface,
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = StitchPalette.OnSurfaceVariant, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = StitchPalette.OnSurfaceVariant, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MarketBadge(
    label: String,
    modifier: Modifier = Modifier,
    icon: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = StitchShape.pill,
        color = StitchPalette.Brand,
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon) {
                Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ProductImagePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.clip(StitchShape.field).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.common_img), color = StitchPalette.OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AddPhotoBox() {
    Box(
        modifier =
            Modifier
                .size(92.dp)
                .clip(StitchShape.cardFeed)
                .background(StitchPalette.BrandMuted)
                .border(1.dp, StitchPalette.Brand, StitchShape.cardFeed),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = StitchPalette.Brand)
            Text(stringResource(R.string.market_add_photo), style = MaterialTheme.typography.labelMedium, color = StitchPalette.Brand, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PublishField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = StitchPalette.OnSurface)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            minLines = minLines,
            modifier = Modifier.fillMaxWidth(),
            shape = StitchShape.field,
            colors = mintTextFieldColors(),
        )
    }
}

@Composable
private fun mintTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = StitchPalette.Brand,
        unfocusedBorderColor = StitchPalette.BorderHairline,
        cursorColor = StitchPalette.Brand,
        focusedContainerColor = StitchPalette.Surface,
        unfocusedContainerColor = StitchPalette.Surface,
    )

private fun Listing.matchesTradeType(key: String): Boolean =
    when (key) {
        "sell" -> type in listOf("sell", "product", "service") && priceCents > 0L
        "trade" -> type == "trade"
        "looking_for" -> type == "looking_for"
        "free" -> type in listOf("free", "adopt") || priceCents <= 0L
        else -> true
    }

private fun Listing.matchesCategory(key: String): Boolean {
    val haystack = "$title $description $type".lowercase()
    return when (key) {
        "toys" -> haystack.contains("toy") || haystack.contains("collar") || haystack.contains("harness") || haystack.contains("carrier") || haystack.contains("product")
        "food" -> haystack.contains("food") || haystack.contains("bowl") || haystack.contains("treat") || haystack.contains("can")
        "apparel" -> haystack.contains("apparel") || haystack.contains("wear") || haystack.contains("collar") || haystack.contains("harness")
        "care" -> haystack.contains("care") || haystack.contains("service") || haystack.contains("feed") || haystack.contains("adopt")
        else -> true
    }
}

private fun tradeTypeLabelRes(type: String): Int =
    when (type) {
        "sell", "product", "service" -> R.string.market_trade_sell
        "trade" -> R.string.market_trade_trade
        "looking_for" -> R.string.market_trade_looking_for
        "free", "adopt" -> R.string.market_trade_free
        else -> R.string.market_trade_sell
    }

private fun formatListingPrice(
    cents: Long,
    currency: String,
): String {
    if (cents <= 0L) return "Free"
    val symbol = if (currency.uppercase() == "CNY") "$" else currency.uppercase()
    val amount = cents / 100.0
    return "$symbol${amount.toString().trimEnd('0').trimEnd('.')}"
}

@Composable
private fun localizedListingPrice(
    cents: Long,
    currency: String,
): String = if (cents <= 0L) stringResource(R.string.common_free) else formatListingPrice(cents, currency)

private fun mockSeller(id: Long): User =
    User(
        id = id,
        username = "jordan_m",
        nickname = "Jordan M.",
        avatarUrl = "",
        bio = "Verified Seller",
        createdAt = "2026-06-01T00:00:00Z",
    )
