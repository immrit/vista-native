# Flutter Feed/Profile Parity Ledger

Mission: `VISUAL-FUNCTIONAL-PARITY-02`
Flutter source of truth: `E:\vista`
Backend contract source: `E:\vista-backend`
Native target: `E:\vista_native_feed_profile`
Audit status: scoped source trace complete; functional implementation verified; visual parity remains Pending per the 2026-08-01 runtime comparison.

## Confirmed call chains

- Feed tab: `HomeScreen._tabAt(0)` → `ExploreFeedScreen` → `_ForYouTab` / `_FollowingTab` → `personalizedFeedProvider` / `fetchFollowingPostsProvider` → `GoPostsRepository.exploreFeed` (`GET /v1/explore`) / `getFollowingFeed` (`GET /v1/feed/following`).
- Feed row: `_ThreadPostItem` → `PostImageCarousel` / `PostFeedVideo` / `PostLikeButton` / `PostCommentButton` / `PostSaveButton` → `ContentNavigation`.
- Post Detail: `ContentNavigation.pushPostDetail` → `PostDetailRoute` → `PostDetailsPage` → `postProvider(postId)` → `GoPostsRepository.getPost` (`GET /v1/posts/{id}`).
- Own Profile: `HomeScreen._tabAt(4)` → `ProfileScreen(currentUserId)` → `userProfileProvider` → `ProfileRepository.fetchProfileById` → self branch `GET /v1/me/profile`.
- Other Profile: Feed/profile navigation → `ProfileRoute` → `ProfileScreen(targetUserId)` → `userProfileProvider` → `GET /v1/profiles/{userId}`.
- Profile posts: `profilePostsProvider(userId)` → `ProfilePostsNotifier` → `GoPostsRepository.getUserPosts` (`GET /v1/users/{userId}/posts`, limit 30, offset).
- Follow: `_FollowButton` → `UserProfileNotifier.toggleFollow` → `ProfileRepository.follow/unfollow` → `POST /v1/me/follow|unfollow`.
- Native feed: `FeedScreen` → `FeedViewModel` → `FeedRepository` → `OfflineFirstFeedRepository` → Room `FeedDao` + `FeedApi`.
- Native profiles: Compose screens → profile ViewModels → offline-first repositories → Room profile DAOs + Retrofit APIs.

## Screen/state ledger

