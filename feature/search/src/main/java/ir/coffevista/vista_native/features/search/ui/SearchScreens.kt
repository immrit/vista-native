package ir.coffevista.vista_native.features.search.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ir.coffevista.vista_native.features.search.data.HashtagSuggestion
import ir.coffevista.vista_native.features.search.data.SearchHistoryItem
import ir.coffevista.vista_native.features.search.data.SearchHistoryType
import ir.coffevista.vista_native.features.search.data.SearchPost
import ir.coffevista.vista_native.features.search.data.SearchUser

private val SearchFieldShape = RoundedCornerShape(18.dp)

@Composable
fun SearchLauncherScreen(
    viewModel: SearchViewModel,
    onOpenWorkspace: () -> Unit,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(SearchTestTags.Launcher),
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(SearchFieldShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f), SearchFieldShape)
                .clickable(
                    role = Role.Button,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onOpenWorkspace,
                )
                .testTag(SearchTestTags.LauncherField)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchGlyph(
                    modifier = Modifier.size(23.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "جستجوی کاربران",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onOpenQrScanner,
                    modifier = Modifier.testTag(SearchTestTags.LauncherQrScanner),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = "اسکن کد QR",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        LauncherContent(
            state = state,
            onHashtag = {
                viewModel.selectHashtag(it)
                onOpenWorkspace()
            },
        )
    }
}

@Composable
fun SearchWorkspaceScreen(
    viewModel: SearchViewModel,
    onUserClick: (SearchUser) -> Unit,
    onPostClick: (SearchPost) -> Unit,
    autoFocus: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchWorkspaceContent(
        state = state,
        onQueryChanged = viewModel::onQueryChanged,
        onSubmit = viewModel::submit,
        onFocusChanged = viewModel::onFocusChanged,
        onClearQuery = viewModel::clearQuery,
        onSelectTab = viewModel::selectTab,
        onHistory = viewModel::selectHistory,
        onDeleteHistory = viewModel::deleteHistory,
        onClearHistory = viewModel::clearHistory,
        onHashtag = viewModel::selectHashtag,
        onUser = { user ->
            viewModel.selectUser(user)
            onUserClick(user)
        },
        onPost = { post ->
            viewModel.selectPost()
            onPostClick(post)
        },
        onRetry = viewModel::retry,
        onLoadMoreUsers = viewModel::loadMoreUsers,
        autoFocus = autoFocus,
        modifier = modifier,
    )
}

