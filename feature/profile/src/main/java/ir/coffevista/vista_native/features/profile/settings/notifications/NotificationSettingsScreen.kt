package ir.coffevista.vista_native.features.profile.settings.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSection
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsSwitch
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsTile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val canEdit = uiState.hasLoaded && !uiState.isSaving
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var systemNotificationsEnabled by remember(context) { mutableStateOf(context.canDisplayNotifications()) }
    // System permission controls delivery, not the user's stored notification preferences.
    // Flutter keeps these choices available while Android notifications are disabled.
    val canManagePush = canEdit
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { systemNotificationsEnabled = context.canDisplayNotifications() }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemNotificationsEnabled = context.canDisplayNotifications()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "اعلان‌ها",
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

                // Section 1: Notifications
                VistaSettingsSection(title = "اعلان‌ها")
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "مجوز اعلان اندروید",
                        subtitle = if (systemNotificationsEnabled) {
                            "فعال"
                        } else {
                            "غیرفعال؛ برای دریافت اعلان آن را فعال کنید"
                        },
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.openNotificationSettings()
                            }
                        },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "اعلان‌های پوش",
                        value = settings.push_notifications,
                        enabled = canManagePush,
                        onChanged = { value -> viewModel.update { it.copy(push_notifications = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Message,
                        title = "پیام‌ها",
                        value = settings.message_notifications,
                        enabled = canManagePush && settings.push_notifications,
                        onChanged = { value -> viewModel.update { it.copy(message_notifications = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.AlternateEmail,
                        title = "منشن‌ها",
                        value = settings.mention_notifications,
                        enabled = canManagePush && settings.push_notifications,
                        onChanged = { value -> viewModel.update { it.copy(mention_notifications = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = "تعاملات اجتماعی",
                        subtitle = "لایک، کامنت، فالو و استوری",
                        value = settings.socialEnabled,
                        enabled = canManagePush && settings.push_notifications,
                        onChanged = { value -> viewModel.update { it.withSocialEnabled(value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Lightbulb,
                        title = "پیشنهادها",
                        value = settings.suggest_notifications,
                        enabled = canManagePush && settings.push_notifications,
                        onChanged = { value -> viewModel.update { it.copy(suggest_notifications = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Preview,
                        title = "پیش‌نمایش پیام",
                        value = settings.show_message_preview,
                        enabled = canManagePush && settings.push_notifications,
                        onChanged = { value -> viewModel.update { it.copy(show_message_preview = value) } },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Sound & Vibration
                VistaSettingsSection(title = "صدا و لرزش")
                VistaSettingsGroup {
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.VolumeUp,
                        title = "صدای پیام درون برنامه",
                        value = settings.in_app_chat_sounds,
                        enabled = canManagePush,
                        onChanged = { value -> viewModel.update { it.copy(in_app_chat_sounds = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.VolumeUp,
                        title = "صدای اعلان",
                        value = settings.sound_enabled,
                        enabled = canManagePush && settings.push_notifications,
                        onChanged = { value -> viewModel.update { it.copy(sound_enabled = value) } },
                    )
                    VistaSettingsDivider()
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Vibration,
                        title = "لرزش",
                        value = settings.vibration_enabled,
                        enabled = canManagePush && settings.push_notifications,
                        onChanged = { value -> viewModel.update { it.copy(vibration_enabled = value) } },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Quiet Hours
                VistaSettingsSection(title = "ساعات سکوت")
                VistaSettingsGroup {
                    VistaSettingsSwitch(
                        icon = Icons.Outlined.Bedtime,
                        title = "فعال‌سازی ساعات سکوت",
                        subtitle = "${settings.quiet_hours_start} تا ${settings.quiet_hours_end}",
                        value = settings.quiet_hours_enabled,
                        enabled = canManagePush && settings.push_notifications,
                        onChanged = { value -> viewModel.update { it.copy(quiet_hours_enabled = value) } },
                    )
                    if (settings.quiet_hours_enabled) {
                        VistaSettingsDivider()
                        VistaSettingsTile(
                            title = "زمان شروع/پایان",
                            subtitle = "${settings.quiet_hours_start} - ${settings.quiet_hours_end}",
                            enabled = settings.push_notifications && canEdit,
                            onClick = {
                                val start = settings.quiet_hours_start.toHourMinute(defaultHour = 22)
                                TimePickerDialog(context, { _, hour, minute ->
                                    TimePickerDialog(context, { _, endHour, endMinute ->
                                        viewModel.update {
                                            it.copy(
                                                quiet_hours_start = formatTime(hour, minute),
                                                quiet_hours_end = formatTime(endHour, endMinute),
                                            )
                                        }
                                    }, settings.quiet_hours_end.toHourMinute(defaultHour = 8).first, settings.quiet_hours_end.toHourMinute(defaultHour = 8).second, true).show()
                                }, start.first, start.second, true).show()
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun String.toHourMinute(defaultHour: Int): Pair<Int, Int> {
    val parts = split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: defaultHour
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return hour to minute
}

private fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

private fun Context.canDisplayNotifications(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return false
    return getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true
}

private fun Context.openNotificationSettings(): Boolean = runCatching {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        },
    )
}.isSuccess
