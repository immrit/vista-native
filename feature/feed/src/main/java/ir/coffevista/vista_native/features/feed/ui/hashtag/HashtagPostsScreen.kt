package ir.coffevista.vista_native.features.feed.ui.hashtag

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.coffevista.vista_native.core.model.session.AuthenticationState
import ir.coffevista.vista_native.core.model.session.AuthenticationStateProvider
import ir.coffevista.vista_native.features.feed.data.FeedPost
import ir.coffevista.vista_native.features.feed.data.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HashtagPostsUiState(
    val posts: List<FeedPost> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val hasMore: Boolean = true,
    val nextOffset: Int = 0,
    val errorMessage: String? = null,
)

@HiltViewModel
class HashtagPostsViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authenticationStateProvider: AuthenticationStateProvider,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HashtagPostsUiState())
    val uiState: StateFlow<HashtagPostsUiState> = mutableUiState.asStateFlow()
    private var boundTag: String? = null

    fun bind(hashtag: String) {
        val normalized = hashtag.removePrefix("#").trim()
        if (normalized.isBlank() || normalized == boundTag) return
        boundTag = normalized
        refresh()
    }

    fun refresh() = request(offset = 0, append = false)

    fun loadMore() {
        val state = uiState.value
        if (state.isLoading || state.isRefreshing || state.isAppending || !state.hasMore) return
        request(offset = state.nextOffset, append = true)
    }

    private fun request(offset: Int, append: Boolean) {
        val hashtag = boundTag ?: return
        val accountId = (authenticationStateProvider.state.value as? AuthenticationState.SignedIn)
            ?.context
            ?.userId
        if (accountId == null) {
            mutableUiState.update { it.copy(isLoading = false, errorMessage = "برای دیدن پست‌ها وارد حساب شوید") }
            return
        }
        mutableUiState.update {
            when {
                append -> it.copy(isAppending = true, errorMessage = null)
                offset == 0 && it.posts.isNotEmpty() -> it.copy(isRefreshing = true, errorMessage = null)
                else -> it.copy(isLoading = true, errorMessage = null)
            }
        }
        viewModelScope.launch {
            runCatching { feedRepository.getHashtagPosts(accountId, hashtag, offset) }
                .onSuccess { snapshot ->
                    mutableUiState.update { current ->
                        val posts = if (append) {
                            (current.posts + snapshot.posts).distinctBy(FeedPost::id)
                        } else {
                            snapshot.posts
                        }
                        current.copy(
                            posts = posts,
                            isLoading = false,
                            isRefreshing = false,
                            isAppending = false,
                            hasMore = snapshot.hasMore,
                            nextOffset = snapshot.nextOffset,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure {
                    mutableUiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isAppending = false,
                            errorMessage = "دریافت پست‌های هشتگ ناموفق بود؛ دوباره تلاش کنید",
                        )
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HashtagPostsScreen(
    hashtag: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
) {
    val viewModel: HashtagPostsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val normalized = remember(hashtag) { hashtag.removePrefix("#").trim() }

    LaunchedEffect(normalized) { viewModel.bind(normalized) }
    LaunchedEffect(listState, state.posts.size, state.hasMore) {
        androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= state.posts.lastIndex - 3) viewModel.loadMore()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#$normalized", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "بازگشت") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
        ) {
            when {
                state.isLoading && state.posts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.posts.isEmpty() -> EmptyHashtagState(state.errorMessage, viewModel::refresh)
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { HashtagHeader(normalized, state.posts.size) }
                    items(state.posts, key = FeedPost::id) { post ->
                        HashtagPostCard(post, onPostClick, onAuthorClick)
                    }
                    if (state.isAppending) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } }
                    state.errorMessage?.takeIf { state.posts.isNotEmpty() }?.let { message ->
                        item { OutlinedButton(onClick = viewModel::loadMore, modifier = Modifier.fillMaxWidth()) { Text(message) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun HashtagHeader(hashtag: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Icon(Icons.Outlined.Tag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column {
            Text("#$hashtag", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("$count پست بارگذاری‌شده", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HashtagPostCard(post: FeedPost, onPostClick: (String) -> Unit, onAuthorClick: (String) -> Unit) {
    androidx.compose.material3.ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onPostClick(post.id) }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onAuthorClick(post.userId) }, verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = post.authorAvatarUrl, contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(10.dp))
                Text(post.authorUsername?.takeIf(String::isNotBlank) ?: post.authorFullName, fontWeight = FontWeight.Bold)
            }
            post.content?.takeIf(String::isNotBlank)?.let { Text(it, maxLines = 4, overflow = TextOverflow.Ellipsis) }
            post.primaryImageUrl?.let { AsyncImage(model = it, contentDescription = "رسانهٔ پست", modifier = Modifier.fillMaxWidth().height(180.dp)) }
        }
    }
}

@Composable
private fun EmptyHashtagState(errorMessage: String?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(errorMessage ?: "پستی برای این هشتگ پیدا نشد", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) { Text("تلاش مجدد") }
    }
}
