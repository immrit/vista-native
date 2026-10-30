package ir.coffevista.vista_native.features.chat.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect as AndroidRect
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.ViewTreeObserver
import androidx.core.content.FileProvider
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import ir.coffevista.vista_native.features.chat.presentation.components.VoiceRecorderDock
import ir.coffevista.vista_native.features.chat.presentation.components.SwipeToReplyLayout
import ir.coffevista.vista_native.features.chat.presentation.components.VoicePlayerBubble
import ir.coffevista.vista_native.core.designsystem.component.VistaBadgeDefaults
import ir.coffevista.vista_native.core.designsystem.component.VistaNotificationBadge
import ir.coffevista.vista_native.features.chat.presentation.components.FullscreenMediaViewerDialog
import ir.coffevista.vista_native.features.chat.presentation.components.MediaViewerItem
import ir.coffevista.vista_native.features.chat.presentation.components.ChatAttachmentBottomSheet
import ir.coffevista.vista_native.features.chat.presentation.components.ChatAttachmentReviewSheet
import ir.coffevista.vista_native.features.chat.presentation.components.ChatLinkPreviewCard
import ir.coffevista.vista_native.features.chat.presentation.components.ChatTextBubbleLayout
import ir.coffevista.vista_native.features.chat.presentation.components.PinnedMessagesBar
import ir.coffevista.vista_native.features.chat.presentation.components.SharedPostCard
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostParser
import ir.coffevista.vista_native.features.chat.presentation.components.UnreadMessagesDivider
import ir.coffevista.vista_native.features.chat.presentation.components.extractFirstUrl
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramEmojiText
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramEmoji
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramEmojiParser
import ir.coffevista.vista_native.features.chat.presentation.components.EmojiCursorHelper
import ir.coffevista.vista_native.features.chat.presentation.components.ChatComposerField
import ir.coffevista.vista_native.features.chat.presentation.components.ChatComposerController
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramXMediaPanel
import ir.coffevista.vista_native.features.chat.presentation.components.KeyboardHeightStore
import ir.coffevista.vista_native.features.chat.presentation.components.MessageContextMenu
import ir.coffevista.vista_native.features.chat.presentation.components.MessageContextCapabilities
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import ir.coffevista.vista_native.core.designsystem.theme.LocalChatEntryMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.features.chat.R as ChatR
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.DownloadState
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import ir.coffevista.vista_native.features.chat.domain.model.GifItem
import ir.coffevista.vista_native.features.chat.domain.model.GroupMember
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.ModerationReason
import ir.coffevista.vista_native.features.chat.domain.model.RealtimeConnectionState
import ir.coffevista.vista_native.features.chat.domain.model.ProfileNote
import ir.coffevista.vista_native.features.chat.domain.model.TransferState
import ir.coffevista.vista_native.features.chat.domain.model.inboxPreviewText
import ir.coffevista.vista_native.features.chat.domain.model.peerIdentityKey
import ir.coffevista.vista_native.features.chat.presentation.conversations.ConversationsUiState
import ir.coffevista.vista_native.features.chat.presentation.conversations.ConversationsViewModel
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesUiState
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesViewModel
import ir.coffevista.vista_native.features.chat.presentation.components.SecretChatIntroCard
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramEncryptionKeyDialog
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramAutoDeleteDialog
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramSecretGreen
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramSecretBadgeBg
import java.text.SimpleDateFormat
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ConversationListRoute(
    viewModel: ConversationsViewModel,
    onOpenConversation: (Conversation) -> Unit,
    onNewMessage: () -> Unit,
    onNewSecretChat: () -> Unit = onNewMessage,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConversationListScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onOpenConversation = onOpenConversation,
        onShowArchived = viewModel::showArchived,
        onNewMessage = onNewMessage,
        onNewSecretChat = onNewSecretChat,
        onSaveOwnNote = viewModel::saveOwnNote,
        onDeleteOwnNote = viewModel::deleteOwnNote,
        onReplyToNote = viewModel::replyToNote,
        onToggleConversationFlag = viewModel::toggleFlag,
        onDeleteConversation = viewModel::deleteConversation,
        onAcceptMessageRequest = viewModel::acceptRequest,
        onRejectMessageRequest = viewModel::rejectRequest,
        onActionFeedbackShown = viewModel::clearActionFeedback,
        onPrepareBlockStatus = viewModel::prepareBlockStatus,
        onToggleBlock = viewModel::toggleBlock,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    state: ConversationsUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onShowArchived: (Boolean) -> Unit = {},
    onNewMessage: () -> Unit = {},
    onNewSecretChat: () -> Unit = onNewMessage,
    onSaveOwnNote: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
    onDeleteOwnNote: ((Boolean) -> Unit) -> Unit = { complete -> complete(false) },
    onReplyToNote: (ProfileNote, Conversation, String, (Boolean) -> Unit) -> Unit = { _, _, _, complete -> complete(false) },
    onToggleConversationFlag: (String, String) -> Unit = { _, _ -> },
    onDeleteConversation: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
    onAcceptMessageRequest: (String) -> Unit = {},
    onRejectMessageRequest: (String) -> Unit = {},
    onActionFeedbackShown: () -> Unit = {},
    onPrepareBlockStatus: (String) -> Unit = {},
    onToggleBlock: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    var noteEditorVisible by rememberSaveable { mutableStateOf(false) }
    var peerNoteReply by remember { mutableStateOf<Pair<ProfileNote, Conversation>?>(null) }
    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var selectedConversationIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var deletingConversations by remember { mutableStateOf(emptyList<Conversation>()) }
    var blockingConversation by remember { mutableStateOf<Conversation?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.actionFeedback) {
        state.actionFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            onActionFeedbackShown()
        }
    }
    BackHandler(enabled = selectedConversationIds.isNotEmpty()) {
        selectedConversationIds = emptyList()
    }
    BackHandler(enabled = selectedConversationIds.isEmpty() && searchVisible) {
        searchVisible = false
        searchQuery = ""
    }
    BackHandler(
        enabled = selectedConversationIds.isEmpty() && !searchVisible && state.includeArchived,
    ) {
        onShowArchived(false)
    }
    val visibleConversations = if (searchQuery.isBlank()) {
        state.conversations
    } else {
        state.conversations.filter { conversation ->
            conversation.title.contains(searchQuery, ignoreCase = true) ||
                conversation.lastMessage.orEmpty().contains(searchQuery, ignoreCase = true)
        }
    }
    val requestConversations = if (state.includeArchived) {
        emptyList()
    } else {
        visibleConversations.filter { it.isMessageRequest }.distinctBy { it.peerIdentityKey() }
    }
    val nonRequestConversations = if (state.includeArchived) {
        visibleConversations
    } else {
        visibleConversations.filterNot { it.isMessageRequest }
    }
    val regularConversations = (nonRequestConversations.filter { it.isPinned } +
        nonRequestConversations.filterNot { it.isPinned })
        .distinctBy { it.peerIdentityKey() }
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectedConversationIds.isNotEmpty()) {
                val selected = state.conversations.filter { it.id in selectedConversationIds }
                val shouldPin = selected.any { !it.isPinned }
                val shouldMute = selected.any { !it.isMuted }
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selectedConversationIds = emptyList() }) {
                            Icon(Icons.Default.Close, contentDescription = "خروج از انتخاب گفتگو")
                        }
                    },
                    title = { Text("${selected.size} انتخاب شده", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = {
                            selected.filter { it.isPinned != shouldPin }.forEach { onToggleConversationFlag(it.id, "pin") }
                            selectedConversationIds = emptyList()
                        }) { Icon(Icons.Default.PushPin, contentDescription = if (shouldPin) "سنجاق کردن گفتگوهای انتخاب شده" else "برداشتن سنجاق گفتگوهای انتخاب شده") }
                        IconButton(onClick = {
                            selected.filter { it.isMuted != shouldMute }.forEach { onToggleConversationFlag(it.id, "mute") }
                            selectedConversationIds = emptyList()
                        }) { Icon(if (shouldMute) Icons.Default.NotificationsOff else Icons.Default.Notifications, contentDescription = if (shouldMute) "بی‌صدا کردن گفتگوهای انتخاب شده" else "فعال کردن صدای گفتگوهای انتخاب شده") }
                        IconButton(onClick = {
                            selected.forEach { onToggleConversationFlag(it.id, "archive") }
                            selectedConversationIds = emptyList()
                        }) {
                            Icon(
                                if (state.includeArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                contentDescription = if (state.includeArchived) {
                                    "خارج کردن گفتگوهای انتخاب شده از بایگانی"
                                } else {
                                    "بایگانی گفتگوهای انتخاب شده"
                                },
                            )
                        }
                        IconButton(onClick = { deletingConversations = selected }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف گفتگوهای انتخاب شده", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                )
            } else TopAppBar(
                title = {
                    if (searchVisible) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            decorationBox = { field ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text("جستجو...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    field()
                                }
                            },
                        )
                    } else {
                        Text(
                            if (state.includeArchived) "گفتگوهای بایگانی" else "پیام‌ها",
                            Modifier.semantics { heading() },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    if (state.includeArchived) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            IconButton(onClick = { onShowArchived(false) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                            }
                        }
                    }
                },
                actions = {
                    ConnectionStatusIndicator(state.connectionState)
                    IconButton(
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) searchQuery = ""
                        },
                    ) {
                        Icon(Icons.Default.Search, contentDescription = if (searchVisible) "بستن جستجو" else "جستجو")
                    }
                    Box {
                        IconButton(onClick = { menuVisible = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌های پیام‌ها")
                        }
                        DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                            DropdownMenuItem(
                                text = { Text(if (state.includeArchived) "گفتگوهای اصلی" else "گفتگوهای بایگانی") },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                onClick = {
                                    menuVisible = false
                                    onShowArchived(!state.includeArchived)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("گفتگوی محرمانه جدید") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    menuVisible = false
                                    onNewSecretChat()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isInitialLoading && state.conversations.isEmpty() -> LoadingConversationList()
                state.error != null && state.conversations.isEmpty() -> ErrorState(state.error, onRefresh)
                visibleConversations.isEmpty() -> EmptyConversationState(
                    searchActive = searchQuery.isNotBlank(),
                    archived = state.includeArchived,
                )
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (!state.includeArchived && searchQuery.isBlank()) {
                            item(key = "profile-notes") {
                                Box(Modifier.padding(vertical = 8.dp)) {
                                    ProfileNotesTray(
                                        notes = state.profileNotes,
                                        conversations = state.conversations,
                                        isLoading = state.isNotesLoading,
                                        onOpenOwnNote = { noteEditorVisible = true },
                                        onOpenPeerNote = { note, conversation -> peerNoteReply = note to conversation },
                                    )
                                }
                            }
                        }
                        if (requestConversations.isNotEmpty()) {
                            item(key = "message-request-header") {
                                ConversationSectionHeader(
                                    title = "درخواست پیام",
                                    count = requestConversations.size,
                                )
                            }
                        }
                        items(requestConversations, key = { "request-${it.id}" }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                selected = conversation.id in selectedConversationIds,
                                requestActionLoading = conversation.id in state.requestActionConversationIds,
                                showRequestActions = true,
                                onClick = {
                                    if (selectedConversationIds.isEmpty()) onOpenConversation(conversation)
                                    else selectedConversationIds = if (conversation.id in selectedConversationIds) {
                                        selectedConversationIds - conversation.id
                                    } else selectedConversationIds + conversation.id
                                },
                                onLongClick = {
                                    selectedConversationIds = if (conversation.id in selectedConversationIds) {
                                        selectedConversationIds - conversation.id
                                    } else selectedConversationIds + conversation.id
                                },
                                onTogglePin = { onToggleConversationFlag(conversation.id, "pin") },
                                onToggleMute = { onToggleConversationFlag(conversation.id, "mute") },
                                onToggleArchive = { onToggleConversationFlag(conversation.id, "archive") },
                                onDelete = { deletingConversations = listOf(conversation) },
                                onAcceptRequest = { onAcceptMessageRequest(conversation.id) },
                                onRejectRequest = { onRejectMessageRequest(conversation.id) },
                                onBlock = conversation.takeIf { it.type == ConversationType.PRIVATE }
                                    ?.peerId?.takeIf(String::isNotBlank)?.let { peerId ->
                                    {
                                        blockingConversation = conversation
                                        onPrepareBlockStatus(peerId)
                                    }
                                },
                            )
                        }
                        items(regularConversations, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                selected = conversation.id in selectedConversationIds,
                                onClick = {
                                    if (selectedConversationIds.isEmpty()) onOpenConversation(conversation)
                                    else selectedConversationIds = if (conversation.id in selectedConversationIds) {
                                        selectedConversationIds - conversation.id
                                    } else selectedConversationIds + conversation.id
                                },
                                onLongClick = {
                                    selectedConversationIds = if (conversation.id in selectedConversationIds) {
                                        selectedConversationIds - conversation.id
                                    } else selectedConversationIds + conversation.id
                                },
                                onTogglePin = { onToggleConversationFlag(conversation.id, "pin") },
                                onToggleMute = { onToggleConversationFlag(conversation.id, "mute") },
                                onToggleArchive = { onToggleConversationFlag(conversation.id, "archive") },
                                onDelete = { deletingConversations = listOf(conversation) },
                                onBlock = conversation.takeIf { it.type == ConversationType.PRIVATE }
                                    ?.peerId?.takeIf(String::isNotBlank)?.let { peerId ->
                                    {
                                        blockingConversation = conversation
                                        onPrepareBlockStatus(peerId)
                                    }
                                },
                            )
                        }
                        item(key = "pagination") {
                            if (state.isAppending) CircularProgressIndicator(Modifier.padding(VistaSpacing.Large).size(24.dp))
                            else if (state.hasMore) LaunchedEffect(state.conversations.size) { onLoadMore() }
                        }
                    }
                }
            }
        }
        }
        if (!searchVisible && !state.includeArchived) {
            ir.coffevista.vista_native.core.designsystem.component.VistaFloatingActionButton(
                onClick = onNewMessage,
                contentDescription = "پیام جدید",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(y = -45.dp)
                    .padding(end = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
    if (noteEditorVisible) {
        OwnNoteEditorSheet(
            current = state.profileNotes.firstOrNull { it.isMine && !it.isExpired() },
            saving = state.isNoteSaving,
            error = state.noteError,
            onDismiss = { noteEditorVisible = false },
            onSave = { content ->
                onSaveOwnNote(content) { saved -> if (saved) noteEditorVisible = false }
            },
            onDelete = {
                onDeleteOwnNote { deleted -> if (deleted) noteEditorVisible = false }
            },
        )
    }
    peerNoteReply?.let { (note, conversation) ->
        PeerNoteReplySheet(
            note = note,
            conversation = conversation,
            sending = state.isNoteReplySending,
            error = state.noteError,
            onDismiss = { peerNoteReply = null },
            onSend = { text ->
                onReplyToNote(note, conversation, text) { sent ->
                    if (sent) peerNoteReply = null
                }
            },
        )
    }
    selectedConversation?.let { conversation ->
        ConversationActionsSheet(
            conversation = conversation,
            onDismiss = { selectedConversation = null },
            onToggle = { flag ->
                onToggleConversationFlag(conversation.id, flag)
                selectedConversation = null
            },
        )
    }
    if (deletingConversations.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { deletingConversations = emptyList() },
            title = { Text("حذف گفتگوها") },
            text = { Text("آیا از حذف ${deletingConversations.size} گفتگوی انتخاب‌شده مطمئن هستید؟ این عمل قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(onClick = {
                    val targets = deletingConversations
                    deletingConversations = emptyList()
                    targets.forEach { conversation -> onDeleteConversation(conversation.id) {} }
                    selectedConversationIds = emptyList()
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingConversations = emptyList() }) { Text("انصراف") } },
        )
    }
    blockingConversation?.let { conversation ->
        val peerId = conversation.peerId.orEmpty()
        val status = state.blockStatusByUserId[peerId]
        val loadingStatus = peerId in state.blockStatusLoadingUserIds
        val applying = peerId in state.blockActionUserIds
        AlertDialog(
            onDismissRequest = { if (!applying) blockingConversation = null },
            title = {
                Text(if (status?.isBlocked == true) "رفع مسدودیت" else "مسدود کردن")
            },
            text = {
                Text(
                    when {
                        loadingStatus -> "در حال بررسی وضعیت ${conversation.title}…"
                        status?.isBlocked == true -> "مسدودیت ${conversation.title} برداشته شود؟"
                        else -> "پس از مسدود کردن ${conversation.title}، امکان تبادل پیام محدود می‌شود."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onToggleBlock(peerId) { success ->
                            if (success) blockingConversation = null
                        }
                    },
                    enabled = status != null && !loadingStatus && !applying,
                ) {
                    if (applying) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(if (status?.isBlocked == true) "رفع مسدودیت" else "مسدود کردن")
                }
            },
            dismissButton = {
                TextButton(onClick = { blockingConversation = null }, enabled = !applying) { Text("انصراف") }
            },
        )
    }
}