| Flutter source | Flutter behavior | Native current implementation | Gap | Required action |
| --- | --- | --- | --- | --- |
| `homeScreen.dart:69-92, build/IndexedStack` | Five lazily-built tabs remain alive; Feed is index 0 and own Profile index 4. | Typed nested Compose graphs with save/restore state. | Architecture is healthier and reusable; shell currently adds a duplicate app bar and debug labels. | Preserve typed graphs/back stacks; remove scoped duplicate/debug chrome and render Flutter navigation assets. |
| `homeScreen.dart:_buildBottomNavWithHalo` | Floating glass island, 30dp SVG icons, 16dp horizontal margin, 30dp radius, 28dp bottom margin plus insets, selected scale animation. | Standard Material navigation bar with glyph text and labels. | Complete visual mismatch; wrong assets/geometry. | Convert/copy canonical Flutter nav icons and implement the same island/halo without changing other tab content. |
| `ExploreFeedScreen.dart:112-245` | Background from theme; floating/snap/pinned app bar, centered black/white Vista logo, notification action, two 15sp tabs, StoryBar, composer FAB. | Inner `VistaTopAppBar("فید")` under Shell top bar; no tabs/logo/story/FAB. | Placeholder/product-chrome mismatch. | Implement Feed-owned app bar/tabs; keep Story/composer actions visibly faithful but non-fake where out of scope. |
| `ExploreFeedScreen.dart:90-103,699-749` | Connectivity stream controls animated 42dp orange offline banner with retry. | Cache failure sets plain stale text inside list. | Geometry/content/transition mismatch; Native network truth comes only from request failure. | Add network-aware banner or repository-derived offline state and retain cache-first content. |
| `ExploreFeedScreen.dart:280-409` | For You uses `/v1/explore`, limit 15, offset, refresh, 480px append threshold, per-row view/dwell events. | Uses `/v1/feed`, limit 15, offset, five-item threshold; no feed events. | Wrong endpoint/product feed; behavior mismatch. | Change primary feed API to `/v1/explore`; keep offset cache/paging; defer analytics events unless an existing safe Native contract is added. |
| `ExploreFeedScreen.dart:413-541` | Following tab uses `/v1/feed/following`, limit 20 and cursor from last `created_at`; independent state. | No Following tab/repository stream. | Missing real product tab. | Add an independently cached/account-scoped Following source only if schema can represent feed type safely; otherwise gate cannot pass. |
| `_ForYouTab` / `_FollowingTab` | Initial empty has upload overlay, feed-specific copy, CTA, always-scrollable refresh. | Generic `VistaEmptyState`, no tab-specific CTA. | Visual and interaction mismatch. | Match source copy/geometry; do not make Search action fake if Search is deferred. |
| `_FeedLoadingState`, `PostCardSkeleton` | Four full card skeletons separated by 0.5dp dividers. | Center spinner and label. | Full mismatch. | Implement source-derived skeleton structure. |
| `_LoadMoreRetryRow` | Existing posts remain; inline orange 18dp error icon, message, retry text. | Existing posts remain; error text and Vista button in centered column. | State logic reusable; layout mismatch. | Reuse ViewModel state and replace footer visuals. |
| `_FeedConnectionBanner` + cached providers | Flutter can show cached profile/posts after request failure; no persisted main-feed cache is evident in the traced provider. | Room-backed account-scoped Feed cache and stale state. | Native architecture is stronger and must be preserved; Flutter banner semantics still need matching. | Keep Room/offline-first flow; reproduce product-visible stale/offline visuals. |
| `_ThreadPostItem:801-1063` | Whole card opens detail; header padding 16/12; 22dp avatar radius; username 15 bold; 14dp verified badge; bullet/time 13; optional 112×28 follow. | 16dp column; generic avatar; full name first, username second; no timestamp/badge/follow. | Major structure/data-display mismatch. | Rebuild card against Flutter source while preserving Native models/ViewModel. |
| `_ThreadPostItem:1020-1057` | Caption 15sp, line height 1.4, six lines, `بیشتر...`; hashtag/mention styled blue and navigable. | Unlimited plain body text. | Missing truncation/bidi/entities. | Add bidi-safe six-line caption and read-more to detail; defer Search navigation when unavailable. |
| `_ThreadPostItem:1065-1146` | Image media has 16dp side padding, 14dp radius, max 280dp, crop; gallery swipe/counter/dots; decode at display size. | Square full-width crop; no radius, gallery, loading/error parity. | Major media mismatch. | Implement 280dp cap/aspect policy, gallery paging and exact placeholders. |
| `_ThreadPostItem:1148-1187`, `PostFeedVideo` | Covered video wins over image, poster supplied, max 280dp, autoplay muted, progress/controls, 14dp radius. | Static thumbnail only and no explicit overlay; no video player. | Visual/behavior mismatch. | Within scope, provide poster plus unambiguous video indicator; do not build unrelated Reels. Record autoplay/player limitation if not completed. |
| `_ThreadPostItem:1189-1305` | 19dp like/comment/save/send, 14dp gaps, always-visible zero counts unless hidden, overflow at end. | Two textual counts only. | Major visible-state mismatch. | Render all read-only display states; keep Like/Save/Share disabled when no healthy Native mutation exists. |
| `post_action_buttons.dart` | Like animation 180ms; comment/send use Flutter PNG assets; save uses bookmark icon. | No action-row components. | Missing assets/animation/states. | Migrate comment/send assets; implement display-only semantics and disabled click affordance where deferred. |
| `personalized_feed_provider.dart` | Initial failure replaces empty state with error; append failure keeps content and blocks auto-retry until explicit retry; dedupe relies mostly on backend. | Same initial/append separation; Room dedup and mutex are stronger. | Core logic largely reusable; Native refresh error copy/state differs. | Preserve Room/mutex/dedup and align visible error transitions/copy. |
| `GoPostsRepository.exploreFeed` | Optional auth `/v1/explore`; backend returns `FeedResponse`. | Authenticated `/v1/feed`. | Endpoint mismatch. | Align endpoint while retaining internal authorization interceptor. |
| `PostDetailPage.dart:1230-1268` | Network-backed `FutureProvider.autoDispose`; loading; errors map to unavailable/deleted; comment input only with content. | Room-only flow; missing post becomes cache error; no network refresh. | Functional mismatch. | Add `GET /v1/posts/{id}` refresh/upsert and cache-first display. |
| `PostDetailPage.dart:317-390` | 10dp Card margin/padding; header, caption/tags, adaptive image/gallery or video max 400, music bubble, action row. | Flat 16dp LazyColumn; square media; no card/tags/music/actions/timestamp. | Major visual mismatch. | Rebuild detail structure; keep comments mutation out of scope. |
| `PostDetailPage.dart:414-447` | Avatar, username, verified badge, Jalali date, vertical overflow. | Generic avatar, full name + username; author not clickable. | Header mismatch and missing navigation. | Match header and route author to self/other profile. |
| `PostDetailPage.dart:851-928` | Like/comment/save/share visible with counts; hidden counts omitted. | Text counts only. | Visible action mismatch. | Implement read-only row; leave new mutations disabled/deferred. |
| `PostDetailPage.dart:930-1044` | Full comments list states and header. | No comments preview. | New Comment implementation is out of scope. | At most render a read-only comments header/preview if existing data contract is reused; do not add mutation/navigation. |
| `PostDetailPage.dart:1271-1336` | Missing/deleted post uses circular delete icon, title/body, directional Back button. | Generic cache-missing error with text Back. | Visual/copy mismatch. | Match unavailable state exactly and distinguish retriable network error from canonical not found. |
| `profileScreen.dart:114-242` | One shared Profile screen for own/other; shimmer then refreshable NestedScrollView; header/action/tab content. | Separate Own/Other screens. | Native separation is architecturally valid, but UI/state composition is incomplete. | Keep separate ViewModels/screens and share visual components. |
| `profileScreen.dart:245-287` | App bar username 20 bold, verified badge, overflow; settings only for own. | Own title `نمایه شما`; Other title `نمایه کاربر`; text Back in some routes. | Major chrome mismatch. | Use username title, exact icons, directional back, and canonical routes. |
| `_ProfileHeader:686-917` | 16dp padding; 84dp avatar, 28dp gap, three stats; name 15 bold; bio 14/1.4 max 5 with first-character bidi; privacy icon; optional join badge. | Centered 88dp/unsized avatar, name and badges, counters below bio. | Major structure mismatch. | Build shared Flutter-derived profile header; add join-order and role data to Native DTO/entity if consumed. |
| `_StatItem` | Count 18 bold; label 13; K/M formatting. | Raw count; mixed Material typography. | Typography/format mismatch. | Add source-equivalent formatter/styles with no clipping. |
| `_ProfileActionBar` own | Two equal 36dp outlined buttons: Edit Profile and Share. | Own Profile shows only full-width destructive Logout. | Placeholder/wrong product behavior. | Replace visible logout body button with Edit/Share; keep logout under existing canonical settings/menu behavior. |
| `_ProfileActionBar` other | Follow/requested/following plus optional Message, two equal 36dp buttons. | One full-width 52dp Follow button; no message display. | Visual mismatch; Chat expansion out of scope. | Match follow half/full geometry; show Message only if canonical existing route is safe, otherwise record deferred. |
| `_FollowButton` | Not-following black/white filled; Following grey outlined; Requested disabled grey; all 36dp radius 8. | Primary/outline VistaButton 52dp; pending labels reflect mutation rather than requested relation. | Major visual mismatch. | Implement relation + mutation-pending visual matrix separately. |
| `UserProfileNotifier.toggleFollow` | Flutter performs canonical request then updates, but does not optimistic-update or rollback UI before response. | Native Room repository performs optimistic update, canonical reconcile, rollback, mutex. | Native architecture/behavior is stronger and explicitly required by mission. | Preserve Native optimistic/canonical/rollback flow; match Flutter final relation visuals. |
| Backend `ProfileResponse.FollowStatus` | `none|following|requested`; private accounts can return `requested`. | Native enum supports all three and rejects unknown. | Correct and reusable. | Retain strict parser; ensure requested follower count is not incremented. |
| Native `OfflineFirstUserProfileRepository.reconcileFollow` | Canonical `requested` restores original follower count. | Matches backend semantics. | No domain gap found. | Preserve; add UI/tests. |
| `_ProfileTabBarDelegate` | Pinned 48dp tab bar, hairline top/bottom, grid/reels/music icons. | No profile tabs. | Missing entire section. | Implement Posts tab in scope; display Reels/Music tabs only as required by Flutter shell but do not build new features beyond existing post data. |
| `_PrivateAccountPlaceholder` | 80dp outlined circle, 40dp lock, title 18 bold, subtitle 14. | Other profile always shows a generic public-post placeholder regardless of privacy. | Incorrect behavior/UI. | Gate posts by `isPrivate && !following/requested/self`; render exact private state. |
| `_EmptyPlaceholder` | 80dp outlined circle, contextual icon/title/subtitle. | Generic `VistaEmptyState` or explicit implementation-stage placeholder copy. | Placeholder prohibited. | Replace with product copy/styling. |
| `_ProfilePageShimmer`, tab shimmers | Full header/action/tab/list shimmer. | Center loading state. | Full visual mismatch. | Implement source-derived profile skeletons. |
| `ProfilePostsNotifier` | `/v1/users/{id}/posts`, limit 30 offset; cache fallback; dedupe in UI map; 75% scroll threshold. | No profile-post repository/UI; Other screen explicitly says not implemented. | Core missing feature. | Add account-scoped Room-backed profile-post query/source without discarding Feed cache architecture. |
| `_PostsGridView` | Despite name, Posts tab is a separated vertical post list with bottom clearance +110. | No posts section. | Missing. | Reuse a shared read-only post card variant matching Flutter Profile list. |
| `ProfileScreen._refreshProfile` | Clears profile UI cache, refetches profile and posts, invalidates settings/providers; nested scroll notifications accepted at all depths. | Profile refreshes only profile. | Missing posts refresh and nested behavior. | Refresh profile and post source together while keeping cached content on failure. |
| `user_profile_provider.dart:190-205` | On profile fetch failure, falls back to cached profile; otherwise remains null/shimmer. | Native has explicit cache/stale/error/not-found states. | Native state model is more complete than current Flutter visible error handling. | Preserve robust Native states but match Flutter visual language for requested mission states. |
| Backend `GET /v1/users/{id}/posts` | Optional auth, `FeedResponse`, offset/cursor parsed. | No Native API. | Missing contract. | Add Retrofit method and DTO reuse; never change backend. |
| Backend `ProfileResponse` | Provides private, join order, premium, message privacy, follow state, zoom flag. | Native Public DTO omits join order/message privacy/zoom; Own DTO is smaller. | Data/domain gaps for visible profile header/actions. | Extend DTO/entity/domain and Room migration only for fields actually rendered. |
| Navigation `ProfileRoute` | 220ms forward / 180ms reverse, fade + slight 0.04 slide. | Compose default transitions and typed routes; Feed author always routes Other first, then self redirect. | Behavior mostly safe but visual transitions/self routing differ. | Route self directly when account id is known; add equivalent transition if supported without destabilizing stacks. |
| Navigation `PostDetailRoute` | 220ms fade; back returns the kept-alive Feed list. | Typed nested graph; Feed list state currently created inside route and expected to save with back stack. | Needs runtime proof. | Hoist/save `LazyListState` as needed and verify exact back restoration. |

