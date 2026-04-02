---
phase: 01-domain-foundation
plan: 01
subsystem: infra
tags: [android, gradle, kotlin, compose, agp, version-catalog]

# Dependency graph
requires: []
provides:
  - Android Gradle project skeleton with AGP 9.1.0, Kotlin 2.3.20, Compose BOM 2026.03.01
  - Version catalog (gradle/libs.versions.toml) with all dependency versions
  - compileSdk=36, targetSdk=35, minSdk=26 configuration
  - UnitCategory enum with 6 categories
  - 6 unit enum classes (LengthUnit, WeightUnit, VolumeUnit, TempUnit, AreaUnit, SpeedUnit)
  - Gradle 9.3.1 wrapper ready for ./gradlew commands
affects: [02-expression-evaluator, 03-conversion-engine, 04-calculator-ui, 05-converter-ui]

# Tech tracking
tech-stack:
  added:
    - AGP 9.1.0 (Android Gradle Plugin)
    - Kotlin 2.3.20 with built-in Compose compiler support
    - Gradle 9.3.1
    - Compose BOM 2026.03.01
    - compose-material3 1.4.0
    - activity-compose 1.12.3
    - lifecycle-viewmodel-compose 2.10.0
    - lifecycle-runtime-compose 2.10.0
    - navigation3-runtime 1.0.1
    - navigation3-ui 1.0.1
    - JUnit 4.13.2
  patterns:
    - Version catalog pattern (gradle/libs.versions.toml) for all dependency versions
    - Single app module structure with com.acalc package
    - Pure-Kotlin domain package (com.acalc.domain) with no Android imports

key-files:
  created:
    - gradle/libs.versions.toml
    - settings.gradle.kts
    - build.gradle.kts
    - app/build.gradle.kts
    - gradle.properties
    - gradlew
    - app/src/main/AndroidManifest.xml
    - app/src/main/kotlin/com/acalc/domain/UnitCategory.kt
    - app/src/main/kotlin/com/acalc/domain/Units.kt
    - .gitignore
  modified: []

key-decisions:
  - "Removed kotlinOptions block from app/build.gradle.kts — AGP 9.1.0 includes Kotlin support without needing separate kotlin-android plugin, and compileOptions sourceCompatibility/targetCompatibility is sufficient"
  - "Added kotlin.compose plugin (org.jetbrains.kotlin.plugin.compose) — required in Kotlin 2.0+ when Compose is enabled; previously bundled in kotlin-android plugin but now separate"
  - "Removed android:theme from AndroidManifest.xml — Material3 theme reference caused AAPT resource linking failure since no theme resources exist in Phase 1; theme will be added in Phase 2 with proper res/values setup"
  - "local.properties not committed — machine-specific path /home/sanya/android-sdk; added to .gitignore per Android convention"

patterns-established:
  - "Pattern 1: Version catalog — all library versions defined in gradle/libs.versions.toml, referenced via libs. accessor in build.gradle.kts files"
  - "Pattern 2: Domain isolation — com.acalc.domain package contains pure Kotlin enums with no Android imports"
  - "Pattern 3: Enum displayName property — each unit enum value carries a displayName String for future UI rendering"

requirements-completed: []

# Metrics
duration: 35min
completed: 2026-04-02
---

# Phase 01 Plan 01: Android Project Skeleton and Unit Enum Types Summary

**Compilable Android project skeleton with AGP 9.1.0/Kotlin 2.3.20/Compose BOM 2026.03.01 and 35 unit enum values across 6 categories matching REQUIREMENTS.md**

## Performance

- **Duration:** ~35 min
- **Started:** 2026-04-02T08:14:00Z
- **Completed:** 2026-04-02T08:50:00Z
- **Tasks:** 2 of 2
- **Files modified:** 10 created, 0 modified

## Accomplishments

- Full Android Gradle project skeleton compiles and runs `./gradlew :app:testDebugUnitTest` successfully (BUILD SUCCESSFUL, 0 tests expected)
- Version catalog with all 14 library entries and correct versions pinned to CLAUDE.md spec
- All 6 unit enum classes defined with exact unit sets from REQUIREMENTS.md CONV-04 through CONV-09 (35 total unit values)
- Kotlin Compose compiler plugin added (required for Kotlin 2.0+) and project verified compilable

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Android Gradle project skeleton with version catalog** - `5395984` (chore)
2. **Task 2: Define unit category and unit enum types** - `0c77b6e` (feat)

## Files Created/Modified

- `gradle/libs.versions.toml` — Version catalog with AGP 9.1.0, Kotlin 2.3.20, Compose BOM 2026.03.01, all 14 library entries
- `settings.gradle.kts` — Root project name ACALC, includes :app module
- `build.gradle.kts` — Root build script with AGP and Kotlin Compose plugins (apply false)
- `app/build.gradle.kts` — App module: compileSdk=36, minSdk=26, targetSdk=35, all dependencies
- `gradle.properties` — JVM args and AndroidX flag
- `gradlew` — Official Gradle 9.3.1 wrapper script
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 9.3.1 distribution URL
- `gradle/wrapper/gradle-wrapper.jar` — Gradle wrapper binary (46KB)
- `app/src/main/AndroidManifest.xml` — Minimal manifest (no Activity, no INTERNET permission)
- `app/src/main/kotlin/com/acalc/domain/UnitCategory.kt` — 6-value enum: LENGTH, WEIGHT, VOLUME, TEMPERATURE, AREA, SPEED
- `app/src/main/kotlin/com/acalc/domain/Units.kt` — 6 unit enums with displayName properties
- `.gitignore` — Standard Android gitignore (excludes .gradle/, build/, local.properties)

