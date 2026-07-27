# DSN-01 Phase 0 — Baseline and Visual Audit

## Git baseline

- source branch: `fnd-01/architecture-foundation`
- source/final FND-01 commit: `1adaafa7ed67a66a333c553eb71b68c66c857daf`
- DSN branch: `dsn-01/design-system-app-shell`
- working tree before tracker: clean
- staged count: 0
- `.git/index.lock`: absent
- `1adaafa` is an ancestor of the DSN branch
- repository integrity: `git fsck --no-dangling --no-reflogs` passed

## Source review

The five Native sources of truth were reviewed in the required order. The
applicable anchors are:

- `VISTA_NATIVE_MASTER_PLAN.md:136-140`: Indigo/Violet/Pink, local Vazirmatn,
  RTL, centralized spacing/radius/elevation/motion/typography/component states.
- `VISTA_NATIVE_MASTER_PLAN.md:254-261`: real Design System, five-tab Shell,
  restoration and independent tab back stacks.
- `docs/audits/2026-07-25-aud-01-parity-ledger.md:116-130`: Flutter tab order
  and Back behavior.
- `docs/plans/2026-07-25-fnd-01-architecture-foundation.md:220-223`: current
  Startup/Auth behavior is preserved and Shell was intentionally deferred.
- `docs/evidence/2026-07-25-fnd-01/phase-13-exit-gate.md`: FND-01 Local Gate
  passed at 88 tests with a clean tree.

Flutter and Backend were not modified.

## Current Native inventory

Current modules:

`app`, `core:common`, `core:model`, `core:network`, `core:database`,
`core:datastore`, `core:security`, `core:testing`, `core:worker`,
`feature:auth`.

| Module | Main source files | Test files |
|---|---:|---:|
| `:app` | 11 | 4 |
| `:core:common` | 7 | 1 |
| `:core:model` | 2 | 0 |
| `:core:network` | 14 | 4 |
| `:core:database` | 4 | 1 |
| `:core:datastore` | 5 | 3 |
| `:core:security` | 2 | 2 |
| `:core:testing` | 1 | 0 |
| `:core:worker` | 4 | 2 |
| `:feature:auth` | 18 | 6 |

`:core:designsystem` and `:feature:shell` do not yet exist. Their consumers are
already concrete: the five existing Startup/Auth surfaces and the authenticated
route that must enter the Shell.

## Flutter visual and shell contract

Read-only source:

- palette/theme: `E:\vista\lib\core\theme\app_theme.dart`
- shell: `E:\vista\lib\features\home\screens\homeScreen.dart`
- font registration: `E:\vista\pubspec.yaml:171-186`

Confirmed brand colors:

| Token | Value |
|---|---|
| Primary / Indigo | `#6366F1` |
| Secondary / Violet | `#8B5CF6` |
| Accent / Pink | `#EC4899` |

Confirmed Flutter scales:

- spacing: `0, 2, 4, 8, 12, 16, 20, 24, 32, 48, 64`
- radius: `8, 12, 16, 20, 24, 28, pill`
- elevation: three semantic levels for card, popover/sheet and dialog/FAB
- icon baseline: 24; shell icons use 28
- surface behavior: light `#F8F9FF/#FFFFFF/#F3F4FF`; dark
  `#09090F/#13131E/#1C1C2E`
- bottom navigation: five tabs, floating/island presentation, selected label and
  icon treatment, state retained after first visit

Flutter tab order:

1. Feed
2. Search
3. Services
4. Chat
5. Profile

Flutter Back behavior is the compatibility baseline: Back on a non-Feed root
returns to Feed; Back on Feed root requires a second Back within two seconds to
exit. DSN-01 may improve implementation structure but must keep this familiar
behavior.

## Font evidence

All seven Native Vazirmatn files byte-match their Flutter sources:

| Weight | SHA-256 |
|---|---|
| Light | `9AB4F094B861B7D4E318F01953B9771DDEB189D7C79BBB3989D5A95A4B07FF60` |
| Regular | `B69FD4C680B8F3F225FEABCC655A2C585D97627B8F5F5C0F9985E894069F3A56` |
| Medium | `B986623E4DDEF10755E04BE39F8EA7BCB1DC08BFE8DD0AA6AF395736F256AD4A` |
| SemiBold | `3F239D8364C14EE32C46B6839FA197C28958358B82A9673D2C6ACC8506D78BC5` |
| Bold | `F635FDBEA28F265DE395BA83B4B1570DCF2F58D13C65469E61903B1C2D2AE723` |
| ExtraBold | `5181C2E436CF1D5CF1C5631821B43D81881DCA040A12C9A442EF5DC0007001CF` |
| Black | `DF4E257BE9DE1574D11BA2970C69228A7DB24174913FF7F776BBF00A9AA5DC1B` |

No font download is required.

## Runtime baseline

- AVD: `Medium_Phone_2`
- API: 33
- ABI: x86_64
- resolution: 1080×2400
- orientation: portrait
- initial theme: light
- `betaDebug` artifact reused from the passed FND matrix and installed
  successfully; no new SDK/image/AVD was installed.

Native baseline screenshots:

- [System startup splash](screenshots/baseline/native-startup-api33-light.png)
- [Auth](screenshots/baseline/native-auth-api33-light.png)
- [Authenticated boundary](screenshots/baseline/native-authenticated-boundary-api33-light.png)

Existing Flutter baseline screenshots remain under
`docs/audits/evidence/2026-07-25-aud-01-gate-closure/`, including all five tabs,
Light/Dark Auth/Home and large-font Services. They are reference evidence and
are not copied or presented as a new golden suite.

## Visual gaps

1. Native theme currently uses Cyan/Coral/Navy instead of the confirmed
   Indigo/Violet/Pink identity.
2. Theme and seven fonts are owned by `:app`, so Feature UI cannot consume a
   dedicated Design System owner.
3. Startup/Auth/Onboarding contain repeated raw `dp`, `sp`, alpha and Material
   component choices.
4. Authenticated boundary is a two-text placeholder with no five-tab Shell.
5. Native has no reusable component catalog, preview matrix or leakage check.
6. The system startup splash still displays the default Android launcher art;
   this is recorded separately from the Compose Startup screen.
7. Existing screenshots prove visual difference only; they do not establish
   pixel-perfect parity or a golden baseline.

## Reused baseline verification

FND-01 evidence is valid for this unchanged production baseline:

- four variants: pass
- 65 unit/contract + 23 device tests: pass
- lint: 0 errors
- R8/resource shrinking: pass
- secret and module checks: pass

These are baseline facts, not DSN-01 completion evidence. All affected checks
must run again after implementation.
