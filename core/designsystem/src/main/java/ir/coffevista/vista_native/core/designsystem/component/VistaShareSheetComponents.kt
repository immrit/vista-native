package ir.coffevista.vista_native.core.designsystem.component

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Unified dark share-sheet design tokens shared identically across Feed and Profile.
 */
object VistaShareSheetTokens {
    val SheetBackground = Color(0xFF16171A)
    val CardBackground = Color(0xFF212328)
    val CardBorder = Color(0xFF2E3138)
    val CircleBackground = Color(0xFF26282E)
    val CircleBorder = Color(0xFF383B44)
    val DividerColor = Color(0xFF2A2C32)
    val HandleColor = Color(0xFF52555E)
    val AccentBlue = VistaBrandColors.Indigo
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFA0A4B0)
}

data class VistaShareRailAction(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
)

/**
 * Single unified Instagram + Threads style share sheet template used identically across Feed and Profile.
 * Displays a top drag handle, search bar, 3-column grid of most-interacted / frequently shared-to contacts,
 * and either a primary Send CTA (when contacts are selected) or the horizontal app/action rail below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VistaUnifiedShareSheet(
    onDismiss: () -> Unit,
    actions: List<VistaShareRailAction>,
    onSendToRecipients: suspend (gateway: VistaDirectShareGateway, recipients: List<VistaShareRecipient>) -> Int,
    onSentSuccess: (sentCount: Int, recipients: List<VistaShareRecipient>) -> Unit = { _, _ -> },
    sendFailureMessage: String = "ارسال انجام نشد. دوباره تلاش کنید.",
) {
    val context = LocalContext.current
    val gateway = LocalVistaDirectShareGateway.current ?: VistaDirectShareRegistry.current
    val scope = rememberCoroutineScope()

    val frequentRecipients by remember(gateway) {
        gateway?.observeFrequentRecipients(limit = 18) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var remoteResults by remember { mutableStateOf<List<VistaShareRecipient>>(emptyList()) }
    var isSearchingRemote by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedCache by remember { mutableStateOf<Map<String, VistaShareRecipient>>(emptyMap()) }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(gateway) {
        gateway?.refreshFrequentRecipients()
    }

    LaunchedEffect(searchQuery, gateway) {
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank() || gateway == null) {
            remoteResults = emptyList()
            isSearchingRemote = false
            return@LaunchedEffect
        }
        isSearchingRemote = true
        delay(220)
        remoteResults = runCatching { gateway.searchRecipients(trimmed) }.getOrDefault(emptyList())
        isSearchingRemote = false
    }

    val displayedRecipients = remember(frequentRecipients, remoteResults, searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            frequentRecipients
        } else {
            val localMatches = frequentRecipients.filter {
                it.displayName.contains(trimmed, ignoreCase = true) ||
                    it.username.contains(trimmed, ignoreCase = true)
            }
            val merged = LinkedHashMap<String, VistaShareRecipient>(localMatches.size + remoteResults.size)
            localMatches.forEach { merged[it.id] = it }
            remoteResults.forEach { user ->
                if (!merged.containsKey(user.id)) merged[user.id] = user
            }
            merged.values.toList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSending) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VistaShareSheetTokens.SheetBackground,
        scrimColor = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 14.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(VistaShareSheetTokens.HandleColor),
                )

                VistaShareSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(16.dp))

                when {
                    displayedRecipients.isEmpty() && isSearchingRemote -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = VistaShareSheetTokens.TextSecondary,
                                strokeWidth = 2.2.dp,
                            )
                        }
                    }

                    displayedRecipients.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "هنوز مخاطبی برای ارسال مستقیم ثبت نشده است"
                                } else {
                                    "کاربری یافت نشد"
                                },
                                fontSize = 13.sp,
                                color = VistaShareSheetTokens.TextSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 256.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(displayedRecipients, key = { it.id }) { recipient ->
                                val isSelected = recipient.id in selectedIds
                                VistaShareRecipientTile(
                                    recipient = recipient,
                                    selected = isSelected,
                                    gateway = gateway,
                                    onClick = {
                                        if (isSending) return@VistaShareRecipientTile
                                        if (isSelected) {
                                            selectedIds = selectedIds - recipient.id
                                            selectedCache = selectedCache - recipient.id
                                        } else {
                                            selectedIds = selectedIds + recipient.id
                                            selectedCache = selectedCache + (recipient.id to recipient)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (selectedIds.isNotEmpty() && gateway != null) {
                    val targets = selectedIds.mapNotNull { id ->
                        selectedCache[id] ?: displayedRecipients.firstOrNull { it.id == id }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = VistaShareSheetTokens.SheetBackground,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Button(
                                enabled = !isSending && targets.isNotEmpty(),
                                onClick = {
                                    isSending = true
                                    scope.launch {
                                        val sentCount = runCatching {
                                            onSendToRecipients(gateway, targets)
                                        }.getOrDefault(0)
                                        isSending = false
                                        if (sentCount > 0) {
                                            onSentSuccess(sentCount, targets)
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, sendFailureMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VistaShareSheetTokens.AccentBlue,
                                    contentColor = Color.White,
                                ),
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    val label = if (targets.size == 1) {
                                        "ارسال به ${targets.first().displayName}"
                                    } else {
                                        "ارسال به ${targets.size} مخاطب"
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    VistaShareActionRail(actions = actions)
                }
            }
        }
    }
}

@Composable
private fun VistaShareSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = VistaShareSheetTokens.CardBackground,
        border = BorderStroke(1.dp, VistaShareSheetTokens.CardBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val r = size.minDimension * 0.34f
                val cx = size.width * 0.42f
                val cy = size.height * 0.42f
                val stroke = 1.8.dp.toPx()
                drawCircle(
                    color = VistaShareSheetTokens.TextSecondary,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = VistaShareSheetTokens.TextSecondary,
                    start = Offset(cx + r * 0.72f, cy + r * 0.72f),
                    end = Offset(size.width * 0.88f, size.height * 0.88f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "جستجو...",
                        fontSize = 14.sp,
                        color = VistaShareSheetTokens.TextSecondary,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = VistaShareSheetTokens.TextPrimary,
                    ),
                    cursorBrush = SolidColor(VistaShareSheetTokens.AccentBlue),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(VistaShareSheetTokens.CircleBackground)
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        val s = 1.6.dp.toPx()
                        drawLine(Color.White, Offset(0f, 0f), Offset(size.width, size.height), s, StrokeCap.Round)
                        drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), s, StrokeCap.Round)
                    }
                }
            }
        }
    }
}

@Composable
private fun VistaShareRecipientTile(
    recipient: VistaShareRecipient,
    selected: Boolean,
    gateway: VistaDirectShareGateway?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(70.dp),
            contentAlignment = Alignment.Center,
        ) {
            VistaShareRecipientAvatar(
                avatarUrl = recipient.avatarUrl,
                displayName = recipient.displayName,
                gateway = gateway,
                size = 64.dp,
                selected = selected,
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(VistaShareSheetTokens.AccentBlue)
                        .border(2.dp, VistaShareSheetTokens.SheetBackground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        val p = Path().apply {
                            moveTo(size.width * 0.16f, size.height * 0.54f)
                            lineTo(size.width * 0.42f, size.height * 0.80f)
                            lineTo(size.width * 0.86f, size.height * 0.24f)
                        }
                        drawPath(
                            path = p,
                            color = Color.White,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp),
        ) {
            Text(
                text = recipient.displayName,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) VistaShareSheetTokens.AccentBlue else VistaShareSheetTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (recipient.isVerified) {
                Spacer(Modifier.width(3.dp))
                VistaVerificationBadge(
                    isVerified = true,
                    verificationType = recipient.verificationType,
                    role = recipient.role,
                    size = 13.dp,
                )
            }
        }
    }
}

@Composable
private fun VistaShareRecipientAvatar(
    avatarUrl: String?,
    displayName: String,
    gateway: VistaDirectShareGateway?,
    size: Dp,
    selected: Boolean,
) {
    val context = LocalContext.current
    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, avatarUrl, gateway) {
        val url = avatarUrl?.trim()?.takeIf(String::isNotEmpty)
        value = if (url != null && gateway != null) {
            runCatching { gateway.loadAvatarBitmap(context, url) }.getOrNull()
        } else {
            null
        }
    }

    val ringColor = if (selected) VistaShareSheetTokens.AccentBlue else VistaShareSheetTokens.CircleBorder
    val ringWidth = if (selected) 2.5.dp else 1.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(VistaShareSheetTokens.CircleBackground)
            .border(ringWidth, ringColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = avatarBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
            Text(
                text = initial,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
fun VistaShareActionRail(
    actions: List<VistaShareRailAction>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = VistaShareSheetTokens.DividerColor,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(actions, key = { it.id }) { action ->
                VistaShareRailButton(action = action)
            }
        }
    }
}

@Composable
private fun VistaShareRailButton(action: VistaShareRailAction) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable(
                enabled = action.enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = action.onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable(enabled = action.enabled, onClick = action.onClick),
            contentAlignment = Alignment.Center,
        ) {
            action.icon()
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = action.label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (action.enabled) VistaShareSheetTokens.TextPrimary else VistaShareSheetTokens.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Loads and renders the genuine OS application icon for any installed app package (`PackageManager.getApplicationIcon`),
 * falling back cleanly when that third-party app is not installed on the current device.
 */