## Decisions Made

- Removed `kotlinOptions` block: AGP 9.1.0 with built-in Kotlin doesn't expose `kotlinOptions` directly when not applying `kotlin-android` separately; `compileOptions` with `JavaVersion.VERSION_17` is sufficient.
- Added `kotlin.compose` plugin: Kotlin 2.0+ requires `org.jetbrains.kotlin.plugin.compose` explicitly when Compose is enabled; without it Gradle fails with "Compose Compiler Gradle plugin is required".
- Removed android:theme from manifest: referencing `Theme.Material3.DynamicColors.DayNight` without the Material3 library's res values causes AAPT resource linking failure at compile time; the theme will be added in Phase 2.
- Created `.gitignore` with `local.properties` excluded: machine-specific SDK path should not be committed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed kotlinOptions block from app/build.gradle.kts**
- **Found during:** Task 1 (build skeleton)
- **Issue:** `kotlinOptions` is an unresolved reference in AGP 9.1.0 when not applying kotlin-android plugin; plan included it per CLAUDE.md spec but AGP 9.1.x requires a different approach
- **Fix:** Removed `kotlinOptions { jvmTarget = "17" }` block; `compileOptions` sourceCompatibility/targetCompatibility VERSION_17 is sufficient
- **Files modified:** app/build.gradle.kts
- **Verification:** `./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL
- **Committed in:** 5395984 (Task 1 commit)

**2. [Rule 3 - Blocking] Added kotlin.compose Compose Compiler plugin**
- **Found during:** Task 1 (build skeleton)
- **Issue:** Kotlin 2.0+ requires `org.jetbrains.kotlin.plugin.compose` explicitly; without it, Gradle errors with "Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required"
- **Fix:** Added `kotlin-compose` plugin entry to libs.versions.toml and applied in root/app build.gradle.kts
- **Files modified:** gradle/libs.versions.toml, build.gradle.kts, app/build.gradle.kts
- **Verification:** Build proceeds past plugin configuration stage
- **Committed in:** 5395984 (Task 1 commit)

**3. [Rule 1 - Bug] Removed android:theme from AndroidManifest.xml**
- **Found during:** Task 1 verification (running testDebugUnitTest)
- **Issue:** `Theme.Material3.DynamicColors.DayNight` not found by AAPT — the Material3 library's theme resources aren't accessible without a res/values/themes.xml stub; causes BUILD FAILED on resource linking
- **Fix:** Removed `android:theme` attribute from `<application>` tag; no Activity exists in Phase 1 so no theme is needed yet
- **Files modified:** app/src/main/AndroidManifest.xml
- **Verification:** `./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL
- **Committed in:** 5395984 (Task 1 commit)

**4. [Rule 3 - Blocking] Created local.properties with Android SDK path**
- **Found during:** Task 1 verification
- **Issue:** No local.properties existed; Gradle couldn't find the Android SDK
- **Fix:** Created `local.properties` with `sdk.dir=/home/sanya/android-sdk` (found at non-standard location)
- **Files modified:** local.properties (not committed — added to .gitignore)
- **Verification:** Build proceeds past SDK resolution
- **Committed in:** Not committed (added to .gitignore per Android convention)

**5. [Rule 1 - Bug] Replaced custom gradlew script with official Gradle script**
- **Found during:** Task 1 verification
- **Issue:** Custom gradlew script I wrote was missing the `APP_ARGS` variable assignment; every invocation ran `:help` regardless of arguments passed
- **Fix:** Downloaded official gradlew script from `https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradlew`
- **Files modified:** gradlew
- **Verification:** `./gradlew assembleDebug` correctly ran assembleDebug task (failed due to missing SDK, not wrong task)
- **Committed in:** 5395984 (Task 1 commit)

---

**Total deviations:** 5 auto-fixed (3 bugs, 2 blocking issues)
**Impact on plan:** All fixes necessary to achieve a compilable project. No scope creep — all changes are minimal corrections to reach the stated goal of `./gradlew :app:testDebugUnitTest` passing.

## Issues Encountered

- Gradle init scripts in `~/.gradle/init.d/` (from GitHub Actions environment) caused confusing output but did not affect build correctness
- Android SDK not at default location — found at `/home/sanya/android-sdk` instead of `~/Android/Sdk`

## User Setup Required

None - no external service configuration required. Android SDK is already installed at `/home/sanya/android-sdk`.

## Next Phase Readiness

- Plan 02 (ExpressionEvaluator) and Plan 03 (ConversionEngine) can now proceed — both depend on the unit enums defined here
- `./gradlew :app:testDebugUnitTest` is functional and will run JUnit 4 tests added in Plans 02 and 03
- Domain package `com.acalc.domain` is established with no Android imports pattern

---
*Phase: 01-domain-foundation*
*Completed: 2026-04-02*
