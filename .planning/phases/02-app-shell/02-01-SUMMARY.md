---
phase: 02-app-shell
plan: 01
subsystem: ui
tags: [android, compose, material3, material-you, dynamic-color, edge-to-edge, theming]

# Dependency graph
requires:
  - phase: 01-domain-foundation
    provides: Android project structure, build scripts, domain classes (ConversionEngine, ExpressionEvaluator)
provides:
  - AcalcTheme composable with Material You dynamic color on API 31+ and purple fallback on API 26-30
  - MainActivity entry point with edge-to-edge display enabled
  - res/values/themes.xml XML stub resolving Phase 1 AAPT deferral
  - res/values-night/themes.xml dark mode variant
  - Updated AndroidManifest.xml with MainActivity launcher declaration
  - Buildable debug APK (app-debug.apk)
affects:
  - 02-02 (AppShell + navigation builds on top of AcalcTheme and MainActivity)
  - 03-calculator (uses AcalcTheme)
  - 04-converter (uses AcalcTheme)

# Tech tracking
tech-stack:
  added:
    - "com.google.android.material:material:1.12.0 — provides Theme.Material3.DayNight.NoActionBar XML parent style"
  patterns:
    - "Material You dynamic color: dynamicLightColorScheme/dynamicDarkColorScheme gated behind Build.VERSION_CODES.S"
    - "enableEdgeToEdge() must be called before setContent {} in ComponentActivity.onCreate"
    - "AcalcTheme wraps all Compose content in MainActivity"

key-files:
  created:
    - app/src/main/kotlin/com/acalc/ui/AppTheme.kt
    - app/src/main/kotlin/com/acalc/ui/MainActivity.kt
    - app/src/main/res/values/themes.xml
    - app/src/main/res/values-night/themes.xml
  modified:
    - app/src/main/AndroidManifest.xml
    - app/build.gradle.kts
    - gradle/libs.versions.toml

key-decisions:
  - "com.google.android.material:material added as dependency — compose-material3 alone does NOT provide Theme.Material3.DayNight.NoActionBar XML style; the View-based material library is required as the XML resource provider"
  - "Theme.Material3.DayNight.NoActionBar chosen as XML theme parent — provides correct Material 3 base without ActionBar for Compose apps"

patterns-established:
  - "AcalcTheme composable pattern: dynamicColor parameter allows test override, falls back to purple M3 baseline on pre-S devices"
  - "Activity pattern: enableEdgeToEdge() always before setContent {}"

requirements-completed: [APP-01]

# Metrics
duration: 3min
completed: 2026-04-02
---

# Phase 02 Plan 01: App Shell Infrastructure Summary

**Material You dynamic theming with edge-to-edge MainActivity — XML theme stub resolves Phase 1 AAPT deferral and produces a buildable debug APK**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-04-02T12:59:59Z
- **Completed:** 2026-04-02T13:03:50Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Created AcalcTheme composable with Material You dynamic color on API 31+ and purple (#6650A4) fallback on API 26-30
- Created MainActivity with enableEdgeToEdge() called before setContent (correct ordering per Android docs)
- Created res/values/themes.xml and res/values-night/themes.xml stubs, resolving the AAPT resource-linking failure deferred from Phase 1
- Updated AndroidManifest.xml to declare MainActivity as launcher activity with android:exported="true"
- Debug APK builds successfully (15MB)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create XML theme stubs and update AndroidManifest** - `32fd9d5` (feat)
2. **Task 2: Create AcalcTheme composable and MainActivity with edge-to-edge** - `374e29f` (feat)

**Plan metadata:** (docs commit to follow)

## Files Created/Modified
- `app/src/main/kotlin/com/acalc/ui/AppTheme.kt` - AcalcTheme composable with Material You + purple fallback
- `app/src/main/kotlin/com/acalc/ui/MainActivity.kt` - ComponentActivity entry point with edge-to-edge
- `app/src/main/res/values/themes.xml` - Theme.Acalc XML stub (resolves Phase 1 AAPT deferral)
- `app/src/main/res/values-night/themes.xml` - Dark mode theme variant
- `app/src/main/AndroidManifest.xml` - Added MainActivity launcher declaration with Theme.Acalc
- `app/build.gradle.kts` - Added com.google.android.material dependency
- `gradle/libs.versions.toml` - Added material = "1.12.0" version entry

## Decisions Made
- Added `com.google.android.material:material:1.12.0` as a dependency — the plan assumed `compose-material3` transitively provides the `Theme.Material3.DayNight.NoActionBar` XML style, but it does not; the View-based material library is required as the XML resource provider for the themes.xml stub.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Added com.google.android.material dependency for Theme.Material3.DayNight.NoActionBar**
- **Found during:** Task 2 (build verification step)
- **Issue:** `./gradlew :app:assembleDebug` failed with `AAPT: error: resource style/Theme.Material3.DayNight.NoActionBar not found`. The plan assumed this style was transitively available via `compose-material3`, but `compose-material3` is a Compose-only library and does not ship View-based XML theme resources. `Theme.Material3.DayNight.NoActionBar` lives in `com.google.android.material`.
- **Fix:** Added `com.google.android.material:material:1.12.0` to `libs.versions.toml` and `app/build.gradle.kts`. No code changes — library only needed as an XML resource provider.
- **Files modified:** app/build.gradle.kts, gradle/libs.versions.toml
- **Verification:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL; APK produced at app/build/outputs/apk/debug/app-debug.apk
- **Committed in:** `374e29f` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 — bug: missing dependency for XML theme parent)
**Impact on plan:** Essential fix for build to succeed. No scope creep — library added purely as resource provider.

## Issues Encountered
- The plan's research doc stated `Theme.Material3.DayNight.NoActionBar` is "provided by `com.google.android.material` which is transitively available via `compose-material3`" — this was incorrect. The transitive dependency chain was verified and `appcompat` / `com.google.android.material` are NOT in the dependency graph. Adding the explicit dependency was the correct resolution.

## User Setup Required
None — no external service configuration required.

## Next Phase Readiness
- AcalcTheme is ready for Plan 02 to use in AppShell
- MainActivity has an empty AcalcTheme {} body — Plan 02 replaces the comment with AppShell()
- All prerequisites for Plan 02 (bottom navigation, placeholder screens) are in place
- No blockers

---
*Phase: 02-app-shell*
*Completed: 2026-04-02*

## Self-Check: PASSED

- AppTheme.kt: FOUND
- MainActivity.kt: FOUND
- res/values/themes.xml: FOUND
- res/values-night/themes.xml: FOUND
- app-debug.apk: FOUND
- 02-01-SUMMARY.md: FOUND
- Commit 32fd9d5: FOUND
- Commit 374e29f: FOUND
