---
phase: 01-domain-foundation
plan: "02"
subsystem: domain
tags: [kotlin, expression-parser, recursive-descent, junit4, tdd]

# Dependency graph
requires:
  - phase: 01-domain-foundation/01-01
    provides: domain package structure (com.acalc.domain), unit enum types, build scaffold with JUnit 4
provides:
  - Custom recursive-descent expression evaluator (ExpressionEvaluator.kt)
  - Full JUnit 4 test suite for arithmetic evaluation (ExpressionEvaluatorTest.kt)
  - Zero-dependency arithmetic parser supporting +, -, *, /, parentheses, unary minus, decimals
affects:
  - Phase 03 (Calculator) — CalculatorViewModel will call ExpressionEvaluator.evaluate()
  - Phase 04 (Unit Converter) — ConverterViewModel will use ExpressionEvaluator for expression input fields

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Recursive-descent parser operating on Double with pos cursor
    - Null-return error contract (any failure returns null, never throws to caller)
    - Leftover-input detection (pos != length) catches malformed expressions

key-files:
  created:
    - app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt
    - app/src/test/kotlin/com/acalc/domain/ExpressionEvaluatorTest.kt
  modified: []

key-decisions:
  - "Custom recursive-descent parser chosen over mXparser — zero dependencies, trivially testable, CLAUDE.md directive"
  - "evaluate() returns Double? with null-for-any-error contract — clean API for ViewModels to display error state"
  - "Parser operates on Double (not BigDecimal) — sufficient precision for expression entry; conversion precision handled by ConversionEngine with BigDecimal"
  - "Null return on Double!! in tests fixed by using !! unwrap operator — JUnit assertEquals(Double, Double, Double) requires non-nullable Double"

patterns-established:
  - "Null contract: ExpressionEvaluator.evaluate() returns null on any error — used by all callers in Phases 3 and 4"
  - "TDD: RED (stub + failing tests) then GREEN (full impl) committed as separate atomic commits"

requirements-completed: []

# Metrics
duration: 5min
completed: 2026-04-02
---

# Phase 01 Plan 02: ExpressionEvaluator Summary

**Zero-dependency recursive-descent arithmetic parser with null-error contract, covering precedence, parentheses, unary minus, and decimals with 21 JUnit 4 tests all passing**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-04-02T12:26:40Z
- **Completed:** 2026-04-02T12:32:00Z
- **Tasks:** 2 (RED + GREEN/REFACTOR)
- **Files modified:** 2

## Accomplishments

- Custom recursive-descent parser (no mXparser, no external dependencies) implemented in ~76 lines of Kotlin
- All 21 test cases pass: basic arithmetic, operator precedence, parentheses, unary minus, decimals, and 6 error cases
- Success criteria SC-1a through SC-1d from ROADMAP.md all met

## Task Commits

1. **Task 1: RED — Write failing ExpressionEvaluator tests** - `1aae4c3` (test)
2. **Task 2: GREEN — Implement recursive descent ExpressionEvaluator** - `845c05c` (feat)

## Files Created/Modified

- `app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt` — Full recursive-descent parser with evaluate() public API
- `app/src/test/kotlin/com/acalc/domain/ExpressionEvaluatorTest.kt` — 21 JUnit 4 tests covering all grammar cases and error paths

## Decisions Made

- Custom recursive-descent parser over mXparser — CLAUDE.md directive, zero dependency, ~80 lines vs 500+ function library
- `evaluate()` returns `Double?` (null on any failure) — clean contract for ViewModel to detect error state without try/catch
- Parser operates on Double rather than BigDecimal — expression entry precision is sufficient; conversion precision handled separately by ConversionEngine in Plan 01-03
- Tests use `!!` unwrap operator to satisfy JUnit `assertEquals(Double, Double, Double)` signature — Kotlin nullability requires explicit unwrap

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed test compilation error: Double? not assignable to Double in assertEquals**
- **Found during:** Task 1 (RED — write failing tests)
- **Issue:** `assertEquals(expected, evaluator.evaluate(...), delta)` fails compilation because `evaluate()` returns `Double?` but JUnit's `assertEquals(Double, Double, Double)` expects non-nullable `Double`
- **Fix:** Added `!!` unwrap operator on all arithmetic test assertions; added `assertNotNull` before the decimal addition result check
- **Files modified:** app/src/test/kotlin/com/acalc/domain/ExpressionEvaluatorTest.kt
- **Verification:** All 21 tests compiled and ran (15 failed as expected in RED phase)
- **Committed in:** 1aae4c3 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 — bug fix for compilation)
**Impact on plan:** Minimal — test intent unchanged, just corrected Kotlin nullability syntax. No scope creep.

## Issues Encountered

None beyond the compilation fix documented above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- ExpressionEvaluator is ready for use by Phase 3 (Calculator) and Phase 4 (Unit Converter)
- Import: `com.acalc.domain.ExpressionEvaluator`
- API: `ExpressionEvaluator().evaluate(input: String): Double?`
- Phase 01-03 (ConversionEngine) can proceed immediately — no dependency on this plan's artifacts

---
*Phase: 01-domain-foundation*
*Completed: 2026-04-02*
