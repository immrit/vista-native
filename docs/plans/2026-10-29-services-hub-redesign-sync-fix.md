# Services Hub Modernization, Admin Sync, Game SSO Fix & UI/UX Redesign

**Type:** feature | bug | ui | refactor
**Status:** done
**Progress:** `[██████████] 100%`

## Problem / Goal
1. The Services screen in `vista_native` needs to be dynamic, modern, and completely synchronized with `vista-manager` (`E:/vista-manager`), including a live-toggleable "Under Maintenance" (در حال تعمیر) mode.
2. The "Game" (ویستا کوییز) section completely fails to launch because of domain mismatch (`coffevista.ir` vs `cafevista.ir`) and rigid host restriction in `InAppWebScreen` and `GameLaunchScreen`.
3. "Nearby" (اطراف من), "Top Groups" (گروه‌ها), and "Contacts" (مخاطبین) need a comprehensive, top-tier UI/UX redesign with RTL-first standards, fluid animations, and robust error/empty states.

## Root cause / Design
- **Game Launch Bug**: `GameLaunchScreen.kt:92` constructs `https://coffevista.ir/game/sso?ticket=...` and `AppNavGraph.kt:872` passes `restrictHost = "coffevista.ir"`. The actual web app is hosted on `cafevista.ir` (c-a-f-e). Navigating or redirecting to `cafevista.ir` is blocked by `isNavigationAllowed()`. Solution: Update URL to `https://cafevista.ir/game/sso`, allow both `cafevista.ir` and subdomains in `isNavigationAllowed`, and configure WebView cookie handling.
- **Maintenance Mode & Hub Sync**: `vista-backend/internal/serviceshub` needs maintenance configuration support (`is_enabled`, `title`, `message`, `retry_after_seconds`). `vista-manager` needs UI controls and API methods to toggle and edit maintenance details. `vista_native` needs `ServicesDto.kt`, `ServicesModels.kt`, and `ServicesScreen.kt` to render an elegant "Under Maintenance" state with retry and refresh.
- **UI/UX Overhaul**:
  - `NearbyScreen.kt`: Modern card discovery with radar sweep visual, distance badges, bio/interest tags, and responsive action controls.
  - `TopGroupsScreen.kt`: High-end directory design with category chips, rank badges (#1, #2), member avatars, and smooth join transitions.
  - `ContactsScreen.kt`: Persian-optimized contacts sync with permission hero state, search bar, Vista user list with direct chat, and invite CTA with system share.

## Phases

### Phase 1 — Investigate & Architecture Specification
- [x] Trace Game launch failure (`GameLaunchScreen.kt`, `InAppWebScreen.kt`, `AppNavGraph.kt`, domain mismatch)
- [x] Trace Backend services-hub contract & admin CRUD (`vista-backend/internal/serviceshub`)
- [x] Trace Android client models and screens (`ServicesScreen.kt`, `NearbyScreen.kt`, `TopGroupsScreen.kt`, `ContactsScreen.kt`)

### Phase 2 — Backend & Admin Implementation (`vista-backend` & `vista-manager`)
- [x] Add maintenance mode config to `vista-backend/internal/serviceshub/` (`models.go`, `repository.go`, `service.go`, `handlers.go`, `routes.go`)
- [x] Extend admin API endpoints: `GET/PUT /api/v1/admin/services-hub/maintenance`
- [x] Update `vista-manager` API client (`src/lib/api/services-hub.ts`) with maintenance types and functions
- [x] Add Maintenance Mode management UI in `vista-manager` (`src/app/dashboard/services/page.tsx`)

### Phase 3 — Fix Game Launch in `vista_native`
- [x] Fix target web URL in `GameLaunchScreen.kt` to `https://cafevista.ir/game/sso?ticket=$encodedTicket`
- [x] Fix `restrictHost` and allowed host logic in `AppNavGraph.kt` and `InAppWebScreen.kt` (allow `cafevista.ir` and subdomains)
- [x] Enable third-party cookies and proper error / retry handling in `InAppWebScreen.kt`

### Phase 4 — Services Hub Sync & Maintenance Mode UI in `vista_native`
- [x] Update `ServicesDto.kt`, `ServicesModels.kt`, `ServicesRepository.kt` to parse `MaintenanceConfig`
- [x] Add Maintenance screen state and view in `ServicesScreen.kt` and `ServicesViewModel.kt`
- [x] Modernize Services screen layout, quick actions, and banner presentation

### Phase 5 — Modern UI/UX Redesign for Nearby, Top Groups, and Contacts
- [x] Redesign `NearbyScreen.kt` & `NearbyCard.kt`: modern radar discovery animations, rich profile cards, haptic feedback, fluid swipe/tap gestures, and filter sheet
- [x] Redesign `TopGroupsScreen.kt`: category tabs, trending badges, member avatar stacks, verified markers, and join action feedback
- [x] Redesign `ContactsScreen.kt`: permission hero card, search with instant filter, segmented Vista members vs invite friends, and invite triggers

### Phase 6 — Verification
- [x] Verify Go backend build (`go build ./...`)
- [x] Verify Next.js manager TypeScript build / checks
- [x] Verify Android Kotlin compilation
- [x] Manual check of RTL, Persian strings, and dark theme consistency

## Decisions & Notes
- Web domain is `cafevista.ir`; API domain is `api.coffevista.ir`. WebView whitelist must accept `cafevista.ir`, `*.cafevista.ir`, `coffevista.ir`, `*.coffevista.ir`.
- Maintenance state in backend is stored in PostgreSQL table `services_hub_settings` so it persists across server restarts.
- RTL layout direction and `VistaFontFamily` (Vazirmatn) applied consistently across all Compose screens.
