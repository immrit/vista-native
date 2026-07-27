# DSN-01 Phase 5–6 — Shell and Navigation Checkpoint

Date: 2026-07-27

## Scope and ownership

- `:feature:shell` owns exactly five tabs in the approved order: Feed, Search,
  Services, Chat and Profile.
- Each tab has a typed nested graph, typed root and typed controlled detail.
- No repository, network source, database entity, fake product feed, messaging,
  profile or service behavior was added.
- The app owns auth/session/deep-link orchestration and passes only a typed,
  screen-agnostic handoff request into Shell.

## Behavior implemented

- tab switch uses `popUpTo(...){ saveState = true }`, `launchSingleTop` and
  `restoreState`.
- tab reselection pops that tab to its typed root.
- Back from a non-Feed root selects Feed.
- Back from Feed root uses a two-second snackbar/exit contract.
- Profile logout first clears `SessionStore`, signs out memory state and removes
  authenticated Shell from the app back stack.
- supported deep links map to a controlled destination in Feed, Chat or Profile
  and keep delivery-id deduplication.

## Automated checkpoint

```text
.\gradlew.bat :feature:shell:testDebugUnitTest :app:assembleBetaDebug
  "-Pkotlin.incremental=false"
  "-Pvista.buildLogicOutput=build-logic-output-dsn01h"
  "-Pvista.isolatedBuildDir=true"
  "-Pvista.isolatedBuildOutput=dsn-build-output-08"
  --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL in 1m 18s`, 234 actionable tasks.

Shell tests: 3, failures 0. They cover the approved tab order, deferred-kind
mapping and root/nested Back policy.

Static checks also passed:

- module boundary and dependency cycle check
- design token leakage check
- `git diff --check`

Runtime tab restoration, logout, rotation, process recreation and deep-link
delivery remain intentionally unticked until API 33 device evidence exists.
