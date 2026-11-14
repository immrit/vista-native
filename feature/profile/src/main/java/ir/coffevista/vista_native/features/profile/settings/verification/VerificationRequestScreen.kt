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
import ir.coffevista.vista_native.features.profile.data.VerificationStatusDto
import kotlinx.coroutines.delay

private val suggestedCategories = listOf(
    "هنرمند / موزیسین",
    "چهره عمومی",
    "تولیدکننده محتوای شاخص",
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
            snackbarHostState.showSnackbar("درخواست تیک مشکی با موفقیت ثبت شد و در صف بررسی قرار گرفت.")
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
                                contentDescription = "بروزرسانی وضعیت",
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Black Tick Identity Hero Card
                BlackTickHeroCard()

                // Server Status / Block Banner
                if (uiState.isLoadingStatus) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = "در حال بررسی سابقه و وضعیت درخواست شما...",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else if (uiState.statusError != null && status == null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = uiState.statusError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::loadVerificationStatus) {
                                Text("تلاش مجدد")
                            }
                        }
                    }
                } else if (status != null) {
                    VerificationServerStatusBanner(status = status)
                }

                // Eligibility explanation card (prevents frivolous requests)
                BlackTickEligibilityCard()

                // Progressive penalty card
                VerificationPenaltyPolicyCard(status = status)

                // Form inputs (shown or disabled cleanly if blocked)
                Text(
                    text = "دسته‌بندی فعالیت رسمی",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    suggestedCategories.forEach { preset ->
                        FilterChip(
                            selected = uiState.category.trim() == preset,
                            onClick = { if (formEnabled) viewModel.onCategoryChanged(preset) },
                            enabled = formEnabled,
                            label = {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.category,
                    onValueChange = viewModel::onCategoryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("یا دسته‌بندی دقیق فعالیت خود را بنویسید") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    enabled = formEnabled,
                )

                Text(
                    text = "تصویر مدرک هویتی یا مجوز معتبر",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
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
                        .clickable(enabled = formEnabled) {
                            showInAppDocPicker = true
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (hasDocument) Icons.Outlined.CheckCircle else Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        tint = if (hasDocument) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = if (hasDocument) "تصویر مدرک انتخاب شد (برای تغییر کلیک کنید)"
                        else "بارگذاری مدرک شناسایی معتبر (کارت ملی، کارت مطبوعات، روزنامه رسمی یا مجوز فعالیت)",
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

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("لینک وب‌سایت، رسانه‌های خبری، سوابق شناخته‌شدگی و توضیحات تکمیلی...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    maxLines = 5,
                    enabled = formEnabled,
                )

                // Explicit acknowledgment of penalty rules before submission
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = formEnabled) {
                            viewModel.onPenaltyAcknowledgedChanged(!uiState.penaltyAcknowledged)
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = uiState.penaltyAcknowledged,
                            onCheckedChange = viewModel::onPenaltyAcknowledgedChanged,
                            enabled = formEnabled,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تأیید می‌کنم شرایط دریافت تیک مشکی را دارم و می‌دانم در صورت رد درخواست، محدودیت زمانی و جریمه سکه اعمال می‌شود.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Button(
                    onClick = viewModel::submit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
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
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = if (canSubmitByServer) "ثبت درخواست تیک مشکی" else "ارسال درخواست غیرفعال است",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BlackTickHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF111827),
        ),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "نشان تایید هویت تیک مشکی",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )
                Text(
                    text = "تیک مشکی نشان رسمی اصالت برای شخصیت‌های شناخته‌شده، هنرمندان، رسانه‌ها و برندهای معتبر در ویستا است.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.82f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun BlackTickEligibilityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "تیک مشکی برای چه کاربرانی صادر می‌شود؟",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
            Text(
                text = "• شخصیت‌های عمومی، هنرمندان، ورزشکاران، روزنامه‌نگاران و تولیدکنندگان محتوای شاخص و اثرگذار.\n" +
                    "• شرکت‌ها، مؤسسات، کسب‌وکارها و برندهای رسمی با مدارک ثبت یا مجوز معتبر.\n" +
                    "• پروفایل باید کامل (تصویر واقعی، نام دقیق، بیوگرافی و فعالیت مستمر و اصیل) باشد و احتمال جعل هویت وجود داشته باشد.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "تفاوت نشان‌ها: تیک آبی مخصوص ناظران و تیم رسمی ویستا است. اگر واجد شرایط تیک مشکی نیستید اما امکانات پیشرفته می‌خواهید، می‌توانید از بخش اشتراک، تیک طلایی (پریمیوم) تهیه کنید.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun VerificationPenaltyPolicyCard(status: VerificationStatusDto?) {
    val amber = Color(0xFFB45309)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF59E0B).copy(alpha = 0.09f),
        ),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = amber,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "جریمه ارسال درخواست بی‌مورد یا نامعتبر",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = amber,
                    ),
                )
            }
            Text(
                text = "برای جلوگیری از ارسال درخواست‌های تکراری و بررسی سریع‌تر متقاضیان واقعی، در صورت رد شدن درخواست شما جریمه پلکانی اعمال می‌شود:\n" +
                    "• بار اول رد شدن: ۷ روز مسدودیت ارسال مجدد + کسر ۱۰۰ سکه\n" +
                    "• بار دوم رد شدن: ۳۰ روز مسدودیت ارسال مجدد + کسر ۲۵۰ سکه\n" +
                    "• بار سوم به بعد: ۹۰ روز مسدودیت ارسال مجدد + کسر ۵۰۰ سکه",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (status != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "سابقه رد: ${status.rejectedCount} بار • سکه فعلی: ${status.currentCoins}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "جریمه بعدی: ${status.nextPenaltyDays} روز / ${status.nextPenaltyCoins} سکه",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = amber,
                            ),
                        )
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
                title = "حساب شما هم‌اکنون دارای $badgeName است",
                body = "نیازی به ارسال درخواست جدید ندارید.",
            )
        }
        "pending_exists" -> {
            StatusBannerCard(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                icon = Icons.Outlined.HourglassTop,
                tint = MaterialTheme.colorScheme.primary,
                title = "درخواست تیک مشکی شما در حال بررسی است",
                body = "تا زمان اعلام نتیجه بررسی توسط تیم ویستا، امکان ارسال درخواست جدید وجود ندارد.",
            )
        }
        "cooldown_active" -> {
            val remainingText = formatCooldownRemaining(status.cooldownRemainingSeconds)
            val reasonPart = status.lastRejectionReason?.takeIf { it.isNotBlank() }
                ?.let { "\nعلت آخرین رد: $it" }
                .orEmpty()
            val coinsPart = if (status.lastPenaltyCoins > 0) {
                "\nجریمه سکه اعمال‌شده: ${status.lastPenaltyCoins} سکه"
            } else {
                ""
            }
            StatusBannerCard(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                icon = Icons.Outlined.WarningAmber,
                tint = MaterialTheme.colorScheme.error,
                title = "ارسال درخواست موقتاً مسدود است ($remainingText)",
                body = "به دلیل رد شدن درخواست قبلی، امکان ثبت درخواست جدید فعلاً غیرفعال است.$coinsPart$reasonPart",
            )
        }
        else -> {
            if (status.latestStatus == "rejected" && status.rejectedCount > 0) {
                val reasonPart = status.lastRejectionReason?.takeIf { it.isNotBlank() }
                    ?.let { "علت رد قبلی: $it\n" }
                    .orEmpty()
                StatusBannerCard(
                    containerColor = Color(0xFFF59E0B).copy(alpha = 0.10f),
                    borderColor = Color(0xFFF59E0B).copy(alpha = 0.35f),
                    icon = Icons.Outlined.Info,
                    tint = Color(0xFFB45309),
                    title = "درخواست قبلی شما رد شده بود",
                    body = "${reasonPart}دوره انتظار به پایان رسیده است؛ پیش از ارسال مجدد مدارک و پروفایل خود را تکمیل کنید.",
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = tint,
                    ),
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
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
        days > 0 -> "$days روز و $hours ساعت باقی‌مانده"
        hours > 0 -> "$hours ساعت و $minutes دقیقه باقی‌مانده"
        else -> "${minutes.coerceAtLeast(1)} دقیقه باقی‌مانده"
    }
}
