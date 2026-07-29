# Flutter Visual Assets and Login Contract Audit

Date: 2026-07-28  
Flutter reference: `E:\vista` (read-only)  
Native consumer: `E:\vista_native`  
Scope: shared visual foundation and Login/OTP only

## Source of truth

- Login/OTP widget: `lib/features/auth/screens/auth_wizard_screen.dart`
- Login background: `lib/features/auth/widgets/ribbon_background.dart`
- theme/tokens: `lib/core/theme/app_theme.dart`
- asset declarations: `pubspec.yaml`
- Login logo sources: `lib/utils/images/logo/black-logo.png` and `logo-white.png`
- font sources: `lib/utils/fonts/`

No asset was downloaded or inferred from a screenshot. Flutter was not modified.

## Asset inventory

The repository contains 4,017 audited non-build asset files:

- 3,967 emoji-family files under `assets/emoji/modern/`: 3,966 PNG
  sprites plus `metadata.bin` (8,968 bytes), indexed by
  `lib/features/emoji/data/modern_emoji_map.json`. They are not consumed by
  Login and are not migrated in this slice.
- 41 image/SVG/audio assets listed below.
- 9 font files listed in the font table.

The Markdown table keeps the 3,967-file emoji family compact because all members have the same dynamic consumer and migration decision. The generated
[`2026-07-28-flutter-visual-assets-index.csv`](2026-07-28-flutter-visual-assets-index.csv)
contains the required per-file path, type, dimensions, SHA-256, consumer,
migration decision, Native destination, and notes for all 4,017 files.

