# Domain Pitfalls

**Domain:** Android calculator + unit converter (Kotlin + Jetpack Compose)
**Researched:** 2026-04-01
**Confidence:** HIGH for floating-point and architecture pitfalls; MEDIUM for Compose-specific edge cases

---

## Critical Pitfalls

Mistakes that cause incorrect results, data loss, or forced rewrites.

---

### Pitfall 1: Using Double/Float Arithmetic Directly for Conversion Results

**What goes wrong:** The standard `Double` type in Kotlin (IEEE 754 double-precision) cannot represent many decimal fractions exactly in binary. A round-trip conversion — e.g., converting 25 mm to inches then back — can silently produce `25.000000000000004` instead of `25.0`. This surfaces as wrong digits in the display even though the logic looks correct.

**Why it happens:** `0.1`, `1.8`, and many other short decimals are non-terminating in binary. When you multiply or divide by them, the rounding error compounds. The inverse conversion factor 1/25.4 (= 5/127) is a repeating decimal in both binary and decimal, amplifying drift.

**Consequences:** Users see unexpected trailing digits in the unit converter output (e.g., `0.9999999...` or `25.000000000000004`). Trust in the app evaporates immediately when converting common everyday values.

**Prevention:**
- Store and compute all conversion values using `BigDecimal` initialized from `String` literals, not from `Double` literals. `BigDecimal("25.4")` is safe; `BigDecimal(25.4)` is not — it inherits the Double's imprecision.
- Use `BigDecimal.divide(divisor, scale, RoundingMode.HALF_UP)` with explicit scale. Never call `.divide()` without a rounding mode — it throws `ArithmeticException` for non-terminating results.
- Round only at the final display step, not during intermediate computation.
- Define all conversion factors as `BigDecimal` constants in a `ConversionConstants.kt` file so they are never re-created inline.

**Detection (warning signs):**
- Round-trip conversion test for any unit pair produces a result that differs from the original by more than 1 ULP of display precision.
- Unit test: `mmToInches(25.4).toString()` returns anything other than `"1"` (or `"1.0"` depending on display scale).

**Phase to address:** Core conversion engine (before any UI is built). Fix this at the model layer — retrofitting BigDecimal after the UI is wired is painful.

---

### Pitfall 2: Chained Intermediate Conversions (Converting Through a Base Unit)

**What goes wrong:** A common architecture converts everything to a single base unit (e.g., meters for length) and then to the target unit. If intermediate `Double` values are used at any hop — `value → meters → target` — two floating-point rounding errors accumulate rather than one.

**Example:** `feet → meters → cm` introduces two multiplications each with their own rounding error. Direct `feet → cm` introduces one.

**Why it happens:** Developers choose a hub-and-spoke model for simplicity (N units need N factors, not N² factors). The architecture is correct; the precision handling within it is the problem.

**Consequences:** Conversions involving obscure pairs (e.g., yards to millimeters) drift more than direct-pair conversions, producing inconsistent precision across units.

**Prevention:**
- Keep the hub-and-spoke architecture (it is correct and maintainable) but ensure the base unit value is `BigDecimal` at every hop.
- Define factors as `BigDecimal` constants. The two-hop precision loss disappears when both factors are exact `BigDecimal` representations.
- Write a test matrix: for every unit pair in each category, verify round-trip to within display precision.

**Detection:** Conversion result for `1 foot → mm → foot` deviates from `1.0` by more than your display rounding threshold.

**Phase to address:** Unit conversion engine foundation. Do not proceed to the live-display UI until a full test matrix passes.

---

### Pitfall 3: Expression Parser Integer Division

**What goes wrong:** The app allows math expressions in unit input fields (e.g., `25.4 + 10`). If the expression evaluator uses integer arithmetic internally — or if the Kotlin division operator `/` is applied to two `Int` values — `5 / 2` evaluates to `2` instead of `2.5`.

