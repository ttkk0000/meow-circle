package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.AdoptionApplication
import com.ttkk0000.meowcircle.AdoptionPet
import com.ttkk0000.meowcircle.AdoptionPetDetailData
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBar
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBarLeading
import com.ttkk0000.meowcircle.kmpapp.ui.components.StitchTopBarTrailing
import kotlinx.coroutines.launch

@Composable
fun StitchAdoptHomeTab(
    sdk: MeowCircleSdk,
    apiBase: String,
    user: User,
    onAvatarPress: () -> Unit,
    onNotifyPress: () -> Unit,
    onPetClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var pets by remember { mutableStateOf<List<AdoptionPet>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var err by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("all") }
    var detail by remember { mutableStateOf<AdoptionPetDetailData?>(null) }
    var detailLoading by remember { mutableStateOf(false) }
    var applyPet by remember { mutableStateOf<AdoptionPet?>(null) }

    fun loadPets() {
        loading = true
        err = null
        scope.launch {
            val species = when (filter) {
                "cat" -> "cat"
                "dog" -> "dog"
                else -> null
            }
            sdk.getAdoptionPets(species = species, status = "available").fold(
                onSuccess = { pets = it },
                onFailure = { err = humanizeClientFailure(it, sdk.baseUrl) },
            )
            loading = false
        }
    }

    LaunchedEffect(filter) {
        loadPets()
    }

    Column(modifier = modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        StitchTopBar(
            apiBase = apiBase,
            user = user,
            title = "领养中心",
            leading = StitchTopBarLeading.Menu,
            trailing = StitchTopBarTrailing.Bell,
            onAvatarPress = onAvatarPress,
            onNotifyPress = onNotifyPress,
        )

        AdoptionHeroSummary(
            pets = pets.orEmpty(),
            loading = loading,
            filter = filter,
            onFilterChange = { filter = it },
        )

        Box(Modifier.weight(1f)) {
            when {
                loading && pets == null -> AdoptionLoadingPane()
                err != null && pets == null -> AdoptionErrorPane(err = err.orEmpty(), onRetry = ::loadPets)
                pets.orEmpty().isEmpty() -> AdoptionEmptyPane(
                    title = "暂无可领养信息",
                    body = "救助人发布新信息后会出现在这里。",
                    icon = Icons.Outlined.Pets,
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(pets.orEmpty(), key = { it.id }) { pet ->
                            AdoptionPetCard(
                                apiBase = apiBase,
                                pet = pet,
                                onClick = {
                                    onPetClick(pet.id)
                                    detailLoading = true
                                    detail = AdoptionPetDetailData(pet = pet, rescuer = null)
                                    scope.launch {
                                        sdk.getAdoptionPet(pet.id).fold(
                                            onSuccess = { detail = it },
                                            onFailure = { detail = AdoptionPetDetailData(pet = pet, rescuer = null) },
                                        )
                                        detailLoading = false
                                    }
                                },
                                onApply = { applyPet = pet },
                            )
                        }
                    }
                }
            }
        }
    }

    detail?.let { current ->
        AdoptionPetDetailDialog(
            apiBase = apiBase,
            detail = current,
            loading = detailLoading,
            onDismiss = { detail = null },
            onApply = {
                applyPet = current.pet
                detail = null
            },
        )
    }

    applyPet?.let { pet ->
        AdoptionApplyDialog(
            sdk = sdk,
            pet = pet,
            onDismiss = { applyPet = null },
            onSubmitted = {
                applyPet = null
                loadPets()
            },
        )
    }
}

@Composable
fun StitchAdoptionRescueTab(
    sdk: MeowCircleSdk,
    apiBase: String,
    user: User,
    onAvatarPress: () -> Unit,
    onNotifyPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pets by remember { mutableStateOf<List<AdoptionPet>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        sdk.getAdoptionPets(status = "available").fold(
            onSuccess = { pets = it },
            onFailure = { err = humanizeClientFailure(it, sdk.baseUrl) },
        )
    }

    Column(modifier = modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        StitchTopBar(
            apiBase = apiBase,
            user = user,
            title = "救助协作",
            leading = StitchTopBarLeading.Menu,
            trailing = StitchTopBarTrailing.Bell,
            onAvatarPress = onAvatarPress,
            onNotifyPress = onNotifyPress,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                RescueSummaryCard(pets = pets.orEmpty(), loading = pets == null && err == null)
            }
            item {
                RescueChecklistCard()
            }
            if (err != null) {
                item { AdoptionErrorPane(err = err.orEmpty(), onRetry = {}) }
            } else {
                items(pets.orEmpty().take(4), key = { it.id }) { pet ->
                    RescuePetRow(apiBase = apiBase, pet = pet)
                }
            }
        }
    }
}