| Flutter path | Type | Dimensions | SHA-256 | Consumer | Migrate? | Native destination / note |
| --- | --- | ---: | --- | --- | --- | --- |
| `assets/images/match_icon.png` | PNG | 1024×1024 | `963E7644446885694AB556F07936E5FEB6635EB0FBE041FF659AB57217BEE70A` | non-Login | No | Out of scope |
| `assets/images/modern_bg.jpg` | JPG | 480×750 | `F014D0EB14213BE2B3726C9542F85B6520409DB5710E300DBC74411F5C7F2430` | non-Login background | No | Out of scope |
| `assets/images/vista_custom_bg.png` | PNG | 1024×1024 | `9D5227CAD4E0FCA40A485D19EEE911BB38B9397EC169BF3B610E9D2BEA0AEA37` | non-Login background | No | Out of scope |
| `assets/images/vista_custom_bg_dark.png` | PNG | 1024×1024 | `D9DEFC6196F344FD3CD034E41F69C8224CDE487623BC04DA5FC0738E4344190C` | non-Login background | No | Out of scope |
| `assets/images/onboarding/viu_connect.png` | PNG | 1003×1568 | `8ED0D9ECFFDEC4274368DF14BC50CC785E7B019DDE3208046208455836EF8805` | onboarding | No | Out of scope |
| `assets/images/onboarding/viu_create.png` | PNG | 1003×1568 | `758F3040BF7F26C90207A6353F413221A51D1535E52BD40394DA83F0E8E0E106` | onboarding | No | Out of scope |
| `assets/images/onboarding/viu_private.png` | PNG | 1003×1568 | `DC156AD0FA14974E97801A322ED0CF3320D8FD117888253D20660E83A9FECD56` | onboarding | No | Out of scope |
| `assets/sounds/message-recive.mp3` | MP3 | — | `D9B387EF345D53A9020FFE0B963F3542A648FB3F9A4FE13E5AC924A5206DB762` | chat notification | No | Non-visual/out of scope |
| `assets/sounds/message-sent.mp3` | MP3 | — | `90DB6ABBAA1C97BBE4D33CAC23433E24B8A83529D1B3B5F803600580DBD615FD` | chat notification | No | Non-visual/out of scope |
| `lib/utils/images/default-avatar.jpg` | JPG | 360×360 | `721F1C8F3C36200DC9C61E1E8FE6AC6F7F07F20D62A09610AE4849EC467AE6FF` | profile fallback | No | Out of scope |
| `lib/utils/images/headerBack.jpg` | JPG | 591×1280 | `3C7AC93726EECF4DB440148C5A5FEEE54430AB9EFCE67ADFBF3A035E3ACA568F` | non-Login header | No | Out of scope |
| `lib/utils/images/support_icon.png` | PNG | 1024×1024 | `8F113E675D87DFD2BA6847C0D0A9328500E6E4BB7689677135468E4154B26A60` | support | No | Out of scope |
| `lib/utils/images/vistalogo-new.png` | PNG | 618×618 | `4ADC291A7914B4077FD79C768B8CA8149D1C330D95AD6205043B818B9FE1411C` | non-Login branding | No | Login uses a different asset |
| `lib/utils/images/vistalogo.png` | PNG | 1024×1024 | `0C0DFD6AFA555C381A7FAC8BF1DFB96BB8CA5CE18044147509ABA4328244F59C` | non-Login branding | No | Login uses a different asset |
| `lib/utils/images/bottomnavigation/email-outline.svg` | SVG | vector | `CAD072DB5A48529253EF5E81BE7B03A4D0BBF3249C8DB137EBF84885A057F7F0` | bottom navigation | No | Out of scope |
| `lib/utils/images/bottomnavigation/email.svg` | SVG | vector | `C6298857BA8B32CDB05772779635972D59EE56726BAD1E2588D484CFA6CB3742` | bottom navigation | No | Out of scope |
| `lib/utils/images/bottomnavigation/grid.svg` | SVG | vector | `CAF566E17D759BA7D88419DDF7CF80809BFDA2F9812FFB9D1F3EBC11BE658299` | bottom navigation | No | Out of scope |
| `lib/utils/images/bottomnavigation/home-outline.svg` | SVG | vector | `DDAC6CE06D68362095F7E8927BE8D5AE4AAE0F1CC5200B990903F40784D7D81F` | bottom navigation | No | Out of scope |
| `lib/utils/images/bottomnavigation/home.svg` | SVG | vector | `3711D76978F9A82A70251CDC10E2F5AFB6D7629C6A5F36B68D9F37BE321FA246` | bottom navigation | No | Out of scope |
| `lib/utils/images/bottomnavigation/magnifying-glass.svg` | SVG | vector | `1C863901A875AF7EF10F909FD1AD363A91F20ED46D9AEBDB490A5838DCB74441` | bottom navigation | No | Out of scope |
| `lib/utils/images/bottomnavigation/user-outline.svg` | SVG | vector | `9754C08179A869486558D59680374CC320744E675CB2DEF782E220D264367F06` | bottom navigation | No | Out of scope |
| `lib/utils/images/bottomnavigation/user.svg` | SVG | vector | `318CF8E520EB5F854533C3EC3A9238AFE7AF73F4F9F5F67C6BD052E9F17E21B4` | bottom navigation | No | Out of scope |
| `lib/utils/images/component/auth_hero.png` | PNG | 1846×1477 | `8B161D217A3BCE0A1BA2B44ED62CFB00A8C005BA69BCBABE95D85FA7860E68F5` | no current Login literal consumer | No | Do not introduce unused illustration |
| `lib/utils/images/component/comment.png` | PNG | 512×512 | `2CB5D8FBF3E664D56EDA1EB0C0989BCED8116BD6FBFD70408405430E6B73C286` | comments | No | Out of scope |
| `lib/utils/images/component/login.png` | PNG | 1024×1024 | `379AD78C7969C8D36D848855BAB2495F00ACAFE68E355320BDD44BC0EB892528` | non-current auth art | No | Current Login does not consume it |
| `lib/utils/images/component/reels.png` | PNG | 200×200 | `AF69DDA3A7B006C83A2D6660B082A24E17AAB3FF0907EEA451CC695282BE779D` | reels | No | Out of scope |
| `lib/utils/images/component/send.png` | PNG | 512×512 | `5A62CBD42BDB3FED4FDA3A3B50DC701918ED98145725F85D3E0CC88F9E1C1B48` | messaging | No | Out of scope |
| `lib/utils/images/logo/black-logo-backgrand.png` | PNG | 1024×1024 | `C13F6AECD294E565010562090C03FD40FA47669038AD6F89853C2FFB2517335A` | alternate logo | No | Current Login does not consume it |
| `lib/utils/images/logo/black-logo.png` | PNG | 1024×1024 | `13A42B455E836D64661E4E07C6DA8D3A25EE5213745F00CCADD3D827DAC2795C` | `AuthWizardScreen` light | Yes | `drawable-nodpi/vista_auth_logo_light.png` |
| `lib/utils/images/logo/logo-nowroz.png` | PNG | 2048×2048 | `70347052486F2D2AA740707358E64C382666EBD7B1F00B71CAAD6EA2D74F51F3` | seasonal logo | No | Not current Login |
| `lib/utils/images/logo/logo-white.png` | PNG | 618×618 | `6DF4233FD7BD60B1DCE2B76A6904B9582F8572263391085CBC54B1A476EAB510` | `AuthWizardScreen` dark | Yes | `drawable-nodpi/vista_auth_logo_dark.png` |
| `lib/utils/images/share_icons/gmail.png` | PNG | 512×512 | `2EC48CC5556438D9C2866F187EC25DA06BC90677D6444CC2E9109F2581681493` | share | No | Out of scope |
| `lib/utils/images/share_icons/modern.png` | PNG | 2048×2048 | `4C5944702A163FAC6E90BDC4AF9424045BB2993F3EFC0F5CD9C0FD5E5DB3B605` | share | No | Out of scope |
| `lib/utils/images/share_icons/Rubika.png` | PNG | 1080×1080 | `3FDF57004B538F921EC7D7D58D62602CC308F396DCEF79100B4A45C246F8D19A` | share | No | Out of scope |
| `lib/utils/images/share_icons/Social.png` | PNG | 2048×2048 | `ACAECF8A707B5EEE5F408C68F8B976FD6AAC01CDA7C8194B77F471E1A43CAAE7` | share | No | Out of scope |
| `lib/utils/images/share_icons/story.png` | PNG | 200×200 | `C733C697D739048748685ADA1B8874E095EA5F33E776A1D438727BFAC6C47F52` | story/share | No | Out of scope |
| `lib/utils/images/share_icons/whatsapp.png` | PNG | 256×256 | `E6BF808A9EBA14DB25FD545F6081FCAEFA22A933348D0E4A42EBB89528277783` | share | No | Out of scope |
| `lib/utils/images/wallpapers/dark_wallpaper.png` | PNG | 1696×2528 | `8AA91473441A72A4B3DF42A58A7B641852112B261E820B202F783F3A1759FE0E` | chat wallpaper | No | Out of scope |
| `lib/utils/images/wallpapers/dark_wallpaper2.png` | PNG | 1024×1536 | `4045F078B12EC546074DB0482C2F6F24E02EA9372678534E948C128C5E28299B` | chat wallpaper | No | Out of scope |
| `lib/utils/images/wallpapers/light_wallpaper.png` | PNG | 1696×2528 | `97DE4604BFBD2F6B786EE9421BE13FDB3901B84E3C22CB4BBC8E9B3D97096ECE` | chat wallpaper | No | Out of scope |
| `lib/utils/images/wallpapers/light_wallpaper1.png` | PNG | 1024×1536 | `0A994B67BEDAA8F8063CB6C98E4A8F24F74C26F922B40B6E60BF3093B6A4FC86` | chat wallpaper | No | Out of scope |

