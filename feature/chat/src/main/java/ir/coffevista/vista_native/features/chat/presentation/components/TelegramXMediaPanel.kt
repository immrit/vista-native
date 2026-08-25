package ir.coffevista.vista_native.features.chat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors
import ir.coffevista.vista_native.features.chat.domain.model.GifItem
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TelegramXTab(val title: String) {
    EMOJI("شکلک‌ها"),
    STICKERS("استیکرها"),
    GIFS("گیف‌ها"),
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramXMediaPanel(
    gifs: List<GifItem>,
    query: String,
    isLoading: Boolean,
    error: String?,
    onLoadGifs: (Boolean) -> Unit,
    onSearchGifs: (String) -> Unit,
    onEmoji: (String) -> Unit,
    onBackspace: () -> Unit,
    onGif: (String) -> Unit,
    onSticker: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recentEmojiStore = remember(context) { RecentEmojiStore(context) }
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { TelegramXTab.entries.size }
    val accentColor = VistaBrandColors.Indigo
    val panelBg = if (isDark) Color(0xFF1B242D) else Color(0xFFF4F6F8)
    val cardBg = if (isDark) Color(0xFF24313E) else Color(0xFFFFFFFF)
    val dividerColor = if (isDark) Color(0xFF2A3746) else Color(0xFFE2E6EA)

    var emojiSearchQuery by remember { mutableStateOf("") }
    var isEmojiSearchActive by remember { mutableStateOf(false) }
    var recentEmojis by remember { mutableStateOf(recentEmojiStore.getRecent()) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == TelegramXTab.GIFS.ordinal && gifs.isEmpty() && !isLoading) {
            onLoadGifs(false)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (modifier == Modifier) Modifier.height(300.dp) else modifier),
        color = panelBg,
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(panelBg)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Search Toggle Button
                IconButton(
                    onClick = { isEmojiSearchActive = !isEmojiSearchActive },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        if (isEmojiSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "جستجو",
                        tint = if (isEmojiSearchActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Centered Tabs with Sliding Indicator
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TelegramXTab.entries.forEachIndexed { index, tab ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            page = index,
                                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                                        )
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                    }
                }

                // Backspace Button
                BackspaceButton(onBackspace = onBackspace)
            }

            // Divider Line
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(dividerColor)
            )

            // Pager Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (TelegramXTab.entries[page]) {
                    TelegramXTab.EMOJI -> EmojiPickerPage(
                        recents = recentEmojis,
                        searchQuery = emojiSearchQuery,
                        isSearchActive = isEmojiSearchActive,
                        onSearchQueryChange = { emojiSearchQuery = it },
                        onEmojiSelected = { emoji ->
                            recentEmojiStore.addRecent(emoji)
                            onEmoji(emoji)
                        },
                        accentColor = accentColor,
                        cardBg = cardBg,
                    )
                    TelegramXTab.STICKERS -> StickerPickerPage(
                        onStickerSelected = onSticker,
                        accentColor = accentColor,
                        cardBg = cardBg,
                    )
                    TelegramXTab.GIFS -> GifPickerPage(
                        gifs = gifs,
                        query = query,
                        isLoading = isLoading,
                        error = error,
                        onRetry = { onLoadGifs(true) },
                        onSearch = onSearchGifs,
                        onGifSelected = onGif,
                        accentColor = accentColor,
                        cardBg = cardBg,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiPickerPage(
    recents: List<String>,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onEmojiSelected: (String) -> Unit,
    accentColor: Color,
    cardBg: Color,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val sections = remember(recents) {
        val list = mutableListOf<EmojiSectionItem>()
        if (recents.isNotEmpty()) {
            list.add(EmojiSectionItem.Header("recent", "🕒", "پرکاربردترین‌ها"))
            list.addAll(recents.map { EmojiSectionItem.Emoji(it, "recent") })
        }
        EmojiCatalog.categories.forEach { category ->
            list.add(EmojiSectionItem.Header(category.id, category.icon, category.title))
            list.addAll(category.emojis.map { EmojiSectionItem.Emoji(it, category.id) })
        }
        list
    }

    val headerIndices = remember(sections) {
        val map = mutableMapOf<String, Int>()
        sections.forEachIndexed { index, item ->
            if (item is EmojiSectionItem.Header) map[item.id] = index
        }
        map
    }

    val activeCategoryId by remember(sections) {
        derivedStateOf {
            val firstVisibleIndex = gridState.firstVisibleItemIndex
            var currentId = if (recents.isNotEmpty()) "recent" else EmojiCatalog.categories.firstOrNull()?.id ?: "smileys"
            for (item in sections.take(firstVisibleIndex + 1)) {
                if (item is EmojiSectionItem.Header) currentId = item.id
            }
            currentId
        }
    }

    val searchResults = remember(searchQuery) {
        if (searchQuery.isNotBlank()) EmojiCatalog.search(searchQuery) else emptyList()
    }

    Column(Modifier.fillMaxSize()) {
        // Collapsible Search Bar
        AnimatedVisibility(
            visible = isSearchActive,
            enter = slideInVertically(tween(180, easing = FastOutSlowInEasing)) + fadeIn(tween(180)),
            exit = slideOutVertically(tween(140, easing = FastOutSlowInEasing)) + fadeOut(tween(140)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "جستجوی شکلک (خنده، قلب، آتش...)",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Sub-Category Bar
        if (!isSearchActive) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (recents.isNotEmpty()) {
                    item {
                        CategoryChip(
                            icon = "🕒",
                            isSelected = activeCategoryId == "recent",
                            onClick = {
                                headerIndices["recent"]?.let { idx ->
                                    scope.launch { gridState.scrollToItem(idx) }
                                }
                            },
                            accentColor = accentColor,
                        )
                    }
                }
                items(EmojiCatalog.categories) { category ->
                    CategoryChip(
                        icon = category.icon,
                        isSelected = activeCategoryId == category.id,
                        onClick = {
                            headerIndices[category.id]?.let { idx ->
                                scope.launch { gridState.scrollToItem(idx) }
                            }
                        },
                        accentColor = accentColor,
                    )
                }
            }
        }

        // Content Area
        if (isSearchActive && searchQuery.isNotBlank()) {
            if (searchResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("شکلکی یافت نشد", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(searchResults) { emoji ->
                        EmojiCell(emoji = emoji, onClick = { onEmojiSelected(emoji) })
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(8),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = sections,
                    key = { index, item ->
                        when (item) {
                            is EmojiSectionItem.Header -> "header_${item.id}"
                            is EmojiSectionItem.Emoji -> "emoji_${item.id}_${item.emoji}_$index"
                        }
                    },
                    span = { _, item ->
                        if (item is EmojiSectionItem.Header) GridItemSpan(8) else GridItemSpan(1)
                    },
                ) { _, item ->
                    when (item) {
                        is EmojiSectionItem.Header -> {
                            Text(
                                text = item.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
                            )
                        }
                        is EmojiSectionItem.Emoji -> {
                            EmojiCell(emoji = item.emoji, onClick = { onEmojiSelected(item.emoji) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isSelected) accentColor.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == "🕒") {
            Text("🕒", fontSize = 16.sp)
        } else {
            TelegramEmoji(emoji = icon, size = if (isSelected) 22.dp else 19.dp)
        }
    }
}

@Composable
private fun EmojiCell(
    emoji: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        TelegramEmoji(emoji = emoji, size = 28.dp)
    }
}

private sealed interface EmojiSectionItem {
    data class Header(val id: String, val icon: String, val title: String) : EmojiSectionItem
    data class Emoji(val emoji: String, val id: String) : EmojiSectionItem
}

@Composable
private fun GifPickerPage(
    gifs: List<GifItem>,
    query: String,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSearch: (String) -> Unit,
    onGifSelected: (String) -> Unit,
    accentColor: Color,
    cardBg: Color,
) {
    val quickReactions = remember {
        listOf(
            "🔥" to "trending",
            "😂" to "خنده",
            "❤️" to "عشق",
            "👍" to "موافق",
            "😭" to "گریه",
            "🎉" to "جشن",
            "😮" to "تعجب",
            "😴" to "خواب",
            "👏" to "تشویق",
            "💔" to "دل‌شکسته",
            "👋" to "سلام",
            "💃" to "رقص",
            "🤔" to "فکر",
            "😡" to "عصبانی",
        )
    }

    var selectedReaction by remember { mutableStateOf("trending") }
    var searchInput by remember { mutableStateOf(query) }

    Column(Modifier.fillMaxSize()) {
        // GIF Search Box
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            BasicTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp),
                singleLine = true,
                cursorBrush = SolidColor(accentColor),
                decorationBox = { innerTextField ->
                    if (searchInput.isEmpty()) {
                        Text(
                            text = "جستجو در گیف‌ها (Tenor)...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Reaction Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(quickReactions) { (emoji, tag) ->
                val isSelected = selectedReaction == tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) accentColor else cardBg)
                        .clickable {
                            selectedReaction = tag
                            if (tag == "trending") onSearch("") else onSearch(tag)
                        }
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(emoji, fontSize = 13.sp)
                        Text(
                            text = if (tag == "trending") "داغ‌ترین‌ها" else tag,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // GIF Masonry Grid
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                isLoading -> CircularProgressIndicator(color = accentColor, modifier = Modifier.size(32.dp))
                error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("تلاش مجدد", fontSize = 13.sp, color = accentColor, modifier = Modifier.clickable(onClick = onRetry))
                    }
                }
                gifs.isEmpty() -> Text("گیفی یافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(gifs, key = { it.id }) { gif ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardBg)
                                    .clickable { onGifSelected(gif.url) },
                            ) {
                                AsyncImage(
                                    model = gif.previewUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerPickerPage(
    onStickerSelected: (String) -> Unit,
    accentColor: Color,
    cardBg: Color,
) {
    val defaultStickers = remember {
        listOf(
            "🌟", "🚀", "🎉", "🔥", "💎", "❤️", "😍", "🥳",
            "🐱", "🐶", "🐼", "🦊", "🦁", "🦄", "🐸", "🐻",
            "👏", "🙌", "✨", "💯", "😎", "🤩", "👑", "🌈",
            "☕", "🍕", "🧁", "🍿", "🍔", "🍓", "🍩", "🍦",
        )
    }

    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(defaultStickers) { sticker ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .clickable { onStickerSelected(sticker) }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TelegramEmoji(emoji = sticker, size = 44.dp)
                }
            }
        }
    }
}

@Composable
private fun BackspaceButton(
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onBackspace()
            delay(400)
            while (isPressed) {
                onBackspace()
                delay(60)
            }
        }
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "حذف",
            tint = if (isPressed) VistaBrandColors.Indigo else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
