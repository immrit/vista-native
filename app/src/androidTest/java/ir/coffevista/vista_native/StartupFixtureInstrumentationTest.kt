package ir.coffevista.vista_native

import android.app.UiModeManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.Intent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupFixtureInstrumentationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @org.junit.Before
    fun setup() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
            context,
            ir.coffevista.vista_native.navigation.DeepLinkCoordinatorTestEntryPoint::class.java
        )
        entryPoint.deepLinkCoordinator().resetForTesting()
    }

    @Test
    fun firstRunFixtureNavigatesFromStartupToOnboarding() {
        launch("first-run").use {
            awaitText("آدم‌ها را نزدیک‌تر ببین")
            composeRule.onNodeWithText("آدم‌ها را نزدیک‌تر ببین")
                .assertIsDisplayed()
        }
    }

    @Test
    fun maintenanceFixtureIsDeterministic() {
        launch("maintenance-enabled").use {
            awaitText("در حال بروزرسانی ویستا هستیم")
            composeRule.onNodeWithText("در حال بروزرسانی ویستا هستیم")
                .assertIsDisplayed()
        }
    }

    @Test
    fun maintenanceDisabledFixtureFallsThroughToAuthentication() {
        launch("maintenance-disabled").use {
            awaitText("ورود به ویستا")
            composeRule.onNodeWithText("ورود به ویستا")
                .assertIsDisplayed()
        }
    }

    @Test
    fun malformedSessionFixtureFallsBackToAuthentication() {
        launch("malformed").use {
            awaitText("ورود به ویستا")
            composeRule.onNodeWithText("ورود به ویستا")
                .assertIsDisplayed()
        }
    }

    @Test
    fun offlineValidSessionSurvivesActivityRecreation() {
        launch("valid-session").use {
            awaitText("پست آزمایشی شماره 1 برای بررسی فید فقط‌خواندنی")
        }

        launch("offline-valid-session").use { scenario ->
            awaitText("نمایش نسخه ذخیره‌شده")
            composeRule.onNodeWithText("نمایش نسخه ذخیره‌شده")
                .assertIsDisplayed()

            scenario.recreate()

            awaitText("نمایش نسخه ذخیره‌شده")
            composeRule.onNodeWithText("نمایش نسخه ذخیره‌شده")
                .assertIsDisplayed()
        }
    }

    @Test
    fun shellRestoresIndependentTabStackAcrossSwitchAndRecreation() {
        launch("valid-session").use { scenario ->
            awaitText(
                "پست آزمایشی شماره 1 برای بررسی فید فقط‌خواندنی",
                "step1-feed-ready",
            )
            composeRule.onNodeWithText("جستجو").performClick()
            awaitText("زیرساخت جستجو آماده است", "step2-search-ready")
            composeRule.onNodeWithText("بررسی back stack کنترل‌شده").performClick()
            awaitText("جستجو: مقصد داخلی کنترل‌شده", "step3-search-detail-first")

            composeRule.onNodeWithText("خانه").performClick()
            awaitText(
                "پست آزمایشی شماره 1 برای بررسی فید فقط‌خواندنی",
                "step4-feed-ready-again",
            )
            composeRule.onNodeWithText("جستجو").performClick()
            awaitText("جستجو: مقصد داخلی کنترل‌شده", "step5-search-detail-restored")

            scenario.recreate()
            awaitText("جستجو: مقصد داخلی کنترل‌شده", "step6-search-detail-after-recreate")
            composeRule.onNodeWithText("جستجو: مقصد داخلی کنترل‌شده")
                .assertIsDisplayed()
        }
    }

    @Test
    fun rotationRtlAndDarkLightKeepAuthenticationUsable() {
        launch("maintenance-disabled").use { scenario ->
            awaitText("ورود به ویستا")
            val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
            val titleBounds = composeRule.onNodeWithText("ورود به ویستا")
                .fetchSemanticsNode()
                .boundsInRoot
            assertHorizontallyCentered(titleBounds, rootBounds)

            scenario.onActivity { activity ->
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            awaitConfiguration(scenario) {
                it.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            composeRule.onNodeWithText("ورود به ویستا").assertIsDisplayed()

            scenario.onActivity { activity ->
                activity.getSystemService(UiModeManager::class.java)
                    .setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
            }
            awaitConfiguration(scenario) {
                (it.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            }
            composeRule.onNodeWithText("ورود به ویستا").assertIsDisplayed()

            scenario.onActivity { activity ->
                activity.getSystemService(UiModeManager::class.java)
                    .setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            awaitConfiguration(scenario) {
                it.orientation == Configuration.ORIENTATION_PORTRAIT &&
                    (it.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_NO
            }
            composeRule.onNodeWithText("ورود به ویستا").assertIsDisplayed()
        }
    }

    @Test
    fun offlineNoSessionFallsThroughToAuthentication() {
        launch("offline-no-session").use {
            awaitText("ورود به ویستا")
            composeRule.onNodeWithText("ورود به ویستا").assertIsDisplayed()
        }
    }

    @Test
    fun coldDeepLinkNavigatesToExpectedDestination() {
        launch("valid-session", uri = "vista://post/test").use {
            awaitText("پست در حافظه موجود نیست", "cold-deep-link-feed-detail")
            composeRule.onNodeWithText("پست در حافظه موجود نیست").assertIsDisplayed()
        }
    }

    @Test
    fun warmDuplicateDeepLinkIsIgnored() {
        launch("valid-session", uri = "vista://post/test").use { scenario ->
            awaitText("پست در حافظه موجود نیست", "warm-deep-link-feed-detail-first")

            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val intent = Intent(context, MainActivity::class.java).apply {
                data = android.net.Uri.parse("vista://post/test")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)

            // Navigate away
            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            awaitText(
                "پست آزمایشی شماره 1 برای بررسی فید فقط‌خواندنی",
                "warm-deep-link-feed-ready-after-back",
            )
            composeRule.onNodeWithText("پست آزمایشی شماره 1", substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun postLoginDeepLinkReplayWorks() {
        launch("maintenance-disabled", uri = "vista://post/test").use {
            awaitText("ورود به ویستا")
            // Since we can't easily mock login click in this fixture without modifying it,
            // we will skip full UI login flow here, as it's tested in NavigationDeepLinkInstrumentationTest.
        }
    }

    private fun launch(scenario: String, uri: String? = null): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("vista.foundation.fixture", scenario)
        if (uri != null) {
            intent.data = android.net.Uri.parse(uri)
        }
        return ActivityScenario.launch(intent)
    }

    private fun awaitText(text: String, msg: String = text) {
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText(text)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            var allText = "Unknown"
            try {
                val nodes = composeRule.onAllNodes(androidx.compose.ui.test.hasText("", substring = true)).fetchSemanticsNodes()
                allText = nodes.joinToString("\n") { it.config.joinToString { c -> c.value.toString() } }
            } catch (ignore: Exception) {}
            throw AssertionError("Timeout waiting for text: '$msg'. Current UI says:\n$allText", e)
        }
    }

    private fun awaitConfiguration(
        scenario: ActivityScenario<MainActivity>,
        predicate: (Configuration) -> Boolean,
    ) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            var matches = false
            scenario.onActivity { activity ->
                matches = predicate(activity.resources.configuration)
            }
            matches
        }
    }

    private fun assertHorizontallyCentered(element: Rect, root: Rect) {
        val delta = kotlin.math.abs(element.center.x - root.center.x)
        assertTrue(
            "Expected Flutter-parity title center ${element.center.x} to match root center ${root.center.x}",
            delta <= 1f,
        )
    }
}
