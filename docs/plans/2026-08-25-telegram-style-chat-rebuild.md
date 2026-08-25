# Telegram-style Native Chat Rebuild

**Type:** ui | perf | refactor
**Status:** planning
**Progress:** `[░░░░░░░░░░] 0%`

## Problem / Goal

Rebuild Vista Native chat so its interaction quality, performance, RTL layout,
selection flow, composer, and context actions match the observed Telegram UX
while retaining Vista's existing Flutter feature set, API contracts, E2EE, and
account-scoped local cache.

The local Telegram checkout is a behavior and visual reference only. No source,
assets, identifiers, or implementation structure will be copied into Vista.

## Root cause / Design

The current screen has overlapping Compose gesture/state paths (message bubble,
selection, context state, swipe reply and the Android editor bridge). The
replacement will make each concern single-owner: one message interaction
controller, one selection state machine, one contextual-action surface, and an
IME-safe composer.

Keep Room as the Native cache. Isar belongs to the Flutter client and is not a
Kotlin/Compose database migration target.

## Phases

### Phase 1 — Baseline and reference contract
- [ ] Record Telegram behavior matrix: tap, hold, multi-select, swipe reply,
  context actions, keyboard open/close, emoji, attachment, voice, search.
- [ ] Record matching Flutter Vista behavior with the same non-sensitive fixture.
- [ ] Capture Native baseline screenshots and frame/ANR evidence on a cold start.
- [ ] Trace all Native chat entry points, repository/cache ownership, and
  interaction state (`ChatScreens`, composer, message bubble, context menu).

### Phase 2 — Interaction architecture
- [ ] Replace overlapping message gesture handlers with one explicit interaction
  state machine (tap, hold, horizontal swipe, selection).
- [ ] Implement a Telegram-style contextual action surface with safe action
  gating for own/peer/deleted/secret messages.
- [ ] Implement deterministic multi-select: first hold selects, taps toggle,
  back exits, and bulk actions respect permissions.
- [ ] Add Compose instrumentation for every transition in the interaction matrix.

### Phase 3 — Composer and keyboard performance
- [ ] Remove synchronous work from editor callbacks and ensure one IME show/hide
  request per user transition.
- [ ] Align reply/edit/emoji/attachment/voice dock behavior with Flutter parity.
- [ ] Add cold-start, IME-open, scroll, and message-list performance probes.

### Phase 4 — Chat visual and content parity
- [ ] Rework message cell geometry, grouping, timestamps, read state, replies,
  reactions, media, audio, documents, pinned messages, and date dividers.
- [ ] Verify RTL, Persian text shaping, Jalali dates, dark/light wallpaper, and
  accessibility labels.
- [ ] Preserve E2EE failure-closed behavior and never log message plaintext.

### Phase 5 — Acceptance
- [ ] Focused JVM tests and Compose instrumentation pass.
- [ ] Current-source APK is installed after force-stop/cold-start.
- [ ] Same-fixture Native-vs-Flutter screenshots and action sweep pass.
- [ ] Real physical Android device confirms keyboard latency, context actions,
  selection, scrolling, voice dock, attachments, and rotation.

## Decisions & Notes

- Do not copy GPL Telegram code. Use it only to derive observable behavior.
- Flutter is the capability source of truth; Native must not invent incompatible
  API, encryption, or local-cache behavior.
- A physical device is mandatory for final latency acceptance. Emulator evidence
  is useful but insufficient for keyboard/jank sign-off.