@Composable
fun VistaInstalledAppCircleIcon(
    packageNames: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    showStoryPlusBadge: Boolean = false,
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val appIconBitmap = remember(context, packageNames) {
        val pm = context.packageManager
        packageNames.firstNotNullOfOrNull { pkg ->
            runCatching { pm.getApplicationIcon(pkg).toShareIconImageBitmap() }.getOrNull()
        }
    }

    if (appIconBitmap == null) {
        fallback()
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = appIconBitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        )
        if (showStoryPlusBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDD2A7B))
                    .border(2.dp, VistaShareSheetTokens.SheetBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(9.dp)) {
                    val s = 1.8.dp.toPx()
                    drawLine(Color.White, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), s, StrokeCap.Round)
                    drawLine(Color.White, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), s, StrokeCap.Round)
                }
            }
        }
    }
}

private fun Drawable.toShareIconImageBitmap(sizePx: Int = 160): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null && !bitmap.isRecycled) {
        return bitmap.asImageBitmap()
    }
    val w = intrinsicWidth.takeIf { it > 0 } ?: sizePx
    val h = intrinsicHeight.takeIf { it > 0 } ?: sizePx
    val targetW = w.coerceAtLeast(sizePx)
    val targetH = h.coerceAtLeast(sizePx)
    val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    setBounds(0, 0, targetW, targetH)
    draw(canvas)
    return bmp.asImageBitmap()
}

