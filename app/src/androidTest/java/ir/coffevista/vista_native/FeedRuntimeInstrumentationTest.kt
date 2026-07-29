package ir.coffevista.vista_native

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.EntryPointAccessors
import ir.coffevista.vista_native.core.database.profile.OwnProfileEntity
import ir.coffevista.vista_native.debug.DebugFeedRuntimeEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedRuntimeInstrumentationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearFeedCache() {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            DebugFeedRuntimeEntryPoint::class.java,
        )
        runBlocking {
            entryPoint.feedRepository().clearAccount(FIXTURE_ACCOUNT)
            entryPoint.ownProfileDao().insertOrUpdate(
                OwnProfileEntity(
                    userId = FIXTURE_ACCOUNT,
                    username = "fixture",
                    fullName = "کاربر آزمایشی",
                    bio = null,
                    avatarUrl = null,
                    isVerified = false,
                    accountType = "personal",
                    postCount = 22,
                    followerCount = 0,
                    followingCount = 0,
                    updatedAt = "2026-07-27T17:21:00Z",
                ),
            )
        }
    }

    @Test
    fun validSessionCoversPaginationRefreshDetailTabAndRecreation() {
        launch("valid-session").use { scenario ->
            awaitText(postText(1))

            compose.onNodeWithTag("feed-list")
                .performScrollToNode(hasText(postText(15)))
            awaitCachedPostCount(22)
            compose.onNodeWithTag("feed-list")
                .performScrollToNode(hasText(postText(22)))
            awaitText(postText(22))

            compose.onNodeWithTag("feed-list")
                .performScrollToNode(hasText(postText(1)))
            val fixture = runtimeEntryPoint().debugFeedApiFixture()
            val firstPageRequestsBeforeRefresh = fixture.firstPageRequestCount()
            compose.onNodeWithTag("feed-pull-to-refresh")
                .performTouchInput { swipeDown() }
            compose.waitUntil(timeoutMillis = 10_000) {
                fixture.firstPageRequestCount() > firstPageRequestsBeforeRefresh
            }

            compose.onNodeWithTag("feed-media-fixture-post-1").performClick()
            awaitText("جزئیات پست")
            compose.onNodeWithTag("feed-like-count-fixture-post-1")
                .fetchSemanticsNode()
            compose.onNodeWithTag("feed-comment-count-fixture-post-1")
                .fetchSemanticsNode()
            compose.onNodeWithTag("post-detail-back").performClick()
            awaitText(postText(1))

            compose.onNodeWithContentDescription("جستجو").performClick()
            awaitText("زیرساخت جستجو آماده است")
            compose.onNodeWithContentDescription("خانه").performClick()
            awaitText(postText(1))

            scenario.recreate()
            awaitText(postText(1))

            compose.onNodeWithContentDescription("نمایه").performClick()
            awaitText("fixture")
            compose.onNodeWithContentDescription("خانه").performClick()
            awaitText(postText(1))
        }
    }

    @Test
    fun offlineScenarioUsesRoomCacheAfterProcessRecreation() {
        launch("valid-session").use {
            awaitText(postText(1))
        }

        launch("offline-valid-session").use { scenario ->
            awaitText("نمایش نسخه ذخیره‌شده")
            awaitText(postText(1))
            scenario.recreate()
            awaitText("نمایش نسخه ذخیره‌شده")
            awaitText(postText(1))
        }
    }

    @Test
    fun errorScenarioWithoutCacheShowsInitialError() {
        launch("feed-error").use {
            awaitText("debug feed offline")
        }
    }

    private fun launch(scenario: String): ActivityScenario<MainActivity> {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("vista.foundation.fixture", scenario)
        return ActivityScenario.launch(intent)
    }

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun awaitCachedPostCount(expected: Int) {
        val repository = runtimeEntryPoint().feedRepository()
        runBlocking {
            withTimeout(10_000) {
                repository.observeFeed(FIXTURE_ACCOUNT)
                    .first { it.posts.size == expected }
            }
        }
    }

    private fun runtimeEntryPoint(): DebugFeedRuntimeEntryPoint =
        EntryPointAccessors.fromApplication(
            context,
            DebugFeedRuntimeEntryPoint::class.java,
        )

    private fun postText(index: Int) =
        "پست آزمایشی شماره $index برای بررسی فید فقط‌خواندنی"

    private companion object {
        const val FIXTURE_ACCOUNT = "fnd-debug-user"
    }
}
