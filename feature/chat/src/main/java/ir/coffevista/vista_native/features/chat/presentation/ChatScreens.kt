package ir.coffevista.vista_native.features.chat.presentation

import ir.coffevista.vista_native.features.chat.presentation.components.OwnNoteEditorSheet
import ir.coffevista.vista_native.features.chat.presentation.components.PeerNoteReplySheet
import ir.coffevista.vista_native.features.chat.presentation.components.ProfileNotesTray
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
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioKind
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioPlayer
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioPlayerSheet
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatAudioTrack
import ir.coffevista.vista_native.features.chat.presentation.audio.ChatMiniAudioPlayer
import ir.coffevista.vista_native.features.chat.presentation.audio.MusicPlayerBubble
import ir.coffevista.vista_native.core.designsystem.component.resolveMusicTitle
import ir.coffevista.vista_native.core.designsystem.component.VistaBadgeDefaults
import ir.coffevista.vista_native.core.designsystem.component.VistaMiniPlayerHost
import ir.coffevista.vista_native.core.designsystem.component.VistaViuStateIllustration
import ir.coffevista.vista_native.core.designsystem.component.VistaViuStateKind
import ir.coffevista.vista_native.core.designsystem.component.inferVistaViuStateKind
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassSource
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlass
import ir.coffevista.vista_native.core.designsystem.glass.vistaGlassHalo
import ir.coffevista.vista_native.core.designsystem.glass.rememberVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.LocalVistaGlassState
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassScrollSync
import ir.coffevista.vista_native.core.designsystem.glass.VistaGlassTokens
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import ir.coffevista.vista_native.core.designsystem.component.VistaNotificationBadge
import ir.coffevista.vista_native.core.designsystem.component.VistaVerificationBadge
import ir.coffevista.vista_native.features.chat.presentation.components.FullscreenMediaViewerDialog
import ir.coffevista.vista_native.features.chat.presentation.components.MediaViewerItem
import ir.coffevista.vista_native.features.chat.presentation.components.ChatAttachmentBottomSheet
import ir.coffevista.vista_native.features.chat.presentation.components.ChatAttachmentReviewSheet
import ir.coffevista.vista_native.features.chat.presentation.components.ChatLinkPreviewCard
import ir.coffevista.vista_native.features.chat.presentation.components.ChatTextBubbleLayout
import ir.coffevista.vista_native.features.chat.presentation.components.SingleEmojiDetector
import ir.coffevista.vista_native.features.chat.presentation.components.dragSelectMessages
import ir.coffevista.vista_native.features.chat.presentation.components.VistaAnimatedEmojiBubble
import ir.coffevista.vista_native.features.chat.presentation.components.PinnedMessagesBar
import ir.coffevista.vista_native.core.common.looksLikeJsonPayload
import ir.coffevista.vista_native.core.common.toSafeUserError
import ir.coffevista.vista_native.core.designsystem.navigation.LocalVistaInternalLinkNavigator
import ir.coffevista.vista_native.features.chat.domain.model.DuelInviteParser
import ir.coffevista.vista_native.features.chat.presentation.components.DuelInviteCard
import ir.coffevista.vista_native.features.chat.presentation.components.DuelPreviewLoadStatus
import ir.coffevista.vista_native.features.chat.presentation.components.SharedPostCard
import ir.coffevista.vista_native.features.chat.domain.model.SharedPostParser
import ir.coffevista.vista_native.features.chat.domain.model.StoryReplyParser
import ir.coffevista.vista_native.features.chat.domain.model.StructuredMessageFormatter
import ir.coffevista.vista_native.features.chat.presentation.messages.DuelPreviewUiState
import ir.coffevista.vista_native.features.chat.presentation.components.UnreadMessagesDivider
import ir.coffevista.vista_native.features.chat.presentation.components.extractFirstUrl
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material.icons.filled.AddComment
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import ir.coffevista.vista_native.core.designsystem.component.VistaEmojiText
import ir.coffevista.vista_native.core.designsystem.component.VistaEmoji
import ir.coffevista.vista_native.core.designsystem.component.rememberVistaHaptics
import ir.coffevista.vista_native.features.chat.domain.repository.ChatResult
import ir.coffevista.vista_native.features.chat.presentation.components.EmojiCursorHelper
import ir.coffevista.vista_native.features.chat.presentation.components.saveChatAttachmentToDevice
import ir.coffevista.vista_native.features.chat.presentation.components.ChatComposerField
import ir.coffevista.vista_native.features.chat.presentation.components.ChatComposerController
import ir.coffevista.vista_native.features.chat.presentation.components.ChatMediaPickerPanel
import ir.coffevista.vista_native.features.chat.presentation.components.KeyboardHeightStore
import ir.coffevista.vista_native.features.chat.presentation.components.MessageContextMenu
import ir.coffevista.vista_native.features.chat.presentation.components.MessageContextCapabilities
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import ir.coffevista.vista_native.features.chat.presentation.components.ActiveSendFlight
import ir.coffevista.vista_native.features.chat.presentation.components.PendingSendFlight
import ir.coffevista.vista_native.features.chat.presentation.components.TELEGRAM_MINIMAL_SEND_DURATION_MS
import ir.coffevista.vista_native.features.chat.presentation.components.TELEGRAM_SEND_DURATION_MS
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramSendBezierEasing
import ir.coffevista.vista_native.features.chat.presentation.components.TelegramSendFlightOverlay
import ir.coffevista.vista_native.features.chat.presentation.components.isEligibleSendFlightCandidate
import ir.coffevista.vista_native.features.chat.presentation.components.resolveSendFlightStartBounds
import ir.coffevista.vista_native.features.chat.presentation.components.resolveSendFlightTargetFallbackBounds
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.Dp
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
import ir.coffevista.vista_native.features.chat.data.remote.IranMediaProxy
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
import ir.coffevista.vista_native.features.chat.presentation.components.SecretChatEncryptionKeyDialog
import ir.coffevista.vista_native.features.chat.presentation.components.SecretChatAutoDeleteDialog
import ir.coffevista.vista_native.features.chat.presentation.components.SecretChatGreen
import ir.coffevista.vista_native.features.chat.presentation.components.SecretChatBadgeBg
import java.text.SimpleDateFormat
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ConversationListRoute(
    viewModel: ConversationsViewModel,
    onOpenConversation: (Conversation) -> Unit,
    onNewMessage: () -> Unit,
    onNewSecretChat: () -> Unit = onNewMessage,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
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
        onRejectAndBlockMessageRequest = viewModel::rejectAndBlockRequest,
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
    onRejectAndBlockMessageRequest: (String) -> Unit = {},
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
                conversation.inboxPreviewText().orEmpty().contains(searchQuery, ignoreCase = true)
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
    val conversationsGlassState = rememberVistaGlassState()
    val conversationsListState = rememberLazyListState()
    VistaGlassScrollSync(conversationsGlassState, conversationsListState)
    val isDarkConversations = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                modifier = Modifier.vistaGlass(
                    state = conversationsGlassState,
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    tint = if (isDarkConversations) Color(0xFF1A222C) else Color(0xFFFFFFFF),
                    isDark = isDarkConversations,
                    blurRadius = VistaGlassTokens.BarBlur,
                    tintAlphaOverride = if (isDarkConversations) 0.58f else 0.62f,
                    drawHighlight = true,
                    rimWidth = 0.dp,
                    shadowElevation = 3.dp,
                ),
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
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
            VistaMiniPlayerHost(glassState = conversationsGlassState)
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .vistaGlassSource(conversationsGlassState)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.isInitialLoading && state.conversations.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding)) { LoadingConversationList() }
                state.error != null && state.conversations.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding)) { ErrorState(state.error, onRefresh) }
                visibleConversations.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding)) {
                    EmptyConversationState(
                        searchActive = searchQuery.isNotBlank(),
                        archived = state.includeArchived,
                    )
                }
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = conversationsListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = padding.calculateTopPadding(),
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp,
                        ),
                    ) {
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
                                onRejectAndBlockRequest = { onRejectAndBlockMessageRequest(conversation.id) },
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
            CompositionLocalProvider(LocalVistaGlassState provides conversationsGlassState) {
                ir.coffevista.vista_native.core.designsystem.component.VistaFloatingActionButton(
                    onClick = onNewMessage,
                    contentDescription = "پیام جدید",
                    modifier = Modifier.align(AbsoluteAlignment.BottomRight),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddComment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
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
    onRejectAndBlockRequest: () -> Unit = {},
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
        val rowHaptics = rememberVistaHaptics()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset.value },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .combinedClickable(
                        role = Role.Button,
                        onClick = {
                            rowHaptics.tap()
                            if (kotlin.math.abs(swipeOffset.value) > 4f) {
                                // Dismiss swipe on tap
                                coroutineScope.launch { swipeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                            } else {
                                onClick()
                            }
                        },
                        onLongClick = {
                            rowHaptics.heavy()
                            onLongClick()
                        },
                    )
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
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            if (conversation.isMuted) {
                                MutedGlyph(MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(4.dp))
                            }
                            VistaEmojiText(
                                text = conversation.title.ifBlank { "کاربر" },
                                modifier = Modifier.weight(1f, fill = false),
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (conversation.type.name == "SECRET") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (conversation.type != ConversationType.GROUP) {
                                ChatUserVerificationBadge(
                                    isVerified = conversation.isVerified,
                                    verificationType = conversation.verificationType,
                                    role = conversation.role,
                                    size = 15.dp,
                                )
                            }
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
                        VistaEmojiText(
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
                    onRejectAndBlock = onRejectAndBlockRequest,
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
internal fun ChatUserVerificationBadge(
    isVerified: Boolean,
    verificationType: String? = null,
    role: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 15.dp,
    isDarkSurface: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
    spacerWidth: Dp = 4.dp,
) {
    if (!isVerified) return
    Spacer(Modifier.width(spacerWidth))
    VistaVerificationBadge(
        isVerified = true,
        verificationType = verificationType,
        role = role,
        size = size,
        isDarkSurface = isDarkSurface,
        modifier = modifier,
    )
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
    onRejectAndBlock: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onRejectAndBlock,
            enabled = !loading,
            modifier = Modifier.weight(1.1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("رد و بلاک", fontSize = 12.sp, maxLines = 1)
        }
        OutlinedButton(
            onClick = onReject,
            enabled = !loading,
            modifier = Modifier.weight(0.8f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        ) {
            Text("رد", fontSize = 12.sp, maxLines = 1)
        }
        Button(
            onClick = onAccept,
            enabled = !loading,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("تایید", fontSize = 12.sp, maxLines = 1)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        conversation.title.ifBlank { "گفتگو" },
                        modifier = Modifier.weight(1f, fill = false),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (conversation.type != ConversationType.GROUP) {
                        ChatUserVerificationBadge(
                            isVerified = conversation.isVerified,
                            verificationType = conversation.verificationType,
                            role = conversation.role,
                            size = 16.dp,
                        )
                    }
                }
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
private fun rememberDebouncedConnectionState(
    state: RealtimeConnectionState,
): RealtimeConnectionState {
    var displayed by remember { mutableStateOf(RealtimeConnectionState.CONNECTED) }
    LaunchedEffect(state) {
        when (state) {
            RealtimeConnectionState.CONNECTED,
            RealtimeConnectionState.PAUSED -> {
                displayed = RealtimeConnectionState.CONNECTED
            }
            RealtimeConnectionState.CONNECTING,
            RealtimeConnectionState.AUTHENTICATING,
            RealtimeConnectionState.RECONNECTING -> {
                delay(2_500L)
                displayed = state
            }
            RealtimeConnectionState.DISCONNECTED,
            RealtimeConnectionState.FAILED -> {
                delay(1_200L)
                displayed = state
            }
        }
    }
    return displayed
}

@Composable
private fun ConnectionStatusIndicator(state: RealtimeConnectionState) {
    val visibleState = rememberDebouncedConnectionState(state)
    val color = when (visibleState) {
        RealtimeConnectionState.CONNECTED -> Color(0xFF2E7D32)
        RealtimeConnectionState.CONNECTING,
        RealtimeConnectionState.AUTHENTICATING,
        RealtimeConnectionState.RECONNECTING -> Color(0xFFFF9800)
        else -> Color(0xFFD32F2F)
    }
    val connecting = visibleState == RealtimeConnectionState.CONNECTING ||
        visibleState == RealtimeConnectionState.AUTHENTICATING ||
        visibleState == RealtimeConnectionState.RECONNECTING
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
        VistaViuStateIllustration(
            kind = when {
                searchActive -> VistaViuStateKind.SearchEmpty
                archived -> VistaViuStateKind.Empty
                else -> VistaViuStateKind.EmptyChat
            },
            size = 156.dp,
        )
        Spacer(Modifier.height(20.dp))
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
                else -> "با دکمه پیام جدید گفتگو را شروع کنید"
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
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(conversationId) { viewModel.bind(conversationId) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isConversationResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner, conversationId, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isConversationResumed = true
                    viewModel.setConversationVisible(conversationId, true)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isConversationResumed = false
                    viewModel.setConversationVisible(conversationId, false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            isConversationResumed = true
            viewModel.setConversationVisible(conversationId, true)
        }
        onDispose {
            isConversationResumed = false
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setConversationVisible(conversationId, false)
        }
    }
    val activeHeartbeatConversationId = state.conversationId ?: conversationId
    LaunchedEffect(activeHeartbeatConversationId, isConversationResumed) {
        if (!isConversationResumed) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(40_000L)
            viewModel.sendConversationHeartbeat(activeHeartbeatConversationId)
        }
    }
    MessageDetailScreen(
        state = state,
        initialMessageId = initialMessageId,
        initialSharedContentId = initialSharedContentId,
        initialSharedText = initialSharedText,
        initialSharedUris = initialSharedUris,
        onInitialSharedContentConsumed = onInitialSharedContentConsumed,
        title = run {
            val convTitle = state.conversation?.title?.trim()
            when {
                !convTitle.isNullOrEmpty() && convTitle != "کاربر" && convTitle != "user" -> convTitle
                title.isNotBlank() && title != "پیام‌ها" && title != "گفتگوی محرمانه" && title != "کاربر" -> title
                !convTitle.isNullOrEmpty() -> convTitle
                else -> title
            }
        },
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onLoadOlder = viewModel::loadOlder,
        onSend = viewModel::send,
        onSendReply = viewModel::sendReply,
        onSendGif = viewModel::sendGif,
        onLoadGifs = viewModel::loadTrendingGifs,
        onToggleSaveGif = viewModel::toggleSaveGif,
        onImportDeviceGif = { uri -> viewModel.importAndSendCustomGif(uri, context.contentResolver) },
        isGifSaved = viewModel::isGifSaved,
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
        onSaveAttachment = viewModel::saveAttachment,
        onSaveAttachmentDirect = { message ->
            when (val res = viewModel.saveAttachmentDirect(message)) {
                is ChatResult.Success -> Result.success(res.value)
                is ChatResult.Failure -> Result.failure(IllegalStateException(res.message))
            }
        },
        onSaveExternalMediaDirect = { sourceUrl, fallbackUrl, kind, fileName, mimeType ->
            when (val res = viewModel.saveExternalMediaDirect(sourceUrl, fallbackUrl, kind, fileName, mimeType)) {
                is ChatResult.Success -> Result.success(res.value)
                is ChatResult.Failure -> Result.failure(IllegalStateException(res.message))
            }
        },
        onOpenProfile = onOpenProfile,
        onLoadPartnerProfile = viewModel::loadPartnerProfile,
        onStartSecretChat = { viewModel.startSecretChat(onStartSecretChat) },
        onSetSecretAutoDelete = viewModel::setSecretAutoDelete,
        onComputeSafetyNumber = viewModel::computeSafetyNumber,
        onClearError = viewModel::clearError,
        onOpenPost = onOpenPost,
        duelPreviews = state.duelPreviews,
        onRequestDuelPreview = viewModel::loadDuelPreview,
        onSessionAlertAction = viewModel::respondToSessionAlert,
        onAcceptMessageRequest = viewModel::acceptMessageRequest,
        onRejectMessageRequest = { viewModel.rejectMessageRequest(onRejected = onBack) },
        onRejectAndBlockMessageRequest = { viewModel.rejectAndBlockMessageRequest(onDone = onBack) },
        onCancelMessageRequest = viewModel::cancelMessageRequest,
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
    onToggleSaveGif: (String) -> Unit = {},
    onImportDeviceGif: (Uri) -> Unit = {},
    isGifSaved: (String) -> Boolean = { false },
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
    onSaveAttachment: (Message, (Boolean, String) -> Unit) -> Unit = { _, complete -> complete(false, "") },
    onSaveAttachmentDirect: (suspend (Message) -> Result<String>)? = null,
    onSaveExternalMediaDirect: (suspend (String, String?, AttachmentKind, String?, String?) -> Result<String>)? = null,
    onOpenProfile: (String) -> Unit = {},
    onLoadPartnerProfile: (String) -> Unit = {},
    onStartSecretChat: () -> Unit = {},
    onSetSecretAutoDelete: (Int) -> Unit = {},
    onComputeSafetyNumber: ((String?) -> Unit) -> Unit = {},
    onClearError: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
    duelPreviews: Map<String, DuelPreviewUiState> = emptyMap(),
    onRequestDuelPreview: (String) -> Unit = {},
    onSessionAlertAction: (Message, String, String) -> Unit = { _, _, _ -> },
    onAcceptMessageRequest: () -> Unit = {},
    onRejectMessageRequest: () -> Unit = {},
    onRejectAndBlockMessageRequest: () -> Unit = {},
    onCancelMessageRequest: () -> Unit = {},
) = CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    val context = LocalContext.current
    val localView = LocalView.current
    val internalNavigator = LocalVistaInternalLinkNavigator.current
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

    LaunchedEffect(context) {
        ChatAudioPlayer.attach(context)
    }
    LaunchedEffect(state.messages) {
        val voiceIds = state.messages
            .filter { it.attachment?.kind == AttachmentKind.VOICE }
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
    var audioPlayerSheetVisible by remember { mutableStateOf(false) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchResultIndex by rememberSaveable { mutableStateOf(0) }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val messageRenderItems = remember(state.messages) {
        buildMessageRenderItems(state.messages).distinctBy { it.stableKey }
    }
    val groupMemberById = remember(state.groupMembers) {
        state.groupMembers.associateBy(GroupMember::userId)
    }
    val conversationTitleFallback = state.conversation?.title?.takeIf(String::isNotBlank) ?: title
    val (voiceQueue, musicQueue) = remember(state.messages, state.downloads, groupMemberById, conversationTitleFallback) {
        val voices = mutableListOf<ChatAudioTrack>()
        val musics = mutableListOf<ChatAudioTrack>()
        state.messages.asReversed().forEach { msg ->
            if (msg.content == MessageContent.Deleted) return@forEach
            val att = msg.attachment ?: return@forEach
            val dl = state.downloads[msg.serverId ?: msg.clientId]
            val local = dl?.localUri?.takeIf(String::isNotBlank) ?: att.localUri?.takeIf(String::isNotBlank)
            val url = local ?: att.remoteUrl?.takeIf(String::isNotBlank) ?: return@forEach
            val senderLabel = when {
                msg.isMine -> "شما"
                state.conversation?.type == ConversationType.GROUP -> groupMemberById[msg.senderId]?.displayName ?: "کاربر"
                else -> conversationTitleFallback
            }
            when (att.kind) {
                AttachmentKind.VOICE -> voices.add(
                    ChatAudioTrack(
                        id = msg.stableKey,
                        url = url,
                        kind = ChatAudioKind.VOICE,
                        title = senderLabel,
                        artist = null,
                        durationMs = (att.durationSeconds ?: 1).coerceAtLeast(1) * 1000L,
                        senderName = senderLabel,
                        conversationId = msg.conversationId,
                        createdAtEpochMillis = msg.createdAtEpochMillis,
                        fallbackUrl = att.remoteUrl?.takeIf { it.isNotBlank() && it != url },
                        conversationTitle = conversationTitleFallback,
                    ),
                )
                AttachmentKind.AUDIO -> musics.add(
                    ChatAudioTrack(
                        id = msg.stableKey,
                        url = url,
                        kind = ChatAudioKind.MUSIC,
                        title = resolveMusicTitle(att.audioTitle, att.fileName ?: att.remoteUrl),
                        artist = att.audioArtist?.trim()?.takeIf(String::isNotEmpty),
                        durationMs = (att.durationSeconds ?: 0).coerceAtLeast(0) * 1000L,
                        senderName = senderLabel,
                        conversationId = msg.conversationId,
                        createdAtEpochMillis = msg.createdAtEpochMillis,
                        fallbackUrl = att.remoteUrl?.takeIf { it.isNotBlank() && it != url },
                        conversationTitle = conversationTitleFallback,
                    ),
                )
                else -> Unit
            }
        }
        voices.toList() to musics.toList()
    }
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
    val hasPendingAttachments = pendingAttachmentDrafts.isNotEmpty()
    LaunchedEffect(hasPendingAttachments) {
        if (hasPendingAttachments) {
            emojiPanelVisible = false
            focusManager.clearFocus(force = true)
            composerController.closeKeyboard()
        }
    }
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
        val shouldScrollToNewest = newest != null && previous != null && previous != newest.stableKey &&
            (newest.isMine || listState.firstVisibleItemIndex <= 1) &&
            (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
        if (shouldScrollToNewest) {
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
    val chatGlassState = rememberVistaGlassState()
    VistaGlassScrollSync(chatGlassState, listState)
    var composerHeightDp by remember { mutableStateOf(72.dp) }
    val chatEntryMode = LocalChatEntryMode.current
    var chatRootTopLeft by remember { mutableStateOf(Offset.Zero) }
    var chatRootSizePx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var composerInputBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var composerReplyBannerBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    val textMeasurer = rememberTextMeasurer()
    var pendingSendFlight by remember(state.conversationId) { mutableStateOf<PendingSendFlight?>(null) }
    var activeSendFlight by remember(state.conversationId) { mutableStateOf<ActiveSendFlight?>(null) }
    var activeFlightTargetBounds by remember(state.conversationId) { mutableStateOf<Pair<String, Rect>?>(null) }
    val flownSendFlightKeys = remember(state.conversationId) { mutableSetOf<String>() }
    var sendFlightSeq by remember { mutableStateOf(0L) }

    val displayedMessages = state.messages
    val displayedRenderItems = messageRenderItems
    LaunchedEffect(displayedRenderItems, pendingSendFlight) {
        val pending = pendingSendFlight ?: return@LaunchedEffect
        val firstRenderItem = displayedRenderItems.firstOrNull()
        val firstPrimary = firstRenderItem?.primary
        if (firstPrimary != null &&
            isEligibleSendFlightCandidate(
                pending = pending,
                candidateStableKey = firstPrimary.stableKey,
                candidateText = (firstPrimary.content as? MessageContent.Text)?.value,
                candidateIsMine = firstPrimary.isMine,
                candidateIsPending = firstPrimary.status == MessageStatus.PENDING,
                flownMessageKeys = flownSendFlightKeys,
            )
        ) {
            flownSendFlightKeys += firstPrimary.stableKey
            val textDir = resolveMessageTextDirection(pending.text)
            val isRtlText = textDir == androidx.compose.ui.text.style.TextDirection.Rtl ||
                textDir == androidx.compose.ui.text.style.TextDirection.ContentOrRtl
            val screenWidthPx = chatRootSizePx.width.toFloat().coerceAtLeast(with(density) { 360.dp.toPx() })
            val screenHeightPx = chatRootSizePx.height.toFloat().coerceAtLeast(with(density) { 400.dp.toPx() })
            val maxBubbleWidthPx = (screenWidthPx * 0.82f).coerceAtLeast(with(density) { 120.dp.toPx() })
            val maxTextWidthPx = (maxBubbleWidthPx - with(density) { 24.dp.toPx() }).toInt().coerceAtLeast(1)
            val measured = runCatching {
                textMeasurer.measure(
                    text = AnnotatedString(pending.text),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        textDirection = textDir,
                    ),
                    constraints = Constraints(maxWidth = maxTextWidthPx),
                )
            }.getOrNull()
            val textWidthPx = measured?.size?.width?.toFloat() ?: with(density) { 96.dp.toPx() }
            val textHeightPx = measured?.size?.height?.toFloat() ?: with(density) { 22.dp.toPx() }
            val singleLine = (measured?.lineCount ?: 1) <= 1
            val metaFooterReservePx = if (singleLine) with(density) { 52.dp.toPx() } else 0f
            val estimatedWidthPx = (textWidthPx + with(density) { 24.dp.toPx() } + metaFooterReservePx)
                .coerceIn(with(density) { 64.dp.toPx() }, maxBubbleWidthPx)
            val estimatedHeightPx = (textHeightPx + with(density) { 16.dp.toPx() } +
                if (pending.replyPreview != null) with(density) { 38.dp.toPx() } else 0f)
                .coerceAtLeast(with(density) { 42.dp.toPx() })

            val startBounds = resolveSendFlightStartBounds(
                inputBoundsLocal = pending.startBoundsLocal,
                estimatedBubbleWidthPx = estimatedWidthPx,
                isRtlText = isRtlText,
            ).let { resolved ->
                val replyTop = pending.replyStartBoundsLocal?.top
                if (replyTop != null && replyTop < resolved.top) {
                    resolved.copy(top = replyTop)
                } else {
                    resolved
                }
            }
            val composerPx = with(density) { composerHeightDp.toPx() }
            val fallbackTarget = resolveSendFlightTargetFallbackBounds(
                rootWidthPx = screenWidthPx,
                rootHeightPx = screenHeightPx,
                composerHeightPx = composerPx,
                estimatedBubbleWidthPx = estimatedWidthPx,
                estimatedBubbleHeightPx = estimatedHeightPx,
                alignToRight = firstPrimary.isMine,
                horizontalMarginPx = with(density) { 12.dp.toPx() },
                bottomMarginPx = with(density) { 9.dp.toPx() },
            )
            val targetGroupPos = messageGroupPosition(
                messages = displayedMessages,
                index = firstRenderItem.primaryIndex,
                spanLength = firstRenderItem.messages.size,
            )
            if (activeFlightTargetBounds?.first != firstPrimary.stableKey) {
                activeFlightTargetBounds = null
            }
            activeSendFlight = ActiveSendFlight(
                flightId = pending.flightId,
                messageKey = firstPrimary.stableKey,
                text = pending.text,
                replyPreview = pending.replyPreview,
                startBoundsLocal = startBounds,
                fallbackTargetBoundsLocal = fallbackTarget,
                targetAlignRight = firstPrimary.isMine,
                targetIsFirstInGroup = targetGroupPos.isFirstInGroup,
                targetIsLastInGroup = targetGroupPos.isLastInGroup,
                replySender = pending.replySender,
                replyStartBoundsLocal = pending.replyStartBoundsLocal,
                formattedTime = formatTime(firstPrimary.createdAtEpochMillis),
                topClipPx = with(density) { effectiveTopPadding.toPx() },
            )
            pendingSendFlight = null
        } else if (System.currentTimeMillis() - pending.createdAtMs > 1_500L) {
            pendingSendFlight = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                chatRootTopLeft = coords.positionInRoot()
                chatRootSizePx = coords.size
            }
            .focusProperties { canFocus = !hasPendingAttachments }
            .background(if (isDarkTheme) Color(0xFF101419) else Color(0xFFDFE5E9)),
    ) {
        val unreadWindow = remember(state.messages) { unreadMessageWindow(state.messages) }
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
        Box(modifier = Modifier.fillMaxSize().vistaGlassSource(chatGlassState)) {
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

            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(emojiPanelVisible) {
                        if (emojiPanelVisible) {
                            detectTapGestures {
                                emojiPanelVisible = false
                            }
                        }
                    },
            ) {
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
                val latestRenderItems by rememberUpdatedState(displayedRenderItems)
                val latestInteraction by rememberUpdatedState(messageInteraction)
                var dragBaseKeys by remember { mutableStateOf(emptyList<String>()) }
                val dragSelectModifier = Modifier.dragSelectMessages(
                    isSelecting = { latestInteraction is MessageInteractionState.Selecting },
                    hitTest = { position ->
                        val info = listState.layoutInfo
                        val viewportHeight = info.viewportSize.height
                        // The list is reversed: item offsets are measured from the bottom.
                        info.visibleItemsInfo.firstOrNull { item ->
                            val top = viewportHeight - item.offset - item.size
                            position.y >= top && position.y < top + item.size
                        }?.index
                    },
                    onDragStart = { _ ->
                        dragBaseKeys = (latestInteraction as? MessageInteractionState.Selecting)
                            ?.messageKeys.orEmpty()
                    },
                    onRangeChanged = { anchor, current ->
                        val keys = (minOf(anchor, current)..maxOf(anchor, current))
                            .mapNotNull { latestRenderItems.getOrNull(it) }
                            .flatMap { it.messages }
                            .filter { it.content != MessageContent.Deleted }
                            .map(Message::stableKey)
                        val merged = (dragBaseKeys + keys).distinct()
                        if (merged.isNotEmpty()) {
                            messageInteraction = MessageInteractionState.Selecting(merged)
                        }
                    },
                    scrollBy = { delta -> listState.scrollBy(delta) },
                    topEdgeInsetPx = with(density) { effectiveTopPadding.toPx() },
                    edgeZonePx = with(density) { 72.dp.toPx() },
                )
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
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                VistaViuStateIllustration(
                                    kind = VistaViuStateKind.EmptyChat,
                                    size = 136.dp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("هنوز پیامی نیست")
                            }
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("chat-message-list").then(dragSelectModifier),
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(
                            start = 6.dp,
                            end = 6.dp,
                            top = effectiveTopPadding + 6.dp,
                            bottom = composerHeightDp + 6.dp,
                        ),
                    ) {
                        itemsIndexed(
                            displayedRenderItems,
                            key = { _, item -> item.stableKey },
                            contentType = { _, item ->
                                when {
                                    item.isAlbum -> "media-album"
                                    item.primary.isSystemNotice -> "system-notice"
                                    else -> item.primary.attachment?.kind?.name ?: "text"
                                }
                            },
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
                            val matchesPendingFlight = index == 0 && isEligibleSendFlightCandidate(
                                pending = pendingSendFlight,
                                candidateStableKey = message.stableKey,
                                candidateText = (message.content as? MessageContent.Text)?.value,
                                candidateIsMine = message.isMine,
                                candidateIsPending = message.status == MessageStatus.PENDING,
                                flownMessageKeys = flownSendFlightKeys,
                            )
                            val isFlyingSendTarget = activeSendFlight?.messageKey == message.stableKey || matchesPendingFlight
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                        placementSpec = tween(
                                            durationMillis = TELEGRAM_SEND_DURATION_MS,
                                            easing = TelegramSendBezierEasing,
                                        ),
                                    )
                                    .graphicsLayer {
                                        alpha = if (isFlyingSendTarget) 0f else 1f
                                    },
                            ) {
                                if (isDateBoundary) {
                                    DateDivider(renderItem.oldest.createdAtEpochMillis)
                                }
                                if (isUnreadBoundary) {
                                    UnreadMessagesDivider(text = "پیام‌های جدید")
                                }
                                if (message.isSystemNotice) {
                                    val rawNotice = (message.content as? MessageContent.Text)?.value.orEmpty()
                                    val noticeText = StructuredMessageFormatter.formatStructuredOrJsonPreview(
                                        raw = rawNotice,
                                        transportType = message.transportType,
                                        sharedPostWithColon = true,
                                    ) ?: rawNotice
                                    if (noticeText.isNotBlank()) {
                                        SecuritySystemDivider(
                                            text = noticeText,
                                            onClick = if (isSecretConversation) {
                                                {
                                                    onComputeSafetyNumber { }
                                                    safetyNumberDialogVisible = true
                                                }
                                            } else {
                                                null
                                            },
                                        )
                                    }
                                } else {
                                    val groupSender = groupMemberById[message.senderId]
                                GroupSenderFrame(
                                    enabled = state.conversation?.type == ConversationType.GROUP && !message.isMine,
                                    senderId = message.senderId,
                                    senderName = groupSender?.displayName ?: "کاربر",
                                    avatarUrl = groupSender?.avatarUrl,
                                    senderIsVerified = groupSender?.isVerified == true,
                                    senderVerificationType = groupSender?.verificationType,
                                    senderRole = groupSender?.role,
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
                                                horizontalAlignment = if (message.isMine) AbsoluteAlignment.Right else AbsoluteAlignment.Left,
                                            ) {
                                                MediaAlbumBubble(
                                                    messages = renderItem.messages,
                                                    downloads = state.downloads,
                                                    settingsPreferences = state.settingsPreferences,
                                                    connectionTransport = state.connectionTransport,
                                                    isFirstInGroup = groupPosition.isFirstInGroup,
                                                    isLastInGroup = groupPosition.isLastInGroup,
                                                    selectionMode = selectedMessageKeys.isNotEmpty(),
                                                    selected = messageKeys.all(selectedMessageKeys::contains),
                                                    onToggleSelection = { toggleMessageSelection(messageKeys) },
                                                    onLongPress = {
                                                        if (messageInteraction is MessageInteractionState.Idle) {
                                                            openMessageContext(message.stableKey, null)
                                                        }
                                                    },
                                                    onOpenContextMenu = { bubbleBounds ->
                                                        if (messageInteraction is MessageInteractionState.Idle &&
                                                            message.content != MessageContent.Deleted
                                                        ) {
                                                            openMessageContext(message.stableKey, bubbleBounds)
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
                                                MessageReactionsRow(
                                                    reactions = message.reactions,
                                                    accountId = message.accountId,
                                                    insideMineBubble = false,
                                                    selectionMode = selectedMessageKeys.isNotEmpty(),
                                                    onToggleSelection = { toggleMessageSelection(messageKeys) },
                                                    onReact = { emoji -> onReact(message, emoji) },
                                                    modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp),
                                                )
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
                                                settingsPreferences = state.settingsPreferences,
                                                connectionTransport = state.connectionTransport,
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
                                                duelPreviews = duelPreviews,
                                                onRequestDuelPreview = onRequestDuelPreview,
                                                pendingSessionAlertActions = state.pendingSessionAlertActions,
                                                onSessionAlertAction = onSessionAlertAction,
                                                onOpenGame = { targetPath ->
                                                    internalNavigator?.openUrl("https://cafevista.ir$targetPath")
                                                },
                                                voiceQueue = voiceQueue,
                                                musicQueue = musicQueue,
                                                senderDisplayName = when {
                                                    message.isMine -> "شما"
                                                    state.conversation?.type == ConversationType.GROUP -> groupSender?.displayName ?: "کاربر"
                                                    else -> conversationTitleFallback
                                                },
                                                onOpenAudioPlayer = { audioPlayerSheetVisible = true },
                                                onBubbleBoundsInRootChanged = if (isFlyingSendTarget) { boundsInRoot ->
                                                    activeFlightTargetBounds = message.stableKey to boundsInRoot.translate(
                                                        -chatRootTopLeft.x,
                                                        -chatRootTopLeft.y,
                                                    )
                                                } else null,
                                            )
                                        }
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
            }
        }

        activeSendFlight?.let { flight ->
            TelegramSendFlightOverlay(
                flight = flight,
                targetBoundsLocal = activeFlightTargetBounds?.takeIf { it.first == flight.messageKey }?.second,
                textDirection = resolveMessageTextDirection(flight.text),
                minimalMotion = chatEntryMode == "minimal",
                onFlightFinished = { finishedKey ->
                    if (activeSendFlight?.messageKey == finishedKey) {
                        activeSendFlight = null
                        if (activeFlightTargetBounds?.first == finishedKey) {
                            activeFlightTargetBounds = null
                        }
                    }
                },
                modifier = Modifier.zIndex(5f),
            )
        }

        CompositionLocalProvider(LocalVistaGlassState provides chatGlassState) {
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
                        modifier = Modifier.padding(end = 12.dp, bottom = 12.dp + composerHeightDp),
                    ) {
                        Box(
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .vistaGlass(
                                        state = chatGlassState,
                                        shape = CircleShape,
                                        tint = if (isDarkTheme) Color(0xFF1A222C) else Color(0xFFFFFFFF),
                                        isDark = isDarkTheme,
                                        blurRadius = VistaGlassTokens.PillBlur,
                                        tintAlphaOverride = if (isDarkTheme) 0.58f else 0.62f,
                                        drawHighlight = true,
                                        rimWidth = 0.6.dp,
                                        shadowElevation = 5.dp,
                                    )
                                    .clip(CircleShape)
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
                                        .background(MaterialTheme.colorScheme.primary)
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .vistaGlassHalo(
                            state = chatGlassState,
                            isDark = isDarkTheme,
                            blurRadius = 18.dp,
                            startOffsetFraction = 0.12f,
                            maxAlpha = if (isDarkTheme) 0.44f else 0.40f,
                        ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            val h = with(density) { size.height.toDp() }
                            if (h != composerHeightDp) composerHeightDp = h
                        },
                ) {
                    val conv = state.conversation
                    val gateStatus = conv?.requestStatus?.trim()?.lowercase()
                    val gateLockReason = conv?.lockReason?.trim()?.lowercase()
                    val isGroupConv = conv?.type == ConversationType.GROUP
                    val isRecipientDecision = !isGroupConv && conv != null && (
                        gateLockReason == "recipient_decision_required" ||
                            (conv.isMessageRequest && (gateStatus == null || gateStatus == "pending")) ||
                            (gateStatus == "pending" && !conv.initiatorUserId.isNullOrBlank() &&
                                !conv.initiatorUserId.equals(conv.accountId, ignoreCase = true))
                        )
                    val isWaitingApproval = !isGroupConv && conv != null && !isRecipientDecision && (
                        gateLockReason == "waiting_recipient_approval" ||
                            (gateStatus == "pending" && !conv.canSendMessage &&
                                conv.initiatorUserId.equals(conv.accountId, ignoreCase = true))
                        )
                    val isTerminalLocked = !isGroupConv && conv != null &&
                        !isRecipientDecision && !isWaitingApproval && (
                        gateLockReason in setOf("request_rejected", "request_blocked", "request_cancelled") ||
                            gateStatus in setOf("rejected", "blocked", "cancelled")
                        )
                    val isPreFirstNotice = !isGroupConv && conv != null &&
                        !isRecipientDecision && !isWaitingApproval && !isTerminalLocked &&
                        gateLockReason == "pre_first_message" && conv.canSendMessage

                    var confirmRejectBlockVisible by remember { mutableStateOf(false) }

                    LaunchedEffect(isRecipientDecision, isWaitingApproval, isTerminalLocked) {
                        if (isRecipientDecision || isWaitingApproval || isTerminalLocked) {
                            emojiPanelVisible = false
                            focusManager.clearFocus(force = true)
                            composerController.closeKeyboard()
                        }
                    }

                    if (isRecipientDecision) {
                        RequestRecipientDecisionBar(
                            peerTitle = title,
                            loading = state.isRequestActionInFlight,
                            onAccept = onAcceptMessageRequest,
                            onReject = onRejectMessageRequest,
                            onRejectAndBlock = { confirmRejectBlockVisible = true },
                        )
                    } else if (isWaitingApproval) {
                        RequestWaitingApprovalBar(
                            peerTitle = title,
                            loading = state.isRequestActionInFlight,
                            onCancel = onCancelMessageRequest,
                        )
                    } else if (isTerminalLocked) {
                        RequestTerminalLockedBar(
                            lockReason = gateLockReason ?: gateStatus ?: "request_rejected",
                        )
                    } else {
                        if (isPreFirstNotice) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            ) {
                                Text(
                                    text = "ارسال اولین پیام به عنوان درخواست گفتگو ثبت می‌شود و تا تایید گیرنده امکان ارسال پیام بعدی نخواهید داشت.",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        ChatComposerBar(
                            conversationId = state.conversationId,
                            title = title,
                            replyingTo = replyingTo,
                            editingMessage = editingMessage,
                            onClearReply = { replyingTo = null },
                            onClearEdit = { editingMessage = null },
                            onSend = { text, onDone ->
                                val cleanText = text.trim()
                                val inputBounds = composerInputBoundsInRoot
                                if (chatEntryMode != "off" && cleanText.isNotEmpty() && inputBounds != null) {
                                    sendFlightSeq += 1L
                                    pendingSendFlight = PendingSendFlight(
                                        flightId = sendFlightSeq,
                                        text = cleanText,
                                        replyPreview = null,
                                        startBoundsLocal = inputBounds.translate(-chatRootTopLeft.x, -chatRootTopLeft.y),
                                        createdAtMs = System.currentTimeMillis(),
                                    )
                                }
                                onSend(text, onDone)
                                if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
                                    coroutineScope.launch { listState.animateScrollToItem(0) }
                                }
                            },
                            onSendReply = { text, reply, onDone ->
                                val cleanText = text.trim()
                                val inputBounds = composerInputBoundsInRoot
                                if (chatEntryMode != "off" && cleanText.isNotEmpty() && inputBounds != null) {
                                    sendFlightSeq += 1L
                                    pendingSendFlight = PendingSendFlight(
                                        flightId = sendFlightSeq,
                                        text = cleanText,
                                        replyPreview = reply.previewText(),
                                        startBoundsLocal = inputBounds.translate(-chatRootTopLeft.x, -chatRootTopLeft.y),
                                        createdAtMs = System.currentTimeMillis(),
                                        replySender = reply.replyDisplayName(title),
                                        replyStartBoundsLocal = composerReplyBannerBoundsInRoot
                                            ?.translate(-chatRootTopLeft.x, -chatRootTopLeft.y),
                                    )
                                }
                                onSendReply(text, reply, onDone)
                                if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) {
                                    coroutineScope.launch { listState.animateScrollToItem(0) }
                                }
                            },
                            onInputFieldBoundsChanged = { composerInputBoundsInRoot = it },
                            onReplyBannerBoundsChanged = { composerReplyBannerBoundsInRoot = it },
                            onEdit = onEdit,
                            onComposerChanged = onComposerChanged,
                            composerController = composerController,
                            focusRequester = focusRequester,
                            onAttach = {
                                emojiPanelVisible = false
                                focusManager.clearFocus(force = true)
                                composerController.closeKeyboard()
                                attachmentSheetVisible = true
                            },
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
                    }

                    if (confirmRejectBlockVisible) {
                        AlertDialog(
                            onDismissRequest = { confirmRejectBlockVisible = false },
                            title = { Text("رد و مسدود کردن کاربر") },
                            text = { Text("درخواست گفتگو رد می‌شود و کاربر $title برای همیشه مسدود خواهد شد. آیا مطمئن هستید؟") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        confirmRejectBlockVisible = false
                                        onRejectAndBlockMessageRequest()
                                    },
                                ) {
                                    Text("رد و بلاک", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { confirmRejectBlockVisible = false }) {
                                    Text("انصراف")
                                }
                            },
                        )
                    }
            val currentPanelHeight = composerPanelHeightDp.dp

            if (emojiPanelVisible && !isRecipientDecision && !isWaitingApproval && !isTerminalLocked) {
                val panelHeight = maxOf(currentPanelHeight, imeBottomDp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(panelHeight)
                ) {
                    ChatMediaPickerPanel(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        gifs = state.gifs,
                        savedGifs = state.savedGifs,
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
                        onImportDeviceGif = { uri ->
                            emojiPanelVisible = false
                            onImportDeviceGif(uri)
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
        }

        val inSelectionMode = selectedMessageKeys.isNotEmpty()
        val topBarBgColor = when {
            inSelectionMode -> MaterialTheme.colorScheme.primary
            isDarkTheme -> Color(0xFF1A222C)
            else -> Color(0xFFFFFFFF)
        }
        val topBarGlassAlpha = when {
            inSelectionMode -> 0.94f
            isDarkTheme -> 0.58f
            else -> 0.62f
        }
        Box(
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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .vistaGlassHalo(
                        state = chatGlassState,
                        isDark = isDarkTheme,
                        blurRadius = 18.dp,
                        startOffsetFraction = 0.05f,
                        maxAlpha = if (isDarkTheme) 0.42f else 0.36f,
                        fromTop = true,
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
                    val selectionShape = RoundedCornerShape(24.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = selectionShape,
                        color = topBarBgColor.copy(alpha = topBarGlassAlpha),
                        shadowElevation = 4.dp,
                    ) {
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
                    }
                } else if (searchVisible) {
                    val searchShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .vistaGlass(
                                state = chatGlassState,
                                shape = searchShape,
                                tint = topBarBgColor,
                                isDark = isDarkTheme,
                                blurRadius = VistaGlassTokens.PillBlur,
                                tintAlphaOverride = topBarGlassAlpha,
                                drawHighlight = true,
                                rimWidth = 0.6.dp,
                                shadowElevation = 4.dp,
                            )
                            .clip(searchShape),
                    ) {
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
                    }
                } else {
                    val capsuleHeight = if (density.fontScale > 1.3f) 56.dp else 46.dp
                    val capsuleShape = RoundedCornerShape(capsuleHeight / 2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // 1. Floating Back Button Island
                        Box(
                            modifier = Modifier
                                .size(capsuleHeight)
                                .vistaGlass(
                                    state = chatGlassState,
                                    shape = CircleShape,
                                    tint = topBarBgColor,
                                    isDark = isDarkTheme,
                                    blurRadius = VistaGlassTokens.PillBlur,
                                    tintAlphaOverride = topBarGlassAlpha,
                                    drawHighlight = true,
                                    rimWidth = 0.6.dp,
                                    shadowElevation = 4.dp,
                                )
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = safeBack)
                                .semantics { contentDescription = "بازگشت" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        // 2. Floating Center Identity Capsule (Avatar + Title + Subtitle)
                        val resolvedAvatar = state.conversation?.avatarUrl
                            ?: state.groupInfo?.imageUrl
                            ?: state.partnerProfile?.avatarUrl
                        val resolvedHeaderTitle: String = when {
                            state.conversation?.type == ConversationType.GROUP -> {
                                state.groupInfo?.name?.takeIf(String::isNotBlank)
                                    ?: state.conversation?.title?.takeIf { it.isNotBlank() && it != "کاربر" && it != "user" }
                                    ?: title
                            }
                            state.conversation?.title?.takeIf { it.isNotBlank() && it != "کاربر" && it != "user" } != null -> {
                                state.conversation!!.title
                            }
                            state.partnerProfile?.fullName?.takeIf(String::isNotBlank) != null -> {
                                state.partnerProfile!!.fullName!!
                            }
                            state.partnerProfile?.username?.takeIf(String::isNotBlank) != null -> {
                                state.partnerProfile!!.username!!
                            }
                            else -> title
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(capsuleHeight)
                                .vistaGlass(
                                    state = chatGlassState,
                                    shape = capsuleShape,
                                    tint = topBarBgColor,
                                    isDark = isDarkTheme,
                                    blurRadius = VistaGlassTokens.PillBlur,
                                    tintAlphaOverride = topBarGlassAlpha,
                                    drawHighlight = true,
                                    rimWidth = 0.6.dp,
                                    shadowElevation = 4.dp,
                                )
                                .clip(capsuleShape)
                                .clickable {
                                    if (state.conversation?.type == ConversationType.GROUP) {
                                        groupSheetVisible = true
                                    } else {
                                        openPartnerDetails()
                                    }
                                }
                                .padding(start = 5.dp, end = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ChatAvatarImage(
                                model = resolvedAvatar,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start,
                            ) {
                                val headerIsVerified = state.conversation?.type != ConversationType.GROUP &&
                                    (state.partnerProfile?.isVerified == true || state.conversation?.isVerified == true)
                                val headerVerificationType = state.partnerProfile?.verificationType
                                    ?.takeIf(String::isNotBlank)
                                    ?: state.conversation?.verificationType
                                val headerRole = state.partnerProfile?.role
                                    ?.takeIf(String::isNotBlank)
                                    ?: state.conversation?.role
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSecretConversation) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = SecretChatGreen,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        resolvedHeaderTitle,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 15.5.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSecretConversation && !isDarkTheme) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (headerIsVerified) {
                                        ChatUserVerificationBadge(
                                            isVerified = true,
                                            verificationType = headerVerificationType,
                                            role = headerRole,
                                            size = 15.dp,
                                        )
                                    }
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
                                        color = SecretChatGreen,
                                        fontSize = 11.sp,
                                        lineHeight = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                } else {
                                    val visibleConnectionState = rememberDebouncedConnectionState(state.connectionState)
                                    val subtitle = when {
                                        visibleConnectionState == RealtimeConnectionState.CONNECTING ||
                                        visibleConnectionState == RealtimeConnectionState.RECONNECTING -> "در حال اتصال..."
                                        visibleConnectionState == RealtimeConnectionState.DISCONNECTED -> "در انتظار شبکه..."
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
                                        fontSize = 11.5.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        // 3. Floating Actions Island
                        Row(
                            modifier = Modifier
                                .height(capsuleHeight)
                                .defaultMinSize(minWidth = capsuleHeight)
                                .vistaGlass(
                                    state = chatGlassState,
                                    shape = capsuleShape,
                                    tint = topBarBgColor,
                                    isDark = isDarkTheme,
                                    blurRadius = VistaGlassTokens.PillBlur,
                                    tintAlphaOverride = topBarGlassAlpha,
                                    drawHighlight = true,
                                    rimWidth = 0.6.dp,
                                    shadowElevation = 4.dp,
                                )
                                .clip(capsuleShape)
                                .padding(horizontal = if (isSecretConversation) 4.dp else 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            if (isSecretConversation) {
                                IconButton(
                                    onClick = { autoDeleteDialogVisible = true },
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = "تنظیم تایمر حذف خودکار",
                                        tint = if (state.secretAutoDeleteSeconds > 0) SecretChatGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onComputeSafetyNumber { }
                                        safetyNumberDialogVisible = true
                                    },
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = "کلید رمزنگاری محرمانه",
                                        tint = SecretChatGreen,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            Box(contentAlignment = Alignment.Center) {
                                var menuVisible by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { menuVisible = true },
                                    modifier = Modifier.size(capsuleHeight),
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "گزینه‌های گفتگو",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
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
                        }
                    }
                }

                if (state.pinnedMessages.isNotEmpty()) {
                    PinnedMessagesBar(
                        glassState = chatGlassState,
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
                if (!inSelectionMode && !searchVisible) {
                    ChatMiniAudioPlayer(
                        glassState = chatGlassState,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(22.dp),
                        onOpenFullPlayer = { audioPlayerSheetVisible = true },
                        onJumpToMessage = { targetKey ->
                            val idx = renderItemIndexForMessage(messageRenderItems, targetKey)
                            if (idx >= 0) coroutineScope.launch { listState.animateScrollToItem(idx) }
                        },
                    )
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        if (autoDeleteDialogVisible) {
            SecretChatAutoDeleteDialog(
                currentSeconds = state.secretAutoDeleteSeconds,
                onDismiss = { autoDeleteDialogVisible = false },
                onSelect = { seconds ->
                    onSetSecretAutoDelete(seconds)
                    autoDeleteDialogVisible = false
                },
            )
        }
        if (safetyNumberDialogVisible) {
            SecretChatEncryptionKeyDialog(
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
        if (attachmentSheetVisible) {
            CompositionLocalProvider(LocalVistaGlassState provides chatGlassState) {
                ChatAttachmentBottomSheet(
                    onDismiss = { attachmentSheetVisible = false },
                    onMediaSelected = { uris -> enqueueSelectedAttachments(uris, groupAsAlbum = true) },
                    onFilesSelected = { uris -> enqueueSelectedAttachments(uris, groupAsAlbum = false) },
                    onDuelInviteSelected = state.conversation?.let { conv ->
                        {
                            val params = buildList {
                                if (conv.type != ConversationType.GROUP && !conv.peerId.isNullOrBlank()) {
                                    add("opponent=${java.net.URLEncoder.encode(conv.peerId, "UTF-8")}")
                                }
                                add("chatConvId=${java.net.URLEncoder.encode(conv.id, "UTF-8")}")
                            }
                            internalNavigator?.openUrl("https://cafevista.ir/game/duel/create?${params.joinToString("&")}")
                        }
                    },
                    glassState = chatGlassState,
                )
            }
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
            glassState = chatGlassState,
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
            onRetry = {
                onRetry(message)
                dismissMessageInteraction()
            },
            onSaveAttachment = if (isSecretConversation) null else {
                {
                    dismissMessageInteraction()
                    onSaveAttachment(message) { _, feedback ->
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            if (feedback.isNotBlank()) {
                                snackbarHostState.showSnackbar(feedback)
                            }
                        }
                    }
                }
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
    if (audioPlayerSheetVisible) {
        ChatAudioPlayerSheet(
            onDismiss = { audioPlayerSheetVisible = false },
            onJumpToMessage = { targetKey ->
                audioPlayerSheetVisible = false
                val idx = renderItemIndexForMessage(messageRenderItems, targetKey)
                if (idx >= 0) coroutineScope.launch { listState.animateScrollToItem(idx) }
            },
            onSaveTrack = if (isSecretConversation) null else { track ->
                val matched = state.messages.firstOrNull { it.stableKey == track.id }
                coroutineScope.launch {
                    val result = if (matched != null && onSaveAttachmentDirect != null) {
                        onSaveAttachmentDirect(matched)
                    } else if (onSaveExternalMediaDirect != null) {
                        onSaveExternalMediaDirect(
                            track.url,
                            track.fallbackUrl,
                            AttachmentKind.AUDIO,
                            "${track.title}.mp3",
                            "audio/mpeg",
                        )
                    } else {
                        saveChatAttachmentToDevice(
                            context = context,
                            sourceUriOrUrl = track.url,
                            fallbackUrl = track.fallbackUrl,
                            kind = AttachmentKind.AUDIO,
                            fileName = "${track.title}.mp3",
                            mimeType = "audio/mpeg",
                        )
                    }
                    val feedback = result.getOrElse { it.message ?: "ذخیره موسیقی انجام نشد" }
                    android.widget.Toast.makeText(context, feedback, android.widget.Toast.LENGTH_SHORT).show()
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(feedback)
                }
            },
        )
    }
    viewedAttachment?.let { attachment ->
        val activeMediaList = remember(state.messages, state.downloads, groupMemberById, conversationTitleFallback) {
            state.messages.asReversed().mapNotNull { msg ->
                if (msg.content == MessageContent.Deleted) return@mapNotNull null
                msg.attachment?.takeIf {
                    it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.GIF || it.kind == AttachmentKind.VIDEO
                }?.let { att ->
                    val dl = state.downloads[msg.serverId ?: msg.clientId]
                    val rawLocalUrl = dl?.localUri?.takeIf(String::isNotBlank)
                        ?: att.localUri?.takeIf(String::isNotBlank)
                    val safeLocalUrl = rawLocalUrl?.takeUnless {
                        it.contains("sec_up_") ||
                            it.contains("enc_chat_") ||
                            it.contains("secret_pending_uploads")
                    }
                    val remoteUrl = att.remoteUrl?.takeIf(String::isNotBlank)
                    val url = safeLocalUrl ?: remoteUrl ?: return@let null
                    val rawText = (msg.content as? MessageContent.Text)?.value?.trim().orEmpty()
                    val cleanCaption = rawText.takeIf {
                        it.isNotEmpty() &&
                            !StructuredMessageFormatter.isStructuredOrJsonPayload(it, msg.transportType)
                    }
                    val senderLabel = when {
                        msg.isMine -> "شما"
                        state.conversation?.type == ConversationType.GROUP -> groupMemberById[msg.senderId]?.displayName ?: "کاربر"
                        else -> conversationTitleFallback
                    }
                    MediaViewerItem(
                        url = url,
                        isVideo = att.kind == AttachmentKind.VIDEO,
                        fallbackUrl = remoteUrl?.takeIf { it != url },
                        title = att.fileName,
                        caption = cleanCaption,
                        isGif = att.kind == AttachmentKind.GIF,
                        messageKey = msg.stableKey,
                        senderName = senderLabel,
                        epochMillis = msg.createdAtEpochMillis,
                        isMine = msg.isMine,
                    )
                }
            }
        }
        val targetLocalUrl = attachment.localUri?.takeIf(String::isNotBlank)
        val targetRemoteUrl = attachment.remoteUrl?.takeIf(String::isNotBlank)
        val targetUrl = targetLocalUrl ?: targetRemoteUrl
        val initialIndex = activeMediaList.indexOfFirst { item ->
            (targetUrl != null && (item.url == targetUrl || item.fallbackUrl == targetUrl)) ||
                (targetRemoteUrl != null && (item.url == targetRemoteUrl || item.fallbackUrl == targetRemoteUrl)) ||
                (targetLocalUrl != null && item.url == targetLocalUrl)
        }.coerceAtLeast(0)

        if (attachment.kind == AttachmentKind.IMAGE || attachment.kind == AttachmentKind.GIF || attachment.kind == AttachmentKind.VIDEO) {
            FullscreenMediaViewerDialog(
                mediaItems = if (activeMediaList.isNotEmpty()) activeMediaList else listOf(
                    MediaViewerItem(
                        url = targetUrl.orEmpty(),
                        isVideo = attachment.kind == AttachmentKind.VIDEO,
                        fallbackUrl = targetRemoteUrl?.takeIf { it != targetUrl },
                        title = attachment.fileName,
                        isGif = attachment.kind == AttachmentKind.GIF,
                    ),
                ),
                initialIndex = initialIndex,
                onDismiss = { viewedAttachment = null },
                allowSave = !isSecretConversation,
                onSaveItem = if (isSecretConversation) null else { item ->
                    val target = item.messageKey?.let { key ->
                        state.messages.firstOrNull { it.stableKey == key }
                    }
                    if (target != null && onSaveAttachmentDirect != null) {
                        onSaveAttachmentDirect(target)
                    } else {
                        val kind = when {
                            item.isVideo -> AttachmentKind.VIDEO
                            item.isGif -> AttachmentKind.GIF
                            else -> AttachmentKind.IMAGE
                        }
                        onSaveExternalMediaDirect?.invoke(item.url, item.fallbackUrl, kind, item.title, null)
                            ?: saveChatAttachmentToDevice(
                                context = context,
                                sourceUriOrUrl = item.url,
                                fallbackUrl = item.fallbackUrl,
                                kind = kind,
                                fileName = item.title,
                            )
                    }
                },
                onForward = if (isSecretConversation) null else { item ->
                    val target = state.messages.firstOrNull { it.stableKey == item.messageKey }
                    viewedAttachment = null
                    if (target != null) forwardingMessage = target
                },
                onDelete = { item ->
                    val target = state.messages.firstOrNull { it.stableKey == item.messageKey }
                    viewedAttachment = null
                    if (target != null) deletingMessage = target
                },
                onShowInChat = { item ->
                    val key = item.messageKey
                    viewedAttachment = null
                    if (key != null) {
                        val idx = renderItemIndexForMessage(messageRenderItems, key)
                        if (idx >= 0) coroutineScope.launch { listState.animateScrollToItem(idx) }
                    }
                },
            )
        } else {
            val saveViewerAttachment: (suspend () -> Result<String>)? = if (isSecretConversation) null else {
                {
                    val matchedMsg = state.messages.firstOrNull { msg ->
                        val att = msg.attachment ?: return@firstOrNull false
                        (attachment.remoteUrl != null && att.remoteUrl == attachment.remoteUrl) ||
                            (attachment.localUri != null && att.localUri == attachment.localUri)
                    }
                    if (matchedMsg != null && onSaveAttachmentDirect != null) {
                        onSaveAttachmentDirect(matchedMsg)
                    } else {
                        val src = attachment.localUri?.takeIf(String::isNotBlank)
                            ?: attachment.remoteUrl?.takeIf(String::isNotBlank)
                            ?: ""
                        onSaveExternalMediaDirect?.invoke(
                            src,
                            attachment.remoteUrl,
                            attachment.kind,
                            attachment.fileName,
                            attachment.mimeType,
                        ) ?: saveChatAttachmentToDevice(
                            context = context,
                            sourceUriOrUrl = src,
                            fallbackUrl = attachment.remoteUrl,
                            kind = attachment.kind,
                            fileName = attachment.fileName,
                            mimeType = attachment.mimeType,
                        )
                    }
                }
            }
            AttachmentViewer(
                attachment = attachment,
                onDismiss = { viewedAttachment = null },
                onSaveToDevice = saveViewerAttachment,
            )
        }
    }
    if (pendingAttachmentDrafts.isNotEmpty()) {
        ChatAttachmentReviewSheet(
            drafts = pendingAttachmentDrafts,
            recipientTitle = title,
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
            onUpdateDraft = { index, updatedDraft ->
                pendingAttachmentDrafts = pendingAttachmentDrafts.toMutableList().also { list ->
                    if (index in list.indices) {
                        list[index] = updatedDraft
                    }
                }
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
}

@Composable
private fun GroupSenderFrame(
    enabled: Boolean,
    senderId: String,
    senderName: String,
    avatarUrl: String?,
    senderIsVerified: Boolean = false,
    senderVerificationType: String? = null,
    senderRole: String? = null,
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
                    Row(
                        modifier = Modifier
                            .clickable { onOpenProfile(senderId) }
                            .padding(start = 14.dp, end = 12.dp, top = 2.dp, bottom = 1.dp)
                            .testTag("group-message-sender:$senderId"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = senderName,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        ChatUserVerificationBadge(
                            isVerified = senderIsVerified,
                            verificationType = senderVerificationType,
                            role = senderRole,
                            size = 13.dp,
                            spacerWidth = 3.dp,
                        )
                    }
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
    settingsPreferences: ir.coffevista.vista_native.core.datastore.SettingsPreferences =
        ir.coffevista.vista_native.core.datastore.SettingsPreferences(),
    connectionTransport: ir.coffevista.vista_native.core.common.ConnectionTransport =
        ir.coffevista.vista_native.core.common.ConnectionTransport.WIFI,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onLongPress: () -> Unit,
    onOpenContextMenu: (Rect?) -> Unit = {},
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
    val coordinatesHolder = remember(primary.stableKey) { LayoutCoordinatesHolder() }
    val caption = visibleMessages.firstNotNullOfOrNull { message ->
        (message.content as? MessageContent.Text)?.value?.trim()?.takeIf(String::isNotEmpty)
    }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cornerRounding = messageBubbleCornerRounding(
        isMine = primary.isMine,
        groupPosition = MessageGroupPosition(isFirstInGroup, isLastInGroup),
    )
    val bubbleShape = telegramXOuterCornerRadii(cornerRounding).toAbsoluteShape()
    val innerGridShape = telegramXInnerCornerRadii(
        cornerRounding = cornerRounding,
        insetDp = 2f,
        roundTop = true,
        roundBottom = caption == null,
    ).toAbsoluteShape()
    val albumHaptics = rememberVistaHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (selectionMode) {
                        onToggleSelection()
                    } else {
                        onOpenContextMenu(coordinatesHolder.coordinates?.takeIf { it.isAttached }?.boundsInWindow())
                    }
                },
                onLongClick = {
                    if (!selectionMode) {
                        albumHaptics.heavy()
                        onLongPress()
                    }
                },
            )
            .absolutePadding(
                left = if (primary.isMine) 12.dp else 6.dp,
                right = if (primary.isMine) 6.dp else 12.dp,
                top = if (isFirstInGroup) 3.dp else 1.5.dp,
                bottom = if (isLastInGroup) 3.dp else 1.5.dp,
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
            val albumBaseModifier = Modifier
                .align(
                    if (primary.isMine) AbsoluteAlignment.CenterRight
                    else AbsoluteAlignment.CenterLeft,
                )
                .widthIn(min = 140.dp, max = screenWidth * 0.74f)
                .fillMaxWidth()
                .shadow(
                    elevation = 1.dp,
                    shape = bubbleShape,
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(bubbleShape)
                .then(
                    if (primary.isMine) {
                        Modifier.background(Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep)))
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    },
                )
                .then(
                    if (selected) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                            bubbleShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .onGloballyPositioned { coordinates ->
                    coordinatesHolder.coordinates = coordinates
                }
                .padding(2.dp)
                .testTag("media-album")
            Column(modifier = albumBaseModifier) {
                Box(modifier = Modifier.fillMaxWidth().clip(innerGridShape)) {
                    MediaAlbumGrid(
                        messages = visibleMessages,
                        downloads = downloads,
                        settingsPreferences = settingsPreferences,
                        connectionTransport = connectionTransport,
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        VistaEmojiText(
                            text = caption,
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(
                                color = if (primary.isMine) Color.White
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                textDirection = resolveMessageTextDirection(caption),
                            ),
                            emojiSize = 19.dp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(modifier = Modifier.align(AbsoluteAlignment.Right)) {
                            AlbumMessageMeta(message = primary)
                        }
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
    settingsPreferences: ir.coffevista.vista_native.core.datastore.SettingsPreferences =
        ir.coffevista.vista_native.core.datastore.SettingsPreferences(),
    connectionTransport: ir.coffevista.vista_native.core.common.ConnectionTransport =
        ir.coffevista.vista_native.core.common.ConnectionTransport.WIFI,
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
    val tile: @Composable (Message, Modifier) -> Unit =
        { message, modifier ->
            MediaAlbumTile(
                message = message,
                downloadTask = downloads[message.serverId ?: message.clientId],
                settingsPreferences = settingsPreferences,
                connectionTransport = connectionTransport,
                modifier = modifier,
                shape = androidx.compose.ui.graphics.RectangleShape,
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
            tile(messages[0], Modifier.weight(1f).fillMaxSize())
            Spacer(Modifier.width(1.5.dp))
            tile(messages[1], Modifier.weight(1f).fillMaxSize())
        }
        3 -> Row(Modifier.aspectRatio(1.5f)) {
            tile(messages[0], Modifier.weight(2f).fillMaxSize())
            Spacer(Modifier.width(1.5.dp))
            Column(Modifier.weight(1f).fillMaxSize()) {
                tile(messages[1], Modifier.weight(1f).fillMaxSize())
                Spacer(Modifier.height(1.5.dp))
                tile(messages[2], Modifier.weight(1f).fillMaxSize())
            }
        }
        4 -> Column(Modifier.aspectRatio(1f)) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                tile(messages[0], Modifier.weight(1f).fillMaxSize())
                Spacer(Modifier.width(1.5.dp))
                tile(messages[1], Modifier.weight(1f).fillMaxSize())
            }
            Spacer(Modifier.height(1.5.dp))
            Row(Modifier.weight(1f).fillMaxWidth()) {
                tile(messages[2], Modifier.weight(1f).fillMaxSize())
                Spacer(Modifier.width(1.5.dp))
                tile(messages[3], Modifier.weight(1f).fillMaxSize())
            }
        }
        else -> {
            val rows = albumRowIndices(messages.size)
            Column(Modifier.aspectRatio(3f / rows.size)) {
                rows.forEachIndexed { rowIndex, indices ->
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        indices.forEachIndexed { columnIndex, messageIndex ->
                            tile(messages[messageIndex], Modifier.weight(1f).fillMaxSize())
                            if (columnIndex != indices.lastIndex) Spacer(Modifier.width(1.5.dp))
                        }
                    }
                    if (rowIndex != rows.lastIndex) Spacer(Modifier.height(1.5.dp))
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
    settingsPreferences: ir.coffevista.vista_native.core.datastore.SettingsPreferences =
        ir.coffevista.vista_native.core.datastore.SettingsPreferences(),
    connectionTransport: ir.coffevista.vista_native.core.common.ConnectionTransport =
        ir.coffevista.vista_native.core.common.ConnectionTransport.WIFI,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
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
    val allowRemotePreview = message.isMine ||
        downloadTask?.state == DownloadState.COMPLETE ||
        shouldAllowRemoteAttachmentPreview(attachment, settingsPreferences, connectionTransport)
    val completedLocalUri = downloadTask?.takeIf { it.state == DownloadState.COMPLETE }?.localUri?.takeIf(String::isNotBlank)
    val rawLocalUri = completedLocalUri ?: attachment.localUri?.takeIf(String::isNotBlank)
    var localLoadFailed by remember(rawLocalUri) { mutableStateOf(false) }
    val usableLocalUri = rawLocalUri?.takeUnless { localLoadFailed }
    val remoteUrl = attachment.remoteUrl?.takeIf(String::isNotBlank)
    val source = usableLocalUri ?: if (allowRemotePreview) remoteUrl else null
    val activeUpload = attachment.transferState == TransferState.UPLOADING ||
        attachment.transferState == TransferState.QUEUED
    val needsDownload = attachment.transferState == TransferState.COMPLETE &&
        completedLocalUri.isNullOrBlank() &&
        attachment.localUri.isNullOrBlank() &&
        !remoteUrl.isNullOrBlank() &&
        !message.isMine
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
                            onAttachmentClick(attachment.copy(localUri = usableLocalUri))
                        !usableLocalUri.isNullOrBlank() -> onAttachmentClick(attachment)
                        allowRemotePreview && !remoteUrl.isNullOrBlank() ->
                            onAttachmentClick(attachment.copy(localUri = null))
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
            onError = {
                if (!rawLocalUri.isNullOrBlank() && source == rawLocalUri && !remoteUrl.isNullOrBlank()) {
                    localLoadFailed = true
                }
            },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    searchQuery: String? = null,
    onRetry: (Message) -> Unit,
    onCancelTransfer: (Message) -> Unit,
    downloadTask: DownloadTask? = null,
    settingsPreferences: ir.coffevista.vista_native.core.datastore.SettingsPreferences =
        ir.coffevista.vista_native.core.datastore.SettingsPreferences(),
    connectionTransport: ir.coffevista.vista_native.core.common.ConnectionTransport =
        ir.coffevista.vista_native.core.common.ConnectionTransport.WIFI,
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
    duelPreviews: Map<String, DuelPreviewUiState> = emptyMap(),
    onRequestDuelPreview: (String) -> Unit = {},
    pendingSessionAlertActions: Set<String> = emptySet(),
    onSessionAlertAction: (Message, String, String) -> Unit = { _, _, _ -> },
    onOpenGame: (String) -> Unit = {},
    voiceQueue: List<ChatAudioTrack> = emptyList(),
    musicQueue: List<ChatAudioTrack> = emptyList(),
    senderDisplayName: String = if (message.isMine) "شما" else "مخاطب",
    onOpenAudioPlayer: () -> Unit = {},
    onBubbleBoundsInRootChanged: ((Rect) -> Unit)? = null,
) {
    val isDeleted = message.content == MessageContent.Deleted
    val duelInvite = remember(message.content, message.transportType) {
        if (isDeleted) null
        else when (val content = message.content) {
            is MessageContent.Structured -> DuelInviteParser.tryParse(content.payload, message.transportType)
            is MessageContent.Text -> DuelInviteParser.tryParse(content.value, message.transportType)
            else -> null
        }
    }
    val sessionAlert = remember(message.content, message.transportType) {
        if (isDeleted || duelInvite != null) null
        else when (val content = message.content) {
            is MessageContent.Structured -> ir.coffevista.vista_native.features.chat.domain.model.SessionAlertParser.tryParse(content.payload, message.transportType)
            is MessageContent.Text -> ir.coffevista.vista_native.features.chat.domain.model.SessionAlertParser.tryParse(content.value, message.transportType)
            else -> null
        }
    }
    val sharedPost = remember(message.content, duelInvite, sessionAlert) {
        if (isDeleted || duelInvite != null || sessionAlert != null) null
        else when (val content = message.content) {
            is MessageContent.Structured -> SharedPostParser.tryParse(content.payload)
            is MessageContent.Text -> SharedPostParser.tryParse(content.value)
            else -> null
        }
    }
    val text = if (duelInvite != null || sessionAlert != null || sharedPost != null) {
        ""
    } else {
        StructuredMessageFormatter.formatBubbleText(message)
    }
    val singleEmoji = remember(text, message.attachment, message.isForwarded, message.replyToContent, sharedPost, duelInvite, sessionAlert, isDeleted) {
        SingleEmojiDetector.extractSingleEmoji(
            text = text,
            hasAttachment = message.attachment != null,
            isForwarded = message.isForwarded,
            hasReply = !message.replyToContent.isNullOrBlank(),
            hasSharedPost = sharedPost != null || duelInvite != null || sessionAlert != null,
            isDeleted = isDeleted,
        )
    }
    val description = buildString {
        append(if (message.isMine) "پیام شما" else "پیام دریافتی")
        append("، ")
        append(statusDescription(message.status))
    }
    val density = LocalDensity.current
    val coordinatesHolder = remember(message.stableKey) { LayoutCoordinatesHolder() }
    val chatEntryMode = LocalChatEntryMode.current
    val entryProgress = if (animateEntry && !message.isMine && chatEntryMode != "off") {
        val anim = remember(message.stableKey) { Animatable(0f) }
        LaunchedEffect(message.stableKey, chatEntryMode) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (chatEntryMode == "minimal") {
                        TELEGRAM_MINIMAL_SEND_DURATION_MS
                    } else {
                        TELEGRAM_SEND_DURATION_MS
                    },
                    easing = TelegramSendBezierEasing,
                ),
            )
        }
        anim.value
    } else {
        1f
    }
    val msgHaptics = rememberVistaHaptics()
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entryProgress
                translationY = (1f - entryProgress) * with(density) { 8.dp.toPx() }
            }
            // The whole row (including the empty space beside the bubble) owns the
            // selection gesture and highlight, so a long-press or tap anywhere on the
            // line selects the message, like a full-width list row.
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (selectionMode) {
                        onToggleSelection()
                    } else if (!isDeleted) {
                        onOpenContextMenu(coordinatesHolder.coordinates?.takeIf { it.isAttached }?.boundsInWindow())
                    }
                },
                onLongClick = {
                    if (!selectionMode && message.content != MessageContent.Deleted) {
                        msgHaptics.heavy()
                        onLongPress()
                    }
                },
            )
            .absolutePadding(
                left = if (message.isMine) 12.dp else 6.dp,
                right = if (message.isMine) 6.dp else 12.dp,
                top = if (isFirstInGroup) TELEGRAM_X_BUBBLE_GAP_SEPARATE_DP.dp else TELEGRAM_X_BUBBLE_GAP_MERGED_DP.dp,
                bottom = if (isLastInGroup) TELEGRAM_X_BUBBLE_GAP_SEPARATE_DP.dp else TELEGRAM_X_BUBBLE_GAP_MERGED_DP.dp,
            ),
        horizontalArrangement = if (message.isMine) Arrangement.Start else Arrangement.End,
    ) {
            val cornerRounding = messageBubbleCornerRounding(
                isMine = message.isMine,
                groupPosition = MessageGroupPosition(isFirstInGroup, isLastInGroup),
            )
            val shape = telegramXOuterCornerRadii(cornerRounding).toAbsoluteShape()
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val isSingleEmoji = singleEmoji != null
            val isRichCardBubble = !isDeleted &&
                !isSingleEmoji &&
                (duelInvite != null || sessionAlert != null || sharedPost != null)
            val isVisualMediaBubble = !isDeleted &&
                !isSingleEmoji &&
                !isRichCardBubble &&
                message.attachment?.kind in setOf(
                    AttachmentKind.IMAGE,
                    AttachmentKind.GIF,
                    AttachmentKind.VIDEO,
                )
            val isFileOrAudioBubble = !isDeleted &&
                !isSingleEmoji &&
                !isRichCardBubble &&
                message.attachment?.kind in setOf(
                    AttachmentKind.VOICE,
                    AttachmentKind.AUDIO,
                    AttachmentKind.DOCUMENT,
                    AttachmentKind.UNKNOWN,
                )
            val hasHeaderAbove = !isDeleted &&
                (message.isForwarded || !message.replyToContent.isNullOrBlank())
            val hasFooterBelowMedia = text.isNotBlank() ||
                (!isDeleted && message.reactions.isNotEmpty())
            val drawTimeOverMedia = isVisualMediaBubble && !hasFooterBelowMedia && !message.isForwarded
            val base = Modifier
                .then(
                    if (isSingleEmoji) {
                        Modifier.wrapContentSize()
                    } else {
                        Modifier
                            .widthIn(min = 60.dp, max = screenWidth * 0.82f)
                            .shadow(
                                elevation = 1.dp,
                                shape = shape,
                                ambientColor = Color.Black.copy(alpha = 0.06f),
                                spotColor = Color.Black.copy(alpha = 0.08f),
                            )
                            .clip(shape)
                    }
                )
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
                            duelInvite != null -> {
                                val previewEntry = duelPreviews[duelInvite.matchId]
                                val preview = previewEntry?.preview
                                val isExpired =
                                    previewEntry?.status == ir.coffevista.vista_native.features.chat.presentation.components.DuelPreviewLoadStatus.EXPIRED_OR_NOT_FOUND ||
                                        preview?.isEffectivelyExpired() == true
                                val isCancelled = preview?.status == "cancelled"
                                val isLockedOut =
                                    preview != null && !preview.isParticipant && !preview.canJoin
                                when {
                                    isExpired && message.isMine -> onOpenGame("/game/duel/create")
                                    isExpired || isCancelled || isLockedOut -> Unit
                                    preview != null && (preview.isParticipant || !preview.canJoin) ->
                                        onOpenGame("/game/match/${duelInvite.matchId}")
                                    else -> onOpenGame(duelInvite.targetGamePath)
                                }
                            }
                            sharedPost != null -> onOpenPost(sharedPost.postId)
                            attachment?.kind == AttachmentKind.AUDIO -> {
                                if (task?.state == DownloadState.DOWNLOADING) onPauseDownload(task.messageId)
                                else if (task != null && task.state in setOf(DownloadState.PAUSED, DownloadState.FAILED, DownloadState.CANCELLED, DownloadState.QUEUED)) onResumeDownload(task.messageId)
                                else if (attachment.transferState == TransferState.COMPLETE && attachment.localUri.isNullOrBlank() && task?.state != DownloadState.COMPLETE && !message.isMine && !attachment.remoteUrl.isNullOrBlank()) onStartDownload(message)
                                else onOpenAudioPlayer()
                            }
                            attachment?.kind == AttachmentKind.VOICE -> {
                                onOpenContextMenu(coordinatesHolder.coordinates?.takeIf { it.isAttached }?.boundsInWindow())
                            }
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
                    onLongClick = {
                        msgHaptics.heavy()
                        onLongPress()
                    },
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
                        .then(
                            if (isSingleEmoji) {
                                base
                            } else if (message.isMine) {
                                base.background(Brush.linearGradient(listOf(VistaBrandColors.Indigo, VistaBrandColors.VioletDeep)))
                            } else {
                                base.background(MaterialTheme.colorScheme.surfaceVariant)
                            }
                        ).then(
                            if (selected) Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                shape = if (isSingleEmoji) RoundedCornerShape(16.dp) else shape,
                            ) else Modifier
                        )
                        .onGloballyPositioned { coordinates ->
                            coordinatesHolder.coordinates = coordinates
                            onBubbleBoundsInRootChanged?.invoke(coordinates.boundsInRoot())
                        }
                        .then(
                            when {
                                isSingleEmoji -> Modifier.padding(0.dp)
                                isVisualMediaBubble -> Modifier.padding(2.dp)
                                isRichCardBubble -> Modifier.padding(3.dp)
                                isFileOrAudioBubble -> Modifier.padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 6.dp)
                                else -> Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 6.dp)
                            },
                        ),
                ) {
                if (!isDeleted && message.isForwarded) {
                    Row(
                        if (isVisualMediaBubble || isRichCardBubble) {
                            Modifier.padding(start = 9.dp, top = 5.dp, end = 9.dp, bottom = 4.dp)
                        } else {
                            Modifier.padding(bottom = 4.dp)
                        },
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
                    val replyKind = message.replyToKind?.trim().orEmpty()
                    val isNoteReply = rawReplyTarget.startsWith("note:") ||
                        replySender.startsWith("یادداشت") ||
                        replyKind.equals("note", ignoreCase = true)
                    val isStoryReply = rawReplyTarget.startsWith("story:") ||
                        replySender.startsWith("استوری") ||
                        replyKind.equals("storyReply", ignoreCase = true) ||
                        replyKind.equals("story_reply", ignoreCase = true) ||
                        StoryReplyParser.tryParse(message.replyToContent, replyKind) != null

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isVisualMediaBubble || isRichCardBubble) {
                                    Modifier.padding(start = 6.dp, top = 4.dp, end = 6.dp, bottom = 4.dp)
                                } else {
                                    Modifier.padding(bottom = 6.dp)
                                },
                            )
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
                            val replyDisplayContent = remember(message.replyToContent, message.replyToKind, isStoryReply, isNoteReply) {
                                StructuredMessageFormatter.formatReplyQuotePreview(
                                    rawReplyContent = message.replyToContent,
                                    replyToKind = message.replyToKind,
                                    isStoryReply = isStoryReply,
                                    isNoteReply = isNoteReply,
                                )
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
                val mediaClipShape = remember(cornerRounding, hasHeaderAbove, hasFooterBelowMedia) {
                    telegramXInnerCornerRadii(
                        cornerRounding = cornerRounding,
                        insetDp = 2f,
                        roundTop = !hasHeaderAbove,
                        roundBottom = !hasFooterBelowMedia,
                    ).toAbsoluteShape()
                }
                val cardShape = remember(cornerRounding, hasHeaderAbove) {
                    telegramXInnerCornerRadii(
                        cornerRounding = cornerRounding,
                        insetDp = 3f,
                        roundTop = !hasHeaderAbove,
                        roundBottom = false,
                        flatCornerDp = 10f,
                    ).toAbsoluteShape()
                }
                val overlayMetaFooter: @Composable () -> Unit = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (selectionMode) {
                                        onToggleSelection()
                                    } else if (!isDeleted) {
                                        onOpenContextMenu(coordinatesHolder.coordinates?.takeIf { it.isAttached }?.boundsInWindow())
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            if (isSecretConversation && secretAutoDeleteSeconds > 0) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "تایمر خودکار",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(Modifier.width(2.5.dp))
                            }
                            if (message.editedAtEpochMillis != null) {
                                Text(
                                    "ویرایش",
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic,
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                formatTime(message.createdAtEpochMillis),
                                color = Color.White,
                                fontSize = 11.sp,
                            )
                            if (message.isMine) {
                                Spacer(Modifier.width(3.dp))
                                MessageStatusMark(message.status)
                            }
                        }
                    }
                }
                if (!isDeleted) message.attachment?.let { attachment ->
                    AttachmentBubble(
                        message = message,
                        attachment = attachment,
                        downloadTask = downloadTask,
                        settingsPreferences = settingsPreferences,
                        connectionTransport = connectionTransport,
                        senderDisplayName = senderDisplayName,
                        isSecretConversation = isSecretConversation,
                        voiceQueue = voiceQueue,
                        musicQueue = musicQueue,
                        onOpenAudioPlayer = onOpenAudioPlayer,
                        onCancel = { onCancelTransfer(message) },
                        onStartDownload = { onStartDownload(message) },
                        onPauseDownload = onPauseDownload,
                        onResumeDownload = onResumeDownload,
                        onCancelDownload = onCancelDownload,
                        onAttachmentClick = onAttachmentClick,
                        mediaClipShape = mediaClipShape,
                        overlayMetaFooter = if (drawTimeOverMedia) overlayMetaFooter else null,
                    )
                    if (text.isNotBlank()) {
                        Spacer(Modifier.height(if (isVisualMediaBubble) 4.dp else 6.dp))
                    } else if (isVisualMediaBubble && !drawTimeOverMedia) {
                        Spacer(Modifier.height(2.dp))
                    }
                }
                val messageTextDir = resolveMessageTextDirection(text)
                val isRtlMessage = messageTextDir == androidx.compose.ui.text.style.TextDirection.Rtl ||
                    messageTextDir == androidx.compose.ui.text.style.TextDirection.ContentOrRtl

                val metaFooter: @Composable () -> Unit = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (selectionMode) {
                                    onToggleSelection()
                                } else if (!isDeleted) {
                                    onOpenContextMenu(coordinatesHolder.coordinates?.takeIf { it.isAttached }?.boundsInWindow())
                                }
                            },
                        ) {
                            if (isSecretConversation && secretAutoDeleteSeconds > 0) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "تایمر خودکار",
                                    tint = if (message.isMine) Color.White.copy(alpha = 0.85f) else SecretChatGreen,
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

                if (duelInvite != null) {
                    val previewState = duelPreviews[duelInvite.matchId]
                    Column(modifier = Modifier.align(AbsoluteAlignment.Right)) {
                        DuelInviteCard(
                            invite = duelInvite,
                            isMine = message.isMine,
                            preview = previewState?.preview,
                            previewStatus = previewState?.status ?: DuelPreviewLoadStatus.IDLE,
                            onRequestPreview = onRequestDuelPreview,
                            onStartGame = onOpenGame,
                            shape = cardShape,
                        )
                        Box(
                            Modifier
                                .align(AbsoluteAlignment.Right)
                                .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 2.dp),
                        ) {
                            metaFooter()
                        }
                    }
                } else if (sessionAlert != null) {
                    Column(modifier = Modifier.align(AbsoluteAlignment.Right)) {
                        ir.coffevista.vista_native.features.chat.presentation.components.SessionAlertCard(
                            alert = sessionAlert,
                            isActionPending = sessionAlert.sessionId in pendingSessionAlertActions,
                            onAction = { action ->
                                onSessionAlertAction(message, sessionAlert.sessionId, action)
                            },
                            shape = cardShape,
                        )
                        Box(
                            Modifier
                                .align(AbsoluteAlignment.Right)
                                .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 2.dp),
                        ) {
                            metaFooter()
                        }
                    }
                } else if (sharedPost != null) {
                    Column(modifier = Modifier.align(AbsoluteAlignment.Right)) {
                        SharedPostCard(
                            post = sharedPost,
                            isMine = message.isMine,
                            onViewPost = onOpenPost,
                            shape = cardShape,
                        )
                        Box(
                            Modifier
                                .align(AbsoluteAlignment.Right)
                                .padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 2.dp),
                        ) {
                            metaFooter()
                        }
                    }
                } else if (singleEmoji != null) {
                    VistaAnimatedEmojiBubble(
                        emoji = singleEmoji,
                        isMine = message.isMine,
                        metaFooter = metaFooter,
                    )
                } else if (text.isNotBlank()) {
                    ChatTextBubbleLayout(
                        text = {
                            VistaEmojiText(
                                text = text,
                                style = TextStyle(
                                    color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    textDirection = messageTextDir,
                                ),
                                emojiSize = 20.dp,
                                highlightQuery = searchQuery,
                                highlightColor = if (message.isMine) {
                                    Color.White.copy(alpha = 0.32f)
                                } else {
                                    VistaBrandColors.Indigo.copy(alpha = 0.28f)
                                },
                            )
                        },
                        footer = metaFooter,
                        modifier = Modifier
                            .align(AbsoluteAlignment.Right)
                            .then(
                                if (isVisualMediaBubble) {
                                    Modifier.padding(start = 10.dp, end = 10.dp, bottom = 4.dp)
                                } else {
                                    Modifier
                                },
                            ),
                        layoutDirection = if (isRtlMessage) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    )
                } else if (!drawTimeOverMedia) {
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.Right)
                            .then(
                                if (isVisualMediaBubble) {
                                    Modifier.padding(start = 10.dp, top = 2.dp, end = 10.dp, bottom = 4.dp)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        metaFooter()
                    }
                }
                val previewUrl = remember(text, duelInvite) {
                    if (!isDeleted && duelInvite == null && text.isNotBlank()) extractFirstUrl(text) else null
                }
                if (previewUrl != null) {
                    Spacer(Modifier.height(6.dp))
                    ChatLinkPreviewCard(
                        url = previewUrl,
                        isMine = message.isMine,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .then(
                                if (isVisualMediaBubble || isRichCardBubble) Modifier.padding(horizontal = 9.dp)
                                else Modifier
                            ),
                    )
                }
                MessageReactionsRow(
                    reactions = if (isDeleted) emptyMap() else message.reactions,
                    accountId = message.accountId,
                    insideMineBubble = message.isMine,
                    selectionMode = selectionMode,
                    onToggleSelection = onToggleSelection,
                    onReact = onReact,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .then(
                            if (isVisualMediaBubble || isRichCardBubble) Modifier.padding(start = 9.dp, end = 9.dp, bottom = 3.dp)
                            else Modifier
                        ),
                )
            }
        }
    }
}

@Composable
private fun MessageReactionsRow(
    reactions: Map<String, Set<String>>,
    accountId: String,
    insideMineBubble: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = reactions,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.92f))
                .togetherWith(fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.92f))
        },
        label = "message-reactions",
    ) { currentReactions ->
        if (currentReactions.isNotEmpty()) {
            LazyRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(currentReactions.entries.toList(), key = { it.key }) { reaction ->
                    val isMyReaction = reaction.value.any { it.equals(accountId, ignoreCase = true) }
                    val chipScale by animateFloatAsState(
                        targetValue = if (isMyReaction) 1.06f else 1f,
                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                        label = "reaction-scale",
                    )
                    val chipColor by animateColorAsState(
                        targetValue = when {
                            isMyReaction && insideMineBubble -> Color.White.copy(alpha = 0.30f)
                            isMyReaction -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                            insideMineBubble -> Color.White.copy(alpha = 0.14f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        animationSpec = tween(180),
                        label = "reaction-color",
                    )
                    val chipBorderColor = when {
                        isMyReaction && insideMineBubble -> Color.White.copy(alpha = 0.70f)
                        isMyReaction -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        else -> Color.Transparent
                    }
                    Row(
                        modifier = Modifier
                            .graphicsLayer { scaleX = chipScale; scaleY = chipScale }
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipColor)
                            .border(
                                width = if (isMyReaction) 1.dp else 0.dp,
                                color = chipBorderColor,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable(role = Role.Button) {
                                if (selectionMode) onToggleSelection() else onReact(reaction.key)
                            }
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        VistaEmoji(emoji = reaction.key, size = 13.dp)
                        Text(
                            "${reaction.value.size}",
                            fontSize = 11.sp,
                            fontWeight = if (isMyReaction) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (insideMineBubble) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
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
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val bannerShape = RoundedCornerShape(18.dp)
    val bannerTint = if (isDark) Color(0xFF1A222C) else Color(0xFFFFFFFF)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .then(
                LocalVistaGlassState.current?.let { gs ->
                    Modifier.vistaGlass(
                        state = gs,
                        shape = bannerShape,
                        tint = bannerTint,
                        isDark = isDark,
                        blurRadius = VistaGlassTokens.PillBlur,
                        tintAlphaOverride = if (isDark) 0.58f else 0.62f,
                        drawHighlight = true,
                        rimWidth = 0.6.dp,
                        shadowElevation = 3.dp,
                    )
                } ?: Modifier
                    .shadow(3.dp, bannerShape)
                    .background(
                        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                        else Color.White.copy(alpha = 0.96f),
                        bannerShape,
                    )
                    .border(
                        0.5.dp,
                        if (isDark) Color(0x22FFFFFF) else Color(0x18000000),
                        bannerShape,
                    ),
            )
            .clip(bannerShape)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                member.displayName,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            ChatUserVerificationBadge(
                                isVerified = member.isVerified,
                                verificationType = member.verificationType,
                                role = member.role,
                                size = 14.dp,
                            )
                        }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        candidate.displayName,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    ChatUserVerificationBadge(
                                        isVerified = candidate.isVerified,
                                        verificationType = candidate.verificationType,
                                        role = candidate.role,
                                        size = 14.dp,
                                    )
                                }
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
        if (message.isMine &&
            message.content is MessageContent.Text &&
            !looksLikeJsonPayload(message.content.value) &&
            !StructuredMessageFormatter.isStructuredOrJsonPayload(message.content.value, message.transportType) &&
            message.attachment == null &&
            message.deletedAtEpochMillis == null
        ) {
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
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                conversation.title,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (conversation.type != ConversationType.GROUP) {
                                ChatUserVerificationBadge(
                                    isVerified = conversation.isVerified,
                                    verificationType = conversation.verificationType,
                                    role = conversation.role,
                                    size = 15.dp,
                                )
                            }
                        }
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

internal fun Message.previewText(): String =
    StructuredMessageFormatter.formatMessagePreview(this, sharedPostWithColon = true)

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

private fun shouldAllowRemoteAttachmentPreview(
    attachment: ir.coffevista.vista_native.features.chat.domain.model.Attachment,
    settings: ir.coffevista.vista_native.core.datastore.SettingsPreferences,
    transport: ir.coffevista.vista_native.core.common.ConnectionTransport,
): Boolean {
    val isImageOrGifOrVoice = attachment.kind in setOf(
        AttachmentKind.IMAGE,
        AttachmentKind.GIF,
        AttachmentKind.VOICE,
    )
    return ir.coffevista.vista_native.core.datastore.MediaDataPolicy.shouldAutoDownloadAttachment(
        transport = transport,
        isImageOrGifOrVoice = isImageOrGifOrVoice,
        sizeBytes = attachment.sizeBytes,
        prefs = settings,
    )
}

@Composable
private fun AttachmentBubble(
    message: Message,
    attachment: ir.coffevista.vista_native.features.chat.domain.model.Attachment,
    downloadTask: DownloadTask? = null,
    settingsPreferences: ir.coffevista.vista_native.core.datastore.SettingsPreferences =
        ir.coffevista.vista_native.core.datastore.SettingsPreferences(),
    connectionTransport: ir.coffevista.vista_native.core.common.ConnectionTransport =
        ir.coffevista.vista_native.core.common.ConnectionTransport.WIFI,
    senderDisplayName: String = if (message.isMine) "شما" else "مخاطب",
    isSecretConversation: Boolean = false,
    voiceQueue: List<ChatAudioTrack> = emptyList(),
    musicQueue: List<ChatAudioTrack> = emptyList(),
    onOpenAudioPlayer: () -> Unit = {},
    onCancel: () -> Unit,
    onStartDownload: () -> Unit = {},
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onAttachmentClick: (ir.coffevista.vista_native.features.chat.domain.model.Attachment) -> Unit = {},
    mediaClipShape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    overlayMetaFooter: (@Composable () -> Unit)? = null,
) {
    val foreground = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val isGif = attachment.kind == AttachmentKind.GIF
    val completedDownloadUri = downloadTask
        ?.takeIf { it.state == DownloadState.COMPLETE }
        ?.localUri
        ?.takeIf { it.isNotBlank() }
    val rawLocalUri = completedDownloadUri ?: attachment.localUri
    val isEncryptedLocalFile = rawLocalUri?.let {
        it.contains("sec_up_") || it.contains("enc_chat_") || it.contains("secret_pending_uploads")
    } == true
    val usableLocalUri = rawLocalUri?.takeUnless { it.isBlank() || isEncryptedLocalFile }
    var localLoadFailed by remember(usableLocalUri) { mutableStateOf(false) }
    val remoteUrl = attachment.remoteUrl?.takeIf { it.isNotBlank() }
    val activeLocalUri = usableLocalUri?.takeUnless {
        localLoadFailed && !isSecretConversation && remoteUrl != null
    }
    val isDownloaded = isGif ||
        !usableLocalUri.isNullOrBlank() ||
        (!isSecretConversation && message.isMine)
    val isDownloading = !isGif && (downloadTask?.state == DownloadState.DOWNLOADING || downloadTask?.state == DownloadState.QUEUED)
    val isFailed = !isGif && (downloadTask?.state == DownloadState.FAILED || downloadTask?.state == DownloadState.CANCELLED)
    val effectiveLocalUri = activeLocalUri
    val allowRemotePreview = isGif ||
        (!isSecretConversation && message.isMine) ||
        !usableLocalUri.isNullOrBlank() ||
        shouldAllowRemoteAttachmentPreview(attachment, settingsPreferences, connectionTransport)
    val mediaModel = effectiveLocalUri?.takeIf { it.isNotBlank() }
        ?: if (isSecretConversation || !allowRemotePreview) null else remoteUrl

    val isActiveUpload = attachment.transferState == TransferState.UPLOADING ||
        attachment.transferState == TransferState.QUEUED

    Column {
        when (attachment.kind) {
            AttachmentKind.IMAGE, AttachmentKind.GIF -> {
                val imageBlurRadius by animateDpAsState(
                    targetValue = if (isDownloaded) 0.dp else 25.dp,
                    label = "chat_image_blur",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 280.dp)
                        .clip(mediaClipShape)
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
                        onError = {
                            if (!usableLocalUri.isNullOrBlank() && mediaModel == usableLocalUri && remoteUrl != null) {
                                localLoadFailed = true
                            }
                        },
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
                                    .align(AbsoluteAlignment.TopLeft)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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
                    if (overlayMetaFooter != null) {
                        Box(
                            modifier = Modifier
                                .align(AbsoluteAlignment.BottomRight)
                                .padding(6.dp),
                        ) {
                            overlayMetaFooter()
                        }
                    }
                }
            }
            AttachmentKind.VIDEO -> {
                val videoBlurRadius by animateDpAsState(
                    targetValue = if (isDownloaded) 0.dp else 25.dp,
                    label = "chat_video_blur",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 260.dp)
                        .clip(mediaClipShape)
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
                            onError = {
                                if (!usableLocalUri.isNullOrBlank() && mediaModel == usableLocalUri && remoteUrl != null) {
                                    localLoadFailed = true
                                }
                            },
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
                                .align(AbsoluteAlignment.TopLeft)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (overlayMetaFooter != null) {
                        Box(
                            modifier = Modifier
                                .align(AbsoluteAlignment.BottomRight)
                                .padding(6.dp),
                        ) {
                            overlayMetaFooter()
                        }
                    }
                }
            }
            AttachmentKind.VOICE -> {
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
                        fallbackUrl = attachment.remoteUrl,
                        senderName = senderDisplayName,
                        conversationId = message.conversationId,
                        conversationTitle = voiceQueue.firstOrNull()?.conversationTitle,
                        createdAtEpochMillis = message.createdAtEpochMillis,
                        sizeBytes = attachment.sizeBytes,
                        voiceQueue = voiceQueue,
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
                            Text("پیام صوتی", color = foreground, maxLines = 1)
                            Text(formatDuration(attachment.durationSeconds), color = foreground.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
            AttachmentKind.AUDIO -> {
                val audioUrl = effectiveLocalUri ?: attachment.remoteUrl
                if (!audioUrl.isNullOrBlank()) {
                    MusicPlayerBubble(
                        attachment = attachment,
                        messageId = message.stableKey,
                        isOutgoing = message.isMine,
                        isDownloaded = isDownloaded,
                        downloadTask = downloadTask,
                        senderName = senderDisplayName,
                        conversationId = message.conversationId,
                        conversationTitle = musicQueue.firstOrNull()?.conversationTitle,
                        createdAtEpochMillis = message.createdAtEpochMillis,
                        musicQueue = musicQueue,
                        onStartDownload = onStartDownload,
                        onCancelDownload = onCancelDownload,
                        onOpenFullPlayer = onOpenAudioPlayer,
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "موسیقی", tint = foreground, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(resolveMusicTitle(attachment.audioTitle, attachment.fileName), color = foreground, maxLines = 1)
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
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            when {
                                isDownloaded -> onAttachmentClick(attachment.copy(localUri = effectiveLocalUri))
                                isDownloading -> downloadTask?.let { onCancelDownload(it.messageId) }
                                isFailed -> downloadTask?.let { onResumeDownload(it.messageId) } ?: onStartDownload()
                                else -> onStartDownload()
                            }
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                when {
                                    isDownloading -> Color.Transparent
                                    isDownloaded -> if (message.isMine) Color.White.copy(alpha = 0.24f) else VistaBrandColors.Indigo.copy(alpha = 0.14f)
                                    else -> if (message.isMine) Color.White.copy(alpha = 0.24f) else VistaBrandColors.Indigo
                                },
                                CircleShape,
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
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = "فایل",
                                    tint = if (message.isMine) Color.White else VistaBrandColors.Indigo,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName ?: "فایل پیوست",
                            color = foreground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
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
                            color = foreground.copy(alpha = 0.74f),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
    onSaveToDevice: (suspend () -> Result<String>)? = null,
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
            isMusic = attachment.kind == AttachmentKind.AUDIO,
            onDismiss = onDismiss,
            onSaveToDevice = onSaveToDevice,
        )
        AttachmentKind.DOCUMENT, AttachmentKind.UNKNOWN -> DocumentAttachmentViewer(
            source = source,
            fileName = attachment.fileName ?: "فایل",
            mimeType = attachment.mimeType,
            onDismiss = onDismiss,
            onSaveToDevice = onSaveToDevice,
        )
    }
}

@Composable
private fun AudioAttachmentPlayer(
    source: String,
    title: String,
    isMusic: Boolean = false,
    onDismiss: () -> Unit,
    onSaveToDevice: (suspend () -> Result<String>)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember(source) { mutableStateOf(false) }
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
            if (onSaveToDevice != null) {
                OutlinedButton(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            val feedback = onSaveToDevice().getOrElse { it.message ?: "ذخیره انجام نشد" }
                            isSaving = false
                            android.widget.Toast.makeText(context, feedback, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            isSaving -> "در حال ذخیره..."
                            isMusic -> "ذخیره موسیقی"
                            else -> "ذخیره صدا"
                        },
                    )
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
    onSaveToDevice: (suspend () -> Result<String>)? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var error by remember(source) { mutableStateOf<String?>(null) }
    var isSaving by remember(source) { mutableStateOf(false) }
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
            if (onSaveToDevice != null) {
                OutlinedButton(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            val result = onSaveToDevice()
                            isSaving = false
                            result.onSuccess { msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                error = err.toSafeUserError("ذخیره فایل انجام نشد")
                            }
                        }
                    },
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaving) "در حال ذخیره..." else "ذخیره در دانلودها")
                }
            }
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
    onInputFieldBoundsChanged: (Rect) -> Unit = {},
    onReplyBannerBoundsChanged: (Rect?) -> Unit = {},
) {
    var composerValue by remember(conversationId) {
        mutableStateOf(TextFieldValue("", TextRange.Zero))
    }

    LaunchedEffect(replyingTo) {
        if (replyingTo == null) {
            onReplyBannerBoundsChanged(null)
        }
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
    val latestEmojiPanelOpen = androidx.compose.runtime.rememberUpdatedState(isEmojiPanelOpen)

    Column(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = replyingTo != null,
            enter = slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(180)),
        ) {
            replyingTo?.let { message ->
                val rawReplyTarget = message.replyToMessageId?.trim().orEmpty()
                val replySender = message.replyDisplayName(title)
                val existingReplySender = message.replyToSenderName?.trim().orEmpty()
                val rawBody = when (val c = message.content) {
                    is MessageContent.Text -> c.value
                    is MessageContent.Structured -> c.payload
                    else -> null
                }
                val isNoteReply = rawReplyTarget.startsWith("note:") || existingReplySender.startsWith("یادداشت")
                val isStoryReply = rawReplyTarget.startsWith("story:") ||
                    existingReplySender.startsWith("استوری") ||
                    message.transportType.equals("storyReply", ignoreCase = true) ||
                    message.transportType.equals("story_reply", ignoreCase = true) ||
                    StoryReplyParser.tryParse(rawBody, message.transportType) != null

                ComposerContextBanner(
                    label = if (isStoryReply) "پاسخ به استوری" else if (isNoteReply) "پاسخ به یادداشت" else "پاسخ به $replySender",
                    preview = message.previewText(),
                    badgeIcon = if (isStoryReply) Icons.Outlined.AutoStories else if (isNoteReply) Icons.Outlined.EditNote else null,
                    badgeText = if (isStoryReply) "استوری" else if (isNoteReply) "یادداشت" else null,
                    onDismiss = onClearReply,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        onReplyBannerBoundsChanged(coords.boundsInRoot())
                    },
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
                            if (!latestEmojiPanelOpen.value) {
                                composerController.openKeyboard()
                            }
                        }
                        else -> onSendReply(currentText, reply) {
                            composerValue = TextFieldValue("", TextRange.Zero)
                            onClearReply()
                            if (!latestEmojiPanelOpen.value) {
                                composerController.openKeyboard()
                            }
                        }
                    }
                    else -> if (currentText.isNotBlank()) {
                        onEdit(editing, currentText)
                        composerValue = TextFieldValue("", TextRange.Zero)
                        onClearEdit()
                        if (!latestEmojiPanelOpen.value) {
                            composerController.openKeyboard()
                        }
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
            onInputFieldBoundsChanged = onInputFieldBoundsChanged,
        )
    }
}

@Composable
internal fun TelegramSmileyIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF7D8B99),
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        // Outer circle outline
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth),
        )
        // Eyes
        val eyeRadius = 1.3.dp.toPx()
        val eyeOffsetY = -2.2.dp.toPx()
        val eyeOffsetX = 4.2.dp.toPx()
        drawCircle(
            color = tint,
            radius = eyeRadius,
            center = Offset(center.x - eyeOffsetX, center.y + eyeOffsetY),
        )
        drawCircle(
            color = tint,
            radius = eyeRadius,
            center = Offset(center.x + eyeOffsetX, center.y + eyeOffsetY),
        )
        // Smile curve
        val smileWidth = 8.5.dp.toPx()
        val smileHeight = 4.3.dp.toPx()
        val smileTop = center.y + 0.6.dp.toPx()
        val path = Path().apply {
            moveTo(center.x - smileWidth / 2f, smileTop)
            quadraticTo(center.x, smileTop + smileHeight, center.x + smileWidth / 2f, smileTop)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
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
    onInputFieldBoundsChanged: (Rect) -> Unit = {},
) {
    val attachmentRotation = remember { Animatable(0f) }
    val composerScope = rememberCoroutineScope()
    var isVoiceActive by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val composerGlass = LocalVistaGlassState.current
    val capsuleBg = if (isDark) Color(0xFF1A222C) else Color.White
    val iconTint = if (isDark) Color(0xFF8C9AA9) else Color(0xFF686E74)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chat-composer-surface")
                .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val isSingleLine = value.text.isBlank() || isVoiceActive
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isSingleLine) Modifier.height(46.dp)
                        else Modifier.defaultMinSize(minHeight = 46.dp)
                    )
                    .then(
                        if (composerGlass != null) {
                            Modifier.vistaGlass(
                                state = composerGlass,
                                shape = RoundedCornerShape(23.dp),
                                tint = capsuleBg,
                                isDark = isDark,
                                blurRadius = VistaGlassTokens.PillBlur,
                                tintAlphaOverride = if (isDark) 0.58f else 0.60f,
                                drawHighlight = true,
                                rimWidth = 0.6.dp,
                                shadowElevation = 4.dp,
                            )
                        } else {
                            Modifier
                                .shadow(if (isDark) 2.dp else 4.dp, RoundedCornerShape(23.dp))
                                .background(capsuleBg.copy(alpha = 0.94f))
                                .border(
                                    0.55.dp,
                                    if (isDark) Color(0x1AFFFFFF) else Color(0x12000000),
                                    RoundedCornerShape(23.dp),
                                )
                        },
                    ),
                shape = RoundedCornerShape(23.dp),
                color = Color.Transparent,
                shadowElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSingleLine) Modifier.height(46.dp)
                            else Modifier.defaultMinSize(minHeight = 46.dp)
                        ),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = if (isVoiceActive) 0f else 1f }
                            .padding(start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onEmoji,
                            modifier = Modifier
                                .focusProperties { canFocus = false }
                                .size(38.dp)
                                .zIndex(1f),
                        ) {
                            if (isEmojiPanelOpen) {
                                Icon(
                                    Icons.Default.Keyboard,
                                    contentDescription = "صفحه‌کلید",
                                    tint = iconTint,
                                    modifier = Modifier.size(23.dp),
                                )
                            } else {
                                TelegramSmileyIcon(tint = iconTint)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    onInputFieldBoundsChanged(coords.boundsInRoot())
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                ChatComposerField(
                                    value = value,
                                    onValueChange = onValueChange,
                                    onFocusText = onFocusText,
                                    controller = controller,
                                    isEmojiPanelOpen = isEmojiPanelOpen,
                                    focusRequester = focusRequester,
                                    modifier = Modifier.fillMaxWidth(),
                                    hint = "پیام",
                                    maxLines = 6,
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isBlank && attachmentsEnabled,
                            enter = scaleIn(tween(180, easing = FastOutSlowInEasing)) + fadeIn(tween(150)),
                            exit = scaleOut(tween(160, easing = FastOutSlowInEasing)) + fadeOut(tween(130)),
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
                                    .size(36.dp),
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = "پیوست فایل",
                                    tint = iconTint,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer { rotationZ = -45f + attachmentRotation.value },
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isNotBlank,
                                enter = scaleIn(
                                    initialScale = 0.55f,
                                    animationSpec = tween(160, easing = TelegramSendBezierEasing),
                                ) + fadeIn(tween(120)),
                                exit = scaleOut(
                                    targetScale = 0.55f,
                                    animationSpec = tween(120, easing = FastOutSlowInEasing),
                                ) + fadeOut(tween(100)),
                            ) {
                                val sendHaptics = rememberVistaHaptics()
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = Color.White),
                                        ) {
                                            sendHaptics.confirm()
                                            onSend()
                                            if (!isEmojiPanelOpen) {
                                                controller.openKeyboard()
                                            }
                                        }
                                        .semantics { contentDescription = "ارسال پیام" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBlank,
                        enter = scaleIn(
                            initialScale = 0.75f,
                            animationSpec = tween(150, easing = TelegramSendBezierEasing),
                        ) + fadeIn(tween(120)),
                        exit = androidx.compose.animation.ExitTransition.None,
                    ) {
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
    val isDark = isSystemInDarkTheme()
    val chipShape = RoundedCornerShape(18.dp)
    val chipTint = if (isDark) Color(0xFF1A222C) else Color(0xFFFFFFFF)
    Text(
        text = localDayLabel(epochMillis),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .then(
                LocalVistaGlassState.current?.let { gs ->
                    Modifier.vistaGlass(
                        state = gs,
                        shape = chipShape,
                        tint = chipTint,
                        isDark = isDark,
                        blurRadius = VistaGlassTokens.PillBlur,
                        tintAlphaOverride = if (isDark) 0.58f else 0.62f,
                        drawHighlight = true,
                        rimWidth = 0.6.dp,
                        shadowElevation = 3.dp,
                    )
                } ?: Modifier
                    .shadow(3.dp, chipShape)
                    .background(chipTint.copy(alpha = 0.90f), chipShape),
            )
            .clip(chipShape)
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
private fun SecuritySystemDivider(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
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
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VistaViuStateIllustration(
            kind = inferVistaViuStateKind(message, isError = true),
            size = 144.dp,
        )
        Spacer(Modifier.height(10.dp))
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
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
    // No strong directional character (pure emoji or punctuation) defaults to LTR
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
    val audioExtensions = setOf(".mp3", ".m4a", ".aac", ".wav", ".ogg", ".flac")
    val kind = when {
        mimeType.equals("image/gif", true) || lowerName.endsWith(".gif") -> AttachmentKind.GIF
        mimeType.startsWith("image/") -> AttachmentKind.IMAGE
        mimeType.startsWith("video/") -> AttachmentKind.VIDEO
        mimeType.startsWith("audio/") || audioExtensions.any(lowerName::endsWith) -> AttachmentKind.AUDIO
        else -> AttachmentKind.DOCUMENT
    }
    var durationSeconds: Int? = null
    var audioTitle: String? = null
    var audioArtist: String? = null
    if (kind == AttachmentKind.AUDIO || kind == AttachmentKind.VIDEO) {
        val retriever = android.media.MediaMetadataRetriever()
        runCatching {
            openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                val durMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                if (durMs != null && durMs > 0L) {
                    durationSeconds = (durMs / 1000L).toInt().coerceAtLeast(1)
                }
                if (kind == AttachmentKind.AUDIO) {
                    audioTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.trim()?.takeIf(String::isNotEmpty)
                    audioArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.trim()?.takeIf(String::isNotEmpty)
                }
            }
        }
        runCatching { retriever.release() }
    }
    val resolvedAudioTitle = if (kind == AttachmentKind.AUDIO) {
        audioTitle ?: resolvedName.substringBeforeLast('.').takeIf(String::isNotBlank)
    } else null
    return ChatAttachmentDraft(
        uri = uri.toString(),
        fileName = resolvedName,
        mimeType = mimeType,
        sizeBytes = resolvedSize,
        kind = kind,
        durationSeconds = durationSeconds,
        audioTitle = resolvedAudioTitle,
        audioArtist = audioArtist,
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
        when (val content = message.content) {
            is MessageContent.Structured -> SharedPostParser.tryParse(content.payload) != null
            is MessageContent.Text -> SharedPostParser.tryParse(content.value) != null
            else -> false
        } ||
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.conversation?.title.orEmpty(),
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ChatUserVerificationBadge(
                    isVerified = state.partnerProfile?.isVerified == true || state.conversation?.isVerified == true,
                    verificationType = state.partnerProfile?.verificationType ?: state.conversation?.verificationType,
                    role = state.partnerProfile?.role ?: state.conversation?.role,
                    size = 19.dp,
                    spacerWidth = 6.dp,
                )
            }

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
    val isDark = isSystemInDarkTheme()
    val pageBgColor = if (isDark) Color(0xFF0F141A) else Color(0xFFF2F4F7)
    val cardBgColor = if (isDark) Color(0xFF1A222C) else Color(0xFFFFFFFF)
    val cardBorderColor = if (isDark) Color(0x20FFFFFF) else Color(0x10000000)
    val detailsScrollState = rememberScrollState()
    val detailsGlassState = rememberVistaGlassState()
    LaunchedEffect(detailsScrollState.value) {
        detailsGlassState.invalidate()
    }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val expandedHeaderSpace = statusBarTop + 190.dp
    val heroAvatar = profile?.avatarUrl ?: conversation?.avatarUrl
    val presenceText = state.presence?.let { presence ->
        if (presence.isOnline) "آنلاین" else formatUserPresence(presence.lastOnlineAtEpochMillis)
    } ?: "آخرین بازدید به تازگی"
    val isPartnerOnline = state.presence?.isOnline == true

    BackHandler(onBack = onDismiss)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = pageBgColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBgColor)
                .padding(contentPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .vistaGlassSource(detailsGlassState)
                    .background(pageBgColor)
                    .verticalScroll(detailsScrollState),
            ) {
                Spacer(Modifier.height(expandedHeaderSpace))

                // 1. Delicate 4-Button Floating Capsules Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ir.coffevista.vista_native.features.chat.presentation.components.VistaProfileGlassActionButton(
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
                    ir.coffevista.vista_native.features.chat.presentation.components.VistaProfileGlassActionButton(
                        icon = if (conversation?.isMuted == true) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                        label = if (conversation?.isMuted == true) "صدادار" else "بی‌صدا",
                        modifier = Modifier.weight(1f),
                    ) {
                        onToggleMute()
                        scope.launch { snackbarHostState.showSnackbar("تنظیمات اعلان گفتگو به‌روزرسانی شد") }
                    }
                    ir.coffevista.vista_native.features.chat.presentation.components.VistaProfileGlassActionButton(
                        icon = Icons.Default.PersonOutline,
                        label = "پروفایل",
                        modifier = Modifier.weight(1f),
                    ) {
                        conversation?.peerId?.let { peerId ->
                            onDismiss()
                            onOpenProfile(peerId)
                        }
                    }
                    ir.coffevista.vista_native.features.chat.presentation.components.VistaProfileGlassActionButton(
                        icon = Icons.Default.Search,
                        label = "جستجو",
                        modifier = Modifier.weight(1f),
                    ) { onOpenSearch() }
                }

                Spacer(Modifier.height(10.dp))

                // 2. Grouped Info Card (Bio, Username with Copy action, Last Seen Status)
                if (state.isPartnerProfileLoading && profile == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                            .padding(horizontal = 14.dp)
                            .background(cardBgColor.copy(alpha = 0.65f), RoundedCornerShape(20.dp)),
                    )
                } else {
                    val cardShape = RoundedCornerShape(20.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .shadow(if (isDark) 1.dp else 2.dp, cardShape)
                            .background(cardBgColor, cardShape)
                            .border(0.55.dp, cardBorderColor, cardShape)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        profile?.bio?.takeIf(String::isNotBlank)?.let { bio ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = bio,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "بیوگرافی",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = cardBorderColor,
                            )
                        }
                        if (username.isNotBlank()) {
                            val localClipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        localClipboard.setText(AnnotatedString("@$username"))
                                        scope.launch { snackbarHostState.showSnackbar("نام کاربری در حافظه کپی شد") }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@$username",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "نام کاربری",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کپی نام کاربری",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3. Floating Segmented Liquid Glass Pill Tabs
                ir.coffevista.vista_native.features.chat.presentation.components.VistaSegmentedGlassTabs(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    modifier = Modifier.padding(horizontal = 14.dp),
                )

                Spacer(Modifier.height(10.dp))

                // 4. Shared Media / Files / Links / Audio / GIFs Card (non-nested so entire page scrolls smoothly)
                val mediaCardShape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .defaultMinSize(minHeight = 460.dp)
                        .shadow(if (isDark) 1.dp else 2.dp, mediaCardShape)
                        .background(cardBgColor, mediaCardShape)
                        .border(0.55.dp, cardBorderColor, mediaCardShape)
                        .clip(mediaCardShape)
                        .padding(4.dp),
                ) {
                    if (selectedTabIndex == 0 && mediaMessages.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            mediaMessages.chunked(3).forEach { rowMessages ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    rowMessages.forEach { message ->
                                        val attachment = message.attachment
                                        var localFailed by remember(attachment?.localUri) { mutableStateOf(false) }
                                        val displayModel = attachment?.localUri?.takeIf { it.isNotBlank() && !localFailed }
                                            ?: attachment?.remoteUrl
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    attachment?.let { att ->
                                                        onAttachmentClick(
                                                            if (localFailed) att.copy(localUri = null) else att,
                                                        )
                                                    }
                                                },
                                        ) {
                                            AsyncImage(
                                                model = displayModel,
                                                contentDescription = attachment?.fileName ?: "رسانه",
                                                contentScale = ContentScale.Crop,
                                                onError = {
                                                    if (!attachment?.localUri.isNullOrBlank() &&
                                                        displayModel == attachment?.localUri &&
                                                        !attachment?.remoteUrl.isNullOrBlank()
                                                    ) {
                                                        localFailed = true
                                                    }
                                                },
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
                                    repeat(3 - rowMessages.size) {
                                        Spacer(Modifier.weight(1f))
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
                            val internalNavigator = ir.coffevista.vista_native.core.designsystem.navigation.LocalVistaInternalLinkNavigator.current
                            Column(Modifier.fillMaxWidth()) {
                                selectedItems.forEach { message: Message ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (selectedTabIndex == 2) {
                                                    extractFirstUrl((message.content as? MessageContent.Text)?.value.orEmpty())?.let { url ->
                                                        if (internalNavigator?.openUrl(url) != true) {
                                                            runCatching { uriHandler.openUri(url) }
                                                        }
                                                    }
                                                } else {
                                                    message.attachment?.let(onAttachmentClick)
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                when (message.attachment?.kind) {
                                                    AttachmentKind.AUDIO, AttachmentKind.VOICE -> Icons.Default.Mic
                                                    AttachmentKind.GIF -> Icons.Default.EmojiEmotions
                                                    else -> Icons.Default.AttachFile
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                message.attachment?.fileName ?: message.previewText(),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Text(
                                                formatConversationTime(message.createdAtEpochMillis),
                                                maxLines = 1,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    VistaViuStateIllustration(
                                        kind = VistaViuStateKind.Empty,
                                        size = 120.dp,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = emptyText,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        fontSize = 14.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(220.dp))
            }

            // Pinned Collapsing Liquid Teardrop / Metaball Notch Header
            ir.coffevista.vista_native.features.chat.presentation.components.VistaNotchMetaballHeader(
                displayName = displayName,
                subtitle = presenceText,
                isSubtitleHighlighted = isPartnerOnline,
                avatarUrl = heroAvatar,
                scrollOffsetPx = detailsScrollState.value,
                glassState = detailsGlassState,
                onBack = onDismiss,
                backContentDescription = "بازگشت از اطلاعات کاربر",
                menuContentDescription = "گزینه‌های اطلاعات کاربر",
                onMenuClick = { partnerMenuExpanded = true },
                menuDropdownContent = {
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
                },
                verificationBadgeContent = {
                    ChatUserVerificationBadge(
                        isVerified = profile?.isVerified == true || conversation?.isVerified == true,
                        verificationType = profile?.verificationType ?: conversation?.verificationType,
                        role = profile?.role ?: conversation?.role,
                        size = 18.dp,
                        isDarkSurface = isDark,
                        spacerWidth = 5.dp,
                    )
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )
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

@Composable
private fun RequestRecipientDecisionBar(
    peerTitle: String,
    loading: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRejectAndBlock: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF17212E) else Color(0xFFF8FAFC)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = cardBg.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$peerTitle درخواست گفتگو ارسال کرده است",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "با تایید این درخواست، امکان گفتگو برای هر دو طرف باز می‌شود.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRejectAndBlock,
                    enabled = !loading,
                    modifier = Modifier.weight(1.1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("رد و بلاک", fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !loading,
                    modifier = Modifier.weight(0.75f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text("رد", fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
                Button(
                    onClick = onAccept,
                    enabled = !loading,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White,
                    ),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text("تایید", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestWaitingApprovalBar(
    peerTitle: String,
    loading: Boolean,
    onCancel: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF17212E) else Color(0xFFF8FAFC)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = cardBg.copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "در انتظار تایید $peerTitle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "پیام شما به عنوان درخواست ارسال شده است؛ تا زمان تایید گیرنده امکان ارسال پیام جدید وجود ندارد.",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onCancel,
                enabled = !loading,
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "لغو درخواست",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestTerminalLockedBar(lockReason: String) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1F1B24) else Color(0xFFFEF2F2)
    val label = when (lockReason.lowercase()) {
        "blocked", "request_blocked" -> "امکان ارسال پیام در این گفتگو مسدود شده است."
        "cancelled", "request_cancelled" -> "این درخواست گفتگو لغو شده است."
        else -> "این درخواست گفتگو توسط گیرنده رد شده است و امکان ارسال پیام جدید وجود ندارد."
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = cardBg.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