/**
 * Dashed Story circle with "+" matching Instagram's "Add to story" glyph.
 */
@Composable
fun VistaAddToStoryCircleIcon(
    gradientAccent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val brush = if (gradientAccent) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF58529),
                Color(0xFFDD2A7B),
                Color(0xFF8134AF),
                Color(0xFF515BD4),
            ),
        )
    } else {
        Brush.linearGradient(listOf(Color.White, Color.White))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VistaShareSheetTokens.CircleBackground, CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val radius = size.minDimension / 2f - 2.dp.toPx()
            drawCircle(
                brush = brush,
                radius = radius,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 8f), 0f),
                ),
            )
            val arm = 6.dp.toPx()
            val c = center
            drawLine(
                brush = brush,
                start = Offset(c.x - arm, c.y),
                end = Offset(c.x + arm, c.y),
                strokeWidth = 2.1.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                brush = brush,
                start = Offset(c.x, c.y - arm),
                end = Offset(c.x, c.y + arm),
                strokeWidth = 2.1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun VistaCopyLinkCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VistaShareSheetTokens.CircleBackground, CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(23.dp)) {
            rotate(-45f) {
                val stroke = 2.1.dp.toPx()
                val linkW = size.width * 0.42f
                val linkH = size.height * 0.28f
                val r = linkH / 2f
                val cy = size.height / 2f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(size.width * 0.08f, cy - linkH / 2f),
                    size = androidx.compose.ui.geometry.Size(linkW, linkH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                    style = Stroke(width = stroke),
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(size.width * 0.50f, cy - linkH / 2f),
                    size = androidx.compose.ui.geometry.Size(linkW, linkH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.34f, cy),
                    end = Offset(size.width * 0.66f, cy),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
fun VistaQrCodeCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VistaShareSheetTokens.CircleBackground, CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val s = 1.9.dp.toPx()
            val q = size.width * 0.36f
            val r = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
            drawRoundRect(Color.White, Offset(0f, 0f), androidx.compose.ui.geometry.Size(q, q), r, Stroke(s))
            drawRoundRect(Color.White, Offset(size.width - q, 0f), androidx.compose.ui.geometry.Size(q, q), r, Stroke(s))
            drawRoundRect(Color.White, Offset(0f, size.height - q), androidx.compose.ui.geometry.Size(q, q), r, Stroke(s))
            drawRect(Color.White, Offset(size.width * 0.56f, size.height * 0.56f), androidx.compose.ui.geometry.Size(size.width * 0.18f, size.height * 0.18f))
            drawRect(Color.White, Offset(size.width * 0.78f, size.height * 0.78f), androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height * 0.22f))
        }
    }
}

