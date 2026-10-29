# Profile Post Actions & Moderation Parity with Flutter

**Type:** feature | ui | refactor
**Status:** completed
**Progress:** `[██████████] 100%`

## Problem / Goal
1. In the profile page of `vista_native` (`ProfileParityComponents.kt`), the action buttons row had incorrect placement (Bookmark was on the trailing side, Share was next to Comment, and 3-dots "More" button was misplaced in the author header instead of the action bar). It now matches the Feed page (`FeedScreen.kt`) and Flutter (`profileScreen.dart`): `[Like + count] [Comment + count] [Bookmark] [Share] [Spacer] [3-dots More]`.
2. The 3-dot "More" menu in Profile and Feed supports:
   - "حذف پست" (Delete post with confirmation dialog) for post owner OR Blue Tick moderators (`isCurrentUserPost || isBlueTick`).
   - "ویرایش پست" (Edit post dialog) for users with Premium privileges (`hasPremiumEdit`) OR "ویرایش ناظر" for Blue Tick moderators (`isBlueTick`), with max length 10,000 for Blue Tick and 1,000 for Premium.
   - "ویرایش پست" with Lock & Premium icon (`edit_locked`) when normal users tap it, prompting the Premium subscription dialog.
   - "کنترل آمار لایک/کامنت" (Engagement privacy toggle for owner with premium, or locked prompt).
   - "کپی متن" (Copy caption) and "گزارش پست" (Report post for other users).
3. Blue Tick users (moderators) can edit and delete ANY post (in Profile, Feed, and PostDetail), matching Flutter `E:/vista` and backend enforcement.

## Root cause / Design
- In `ProfileParityComponents.kt`, `ProfilePostCard` placed the 3-dots button inside the top author row (lines 765-812) and had an incomplete menu without edit or engagement controls. The bottom action row placed Bookmark after Spacer(weight 1f).
- In `FeedScreen.kt` and `ProfileParityComponents.kt`, edit and delete were only checking `isOwnPost`, ignoring Blue Tick badge / moderator role.
- `ProfilePostsViewModel` and `ProfileParityList` lacked edit/delete/report/visibility change callbacks and current user role/badge state.
- Backend `internal/posts/repository.go` and `service.go` constrained `DeletePost` and `UpdatePost` strictly to `user_id = $2`, which needed moderator allowance (`isModerator`).

## Phases

### Phase 1 — Investigate & Compare with Flutter
- [x] Trace Flutter `E:/vista` implementation in `profileScreen.dart`, `ExploreFeedScreen.dart`, and `standard_edit_post_dialog.dart`.
- [x] Trace `vista_native` action row in `FeedScreen.kt` vs `ProfileParityComponents.kt`.
- [x] Trace permissions logic for Blue Tick (`is_verified == true && verification_type == 'blueTick'`) and Premium.

### Phase 2 — Implement in vista_native
- [x] Align `ProfilePostCard` layout in `ProfileParityComponents.kt` with `FeedScreen.kt` action row:
      - Removed 3-dots from author row.
      - Ordered action row: Like -> Comment -> Bookmark -> Share -> Spacer -> 3-dots More.
- [x] Implement complete 3-dot menu in `ProfilePostCard`:
      - "گزارش پست" (for !isOwnPost)
      - "کپی متن" (for all posts with content)
      - "حذف پست" (for isOwnPost || isBlueTick) with confirmation dialog
      - "ویرایش پست" / "ویرایش ناظر" (for isBlueTick || hasPremiumEdit) opening `VistaEditPostDialog`
      - "ویرایش پست" (locked) for normal users (navigating to pricing/dialog)
      - Like / Comment visibility toggles for owner (or locked)
- [x] Update `ProfilePostsViewModel` to support `deletePost`, `updatePostContent`, `toggleEngagementVisibility`, and expose viewer's profile & moderation status (`isBlueTick`, `hasPremiumEdit`).
- [x] Connect callbacks in `OwnProfileScreen.kt`, `OtherUserProfileScreen.kt`, and `AppNavGraph.kt`.
- [x] Update `FeedScreen.kt` and `PostDetailScreen.kt` so Blue Tick moderators also have edit ("ویرایش ناظر") and delete options on other users' posts.

### Phase 3 — Backend Moderation Verification (vista-backend)
- [x] Updated `e:\vista-backend\internal\posts\repository.go` and `service.go` so `isModerator` (Blue Tick or admin/moderator role) can delete and update any post via `DELETE /v1/posts/{id}` and `PATCH /v1/posts/{id}` (marking `edited_by_vista = TRUE`, `moderator_id`, `moderated_at`).

### Phase 4 — Verify
- [x] Verify Gradle build / compilation of `vista_native` (`:app:compileBetaDebugKotlin` BUILD SUCCESSFUL).
- [x] Verify Go build of `vista-backend` (`go build ./...` BUILD SUCCESSFUL).
- [x] Verify RTL layout, dialogs, and character limits (10,000 for Blue Tick, 1,000 for Premium).

## Decisions & Notes
- Max char length for edit: 10,000 for Blue Tick / moderator, 1,000 for Premium (gold/black/premium), 500 for normal.
- Post action layout in `FeedScreen.kt` is the canonical reference.