@Composable
private fun ProfileNotesTray(
    notes: List<ProfileNote>,
    conversations: List<Conversation>,
    isLoading: Boolean,
    onOpenOwnNote: () -> Unit,
    onOpenPeerNote: (ProfileNote, Conversation) -> Unit,
) {
    val own = remember(notes) { notes.firstOrNull { it.isMine && !it.isExpired() } }
    val peerNotes = remember(notes, conversations) {
        val privateConvsByPeer = HashMap<String, Conversation>(conversations.size)
        conversations.forEach { conv ->
            if (conv.type == ConversationType.PRIVATE) {
                conv.peerId?.trim()?.takeIf(String::isNotEmpty)?.let { pid ->
                    val lower = pid.lowercase()
                    privateConvsByPeer.putIfAbsent(lower, conv)
                    privateConvsByPeer.putIfAbsent(lower.replace("-", ""), conv)
                }
            }
        }
        notes.asSequence()
            .filter { !it.isMine && !it.isExpired() && it.userId.isNotBlank() }
            .distinctBy { it.userId }
            .mapNotNull { note ->
                val cleanNoteUserId = note.userId.trim().lowercase()
                val conv = privateConvsByPeer[cleanNoteUserId]
                    ?: privateConvsByPeer[cleanNoteUserId.replace("-", "")]
                conv?.let { note to it }
            }
            .toList()
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        item(key = "own-note") {
            ProfileNoteItem(
                text = own?.content,
                label = if (own == null) "یادداشت" else "یادداشت شما",
                avatarUrl = null,
                loading = isLoading,
                onClick = onOpenOwnNote,
            )
        }
        items(peerNotes, key = { "peer-note-${it.first.userId}" }) { (note, conversation) ->
            ProfileNoteItem(
                text = note.content,
                label = conversation.title,
                avatarUrl = conversation.avatarUrl,
                loading = false,
                onClick = { onOpenPeerNote(note, conversation) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeerNoteReplySheet(
    note: ProfileNote,
    conversation: Conversation,
    sending: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var reply by remember(note.id) { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                Modifier.fillMaxWidth().imePadding().padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fallback = painterResource(R.drawable.vista_default_avatar)
                AsyncImage(
                    model = conversation.avatarUrl,
                    contentDescription = null,
                    placeholder = fallback,
                    error = fallback,
                    fallback = fallback,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                )
                Spacer(Modifier.height(8.dp))
                Text(conversation.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    note.content,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it.take(16_000) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sending,
                    singleLine = false,
                    maxLines = 5,
                    label = { Text("پاسخ شما به این یادداشت...") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onSend(reply.trim()) },
                    enabled = reply.isNotBlank() && !sending,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("ارسال پاسخ")
                }
                TextButton(onClick = onDismiss, enabled = !sending) { Text("انصراف") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ProfileNoteItem(
    text: String?,
    label: String,
    avatarUrl: String?,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(72.dp).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(82.dp).fillMaxWidth()) {
            ChatAvatarImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(64.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = CircleShape,
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 65.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    loading -> CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                    text.isNullOrBlank() -> Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Text(
                        text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnNoteEditorSheet(
    current: ProfileNote?,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var content by remember(current?.id) { mutableStateOf(current?.content.orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("یادداشت جدید", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        content.ifBlank { "یه چیزی بنویس..." },
                        textAlign = TextAlign.Center,
                        color = if (content.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 60) content = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("یادداشت شما") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !saving,
                    supportingText = { Text("${content.length}/60") },
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onSave(content.trim()) },
                        enabled = !saving && content.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("اشتراک")
                    }
                    if (current != null) {
                        TextButton(onClick = onDelete, enabled = !saving) { Text("حذف") }
                    }
                    TextButton(onClick = onDismiss, enabled = !saving) { Text("لغو") }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleArchive: () -> Unit = {},
    onDelete: () -> Unit = {},
    requestActionLoading: Boolean = false,
    showRequestActions: Boolean = false,
    onAcceptRequest: () -> Unit = {},
    onRejectRequest: () -> Unit = {},
    onBlock: (() -> Unit)? = null,
) {
    val typing = conversation.typingUserIds.isNotEmpty()
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val swipeOffset = remember { Animatable(0f) }
    val maxStartSwipePx = with(density) { 144.dp.toPx() }
    val maxEndSwipePx = with(density) { (if (onBlock == null) 144.dp else 216.dp).toPx() }
    val revealThreshold = with(density) { 40.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(conversation.id) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        coroutineScope.launch {
                            // Read the final drag offset here. A value captured during
                            // composition can still describe the pre-gesture (closed) state.
                            if (kotlin.math.abs(swipeOffset.value) > revealThreshold) {
                                // Snap to reveal position
                                val target = if (swipeOffset.value > 0) maxStartSwipePx else -maxEndSwipePx
                                swipeOffset.animateTo(
                                    target,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            } else {
                                swipeOffset.animateTo(
                                    0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            swipeOffset.animateTo(
                                0f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        coroutineScope.launch {
                            val newOffset = (swipeOffset.value + dragAmount).coerceIn(-maxEndSwipePx, maxStartSwipePx)
                            swipeOffset.snapTo(newOffset)
                        }
                    },
                )
            },
    ) {
        // Left-reveal actions (swipe right = reveal left actions: pin, mute)
        Row(
            modifier = Modifier
                .align(androidx.compose.ui.AbsoluteAlignment.CenterLeft)
                .height(76.dp)
                .graphicsLayer {
                    val progress = if (maxStartSwipePx > 0f) (swipeOffset.value / maxStartSwipePx).coerceIn(0f, 1f) else 0f
                    alpha = if (swipeOffset.value > 0f) progress else 0f
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeActionButton(
                icon = if (conversation.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                label = if (conversation.isPinned) "برداشتن" else "سنجاق",
                color = Color(0xFF1565C0),
                onClick = {
                    coroutineScope.launch { swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onTogglePin()
                },
            )
            SwipeActionButton(
                icon = if (conversation.isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                label = if (conversation.isMuted) "صدادار" else "بی‌صدا",
                color = Color(0xFF6A1B9A),
                onClick = {
                    coroutineScope.launch { swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onToggleMute()
                },
            )
        }
        // Right-reveal actions (swipe left = reveal right actions: archive, delete)
        Row(
            modifier = Modifier
                .align(androidx.compose.ui.AbsoluteAlignment.CenterRight)
                .height(76.dp)
                .graphicsLayer {
                    val progress = if (maxEndSwipePx > 0f) (-swipeOffset.value / maxEndSwipePx).coerceIn(0f, 1f) else 0f
                    alpha = if (swipeOffset.value < 0f) progress else 0f
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeActionButton(
                icon = Icons.Default.Archive,
                label = if (conversation.isArchived) "بازگرداندن" else "بایگانی",
                color = Color(0xFF00796B),
                onClick = {
                    coroutineScope.launch { swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onToggleArchive()
                },
            )
            if (onBlock != null) {
                SwipeActionButton(
                    icon = Icons.Default.Block,
                    label = "مسدود",
                    color = Color(0xFFE65100),
                    onClick = {
                        coroutineScope.launch { swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onBlock()
                    },
                )
            }
            SwipeActionButton(
                icon = Icons.Default.Delete,
                label = "حذف",
                color = Color(0xFFB71C1C),
                onClick = {
                    coroutineScope.launch { swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onDelete()
                },
            )
        }
        // Main row content, translated by swipe offset
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset.value },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .combinedClickable(
                        role = Role.Button,
                        onClick = {
                            if (kotlin.math.abs(swipeOffset.value) > 4f) {
                                // Dismiss swipe on tap
                                coroutineScope.launch { swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            } else {
                                onClick()
                            }
                        },
                        onLongClick = onLongClick,
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .semantics {
                        contentDescription = buildString {
                            append("گفتگو با ${conversation.title.ifBlank { "کاربر" }}")
                            if (conversation.unreadCount > 0) append(", ${conversation.unreadCount} پیام خوانده‌نشده")
                            if (typing) append(", در حال نوشتن")
                            if (conversation.isMuted) append(", بی‌صدا")
                            if (conversation.isPinned) append(", سنجاق‌شده")
                            if (conversation.isArchived) append(", بایگانی‌شده")
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(54.dp)) {
                    ChatAvatarImage(
                        model = conversation.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(6.dp, CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                shape = CircleShape,
                            )
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    if (conversation.isPinned) {
                        Box(
                            Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(Color(0xFFFFA500)),
                            contentAlignment = Alignment.Center,
                        ) { PinGlyph() }
                    }
                    if (conversation.isArchived) {
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = "بایگانی‌شده",
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                }
                if (selected) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.size(32.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (conversation.isMuted) {
                            MutedGlyph(MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                        }
                        TelegramEmojiText(
                            text = conversation.title.ifBlank { "کاربر" },
                            modifier = Modifier.weight(1f),
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (conversation.type.name == "SECRET") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (conversation.type == ConversationType.GROUP) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                        if (conversation.type == ConversationType.SECRET) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF2E7D32),
                            )
                        }
                        conversation.lastMessageAtEpochMillis?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                formatConversationTime(it),
                                fontSize = 12.sp,
                                fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                                color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!typing && conversation.isLastMessageFromMe && conversation.lastMessage != null) {
                            InboxMessageStatusMark(conversation.lastMessageStatus)
                            Spacer(Modifier.width(4.dp))
                        }
                        TelegramEmojiText(
                            text = if (typing) {
                                "در حال نوشتن..."
                            } else {
                                conversation.inboxPreviewText() ?: "پیام جدیدی ارسال کنید"
                            },
                            modifier = Modifier.weight(1f),
                            style = TextStyle(
                                color = if (typing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = if (typing || conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                                fontStyle = if (typing) FontStyle.Italic else FontStyle.Normal,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (conversation.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            VistaNotificationBadge(
                                count = conversation.unreadCount,
                                containerColor = if (conversation.isMuted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    VistaBadgeDefaults.ChatBadgeColor
                                },
                            )
                        }
                    }
                }
            }
            if (showRequestActions) {
                MessageRequestActions(
                    loading = requestActionLoading,
                    onAccept = onAcceptRequest,
                    onReject = onRejectRequest,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun InboxMessageStatusMark(status: MessageStatus) {
    when (status) {
        MessageStatus.PENDING -> CircularProgressIndicator(
            modifier = Modifier.size(13.dp),
            strokeWidth = 1.25.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MessageStatus.FAILED -> Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "ارسال ناموفق",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp),
        )
        MessageStatus.SENT -> Icon(
            imageVector = Icons.Default.Done,
            contentDescription = "ارسال شد",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        MessageStatus.DELIVERED -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "تحویل شد",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
        MessageStatus.READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "خوانده شد",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun ConversationSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MessageRequestActions(
    loading: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onReject,
            enabled = !loading,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("رد")
        }
        Button(
            onClick = onAccept,
            enabled = !loading,
            modifier = Modifier.weight(1f),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("قبول")
            }
        }
    }
}

@Composable
private fun SwipeActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .height(76.dp)
            .background(color)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color.White, fontSize = 10.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationActionsSheet(
    conversation: Conversation,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                Text(
                    conversation.title.ifBlank { "گفتگو" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                ConversationActionRow(
                    text = if (conversation.isPinned) "برداشتن سنجاق" else "سنجاق کردن",
                    icon = Icons.Default.PushPin,
                    onClick = { onToggle("pin") },
                )
                ConversationActionRow(
                    text = if (conversation.isMuted) "فعال کردن اعلان‌ها" else "بی‌صدا کردن",
                    icon = if (conversation.isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    onClick = { onToggle("mute") },
                )
                ConversationActionRow(
                    text = if (conversation.isArchived) "خارج کردن از بایگانی" else "بایگانی گفتگو",
                    icon = Icons.Default.Archive,
                    onClick = { onToggle("archive") },
                )
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun ConversationActionRow(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Text(text, fontSize = 15.sp)
    }
}

@Composable
private fun ConnectionStatusIndicator(state: RealtimeConnectionState) {
    val color = when (state) {
        RealtimeConnectionState.CONNECTED -> Color(0xFF2E7D32)
        RealtimeConnectionState.CONNECTING,
        RealtimeConnectionState.AUTHENTICATING,
        RealtimeConnectionState.RECONNECTING -> Color(0xFFFF9800)
        else -> Color(0xFFD32F2F)
    }
    val connecting = state == RealtimeConnectionState.CONNECTING ||
        state == RealtimeConnectionState.AUTHENTICATING ||
        state == RealtimeConnectionState.RECONNECTING
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .height(if (connecting) 20.dp else 10.dp)
            .widthIn(min = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = if (connecting) 8.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (connecting) Text("در حال اتصال...", color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun LoadingConversationList() {
    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
        items(10, key = { "loading-$it" }) {
            Row(
                Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.width(120.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(200.dp).height(14.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                }
                Box(Modifier.width(40.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
    }
}

@Composable
private fun EmptyConversationState(searchActive: Boolean, archived: Boolean = false) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            if (archived && !searchActive) {
                Icon(
                    Icons.Default.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp),
                )
            } else {
                EmptyChatGlyph(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            when {
                searchActive -> "نتیجه‌ای یافت نشد"
                archived -> "هیچ گفتگوی بایگانی‌شده‌ای وجود ندارد"
                else -> "هیچ گفتگویی وجود ندارد"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                searchActive -> "عبارت دیگری را امتحان کنید"
                archived -> "گفتگوهایی که بایگانی می‌کنید اینجا نمایش داده می‌شوند"
                else -> "با دکمه مداد پیام جدید شروع کنید"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun PinGlyph() {
    Canvas(Modifier.size(10.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.14f)
        drawLine(Color.White, start = center.copy(y = size.height * 0.12f), end = center.copy(y = size.height * 0.86f), strokeWidth = stroke.width)
        drawLine(Color.White, start = center.copy(x = size.width * 0.2f, y = size.height * 0.32f), end = center.copy(x = size.width * 0.8f, y = size.height * 0.32f), strokeWidth = stroke.width)
        drawCircle(Color.White, radius = size.minDimension * 0.22f, center = center.copy(y = size.height * 0.34f), style = stroke)
    }
}

@Composable
private fun MutedGlyph(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.42f)
            lineTo(size.width * 0.34f, size.height * 0.42f)
            lineTo(size.width * 0.58f, size.height * 0.2f)
            lineTo(size.width * 0.58f, size.height * 0.8f)
            lineTo(size.width * 0.34f, size.height * 0.58f)
            lineTo(size.width * 0.12f, size.height * 0.58f)
            close()
        }
        drawPath(path, color)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width * 0.14f, size.height * 0.14f), end = androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height * 0.86f), strokeWidth = size.minDimension * 0.12f)
    }
}

@Composable
private fun EmptyChatGlyph(color: Color) {
    Canvas(Modifier.size(48.dp)) {
        val strokeWidth = size.minDimension * 0.07f
        val outline = Stroke(width = strokeWidth)
        val left = size.width * 0.12f
        val top = size.height * 0.18f
        val right = size.width * 0.88f
        val bottom = size.height * 0.72f
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.12f),
            style = outline,
        )
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width * 0.34f, bottom), end = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.86f), strokeWidth = strokeWidth)
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.86f), end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, bottom), strokeWidth = strokeWidth)
    }
}

@Composable
fun MessageDetailRoute(
    conversationId: String,
    initialMessageId: String? = null,
    initialSharedContentId: String? = null,
    initialSharedText: String? = null,
    initialSharedUris: List<android.net.Uri> = emptyList(),
    onInitialSharedContentConsumed: (String) -> Unit = {},
    title: String,
    viewModel: MessagesViewModel,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onStartSecretChat: (Conversation) -> Unit = {},
    onOpenPost: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(conversationId) { viewModel.bind(conversationId) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, conversationId, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setConversationVisible(conversationId, true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setConversationVisible(conversationId, false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.setConversationVisible(conversationId, true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setConversationVisible(conversationId, false)
        }
    }
    MessageDetailScreen(
        state = state,
        initialMessageId = initialMessageId,
        initialSharedContentId = initialSharedContentId,
        initialSharedText = initialSharedText,
        initialSharedUris = initialSharedUris,
        onInitialSharedContentConsumed = onInitialSharedContentConsumed,
        title = state.conversation?.title?.takeIf(String::isNotBlank) ?: title,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onLoadOlder = viewModel::loadOlder,
        onSend = viewModel::send,
        onSendReply = viewModel::sendReply,
        onSendGif = viewModel::sendGif,
        onLoadGifs = viewModel::loadTrendingGifs,
        onSearchGifs = viewModel::searchGifs,
        onRetry = viewModel::retry,
        onCancelTransfer = viewModel::cancelTransfer,
        onEdit = viewModel::edit,
        onDelete = viewModel::delete,
        onReact = viewModel::react,
        onTogglePinned = viewModel::togglePinned,
        onForward = viewModel::forward,
        onRefreshGroup = viewModel::loadGroupDetails,
        onUpdateGroupName = viewModel::updateGroupName,
        onAddGroupMembers = viewModel::addGroupMembers,
        onSearchGroupUsers = viewModel::searchGroupUsers,
        onRemoveGroupMember = viewModel::removeGroupMember,
        onSetGroupAdmin = viewModel::setGroupAdmin,
        onSetGroupInviteEnabled = viewModel::setGroupInviteEnabled,
        onRegenerateGroupInvite = viewModel::regenerateGroupInvite,
        onLeaveGroup = { complete -> viewModel.leaveGroup { left -> if (left) onBack(); complete(left) } },
        onDeleteGroup = { complete -> viewModel.deleteGroup { deleted -> if (deleted) onBack(); complete(deleted) } },
        onComposerChanged = viewModel::composerChanged,
        onSendVoice = viewModel::sendAttachment,
        onAttachDraft = viewModel::sendAttachment,
        onSearch = viewModel::search,
        onToggleMute = viewModel::toggleMute,
        onToggleBlock = viewModel::toggleBlock,
        onReportUser = viewModel::reportUser,
        onDeleteConversation = { convId, complete -> viewModel.deleteConversation(convId) { success -> if (success) onBack(); complete(success) } },
        onStartDownload = viewModel::startDownload,
        onPauseDownload = viewModel::pauseDownload,
        onResumeDownload = viewModel::resumeDownload,
        onCancelDownload = viewModel::cancelDownload,
        onOpenProfile = onOpenProfile,
        onLoadPartnerProfile = viewModel::loadPartnerProfile,
        onStartSecretChat = { viewModel.startSecretChat(onStartSecretChat) },
        onSetSecretAutoDelete = viewModel::setSecretAutoDelete,
        onComputeSafetyNumber = viewModel::computeSafetyNumber,
        onClearError = viewModel::clearError,
        onOpenPost = onOpenPost,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MessageDetailScreen(
    state: MessagesUiState,
    initialMessageId: String? = null,
    initialSharedContentId: String? = null,
    initialSharedText: String? = null,
    initialSharedUris: List<android.net.Uri> = emptyList(),
    onInitialSharedContentConsumed: (String) -> Unit = {},
    title: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadOlder: () -> Unit,
    onSend: (String, () -> Unit) -> Unit,
    onSendReply: (String, Message, () -> Unit) -> Unit = { _, _, _ -> },
    onSendGif: (String) -> Unit = {},
    onLoadGifs: (Boolean) -> Unit = {},
    onSearchGifs: (String) -> Unit = {},
    onRetry: (Message) -> Unit,
    onCancelTransfer: (Message) -> Unit = {},
    onEdit: (Message, String) -> Unit = { _, _ -> },
    onDelete: (Message, Boolean) -> Unit = { _, _ -> },
    onReact: (Message, String) -> Unit = { _, _ -> },
    onTogglePinned: (Message) -> Unit = {},
    onForward: (Message, String) -> Unit = { _, _ -> },
    onRefreshGroup: () -> Unit = {},
    onUpdateGroupName: (String) -> Unit = {},
    onAddGroupMembers: (List<String>) -> Unit = {},
    onSearchGroupUsers: (String) -> Unit = {},
    onRemoveGroupMember: (String) -> Unit = {},
    onSetGroupAdmin: (String, Boolean) -> Unit = { _, _ -> },
    onSetGroupInviteEnabled: (Boolean) -> Unit = {},
    onRegenerateGroupInvite: () -> Unit = {},
    onLeaveGroup: ((Boolean) -> Unit) -> Unit = { complete -> complete(false) },
    onDeleteGroup: ((Boolean) -> Unit) -> Unit = { complete -> complete(false) },
    onComposerChanged: (String) -> Unit = {},
    onSendVoice: (ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft) -> Unit = {},
    onAttachDraft: (ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft) -> Unit = {},
    onVoice: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleBlock: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
    onReportUser: (String, ModerationReason, String?, (Boolean) -> Unit) -> Unit = { _, _, _, complete -> complete(false) },
    onDeleteConversation: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
    onStartDownload: (Message) -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onLoadPartnerProfile: (String) -> Unit = {},
    onStartSecretChat: () -> Unit = {},
    onSetSecretAutoDelete: (Int) -> Unit = {},
    onComputeSafetyNumber: ((String?) -> Unit) -> Unit = {},
    onClearError: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    val context = LocalContext.current
    val localView = LocalView.current
    val isSecretConversation = state.conversation?.type == ConversationType.SECRET
    val isDarkTheme = isSystemInDarkTheme()
    val isDebuggable = remember(context) {
        runCatching {
            (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }.getOrDefault(true)
    }
    DisposableEffect(localView, isSecretConversation, isDarkTheme) {
        val activity = localView.context.findActivity()
        val window = activity?.takeUnless { it.isFinishing || it.isDestroyed }?.window
        if (window != null) {
            runCatching {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, localView)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
            }
        }
        if (isSecretConversation) {
            runCatching { window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
        }
        onDispose {
            if (isSecretConversation) {
                runCatching { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
            }
        }
    }
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordAudioPermission = granted
    }
    var attachmentSheetVisible by remember { mutableStateOf(false) }
    // Composer geometry and editable state are reconstructed from the active
    // conversation. Restoring a raw TextFieldValue after process recreation
    // can resurrect a stale selection/draft and corrupt input-surface swaps.
    var autoDeleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var safetyNumberDialogVisible by rememberSaveable { mutableStateOf(false) }
    var emojiPanelVisible by remember { mutableStateOf(false) }
    val composerController = remember { ChatComposerController() }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(state.messages) {
        val voiceIds = state.messages
            .filter { it.attachment?.kind == AttachmentKind.VOICE || it.attachment?.kind == AttachmentKind.AUDIO }
            .map { it.stableKey }
        ir.coffevista.vista_native.features.chat.presentation.components.VoicePlaybackCoordinator.updateVoiceQueue(voiceIds)
    }

    val safeBack = {
        runCatching {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            composerController.closeKeyboard()
            val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            val activity = context.findActivity()
            activity?.currentFocus?.let { view ->
                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
                composerController.closeKeyboard()
                val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                val activity = context.findActivity()
                activity?.currentFocus?.let { view ->
                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                    view.clearFocus()
                }
            }
        }
    }
    var messageInteraction by remember(state.conversationId) {
        mutableStateOf<MessageInteractionState>(MessageInteractionState.Idle)
    }
    val selectedMessageKeys = messageInteraction.selectedMessageKeys()
    fun dismissMessageInteraction() {
        messageInteraction = MessageInteractionState.Idle
    }
    fun startSelection(messageKeys: List<String>) {
        // One render row owns one selection gesture. For an album that means
        // every backing message enters selection together, matching Flutter.
        val distinctKeys = messageKeys.distinct()
        if (distinctKeys.isEmpty()) return
        emojiPanelVisible = false
        messageInteraction = MessageInteractionState.Selecting(distinctKeys)
    }
    fun startSelection(messageKey: String) = startSelection(listOf(messageKey))
    fun openMessageContext(messageKey: String, bubbleBounds: Rect?) {
        // Keep the active composer surface in place. The bubble bounds are
        // captured in window coordinates; dismissing the IME/emoji panel here
        // relays out the LazyColumn before the overlay is drawn and leaves the
        // captured anchor detached from the real bubble. The full-screen
        // overlay consumes input and its placement already respects IME/system
        // insets, so preserving the viewport is both safer and visually stable.
        messageInteraction = MessageInteractionState.Context(messageKey, bubbleBounds)
    }
    fun toggleMessageSelection(messageKeys: List<String>) {
        val selecting = messageInteraction as? MessageInteractionState.Selecting ?: return
        val distinctKeys = messageKeys.distinct()
        val allSelected = distinctKeys.all(selecting.messageKeys::contains)
        val updated = if (allSelected) {
            selecting.messageKeys - distinctKeys.toSet()
        } else {
            (selecting.messageKeys + distinctKeys).distinct()
        }
        messageInteraction = updated.takeIf(List<String>::isNotEmpty)
            ?.let(MessageInteractionState::Selecting)
            ?: MessageInteractionState.Idle
    }
    fun toggleMessageSelection(messageKey: String) = toggleMessageSelection(listOf(messageKey))
    LaunchedEffect(state.messages, messageInteraction) {
        val selectableKeys = state.messages
            .asSequence()
            .filter { it.content != MessageContent.Deleted }
            .map { it.stableKey }
            .toSet()
        messageInteraction = messageInteraction.retainSelection(selectableKeys)
    }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val density = LocalDensity.current
    val composerHostView = LocalView.current

    var composerPanelHeightDp by remember(configuration.screenHeightDp, isLandscape) {
        mutableFloatStateOf(KeyboardHeightStore.getKeyboardHeightDp(context, isLandscape, screenHeightDp))
    }

    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    val navBottomDp = with(density) { navBottomPx.toDp() }
    val isImeVisible = WindowInsets.isImeVisible

    // Track peak resting keyboard height while IME is open.
    // Avoids capturing intermediate opening or closing animation frames.
    var peakImeHeightDp by remember { mutableFloatStateOf(0f) }
    val isImeOpen = isImeVisible || imeBottomDp.value >= 160f

    LaunchedEffect(isImeOpen, imeBottomDp.value) {
        val currentDp = imeBottomDp.value
        if (isImeOpen) {
            if (currentDp > peakImeHeightDp) {
                peakImeHeightDp = currentDp
            }
            if (currentDp >= 160f && currentDp >= (peakImeHeightDp - 1f)) {
                kotlinx.coroutines.delay(80)
                if (isImeOpen && imeBottomDp.value >= (peakImeHeightDp - 2f)) {
                    val finalVal = maxOf(imeBottomDp.value, peakImeHeightDp)
                    composerPanelHeightDp = finalVal
                    KeyboardHeightStore.saveKeyboardHeightDp(context, isLandscape, finalVal, screenHeightDp)
                    if (emojiPanelVisible) {
                        emojiPanelVisible = false
                    }
                }
            }
        } else {
            peakImeHeightDp = 0f
        }
    }

    val isPanelActive = emojiPanelVisible
    BackHandler(enabled = !emojiPanelVisible && selectedMessageKeys.isEmpty()) {
        safeBack()
    }
    BackHandler(enabled = emojiPanelVisible) {
        emojiPanelVisible = false
        composerController.closeKeyboard()
    }
    // Back handlers are dispatched in reverse composition order. Register
    // selection after the composer panel so Back always leaves selection
    // before it hides any lower-priority composer surface.
    BackHandler(enabled = selectedMessageKeys.isNotEmpty()) {
        dismissMessageInteraction()
    }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var deletingMessage by remember { mutableStateOf<Message?>(null) }
    var deletingMessages by remember { mutableStateOf(emptyList<Message>()) }
    var forwardingMessage by remember { mutableStateOf<Message?>(null) }
    var forwardingMessages by remember { mutableStateOf(emptyList<Message>()) }
    var infoMessage by remember { mutableStateOf<Message?>(null) }
    var groupSheetVisible by rememberSaveable { mutableStateOf(false) }
    var partnerProfileVisible by rememberSaveable { mutableStateOf(false) }
    var blockingPeerId by remember { mutableStateOf<String?>(null) }
    var terminalGroupAction by remember { mutableStateOf<String?>(null) }
    var viewedAttachment by remember { mutableStateOf<ir.coffevista.vista_native.features.chat.domain.model.Attachment?>(null) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchResultIndex by rememberSaveable { mutableStateOf(0) }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val messageRenderItems = remember(state.messages) { buildMessageRenderItems(state.messages) }
    var initialMessageHandled by rememberSaveable(state.conversationId, initialMessageId) {
        mutableStateOf(false)
    }
    LaunchedEffect(initialMessageId, messageRenderItems) {
        if (initialMessageHandled || initialMessageId.isNullOrBlank()) return@LaunchedEffect
        val targetIndex = renderItemIndexForMessage(messageRenderItems, initialMessageId)
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            initialMessageHandled = true
        }
    }
    val presentedMessageKeys = remember(state.conversationId) { mutableSetOf<String>() }
    val visibleMessageKeys = remember(state.messages) { state.messages.map(Message::stableKey) }
    // Message-entry eligibility must remain stable while the composer changes.
    // Re-reading wall time from every visible cell makes a typing recompose do
    // unnecessary work and can make a just-arrived message change animation
    // eligibility halfway through the same list snapshot.
    val messageEntrySnapshotTime = remember(state.messages) { System.currentTimeMillis() }
    LaunchedEffect(visibleMessageKeys) {
        presentedMessageKeys.addAll(visibleMessageKeys)
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingAttachmentDrafts by remember(state.conversationId) {
        mutableStateOf(emptyList<ChatAttachmentDraft>())
    }
    var pendingAttachmentsAsAlbum by remember(state.conversationId) { mutableStateOf(false) }
    fun enqueueSelectedAttachments(uris: List<android.net.Uri>, groupAsAlbum: Boolean) {
        val drafts = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            context.contentResolver.toChatAttachmentDraft(uri)
        }
        if (drafts.isNotEmpty()) {
            pendingAttachmentDrafts = drafts
            pendingAttachmentsAsAlbum = groupAsAlbum
        }
        if (drafts.size != uris.size) {
            coroutineScope.launch { snackbarHostState.showSnackbar("اطلاعات بعضی فایل‌های انتخاب‌شده قابل خواندن نیست") }
        }
    }
    var newestMessageKey by remember(state.conversationId) { mutableStateOf<String?>(null) }
    fun openPartnerDetails() {
        state.conversation?.peerId?.takeIf(String::isNotBlank)?.let(onLoadPartnerProfile)
        partnerProfileVisible = true
    }
    LaunchedEffect(state.searchQuery, state.searchResults.size) {
        searchResultIndex = 0
    }
    LaunchedEffect(state.error) {
        val errorText = state.error ?: return@LaunchedEffect
        if (!errorText.contains("اتصال برقرار نشد") &&
            !errorText.contains("ارتباط با سرویس گفتگو") &&
            !errorText.contains("خطا در برقراری ارتباط") &&
            !errorText.contains("سرور در دسترس نیست")
        ) {
            snackbarHostState.showSnackbar(errorText)
        }
        onClearError()
    }
    LaunchedEffect(state.conversationId, state.messages.firstOrNull()?.stableKey) {
        val newest = state.messages.firstOrNull()
        val previous = newestMessageKey
        newestMessageKey = newest?.stableKey
        if (
            newest != null && previous != null && previous != newest.stableKey &&
            (newest.isMine || listState.firstVisibleItemIndex <= 1)
        ) {
            listState.animateScrollToItem(0)
        }
    }
    LaunchedEffect(initialSharedContentId, state.conversationId) {
        val requestId = initialSharedContentId ?: return@LaunchedEffect
        if (state.conversationId.isNullOrBlank()) return@LaunchedEffect
        val sharedText = initialSharedText
        if (!sharedText.isNullOrBlank()) onComposerChanged(sharedText)
        if (initialSharedUris.isNotEmpty()) {
            enqueueSelectedAttachments(
                uris = initialSharedUris,
                groupAsAlbum = initialSharedUris.size > 1,
            )
        }
        onInitialSharedContentConsumed(requestId)
    }

    var headerHeightDp by remember { mutableStateOf(0.dp) }
    val statusBarTopDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val effectiveTopPadding = if (headerHeightDp > 0.dp) headerHeightDp else (statusBarTopDp + 56.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF101419) else Color(0xFFDFE5E9)),
    ) {
        Image(
            painter = painterResource(
                if (isDarkTheme) ChatR.drawable.vista_custom_bg_dark
                else ChatR.drawable.vista_custom_bg,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color = if (isDarkTheme) Color.Black.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.2f),
                blendMode = if (isDarkTheme) BlendMode.Darken else BlendMode.Lighten,
            ),
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(emojiPanelVisible) {
                        if (emojiPanelVisible) {
                            detectTapGestures {
                                emojiPanelVisible = false
                            }
                        }
                    },
            ) {
                val isDragged by listState.interactionSource.collectIsDraggedAsState()
                LaunchedEffect(isDragged) {
                    if (isDragged) {
                        if (emojiPanelVisible) {
                            emojiPanelVisible = false
                        }
                        composerController.closeKeyboard()
                    }
                }
                val displayedMessages = state.messages
                val displayedRenderItems = remember(displayedMessages) {
                    buildMessageRenderItems(displayedMessages).distinctBy { it.stableKey }
                }
                LaunchedEffect(searchVisible, searchResultIndex, state.searchResults) {
                    if (searchVisible && state.searchResults.isNotEmpty()) {
                        val targetMessage = state.searchResults.getOrNull(searchResultIndex)
                        if (targetMessage != null) {
                            val targetIndex = displayedRenderItems.indexOfFirst { item ->
                                item.messages.any { it.stableKey == targetMessage.stableKey || it.clientId == targetMessage.clientId }
                            }
                            if (targetIndex >= 0) {
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                    }
                }
                val unreadWindow = remember(state.messages) { unreadMessageWindow(state.messages) }
                val groupMemberById = remember(state.groupMembers) {
                    state.groupMembers.associateBy(GroupMember::userId)
                }
                val floatingDateEpochMillis by remember(displayedRenderItems, listState) {
                    derivedStateOf {
                        val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
                        visibleMessageDateEpochMillis(
                            renderItems = displayedRenderItems,
                            visibleIndices = visibleIndices.ifEmpty { listOf(listState.firstVisibleItemIndex) },
                        )
                    }
                }
                var floatingDateVisible by remember(state.conversationId) { mutableStateOf(false) }
                LaunchedEffect(
                    listState.isScrollInProgress,
                    floatingDateEpochMillis,
                    searchVisible,
                ) {
                    if (searchVisible || floatingDateEpochMillis == null) {
                        floatingDateVisible = false
                    } else {
                        floatingDateVisible = true
                        if (!listState.isScrollInProgress) {
                            kotlinx.coroutines.delay(2_000)
                            floatingDateVisible = false
                        }
                    }
                }
                when {
                    state.isInitialLoading && state.messages.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null && state.messages.isEmpty() -> ErrorState(state.error, onRefresh)
                    state.messages.isEmpty() -> {
                        if (isSecretConversation) {
                            SecretChatIntroCard(
                                onSetTimer = { autoDeleteDialogVisible = true },
                                onViewKey = {
                                    onComputeSafetyNumber { }
                                    safetyNumberDialogVisible = true
                                },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        } else {
                            Text("هنوز پیامی نیست", Modifier.align(Alignment.Center))
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("chat-message-list"),
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(
                            start = 6.dp,
                            end = 6.dp,
                            top = effectiveTopPadding + 6.dp,
                            bottom = 6.dp,
                        ),
                    ) {
                        itemsIndexed(
                            displayedRenderItems,
                            key = { _, item -> item.stableKey },
                            contentType = { _, item -> if (item.isAlbum) "media-album" else item.primary.attachment?.kind?.name ?: "text" },
                        ) { _, renderItem ->
                            val message = renderItem.primary
                            val index = renderItem.primaryIndex
                            val messageKeys = renderItem.messages.map(Message::stableKey)
                            val groupPosition = messageGroupPosition(
                                messages = displayedMessages,
                                index = index,
                                spanLength = renderItem.messages.size,
                            )
                            val animateEntry = shouldAnimateMessageEntry(
                                message = message,
                                presentedMessageKeys = presentedMessageKeys,
                                nowEpochMillis = messageEntrySnapshotTime,
                            )
                            val older = displayedMessages.getOrNull(index + renderItem.messages.size)
                            val isUnreadBoundary = !searchVisible && (
                                if (state.firstUnreadMessageId != null) {
                                    renderItem.messages.any {
                                        it.stableKey == state.firstUnreadMessageId ||
                                            it.clientId == state.firstUnreadMessageId ||
                                            it.serverId == state.firstUnreadMessageId
                                    }
                                } else {
                                    val oldestUnreadIndex = if (unreadWindow.count > 0) unreadWindow.count - 1 else null
                                    oldestUnreadIndex != null && oldestUnreadIndex in index until (index + renderItem.messages.size)
                                }
                            )
                            val isDateBoundary = !searchVisible && (
                                older == null ||
                                !isSameLocalDay(renderItem.oldest.createdAtEpochMillis, older.createdAtEpochMillis)
                            )
                            Column(modifier = Modifier.fillMaxWidth().animateItem()) {
                                if (isDateBoundary) {
                                    DateDivider(renderItem.oldest.createdAtEpochMillis)
                                }
                                if (isUnreadBoundary) {
                                    UnreadMessagesDivider(text = "پیام‌های جدید")
                                }
                                val groupSender = groupMemberById[message.senderId]
                                GroupSenderFrame(
                                    enabled = state.conversation?.type == ConversationType.GROUP && !message.isMine,
                                    senderId = message.senderId,
                                    senderName = groupSender?.displayName ?: "کاربر",
                                    avatarUrl = groupSender?.avatarUrl,
                                    isFirstInGroup = groupPosition.isFirstInGroup,
                                    isLastInGroup = groupPosition.isLastInGroup,
                                    onOpenProfile = onOpenProfile,
                                ) {
                                    if (renderItem.isAlbum) {
                                        SwipeToReplyLayout(
                                            enabled = messageInteraction is MessageInteractionState.Idle &&
                                                message.content != MessageContent.Deleted,
                                            bubbleOnRight = message.isMine,
                                            onReply = {
                                                replyingTo = message
                                                editingMessage = null
                                            },
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
                                            ) {
                                                MediaAlbumBubble(
                                                    messages = renderItem.messages,
                                                    downloads = state.downloads,
                                                    selectionMode = selectedMessageKeys.isNotEmpty(),
                                                    selected = messageKeys.all(selectedMessageKeys::contains),
                                                    onToggleSelection = { toggleMessageSelection(messageKeys) },
                                                    onLongPress = {
                                                        if (messageInteraction is MessageInteractionState.Idle) {
                                                            openMessageContext(message.stableKey, null)
                                                        }
                                                    },
                                                    onRetry = onRetry,
                                                    onCancelTransfer = onCancelTransfer,
                                                    onStartDownload = onStartDownload,
                                                    onPauseDownload = onPauseDownload,
                                                    onResumeDownload = onResumeDownload,
                                                    onCancelDownload = onCancelDownload,
                                                    onAttachmentClick = { viewedAttachment = it },
                                                )
                                                if (message.reactions.isNotEmpty()) {
                                                    LazyRow(
                                                        modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    ) {
                                                        items(message.reactions.entries.toList(), key = { it.key }) { reaction ->
                                                            val isMyReaction = message.accountId in reaction.value
                                                            Row(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(
                                                                        if (isMyReaction) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                                        else if (message.isMine) Color.White.copy(alpha = 0.16f)
                                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                                    )
                                                                    .clickable { onReact(message, reaction.key) }
                                                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                            ) {
                                                                TelegramEmoji(emoji = reaction.key, size = 13.dp)
                                                                if (reaction.value.size > 1) {
                                                                    Text(
                                                                        reaction.value.size.toString().toPersianDigits(),
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Medium,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        SwipeToReplyLayout(
                                            enabled = messageInteraction is MessageInteractionState.Idle &&
                                                message.content != MessageContent.Deleted,
                                            bubbleOnRight = message.isMine,
                                            onReply = {
                                                replyingTo = message
                                                editingMessage = null
                                            },
                                        ) {
                                            MessageBubble(
                                                message = message,
                                                searchQuery = state.searchQuery.takeIf {
                                                    searchVisible && it.trim().length >= 2
                                                },
                                                onRetry = onRetry,
                                                onCancelTransfer = onCancelTransfer,
                                                downloadTask = state.downloads[message.serverId ?: message.clientId],
                                                onStartDownload = onStartDownload,
                                                onPauseDownload = onPauseDownload,
                                                onResumeDownload = onResumeDownload,
                                                onCancelDownload = onCancelDownload,
                                                selectionMode = selectedMessageKeys.isNotEmpty(),
                                                selected = message.stableKey in selectedMessageKeys,
                                                onToggleSelection = {
                                                    toggleMessageSelection(message.stableKey)
                                                },
                                                onLongPress = {
                                                    if (messageInteraction is MessageInteractionState.Idle &&
                                                        message.content != MessageContent.Deleted
                                                    ) {
                                                        startSelection(message.stableKey)
                                                    }
                                                },
                                                onOpenContextMenu = { bubbleBounds ->
                                                    if (messageInteraction is MessageInteractionState.Idle &&
                                                        message.content != MessageContent.Deleted
                                                    ) {
                                                        openMessageContext(message.stableKey, bubbleBounds)
                                                    }
                                                },
                                                onReply = {
                                                    replyingTo = message
                                                    editingMessage = null
                                                },
                                                onReact = { emoji -> onReact(message, emoji) },
                                                animateEntry = animateEntry,
                                                isFirstInGroup = groupPosition.isFirstInGroup,
                                                isLastInGroup = groupPosition.isLastInGroup,
                                                isSecretConversation = isSecretConversation,
                                                secretAutoDeleteSeconds = state.secretAutoDeleteSeconds,
                                                onAttachmentClick = { viewedAttachment = it },
                                                onJumpToRepliedMessage = {
                                                    val target = state.messages.firstOrNull { candidate ->
                                                        (candidate.serverId != null && candidate.serverId == message.replyToMessageId) ||
                                                            (candidate.clientId.isNotBlank() && candidate.clientId == message.replyToMessageId) ||
                                                            (candidate.content is MessageContent.Text &&
                                                                candidate.content.value == message.replyToContent)
                                                    }
                                                    val targetIndex = target?.let {
                                                        renderItemIndexForMessage(messageRenderItems, it.stableKey)
                                                    } ?: -1
                                                    if (targetIndex >= 0) {
                                                        if (searchVisible) {
                                                            searchVisible = false
                                                            searchResultIndex = 0
                                                            onSearch("")
                                                        }
                                                        coroutineScope.launch { listState.animateScrollToItem(targetIndex) }
                                                    }
                                                },
                                                onOpenPost = onOpenPost,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (!searchVisible) item(key = "older") {
                            if (state.isAppending) CircularProgressIndicator(Modifier.padding(VistaSpacing.Large).size(24.dp))
                            else if (state.hasMore) LaunchedEffect(state.messages.size) { onLoadOlder() }
                        }
                    }
                }

                FloatingMessageDateOverlay(
                    visible = floatingDateVisible && !searchVisible,
                    epochMillis = floatingDateEpochMillis,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = effectiveTopPadding + 8.dp),
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    val showScrollToBottom by remember {
                        derivedStateOf { listState.firstVisibleItemIndex > 2 }
                    }
                    val unreadBelow by remember(displayedRenderItems, unreadWindow.count) {
                        derivedStateOf {
                            val firstIdx = listState.firstVisibleItemIndex
                            if (firstIdx <= 0) 0
                            else {
                                val hiddenMessageCount = displayedRenderItems
                                    .take(firstIdx)
                                    .sumOf { it.messages.size }
                                minOf(hiddenMessageCount, unreadWindow.count)
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollToBottom && !searchVisible,
                        enter = androidx.compose.animation.scaleIn(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                            initialScale = 0.5f,
                        ) + androidx.compose.animation.fadeIn(tween(160)),
                        exit = androidx.compose.animation.scaleOut(
                            animationSpec = tween(140),
                            targetScale = 0.5f,
                        ) + androidx.compose.animation.fadeOut(tween(140)),
                        modifier = Modifier.padding(end = 12.dp, bottom = 12.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .shadow(6.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(role = Role.Button, onClick = {
                                        coroutineScope.launch { listState.animateScrollToItem(0) }
                                    })
                                    .semantics { contentDescription = "رفتن به آخرین پیام" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            if (unreadBelow > 0) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color(0xFF1E88E5))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        if (unreadBelow > 99) "99+" else unreadBelow.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
            ChatComposerBar(
                conversationId = state.conversationId,
                title = title,
                replyingTo = replyingTo,
                editingMessage = editingMessage,
                onClearReply = { replyingTo = null },
                onClearEdit = { editingMessage = null },
                onSend = { text, onDone ->
                    onSend(text, onDone)
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                onSendReply = { text, reply, onDone ->
                    onSendReply(text, reply, onDone)
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                onEdit = onEdit,
                onComposerChanged = onComposerChanged,
                composerController = composerController,
                focusRequester = focusRequester,
                onAttach = { attachmentSheetVisible = true },
                onEmoji = {
                    if (emojiPanelVisible) {
                        composerController.openKeyboard()
                    } else {
                        if (imeBottomDp.value >= 160f) {
                            composerPanelHeightDp = imeBottomDp.value
                            KeyboardHeightStore.saveKeyboardHeightDp(context, isLandscape, imeBottomDp.value, screenHeightDp)
                        }
                        emojiPanelVisible = true
                        composerController.closeKeyboard()
                    }
                },
                isEmojiPanelOpen = emojiPanelVisible,
                onFocusText = {
                    if (emojiPanelVisible) {
                        composerController.openKeyboard()
                    }
                },
                onSendVoice = onSendVoice,
                hasRecordPermission = hasRecordAudioPermission,
                onPermissionRequired = { recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                attachmentsEnabled = true,
            )
            if (attachmentSheetVisible) {
                ChatAttachmentBottomSheet(
                    onDismiss = { attachmentSheetVisible = false },
                    onMediaSelected = { uris -> enqueueSelectedAttachments(uris, groupAsAlbum = true) },
                    onFilesSelected = { uris -> enqueueSelectedAttachments(uris, groupAsAlbum = false) }
                )
            }
            if (pendingAttachmentDrafts.isNotEmpty()) {
                ChatAttachmentReviewSheet(
                    drafts = pendingAttachmentDrafts,
                    onDismiss = {
                        pendingAttachmentDrafts = emptyList()
                        pendingAttachmentsAsAlbum = false
                    },
                    onRemove = { index ->
                        pendingAttachmentDrafts = pendingAttachmentDrafts.filterIndexed { itemIndex, _ ->
                            itemIndex != index
                        }
                        if (pendingAttachmentDrafts.isEmpty()) pendingAttachmentsAsAlbum = false
                    },
                    onMove = { fromIndex, toIndex ->
                        pendingAttachmentDrafts = reorderAttachmentDrafts(
                            drafts = pendingAttachmentDrafts,
                            fromIndex = fromIndex,
                            toIndex = toIndex,
                        )
                    },
                    onSend = { caption ->
                        val drafts = pendingAttachmentDrafts
                        val groupId = if (pendingAttachmentsAsAlbum && drafts.size > 1) {
                            UUID.randomUUID().toString()
                        } else {
                            null
                        }
                        pendingAttachmentDrafts = emptyList()
                        pendingAttachmentsAsAlbum = false
                        prepareAttachmentDraftsForSend(
                            drafts = drafts,
                            groupAsAlbum = groupId != null,
                            caption = caption,
                            mediaGroupId = groupId,
                        ).forEach(onAttachDraft)
                    },
                )
            }
            val currentPanelHeight = composerPanelHeightDp.dp

            if (emojiPanelVisible) {
                val panelHeight = maxOf(currentPanelHeight, imeBottomDp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(panelHeight)
                ) {
                    TelegramXMediaPanel(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        gifs = state.gifs,
                        query = state.gifQuery,
                        isLoading = state.isLoadingGifs,
                        error = state.gifError,
                        onLoadGifs = { force -> onLoadGifs(force) },
                        onSearchGifs = onSearchGifs,
                        onEmoji = { emoji ->
                            composerController.insertEmoji(emoji)
                        },
                        onBackspace = {
                            composerController.dispatchBackspace()
                        },
                        onGif = { gifUrl ->
                            emojiPanelVisible = false
                            onSendGif(gifUrl)
                        },
                        onSticker = { sticker ->
                            composerController.insertEmoji(sticker)
                        },
                    )
                }
            } else if (imeBottomDp > 0.dp) {
                Spacer(Modifier.height(imeBottomDp))
            } else {
                Spacer(Modifier.height(navBottomDp))
            }
            }
        }

        val inSelectionMode = selectedMessageKeys.isNotEmpty()
        val topBarBgColor = when {
            inSelectionMode -> MaterialTheme.colorScheme.primary
            isDarkTheme -> Color(0xFF17212B)
            else -> Color(0xFFFFFFFF)
        }
        val topBarGlassAlpha = when {
            inSelectionMode -> 0.95f
            isDarkTheme -> 0.85f
            else -> 0.88f
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    val h = with(density) { coordinates.size.height.toDp() }
                    if (h > 0.dp && headerHeightDp != h) {
                        headerHeightDp = h
                    }
                },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = topBarBgColor.copy(alpha = topBarGlassAlpha),
                shadowElevation = 0.dp,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        if (isDarkTheme) Color(0x20FFFFFF) else Color(0x45FFFFFF),
                                        if (isDarkTheme) Color(0x08FFFFFF) else Color(0x15FFFFFF),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
                    ) {
                if (selectedMessageKeys.isNotEmpty()) {
                    val selectedMessages = state.messages.filter { it.stableKey in selectedMessageKeys }
                    val secret = state.conversation?.type == ConversationType.SECRET
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        navigationIcon = {
                            IconButton(onClick = ::dismissMessageInteraction) {
                                Icon(Icons.Default.Close, contentDescription = "خروج از انتخاب پیام")
                            }
                        },
                        title = { Text("${selectedMessages.size} انتخاب شده", fontWeight = FontWeight.SemiBold) },
                        actions = {
                            if (!secret) {
                                if (selectedMessages.size == 1) {
                                    IconButton(
                                        onClick = {
                                            replyingTo = selectedMessages.first()
                                            editingMessage = null
                                            dismissMessageInteraction()
                                        },
                                    ) { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "پاسخ به پیام") }
                                }
                                IconButton(
                                    enabled = selectedMessages.isNotEmpty(),
                                    onClick = {
                                        clipboard.setText(AnnotatedString(selectedMessages.joinToString("\n\n") { it.previewText() }))
                                        dismissMessageInteraction()
                                    },
                                ) { Icon(Icons.Default.ContentCopy, contentDescription = "کپی پیام‌های انتخاب شده") }
                                IconButton(
                                    enabled = selectedMessages.isNotEmpty(),
                                    onClick = { forwardingMessages = selectedMessages },
                                ) { Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "فوروارد پیام‌های انتخاب شده") }
                            }
                            IconButton(
                                enabled = selectedMessages.isNotEmpty(),
                                onClick = { deletingMessages = selectedMessages },
                            ) { Icon(Icons.Default.Delete, contentDescription = "حذف پیام‌های انتخاب شده") }
                        },
                    )
                } else if (searchVisible) {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                        title = {
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = onSearch,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Right,
                                ),
                                decorationBox = { field ->
                                    Box(contentAlignment = Alignment.CenterEnd) {
                                        if (state.searchQuery.isBlank()) {
                                            Text("جستجو در پیام‌ها...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        field()
                                    }
                                },
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                searchVisible = false
                                searchResultIndex = 0
                                onSearch("")
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بستن جستجوی پیام‌ها")
                            }
                        },
                        actions = {
                            if (state.isSearching) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (state.searchQuery.trim().length >= 2) {
                                Text(
                                    if (state.searchResults.isEmpty()) "نتیجه‌ای یافت نشد"
                                    else "${searchResultIndex + 1}/${state.searchResults.size}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(
                                    enabled = searchResultIndex > 0,
                                    onClick = { searchResultIndex -= 1 },
                                ) { Text("↑") }
                                TextButton(
                                    enabled = searchResultIndex < state.searchResults.lastIndex,
                                    onClick = { searchResultIndex += 1 },
                                ) { Text("↓") }
                            }
                        },
                    )
                } else {
                    val conversationTopBarHeight = if (density.fontScale > 1.3f) 72.dp else 56.dp
                    TopAppBar(
                        modifier = Modifier.height(conversationTopBarHeight),
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (state.conversation?.type == ConversationType.GROUP) {
                                            groupSheetVisible = true
                                        } else {
                                            openPartnerDetails()
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ChatAvatarImage(
                                    model = state.conversation?.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp).clip(CircleShape),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isSecretConversation) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = TelegramSecretGreen,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(
                                            title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSecretConversation && !isDarkTheme) Color(0xFF1B5E20) else Color.Unspecified,
                                        )
                                    }
                                    if (isSecretConversation) {
                                        val secretText = if (state.secretAutoDeleteSeconds > 0) {
                                            "گفتگوی محرمانه • تایمر: ${formatSecretAutoDeleteLabel(state.secretAutoDeleteSeconds)}"
                                        } else {
                                            "گفتگوی محرمانه • رمزگذاری سرتاسری"
                                        }
                                        Text(
                                            secretText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = TelegramSecretGreen,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    } else {
                                        val subtitle = when {
                                            state.connectionState == RealtimeConnectionState.CONNECTING ||
                                            state.connectionState == RealtimeConnectionState.RECONNECTING -> "در حال اتصال..."
                                            state.connectionState == RealtimeConnectionState.DISCONNECTED -> "در انتظار شبکه..."
                                            state.conversation?.type == ConversationType.GROUP -> when {
                                                state.conversation.typingUserIds.isNotEmpty() -> "در حال نوشتن..."
                                                state.isGroupLoading && state.groupMembers.isEmpty() -> "در حال بررسی اعضا..."
                                                else -> "${maxOf(state.groupInfo?.memberCount ?: 0, state.groupMembers.size)} عضو"
                                            }
                                            state.conversation?.typingUserIds?.isNotEmpty() == true -> "در حال نوشتن..."
                                            state.presence?.isOnline == true -> "آنلاین"
                                            state.presence?.canViewLastSeen == true ->
                                                formatUserPresence(state.presence.lastOnlineAtEpochMillis)
                                            else -> "آخرین بازدید به تازگی"
                                        }
                                        Text(
                                            subtitle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (subtitle == "آنلاین" || subtitle.startsWith("در حال")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = safeBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", modifier = Modifier.size(24.dp))
                            }
                        },
                        actions = {
                            if (isSecretConversation) {
                                IconButton(onClick = { autoDeleteDialogVisible = true }) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = "تنظیم تایمر حذف خودکار",
                                        tint = if (state.secretAutoDeleteSeconds > 0) TelegramSecretGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                IconButton(onClick = {
                                    onComputeSafetyNumber { }
                                    safetyNumberDialogVisible = true
                                }) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = "کلید رمزنگاری محرمانه",
                                        tint = TelegramSecretGreen,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            Box {
                                var menuVisible by remember { mutableStateOf(false) }
                                IconButton(onClick = { menuVisible = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌های گفتگو")
                                }
                                DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                                    DropdownMenuItem(
                                        text = { Text("جستجو") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        onClick = {
                                            menuVisible = false
                                            searchVisible = true
                                        },
                                    )
                                    if (state.conversation?.type == ConversationType.GROUP) {
                                        DropdownMenuItem(
                                            text = { Text("اطلاعات گروه") },
                                            leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                            onClick = {
                                                menuVisible = false
                                                groupSheetVisible = true
                                                onRefreshGroup()
                                            },
                                        )
                                    } else {
                                        if (isSecretConversation) {
                                            DropdownMenuItem(
                                                text = { Text("تایمر حذف خودکار") },
                                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                                onClick = {
                                                    menuVisible = false
                                                    autoDeleteDialogVisible = true
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("کد امنیتی (تأیید هویت E2EE)") },
                                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                                onClick = {
                                                    menuVisible = false
                                                    onComputeSafetyNumber { }
                                                    safetyNumberDialogVisible = true
                                                },
                                            )
                                        } else if (state.conversation?.type == ConversationType.PRIVATE) {
                                            DropdownMenuItem(
                                                text = { Text("شروع گفتگوی محرمانه") },
                                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                                onClick = {
                                                    menuVisible = false
                                                    onStartSecretChat()
                                                },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("اطلاعات کاربر") },
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                            onClick = {
                                                menuVisible = false
                                                openPartnerDetails()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("مسدود کردن", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                            onClick = {
                                                menuVisible = false
                                                blockingPeerId = state.conversation?.peerId
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                }

                if (state.pinnedMessages.isNotEmpty()) {
                    PinnedMessagesBar(
                        pinnedMessages = state.pinnedMessages,
                        onMessageClick = { pinned ->
                            val index = renderItemIndexForMessage(
                                messageRenderItems,
                                pinned.serverId ?: pinned.clientId,
                            )
                            if (index >= 0) coroutineScope.launch { listState.animateScrollToItem(index) }
                        },
                        onUnpinClick = { pinned ->
                            onTogglePinned(pinned)
                        },
                    )
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = if (inSelectionMode) {
                        Color.Transparent
                    } else if (isDarkTheme) {
                        Color(0x28FFFFFF)
                    } else {
                        Color(0x18000000)
                    },
                )
            }
        }
    }
    val featherColor = if (isDarkTheme) Color(0xFF17212B) else Color(0xFFFFFFFF)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        featherColor.copy(alpha = if (isDarkTheme) 0.35f else 0.40f),
                        featherColor.copy(alpha = if (isDarkTheme) 0.12f else 0.15f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        if (autoDeleteDialogVisible) {
            TelegramAutoDeleteDialog(
                currentSeconds = state.secretAutoDeleteSeconds,
                onDismiss = { autoDeleteDialogVisible = false },
                onSelect = { seconds ->
                    onSetSecretAutoDelete(seconds)
                    autoDeleteDialogVisible = false
                },
            )
        }
        if (safetyNumberDialogVisible) {
            TelegramEncryptionKeyDialog(
                safetyNumber = state.secretSafetyNumber,
                peerName = state.conversation?.title ?: "مخاطب",
                isLoading = state.isSafetyNumberLoading,
                onDismiss = { safetyNumberDialogVisible = false },
            )
        }
        AnimatedVisibility(
            visible = partnerProfileVisible && state.conversation?.type != ConversationType.GROUP,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
            ) + fadeOut(animationSpec = tween(180)),
        ) {
            PartnerDetailsScreen(
                state = state,
                onDismiss = { partnerProfileVisible = false },
                onToggleMute = onToggleMute,
                onOpenProfile = onOpenProfile,
                onOpenSearch = {
                    partnerProfileVisible = false
                    searchVisible = true
                },
                onToggleBlock = onToggleBlock,
                onReportUser = onReportUser,
                onDeleteConversation = onDeleteConversation,
                onAttachmentClick = { viewedAttachment = it },
            )
        }

        AnimatedVisibility(
            visible = groupSheetVisible && state.conversation?.type == ConversationType.GROUP,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
            ) + fadeOut(animationSpec = tween(180)),
        ) {
            GroupDetailsScreen(
                state = state,
                clipboard = clipboard,
                onDismiss = { groupSheetVisible = false },
                onRefresh = onRefreshGroup,
                onToggleMute = onToggleMute,
                onOpenSearch = {
                    groupSheetVisible = false
                    searchVisible = true
                },
                onUpdateName = onUpdateGroupName,
                onAddMembers = onAddGroupMembers,
                onSearchUsers = onSearchGroupUsers,
                onRemoveMember = onRemoveGroupMember,
                onSetAdmin = onSetGroupAdmin,
                onSetInviteEnabled = onSetGroupInviteEnabled,
                onRegenerateInvite = onRegenerateGroupInvite,
                onLeave = { terminalGroupAction = "leave" },
                onDelete = { terminalGroupAction = "delete" },
                onAttachmentClick = { viewedAttachment = it },
            )
        }
    }

    (messageInteraction as? MessageInteractionState.Context)?.let { contextInteraction ->
        val message = state.messages.firstOrNull { it.stableKey == contextInteraction.messageKey }
        if (message == null) {
            LaunchedEffect(contextInteraction.messageKey) {
                dismissMessageInteraction()
            }
            return@let
        }
        MessageContextMenu(
            message = message,
            bubbleBounds = contextInteraction.bubbleBounds,
            capabilities = MessageContextCapabilities(
                conversationType = state.conversation?.type ?: ConversationType.PRIVATE,
                allowsPinning = state.conversation?.type != ConversationType.GROUP ||
                    state.groupInfo?.isAdmin == true,
            ),
            onDismiss = ::dismissMessageInteraction,
            onReact = { emoji ->
                onReact(message, emoji)
                dismissMessageInteraction()
            },
            onReply = {
                replyingTo = message
                editingMessage = null
                dismissMessageInteraction()
            },
            onCopy = {
                clipboard.setText(AnnotatedString(message.previewText()))
                dismissMessageInteraction()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("متن در حافظه کپی شد")
                }
            },
            onForward = {
                forwardingMessage = message
                dismissMessageInteraction()
            },
            onSelect = {
                startSelection(message.stableKey)
            },
            onInfo = {
                infoMessage = message
                dismissMessageInteraction()
            },
            onTogglePinned = {
                onTogglePinned(message)
                dismissMessageInteraction()
            },
            onEdit = {
                editingMessage = message
                replyingTo = null
                dismissMessageInteraction()
            },
            onDelete = {
                deletingMessage = message
                dismissMessageInteraction()
            },
        )
    }

    forwardingMessage?.let { message ->
        ForwardMessageSheet(
            conversations = state.forwardTargets,
            onDismiss = { forwardingMessage = null },
            onSelect = { target ->
                onForward(message, target.id)
                forwardingMessage = null
            },
        )
    }

    if (forwardingMessages.isNotEmpty()) {
        ForwardMessageSheet(
            conversations = state.forwardTargets,
            onDismiss = { forwardingMessages = emptyList() },
            onSelect = { target ->
                forwardingMessages.forEach { onForward(it, target.id) }
                forwardingMessages = emptyList()
                dismissMessageInteraction()
            },
        )
    }

    deletingMessage?.let { message ->
        DeleteMessageDialog(
            isMine = message.isMine,
            onDismiss = { deletingMessage = null },
            onDelete = { forEveryone ->
                onDelete(message, forEveryone)
                deletingMessage = null
            },
        )
    }

    if (deletingMessages.isNotEmpty()) {
        DeleteMessagesDialog(
            count = deletingMessages.size,
            allMine = deletingMessages.all { it.isMine },
            onDismiss = { deletingMessages = emptyList() },
            onDelete = { forEveryone ->
                deletingMessages.forEach { onDelete(it, forEveryone) }
                deletingMessages = emptyList()
                dismissMessageInteraction()
            },
        )
    }

    infoMessage?.let { message ->
        MessageInfoDialog(message = message, onDismiss = { infoMessage = null })
    }
    blockingPeerId?.let { peerId ->
        AlertDialog(
            onDismissRequest = { blockingPeerId = null },
            title = { Text("مسدود کردن") },
            text = { Text("آیا از مسدود کردن ${state.conversation?.title.orEmpty()} اطمینان دارید؟") },
            confirmButton = {
                TextButton(onClick = {
                    onToggleBlock(peerId) { success -> if (success) blockingPeerId = null }
                }) { Text("بله", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { blockingPeerId = null }) { Text("انصراف") } },
        )
    }

    terminalGroupAction?.let { action ->
        AlertDialog(
            onDismissRequest = { terminalGroupAction = null },
            title = { Text(if (action == "delete") "حذف گروه" else "ترک گروه") },
            text = {
                Text(
                    if (action == "delete") "گروه برای همه اعضا حذف شود؟ این عمل قابل بازگشت نیست."
                    else "از این گروه خارج می‌شوید. ادامه می‌دهید؟",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val complete: (Boolean) -> Unit = { if (it) groupSheetVisible = false }
                        if (action == "delete") onDeleteGroup(complete) else onLeaveGroup(complete)
                        terminalGroupAction = null
                    },
                ) { Text("تأیید", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { terminalGroupAction = null }) { Text("انصراف") } },
        )
    }
    viewedAttachment?.let { attachment ->
        val activeMediaList = remember(state.messages) {
            state.messages.mapNotNull { msg ->
                msg.attachment?.takeIf {
                    it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.GIF || it.kind == AttachmentKind.VIDEO
                }?.let { att ->
                    val url = att.localUri?.takeIf(String::isNotBlank) ?: att.remoteUrl?.takeIf(String::isNotBlank)
                    if (url != null) {
                        MediaViewerItem(
                            url = url,
                            isVideo = att.kind == AttachmentKind.VIDEO,
                            title = att.fileName,
                            caption = (msg.content as? MessageContent.Text)?.value,
                        )
                    } else null
                }
            }
        }
        val targetUrl = attachment.localUri?.takeIf(String::isNotBlank) ?: attachment.remoteUrl?.takeIf(String::isNotBlank)
        val initialIndex = activeMediaList.indexOfFirst { it.url == targetUrl }.coerceAtLeast(0)

        if (attachment.kind == AttachmentKind.IMAGE || attachment.kind == AttachmentKind.GIF || attachment.kind == AttachmentKind.VIDEO) {
            FullscreenMediaViewerDialog(
                mediaItems = if (activeMediaList.isNotEmpty()) activeMediaList else listOf(
                    MediaViewerItem(
                        url = targetUrl.orEmpty(),
                        isVideo = attachment.kind == AttachmentKind.VIDEO,
                        title = attachment.fileName,
                    )
                ),
                initialIndex = initialIndex,
                onDismiss = { viewedAttachment = null },
            )
        } else {
            AttachmentViewer(attachment = attachment, onDismiss = { viewedAttachment = null })
        }
    }
}

@Composable
private fun GroupSenderFrame(
    enabled: Boolean,
    senderId: String,
    senderName: String,
    avatarUrl: String?,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier.width(38.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (isLastInGroup) {
                    ChatAvatarImage(
                        model = avatarUrl,
                        contentDescription = "نمایه $senderName",
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onOpenProfile(senderId) }
                            .testTag("group-message-avatar:$senderId"),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                if (isFirstInGroup) {
                    Text(
                        text = senderName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onOpenProfile(senderId) }
                            .padding(start = 14.dp, end = 12.dp, top = 2.dp, bottom = 1.dp)
                            .testTag("group-message-sender:$senderId"),
                    )
                }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaAlbumBubble(
    messages: List<Message>,
    downloads: Map<String, DownloadTask>,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onRetry: (Message) -> Unit,
    onCancelTransfer: (Message) -> Unit,
    onStartDownload: (Message) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onAttachmentClick: (ir.coffevista.vista_native.features.chat.domain.model.Attachment) -> Unit,
) {
    val visibleMessages = messages.take(10)
    if (visibleMessages.size < 2) return
    val primary = visibleMessages.first()
    val caption = visibleMessages.firstNotNullOfOrNull { message ->
        (message.content as? MessageContent.Text)?.value?.trim()?.takeIf(String::isNotEmpty)
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .absolutePadding(
                left = if (primary.isMine) 12.dp else 6.dp,
                right = if (primary.isMine) 6.dp else 12.dp,
                top = 3.dp,
                bottom = 3.dp,
            ),
        horizontalArrangement = if (primary.isMine) Arrangement.Start else Arrangement.End,
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (selectionMode) {
                IconButton(
                    onClick = onToggleSelection,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .absoluteOffset(x = if (primary.isMine) (-6).dp else 0.dp)
                        .size(48.dp)
                        .testTag("media-album-selection")
                        .semantics {
                            contentDescription = if (selected) {
                                "آلبوم انتخاب شده، لغو انتخاب"
                            } else {
                                "انتخاب آلبوم"
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(
                                1.5.dp,
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                CircleShape,
                            )
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Done,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(
                        if (primary.isMine) AbsoluteAlignment.CenterRight
                        else AbsoluteAlignment.CenterLeft,
                    )
                    .widthIn(min = 140.dp, max = screenWidth * 0.74f)
                    .fillMaxWidth()
                    .then(
                        if (selected) {
                            Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                RoundedCornerShape(12.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .testTag("media-album"),
            ) {
                Box {
                    MediaAlbumGrid(
                        messages = visibleMessages,
                        downloads = downloads,
                        selectionMode = selectionMode,
                        onToggleSelection = onToggleSelection,
                        onLongPress = onLongPress,
                        onRetry = onRetry,
                        onCancelTransfer = onCancelTransfer,
                        onStartDownload = onStartDownload,
                        onPauseDownload = onPauseDownload,
                        onResumeDownload = onResumeDownload,
                        onCancelDownload = onCancelDownload,
                        onAttachmentClick = onAttachmentClick,
                    )
                    if (caption == null) {
                        AlbumMessageMeta(
                            message = primary,
                            overlay = true,
                            modifier = Modifier
                                .align(AbsoluteAlignment.BottomRight)
                                .padding(6.dp),
                        )
                    }
                }
                if (caption != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (primary.isMine) VistaBrandColors.VioletDeep
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        TelegramEmojiText(
                            text = caption,
                            modifier = Modifier.weight(1f),
                            style = TextStyle(
                                color = if (primary.isMine) Color.White
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 18.9.sp,
                                textDirection = resolveMessageTextDirection(caption),
                            ),
                            emojiSize = 18.dp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        AlbumMessageMeta(message = primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaAlbumGrid(
    messages: List<Message>,
    downloads: Map<String, DownloadTask>,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onRetry: (Message) -> Unit,
    onCancelTransfer: (Message) -> Unit,
    onStartDownload: (Message) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onAttachmentClick: (ir.coffevista.vista_native.features.chat.domain.model.Attachment) -> Unit,
) {
    val tile: @Composable (Message, Modifier, AbsoluteRoundedCornerShape) -> Unit =
        { message, modifier, shape ->
            MediaAlbumTile(
                message = message,
                downloadTask = downloads[message.serverId ?: message.clientId],
                modifier = modifier,
                shape = shape,
                selectionMode = selectionMode,
                onToggleSelection = onToggleSelection,
                onLongPress = onLongPress,
                onRetry = onRetry,
                onCancelTransfer = onCancelTransfer,
                onStartDownload = onStartDownload,
                onPauseDownload = onPauseDownload,
                onResumeDownload = onResumeDownload,
                onCancelDownload = onCancelDownload,
                onAttachmentClick = onAttachmentClick,
            )
        }
    when (messages.size) {
        2 -> Row(Modifier.aspectRatio(1.75f)) {
            tile(
                messages[0],
                Modifier.weight(1f).fillMaxSize(),
                AbsoluteRoundedCornerShape(topLeft = 12.dp, bottomLeft = 12.dp),
            )
            Spacer(Modifier.width(1.dp))
            tile(
                messages[1],
                Modifier.weight(1f).fillMaxSize(),
                AbsoluteRoundedCornerShape(topRight = 12.dp, bottomRight = 12.dp),
            )
        }
        3 -> Row(Modifier.aspectRatio(1.5f)) {
            tile(
                messages[0],
                Modifier.weight(2f).fillMaxSize(),
                AbsoluteRoundedCornerShape(topLeft = 12.dp, bottomLeft = 12.dp),
            )
            Spacer(Modifier.width(1.dp))
            Column(Modifier.weight(1f).fillMaxSize()) {
                tile(
                    messages[1],
                    Modifier.weight(1f).fillMaxSize(),
                    AbsoluteRoundedCornerShape(topRight = 12.dp),
                )
                Spacer(Modifier.height(1.dp))
                tile(
                    messages[2],
                    Modifier.weight(1f).fillMaxSize(),
                    AbsoluteRoundedCornerShape(bottomRight = 12.dp),
                )
            }
        }
        4 -> Column(Modifier.aspectRatio(1f)) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                tile(
                    messages[0],
                    Modifier.weight(1f).fillMaxSize(),
                    AbsoluteRoundedCornerShape(topLeft = 12.dp),
                )
                Spacer(Modifier.width(1.dp))
                tile(
                    messages[1],
                    Modifier.weight(1f).fillMaxSize(),
                    AbsoluteRoundedCornerShape(topRight = 12.dp),
                )
            }
            Spacer(Modifier.height(1.dp))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                tile(
                    messages[2],
                    Modifier.weight(1f).fillMaxSize(),
                    AbsoluteRoundedCornerShape(bottomLeft = 12.dp),
                )
                Spacer(Modifier.width(1.dp))
                tile(
                    messages[3],
                    Modifier.weight(1f).fillMaxSize(),
                    AbsoluteRoundedCornerShape(bottomRight = 12.dp),
                )
            }
        }
        else -> {
            val rows = albumRowIndices(messages.size)
            Column(Modifier.aspectRatio(3f / rows.size)) {
                rows.forEachIndexed { rowIndex, indices ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        indices.forEachIndexed { columnIndex, messageIndex ->
                            tile(
                                messages[messageIndex],
                                Modifier.weight(1f).fillMaxSize(),
                                adaptiveAlbumTileShape(
                                    rowIndex = rowIndex,
                                    columnIndex = columnIndex,
                                    rowCount = rows.size,
                                    columnCount = indices.size,
                                ),
                            )
                            if (columnIndex != indices.lastIndex) Spacer(Modifier.width(1.dp))
                        }
                    }
                    if (rowIndex != rows.lastIndex) Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaAlbumTile(
    message: Message,
    downloadTask: DownloadTask?,
    modifier: Modifier,
    shape: AbsoluteRoundedCornerShape,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onRetry: (Message) -> Unit,
    onCancelTransfer: (Message) -> Unit,
    onStartDownload: (Message) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onAttachmentClick: (ir.coffevista.vista_native.features.chat.domain.model.Attachment) -> Unit,
) {
    val attachment = message.attachment ?: return
    val source = attachment.localUri ?: attachment.remoteUrl
    val activeUpload = attachment.transferState == TransferState.UPLOADING ||
        attachment.transferState == TransferState.QUEUED
    val needsDownload = attachment.transferState == TransferState.COMPLETE &&
        attachment.localUri.isNullOrBlank() &&
        !attachment.remoteUrl.isNullOrBlank()
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.12f))
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelection()
                        return@combinedClickable
                    }
                    when {
                        message.status == MessageStatus.FAILED ||
                            attachment.transferState == TransferState.FAILED ||
                            attachment.transferState == TransferState.CANCELLED -> onRetry(message)
                        downloadTask?.state == DownloadState.COMPLETE &&
                            !downloadTask.localUri.isNullOrBlank() ->
                            onAttachmentClick(attachment.copy(localUri = downloadTask.localUri))
                        !attachment.localUri.isNullOrBlank() -> onAttachmentClick(attachment)
                        downloadTask?.state == DownloadState.DOWNLOADING ->
                            onPauseDownload(downloadTask.messageId)
                        downloadTask != null && downloadTask.state in setOf(
                            DownloadState.PAUSED,
                            DownloadState.FAILED,
                            DownloadState.CANCELLED,
                            DownloadState.QUEUED,
                        ) -> onResumeDownload(downloadTask.messageId)
                        needsDownload -> onStartDownload(message)
                    }
                },
                onLongClick = onLongPress,
            )
            .testTag("media-album-tile:" + message.stableKey)
            .semantics {
                contentDescription = attachment.fileName ?: "تصویر آلبوم"
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = source,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        when {
            activeUpload -> {
                CircularProgressIndicator(
                    progress = { attachment.progress.coerceIn(0f, 1f) },
                    color = Color.White,
                    trackColor = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                )
                IconButton(
                    onClick = { onCancelTransfer(message) },
                    modifier = Modifier.align(Alignment.BottomEnd).size(36.dp),
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "لغو ارسال تصویر", tint = Color.White)
                }
            }
            downloadTask?.state == DownloadState.DOWNLOADING -> {
                CircularProgressIndicator(
                    progress = { downloadTask.progress.coerceIn(0f, 1f) },
                    color = Color.White,
                    trackColor = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                    strokeWidth = 3.dp,
                )
                IconButton(
                    onClick = { onCancelDownload(downloadTask.messageId) },
                    modifier = Modifier.align(Alignment.BottomEnd).size(36.dp),
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "لغو دانلود تصویر", tint = Color.White)
                }
            }
            downloadTask?.state in setOf(DownloadState.PAUSED, DownloadState.QUEUED) ->
                AlbumTileOverlayIcon(Icons.Default.PlayArrow, "ادامه دانلود")
            downloadTask?.state in setOf(DownloadState.FAILED, DownloadState.CANCELLED) ||
                attachment.transferState in setOf(TransferState.FAILED, TransferState.CANCELLED) ||
                message.status == MessageStatus.FAILED ->
                AlbumTileOverlayIcon(Icons.Default.Refresh, "تلاش مجدد")
            needsDownload && downloadTask?.state != DownloadState.COMPLETE ->
                AlbumTileOverlayIcon(Icons.Default.ArrowDownward, "دانلود تصویر")
        }
    }
}

@Composable
private fun AlbumTileOverlayIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.48f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun AlbumMessageMeta(
    message: Message,
    overlay: Boolean = false,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .then(
                    if (overlay) {
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                formatTime(message.createdAtEpochMillis),
                color = if (overlay) {
                    Color.White
                } else if (message.isMine) {
                    Color.White.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            if (message.isMine) {
                Spacer(Modifier.width(3.dp))
                MessageStatusMark(message.status)
            }
        }
    }
}

private fun adaptiveAlbumTileShape(
    rowIndex: Int,
    columnIndex: Int,
    rowCount: Int,
    columnCount: Int,
): AbsoluteRoundedCornerShape = AbsoluteRoundedCornerShape(
    topLeft = if (rowIndex == 0 && columnIndex == 0) 12.dp else 0.dp,
    topRight = if (rowIndex == 0 && columnIndex == columnCount - 1) 12.dp else 0.dp,
    bottomLeft = if (rowIndex == rowCount - 1 && columnIndex == 0) 12.dp else 0.dp,
    bottomRight = if (rowIndex == rowCount - 1 && columnIndex == columnCount - 1) 12.dp else 0.dp,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    searchQuery: String? = null,
    onRetry: (Message) -> Unit,
    onCancelTransfer: (Message) -> Unit,
    downloadTask: DownloadTask? = null,
    onStartDownload: (Message) -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onOpenContextMenu: (Rect?) -> Unit = {},
    onReply: () -> Unit,
    onReact: (String) -> Unit = {},
    animateEntry: Boolean = false,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    isSecretConversation: Boolean = false,
    secretAutoDeleteSeconds: Int = 0,
    onAttachmentClick: (ir.coffevista.vista_native.features.chat.domain.model.Attachment) -> Unit,
    onJumpToRepliedMessage: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
) {
    val isDeleted = message.content == MessageContent.Deleted
    val sharedPost = remember(message.content) {
        if (isDeleted) null
        else when (val content = message.content) {
            is MessageContent.Structured -> SharedPostParser.tryParse(content.payload)
            is MessageContent.Text -> SharedPostParser.tryParse(content.value)
            else -> null
        }
    }
    val text = when (val content = message.content) {
        is MessageContent.Text -> if (sharedPost != null) "" else content.value
        is MessageContent.Structured -> if (sharedPost != null) "" else content.payload
        MessageContent.Deleted -> "این پیام حذف شده است"
        MessageContent.EncryptedUnavailable -> "🔒 پیام رمزگذاری‌شده"
    }
    val description = buildString {
        append(if (message.isMine) "پیام شما" else "پیام دریافتی")
        append("، ")
        append(statusDescription(message.status))
    }
    val density = LocalDensity.current
    val coordinatesHolder = remember(message.stableKey) { LayoutCoordinatesHolder() }
    val chatEntryMode = LocalChatEntryMode.current
    val entryProgress = if (animateEntry && chatEntryMode != "off") {
        val progress by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (chatEntryMode == "minimal") 120 else 220,
                easing = FastOutSlowInEasing,
            ),
            label = "message-entry",
        )
        progress
    } else {
        1f
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entryProgress
                translationY = (1f - entryProgress) * with(density) { 8.dp.toPx() }
            }
            .absolutePadding(
                left = if (message.isMine) 12.dp else 6.dp,
                right = if (message.isMine) 6.dp else 12.dp,
                top = 3.dp,
                bottom = 3.dp,
            ),
        horizontalArrangement = if (message.isMine) Arrangement.Start else Arrangement.End,
    ) {
            val cornerRounding = messageBubbleCornerRounding(
                isMine = message.isMine,
                groupPosition = MessageGroupPosition(isFirstInGroup, isLastInGroup),
            )
            val shape = AbsoluteRoundedCornerShape(
                topLeft = if (cornerRounding.topLeft) 18.dp else 6.dp,
                topRight = if (cornerRounding.topRight) 18.dp else 6.dp,
                bottomLeft = if (cornerRounding.bottomLeft) 18.dp else 6.dp,
                bottomRight = if (cornerRounding.bottomRight) 18.dp else 6.dp,
            )
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val base = Modifier
                .widthIn(min = 60.dp, max = screenWidth * 0.82f)
                .shadow(
                    elevation = 1.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(shape)
                .combinedClickable(
                    onClick = {
                        if (isDeleted) return@combinedClickable
                        if (selectionMode) {
                            onToggleSelection()
                            return@combinedClickable
                        }
                        val attachment = message.attachment
                        val task = downloadTask
                        when {
                            message.status == MessageStatus.FAILED ||
                                attachment?.transferState == TransferState.FAILED ||
                                attachment?.transferState == TransferState.CANCELLED -> onRetry(message)
                            sharedPost != null -> onOpenPost(sharedPost.postId)
                            task?.state == DownloadState.COMPLETE && !task.localUri.isNullOrBlank() && attachment != null ->
                                onAttachmentClick(attachment.copy(localUri = task.localUri))
                            attachment?.transferState == TransferState.COMPLETE && !attachment.localUri.isNullOrBlank() ->
                                onAttachmentClick(attachment)
                            task?.state == DownloadState.DOWNLOADING -> onPauseDownload(task.messageId)
                            task != null && task.state in setOf(DownloadState.PAUSED, DownloadState.FAILED, DownloadState.CANCELLED, DownloadState.QUEUED) ->
                                onResumeDownload(task.messageId)
                            attachment?.transferState == TransferState.COMPLETE && !attachment.remoteUrl.isNullOrBlank() ->
                                onStartDownload(message)
                            // Text payloads have no direct primary action.
                            // One tap opens the already permission-gated
                            // contextual surface; media/retry transfers keep
                            // their established direct interactions above.
                            else -> onOpenContextMenu(coordinatesHolder.coordinates?.takeIf { it.isAttached }?.boundsInWindow())
                        }
                    },
                    onLongClick = { onLongPress() },
                )
                .semantics { contentDescription = description }
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                if (selectionMode) {
                    val selectionIndicatorColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        animationSpec = tween(durationMillis = 120),
                        label = "message-selection-indicator",
                    )
                    IconButton(
                        onClick = onToggleSelection,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            // Rows have asymmetric physical outer padding. Offset
                            // only the outgoing row so every checkbox occupies the
                            // same fixed left rail without moving either bubble.
                            .absoluteOffset(x = if (message.isMine) (-6).dp else 0.dp)
                            .size(48.dp)
                            .testTag("message-selection-checkbox:${message.stableKey}")
                            .semantics {
                                contentDescription = if (selected) {
                                    "پیام انتخاب شده، لغو انتخاب"
                                } else {
                                    "انتخاب پیام"
                                }
                            },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, selectionIndicatorColor, CircleShape)
                                .background(
                                    if (selected) selectionIndicatorColor else Color.Transparent,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        // This screen is RTL, where logical End resolves to the
                        // physical left edge. Message ownership, however, has a
                        // product-level physical contract: our messages are on
                        // the right and peer messages are on the left.
                        .align(
                            if (message.isMine) {
                                AbsoluteAlignment.CenterRight
                            } else {
                                AbsoluteAlignment.CenterLeft
                            },
                        )
                        // Selection is rendered in a separate fixed rail below.  Never
                        // reserve its width inside the bubble: doing that shifts the
                        // text and makes selected outgoing messages visibly "jump".
                        .then(if (message.isMine) {
                            base.background(Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep)))
                        } else {
                            base.background(MaterialTheme.colorScheme.surfaceVariant)
                        }).then(
                        if (selected) Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                            shape = shape,
                        ) else Modifier
                    )
                        .onGloballyPositioned { coordinates ->
                            coordinatesHolder.coordinates = coordinates
                        }
                        .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 6.dp),
                ) {
                if (!isDeleted && message.isForwarded) {
                    Row(
                        Modifier.padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Forward,
                            contentDescription = null,
                            tint = if (message.isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "فوروارد شده از ${message.forwardedFromSenderName?.trim()?.takeIf(String::isNotEmpty) ?: "کاربر"}",
                            color = if (message.isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
                if (!isDeleted && !message.replyToContent.isNullOrBlank()) {
                    val rawReplyTarget = message.replyToMessageId?.trim().orEmpty()
                    val replySender = message.replyToSenderName?.trim().orEmpty()
                    val isNoteReply = rawReplyTarget.startsWith("note:") || replySender.startsWith("یادداشت")
                    val isStoryReply = rawReplyTarget.startsWith("story:") || replySender.startsWith("استوری")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (message.isMine) Color.White.copy(alpha = 0.15f)
                                else Color.Black.copy(alpha = 0.05f),
                            )
                            .clickable(onClick = onJumpToRepliedMessage)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(if (isNoteReply || isStoryReply) 46.dp else 40.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(if (message.isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            if (isStoryReply || isNoteReply) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(bottom = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (message.isMine) Color.White.copy(alpha = 0.22f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isStoryReply) Icons.Outlined.AutoStories else Icons.Outlined.EditNote,
                                        contentDescription = null,
                                        tint = if (message.isMine) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = if (isStoryReply) "پاسخ به استوری" else "پاسخ به یادداشت",
                                        color = if (message.isMine) Color.White else MaterialTheme.colorScheme.primary,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Text(
                                if (isStoryReply || isNoteReply) replySender.ifBlank { "کاربر" }
                                else "پاسخ به ${replySender.ifBlank { "کاربر" }}",
                                color = if (message.isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            val replyDisplayContent = remember(message.replyToContent) {
                                SharedPostParser.tryParse(message.replyToContent)?.let { parsedPost ->
                                    "پست از ${parsedPost.authorName}: ${parsedPost.content.ifBlank { "رسانه" }}"
                                } ?: message.replyToContent
                            }
                            Text(
                                replyDisplayContent,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (message.isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                if (!isDeleted) message.attachment?.let { attachment ->
                    AttachmentBubble(
                        message = message,
                        attachment = attachment,
                        downloadTask = downloadTask,
                        onCancel = { onCancelTransfer(message) },
                        onStartDownload = { onStartDownload(message) },
                        onPauseDownload = onPauseDownload,
                        onResumeDownload = onResumeDownload,
                        onCancelDownload = onCancelDownload,
                        onAttachmentClick = onAttachmentClick,
                    )
                    if (text.isNotBlank()) Spacer(Modifier.height(6.dp))
                }
                val messageTextDir = resolveMessageTextDirection(text)
                val isRtlMessage = messageTextDir == androidx.compose.ui.text.style.TextDirection.Rtl ||
                    messageTextDir == androidx.compose.ui.text.style.TextDirection.ContentOrRtl

                val metaFooter: @Composable () -> Unit = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSecretConversation && secretAutoDeleteSeconds > 0) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "تایمر خودکار",
                                    tint = if (message.isMine) Color.White.copy(alpha = 0.85f) else TelegramSecretGreen,
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(Modifier.width(2.5.dp))
                            }
                            if (message.editedAtEpochMillis != null) {
                                Text(
                                    "ویرایش شده",
                                    color = if (message.isMine) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic,
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                formatTime(message.createdAtEpochMillis),
                                color = if (message.isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                            )
                            if (message.isMine) {
                                Spacer(Modifier.width(3.dp))
                                MessageStatusMark(message.status)
                            }
                        }
                    }
                }

                if (sharedPost != null) {
                    Column {
                        SharedPostCard(
                            post = sharedPost,
                            isMine = message.isMine,
                            onViewPost = onOpenPost,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.align(if (isRtlMessage) Alignment.Start else Alignment.End)) {
                            metaFooter()
                        }
                    }
                } else if (text.isNotBlank()) {
                    ChatTextBubbleLayout(
                        text = {
                            TelegramEmojiText(
                                text = text,
                                style = TextStyle(
                                    color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.5.sp,
                                    lineHeight = 21.75.sp,
                                    textDirection = messageTextDir,
                                ),
                                emojiSize = 18.dp,
                                highlightQuery = searchQuery,
                                highlightColor = if (message.isMine) {
                                    Color.White.copy(alpha = 0.32f)
                                } else {
                                    VistaBrandColors.Indigo.copy(alpha = 0.28f)
                                },
                            )
                        },
                        footer = metaFooter,
                        layoutDirection = if (isRtlMessage) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    )
                } else {
                    Box(Modifier.align(if (isRtlMessage) Alignment.Start else Alignment.End)) {
                        metaFooter()
                    }
                }
                val previewUrl = remember(text) { if (!isDeleted && text.isNotBlank()) extractFirstUrl(text) else null }
                if (previewUrl != null) {
                    Spacer(Modifier.height(6.dp))
                    ChatLinkPreviewCard(
                        url = previewUrl,
                        isMine = message.isMine,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                AnimatedContent(
                    targetState = if (isDeleted) emptyMap() else message.reactions,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.92f))
                            .togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.92f))
                    },
                    label = "message-reactions",
                ) { reactions ->
                    if (reactions.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.padding(top = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(reactions.entries.toList(), key = { it.key }) { reaction ->
                                val isMyReaction = message.accountId in reaction.value
                                val chipScale by animateFloatAsState(
                                    targetValue = if (isMyReaction) 1.06f else 1f,
                                    animationSpec = tween(160, easing = FastOutSlowInEasing),
                                    label = "reaction-scale",
                                )
                                val chipColor by animateColorAsState(
                                    targetValue = when {
                                        isMyReaction -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        message.isMine -> Color.White.copy(alpha = 0.16f)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    animationSpec = tween(180),
                                    label = "reaction-color",
                                )
                                Row(
                                    modifier = Modifier
                                        .graphicsLayer { scaleX = chipScale; scaleY = chipScale }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipColor)
                                        .clickable(role = Role.Button) { onReact(reaction.key) }
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    TelegramEmoji(emoji = reaction.key, size = 13.dp)
                                    Text(
                                        "${reaction.value.size}",
                                        fontSize = 11.sp,
                                        color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposerContextBanner(
    label: String,
    preview: String,
    onDismiss: () -> Unit,
    badgeIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badgeText: String? = null,
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .shadow(3.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                else Color.White.copy(alpha = 0.96f),
            )
            .border(
                0.5.dp,
                if (isDark) Color(0x22FFFFFF) else Color(0x18000000),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.5.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (badgeIcon != null && badgeText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp),
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = badgeText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = preview,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "لغو",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDetailsSheet(
    state: MessagesUiState,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateName: (String) -> Unit,
    onAddMembers: (List<String>) -> Unit,
    onSearchUsers: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetAdmin: (String, Boolean) -> Unit,
    onSetInviteEnabled: (Boolean) -> Unit,
    onRegenerateInvite: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    val info = state.groupInfo
    var editedName by remember(info?.id, info?.name) { mutableStateOf(info?.name.orEmpty()) }
    val currentUserId = info?.currentUserId
    val isCreator = currentUserId != null && currentUserId == info?.createdByUserId
    val remainingSlots = ((info?.maxMembers ?: 20) - state.groupMembers.size).coerceAtLeast(0)
    var pendingMemberAction by remember { mutableStateOf<Pair<GroupMember, Boolean>?>(null) }
    var pendingRemoval by remember { mutableStateOf<GroupMember?>(null) }
    var addMembersExpanded by rememberSaveable(info?.id) { mutableStateOf(false) }
    var selectedNewMemberIds by remember(info?.id) { mutableStateOf(emptySet<String>()) }
    val existingMemberIds = state.groupMembers.mapTo(mutableSetOf()) { it.userId }
    val conversationCandidates = state.forwardTargets.asSequence()
        .filter { it.type == ConversationType.PRIVATE && !it.peerId.isNullOrBlank() }
        .mapNotNull { candidate ->
            candidate.peerId?.takeIf(String::isNotBlank)?.let { peerId ->
                ChatUser(
                    id = peerId,
                    username = candidate.title,
                    fullName = candidate.title,
                    avatarUrl = candidate.avatarUrl,
                    conversationId = candidate.id,
                )
            }
        }
        .toList()
    val localQuery = state.groupUserQuery.trim()
    val queriedCandidates = state.groupUserResults ?: conversationCandidates.filter { candidate ->
        localQuery.isEmpty() ||
            candidate.displayName.contains(localQuery, ignoreCase = true) ||
            candidate.username.contains(localQuery.removePrefix("@"), ignoreCase = true)
    }
    val addCandidates = (queriedCandidates + if (localQuery.isEmpty()) conversationCandidates else emptyList())
        .filter { it.id !in existingMemberIds }
        .distinctBy(ChatUser::id)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .padding(horizontal = 18.dp)
                .testTag("group-details-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "group-header") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val fallback = painterResource(R.drawable.vista_default_avatar)
                    AsyncImage(
                        model = info?.imageUrl ?: state.conversation?.avatarUrl,
                        contentDescription = null,
                        placeholder = fallback,
                        error = fallback,
                        fallback = fallback,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(info?.name ?: state.conversation?.title.orEmpty(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${info?.memberCount ?: state.groupMembers.size} عضو",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    if (state.isGroupLoading) CircularProgressIndicator(Modifier.padding(8.dp).size(22.dp), strokeWidth = 2.dp)
                    state.groupError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }
            if (info?.isAdmin == true) {
                item(key = "group-edit") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it.take(50) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("نام گروه") },
                        )
                        TextButton(
                            onClick = { onUpdateName(editedName.trim()) },
                            enabled = editedName.isNotBlank() && editedName.trim() != info.name && !state.isGroupLoading,
                        ) { Text("ذخیره") }
                    }
                }
            }
            item(key = "invite") {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                    Text("لینک دعوت", fontWeight = FontWeight.SemiBold)
                    val code = info?.inviteCode
                    if (!code.isNullOrBlank()) {
                        val link = "https://cafevista.ir/group/$code"
                        Text(link, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(link)) }) { Text("کپی لینک") }
                    } else {
                        Text("لینک فعالی وجود ندارد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    if (info?.isAdmin == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("فعال", modifier = Modifier.weight(1f))
                            ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(
                                checked = info.inviteEnabled,
                                onCheckedChange = onSetInviteEnabled,
                                enabled = !state.isGroupLoading,
                            )
                        }
                        TextButton(onClick = onRegenerateInvite, enabled = !state.isGroupLoading) { Text("ساخت لینک جدید") }
                    }
                }
            }
            item(key = "members-title") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("اعضا", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "تازه‌سازی اعضا") }
                }
            }
            items(
                state.groupMembers.sortedWith(
                    compareByDescending<GroupMember> { it.userId == info?.createdByUserId }
                        .thenByDescending { it.isAdmin }
                        .thenBy { it.displayName.lowercase() },
                ),
                key = { it.userId },
            ) { member ->
                val isMe = member.userId == currentUserId
                val isTargetCreator = member.userId == info?.createdByUserId
                val canManage = !isMe && (
                    isCreator || (info?.isAdmin == true && !member.isAdmin && !isTargetCreator)
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("group-member:${member.userId}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val fallback = painterResource(R.drawable.vista_default_avatar)
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = null,
                        placeholder = fallback,
                        error = fallback,
                        fallback = fallback,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(member.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            buildString {
                                append(
                                    when {
                                        isTargetCreator -> "سازنده"
                                        member.isAdmin -> "مدیر"
                                        else -> "عضو"
                                    },
                                )
                                if (isMe) append(" • شما")
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (canManage) {
                        TextButton(
                            onClick = { pendingMemberAction = member to !member.isAdmin },
                            enabled = !state.isGroupLoading,
                            modifier = Modifier.testTag("group-admin-action:${member.userId}"),
                        ) {
                            Text(if (member.isAdmin) "عزل" else "مدیر")
                        }
                        TextButton(
                            onClick = { pendingRemoval = member },
                            enabled = !state.isGroupLoading,
                            modifier = Modifier.testTag("group-remove-action:${member.userId}"),
                        ) {
                            Text("حذف", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (info?.isAdmin == true) {
                item(key = "add-title") {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (remainingSlots > 0) "افزودن عضو • $remainingSlots جای خالی" else "ظرفیت گروه تکمیل است",
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingSlots > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        )
                        if (remainingSlots > 0) {
                            TextButton(
                                onClick = {
                                    addMembersExpanded = !addMembersExpanded
                                    if (!addMembersExpanded) {
                                        selectedNewMemberIds = emptySet()
                                        onSearchUsers("")
                                    }
                                },
                            ) { Text(if (addMembersExpanded) "بستن" else "انتخاب") }
                        }
                    }
                }
            }
            if (info?.isAdmin == true && remainingSlots > 0 && addMembersExpanded) {
                item(key = "add-search") {
                    OutlinedTextField(
                        value = state.groupUserQuery,
                        onValueChange = onSearchUsers,
                        modifier = Modifier.fillMaxWidth().testTag("group-add-search"),
                        singleLine = true,
                        label = { Text("جستجو در کل کاربران") },
                        trailingIcon = {
                            if (state.isGroupUserSearching) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        },
                    )
                }
                state.groupUserSearchError?.let { error ->
                    item(key = "add-search-error") {
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
                if (addCandidates.isEmpty() && !state.isGroupUserSearching) {
                    item(key = "add-empty") {
                        Text(
                            "کاربر دیگری پیدا نشد",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(addCandidates.take(50), key = { "add:${it.id}" }) { candidate ->
                        val selected = candidate.id in selectedNewMemberIds
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !state.isGroupLoading) {
                                    selectedNewMemberIds = when {
                                        selected -> selectedNewMemberIds - candidate.id
                                        selectedNewMemberIds.size < remainingSlots -> selectedNewMemberIds + candidate.id
                                        else -> selectedNewMemberIds
                                    }
                                }
                                .padding(vertical = 8.dp)
                                .testTag("group-add-candidate:${candidate.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChatAvatarImage(
                                model = candidate.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(candidate.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                candidate.username.takeIf(String::isNotBlank)?.let {
                                    Text("@$it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Checkbox(checked = selected, onCheckedChange = null)
                        }
                    }
                }
                item(key = "add-submit") {
                    Button(
                        onClick = {
                            onAddMembers(selectedNewMemberIds.toList())
                            selectedNewMemberIds = emptySet()
                            onSearchUsers("")
                            addMembersExpanded = false
                        },
                        enabled = selectedNewMemberIds.isNotEmpty() && !state.isGroupLoading,
                        modifier = Modifier.fillMaxWidth().testTag("group-add-submit"),
                    ) {
                        Text("افزودن ${selectedNewMemberIds.size} عضو")
                    }
                }
            }

            item(key = "shared-media") {
                Spacer(Modifier.height(32.dp))

                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val tabs = listOf("رسانه", "فایل", "صدا")
                
                val mediaMessages = state.sharedMedia.filter { 
                    it.attachment?.mimeType?.startsWith("image/") == true || 
                    it.attachment?.mimeType?.startsWith("video/") == true 
                }
                val fileMessages = state.sharedMedia.filter { 
                    it.attachment?.mimeType?.startsWith("application/") == true ||
                    it.attachment?.mimeType?.startsWith("text/") == true
                }
                val audioMessages = state.sharedMedia.filter { 
                    it.attachment?.mimeType?.startsWith("audio/") == true 
                }

                val currentList = when (selectedTabIndex) {
                    0 -> mediaMessages
                    1 -> fileMessages
                    2 -> audioMessages
                    else -> emptyList()
                }

                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        val count = when(index) {
                            0 -> mediaMessages.size
                            1 -> fileMessages.size
                            2 -> audioMessages.size
                            else -> 0
                        }
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text("$title ($count)") }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    if (currentList.isEmpty()) {
                        Text(
                            text = "لیست ${tabs[selectedTabIndex]} خالی است",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(currentList, key = { "media:${it.clientId}" }) { message ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = message.attachment?.fileName ?: "فایل بدون نام",
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            item(key = "group-terminal") {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (isCreator) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("حذف گروه برای همه", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
                        Text("ترک گروه", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
    pendingMemberAction?.let { (member, makeAdmin) ->
        AlertDialog(
            onDismissRequest = { pendingMemberAction = null },
            title = { Text(if (makeAdmin) "مدیر کردن عضو" else "برداشتن دسترسی مدیر") },
            text = {
                Text(
                    if (makeAdmin) "${member.displayName} می‌تواند اعضا و اطلاعات گروه را مدیریت کند. ادامه می‌دهید؟"
                    else "دسترسی مدیریت ${member.displayName} برداشته شود؟",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetAdmin(member.userId, makeAdmin)
                    pendingMemberAction = null
                }) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMemberAction = null }) { Text("انصراف") }
            },
        )
    }
    pendingRemoval?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("حذف عضو") },
            text = { Text("آیا از حذف ${member.displayName} از گروه مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveMember(member.userId)
                    pendingRemoval = null
                }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("انصراف") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionsSheet(
    message: Message,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onInfo: () -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            message.previewText(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(listOf("❤️", "👍", "😂", "😮", "😢", "🔥")) { emoji ->
                Text(
                    emoji,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onReact(emoji) }
                        .padding(8.dp),
                    fontSize = 21.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        MessageActionRow(Icons.AutoMirrored.Filled.Reply, "پاسخ", onReply)
        if (message.content is MessageContent.Text && message.attachment == null) {
            MessageActionRow(Icons.Default.ContentCopy, "کپی متن", onCopy)
        }
        MessageActionRow(Icons.AutoMirrored.Filled.Forward, "فوروارد", onForward)
        MessageActionRow(Icons.Default.Info, "جزئیات پیام", onInfo)
        if (message.serverId != null) {
            MessageActionRow(Icons.Default.PushPin, if (message.isPinned) "برداشتن سنجاق" else "سنجاق کردن", onTogglePinned)
        }
        if (message.isMine && message.content is MessageContent.Text && message.attachment == null && message.deletedAtEpochMillis == null) {
            MessageActionRow(Icons.Default.Edit, "ویرایش", onEdit)
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        MessageActionRow(Icons.Default.Delete, "حذف", onDelete, destructive = true)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun MessageActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = color, fontSize = 15.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardMessageSheet(
    conversations: List<Conversation>,
    onDismiss: () -> Unit,
    onSelect: (Conversation) -> Unit,
) {
    val eligibleConversations = remember(conversations) {
        conversations.filter { it.type != ConversationType.SECRET }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "فوروارد به",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (eligibleConversations.isEmpty()) {
            Text(
                "گفتگوی دیگری در دسترس نیست",
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(eligibleConversations, key = { it.id }) { conversation ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(conversation) }.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChatAvatarImage(
                            model = conversation.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp).clip(CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DeleteMessageDialog(
    isMine: Boolean,
    onDismiss: () -> Unit,
    onDelete: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حذف پیام") },
        text = { Text("آیا از حذف این پیام مطمئن هستید؟") },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (isMine) TextButton(onClick = { onDelete(true) }) {
                    Text("حذف برای همه", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { onDelete(false) }) {
                    Text("حذف برای من", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun DeleteMessagesDialog(
    count: Int,
    allMine: Boolean,
    onDismiss: () -> Unit,
    onDelete: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حذف پیام‌ها") },
        text = { Text("آیا از حذف $count پیام انتخاب‌شده مطمئن هستید؟") },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (allMine) TextButton(onClick = { onDelete(true) }) {
                    Text("حذف برای همه", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { onDelete(false) }) {
                    Text("حذف برای من", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun MessageInfoDialog(message: Message, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("جزئیات پیام") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("زمان: ${formatFullDateTime(message.createdAtEpochMillis)}")
                Text("وضعیت: ${statusDescription(message.status)}")
                if (message.editedAtEpochMillis != null) Text("این پیام ویرایش شده است")
                if (message.isForwarded) Text("پیام فوروارد شده")
                message.attachment?.let { attachment ->
                    Text("فایل: ${attachment.fileName ?: attachment.kind.name}")
                    attachment.sizeBytes?.let { Text("حجم: ${formatBytes(it)}") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } },
    )
}

internal fun Message.previewText(): String = when (val value = content) {
    is MessageContent.Text -> {
        val parsed = SharedPostParser.tryParse(value.value)
        if (parsed != null) "پست از ${parsed.authorName}: ${parsed.content.ifBlank { "رسانه" }}"
        else value.value.ifBlank { attachment?.fileName ?: "پیوست" }
    }
    is MessageContent.Structured -> {
        val parsed = SharedPostParser.tryParse(value.payload)
        if (parsed != null) "پست از ${parsed.authorName}: ${parsed.content.ifBlank { "رسانه" }}"
        else value.payload
    }
    MessageContent.Deleted -> "این پیام حذف شده است"
    MessageContent.EncryptedUnavailable -> "پیام رمزگذاری‌شده"
}

private fun Message.replyDisplayName(conversationTitle: String): String =
    if (isMine) "خودتان" else conversationTitle

private fun formatFullDateTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.forLanguageTag("fa-IR")).format(Date(epochMillis))

private fun formatUserPresence(epochMillis: Long?): String {
    if (epochMillis == null) return "آخرین بازدید به تازگی"
    val now = System.currentTimeMillis()
    if (now - epochMillis in 0 until 60_000L) return "آنلاین"

    val locale = Locale.forLanguageTag("fa-IR")
    val time = SimpleDateFormat("HH:mm", locale).format(Date(epochMillis))
    val day = java.util.Calendar.getInstance().apply { timeInMillis = epochMillis }
    val today = java.util.Calendar.getInstance()
    val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    fun sameDate(a: java.util.Calendar, b: java.util.Calendar) =
        a.get(java.util.Calendar.ERA) == b.get(java.util.Calendar.ERA) &&
            a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)

    if (sameDate(day, today)) return "آخرین بازدید امروز ساعت $time"
    if (sameDate(day, yesterday)) return "آخرین بازدید دیروز ساعت $time"

    val persian = day.toJalaliDate()
    val currentPersian = today.toJalaliDate()
    val monthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )
    val date = "${persian.day} ${monthNames[persian.month - 1]}".toPersianDigits()
    val year = if (persian.year == currentPersian.year) "" else " ${persian.year}".toPersianDigits()
    return "آخرین بازدید $date$year ساعت $time"
}

private data class JalaliDate(val year: Int, val month: Int, val day: Int)

private fun java.util.Calendar.toJalaliDate(): JalaliDate {
    val gy = get(java.util.Calendar.YEAR)
    val gm = get(java.util.Calendar.MONTH) + 1
    val gd = get(java.util.Calendar.DAY_OF_MONTH)
    val monthDays = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val adjustedYear = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((adjustedYear + 3) / 4) -
        ((adjustedYear + 99) / 100) + ((adjustedYear + 399) / 400) + gd + monthDays[gm - 1]
    var jy = -1595 + 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + days / 31
        jd = 1 + days % 31
    } else {
        jm = 7 + (days - 186) / 30
        jd = 1 + (days - 186) % 30
    }
    return JalaliDate(jy, jm, jd)
}

internal fun String.toPersianDigits(): String = map { char ->
    if (char in '0'..'9') "۰۱۲۳۴۵۶۷۸۹"[char - '0'] else char
}.joinToString("")

@Composable
private fun AttachmentBubble(
    message: Message,
    attachment: ir.coffevista.vista_native.features.chat.domain.model.Attachment,
    downloadTask: DownloadTask? = null,
    onCancel: () -> Unit,
    onStartDownload: () -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onAttachmentClick: (ir.coffevista.vista_native.features.chat.domain.model.Attachment) -> Unit = {},
) {
    val foreground = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val isDownloaded = message.isMine ||
        !attachment.localUri.isNullOrBlank() ||
        (downloadTask?.state == DownloadState.COMPLETE && !downloadTask.localUri.isNullOrBlank())
    val isDownloading = downloadTask?.state == DownloadState.DOWNLOADING || downloadTask?.state == DownloadState.QUEUED
    val isFailed = downloadTask?.state == DownloadState.FAILED || downloadTask?.state == DownloadState.CANCELLED
    val effectiveLocalUri = downloadTask?.localUri?.takeIf { it.isNotBlank() } ?: attachment.localUri
    val mediaModel = effectiveLocalUri ?: attachment.remoteUrl

    val isActiveUpload = attachment.transferState == TransferState.UPLOADING ||
        attachment.transferState == TransferState.QUEUED

    Column {
        when (attachment.kind) {
            AttachmentKind.IMAGE, AttachmentKind.GIF -> {
                val imageBlurRadius by animateDpAsState(
                    targetValue = if (isDownloaded) 0.dp else 25.dp,
                    label = "telegram_image_blur",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            when {
                                isDownloaded -> onAttachmentClick(attachment.copy(localUri = effectiveLocalUri))
                                isDownloading -> downloadTask?.let { onCancelDownload(it.messageId) }
                                isFailed -> downloadTask?.let { onResumeDownload(it.messageId) } ?: onStartDownload()
                                else -> onStartDownload()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = mediaModel,
                        contentDescription = attachment.fileName ?: "تصویر پیوست",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (!isDownloaded) {
                                    Modifier
                                        .blur(imageBlurRadius)
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(Color.Black.copy(alpha = 0.40f))
                                        }
                                } else Modifier
                            ),
                    )

                    if (!isDownloaded) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                isDownloading -> {
                                    CircularProgressIndicator(
                                        progress = { downloadTask?.progress?.takeIf { it > 0f } ?: 0.1f },
                                        modifier = Modifier.size(46.dp),
                                        strokeWidth = 3.dp,
                                        color = Color.White,
                                        trackColor = Color.White.copy(alpha = 0.25f),
                                    )
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "لغو دانلود",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                isFailed -> {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "تلاش مجدد",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                else -> {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "دانلود",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            }
                        }

                        val badgeText = when {
                            isDownloading -> "${((downloadTask?.progress ?: 0f) * 100).toInt()}%"
                            attachment.sizeBytes != null && attachment.sizeBytes > 0 -> formatBytes(attachment.sizeBytes)
                            else -> null
                        }
                        if (badgeText != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = badgeText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
            AttachmentKind.VIDEO -> {
                val videoBlurRadius by animateDpAsState(
                    targetValue = if (isDownloaded) 0.dp else 25.dp,
                    label = "telegram_video_blur",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable {
                            when {
                                isDownloaded -> onAttachmentClick(attachment.copy(localUri = effectiveLocalUri))
                                isDownloading -> downloadTask?.let { onCancelDownload(it.messageId) }
                                isFailed -> downloadTask?.let { onResumeDownload(it.messageId) } ?: onStartDownload()
                                else -> onStartDownload()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!mediaModel.isNullOrBlank()) {
                        AsyncImage(
                            model = mediaModel,
                            contentDescription = attachment.fileName ?: "ویدیو",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (!isDownloaded) {
                                        Modifier
                                            .blur(videoBlurRadius)
                                            .drawWithContent {
                                                drawContent()
                                                drawRect(Color.Black.copy(alpha = 0.40f))
                                            }
                                    } else Modifier
                                ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            isDownloading -> {
                                CircularProgressIndicator(
                                    progress = { downloadTask?.progress?.takeIf { it > 0f } ?: 0.1f },
                                    modifier = Modifier.size(46.dp),
                                    strokeWidth = 3.dp,
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.25f),
                                )
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "لغو دانلود ویدیو",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            !isDownloaded -> {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "دانلود ویدیو",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "پخش ویدیو",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                    val durationStr = formatDuration(attachment.durationSeconds)
                    val sizeStr = formatBytes(attachment.sizeBytes)
                    val badge = when {
                        isDownloading -> "${((downloadTask?.progress ?: 0f) * 100).toInt()}%"
                        durationStr.isNotBlank() && sizeStr.isNotBlank() -> "$durationStr • $sizeStr"
                        durationStr.isNotBlank() -> durationStr
                        else -> sizeStr.takeIf { it.isNotBlank() }
                    }
                    if (!badge.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            AttachmentKind.VOICE, AttachmentKind.AUDIO -> {
                val audioUrl = effectiveLocalUri ?: attachment.remoteUrl
                if (!audioUrl.isNullOrBlank()) {
                    VoicePlayerBubble(
                        audioUrl = audioUrl,
                        durationSeconds = (attachment.durationSeconds ?: 1).coerceAtLeast(1),
                        isOutgoing = message.isMine,
                        isDownloaded = isDownloaded,
                        downloadTask = downloadTask,
                        waveformAmplitudes = attachment.parsedWaveform,
                        messageId = message.stableKey,
                        onStartDownload = onStartDownload,
                        onCancelDownload = onCancelDownload,
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "پخش صدا", tint = foreground, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(attachment.audioTitle ?: attachment.fileName ?: "پیام صوتی", color = foreground, maxLines = 1)
                            Text(formatDuration(attachment.durationSeconds), color = foreground.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
            AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> {
                val extension = (attachment.fileName ?: "").substringAfterLast('.', "").uppercase()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (message.isMine) Color.White.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .clickable {
                            when {
                                isDownloaded -> onAttachmentClick(attachment.copy(localUri = effectiveLocalUri))
                                isDownloading -> downloadTask?.let { onCancelDownload(it.messageId) }
                                isFailed -> downloadTask?.let { onResumeDownload(it.messageId) } ?: onStartDownload()
                                else -> onStartDownload()
                            }
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                when {
                                    isDownloading -> Color.Transparent
                                    isDownloaded -> if (message.isMine) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else -> if (message.isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            isDownloading -> {
                                CircularProgressIndicator(
                                    progress = { downloadTask?.progress?.takeIf { it > 0f } ?: 0.1f },
                                    modifier = Modifier.size(42.dp),
                                    strokeWidth = 2.5.dp,
                                    color = foreground,
                                    trackColor = foreground.copy(alpha = 0.2f),
                                )
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "لغو دانلود",
                                    tint = foreground,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            !isDownloaded -> {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "دانلود فایل",
                                    tint = if (message.isMine) Color.White else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = "فایل",
                                    tint = if (message.isMine) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName ?: "فایل پیوست",
                            color = foreground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        val subtitle = when {
                            isDownloading -> {
                                val pct = ((downloadTask?.progress ?: 0f) * 100).toInt()
                                val downloaded = formatBytes(downloadTask?.receivedBytes)
                                val total = formatBytes(attachment.sizeBytes)
                                if (downloaded.isNotBlank() && total.isNotBlank()) "$pct٪ • $downloaded / $total"
                                else "$pct٪ • در حال دانلود..."
                            }
                            !isDownloaded -> {
                                val size = formatBytes(attachment.sizeBytes)
                                if (size.isNotBlank()) "$size • برای دانلود لمس کنید" else "برای دانلود لمس کنید"
                            }
                            else -> {
                                val size = formatBytes(attachment.sizeBytes)
                                if (extension.isNotBlank() && size.isNotBlank()) "$size • $extension"
                                else size.ifBlank { extension.ifBlank { "فایل دانلود شده" } }
                            }
                        }
                        Text(
                            text = subtitle,
                            color = foreground.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
        if (isActiveUpload) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { attachment.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = foreground,
                    trackColor = foreground.copy(alpha = 0.25f),
                )
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "لغو ارسال فایل",
                        tint = foreground,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else if (attachment.transferState == TransferState.FAILED) {
            Text("ارسال فایل ناموفق بود؛ برای تلاش مجدد لمس کنید", color = Color(0xFFFFCDD2), fontSize = 11.sp)
        } else if (attachment.transferState == TransferState.CANCELLED) {
            Text("ارسال فایل لغو شد؛ برای تلاش مجدد لمس کنید", color = foreground.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun AttachmentViewer(
    attachment: ir.coffevista.vista_native.features.chat.domain.model.Attachment,
    onDismiss: () -> Unit,
) {
    val source = attachment.localUri?.takeIf(String::isNotBlank)
        ?: attachment.remoteUrl?.takeIf(String::isNotBlank)
        ?: return
    when (attachment.kind) {
        AttachmentKind.IMAGE, AttachmentKind.GIF -> Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = source,
                    contentDescription = attachment.fileName ?: "تصویر",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                ) { Text("بستن", color = Color.White) }
            }
        }
        AttachmentKind.VIDEO -> Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { context ->
                        android.widget.VideoView(context).apply {
                            setVideoURI(Uri.parse(source))
                            setMediaController(android.widget.MediaController(context).also { it.setAnchorView(this) })
                            setOnPreparedListener { it.isLooping = false; start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                ) { Text("بستن", color = Color.White) }
            }
        }
        AttachmentKind.VOICE, AttachmentKind.AUDIO -> AudioAttachmentPlayer(
            source = source,
            title = attachment.audioTitle ?: attachment.fileName ?: "پیام صوتی",
            onDismiss = onDismiss,
        )
        AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> DocumentAttachmentViewer(
            source = source,
            fileName = attachment.fileName ?: "فایل",
            mimeType = attachment.mimeType,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun AudioAttachmentPlayer(source: String, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var prepared by remember(source) { mutableStateOf(false) }
    var playing by remember(source) { mutableStateOf(false) }
    var failed by remember(source) { mutableStateOf(false) }
    var positionMillis by remember(source) { mutableIntStateOf(0) }
    var durationMillis by remember(source) { mutableIntStateOf(0) }
    var playbackSpeed by remember(source) { mutableFloatStateOf(1f) }
    val player = remember(source) { MediaPlayer() }
    DisposableEffect(player, source) {
        runCatching {
            player.setDataSource(context, Uri.parse(source))
            player.setOnPreparedListener {
                prepared = true
                durationMillis = it.duration.coerceAtLeast(0)
            }
            player.setOnCompletionListener {
                playing = false
                positionMillis = durationMillis
            }
            player.setOnErrorListener { _, _, _ -> failed = true; true }
            player.prepareAsync()
        }.onFailure { failed = true }
        onDispose { runCatching { player.release() } }
    }
    LaunchedEffect(player, playing) {
        while (playing) {
            positionMillis = runCatching { player.currentPosition }.getOrDefault(positionMillis)
            kotlinx.coroutines.delay(100)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
            Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            if (failed) Text("پخش فایل صوتی ممکن نیست", color = MaterialTheme.colorScheme.error)
            else {
                Slider(
                    value = positionMillis.toFloat().coerceIn(0f, durationMillis.coerceAtLeast(1).toFloat()),
                    onValueChange = { positionMillis = it.toInt() },
                    onValueChangeFinished = {
                        if (prepared) {
                            player.seekTo(positionMillis.coerceIn(0, durationMillis))
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    },
                    valueRange = 0f..durationMillis.coerceAtLeast(1).toFloat(),
                    enabled = prepared && durationMillis > 0,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "موقعیت پخش صدا" },
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatPlayerDuration(positionMillis), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPlayerDuration(durationMillis), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            if (playing) {
                                player.pause()
                                positionMillis = runCatching { player.currentPosition }.getOrDefault(positionMillis)
                            } else {
                                if (durationMillis > 0 && positionMillis >= durationMillis) {
                                    player.seekTo(0)
                                    positionMillis = 0
                                }
                                player.start()
                            }
                            playing = !playing
                        },
                        enabled = prepared,
                    ) {
                        if (!prepared) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else AnimatedContent(
                            targetState = playing,
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f))
                                    .togetherWith(fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.8f))
                            },
                            label = "audio-play-pause",
                        ) { isPlaying ->
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "توقف" else "پخش",
                            )
                        }
                    }
                    TextButton(
                        enabled = prepared,
                        onClick = {
                            playbackSpeed = when (playbackSpeed) {
                                1f -> 1.5f
                                1.5f -> 2f
                                else -> 1f
                            }
                            runCatching { player.playbackParams = PlaybackParams().setSpeed(playbackSpeed) }
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                    ) { Text("${playbackSpeed}×") }
                }
            }
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    }
}

internal fun formatPlayerDuration(millis: Int): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1_000)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun DocumentAttachmentViewer(
    source: String,
    fileName: String,
    mimeType: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var error by remember(source) { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Text(fileName, maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            Button(
                onClick = {
                    val parsed = Uri.parse(source)
                    val contentUri = if (parsed.scheme == "file" && !parsed.path.isNullOrBlank()) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.files",
                            File(requireNotNull(parsed.path)),
                        )
                    } else parsed
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, mimeType ?: "application/octet-stream")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = android.content.ClipData.newRawUri(fileName, contentUri)
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, "باز کردن فایل")) }
                        .onFailure { error = "برنامه‌ای برای نمایش این فایل پیدا نشد" }
                },
            ) { Text("باز کردن فایل") }
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    }
}

@Composable
private fun MessageStatusMark(status: MessageStatus) {
    val description = when (status) {
        MessageStatus.PENDING -> "در انتظار ارسال"
        MessageStatus.FAILED -> "ارسال ناموفق؛ برای تلاش مجدد ضربه بزنید"
        MessageStatus.SENT -> "ارسال شد"
        MessageStatus.DELIVERED -> "تحویل شد"
        MessageStatus.READ -> "خوانده شد"
    }
    Box(
        modifier = Modifier
            .width(15.dp)
            .height(12.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = status,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(200)).togetherWith(fadeOut(tween(200)))
            },
            label = "message-delivery-status",
        ) { currentStatus ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (currentStatus == MessageStatus.FAILED) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(12.dp),
                    )
                } else {
                    val markColor = if (currentStatus == MessageStatus.READ) {
                        Color(0xFF4FC3F7)
                    } else {
                        Color.White.copy(alpha = 0.7f)
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val side = size.height
                        val strokeWidth = side * 0.10f
                        val stroke = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        )
                        when (currentStatus) {
                            MessageStatus.PENDING -> {
                                val radius = side * 0.40f
                                val center = Offset(side * 0.50f, side * 0.50f)
                                drawCircle(markColor, radius, center, style = stroke)
                                drawLine(
                                    color = markColor,
                                    start = center,
                                    end = Offset(center.x + radius * 0.50f, center.y),
                                    strokeWidth = strokeWidth * 0.80f,
                                    cap = StrokeCap.Round,
                                )
                                drawLine(
                                    color = markColor,
                                    start = center,
                                    end = Offset(center.x, center.y - radius * 0.60f),
                                    strokeWidth = strokeWidth * 0.80f,
                                    cap = StrokeCap.Round,
                                )
                            }
                            MessageStatus.SENT -> {
                                val path = Path().apply {
                                    moveTo(side * 0.25f, side * 0.55f)
                                    lineTo(side * 0.45f, side * 0.75f)
                                    lineTo(side * 0.80f, side * 0.30f)
                                }
                                drawPath(path, markColor, style = stroke)
                            }
                            MessageStatus.DELIVERED,
                            MessageStatus.READ,
                            -> {
                                val back = Path().apply {
                                    moveTo(side * 0.10f, side * 0.56f)
                                    lineTo(side * 0.28f, side * 0.74f)
                                    lineTo(side * 0.56f, side * 0.36f)
                                }
                                val front = Path().apply {
                                    moveTo(side * 0.34f, side * 0.56f)
                                    lineTo(side * 0.52f, side * 0.74f)
                                    lineTo(side * 0.86f, side * 0.30f)
                                }
                                drawPath(back, markColor, style = stroke)
                                drawPath(front, markColor, style = stroke)
                            }
                            MessageStatus.FAILED -> Unit
                        }
                    }
                }
            }
        }
    }
}

private class LayoutCoordinatesHolder {
    var coordinates: androidx.compose.ui.layout.LayoutCoordinates? = null
}

private fun formatSecretAutoDeleteLabel(seconds: Int): String = when {
    seconds <= 0 -> "خاموش"
    seconds < 60 -> "$seconds ثانیه"
    seconds < 3600 -> "${seconds / 60} دقیقه"
    seconds < 86400 -> "${seconds / 3600} ساعت"
    else -> "${seconds / 86400} روز"
}

@Composable
private fun ChatComposerBar(
    conversationId: String?,
    title: String,
    replyingTo: Message?,
    editingMessage: Message?,
    onClearReply: () -> Unit,
    onClearEdit: () -> Unit,
    onSend: (String, () -> Unit) -> Unit,
    onSendReply: (String, Message, () -> Unit) -> Unit,
    onEdit: (Message, String) -> Unit,
    onComposerChanged: (String) -> Unit,
    composerController: ChatComposerController,
    focusRequester: FocusRequester,
    onAttach: () -> Unit,
    onEmoji: () -> Unit,
    isEmojiPanelOpen: Boolean,
    onFocusText: () -> Unit,
    onSendVoice: (ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft) -> Unit,
    hasRecordPermission: Boolean,
    onPermissionRequired: () -> Unit,
    attachmentsEnabled: Boolean,
) {
    var composerValue by remember(conversationId) {
        mutableStateOf(TextFieldValue("", TextRange.Zero))
    }

    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            val text = editingMessage.previewText()
            composerValue = TextFieldValue(text, TextRange(text.length))
        }
    }

    // composerValue is a local delegated state value. Keeping these trivial
    // predicates inside an unkeyed derivedStateOf captured the initial blank
    // value on the AndroidView-backed composer, so typed text reached state but
    // the trailing control remained the voice recorder forever.
    val isBlank = composerValue.text.isBlank()
    val isNotBlank = !isBlank

    Column(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = replyingTo != null,
            enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(180)),
        ) {
            replyingTo?.let { message ->
                val rawReplyTarget = message.replyToMessageId?.trim().orEmpty()
                val replySender = message.replyDisplayName(title)
                val isNoteReply = rawReplyTarget.startsWith("note:") || replySender.startsWith("یادداشت")
                val isStoryReply = rawReplyTarget.startsWith("story:") || replySender.startsWith("استوری")

                ComposerContextBanner(
                    label = if (isStoryReply) "پاسخ به استوری" else if (isNoteReply) "پاسخ به یادداشت" else "پاسخ به $replySender",
                    preview = message.previewText(),
                    badgeIcon = if (isStoryReply) Icons.Outlined.AutoStories else if (isNoteReply) Icons.Outlined.EditNote else null,
                    badgeText = if (isStoryReply) "استوری" else if (isNoteReply) "یادداشت" else null,
                    onDismiss = onClearReply,
                )
            }
        }
        AnimatedVisibility(
            visible = editingMessage != null,
            enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(180)),
        ) {
            editingMessage?.let { message ->
                ComposerContextBanner(
                    label = "ویرایش پیام",
                    preview = message.previewText(),
                    onDismiss = {
                        onClearEdit()
                        composerValue = TextFieldValue("", TextRange.Zero)
                    },
                )
            }
        }
        Composer(
            value = composerValue,
            isBlank = isBlank,
            isNotBlank = isNotBlank,
            controller = composerController,
            onValueChange = { newValue ->
                val snappedSelection = EmojiCursorHelper.snapSelection(newValue.text, newValue.selection)
                val safeValue = if (snappedSelection != newValue.selection) {
                    newValue.copy(selection = snappedSelection)
                } else {
                    newValue
                }
                composerValue = safeValue
                onComposerChanged(safeValue.text)
            },
            onSend = {
                val currentText = composerValue.text
                when (val editing = editingMessage) {
                    null -> when (val reply = replyingTo) {
                        null -> onSend(currentText) {
                            composerValue = TextFieldValue("", TextRange.Zero)
                            composerController.openKeyboard()
                        }
                        else -> onSendReply(currentText, reply) {
                            composerValue = TextFieldValue("", TextRange.Zero)
                            onClearReply()
                            composerController.openKeyboard()
                        }
                    }
                    else -> if (currentText.isNotBlank()) {
                        onEdit(editing, currentText)
                        composerValue = TextFieldValue("", TextRange.Zero)
                        onClearEdit()
                        composerController.openKeyboard()
                    }
                }
            },
            focusRequester = focusRequester,
            onAttach = onAttach,
            onEmoji = onEmoji,
            isEmojiPanelOpen = isEmojiPanelOpen,
            onFocusText = onFocusText,
            onSendVoice = onSendVoice,
            hasRecordPermission = hasRecordPermission,
            onPermissionRequired = onPermissionRequired,
            attachmentsEnabled = attachmentsEnabled,
        )
    }
}

@Composable
private fun Composer(
    value: TextFieldValue,
    isBlank: Boolean,
    isNotBlank: Boolean,
    controller: ChatComposerController,
    onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onEmoji: () -> Unit,
    isEmojiPanelOpen: Boolean = false,
    onFocusText: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    onSendVoice: (ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft) -> Unit,
    hasRecordPermission: Boolean,
    onPermissionRequired: () -> Unit,
    attachmentsEnabled: Boolean,
) {
    val attachmentRotation = remember { Animatable(0f) }
    val composerScope = rememberCoroutineScope()
    var isVoiceActive by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chat-composer-surface")
                .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (isVoiceActive) 0f else 1f },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f)
                            else Color.White.copy(alpha = 0.94f),
                        )
                        .border(
                            0.5.dp,
                            if (isSystemInDarkTheme()) Color(0x20FFFFFF) else Color(0x15000000),
                            RoundedCornerShape(24.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(
                        visible = isBlank && attachmentsEnabled,
                        enter = scaleIn(tween(200, easing = FastOutSlowInEasing)) + fadeIn(tween(160)),
                        exit = scaleOut(tween(180, easing = FastOutSlowInEasing)) + fadeOut(tween(140)),
                    ) {
                        IconButton(
                            onClick = {
                                composerScope.launch {
                                    attachmentRotation.snapTo(0f)
                                    attachmentRotation.animateTo(15f, tween(110, easing = FastOutSlowInEasing))
                                    attachmentRotation.animateTo(0f, tween(110, easing = FastOutSlowInEasing))
                                }
                                onAttach()
                            },
                            modifier = Modifier
                                .focusProperties { canFocus = false }
                                .size(34.dp)
                                .graphicsLayer { rotationZ = attachmentRotation.value },
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "پیوست فایل", modifier = Modifier.size(22.dp))
                        }
                    }
                    IconButton(
                        onClick = {
                            if (isEmojiPanelOpen) {
                                focusRequester.requestFocus()
                            }
                            onEmoji()
                        },
                        modifier = Modifier
                            .focusProperties { canFocus = false }
                            .size(40.dp)
                            .zIndex(1f),
                    ) {
                        Icon(
                            if (isEmojiPanelOpen) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                            contentDescription = if (isEmojiPanelOpen) "صفحه‌کلید" else "شکلک‌ها",
                            modifier = Modifier.size(23.dp),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            ChatComposerField(
                                value = value,
                                onValueChange = onValueChange,
                                onFocusText = onFocusText,
                                controller = controller,
                                isEmojiPanelOpen = isEmojiPanelOpen,
                                focusRequester = focusRequester,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                hint = "پیام...",
                                maxLines = 6,
                            )
                        }
                    }
                }

                if (isNotBlank) {
                    IconButton(
                        onClick = {
                            onSend()
                            controller.openKeyboard()
                        },
                        modifier = Modifier
                            .focusProperties { canFocus = false }
                            .size(48.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep)))
                            .semantics { contentDescription = "ارسال پیام" },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            if (isBlank) {
                VoiceRecorderDock(
                    modifier = Modifier.fillMaxWidth(),
                    onSendVoice = onSendVoice,
                    onPermissionRequired = onPermissionRequired,
                    hasRecordPermission = hasRecordPermission,
                    onActiveStateChange = { isVoiceActive = it },
                )
            }
        }
    }
}


@Composable
private fun FloatingMessageDateOverlay(
    visible: Boolean,
    epochMillis: Long?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && epochMillis != null,
        modifier = modifier.testTag("chat-floating-date"),
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.85f, animationSpec = tween(200)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.85f, animationSpec = tween(200)),
    ) {
        epochMillis?.let { FloatingMessageDateChip(it) }
    }
}

@Composable
private fun FloatingMessageDateChip(epochMillis: Long) {
    Text(
        text = localDayLabel(epochMillis),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics { contentDescription = "تاریخ گفتگو: " + localDayLabel(epochMillis) },
    )
}

@Composable
private fun DateDivider(epochMillis: Long) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            localDayLabel(epochMillis),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ConnectionBanner(
    text: String,
    onRetry: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(Color(0xFFFF9100))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onRetry != null && !isRefreshing) {
            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            ) {
                Text("تلاش مجدد", style = MaterialTheme.typography.labelLarge)
            }
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
        IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, contentDescription = "تلاش مجدد") }
    }
}

private fun statusDescription(status: MessageStatus): String = when (status) {
    MessageStatus.PENDING -> "در حال ارسال"
    MessageStatus.FAILED -> "ارسال ناموفق؛ برای تلاش مجدد لمس کنید"
    MessageStatus.SENT -> "ارسال شد"
    MessageStatus.DELIVERED -> "تحویل شد"
    MessageStatus.READ -> "خوانده شد"
}

/** Mirrors Flutter's per-message Bidi resolver: the first strong character
 * selects the paragraph direction; emoji/punctuation-only content stays LTR. */
internal fun resolveMessageTextDirection(text: String): TextDirection {
    var offset = 0
    while (offset < text.length) {
        val codePoint = Character.codePointAt(text, offset)
        when (Character.getDirectionality(codePoint)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return TextDirection.Ltr
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            -> return TextDirection.Rtl
        }
        offset += Character.charCount(codePoint)
    }
    return TextDirection.Ltr
}

internal fun shouldAnimateMessageEntry(
    message: Message,
    presentedMessageKeys: Set<String>,
    nowEpochMillis: Long,
): Boolean = presentedMessageKeys.isNotEmpty() &&
    message.stableKey !in presentedMessageKeys &&
    nowEpochMillis - message.createdAtEpochMillis in 0 until 60_000L &&
    (!message.isMine || message.status == MessageStatus.PENDING)

private fun formatTime(epochMillis: Long): String = runCatching {
    TIME_FORMATTER.get()?.format(Date(epochMillis)).orEmpty()
}.getOrDefault("")

internal fun bundledAvatarDrawableName(source: String?): String? {
    val normalized = source?.trim()?.lowercase(Locale.US) ?: return null
    return when {
        normalized.endsWith("/vistalogo-new.png") || normalized == "vistalogo-new.png" -> "vista_logo_mark"
        else -> null
    }
}

@Composable
internal fun ChatAvatarImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bundledName = bundledAvatarDrawableName(model)
    val bundledId = remember(bundledName, context.packageName) {
        bundledName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
    }
    val fallback = painterResource(R.drawable.vista_default_avatar)
    if (bundledId != 0) {
        Image(
            painter = painterResource(bundledId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            placeholder = fallback,
            error = fallback,
            fallback = fallback,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

/** Exact inbox timestamp policy used by Flutter's SwipeableConversationItem. */
internal fun formatConversationTime(
    epochMillis: Long,
    nowEpochMillis: Long = System.currentTimeMillis(),
): String {
    val differenceDays = (nowEpochMillis - epochMillis) / DAY_MILLIS
    return when {
        differenceDays > 6 -> {
            val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
            "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}"
        }
        differenceDays > 1 -> "$differenceDays روز"
        differenceDays == 1L -> "دیروز"
        else -> formatTime(epochMillis)
    }
}

private fun isSameLocalDay(firstEpochMillis: Long, secondEpochMillis: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = firstEpochMillis }
    val second = Calendar.getInstance().apply { timeInMillis = secondEpochMillis }
    return first.get(Calendar.ERA) == second.get(Calendar.ERA) &&
        first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

internal fun localDayLabel(
    epochMillis: Long,
    nowEpochMillis: Long = System.currentTimeMillis(),
): String {
    val messageDay = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        timeInMillis = nowEpochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val days = ((today.timeInMillis - messageDay.timeInMillis) / DAY_MILLIS).toInt()
    return when {
        days == 0 -> "امروز"
        days == 1 -> "دیروز"
        days in 2..6 -> when (messageDay.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            else -> "جمعه"
        }
        else -> {
            val jalali = messageDay.toJalaliDate()
            val todayJalali = today.toJalaliDate()
            val monthNames = arrayOf(
                "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
            )
            buildString {
                append(jalali.day)
                append(' ')
                append(monthNames[jalali.month - 1])
                if (jalali.year != todayJalali.year) {
                    append(' ')
                    append(jalali.year)
                }
            }.toPersianDigits()
        }
    }
}

private fun formatDuration(seconds: Int?): String {
    val safe = seconds?.coerceAtLeast(0) ?: 0
    return "%d:%02d".format(Locale.US, safe / 60, safe % 60)
}

private fun formatBytes(bytes: Long?): String = when {
    bytes == null || bytes <= 0 -> ""
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(Locale.US, bytes / 1024f)
    else -> "%.1f MB".format(Locale.US, bytes / (1024f * 1024f))
}

private fun android.content.ContentResolver.toChatAttachmentDraft(
    uri: android.net.Uri,
): ChatAttachmentDraft? {
    val mimeType = getType(uri)?.trim()?.takeIf(String::isNotEmpty) ?: "application/octet-stream"
    var fileName: String? = null
    var sizeBytes: Long? = null
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) fileName = cursor.getString(nameIndex)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
        }
    }
    val resolvedSize = sizeBytes?.takeIf { it > 0 } ?: runCatching {
        openAssetFileDescriptor(uri, "r")?.use { it.length }
    }.getOrNull()?.takeIf { it > 0 } ?: return null
    val resolvedName = fileName?.trim()?.takeIf(String::isNotEmpty) ?: "attachment"
    val lowerName = resolvedName.lowercase(Locale.US)
    val kind = when {
        mimeType.equals("image/gif", true) || lowerName.endsWith(".gif") -> AttachmentKind.GIF
        mimeType.startsWith("image/") -> AttachmentKind.IMAGE
        mimeType.startsWith("video/") -> AttachmentKind.VIDEO
        mimeType.startsWith("audio/") -> AttachmentKind.AUDIO
        else -> AttachmentKind.DOCUMENT
    }
    return ChatAttachmentDraft(
        uri = uri.toString(),
        fileName = resolvedName,
        mimeType = mimeType,
        sizeBytes = resolvedSize,
        kind = kind,
    )
}

private val TIME_FORMATTER = ThreadLocal.withInitial {
    // Flutter's current runtime uses Latin digits for message timestamps even
    // inside the RTL chat lane, so keep the same stable wire-to-UI rendering.
    SimpleDateFormat("HH:mm", Locale.US)
}

private val DAY_FORMATTER = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy/MM/dd", Locale.forLanguageTag("fa"))
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartnerProfileSheet(
    state: MessagesUiState,
    onDismiss: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onToggleBlock: (String, (Boolean) -> Unit) -> Unit,
    onReportUser: (String, ModerationReason, String?, (Boolean) -> Unit) -> Unit,
) {
    var blockConfirmVisible by remember { mutableStateOf(false) }
    var reportVisible by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf(ModerationReason.INAPPROPRIATE_CONTENT) }
    var reportDetails by rememberSaveable { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val postMessages = state.messages.filter { message ->
        message.content is MessageContent.Structured ||
            message.transportType.equals("sharedPost", true) ||
            message.transportType.equals("storyReply", true)
    }
    val mediaMessages = state.sharedMedia.filter {
        it.attachment?.kind in setOf(AttachmentKind.IMAGE, AttachmentKind.VIDEO)
    }
    val fileMessages = state.sharedMedia.filter { it.attachment?.kind == AttachmentKind.DOCUMENT }
    val linkMessages = state.messages.filter { message ->
        (message.content as? MessageContent.Text)?.value?.let { text ->
            text.contains("http://", ignoreCase = true) ||
                text.contains("https://", ignoreCase = true) ||
                text.contains("www.", ignoreCase = true)
        } == true
    }
    val audioMessages = state.sharedMedia.filter {
        it.attachment?.kind == AttachmentKind.AUDIO || it.attachment?.kind == AttachmentKind.VOICE
    }
    val gifMessages = state.sharedMedia.filter { it.attachment?.kind == AttachmentKind.GIF }
    val groupMessages = emptyList<Message>()

    val tabs = listOf("پست‌ها", "رسانه", "فایل‌ها", "لینک‌ها", "صدا", "GIF", "گروه‌ها")
    val currentList = when (selectedTabIndex) {
        0 -> postMessages
        1 -> mediaMessages
        2 -> fileMessages
        3 -> linkMessages
        4 -> audioMessages
        5 -> gifMessages
        else -> groupMessages
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChatAvatarImage(
                model = state.conversation?.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(CircleShape),
            )
            Spacer(Modifier.height(16.dp))
            Text(state.conversation?.title.orEmpty(), fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileActionButton("پیام", Icons.Default.ChatBubbleOutline, Modifier.weight(1f), onDismiss)
                ProfileActionButton(
                    if (state.conversation?.isMuted == true) "صدادار" else "بی‌صدا",
                    if (state.conversation?.isMuted == true) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    Modifier.weight(1f),
                    onToggleMute,
                )
                ProfileActionButton(
                    "پروفایل",
                    Icons.Default.PersonOutline,
                    Modifier.weight(1f),
                    { state.conversation?.peerId?.let(onOpenProfile) },
                )
                ProfileActionButton("جستجو", Icons.Default.Search, Modifier.weight(1f), onOpenSearch)
            }

            Spacer(Modifier.height(20.dp))

            ScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 4.dp) {
                tabs.forEachIndexed { index, title ->
                    val count = when(index) {
                        0 -> postMessages.size
                        1 -> mediaMessages.size
                        2 -> fileMessages.size
                        3 -> linkMessages.size
                        4 -> audioMessages.size
                        5 -> gifMessages.size
                        else -> 0
                    }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(if (index == 6) title else "$title ($count)") }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                if (currentList.isEmpty()) {
                    Text(
                        text = if (selectedTabIndex == 6) "گروه مشترکی یافت نشد" else "بدون ${tabs[selectedTabIndex]}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(currentList, key = { "media:${it.clientId}" }) { message ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                message.attachment?.takeIf { it.kind in setOf(AttachmentKind.IMAGE, AttachmentKind.GIF) }?.let { attachment ->
                                    AsyncImage(
                                        model = attachment.localUri ?: attachment.remoteUrl,
                                        contentDescription = attachment.fileName ?: "رسانه",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(
                                    text = message.attachment?.fileName ?: message.previewText(),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            state.moderationError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { blockConfirmVisible = true },
                    enabled = !state.isModerationLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.blockStatus?.isBlocked == true) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.blockStatus?.isBlocked == true) "رفع مسدودیت" else "مسدود کردن")
                }
                Button(
                    onClick = { reportVisible = true },
                    enabled = !state.isModerationLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                    modifier = Modifier.weight(1f),
                ) { Text("گزارش کاربر") }
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    if (blockConfirmVisible) {
        val isUnblock = state.blockStatus?.isBlocked == true
        AlertDialog(
            onDismissRequest = { blockConfirmVisible = false },
            title = { Text(if (isUnblock) "رفع مسدودیت" else "مسدود کردن") },
            text = {
                Text(
                    if (isUnblock) "دوباره می‌توانید با ${state.conversation?.title.orEmpty()} پیام رد و بدل کنید."
                    else "پس از مسدودسازی، پیام‌های یکدیگر را نمی‌بینید و گفتگوهای قبلی موقتاً در دسترس نخواهد بود.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.conversation?.peerId?.let { peerId ->
                            onToggleBlock(peerId) { success -> if (success) blockConfirmVisible = false }
                        }
                    }
                ) {
                    Text(if (isUnblock) "رفع مسدودیت" else "مسدود کردن", color = if (isUnblock) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { blockConfirmVisible = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (reportVisible) {
        AlertDialog(
            onDismissRequest = { reportVisible = false },
            title = { Text("گزارش کاربر") },
            text = {
                Column(
                    Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("دلیل گزارش", fontWeight = FontWeight.SemiBold)
                    ModerationReason.entries.forEach { reason ->
                        val selected = selectedReason == reason
                        TextButton(
                            onClick = { selectedReason = reason },
                            modifier = Modifier.fillMaxWidth().background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            ),
                        ) { Text(reason.persianLabel(), color = MaterialTheme.colorScheme.onSurface) }
                    }
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it.take(500) },
                        label = { Text("توضیحات اضافی (اختیاری)") },
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("گزارش به‌صورت محرمانه برای بررسی پشتیبانی ارسال می‌شود.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isModerationLoading,
                    onClick = {
                        state.conversation?.peerId?.let { peerId ->
                            onReportUser(peerId, selectedReason, reportDetails) { success ->
                                if (success) reportVisible = false
                            }
                        }
                    },
                ) { Text("ارسال گزارش", color = Color(0xFFF57C00)) }
            },
            dismissButton = { TextButton(onClick = { reportVisible = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun PartnerDetailsScreen(
    state: MessagesUiState,
    onDismiss: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onToggleBlock: (String, (Boolean) -> Unit) -> Unit,
    onReportUser: (String, ModerationReason, String?, (Boolean) -> Unit) -> Unit = { _, _, _, complete -> complete(false) },
    onDeleteConversation: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
    onAttachmentClick: (ir.coffevista.vista_native.features.chat.domain.model.Attachment) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val conversation = state.conversation
    val displayName = conversation?.title.orEmpty()
    val profile = state.partnerProfile
    val username = profile?.username?.removePrefix("@")?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: displayName.removePrefix("@").trim()
    var selectedTabIndex by rememberSaveable(conversation?.id) { mutableIntStateOf(0) }
    var blockConfirmVisible by rememberSaveable(conversation?.id) { mutableStateOf(false) }
    var partnerMenuExpanded by rememberSaveable(conversation?.id) { mutableStateOf(false) }
    var reportDialogVisible by rememberSaveable(conversation?.id) { mutableStateOf(false) }
    var selectedReportReason by remember(conversation?.id) { mutableStateOf(ModerationReason.SPAM) }
    var reportDetails by remember(conversation?.id) { mutableStateOf("") }
    var clearHistoryConfirmVisible by rememberSaveable(conversation?.id) { mutableStateOf(false) }
    val mediaMessages = state.sharedMedia.filter {
        it.attachment?.kind in setOf(AttachmentKind.IMAGE, AttachmentKind.VIDEO)
    }
    val fileMessages = state.sharedMedia.filter { it.attachment?.kind == AttachmentKind.DOCUMENT }
    val audioMessages = state.sharedMedia.filter {
        it.attachment?.kind == AttachmentKind.AUDIO || it.attachment?.kind == AttachmentKind.VOICE
    }
    val gifMessages = state.sharedMedia.filter { it.attachment?.kind == AttachmentKind.GIF }
    val linkMessages = state.messages.filter { message ->
        (message.content as? MessageContent.Text)?.value?.let { text ->
            text.contains("http://", ignoreCase = true) ||
                text.contains("https://", ignoreCase = true) ||
                text.contains("www.", ignoreCase = true)
        } == true
    }
    // Do not render labels for unsupported/fake datasets. Every tab below has a
    // concrete Room-backed source and a deterministic index-to-data mapping.
    val tabs = listOf("رسانه", "فایل‌ها", "لینک‌ها", "صدا", "GIF")

    BackHandler(onBack = onDismiss)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val heroAvatar = profile?.avatarUrl ?: conversation?.avatarUrl
                if (!heroAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = heroAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(25.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.40f),
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White.copy(alpha = 0.20f),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.66f),
                                ),
                            ),
                        ),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.30f), CircleShape),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت از اطلاعات کاربر",
                        tint = Color.White,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    IconButton(
                        onClick = { partnerMenuExpanded = true },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.30f), CircleShape),
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌های اطلاعات کاربر", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = partnerMenuExpanded,
                        onDismissRequest = { partnerMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("گزارش کاربر") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                partnerMenuExpanded = false
                                reportDialogVisible = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("مسدود کردن", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Block, tint = MaterialTheme.colorScheme.error, contentDescription = null) },
                            onClick = {
                                partnerMenuExpanded = false
                                blockConfirmVisible = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("پاک کردن تاریخچه گفتگو", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, tint = MaterialTheme.colorScheme.error, contentDescription = null) },
                            onClick = {
                                partnerMenuExpanded = false
                                clearHistoryConfirmVisible = true
                            },
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape),
                    ) {
                        val cleanAvatar = heroAvatar?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
                        if (!cleanAvatar.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = cleanAvatar,
                                contentDescription = "تصویر $displayName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFF6C5CE7),
                                                        Color(0xFF0984E3),
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(displayName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                                error = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFF6C5CE7),
                                                        Color(0xFF0984E3),
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(displayName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                                    }
                                },
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF6C5CE7),
                                                Color(0xFF0984E3),
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(displayName.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.presence?.let { presence ->
                            if (presence.isOnline) "آنلاین" else formatUserPresence(presence.lastOnlineAtEpochMillis)
                        } ?: "در حال بررسی...",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 14.sp,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PartnerDetailsAction(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "پیام",
                    modifier = Modifier.weight(1f),
                ) {
                    if (conversation?.type == ConversationType.SECRET && !conversation.peerId.isNullOrBlank()) {
                        onDismiss()
                        onOpenProfile(conversation.peerId)
                    } else {
                        onDismiss()
                    }
                }
                PartnerDetailsAction(
                    icon = if (conversation?.isMuted == true) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    label = if (conversation?.isMuted == true) "صدادار" else "بی‌صدا",
                    modifier = Modifier.weight(1f),
                ) {
                    onToggleMute()
                    scope.launch { snackbarHostState.showSnackbar("تنظیمات اعلان گفتگو به‌روزرسانی شد") }
                }
                PartnerDetailsAction(
                    icon = Icons.Default.PersonOutline,
                    label = "پروفایل",
                    modifier = Modifier.weight(1f),
                ) {
                    conversation?.peerId?.let { peerId ->
                        onDismiss()
                        onOpenProfile(peerId)
                    }
                }
                PartnerDetailsAction(
                    icon = Icons.Default.Search,
                    label = "جستجو",
                    modifier = Modifier.weight(1f),
                ) { onOpenSearch() }
            }

            if (state.isPartnerProfileLoading && profile == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                )
            } else if (username.isNotBlank() || !profile?.bio.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "@$username",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                    )
                    profile?.bio?.let { bio ->
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = bio,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    state.presence?.let { presence ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (presence.isOnline) "آنلاین" else formatUserPresence(presence.lastOnlineAtEpochMillis),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
            ) {
                ScrollableTabRow(selectedTabIndex = selectedTabIndex, edgePadding = 0.dp) {
                    tabs.forEachIndexed { index, label ->
                        Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(label) })
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                if (selectedTabIndex == 0 && mediaMessages.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(1.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        gridItems(mediaMessages, key = { it.stableKey }) { message ->
                            val attachment = message.attachment
                            Box(
                                Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { message.attachment?.let(onAttachmentClick) }
                            ) {
                                AsyncImage(
                                    model = attachment?.localUri ?: attachment?.remoteUrl,
                                    contentDescription = attachment?.fileName ?: "رسانه",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (attachment?.kind == AttachmentKind.VIDEO) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "ویدیو",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val selectedItems: List<Message> = when (selectedTabIndex) {
                        0 -> mediaMessages
                        1 -> fileMessages
                        2 -> linkMessages
                        3 -> audioMessages
                        4 -> gifMessages
                        else -> emptyList()
                    }
                    if (selectedItems.isNotEmpty() && selectedTabIndex != 0) {
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(selectedItems, key = { it.stableKey }) { message: Message ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedTabIndex == 2) {
                                                extractFirstUrl((message.content as? MessageContent.Text)?.value.orEmpty())?.let {
                                                    runCatching { uriHandler.openUri(it) }
                                                }
                                            } else {
                                                message.attachment?.let(onAttachmentClick)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        when (message.attachment?.kind) {
                                            AttachmentKind.AUDIO, AttachmentKind.VOICE -> Icons.Default.Mic
                                            AttachmentKind.GIF -> Icons.Default.EmojiEmotions
                                            else -> Icons.Default.AttachFile
                                        },
                                        contentDescription = null,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(message.attachment?.fileName ?: message.previewText(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(formatConversationTime(message.createdAtEpochMillis), maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        return@Box
                    }
                    val icon = when (selectedTabIndex) {
                        0 -> Icons.Default.AttachFile
                        1 -> Icons.Default.Description
                        2 -> Icons.Default.AttachFile
                        3 -> Icons.Default.Mic
                        else -> Icons.Default.EmojiEmotions
                    }
                    val emptyText = when (selectedTabIndex) {
                        0 -> "هیچ رسانه‌ای یافت نشد"
                        1 -> "هیچ فایلی یافت نشد"
                        2 -> "هیچ لینکی یافت نشد"
                        3 -> "هیچ صدایی یافت نشد"
                        else -> "هیچ گیفی یافت نشد"
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = emptyText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

    if (blockConfirmVisible) {
        val blocked = state.blockStatus?.isBlocked == true
        AlertDialog(
            onDismissRequest = { blockConfirmVisible = false },
            title = { Text(if (blocked) "رفع مسدودیت" else "مسدود کردن") },
            text = {
                Text(
                    if (blocked) "دوباره می‌توانید با $displayName پیام رد و بدل کنید."
                    else "پس از مسدودسازی، پیام‌های یکدیگر را نمی‌بینید.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversation?.peerId?.let { peerId ->
                            onToggleBlock(peerId) { success ->
                                if (success) {
                                    blockConfirmVisible = false
                                    scope.launch { snackbarHostState.showSnackbar(if (blocked) "کاربر از حالت مسدود خارج شد" else "کاربر مسدود شد") }
                                }
                            }
                        }
                    },
                ) { Text(if (blocked) "رفع مسدودیت" else "مسدود کردن", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { blockConfirmVisible = false }) { Text("انصراف") } },
        )
    }

    if (reportDialogVisible) {
        AlertDialog(
            onDismissRequest = { reportDialogVisible = false },
            title = { Text("گزارش $displayName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("دلیل گزارش را انتخاب کنید:", fontSize = 13.sp)
                    ModerationReason.values().forEach { reason ->
                        val selected = selectedReportReason == reason
                        TextButton(
                            onClick = { selectedReportReason = reason },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(8.dp),
                                ),
                        ) {
                            Text(reason.persianLabel(), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it.take(300) },
                        label = { Text("توضیحات (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversation?.peerId?.let { peerId ->
                            onReportUser(peerId, selectedReportReason, reportDetails.takeIf(String::isNotBlank)) { success ->
                                reportDialogVisible = false
                                if (success) {
                                    scope.launch { snackbarHostState.showSnackbar("گزارش شما ارسال شد") }
                                }
                            }
                        }
                    },
                ) { Text("ارسال گزارش", color = Color(0xFFF57C00)) }
            },
            dismissButton = { TextButton(onClick = { reportDialogVisible = false }) { Text("انصراف") } },
        )
    }

    if (clearHistoryConfirmVisible) {
        AlertDialog(
            onDismissRequest = { clearHistoryConfirmVisible = false },
            title = { Text("پاک کردن تاریخچه") },
            text = { Text("آیا از پاک کردن تمامی پیام‌های این گفتگو اطمینان دارید؟ این عمل غیرقابل بازگشت است.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversation?.id?.let { convId ->
                            onDeleteConversation(convId) { success ->
                                clearHistoryConfirmVisible = false
                                if (success) {
                                    onDismiss()
                                }
                            }
                        }
                    },
                ) { Text("پاک کردن", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { clearHistoryConfirmVisible = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun PartnerDetailsAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "partnerActionPressScale",
    )
    val foreground = when {
        destructive -> MaterialTheme.colorScheme.error
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = foreground, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
    }
}

private fun ModerationReason.persianLabel(): String = when (this) {
    ModerationReason.INAPPROPRIATE_CONTENT -> "محتوای نامناسب"
    ModerationReason.HARASSMENT -> "آزار و اذیت"
    ModerationReason.SPAM -> "اسپم"
    ModerationReason.IMPERSONATION -> "جعل هویت"
    ModerationReason.SCAM -> "کلاهبرداری"
    ModerationReason.HATE_SPEECH -> "سخنان نفرت‌انگیز"
    ModerationReason.VIOLENCE -> "خشونت"
    ModerationReason.OTHER -> "سایر موارد"
}

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

