# INT-01 Design System Reconciliation

## Decision

The authoritative global Design System remains the Login/OTP baseline at
`a8bd9675ffa6ae434e826fc9c6ccd9e0a8fa1b4c`. Neither input branch is allowed
to redefine global palette, typography, spacing, semantic colors, Auth
controls, Login geometry, or system-bar defaults without same-state Flutter
evidence.

## Audited result

- The integrated Feed/Profile and Search range changes no Kotlin source under
  `core:designsystem` relative to the Login base.
- Feed/Profile adds only three screen-consumer raster assets:
  `vista_default_avatar.jpg`, `vista_post_comment.png`, and
  `vista_post_send.png`.
- Profile adds screen-specific vector drawables in `feature:profile`; none is a
  global token or shared Auth control.
- Search adds `feature:shell/.../ic_search_flutter.xml`, derived from the
  Flutter navigation asset declared as
  `assets/icons/navigation/magnifying-glass.svg` in
  `lib/features/home/screens/homeScreen.dart`.
- No input changes the font family, theme/color/typography source, global
  spacing scale, Login assets, Login controls, manifest, or system-bar setup.
- Shell reconciliation keeps the Login theme and the Feed/Profile single
  scaffold model. It does not wrap feature destinations in an additional
  `VistaScaffold`, avoiding duplicate app bars and bottom navigation.

## Conflict classification

| Item | Classification | Resolution |
|---|---|---|
| Login palette/typography | Global | Preserved byte-for-byte from Login base |
| Feed post action images | Feed/Post Detail specific | Accepted as additive consumer assets |
| Profile grid/settings/status icons | Profile specific | Kept inside `feature:profile` |
| Search bottom-navigation icon | Shell navigation asset | Uses the real Flutter asset shape; no Material approximation |
| Bottom Island geometry | Shared Shell component | One implementation retained; Search Workspace consumes it |

## Verification

- Design System unit tests: 9/9 passed, 0 failed, 0 errors, 0 skipped.
- Integrated BetaDebug compilation and app instrumentation Hilt/Shell gates
  passed.
- Login screenshot regression remains **Pending** until the Integration APK is
  clean-installed and the nine comparable Auth states are recaptured. Prior
  Login screenshots are baseline guidance, not fresh INT-01 runtime evidence.

## Gate

`Design System reconciliation: Passed (source and test level)`

`Login visual regression: Pending (fresh Integration capture required)`