## Font inventory

| Flutter file | Weight/use | SHA-256 | Native result |
| --- | --- | --- | --- |
| `VazirmatnBlack.ttf` | 900 | `DF4E257BE9DE1574D11BA2970C69228A7DB24174913FF7F776BBF00A9AA5DC1B` | byte-identical |
| `VazirmatnExtraBold.ttf` | 800 | `5181C2E436CF1D5CF1C5631821B43D81881DCA040A12C9A442EF5DC0007001CF` | byte-identical |
| `VazirmatnBold.ttf` | 700 | `F635FDBEA28F265DE395BA83B4B1570DCF2F58D13C65469E61903B1C2D2AE723` | byte-identical |
| `VazirmatnSemiBold.ttf` | 600 | `3F239D8364C14EE32C46B6839FA197C28958358B82A9673D2C6ACC8506D78BC5` | byte-identical |
| `VazirmatnMedium.ttf` | 500 | `B986623E4DDEF10755E04BE39F8EA7BCB1DC08BFE8DD0AA6AF395736F256AD4A` | byte-identical |
| `VazirmatnRegular.ttf` | 400 | `B69FD4C680B8F3F225FEABCC655A2C585D97627B8F5F5C0F9985E894069F3A56` | byte-identical |
| `VazirmatnLight.ttf` | 300 | `9AB4F094B861B7D4E318F01953B9771DDEB189D7C79BBB3989D5A95A4B07FF60` | byte-identical |
| `Vazir.ttf` | legacy fallback | `8CFDC9FFEE39C559D9FB2C18493621C2F24528AC601BB3A2EB1C1767B9021A5A` | No; not used by current Login |
| `BauhausBold.ttf` | optional Latin brand font | `9DF125E03C81CA3E92074642FCA307D36E4FA1F1D230F2BA5EF45E1B570BC9D0` | No; not used by current Login |

The seven Native Vazirmatn files match their Flutter sources byte-for-byte.

## Login icon inventory

