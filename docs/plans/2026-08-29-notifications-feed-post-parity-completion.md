# Notifications, Feed, Post Detail Parity Completion Plan

Date: 2026-08-29

## Goal

Bring Native Vista to strict Flutter parity for:
- Feed screen
- Post detail screen
- Like/bookmark/share behavior
- Comments and nested replies
- Notification bell, unread badge, and full notification list screen
- Notification-driven navigation to post, comment, reply, profile, chat, follow request, suggestions, and moderation appeal flows

Flutter remains the behavioral and visual source of truth.

## Current Status

Runtime comparison status: `PARTIAL / BLOCKED`

Known blockers:
- Fresh Native APK was not produced in the previous runtime run because Gradle wrapper execution failed before Kotlin compilation.
- Fresh Flutter APK also failed to build because Gradle wrapper networking failed.
- Current pixel comparison used installed APKs and live dynamic feed data, so it cannot be treated as deterministic acceptance.
- ADB transport became `offline` after captures, so fresh post-patch runtime validation has not happened.

Recently implemented Native fixes:
- Feed story tray first item now matches Flutter's `استوری جدید` add-story tile.
- Feed story tray keeps actual active story users in the player index list.
- Feed tab typography, story tray height, FAB side/offset, verified badge shape, notification badge UI, and notification bell callback were improved.

## Flutter Reference Inventory

Primary files:
- `E:\vista\lib\features\posts\screens\ExploreFeedScreen.dart`
- `E:\vista\lib\features\posts\screens\PostDetailPage.dart`
- `E:\vista\lib\features\posts\screens\notificationScreen.dart`
- `E:\vista\lib\provider\notification_provider.dart`
- `E:\vista\lib\model\notificationModel.dart`
- `E:\vista\lib\utils\comments_bottom_sheet.dart`
- `E:\vista\lib\widgets\comment_input_field.dart`
- `E:\vista\lib\services\notification_navigation_service.dart`
- `E:\vista\lib\widgets\verification_badge_icon.dart`
- `E:\vista\lib\features\stories\presentation\widgets\story_bar\story_bar.dart`

Native target areas:
- `E:\vista_native\feature\feed\src\main\java\ir\coffevista\vista_native\features\feed`
- `E:\vista_native\feature\shell\src\main\java\ir\coffevista\vista_native\features\shell`
- `E:\vista_native\app\src\main\java\ir\coffevista\vista_native\navigation`
- `E:\vista_native\app\src\main\java\ir\coffevista\vista_native\notifications`

## Phase 1 - Stabilize Verification Path

Status: `pending`

Tasks:
- Restore a reliable Native Gradle build path without deleting user work.
- Verify whether the Gradle wrapper distribution is already cached locally; if not, fix wrapper/network access explicitly.
- Restore ADB/emulator from `offline` to `device`.
- Build and install fresh Native production debug APK.
- Build and install fresh Flutter debug/reference APK.
- Confirm package identities before install:
  - Flutter: `ir.coffevista.vista`
  - Native: `ir.coffevista.vista_native.production`
- Capture baseline screenshots after login settle.

Acceptance:
- Build logs prove current source was compiled.
- APK timestamps are newer than modified source files.
- ADB reports `device`.
- Both apps launch to feed with the same authenticated account.

## Phase 2 - Native Notification Data Layer

Status: `implemented / build-blocked`

Implement Flutter-equivalent notification domain:
- `NotificationModel` equivalent with fields:
  - `id`
  - `senderId`
  - `recipientId`
  - `content`
  - `createdAt`
  - `type`
  - `username`
  - `fullName`
  - `userIsVerified`
  - `avatarUrl`
  - `postId`
  - `commentId`
  - `parentCommentId`
  - `isRead`
  - `verificationType`
  - `openScreen`
  - `conversationId`
  - `followerId`
  - `deeplink`
  - `metadata`
- Canonical type mapping:
  - `post_like` -> `like`
  - `new_comment`, `post_comment` -> `comment`
  - `reply_comment` -> `comment_reply`
  - `comment_mention`, `post_mention`, `mention` -> `mention`
  - `new_message`, `chat_message` -> `message`
  - `message_reaction` -> `reaction`
  - `suggested_follow` -> `suggest_follow`
  - `suggested_post` -> `suggest_post`
- REST API:
  - `GET /notifications?limit=20&offset=N`
  - `POST /notifications/read-all`
  - `POST /notifications/{id}/read`
  - `DELETE /notifications`
  - `DELETE /notifications/{id}`
  - `POST /me/follow-request/respond`
- Repository behavior:
  - cache-first display if local data exists
  - refresh must not blank existing list on transient failure
  - offset pagination with duplicate-id merge protection
  - 429 cooldown
  - unread count flow
  - unread counts by filter flow
  - filtered notification flow
  - optimistic mark-read and follow-request removal

