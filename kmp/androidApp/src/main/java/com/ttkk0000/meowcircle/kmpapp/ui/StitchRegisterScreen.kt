package com.ttkk0000.meowcircle.kmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ttkk0000.meowcircle.ApiException
import com.ttkk0000.meowcircle.MeowCircleSdk
import com.ttkk0000.meowcircle.User
import com.ttkk0000.meowcircle.humanizeClientFailure
import com.ttkk0000.meowcircle.kmpapp.R
import com.ttkk0000.meowcircle.kmpapp.theme.StitchLoginRef
import com.ttkk0000.meowcircle.kmpapp.theme.StitchPalette
import com.ttkk0000.meowcircle.kmpapp.theme.StitchShape
import kotlinx.coroutines.launch

private data class RegisterOption(
    val key: String,
    val titleRes: Int,
    val noteRes: Int,
    val icon: ImageVector,
)

private val REGISTER_ROLES =
    listOf(
        RegisterOption("cat_parent", R.string.register_role_cat_parent, R.string.register_role_cat_parent_note, Icons.Filled.Pets),
        RegisterOption("rescuer", R.string.register_role_rescuer, R.string.register_role_rescuer_note, Icons.Outlined.Groups),
        RegisterOption("market", R.string.register_role_market, R.string.register_role_market_note, Icons.Outlined.ShoppingBag),
    )

private val REGISTER_TAGS =
    listOf(
        "cats" to R.string.register_tag_cats,
        "market" to R.string.register_tag_market,
        "walks" to R.string.register_tag_walks,
        "care" to R.string.register_tag_care,
        "adoption" to R.string.register_tag_adoption,
    )

