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
import ir.coffevista.vista_native.features.chat.presentation.components.ChatAttachmentBottomSheet
import ir.coffevista.vista_native.features.chat.presentation.components.ChatLinkPreviewCard
import ir.coffevista.vista_native.features.chat.presentation.components.ChatTextBubbleLayout
import ir.coffevista.vista_native.features.chat.presentation.components.PinnedMessagesBar
import ir.coffevista.vista_native.features.chat.presentation.components.UnreadMessagesDivider
import ir.coffevista.vista_native.features.chat.presentation.components.extractFirstUrl
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import ir.coffevista.vista_native.features.chat.presentation.components.MessageContextMenu
import ir.coffevista.vista_native.features.chat.presentation.components.MessageContextCapabilities
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.features.chat.R as ChatR
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.core.designsystem.tokens.VistaSpacing
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.DownloadState
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import ir.coffevista.vista_native.features.chat.domain.model.GifItem
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
import ir.coffevista.vista_native.features.chat.presentation.conversations.ConversationsUiState
import ir.coffevista.vista_native.features.chat.presentation.conversations.ConversationsViewModel
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesUiState
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesViewModel
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
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConversationListScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onOpenConversation = onOpenConversation,
        onShowArchived = viewModel::showArchived,
        onNewMessage = onNewMessage,
        onSaveOwnNote = viewModel::saveOwnNote,
        onDeleteOwnNote = viewModel::deleteOwnNote,
        onReplyToNote = viewModel::replyToNote,
        onToggleConversationFlag = viewModel::toggleFlag,
        onDeleteConversation = viewModel::deleteConversation,
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
    onSaveOwnNote: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
    onDeleteOwnNote: ((Boolean) -> Unit) -> Unit = { complete -> complete(false) },
    onReplyToNote: (ProfileNote, Conversation, String, (Boolean) -> Unit) -> Unit = { _, _, _, complete -> complete(false) },
    onToggleConversationFlag: (String, String) -> Unit = { _, _ -> },
    onDeleteConversation: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    var noteEditorVisible by rememberSaveable { mutableStateOf(false) }
    var peerNoteReply by remember { mutableStateOf<Pair<ProfileNote, Conversation>?>(null) }
    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var selectedConversationIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var deletingConversations by remember { mutableStateOf(emptyList<Conversation>()) }
    BackHandler(enabled = selectedConversationIds.isNotEmpty()) {
        selectedConversationIds = emptyList()
    }
    val visibleConversations = if (searchQuery.isBlank()) {
        state.conversations
    } else {
        state.conversations.filter { conversation ->
            conversation.title.contains(searchQuery, ignoreCase = true) ||
                conversation.lastMessage.orEmpty().contains(searchQuery, ignoreCase = true)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
                        }) { Icon(Icons.Default.Archive, contentDescription = "بایگانی گفتگوهای انتخاب شده") }
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
                                    onNewMessage()
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
                visibleConversations.isEmpty() -> EmptyConversationState(searchQuery.isNotBlank())
                else -> LazyColumn(Modifier.fillMaxSize()) {
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
                    if (state.isOffline) item(key = "offline") { ConnectionBanner("نمایش گفتگوهای ذخیره‌شده") }
                    items(visibleConversations, key = { it.id }) { conversation ->
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
                        )
                    }
                    item(key = "pagination") {
                        if (state.isAppending) CircularProgressIndicator(Modifier.padding(VistaSpacing.Large).size(24.dp))
                        else if (state.hasMore) LaunchedEffect(state.conversations.size) { onLoadMore() }
                    }
                }
            }
            if (state.isRefreshing && state.conversations.isNotEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.TopCenter).padding(VistaSpacing.Small).size(24.dp))
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
}

