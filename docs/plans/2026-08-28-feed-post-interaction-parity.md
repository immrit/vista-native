# Flutter to Native Feed and Post Interaction Parity

**Type:** feature
**Status:** in-progress
**Progress:** `[███████░░░] 70%`

## Problem / Goal
Perform a strict behavior and UI parity audit between `E:\vista` and
`E:\vista_native` for feed, post detail, likes, comments, replies, sharing,
media actions, and related post interactions. Implement confirmed Native gaps
without claiming parity from file-name similarity alone.

## Root cause / Design
Flutter is the behavioral reference. Each claim must trace from the Flutter
screen/provider/repository/API path to the Native screen/ViewModel/repository/
API path. Native should reuse existing feed and comment layers, add only
missing contracts, and preserve server verification and optimistic-state
rollback semantics.

## Phases

### Phase 1 — Investigate
- [x] Reproduce / locate Flutter Feed, PostDetail, comments, replies, and share paths
- [x] Trace Flutter provider/repository/API contracts and interaction states
- [x] Trace Native Feed, PostDetail, comments, replies, share, and API paths
- [x] Record a strict parity matrix with evidence and blockers
- [x] Identify current Native UTF-8/mojibake regression in Feed/PostDetail strings

### Phase 2 — Implement
- [x] Complete typed direct-message share handoff and multi-conversation picker
- [ ] Complete Vista-story share renderer and editor handoff
- [x] Repair corrupted Persian strings introduced in Feed/PostDetail WIP
- [x] Implement double-tap like without unlike regression and add visual heart feedback
- [x] Implement music bubble surface for posts with `musicUrl`
- [x] Style hashtags/mentions in captions to match Flutter's entity emphasis
- [x] Preserve selected comment mentions as stable user IDs through submit
- [ ] Implement missing post-detail actions and media behavior
- [ ] Align loading, error, rollback, pagination, and empty states

### Phase 3 — Verify
- [ ] Run focused Native unit tests for changed behavior (blocked: Gradle loopback)
- [ ] Run Native compile and relevant test suites (blocked: Gradle loopback)
- [ ] Perform device/manual comparison for Feed and PostDetail
- [ ] Update parity matrix and exit criteria with evidence

## Decisions & Notes
- Pixel parity cannot be asserted from source inspection alone; it requires
  identical device dimensions, density, font scale, theme, locale, and captured
  screenshots.
- External share targets and backend behavior require runtime verification even
  when the Native intent/API code compiles.
- Current Native WIP contains mojibake Persian strings in `FeedScreen.kt` and
  `PostDetailScreen.kt`; source parity cannot pass until those are restored.
- 2026-08-28: Restored corrupted Persian strings in Feed/PostDetail, guarded
  media double-tap so it does not unlike an already-liked post, added a heart
  burst overlay, added a Feed/PostDetail music bubble backed by Media3, and
  styled hashtag/mention spans in captions.
- 2026-09-02: The share sheet's direct-message and add-to-story callbacks are
  optional and are not supplied by Feed/Post Detail, so those controls still
  invoke external sharing; they remain open. Comment composer candidates now
  come from loaded comment authors and submit `mentionedUserIds`, rather than
  discarding mention identity.
- 2026-09-02: Direct share now serializes Flutter-compatible `sharedPost`
  payloads, persists/retries them through ChatRepository, and lets the user
  choose one or more existing conversations. Share analytics is emitted only
  after a successful destination send. Story-template generation remains open.
- Verification blocker: `:feature:feed:compileDebugKotlin --no-daemon
  --stacktrace` fails before Kotlin with `java.io.IOException: Unable to
  establish loopback connection`; initial sandbox run also hit Gradle download
  network permission.
