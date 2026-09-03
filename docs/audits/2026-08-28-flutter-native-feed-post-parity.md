# Flutter / Native Feed and Post Parity Matrix

**Review date:** 2026-08-28  
**Reference:** `E:\vista`  
**Target:** `E:\vista_native`

## Verdict

Source-level parity is **not complete**. Native now covers the main feed,
post detail, save, report, delete, visibility, comment, reply, edit, delete,
report, pagination, stale-cache, external share paths, gallery paging,
double-tap like guard/feedback, music surface, and visual hashtag/mention
emphasis. The strict gaps found in this review were:

1. Share used a different URL and bypassed Flutter's share-options surface.
2. Feed media opened only the first image instead of a swipeable gallery.
3. Feed media had no double-tap like gesture/feedback and briefly regressed
   into toggling unlike on already-liked posts.
4. A WIP Native edit corrupted Persian labels into mojibake in Feed/PostDetail.
5. Runtime pixel parity and external-target behavior remain unverified on the
   same physical/emulated device.

## Matrix

| Capability | Flutter evidence | Native evidence | Status |
|---|---|---|---|
| Explore/following feed | `ExploreFeedScreen.dart`, `personalized_feed_provider.dart` | `FeedScreen.kt`, `FeedViewModel.kt`, `FeedRepository.kt` | Covered |
| Pull-to-refresh | `ExploreFeedScreen.dart` | `FeedScreen.kt` | Covered |
| Pagination | feed provider load-more path | `FeedViewModel.loadMore()` and repository append | Covered |
| Offline/stale feed | provider/cache paths | `OfflineFirstFeedRepository`, stale UI state | Covered |
| Post detail fetch | `PostDetailPage.dart` | `PostDetailScreen.kt`, `PostDetailViewModel.kt` | Covered |
| Like button/count | `post_action_buttons.dart`, `likeStateProvider` | `VistaFeedPostCard`, `FeedViewModel`, repository rollback | Covered |
| Double-tap like | `double_tap_like_overlay.dart` | `FeedScreen.kt` `detectTapGestures`, already-liked guard, heart burst | Implemented in this review; runtime unverified |
| Save/bookmark | `saved_posts_provider.dart`, `go_posts_repository.dart` | `toggleSave`, saved state repository | Covered |
| Comment list | `comments_bottom_sheet.dart`, `commentsProvider` | `CommentsBottomSheet`, `CommentsViewModel` | Covered |
| Comment pagination/retry | Flutter comments provider | Native comments state/load-more/retry | Covered |
| Reply selection/submission | Flutter `replyingToCommentId` | `replyingTo`, `submitComment(parentCommentId)` | Covered |
| Load reply subtree | Flutter `loadReplies` | Native `loadReplies` and complete subtree reload | Covered |
| Comment edit | Flutter comment menu/provider | Native inline edit and permission checks | Covered |
| Comment delete | Flutter optimistic tree deletion | Native optimistic removal and server delete | Covered |
| Comment report | Flutter report action/repository | Native report API and action | Covered |
| Mentions | Flutter add mentions contract | Native API exists; composer extraction/runtime behavior needs device check | Partial |
| Multi-image post | Flutter `post_image_carousel.dart` | Native `HorizontalPager` in shared `PostMedia` with current-page counter | Implemented in this review; runtime unverified |
| Video post | Flutter feed video widget | Native `VistaFeedVideoPlayer` | Covered, runtime playback still required |
| Post share text/link | `smart_share_service.dart` | `PostShareBottomSheet.kt` | Implemented in this review |
| Share options surface | Flutter `share_bottom_sheet.dart` | Native share/copy/browser sheet | Partial: story-template/social-app-specific targets not ported |
| Share analytics | Flutter `trackFeedEvent('share')` | Native callback after share/copy/browser selection | Covered |
| Copy caption | Flutter post menu | Native clipboard menu | Covered |
| Report/not interested | Flutter post menu/provider | Native post action menu/ViewModel | Covered |
| Hide like/comment counts | Flutter post menu/update API | Native post action menu/update API | Covered |
| Own-post delete | Flutter confirmation/menu | Native confirmation/menu/ViewModel | Covered |
| Music bubble/playback | Flutter `post_music_bubble.dart` | Native `PostMusicBubble` in shared Feed/PostDetail card using `musicUrl`/`musicTitle` and Media3 | Source implemented; runtime playback unverified |
| Moderation banner/appeal | Flutter moderation widgets and appeal route | DTO fields exist; Feed card/banner/appeal UI not confirmed | Partial |
| Hashtag rich text/navigation | Flutter hashtag widgets | Native caption styles hashtag/mention spans | Partial: visual emphasis implemented; tap navigation still pending |
| Double-tap visual heart overlay | Flutter animated overlay | Native heart burst overlay | Source implemented; runtime unverified |
| Multi-image indicator | Flutter dots/counter | Native current-page counter only | Partial |
| Persian text encoding | Flutter source strings are valid UTF-8 | Native Feed/PostDetail WIP had mojibake; restored in this review | Source fixed; runtime unverified |

## Immediate implementation queue

### P0

- Add hashtag/mention tap navigation to Search/Profile when shell exposes a
  direct query/username route.
- Confirm comments-disabled behavior and prevent composer opening when disabled.

### P1

- Port Story Template share from a post.
- Add app-targeted share actions only where Android package availability is
  confirmed; keep generic chooser as fallback.
- Port moderation banner and appeal entry point.
- Match gallery dots, counter placement, and swipe physics.

### P2

- Run screenshot comparison at the same resolution/density/font scale.
- Test light/dark RTL, long captions, video, multiple images, empty/error
  comments, reply/edit/delete, and share cancellation.
- Re-run `:feature:feed:compileDebugKotlin` after fixing the local Gradle
  loopback failure.

## Important qualification

“Covered” means the source path and server contract exist in Native. It does
not mean pixel-level or device-level parity has been proven. The final exit
gate requires runtime screenshots and interaction recordings on both builds.