**Why it happens:** Kotlin's `/` operator performs integer division when both operands are `Int`. A naive parser that tokenizes literals as `Int` will silently truncate fractions. This is a silent bug: no exception, wrong answer.

**Consequences:** Expressions like `3/4 in` evaluate to `0 in` — completely wrong. The user types a valid expression and gets a nonsensical conversion result with no error message.

**Prevention:**
- Tokenize all numeric literals as `Double` or `BigDecimal` in the lexer, never as `Int`.
- Add explicit test cases: `evaluate("1/4")` must equal `0.25`, not `0`.
- If using a library (e.g., ExprK, mXparser), verify the library handles integer literals as floating-point by default.

**Detection:** `evaluate("3/2")` returns `1` instead of `1.5`.

**Phase to address:** Expression evaluator implementation (before wiring to the unit input fields).

---

### Pitfall 4: Infinite Recomposition Loop From Bidirectional State Updates

**What goes wrong:** The unit converter has two input fields that update each other in real-time (live dual display). A naive implementation where Field A's `onValueChange` writes to a `MutableState` that Field B observes, and Field B's `onValueChange` writes back, creates a circular state update. Each write triggers a recomposition which triggers the other write, looping until a frame is dropped or the app hangs.

**Why it happens:** Compose recomposition is synchronous within a frame. If state A's change handler sets state B, and state B is read by the composable that also reads state A, the dependency chain can re-trigger itself.

**Consequences:** App becomes unresponsive when a user types in either field. In worst cases, it crashes with a stack overflow. In mild cases, typing lags noticeably.

**Prevention:**
- Maintain a single source of truth: one `activeField` enum in the ViewModel and one `rawInput: String` state. The ViewModel computes the other field's value as a derived computation, not as a separate mutable state that feeds back.
- Use `derivedStateOf` in the composable layer to prevent recompositions when only unrelated state changes.
- Never write to state inside Composition (in `@Composable` function body). All state writes must happen in event handlers (lambdas, not during composition).
- The ViewModel pattern: user edits field A → ViewModel updates `fieldAInput` → ViewModel derives `fieldBDisplay` → UI reads both. Field B display is read-only from UI's perspective.

**Detection:** Typing a single character causes the cursor to jump, the display to flicker, or the app to stutter. Enabling Compose Layout Inspector shows excessive recomposition counts.

**Phase to address:** Live dual-display feature (phase where bidirectional conversion UI is wired up).

---

### Pitfall 5: Decimal Separator Locale Mismatch

**What goes wrong:** On Android devices configured to a locale that uses a comma as the decimal separator (e.g., German, French, many European locales), `KeyboardType.Decimal` may produce `,` instead of `.` in the TextField. If the expression evaluator or `BigDecimal` parser only accepts `.` as a decimal separator, input like `3,14` fails silently or throws a parse exception displayed as `Error`.

**Why it happens:** `KeyboardType.Number` and `KeyboardType.Decimal` are hints to the IME, not enforcement mechanisms. Individual IMEs on various devices handle locale differently. Filtering in `onValueChange` to reject commas breaks input on those devices.

**Consequences:** The app is unusable on devices with non-English locales. Since this is a personal-use app, this may not matter initially — but it is still a silent crash risk if the parse is not guarded.

**Prevention:**
- Normalize the decimal separator before parsing: replace `,` with `.` using `input.replace(',', '.')` before feeding to the evaluator.
- Alternatively, use `DecimalFormatSymbols(Locale.getDefault()).decimalSeparator` to detect the device locale and accept whichever separator it uses.
- Never use `onValueChange` filtering to block commas outright — this causes cursor and IME state desynchronization on some keyboards.
- Wrap `BigDecimal(String)` and expression parsing calls in `try/catch`; display a user-visible error instead of a crash.

**Detection:** Test on a device or emulator with locale set to `de-DE` or `fr-FR`. Type a decimal number; observe whether it parses correctly.

**Phase to address:** Input handling / expression evaluator wiring.

---

## Moderate Pitfalls

---

