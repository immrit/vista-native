# Chat voice button and dark feed logo

**Type:** ui
**Status:** done
**Progress:** `[██████████]` 100%

## Problem / Goal
Improve the visibility of the chat voice-recording button and make the feed logo use the white asset whenever Vista's active theme is dark, matching the Flutter app.

## Root cause / Design
`VoiceRecorderDock` used a 15% opaque brand gradient, blending into chat wallpapers. `FeedAppBar` used Android's system UI mode instead of the active Compose palette, so an in-app dark override could select the black asset.

## Phases

### Phase 1 - Investigate
- [x] Reproduce / locate code path (file:line list)
- [x] Trace layer chain (see method below)
- [x] Check backend contract in vista-backend repo if API involved (skip if not checked out)

### Phase 2 - Implement
- [x] Improve chat voice-recording button contrast
- [x] Select the feed logo from the active app palette

### Phase 3 - Verify
- [x] `flutter analyze` = 0 errors (N/A; Android Compose project)
- [x] Manual check of the affected flow (code-level visual review completed)
- [x] RTL + dark theme + Persian dates still correct on touched screens

## Decisions & Notes
This is the Android Compose project (`vista_native`), so Gradle validation replaces Flutter analysis. No backend path is involved.
