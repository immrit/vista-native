# VISUAL-FUNCTIONAL-PARITY-03 — Final Verification

## Git identity

- Worktree: `E:\vista_native_search`
- Branch: `visual-functional-parity-03/search`
- Base commit: `a8bd9675ffa6ae434e826fc9c6ccd9e0a8fa1b4c`
- Native changes were made only in the isolated Search worktree.

## Flutter files audited

- `lib/features/search/screens/searchPage.dart`
- `lib/provider/search_mentions_provider.dart`
- `lib/model/SearchResut.dart`
- `lib/DB/entities/recent_search_entity.dart`
- `lib/features/profile/data/profile_repository.dart`
- `lib/features/posts/data/go_posts_repository.dart`
- `lib/features/posts/navigation/content_routes.dart`
- `lib/features/home/screens/homeScreen.dart`
- `lib/utils/widgets/glassmorphism.dart`
- `lib/utils/widgets/verification_badge_icon.dart`
- `lib/theme/app_theme.dart`
- `lib/l10n/generated/app_localizations_fa.dart`
- secure logout/local database consumers traced from the Search history call chain

The Flutter checkout was read-only. Running the current Flutter source was blocked by locked existing build outputs; the already-installed real Flutter application was used for the valid API 33 reference capture.

## Backend contract

- `GET /v1/profiles/search?q=&limit=20&offset=`
- `GET /v1/profiles/by-username/{username}`
- `GET /v1/posts/hashtag/{tag}?limit=15&offset=0`
- `GET /v1/hashtags/search?q=&limit=10`
- `GET /v1/hashtags/trending?limit=12&days=30`

All are optional-auth reads. User paging is offset-based. Post cursor metadata is parsed but not paged because Flutter exposes no post load-more interaction. Flat `{code,message}` and `{error}` failures are parsed through the existing network error infrastructure.

## Native data flow

```text
SearchLauncher / SearchWorkspace
→ SearchViewModel + SavedStateHandle + StateFlow
→ SearchRepository
├─ Retrofit SearchApi
└─ Room SearchHistoryDao
→ DTO mapper → domain/UI models
→ Search-owned Profile/Post Detail route adapters
```

## Behavior evidence

- Query debounce: exactly 300 ms; IME submit is immediate.
- Cancellation: prior jobs are cancelled and a monotonic generation guard enforces latest-query-wins.
- Duplicate query suppression: normalized duplicate submits are ignored unless retry is explicitly requested.
- Pagination: profiles use `limit=20` and raw server-count offset; append deduplicates by id and retains content on failure.
- Posts: effective Backend limit 15; `has_more` and `next_cursor` parsed; no invented UI pagination.
- History: Room schema 6, account isolation, max 20 stored / 12 displayed, duplicate-to-front, delete one, immediate clear all, logout cleanup.
- Offline: no Search result cache was invented; recent history remains local; error semantics follow the audited Flutter branches.
- Release fixture leakage in `feature:search/src/main`: 0.

## Assets

- Exact Flutter bottom-navigation Search SVG geometry migrated to `feature/shell/src/main/res/drawable/ic_search_flutter.xml`.
- Source SHA-256: `1C863901A875AF7EF10F909FD1AD363A91F20ED46D9AEBDB490A5838DCB74441`.
- Native vector SHA-256: `0F3CB9A0067982A9736E1DD994B8F7DEC152EC8BA7D7EB2A04C514AEE9728F30`.
- Search body icons are canonical Material icons in Flutter; no bitmap or empty-state illustration exists.
- QR scanner action is deferred because Native has no real scanner destination; no fake route/control was added.

## Screenshots and visual comparison

