---
phase: 03-calculator
plan: "01"
subsystem: calculator-viewmodel
tags: [tdd, viewmodel, stateflow, calculator, unit-tests]
dependency_graph:
  requires:
    - app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt
  provides:
    - app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorViewModel.kt
    - app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorState (data class)
  affects:
    - app/src/main/kotlin/com/acalc/ui/screens/CalculatorScreen.kt (binds ViewModel in Plan 02)
tech_stack:
  added: []
  patterns:
    - ViewModel + StateFlow (MVVM)
    - TDD red-green-refactor
    - Custom expression evaluator delegation
key_files:
  created:
    - app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorViewModel.kt
    - app/src/test/kotlin/com/acalc/ui/viewmodel/CalculatorViewModelTest.kt
  modified: []
decisions:
  - "resultShown boolean flag in ViewModel (not in CalculatorState) — UI doesn't need this internal continuation flag"
  - "compute() helper extracted to avoid duplication between onEquals() and onPercent()"
  - "currentToken() helper used by both onDecimal() and onPercent() to find last number token"
  - "x-to-star substitution done in compute() not in onOperator() — expression string stores x for display, * sent only to evaluator"
  - "Unformatted result stored back to expression field for continued ops — formatResult result stored in state.result only"
metrics:
  duration_seconds: 4577
  completed_date: "2026-04-02"
  tasks_completed: 1
  files_created: 2
  files_modified: 0
---

# Phase 03 Plan 01: CalculatorViewModel Summary

**One-liner:** TDD-built CalculatorViewModel with StateFlow exposing expression/result/isError state, full operator precedence via ExpressionEvaluator, decimal guard, thousands-separator formatting, and error handling.

## What Was Built

`CalculatorViewModel` is a pure Kotlin class (no Android/Compose dependency) that manages all calculator input and output logic:

- **`CalculatorState`** data class with `expression: String`, `result: String`, `isError: Boolean`
- **`state: StateFlow<CalculatorState>`** for UI observation via `collectAsStateWithLifecycle()`
- **7 public input handlers:** `onDigit`, `onOperator`, `onDecimal`, `onClear`, `onBackspace`, `onPercent`, `onEquals`
- **3 private helpers:** `currentToken()`, `compute()`, `formatResult()`

### Key behaviors implemented

| CALC | Requirement | Implementation |
|------|-------------|----------------|
| CALC-01 | Basic arithmetic with operator precedence | Delegates to `ExpressionEvaluator.evaluate()` |
| CALC-02 | Decimal guard — no double-decimal | `currentToken()` checks for existing `.` before appending |
| CALC-03 | Clear resets all; backspace removes last char | `onClear()` resets to `CalculatorState()`; `onBackspace()` uses `dropLast(1)` |
| CALC-04 | Percent evaluates as /100 | Appends `/100` to expression and calls `compute()` |
| CALC-05 | Expression visible while typing | `_state.value.expression` updated on every input |
| CALC-06 | Division by zero returns "Error" | `evaluator.evaluate()` returns null; ViewModel maps null → "Error" + `isError=true` |
| CALC-07 | Thousands separators on large results | `NumberFormat.getIntegerInstance()` for whole numbers; `NumberFormat.getNumberInstance()` with grouping for decimals |

### Edge cases handled

- Operator replacement: trailing operator replaced, not doubled
- Trailing operator trimmed before evaluation
- Result continuation: after equals, digit starts fresh; operator continues from result
- Leading operator on empty expression ignored except `-` (unary minus)
- `x` display operator substituted with `*` before passing to evaluator
- Whole-number results stripped of trailing `.0`

## Test Coverage

**21 unit tests** in `CalculatorViewModelTest.kt` — all pass.

Tests exercise every CALC requirement plus edge cases: operator replacement, trailing operator trimming, result continuation (fresh digit + continued operator), leading operator guard, backspace-on-unary-minus, whole-number display, and x-to-star substitution.

## TDD Execution

| Phase | Commit | Status |
|-------|--------|--------|
| RED | `e9e6e13` | 19/21 tests failing (stub ViewModel compiles, does nothing) |
| GREEN | `e7fcf47` | 21/21 tests passing |
| REFACTOR | (none needed) | Code already clean from GREEN |

## Deviations from Plan

None — plan executed exactly as written. The `currentToken()` helper extraction the plan suggested for REFACTOR was done naturally during GREEN implementation, eliminating the need for a separate REFACTOR commit.

## Known Stubs

None. All behaviors are fully implemented and verified by passing tests.

## Self-Check

Files created:
- `app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorViewModel.kt` — FOUND
- `app/src/test/kotlin/com/acalc/ui/viewmodel/CalculatorViewModelTest.kt` — FOUND

Commits:
- `e9e6e13` — RED phase commit — FOUND
- `e7fcf47` — GREEN phase commit — FOUND

Test count: 21 (@Test annotations) — meets >= 12 requirement

Full test suite: PASSED (BUILD SUCCESSFUL, no regressions in domain tests)
