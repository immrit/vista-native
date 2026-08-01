# Flutter Search Parity Ledger

Reference checkout: `E:\vista`
Observed branch/HEAD: `main` / `64994fa1dfa6f1b146cb9d132758f105670e4d71`
Audit mode: source read-only. The checkout already contained unrelated modified/untracked files; none were changed by this mission.

## Real call chain

```text
HomeScreen tab 1
→ SearchPage launcher
→ Navigator.push(SearchPage(openAsWorkspace: true))
→ 300 ms Timer / immediate submit
→ SearchNotifier.search
├─ ProfileRepository.searchProfiles
│  ├─ GET /v1/profiles/by-username/{username} (eligible first page only)
│  └─ GET /v1/profiles/search?q=&limit=20&offset=
└─ SearchService / GoPostsRepository
   ├─ GET /v1/posts/hashtag/{tag}?limit=60&offset=0
   ├─ GET /v1/hashtags/search?q=&limit=10
   └─ GET /v1/hashtags/trending?limit=12&days=30
→ ProfileModel / PublicPostModel / HashtagSuggestion
→ _UserTile / _PostGridItem / suggestions / recents
→ ContentNavigation.pushProfile / pushPostDetail
```

## Ledger

| Flutter source | Behavior | Native current state | Gap | Required action |
| --- | --- | --- | --- | --- |
| `lib/features/home/screens/homeScreen.dart:69-92` | Tabs are built lazily, then retained in an `IndexedStack`; Search is tab index 1. | Shell has a typed Search graph but only a placeholder. | Search product destination absent. | Replace only Search graph content; preserve Shell tab save/restore. |
| `homeScreen.dart:490-623` | Search tab uses the same custom 24×24-viewBox SVG active/inactive, rendered 30×30, tinted primary or grey, with 200/250 ms selection animation. | Shell shows the glyph `⌕` in a standard M3 navigation bar. | Asset, size, tint, and animation mismatch. | Migrate the exact vector; isolate Shell integration. |
| `searchPage.dart:351-418` | Tab root is a launcher: glass field opens a full workspace; it is not directly editable. | Placeholder field is directly editable and Shell adds a top app bar. | Structure and interaction mismatch. | Implement separate launcher/workspace destinations and suppress duplicate top bar. |
| `searchPage.dart:420-449` | Launcher loads trending tags, supports pull-to-refresh, shows loading/empty/chips. | No data or state. | Entire launcher body missing. | Implement contract-backed trending state. |
| `searchPage.dart:451-498` | Workspace field has 16/12/16/8 outer padding, radius 18, blur 24, 8×4 inner padding, search/clear/QR Material icons, and IME Search. | No parity field. | Entire visual/keyboard contract missing. | Build Compose field from extracted source values; QR action remains deferred unless a real scanner route exists. |
| `searchPage.dart:500-513` | Three real tabs: همه، افراد، تگ‌ها. | No categories. | Tabs missing. | Implement exactly three tabs; do not invent filters. |
| `searchPage.dart:200-228` | Typing debounce is 300 ms; submit is immediate; blank clears all; `#` selects tag tab. | No search controller. | Behavior missing. | Implement in ViewModel with cancellable coroutine jobs. |
| `search_mentions_provider.dart:104-188` | Monotonic request id enforces latest-query-wins and clears prior results during new search. | No Search ViewModel. | State/cancellation missing. | Use job cancellation plus generation guard. |
| `search_mentions_provider.dart:190-228` | User load-more only: limit 20, offset by received count, blocked while busy, append error kept in state. | No pagination. | Missing. | Implement user offset pagination and content-preserving append error. |
| `search_mentions_provider.dart:230-248` | User order is server relevance; non-space Persian/Latin/alnum/underscore query may also search posts as a tag. | No normalization policy. | Missing. | Preserve server order and supplementary-tag predicate. |
| `profile_repository.dart:215-285` | Leading `@` removed. Eligible first-page usernames also call exact by-username, prepend exact result, and deduplicate by id. Network/Dio failures are swallowed into empty lists. | Profile module has public-by-id only. | Exact enrichment and Flutter's error semantics missing. | Add Search-owned exact/profile calls; document deliberate empty-on-profile-search failure. |
| `go_posts_repository.dart:160-203` | Direct tag posts, trending, and tag suggestions use optional auth. Flutter asks for tag-post limit 60. | Feed API only exposes `/v1/feed`. | Search endpoints absent. | Add Search API; use Backend effective limit 15 because 60 violates its cap. |
| `searchPage.dart:515-548` | Full-page loading replaces results; post-load errors can render a retry banner once loading ends. | No states. | Missing. | Model Loading, Content/Empty, Error, Appending, AppendError. |
| `searchPage.dart:575-628` | `#` suggestions have a 300 ms secondary debounce, max six visible rows, 16 radius avatar, usage count, local trending fallback on error. | No suggestion state. | Missing. | Implement separate cancellable suggestion job and fallback. |
| `searchPage.dart:630-641` | Loading uses two `PostCardSkeleton` rows, not an indeterminate full-screen spinner. | No skeleton. | Missing. | Recreate Search-used skeleton geometry in feature module. |
| `searchPage.dart:643-688` | All tab shows up to five users and up to nine square posts; “show all users” selects People. | No mixed result composition. | Missing. | Implement the same caps and transition. |
| `searchPage.dart:690-732` | People list triggers load-more within 240 logical pixels of the end. | No list. | Missing. | Add LazyList threshold guard. |
| `searchPage.dart:734-809` | Tag results are a 3-column square grid, 2 spacing, 16 horizontal padding, and no post pagination trigger. | No grid. | Missing. | Preserve no-load-more interaction; parse server metadata for auditability. |
| `searchPage.dart:230-305,811-889` | History is Isar-local, descending timestamp, duplicate query replaced, max 20 stored, 12 displayed, delete-one and immediate clear-all with no confirmation dialog. | No Search history. | Missing. | Use account-isolated Room history, max 20, immediate clear-all. |
| `recent_search_entity.dart` | Only `hashtag` and `user` types exist; query is indexed, not account-scoped. | No entity. | Native can improve isolation without changing visible behavior. | Composite account/query key and logout cleanup. |
| `secure_logout_service.dart` | Secure logout wipes the whole local DB, therefore Search history is cleared. | Logout clears Feed/Profile caches and session only. | Search history cleanup missing. | Add Search account clear to logout composition. |
| `searchPage.dart:926-1006` | User row: 46 logical-pixel avatar, titleSmall/bodySmall, highlighted match, optional Material verified icon, `@username`, directional chevron. | No row. | Missing. | Build exact row and keep `@username` ordering under RTL. |
| `searchPage.dart:1009-1041` | Post tap routes by id; image uses cover + radius 8; missing media shows Material article icon. | Post Detail route exists under Feed graph. | Search-owned navigation adapter missing. | Reuse Post Detail screen under Search graph; do not duplicate implementation. |
| `content_routes.dart` | Profile transition 220/180 ms fade+horizontal slide; Post Detail is 220/180 ms fade. Back pops to preserved Search route. | Shell sibling destination would switch selected tab to Feed. | Navigation ownership mismatch. | Add Search-graph adapter destinations reusing existing screens. |
| `app_theme.dart` | Light background `#F8F9FF`, surface white, primary `#6366F1`, error `#EF4444`, Vazirmatn typography. | Native tokens already match these core values. | Search components not yet consuming them. | Consume existing Native theme; do not change global tokens. |
| `glassmorphism.dart` | Glass border is `color × opacity × 2.1`, width .5; shadow black 8%, blur 24, y=8; Backdrop blur uses supplied sigma. | No Search glass surface. | Missing. | Implement a Search-local parity surface without changing shared components. |
| `verification_badge_icon.dart` | Badge is Flutter Material `verified`, not a custom asset. | Profile feature has its own visuals but Search row absent. | Consumer missing, asset migration unnecessary. | Use canonical Compose vector/shape behavior and audited colors. |
| `l10n/generated/app_localizations_fa.dart` | Key labels include جستجوی کاربران, نتیجه‌ای یافت نشد, تلاش مجدد. Several Search section strings remain hard-coded Persian. | Shell only exposes a tab label. | Search strings absent. | Add feature-owned Persian strings matching observed literals. |

