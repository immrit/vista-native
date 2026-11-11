package ir.coffevista.vista_native.features.chat.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ir.coffevista.vista_native.core.common.toPersianDigits
import ir.coffevista.vista_native.core.designsystem.component.VistaGalleryFilter
import ir.coffevista.vista_native.core.designsystem.component.ensureShareableContentUri
import ir.coffevista.vista_native.core.designsystem.glass.LocalVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassHalo
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

private enum class AttachmentSheetTab {
    GALLERY,
    DOCUMENT,
    MUSIC,
    CAMERA,
    DUEL,
}

private val TelegramGlassBlue = Color(0xFF2AABEE)

/**
 * باتم‌شیت پیوست صفحه چت به سبک تلگرام با طراحی Liquid Glass:
 * - باز شدن مستقیم روی گالری ۳ ستونه با کاشی پیش‌نمایش دوربین در بالای گرید و انتخاب چندگانه
 * - نمایش و جستجوی مستقیم فایل‌ها و موسیقی‌های حافظه گوشی درون همین باتم‌شیت بدون باز کردن فایل‌منیجر سیستم
 * - کپسول شناور Liquid Glass در پایین شیت شامل تب‌های پیوست (گالری، فایل، موسیقی، دوربین، دوئل)
 *   که محتوای شیت هنگام اسکرول به‌صورت زنده از پشت آن بلور می‌شوند.
 */
