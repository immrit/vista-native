# Integration Ledger: INT-01

Base: a8bd967 (Login/OTP)
Target: integration/native-ui-parity

## Branches to Integrate
1. visual-parity-02/feed-profile (HEAD: 20b1694)
2. visual-functional-parity-03/search (HEAD: 4c9248b)

## High Risk Files
- settings.gradle.kts
- gradle/libs.versions.toml
- app/build.gradle.kts
- feature:shell / core:navigation
- core:designsystem (theme, colors, typography)
- core:database (Room schemas, migrations)
- core:network (Hilt API bindings)
