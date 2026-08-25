# Chat Flutter-to-Native Capability Matrix

**Fixture:** authenticated `ahmad` account, emulator-5554 (1080x2280, API 33)  
**Reference captures:** `artifacts/flutter-chat-parity-20260821-list.png`, `artifacts/flutter-chat-parity-20260821-raha.png`  
**Native captures:** `artifacts/native-chat-parity-20260821-list.png`, `artifacts/native-chat-parity-20260821-raha.png`

## Runtime result

Both applications displayed the same real `raha.81` conversation and the same message sequence from their offline-first stores. Live refresh and realtime could not be confirmed: the emulator has no functional DNS/network at capture time (`UnknownHostException` from the system and Native realtime client). This is an environment block, not evidence that either client has a server-pagination defect.

## Matrix

| Capability | Flutter source / runtime truth | Native source / runtime evidence | State / next action |
| --- | --- | --- | --- |
| Conversation list and real content | `ChatConversationsScreen.dart`; live capture shows `raha.81` preview | `ConversationListRoute` / `OfflineFirstChatRepository`; paired capture shows same title and preview | Implemented; connection state remains environment-blocked. |
| Detail loading / older pages | `chat_messages_provider.dart`, `chat_repository_impl.dart` | `MessagesViewModel.refresh/loadOlder` -> `OfflineFirstChatRepository.fetchMessages`; paired `raha.81` messages match cache | No local data-loss repro. Retest live pagination after DNS is restored. |
| Keyboard | Flutter isolates wallpaper/list with `KeyboardStableMediaQuery`; capture reached IME-open state but live reconnect UI obscured lower area | Root column formerly consumed IME insets, remeasuring `LazyColumn`; changed to graphics-layer composer translation | Fixed in source; targeted build/device proof still pending. |
| Text composer / emoji / GIF / attachment | `animated_chat_input.dart`, `vista_emoji_panel.dart` | `Composer`, `MediaQuickPanel`, `ChatAttachmentBottomSheet` | Present; verify every picker action on a live network device. |
| Message actions | Flutter long-press, reply, forward, edit/delete, reactions | `MessageBubble`, `MessageActionsSheet`, `SwipeToReplyLayout` | Present in source; manual action sweep pending. |
| Voice hold / cancel / lock | Flutter state machine and waveform in `animated_chat_input.dart` | `VoiceRecorderDock` with MediaRecorder and waveform | Hardened state mutation and added haptic start/lock; manual permission + cancel/lock verification pending. |
| Header and connection error treatment | Flutter uses compact header and transient connection state | Native paired capture uses taller header plus persistent reconnect banner/snackbar | Visual mismatch retained for deliberate follow-up; do not tune against an offline fixture. |

## Safety and implementation notes

- The source contains no copied upstream code. Public-source review informed only general principles: stable list identity/content types and isolated keyboard animation work.
- No credentials, tokens, or message content were written to code or diagnostic logs. Runtime screenshots contain user-visible conversation data and remain local artifacts.
- The Native real-time error was `UnknownHostException`; do not mask it as an empty conversation or destructive cache refresh.