@Composable
internal fun SearchWorkspaceContent(
    state: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClearQuery: () -> Unit,
    onSelectTab: (SearchTab) -> Unit,
    onHistory: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onHashtag: (String) -> Unit,
    onUser: (SearchUser) -> Unit,
    onPost: (SearchPost) -> Unit,
    onRetry: () -> Unit,
    onLoadMoreUsers: () -> Unit,
    autoFocus: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(SearchTestTags.Workspace),
    ) {
        SearchField(
            value = state.query,
            onValueChange = onQueryChanged,
            onSubmit = onSubmit,
            onFocusChanged = onFocusChanged,
            onClear = onClearQuery,
            focusRequester = focusRequester,
        )
        if (state.hasQuery) {
            SearchTabs(
                selected = state.selectedTab,
                onSelected = onSelectTab,
            )
        }
        AnimatedContent(
            targetState = state.phase to state.selectedTab,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
            },
            label = "search-body",
            modifier = Modifier.fillMaxSize(),
        ) { (phase, selectedTab) ->
            SearchBody(
                state = state.copy(phase = phase, selectedTab = selectedTab),
                onHistory = onHistory,
                onDeleteHistory = onDeleteHistory,
                onClearHistory = onClearHistory,
                onHashtag = onHashtag,
                onUser = onUser,
                onPost = onPost,
                onRetry = onRetry,
                onLoadMoreUsers = onLoadMoreUsers,
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Box(
        modifier = Modifier
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(SearchFieldShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f), SearchFieldShape)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SearchGlyph(
                modifier = Modifier.size(23.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(SearchTestTags.Field)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSubmit()
                        keyboard?.hide()
                        focusManager.clearFocus()
                    },
                ),
                singleLine = true,
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = "جستجوی کاربران",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                SmallIconButton(
                    description = "پاک کردن عبارت جستجو",
                    tag = SearchTestTags.ClearQuery,
                    onClick = onClear,
                ) { color -> CloseGlyph(Modifier.size(21.dp), color) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LauncherContent(
    state: SearchUiState,
    onHashtag: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
    ) {
        Text(
            text = "ترندهای امروز",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        if (state.isLoadingTrending && state.trending.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            TrendingChips(state.trending, onHashtag)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendingChips(
    items: List<HashtagSuggestion>,
    onHashtag: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(12).forEach { item ->
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(20.dp),
                    )
                    .clickable { onHashtag(item.tag) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrendingGlyph(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "#${item.tag}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchTabs(
    selected: SearchTab,
    onSelected: (SearchTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag(SearchTestTags.Tabs),
    ) {
        listOf(
            SearchTab.All to "همه",
            SearchTab.People to "افراد",
            SearchTab.Tags to "تگ‌ها",
        ).forEach { (tab, title) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected == tab) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(bottom = 9.dp),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.48f)
                        .height(2.dp)
                        .background(
                            if (selected == tab) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                        ),
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SearchBody(
    state: SearchUiState,
    onHistory: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onHashtag: (String) -> Unit,
    onUser: (SearchUser) -> Unit,
    onPost: (SearchPost) -> Unit,
    onRetry: () -> Unit,
    onLoadMoreUsers: () -> Unit,
) {
    when {
        !state.hasQuery -> InitialContent(
            state,
            onHistory,
            onDeleteHistory,
            onClearHistory,
            onHashtag,
        )
        state.phase == SearchPhase.Loading -> LoadingContent()
        state.phase == SearchPhase.Error -> ErrorContent(state.errorMessage, onRetry)
        state.phase == SearchPhase.Empty -> EmptyContent()
        state.phase == SearchPhase.Typing && state.users.isEmpty() && state.posts.isEmpty() ->
            SuggestionsContent(state, onHashtag)
        else -> ResultsContent(
            state = state,
            onUser = onUser,
            onPost = onPost,
            onHashtag = onHashtag,
            onLoadMoreUsers = onLoadMoreUsers,
        )
    }
}

@Composable
private fun InitialContent(
    state: SearchUiState,
    onHistory: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onHashtag: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
    ) {
        if (state.history.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "جستجوهای اخیر",
                    action = "پاک کردن همه",
                    onAction = onClearHistory,
                    modifier = Modifier.testTag(SearchTestTags.ClearHistory),
                )
            }
            items(state.history, key = SearchHistoryItem::query) { item ->
                HistoryRow(item, onHistory, onDeleteHistory)
            }
        }
        item {
            SectionHeader(title = "ترندهای امروز")
        }
        if (state.isLoadingTrending && state.trending.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        } else {
            item {
                TrendingChips(state.trending, onHashtag)
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: SearchHistoryItem,
    onHistory: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onHistory(item.query) }
            .testTag(SearchTestTags.history(item.query)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HistoryGlyph(
            modifier = Modifier.size(21.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = item.query,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SmallIconButton(
            description = "حذف ${item.query}",
            onClick = { onDeleteHistory(item.query) },
        ) { color -> CloseGlyph(Modifier.size(18.dp), color) }
    }
}

@Composable
private fun HashtagRow(
    item: HashtagSuggestion,
    onClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable { onClick(item.tag) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "#",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("#${item.tag}", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "${item.usageCount} پست",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        if (action != null) {
            Text(
                text = action,
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchTestTags.Loading),
        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
    ) {
        items(2) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.32f)
                                .height(11.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(0.62f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
                Box(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .aspectRatio(1.65f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(SearchTestTags.Empty),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SearchOffGlyph(
            modifier = Modifier.size(52.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Text("نتیجه‌ای پیدا نشد", style = MaterialTheme.typography.titleMedium)
        Text(
            "عبارت دیگری را امتحان کنید",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(SearchTestTags.Error),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("جستجو انجام نشد", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = message ?: "لطفاً اتصال اینترنت را بررسی کنید.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "تلاش دوباره",
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag(SearchTestTags.Retry),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SuggestionsContent(
    state: SearchUiState,
    onHashtag: (String) -> Unit,
) {
    val suggestions = if (state.suggestions.isEmpty()) state.trending else state.suggestions
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchTestTags.Suggestions),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        if (state.isLoadingSuggestions) {
            item {
                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
        items(suggestions.take(8), key = HashtagSuggestion::tag) {
            HashtagRow(it, onHashtag)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultsContent(
    state: SearchUiState,
    onUser: (SearchUser) -> Unit,
    onPost: (SearchPost) -> Unit,
    onHashtag: (String) -> Unit,
    onLoadMoreUsers: () -> Unit,
) {
    when (state.selectedTab) {
        SearchTab.People -> UserResults(state, onUser, onLoadMoreUsers)
        SearchTab.Tags -> {
            if (state.posts.isNotEmpty()) {
                PostResults(state.posts, onPost)
            } else {
                SuggestionsContent(state, onHashtag)
            }
        }
        SearchTab.All -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (state.users.isNotEmpty()) {
                item { SectionHeader("افراد", modifier = Modifier.padding(horizontal = 16.dp)) }
                items(state.users.take(5), key = SearchUser::id) {
                    UserRow(it, onUser)
                }
            }
            if (state.posts.isNotEmpty()) {
                item { SectionHeader("پست‌ها", modifier = Modifier.padding(horizontal = 16.dp)) }
                item {
                    PostStrip(state.posts, onPost)
                }
            }
        }
    }
}

@Composable
private fun UserResults(
    state: SearchUiState,
    onUser: (SearchUser) -> Unit,
    onLoadMoreUsers: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchTestTags.UserList),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(state.users, key = SearchUser::id) { UserRow(it, onUser) }
        if (state.hasMoreUsers) {
            item {
                LaunchedEffect(state.nextUserOffset) { onLoadMoreUsers() }
            }
        }
        if (state.isAppendingUsers) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag(SearchTestTags.AppendLoading),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
        state.appendErrorMessage?.let { error ->
            item {
                Text(
                    text = error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag(SearchTestTags.AppendError)
                        .clickable { onLoadMoreUsers() },
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun UserRow(user: SearchUser, onClick: (SearchUser) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clickable { onClick(user) }
            .padding(horizontal = 16.dp)
            .testTag(SearchTestTags.user(user.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (user.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    VerifiedBadge()
                }
            }
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr,
            ) {
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun PostResults(posts: List<SearchPost>, onClick: (SearchPost) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .testTag(SearchTestTags.PostGrid),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(posts, key = SearchPost::id) { PostTile(it, onClick) }
    }
}

@Composable
private fun PostStrip(posts: List<SearchPost>, onClick: (SearchPost) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(posts.take(9), key = SearchPost::id) {
            PostTile(it, onClick, Modifier.size(112.dp))
        }
    }
}

@Composable
private fun PostTile(
    post: SearchPost,
    onClick: (SearchPost) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick(post) }
            .testTag(SearchTestTags.post(post.id)),
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = "تصویر پست",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (post.videoUrl != null) {
            Text(
                text = "▶",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp),
                color = Color.White,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun VerifiedBadge() {
    Box(
        Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallIconButton(
    description: String,
    tag: String? = null,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        icon(MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        drawCircle(
            color = color,
            radius = size.minDimension * 0.29f,
            center = Offset(size.width * 0.43f, size.height * 0.43f),
            style = Stroke(stroke),
        )
        drawLine(
            color,
            Offset(size.width * 0.64f, size.height * 0.64f),
            Offset(size.width * 0.86f, size.height * 0.86f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun CloseGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        drawLine(color, Offset(size.width * .25f, size.height * .25f), Offset(size.width * .75f, size.height * .75f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .75f, size.height * .25f), Offset(size.width * .25f, size.height * .75f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun HistoryGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val stroke = size.minDimension * .08f
        drawArc(color, 45f, 300f, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawLine(color, center, Offset(center.x, size.height * .28f), stroke, StrokeCap.Round)
        drawLine(color, center, Offset(size.width * .68f, center.y), stroke, StrokeCap.Round)
    }
}

@Composable
private fun TrendingGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val stroke = size.minDimension * .1f
        drawLine(
            color,
            Offset(size.width * .14f, size.height * .68f),
            Offset(size.width * .42f, size.height * .42f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width * .42f, size.height * .42f),
            Offset(size.width * .62f, size.height * .58f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(size.width * .62f, size.height * .58f),
            Offset(size.width * .86f, size.height * .27f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun SearchOffGlyph(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val stroke = size.minDimension * .055f
        drawCircle(color, size.minDimension * .27f, Offset(size.width * .42f, size.height * .42f), style = Stroke(stroke))
        drawLine(color, Offset(size.width * .62f, size.height * .62f), Offset(size.width * .82f, size.height * .82f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .18f, size.height * .18f), Offset(size.width * .82f, size.height * .82f), stroke, StrokeCap.Round)
    }
}
