package ir.coffevista.vista_native.features.profile.settings.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsDivider
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsGroup
import ir.coffevista.vista_native.features.profile.settings.components.VistaSettingsTile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit,
    onSlideshowClick: () -> Unit,
    onTermsClick: () -> Unit,
    onContactUsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onFaqClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val installedVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    var showRatingDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LocalLayoutDirection.current) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "درباره ویستا",
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

                // Group 1: General & Legal
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Info,
                        iconColor = Color(0xFF2196F3),
                        iconContainerColor = Color(0xFF2196F3).copy(alpha = 0.12f),
                        title = "درباره ویستا",
                        subtitle = "اطلاعات کلی درباره برنامه و تیم سازنده",
                        onClick = onSlideshowClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Gavel,
                        iconColor = Color(0xFFFF9800),
                        iconContainerColor = Color(0xFFFF9800).copy(alpha = 0.12f),
                        title = "شرایط و قوانین",
                        subtitle = "قوانین استفاده از ویستا",
                        onClick = onTermsClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.ContactSupport,
                        iconColor = Color(0xFF4CAF50),
                        iconContainerColor = Color(0xFF4CAF50).copy(alpha = 0.12f),
                        title = "تماس با ما",
                        subtitle = "راه‌های ارتباط با تیم پشتیبانی",
                        onClick = onContactUsClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Security,
                        iconColor = Color(0xFFF44336),
                        iconContainerColor = Color(0xFFF44336).copy(alpha = 0.12f),
                        title = "سیاست حریم خصوصی",
                        subtitle = "نحوه حفاظت از اطلاعات شخصی شما",
                        onClick = onPrivacyPolicyClick,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Group 2: Support & Rating
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Help,
                        iconColor = Color(0xFF9C27B0),
                        iconContainerColor = Color(0xFF9C27B0).copy(alpha = 0.12f),
                        title = "سوالات متداول",
                        subtitle = "پاسخ سوالات رایج کاربران",
                        onClick = onFaqClick,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Star,
                        iconColor = Color(0xFFFFB300),
                        iconContainerColor = Color(0xFFFFB300).copy(alpha = 0.12f),
                        title = "امتیاز به ویستا",
                        subtitle = "نظر خود را در مورد برنامه بدهید",
                        onClick = { showRatingDialog = true },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Group 3: App info & Update
                VistaSettingsGroup {
                    VistaSettingsTile(
                        icon = Icons.Outlined.Code,
                        iconColor = Color(0xFF009688),
                        iconContainerColor = Color(0xFF009688).copy(alpha = 0.12f),
                        title = "نسخه برنامه",
                        subtitle = "نسخه $installedVersion",
                        showChevron = false,
                    )
                    VistaSettingsDivider()
                    VistaSettingsTile(
                        icon = Icons.Outlined.Update,
                        iconColor = Color(0xFF3F51B5),
                        iconContainerColor = Color(0xFF3F51B5).copy(alpha = 0.12f),
                        title = "بررسی به‌روزرسانی",
                        subtitle = "جستجو برای نسخه جدید",
                        onClick = {
                            if (!openVistaBazaarPage(context)) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("امکان باز کردن صفحهٔ ویستا در کافه‌بازار وجود ندارد")
                                }
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showRatingDialog) {
            AlertDialog(
                onDismissRequest = { showRatingDialog = false },
                title = { Text("امتیاز به ویستا در کافه‌بازار") },
                text = { Text("حمایت شما با ثبت نظر و امتیاز در کافه‌بازار باعث دلگرمی و بهبود مداوم ویستا می‌شود.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRatingDialog = false
                        if (!openVistaBazaarPage(context)) {
                            scope.launch {
                                snackbarHostState.showSnackbar("امکان باز کردن صفحهٔ امتیازدهی در کافه‌بازار وجود ندارد")
                            }
                        }
                    }) {
                        Text("ثبت نظر در بازار", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRatingDialog = false }) {
                        Text("بعداً")
                    }
                },
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

private fun openVistaBazaarPage(context: android.content.Context): Boolean = runCatching {
    val bazaarUri = Uri.parse("bazaar://details?id=ir.coffevista.vista")
    context.startActivity(Intent(Intent.ACTION_VIEW, bazaarUri))
}.recoverCatching {
    val webUri = Uri.parse("https://cafebazaar.ir/app/ir.coffevista.vista")
    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
}.isSuccess
