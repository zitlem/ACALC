# Phase 3: Calculator - Research

**Researched:** 2026-04-02
**Domain:** Android Jetpack Compose calculator UI — ViewModel, StateFlow, Material 3 button grid
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Standard calculator keypad layout: digits 0-9, operators +, -, x, /, decimal point, equals, C (clear), backspace, and % key
- **D-02:** Expression display at top shows the full expression being built (CALC-05); result appears after pressing =
- **D-03:** Calculator buttons use Material 3 styling with distinct colors for digits vs operators vs action keys
- **D-04:** Building an expression string that gets evaluated by the existing ExpressionEvaluator from Phase 1
- **D-05:** Double-tapping decimal point does not insert a second decimal in the current number
- **D-06:** C clears all input, backspace deletes last character
- **D-07:** Operator chaining allowed (e.g., 2 + 3 * 4 evaluates with correct precedence)
- **D-08:** Large numbers display with thousands separators (CALC-07)
- **D-09:** Division by zero shows "Error" message, not a crash (CALC-06)
- **D-10:** Result displays without trailing zeros (e.g., "4" not "4.0" for whole number results)
- **D-11:** CalculatorViewModel holds expression state as StateFlow, CalculatorScreen observes via collectAsStateWithLifecycle()
- **D-12:** ViewModel uses ExpressionEvaluator.evaluate() from Phase 1 domain layer — no new parsing logic

### Claude's Discretion

- Exact button sizes and spacing
- Color choices for operator vs digit vs action keys within Material 3 palette
- Animation/transition when showing results
- Whether to show secondary result line while typing

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CALC-01 | User can perform basic arithmetic (+, -, x, /) | ViewModel expression string + ExpressionEvaluator.evaluate(); x-to-* substitution before calling evaluate() |
| CALC-02 | User can input decimal numbers | Decimal guard logic in ViewModel.onDecimal(): scan current token for existing dot |
| CALC-03 | User can clear all input (C) and backspace last character | ViewModel.onClear() resets state; ViewModel.onBackspace() drops last char from expression |
| CALC-04 | User can calculate percentages | ViewModel.onPercent() appends "/100" to current number token before evaluation |
| CALC-05 | User can see the full expression while typing (not just last number) | CalculatorState.expression displayed in top Text; result shown separately only after = |
| CALC-06 | User sees readable error for division by zero | ExpressionEvaluator returns null for division-by-zero and all errors; ViewModel maps null -> "Error" display string |
| CALC-07 | Large numbers display with thousands separators | NumberFormat.getNumberInstance(Locale.getDefault()) or String.format with grouping on result formatting |
</phase_requirements>

---

## Summary

Phase 3 replaces the `CalculatorScreen` placeholder with a fully functional calculator. The domain layer (`ExpressionEvaluator`) is already built and tested — the work is entirely UI + ViewModel. The ViewModel holds a mutable expression string, emits it as `StateFlow<CalculatorState>`, and `CalculatorScreen` renders a button grid and display area driven by that state.

The critical design is the **expression string model**: button presses append/modify a `String` (e.g., `"12+3*"`) which is displayed directly. On `=`, the string is sanitized (remove trailing operator, replace `x` with `*`) and passed to `ExpressionEvaluator.evaluate()`. The result is formatted as a display string and held in a separate state field. This is simpler and more correct than maintaining a token list.

The display follows a two-line model: line 1 shows the expression being built; line 2 shows the last result (blank while typing, populated after `=`). This matches ClevCalc's visual pattern and satisfies CALC-05 directly.

**Primary recommendation:** Single `CalculatorViewModel` with a `CalculatorState` data class; pure expression-string model; `CalculatorScreen` is a stateless composable receiving state + lambda callbacks; button grid built with `LazyVerticalGrid` or a manual `Column`/`Row` layout.

---

## Standard Stack

All dependencies are already present in `app/build.gradle.kts`. No new dependencies needed for this phase.

