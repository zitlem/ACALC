# Phase 3: Calculator - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a fully functional arithmetic calculator UI replacing the Calculator placeholder screen. Users can build expressions, see them in real-time, evaluate with =, and handle errors gracefully.

</domain>

<decisions>
## Implementation Decisions

### Calculator Layout
- **D-01:** Standard calculator keypad layout: digits 0-9, operators +, -, x, /, decimal point, equals, C (clear), backspace, and % key
- **D-02:** Expression display at top shows the full expression being built (CALC-05), result appears after pressing =
- **D-03:** Calculator buttons use Material 3 styling with distinct colors for digits vs operators vs action keys

### Input Behavior
- **D-04:** Building an expression string that gets evaluated by the existing ExpressionEvaluator from Phase 1
- **D-05:** Double-tapping decimal point does not insert a second decimal in the current number
- **D-06:** C clears all input, backspace deletes last character
- **D-07:** Operator chaining allowed (e.g., 2 + 3 * 4 evaluates with correct precedence)

### Display & Formatting
- **D-08:** Large numbers display with thousands separators (CALC-07)
- **D-09:** Division by zero shows "Error" message, not a crash (CALC-06)
- **D-10:** Result displays without trailing zeros (e.g., "4" not "4.0" for whole number results)

### Architecture
- **D-11:** CalculatorViewModel holds expression state as StateFlow, CalculatorScreen observes via collectAsStateWithLifecycle()
- **D-12:** ViewModel uses ExpressionEvaluator.evaluate() from Phase 1 domain layer — no new parsing logic

### Claude's Discretion
- Exact button sizes and spacing
- Color choices for operator vs digit vs action keys within Material 3 palette
- Animation/transition when showing results
- Whether to show secondary result line while typing

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Technology Stack
- `CLAUDE.md` — Full technology stack, especially ViewModel + StateFlow pattern, collectAsStateWithLifecycle()

### Domain Layer
- `app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt` — evaluate(String): Double? API to reuse
- `app/src/test/kotlin/com/acalc/domain/ExpressionEvaluatorTest.kt` — Test cases showing expected behavior

### UI Layer (Phase 2 output)
- `app/src/main/kotlin/com/acalc/ui/screens/CalculatorScreen.kt` — Current placeholder to replace
- `app/src/main/kotlin/com/acalc/ui/AppShell.kt` — Navigation wiring
- `app/src/main/kotlin/com/acalc/ui/AppTheme.kt` — AcalcTheme composable

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ExpressionEvaluator.evaluate(String): Double?` — returns null on error, handles +, -, *, /, parentheses, unary minus
- `AcalcTheme` with Material 3 color scheme — use MaterialTheme.colorScheme for button colors

### Established Patterns
- Package structure: `com.acalc.ui.screens` for screen composables, `com.acalc.ui` for shared UI
- ViewModel + StateFlow per CLAUDE.md — no LiveData

### Integration Points
- `CalculatorScreen.kt` receives `modifier: Modifier` from AppShell's innerPadding
- ViewModel created via `viewModel { CalculatorViewModel() }` — no DI

</code_context>

<specifics>
## Specific Ideas

- ExpressionEvaluator uses * for multiplication internally — UI should display "x" but convert to "*" for evaluation
- Inspired by ClevCalc's clean calculator UX

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 03-calculator*
*Context gathered: 2026-04-02 via auto mode*
