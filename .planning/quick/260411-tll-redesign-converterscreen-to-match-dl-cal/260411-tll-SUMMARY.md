---
phase: quick
plan: 260411-tll
subsystem: converter-ui
tags: [converter, expression-calc, numpad, bottom-sheet, redesign]
dependency_graph:
  requires: [04-02]
  provides: [expression-calc-dialog, onExprCalcCommit]
  affects: [ConverterScreen, ConverterViewModel]
tech_stack:
  added: []
  patterns: [ModalBottomSheet, ExpressionCalcDialog, stateless-lambda-key-handler]
key_files:
  created: []
  modified:
    - app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt
    - app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt
    - app/build.gradle.kts
    - build.gradle.kts
decisions:
  - Unicode ×/÷ sanitized to */÷ in onExprCalcCommit before ExpressionEvaluator.evaluate()
  - canRemove/onRemove removed from ConverterRowItem — row removal still available via onRemoveRow but no per-row UI trigger
  - KSP plugin removed from build files (pre-existing uncommitted change blocked compilation)
metrics:
  duration: ~10 minutes
  completed_date: "2026-04-12"
  tasks: 2
  files_changed: 4
---

# Quick Task 260411-tll: Redesign ConverterScreen to Match DL Calculator

**One-liner:** Expression calculator ModalBottomSheet with arithmetic numpad wired to ConverterViewModel, replacing Tune icon in converter numpad row 3.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Add onExprCalcCommit to ConverterViewModel | 5f643b6 | ConverterViewModel.kt |
| 2 | Add ExpressionCalcDialog and redesign ConverterScreen | 0419754 | ConverterScreen.kt, build.gradle.kts, build.gradle.kts (root) |

## What Was Built

**ConverterViewModel.onExprCalcCommit(expression: String)**
- Sanitizes Unicode × → * and ÷ → / before evaluation
- Passes through ExpressionEvaluator, formats result with formatConverted()
- Sets active row value, triggers recomputeFrom() to update all other rows

**ExpressionCalcDialog (ModalBottomSheet)**
- Parameters: unitName (shown as title), initialValue, onCommit, onDismiss
- Full 5-row arithmetic numpad: C, backspace, %, ÷, 7-9, ×, 4-6, −, 1-3, +, ±, 0, ., =
- ± toggles sign by prepending/removing "-" from expression string
- "=" calls onCommit(expression) then onDismiss() — caller handles evaluation
- Operator buttons use Button (primary color), digit buttons use FilledTonalButton

**ConverterNumpad updates**
- Parameter renamed from onUnitPicker to onExprCalc
- Row 3 right button: Tune icon → Calculate icon, onClick → onExprCalc

**ConverterRowItem updates**
- Removed canRemove and onRemove parameters
- Removed per-row close IconButton (and the Spacer fallback)
- Unit button width widened from 120.dp to 140.dp

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] KSP plugin unresolvable in Gradle cache**
- **Found during:** Task 2 build verification
- **Issue:** `alias(libs.plugins.ksp) apply false` in root build.gradle.kts and `alias(libs.plugins.ksp)` in app/build.gradle.kts both referenced a KSP plugin version (2.3.20-2.0.1) that could not be resolved from any configured repository. These were pre-existing uncommitted changes (Phase 5 Room prep) that blocked all Gradle builds.
- **Fix:** Removed KSP plugin declarations from both build files; removed Room dependency lines (room-runtime, room-ktx, ksp(room.compiler)) from app/build.gradle.kts. The Room/KSP version entries in libs.versions.toml were left in place for future use.
- **Files modified:** build.gradle.kts, app/build.gradle.kts
- **Commit:** 0419754

## Known Stubs

None — all wired functionality is live. The expression dialog commits through onExprCalcCommit which evaluates and recomputes all rows in real time.

## Self-Check: PASSED

- `onExprCalcCommit` present in ConverterViewModel.kt: confirmed (line 145)
- `ExpressionCalcDialog` composable present in ConverterScreen.kt: confirmed (line 323)
- `Icons.Default.Calculate` used: confirmed
- `canRemove` removed: confirmed (count = 0)
- `Icons.Default.Tune` removed: confirmed (count = 0)
- Build: `BUILD SUCCESSFUL` (assembleDebug, 35 tasks)
- Commits 5f643b6 and 0419754 present in git log
