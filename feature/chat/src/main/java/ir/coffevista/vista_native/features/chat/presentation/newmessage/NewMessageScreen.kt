package ir.coffevista.vista_native.features.chat.presentation.newmessage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.R
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser

@Composable
fun NewMessageRoute(
    viewModel: NewMessageViewModel,
    onBack: () -> Unit,
    onOpenConversation: (conversationId: String, title: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NewMessageScreen(
        state = state,
        onBack = onBack,
        onQueryChanged = viewModel::queryChanged,
        onSecretModeChanged = viewModel::setSecretMode,
        onGroupModeChanged = viewModel::setGroupMode,
        onGroupNameChanged = viewModel::groupNameChanged,
        onUser = { user ->
            if (state.isGroupMode) viewModel.toggleGroupUser(user)
            else viewModel.openUser(user, onOpenConversation)
        },
        onCreateGroup = { viewModel.createGroup(onOpenConversation) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    state: NewMessageUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSecretModeChanged: (Boolean) -> Unit,
    onGroupModeChanged: (Boolean) -> Unit = {},
    onGroupNameChanged: (String) -> Unit = {},
    onUser: (ChatUser) -> Unit,
    onCreateGroup: () -> Unit = {},
) = CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            state.isGroupMode -> "گروه جدید"
                            state.isSecretMode -> "گفتگوی محرمانه"
                            else -> "پیام جدید"
                        },
                        color = if (state.isSecretMode) ColorSecret else MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Ltr) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                actions = {
                    if (state.isGroupMode) {
                        TextButton(
                            onClick = onCreateGroup,
                            enabled = !state.isCreatingGroup && state.groupName.isNotBlank() && state.selectedUserIds.isNotEmpty(),
                        ) {
                            if (state.isCreatingGroup) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("ایجاد", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isGroupMode) {
                OutlinedTextField(
                    value = state.groupName,
                    onValueChange = onGroupNameChanged,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text("نام گروه") },
                    supportingText = { Text("${state.selectedUserIds.size + 1} از ۲۰ عضو") },
                    shape = RoundedCornerShape(12.dp),
                )
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("جستجو") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
            )
            Row(
                Modifier.fillMaxWidth().clickable { onGroupModeChanged(!state.isGroupMode) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("گروه جدید", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("تا ۲۰ عضو", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(checked = state.isGroupMode, onCheckedChange = onGroupModeChanged)
            }
            if (!state.isGroupMode) {
                Row(
                    Modifier.fillMaxWidth().clickable { onSecretModeChanged(!state.isSecretMode) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(ColorSecret.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Lock, contentDescription = null, tint = ColorSecret, modifier = Modifier.size(24.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("گفتگوی محرمانه جدید", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("تاریخچه موجود در Native محافظت‌شده می‌ماند", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ir.coffevista.vista_native.core.designsystem.component.VistaSwitch(checked = state.isSecretMode, onCheckedChange = onSecretModeChanged)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Text(
                "پیشنهادی",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.visibleUsers.isEmpty() -> Text(
                        if (state.query.isBlank()) "هنوز گفتگویی نداشتید" else "نتیجه‌ای یافت نشد",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(state.visibleUsers, key = ChatUser::id) { user ->
                            NewMessageUserRow(
                                user = user,
                                loading = state.openingUserId == user.id,
                                selected = user.id in state.selectedUserIds,
                                onClick = { onUser(user) },
                            )
                        }
                    }
                }
                if (state.isSearching) CircularProgressIndicator(Modifier.align(Alignment.TopCenter).size(22.dp))
                state.error?.let { error ->
                    Text(
                        error,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewMessageUserRow(user: ChatUser, loading: Boolean, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fallback = painterResource(R.drawable.vista_default_avatar)
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            placeholder = fallback,
            fallback = fallback,
            error = fallback,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(user.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (user.username.isNotBlank() && user.username != user.displayName) {
                Text("@${user.username}", maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        when {
            loading -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            selected -> Box(
                Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) { Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp) }
        }
    }
}

private val ColorSecret = androidx.compose.ui.graphics.Color(0xFF2E7D32)
