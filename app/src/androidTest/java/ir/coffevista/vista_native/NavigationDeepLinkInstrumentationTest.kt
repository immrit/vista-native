package ir.coffevista.vista_native

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.coffevista.vista_native.navigation.DeepLinkDeliveryState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationDeepLinkInstrumentationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun controlledFailureRouteRestoresAfterActivityRecreation() {
        composeRule.runOnUiThread {
            composeRule.activity.authenticationStateOwner.accept(
                ir.coffevista.vista_native.core.model.session.AuthenticatedContext(
                    userId = "test", profileCompleted = true, passwordRequired = false, offline = false, displayName = "Test"
                )
            )
            composeRule.activity.deepLinkCoordinator.onSessionResolved(authenticated = false)
            composeRule.activity.deepLinkCoordinator.submit(
                rawUri = "vista://chat-detail/legacy-route",
                authenticated = true,
            )
        }
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("این لینک توسط ویستا پشتیبانی نمی‌شود.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("این لینک توسط ویستا پشتیبانی نمی‌شود.")
            .assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("این لینک توسط ویستا پشتیبانی نمی‌شود.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("این لینک توسط ویستا پشتیبانی نمی‌شود.")
            .assertIsDisplayed()
    }

    @Test
    fun duplicateWarmDeliveryCreatesOnlyOneNavigationEffect() {
        composeRule.runOnUiThread {
            composeRule.activity.deepLinkCoordinator.onSessionResolved(authenticated = false)
            repeat(2) {
                composeRule.activity.deepLinkCoordinator.submit(
                    rawUri = "vista://unknown/42",
                    authenticated = true,
                )
            }
        }
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("این لینک توسط ویستا پشتیبانی نمی‌شود.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("این لینک توسط ویستا پشتیبانی نمی‌شود.")
            .assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("این لینک توسط ویستا پشتیبانی نمی‌شود.")
            .assertDoesNotExist()
    }

    @Test
    fun pendingColdDestinationSurvivesRecreationAndReplaysOnceAfterLogin() {
        composeRule.runOnUiThread {
            composeRule.activity.deepLinkCoordinator.submit(
                rawUri = "vista://post/post_42",
                authenticated = false,
            )
        }
        val pendingId = composeRule.activity.deepLinkCoordinator.state.value.pendingId()

        composeRule.activityRule.scenario.recreate()

        val restored = composeRule.activity.deepLinkCoordinator.state.value
        org.junit.Assert.assertEquals(pendingId, restored.pendingId())

        composeRule.runOnUiThread {
            if (restored is DeepLinkDeliveryState.PendingSession) {
                composeRule.activity.deepLinkCoordinator.onSessionResolved(authenticated = false)
            }
            composeRule.activity.authenticationStateOwner.accept(
                ir.coffevista.vista_native.core.model.session.AuthenticatedContext(
                    userId = "test", profileCompleted = true, passwordRequired = false, offline = false, displayName = "Test"
                )
            )
            composeRule.activity.deepLinkCoordinator.onAuthenticationChanged(authenticated = true)
        }
        composeRule.waitUntil(5000) {
            composeRule.onAllNodesWithText("این پست در دسترس نیست").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("این پست در دسترس نیست")
            .assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText("این پست در دسترس نیست")
            .assertDoesNotExist()
    }
}

private fun DeepLinkDeliveryState.pendingId(): Long = when (this) {
    is DeepLinkDeliveryState.PendingSession -> id
    is DeepLinkDeliveryState.PendingAuthentication -> id
    else -> error("Expected a pending deep-link state, got $this")
}
