package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ttkk0000.meowcircle.AdoptionPetDetailData
import com.ttkk0000.meowcircle.AdoptionPet
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape

@Composable
fun StitchAdoptionDetailScreen(
    sdk: MeowCircleSdk,
    apiBase: String,
    petId: Long,
    onBack: () -> Unit,
    onApply: (Long) -> Unit,
) {
    var detail by remember { mutableStateOf<AdoptionPetDetailData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(petId) {
        sdk.getAdoptionPet(petId).fold(
            onSuccess = { detail = it },
            onFailure = { error = it.message ?: "Unable to load adoption details." },
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(StitchPalette.Canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = StitchPalette.OnSurface)
            }
            Text("领养详情", style = MaterialTheme.typography.titleLarge, color = StitchPalette.OnSurface, fontWeight = FontWeight.Bold)
        }
        when {
            detail == null && error == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StitchPalette.Brand)
            }
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(error.orEmpty(), color = StitchPalette.Error)
            }
            else -> {
                val pet = detail!!.pet
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    pet.mediaIds?.firstOrNull()?.let { mediaId ->
                        AsyncImage(
                            model = "${apiBase.removeSuffix("/")}/api/v1/media/${mediaId}/content",
                            contentDescription = pet.name,
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(StitchShape.cardFeed),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(pet.name, style = MaterialTheme.typography.headlineMedium, color = StitchPalette.OnSurface, fontWeight = FontWeight.Black)
                    Text("${pet.species} · ${pet.breed} · ${pet.age}", style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = StitchPalette.OnSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(pet.city, style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
                    }
                    Surface(shape = StitchShape.pill, color = StitchPalette.SurfaceLow) {
                        Text(pet.health, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = StitchPalette.OnSurfaceVariant)
                    }
                    Text(pet.description, style = MaterialTheme.typography.bodyLarge, color = StitchPalette.OnSurface)
                    detail!!.rescuer?.let { rescuer ->
                        Text("救助人：${rescuer.nickname.ifBlank { rescuer.username }}", style = MaterialTheme.typography.bodyMedium, color = StitchPalette.OnSurfaceVariant)
                    }
                }
                Button(
                    onClick = { onApply(petId) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = StitchShape.field,
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPalette.Brand),
                ) {
                    Text("申请领养", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