/** M&D mobile register: Create Account plus the three onboarding frames from the app board. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchRegisterScreen(
    sdk: MeowCircleSdk,
    onBack: () -> Unit,
    onRegistered: (User) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }
    var username by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("cat_parent") }
    var petName by remember { mutableStateOf("Latte") }
    val defaultPetType = stringResource(R.string.register_pet_type_default)
    val defaultPetBio = stringResource(R.string.register_pet_bio_default)
    var petType by remember(defaultPetType) { mutableStateOf(defaultPetType) }
    var petBio by remember(defaultPetBio) { mutableStateOf(defaultPetBio) }
    var selectedTags by remember { mutableStateOf(setOf("cats", "care")) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val usernameShortError = stringResource(R.string.register_username_short)
    val passwordShortError = stringResource(R.string.register_password_short)
    val passwordMismatchError = stringResource(R.string.register_password_mismatch)

    fun validateAccount(): Boolean {
        err =
            when {
                username.trim().length < 3 -> usernameShortError
                password.length < 6 -> passwordShortError
                password != confirm -> passwordMismatchError
                else -> null
            }
        return err == null
    }

    fun finishRegistration() {
        if (!validateAccount()) {
            step = 0
            return
        }
        scope.launch {
            busy = true
            sdk
                .register(
                    username = username.trim(),
                    password = password,
                    nickname = nickname.trim().ifBlank { petName.trim() },
                    phone = phone.trim(),
                    smsCode = smsCode.trim(),
                )
                .fold(
                    onSuccess = { onRegistered(it) },
                    onFailure = { e ->
                        err = (e as? ApiException)?.message ?: humanizeClientFailure(e, sdk.baseUrl)
                        step = 0
                    },
                )
            busy = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StitchLoginRef.Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "M&D",
                        fontWeight = FontWeight.Bold,
                        color = StitchLoginRef.PrimaryContainer,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (step == 0) onBack() else step -= 1
                        },
                        enabled = !busy,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    Spacer(Modifier.size(48.dp))
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = StitchLoginRef.Background,
                        titleContentColor = StitchLoginRef.PrimaryContainer,
                        navigationIconContentColor = StitchLoginRef.OnSurface,
                    ),
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OnboardingMark()
            when (step) {
                0 ->
                    RegisterAccountStep(
                        username = username,
                        onUsernameChange = { username = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        smsCode = smsCode,
                        onSmsCodeChange = { smsCode = it },
                        nickname = nickname,
                        onNicknameChange = { nickname = it },
                        password = password,
                        onPasswordChange = { password = it },
                        confirm = confirm,
                        onConfirmChange = { confirm = it },
                        error = err,
                        busy = busy,
                        onNext = {
                            if (validateAccount()) step = 1
                        },
                        onBack = onBack,
                    )
                1 ->
                    RegisterChoiceStep(
                        title = stringResource(R.string.register_role_title),
                        subtitle = stringResource(R.string.register_role_subtitle),
                        options = REGISTER_ROLES,
                        selected = role,
                        onSelect = { role = it },
                        onNext = { step = 2 },
                    )
                2 ->
                    RegisterPetStep(
                        petName = petName,
                        onPetNameChange = { petName = it },
                        petType = petType,
                        onPetTypeChange = { petType = it },
                        petBio = petBio,
                        onPetBioChange = { petBio = it },
                        onNext = { step = 3 },
                    )
                else ->
                    RegisterFeedStep(
                        selectedTags = selectedTags,
                        onToggleTag = { tag ->
                            selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                        },
                        busy = busy,
                        onFinish = ::finishRegistration,
                    )
            }
        }
    }
}

@Composable
private fun OnboardingMark() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "M&D",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = StitchLoginRef.PrimaryContainer,
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(start = 82.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(StitchPalette.SecondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Pets, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(18.dp))
            }
        }
        Text(stringResource(R.string.register_subtitle), style = MaterialTheme.typography.bodyMedium, color = StitchLoginRef.Outline)
    }
}

@Composable
private fun RegisterAccountStep(
    username: String,
    onUsernameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirm: String,
    onConfirmChange: (String) -> Unit,
    error: String?,
    busy: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Text(
        stringResource(R.string.register_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = StitchLoginRef.PrimaryContainer,
    )
    RegField(username, onUsernameChange, stringResource(R.string.register_username), Icons.Outlined.Person, Modifier.fillMaxWidth())
    RegField(phone, onPhoneChange, stringResource(R.string.register_phone), Icons.Outlined.Smartphone, Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        RegField(smsCode, onSmsCodeChange, stringResource(R.string.register_sms), Icons.Outlined.MarkEmailRead, Modifier.weight(1f))
        Text(stringResource(R.string.register_sms_optional), style = MaterialTheme.typography.labelMedium, color = StitchLoginRef.Outline)
    }
    RegField(nickname, onNicknameChange, stringResource(R.string.register_nickname_optional), Icons.Outlined.Badge, Modifier.fillMaxWidth())
    RegField(password, onPasswordChange, stringResource(R.string.register_password), Icons.Outlined.Lock, Modifier.fillMaxWidth(), isPassword = true)
    RegField(confirm, onConfirmChange, stringResource(R.string.register_confirm_password), Icons.Outlined.Lock, Modifier.fillMaxWidth(), isPassword = true)
    error?.let {
        Text(it, color = StitchPalette.Error, style = MaterialTheme.typography.bodySmall)
    }
    PrimaryRegisterButton(text = stringResource(R.string.register_next), busy = busy, onClick = onNext)
    Text(stringResource(R.string.register_agreement), style = MaterialTheme.typography.labelSmall, color = StitchLoginRef.Outline)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.register_have_account), style = MaterialTheme.typography.bodyMedium, color = StitchLoginRef.Outline)
        Spacer(Modifier.size(4.dp))
        Text(
            stringResource(R.string.register_login_now),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = StitchLoginRef.PrimaryContainer,
            modifier = Modifier.clickable(enabled = !busy) { onBack() },
        )
    }
}

@Composable
private fun RegisterChoiceStep(
    title: String,
    subtitle: String,
    options: List<RegisterOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit,
) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = StitchLoginRef.PrimaryContainer)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = StitchLoginRef.Outline)
    options.forEach { option ->
        RegisterOptionCard(option = option, selected = selected == option.key, onClick = { onSelect(option.key) })
    }
    PrimaryRegisterButton(text = stringResource(R.string.register_next), busy = false, onClick = onNext)
}

@Composable
private fun RegisterPetStep(
    petName: String,
    onPetNameChange: (String) -> Unit,
    petType: String,
    onPetTypeChange: (String) -> Unit,
    petBio: String,
    onPetBioChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    Text(stringResource(R.string.register_pet_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = StitchLoginRef.PrimaryContainer)
    Text(stringResource(R.string.register_pet_subtitle), style = MaterialTheme.typography.bodyMedium, color = StitchLoginRef.Outline)
    Surface(
        shape = StitchShape.cardFeed,
        color = StitchPalette.Surface,
        border = BorderStroke(1.dp, StitchPalette.BorderHairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(78.dp).clip(CircleShape).background(StitchPalette.SecondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Pets, contentDescription = null, tint = StitchPalette.Brand, modifier = Modifier.size(40.dp))
            }
            RegField(petName, onPetNameChange, stringResource(R.string.register_pet_name), Icons.Filled.Pets, Modifier.fillMaxWidth())
            RegField(petType, onPetTypeChange, stringResource(R.string.register_pet_type), Icons.Outlined.FavoriteBorder, Modifier.fillMaxWidth())
            RegField(petBio, onPetBioChange, stringResource(R.string.register_pet_bio), Icons.Outlined.Badge, Modifier.fillMaxWidth())
        }
    }
    PrimaryRegisterButton(text = stringResource(R.string.register_next), busy = false, onClick = onNext)
}

@Composable
private fun RegisterFeedStep(
    selectedTags: Set<String>,
    onToggleTag: (String) -> Unit,
    busy: Boolean,
    onFinish: () -> Unit,
) {
    Text(stringResource(R.string.register_feed_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = StitchLoginRef.PrimaryContainer)
    Text(stringResource(R.string.register_feed_subtitle), style = MaterialTheme.typography.bodyMedium, color = StitchLoginRef.Outline)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        REGISTER_TAGS.forEach { (key, labelRes) ->
            val selected = key in selectedTags
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onToggleTag(key) },
                shape = StitchShape.field,
                color = if (selected) StitchPalette.BrandMuted else StitchPalette.Surface,
                border = BorderStroke(1.dp, if (selected) StitchPalette.Brand else StitchPalette.BorderHairline),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(if (selected) StitchPalette.Brand else StitchPalette.SurfaceLow))
                    Text(stringResource(labelRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = StitchPalette.OnSurface)
                }
            }
        }
    }
    PrimaryRegisterButton(text = stringResource(R.string.register_start), busy = busy, onClick = onFinish)
}

@Composable
private fun RegisterOptionCard(
    option: RegisterOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = StitchShape.cardFeed,
        color = if (selected) StitchPalette.BrandMuted else StitchPalette.Surface,
        border = BorderStroke(1.dp, if (selected) StitchPalette.Brand else StitchPalette.BorderHairline),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(StitchPalette.SurfaceLow), contentAlignment = Alignment.Center) {
                Icon(option.icon, contentDescription = null, tint = StitchPalette.Brand)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(option.titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = StitchPalette.OnSurface)
                Text(stringResource(option.noteRes), style = MaterialTheme.typography.bodySmall, color = StitchPalette.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PrimaryRegisterButton(
    text: String,
    busy: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        shape = StitchShape.field,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = StitchLoginRef.PrimaryContainer,
                contentColor = Color.White,
                disabledContainerColor = StitchLoginRef.PrimaryContainer.copy(alpha = 0.5f),
            ),
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(8.dp))
                Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RegField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.heightIn(min = 56.dp),
        placeholder = { Text(placeholder, color = StitchLoginRef.Outline.copy(alpha = 0.6f)) },
        leadingIcon = {
            Icon(leading, contentDescription = null, tint = StitchLoginRef.Outline, modifier = Modifier.size(22.dp))
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        shape = StitchShape.field,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StitchLoginRef.SurfaceContainerLowest,
                unfocusedContainerColor = StitchLoginRef.SurfaceContainerLowest,
                focusedTextColor = StitchLoginRef.OnSurface,
                unfocusedTextColor = StitchLoginRef.OnSurface,
                focusedBorderColor = StitchLoginRef.PrimaryContainer,
                unfocusedBorderColor = StitchPalette.BorderHairline,
                cursorColor = StitchLoginRef.PrimaryContainer,
                focusedLeadingIconColor = StitchLoginRef.PrimaryContainer,
                unfocusedLeadingIconColor = StitchLoginRef.Outline,
            ),
    )
}
