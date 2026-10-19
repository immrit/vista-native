package ir.coffevista.vista_native.features.chat.presentation

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.core.designsystem.component.VistaSwitch
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.GroupMember
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    state: MessagesUiState,
    clipboard: ClipboardManager,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenSearch: () -> Unit,
    onUpdateName: (String) -> Unit,
    onAddMembers: (List<String>) -> Unit,
    onSearchUsers: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetAdmin: (String, Boolean) -> Unit,
    onSetInviteEnabled: (Boolean) -> Unit,
    onRegenerateInvite: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val info = state.groupInfo
    val conversation = state.conversation
    val displayName = info?.name ?: conversation?.title.orEmpty().ifBlank { "گروه" }

    val currentUserId = info?.currentUserId
    val isCreator = currentUserId != null && currentUserId == info?.createdByUserId
    val isAdmin = info?.isAdmin == true || isCreator
    val remainingSlots = ((info?.maxMembers ?: 20) - state.groupMembers.size).coerceAtLeast(0)

    var selectedTabIndex by rememberSaveable(conversation?.id) { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var editNameDialogVisible by remember { mutableStateOf(false) }
    var editedNameText by remember(info?.name) { mutableStateOf(info?.name.orEmpty()) }

    var pendingMemberAction by remember { mutableStateOf<Pair<GroupMember, Boolean>?>(null) }
    var pendingRemoval by remember { mutableStateOf<GroupMember?>(null) }
    var addMembersExpanded by rememberSaveable(info?.id) { mutableStateOf(false) }
    var memberSearchQuery by rememberSaveable(info?.id) { mutableStateOf("") }
    var selectedNewMemberIds by remember(info?.id) { mutableStateOf(emptySet<String>()) }

    val existingMemberIds = remember(state.groupMembers) {
        state.groupMembers.mapTo(mutableSetOf()) { it.userId }
    }

    val conversationCandidates = remember(state.forwardTargets) {
        state.forwardTargets.asSequence()
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
    }
    val localQuery = state.groupUserQuery.trim()
    val queriedCandidates = state.groupUserResults ?: conversationCandidates.filter { candidate ->
        localQuery.isEmpty() ||
            candidate.displayName.contains(localQuery, ignoreCase = true) ||
            candidate.username.contains(localQuery.removePrefix("@"), ignoreCase = true)
    }
    val addCandidates = remember(queriedCandidates, conversationCandidates, localQuery, existingMemberIds) {
        (queriedCandidates + if (localQuery.isEmpty()) conversationCandidates else emptyList())
            .filter { it.id !in existingMemberIds }
            .distinctBy(ChatUser::id)
    }

    // Filter shared media datasets
    val mediaMessages = remember(state.sharedMedia) {
        state.sharedMedia.filter {
            it.attachment?.kind in setOf(AttachmentKind.IMAGE, AttachmentKind.VIDEO) ||
                it.attachment?.mimeType?.startsWith("image/") == true ||
                it.attachment?.mimeType?.startsWith("video/") == true
        }
    }
    val fileMessages = remember(state.sharedMedia) {
        state.sharedMedia.filter {
            it.attachment?.kind == AttachmentKind.DOCUMENT ||
                it.attachment?.mimeType?.startsWith("application/") == true ||
                it.attachment?.mimeType?.startsWith("text/") == true
        }
    }
    val audioMessages = remember(state.sharedMedia) {
        state.sharedMedia.filter {
            it.attachment?.kind in setOf(AttachmentKind.AUDIO, AttachmentKind.VOICE) ||
                it.attachment?.mimeType?.startsWith("audio/") == true
        }
    }
    val linkMessages = remember(state.messages) {
        state.messages.filter { message ->
            (message.content as? MessageContent.Text)?.value?.let { text ->
                text.contains("http://", ignoreCase = true) ||
                    text.contains("https://", ignoreCase = true) ||
                    text.contains("www.", ignoreCase = true)
            } == true
        }
    }

    val tabs = listOf("اعضا", "رسانه", "فایل‌ها", "لینک‌ها", "صدا")

    // Filter and sort group members
    val filteredMembers = remember(state.groupMembers, memberSearchQuery, info?.createdByUserId) {
        val query = memberSearchQuery.trim().lowercase()
        state.groupMembers
            .filter { member ->
                query.isEmpty() ||
                    member.displayName.lowercase().contains(query) ||
                    member.username.lowercase().contains(query)
            }
            .sortedWith(
                compareByDescending<GroupMember> { it.userId == info?.createdByUserId }
                    .thenByDescending { it.isAdmin }
                    .thenBy { it.displayName.lowercase() }
            )
    }

    val inviteLink = info?.inviteCode?.let { "https://cafevista.ir/group/$it" }

    BackHandler(onBack = onDismiss)
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("group-details-screen"),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            // Immersive Parallax/Blurred Hero Header (280dp, matching PartnerDetailsScreen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                val heroAvatar = (info?.imageUrl ?: conversation?.avatarUrl)?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
                if (!heroAvatar.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = heroAvatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(25.dp),
                        loading = { HeroBackdropFallback(displayName) },
                        error = { HeroBackdropFallback(displayName) },
                    )
                } else {
                    HeroBackdropFallback(displayName)
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
                        contentDescription = "بازگشت از اطلاعات گروه",
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
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.30f), CircleShape),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "گزینه‌های گروه",
                            tint = Color.White,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text("ویرایش نام گروه") },
                                onClick = {
                                    menuExpanded = false
                                    editedNameText = displayName
                                    editNameDialogVisible = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("افزودن عضو") },
                                onClick = {
                                    menuExpanded = false
                                    selectedTabIndex = 0
                                    addMembersExpanded = true
                                },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                            )
                        }
                        if (inviteLink != null) {
                            DropdownMenuItem(
                                text = { Text("کپی لینک دعوت") },
                                onClick = {
                                    menuExpanded = false
                                    clipboard.setText(AnnotatedString(inviteLink))
                                    scope.launch { snackbarHostState.showSnackbar("لینک دعوت کپی شد") }
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            )
                            DropdownMenuItem(
                                text = { Text("اشتراک لینک دعوت") },
                                onClick = {
                                    menuExpanded = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "دعوت به گروه $displayName در Vista:\n$inviteLink")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "اشتراک لینک دعوت"))
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("تازه‌سازی اطلاعات") },
                            onClick = {
                                menuExpanded = false
                                onRefresh()
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isCreator) "حذف گروه برای همه" else "ترک گروه",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                if (isCreator) onDelete() else onLeave()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }

                // Avatar and Group title
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
                            SubcomposeAsyncImage(
                                model = heroAvatar,
                                contentDescription = "تصویر $displayName",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = { GroupAvatarFallback(displayName) },
                                error = { GroupAvatarFallback(displayName) },
                            )
                        } else {
                            GroupAvatarFallback(displayName)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    val adminCount = state.groupMembers.count { it.isAdmin || it.userId == info?.createdByUserId }
                    Text(
                        text = "${info?.memberCount ?: state.groupMembers.size} عضو • $adminCount مدیر",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                    )
                }
            }

            // Quick Actions Row (Messages, Mute, Search, Add/Invite with spring tactile feedback)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TactileGroupAction(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "پیام",
                    modifier = Modifier.weight(1f),
                ) { onDismiss() }
                TactileGroupAction(
                    icon = if (conversation?.isMuted == true) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                    label = if (conversation?.isMuted == true) "صدادار" else "بی‌صدا",
                    modifier = Modifier.weight(1f),
                ) {
                    onToggleMute()
                    scope.launch { snackbarHostState.showSnackbar("تنظیمات اعلان گروه به‌روزرسانی شد") }
                }
                TactileGroupAction(
                    icon = Icons.Default.Search,
                    label = "جستجو",
                    modifier = Modifier.weight(1f),
                ) {
                    selectedTabIndex = 0
                    onOpenSearch()
                }
                TactileGroupAction(
                    icon = if (isAdmin) Icons.Default.PersonAdd else Icons.Default.AttachFile,
                    label = if (isAdmin) "افزودن" else "دعوت",
                    modifier = Modifier.weight(1f),
                ) {
                    if (isAdmin) {
                        selectedTabIndex = 0
                        addMembersExpanded = !addMembersExpanded
                    } else if (inviteLink != null) {
                        clipboard.setText(AnnotatedString(inviteLink))
                        scope.launch { snackbarHostState.showSnackbar("لینک دعوت کپی شد") }
                    }
                }
            }

            // Group Metadata Card (Capacity, Role, Bio, Invite Link, Status)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(vertical = 6.dp),
            ) {
                // Capacity Row
                GroupInfoRow(
                    icon = Icons.Default.Group,
                    title = "ظرفیت گروه",
                    value = "${state.groupMembers.size} از ${info?.maxMembers ?: 20} عضو ($remainingSlots جای خالی)",
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Role Row
                val roleTitle = when {
                    isCreator -> "سازنده گروه"
                    isAdmin -> "مدیر گروه"
                    else -> "عضو عادی"
                }
                GroupInfoRow(
                    icon = Icons.Default.Security,
                    title = "نقش شما",
                    value = roleTitle,
                )

                // Invite Link Row
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (info?.inviteEnabled == false) "لینک دعوت (غیرفعال)" else "لینک دعوت",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = inviteLink ?: "لینک فعالی وجود ندارد",
                            fontSize = 14.sp,
                            color = if (inviteLink != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (inviteLink != null) {
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(inviteLink))
                                scope.launch { snackbarHostState.showSnackbar("لینک دعوت کپی شد") }
                            },
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی لینک", modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "دعوت به گروه $displayName در Vista:\n$inviteLink")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "اشتراک لینک دعوت"))
                            },
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "اشتراک لینک", modifier = Modifier.size(20.dp))
                        }
                    }
                    if (isAdmin) {
                        IconButton(
                            onClick = onRegenerateInvite,
                            enabled = !state.isGroupLoading,
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "ساخت لینک جدید", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Invite Switch (Admin only)
                if (isAdmin) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "امکان پیوستن با لینک دعوت",
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        VistaSwitch(
                            checked = info?.inviteEnabled == true,
                            onCheckedChange = onSetInviteEnabled,
                            enabled = !state.isGroupLoading,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 5 Tabs Section (اعضا, رسانه, فایل‌ها, لینک‌ها, صدا)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 8.dp,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) },
                ) {
                    tabs.forEachIndexed { index, label ->
                        val count = when (index) {
                            0 -> state.groupMembers.size
                            1 -> mediaMessages.size
                            2 -> fileMessages.size
                            3 -> linkMessages.size
                            4 -> audioMessages.size
                            else -> 0
                        }
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text("$label ($count)", fontSize = 13.sp) },
                        )
                    }
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            // Members Tab
                            Column(Modifier.fillMaxSize()) {
                                // Search bar inside members tab
                                OutlinedTextField(
                                    value = memberSearchQuery,
                                    onValueChange = { memberSearchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    placeholder = { Text("جستجوی اعضا...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        if (memberSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { memberSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "پاک کردن", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                )

                                // Add Members Panel (collapsible)
                                AnimatedVisibility(
                                    visible = isAdmin && addMembersExpanded && remainingSlots > 0,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "افزودن کاربر به گروه ($remainingSlots جای خالی)",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(
                                                onClick = { addMembersExpanded = false },
                                                modifier = Modifier.size(24.dp),
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = state.groupUserQuery,
                                            onValueChange = onSearchUsers,
                                            modifier = Modifier.fillMaxWidth().testTag("group-add-search-input"),
                                            singleLine = true,
                                            placeholder = { Text("جستجو در کاربران...", fontSize = 12.sp) },
                                            trailingIcon = {
                                                if (state.isGroupUserSearching) {
                                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        state.groupUserSearchError?.let { err ->
                                            Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        if (addCandidates.isNotEmpty()) {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                items(addCandidates.take(30), key = { "cand:${it.id}" }) { cand ->
                                                    val selected = cand.id in selectedNewMemberIds
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                selectedNewMemberIds = when {
                                                                    selected -> selectedNewMemberIds - cand.id
                                                                    selectedNewMemberIds.size < remainingSlots -> selectedNewMemberIds + cand.id
                                                                    else -> selectedNewMemberIds
                                                                }
                                                            }
                                                            .padding(vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        ChatAvatarImage(
                                                            model = cand.avatarUrl,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(36.dp).clip(CircleShape),
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Column(Modifier.weight(1f)) {
                                                            Text(cand.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                                                            cand.username.takeIf(String::isNotBlank)?.let {
                                                                Text("@$it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                        Checkbox(checked = selected, onCheckedChange = null)
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    onAddMembers(selectedNewMemberIds.toList())
                                                    selectedNewMemberIds = emptySet()
                                                    onSearchUsers("")
                                                    addMembersExpanded = false
                                                },
                                                enabled = selectedNewMemberIds.isNotEmpty() && !state.isGroupLoading,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                            ) {
                                                Text("افزودن ${selectedNewMemberIds.size} عضو")
                                            }
                                        } else if (!state.isGroupUserSearching && state.groupUserQuery.isNotBlank()) {
                                            Text(
                                                "کاربری با این مشخصات یافت نشد",
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                                textAlign = TextAlign.Center,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }

                                // Members List
                                if (filteredMembers.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            if (memberSearchQuery.isBlank()) "هیچ عضوی یافت نشد" else "عضوی با این نام پیدا نشد",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        contentPadding = PaddingValues(bottom = 12.dp),
                                    ) {
                                        items(filteredMembers, key = { it.userId }) { member ->
                                            val isMe = member.userId == currentUserId
                                            val isTargetCreator = member.userId == info?.createdByUserId
                                            val canManage = !isMe && (
                                                isCreator || (info?.isAdmin == true && !member.isAdmin && !isTargetCreator)
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp, horizontal = 4.dp)
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
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 14.sp,
                                                        )
                                                        if (isMe) {
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                "شما",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier
                                                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        when {
                                                            isTargetCreator -> "سازنده"
                                                            member.isAdmin -> "مدیر"
                                                            else -> "عضو"
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
                                                        Text(if (member.isAdmin) "عزل" else "مدیر", fontSize = 12.sp)
                                                    }
                                                    TextButton(
                                                        onClick = { pendingRemoval = member },
                                                        enabled = !state.isGroupLoading,
                                                        modifier = Modifier.testTag("group-remove-action:${member.userId}"),
                                                    ) {
                                                        Text("حذف", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Media Tab (Photos & Videos)
                            if (mediaMessages.isNotEmpty()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(1.dp),
                                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp),
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    gridItems(mediaMessages, key = { it.stableKey }) { msg ->
                                        val attachment = msg.attachment
                                        Box(Modifier.aspectRatio(1f)) {
                                            AsyncImage(
                                                model = attachment?.localUri ?: attachment?.remoteUrl,
                                                contentDescription = attachment?.fileName ?: "رسانه",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                            if (attachment?.kind == AttachmentKind.VIDEO || attachment?.mimeType?.startsWith("video/") == true) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = "ویدیو",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(6.dp)
                                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                                        .size(24.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptyTabState(
                                    icon = Icons.Default.AttachFile,
                                    message = "هیچ رسانه‌ای در این گفتگو یافت نشد",
                                )
                            }
                        }
                        2 -> {
                            // Files Tab
                            if (fileMessages.isNotEmpty()) {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(fileMessages, key = { it.stableKey }) { msg ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp),
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    msg.attachment?.fileName ?: msg.previewText(),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                Text(
                                                    formatConversationTime(msg.createdAtEpochMillis),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptyTabState(
                                    icon = Icons.Default.Description,
                                    message = "هیچ فایلی در این گفتگو یافت نشد",
                                )
                            }
                        }
                        3 -> {
                            // Links Tab
                            if (linkMessages.isNotEmpty()) {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(linkMessages, key = { it.stableKey }) { msg ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp),
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    msg.previewText(),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                                Text(
                                                    formatConversationTime(msg.createdAtEpochMillis),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptyTabState(
                                    icon = Icons.Default.AttachFile,
                                    message = "هیچ لینکی در این گفتگو یافت نشد",
                                )
                            }
                        }
                        4 -> {
                            // Voice Tab
                            if (audioMessages.isNotEmpty()) {
                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(audioMessages, key = { it.stableKey }) { msg ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp),
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    msg.attachment?.fileName ?: "پیام صوتی",
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                                Text(
                                                    formatConversationTime(msg.createdAtEpochMillis),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptyTabState(
                                    icon = Icons.Default.Mic,
                                    message = "هیچ پیام صوتی در این گفتگو یافت نشد",
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Danger Zone (Delete / Leave Group)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                if (isCreator) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("حذف گروه برای همه", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    TextButton(
                        onClick = onLeave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("ترک گروه", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Edit Name Dialog
    if (editNameDialogVisible) {
        AlertDialog(
            onDismissRequest = { editNameDialogVisible = false },
            title = { Text("ویرایش نام گروه") },
            text = {
                OutlinedTextField(
                    value = editedNameText,
                    onValueChange = { editedNameText = it.take(50) },
                    singleLine = true,
                    label = { Text("نام گروه") },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editedNameText.trim()
                        if (trimmed.isNotEmpty() && trimmed != info?.name) {
                            onUpdateName(trimmed)
                        }
                        editNameDialogVisible = false
                    },
                    enabled = editedNameText.isNotBlank() && !state.isGroupLoading,
                ) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { editNameDialogVisible = false }) { Text("انصراف") }
            },
        )
    }

    // Set Admin Dialog
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

    // Remove Member Dialog
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

@Composable
private fun TactileGroupAction(
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
        label = "actionPressScale",
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
private fun GroupInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun EmptyTabState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(54.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GroupAvatarFallback(displayName: String) {
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
        Text(
            text = displayName.firstOrNull()?.uppercase() ?: "G",
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HeroBackdropFallback(displayName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF6C5CE7).copy(alpha = 0.55f),
                        Color(0xFF0984E3).copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName.firstOrNull()?.uppercase() ?: "G",
            fontSize = 80.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.20f),
        )
    }
}