@Composable
private fun FallbackSocialCircle(
    bgColor: Color,
    borderColor: Color = Color.Transparent,
    label: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor, CircleShape)
            .let {
                if (borderColor != Color.Transparent) it.border(1.dp, borderColor, CircleShape) else it
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
fun VistaMoreShareCircleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VistaShareSheetTokens.CircleBackground, CircleShape)
            .border(1.dp, VistaShareSheetTokens.CircleBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.0.dp.toPx()
            val tray = Path().apply {
                moveTo(w * 0.18f, h * 0.48f)
                lineTo(w * 0.18f, h * 0.82f)
                lineTo(w * 0.82f, h * 0.82f)
                lineTo(w * 0.82f, h * 0.48f)
            }
            drawPath(
                path = tray,
                color = Color.White,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawLine(
                color = Color.White,
                start = Offset(w * 0.50f, h * 0.62f),
                end = Offset(w * 0.50f, h * 0.16f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            val arrow = Path().apply {
                moveTo(w * 0.33f, h * 0.33f)
                lineTo(w * 0.50f, h * 0.15f)
                lineTo(w * 0.67f, h * 0.33f)
            }
            drawPath(
                path = arrow,
                color = Color.White,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

fun shareTextToTelegram(context: Context, text: String, shareUrl: String) {
    val telegramPackages = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
    )
    for (pkg in telegramPackages) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = pkg
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Try next client package
        }
    }
    try {
        val webUri = Uri.parse("https://t.me/share/url").buildUpon()
            .appendQueryParameter("url", shareUrl)
            .appendQueryParameter("text", text)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        shareTextViaSystemChooser(context, text, "اشتراک‌گذاری در تلگرام")
    }
}

fun shareTextToWhatsApp(context: Context, text: String) {
    for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = pkg
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Try next WhatsApp package
        }
    }
    shareTextViaSystemChooser(context, text, "اشتراک‌گذاری در واتساپ")
}

fun shareTextToThreads(context: Context, text: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.instagram.barcelona"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    } catch (_: Exception) {
        // Fallback to Threads web composer
    }
    try {
        val webUri = Uri.parse("https://www.threads.net/intent/post").buildUpon()
            .appendQueryParameter("text", text)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        shareTextViaSystemChooser(context, text, "اشتراک‌گذاری در تردز")
    }
}

fun shareTextToX(context: Context, text: String) {
    for (pkg in listOf("com.twitter.android", "com.twitter.android.lite")) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = pkg
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Fallback to X web intent
        }
    }
    try {
        val webUri = Uri.parse("https://twitter.com/intent/tweet").buildUpon()
            .appendQueryParameter("text", text)
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        shareTextViaSystemChooser(context, text, "اشتراک‌گذاری در X")
    }
}

fun shareTextViaSystemChooser(context: Context, text: String, title: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}

