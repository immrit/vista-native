package ir.coffevista.vista_native

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import ir.coffevista.vista_native.debug.DebugPublicProfileRuntimeEntryPoint
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OtherUserProfileRuntimeInstrumentationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearCachesAndSeedOwnProfile() {
        val profileEntryPoint = profileEntryPoint()
        val feedEntryPoint = EntryPointAccessors.fromApplication(
            context,
            DebugFeedRuntimeEntryPoint::class.java,
        )
        runBlocking {
            profileEntryPoint.userProfileRepository().clearAccount(FIXTURE_ACCOUNT)
            feedEntryPoint.feedRepository().clearAccount(FIXTURE_ACCOUNT)
            feedEntryPoint.ownProfileDao().insertOrUpdate(
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
                    updatedAt = "2026-07-28T09:30:00Z",
                ),
            )
        }
    }

    @Test
    fun authorNavigationFollowUnfollowRollbackRefreshRecreationAndBack() {
        launch("valid-session").use { scenario ->
            awaitText(postText(1))
            compose.onNodeWithTag("feed-author-fixture-author-2").performClick()
            awaitText("کاربر آزمایشی 2")
            awaitText("دنبال کردن")
            awaitText("42")

            compose.onNodeWithText("دنبال کردن").performClick()
            awaitText("دنبال می‌کنید")
            awaitText("43")

            compose.onNodeWithText("دنبال می‌کنید").performClick()
            awaitText("دنبال کردن")
            awaitText("42")

            profileEntryPoint().debugPublicProfileApiFixture().failNextMutation()
            compose.onNodeWithText("دنبال کردن").performClick()
            awaitText("خطا در اتصال به سرور. لطفاً اینترنت خود را بررسی کنید")
            awaitText("دنبال کردن")
            awaitText("42")

            compose.onNodeWithTag("other-profile-content")
                .performTouchInput { swipeDown() }
            awaitText("کاربر آزمایشی 2")

            scenario.recreate()
            awaitText("نمایه کاربر")
            awaitText("کاربر آزمایشی 2")

            compose.onNodeWithTag("other-profile-back").performClick()
            awaitText(postText(1))
            compose.onNodeWithText("جستجو").performClick()
            awaitText("زیرساخت جستجو آماده است")
            compose.onNodeWithText("خانه").performClick()
            awaitText(postText(1))
        }
    }

    @Test
    fun offlineScenarioUsesViewerScopedProfileCache() {
        launch("valid-session").use {
            awaitText(postText(1))
            compose.onNodeWithTag("feed-author-fixture-author-2").performClick()
            awaitText("کاربر آزمایشی 2")
        }

        launch("offline-valid-session").use { scenario ->
            awaitText(postText(1))
            compose.onNodeWithTag("feed-author-fixture-author-2").performClick()
            awaitText("نمایش نسخه ذخیره‌شده")
            awaitText("کاربر آزمایشی 2")
            scenario.recreate()
            awaitText("نمایش نسخه ذخیره‌شده")
        }
    }

    @Test
    fun selfAuthorRedirectsToOwnProfileAndLogoutReturnsToAuth() {
        launch("valid-session").use {
            awaitText(postText(1))
            compose.onNodeWithTag("feed-list")
                .performScrollToNode(hasText(postText(2)))
            compose.onNodeWithTag("feed-author-fnd-debug-user").performClick()
            awaitText("نمایه شما")
            compose.onNodeWithText("خروج از حساب")
                .performScrollTo()
                .performClick()
            awaitText("ورود به ویستا")
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

    private fun profileEntryPoint(): DebugPublicProfileRuntimeEntryPoint =
        EntryPointAccessors.fromApplication(
            context,
            DebugPublicProfileRuntimeEntryPoint::class.java,
        )

    private fun postText(index: Int) =
        "پست آزمایشی شماره $index برای بررسی فید فقط‌خواندنی"

    private companion object {
        const val FIXTURE_ACCOUNT = "fnd-debug-user"
    }
}
