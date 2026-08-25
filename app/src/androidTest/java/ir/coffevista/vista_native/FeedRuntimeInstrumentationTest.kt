package ir.coffevista.vista_native

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

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
            compose.onNodeWithTag("post-detail-content")
                .performScrollToNode(androidx.compose.ui.test.hasTestTag("feed-like-count-fixture-post-1"))
            compose.onNodeWithTag("feed-like-count-fixture-post-1")
                .fetchSemanticsNode()
            compose.onNodeWithTag("feed-comment-count-fixture-post-1")
                .fetchSemanticsNode()
            compose.onNodeWithTag("post-detail-back").performClick()
            awaitText(postText(1))

            compose.onNodeWithContentDescription("جستجو").performClick()
            awaitText("جستجوی کاربران")
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
            awaitText("به‌روزرسانی فید ناموفق بود")
            compose.onNodeWithTag("feed-initial-error").assertIsDisplayed()
        }
    }

    @Test
    fun shellTypographyAndInsetsRemainStableAcrossSupportedFontScales() {
        val originalScale = shell("settings get system font_scale").trim().ifBlank { "1.0" }
        try {
            listOf("1.0", "1.3", "1.5", "2.0").forEach { scale ->
                shell("settings put system font_scale $scale")
                awaitDeviceFontScale(scale.toFloat())
                launch("valid-session").use { scenario ->
                    awaitFontScale(scenario, scale.toFloat())
                    awaitText(postText(1))

                    compose.onNodeWithTag("feed-app-bar").assertIsDisplayed()
                    compose.onNodeWithTag("feed-tabs").assertIsDisplayed()
                    compose.onNodeWithTag("feed-follow-fixture-post-1").assertIsDisplayed()
                    assertNoOverlap(
                        compose.onAllNodesWithTag("feed-author-fixture-author-2")[0]
                            .fetchSemanticsNode().boundsInRoot,
                        compose.onNodeWithTag("feed-follow-fixture-post-1")
                            .fetchSemanticsNode().boundsInRoot,
                        "author/follow overlap at font scale $scale",
                    )

                    val systemInsets = arrayOfNulls<androidx.core.graphics.Insets>(1)
                    scenario.onActivity { activity ->
                        val root = activity.window.decorView
                        systemInsets[0] = ViewCompat.getRootWindowInsets(root)
                            ?.getInsets(WindowInsetsCompat.Type.systemBars())
                        assertSystemBarColors(activity, "#F8F9FF", "#FFFFFF")
                    }
                    val appBarTop = compose.onNodeWithTag("feed-app-bar")
                        .fetchSemanticsNode().boundsInRoot.top
                    assertTrue(
                        "status inset missing at font scale $scale",
                        appBarTop >= (systemInsets[0]?.top ?: 0),
                    )
                    ShellTabTags.forEach { tag -> compose.onNodeWithTag(tag).assertIsDisplayed() }
                    compose.onNodeWithTag("shell-tab-feed").assertIsSelected()
                }
            }
        } finally {
            shell("settings put system font_scale $originalScale")
        }
    }

    @Test
    fun shellSystemBarsMatchFlutterLightAndDarkContract() {
        val originalNight = shell("cmd uimode night").substringAfter(':', "no").trim()
            .takeIf { it in setOf("yes", "no", "auto") } ?: "no"
        try {
            launch("valid-session").use { scenario ->
                listOf(
                    "no" to ("#F8F9FF" to "#FFFFFF"),
                    "yes" to ("#09090F" to "#09090F"),
                ).forEach { (mode, colors) ->
                    shell("cmd uimode night $mode")
                    awaitDeviceNightMode(mode == "yes")
                    scenario.recreate()
                    awaitNightMode(scenario, mode == "yes")
                    awaitText(postText(1))
                    scenario.onActivity { activity ->
                        assertSystemBarColors(activity, colors.first, colors.second)
                    }
                }
            }
        } finally {
            shell("cmd uimode night $originalNight")
        }
    }

    @Test
    fun shellImeInsetsRemainSingleConsumedAcrossOpenAndClose() {
        launch("valid-session").use { scenario ->
            awaitText(postText(1))
            compose.onNodeWithTag("shell-tab-search").performClick()
            compose.onNodeWithTag("search-launcher-field").assertIsDisplayed()
            val closedPlacement = captureBottomIslandPlacement(scenario)
            compose.onNodeWithTag("search-launcher-field").performClick()
            compose.onNodeWithTag("search-workspace").assertIsDisplayed()
            compose.onNodeWithTag("search-field").assertIsDisplayed()

            awaitImeVisibility(scenario, visible = true)
            awaitBottomIslandMovement(scenario, closedPlacement)
            val openPlacement = captureBottomIslandPlacement(scenario)
            assertSingleImeMovement(closedPlacement, openPlacement)

            compose.onNodeWithTag("search-field").performTextInput("vista")
            compose.onNodeWithTag("search-field").performImeAction()
            awaitImeVisibility(scenario, visible = false)
            awaitBottomIslandRestored(scenario, closedPlacement)
            val restoredPlacement = captureBottomIslandPlacement(scenario)
            assertTrue(
                "bottom island did not return after IME close",
                kotlin.math.abs(restoredPlacement.tabBottom - closedPlacement.tabBottom) <=
                    8f * restoredPlacement.density,
            )
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

    private fun awaitFontScale(scenario: ActivityScenario<MainActivity>, expected: Float) {
        compose.waitUntil(timeoutMillis = 10_000) {
            var actual = 0f
            scenario.onActivity { actual = it.resources.configuration.fontScale }
            kotlin.math.abs(actual - expected) < 0.01f
        }
    }

    private fun awaitImeVisibility(
        scenario: ActivityScenario<MainActivity>,
        visible: Boolean,
    ) {
        compose.waitUntil(timeoutMillis = 10_000) {
            var actual = false
            scenario.onActivity { activity ->
                actual = ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            }
            actual == visible
        }
    }

    private fun captureBottomIslandPlacement(
        scenario: ActivityScenario<MainActivity>,
    ): BottomIslandPlacement {
        val tabBottom = compose.onNodeWithTag("shell-tab-search")
            .fetchSemanticsNode().boundsInRoot.bottom
        var placement: BottomIslandPlacement? = null
        scenario.onActivity { activity ->
            val root = activity.window.decorView
            val insets = ViewCompat.getRootWindowInsets(root)
            val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            val navigation = insets
                ?.getInsets(WindowInsetsCompat.Type.navigationBars())
                ?.bottom ?: 0
            placement = BottomIslandPlacement(
                tabBottom = tabBottom,
                imeInset = ime,
                navigationInset = navigation,
                density = activity.resources.displayMetrics.density,
            )
        }
        return checkNotNull(placement)
    }

    private fun awaitBottomIslandMovement(
        scenario: ActivityScenario<MainActivity>,
        closed: BottomIslandPlacement,
    ) {
        compose.waitUntil(timeoutMillis = 10_000) {
            val open = captureBottomIslandPlacement(scenario)
            val movement = closed.tabBottom - open.tabBottom
            open.imeInset > open.navigationInset && movement >= open.imeInset * 0.60f
        }
    }

    private fun awaitBottomIslandRestored(
        scenario: ActivityScenario<MainActivity>,
        closed: BottomIslandPlacement,
    ) {
        compose.waitUntil(timeoutMillis = 10_000) {
            val restored = captureBottomIslandPlacement(scenario)
            kotlin.math.abs(restored.tabBottom - closed.tabBottom) <= 8f * restored.density
        }
    }

    private fun assertSingleImeMovement(
        closed: BottomIslandPlacement,
        open: BottomIslandPlacement,
    ) {
        val movement = closed.tabBottom - open.tabBottom
        val minimum = open.imeInset * 0.60f
        val maximum = open.imeInset * 1.35f
        assertTrue(
            "IME movement=$movement expected=$minimum..$maximum",
            open.imeInset > open.navigationInset && movement in minimum..maximum,
        )
    }

    private fun awaitDeviceFontScale(expected: Float) {
        awaitDeviceConfiguration {
            kotlin.math.abs(context.resources.configuration.fontScale - expected) < 0.01f
        }
    }

    private fun awaitDeviceNightMode(dark: Boolean) {
        val expected = if (dark) "yes" else "no"
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            val actual = shell("cmd uimode night").substringAfter(':', "").trim()
            if (actual == expected) return
            SystemClock.sleep(100)
        }
        val actual = shell("cmd uimode night").substringAfter(':', "").trim()
        assertEquals("device night mode did not settle", expected, actual)
    }

    private fun awaitDeviceConfiguration(predicate: () -> Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (predicate()) return
            SystemClock.sleep(100)
        }
        assertTrue("device configuration did not settle before launch", predicate())
    }

    private fun awaitNightMode(scenario: ActivityScenario<MainActivity>, dark: Boolean) {
        val expected = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        compose.waitUntil(timeoutMillis = 10_000) {
            var actual = Configuration.UI_MODE_NIGHT_UNDEFINED
            scenario.onActivity {
                actual = it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            }
            actual == expected
        }
    }

    private fun assertNoOverlap(first: Rect, second: Rect, message: String) {
        assertTrue(message, first.intersect(second).isEmpty)
    }

    @Suppress("DEPRECATION")
    private fun assertSystemBarColors(
        activity: MainActivity,
        statusBarColor: String,
        navigationBarColor: String,
    ) {
        if (Build.VERSION.SDK_INT >= 35) {
            assertEquals(Color.TRANSPARENT, activity.window.statusBarColor)
            assertEquals(Color.TRANSPARENT, activity.window.navigationBarColor)
        } else {
            assertEquals(statusBarColor.toColorInt(), activity.window.statusBarColor)
            assertEquals(navigationBarColor.toColorInt(), activity.window.navigationBarColor)
        }
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.bufferedReader().readText()
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
        val ShellTabTags = listOf(
            "shell-tab-feed",
            "shell-tab-search",
            "shell-tab-services",
            "shell-tab-chat",
            "shell-tab-profile",
        )
    }

    private data class BottomIslandPlacement(
        val tabBottom: Float,
        val imeInset: Int,
        val navigationInset: Int,
        val density: Float,
    )
}