## Reusable Native architecture

- Keep Compose → ViewModel → repository contract → Room/API separation.
- Keep account-scoped Feed/Public Profile rows, atomic Room transactions, deduplication, append mutex, error parsing, TLS/auth interceptors, and typed navigation.
- Keep optimistic follow mutation, canonical server reconciliation, rollback, and duplicate prevention.
- Extend rather than replace the existing Feed/Profile modules and database migration chain.

## Confirmed placeholders or incorrect product UI

- Shell `CurrentRoute` and `DeepLinkDebug`.
- Duplicate Shell app bar plus Feed/Profile inner app bars.
- `OtherUserProfileScreen`: “نمایش شبکه پست‌ها در این مرحله پیاده‌سازی نمی‌شود.”
- Own Profile destructive Logout button as the primary body action instead of Flutter Edit/Share.
- Generic centered spinners instead of Flutter feed/profile skeletons.
- Material/glyph navigation instead of Flutter custom assets.

## Deferred by scope

- New Like/Save mutations, new Comment mutation, Share expansion, Report expansion, Story implementation, Composer, Search implementation, Chat implementation, followers/following lists, Edit Profile implementation, Reels implementation, and Music implementation.
- Visible states/routes may be represented only when backed by an existing canonical route or read-only contract; no fake success or no-op action will be introduced.