### Core (already in build.gradle.kts)

| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| `lifecycle-viewmodel-compose` | 2.10.0 | `viewModel()` composable, ViewModel survival across recompositions | CLAUDE.md directive; already declared |
| `lifecycle-runtime-compose` | 2.10.0 | `collectAsStateWithLifecycle()` | Lifecycle-safe StateFlow collection; already declared |
| `compose-material3` | 1.4.0 (via BOM) | `Button`, `FilledTonalButton`, `OutlinedButton`, `Text`, `Surface`, color scheme | Already declared |
| `compose-foundation` | via BOM | `LazyVerticalGrid`, `GridCells`, layout primitives | Already declared |
| `compose-ui` | via BOM | `Modifier`, `fillMaxSize`, `padding`, `Box`, `Column`, `Row` | Already declared |

### No New Dependencies Required

Zero new libraries for this phase. The expression evaluator, ViewModel bridge, lifecycle-aware collection, and Material 3 components are all present.

---

## Architecture Patterns

### Recommended Project Structure (additions for this phase)

```
app/src/main/kotlin/com/acalc/
├── ui/
│   ├── screens/
│   │   └── CalculatorScreen.kt       # Replace placeholder — stateless composable
│   └── viewmodel/
│       └── CalculatorViewModel.kt    # New — holds CalculatorState as StateFlow
└── domain/
    └── ExpressionEvaluator.kt        # Existing — do not modify
```

Note: No `viewmodel/` package exists yet. Either create it or put `CalculatorViewModel` directly in `com.acalc.ui` — the latter is simpler for a two-ViewModel project.

### Pattern 1: CalculatorState Data Class

All UI-visible state in one immutable data class. ViewModel holds `MutableStateFlow<CalculatorState>`.

```kotlin
data class CalculatorState(
    val expression: String = "",        // raw expression string shown in display
    val result: String = "",            // formatted result shown after =; "" while typing
    val isError: Boolean = false        // drives error color on result line
)
```