### Pitfall 6: Temperature Conversion Not Using Offset-Aware Formula

**What goes wrong:** Temperature conversions are not linear scaling — they require an additive offset. Celsius to Fahrenheit is `(C × 9/5) + 32`. If a developer implements temperature in the same hub-and-spoke conversion system as linear units (e.g., multiply by a factor to get to a "base"), they must choose a base unit (Kelvin) and implement the correct affine formula for each conversion, not a simple multiplicative factor.

**Common bug:** Using `factor × value` for temperature when the formula requires `factor × (value − offset)`. For example, treating Fahrenheit as `value × 0.5556` instead of `(value − 32) × 0.5556` produces completely wrong results.

**Consequences:** `32°F` converts to `17.78°C` instead of `0°C`. The error is not subtle.

**Prevention:**
- Temperature must be handled as a special case with two parameters per conversion: a multiplier and an offset.
- Define a `TemperatureUnit` sealed class with `toKelvin(value: BigDecimal): BigDecimal` and `fromKelvin(value: BigDecimal): BigDecimal` methods on each variant.
- Do not attempt to fit temperature into a generic multiplicative conversion table.

**Detection:** `convert(32, F, C)` must return `0`. `convert(0, C, K)` must return `273.15`. Add these as unit tests before any UI work.

**Phase to address:** Unit conversion engine (at the same time as linear conversions).

---

### Pitfall 7: Expression Evaluator Not Handling Operator Precedence Correctly

**What goes wrong:** A naive left-to-right expression evaluator processes `2 + 3 * 4` as `(2 + 3) * 4 = 20` instead of the correct `2 + (3 * 4) = 14`. This is a fundamental expression parsing error that users will catch immediately.

**Prevention:**
- Do not write a naive left-to-right evaluator.
- Use a proper recursive descent parser or the Shunting-Yard algorithm, both of which naturally encode operator precedence.
- Alternatively, use a battle-tested library: `ExprK` (Kotlin-native, minimal dependency) or `mXparser` (feature-rich, larger footprint). For a personal-use app with no size constraints, either is fine.
- Test: `evaluate("2+3*4")` == `14`, `evaluate("10-2-3")` == `5` (left-associative subtraction), `evaluate("8/4/2")` == `1`.

**Detection:** Test `2 + 3 * 4` in the expression input field immediately after wiring it up.

**Phase to address:** Expression evaluator (standalone, testable, before UI integration).

---

### Pitfall 8: BigDecimal Display Showing Trailing Zeros or Scientific Notation

**What goes wrong:** `BigDecimal` preserves scale, so `BigDecimal("1.0000") / BigDecimal("1")` can return `1.0000` or `1E+0` depending on the operation. Displaying a `BigDecimal` directly via `.toString()` produces output like `1.000000000000000` or `2.54E+1` that looks wrong in a calculator UI.

**Consequences:** The display shows `25.4000000000` instead of `25.4`, or `2.54E+1` instead of `25.4`. Users see this as a bug.

**Prevention:**
- Use `BigDecimal.stripTrailingZeros().toPlainString()` for display rendering.
- Cap display to a maximum of 10–12 significant figures using `setScale(N, RoundingMode.HALF_UP)` before stripping zeros.
- Test: converting `1 inch → mm` shows `25.4`, not `25.4000000` or `2.54E+1`.

**Detection:** Check the display output for any conversion that produces a round or well-known result (e.g., `1 inch = 25.4 mm`, `0°C = 32°F`).

**Phase to address:** Display formatting layer (before user testing any conversion values).

---

### Pitfall 9: TextField IME Filtering via onValueChange Causing Cursor Glitches

**What goes wrong:** It is tempting to filter input characters in `onValueChange` to block invalid input (e.g., prevent two decimal points, block letters). However, filtering by not passing invalid characters back to state desynchronizes the Compose `TextField` state from the IME's internal state. This causes cursor jumps, duplicated characters, and unpredictable behavior on some keyboards.

