---
phase: 01-domain-foundation
plan: 03
subsystem: domain
tags: [kotlin, bigdecimal, unit-conversion, tdd, junit4]

# Dependency graph
requires:
  - phase: 01-domain-foundation/01-01
    provides: Unit enum types (LengthUnit, WeightUnit, VolumeUnit, TempUnit, AreaUnit, SpeedUnit) consumed by ConversionEngine
provides:
  - ConversionEngine with BigDecimal precision conversion for all 6 unit categories
  - Comprehensive test suite covering all categories, round-trips, and temperature edge cases
affects:
  - 02-calculator-screen (ConversionEngine ready for ConverterViewModel)
  - 03-converter-screen (ConversionEngine is the core conversion logic provider)
  - 04-converter-live-update (ConversionEngine BigDecimal precision ensures lossless live updates)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "BigDecimal string-constructor for all numeric constants — never BigDecimal(double)"
    - "MathContext.DECIMAL128 on all divide() calls — prevents ArithmeticException on non-terminating decimals"
    - "Multiplicative factor maps with a common base unit — N factors, not N^2 conversions"
    - "Temperature via Celsius intermediate — no direct cross-unit formulas"
    - "Identity short-circuit (from == to) returns value unchanged — preserves exact input scale"

key-files:
  created:
    - app/src/main/kotlin/com/acalc/domain/ConversionEngine.kt
    - app/src/test/kotlin/com/acalc/domain/ConversionEngineTest.kt
  modified: []

key-decisions:
  - "BigDecimal string constructor used throughout — avoids floating-point representation errors in conversion factors"
  - "Generic convertMultiplicative<T>() reuses logic across 5 multiplicative categories via type parameter"
  - "Speed km/h and knots factors computed via BigDecimal.divide() at initialization — not hard-coded approximations"

patterns-established:
  - "ConversionEngine: multiplicative base-unit factor pattern for all non-temperature categories"
  - "Temperature: always convert through Celsius intermediate — never direct cross-unit formula"
  - "Test assertions: assertEquals(0, expected.compareTo(actual)) for exact BigDecimal comparison"
  - "Test assertions: BigDecimal subtract + abs < tolerance for approximate comparisons"

requirements-completed: []

# Metrics
duration: 2min
completed: 2026-04-02
---

# Phase 01 Plan 03: ConversionEngine Summary

**BigDecimal unit conversion engine for all 6 categories (length, weight, volume, temperature, area, speed) with exact factor maps and offset-aware temperature formulas, verified by 22 JUnit 4 tests**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-02T12:30:22Z
- **Completed:** 2026-04-02T12:32:00Z
- **Tasks:** 2 (RED + GREEN)
- **Files modified:** 2

## Accomplishments
- ConversionEngine with 6 overloaded convert() methods covering all unit categories
- Multiplicative factor maps for length, weight, volume, area, speed using exact BigDecimal string constants
- Offset-aware temperature conversion through Celsius intermediate — handles F/C/K cross-conversions
- 22 JUnit 4 tests covering all category conversions, round-trips, edge cases, and identity checks
- 25 mm -> in -> mm round-trip is lossless (SC-2), 32°F = 0°C (SC-3a), 0°C = 273.15 K (SC-3b)

## Task Commits

Each task was committed atomically:

1. **Task 1: RED — Write failing ConversionEngine tests** - `4c7adcb` (test)
2. **Task 2: GREEN — Implement ConversionEngine to pass all tests** - `432c27c` (feat)

_Note: TDD tasks have two commits (test RED → feat GREEN). No refactor needed._

## Files Created/Modified
- `app/src/main/kotlin/com/acalc/domain/ConversionEngine.kt` - Full BigDecimal conversion engine (105 lines)
- `app/src/test/kotlin/com/acalc/domain/ConversionEngineTest.kt` - 22 comprehensive conversion tests (155 lines)

## Decisions Made
- BigDecimal string constructor throughout — `BigDecimal("0.0254")` never `BigDecimal(0.0254)` — avoids precision loss from double literals
- Generic `convertMultiplicative<T>()` with type parameter and factor map eliminates code duplication across 5 of 6 categories
- Temperature always routes through Celsius intermediate rather than having direct cross-unit formulas — reduces error surface from 6 formulas to 4

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- ConversionEngine is complete and fully tested — ready for ConverterViewModel in Phase 3
- ExpressionEvaluator (Plan 02) + ConversionEngine (Plan 03) together form the complete domain foundation
- Phase 01 domain-foundation is now fully complete — all 3 plans done

## Self-Check: PASSED

All files confirmed present. All commits confirmed in git log.

---
*Phase: 01-domain-foundation*
*Completed: 2026-04-02*
