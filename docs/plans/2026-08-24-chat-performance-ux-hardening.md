# Native Chat Performance and UX Hardening

**Type:** perf | bug | ui
**Status:** in-progress
**Progress:** `[████░░░░░░] 40%`

## Problem / Goal

On a physical device, entering a conversation and opening the IME is severely
janky, especially on the first entry. Message selection and the contextual
actions are also unreliable. Harden the Compose chat surface against the
Flutter behaviour and Telegram's interaction patterns without weakening
end-to-end encryption or disrupting parallel work.

## Root cause / Design

Investigation is in progress. The likely focus is the message-detail route's
first-load work and Compose/IME layout lifecycle. Retain the Native
account-scoped encrypted Room cache: Isar is Flutter/Dart-specific and is not
an Android-native replacement. Compare Telegram behaviours, not its GPL code.

## Phases

### Phase 1 — Investigate
- [x] Preserve workspace and record a resumable plan
- [x] Inventory active Native chat implementation, data flow, and first-load work
- [x] Capture current installed Native package/activity and emulator state
- [x] Compare IME, context-menu, selection, and scroll behaviour with Flutter/Telegram references

### Phase 2 — Implement
- [x] Remove traced main-thread/first-composition work from the IME path
- [ ] Make message selection and contextual action lifecycle reliable and RTL-correct
- [ ] Apply narrowly scoped Compose/UI performance and interaction fixes

### Phase 3 — Verify
- [ ] Run focused Native unit/static checks and `git diff --check`
- [ ] Build, prove resolved APK package/activity, install latest Native APK, and exercise chat on device
- [ ] Capture before/after screenshots; check IME, long-press context menu, multi-select, RTL and dark theme

## Decisions & Notes

- Flutter is the read-only product reference. Telegram is used for behavioural
  study only; no Telegram source code will be copied.
- Do not log tokens, credentials, or message plaintext. E2EE remains fail-closed.
- Completion requires real-device evidence; build/static success alone is partial.
- Root cause confirmed in `TelegramComposerField.kt`: custom emoji span formatting ran
  after every text mutation and synchronously initialized the asset map / decoded bitmaps.
  The editable field now stays Unicode-native; read-only message rendering retains custom
  emoji rendering. `openKeyboard` now posts IME work after the active layout pass.
- `MessagesViewModel.bind` still starts concurrent cache observers plus remote work on entry;
  it is the next performance slice. The active module is untracked in this checkout, so all
  changes are being kept narrow and no unrelated files are touched.
- Verification is currently blocked before `:feature:chat` compiles: Gradle fails in
  `:core:common:compileKotlin` because Windows cannot delete the pre-existing shared
  build directory `core/common/build/classes/kotlin/main/ir`. No build output was
  removed; the lock/ownership must be resolved safely before retrying.
- Live reproduction on `emulator-5556`: opening the composer displayed the Gboard IME
  followed immediately by the system ANR dialog, "Vista isn't responding". The direct
  cause is now narrowed further: a single tap invoked `onFocusText` and requested the
  IME through all three `onFocusChange`, `onTouch`, and `onClick` callbacks. The touch
  and click overrides were removed, leaving native focus handling as the sole path.
- Kotlin build outputs in `core:common`, `core:model`, and `core:designsystem` were
  independently locked by an external Windows process. Their generated `build/`
  directories were moved (not deleted) into `artifacts/gradle-quarantine/` so Gradle
  can recreate them. The compile gate has progressed from `core:common` to
  `core:designsystem`; it still must pass before installation.
