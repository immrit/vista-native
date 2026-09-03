# Safe-area insets across native screens

**Type:** bug
**Status:** done
**Progress:** `[â–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆâ–ˆ] 100%`

## Problem / Goal
The native Android Compose app renders edge-to-edge, but several screens and overlays do not consume status-bar, navigation-bar, or IME insets. Content such as comments and informational pages can therefore be obscured by device system areas.

## Root cause / Design
`MainActivity` enables edge-to-edge globally. Each screen must explicitly consume the relevant insets, while full-bleed experiences may keep backgrounds edge-to-edge and pad only interactive content. Reuse the existing `VistaScaffold` and Compose inset modifiers, adding a shared screen-level convention where needed.

## Phases

### Phase 1 â€” Investigate
- [x] Reproduce / locate code path (file:line list)
- [x] Trace layer chain (see method below)
- [ ] Check backend contract in vista-backend repo if API involved (skip if not checked out)

### Phase 2 â€” Implement
- [x] Make the app shell and shared scaffold consume system insets
- [x] Patch screens and bottom-sheet/comment surfaces missing safe-area handling
- [x] Preserve intentional full-bleed story/media layouts while protecting controls

### Phase 3 â€” Verify
- [x] `:core:designsystem:compileDebugKotlin` succeeds
- [ ] Manual check of the affected flow (comments, about slideshow, startup, search, game)
- [x] RTL + dark theme + Persian dates remain unchanged in touched screens

## Decisions & Notes
Running log: this checkout is native Android/Jetpack Compose rather than Flutter; the equivalent implementation is Window Insets. `:app:compileBetaDebugKotlin` was attempted but the full workspace exceeded the environment timeout; the shared design-system module compiles successfully. Existing unrelated working-tree changes are preserved.