/**
 * Builds the single unified horizontal share rail shared across Feed and Profile,
 * displaying real installed app icons via [PackageManager.getApplicationIcon].
 */
fun buildVistaStandardShareActions(
    isBusy: Boolean,
    showAddToVistaStory: Boolean = true,
    onAddToVistaStory: () -> Unit,
    onShareInstagramStory: () -> Unit,
    onCopyLink: () -> Unit,
    onShareTelegram: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareThreads: () -> Unit,
    onShareX: () -> Unit,
    onShareMore: () -> Unit,
    onShowQrCard: (() -> Unit)? = null,
): List<VistaShareRailAction> = buildList {
    if (showAddToVistaStory) {
        add(
            VistaShareRailAction(
                id = "add_to_story",
                label = if (isBusy) "در حال ساخت..." else "استوری ویستا",
                enabled = !isBusy,
                onClick = onAddToVistaStory,
                icon = { VistaAddToStoryCircleIcon(gradientAccent = false) },
            ),
        )
    }
    add(
        VistaShareRailAction(
            id = "instagram_story",
            label = if (isBusy) "در حال ساخت..." else "استوری اینستا",
            enabled = !isBusy,
            onClick = onShareInstagramStory,
            icon = {
                VistaInstalledAppCircleIcon(
                    packageNames = listOf("com.instagram.android"),
                    contentDescription = "استوری اینستاگرام",
                    showStoryPlusBadge = true,
                    fallback = { VistaAddToStoryCircleIcon(gradientAccent = true) },
                )
            },
        ),
    )
    add(
        VistaShareRailAction(
            id = "copy_link",
            label = "کپی لینک",
            onClick = onCopyLink,
            icon = { VistaCopyLinkCircleIcon() },
        ),
    )
    add(
        VistaShareRailAction(
            id = "telegram",
            label = "تلگرام",
            onClick = onShareTelegram,
            icon = {
                VistaInstalledAppCircleIcon(
                    packageNames = listOf(
                        "org.telegram.messenger",
                        "org.telegram.messenger.web",
                        "org.thunderdog.challegram",
                    ),
                    contentDescription = "تلگرام",
                    fallback = { FallbackSocialCircle(Color(0xFF2AABEE), label = "T") },
                )
            },
        ),
    )
    add(
        VistaShareRailAction(
            id = "whatsapp",
            label = "واتساپ",
            onClick = onShareWhatsApp,
            icon = {
                VistaInstalledAppCircleIcon(
                    packageNames = listOf("com.whatsapp", "com.whatsapp.w4b"),
                    contentDescription = "واتساپ",
                    fallback = { FallbackSocialCircle(Color(0xFF25D366), label = "W") },
                )
            },
        ),
    )
    add(
        VistaShareRailAction(
            id = "threads",
            label = "تردز",
            onClick = onShareThreads,
            icon = {
                VistaInstalledAppCircleIcon(
                    packageNames = listOf("com.instagram.barcelona"),
                    contentDescription = "تردز",
                    fallback = {
                        FallbackSocialCircle(
                            bgColor = Color(0xFF101012),
                            borderColor = Color(0xFF3A3C44),
                            label = "@",
                        )
                    },
                )
            },
        ),
    )
    add(
        VistaShareRailAction(
            id = "x",
            label = "X",
            onClick = onShareX,
            icon = {
                VistaInstalledAppCircleIcon(
                    packageNames = listOf("com.twitter.android", "com.twitter.android.lite"),
                    contentDescription = "X",
                    fallback = {
                        FallbackSocialCircle(
                            bgColor = Color.Black,
                            borderColor = Color(0xFF3A3C44),
                            label = "𝕏",
                        )
                    },
                )
            },
        ),
    )
    if (onShowQrCard != null) {
        add(
            VistaShareRailAction(
                id = "qr_card",
                label = "کارت QR",
                onClick = onShowQrCard,
                icon = { VistaQrCodeCircleIcon() },
            ),
        )
    }
    add(
        VistaShareRailAction(
            id = "more",
            label = "بیشتر",
            onClick = onShareMore,
            icon = { VistaMoreShareCircleIcon() },
        ),
    )
}
