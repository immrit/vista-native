# Search UX Unification, Conversation List & Realtime Overhaul

**Type:** bug | ui | perf | refactor
**Status:** done
**Progress:** `[██████████] 100%`

## Problem / Goal
1. **Search Screen Screen-Hop / Duplicate UX**: Clicking on the search box gives the impression of a duplicate screen opening where the user must tap the search box a second time to start typing. In addition, a back arrow was shown on the root search screen. Goal: Unified search tab (`SearchWorkspaceScreen`) with magnifying glass leading icon (no back arrow on root), instant focus, and soft keyboard opening on the very first tap.
2. **Missing Regular Chat with Ahmad in Conversation List**: The user has an active non-secret conversation with Ahmad with real messages (`Salam Ahmad`, `jjjj`, `تست`, etc.), but only the secret chat (`🔒 پیام محرمانه`) appeared in `پیام‌ها`. The regular chat only opened when navigating to Ahmad's profile and tapping "پیام". Goal: Both regular and secret chats must appear cleanly and simultaneously in the conversation list without cannibalizing each other.
3. **Conversations Realtime Update & Sorting**: When new messages arrive or are sent, the conversation list must immediately reflect the update and sort the newest conversation to the top with Telegram-grade speed.
4. **Keyboard Management**: Exiting chat or switching tabs must immediately dismiss the soft keyboard.

## Root cause / Design
1. **Search**:
   - `VistaShell.kt` passed `{}` instead of `null` for `onBack` in `ShellRoutes.SearchRoot`. `SearchScreens.kt` evaluated `showBackIcon = onBack != null`, causing a back arrow to show even when query was empty.
   - `ShellFeatureContent.kt` had non-nullable `onBack: () -> Unit` for `searchWorkspace`. Making it `(() -> Unit)? = null` allows `SearchRoot` to pass `null`.
   - In `SearchScreens.kt`, `SearchField` click requests focus and opens keyboard on first tap.
2. **Missing Regular Chat & Conversation Resolution**:
   - In `ChatDatabase.kt:283` (`conversation`), `WHERE id = :conversationId` matched the existing SECRET chat when queried with Ahmad's `userId`.
   - In `ChatDatabase.kt:597` (`mergeMessageAndConversation`), `current` found the secret chat and updated its preview with ciphertext instead of keeping the private chat separate.
   - In `OfflineFirstChatRepository.kt:312` (`refreshSuggestedUsers`), `runCatching { api.deleteConversation(redId) }` was deleting conversations on the remote backend!
   - In `recoverOrphanedConversations` (`OfflineFirstChatRepository.kt:2345`), if any message had `contentKind == "encrypted_unavailable"`, it misclassified the conversation as `SECRET`. Text messages must be classified as `PRIVATE`.
   - In `MessagesViewModel.kt`: If `bind(conversationId)` receives a peer `userId`, it must resolve the canonical conversation in the background and migrate local messages.
   - In `ChatScreens.kt` `ChatProfileBottomSheet`: The "پیام" action inside secret chat must switch to regular chat.
3. **Realtime Ordering**:
   - In `ChatDatabase.kt`, Room query orders by `isPinned DESC, lastMessageAtEpochMillis DESC`. Bumping `lastMessageAtEpochMillis` to `maxOf(message.createdAtEpochMillis, mutationAtEpochMillis, base.lastMessageAtEpochMillis ?: 0L)` ensures Room immediately emits the conversation sorted to the top.

## Phases

### Phase 1 — Investigate & Root-Cause Tracing
- [x] Reproduce duplicate search screen and lack of focus
- [x] Trace missing regular chat with Ahmad vs secret chat on live device
- [x] Trace realtime event and conversation recency sorting
- [x] Verify keyboard dismiss on destination change

### Phase 2 — Search UX Unification
- [x] Make `searchWorkspace` `onBack` nullable in `ShellFeatureContent.kt`
- [x] Make `ShellRoutes.SearchRoot` pass `null` for `onBack` in `VistaShell.kt`
- [x] Set `showBackIcon = onBack != null` in `SearchScreens.kt`
- [x] Ensure single-tap opens keyboard instantly on `SearchField`

### Phase 3 — Conversation List, Ahmad Regular Chat & Realtime Fixes
- [x] Fix `ChatDatabase.kt` `mergeMessageAndConversation` to respect conversation type and prevent SECRET/PRIVATE collision
- [x] Remove remote `api.deleteConversation(redId)` from `OfflineFirstChatRepository.kt`
- [x] Fix `recoverOrphanedConversations` to classify text messages as `PRIVATE`
- [x] Add canonical conversation resolution in `MessagesViewModel.kt`
- [x] Wire "پیام" action in `ChatProfileBottomSheet` to open regular chat

### Phase 4 — Verification & Release Build
- [x] Run unit tests: `:feature:search:testDebugUnitTest`, `:feature:chat:testDebugUnitTest` (ALL PASSED)
- [x] Build and install APK via `:app:installProductionDebug` on `emulator-5554`
- [x] Verify search single-tap UX, instant keyboard opening, and search glyph icon without back arrow
- [x] Verify both regular and secret chat for Ahmad coexist simultaneously in conversations list
- [x] Verify instant sorting to top on new message send & delivery
- [x] Verify keyboard automatically dismisses when exiting chat or switching tabs

## Decisions & Notes
- E:\vista remains strictly read-only. All changes in E:\vista_native.
- RTL/Persian strings and dark mode compliance maintained.
