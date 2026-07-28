package ir.coffevista.vista_native.features.feed.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.coffevista.vista_native.core.designsystem.theme.VistaTheme
import ir.coffevista.vista_native.features.feed.data.FeedPost
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedInstrumentationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun initialContentRendersAndPostClickNavigatesById() {
        var selectedId: String? = null
        compose.setContent {
            VistaTheme {
                FeedScreenContent(
                    uiState = FeedUiState.Content(posts = listOf(post("post-1"))),
                    onRefresh = {},
                    onLoadMore = {},
                    onPostClick = { selectedId = it },
                )
            }
        }

        compose.onNodeWithTag(FeedTestTags.post("post-1")).assertExists().performClick()

        compose.runOnIdle { assertEquals("post-1", selectedId) }
    }

    @Test
    fun pullGestureInvokesRefresh() {
        val refreshes = AtomicInteger(0)
        compose.setContent {
            VistaTheme {
                FeedScreenContent(
                    uiState = FeedUiState.Content(
                        posts = listOf(post("post-1")),
                        hasMore = false,
                    ),
                    onRefresh = { refreshes.incrementAndGet() },
                    onLoadMore = {},
                    onPostClick = {},
                )
            }
        }

        compose.onNodeWithTag(FeedTestTags.PullToRefresh)
            .performTouchInput { swipeDown() }

        compose.waitForIdle()
        assertTrue(refreshes.get() > 0)
    }

    @Test
    fun scrollingNearEndTriggersAppendRequest() {
        val appends = AtomicInteger(0)
        val posts = (1..30).map { post("post-$it") }
        compose.setContent {
            VistaTheme {
                FeedScreenContent(
                    uiState = FeedUiState.Content(posts = posts, hasMore = true),
                    onRefresh = {},
                    onLoadMore = { appends.incrementAndGet() },
                    onPostClick = {},
                )
            }
        }

        compose.onNodeWithTag(FeedTestTags.List).performScrollToIndex(29)
        compose.waitForIdle()

        assertTrue(appends.get() > 0)
    }

    @Test
    fun offlineCacheAndEndReachedStatesAreVisible() {
        compose.setContent {
            VistaTheme {
                FeedScreenContent(
                    uiState = FeedUiState.Content(
                        posts = listOf(post("cached")),
                        isOffline = true,
                        isStale = true,
                        hasMore = false,
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onPostClick = {},
                )
            }
        }

        compose.onNodeWithTag(FeedTestTags.Offline).assertExists()
        compose.onNodeWithTag(FeedTestTags.EndReached).assertExists()
    }

    @Test
    fun postDetailShowsReadOnlyContentAndBackWorks() {
        val backed = AtomicBoolean(false)
        compose.setContent {
            VistaTheme {
                PostDetailContent(
                    uiState = PostDetailUiState.Content(
                        post("video").copy(
                            imageUrl = "file:///android_asset/video-cover.jpg",
                            videoUrl = "file:///android_asset/video.mp4",
                        ),
                    ),
                    onBack = { backed.set(true) },
                )
            }
        }

        compose.onNodeWithTag(PostDetailTestTags.Author).assertExists()
        compose.onNodeWithTag(PostDetailTestTags.Caption).assertExists()
        compose.onNodeWithTag(PostDetailTestTags.Media).assertExists()
        compose.onNodeWithTag(PostDetailTestTags.Counts).assertExists()
        compose.onNodeWithTag(PostDetailTestTags.Back).performClick()

        compose.runOnIdle { assertTrue(backed.get()) }
    }

    @Test
    fun emptyCacheErrorShowsRecoverableBackPath() {
        val backed = AtomicBoolean(false)
        compose.setContent {
            VistaTheme {
                PostDetailContent(
                    uiState = PostDetailUiState.Error("Post not found"),
                    onBack = { backed.set(true) },
                )
            }
        }

        compose.onNodeWithTag(PostDetailTestTags.Error).assertExists()
        compose.onNodeWithTag(PostDetailTestTags.Back).performClick()
        compose.runOnIdle { assertTrue(backed.get()) }
    }

    private fun post(id: String) = FeedPost(
        id = id,
        userId = "author",
        content = "caption-$id",
        imageUrl = null,
        imageUrls = emptyList(),
        videoUrl = null,
        musicUrl = null,
        aspectRatio = null,
        musicTitle = null,
        tags = emptyList(),
        likeCount = 1,
        commentCount = 2,
        isLiked = false,
        isSaved = false,
        hideLikeCount = false,
        hideCommentCount = false,
        authorUsername = "author",
        authorFullName = "Author",
        authorAvatarUrl = null,
        authorIsVerified = false,
        authorVerificationType = null,
        createdAt = "2026-07-27T17:21:49Z",
    )
}