@Composable
fun StitchAdoptionApplicationsTab(
    sdk: MeowCircleSdk,
    apiBase: String,
    user: User,
    onAvatarPress: () -> Unit,
    onNotifyPress: () -> Unit,
    onBrowsePets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var applications by remember { mutableStateOf<List<AdoptionApplication>?>(null) }
    var pets by remember { mutableStateOf<List<AdoptionPet>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        sdk.getMyAdoptionApplications().fold(
            onSuccess = { applications = it },
            onFailure = { err = humanizeClientFailure(it, sdk.baseUrl) },
        )
        sdk.getAdoptionPets().fold(
            onSuccess = { pets = it },
            onFailure = {},
        )
    }

    Column(modifier = modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        StitchTopBar(
            apiBase = apiBase,
            user = user,
            title = "领养申请",
            leading = StitchTopBarLeading.Menu,
            trailing = StitchTopBarTrailing.Bell,
            onAvatarPress = onAvatarPress,
            onNotifyPress = onNotifyPress,
        )

        Box(Modifier.weight(1f)) {
            when {
                applications == null && err == null -> AdoptionLoadingPane()
                err != null && applications == null -> AdoptionErrorPane(
                    err = err.orEmpty(),
                    onRetry = {
                        err = null
                        applications = null
                        scope.launch {
                            sdk.getMyAdoptionApplications().fold(
                                onSuccess = { applications = it },
                                onFailure = { err = humanizeClientFailure(it, sdk.baseUrl) },
                            )
                        }
                    },
                )
                applications.orEmpty().isEmpty() -> {
                    AdoptionEmptyPane(
                        title = "还没有申请记录",
                        body = "看到合适的猫猫或 doggie 后，可以从领养中心提交申请。",
                        icon = Icons.Outlined.Article,
                        actionText = "去看看",
                        onAction = onBrowsePets,
                    )
                }
                else -> {
                    val petMap = pets.associateBy { it.id }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            ApplicationsSummaryCard(applications.orEmpty())
                        }
                        items(applications.orEmpty(), key = { it.id }) { app ->
                            AdoptionApplicationCard(
                                application = app,
                                pet = petMap[app.petId],
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdoptionHeroSummary(
    pets: List<AdoptionPet>,
    loading: Boolean,
    filter: String,
    onFilterChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = StitchPalette.Surface,
            shape = StitchShape.cardFeed,
            border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = StitchPalette.Brand)
                    Column(Modifier.weight(1f)) {
                        Text("先了解，再领养", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = StitchPalette.OnSurface)
                        Text("资料、健康状态和申请流程分开呈现，避免和交易市集混在一起。", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdoptionMetric(label = "可领养", value = if (loading) "--" else pets.size.toString(), modifier = Modifier.weight(1f))
                    AdoptionMetric(label = "城市", value = if (loading) "--" else pets.map { it.city }.distinct().size.toString(), modifier = Modifier.weight(1f))
                    AdoptionMetric(label = "猫猫", value = if (loading) "--" else pets.count { it.species.contains("cat", true) }.toString(), modifier = Modifier.weight(1f))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "全部", "cat" to "猫猫", "dog" to "doggie").forEach { (key, label) ->
                AdoptionFilterChip(label = label, selected = filter == key, onClick = { onFilterChange(key) })
            }
        }
    }
}

@Composable
private fun AdoptionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = StitchPalette.SurfaceLow,
        shape = StitchShape.neutralCard,
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = StitchPalette.Brand)
            Text(label, style = MaterialTheme.typography.labelSmall, color = StitchPalette.OnSurfaceVariant)
        }
    }
}

@Composable
private fun AdoptionFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = StitchShape.pill,
        color = if (selected) StitchPalette.Brand else StitchPalette.Surface,
        border = BorderStroke(1.dp, if (selected) StitchPalette.Brand else StitchPalette.OutlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else StitchPalette.OnSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
        )
    }
}

