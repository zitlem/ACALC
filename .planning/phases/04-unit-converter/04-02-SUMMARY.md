---
phase: 04-unit-converter
plan: "02"
subsystem: ui-converter
tags: [compose, converter, ui, viewmodel, bidirectional]
dependency_graph:
  requires: ["04-01"]
  provides: ["CONV-01", "CONV-02", "CONV-03", "CONV-04", "CONV-05", "CONV-06", "CONV-07", "CONV-08", "CONV-09", "CONV-10", "CONV-13"]
  affects: [ConverterScreen, AppShell]
tech_stack:
  added: []
  patterns:
    - "Stateless inner composable (ConverterContent) for @Preview support"
    - "ExposedDropdownMenuBox with ExposedDropdownMenuAnchorType.PrimaryNotEditable (non-deprecated API)"
    - "PrimaryScrollableTabRow for category tabs"
    - "KeyboardType.Text for expression-supporting input fields"
    - "collectAsStateWithLifecycle for lifecycle-aware StateFlow observation"
key_files:
  created: []
  modified:
    - app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt
decisions:
  - "Used PrimaryScrollableTabRow instead of deprecated ScrollableTabRow for category tabs"
  - "Used ExposedDropdownMenuAnchorType instead of deprecated MenuAnchorType alias"
  - "KeyboardType.Text on input fields to allow expression operators (+, -, *, /)"
  - "getUnitsForCategory returns List<Pair<String,String>> — used second element (displayName) in dropdown items"
  - "Auto-approved human-verify checkpoint (auto-chain active)"
metrics:
  duration_minutes: 4
  completed_date: "2026-04-02"
  tasks_completed: 2
  files_modified: 1
---

# Phase 04 Plan 02: Converter UI Summary

**One-liner:** Full converter UI with PrimaryScrollableTabRow category tabs, bidirectional OutlinedTextField inputs, and ExposedDropdownMenuBox unit selectors wired to ConverterViewModel via collectAsStateWithLifecycle.

## What Was Built

Replaced the placeholder ConverterScreen (34 lines) with a complete 249-line converter UI:

- `ConverterScreen` (outer) — creates ViewModel via `viewModel<ConverterViewModel> { ConverterViewModel() }`, collects state with `collectAsStateWithLifecycle()`, delegates to stateless inner composable
- `ConverterContent` (stateless inner) — accepts state + callbacks + pre-computed unit display data; supports `@Preview`
- `ConversionRow` — reusable Card containing an OutlinedTextField and a UnitDropdown side by side
- `UnitDropdown` — ExposedDropdownMenuBox with readOnly anchor using `ExposedDropdownMenuAnchorType.PrimaryNotEditable`; local `remember { mutableStateOf(false) }` for expanded state only
- `@Preview` at bottom using hard-coded length state for fast UI iteration

## Tasks

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Build ConverterScreen UI with category tabs, input fields, and unit dropdowns | c184fdb | ConverterScreen.kt |
| 2 | Verify converter UI (checkpoint:human-verify) | — | Auto-approved (auto-chain) |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Replaced deprecated `ScrollableTabRow` with `PrimaryScrollableTabRow`**
- **Found during:** Task 1 build verification
- **Issue:** `ScrollableTabRow` deprecated in Material3 1.4.0 — compiler error-level warning treated as error by Kotlin strict mode
- **Fix:** Replaced import and usage with `PrimaryScrollableTabRow`
- **Files modified:** ConverterScreen.kt
- **Commit:** c184fdb

**2. [Rule 1 - Bug] Replaced deprecated `MenuAnchorType` alias with `ExposedDropdownMenuAnchorType`**
- **Found during:** Task 1 build verification
- **Issue:** `MenuAnchorType` is a deprecated typealias for `ExposedDropdownMenuAnchorType` in Material3 1.4.0
- **Fix:** Updated import and all usages to use `ExposedDropdownMenuAnchorType` directly
- **Files modified:** ConverterScreen.kt
- **Commit:** c184fdb

**3. [Rule 2 - Missing] Added `@OptIn(ExperimentalMaterial3Api::class)` on `UnitDropdown`**
- **Found during:** Task 1 first build attempt
- **Issue:** `ExposedDropdownMenuBox` and related APIs require opt-in annotation
- **Fix:** Added `@OptIn(ExperimentalMaterial3Api::class)` and corresponding import
- **Files modified:** ConverterScreen.kt
- **Commit:** c184fdb

**4. [Rule 1 - API Mismatch] `getUnitsForCategory` returns `List<Pair<String, String>>` not `List<String>`**
- **Found during:** Reading actual ConverterViewModel implementation (plan interface was simplified)
- **Issue:** Plan interface showed `List<String>` but actual method returns `List<Pair<String, String>>`
- **Fix:** Used second element of each pair (`display`) in DropdownMenuItem text
- **Files modified:** ConverterScreen.kt
- **Commit:** c184fdb

## Verification

- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL, no warnings
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL, all tests pass
- All 13 acceptance criteria from plan pass

## Known Stubs

None — all data flows from ConverterViewModel which was fully implemented in Plan 01.

## Self-Check: PASSED

- ConverterScreen.kt exists at expected path: FOUND
- Task 1 commit c184fdb exists: FOUND
- Build compiles clean: VERIFIED
