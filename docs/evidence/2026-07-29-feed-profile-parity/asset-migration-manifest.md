# Feed/Profile Asset Migration Manifest

Date: 2026-07-29
Rule: no internet/downloaded/approximate asset is used. Hashes are SHA-256 of the Flutter source bytes. Raster copies are byte-identical. SVG conversions must preserve the exact 24×24 source geometry and tint behavior.

| Flutter source | Destination / Native consumer | Source dimensions | SHA-256 | Tint behavior | Conversion |
| --- | --- | ---: | --- | --- | --- |
| `E:\vista\lib\utils\images\logo\logo-white.png` | Existing `app/src/main/res/drawable-nodpi/vista_logo_auth_dark.png`; Feed app bar receives this canonical app resource | 618×618 | `6df4233fd7bd60b1dce2b76a6904b9582f8572263391085cbc54b1a476eab510` | None | Already present byte-identical; no duplicate copy |
| `E:\vista\lib\utils\images\logo\black-logo.png` | Existing `app/src/main/res/drawable-nodpi/vista_logo_auth_light.png`; Feed app bar receives this canonical app resource | 1024×1024 | `13a42b455e836d64661e4e07c6da8d3a25ee5213745f00ccadd3d827dac2795c` | None | Already present byte-identical; no duplicate copy |
| `E:\vista\lib\utils\images\component\comment.png` | `core/designsystem/src/main/res/drawable-nodpi/vista_post_comment.png`; shared read-only post action row | 512×512 | `2cb5d8fbf3e664d56eda1eb0c0989bced8116bd6fbfd70408405430e6b73c286` | Flutter applies light/dark foreground tint | Byte-identical copy |
| `E:\vista\lib\utils\images\component\send.png` | `core/designsystem/src/main/res/drawable-nodpi/vista_post_send.png`; shared read-only post action row | 512×512 | `5a62cbd42bdb3fed4fda3a3b50dc701918ed98145725f85d3e0cc88f9e1c1b48` | Flutter applies light/dark foreground tint | Byte-identical copy |
| `E:\vista\lib\utils\images\default-avatar.jpg` | `core/designsystem/src/main/res/drawable-nodpi/vista_default_avatar.jpg`; post-detail avatar fallback | 360×360 | `721f1c8f3c36200dc9c61e1e8fe6ac6f7f07f20d62a09610ae4849ec467ae6ff` | None | Byte-identical copy |
| `E:\vista\lib\utils\images\bottomnavigation\home.svg` | `feature/shell/.../VistaNavigationIcons.kt` → active Feed icon | viewBox 0 0 24 24 | `3711d76978f9a82a70251cdc10e2f5afb6d7629c6a5f36b68d9f37be321fa246` | `currentColor`; selected primary | SVG path → Compose ImageVector, pending verification |
| `E:\vista\lib\utils\images\bottomnavigation\home-outline.svg` | same → inactive Feed icon | viewBox 0 0 24 24 | `ddac6ce06d68362095f7e8927be8d5ae4aae0f1cc5200b990903f40784d7d81f` | `currentColor`; grey | SVG stroke paths → Compose vector, pending verification |
| `E:\vista\lib\utils\images\bottomnavigation\magnifying-glass.svg` | same → Search icon | viewBox 0 0 24 24 | `1c863901a875af7ef10f909fd1ad363a91f20ed46d9aebdb490a5838dcb74441` | `currentColor` | SVG circle/path → Compose vector, pending verification |
| `E:\vista\lib\utils\images\bottomnavigation\grid.svg` | same → Services icon | viewBox 0 0 24 24 | `caf566e17d759ba7d88419ddf7cf80809bfda2f9812ffb9d1f3ebc11be658299` | `currentColor`; selected white | SVG rounded rects → Compose vector, pending verification |
| `E:\vista\lib\utils\images\bottomnavigation\email.svg` | same → active Chat icon | viewBox 0 0 24 24 | `c6298857ba8b32cdb05772779635972d59ee56726bad1e2588d484cfa6cb3742` | `currentColor` | SVG path → Compose ImageVector, pending verification |
| `E:\vista\lib\utils\images\bottomnavigation\email-outline.svg` | same → inactive Chat icon | viewBox 0 0 24 24 | `cad072db5a48529253ef5e81be7b03a4d0bbf3249c8db137ebf84885a057f7f0` | `currentColor` | SVG stroke/circles → Compose vector, pending verification |
| `E:\vista\lib\utils\images\bottomnavigation\user.svg` | same → active Profile icon | viewBox 0 0 24 24 | `318cf8e520eb5f854533c3ec3a9238afe7af73f4f9f5f67c6bd052e9f17e21b4` | `currentColor` | SVG circle/path → Compose vector, pending verification |
| `E:\vista\lib\utils\images\bottomnavigation\user-outline.svg` | same → inactive Profile icon | viewBox 0 0 24 24 | `9754c08179a869486558d59680374cc320744e675cb2def782e220d264367f06` | `currentColor` | SVG stroke circle/path → Compose vector, pending verification |

## Source-defined icons that are not external assets

| Consumer | Flutter source | Native rule |
| --- | --- | --- |
| Verification badge | `VerificationBadgeIcon` resolves to Material `Icons.verified` with blue/gold/black role-aware color | A Material verified glyph is correct here because Flutter itself uses it; preserve role/type color |
| Feed like/save/overflow/video/private/profile actions | Flutter Material icons in the scoped source | Use the corresponding Compose Material/vector geometry; do not substitute unrelated glyph text |
| Profile tabs | Flutter Material grid/play/music icons | Render only source-equivalent icon geometry; Posts tab is functional in scope |

## Login branch dependency

The Login worktree has not published new commits beyond base `f663484` at this audit checkpoint. Its dirty work moves brand assets into `:core:designsystem`. Feed does not duplicate the two logo files; it currently references the byte-identical existing app resources. Reconcile resource names after Login publishes a stable commit and record the imported hash/commit here.
