package ir.coffevista.vista_native.features.chat.presentation.newmessage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
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
        onRetry = viewModel::retry,
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
    onRetry: () -> Unit = {},
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
                    supportingText = { Text("اعضای انتخاب‌شده: ${state.selectedUserIds.size} از ۱۹") },
                    shape = RoundedCornerShape(12.dp),
                )
            }
                if (state.selectedUsers.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.selectedUsers, key = ChatUser::id) { user ->
                            SelectedGroupMemberChip(
                                user = user,
                                onRemove = { onUser(user) },
                            )
                        }
                    }
                }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("جستجو") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (state.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "پاک کردن جستجو")
                        }
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(12.dp),
            )
            if (!state.isGroupMode) {
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
            if (state.visibleUsers.isNotEmpty()) {
                Text(
                    if (state.query.isBlank()) "پیشنهادی" else "نتایج جستجو",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.visibleUsers.isEmpty() -> Column(
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.query.isBlank()) "هنوز گفتگویی نداشتید" else "نتیجه‌ای یافت نشد",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                        )
                        if (state.canRetry) {
                            TextButton(onClick = onRetry) { Text("تلاش مجدد") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedGroupMemberChip(user: ChatUser, onRemove: () -> Unit) {
    val fallback = painterResource(R.drawable.vista_default_avatar)
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onRemove)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            placeholder = fallback,
            fallback = fallback,
            error = fallback,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(28.dp).clip(CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            user.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Icon(
            Icons.Default.Close,
            contentDescription = "حذف ${user.displayName}",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
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
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "انتخاب شده",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            else -> Box(Modifier.size(24.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

private val ColorSecret = androidx.compose.ui.graphics.Color(0xFF2E7D32)
