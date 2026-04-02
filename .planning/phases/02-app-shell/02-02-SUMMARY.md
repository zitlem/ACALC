---
phase: 02-app-shell
plan: 02
subsystem: ui
tags: [navigation3, jetpack-compose, material3, kotlin, android, navigation-bar]

# Dependency graph
requires:
  - phase: 02-app-shell plan 01
    provides: MainActivity with AcalcTheme, AppTheme composable, AndroidManifest with Theme.Acalc, edge-to-edge setup
provides:
  - AppShell.kt — Scaffold + NavigationBar (2 tabs) + NavDisplay using Navigation3 1.0.1
  - CalculatorScreen.kt — placeholder with centered icon and "Calculator" text
  - ConverterScreen.kt — placeholder with centered icon and "Unit Converter" text
  - TabRoute/CalculatorRoute/ConverterRoute — NavKey-implementing route objects
  - Functional two-tab app with bottom navigation, Material 3 theming throughout
  - Debug APK built and verified (app-debug.apk, 15MB)
affects: [phase-03-calculator, phase-04-converter, any phase adding screens to navigation]

# Tech tracking
tech-stack:
  added:
    - "androidx.compose.material:material-icons-core (via BOM 2026.03.01) — Icons.Default.* for bottom navigation icons"
  patterns:
    - "Navigation3 tab switching: backStack.clear() + backStack.add(route) replaces top-level tab — not push navigation"
    - "NavKey interface required on route objects in Navigation3 1.0.1 (not plain data objects)"
    - "entry<K> is a member of EntryProviderScope — NOT a top-level import from navigation3.runtime"
    - "innerPadding from Scaffold passed to screen composables via Modifier parameter, not applied to NavDisplay"

key-files:
  created:
    - app/src/main/kotlin/com/acalc/ui/AppShell.kt
    - app/src/main/kotlin/com/acalc/ui/screens/CalculatorScreen.kt
    - app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt
  modified:
    - app/src/main/kotlin/com/acalc/ui/MainActivity.kt
    - gradle/libs.versions.toml
    - app/build.gradle.kts

key-decisions:
  - "NavKey interface required: TabRoute sealed interface implements NavKey; data objects implement TabRoute"
  - "material-icons-core added to deps: Icons.Default.Home and Icons.Default.Refresh used as placeholders (avoids material-icons-extended 5MB overhead)"
  - "entry<K> import removed: it is a member of EntryProviderScope, not a top-level import — plan docs were incorrect"

patterns-established:
  - "Route objects: sealed interface : NavKey, data objects implement the sealed interface"
  - "Tab navigation: backStack.clear() then backStack.add(newRoute) for top-level tab switches"
  - "Screen composables: accept modifier: Modifier = Modifier and apply it as root modifier"

requirements-completed: [APP-02]

# Metrics
duration: 18min
completed: 2026-04-02
---

# Phase 02 Plan 02: App Shell Navigation Summary

**Navigation3 two-tab bottom navigation shell with Scaffold, NavigationBar, and NavDisplay — Calculator and Converter placeholder screens wired into MainActivity**

## Performance

- **Duration:** 18 min
- **Started:** 2026-04-02T09:00:00Z
- **Completed:** 2026-04-02T09:18:00Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Two-tab bottom navigation using Navigation3 1.0.1 with proper NavKey route objects
- CalculatorScreen and ConverterScreen placeholder composables with centered icon + text
- AppShell wired into MainActivity inside AcalcTheme — app fully functional
- Debug APK built (15MB), all existing unit tests pass with no regressions

## Task Commits

Each task was committed atomically:

1. **Task 1: Create placeholder screens and AppShell with Navigation3 tab navigation** - `2f0ff63` (feat)
2. **Task 2: Wire AppShell into MainActivity and verify full build** - `867b893` (feat)

**Plan metadata:** (docs commit — pending)

