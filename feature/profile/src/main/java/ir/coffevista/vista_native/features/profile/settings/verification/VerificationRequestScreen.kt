package ir.coffevista.vista_native.features.profile.settings.verification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.designsystem.component.VistaCoinIcon
import ir.coffevista.vista_native.core.designsystem.component.rememberBufferedTextState
import ir.coffevista.vista_native.features.profile.data.VerificationStatusDto
import kotlinx.coroutines.delay

private val suggestedCategories = listOf(
    "هنرمند / موزیسین",
    "چهره عمومی",
    "تولیدکننده محتوا",
    "خبرنگار / رسانه",
    "ورزشکار",
    "برند یا شرکت رسمی",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VerificationRequestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerificationRequestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showInAppDocPicker by remember { mutableStateOf(false) }

    if (showInAppDocPicker) {
        ir.coffevista.vista_native.core.designsystem.component.VistaInAppMediaPickerBottomSheet(
            mode = ir.coffevista.vista_native.core.designsystem.component.VistaInAppPickerMode.IMAGE_SINGLE,
            title = "انتخاب تصویر مدرک هویتی",
            onDismiss = { showInAppDocPicker = false },
            onSingleSelected = { uri -> viewModel.onDocumentSelected(uri) },
        )
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            snackbarHostState.showSnackbar("درخواست تیک مشکی با موفقیت ثبت شد.")
            delay(1500)
            onBack()
        }
    }

    val status = uiState.verificationStatus
    val canSubmitByServer = status?.canSubmit != false
    val formEnabled = !uiState.isSubmitting && !uiState.isLoadingStatus && canSubmitByServer

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "درخواست تیک مشکی",
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
                    actions = {
                        IconButton(
                            onClick = viewModel::loadVerificationStatus,
                            enabled = !uiState.isLoadingStatus,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "بروزرسانی",
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
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BlackTickHeroCard()

                if (uiState.isLoadingStatus) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("در حال بررسی وضعیت درخواست...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (uiState.statusError != null && status == null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = uiState.statusError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::loadVerificationStatus) { Text("تلاش مجدد") }
                        }
                    }
                } else if (status != null) {
                    VerificationServerStatusBanner(status = status)
                }

                // Compact Eligibility + Penalty Rules Card
                BlackTickRulesCard(status = status)

                Text(
                    text = "دسته‌بندی فعالیت",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )

                val (bufferedCategory, onCategoryChange) = rememberBufferedTextState(
                    externalValue = uiState.category,
                    onValueChange = viewModel::onCategoryChanged,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    suggestedCategories.forEach { preset ->
                        FilterChip(
                            selected = bufferedCategory.trim() == preset,
                            onClick = { if (formEnabled) onCategoryChange(preset) },
                            enabled = formEnabled,
                            label = { Text(text = preset, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }

                OutlinedTextField(
                    value = bufferedCategory,
                    onValueChange = onCategoryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("یا دسته‌بندی خود را بنویسید") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    enabled = formEnabled,
                )

                Text(
                    text = "تصویر مدرک معتبر",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )

                val hasDocument = uiState.selectedDocumentUri != null
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hasDocument) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            else MaterialTheme.colorScheme.surface,
                        )
                        .border(
                            width = 1.dp,
                            color = if (hasDocument) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable(enabled = formEnabled) { showInAppDocPicker = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (hasDocument) Icons.Outlined.CheckCircle else Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        tint = if (hasDocument) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = if (hasDocument) "مدرک انتخاب شد (برای تغییر لمس کنید)"
                        else "بارگذاری مدرک هویتی یا مجوز فعالیت",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (hasDocument) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (hasDocument) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                }

                if (uiState.isUploading) {
                    LinearProgressIndicator(
                        progress = { uiState.uploadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                    )
                }

                val (bufferedNotes, onNotesChange) = rememberBufferedTextState(
                    externalValue = uiState.notes,
                    onValueChange = viewModel::onNotesChanged,
                )
                OutlinedTextField(
                    value = bufferedNotes,
                    onValueChange = onNotesChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("توضیحات یا لینک سوابق و وب‌سایت (اختیاری)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    maxLines = 4,
                    enabled = formEnabled,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .clickable(enabled = formEnabled) {
                            viewModel.onPenaltyAcknowledgedChanged(!uiState.penaltyAcknowledged)
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = uiState.penaltyAcknowledged,
                        onCheckedChange = viewModel::onPenaltyAcknowledgedChanged,
                        enabled = formEnabled,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "شرایط تیک مشکی و جریمه رد درخواست را می‌پذیرم.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Button(
                    onClick = viewModel::submit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF111827),
                        contentColor = Color.White,
                    ),
                    enabled = formEnabled && uiState.penaltyAcknowledged,
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = if (canSubmitByServer) "ثبت درخواست تیک مشکی" else "ارسال موقتاً غیرفعال است",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BlackTickHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "تیک مشکی • نشان تایید اصالت",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )
                Text(
                    text = "مخصوص چهره‌های شناخته‌شده، هنرمندان، رسانه‌ها و برندهای رسمی.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.8f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun BlackTickRulesCard(status: VerificationStatusDto?) {
    val amber = Color(0xFFB45309)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "شرایط واجدین",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "• فعالیت رسمی/عمومی با پروفایل کامل و مدارک معتبر\n" +
                        "• تیک آبی برای مدیران و تیک طلایی برای اشتراک پریمیوم است.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = amber,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "جریمه درخواست نامعتبر (رد شدن):",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = amber,
                            ),
                        )
                    }
                    Text(
                        text = "بار ۱: ۷ روز + ۱۰۰ سکه  •  بار ۲: ۳۰ روز + ۲۵۰ سکه  •  بار ۳+: ۹۰ روز + ۵۰۰ سکه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (status != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            VistaCoinIcon(size = 15.dp)
                            Text(
                                text = "سابقه رد: ${status.rejectedCount} بار  |  سکه فعلی: ${status.currentCoins}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = amber,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationServerStatusBanner(status: VerificationStatusDto) {
    when (status.blockedReason) {
        "already_verified" -> {
            val badgeName = if (status.verificationType == "blueTick") "تیک آبی" else "تیک مشکی"
            StatusBannerCard(
                containerColor = Color(0xFF10B981).copy(alpha = 0.12f),
                borderColor = Color(0xFF10B981).copy(alpha = 0.4f),
                icon = Icons.Outlined.CheckCircle,
                tint = Color(0xFF047857),
                title = "حساب شما دارای $badgeName است",
                body = "نیازی به ثبت درخواست جدید ندارید.",
            )
        }
        "pending_exists" -> {
            StatusBannerCard(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                icon = Icons.Outlined.HourglassTop,
                tint = MaterialTheme.colorScheme.primary,
                title = "درخواست قبلی در حال بررسی است",
                body = "تا اعلام نتیجه، ارسال درخواست جدید فعال نیست.",
            )
        }
        "cooldown_active" -> {
            val remainingText = formatCooldownRemaining(status.cooldownRemainingSeconds)
            val reasonPart = status.lastRejectionReason?.takeIf { it.isNotBlank() }
                ?.let { " — علت: $it" }
                .orEmpty()
            StatusBannerCard(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                icon = Icons.Outlined.WarningAmber,
                tint = MaterialTheme.colorScheme.error,
                title = "محدودیت ارسال مجدد ($remainingText)",
                body = "درخواست قبلی رد شده است$reasonPart.",
            )
        }
        else -> {
            if (status.latestStatus == "rejected" && status.rejectedCount > 0) {
                val reasonPart = status.lastRejectionReason?.takeIf { it.isNotBlank() }
                    ?.let { "علت قبلی: $it" }
                    ?: "پیش از ارسال مجدد مدارک را کامل‌تر بارگذاری کنید."
                StatusBannerCard(
                    containerColor = Color(0xFFF59E0B).copy(alpha = 0.10f),
                    borderColor = Color(0xFFF59E0B).copy(alpha = 0.35f),
                    icon = Icons.Outlined.Info,
                    tint = Color(0xFFB45309),
                    title = "امکان ارسال مجدد فعال است",
                    body = reasonPart,
                )
            }
        }
    }
}

@Composable
private fun StatusBannerCard(
    containerColor: Color,
    borderColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = tint),
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun formatCooldownRemaining(seconds: Long): String {
    if (seconds <= 0L) return "به زودی"
    val days = seconds / 86400L
    val hours = (seconds % 86400L) / 3600L
    val minutes = (seconds % 3600L) / 60L
    return when {
        days > 0 -> "$days روز و $hours ساعت"
        hours > 0 -> "$hours ساعت و $minutes دقیقه"
        else -> "${minutes.coerceAtLeast(1)} دقیقه"
    }
}