Acceptance:
- Unit tests cover canonical type mapping, filter counts, pagination merge, failed refresh preserving state, mark all read, mark one read, remove notification, follow request success, 404 already-handled.

Implementation notes:
- Added `NotificationApi`, `NotificationRepository`, `DefaultNotificationRepository`, and `NotificationModels` under the feed feature.
- Added REST endpoints for notifications, read state, delete, and follow-request responses.
- Added unread count and unread-count-by-filter flows.
- Added canonical type mapping and flexible metadata decoding.
- Compile/runtime verification is still blocked by Gradle wrapper HTTPS fetch failure.

## Phase 3 - Native Notifications Screen UI

Status: `implemented / build-blocked`

Create a full Flutter-equivalent notification list screen, not settings:
- App bar:
  - centered title `اعلان‌ها`
  - done-all action to mark all read
- Initial loading:
  - tabs shimmer
  - list shimmer with 48dp avatar, title/time placeholders, content lines
- Tabs:
  - `همه`
  - `درخواست‌ها`
  - `دنبال‌کننده‌ها`
  - `لایک‌ها`
  - `کامنت‌ها`
  - `پاسخ‌ها`
  - `منشن‌ها`
  - `پیشنهادها`
- ButtonsTabBar visual parity:
  - selected primary pill
  - unselected grey pill
  - icon + text
  - red unread badge per tab
  - 40dp height, 18dp radius, compact horizontal padding
- List:
  - grouped headers: `امروز`, `این هفته`, `قدیمی‌تر`
  - batch like notifications for same post within 12h
  - unread row highlight
  - 3dp leading primary indicator for unread rows
  - 48dp avatar with overlaid type icon at bottom-right
  - username + inline verified badge + unread dot
  - two-line content
  - relative time column width 96dp
  - divider inset equivalent to Flutter `right: 86, left: 14`
  - empty state with notification-off icon, text, and retry action
  - append loader at bottom when `hasMore`
  - pull-to-refresh
  - load-more threshold equivalent to Flutter `maxScrollExtent - 240`
- Follow request actions:
  - compact reject outlined button `رد`
  - compact accept filled button `پذیرفتن`
  - inline loading spinner
  - success/already-handled/error snackbar colors

Acceptance:
- Compose UI tests cover every tab, empty state, loading state, unread badge, batching, follow request buttons, mark all read, pagination loader.
- Screenshot tests or emulator screenshots cover light and dark mode.

Implementation notes:
- Added `NotificationsViewModel` and `NotificationsScreen`.
- Implemented app bar, mark-all-read action, horizontal pill tabs, unread tab badges, shimmer states, grouped list rows, batched likes, unread highlight, actor avatar/type icon overlay, follow-request actions, empty state, pull-to-refresh, and load-more.
- Compose UI tests and screenshot verification are still pending because the module does not currently compile in this environment.

## Phase 4 - Notification Navigation

Status: `implemented / build-blocked`

Replace temporary bell route to settings with the real notification list route.

Implement Native equivalent of Flutter `NotificationNavigationService`:
- `like`, `comment`, `comment_reply`, `mention`, `suggest_post` -> post detail route.
- If `commentId` or `parentCommentId` exists, post detail must open/comment-focus or scroll to comment where feasible.
- `follow`, `follow_request_accepted`, actor fallback from metadata -> profile route.
- `follow_request` -> notification list row action, actor avatar tap opens profile.
- `message`, `new_message`, `chat_message` -> chat route using `conversationId`.
- `daily_suggestion_digest`, `suggest_follow` -> relevant discover/search/user suggestions route, or a clearly matched existing Native route.
- moderation/appeal notifications -> appeal route if Native has it; otherwise create the missing route.
- mark unread notifications as read before navigation, including all members of a batched like row.

Acceptance:
- Navigation contract tests for every notification type and metadata fallback.
- Manual emulator run proves bell opens notification list, row tap navigates correctly, avatar tap opens actor profile.

Implementation notes:
- Added `ShellRoutes.Notifications`.
- Added real `notifications` content slot to `ShellFeatureContent`.
- Feed bell now navigates to `NotificationsScreen`, not notification settings.
- Notification rows route to post detail, profile, chat detail, search workspace/suggestions, and appeal/post detail fallback based on notification type and metadata.

## Phase 5 - Feed Badge Wiring

Status: `implemented / build-blocked`

Wire notification unread count into feed app bar:
- Add notification ViewModel or shared repository flow.
- Feed `NotificationBell` receives real unread count, not default `0`.
- Badge format matches Flutter:
  - hidden when count is zero
  - `99+` above 99
  - red pill/circle with white bold 10sp text
  - position top-end with negative offset
- Opening notifications marks all as read only after screen initialization, matching Flutter.

Acceptance:
- Unit test for unread count updates.
- UI test for zero count hidden, small count visible, `99+` visible.
- Runtime screenshot shows badge parity.