Flutter uses Material icons, not custom image assets, for `person_outline`, `lock_outline`, `visibility`, `visibility_off`, and the directional back arrow. Native therefore uses canonical Android vector equivalents. No approximate third-party icon or placeholder is used.

## Exact color mapping

| Flutter token/source | Exact value | Native token/use | Previous Native | Result |
| --- | --- | --- | --- | --- |
| `AppColors.primary` | `#6366F1` | `VistaBrandColors.Indigo` / focus | already exact | exact |
| `AppColors.primaryEnd` | `#8B5CF6` | `VistaBrandColors.Violet` | already exact | exact |
| `AppColors.primaryDark` | `#4F46E5` | primary button | already exact | exact |
| `AppColors.accent` | `#EC4899` | `VistaBrandColors.Pink` | already exact | exact |
| `AppColors.background` | `#F8F9FF` | semantic background | exact token retained | exact |
| `AppColors.surface` | `#FFFFFF` | surface | exact | exact |
| `AppColors.surfaceVariant` | `#F3F4FF` | auth field/OTP fill | exact | exact |
| `AppColors.border` | `#E5E7EB` | outline/divider | non-canonical divider | reconciled |
| `AppColors.textPrimary` | `#0F1117` | content primary | non-canonical black | reconciled |
| `AppColors.textSecondary` | `#6B7280` | content secondary | non-canonical gray | reconciled |
| auth hint | `#707787` | auth hint/icon tint | absent | exact screen value |
| `AppColors.error` | `#EF4444` | Material/TextField error | non-canonical | reconciled; OTP uses only the primary focus border |
| OTP inline error | `#E53935` | auth inline error | absent | exact screen value |
| `AppColors.success` | `#10B981` | semantic success | non-canonical | reconciled |
| `AppColors.warning` | `#F59E0B` | semantic warning | non-canonical | reconciled |
| footer/timer `Colors.grey` | `#9E9E9E` | auth footer/timer | absent | exact screen value |
| disabled button (measured runtime) | `#D9D9DE` | auth disabled container | `#E5E7EB` | reconciled |
| ribbon base/orbs | `#FFFFFF`, `#F5F5F7`, `#EBEBF0`, `#EEEEEE` | Compose ribbon | placeholders | exact values/geometry |

The auth button is solid `#4F46E5`; Flutter does not use a gradient on this screen.

## Typography mapping

Family: `Vazirmatn`. The primary Login mappings are:

| Role | Flutter | Native |
| --- | --- | --- |
| title/OTP title | 22sp, 700, line-height 1.25 (27.5sp), tracking -0.2sp | exact |
| input/hint | 16sp, 400, line-height 1.55 | exact |
| OTP subtitle/error/timer | 14sp, 400, line-height 1.5 | exact |
| button | 15sp, 600 | exact |
| footer | 13sp, 400, line-height 1.45 | exact |
| OTP digits | 22sp, 700 | exact |

The complete reconciled scale is 32/800, 26/800, 22/700, 18/600, 18/700, 16/600, 14/600, 16/400, 14/400, 13/400, 14/600, 12/500, and 11/500 with the exact Flutter line heights and letter spacing.

## Login geometry and behavior

| Element | Flutter source/runtime | Native result |
| --- | --- | --- |
| safe/scroll padding | 24dp horizontal/vertical, full viewport child | matched, including Flutter's +24dp centered-child offset |
| initial top spacer | 16dp | exact |
| logo | 100dp, contain | exact source asset and size |
| logo → title | 48dp | exact |
| title → field | source 32dp; measured visible gap accounts for platform font boxes | 30dp Compose calibration yields same visible geometry |
| field | source padding 16dp × 14dp, radius 12dp, border 1/1.5dp | measured 53dp Native control height, exact radius/borders |
| field → button | 24dp | exact |
| button | source padding 24dp × 14dp, radius 12dp | measured 51dp Native height |
| button → footer | 20dp | exact |
| OTP back/top | 40dp + Material back icon + 24dp | exact canonical vector and right alignment in RTL |
| OTP cells | 56×60dp, radius 12dp, spacing 8dp | exact |
| page transition | 600ms emphasized | exact |
| button state transition | 150ms | exact token |
| keyboard | resize, drag/tap dismiss | real Gboard verified |
| phone/OTP | LTR inside RTL shell | Unicode/isolation and runtime verified |

## Ownership and migration decision

The migrated logo/font files already exist in the Vista-controlled Flutter repository; no external license file or independent ownership record was found during this audit. Their origin is recorded, hashes are preserved, and no unrelated asset with unclear ownership was migrated. Non-Login assets remain in Flutter and require their own screen-level parity gate before migration.
