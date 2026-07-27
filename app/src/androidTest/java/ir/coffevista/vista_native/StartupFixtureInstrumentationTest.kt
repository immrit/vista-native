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
        launch("offline-valid-session").use { scenario ->
            awaitText("به ویستا خوش آمدید")
            composeRule.onNodeWithText("به ویستا خوش آمدید")
                .assertIsDisplayed()

            scenario.recreate()

            awaitText("به ویستا خوش آمدید")
            composeRule.onNodeWithText("به ویستا خوش آمدید")
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
            assertRightBiased(titleBounds, rootBounds)

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
    fun logoutLeavesAuthenticatedBoundaryAndReturnsToAuth() {
        launch("valid-session").use { scenario ->
            awaitText("به ویستا خوش آمدید")

            scenario.onActivity { activity ->
                activity.startupFixtures.forEach { fixture -> fixture.configure("malformed") }
                activity.authenticationStateOwner.signOut()
            }

            awaitText("ورود به ویستا")
            composeRule.onNodeWithText("ورود به ویستا")
                .assertIsDisplayed()
        }
    }

    private fun launch(scenario: String): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("vista.foundation.fixture", scenario)
        return ActivityScenario.launch(intent)
    }

    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text)
                .fetchSemanticsNodes()
                .isNotEmpty()
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

    private fun assertRightBiased(element: Rect, root: Rect) {
        assertTrue(
            "Expected RTL title center ${element.center.x} to be right of root center ${root.center.x}",
            element.center.x > root.center.x,
        )
    }
}