## Behavioral findings

- Minimum effective query length is one non-whitespace character. Empty input performs no request.
- The main query debounce and hashtag-suggestion debounce are independently 300 ms.
- User pagination is offset-based. Hashtag posts expose server paging metadata but Flutter does not request another page.
- Flutter does not cache Search results for offline use. Direct hashtag failures can show retry; profile-search failures are swallowed as empty; supplementary non-`#` tag failure is swallowed.
- History is saved only after selecting a user/tag/post result or trending chip, not for every typed/submitted query.
- Clear history is immediate; there is no confirmation dialog in the reference implementation.
- No Search analytics call exists in the audited call chain.
- No Search-specific bitmap or empty illustration is consumed; body icons are Flutter Material icons.

## Native closure update

The ledger table above records the base-commit gap. The implementation outcome is tracked separately so the initial audit evidence remains intact.

| Native evidence | Closed behavior | Remaining gap |
| --- | --- | --- |
| `feature/search/.../SearchViewModel.kt` | 300 ms debounce, immediate submit, job cancellation, generation guard, duplicate suppression, query/tab restoration, user append state | Full process-death and rotation runtime evidence remains open. |
| `feature/search/.../DefaultSearchRepository.kt` and `SearchApi.kt` | All five audited Backend calls, exact-user merge, nullable mapping, user/post deduplication, effective limits, error parsing | No new endpoint or fake result was introduced. |
| Room schema 6 and `SearchHistoryDao` | Account-scoped max-20 history, 12-item display, recency replacement, delete, clear, logout cleanup | Flutter itself is not account-scoped; Native isolation is a privacy-preserving internal improvement. |
| `SearchScreens.kt` | Launcher/workspace split, three tabs, RTL-safe field, skeletons, user rows, result grid, history, empty/error/retry states | QR scanner is deferred; exhaustive screenshot parity remains open. |
| `VistaShell.kt` and `ShellRoutes.kt` | Search-owned routes reuse existing own/public Profile and Post Detail screens | Existing Post Detail is feed-cache-backed; a Search-only post may not resolve until the Feed/Profile integration branch provides the final fetch contract. |
| Unit and Compose tests | 28 Search unit tests plus 5 Compose device tests passed with zero skipped | Required exhaustive navigation/process/logout instrumentation set is not complete. |
