# Feed Runtime Pixel Compare - 2026-08-29

## Scope

Strict runtime comparison between Flutter Vista and Native Vista feed screen on the same emulator after recent local changes.

Device:
- ADB serial: `emulator-5554`
- Physical size: `1080x2400`
- Density: `420`

## Installed APKs

Flutter reference APK:
- Path: `E:\vista\build\app\outputs\flutter-apk\app-debug.apk`
- Package: `ir.coffevista.vista`
- Version: `4049 / 2.6.2`
- Last modified: `2026-08-26 20:56:42`
- Launch activity: `ir.coffevista.vista.MainActivity`

Native APK:
- Path: `E:\vista_native\vista-production.apk`
- Package: `ir.coffevista.vista_native.production`
- Version: `4050 / 2.6.3`
- Last modified: `2026-08-28 19:01:57`
- Launch activity: `ir.coffevista.vista_native.MainActivity`

## Build Status

Result: `PARTIAL`

This run installed and launched available APKs, but did not produce fresh current-source APKs:
- Native `:app:assembleProductionDebug` through `scripts/gradle_run.py` was blocked by `workflow lock is unavailable`.
- Flutter `flutter build apk --debug` failed before producing a fresh reference APK with `java.io.IOException: Unable to establish loopback connection`.

Therefore this is a live installed-APK comparison, not a fully valid latest-source acceptance gate.

## Captures

Initial 10 second captures:
- Flutter: `E:\vista_native\artifacts\screenshots\2026-08-29-feed-pixel-compare\flutter.png`
- Native: `E:\vista_native\artifacts\screenshots\2026-08-29-feed-pixel-compare\native.png`

Both initial captures were still splash/loading states, so they were rejected for feed parity.

Feed captures after longer wait:
- Flutter: `E:\vista_native\artifacts\screenshots\2026-08-29-feed-pixel-compare\flutter-wait45.png`
- Native: `E:\vista_native\artifacts\screenshots\2026-08-29-feed-pixel-compare\native-wait45.png`
- Contact sheet: `E:\vista_native\artifacts\screenshots\2026-08-29-feed-pixel-compare\contact-sheet-wait45.png`
- Amplified diff: `E:\vista_native\artifacts\screenshots\2026-08-29-feed-pixel-compare\diff-amplified-wait45.png`
- UI hierarchy, Flutter: `E:\vista_native\artifacts\screenshots\2026-08-29-feed-pixel-compare\window-flutter.xml`
- UI hierarchy, Native: `E:\vista_native\artifacts\screenshots\2026-08-29-feed-pixel-compare\window-native.xml`

## Pixel Metric

Compared images:
- Flutter: `flutter-wait45.png`
- Native: `native-wait45.png`

Result:
- Size: `1080x2400`
- Total pixels: `2,592,000`
- Pixels changed over threshold 8: `497,883`
- Changed percentage: `19.21%`
- Mean absolute channel delta: `13.85`
- Max channel delta: `255`

Important limitation: the feed data was not identical between the two apps. The metric includes dynamic content differences such as different posts, users, avatars, like states, notification badge state, dates, and loaded media. This number cannot be used as a pure UI-layout parity score.

## Strict Visual Findings

Status: `NOT PIXEL-PARITY`

Observed differences that are not only content differences:
- Header/tab typography differs. Flutter renders selected/unselected tab text lighter and visually smaller; Native is heavier/bolder.
- Header vertical layout differs. Native has the tab row and divider shifted compared with Flutter.
- Story creator differs. Flutter shows a large outlined circle with centered plus and label `استوری جدید`; Native shows an avatar placeholder with a small plus badge and label `استوری شما`.
- Notification badge differs. Flutter shows a red count badge on the bell; Native capture has no count badge.
- Feed card rhythm differs. Native posts are more densely stacked vertically in the same viewport.
- Post action row spacing and icon alignment differ. Native action icons sit lower/closer to text and show different active/inactive visual weight.
- Bottom navigation differs. Flutter bottom nav has stronger translucent/media-backed blur and a different selected tab container; Native appears flatter/whiter with different icon sizing and positioning.
- Floating create button differs. Flutter FAB overlaps the follow button/bottom region differently from Native; Native FAB sits lower and closer to nav overlap.
- Verification badge visuals differ. Flutter uses a spiky verified badge shape; Native uses a plain blue circle/check style in the captured feed.

## Runtime Notes

Native reached the feed after a longer wait, but logcat repeatedly showed realtime chat websocket `401` messages:
- `VistaChatRealtime: WebSocket failure type=ProtocolException ... http=401`

Flutter reached the feed after a longer wait and showed repeated feed event API calls:
- `https://api.coffevista.ir/v1/feed/event`

After captures were complete, ADB shell commands started hanging. Restarting ADB changed the emulator transport to `offline`, so the final attempt to leave Native foregrounded could not be re-verified. The feed captures above were taken before this transport failure.

## Acceptance Verdict

Current status: `PARTIAL / BLOCKED`

Accepted:
- Emulator was started and visible.
- Both APKs were installed on the same emulator.
- Both apps were launched.
- Feed screenshots were captured after runtime settle.
- Pixel diff/contact sheet artifacts were generated.

Not accepted:
- Fresh latest-source Native build was not produced in this run.
- Fresh latest-source Flutter reference build was not produced in this run.
- Feed state was not deterministic or identical across apps.
- Pixel parity is not achieved based on the live installed APK captures.

Next gate before closing parity:
- Clear the Gradle workflow lock issue without deleting unrelated/user work.
- Produce fresh Flutter and Native APKs from current source.
- Launch both with the same authenticated account and the same deterministic feed position.
- Capture feed, post detail, comment sheet, reply composer, like/unlike, bookmark, report/edit/delete, and share flows.
- Re-run pixel comparison per screen/state, not only the main feed.

## Follow-up Implementation - 2026-08-29

Implemented Native parity fixes after this runtime comparison:
- `StoryTray` now follows Flutter `StoryBar` structure more closely: the first item is always the `استوری جدید` add-story button, not a conditional `استوری شما` tile.
- `StoryTray` no longer removes the current user's active stories from the visible story list; active story users remain in the same player index space passed to the story player.
- Story tray spacing, ring size, add button size, text width, and label typography were aligned to Flutter's `AddStoryButton` and `_AnimatedStoryRing` dimensions.
- Feed story tray is constrained to Flutter's `115dp` height.
- Feed tab text was adjusted toward Flutter's `TabBar`: `16sp`, selected primary color, lighter unselected text, and less aggressive selected weight.
- Feed FAB was moved to bottom-start with Flutter's `bottom: 90` visual offset instead of bottom-end.
- Feed verified badge rendering was changed from a plain circle to a spiky verified-style badge closer to Flutter's `Icons.verified`.
- Feed notification bell now supports the Flutter-style red count badge UI.
- Shell feed root now passes a notification click callback into `FeedScreen`; the bell is no longer a dead no-op.

Remaining strict gaps:
- Native still does not have a Flutter-equivalent notifications list screen. The current callback routes to the existing notification settings screen as a temporary non-dead path, not full parity with Flutter `NotificationsPage`.
- Notification unread count is not yet wired from a Native social-notification repository/API, so the badge UI exists but still needs real count data.
- Bottom navigation differences are in `VistaShell`, not the feed module; full Flutter blur/material parity still needs a separate shell pass.
- These source changes are not runtime-accepted yet because Gradle wrapper execution failed before Kotlin compilation with HTTPS wrapper fetch failure.