Implementation notes:
- FeedViewModel now exposes `unreadNotificationCount`.
- FeedScreen passes the unread count into the bell badge UI.
- Runtime screenshot proof is pending until build and emulator are restored.

## Phase 6 - Feed Visual Parity Pass

Status: `pending`

Tighten mismatches found in the runtime contact sheet:
- Header vertical positioning and logo size.
- TabRow height, indicator width, indicator y-position, divider color.
- Story tray create tile, active story ring, username width, verified badge for story users.
- Post header density:
  - avatar size and spacing
  - username weight
  - date color and position
  - follow button width/height/radius
- Post content text:
  - font size
  - line height
  - max lines
  - hashtag/mention color and tap targets
- Action row:
  - icon order under RTL
  - 19dp icon size
  - count spacing
  - hidden like/comment count behavior
  - bookmark active state
  - share action opens bottom sheet
- FAB:
  - bottom-start placement
  - bottom offset against shell nav
  - shadow/overlap
- Bottom navigation:
  - Flutter-style translucent/blurred island
  - selected tab container
  - icon sizes and badge placement
  - system nav bar inset interaction

Acceptance:
- Pixel diff after deterministic fixture or same live feed position improves materially and no structural mismatch remains.
- All text fits in Persian RTL.

## Phase 7 - Post Detail Parity

Status: `pending`

Audit and complete post detail against Flutter:
- Header title/back behavior.
- Media/gallery/video/music rendering.
- Like optimistic toggle and rollback.
- Double-tap like semantics where relevant.
- Bookmark state.
- Share bottom sheet parity.
- Owner menu:
  - edit post
  - delete post
  - hide/show like count
  - hide/show comment count
- Non-owner menu:
  - report post
  - not interested
- Moderation banner and appeal action.
- Comment section placement and IME behavior.

Acceptance:
- Unit/UI tests for like/bookmark/share/menu visibility.
- Runtime screenshots for owner and non-owner post.

## Phase 8 - Comments and Replies Parity

Status: `pending`

Complete comment behavior:
- Fetch root comments and nested replies.
- Load more comments.
- Load replies per parent.
- Reply state with username preview.
- Cancel reply.
- Submit root comment.
- Submit nested reply with `parentCommentId`.
- Edit own comment.
- Delete own comment.
- Report others' comments.
- Mention parsing and mention suggestions if Flutter has them active in this flow.
- Empty/loading/error states.
- Pull-to-refresh.
- IME-safe composer positioning.

Acceptance:
- Repository tests for `COMMENT_NOT_FOUND`, nested replies, edit/delete/report failures.
- Compose UI tests for reply composer state, cancel, submit, edit, delete, report.
- Runtime screenshot/video of keyboard open on post detail and comments bottom sheet.

## Phase 9 - Share Parity

Status: `pending`

Validate and complete share:
- Feed action row share button opens Native share bottom sheet.
- Post detail share opens same sheet.
- Copy link uses canonical URL matching Flutter/backend expectation.
- External Android chooser uses `ACTION_SEND`.
- Direct app shortcuts follow Flutter SmartShare behavior where feasible.
- Add-to-story route from share sheet creates a story draft/template equivalent.
- Share event tracking happens once per completed share action.

Acceptance:
- UI test or manual capture for share sheet.
- Intent interception test for `ACTION_SEND`.
- Copy-link test.

## Phase 10 - Deterministic Pixel Gate

Status: `pending`

Create a strict comparison setup:
- Prefer same authenticated account and same live feed position.
- If live data is unstable, create a test-only deterministic fixture mode that uses identical post/story/notification/comment payloads in both Flutter and Native without shipping it in production.
- Capture these states:
  - feed for-you
  - feed following
  - notifications all tab
  - notifications requests tab
  - post detail
  - comments sheet
  - reply composer active
  - share sheet
  - report dialog
  - owner edit dialog
- Generate contact sheets and amplified diffs.
- Keep raw screenshots under `E:\vista_native\artifacts\screenshots\YYYY-MM-DD-*`.

Acceptance:
- Every captured state has source APK timestamp proof.
- Every diff is linked in the audit report.
- Any remaining mismatch is either fixed or explicitly accepted by the user.

## Phase 11 - Documentation and Closeout

Status: `pending`

Update:
- `docs/audits/2026-08-29-feed-runtime-pixel-compare.md`
- this plan file
- a final gap ledger with:
  - Done
  - Runtime verified
  - Build blocked
  - Visual mismatch
  - Product decision needed

Completion criteria:
- Fresh Native source builds.
- Fresh Flutter source builds or an explicitly approved reference APK is used.
- Emulator run is completed after install.
- Pixel comparison is feed-to-feed, detail-to-detail, notification-to-notification.
- Like/comment/reply/share/notification flows are manually replayed or covered by instrumentation.
