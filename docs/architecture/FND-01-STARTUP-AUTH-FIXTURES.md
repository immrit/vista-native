# FND-01 Startup/Auth Parity and Debug Fixture Contract

## Scope and parity source

Flutter was inspected read-only at:

- `lib/features/auth/widgets/session_auth_wrapper.dart`
- `lib/services/onboarding_service.dart`
- the existing lookup, password, OTP and 2FA state owners under `lib/features/auth`

The retained startup order is maintenance, local session, refresh when required, and then the
versioned onboarding/auth boundary. A confirmed authentication rejection clears the account
session. A transient network failure preserves a valid local session and marks the authenticated
context offline. No-session continues through onboarding version `1.0.0` and then authentication.

Native intentionally does not implement the later profile-completion UI, the five-tab shell or any
feature destination in FND-01. The authenticated boundary remains the pre-existing placeholder.

## Fixture boundary

`StartupFixture` is a small extension point consumed by the real `StartupResolver`. Its production
Hilt graph contains an empty set through `@Multibinds`. The only implementation,
`DebugStartupFixture`, lives in `app/src/debug` and is therefore absent from release artifacts.

The intent extra `vista.foundation.fixture` accepts these debug-only scenarios:

| Scenario | Deterministic destination |
|---|---|
| `first-run` | Onboarding |
| `maintenance-enabled` | Maintenance |
| `maintenance-disabled` | Authentication |
| `valid-session` | Authenticated |
| `access-expired-refresh-success` | Authenticated |
| `refresh-expired` | Authentication |
| `revoked` | Authentication |
| `malformed` | Authentication |
| `offline-valid-session` | Authenticated with `offline=true` |
| `offline-no-session` | Authentication |

`none` is the only scenario that delegates to production collaborators. Thus an explicit test
scenario never depends on the network or backend state.

## Preserved contracts

- Valid access tokens pass without refresh.
- Missing/expired refresh credentials and revoked/malformed sessions fail closed to Auth.
- Transient refresh failure preserves a valid local context as offline.
- Login lookup can continue to password, OTP and password-required/2FA states.
- Successful authentication enters the authenticated boundary; logout leaves it.
- Onboarding completion and version matching still use the real DataStore-backed owner.
- Fixture configuration is performed before Compose content and startup/deep-link resolution.

## Runtime UI coverage

API 33 runtime covered maintenance, invalid session, offline valid session, process recreation,
logout, RTL, light/dark and a forced 90-degree display rotation. The rotation hierarchy reported
`rotation="1"` with bounds `2400x1080` while the authenticated placeholder remained visible.

No test fixture, screen or module for DSN-01 or later features was introduced.