@Composable
fun AdoptionPetCard(
    apiBase: String,
    pet: AdoptionPet,
    onClick: () -> Unit,
    onApply: () -> Unit,
) {
    Card(
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column {
            AdoptionPetImage(apiBase = apiBase, pet = pet, height = 178)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = pet.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = StitchPalette.OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${pet.speciesLabel()} · ${pet.breed} · ${pet.age}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StitchPalette.OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AdoptionStatusBadge(pet.status)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp), tint = StitchPalette.OnSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(pet.city, style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text(pet.health, style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    pet.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchPalette.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onApply,
                        shape = StitchShape.pill,
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                        modifier = Modifier.height(38.dp),
                    ) {
                        Text("申请领养", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdoptionPetImage(apiBase: String, pet: AdoptionPet, height: Int) {
    val firstMedia = pet.mediaIds?.firstOrNull()
    if (firstMedia != null) {
        AsyncImage(
            model = "${apiBase.removeSuffix("/")}/api/v1/media/$firstMedia/content",
            contentDescription = pet.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(StitchShape.cardFeedTop),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .background(StitchPalette.SurfaceLow)
                .clip(StitchShape.cardFeedTop),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Pets, contentDescription = null, modifier = Modifier.size(48.dp), tint = StitchPalette.OnSurfaceVariant)
        }
    }
}

@Composable
private fun AdoptionStatusBadge(status: String) {
    val available = status.equals("available", ignoreCase = true)
    Surface(
        shape = StitchShape.pill,
        color = if (available) StitchPalette.BrandMuted else StitchPalette.SurfaceLow,
    ) {
        Text(
            text = if (available) "可申请" else adoptionStatusLabel(status),
            color = if (available) StitchPalette.Brand else StitchPalette.OnSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun AdoptionPetDetailDialog(
    apiBase: String,
    detail: AdoptionPetDetailData,
    loading: Boolean,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(detail.pet.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${detail.pet.speciesLabel()} · ${detail.pet.city}", style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AdoptionPetImage(apiBase = apiBase, pet = detail.pet, height = 150)
                if (loading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = StitchPalette.Brand)
                        Text("正在同步救助人信息", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                    }
                }
                DetailLine(label = "基本信息", value = "${detail.pet.breed} · ${detail.pet.age}")
                DetailLine(label = "健康状态", value = detail.pet.health)
                DetailLine(label = "救助人", value = detail.rescuer?.let { it.nickname.ifBlank { it.username } } ?: "资料同步中")
                DetailLine(label = "说明", value = detail.pet.description)
            }
        },
        confirmButton = {
            Button(onClick = onApply, shape = StitchShape.pill, colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand, contentColor = Color.White)) {
                Text("申请领养")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        shape = StitchShape.dialog,
        containerColor = StitchPalette.Surface,
    )
}

@Composable
private fun AdoptionApplyDialog(
    sdk: MeowCircleSdk,
    pet: AdoptionPet,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var message by remember(pet.id) { mutableStateOf("我想了解 ${pet.name} 的领养要求，可以提供稳定室内环境并配合回访。") }
    var contact by remember(pet.id) { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("申请领养 ${pet.name}", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("请补充联系方式和家庭情况，救助人确认后会通过消息联系你。", style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("申请说明") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = adoptionTextFieldColors(),
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("联系方式") },
                    singleLine = true,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = adoptionTextFieldColors(),
                )
                feedback?.let {
                    Text(it, color = StitchPalette.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (message.trim().isBlank() || contact.trim().isBlank()) {
                        feedback = "申请说明和联系方式都需要填写。"
                        return@Button
                    }
                    submitting = true
                    feedback = null
                    scope.launch {
                        sdk.applyForAdoption(pet.id, message, contact).fold(
                            onSuccess = { onSubmitted() },
                            onFailure = {
                                val raw = (it as? ApiException)?.message ?: humanizeClientFailure(it, sdk.baseUrl)
                                feedback = raw
                            },
                        )
                        submitting = false
                    }
                },
                enabled = !submitting,
                shape = StitchShape.pill,
                colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand, contentColor = Color.White),
            ) {
                Text(if (submitting) "提交中" else "提交申请")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("取消") }
        },
        shape = StitchShape.dialog,
        containerColor = StitchPalette.Surface,
    )
}

@Composable
private fun adoptionTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StitchPalette.Brand,
    unfocusedBorderColor = StitchPalette.OutlineVariant,
    focusedLabelColor = StitchPalette.Brand,
    cursorColor = StitchPalette.Brand,
)

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = StitchPalette.OnSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurface)
    }
}

