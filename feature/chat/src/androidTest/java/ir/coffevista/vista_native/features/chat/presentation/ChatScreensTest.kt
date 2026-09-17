package ir.coffevista.vista_native.features.chat.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.swipe
import androidx.compose.material3.Text
import androidx.test.platform.app.InstrumentationRegistry
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ir.coffevista.vista_native.core.designsystem.theme.VistaTheme
import ir.coffevista.vista_native.features.chat.domain.model.Conversation
import ir.coffevista.vista_native.features.chat.domain.model.ConversationType
import ir.coffevista.vista_native.features.chat.domain.model.BlockStatus
import ir.coffevista.vista_native.features.chat.domain.model.ChatPartnerProfile
import ir.coffevista.vista_native.features.chat.domain.model.ChatUser
import ir.coffevista.vista_native.features.chat.domain.model.Message
import ir.coffevista.vista_native.features.chat.domain.model.MessageContent
import ir.coffevista.vista_native.features.chat.domain.model.MessageStatus
import ir.coffevista.vista_native.features.chat.domain.model.ProfileNote
import ir.coffevista.vista_native.features.chat.domain.model.GroupInfo
import ir.coffevista.vista_native.features.chat.domain.model.GroupMember
import ir.coffevista.vista_native.features.chat.domain.model.Attachment
import ir.coffevista.vista_native.features.chat.domain.model.AttachmentKind
import ir.coffevista.vista_native.features.chat.domain.model.TransferState
import ir.coffevista.vista_native.features.chat.domain.model.DownloadState
import ir.coffevista.vista_native.features.chat.domain.model.DownloadTask
import ir.coffevista.vista_native.features.chat.presentation.conversations.ConversationsUiState
import ir.coffevista.vista_native.features.chat.presentation.messages.MessagesUiState
import ir.coffevista.vista_native.features.chat.presentation.newmessage.NewMessageScreen
import ir.coffevista.vista_native.features.chat.presentation.newmessage.NewMessageUiState
import ir.coffevista.vista_native.features.chat.presentation.components.SwipeToReplyLayout
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ChatScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun conversationUnreadAndTypingHaveAccessibleState() {
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = listOf(
                            Conversation(
                                accountId = "account",
                                id = "conversation",
                                type = ConversationType.PRIVATE,
                                title = "کاربر نمونه",
                                avatarUrl = null,
                                peerId = "peer",
                                lastMessage = "نمونه",
                                lastMessageAtEpochMillis = 1,
                                unreadCount = 2,
                                isArchived = false,
                                isPinned = false,
                                isMuted = false,
                                requestStatus = null,
                                typingUserIds = setOf("peer"),
                            ),
                        ),
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                )
            }
        }
        compose.onNodeWithContentDescription("گفتگو با کاربر نمونه", substring = true).assertExists()
        compose.onNodeWithText("در حال نوشتن...", substring = true).assertExists()
    }

    @Test
    fun outgoingInboxPreviewShowsRealReadReceipt() {
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = listOf(
                            conversationFixture().copy(
                                isLastMessageFromMe = true,
                                lastMessageStatus = MessageStatus.READ,
                            ),
                        ),
                        isInitialLoading = false,
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                )
            }
        }

        compose.onNodeWithContentDescription("خوانده شد").assertIsDisplayed()
    }

    @Test
    fun cachedConversationBannerOffersARealRetryAction() {
        var refreshCount = 0
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = listOf(conversationFixture()),
                        isInitialLoading = false,
                        isOffline = true,
                    ),
                    onRefresh = { refreshCount += 1 },
                    onLoadMore = {},
                    onOpenConversation = {},
                )
            }
        }

        compose.onNodeWithText("نمایش گفتگوهای ذخیره‌شده").assertIsDisplayed()
        compose.onNodeWithText("تلاش مجدد").performClick()
        compose.runOnIdle { assertEquals(1, refreshCount) }
    }

    @Test
    fun conversationsMenuOffersSecretChatEntryPoint() {
        var newMessageRequested = false
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(isInitialLoading = false),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                    onNewMessage = { newMessageRequested = true },
                )
            }
        }

        compose.onNodeWithContentDescription("گزینه‌های پیام‌ها").performClick()
        compose.onNodeWithText("گفتگوی محرمانه جدید").performClick()
        compose.runOnIdle { assertTrue(newMessageRequested) }
    }

    @Test
    fun newMessageSecretModeChangesTheEntryScreen() {
        var screenState by mutableStateOf(NewMessageUiState())
        compose.setContent {
            VistaTheme {
                NewMessageScreen(
                    state = screenState,
                    onBack = {},
                    onQueryChanged = {},
                    onSecretModeChanged = { enabled -> screenState = screenState.copy(isSecretMode = enabled) },
                    onUser = {},
                )
            }
        }

        compose.onNodeWithText("گفتگوی محرمانه جدید").performClick()
        compose.onNodeWithText("گفتگوی محرمانه").assertIsDisplayed()
    }

    @Test
    fun newMessageSearchCanBeClearedAndRetriedWithoutStaleSuggestions() {
        var clearedQuery: String? = null
        var retryCount = 0
        compose.setContent {
            VistaTheme {
                NewMessageScreen(
                    state = NewMessageUiState(
                        query = "raha",
                        searchResults = emptyList(),
                        isLoading = false,
                        isSearching = false,
                        error = "جستجو انجام نشد",
                        canRetry = true,
                    ),
                    onBack = {},
                    onQueryChanged = { clearedQuery = it },
                    onSecretModeChanged = {},
                    onRetry = { retryCount += 1 },
                    onUser = {},
                )
            }
        }

        compose.onNodeWithText("نتیجه‌ای یافت نشد").assertIsDisplayed()
        compose.onNodeWithText("جستجو انجام نشد").assertIsDisplayed()
        compose.onNodeWithContentDescription("پاک کردن جستجو").performClick()
        compose.onNodeWithText("تلاش مجدد").performClick()
        compose.runOnIdle {
            assertEquals("", clearedQuery)
            assertEquals(1, retryCount)
        }
    }
    @Test
    fun newGroupShowsRemovableSelectedMemberAndHonestLimit() {
        val member = ChatUser("peer", "peer", "Raha", null)
        var removedId: String? = null
        compose.setContent {
            VistaTheme {
                NewMessageScreen(
                    state = NewMessageUiState(
                        suggestedUsers = listOf(member),
                        isLoading = false,
                        isGroupMode = true,
                        groupName = "Team",
                        selectedUserIds = setOf(member.id),
                        selectedUsers = listOf(member),
                    ),
                    onBack = {},
                    onQueryChanged = {},
                    onSecretModeChanged = {},
                    onGroupModeChanged = {},
                    onGroupNameChanged = {},
                    onUser = { removedId = it.id },
                )
            }
        }

        compose.onNodeWithText("اعضای انتخاب‌شده: 1 از ۱۹").assertIsDisplayed()
        compose.onNodeWithContentDescription("حذف Raha").performClick()
        compose.onAllNodesWithText("گفتگوی محرمانه جدید").assertCountEquals(0)
        compose.runOnIdle { assertEquals(member.id, removedId) }
    }

    @Test
    fun conversationLongPressStartsFlutterMultiSelection() {
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = listOf(conversationFixture()),
                        isInitialLoading = false,
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                )
            }
        }

        compose.onNodeWithContentDescription("گفتگو با کاربر نمونه").performTouchInput { longClick() }
        compose.onNodeWithText("1 انتخاب شده").assertExists()
        compose.onNodeWithContentDescription("سنجاق کردن گفتگوهای انتخاب شده").assertExists()
        compose.onNodeWithContentDescription("بی‌صدا کردن گفتگوهای انتخاب شده").assertExists()
        compose.onNodeWithContentDescription("بایگانی گفتگوهای انتخاب شده").assertExists()
        compose.onNodeWithContentDescription("حذف گفتگوهای انتخاب شده").assertExists()
    }

    @Test
    fun archivedFolderHasSpecificEmptyState() {
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = emptyList(),
                        isInitialLoading = false,
                        includeArchived = true,
                        hasMore = false,
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                )
            }
        }

        compose.onNodeWithText("هیچ گفتگوی بایگانی‌شده‌ای وجود ندارد").assertIsDisplayed()
        compose.onNodeWithText("گفتگوهایی که بایگانی می‌کنید اینجا نمایش داده می‌شوند").assertIsDisplayed()
    }

    @Test
    fun archivedConversationExposesBadgeAndUnarchiveSelectionAction() {
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = listOf(conversationFixture().copy(isArchived = true)),
                        isInitialLoading = false,
                        includeArchived = true,
                        hasMore = false,
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                )
            }
        }

        compose.onNodeWithContentDescription("بایگانی‌شده").assertIsDisplayed()
        compose.onNodeWithContentDescription("گفتگو با کاربر نمونه", substring = true)
            .performTouchInput { longClick() }
        compose.onNodeWithContentDescription("خارج کردن گفتگوهای انتخاب شده از بایگانی").assertIsDisplayed()
    }

    @Test
    fun messageRequestsAreGroupedAndExposeAcceptRejectActions() {
        var acceptedId: String? = null
        var rejectedId: String? = null
        val request = conversationFixture().copy(
            id = "request-conversation",
            isMessageRequest = true,
            requestStatus = "pending",
        )
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = listOf(request, conversationFixture()),
                        isInitialLoading = false,
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                    onAcceptMessageRequest = { acceptedId = it },
                    onRejectMessageRequest = { rejectedId = it },
                )
            }
        }

        compose.onNodeWithText("درخواست پیام").assertIsDisplayed()
        compose.onNodeWithText("قبول").performClick()
        compose.runOnIdle { assertEquals(request.id, acceptedId) }
        compose.onNodeWithText("رد").performClick()
        compose.runOnIdle { assertEquals(request.id, rejectedId) }
    }

    @Test
    fun privateConversationSwipeExposesConfirmedBlockAction() {
        var preparedPeer: String? = null
        var toggledPeer: String? = null
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = listOf(conversationFixture()),
                        isInitialLoading = false,
                        blockStatusByUserId = mapOf(
                            "fixture-peer" to BlockStatus(isBlocked = false, isBlockedBy = false),
                        ),
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                    onPrepareBlockStatus = { preparedPeer = it },
                    onToggleBlock = { peerId, complete ->
                        toggledPeer = peerId
                        complete(true)
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("گفتگو با کاربر نمونه").performTouchInput {
            swipe(
                start = Offset(width - 1f, center.y),
                end = Offset(1f, center.y),
                durationMillis = 500,
            )
        }
        compose.onNodeWithText("مسدود").performClick()
        compose.runOnIdle { assertEquals("fixture-peer", preparedPeer) }
        compose.onAllNodesWithText("مسدود کردن")[1].performClick()
        compose.runOnIdle { assertEquals("fixture-peer", toggledPeer) }
    }

    @Test
    fun failedMessageExposesRetryActionWithoutPrintingItsBody() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "client",
                                serverId = null,
                                senderId = "account",
                                content = MessageContent.Text("fixture"),
                                createdAtEpochMillis = 1,
                                status = MessageStatus.FAILED,
                                isMine = true,
                            ),
                        ),
                        isInitialLoading = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }
        compose.onNodeWithContentDescription("پیام شما، ارسال ناموفق؛ برای تلاش مجدد لمس کنید").assertExists()
        compose.onNodeWithText("پیام...").assertExists()
    }

    @Test
    fun idleTextMessageTapOpensContextActions() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "client",
                                serverId = "server",
                                senderId = "account",
                                content = MessageContent.Text("متن نمونه"),
                                createdAtEpochMillis = 1,
                                status = MessageStatus.SENT,
                                isMine = true,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithContentDescription("پیام شما، ارسال شد").performClick()
        compose.onNodeWithText("پاسخ").assertExists()
        compose.onNodeWithText("کپی").assertExists()
        compose.onAllNodesWithText("1 انتخاب شده").assertCountEquals(0)
    }

    @Test
    fun openingContextMenuWhileImeIsOpenKeepsComposerAndBubbleGeometryStable() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "client",
                                serverId = "server",
                                senderId = "account",
                                content = MessageContent.Text("متن ثابت برای منوی پیام"),
                                createdAtEpochMillis = 1,
                                status = MessageStatus.SENT,
                                isMine = true,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        val composerNode = compose.onNodeWithTag("chat-composer-surface")
        val messageNode = compose.onNodeWithContentDescription("پیام شما، ارسال شد")
        val composerBeforeIme = composerNode.fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("پیام...").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            composerNode.fetchSemanticsNode().boundsInRoot.top < composerBeforeIme.top - 40f
        }
        compose.waitForIdle()

        val composerBeforeMenu = composerNode.fetchSemanticsNode().boundsInRoot
        val bubbleBeforeMenu = messageNode.fetchSemanticsNode().boundsInRoot
        messageNode.performClick()
        compose.waitForIdle()

        compose.onNodeWithText("پاسخ").assertExists()
        val composerAfterMenu = composerNode.fetchSemanticsNode().boundsInRoot
        val bubbleAfterMenu = messageNode.fetchSemanticsNode().boundsInRoot
        assertEquals(composerBeforeMenu.top, composerAfterMenu.top, 0.5f)
        assertEquals(composerBeforeMenu.bottom, composerAfterMenu.bottom, 0.5f)
        assertEquals(bubbleBeforeMenu.top, bubbleAfterMenu.top, 0.5f)
        assertEquals(bubbleBeforeMenu.bottom, bubbleAfterMenu.bottom, 0.5f)
    }

    @Test
    fun messageLongPressStartsSelectionWithAlignedSelectionToolbar() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        conversation = conversationFixture(),
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "mine",
                                serverId = "mine-server",
                                senderId = "account",
                                content = MessageContent.Text("پیام اول"),
                                createdAtEpochMillis = 2,
                                status = MessageStatus.SENT,
                                isMine = true,
                            ),
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "peer",
                                serverId = "peer-server",
                                senderId = "peer",
                                content = MessageContent.Text("پیام دوم"),
                                createdAtEpochMillis = 1,
                                status = MessageStatus.SENT,
                                isMine = false,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithContentDescription("پیام شما، ارسال شد").performTouchInput { longClick() }
        compose.onNodeWithText("1 انتخاب شده").assertExists()
        compose.onNodeWithContentDescription("پیام انتخاب شده، لغو انتخاب").assertExists()
        compose.onNodeWithContentDescription("پیام دریافتی، ارسال شد").performClick()
        compose.onNodeWithText("2 انتخاب شده").assertExists()
        val ownCheckbox = compose
            .onAllNodesWithTag("message-selection-checkbox:mine-server")
            .fetchSemanticsNodes()
            .single()
        val peerCheckbox = compose
            .onAllNodesWithTag("message-selection-checkbox:peer-server")
            .fetchSemanticsNodes()
            .single()
        assertEquals(ownCheckbox.boundsInRoot.left, peerCheckbox.boundsInRoot.left, 0.5f)
        val ownBubble = compose
            .onAllNodesWithContentDescription("پیام شما، ارسال شد")
            .fetchSemanticsNodes()
            .single()
        val peerBubble = compose
            .onAllNodesWithContentDescription("پیام دریافتی، ارسال شد")
            .fetchSemanticsNodes()
            .single()
        assertTrue(
            "Outgoing bubbles must remain physically right of incoming bubbles in RTL.",
            ownBubble.boundsInRoot.left > peerBubble.boundsInRoot.left,
        )
        compose.onNodeWithContentDescription("کپی پیام‌های انتخاب شده").assertExists()
        compose.onNodeWithContentDescription("فوروارد پیام‌های انتخاب شده").assertExists()
        compose.onNodeWithContentDescription("حذف پیام‌های انتخاب شده").assertExists()
    }

    @Test
    fun composerRemainsPinnedAcrossTenEmojiKeyboardSwitches() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = emptyList(),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        // Start from a real, measured IME footprint. A plain click can return
        // before Android 13 has animated the IME, so wait for the composer to
        // move before freezing the baseline. This exercises the same geometry
        // path that production uses when switching to the emoji panel.
        val composerNode = compose.onNodeWithTag("chat-composer-surface")
        val composerBeforeIme = composerNode.fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("پیام...").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            composerNode.fetchSemanticsNode().boundsInRoot.top < composerBeforeIme.top - 40f
        }
        compose.waitForIdle()
        val baseline = composerNode.fetchSemanticsNode().boundsInRoot

        repeat(10) { switchIndex ->
            val control = if (switchIndex % 2 == 0) "شکلک‌ها" else "صفحه‌کلید"
            compose.onNodeWithContentDescription(control).performClick()
            compose.waitForIdle()
            val current = composerNode.fetchSemanticsNode().boundsInRoot
            assertEquals(
                "Composer top moved on switch ${switchIndex + 1}.",
                baseline.top,
                current.top,
                0.5f,
            )
            assertEquals(
                "Composer bottom moved on switch ${switchIndex + 1}.",
                baseline.bottom,
                current.bottom,
                0.5f,
            )
        }
    }

    @Test
    fun peerMessageSwipeLeftStartsReply() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "peer-client",
                                serverId = "peer-server",
                                senderId = "peer",
                                content = MessageContent.Text("متن دریافتی برای پاسخ"),
                                createdAtEpochMillis = 1,
                                status = MessageStatus.READ,
                                isMine = false,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithContentDescription("پیام دریافتی، خوانده شد")
            .performTouchInput {
                swipe(
                    start = Offset((center.x * 2f) - 1f, center.y),
                    end = Offset(1f, center.y),
                    durationMillis = 500,
                )
            }
        compose.onNodeWithText("پاسخ به", substring = true).assertExists()
        compose.onAllNodesWithText("متن دریافتی برای پاسخ")[0].assertExists()
    }

    @Test
    fun disabledSwipeDoesNotCompeteWithSelectionGestures() {
        var replyCount = 0
        compose.setContent {
            VistaTheme {
                SwipeToReplyLayout(
                    enabled = false,
                    onReply = { replyCount += 1 },
                ) {
                    Text("پیام انتخاب‌شده")
                }
            }
        }

        compose.onNodeWithText("پیام انتخاب‌شده").performTouchInput {
            swipe(
                start = Offset(1f, center.y),
                end = Offset((center.x * 2f) - 1f, center.y),
                durationMillis = 500,
            )
        }

        compose.runOnIdle { assertEquals(0, replyCount) }
    }

    @Test
    fun rightSwipeDoesNotStartReply() {
        var replyCount = 0
        compose.setContent {
            VistaTheme {
                SwipeToReplyLayout(onReply = { replyCount += 1 }) {
                    Text("پیام با حرکت چپ")
                }
            }
        }

        compose.onNodeWithText("پیام با حرکت چپ").performTouchInput {
            swipe(
                start = Offset(1f, center.y),
                end = Offset((center.x * 2f) - 1f, center.y),
                durationMillis = 500,
            )
        }

        compose.runOnIdle { assertEquals(0, replyCount) }
    }

    @Test
    fun ownMessageRepliesOnlyOnRightSwipe() {
        var replyCount = 0
        compose.setContent {
            VistaTheme {
                SwipeToReplyLayout(
                    bubbleOnRight = true,
                    onReply = { replyCount += 1 },
                ) {
                    Text("پیام خودم")
                }
            }
        }

        compose.onNodeWithText("پیام خودم").performTouchInput {
            swipe(
                start = Offset((center.x * 2f) - 1f, center.y),
                end = Offset(1f, center.y),
                durationMillis = 500,
            )
        }
        compose.runOnIdle { assertEquals(0, replyCount) }

        compose.onNodeWithText("پیام خودم").performTouchInput {
            swipe(
                start = Offset(1f, center.y),
                end = Offset((center.x * 2f) - 1f, center.y),
                durationMillis = 500,
            )
        }

        compose.runOnIdle { assertEquals(1, replyCount) }
    }

    @Test
    fun existingReactionChipTogglesThroughRepositoryAction() {
        var toggledEmoji: String? = null
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "reaction-client",
                                serverId = "reaction-server",
                                senderId = "peer",
                                content = MessageContent.Text("پیام واکنش"),
                                createdAtEpochMillis = 1,
                                status = MessageStatus.READ,
                                reactions = mapOf("❤️" to setOf("account", "peer")),
                                isMine = false,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                    onReact = { _, emoji -> toggledEmoji = emoji },
                )
            }
        }

        compose.onNodeWithText("❤️ 2").performClick()
        compose.runOnIdle { assertEquals("❤️", toggledEmoji) }
    }

    @Test
    fun blockFromConversationMenuRequiresConfirmation() {
        var blockedPeerId: String? = null
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        conversation = Conversation(
                            accountId = "account",
                            id = "conversation",
                            type = ConversationType.PRIVATE,
                            title = "کاربر نمونه",
                            avatarUrl = null,
                            peerId = "peer",
                            lastMessage = null,
                            lastMessageAtEpochMillis = 1,
                            unreadCount = 0,
                            isArchived = false,
                            isPinned = false,
                            isMuted = false,
                            requestStatus = null,
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "کاربر نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                    onToggleBlock = { peerId, complete ->
                        blockedPeerId = peerId
                        complete(true)
                    },
                )
            }
        }

        compose.onNodeWithContentDescription("گزینه‌های گفتگو").performClick()
        compose.onNodeWithText("مسدود کردن").performClick()
        compose.onNodeWithText("آیا از مسدود کردن کاربر نمونه اطمینان دارید؟").assertExists()
        assertEquals(null, blockedPeerId)
        compose.onNodeWithText("انصراف").performClick()
        assertEquals(null, blockedPeerId)
    }

    @Test
    fun partnerInfoMatchesFlutterActionsTabsAndRoutes() {
        var loadedProfilePeerId: String? = null
        var openedProfilePeerId: String? = null
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        conversation = Conversation(
                            accountId = "account",
                            id = "conversation",
                            type = ConversationType.PRIVATE,
                            title = "کاربر نمونه",
                            avatarUrl = null,
                            peerId = "peer",
                            lastMessage = null,
                            lastMessageAtEpochMillis = 1,
                            unreadCount = 0,
                            isArchived = false,
                            isPinned = false,
                            isMuted = false,
                            requestStatus = null,
                        ),
                        partnerProfile = ChatPartnerProfile(
                            userId = "peer",
                            username = "user_from_profile",
                            bio = "معرفی واقعی کاربر",
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "کاربر نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                    onLoadPartnerProfile = { loadedProfilePeerId = it },
                    onOpenProfile = { openedProfilePeerId = it },
                )
            }
        }

        compose.onNodeWithText("کاربر نمونه").performClick()
        assertEquals("peer", loadedProfilePeerId)
        compose.onNodeWithContentDescription("بازگشت از اطلاعات کاربر").assertExists()
        compose.onNodeWithText("@user_from_profile").assertExists()
        compose.onNodeWithText("معرفی واقعی کاربر").assertExists()
        compose.onNodeWithText("پیام").assertExists()
        compose.onNodeWithText("بی‌صدا").assertExists()
        compose.onNodeWithText("پروفایل").assertExists()
        compose.onNodeWithText("جستجو").assertExists()
        compose.onNodeWithText("پست‌ها").assertExists()
        compose.onNodeWithText("رسانه").assertExists()
        compose.onNodeWithText("فایل‌ها").assertExists()
        compose.onNodeWithText("لینک‌ها").assertExists()
        compose.onNodeWithText("صدا").assertExists()
        compose.onNodeWithText("GIF").assertExists()
        compose.onNodeWithText("گروه‌ها").assertExists()
        compose.onNodeWithText("هیچ پست‌هایی یافت نشد").assertExists()
        compose.onNodeWithText("صدا").performClick()
        compose.onNodeWithText("هیچ صدایی یافت نشد").assertExists()
        compose.onNodeWithText("پروفایل").performClick()
        assertEquals("peer", openedProfilePeerId)

        compose.onNodeWithText("کاربر نمونه").performClick()
        compose.onNodeWithText("جستجو").performClick()
        compose.onNodeWithText("جستجو در پیام‌ها...").assertExists()
    }

    @Test
    fun peerNoteOpensQuickReplySheet() {
        compose.setContent {
            VistaTheme {
                ConversationListScreen(
                    state = ConversationsUiState(
                        conversations = listOf(
                            Conversation(
                                accountId = "account",
                                id = "conversation",
                                type = ConversationType.PRIVATE,
                                title = "همتا",
                                avatarUrl = null,
                                peerId = "peer",
                                lastMessage = null,
                                lastMessageAtEpochMillis = 1,
                                unreadCount = 0,
                                isArchived = false,
                                isPinned = false,
                                isMuted = false,
                                requestStatus = null,
                            ),
                        ),
                        profileNotes = listOf(
                            ProfileNote("note", "peer", "یادداشت همتا", 1, Long.MAX_VALUE, false),
                        ),
                        isInitialLoading = false,
                        isNotesLoading = false,
                    ),
                    onRefresh = {},
                    onLoadMore = {},
                    onOpenConversation = {},
                )
            }
        }

        compose.onNodeWithText("یادداشت همتا").performClick()
        compose.onNodeWithText("پاسخ شما به این یادداشت...").assertExists()
        compose.onNodeWithText("ارسال پاسخ").assertExists()
    }

    @Test
    fun groupIncomingMessagesExposeSenderIdentityAtClusterEdges() {
        var openedProfile = ""
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "group",
                        conversation = Conversation(
                            accountId = "account",
                            id = "group",
                            type = ConversationType.GROUP,
                            title = "گروه نمونه",
                            avatarUrl = null,
                            peerId = null,
                            lastMessage = null,
                            lastMessageAtEpochMillis = 2,
                            unreadCount = 0,
                            isArchived = false,
                            isPinned = false,
                            isMuted = false,
                            requestStatus = null,
                        ),
                        groupMembers = listOf(
                            GroupMember("peer", "peer", "فرستنده نمونه", null, false, null),
                        ),
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "group",
                                clientId = "newest",
                                serverId = "newest",
                                senderId = "peer",
                                content = MessageContent.Text("دوم"),
                                createdAtEpochMillis = 2_000,
                                status = MessageStatus.READ,
                                isMine = false,
                            ),
                            Message(
                                accountId = "account",
                                conversationId = "group",
                                clientId = "oldest",
                                serverId = "oldest",
                                senderId = "peer",
                                content = MessageContent.Text("اول"),
                                createdAtEpochMillis = 1_000,
                                status = MessageStatus.READ,
                                isMine = false,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گروه نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                    onOpenProfile = { openedProfile = it },
                )
            }
        }

        compose.onAllNodesWithTag("group-message-sender:peer").assertCountEquals(1)
        compose.onAllNodesWithTag("group-message-avatar:peer").assertCountEquals(1)
        compose.onNodeWithTag("group-message-sender:peer").performClick()
        assertEquals("peer", openedProfile)
    }

    @Test
    fun mediaGroupRendersAsOneAlbumAndSelectsEveryBackingMessage() {
        val albumMessages = listOf(
            Message(
                accountId = "account",
                conversationId = "conversation",
                clientId = "newest-image",
                serverId = "newest-image",
                senderId = "peer",
                content = MessageContent.Text("کپشن آلبوم"),
                createdAtEpochMillis = 2_000,
                status = MessageStatus.READ,
                attachment = Attachment(
                    kind = AttachmentKind.IMAGE,
                    remoteUrl = "https://example.test/newest.jpg",
                    fileName = "newest.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 1L,
                    mediaGroupId = "album-1",
                ),
                isMine = false,
            ),
            Message(
                accountId = "account",
                conversationId = "conversation",
                clientId = "older-image",
                serverId = "older-image",
                senderId = "peer",
                content = MessageContent.Text(""),
                createdAtEpochMillis = 1_000,
                status = MessageStatus.READ,
                attachment = Attachment(
                    kind = AttachmentKind.IMAGE,
                    remoteUrl = "https://example.test/older.jpg",
                    fileName = "older.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = 1L,
                    mediaGroupId = "album-1",
                ),
                isMine = false,
            ),
        )
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        conversation = Conversation(
                            accountId = "account",
                            id = "conversation",
                            type = ConversationType.PRIVATE,
                            title = "گفتگو",
                            avatarUrl = null,
                            peerId = "peer",
                            lastMessage = null,
                            lastMessageAtEpochMillis = 2_000,
                            unreadCount = 0,
                            isArchived = false,
                            isPinned = false,
                            isMuted = false,
                            requestStatus = null,
                        ),
                        messages = albumMessages,
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onAllNodesWithTag("media-album").assertCountEquals(1)
        compose.onNodeWithTag("media-album-tile:newest-image").assertExists()
        compose.onNodeWithTag("media-album-tile:older-image").assertExists()
        compose.onNodeWithText("کپشن آلبوم").assertExists()
        compose.onNodeWithTag("media-album-tile:newest-image").performTouchInput { longClick() }
        compose.onNodeWithText("2 انتخاب شده").assertExists()
        compose.onNodeWithTag("media-album-tile:newest-image").performClick()
        compose.onNodeWithText("2 انتخاب شده").assertDoesNotExist()
    }

    @Test
    fun groupConversationOpensMemberAndInviteDetails() {
        var addedIds = emptyList<String>()
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "group",
                        conversation = Conversation(
                            accountId = "account",
                            id = "group",
                            type = ConversationType.GROUP,
                            title = "گروه نمونه",
                            avatarUrl = null,
                            peerId = null,
                            lastMessage = null,
                            lastMessageAtEpochMillis = 1,
                            unreadCount = 0,
                            isArchived = false,
                            isPinned = false,
                            isMuted = false,
                            requestStatus = null,
                        ),
                        groupInfo = GroupInfo("group", "گروه نمونه", null, 2, 20, "invite-code", true, true, "account", "account"),
                        groupMembers = listOf(
                            GroupMember("account", "owner", "سازنده نمونه", null, true, null),
                            GroupMember("peer", "peer", "عضو نمونه", null, false, null),
                        ),
                        groupUserResults = listOf(
                            ChatUser("add-peer", "new_peer", "عضو تازه", null),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گروه نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                    onAddGroupMembers = { addedIds = it },
                )
            }
        }

        compose.onNodeWithText("2 عضو").assertExists()
        compose.onNodeWithContentDescription("گزینه‌های گفتگو").performClick()
        compose.onNodeWithText("اطلاعات گروه").performClick()
        compose.onNodeWithTag("group-details-list").performScrollToNode(hasText("عضو نمونه"))
        compose.onNodeWithText("عضو نمونه").assertExists()
        compose.onNodeWithText("سازنده • شما").assertExists()
        compose.onNodeWithTag("group-admin-action:account").assertDoesNotExist()
        compose.onNodeWithTag("group-remove-action:account").assertDoesNotExist()
        compose.onNodeWithTag("group-admin-action:peer").performClick()
        compose.onNodeWithText("مدیر کردن عضو").assertExists()
        compose.onNodeWithText("انصراف").performClick()
        compose.onNodeWithText("کپی لینک").assertExists()
        compose.onNodeWithTag("group-details-list").performScrollToNode(hasText("انتخاب"))
        compose.onNodeWithText("انتخاب").performClick()
        compose.onNodeWithTag("group-details-list").performScrollToNode(hasText("عضو تازه"))
        compose.onNodeWithTag("group-add-candidate:add-peer").performClick()
        compose.onNodeWithTag("group-details-list").performScrollToNode(hasText("افزودن 1 عضو"))
        compose.onNodeWithTag("group-add-submit").performClick()
        assertEquals(listOf("add-peer"), addedIds)
        compose.onNodeWithTag("group-details-list").performScrollToNode(hasText("حذف گروه برای همه"))
        compose.onNodeWithText("حذف گروه برای همه").assertExists()
        compose.onNodeWithText("ترک گروه").assertDoesNotExist()
    }

    @Test
    fun groupAdminCannotManageCreatorOrSelfAndCannotDeleteGroup() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "group",
                        conversation = Conversation(
                            accountId = "admin",
                            id = "group",
                            type = ConversationType.GROUP,
                            title = "گروه نمونه",
                            avatarUrl = null,
                            peerId = null,
                            lastMessage = null,
                            lastMessageAtEpochMillis = 1,
                            unreadCount = 0,
                            isArchived = false,
                            isPinned = false,
                            isMuted = false,
                            requestStatus = null,
                        ),
                        groupInfo = GroupInfo("group", "گروه نمونه", null, 3, 20, "invite-code", true, true, "owner", "admin"),
                        groupMembers = listOf(
                            GroupMember("owner", "owner", "سازنده", null, true, null),
                            GroupMember("admin", "admin", "مدیر فعلی", null, true, null),
                            GroupMember("regular", "regular", "عضو عادی", null, false, null),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گروه نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithContentDescription("گزینه‌های گفتگو").performClick()
        compose.onNodeWithText("اطلاعات گروه").performClick()
        compose.onNodeWithTag("group-details-list").performScrollToNode(hasTestTag("group-admin-action:regular"))
        compose.onNodeWithTag("group-admin-action:owner").assertDoesNotExist()
        compose.onNodeWithTag("group-remove-action:owner").assertDoesNotExist()
        compose.onNodeWithTag("group-admin-action:admin").assertDoesNotExist()
        compose.onNodeWithTag("group-remove-action:admin").assertDoesNotExist()
        compose.onNodeWithTag("group-admin-action:regular").assertExists()
        compose.onNodeWithTag("group-remove-action:regular").performClick()
        compose.onNodeWithText("حذف عضو").assertExists()
        compose.onNodeWithText("انصراف").performClick()
        compose.onNodeWithTag("group-details-list").performScrollToNode(hasText("ترک گروه"))
        compose.onNodeWithText("ترک گروه").assertExists()
        compose.onNodeWithText("حذف گروه برای همه").assertDoesNotExist()
    }

    @Test
    fun replyAndForwardHeadersKeepTheirSenderNames() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "message",
                                serverId = "message",
                                senderId = "peer",
                                content = MessageContent.Text("متن جدید"),
                                createdAtEpochMillis = 2,
                                status = MessageStatus.READ,
                                replyToMessageId = "original",
                                replyToContent = "خط اول پاسخ\nخط دوم پاسخ",
                                replyToSenderName = "کاربر پاسخ",
                                isForwarded = true,
                                forwardedFromSenderName = "فرستنده اصلی",
                                isMine = false,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("فوروارد شده از فرستنده اصلی").assertExists()
        compose.onNodeWithText("پاسخ به کاربر پاسخ").assertExists()
        compose.onNodeWithText("خط اول پاسخ\nخط دوم پاسخ").assertExists()
    }

    @Test
    fun documentBubbleOpensPreviewActions() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "client-doc",
                                serverId = "server-doc",
                                senderId = "peer",
                                content = MessageContent.Text(""),
                                createdAtEpochMillis = 1,
                                status = MessageStatus.SENT,
                                attachment = Attachment(
                                    kind = AttachmentKind.DOCUMENT,
                                    remoteUrl = "https://example.test/fixture.pdf",
                                    fileName = "fixture.pdf",
                                    mimeType = "application/pdf",
                                    sizeBytes = 128,
                                ),
                                isMine = false,
                            ),
                        ),
                        downloads = mapOf(
                            "server-doc" to DownloadTask(
                                accountId = "account",
                                conversationId = "conversation",
                                messageId = "server-doc",
                                localUri = "file:///data/local/tmp/fixture.pdf",
                                mimeType = "application/pdf",
                                fileName = "fixture.pdf",
                                receivedBytes = 128,
                                totalBytes = 128,
                                state = DownloadState.COMPLETE,
                                attempts = 1,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("fixture.pdf").performClick()
        compose.onNodeWithText("باز کردن فایل").assertExists()
        compose.onNodeWithText("بستن").assertExists()
    }

    @Test
    fun cachedAudioOpensSeekDurationSpeedAndPlaybackControls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = File(context.cacheDir, "chat-player-fixture.wav")
        writeSilentWav(audio, durationSeconds = 3)
        try {
            compose.setContent {
                VistaTheme {
                    MessageDetailScreen(
                        state = MessagesUiState(
                            conversationId = "conversation",
                            messages = listOf(
                                Message(
                                    accountId = "account",
                                    conversationId = "conversation",
                                    clientId = "audio-client",
                                    serverId = "audio-server",
                                    senderId = "peer",
                                    content = MessageContent.Text(""),
                                    createdAtEpochMillis = 1,
                                    status = MessageStatus.READ,
                                    attachment = Attachment(
                                        kind = AttachmentKind.AUDIO,
                                        localUri = Uri.fromFile(audio).toString(),
                                        fileName = "fixture.wav",
                                        mimeType = "audio/wav",
                                        sizeBytes = audio.length(),
                                durationSeconds = 3,
                                    ),
                                    isMine = false,
                                ),
                            ),
                            isInitialLoading = false,
                            hasMore = false,
                        ),
                        title = "گفتگو",
                        onBack = {},
                        onRefresh = {},
                        onLoadOlder = {},
                        onSend = { _, _ -> },
                        onRetry = {},
                    )
                }
            }

            compose.onNodeWithContentDescription("پخش ویس").performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithContentDescription("مکث ویس").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("voice-playback-position").assertExists()
            compose.onNodeWithText("1X").assertExists()
            compose.onNodeWithContentDescription("مکث ویس").assertExists()
        } finally {
            audio.delete()
        }
    }

    @Test
    fun attachmentLongPressStartsSelectionWithoutTriggeringDownload() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "client-attachment-actions",
                                serverId = "server-attachment-actions",
                                senderId = "account",
                                content = MessageContent.Text(""),
                                createdAtEpochMillis = 1,
                                status = MessageStatus.READ,
                                attachment = Attachment(
                                    kind = AttachmentKind.DOCUMENT,
                                    remoteUrl = "https://example.test/actions.pdf",
                                    fileName = "actions.pdf",
                                    mimeType = "application/pdf",
                                    sizeBytes = 128,
                                ),
                                isMine = true,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("actions.pdf").performTouchInput { longClick() }
        compose.onNodeWithText("1 انتخاب شده").assertExists()
        compose.onNodeWithContentDescription("پیام انتخاب شده، لغو انتخاب").assertExists()
        compose.onNodeWithContentDescription("حذف پیام‌های انتخاب شده").assertExists()
    }

    @Test
    fun remoteAttachmentExposesPersistentDownloadProgressPauseAndCancel() {
        var paused = 0
        var cancelled = 0
        val message = Message(
            accountId = "account",
            conversationId = "conversation",
            clientId = "download-client",
            serverId = "download-server",
            senderId = "peer",
            content = MessageContent.Text(""),
            createdAtEpochMillis = 1,
            status = MessageStatus.DELIVERED,
            attachment = Attachment(
                kind = AttachmentKind.DOCUMENT,
                remoteUrl = "https://example.test/download.pdf",
                fileName = "download.pdf",
                mimeType = "application/pdf",
                sizeBytes = 100,
            ),
            isMine = false,
        )
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(message),
                        downloads = mapOf(
                            "download-server" to DownloadTask(
                                accountId = "account",
                                conversationId = "conversation",
                                messageId = "download-server",
                                localUri = null,
                                mimeType = "application/pdf",
                                fileName = "download.pdf",
                                receivedBytes = 50,
                                totalBytes = 100,
                                state = DownloadState.DOWNLOADING,
                                attempts = 1,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                    onPauseDownload = { paused += 1 },
                    onCancelDownload = { cancelled += 1 },
                )
            }
        }

        compose.onNodeWithText("50٪").assertExists()
        compose.onNodeWithContentDescription("توقف دانلود").performClick()
        compose.onNodeWithContentDescription("لغو دانلود").performClick()
        compose.runOnIdle {
            assertEquals(1, paused)
            assertEquals(1, cancelled)
        }
    }

    @Test
    fun deletedMessageNeverLeaksAttachmentOrReplyContent() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            Message(
                                accountId = "account",
                                conversationId = "conversation",
                                clientId = "deleted-client",
                                serverId = "deleted-server",
                                senderId = "account",
                                content = MessageContent.Deleted,
                                createdAtEpochMillis = 1,
                                status = MessageStatus.READ,
                                replyToContent = "reply must be hidden",
                                attachment = Attachment(
                                    kind = AttachmentKind.DOCUMENT,
                                    remoteUrl = "https://example.test/private.pdf",
                                    fileName = "private.pdf",
                                    mimeType = "application/pdf",
                                    sizeBytes = 128,
                                ),
                                reactions = mapOf("❤️" to setOf("peer")),
                                isMine = true,
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("این پیام حذف شده است").assertExists()
        assertEquals(0, compose.onAllNodesWithText("private.pdf").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("reply must be hidden").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("❤️ 1").fetchSemanticsNodes().size)
    }

    @Test
    fun activeTransferCanCancelAndFailedTransferCanRetryFromAttachmentSurface() {
        var cancelled = 0
        var retried = 0
        val base = Message(
            accountId = "account",
            conversationId = "conversation",
            clientId = "active",
            serverId = null,
            senderId = "account",
            content = MessageContent.Text(""),
            createdAtEpochMillis = 2,
            status = MessageStatus.PENDING,
            attachment = Attachment(
                kind = AttachmentKind.DOCUMENT,
                localUri = "content://fixture/active",
                fileName = "active.pdf",
                mimeType = "application/pdf",
                sizeBytes = 128,
                progress = 0.5f,
                transferState = TransferState.UPLOADING,
            ),
            isMine = true,
        )
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        messages = listOf(
                            base,
                            base.copy(
                                clientId = "failed",
                                createdAtEpochMillis = 1,
                                status = MessageStatus.FAILED,
                                attachment = base.attachment?.copy(
                                    fileName = "failed.pdf",
                                    progress = 0.3f,
                                    transferState = TransferState.FAILED,
                                ),
                            ),
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = { retried += 1 },
                    onCancelTransfer = { cancelled += 1 },
                )
            }
        }

        compose.onNodeWithContentDescription("لغو ارسال فایل").performClick()
        compose.onNodeWithText("failed.pdf").performClick()
        assertEquals(1, cancelled)
        assertEquals(1, retried)
    }

    @Test
    fun ownNewMessageSmoothlyReturnsHistoryToNewestItem() {
        val history = (1..24).map { index ->
            Message(
                accountId = "account",
                conversationId = "conversation",
                clientId = "history-$index",
                serverId = "server-$index",
                senderId = "peer",
                content = MessageContent.Text("history-$index"),
                createdAtEpochMillis = index.toLong(),
                status = MessageStatus.DELIVERED,
                isMine = false,
            )
        }.reversed()
        var screenState by mutableStateOf(
            MessagesUiState(
                conversationId = "conversation",
                messages = history,
                isInitialLoading = false,
                hasMore = false,
            ),
        )
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = screenState,
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }
        compose.onNodeWithTag("chat-message-list").performScrollToIndex(14)

        compose.runOnIdle {
            screenState = screenState.copy(
                messages = listOf(
                    Message(
                        accountId = "account",
                        conversationId = "conversation",
                        clientId = "new-own-message",
                        serverId = null,
                        senderId = "account",
                        content = MessageContent.Text("new-own-message"),
                        createdAtEpochMillis = 100,
                        status = MessageStatus.PENDING,
                        isMine = true,
                    ),
                ) + history,
            )
        }

        compose.waitForIdle()
        compose.onNodeWithText("new-own-message").assertIsDisplayed()
    }

    @Test
    fun microphoneActionUsesVoiceRecordingFlow() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(conversationId = "conversation", isInitialLoading = false, hasMore = false),
                    title = "گفتگو",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithContentDescription("نگه‌داشتن برای ضبط صدا").assertExists()
    }

    @Test
    fun secretConversationHidesUnencryptedMediaControls() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "secret-conversation",
                        conversation = conversationFixture().copy(
                            id = "secret-conversation",
                            type = ConversationType.SECRET,
                        ),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "کاربر نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("گفتگوی محرمانه • رمزگذاری سراسری فعال").assertExists()
        compose.onAllNodesWithContentDescription("پیوست فایل").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("نگه‌داشتن برای ضبط صدا").assertCountEquals(0)
    }

    @Test
    fun chatMenuOpensMessageSearchBar() {
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        conversation = conversationFixture(),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "کاربر نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithContentDescription("گزینه‌های گفتگو").performClick()
        compose.onNodeWithText("جستجو").performClick()
        compose.onNodeWithText("جستجو در پیام‌ها...").assertExists()
        compose.onNodeWithContentDescription("بستن جستجوی پیام‌ها").assertExists()
    }

    @Test
    fun privateConversationMenuOffersSecretChatEntryPoint() {
        var secretRequested = false
        compose.setContent {
            VistaTheme {
                MessageDetailScreen(
                    state = MessagesUiState(
                        conversationId = "conversation",
                        conversation = conversationFixture(),
                        isInitialLoading = false,
                        hasMore = false,
                    ),
                    title = "کاربر نمونه",
                    onBack = {},
                    onRefresh = {},
                    onLoadOlder = {},
                    onSend = { _, _ -> },
                    onRetry = {},
                    onStartSecretChat = { secretRequested = true },
                )
            }
        }

        compose.onNodeWithContentDescription("گزینه‌های گفتگو").performClick()
        compose.onNodeWithText("شروع گفتگوی محرمانه").performClick()
        compose.runOnIdle { assertTrue(secretRequested) }
    }

    private fun conversationFixture() = Conversation(
        accountId = "account",
        id = "conversation",
        type = ConversationType.PRIVATE,
        title = "کاربر نمونه",
        avatarUrl = null,
        peerId = "peer",
        lastMessage = "نمونه",
        lastMessageAtEpochMillis = 1,
        unreadCount = 0,
        isArchived = false,
        isPinned = false,
        isMuted = false,
        requestStatus = null,
    )
}

private fun writeSilentWav(target: File, durationSeconds: Int) {
    val sampleRate = 8_000
    val dataSize = sampleRate * durationSeconds * 2
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray())
        putInt(36 + dataSize)
        put("WAVEfmt ".toByteArray())
        putInt(16)
        putShort(1)
        putShort(1)
        putInt(sampleRate)
        putInt(sampleRate * 2)
        putShort(2)
        putShort(16)
        put("data".toByteArray())
        putInt(dataSize)
    }.array()
    target.outputStream().use { output ->
        output.write(header)
        output.write(ByteArray(dataSize))
    }
}
