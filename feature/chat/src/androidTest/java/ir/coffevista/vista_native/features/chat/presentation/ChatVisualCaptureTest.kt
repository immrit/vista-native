package ir.coffevista.vista_native.features.chat.presentation

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ir.coffevista.vista_native.core.designsystem.theme.VistaTheme
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.presentation.conversations.ConversationsUiState
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesUiState
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatVisualCaptureTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun captureDeterministicProductionComposableStates() {
        var visual by mutableStateOf<VisualState>(VisualState.Conversation(ConversationsUiState()))
        var dark by mutableStateOf(false)
        var fontScale by mutableFloatStateOf(1f)

        compose.setContent {
            VisualTheme(dark = dark, fontScale = fontScale) {
                when (val state = visual) {
                    is VisualState.Conversation -> ConversationListScreen(
                        state = state.value,
                        onRefresh = {},
                        onLoadMore = {},
                        onOpenConversation = {},
                    )
                    is VisualState.Messages -> MessageDetailScreen(
                        state = state.value,
                        title = "گفتگوی نمونه",
                        onBack = {},
                        onRefresh = {},
                        onLoadOlder = {},
                        onSend = { _, accepted -> accepted() },
                        onRetry = {},
                    )
                }
            }
        }

        capture("native_01_loading") { visual = VisualState.Conversation(ConversationsUiState()) }
        capture("native_02_empty") {
            visual = VisualState.Conversation(
                ConversationsUiState(isInitialLoading = false, hasMore = false),
            )
        }
        capture("native_03_content") { visual = VisualState.Conversation(ChatVisualFixtures.conversationContent) }
        capture("native_04_unread") {
            visual = VisualState.Conversation(ChatVisualFixtures.conversationState("conversation-unread"))
        }
        capture("native_05_typing") {
            visual = VisualState.Conversation(ChatVisualFixtures.conversationState("conversation-typing"))
        }
        capture("native_06_long_text") {
            visual = VisualState.Conversation(
                ChatVisualFixtures.conversationState("conversation-long", "conversation-mixed"),
            )
        }
        capture("native_07_offline") { visual = VisualState.Conversation(ChatVisualFixtures.conversationOffline) }
        capture("native_08_error") {
            visual = VisualState.Conversation(
                ConversationsUiState(isInitialLoading = false, hasMore = false, error = "خطای آزمایشی"),
            )
        }
        capture("native_09_pagination") { visual = VisualState.Conversation(ChatVisualFixtures.conversationPagination) }
        capture("native_10_history_mixed") { visual = VisualState.Messages(ChatVisualFixtures.messageHistory) }
        capture("native_11_long_multiline") {
            visual = VisualState.Messages(ChatVisualFixtures.messageState("message-long", "message-emoji"))
        }
        capture("native_12_sending") {
            visual = VisualState.Messages(ChatVisualFixtures.messageState("message-sending"))
        }
        capture("native_13_delivered_read") {
            visual = VisualState.Messages(ChatVisualFixtures.messageState("message-read", "message-delivered"))
        }
        capture("native_14_failed_retry") {
            visual = VisualState.Messages(ChatVisualFixtures.messageState("message-failed"))
        }
        capture("native_15_reply") {
            visual = VisualState.Messages(ChatVisualFixtures.messageState("message-reply"))
        }
        capture("native_17_offline_reconnect") { visual = VisualState.Messages(ChatVisualFixtures.messageReconnect) }
        capture("native_18_font_scale_200", before = {
            fontScale = 2f
            visual = VisualState.Messages(ChatVisualFixtures.messageHistory)
        })
        capture("native_19_dark_theme", before = {
            fontScale = 1f
            dark = true
            visual = VisualState.Messages(ChatVisualFixtures.messageHistory)
        })

        compose.runOnIdle {
            dark = false
            visual = VisualState.Messages(ChatVisualFixtures.messageHistory)
        }
        compose.onNode(hasSetTextAction()).performClick().performTextInput("متن چندخطی نمونه")
        compose.waitForIdle()
        saveScreenshot("native_16_keyboard_open")
    }

    @Test
    fun composerTextSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = ChatVisualFixtures.messageHistory,
                    title = "گفتگوی نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, accepted -> accepted() },
                    onRetry = {},
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("متن بازیابی‌شده")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextContains("متن بازیابی‌شده")
        saveScreenshot("native_20_process_restored")
    }

    private fun capture(name: String, before: () -> Unit) {
        compose.runOnIdle(before)
        compose.waitForIdle()
        saveScreenshot(name)
    }

    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        SystemClock.sleep(250)
        val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir("chat-visual"))
        val target = File(directory, "$name.png")
        val bitmap = compose.onRoot(useUnmergedTree = true).captureToImage().asAndroidBitmap()
        FileOutputStream(target).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Composable
    private fun VisualTheme(dark: Boolean, fontScale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            VistaTheme(darkTheme = dark, content = content)
        }
    }

    private sealed interface VisualState {
        data class Conversation(val value: ConversationsUiState) : VisualState
        data class Messages(val value: MessagesUiState) : VisualState
    }
}
