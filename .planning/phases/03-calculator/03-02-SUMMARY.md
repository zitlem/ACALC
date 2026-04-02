---
phase: 03-calculator
plan: 02
subsystem: ui
tags: [compose, material3, viewmodel, stateflow, calculator]

# Dependency graph
requires:
  - phase: 03-01
    provides: CalculatorViewModel with StateFlow<CalculatorState> and all input handlers
  - phase: 02-app-shell
    provides: AppShell routing, AcalcTheme, innerPadding modifier pattern
provides:
  - Full calculator UI with expression display, result display, and 5x4 button grid
  - CalculatorScreen wired to CalculatorViewModel via viewModel{} + collectAsStateWithLifecycle()
  - Material 3 color-coded buttons (FilledTonalButton digits, Button operators, OutlinedButton actions, tertiary equals)
  - @Preview composable via stateless CalculatorContent pattern
affects:
  - 04-converter (established stateless composable pattern for screens with @Preview)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Stateless inner composable pattern: CalculatorScreen(ViewModel owner) -> CalculatorContent(stateless, previewable)"
    - "viewModel<T> { T() } explicit type argument syntax for Compose viewModel factory"
    - "DisplayArea with Spacer(weight(1f)) to push text to bottom of available space"

key-files:
  created: []
  modified:
    - app/src/main/kotlin/com/acalc/ui/screens/CalculatorScreen.kt
    - app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorViewModel.kt

key-decisions:
  - "CalculatorViewModel extended ViewModel() to enable Compose viewModel{} integration — plan interface showed plain class, actual usage requires ViewModel base class"
  - "viewModel<CalculatorViewModel> { CalculatorViewModel() } explicit type argument used to avoid Kotlin intersection type inference warning"
  - "AspectRatio(1f) applied to all buttons for square shape — allows natural scaling with screen size"

patterns-established:
  - "Screen composable pattern: public CalculatorScreen owns ViewModel, private CalculatorContent is stateless and previewable"
  - "Button row layout: Row with Modifier.weight(1f) per button, padding(4.dp) per button, aspectRatio(1f) for square shape"

requirements-completed: [CALC-01, CALC-02, CALC-03, CALC-05]

# Metrics
duration: 5min
completed: 2026-04-02
---

# Phase 03 Plan 02: Calculator UI Summary

**Material 3 calculator screen with 5x4 button grid, dual-line display (expression + result), and color-coded buttons wired to CalculatorViewModel**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-04-02T21:44:43Z
- **Completed:** 2026-04-02T21:50:00Z
- **Tasks:** 2 (1 auto + 1 checkpoint auto-approved)
- **Files modified:** 2

## Accomplishments
- Replaced placeholder CalculatorScreen with full calculator implementation (252 lines vs 35)
- DisplayArea renders expression in headlineMedium and result in displaySmall, pushed to bottom with Spacer(weight(1f))
- ButtonGrid: 5-row x 4-column layout with correct key placement per D-01; Row 5 has "0" at weight(2f) for wide button
- Three button style categories: FilledTonalButton (digits/decimal), Button (operators), OutlinedButton (actions C/backspace/%), tertiary-colored Button (equals)
- Stateless CalculatorContent composable enables @Preview without ViewModel
- APK assembles successfully; all 24 unit tests pass

## Task Commits

Each task was committed atomically:

1. **Task 1: Replace CalculatorScreen placeholder with full calculator UI** - `5c1aa4d` (feat)
2. **Task 2: Visual verification of calculator UI** - auto-approved (no commit — checkpoint only)

**Plan metadata:** (docs commit — recorded below)

## Files Created/Modified
- `app/src/main/kotlin/com/acalc/ui/screens/CalculatorScreen.kt` - Full calculator UI: DisplayArea, ButtonGrid, all button composables, @Preview
- `app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorViewModel.kt` - Added ViewModel() base class (deviation fix)

## Decisions Made
- CalculatorViewModel extended ViewModel() — required for Compose viewModel{} factory; was a plain class in plan interface but needed the base class
- Used explicit `viewModel<CalculatorViewModel> { }` type argument to avoid Kotlin intersection type warning
- Button aspect ratio 1:1 with padding(4.dp) provides clean square grid layout that scales with device

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] CalculatorViewModel extended ViewModel() base class**
- **Found during:** Task 1 (compileDebugKotlin)
- **Issue:** Plan interface showed `class CalculatorViewModel` without ViewModel() supertype. Compose's `viewModel { }` factory requires the class to extend `androidx.lifecycle.ViewModel`, causing a "Return type mismatch" compilation error.
- **Fix:** Added `import androidx.lifecycle.ViewModel` and changed class declaration to `class CalculatorViewModel : ViewModel()`
- **Files modified:** app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorViewModel.kt
- **Verification:** `./gradlew :app:compileDebugKotlin` exits 0, `./gradlew :app:testDebugUnitTest` exits 0 (24 tests pass)
- **Committed in:** 5c1aa4d (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug in ViewModel class declaration)
**Impact on plan:** Essential fix — without it the app cannot compile. ViewModel base class is required by the Compose viewModel factory. No scope creep.

## Issues Encountered
- Kotlin type inference produced an intersection type warning for `viewModel { CalculatorViewModel() }` without explicit type argument. Fixed by using `viewModel<CalculatorViewModel> { }` syntax.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Calculator screen fully functional, ready for Phase 4 (Unit Converter)
- Stateless composable pattern (CalculatorContent) established for use in ConverterScreen
- CalculatorViewModel : ViewModel() pattern established; ConverterViewModel should follow the same

---
*Phase: 03-calculator*
*Completed: 2026-04-02*
