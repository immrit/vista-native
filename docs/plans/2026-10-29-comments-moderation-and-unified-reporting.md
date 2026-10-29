# Comments Moderation & Unified Reporting to Vista Manager

**Type:** feature | bug | ui | security
**Status:** completed
**Progress:** `[██████████] 100%`

## Problem / Goal
1. Expand comment permissions:
   - Comment authors can edit (with badges/verification or normal limits) and delete their own comments.
   - Post owners can delete any comment under their posts.
   - Blue Tick verified users (`is_verified == true && verification_type == 'blueTick'`) and moderators/admins can edit ("ویرایش ناظر") and delete ANY comment on the platform.
2. Put the post comments feature "under the guillotine of testing":
   - Unit tests covering `CommentsViewModel` for edit, delete, reply, mention candidates, pagination, and Blue Tick/moderator moderation permissions.
   - Verify backend `internal/comments` logic for `Update` and `Delete`.
3. Unified Reporting System:
   - Implement real violation reporting for **Profiles**, **Posts**, **Comments**, and **Stories** in `vista_native`.
   - Ensure all reports funnel directly into `vista-backend` database tables (`reports`, `profile_reports`, `comment_reports`, `story_reports`).
   - Verify compatibility and visibility in `vista-manager` (`/dashboard/moderation`).

## Root cause / Design
- In `CommentsViewModel.kt`, `canDelete` required `currentUserRole == "admin"` and missed `isBlueTick` supervisors. `canEdit` blocked Blue Tick moderators from editing non-owned comments.
- In `CommentsBottomSheet.kt`, comment 3-dot menu lacked "کپی متن" and didn't show "ویرایش ناظر" for moderators.
- In `internal/comments/repository.go`, `Update` strictly required `user_id = $2`, preventing moderators from editing comments. `Delete` checked `pr.role = 'admin'` but missed `verification_type = 'blueTick'` and `pr.role = 'moderator'`.
- In `OtherUserProfileScreen.kt`, reporting was a stub `Toast.makeText` rather than calling the profile report API and showing `VistaReportReasonDialog`.
- In `StoryPlayerScreen.kt` and `internal/stories/repository.go`, story reports lacked `additional_details` column population.

## Phases

### Phase 1 — Investigate & Trace Backend & Manager Contracts
- [x] Verify `vista-backend` schemas for `reports`, `profile_reports`, `comment_reports`, and `story_reports`.
- [x] Verify `vista-manager` `/dashboard/moderation` data expectations.
- [x] Trace `OtherUserProfileScreen` and profile report API path.
- [x] Trace `StoryPlayerScreen` and story report API path.
- [x] Trace `CommentsBottomSheet` and `CommentsViewModel`.

### Phase 2 — Backend Moderation & Reporting Enhancements (vista-backend)
- [x] Update `internal/comments/repository.go` and `service.go` to allow Blue Tick supervisors / moderators to edit and delete comments.
- [x] Update `internal/stories/repository.go` and `service.go` to persist `additional_details` in `story_reports`.
- [x] Ensure `internal/profile/repository.go` and `service.go` cleanly accept profile reports.
- [x] Compile and verify `vista-backend` with `go build ./...`.

### Phase 3 — Comment Permissions & UI in vista_native
- [x] Update `CommentsViewModel.kt`:
      - Allow Blue Tick moderators (`currentUserHasBlueBadge || currentUserRole in ['admin', 'moderator']`) to delete any comment and edit any comment.
      - Ensure post owners can delete any comment on their post.
      - Ensure comment authors can edit and delete their comment.
- [x] Update `CommentsBottomSheet.kt`:
      - Distinguish "ویرایش" vs "ویرایش ناظر".
      - Add "کپی متن" to comment menu.
      - Wire `VistaReportReasonDialog` for reporting comments with both reason and additional details.
- [x] Wire Profile reporting in `OtherUserProfileScreen.kt`, `OtherUserProfileViewModel.kt`, `ProfileRepository.kt`, and `ProfileApi.kt`.
- [x] Verify Story reporting in `StoryPlayerScreen.kt` with reason and details.

### Phase 4 — Rigorous Testing ("Guillotine of Testing")
- [x] Add/update comprehensive tests in `CommentsViewModelTest.kt`.
- [x] Test edit, delete, reply, mention candidates, and moderation permissions.
- [x] Run test suite with Gradle.
- [x] Verify compilation with `./gradlew :app:compileBetaDebugKotlin`.

### Phase 5 — Strict Visibility Guard & Admin Content Studio Tools (vista-manager Parity)
- [x] Backend: Expand `UpdatePostInput` to support `moderation_reason`, `clear_content`, `clear_image`, `clear_video`, `clear_music`, `notify_user` in `internal/posts/repository.go` and `internal/posts/service.go`.
- [x] Backend: Verify backend Go build with `go build ./...`.
- [x] Native: Strict visibility guard:
      - Posts: Only visible to author or Blue Tick; author can only edit if premium (Gold Tick / Premium); non-authors/non-blue-tick never see edit or delete.
      - Comments: Only visible to author or Blue Tick; author can only edit if premium; post owner can delete any comment on their post; non-authors/non-owners/non-blue-tick never see edit or delete.
- [x] Native: Create `VistaAdminEditPostDialog.kt` matching `vista-manager` `/dashboard/content` (Presets, Moderation Reason, Clear Image/Video/Music, Notify User checkbox, Moderation Banner preview).
- [x] Native: Wire `VistaAdminEditPostDialog` in `FeedScreen.kt`, `PostDetailScreen.kt`, and `ProfileParityComponents.kt`.
- [x] Native: Pass `postOwnerId` in `ReelsViewerScreen.kt` to `CommentsBottomSheet`.
- [x] Native: Run unit tests and compile `:app:compileBetaDebugKotlin`.

## Decisions & Notes
- Blue Tick users are platform supervisors and can moderate comments across all posts.
- All report dialogs use the centralized `VistaReportReasonDialog` from `core:designsystem`.