@Composable
private fun RescueSummaryCard(pets: List<AdoptionPet>, loading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MedicalServices, contentDescription = null, tint = StitchPalette.Brand)
                Column(Modifier.weight(1f)) {
                    Text("同城救助看板", style = MaterialTheme.typography.titleMedium, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black)
                    Text("把待领养、健康观察和回访提醒集中管理。", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdoptionMetric("待领养", if (loading) "--" else pets.size.toString(), modifier = Modifier.weight(1f))
                AdoptionMetric("猫猫", if (loading) "--" else pets.count { it.species.contains("cat", true) }.toString(), modifier = Modifier.weight(1f))
                AdoptionMetric("doggie", if (loading) "--" else pets.count { it.species.contains("dog", true) }.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RescueChecklistCard() {
    Card(
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = StitchPalette.Brand)
                Text("救助发布前检查", style = MaterialTheme.typography.titleMedium, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black)
            }
            listOf("确认健康状态和免疫记录", "补齐清晰近照与城市信息", "说明回访、押金或绝育约定", "领养申请统一走站内记录").forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = StitchShape.pill, color = StitchPalette.BrandMuted) {
                        Text((index + 1).toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = StitchPalette.Brand, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(item, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurface)
                }
            }
        }
    }
}

@Composable
private fun RescuePetRow(apiBase: String, pet: AdoptionPet) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = StitchShape.cardFeed,
        color = StitchPalette.Surface,
        border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp).clip(StitchShape.neutralCard)) {
                AdoptionPetImage(apiBase = apiBase, pet = pet, height = 72)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pet.name, style = MaterialTheme.typography.titleSmall, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black)
                Text("${pet.city} · ${pet.health}", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            AdoptionStatusBadge(pet.status)
        }
    }
}

@Composable
private fun ApplicationsSummaryCard(apps: List<AdoptionApplication>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdoptionMetric(label = "全部", value = apps.size.toString(), modifier = Modifier.weight(1f))
            AdoptionMetric(label = "审核中", value = apps.count { it.status == "submitted" || it.status == "reviewing" }.toString(), modifier = Modifier.weight(1f))
            AdoptionMetric(label = "已通过", value = apps.count { it.status == "approved" }.toString(), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdoptionApplicationCard(application: AdoptionApplication, pet: AdoptionPet?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = StitchShape.cardFeed,
        colors = CardDefaults.cardColors(containerColor = StitchPalette.Surface),
        border = BorderStroke(1.dp, StitchPalette.OutlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pet?.name ?: "领养信息 #${application.petId}", style = MaterialTheme.typography.titleMedium, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black)
                    Text(pet?.let { "${it.city} · ${it.speciesLabel()}" } ?: "详情同步中", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
                }
                ApplicationStatusBadge(application.status)
            }
            HorizontalDivider(color = StitchPalette.OutlineVariant)
            Text(application.message, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
            application.contactInfo?.takeIf { it.isNotBlank() }?.let {
                Text("联系方式：$it", style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ApplicationStatusBadge(status: String) {
    Surface(shape = StitchShape.pill, color = StitchPalette.BrandMuted) {
        Text(
            text = applicationStatusLabel(status),
            color = StitchPalette.Brand,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun AdoptionLoadingPane() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = StitchPalette.Brand)
    }
}

@Composable
private fun AdoptionErrorPane(err: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(err, color = StitchPalette.Error, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun AdoptionEmptyPane(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(58.dp), tint = StitchPalette.OnSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            Button(onClick = onAction, shape = StitchShape.pill, colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand, contentColor = Color.White)) {
                Text(actionText)
            }
        }
    }
}

private fun AdoptionPet.speciesLabel(): String =
    when {
        species.contains("cat", ignoreCase = true) -> "猫猫"
        species.contains("dog", ignoreCase = true) -> "doggie"
        else -> species
    }

private fun adoptionStatusLabel(status: String): String =
    when (status.lowercase()) {
        "available" -> "可申请"
        "adopted" -> "已领养"
        "matched" -> "已匹配"
        "paused" -> "暂停"
        else -> status.ifBlank { "未知" }
    }

private fun applicationStatusLabel(status: String): String =
    when (status.lowercase()) {
        "submitted" -> "已提交"
        "reviewing" -> "审核中"
        "needs_info" -> "待补充"
        "approved" -> "已通过"
        "rejected" -> "未通过"
        "withdrawn" -> "已撤回"
        else -> status.ifBlank { "未知" }
    }
