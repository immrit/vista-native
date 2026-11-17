package ir.coffevista.vista_native.features.profile.settings.privacy

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.coffevista.vista_native.core.designsystem.component.VistaInAppMediaPickerBottomSheet
import ir.coffevista.vista_native.core.designsystem.component.VistaInAppPickerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SessionItemUiModel(
    val id: String,
    val deviceName: String,
    val clientSubtitle: String,
    val platform: String,
    val ip: String,
    val location: String,
    val lastActiveText: String,
    val createdAtText: String,
    val approvalStatus: String,
    val isCurrent: Boolean,
)

private data class TelegramDevicesPalette(
    val screenBg: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val sectionTitle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val ctaBlue: Color,
    val dangerRed: Color,
    val onlineBlue: Color,
    val backButtonBg: Color,
)

@Composable
private fun rememberTelegramDevicesPalette(): TelegramDevicesPalette {
    val dark = isSystemInDarkTheme()
    return remember(dark) {
        if (dark) {
            TelegramDevicesPalette(
                screenBg = Color(0xFF09090F),
                cardBg = Color(0xFF13131E),
                cardBorder = Color(0xFF2A2A45),
                sectionTitle = Color(0xFF818CF8),
                textPrimary = Color(0xFFF0F0FF),
                textSecondary = Color(0xFF8B8BAD),
                divider = Color(0xFF2A2A45),
                ctaBlue = Color(0xFF6366F1),
                dangerRed = Color(0xFFF87171),
                onlineBlue = Color(0xFF818CF8),
                backButtonBg = Color(0xFF1C1C2E),
            )
        } else {
            TelegramDevicesPalette(
                screenBg = Color(0xFFF8F9FF),
                cardBg = Color(0xFFFFFFFF),
                cardBorder = Color(0xFFE5E7EB),
                sectionTitle = Color(0xFF4F46E5),
                textPrimary = Color(0xFF0F1117),
                textSecondary = Color(0xFF6B7280),
                divider = Color(0xFFE5E7EB),
                ctaBlue = Color(0xFF6366F1),
                dangerRed = Color(0xFFEF4444),
                onlineBlue = Color(0xFF4F46E5),
                backButtonBg = Color(0xFFF3F4FF),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionsScreen(
    onBack: () -> Unit,
    viewModel: ActiveSessionsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = rememberTelegramDevicesPalette()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSession by remember { mutableStateOf<SessionItemUiModel?>(null) }
    var confirmTerminateAll by remember { mutableStateOf(false) }
    var showTtlSheet by remember { mutableStateOf(false) }
    var showLiveQrScanner by remember { mutableStateOf(false) }
    var showAddDeviceSheet by remember { mutableStateOf(false) }
    var showQrGalleryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.feedbackMessage) {
        val msg = uiState.feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearFeedback()
    }

    val currentSession = uiState.sessions.firstOrNull(SessionItemUiModel::isCurrent)
    val otherSessions = uiState.sessions.filterNot(SessionItemUiModel::isCurrent)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = palette.screenBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TelegramDevicesHeader(
                    palette = palette,
                    isLoading = uiState.isMutating || uiState.isSavingSettings,
                    onBack = onBack,
                )
            },
        ) { padding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = palette.ctaBlue)
                }
            } else if (uiState.errorMessage != null && uiState.sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = palette.cardBg,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, palette.cardBorder),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Devices,
                                contentDescription = null,
                                tint = palette.sectionTitle,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                color = palette.textPrimary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                            )
                            Button(
                                onClick = viewModel::refresh,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.ctaBlue),
                                shape = RoundedCornerShape(24.dp),
                            ) {
                                Text("تلاش مجدد", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // ── Hero Card: Illustration + QR Caption + Add Device Pill CTA ─────────
                    item {
                        DevicesHeroCard(
                            palette = palette,
                            onAddDevice = { showLiveQrScanner = true },
                        )
                    }

                    // ── This Device Section ─────────────────────────────────────────────────
                    if (currentSession != null) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = palette.cardBg,
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(0.75.dp, palette.cardBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "این دستگاه",
                                            color = palette.sectionTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                                        )
                                        TelegramSessionRow(
                                            session = currentSession,
                                            palette = palette,
                                            onClick = { selectedSession = currentSession },
                                        )
                                        if (otherSessions.isNotEmpty()) {
                                            HorizontalDivider(
                                                color = palette.divider,
                                                thickness = 0.6.dp,
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = !uiState.isMutating) {
                                                        confirmTerminateAll = true
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            ) {
                                                Box(
                                                    modifier = Modifier.width(42.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.PanTool,
                                                        contentDescription = null,
                                                        tint = palette.dangerRed,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                }
                                                Text(
                                                    text = "خاتمه دادن به تمام نشست‌های دیگر",
                                                    color = palette.dangerRed,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (otherSessions.isNotEmpty()) {
                                    Text(
                                        text = "همه دستگاه‌ها به جز این دستگاه از حساب خارج می‌شوند.",
                                        color = palette.textSecondary,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    )
                                }
                            }
                        }
                    }

                    // ── Active Sessions Card ────────────────────────────────────────────────
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = palette.cardBg,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(0.75.dp, palette.cardBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "نشست‌های فعال",
                                        color = palette.sectionTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                                    )
                                    if (otherSessions.isEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 18.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.VerifiedUser,
                                                contentDescription = null,
                                                tint = palette.sectionTitle,
                                                modifier = Modifier.size(22.dp),
                                            )
                                            Text(
                                                text = "هیچ دستگاه دیگری به حساب شما متصل نیست.",
                                                color = palette.textSecondary,
                                                fontSize = 13.5.sp,
                                            )
                                        }
                                    } else {
                                        otherSessions.forEachIndexed { idx, session ->
                                            TelegramSessionRow(
                                                session = session,
                                                palette = palette,
                                                onClick = { selectedSession = session },
                                            )
                                            if (idx < otherSessions.lastIndex) {
                                                HorizontalDivider(
                                                    color = palette.divider,
                                                    thickness = 0.6.dp,
                                                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "نسخه رسمی ویستا برای اندروید و وب در دسترس است.",
                                color = palette.textSecondary,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }

                    // ── Automatically Terminate Old Sessions Card ───────────────────────────
                    item {
                        Surface(
                            color = palette.cardBg,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(0.75.dp, palette.cardBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "خاتمه خودکار نشست‌های قدیمی",
                                    color = palette.sectionTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !uiState.isSavingSettings) { showTtlSheet = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "در صورت عدم فعالیت به مدت",
                                        color = palette.textPrimary,
                                        fontSize = 15.sp,
                                    )
                                    Text(
                                        text = formatAutoTerminateLabel(uiState.autoTerminateDays),
                                        color = palette.sectionTitle,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Confirm Terminate All Other Sessions Dialog ─────────────────────────────────────
        if (confirmTerminateAll) {
            AlertDialog(
                onDismissRequest = { confirmTerminateAll = false },
                containerColor = palette.cardBg,
                titleContentColor = palette.textPrimary,
                textContentColor = palette.textSecondary,
                title = {
                    Text("خاتمه دادن به تمام نشست‌های دیگر؟", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("با تایید این مورد، همه دستگاه‌های دیگر به جز این دستگاه بلافاصله از حساب خارج می‌شوند.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmTerminateAll = false
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                viewModel.authorizeSensitiveAction(activity) {
                                    viewModel.terminateAllOther()
                                }
                            } else {
                                viewModel.terminateAllOther()
                            }
                        },
                    ) {
                        Text("خاتمه همه", color = palette.dangerRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmTerminateAll = false }) {
                        Text("انصراف", color = palette.textSecondary)
                    }
                },
            )
        }

        // ── Session Details Bottom Sheet ────────────────────────────────────────────────────
        selectedSession?.let { target ->
            SessionDetailSheet(
                session = target,
                palette = palette,
                isMutating = uiState.isMutating,
                onDismiss = { selectedSession = null },
                onApprove = {
                    viewModel.approvePendingSession(target) {
                        selectedSession = null
                    }
                },
                onTerminate = {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        viewModel.authorizeSensitiveAction(activity) {
                            viewModel.terminate(target) {
                                selectedSession = null
                            }
                        }
                    } else {
                        viewModel.terminate(target) {
                            selectedSession = null
                        }
                    }
                },
            )
        }

        // ── Auto-Terminate TTL Selector Bottom Sheet ────────────────────────────────────────
        if (showTtlSheet) {
            AutoTerminateTtlSheet(
                selectedDays = uiState.autoTerminateDays,
                palette = palette,
                onSelect = { days ->
                    showTtlSheet = false
                    viewModel.updateAutoTerminateDays(days)
                },
                onDismiss = { showTtlSheet = false },
            )
        }

        // ── Live Telegram-Style QR Scanner ──────────────────────────────────────────────────
        if (showLiveQrScanner) {
            TelegramQrScannerDialog(
                isApproving = uiState.isApprovingQr,
                onQrRecognized = { raw ->
                    viewModel.approveQrToken(raw) {
                        showLiveQrScanner = false
                    }
                },
                onPickGallery = { showQrGalleryPicker = true },
                onManualEntry = {
                    showLiveQrScanner = false
                    showAddDeviceSheet = true
                },
                onDismiss = { showLiveQrScanner = false },
            )
        }

        // ── Manual QR Token Entry Sheet ─────────────────────────────────────────────────────
        if (showAddDeviceSheet) {
            AddDeviceManualCodeSheet(
                palette = palette,
                isApproving = uiState.isApprovingQr,
                onApproveToken = { raw ->
                    viewModel.approveQrToken(raw) {
                        showAddDeviceSheet = false
                    }
                },
                onDismiss = { showAddDeviceSheet = false },
            )
        }

        if (showQrGalleryPicker) {
            VistaInAppMediaPickerBottomSheet(
                mode = VistaInAppPickerMode.IMAGE_SINGLE,
                title = "انتخاب تصویر کد QR ورود",
                onDismiss = { showQrGalleryPicker = false },
                onSingleSelected = { uri ->
                    showQrGalleryPicker = false
                    scope.launch {
                        val decoded = withContext(Dispatchers.IO) { decodeQrTextFromUri(context, uri) }
                        if (decoded != null) {
                            viewModel.approveQrToken(decoded) {
                                showLiveQrScanner = false
                                showAddDeviceSheet = false
                            }
                        } else {
                            snackbarHostState.showSnackbar("کد QR معتبری در تصویر پیدا نشد")
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TelegramDevicesHeader(
    palette: TelegramDevicesPalette,
    isLoading: Boolean,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "دستگاه‌ها",
                color = palette.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp),
                shape = CircleShape,
                color = palette.backButtonBg,
                onClick = onBack,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "بازگشت",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (isLoading) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = palette.ctaBlue,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun DevicesHeroCard(
    palette: TelegramDevicesPalette,
    onAddDevice: () -> Unit,
) {
    Surface(
        color = palette.cardBg,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.75.dp, palette.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TelegramLaptopIllustration(modifier = Modifier.size(width = 148.dp, height = 104.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                text = buildAnnotatedString {
                    append("با اسکن کد QR می‌توانید وارد ")
                    withStyle(SpanStyle(color = palette.ctaBlue, fontWeight = FontWeight.Medium)) {
                        append("ویستا وب")
                    }
                    append(" یا ")
                    withStyle(SpanStyle(color = palette.ctaBlue, fontWeight = FontWeight.Medium)) {
                        append("نسخه دسکتاپ")
                    }
                    append(" شوید.")
                },
                color = palette.textPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onAddDevice,
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.ctaBlue,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "افزودن دستگاه",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TelegramLaptopIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Screen outer aluminum frame
        val bezelLeft = w * 0.14f
        val bezelRight = w * 0.86f
        val bezelTop = h * 0.04f
        val bezelBottom = h * 0.68f
        drawRoundRect(
            color = Color(0xFF1F2630),
            topLeft = Offset(bezelLeft, bezelTop),
            size = Size(bezelRight - bezelLeft, bezelBottom - bezelTop),
            cornerRadius = CornerRadius(12f, 12f),
        )
        // Inner screen (chat window)
        val scrLeft = bezelLeft + 7f
        val scrTop = bezelTop + 7f
        val scrW = (bezelRight - bezelLeft) - 14f
        val scrH = (bezelBottom - bezelTop) - 14f
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFFE0E7FF), Color(0xFFEDE9FE))),
            topLeft = Offset(scrLeft, scrTop),
            size = Size(scrW, scrH),
            cornerRadius = CornerRadius(6f, 6f),
        )
        // Left chat list sidebar
        val sideW = scrW * 0.34f
        drawRect(
            color = Color.White,
            topLeft = Offset(scrLeft, scrTop),
            size = Size(sideW, scrH),
        )
        // Sidebar dots & lines
        val rowColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF10B981), Color(0xFFEC4899))
        rowColors.forEachIndexed { idx, c ->
            val cy = scrTop + 14f + idx * 24f
            if (cy + 10f < scrTop + scrH) {
                drawCircle(color = c, radius = 6f, center = Offset(scrLeft + 12f, cy))
                drawRoundRect(
                    color = Color(0xFFE0E5EC),
                    topLeft = Offset(scrLeft + 23f, cy - 3f),
                    size = Size(sideW - 30f, 5f),
                    cornerRadius = CornerRadius(3f, 3f),
                )
            }
        }
        // Chat bubbles in right pane
        val chatLeft = scrLeft + sideW + 8f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(chatLeft, scrTop + 14f),
            size = Size(scrW * 0.38f, 14f),
            cornerRadius = CornerRadius(7f, 7f),
        )
        drawRoundRect(
            color = Color(0xFFEFFEDD),
            topLeft = Offset(scrLeft + scrW - scrW * 0.40f - 8f, scrTop + 36f),
            size = Size(scrW * 0.40f, 18f),
            cornerRadius = CornerRadius(7f, 7f),
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(chatLeft, scrTop + 62f),
            size = Size(scrW * 0.32f, 13f),
            cornerRadius = CornerRadius(7f, 7f),
        )
        // Webcam dot
        drawCircle(color = Color(0xFF4B5563), radius = 2.2f, center = Offset(w * 0.5f, bezelTop + 3.5f))

        // Trapezoid keyboard deck
        val deckPath = Path().apply {
            moveTo(bezelLeft - 4f, bezelBottom + 2f)
            lineTo(bezelRight + 4f, bezelBottom + 2f)
            lineTo(w * 0.96f, h * 0.93f)
            lineTo(w * 0.04f, h * 0.93f)
            close()
        }
        drawPath(deckPath, color = Color(0xFFDDE2E8))
        // Bottom aluminum lip
        drawRoundRect(
            color = Color(0xFFBAC2CC),
            topLeft = Offset(w * 0.04f, h * 0.92f),
            size = Size(w * 0.92f, h * 0.055f),
            cornerRadius = CornerRadius(6f, 6f),
        )
        // Keyboard grid block
        drawRoundRect(
            color = Color(0xFF28303B),
            topLeft = Offset(w * 0.16f, bezelBottom + 8f),
            size = Size(w * 0.68f, (h * 0.78f) - bezelBottom),
            cornerRadius = CornerRadius(4f, 4f),
        )
        // Trackpad
        drawRoundRect(
            color = Color(0xFFC5CCD6),
            topLeft = Offset(w * 0.39f, h * 0.81f),
            size = Size(w * 0.22f, h * 0.09f),
            cornerRadius = CornerRadius(4f, 4f),
        )
    }
}

@Composable
private fun TelegramSessionRow(
    session: SessionItemUiModel,
    palette: TelegramDevicesPalette,
    onClick: () -> Unit,
) {
    val (badgeColor, icon) = platformBadgePresentation(session.platform)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = session.deviceName,
                    color = palette.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (session.approvalStatus == "pending" && !session.isCurrent) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "در انتظار تایید",
                            color = Color(0xFFF59E0B),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = session.clientSubtitle,
                color = palette.textPrimary.copy(alpha = 0.85f),
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.location,
                    color = palette.textSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = " · ",
                    color = palette.textSecondary,
                    fontSize = 12.5.sp,
                )
                Text(
                    text = session.lastActiveText,
                    color = if (session.isCurrent || session.lastActiveText == "آنلاین") palette.onlineBlue else palette.textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = if (session.isCurrent) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

private fun platformBadgePresentation(platform: String): Pair<Color, ImageVector> = when {
    platform.contains("android") -> Color(0xFF4CAF50) to Icons.Rounded.Android
    platform.contains("ios") || platform.contains("iphone") || platform.contains("ipad") ->
        Color(0xFF6366F1) to Icons.Rounded.PhoneIphone
    platform.contains("web") -> Color(0xFF8B5CF6) to Icons.Rounded.Language
    platform.contains("win") || platform.contains("mac") || platform.contains("linux") || platform.contains("desktop") ->
        Color(0xFF6366F1) to Icons.Rounded.Computer
    else -> Color(0xFF6366F1) to Icons.Rounded.Devices
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailSheet(
    session: SessionItemUiModel,
    palette: TelegramDevicesPalette,
    isMutating: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onTerminate: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (badgeColor, icon) = platformBadgePresentation(session.platform)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.cardBg,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Text(
                    text = session.deviceName,
                    color = palette.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${session.location} · ${session.lastActiveText}",
                    color = if (session.isCurrent) palette.onlineBlue else palette.textSecondary,
                    fontSize = 13.5.sp,
                )

                Surface(
                    color = palette.screenBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DetailRow("نسخه برنامه", session.clientSubtitle, palette)
                        if (session.ip.isNotBlank()) {
                            DetailRow("آدرس IP", session.ip, palette)
                        }
                        DetailRow("موقعیت مکانی", session.location, palette)
                        DetailRow("اولین ورود", session.createdAtText, palette)
                        DetailRow(
                            "وضعیت تایید",
                            when (session.approvalStatus) {
                                "pending" -> "در انتظار تایید"
                                "rejected" -> "رد شده"
                                else -> "تایید شده"
                            },
                            palette,
                        )
                    }
                }

                if (!session.isCurrent) {
                    if (session.approvalStatus == "pending") {
                        Button(
                            onClick = onApprove,
                            enabled = !isMutating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تایید این ورود", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedButton(
                        onClick = onTerminate,
                        enabled = !isMutating,
                        border = BorderStroke(1.dp, palette.dangerRed.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.dangerRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("خاتمه دادن به این نشست", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, palette: TelegramDevicesPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = palette.textSecondary, fontSize = 13.sp)
        Text(value, color = palette.textPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoTerminateTtlSheet(
    selectedDays: Int,
    palette: TelegramDevicesPalette,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(7, 30, 90, 180, 365)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.cardBg) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp),
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = palette.sectionTitle)
                    Text(
                        text = "خاتمه خودکار در صورت عدم فعالیت",
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                options.forEach { days ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(days) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatAutoTerminateLabel(days),
                            color = palette.textPrimary,
                            fontSize = 15.sp,
                        )
                        RadioButton(
                            selected = selectedDays == days,
                            onClick = { onSelect(days) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceManualCodeSheet(
    palette: TelegramDevicesPalette,
    isApproving: Boolean,
    onApproveToken: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var manualCode by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.cardBg) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = palette.ctaBlue)
                    Text(
                        text = "اتصال دستگاه با کد دستی",
                        color = palette.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "کد یا لینک اتصال نمایش‌داده‌شده در صفحه ورود ویستا وب را وارد نمایید.",
                    color = palette.textSecondary,
                    fontSize = 13.5.sp,
                )

                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("کد یا لینک QR") },
                    placeholder = { Text("vista://login?token=...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                )

                Button(
                    onClick = { onApproveToken(manualCode) },
                    enabled = manualCode.isNotBlank() && !isApproving,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.ctaBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                ) {
                    if (isApproving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("تایید و ورود دستگاه", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
