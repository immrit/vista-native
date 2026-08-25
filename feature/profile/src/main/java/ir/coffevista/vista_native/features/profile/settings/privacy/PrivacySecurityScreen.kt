package ir.coffevista.vista_native.features.profile.settings.privacy

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.features.profile.settings.components.VistaChoiceOption
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsChoiceBottomSheet
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSwitch
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsTile
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    profile: OwnProfileEntity?,
    onBack: () -> Unit,
    onBlockedUsersClick: () -> Unit,
    onActiveSessionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrivacySettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val canEdit = uiState.hasLoaded && !uiState.isSaving
    val context = LocalContext.current
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    // BottomSheet states
    var activeSheet by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "حریم خصوصی و امنیت",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Privacy
                VistaSettingsSection(title = "حریم خصوصی")
                VistaSettingsGroup {
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Lock,
                        title = "حساب خصوصی",
                        value = settings.is_private,
                        enabled = canEdit,
                        onChanged = viewModel::setPrivate,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.AccessTime,
                        title = "آخرین بازدید",
                        trailing = {
                            Text(
                                text = when (settings.last_seen_visibility) {
                                    "everyone" -> "همه"
                                    "my_contacts" -> "فقط مخاطبین"
                                    else -> "هیچکس"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        enabled = canEdit,
                        onClick = { activeSheet = "lastSeen" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "پیام از طرف",
                        trailing = {
                            Text(
                                text = when (settings.message_privacy) {
                                    "everyone" -> "همه"
                                    "friends" -> "دوستان من"
                                    else -> "هیچکس"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        enabled = canEdit,
                        onClick = { activeSheet = "messagePrivacy" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.GroupAdd,
                        title = "افزودن به گروه",
                        trailing = {
                            Text(
                                text = when (settings.group_add_privacy) {
                                    "everyone" -> "همه"
                                    "following" -> "فقط دنبال‌کننده‌ها"
                                    else -> "هیچکس"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                ),
                            )
                        },
                        enabled = canEdit,
                        onClick = { activeSheet = "groupAddPrivacy" },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.DoneAll,
                        title = "تیک خوانده‌شدن",
                        value = settings.read_receipts,
                        enabled = canEdit,
                        onChanged = { value -> viewModel.update { it.copy(read_receipts = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.PhotoSizeSelectLarge,
                        title = "بزرگنمایی تصویر پروفایل",
                        subtitle = "اگر غیرفعال باشد، دیگران نمی‌توانند عکس پروفایل شما را بزرگ کنند",
                        value = settings.allow_profile_zoom,
                        enabled = canEdit,
                        onChanged = { value -> viewModel.update { it.copy(allow_profile_zoom = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Block,
                        title = "کاربران مسدود شده",
                        onClick = onBlockedUsersClick,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Security
                VistaSettingsSection(title = "امنیت")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Devices,
                        title = "نشست‌های فعال",
                        onClick = onActiveSessionsClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Fingerprint,
                        title = "ورود بیومتریک",
                        subtitle = if (uiState.biometricAvailable) null else "این دستگاه بیومتریک یا قفل امن در دسترس ندارد",
                        value = uiState.biometricEnabled,
                        enabled = !uiState.biometricLoading && (uiState.biometricAvailable || uiState.biometricEnabled),
                        onChanged = { enabled ->
                            val activity = context as? FragmentActivity
                            if (activity == null) {
                                Toast.makeText(context, "امکان نمایش تایید هویت در این صفحه وجود ندارد", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.setBiometricEnabled(activity, enabled)
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Choice Bottom Sheets
        when (activeSheet) {
            "lastSeen" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "آخرین بازدید",
                    selectedValue = settings.last_seen_visibility,
                    options = listOf(
                        VistaChoiceOption("everyone", "همه"),
                        VistaChoiceOption("my_contacts", "فقط مخاطبین"),
                        VistaChoiceOption("nobody", "هیچکس"),
                    ),
                    onOptionSelected = {
                        viewModel.update { settings -> settings.copy(last_seen_visibility = it) }
                        activeSheet = null
                    },
                    onDismissRequest = { activeSheet = null },
                )
            }
            "messagePrivacy" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "پیام از طرف",
                    selectedValue = settings.message_privacy,
                    options = listOf(
                        VistaChoiceOption("everyone", "همه"),
                        VistaChoiceOption("friends", "دوستان من"),
                        VistaChoiceOption("nobody", "هیچکس"),
                    ),
                    onOptionSelected = {
                        viewModel.update { settings -> settings.copy(message_privacy = it) }
                        activeSheet = null
                    },
                    onDismissRequest = { activeSheet = null },
                )
            }
            "groupAddPrivacy" -> {
                VistaSettingsChoiceBottomSheet(
                    title = "افزودن به گروه",
                    selectedValue = settings.group_add_privacy,
                    options = listOf(
                        VistaChoiceOption("everyone", "همه"),
                        VistaChoiceOption("following", "فقط دنبال‌کننده‌ها"),
                        VistaChoiceOption("nobody", "هیچکس"),
                    ),
                    onOptionSelected = {
                        viewModel.update { settings -> settings.copy(group_add_privacy = it) }
                        activeSheet = null
                    },
                    onDismissRequest = { activeSheet = null },
                )
            }
        }
    }
}