**Why it happens:** The IME (soft keyboard) maintains its own internal text state. If Compose's state diverges from it, the IME tries to reconcile the mismatch, producing erratic cursor behavior.

**Prevention:**
- Use `TextFieldState` (the newer API, stable as of Compose 1.7 / August 2025) with `InputTransformation` for filtering. This integrates at the lower-level text editing layer and avoids IME desynchronization.
- If staying with the legacy `value`/`onValueChange` API, be extremely conservative about what you reject — only reject characters that you can guarantee will not confuse the IME (e.g., reject only clearly invalid characters, never silently drop partial input).
- Show validation errors in the UI rather than silently blocking input.

**Detection:** Type rapidly in the input field with filtering enabled. Observe if the cursor jumps or characters double-up.

**Phase to address:** UI layer, when implementing calculator and unit input fields.

---

## Minor Pitfalls

---

### Pitfall 10: Forgetting Division by Zero and Empty-Expression Display States

**What goes wrong:** Integer `divide` throws `ArithmeticException`; `Double` division returns `Infinity` or `NaN` silently; `BigDecimal.divide` without a scale on non-terminating results throws `ArithmeticException`. Any of these unguarded can surface as an unhandled exception that crashes the app, or as `Infinity` or `NaN` displayed in the result field.

**Prevention:**
- Wrap all expression evaluation in a `try/catch`. On exception, display `"Error"` in the result field.
- Before calling `.divide()`, check if the divisor is zero: `if (divisor.compareTo(BigDecimal.ZERO) == 0) return ErrorResult`.
- Test: entering `5/0` should show `"Error"`, not crash.
- Test: empty field should show nothing, not `"Error"` or `"0"`.

**Phase to address:** Expression evaluator and display logic.

---

### Pitfall 11: Hardcoding Conversion Factor Precision Inadequate for Derived Units

**What goes wrong:** Area conversions use squared linear factors. If the linear factor for, say, feet to meters is stored as `0.3048` (exact by definition), then square feet to square meters requires `0.3048² = 0.09290304`. If the area factor is independently hardcoded as `0.0929` (a truncated approximation), the error is amplified for large values.

**Prevention:**
- Derive area conversion factors programmatically from squared linear factors rather than hardcoding them separately.
- For speed conversions that mix units (e.g., knots = nautical miles per hour), derive the factor from the underlying distance/time unit factors rather than hardcoding independently.

**Detection:** `1 sq foot → sq meters → sq feet` round-trip deviates from `1` more than display tolerance.

**Phase to address:** Unit conversion constants definition.

---

### Pitfall 12: No Debounce on Live Conversion Causing Lag on Low-End Devices

**What goes wrong:** Every keystroke triggers a `BigDecimal` parse and conversion computation synchronously on the main thread. For simple conversions this is fine. For expression evaluation (which involves parsing), this can cause frame drops on low-end devices if the expression grows long.

**Prevention:**
- Keep the ViewModel's conversion logic on the main thread for now (BigDecimal arithmetic for simple values is fast enough on modern Android).
- If expression evaluation is added, apply `debounce(50ms)` in the StateFlow pipeline inside the ViewModel using `.debounce(50L)` on the input Flow.
- Use `distinctUntilChanged()` to avoid reprocessing unchanged values.
- Never do conversion computation inside a `@Composable` function body — always in the ViewModel.

**Detection:** Profile with Android Studio's CPU profiler while typing rapidly in the converter field. Watch for frame time spikes exceeding 16ms.

