# Native Chat & Messenger Feature Parity & Fluidity

**Type:** feature | ui | perf
**Status:** planning
**Progress:** `[░░░░░░░░░░] 0%`

## Problem / Goal
Implement complete, 1:1 visual and functional parity of the Vista Messenger (bakhsh-e payam-resan) from the Flutter codebase (`e:\vista`) into the Native Android Kotlin/Compose codebase (`e:\vista_native`). Ensure all touchable actions, flows, navigation, real credentials authentication (`ahmad`), conversations list, message detail screen, interactive message types, audio player, voice recorder, attachment sheets, and ultra-fluid animations matching modern top-tier messengers (smooth 60/120fps scrolling physics, interactive gestures, haptics, spring animations) are fully implemented, verified with actual screenshots and comparisons against the live Flutter reference.

## Root cause / Design
- **Flutter Reference Architecture:**
  - `lib/features/chat/screens/ChatConversationsScreen.dart`: Conversations list, Notes Tray (`NotesTray.dart`), unread badges, relative Shamsi date formatting, search bar with debounce, connection indicator, swipe-to-action (`SwipeableConversationItem.dart`), multi-select bar, and compose FAB.
  - `lib/features/chat/screens/modern_chat_screen.dart` + parts: Wallpaper doodle background (`vista_custom_bg.png`/`vista_custom_bg_dark.png`), reverse list with date dividers, custom bubble geometries (mine purple gradient vs peer white/dark surface), reply banners, forwarded previews, embedded shared posts, audio/voice notes waveform player, attachments (images, video, documents, GIF/stickers), reactions, swipe-to-reply gesture, action sheets, and animated composer dock.
  - `lib/features/chat/screens/new_message_screen.dart`, `group_create_screen.dart`, `group_details_screen.dart`: Group and secret chat creation, search contacts, group profile/settings.
- **Native Implementation Strategy:**
  - Enhance `:feature:chat` presentation (`ChatScreens.kt`, `ConversationsViewModel.kt`, `MessagesViewModel.kt`) to achieve exact 1:1 visual fidelity with Flutter design tokens, typography (Vazirmatn), geometries, and responsive RTL layout.
  - Integrate Telegram/Telegram X style fluid mechanics in Jetpack Compose:
    - Zero-jank LazyColumn with stable keys and immutable UI states.
    - Physics-based interactive swipe-to-reply with spring snap-back, progressive rotation, and haptic feedback (`HapticFeedbackConstants.CLOCK_TICK`).
    - Bubble entry animation (`slideInVertically` + `scaleIn` + `fadeIn`).
    - Morphing mic / send button with spring scale.
    - Audio waveform playback with animated amplitudes and speed toggle (1x / 1.5x / 2x).
    - Floating reaction bar with spring popping emoji.
    - Seamless IME / keyboard insets without jitter.
    - Header navigation to User Profile and Group Profile.

## Phases

### Phase 1 — Investigate & Reference Audit
- [ ] Capture live Flutter runtime reference screenshots for all messenger screens (Conversations list, Chat screen, Notes tray, Attachment sheet, Group profile).
- [ ] Audit native `:feature:chat` codebase against Flutter contracts and UI components.
- [ ] Verify test account authentication (`ahmad` / `1012@ahmad`) on Native.

### Phase 2 — Conversations Screen Parity & Polish
- [ ] Top bar: Persian title "پیام‌ها", live animated connection dot (green/orange/red), expandable search with debounce, 3-dots popup menu.
- [ ] Notes tray: Exact visual replica of story/notes row, note composer bottom sheet (60 char limit), friend note preview & quick reply bottom sheet.
- [ ] Conversation List & Items:
  - Exact avatar styling with online dot / group badges.
  - Persian typography (Vazirmatn), bold titles, relative Shamsi timestamp formatting.
  - Message status checkmarks (single sent, double seen) for outbound last messages.
  - Vibrant blue unread badge with Persian digits.
  - Interactive swipe actions (Archive, Pin, Mute, Delete) with colorful backgrounds and spring drag.
  - Long press multi-select mode with top selection action bar.
- [ ] Purple gradient compose FAB navigating to New Message / Group creation.

### Phase 3 — Chat Detail Screen Parity & Fluidity
- [ ] Wallpaper background with doodle pattern (`vista_custom_bg.png` / `vista_custom_bg_dark.png`) and dark theme adaptation.
- [ ] AppBar header: Back button, Avatar, Contact/Group Name, Subtitle (online status / last seen / "در حال نوشتن..." typing indicator / member count), pinned message banner, 3-dots menu.
- [ ] Header tap navigation to User Profile or Group Profile.
- [ ] Message Bubbles:
  - Geometry & colors: Outgoing purple gradient (`#6C5CE7` -> `#7C4DFF`) vs incoming clean surface bubble.
  - Reply quote preview inside bubble with colored vertical accent line (tap scrolls smoothly to target message with highlight animation).
  - Forwarded banner and shared post card integration.
  - Status indicators (clock, single check, double check).
  - Reaction pills attached to bubble with reactor count sheet.
  - Double-tap quick reaction (❤️).
- [ ] Rich Attachment Bubbles:
  - Images with rounded corners, loading placeholder, tap opens fullscreen image viewer with zoom.
  - Videos with duration badge and play overlay.
  - Voice / Audio note player with waveform visualization, scrubbable progress bar, play/pause, and 1x/1.5x/2x speed toggle.
  - Document / File bubble with icon, file size, download progress, and external open handler.
  - GIF & Sticker looping bubbles.
- [ ] Interactive Gestures & Animations:
  - Swipe to reply with spring physics, animated reply icon, and haptic feedback.
  - Long press context menu with reaction picker and actions (Reply, Copy, Forward, Pin, Edit, Delete).
  - Bubble entry animation with spring curve.
  - Scroll-to-bottom button with unread counter.
- [ ] Bottom Composer Dock:
  - Sliding reply / edit context banner with cancel button.
  - Paperclip attachment sheet (Gallery, Camera, File, Audio, Contact).
  - Emoji / Sticker / GIF panel with tabs and live search.
  - RTL expanding text input.
  - Voice recording bar (hold to record with animated pulsating waveform, swipe up to lock, swipe left to cancel) and smooth morph to Send button.

### Phase 4 — Verification & Parity Comparison
- [ ] Build and install debug APK on emulator.
- [ ] Log in with `ahmad` / `1012@ahmad` and load real conversation threads.
- [ ] Capture side-by-side screenshots of Native vs Flutter (Conversations list, Chat screen, Notes tray, Context menus, Input dock).
- [ ] Verify 0 compilation errors and smooth 60fps performance on all interactions.

## Decisions & Notes
- No mention of external messenger names in code identifiers, classes, or filenames as instructed.
- All Persian strings and formatting use standard Vazirmatn font metrics and Persian numeral digits.
