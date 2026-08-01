# Search Asset Migration Manifest

The Login inventory was reused as the baseline. Only Search-specific consumers were audited.

| Flutter path / source | SHA-256 / dimensions | Consumer | Native destination | Tint behavior | Migration |
| --- | --- | --- | --- | --- | --- |
| `lib/utils/images/bottomnavigation/magnifying-glass.svg` | Source: `1C863901A875AF7EF10F909FD1AD363A91F20ED46D9AEBDB490A5838DCB74441`; viewBox `0 0 24 24`; 278 bytes. Native vector: `0F3CB9A0067982A9736E1DD994B8F7DEC152EC8BA7D7EB2A04C514AEE9728F30`; 562 bytes | `HomeScreen` Search active/inactive tab | `feature/shell/src/main/res/drawable/ic_search_flutter.xml` | Active `#6366F1`; inactive Flutter light `Colors.grey[600]`; SVG uses `currentColor`; rendered 30×30 and selected scale 1.15 | Exact circle/path geometry converted to Android VectorDrawable; no redrawing or Internet asset. |
| Flutter Material `search_rounded` | Canonical framework icon; no file hash | Launcher/workspace leading icon | Compose canonical Search vector | Theme `onSurfaceVariant` in launcher; inherited icon color in field | Use canonical Material/Compose vector because Flutter source explicitly uses Material. |
| Flutter Material `close_rounded` | Canonical framework icon; no file hash | Clear query and delete recent item | Compose canonical close vector | Inherited content color | Use canonical Material/Compose vector. |
| Flutter Material `qr_code_scanner_rounded` | Canonical framework icon; no file hash | Workspace suffix action | Deferred until a real Native scanner destination exists | Inherited content color | Do not add a fake route. Preserve the ledger gap until capability exists or integration explicitly defers it. |
| Flutter Material `tag_rounded`, `person_search_rounded`, `trending_up_rounded` | Canonical framework icons; no file hashes | Recents and trending chips | Compose vectors or exact local paths | Inherited/chip content colors | Canonical vectors permitted. |
| Flutter Material `verified` | Canonical framework icon; no file hash | Verified user result | Compose verified vector/shape | Resolved from verification type/role; never guessed | Canonical vector permitted; render only from real data. |
| Flutter Material `article_outlined`, `image_outlined` | Canonical framework icons; no file hashes | Post missing-media/error fallback | Compose canonical/local vectors | Theme content color | Canonical vectors permitted. |
| Flutter Material `search_off_rounded` | Canonical framework icon, 52 logical px | Empty states | Compose SearchOff/local path | `onSurfaceVariant` | No custom empty illustration exists; do not invent one. |

## Negative evidence

- `SearchPage` contains no `Image.asset`, `AssetImage`, or `SvgPicture` consumer.
- The Search body has no dedicated bitmap, SVG illustration, or downloaded media.
- The verified badge is a Flutter Material icon, not a custom badge asset.
- Search active/inactive bottom-navigation visuals share the same custom SVG; only tint and selection scale differ.
