package ir.coffevista.vista_native

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiltCompositionSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun realApplicationGraphInjectsMainActivity() {
        assertNotNull(composeRule.activity.authenticationStateOwner)
        assertNotNull(composeRule.activity.deepLinkCoordinator)
        assertNotNull(
            (composeRule.activity.application as VistaApplication).workerFactory,
        )
    }
}