## Files Created/Modified
- `app/src/main/kotlin/com/acalc/ui/AppShell.kt` — Scaffold + NavigationBar + NavDisplay; CalculatorRoute/ConverterRoute; tab switch logic
- `app/src/main/kotlin/com/acalc/ui/screens/CalculatorScreen.kt` — centered Box with Home icon and "Calculator" text
- `app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt` — centered Box with Refresh icon and "Unit Converter" text
- `app/src/main/kotlin/com/acalc/ui/MainActivity.kt` — added AppShell() call inside AcalcTheme block
- `gradle/libs.versions.toml` — added compose-material-icons-core library entry (BOM-versioned)
- `app/build.gradle.kts` — added implementation(libs.compose.material.icons.core)

## Decisions Made
- Used `Icons.Default.Home` for Calculator tab and `Icons.Default.Refresh` for Converter tab (core icons, no extended set needed for placeholders)
- `material-icons-core` added as dependency (part of BOM, ~200KB — not the extended 5MB set)
- Route objects use `sealed interface TabRoute : NavKey` pattern so the type hierarchy is clear and extensible

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] NavKey interface required on route objects**
- **Found during:** Task 2 (assembleDebug failed)
- **Issue:** `rememberNavBackStack(CalculatorRoute)` failed with type mismatch — Navigation3 1.0.1 requires `NavKey` implementors, not arbitrary `Any` objects
- **Fix:** Changed `sealed interface TabRoute` to `sealed interface TabRoute : NavKey`; added `import androidx.navigation3.runtime.NavKey`
- **Files modified:** AppShell.kt
- **Verification:** Build passes after fix
- **Committed in:** 867b893 (Task 2 commit)

**2. [Rule 3 - Blocking] Removed invalid `entry` top-level import**
- **Found during:** Task 2 (assembleDebug failed at line 15)
- **Issue:** Plan code included `import androidx.navigation3.runtime.entry` but no such top-level function exists — `entry<K>` is a member function of `EntryProviderScope` called within the `entryProvider { }` lambda
- **Fix:** Removed the `import androidx.navigation3.runtime.entry` line; `entry<K>` works without import as a scope member
- **Files modified:** AppShell.kt
- **Verification:** Build passes; entryProvider block with `entry<CalculatorRoute>` and `entry<ConverterRoute>` compiles correctly
- **Committed in:** 867b893 (Task 2 commit)

**3. [Rule 3 - Blocking] Added material-icons-core dependency**
- **Found during:** Task 2 (compileDebugKotlin failed on Icons.Default.* imports in all three files)
- **Issue:** `Icons.Default.Home` and `Icons.Default.Refresh` require `androidx.compose.material:material-icons-core` which was not in the project dependencies
- **Fix:** Added library entry to libs.versions.toml and `implementation(libs.compose.material.icons.core)` to app/build.gradle.kts
- **Files modified:** gradle/libs.versions.toml, app/build.gradle.kts
- **Verification:** Icons resolve correctly; build passes
- **Committed in:** 867b893 (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (all Rule 3 — blocking build issues)
**Impact on plan:** All three fixes were necessary for compilation. The plan's documentation of Navigation3 APIs was partially incorrect (NavKey requirement and entry import). No scope creep.

## Issues Encountered
- Navigation3 1.0.1 requires route objects to implement `NavKey` interface — plan stated "Any type" but the actual API enforces `NavKey` constraint
- `entry<K>` reified function is a member of `EntryProviderScope`, not importable as a standalone function — Kotlin inline/reified mechanics mean it's called within the lambda scope without import
- `material-icons-core` not included in base Compose BOM dependencies by default — must be explicitly added

## Known Stubs
- `CalculatorScreen` shows placeholder only (icon + text) — will be replaced in Phase 3
- `ConverterScreen` shows placeholder only (icon + text) — will be replaced in Phase 4
- Icons (Home/Refresh) are generic placeholders — appropriate icons could be added in Phase 5

These stubs are intentional: the plan's goal (functional two-tab navigation) is fully achieved. The placeholder screens will be replaced in Phases 3 and 4.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- App shell is complete — two-tab navigation works with proper Material 3 styling
- Phase 3 (Calculator) can replace CalculatorScreen content and add CalculatorViewModel
- Phase 4 (Converter) can replace ConverterScreen content and add ConverterViewModel
- Navigation back stack is single-entry per tab — Phases 3/4 may need to add `rememberSavedStateNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()` to entryDecorators when ViewModels are introduced

---
*Phase: 02-app-shell*
*Completed: 2026-04-02*