@Composable
fun ChatAttachmentBottomSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>) -> Unit,
    onFilesSelected: (List<Uri>) -> Unit,
    onDuelInviteSelected: (() -> Unit)? = null,
    glassState: VistaGlassState? = LocalVistaGlassState.current,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    val sheetGlassState = rememberVistaGlassState()
    val gridState = rememberLazyGridState()
    val filesListState = rememberLazyListState()
    val musicListState = rememberLazyListState()

    val selectedGalleryUris = remember { mutableStateListOf<Uri>() }
    val selectedFileUris = remember { mutableStateListOf<Uri>() }
    val selectedFileNamesByUri = remember { mutableStateMapOf<Uri, String>() }

    var galleryFilter by remember { mutableStateOf(VistaGalleryFilter.ALL) }
    var activeTab by remember { mutableStateOf(AttachmentSheetTab.GALLERY) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraPreviewActive by remember { mutableStateOf(true) }
    var isDismissing by remember { mutableStateOf(false) }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val selected = cameraUri
        cameraUri = null
        if (success && selected != null) {
            onMediaSelected(listOf(selected))
            onDismiss()
        } else {
            isCameraPreviewActive = true
            activeTab = AttachmentSheetTab.GALLERY
        }
    }

    val startCameraCapture = remember(context) {
        {
            isCameraPreviewActive = false
            val cacheDir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File(cacheDir, "chat_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(context, "${context.packageName}.files", file).also { uri ->
                cameraUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            startCameraCapture()
        } else {
            activeTab = AttachmentSheetTab.GALLERY
        }
    }

    val launchCameraWithPermission: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            hasCameraPermission = true
            startCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val totalSelectedCount = selectedGalleryUris.size + selectedFileUris.size
    val currentActiveTab by rememberUpdatedState(activeTab)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(900f),
        ) {
            val maxHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            val statusTopPx = WindowInsets.statusBars.getTop(density).toFloat()
            val collapsedHeightPx = (maxHeightPx * 0.62f).coerceIn(320f, maxHeightPx)
            val expandedHeightPx = (maxHeightPx - statusTopPx - with(density) { 12.dp.toPx() })
                .coerceAtLeast(collapsedHeightPx)

            val sheetHeightPx = remember { Animatable(0f) }

            LaunchedEffect(collapsedHeightPx) {
                if (sheetHeightPx.value == 0f && !isDismissing) {
                    sheetHeightPx.animateTo(
                        targetValue = collapsedHeightPx,
                        animationSpec = spring(
                            dampingRatio = 0.86f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }

            fun dismissAnimated() {
                if (isDismissing) return
                isDismissing = true
                isCameraPreviewActive = false
                scope.launch {
                    sheetHeightPx.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 200),
                    )
                    onDismiss()
                }
            }

            suspend fun settleSheet(velocityY: Float = 0f) {
                val current = sheetHeightPx.value
                val midPoint = (collapsedHeightPx + expandedHeightPx) * 0.5f
                val dismissThreshold = collapsedHeightPx * 0.68f
                when {
                    velocityY > 1200f && current <= collapsedHeightPx + 40f -> dismissAnimated()
                    velocityY < -1200f -> sheetHeightPx.animateTo(
                        expandedHeightPx,
                        spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium),
                    )
                    current < dismissThreshold -> dismissAnimated()
                    current > midPoint -> sheetHeightPx.animateTo(
                        expandedHeightPx,
                        spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium),
                    )
                    else -> sheetHeightPx.animateTo(
                        collapsedHeightPx,
                        spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium),
                    )
                }
            }

            BackHandler(enabled = true) {
                dismissAnimated()
            }

            val enterProgress = (sheetHeightPx.value / collapsedHeightPx).coerceIn(0f, 1f)
            val sheetNestedScroll = remember(collapsedHeightPx, expandedHeightPx) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (source != NestedScrollSource.UserInput) return Offset.Zero
                        val deltaY = available.y
                        // کشیدن به بالا وقتی شیت هنوز کاملاً باز نشده است
                        if (deltaY < 0f && sheetHeightPx.value < expandedHeightPx - 0.5f) {
                            val next = (sheetHeightPx.value - deltaY).coerceAtMost(expandedHeightPx)
                            val consumedY = -(next - sheetHeightPx.value)
                            scope.launch { sheetHeightPx.snapTo(next) }
                            return Offset(0f, consumedY)
                        }
                        // کشیدن به پایین وقتی لیست/گرید در بالای صفحه است
                        val atTop = when (currentActiveTab) {
                            AttachmentSheetTab.DOCUMENT ->
                                filesListState.firstVisibleItemIndex == 0 &&
                                    filesListState.firstVisibleItemScrollOffset == 0
                            AttachmentSheetTab.MUSIC ->
                                musicListState.firstVisibleItemIndex == 0 &&
                                    musicListState.firstVisibleItemScrollOffset == 0
                            else ->
                                gridState.firstVisibleItemIndex == 0 &&
                                    gridState.firstVisibleItemScrollOffset == 0
                        }
                        if (deltaY > 0f && atTop && sheetHeightPx.value > 0f) {
                            val next = (sheetHeightPx.value - deltaY).coerceAtLeast(0f)
                            val consumedY = sheetHeightPx.value - next
                            scope.launch { sheetHeightPx.snapTo(next) }
                            return Offset(0f, consumedY)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        if (sheetHeightPx.value in 1f..(expandedHeightPx - 1f) &&
                            sheetHeightPx.value != collapsedHeightPx
                        ) {
                            settleSheet(available.y)
                        }
                        return Velocity.Zero
                    }
                }
            }

            // پس‌زمینه نیمه‌تاریک پشت شیت
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (if (isDark) 0.50f else 0.34f) * enterProgress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = ::dismissAnimated,
                    ),
            )

            // بدنه اصلی باتم‌شیت شیشه‌ای تلگرامی
            val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            val sheetTint = if (isDark) Color(0xFF141B23) else Color(0xFFF6F8FB)
            val currentHeightDp = with(density) { sheetHeightPx.value.roundToInt().coerceAtLeast(0).toDp() }
            val navBottomInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(currentHeightDp)
                    .then(
                        if (glassState != null) {
                            Modifier.vistaGlass(
                                state = glassState,
                                shape = sheetShape,
                                tint = sheetTint,
                                isDark = isDark,
                                blurRadius = VistaGlassTokens.PopupBlur,
                                tintAlphaOverride = if (isDark) 0.88f else 0.92f,
                                drawHighlight = true,
                                rimWidth = 0.8.dp,
                                enableRefraction = false,
                                shadowElevation = 18.dp,
                            )
                        } else {
                            Modifier
                                .shadow(18.dp, sheetShape)
                                .clip(sheetShape)
                                .background(sheetTint.copy(alpha = 0.97f))
                                .border(
                                    width = 0.8.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                                    shape = sheetShape,
                                )
                        }
                    )
                    .clip(sheetShape)
                    .nestedScroll(sheetNestedScroll)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // هندل کشیدن و هدر مینیمال بالای گالری
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(collapsedHeightPx, expandedHeightPx) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dragAmount ->
                                        val next = (sheetHeightPx.value - dragAmount)
                                            .coerceIn(0f, expandedHeightPx)
                                        scope.launch { sheetHeightPx.snapTo(next) }
                                    },
                                    onDragEnd = {
                                        scope.launch { settleSheet() }
                                    },
                                    onDragCancel = {
                                        scope.launch { settleSheet() }
                                    },
                                )
                            }
                            .padding(top = 8.dp, bottom = 4.dp, start = 12.dp, end = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isDark) Color.White.copy(alpha = 0.28f)
                                    else Color.Black.copy(alpha = 0.18f),
                                ),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val headerTitle = when {
                                totalSelectedCount > 0 -> "$totalSelectedCount مورد انتخاب شده".toPersianDigits()
                                activeTab == AttachmentSheetTab.DOCUMENT -> "فایل‌ها"
                                activeTab == AttachmentSheetTab.MUSIC -> "موسیقی"
                                else -> "گالری"
                            }
                            Text(
                                text = headerTitle,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (activeTab == AttachmentSheetTab.GALLERY) {
                                    listOf(
                                        VistaGalleryFilter.ALL to "همه",
                                        VistaGalleryFilter.IMAGES to "عکس",
                                        VistaGalleryFilter.VIDEOS to "ویدیو",
                                    ).forEach { (option, label) ->
                                        val isFilterSelected = galleryFilter == option
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isFilterSelected) TelegramGlassBlue.copy(alpha = if (isDark) 0.24f else 0.14f)
                                                    else Color.Transparent,
                                                )
                                                .clickable { galleryFilter = option }
                                                .padding(horizontal = 9.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isFilterSelected) TelegramGlassBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = ::dismissAnimated,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "بستن",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    // محتوای تب فعال درون شیت (به‌عنوان سورس بلور زنده برای کپسول شیشه‌ای پایین)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        val bottomSpacer = 92.dp + navBottomInset
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                fadeIn(tween(170)).togetherWith(fadeOut(tween(130)))
                            },
                            label = "attachment-sheet-tab-content",
                        ) { tab ->
                            when (tab) {
                                AttachmentSheetTab.DOCUMENT -> {
                                    ChatAttachmentFilesTabContent(
                                        selectedUris = selectedFileUris,
                                        selectedNamesByUri = selectedFileNamesByUri,
                                        listState = filesListState,
                                        glassSourceState = sheetGlassState,
                                        bottomContentPadding = bottomSpacer,
                                    )
                                }

                                AttachmentSheetTab.MUSIC -> {
                                    ChatAttachmentMusicTabContent(
                                        selectedUris = selectedFileUris,
                                        listState = musicListState,
                                        glassSourceState = sheetGlassState,
                                        bottomContentPadding = bottomSpacer,
                                    )
                                }

                                else -> {
                                    TelegramGalleryGridContent(
                                        selectedUris = selectedGalleryUris,
                                        filter = galleryFilter,
                                        onCameraTileClick = launchCameraWithPermission,
                                        hasCameraPermission = hasCameraPermission,
                                        isCameraPreviewActive = isCameraPreviewActive && activeTab == AttachmentSheetTab.GALLERY,
                                        gridState = gridState,
                                        glassSourceState = sheetGlassState,
                                        bottomContentPadding = bottomSpacer,
                                    )
                                }
                            }
                        }
                    }
                }

                // کپسول شناور Liquid Glass در پایین شیت (مشابه تلگرام)
                if (enterProgress > 0.25f) {
                    AttachmentGlassFloatingBar(
                        glassState = sheetGlassState,
                        isDark = isDark,
                        navBottomInset = navBottomInset,
                        activeTab = activeTab,
                        selectedCount = totalSelectedCount,
                        showDuelInvite = onDuelInviteSelected != null,
                        onSelectGallery = {
                            isCameraPreviewActive = true
                            activeTab = AttachmentSheetTab.GALLERY
                        },
                        onSelectDocument = {
                            isCameraPreviewActive = false
                            activeTab = AttachmentSheetTab.DOCUMENT
                        },
                        onSelectMusic = {
                            isCameraPreviewActive = false
                            activeTab = AttachmentSheetTab.MUSIC
                        },
                        onSelectCamera = {
                            activeTab = AttachmentSheetTab.CAMERA
                            launchCameraWithPermission()
                        },
                        onSelectDuelInvite = {
                            activeTab = AttachmentSheetTab.DUEL
                            onDismiss()
                            onDuelInviteSelected?.invoke()
                        },
                        onClearSelection = {
                            selectedGalleryUris.clear()
                            selectedFileUris.clear()
                            selectedFileNamesByUri.clear()
                        },
                        onSendSelected = {
                            val galleryToSend = selectedGalleryUris.toList()
                            val filesToSend = selectedFileUris.toList()
                            val namesSnapshot = selectedFileNamesByUri.toMap()
                            scope.launch {
                                if (filesToSend.isNotEmpty()) {
                                    val shareableFiles = filesToSend.map { uri ->
                                        ensureShareableContentUri(
                                            context = context,
                                            uri = uri,
                                            fallbackDisplayName = namesSnapshot[uri].orEmpty(),
                                        )
                                    }
                                    onFilesSelected(shareableFiles)
                                }
                                if (galleryToSend.isNotEmpty()) {
                                    onMediaSelected(galleryToSend)
                                }
                                dismissAnimated()
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentGlassFloatingBar(
    glassState: VistaGlassState,
    isDark: Boolean,
    navBottomInset: androidx.compose.ui.unit.Dp,
    activeTab: AttachmentSheetTab,
    selectedCount: Int,
    showDuelInvite: Boolean,
    onSelectGallery: () -> Unit,
    onSelectDocument: () -> Unit,
    onSelectMusic: () -> Unit,
    onSelectCamera: () -> Unit,
    onSelectDuelInvite: () -> Unit,
    onClearSelection: () -> Unit,
    onSendSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capsuleShape = RoundedCornerShape(32.dp)
    val capsuleTint = if (isDark) Color(0xFF1A232E) else Color.White
    val activeAccent = TelegramGlassBlue
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.78f) else Color(0xFF334155)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp + navBottomInset)
            .vistaGlassHalo(
                state = glassState,
                isDark = isDark,
                tint = if (isDark) Color(0xFF10161E) else Color(0xFFF6F8FB),
                blurRadius = 22.dp,
                startOffsetFraction = 0.14f,
                maxAlpha = if (isDark) 0.58f else 0.62f,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp, bottom = 10.dp + navBottomInset)
                .fillMaxWidth()
                .height(64.dp)
                .vistaGlass(
                    state = glassState,
                    shape = capsuleShape,
                    tint = capsuleTint,
                    isDark = isDark,
                    blurRadius = VistaGlassTokens.BarBlur,
                    tintAlphaOverride = if (isDark) 0.76f else 0.84f,
                    drawHighlight = true,
                    rimWidth = 0.8.dp,
                    shadowElevation = 12.dp,
                ),
            shape = capsuleShape,
            color = Color.Transparent,
            shadowElevation = 0.dp,
        ) {
            AnimatedContent(
                targetState = selectedCount > 0,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f))
                },
                label = "attachment-bottom-capsule",
            ) { hasSelection ->
                if (hasSelection) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(role = Role.Button, onClick = onClearSelection)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "لغو",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = "$selectedCount مورد انتخاب شده".toPersianDigits(),
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(TelegramGlassBlue, VistaBrandColors.Indigo)
                                    ),
                                )
                                .clickable(role = Role.Button, onClick = onSendSelected)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    text = "ارسال ($selectedCount)".toPersianDigits(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        AttachmentGlassTabItem(
                            icon = if (activeTab == AttachmentSheetTab.GALLERY) Icons.Default.Image else Icons.Outlined.Image,
                            label = "گالری",
                            selected = activeTab == AttachmentSheetTab.GALLERY,
                            isDark = isDark,
                            activeColor = activeAccent,
                            inactiveColor = inactiveColor,
                            onClick = onSelectGallery,
                            modifier = Modifier.weight(1f),
                        )
                        AttachmentGlassTabItem(
                            icon = if (activeTab == AttachmentSheetTab.DOCUMENT) Icons.Default.Description else Icons.Outlined.Description,
                            label = "فایل",
                            selected = activeTab == AttachmentSheetTab.DOCUMENT,
                            isDark = isDark,
                            activeColor = activeAccent,
                            inactiveColor = inactiveColor,
                            onClick = onSelectDocument,
                            modifier = Modifier.weight(1f),
                        )
                        AttachmentGlassTabItem(
                            icon = if (activeTab == AttachmentSheetTab.MUSIC) Icons.Default.Audiotrack else Icons.Outlined.Audiotrack,
                            label = "موسیقی",
                            selected = activeTab == AttachmentSheetTab.MUSIC,
                            isDark = isDark,
                            activeColor = activeAccent,
                            inactiveColor = inactiveColor,
                            onClick = onSelectMusic,
                            modifier = Modifier.weight(1f),
                        )
                        AttachmentGlassTabItem(
                            icon = if (activeTab == AttachmentSheetTab.CAMERA) Icons.Default.CameraAlt else Icons.Outlined.CameraAlt,
                            label = "دوربین",
                            selected = activeTab == AttachmentSheetTab.CAMERA,
                            isDark = isDark,
                            activeColor = activeAccent,
                            inactiveColor = inactiveColor,
                            onClick = onSelectCamera,
                            modifier = Modifier.weight(1f),
                        )
                        if (showDuelInvite) {
                            AttachmentGlassTabItem(
                                icon = if (activeTab == AttachmentSheetTab.DUEL) Icons.Default.SportsEsports else Icons.Outlined.SportsEsports,
                                label = "دوئل",
                                selected = activeTab == AttachmentSheetTab.DUEL,
                                isDark = isDark,
                                activeColor = activeAccent,
                                inactiveColor = inactiveColor,
                                onClick = onSelectDuelInvite,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentGlassTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    isDark: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(26.dp)
    val tintColor = if (selected) activeColor else inactiveColor
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(pillShape)
            .then(
                if (selected) {
                    Modifier
                        .background(
                            activeColor.copy(alpha = if (isDark) 0.22f else 0.13f),
                            pillShape,
                        )
                        .border(
                            width = 0.7.dp,
                            color = activeColor.copy(alpha = if (isDark) 0.40f else 0.28f),
                            shape = pillShape,
                        )
                } else {
                    Modifier
                }
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(21.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = tintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