**Phase to address:** ViewModel wiring for live conversion.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|---|---|---|
| Conversion constant definitions | Pitfall 1: Double precision loss in factors | Use `BigDecimal(String)` constants from day one |
| Temperature unit implementation | Pitfall 6: Treating temperature as linear scaling | Use affine formula with explicit offset |
| Area/speed unit implementation | Pitfall 11: Independently hardcoded derived factors | Derive from squared/composed base factors |
| Expression evaluator (math in input fields) | Pitfall 3: Integer division; Pitfall 7: Precedence | Use Shunting-Yard or ExprK; test `1/4` and `2+3*4` |
| Live dual-display UI | Pitfall 4: Circular state update loop | Single source of truth in ViewModel; derived display only |
| TextField input implementation | Pitfall 5: Locale comma/period; Pitfall 9: IME cursor glitches | Normalize separators; use `InputTransformation` API |
| Result display rendering | Pitfall 8: BigDecimal scientific notation / trailing zeros | Use `stripTrailingZeros().toPlainString()` |
| Division and empty states | Pitfall 10: Division by zero crash | Wrap evaluator in try/catch; check divisor before divide |
| Performance on live updates | Pitfall 12: Synchronous parsing on main thread | `debounce(50ms)` + `distinctUntilChanged()` in ViewModel |
| Chain conversions (all unit categories) | Pitfall 2: Two-hop floating-point error accumulation | `BigDecimal` at every hop; test full round-trip matrix |

---

## Sources

- [Fixing floating-point arithmetics with Kotlin — Nicolas Frankel](https://blog.frankel.ch/fixing-floating-point-arithmetics-with-kotlin/)
- [BigDecimal for High-Precision Arithmetic in Kotlin — Sling Academy](https://www.slingacademy.com/article/using-bigdecimal-for-high-precision-arithmetic-kotlin/)
- [Android BigDecimal API Reference](https://developer.android.com/reference/kotlin/android/icu/math/BigDecimal)
- [Floating-point for decimals — Roman Elizarov / Medium](https://elizarov.medium.com/floating-point-for-decimals-fc2861898455)
- [Jetpack Compose Performance — Android Developers](https://developer.android.com/develop/ui/compose/performance)
- [Compose TextFieldState Performance Revolution — Medium](https://medium.com/@sivavishnu0705/why-recompose-on-every-keystroke-the-performance-revolution-of-textfieldstate-1a2c7ae20800)
- [Overcoming Common Performance Pitfalls in Jetpack Compose — ProAndroidDev](https://proandroiddev.com/overcoming-common-performance-pitfalls-in-jetpack-compose-98e6b155fbb4)
- [Effective State Management for TextField in Compose — Android Developers / Medium](https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5)
- [Migrate to State-Based Text Fields — Android Developers](https://developer.android.com/develop/ui/compose/text/migrate-state-based)
- [Where to Hoist State — Android Developers](https://developer.android.com/develop/ui/compose/state-hoisting)
- [Gotchas in Jetpack Compose Recomposition — Stitch Fix Engineering](https://multithreaded.stitchfix.com/blog/2022/08/05/jetpack-compose-recomposition/)
- [TextField KeyboardType.Number decimal issue — Google Issue Tracker](https://issuetracker.google.com/issues/209835363)
- [Decimal Input Formatting with VisualTransformation — DEV Community](https://dev.to/tuvakov/decimal-input-formatting-with-jetpack-composes-visualtransformation-110n)
- [Common Errors in Unit Conversions — WorksheetGenius](https://worksheetgenius.com/blog/common-errors-in-unit-conversions/)
- [Conversion of Units of Measurement — UT Austin / Novak](https://www.cs.utexas.edu/~novak/units95.html)
- [Division by Zero in Java — Baeldung](https://www.baeldung.com/java-division-by-zero)
- [Making Arithmetic Parser with Kotlin — Medium / Coding Blocks](https://medium.com/coding-blocks/making-arithmetic-parser-with-kotlin-4097115f5af)
- [ExprK — Simple mathematical expression evaluator for Kotlin](https://github.com/Keelar/ExprK)
- [Implementing Debouncing with Flow in Jetpack Compose](https://coldfusion-example.blogspot.com/2025/01/implementing-debouncing-with-flow-in.html)
- [Never Use Float and Double for Monetary Calculations — DZone](https://dzone.com/articles/never-use-float-and-double-for-monetary-calculatio)