The `expression` field is exactly what the user sees while typing. `result` is shown only after `=` is pressed (or can be shown live as a preview — Claude's discretion).

### Pattern 2: ViewModel Expression-String Model

The ViewModel owns a single `String` representing the expression under construction. Each button press mutates this string through a sealed operation.

```kotlin
class CalculatorViewModel : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    private val evaluator = ExpressionEvaluator()

    fun onDigit(digit: String) { /* append digit */ }
    fun onOperator(op: String) { /* append operator, replace trailing operator */ }
    fun onDecimal() { /* append "." only if current token has none */ }
    fun onClear() { _state.value = CalculatorState() }
    fun onBackspace() { /* drop last character from expression */ }
    fun onPercent() { /* append "/100" to current number token */ }
    fun onEquals() { /* evaluate and populate result */ }
}
```

### Pattern 3: CalculatorScreen as Stateless Composable

```kotlin
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val viewModel: CalculatorViewModel = viewModel { CalculatorViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()

    CalculatorContent(
        state = state,
        onDigit = viewModel::onDigit,
        onOperator = viewModel::onOperator,
        onDecimal = viewModel::onDecimal,
        onClear = viewModel::onClear,
        onBackspace = viewModel::onBackspace,
        onPercent = viewModel::onPercent,
        onEquals = viewModel::onEquals,
        modifier = modifier
    )
}
```

Separating `CalculatorContent` (pure composable taking state + lambdas) from `CalculatorScreen` (ViewModel wiring) allows `@Preview` on `CalculatorContent` without a ViewModel.

### Pattern 4: Button Grid Layout

Standard calculator has 5 rows × 4 columns = 20 cells. Use `Column` of `Row`s rather than `LazyVerticalGrid` — the button count is fixed and small, eager layout is correct.

```
Row 1: C    ←    %    ÷
Row 2: 7    8    9    ×
Row 3: 4    5    6    −
Row 4: 1    2    3    +
Row 5: .    0    [0 spans 2? or single]   =
```

Typical layout decision: `0` spans two columns, `=` takes the rightmost column in row 5. Alternatively standard grid without spanning. The non-spanning layout (all cells equal) is simpler in Compose.

Use `weight(1f)` on each button via `RowScope` to fill available width equally.

### Pattern 5: x -> * Substitution Before Evaluation

The UI displays `x` as the multiplication symbol (CONTEXT D-03, ClevCalc UX). ExpressionEvaluator only understands `*`. Apply substitution inside `onEquals()`:

```kotlin
fun onEquals() {
    val sanitized = expression
        .replace("x", "*")
        .trimEnd { it in "+-x*/" }  // remove trailing operator
    val result = evaluator.evaluate(sanitized)
    // ...
}
```

Store `x` internally in the expression string (not `*`) so the display is always clean.

### Pattern 6: Result Formatting

```kotlin
fun formatResult(value: Double): String {
    return if (value == kotlin.math.floor(value) && !value.isInfinite()) {
        // Whole number — no decimal point, with grouping
        NumberFormat.getIntegerInstance().format(value.toLong())
    } else {
        // Decimal — limit to reasonable precision, with grouping
        NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 10
            isGroupingUsed = true
        }.format(value)
    }
}
```

`NumberFormat.getIntegerInstance()` uses the device locale for thousands separator — satisfies CALC-07 without manual string manipulation.

### Anti-Patterns to Avoid

- **Storing expression as token list:** Complex to manage, unnecessary. A plain String is correct for this scope.
- **Calling ExpressionEvaluator on every keystroke for live preview:** May seem smart but produces confusing display on partial input (e.g., `"12+"` evaluates to null, flickers). Show live preview only if the expression evaluates cleanly; otherwise show blank result line. Alternatively, skip live preview entirely and only evaluate on `=` (simpler, matches the locked decision D-02).
- **Using LiveData:** CLAUDE.md forbids it. StateFlow only.
- **Putting ViewModel construction in AppShell:** ViewModels for screens should be created at the screen composable level, not hoisted to AppShell.
- **Mixing display symbol and evaluator symbol in internal state:** Store `x` throughout — never mix `x` and `*` in the same expression string. Substitute only at evaluation time.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Thousands separators | Manual string splitting | `NumberFormat.getIntegerInstance()` / `NumberFormat.getNumberInstance()` | Handles locale-specific separators (comma vs period), negative numbers, edge cases |
| Expression evaluation | New parser | `ExpressionEvaluator.evaluate()` from Phase 1 | Already exists, tested, handles precedence, unary minus, division-by-zero |
| StateFlow ↔ Compose bridge | Manual `LaunchedEffect` + `remember` | `collectAsStateWithLifecycle()` from lifecycle-runtime-compose | Lifecycle-safe; already a project dependency |
| ViewModel instantiation | Manual constructor in composable | `viewModel { CalculatorViewModel() }` | Survives configuration changes; pattern established in CLAUDE.md |

---

## Common Pitfalls

### Pitfall 1: Double-Decimal Guard Scope

**What goes wrong:** Checking if `expression` contains a dot instead of checking if the *current token* contains a dot. `"1.5+3"` has a dot, but `"3"` at the end should still accept a decimal.

**Why it happens:** Simple string search (`expression.contains(".")`) is wrong. The guard must scan the expression string backwards from the end to find the start of the current number token, then check only that token.

**How to avoid:** In `onDecimal()`, find the last occurrence of any operator (`+`, `-`, `x`, `/`) in `expression`. Everything after that position is the current token. If the current token already contains `.`, do nothing.

**Warning signs:** User types `1.5+3` then taps `.` and gets `1.5+3.` — correct. User types `1.5+3.` then taps `.` again and gets `1.5+3..` — this is the bug.

### Pitfall 2: Trailing Operator Before Evaluation

**What goes wrong:** User types `12+` then presses `=`. Expression `"12+"` is passed to ExpressionEvaluator, which returns null, and "Error" is shown.

**Why it happens:** No sanitization before evaluation.

**How to avoid:** In `onEquals()`, trim trailing operators (`+`, `-`, `x`, `/`) from the expression string before passing to the evaluator. `"12+"` becomes `"12"` and evaluates to `12`.

**Warning signs:** Pressing `=` immediately after an operator shows "Error" instead of the last valid number.

### Pitfall 3: Operator Chaining — Replace Last Operator

**What goes wrong:** User types `5`, `+`, `+` and gets expression `"5++"`. The evaluator returns null.

**Why it happens:** `onOperator()` blindly appends without checking if the last character is already an operator.

**How to avoid:** In `onOperator()`, if the last character of `expression` is an operator, replace it instead of appending. This gives the ClevCalc behavior of "change operator" if you mis-tap.

**Warning signs:** Double-tapping an operator key gives double operator in display.

### Pitfall 4: Starting Expression With an Operator

**What goes wrong:** User's first tap is `+` — expression becomes `"+"`. Evaluator returns null when `=` is pressed.

**How to avoid:** In `onOperator()`, if `expression` is empty (or contains only operators), ignore the tap except for `-` (which can begin a negative number as unary minus).

### Pitfall 5: Result State After Equals

**What goes wrong:** After pressing `=`, if the user presses a digit, behavior is ambiguous — does it append to the result or start a new expression?

**Why it happens:** The ViewModel doesn't track "are we in result mode?"

**How to avoid:** Add a `resultShown: Boolean` flag to `CalculatorState` (or as a separate private field in the ViewModel). When `resultShown == true`:
- Pressing a digit starts a fresh expression (clear expression, set to digit)
- Pressing an operator continues from the result (set expression to result + operator)

This is the standard calculator UX convention.

### Pitfall 6: Formatting Whole Numbers

**What goes wrong:** `4.0` is displayed instead of `4` for integer results.

**Why it happens:** Calling `.toString()` on a `Double` always includes decimal notation.

**How to avoid:** Check `value == floor(value)` before formatting. For integers, use `toLong()` before `NumberFormat.getIntegerInstance().format()`. (Covered in Pattern 6 above.)

### Pitfall 7: Negative Number Display After Backspace

**What goes wrong:** Expression is `"-5"`, user presses backspace, gets `"-"`. This is a malformed expression shown in display and causes null on evaluation.

**How to avoid:** In `onBackspace()`, after dropping the last character, check if the result is just `"-"` and clear to `""` instead.

---

## Code Examples

### CalculatorState

```kotlin
// Internal to CalculatorViewModel.kt
data class CalculatorState(
    val expression: String = "",
    val result: String = "",
    val isError: Boolean = false,
    val resultShown: Boolean = false
)
```

### Decimal Guard

```kotlin
private fun currentTokenHasDecimal(): Boolean {
    val lastOpIndex = expression.indexOfLast { it in "+-x/" }
    val currentToken = if (lastOpIndex == -1) expression else expression.substring(lastOpIndex + 1)
    return currentToken.contains(".")
}

fun onDecimal() {
    if (!currentTokenHasDecimal()) {
        val prefix = if (expression.isEmpty() || expression.last() in "+-x/") "0" else ""
        expression += "${prefix}."
        _state.value = _state.value.copy(expression = expression, resultShown = false)
    }
}
```

### onEquals

```kotlin
fun onEquals() {
    if (expression.isEmpty()) return
    val sanitized = expression
        .trimEnd { it in "+-x/" }
        .replace("x", "*")
    val value = evaluator.evaluate(sanitized)
    if (value == null) {
        _state.value = _state.value.copy(result = "Error", isError = true, resultShown = true)
    } else {
        val formatted = formatResult(value)
        expression = formatted.replace(",", "").replace(" ", "") // store unformatted for continued ops
        _state.value = _state.value.copy(
            result = formatted,
            isError = false,
            resultShown = true
        )
    }
}
```

### Button Color Assignment (Material 3)

Material 3 on Android provides `MaterialTheme.colorScheme`. Recommended button roles:

| Key Category | Composable | Color Token |
|---|---|---|
| Digits (0-9, .) | `FilledTonalButton` | `secondaryContainer` / `onSecondaryContainer` |
| Operators (+, -, x, /) | `Button` (filled) | `primary` / `onPrimary` |
| Action (C, ←, %) | `OutlinedButton` | outline style |
| Equals (=) | `Button` | `tertiary` / `onTertiary` (or `primary`) |

`FilledTonalButton` is the Material 3 component that renders with `secondaryContainer` background by default — it naturally provides visual differentiation without custom colors.

### Preview-Friendly Composable Split

```kotlin
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val viewModel: CalculatorViewModel = viewModel { CalculatorViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalculatorContent(state = state, onDigit = viewModel::onDigit, /* ... */, modifier = modifier)
}

@Preview
@Composable
private fun CalculatorContentPreview() {
    AcalcTheme {
        CalculatorContent(
            state = CalculatorState(expression = "12+34", result = "46"),
            onDigit = {}, onOperator = {}, onDecimal = {}, onClear = {},
            onBackspace = {}, onPercent = {}, onEquals = {}
        )
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ViewModel + LiveData | ViewModel + StateFlow + `collectAsStateWithLifecycle()` | Android 2022+ / lifecycle 2.6+ | No LiveData anywhere in this project; already established in CLAUDE.md |
| `viewModel()` without factory lambda | `viewModel { MyViewModel() }` (factory lambda) | lifecycle-viewmodel-compose 2.5+ | No Hilt needed for simple ViewModels; already used by the project convention |
| Groovy DSL build scripts | Kotlin DSL (.kts) | Gradle 8.2 default | Already using Kotlin DSL |

---

## Open Questions

1. **Live result preview while typing**
   - What we know: D-02 says "result appears after pressing =" — so no live preview required
   - What's unclear: Whether a subtle secondary line showing the current result (if expression is valid) would improve UX (Claude's discretion)
   - Recommendation: Implement the required behavior (result on `=` only) first; adding a live preview secondary line is a single additional `collectAsStateWithLifecycle` binding with no architectural change

2. **Percent key behavior**
   - What we know: CALC-04 requires percent; CONTEXT says `%` key works correctly
   - What's unclear: Exact semantic — does `50%` = `0.5`, or does `100+10%` = `110` (percentage-of-base behavior)?
   - Recommendation: Implement `%` as `÷ 100` appended to the current token (i.e., `50%` evaluates as `50/100` = `0.5`). This is the simplest consistent behavior and avoids context-aware percentage logic. Simpler than context-aware (which would require tracking the "base" operand).

3. **Button layout for row 5 / zero key**
   - What we know: Keys are `.`, `0`, `=` — that's 3 keys for 4 columns
   - What's unclear: Whether `0` spans 2 columns (like a physical calculator) or whether there's a 4th key in that row
   - Recommendation: Use a non-spanning 4-cell final row: `(` or blank | `.` | `0` | `=`. Or simply have `0` span 2 columns using a `weight(2f)` modifier. Either is Claude's discretion.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 3 is purely code changes (Kotlin/Compose ViewModel and UI). No external tools, services, CLIs, databases, or runtimes beyond the existing Android build toolchain, which was verified operational in Phase 1 and Phase 2.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (junit:junit:4.13.2) |
| Config file | None — standard Android test runner auto-discovers |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.acalc.ui.viewmodel.*"` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CALC-01 | Basic arithmetic evaluates correctly via ViewModel | unit | `./gradlew :app:testDebugUnitTest --tests "com.acalc.ui.viewmodel.CalculatorViewModelTest"` | Wave 0 |
| CALC-02 | Decimal input; double-decimal guard | unit | same | Wave 0 |
| CALC-03 | C clears state; backspace removes last char | unit | same | Wave 0 |
| CALC-04 | Percent key appends /100 and evaluates | unit | same | Wave 0 |
| CALC-05 | Expression string updated on each keypress | unit | same | Wave 0 |
| CALC-06 | Division by zero produces "Error" string in state | unit | same | Wave 0 |
| CALC-07 | Result formatted with thousands separators | unit | same | Wave 0 |

All tests target `CalculatorViewModel` directly (pure JVM, no Android instrumentation needed).

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.acalc.ui.viewmodel.CalculatorViewModelTest"`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/kotlin/com/acalc/ui/viewmodel/CalculatorViewModelTest.kt` — covers CALC-01 through CALC-07

*(Existing test infrastructure: `ExpressionEvaluatorTest.kt` and `ConversionEngineTest.kt` already present and passing. Test runner is confirmed working from Phase 1.)*

---

## Project Constraints (from CLAUDE.md)

| Directive | Impact on This Phase |
|-----------|---------------------|
| Kotlin + Jetpack Compose only; no XML Views | CalculatorScreen is a Compose-only composable |
| Material 3 / Material You theming | Button colors use `MaterialTheme.colorScheme`; wrap in `AcalcTheme` for previews |
| ViewModel + StateFlow; no LiveData | `CalculatorViewModel` uses `MutableStateFlow<CalculatorState>` exclusively |
| `collectAsStateWithLifecycle()` for Flow collection | Used in `CalculatorScreen` to observe ViewModel state |
| No Hilt/Dagger; manual ViewModel construction | `viewModel { CalculatorViewModel() }` in screen composable |
| Custom recursive-descent parser (no mXparser) | Already done in Phase 1; reuse `ExpressionEvaluator` directly |
| No Room/SQLite (no persistence in v1) | No history storage; state is in-memory only |
| No network calls | N/A for this phase |
| Package structure: `com.acalc.ui.screens` for screens | `CalculatorScreen.kt` stays in `com.acalc.ui.screens`; ViewModel in `com.acalc.ui.viewmodel` or `com.acalc.ui` |

---

## Sources

### Primary (HIGH confidence)

- Codebase: `app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt` — verified API: `evaluate(String): Double?`, returns null on all errors including division by zero, handles +, -, *, /, parentheses, unary minus
- Codebase: `app/src/test/kotlin/com/acalc/domain/ExpressionEvaluatorTest.kt` — verified test contract: `"5/0"` → null, `"2+3*4"` → 14.0 (precedence correct)
- Codebase: `app/build.gradle.kts` + `gradle/libs.versions.toml` — verified all required dependencies (lifecycle-viewmodel-compose 2.10.0, lifecycle-runtime-compose 2.10.0, compose-material3 via BOM 2026.03.01) are already declared
- Codebase: `app/src/main/kotlin/com/acalc/ui/AppShell.kt` — verified integration point: `CalculatorScreen(modifier = Modifier.padding(innerPadding))` call signature
- `CLAUDE.md` — authoritative technology stack, ViewModel + StateFlow pattern, forbidden libraries

### Secondary (MEDIUM confidence)

- Standard Android calculator UX convention (ClevCalc-inspired per CONTEXT.md): expression-string display model, result-on-equals, operator-replacement behavior — widely established pattern in calculator apps

---

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — all dependencies verified present in build files; no new libraries needed
- Architecture: HIGH — ViewModel + StateFlow pattern established in CLAUDE.md and verified via existing codebase conventions
- Pitfalls: HIGH — derived from direct analysis of ExpressionEvaluator API contract and standard calculator input handling edge cases
- Test strategy: HIGH — JUnit 4 confirmed working from Phase 1; CalculatorViewModel is pure JVM logic, no Android instrumentation needed

**Research date:** 2026-04-02
**Valid until:** 2026-05-02 (stable domain — Compose + ViewModel APIs are not changing rapidly)