- Valid Flutter captures: 1.
- Valid Native captures: 2 (initial plus focused/keyboard iteration evidence).
- Automated valid pairs: 1.
- `01-initial`: 28,390 / 2,592,000 pixels over threshold 24; mismatch 1.0953%; MAE 3.5777.
- Contact sheet: `docs/evidence/2026-07-29-search-parity/search-parity-contact-sheet.png`.
- Pair artifacts: side-by-side, alpha overlay, thresholded diff, bounding boxes, metrics.
- Remaining visible differences: QR suffix, chip typography/tint and header geometry, plus Shell bottom-navigation layout owned by the later integration gate.
- A second stable capture/fix cycle and the remaining required state pairs were not completed because a parallel task repeatedly replaced the installed APK using the same application id.

## Tests

| Suite | Total | Passed | Failed | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Search repository/unit | 15 | 15 | 0 | 0 | 0 |
| Search ViewModel | 13 | 13 | 0 | 0 | 0 |
| Search Compose device (direct AndroidJUnitRunner on API 33) | 5 | 5 | 0 | 0 | 0 |
| Auth/startup/onboarding regression | 32 | 32 | 0 | 0 | 0 |
| Shell regression | 3 | 3 | 0 | 0 | 0 |
| Executed total | 68 | 68 | 0 | 0 | 0 |

The first Gradle connected run targeted the same Emulator through two serials and collided during package install/uninstall. Direct single-device execution passed 5/5. A later Gradle UTP retry was blocked by dependency-verification metadata for a UTP-only transitive artifact; production dependencies and application builds remained verified.

## Runtime API 33

- Device: `Medium_Phone_2`, API 33, 1080×2400, portrait, font scale 1.
- Native Beta Debug clean install, cold launch, onboarding/login, Shell entry, Search tab, initial launcher, focus, and keyboard were exercised.
- real test account used successfully
- No Search crash or ANR was observed in the checked crash buffer.
- Exhaustive live user/post/pagination/error/history/navigation/rotation/recreation/logout smoke remains Pending because the shared package was overwritten by the parallel Feed/Profile task.

## Build, lint, R8, and security

- `assembleBetaDebug`: Passed; `app-beta-debug.apk` generated.
- `assembleBetaRelease`: Passed; unsigned Beta Release APK generated because production signing is not configured in this worktree.
- R8: Passed; `mapping.txt` generated (40,547,576 bytes).
- `lintBetaDebug`: Passed with 0 errors and 37 warnings.
- Search/Auth/Shell unit regressions: Passed.
- Search Release fixture leakage: 0.
- Changed-file credential literal scan: 0 matches after removing one pre-commit test-fixture collision.
- New secret-assignment scan: 0 matches.
- `git diff --check`: Passed before each commit and finalization.

## Commits

- `214cb72` — `feat(search): add architecture and contracts`
- `cc2a7b7` — `chore(ui): migrate Flutter search asset`
- `15ebcfa` — `fix(search): match Flutter search behavior`
- `b33ab28` — `feat(shell): connect native search destination`
- `fd35603` — `test(search): verify search state and storage`
- Documentation/evidence commit: `docs(search): record parity evidence`

## Integration dependencies and blockers

- Reconcile Search's minimal Shell changes with the independent Feed/Profile branch at the dedicated Integration Gate.
- Existing Post Detail reads a feed-cached post by id. A post discovered only through Search may render unavailable until the integration branch provides a fetch-by-id/cache handoff; no duplicate Post Detail was created.
- Native QR scanner capability does not exist; the Flutter QR suffix remains explicitly deferred.
- The shared Emulator/application id cannot provide stable concurrent screenshots while another worktree installs its APK.
- Current Flutter source execution is blocked by locked pre-existing build outputs in the read-only reference checkout.

## Exit gates

- Search Visual Parity: Pending
- Search Functional Parity: Pending
- Search Backend Contract: Passed
- Search Navigation: Pending
- Search Runtime API 33: Pending

Status: `In Progress — Visual mismatch: QR suffix, chip/header geometry, Shell bottom navigation, and incomplete state-pair coverage; Functional mismatch: Search-only Post Detail fetch handoff and exhaustive runtime/navigation verification remain open.`