@Composable
private fun ProfileNotesTray(
    notes: List<ProfileNote>,
    conversations: List<Conversation>,
    isLoading: Boolean,
    onOpenOwnNote: () -> Unit,
    onOpenPeerNote: (ProfileNote, Conversation) -> Unit,
) {
    val own = notes.firstOrNull { it.isMine && !it.isExpired() }
    val peerNotes = notes.asSequence()
        .filter { !it.isMine && !it.isExpired() }
        .mapNotNull { note ->
            conversations.firstOrNull { it.peerId?.trim() == note.userId.trim() }?.let { note to it }
        }
        .toList()
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
        items(peerNotes, key = { it.first.userId }) { (note, conversation) ->
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
) {
    val typing = conversation.typingUserIds.isNotEmpty()
    val density = LocalDensity.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val swipeOffset = remember { Animatable(0f) }
    val maxSwipePx = with(density) { 100.dp.toPx() }
    val revealThreshold = with(density) { 40.dp.toPx() }
    // Reveal actions on right-swipe (start) and left-swipe (end)
    val isRevealed = kotlin.math.abs(swipeOffset.value) > revealThreshold
    val revealProgress = (kotlin.math.abs(swipeOffset.value) / maxSwipePx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(conversation.id) {
                detectHorizontalDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        coroutineScope.launch {
                            if (isRevealed) {
                                // Snap to reveal position
                                val target = if (swipeOffset.value > 0) maxSwipePx * 0.7f else -maxSwipePx * 0.7f
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
                            val newOffset = (swipeOffset.value + dragAmount).coerceIn(-maxSwipePx, maxSwipePx)
                            swipeOffset.snapTo(newOffset)
                        }
                    },
                )
            },
    ) {
        // Left-reveal actions (swipe right = reveal left actions: pin, mute)
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(76.dp)
                .graphicsLayer { alpha = if (swipeOffset.value > 0) revealProgress else 0f },
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
                .align(Alignment.CenterEnd)
                .height(76.dp)
                .graphicsLayer { alpha = if (swipeOffset.value < 0) revealProgress else 0f },
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
                            Box(
                                Modifier
                                    .heightIn(min = 22.dp)
                                    .widthIn(min = 22.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (conversation.isMuted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        else Color(0xFF1E88E5)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
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
private fun EmptyConversationState(searchActive: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) { EmptyChatGlyph(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) }
        Spacer(Modifier.height(24.dp))
        Text(
            if (searchActive) "نتیجه‌ای یافت نشد" else "هیچ گفتگویی وجود ندارد",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (searchActive) "عبارت دیگری را امتحان کنید" else "با دکمه مداد پیام جدید شروع کنید",
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
    title: String,
    viewModel: MessagesViewModel,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onStartSecretChat: (Conversation) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        context.contentResolver.toChatAttachmentDraft(uri)?.let(viewModel::sendAttachment)
    }
    LaunchedEffect(conversationId) { viewModel.bind(conversationId) }
    MessageDetailScreen(
        state = state,
        initialMessageId = initialMessageId,
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
        onAddGroupMember = viewModel::addGroupMember,
        onRemoveGroupMember = viewModel::removeGroupMember,
        onSetGroupAdmin = viewModel::setGroupAdmin,
        onSetGroupInviteEnabled = viewModel::setGroupInviteEnabled,
        onRegenerateGroupInvite = viewModel::regenerateGroupInvite,
        onLeaveGroup = { complete -> viewModel.leaveGroup { left -> if (left) onBack(); complete(left) } },
        onDeleteGroup = { complete -> viewModel.deleteGroup { deleted -> if (deleted) onBack(); complete(deleted) } },
        onComposerChanged = viewModel::composerChanged,
        onAttach = { attachmentLauncher.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf", "text/*", "application/zip")) },
        onSendVoice = viewModel::sendAttachment,
        onAttachDraft = viewModel::sendAttachment,
        onSearch = viewModel::search,
        onToggleMute = viewModel::toggleMute,
        onToggleBlock = viewModel::toggleBlock,
        onReportUser = viewModel::reportUser,
        onStartDownload = viewModel::startDownload,
        onPauseDownload = viewModel::pauseDownload,
        onResumeDownload = viewModel::resumeDownload,
        onCancelDownload = viewModel::cancelDownload,
        onOpenProfile = onOpenProfile,
        onLoadPartnerProfile = viewModel::loadPartnerProfile,
        onStartSecretChat = { viewModel.startSecretChat(onStartSecretChat) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MessageDetailScreen(
    state: MessagesUiState,
    initialMessageId: String? = null,
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
    onAddGroupMember: (String) -> Unit = {},
    onRemoveGroupMember: (String) -> Unit = {},
    onSetGroupAdmin: (String, Boolean) -> Unit = { _, _ -> },
    onSetGroupInviteEnabled: (Boolean) -> Unit = {},
    onRegenerateGroupInvite: () -> Unit = {},
    onLeaveGroup: ((Boolean) -> Unit) -> Unit = { complete -> complete(false) },
    onDeleteGroup: ((Boolean) -> Unit) -> Unit = { complete -> complete(false) },
    onComposerChanged: (String) -> Unit = {},
    onAttach: () -> Unit = {},
    onSendVoice: (ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft) -> Unit = {},
    onAttachDraft: (ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft) -> Unit = {},
    onVoice: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onToggleMute: () -> Unit = {},
    onToggleBlock: (String, (Boolean) -> Unit) -> Unit = { _, complete -> complete(false) },
    onReportUser: (String, ModerationReason, String?, (Boolean) -> Unit) -> Unit = { _, _, _, complete -> complete(false) },
    onStartDownload: (Message) -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onLoadPartnerProfile: (String) -> Unit = {},
    onStartSecretChat: () -> Unit = {},
) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    val context = LocalContext.current
    val localView = LocalView.current
    val isSecretConversation = state.conversation?.type == ConversationType.SECRET
    DisposableEffect(localView, isSecretConversation) {
        val window = (localView.context as? android.app.Activity)?.window
        if (isSecretConversation) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (isSecretConversation) {
                window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
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
    var composer by remember(state.conversationId) { mutableStateOf("") }
    var composerValue by remember(state.conversationId) {
        mutableStateOf(TextFieldValue(composer, TextRange(composer.length)))
    }
    // Hoist blank check to derivedStateOf → AnimatedVisibility/AnimatedContent don't
    // rebuild on every keystroke, only on blank↔filled transition
    val isComposerBlank = composer.isBlank()
    val isComposerNotBlank = composer.isNotBlank()
    var emojiPanelVisible by remember { mutableStateOf(false) }
    val composerController = remember { ChatComposerController() }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var messageInteraction by remember(state.conversationId) {
        mutableStateOf<MessageInteractionState>(MessageInteractionState.Idle)
    }
    val selectedMessageKeys = messageInteraction.selectedMessageKeys()
    fun dismissMessageInteraction() {
        messageInteraction = MessageInteractionState.Idle
    }
    fun startSelection(messageKey: String) {
        // Selection owns the message surface. Keeping the emoji panel open
        // behind its toolbar gives Back the wrong priority and leaves two
        // competing interaction modes visible.
        emojiPanelVisible = false
        messageInteraction = MessageInteractionState.Selecting(listOf(messageKey))
    }
    fun openMessageContext(messageKey: String, bubbleBounds: Rect?) {
        // A context surface owns the message interaction. Close the composer
        // surfaces first so its anchor is never covered by a stale IME/panel.
        emojiPanelVisible = false
        focusManager.clearFocus(force = true)
        composerController.closeKeyboard()
        messageInteraction = MessageInteractionState.Context(messageKey, bubbleBounds)
    }
    fun toggleMessageSelection(messageKey: String) {
        messageInteraction = messageInteraction.toggleSelection(messageKey)
    }
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
    val defaultKeyboardHeight = remember(configuration.screenHeightDp, isLandscape) {
        if (isLandscape) {
            (configuration.screenHeightDp.dp * 0.5f).coerceIn(160.dp, 240.dp)
        } else {
            // This is only the cold-start fallback (when the user opens emoji
            // before focusing the composer).  Once the IME has appeared, the
            // panel below is always sized from its actual inset instead.
            300.dp
        }
    }
    val density = LocalDensity.current
    val composerHostView = LocalView.current
    var visibleKeyboardHeightPx by remember(composerHostView) { mutableIntStateOf(0) }
    var visibleKeyboardPanelHeightPx by remember(composerHostView) { mutableIntStateOf(0) }
    DisposableEffect(composerHostView) {
        val root = composerHostView.rootView
        val visibleFrame = AndroidRect()
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            root.getWindowVisibleDisplayFrame(visibleFrame)
            val coveredHeight = (root.height - visibleFrame.bottom).coerceAtLeast(0)
            // With adjustResize, most of the IME is removed from root.height
            // before visible-frame measurement. The physical root delta keeps
            // the emoji replacement equal to the real keyboard footprint.
            val resizedRootHeight = (
                root.resources.displayMetrics.heightPixels - root.height
            ).coerceAtLeast(0)
            val keyboardFootprint = maxOf(coveredHeight, resizedRootHeight)
            // Ignore status/navigation-only changes. A keyboard surface is
            // materially taller than 160dp on supported phone layouts.
            if (keyboardFootprint >= with(density) { 160.dp.roundToPx() }) {
                visibleKeyboardHeightPx = keyboardFootprint
                // The visible frame already represents the exact footprint
                // that the IME takes from this window.  Adding the navigation
                // bar again makes the custom emoji panel taller than a real
                // keyboard by one gesture-navigation inset (32–36 px here).
                visibleKeyboardPanelHeightPx = keyboardFootprint
            }
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            root.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    // Compose's insets can be consumed on Android 13 with adjustNothing.  The
    // visible display frame above remains tied to the IME's actual footprint.
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density)
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    val navBottomDp = with(density) { navBottomPx.toDp() }
    val visibleKeyboardHeightDp = with(density) { visibleKeyboardHeightPx.toDp() }
    // Use the same window footprint measured while the IME was visible.  The
    // value must not add system bars: they are already accounted for by the
    // window's visible display frame.
    val panelReservationDp = with(density) {
        (visibleKeyboardPanelHeightPx.takeIf { it > 0 } ?: visibleKeyboardHeightPx).toDp()
    }
    val isKeyboardOpen = imeBottomPx > navBottomPx + with(density) { 20.dp.roundToPx() }
    // Emoji is a keyboard replacement, not an extra bottom sheet. Cap its
    // reservation to the active viewport and reuse the same value while the
    // IME is returning so the composer cannot jump between the two surfaces.
    // `screenHeightDp` follows the resized app window while the IME is present.
    // Using it here caps emoji to that shrunken viewport and moves the composer
    // between keyboard and emoji. DisplayMetrics keeps the physical screen bound.
    val physicalScreenHeight = with(density) {
        composerHostView.resources.displayMetrics.heightPixels.toDp()
    }
    val responsivePanelLimit = (physicalScreenHeight * if (isLandscape) 0.58f else 0.48f)
        .coerceIn(180.dp, 420.dp)
    val responsivePanelFallback = defaultKeyboardHeight.coerceIn(180.dp, responsivePanelLimit)
    // Some adjustResize implementations only expose a navigation-sized inset.
    // Keep the custom panel large enough to replace a phone keyboard even when
    // that incomplete value is all the platform reports.
    val emojiPanelMinimum = (physicalScreenHeight * if (isLandscape) 0.42f else 0.37f)
        .coerceIn(180.dp, responsivePanelLimit)
    val responsivePanelReservation = when {
        panelReservationDp >= 160.dp -> panelReservationDp.coerceIn(180.dp, responsivePanelLimit)
        isKeyboardOpen && imeBottomDp >= 160.dp -> imeBottomDp.coerceIn(180.dp, responsivePanelLimit)
        else -> responsivePanelFallback
    }
    // This is geometry, not user state. Persisting it restores a stale IME
    // height after process recreation or a density/orientation change.
    var composerPanelHeightDp by remember(configuration.screenHeightDp, isLandscape) {
        mutableStateOf(defaultKeyboardHeight.value)
    }
    var isSwitchingToKeyboard by remember { mutableStateOf(false) }
    LaunchedEffect(isSwitchingToKeyboard) {
        if (isSwitchingToKeyboard) {
            // Run only after the emoji surface has left composition.  A fixed
            // delay races that commit on slower frames and can leave the
            // keyboard button visibly inert on alternating taps.
            composerController.openKeyboard()
        }
    }
    LaunchedEffect(
        visibleKeyboardPanelHeightPx,
        visibleKeyboardHeightPx,
        imeBottomPx,
        navBottomPx,
        isKeyboardOpen,
        responsivePanelReservation,
    ) {
        // On Android 13 with `adjustNothing`, Compose can transiently report a
        // small navigation-like IME inset (~100dp) even though the keyboard is
        // visibly full height.  Never let that partial value replace the
        // portrait fallback; genuine full IME surfaces are substantially taller.
        if (visibleKeyboardHeightDp.value >= 160f ||
            (isKeyboardOpen && imeBottomDp.value >= 160f)
        ) {
            composerPanelHeightDp = responsivePanelReservation.value
        }
        if (isKeyboardOpen && isSwitchingToKeyboard) {
            isSwitchingToKeyboard = false
        }
    }
    val isPanelActive = emojiPanelVisible || isSwitchingToKeyboard
    BackHandler(enabled = emojiPanelVisible) {
        emojiPanelVisible = false
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
    var initialMessageHandled by rememberSaveable(state.conversationId, initialMessageId) {
        mutableStateOf(false)
    }
    LaunchedEffect(initialMessageId, state.messages) {
        if (initialMessageHandled || initialMessageId.isNullOrBlank()) return@LaunchedEffect
        val targetIndex = state.messages.indexOfFirst { message ->
            message.serverId == initialMessageId || message.clientId == initialMessageId
        }
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
    var newestMessageKey by remember(state.conversationId) { mutableStateOf<String?>(null) }
    fun openPartnerDetails() {
        state.conversation?.peerId?.takeIf(String::isNotBlank)?.let(onLoadPartnerProfile)
        partnerProfileVisible = true
    }
    LaunchedEffect(state.searchQuery, state.searchResults.size) {
        searchResultIndex = 0
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) Color(0xFF101419) else Color(0xFFDFE5E9)),
    ) {
        Image(
            painter = painterResource(
                if (isSystemInDarkTheme()) ChatR.drawable.vista_custom_bg_dark
                else ChatR.drawable.vista_custom_bg,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (isSystemInDarkTheme()) 0.8f else 0.9f,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            if (selectedMessageKeys.isNotEmpty()) {
                val selectedMessages = state.messages.filter { it.stableKey in selectedMessageKeys }
                val secret = state.conversation?.type == ConversationType.SECRET
                TopAppBar(
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
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
                // A fixed 56dp app bar clips the presence line at large font
                // scales. Keep the compact default while reserving two lines
                // when the user has requested an accessibility text size.
                val conversationTopBarHeight = if (density.fontScale > 1.3f) 72.dp else 56.dp
                TopAppBar(
                    modifier = Modifier.height(conversationTopBarHeight),
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
                                Text(
                                    title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (isSecretConversation) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF43A047),
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            "گفتگوی محرمانه • رمزگذاری سراسری فعال",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Color(0xFF43A047),
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                                val subtitle = when {
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
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", modifier = Modifier.size(24.dp))
                        }
                    },
                    actions = {
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
                                    if (state.conversation?.type == ConversationType.PRIVATE) {
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
                        val index = state.messages.indexOfFirst { message ->
                            message.serverId != null && message.serverId == pinned.serverId ||
                                message.clientId == pinned.clientId
                        }
                        if (index >= 0) coroutineScope.launch { listState.animateScrollToItem(index) }
                    },
                    onUnpinClick = { pinned ->
                        onTogglePinned(pinned)
                    },
                )
            }

            if (state.connectionState == RealtimeConnectionState.RECONNECTING ||
                state.connectionState == RealtimeConnectionState.DISCONNECTED
            ) {
                ConnectionBanner("در حال اتصال مجدد…")
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                val displayedMessages = if (searchVisible && state.searchResults.isNotEmpty()) {
                    listOf(state.searchResults[searchResultIndex.coerceIn(0, state.searchResults.lastIndex)])
                } else {
                    state.messages
                }
                when {
                    state.isInitialLoading && state.messages.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null && state.messages.isEmpty() -> ErrorState(state.error, onRefresh)
                    searchVisible && state.searchQuery.trim().length >= 2 && !state.isSearching && state.searchResults.isEmpty() ->
                        Text("نتیجه‌ای یافت نشد", Modifier.align(Alignment.Center))
                    state.messages.isEmpty() -> Text("هنوز پیامی نیست", Modifier.align(Alignment.Center))
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("chat-message-list"),
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    ) {
                        itemsIndexed(
                            displayedMessages,
                            key = { _, message -> message.stableKey },
                            contentType = { _, message ->
                                message.attachment?.kind?.name ?: "text"
                            },
                        ) { index, message ->
                            val groupPosition = messageGroupPosition(
                                messages = displayedMessages,
                                index = index,
                            )
                            val animateEntry = shouldAnimateMessageEntry(
                                message = message,
                                presentedMessageKeys = presentedMessageKeys,
                                nowEpochMillis = messageEntrySnapshotTime,
                            )
                            SwipeToReplyLayout(
                                enabled = messageInteraction is MessageInteractionState.Idle &&
                                    message.content != MessageContent.Deleted,
                                bubbleOnRight = message.isMine,
                                onReply = {
                                    replyingTo = message
                                    editingMessage = null
                                }
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
                                    onAttachmentClick = { viewedAttachment = it },
                                    onJumpToRepliedMessage = {
                                        val targetIndex = state.messages.indexOfFirst { msg ->
                                            (msg.serverId != null && msg.serverId == message.replyToMessageId) ||
                                                (msg.clientId.isNotBlank() && msg.clientId == message.replyToMessageId) ||
                                                (msg.content is MessageContent.Text && (msg.content as MessageContent.Text).value == message.replyToContent)
                                        }
                                        if (targetIndex >= 0) {
                                            coroutineScope.launch { listState.animateScrollToItem(targetIndex) }
                                        }
                                    },
                                )
                            }
                            val older = if (searchVisible) null else state.messages.getOrNull(index + 1)
                            val isUnreadBoundary = !searchVisible &&
                                !message.isMine && message.status != MessageStatus.READ &&
                                (older == null || older.isMine || older.status == MessageStatus.READ)
                            if (isUnreadBoundary) {
                                UnreadMessagesDivider()
                            }
                            if (older != null && !isSameLocalDay(message.createdAtEpochMillis, older.createdAtEpochMillis)) {
                                DateDivider(message.createdAtEpochMillis)
                            }
                        }
                        if (!searchVisible) item(key = "older") {
                            if (state.isAppending) CircularProgressIndicator(Modifier.padding(VistaSpacing.Large).size(24.dp))
                            else if (state.hasMore) LaunchedEffect(state.messages.size) { onLoadOlder() }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    val showScrollToBottom = listState.firstVisibleItemIndex > 3
                    if (showScrollToBottom && !searchVisible) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp, bottom = 12.dp),
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
                            val unreadBelow = state.messages.take(listState.firstVisibleItemIndex).count { !it.isMine && it.status != MessageStatus.READ }
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
            AnimatedVisibility(
                visible = replyingTo != null,
                enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(180)),
            ) {
                replyingTo?.let { message ->
                    ComposerContextBanner(
                        label = "پاسخ به ${message.replyDisplayName(title)}",
                        preview = message.previewText(),
                        onDismiss = { replyingTo = null },
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
                            editingMessage = null
                            composer = ""
                            composerValue = TextFieldValue("", TextRange.Zero)
                        },
                    )
                }
            }
            Composer(
                value = composerValue,
                isBlank = isComposerBlank,
                isNotBlank = isComposerNotBlank,
                controller = composerController,
                onValueChange = { newValue ->
                    val snappedSelection = EmojiCursorHelper.snapSelection(newValue.text, newValue.selection)
                    val safeValue = if (snappedSelection != newValue.selection) {
                        newValue.copy(selection = snappedSelection)
                    } else {
                        newValue
                    }
                    composerValue = safeValue
                    composer = safeValue.text
                    onComposerChanged(safeValue.text)
                },
                onSend = {
                    when (val editing = editingMessage) {
                        null -> when (val reply = replyingTo) {
                            null -> onSend(composer) {
                                composer = ""
                                composerValue = TextFieldValue("", TextRange.Zero)
                            }
                            else -> onSendReply(composer, reply) {
                                composer = ""
                                composerValue = TextFieldValue("", TextRange.Zero)
                                replyingTo = null
                            }
                        }
                        else -> if (composer.isNotBlank()) {
                            onEdit(editing, composer)
                            composer = ""
                            composerValue = TextFieldValue("", TextRange.Zero)
                            editingMessage = null
                        }
                    }
                },
                focusRequester = focusRequester,
                onAttach = { if (!isSecretConversation) attachmentSheetVisible = true },
                onEmoji = {
                    if (emojiPanelVisible) {
                        isSwitchingToKeyboard = true
                        emojiPanelVisible = false
                    } else {
                        // Do not leave the BasicTextField focused beneath the
                        // panel: Gboard may otherwise immediately re-show and
                        // consume the same interaction as a text edit.
                        focusManager.clearFocus(force = true)
                        isSwitchingToKeyboard = false
                        emojiPanelVisible = true
                        composerController.closeKeyboard()
                    }
                },
                isEmojiPanelOpen = emojiPanelVisible,
                onFocusText = {
                    if (emojiPanelVisible) {
                        isSwitchingToKeyboard = true
                        emojiPanelVisible = false
                    }
                },
                onSendVoice = { draft -> if (!isSecretConversation) onSendVoice(draft) },
                hasRecordPermission = hasRecordAudioPermission,
                onPermissionRequired = { recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                attachmentsEnabled = !isSecretConversation,
            )
            if (attachmentSheetVisible) {
                ChatAttachmentBottomSheet(
                    onDismiss = { attachmentSheetVisible = false },
                    onMediaSelected = { uri, mimeType ->
                        val draft = ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft(
                            uri = uri.toString(),
                            fileName = "media_${System.currentTimeMillis()}.jpg",
                            mimeType = mimeType,
                            sizeBytes = 0L,
                            kind = if (mimeType.startsWith("video/")) ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind.VIDEO
                                   else ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind.IMAGE,
                        )
                        onAttachDraft(draft)
                    },
                    onFileSelected = { uri ->
                        val draft = ir.coffevista.vista_native.features.chat.domain.model.ChatAttachmentDraft(
                            uri = uri.toString(),
                            fileName = "file_${System.currentTimeMillis()}",
                            mimeType = "application/octet-stream",
                            sizeBytes = 0L,
                            kind = ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind.DOCUMENT,
                        )
                        onAttachDraft(draft)
                    }
                )
            }
            if (isPanelActive) {
                val panelHeight = if (emojiPanelVisible) {
                    composerPanelHeightDp.dp.coerceAtLeast(emojiPanelMinimum)
                } else {
                    composerPanelHeightDp.dp
                }
                Box(Modifier.height(panelHeight)) {
                    if (emojiPanelVisible) {
                        TelegramXMediaPanel(
                            modifier = Modifier.fillMaxSize(),
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
                }
            } else if (isKeyboardOpen) {
                // Keep the same reserved footprint that the emoji surface uses.
                // WindowInsets can change by the navigation-bar height while the
                // IME animates; using it directly here makes the text box jump.
                Spacer(Modifier.height(composerPanelHeightDp.dp))
            } else {
                Spacer(Modifier.height(navBottomDp))
            }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        if (partnerProfileVisible && state.conversation?.type != ConversationType.GROUP) {
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
                val text = message.previewText()
                composer = text
                composerValue = TextFieldValue(text, TextRange(text.length))
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
    if (groupSheetVisible && state.conversation?.type == ConversationType.GROUP) {
        GroupDetailsSheet(
            state = state,
            clipboard = clipboard,
            onDismiss = { groupSheetVisible = false },
            onRefresh = onRefreshGroup,
            onUpdateName = onUpdateGroupName,
            onAddMember = onAddGroupMember,
            onRemoveMember = onRemoveGroupMember,
            onSetAdmin = onSetGroupAdmin,
            onSetInviteEnabled = onSetGroupInviteEnabled,
            onRegenerateInvite = onRegenerateGroupInvite,
            onLeave = { terminalGroupAction = "leave" },
            onDelete = { terminalGroupAction = "delete" },
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
        AttachmentViewer(attachment = attachment, onDismiss = { viewedAttachment = null })
    }
}

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
    onAttachmentClick: (ir.coffevista.vista_native.features.chat.domain.model.Attachment) -> Unit,
    onJumpToRepliedMessage: () -> Unit = {},
) {
    val isDeleted = message.content == MessageContent.Deleted
    val text = when (val content = message.content) {
        is MessageContent.Text -> content.value
        is MessageContent.Structured -> content.payload
        MessageContent.Deleted -> "این پیام حذف شده است"
        MessageContent.EncryptedUnavailable -> "🔒 پیام رمزگذاری‌شده"
    }
    val description = buildString {
        append(if (message.isMine) "پیام شما" else "پیام دریافتی")
        append("، ")
        append(statusDescription(message.status))
    }
    val density = LocalDensity.current
    var bubbleBounds by remember(message.stableKey) { mutableStateOf<Rect?>(null) }
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
                            else -> onOpenContextMenu(bubbleBounds)
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
                            bubbleBounds = coordinates.boundsInWindow()
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
                            "فوروارد شده",
                            color = if (message.isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                        )
                    }
                }
                if (!isDeleted && !message.replyToContent.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onJumpToRepliedMessage)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(if (message.isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "پاسخ به",
                                color = if (message.isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                message.replyToContent,
                                maxLines = 1,
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
                    )
                    if (text.isNotBlank()) Spacer(Modifier.height(6.dp))
                }
                val messageTextDir = resolveMessageTextDirection(text)
                val isRtlMessage = messageTextDir == androidx.compose.ui.text.style.TextDirection.Rtl ||
                    messageTextDir == androidx.compose.ui.text.style.TextDirection.ContentOrRtl

                val metaFooter: @Composable () -> Unit = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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

                if (text.isNotBlank()) {
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                preview,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        TextButton(onClick = onDismiss) { Text("لغو") }
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
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetAdmin: (String, Boolean) -> Unit,
    onSetInviteEnabled: (Boolean) -> Unit,
    onRegenerateInvite: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    val info = state.groupInfo
    var editedName by remember(info?.id, info?.name) { mutableStateOf(info?.name.orEmpty()) }
    val existingMemberIds = state.groupMembers.mapTo(mutableSetOf()) { it.userId }
    val addCandidates = state.forwardTargets.filter { candidate ->
        candidate.type == ConversationType.PRIVATE &&
            !candidate.peerId.isNullOrBlank() && candidate.peerId !in existingMemberIds
    }
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
                            onValueChange = { editedName = it.take(100) },
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
            items(state.groupMembers, key = { it.userId }) { member ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        Text(if (member.isAdmin) "مدیر" else "عضو", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (info?.isAdmin == true) {
                        TextButton(onClick = { onSetAdmin(member.userId, !member.isAdmin) }, enabled = !state.isGroupLoading) {
                            Text(if (member.isAdmin) "عزل" else "مدیر")
                        }
                        TextButton(onClick = { onRemoveMember(member.userId) }, enabled = !state.isGroupLoading) {
                            Text("حذف", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (info?.isAdmin == true && addCandidates.isNotEmpty()) {
                item(key = "add-title") { Text("افزودن عضو", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                items(addCandidates.take(20), key = { "add:${it.id}" }) { candidate ->
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !state.isGroupLoading) {
                            candidate.peerId?.let(onAddMember)
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(candidate.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("+ افزودن", color = MaterialTheme.colorScheme.primary)
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
                TextButton(onClick = onLeave, modifier = Modifier.fillMaxWidth()) {
                    Text("ترک گروه", color = MaterialTheme.colorScheme.error)
                }
                if (info?.isAdmin == true) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("حذف گروه برای همه", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "فوروارد به",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (conversations.isEmpty()) {
            Text(
                "گفتگوی دیگری در دسترس نیست",
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                items(conversations, key = { it.id }) { conversation ->
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
    is MessageContent.Text -> value.value.ifBlank { attachment?.fileName ?: "پیوست" }
    is MessageContent.Structured -> value.payload
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

private fun String.toPersianDigits(): String = map { char ->
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
) {
    val foreground = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val isActive = attachment.transferState == TransferState.UPLOADING ||
        attachment.transferState == TransferState.QUEUED
    Column {
    when (attachment.kind) {
        AttachmentKind.IMAGE, AttachmentKind.GIF -> AsyncImage(
            model = attachment.localUri ?: attachment.remoteUrl,
            contentDescription = attachment.fileName ?: "تصویر پیوست",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 280.dp).clip(RoundedCornerShape(12.dp)),
        )
        AttachmentKind.VIDEO -> Box(
            Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.PlayArrow, contentDescription = "پخش ویدیو", tint = Color.White, modifier = Modifier.size(48.dp)) }
        AttachmentKind.VOICE, AttachmentKind.AUDIO -> {
            val audioUrl = attachment.localUri ?: attachment.remoteUrl
            if (!audioUrl.isNullOrBlank()) {
                VoicePlayerBubble(
                    audioUrl = audioUrl,
                    durationSeconds = (attachment.durationSeconds ?: 1).coerceAtLeast(1),
                    isOutgoing = message.isMine,
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
        AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = foreground, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(attachment.fileName ?: "فایل", color = foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatBytes(attachment.sizeBytes), color = foreground.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
    if (isActive) {
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
    val needsDownload = attachment.transferState == TransferState.COMPLETE &&
        attachment.localUri.isNullOrBlank() && !attachment.remoteUrl.isNullOrBlank()
    if (needsDownload) {
        Spacer(Modifier.height(6.dp))
        when (downloadTask?.state) {
            null -> TextButton(onClick = onStartDownload) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("دانلود", color = foreground, fontSize = 11.sp)
            }
            DownloadState.DOWNLOADING -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { downloadTask.progress.takeIf { it > 0f } ?: 0f },
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = foreground,
                    trackColor = foreground.copy(alpha = 0.2f),
                )
                Spacer(Modifier.width(8.dp))
                Text("${(downloadTask.progress * 100).toInt()}٪", color = foreground.copy(alpha = 0.75f), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onPauseDownload(downloadTask.messageId) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Pause, contentDescription = "توقف دانلود", tint = foreground, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onCancelDownload(downloadTask.messageId) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Cancel, contentDescription = "لغو دانلود", tint = foreground, modifier = Modifier.size(20.dp))
                }
            }
            DownloadState.QUEUED, DownloadState.PAUSED -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (downloadTask.state == DownloadState.QUEUED) "در صف دانلود" else "دانلود متوقف شده",
                    color = foreground.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onResumeDownload(downloadTask.messageId) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "ادامه دانلود", tint = foreground, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onCancelDownload(downloadTask.messageId) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Cancel, contentDescription = "لغو دانلود", tint = foreground, modifier = Modifier.size(20.dp))
                }
            }
            DownloadState.FAILED, DownloadState.CANCELLED -> TextButton(
                onClick = { onResumeDownload(downloadTask.messageId) },
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("تلاش مجدد دانلود", color = foreground, fontSize = 11.sp)
            }
            DownloadState.COMPLETE -> Text("آماده باز کردن", color = foreground.copy(alpha = 0.75f), fontSize = 11.sp)
        }
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
    AnimatedContent(
        targetState = status,
        transitionSpec = {
            (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f))
                .togetherWith(fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.8f))
        },
        label = "message-delivery-status",
    ) { currentStatus ->
        when (currentStatus) {
            MessageStatus.PENDING -> CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.dp, color = Color.White.copy(alpha = 0.7f))
            MessageStatus.FAILED -> Icon(Icons.Default.Refresh, "ارسال مجدد", tint = Color(0xFFE57373), modifier = Modifier.size(12.dp))
            MessageStatus.SENT -> Icon(Icons.Default.Done, "ارسال شد", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
            MessageStatus.DELIVERED -> Icon(Icons.Default.DoneAll, "تحویل شد", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
            MessageStatus.READ -> Icon(Icons.Default.DoneAll, "خوانده شد", tint = Color(0xFF4FC3F7), modifier = Modifier.size(13.dp))
        }
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
    var isVoiceHolding by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("chat-composer-surface")
                .padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!isVoiceHolding) {
                Row(
                    Modifier
                        .weight(1f)
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f)
                            else Color.White.copy(alpha = 0.94f),
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
                            modifier = Modifier.size(34.dp).graphicsLayer { rotationZ = attachmentRotation.value },
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "پیوست فایل", modifier = Modifier.size(22.dp))
                        }
                    }
                    IconButton(
                        onClick = onEmoji,
                        modifier = Modifier
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
            }

            if (attachmentsEnabled || isNotBlank) {
                AnimatedContent(
                    targetState = isNotBlank,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.6f))
                            .togetherWith(fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.7f))
                    },
                    label = "composer-send-voice",
                    modifier = if (isVoiceHolding) Modifier.weight(1f) else Modifier,
                ) { hasText ->
                    if (!hasText) {
                        VoiceRecorderDock(
                            modifier = if (isVoiceHolding) Modifier.fillMaxWidth() else Modifier,
                            onSendVoice = onSendVoice,
                            onPermissionRequired = onPermissionRequired,
                            hasRecordPermission = hasRecordPermission,
                            onHoldingStateChange = { isVoiceHolding = it },
                        )
                    } else {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep)))
                                .clickable(role = Role.Button, onClick = onSend)
                                .semantics { contentDescription = "ارسال پیام" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
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
private fun ConnectionBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(Color(0xFFFF9100))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "تلاش مجدد",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = Color.White,
            strokeWidth = 2.dp,
        )
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
private fun ChatAvatarImage(
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

private fun localDayLabel(epochMillis: Long): String {
    val messageDay = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val days = ((today.timeInMillis - messageDay.timeInMillis) / DAY_MILLIS).toInt()
    return when (days) {
        0 -> "امروز"
        1 -> "دیروز"
        else -> DAY_FORMATTER.get()?.format(Date(epochMillis)).orEmpty()
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

    val mediaMessages = state.sharedMedia.filter {
        it.attachment?.kind in setOf(AttachmentKind.IMAGE, AttachmentKind.VIDEO, AttachmentKind.GIF)
    }
    val fileMessages = state.sharedMedia.filter { it.attachment?.kind == AttachmentKind.DOCUMENT }
    val audioMessages = state.sharedMedia.filter {
        it.attachment?.kind == AttachmentKind.AUDIO || it.attachment?.kind == AttachmentKind.VOICE
    }
    val linkMessages = state.messages.filter { message ->
        (message.content as? MessageContent.Text)?.value?.let { text ->
            text.contains("http://", ignoreCase = true) ||
                text.contains("https://", ignoreCase = true) ||
                text.contains("www.", ignoreCase = true)
        } == true
    }
    // Kept in the same order as the Flutter chat-profile route.  The tabs are
    // intentionally stable so a selection does not jump while profile data is
    // refreshed in the background.
    val tabs = listOf("پست‌ها", "رسانه", "فایل‌ها", "لینک‌ها", "صدا", "GIF", "گروه‌ها")
    val currentList = when (selectedTabIndex) {
        0 -> mediaMessages
        1 -> fileMessages
        2 -> audioMessages
        else -> linkMessages
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

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    val count = when(index) {
                        0 -> mediaMessages.size
                        1 -> fileMessages.size
                        2 -> audioMessages.size
                        else -> linkMessages.size
                    }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text("$title ($count)") }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                if (currentList.isEmpty()) {
                    Text(
                        text = "بدون ${tabs[selectedTabIndex]}",
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
    val mediaMessages = state.sharedMedia.filter {
        it.attachment?.kind in setOf(AttachmentKind.IMAGE, AttachmentKind.VIDEO, AttachmentKind.GIF)
    }
    val tabs = listOf("پست‌ها", "رسانه", "فایل‌ها", "لینک‌ها", "صدا", "GIF", "گروه‌ها")

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
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
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
                        .padding(8.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت از اطلاعات کاربر",
                        tint = Color.White,
                    )
                }
                IconButton(
                    onClick = { scope.launch { snackbarHostState.showSnackbar("گزینه‌های بیشتر به‌زودی اضافه می‌شود") } },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.30f), CircleShape),
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "گزینه‌های اطلاعات کاربر", tint = Color.White)
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
                        if (!heroAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = heroAvatar,
                                contentDescription = "تصویر $displayName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(displayName.firstOrNull()?.uppercase() ?: "?", fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
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
                ) { onDismiss() }
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
                if (selectedTabIndex == 1 && mediaMessages.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(1.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        gridItems(mediaMessages, key = { it.stableKey }) { message ->
                            val attachment = message.attachment
                            Box(Modifier.aspectRatio(1f)) {
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
                    val icon = when (selectedTabIndex) {
                        0 -> Icons.Default.Description
                        1 -> Icons.Default.Description
                        2 -> Icons.Default.Description
                        3 -> Icons.Default.AttachFile
                        4 -> Icons.Default.Mic
                        5 -> Icons.Default.EmojiEmotions
                        else -> Icons.Default.Group
                    }
                    val emptyText = when (selectedTabIndex) {
                        0 -> "هیچ پست‌هایی یافت نشد"
                        1 -> "هیچ رسانه‌ای یافت نشد"
                        2 -> "هیچ فایلی یافت نشد"
                        3 -> "هیچ لینکی یافت نشد"
                        4 -> "هیچ صدایی یافت نشد"
                        5 -> "هیچ گیفی یافت نشد"
                        6 -> "هیچ گروه مشترکی یافت نشد"
                        else -> "هیچ موردی یافت نشد"
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
    val foreground = when {
        destructive -> MaterialTheme.colorScheme.error
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .clickable(enabled = enabled, onClick = onClick)
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
