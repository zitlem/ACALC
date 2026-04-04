---
phase: 04-unit-converter
plan: "01"
subsystem: converter-viewmodel
tags: [tdd, viewmodel, bidirectional-conversion, state-management, unit-converter]
dependency_graph:
  requires: [domain/ExpressionEvaluator, domain/ConversionEngine, domain/Units, domain/UnitCategory]
  provides: [ConverterViewModel, ConverterState, UnitPair, CategoryState, ActiveField]
  affects: [04-02-converter-ui]
tech_stack:
  added: []
  patterns: [ViewModel+StateFlow, sealed-class-dispatch, per-category-state-map, TDD-red-green]
key_files:
  created:
    - app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt
    - app/src/test/kotlin/com/acalc/ui/viewmodel/ConverterViewModelTest.kt
  modified: []
decisions:
  - "Trailing decimal/operator guard added in ViewModel — '25.' is treated as still-typing rather than evaluated as 25.0, matching Pitfall 6 requirement"
  - "convertForCurrentCategory reads _state.value.units at call time — correct since state is always updated before method is called"
  - "categoryStateMap pre-populated with defaults at construction and kept in sync via updateAndSave — ensures category switching is O(1) with no re-initialization"
metrics:
  duration_minutes: 8
  completed_date: "2026-04-03"
  tasks_completed: 1
  files_created: 2
  files_modified: 0
---

# Phase 04 Plan 01: ConverterViewModel TDD Summary

**One-liner:** TDD ConverterViewModel with sealed UnitPair dispatch, bidirectional activeField guard, per-category state persistence, and expression evaluation before conversion.

## What Was Built

A fully tested `ConverterViewModel` that handles all converter logic in isolation before any UI exists:

- **State model:** `UnitPair` sealed class with 6 typed subclasses (Length, Weight, Volume, Temperature, Area, Speed), `CategoryState`, `ActiveField` enum, `ConverterState` data class
- **Bidirectional conversion:** `onTopChanged` and `onBottomChanged` set `activeField` first to prevent circular updates, evaluate expression via `ExpressionEvaluator`, then dispatch to `ConversionEngine`
- **Per-category state persistence:** `categoryStateMap` stores/restores `CategoryState` on `onCategorySelected` so switching categories and back restores the user's inputs
- **Expression evaluation:** `evaluator.evaluate(raw)` called before conversion; null result (incomplete expression) leaves the other field unchanged
- **Trailing input guard:** Raw strings ending in `.`, `,`, `+`, `-`, `*`, `/` are treated as mid-edit and skip conversion — prevents premature update when user types "25." as a leading partial decimal
- **Result formatting:** `setScale(10, HALF_UP).stripTrailingZeros().toPlainString()` — strips trailing zeros, no scientific notation, capped at 10 decimal places
- **UI helpers:** `getUnitsForCategory()` for dropdown population, `getTopUnitDisplayName()` / `getBottomUnitDisplayName()` for display

## Tests Written

18 tests covering all CONV-* requirements:

| Test | Requirement |
|------|-------------|
| test_defaultState_isLength_mmToInch | CONV-13 |
| test_allCategoriesHaveDefaults | CONV-13 |
| test_onTopChanged_convertsToBottom | CONV-01 |
| test_onBottomChanged_convertsToTop | CONV-02 |
| test_expressionInput_evaluatesThenConverts | CONV-03 |
| test_incompleteExpression_doesNotClearOtherField | Pitfall 6 |
| test_categorySwitchSavesAndRestores | CONV-10 |
| test_categorySwitchToWeight_showsDefaults | CONV-10 |
| test_temperature_100CtoF_is212 | CONV-07 |
| test_onTopUnitChanged_recomputes | — |
| test_onBottomUnitChanged_recomputes | — |
| test_formatConverted_stripsTrailingZeros | — |
| test_emptyInput_noConversion | — |
| test_lengthCategory_has8Units | CONV-04 |
| test_weightCategory_has6Units | CONV-05 |
| test_volumeCategory_has7Units | CONV-06 |
| test_areaCategory_has7Units | CONV-08 |
| test_speedCategory_has4Units | CONV-09 |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Trailing decimal guard for "25." input**
- **Found during:** GREEN phase — `test_incompleteExpression_doesNotClearOtherField` failed
- **Issue:** The `ExpressionEvaluator` parser accepts "25." as a valid number (25.0) since `parseNumber` consumes all digits and dots. Plan's Pitfall 6 requires "25." to be treated as incomplete.
- **Fix:** Added trailing character guard in `onTopChanged`/`onBottomChanged`: if `raw.trimEnd().last() in ".,+-*/"`, return early before evaluation.
- **Files modified:** `ConverterViewModel.kt`
- **Commit:** 79d82e5

## TDD Cycle

| Phase | Commit | Tests |
|-------|--------|-------|
| RED | 08513ef | 17 failing (1 pass — state model already correct) |
| GREEN | 79d82e5 | 18 passing |
| REFACTOR | — | No structural changes needed; code is clean |

## Known Stubs

None — all conversion logic is fully wired to `ConversionEngine` and `ExpressionEvaluator`. No placeholder data.

## Self-Check: PASSED
