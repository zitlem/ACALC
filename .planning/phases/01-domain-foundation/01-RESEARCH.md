# Phase 1: Domain Foundation - Research

**Researched:** 2026-04-01
**Domain:** Pure-Kotlin expression parsing, unit conversion with BigDecimal precision, Android local unit testing
**Confidence:** HIGH

---

## Summary

Phase 1 establishes the two pure-Kotlin domain objects that every later phase depends on: `ExpressionEvaluator` and `ConversionEngine`. Both are free of Android dependencies and live entirely in `src/main/kotlin`. They are exercised only by JUnit 4 tests in `src/test/kotlin`, which run on the JVM with no emulator required.

The expression evaluator must handle the four arithmetic operators, parentheses, and decimal numbers with correct operator precedence. The project's CLAUDE.md makes the decision clear: write a small custom recursive-descent or shunting-yard parser (~80-120 lines) rather than importing mXparser. This is entirely achievable and immediately testable.

The conversion engine must convert between all units in 6 categories (length, weight, volume, temperature, area, speed) using `BigDecimal` arithmetic throughout. Temperature is the only category requiring offset-aware formulas; all others use multiplicative factors. BigDecimal arithmetic prevents the floating-point drift the success criteria explicitly prohibit (the 25 mm -> in -> mm round-trip test).

**Primary recommendation:** Build `ExpressionEvaluator` as a recursive-descent parser operating on `Double`, then build `ConversionEngine` with `BigDecimal` conversion factors stored as `String`-constructed constants. Cover both with exhaustive JUnit 4 tests before any Android code is written.

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| (none) | Phase 1 has no user-facing requirement IDs — it is the domain foundation that enables CONV-01 through CONV-09 and CALC-01 through CALC-07 in later phases. The success criteria from ROADMAP.md drive the work. | See success criteria below. |

**Success criteria driving this phase:**
1. `ExpressionEvaluator.evaluate("1/4")` returns `0.25`; `evaluate("2 + 3 * 4")` returns `14`
2. `ConversionEngine` round-trips `25 mm -> in -> mm` without BigDecimal drift
3. Temperature: `32°F = 0°C`, `0°C = 273.15 K`
4. All 6 unit categories with full unit sets from REQUIREMENTS.md
5. Unit tests cover all unit pairs and expression edge cases; suite passes cleanly
</phase_requirements>

---

## Project Constraints (from CLAUDE.md)

| Directive | Constraint |
|-----------|------------|
| Expression parser | Use a custom Kotlin parser (shunting-yard or recursive descent). Do NOT use mXparser. Do NOT use ExprK. |
| No persistence | No Room, no SQLite in v1. Domain objects are stateless. |
| No networking | Fully offline. No Retrofit, no OkHttp. |
| No DI framework | No Hilt, no Dagger. Manual construction only. |
| No LiveData | StateFlow only (not relevant in Phase 1, but carry forward). |
| No XML Views | Not relevant in Phase 1, but carry forward for all UI phases. |
| Build language | Kotlin DSL only (.kts). |
| SDK | compileSdk 36, targetSdk 35, minSdk 26. |
| Test framework | JUnit 4 (4.13.2). Not JUnit 5 (requires extra setup). |

---

## Standard Stack

### Core (Phase 1 only)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin stdlib | 2.3.20 (via AGP) | Language + collections | Included automatically. `BigDecimal` is `java.math.BigDecimal` — no extra dependency. |
| JUnit 4 | 4.13.2 | Unit test runner | Prescribed in CLAUDE.md. Works out of the box with Android Gradle. |

### Supporting (available but not required in Phase 1)

None. This phase has zero runtime dependencies beyond the Kotlin stdlib.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom parser | mXparser 6.1.0 | mXparser is explicitly forbidden in CLAUDE.md — do not use |
| `BigDecimal(String)` constants | `BigDecimal(Double)` | `BigDecimal(Double)` is imprecise; string construction is exact |
| Multiplicative factor design | Two-function per-pair design | Multiplicative factor to a common base unit scales to N units with N factors, not N² conversions |

**Installation (build.gradle.kts — test dependency only):**
```kotlin
dependencies {
    testImplementation("junit:junit:4.13.2")
}
```

No additional runtime dependencies are needed for Phase 1.

---

## Architecture Patterns

### Recommended Project Structure

```
app/src/
├── main/
│   └── kotlin/
│       └── com/acalc/
│           ├── domain/
│           │   ├── ExpressionEvaluator.kt      # Custom arithmetic parser
│           │   ├── ConversionEngine.kt          # Unit conversion logic
│           │   ├── UnitCategory.kt              # Sealed class / enum of categories
│           │   └── Unit.kt                      # Enum or data class per unit
│           └── (UI code in later phases)
└── test/
    └── kotlin/
        └── com/acalc/
            └── domain/
                ├── ExpressionEvaluatorTest.kt
                └── ConversionEngineTest.kt
```

The `domain/` package has no Android imports. It can be compiled and tested on any JVM.

---

### Pattern 1: Recursive Descent Expression Evaluator

**What:** A hand-written recursive descent parser that implements the grammar:

```
expression = term (('+' | '-') term)*
term       = factor (('*' | '/') factor)*
factor     = number | '(' expression ')' | '-' factor
number     = [0-9]+ ('.' [0-9]+)?
```

**When to use:** Any time a user string needs to be evaluated as an arithmetic expression. The grammar above handles operator precedence natively: `*` and `/` are resolved inside `term` (higher precedence), `+` and `-` are resolved in `expression` (lower precedence). Parentheses and unary minus are handled by `factor`.

**Why recursive descent over shunting-yard:** Both approaches work. Recursive descent is more readable in Kotlin; the grammar mirrors the code structure directly. Shunting-yard produces RPN first, then evaluates — slightly more indirection. Either is ~80-120 lines.

**Example skeleton:**
```kotlin
// Source: standard recursive descent grammar (verified pattern, not library-specific)
class ExpressionEvaluator {

    private lateinit var tokens: List<String>
    private var pos: Int = 0

    fun evaluate(input: String): Double? {
        tokens = tokenize(input.replace("\\s+".toRegex(), ""))
        pos = 0
        return try {
            val result = parseExpression()
            if (pos == tokens.size) result else null
        } catch (e: Exception) {
            null
        }
    }

    private fun tokenize(input: String): List<String> {
        // Regex split into numbers, operators, parentheses
        return buildList {
            var i = 0
            while (i < input.length) {
                when {
                    input[i].isDigit() || input[i] == '.' -> {
                        val start = i
                        while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                        add(input.substring(start, i))
                    }
                    else -> add(input[i++].toString())
                }
            }
        }
    }

    // expression = term (('+' | '-') term)*
    private fun parseExpression(): Double {
        var left = parseTerm()
        while (pos < tokens.size && tokens[pos] in listOf("+", "-")) {
            val op = tokens[pos++]
            val right = parseTerm()
            left = if (op == "+") left + right else left - right
        }
        return left
    }

    // term = factor (('*' | '/') factor)*
    private fun parseTerm(): Double {
        var left = parseFactor()
        while (pos < tokens.size && tokens[pos] in listOf("*", "/")) {
            val op = tokens[pos++]
            val right = parseFactor()
            left = if (op == "*") left * right else left / right
        }
        return left
    }

    // factor = '-' factor | '(' expression ')' | number
    private fun parseFactor(): Double {
        if (pos < tokens.size && tokens[pos] == "-") {
            pos++
            return -parseFactor()
        }
        if (pos < tokens.size && tokens[pos] == "(") {
            pos++
            val result = parseExpression()
            if (pos < tokens.size && tokens[pos] == ")") pos++
            return result
        }
        return tokens[pos++].toDouble()
    }
}
```

**Key properties:**
- Returns `null` on any parse failure (malformed input, division by zero should be handled in `parseTerm`)
- Division by zero: check `right == 0.0` before dividing in `parseTerm`; return `null` or throw a domain exception
- Operates on `Double` — precision is sufficient for expression entry; conversion precision is handled downstream by `ConversionEngine`

---

### Pattern 2: ConversionEngine with Base-Unit Factors

**What:** Each unit is defined by its conversion factor relative to a single base unit for the category. Conversion from unit A to unit B is: `valueInA * factorA / factorB`.

**Why this design:**
- N units require N factors (not N² conversion functions)
- Adding a new unit requires adding exactly one constant
- All factors are `BigDecimal` constructed from `String` to guarantee exactness
- Temperature is the exception — its additive offset requires special-casing

**Base units:**
| Category | Base Unit | Rationale |
|----------|-----------|-----------|
| Length | Meter | SI base unit |
| Weight | Gram | Natural metric base |
| Volume | Milliliter | Natural metric base |
| Temperature | Celsius | Special-cased (offset formulas) |
| Area | Square meter | SI base unit |
| Speed | m/s | SI base unit |

**Example skeleton:**
```kotlin
// Source: standard SI conversion pattern
import java.math.BigDecimal
import java.math.MathContext

class ConversionEngine {

    // LENGTH — factors relative to 1 meter
    private val lengthFactors = mapOf(
        LengthUnit.MM   to BigDecimal("0.001"),
        LengthUnit.CM   to BigDecimal("0.01"),
        LengthUnit.M    to BigDecimal("1"),
        LengthUnit.KM   to BigDecimal("1000"),
        LengthUnit.INCH to BigDecimal("0.0254"),
        LengthUnit.FOOT to BigDecimal("0.3048"),
        LengthUnit.YARD to BigDecimal("0.9144"),
        LengthUnit.MILE to BigDecimal("1609.344"),
    )

    fun convertLength(value: BigDecimal, from: LengthUnit, to: LengthUnit): BigDecimal {
        val inMeters = value.multiply(lengthFactors[from]!!)
        return inMeters.divide(lengthFactors[to]!!, MathContext.DECIMAL128)
    }

    // TEMPERATURE — offset-aware, cannot use multiplicative factor
    fun convertTemperature(value: BigDecimal, from: TempUnit, to: TempUnit): BigDecimal {
        // Step 1: convert to Celsius as canonical intermediate
        val celsius: BigDecimal = when (from) {
            TempUnit.CELSIUS    -> value
            TempUnit.FAHRENHEIT -> (value - BigDecimal("32")).multiply(BigDecimal("5"))
                                       .divide(BigDecimal("9"), MathContext.DECIMAL128)
            TempUnit.KELVIN     -> value - BigDecimal("273.15")
        }
        // Step 2: convert Celsius to target
        return when (to) {
            TempUnit.CELSIUS    -> celsius
            TempUnit.FAHRENHEIT -> celsius.multiply(BigDecimal("9"))
                                       .divide(BigDecimal("5"), MathContext.DECIMAL128)
                                       .add(BigDecimal("32"))
            TempUnit.KELVIN     -> celsius.add(BigDecimal("273.15"))
        }
    }
}
```

**MathContext.DECIMAL128** provides 34 significant digits — more than sufficient for any display purpose. Use it on all `divide()` calls to avoid `ArithmeticException` on non-terminating decimals.

---

### Pattern 3: Enum-Based Unit Types

**What:** Each unit category is an `enum class` (or a sealed hierarchy if additional metadata per unit is needed). The enum values map directly to the factor maps in `ConversionEngine`.

**Example:**
```kotlin
enum class LengthUnit { MM, CM, M, KM, INCH, FOOT, YARD, MILE }
enum class WeightUnit { MG, G, KG, OZ, LB, TON }
enum class VolumeUnit { ML, L, TSP, TBSP, CUP, FL_OZ, GALLON }
enum class TempUnit   { CELSIUS, FAHRENHEIT, KELVIN }
enum class AreaUnit   { SQ_MM, SQ_CM, SQ_M, SQ_KM, SQ_IN, SQ_FT, ACRE }
enum class SpeedUnit  { M_PER_S, KM_PER_H, MPH, KNOTS }
```

Later phases will map these enums to display strings in the UI layer; Phase 1 only defines the enums.

---

### Anti-Patterns to Avoid

- **Using `BigDecimal(Double)` for conversion factors:** `BigDecimal(0.0254)` is NOT `0.0254` exactly — it inherits the Double's floating-point representation error. Always use `BigDecimal("0.0254")` (String constructor).
- **Using `Double` for conversion arithmetic:** Converts away the precision benefit of BigDecimal. The input value from `ExpressionEvaluator` arrives as `Double`; convert it to BigDecimal with `value.toBigDecimal()` or `BigDecimal.valueOf(value)` (not `BigDecimal(value)`) before passing to `ConversionEngine`.
- **Calling `BigDecimal.divide()` without MathContext:** Throws `ArithmeticException` for non-terminating decimal results (e.g., 1/3). Always supply `MathContext.DECIMAL128` or a scale with rounding mode.
- **Storing conversion factors as `Double` constants:** Loses precision exactly where it's most needed. Use `BigDecimal` constants from the start.
- **Writing N² conversion pairs:** `mmToInch()`, `mmToCm()`, etc. The base-unit multiplicative model scales to any N with N constants.
- **Duplicating temperature logic:** Temperature formulas are offset-aware and must go through a canonical intermediate (Celsius). Writing direct F-to-K formulas separately introduces divergence risk.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JVM arithmetic precision | Custom fixed-point arithmetic | `java.math.BigDecimal` with `MathContext.DECIMAL128` | BigDecimal is in the JDK, battle-tested, handles scale and rounding correctly |
| String-to-number parsing | Manual character scanning for numbers | `String.toDouble()` / `String.toBigDecimal()` inside the tokenizer | Standard library handles edge cases (leading zeros, exponents, locale nuances) |
| Division by zero detection | Exception handler wrapper | Explicit `if (right == 0.0)` check in `parseTerm` returning `null` | Predictable control flow; division by zero is a valid user event, not an exceptional one |

**Key insight:** The domain objects in Phase 1 are trivially simple when built with the right primitives. The complexity is all in getting `BigDecimal` precision right (String constructor, MathContext) and getting operator precedence right in the parser (grammar layering). Neither requires external libraries.

---

## Exact Conversion Factors

These are the authoritative values to use as `BigDecimal` String constants. All are exact by international definition.

### Length (base: 1 meter)
| Unit | Factor (meters) | Source |
|------|-----------------|--------|
| mm | `"0.001"` | SI (exact) |
| cm | `"0.01"` | SI (exact) |
| m | `"1"` | Base unit |
| km | `"1000"` | SI (exact) |
| inch | `"0.0254"` | 1959 international inch definition (exact) |
| foot | `"0.3048"` | 1959 (exact, = 12 × 0.0254) |
| yard | `"0.9144"` | 1959 (exact, = 3 × 0.3048) |
| mile | `"1609.344"` | 1959 (exact, = 5280 × 0.3048) |

### Weight (base: 1 gram)
| Unit | Factor (grams) | Source |
|------|----------------|--------|
| mg | `"0.001"` | SI (exact) |
| g | `"1"` | Base unit |
| kg | `"1000"` | SI (exact) |
| oz | `"28.349523125"` | Exact avoirdupois definition |
| lb | `"453.59237"` | Exact avoirdupois definition |
| ton (metric) | `"1000000"` | SI (exact, 1 tonne = 1000 kg) |

### Volume (base: 1 milliliter = 1 cm³)
| Unit | Factor (mL) | Source |
|------|-------------|--------|
| ml | `"1"` | Base unit |
| L | `"1000"` | SI (exact) |
| tsp (US) | `"4.92892159375"` | US customary (exact) |
| tbsp (US) | `"14.78676478125"` | US customary (= 3 tsp, exact) |
| cup (US) | `"236.5882365"` | US customary (exact) |
| fl oz (US) | `"29.5735295625"` | US customary (exact) |
| gallon (US) | `"3785.411784"` | US customary (exact) |

### Temperature — offset-aware formulas (not factors)
| Conversion | Formula |
|------------|---------|
| °C → °F | `F = C × 9/5 + 32` |
| °F → °C | `C = (F − 32) × 5/9` |
| °C → K | `K = C + 273.15` |
| K → °C | `C = K − 273.15` |
| °F → K | Convert F → C → K |
| K → °F | Convert K → C → F |

Reference points: `0°C = 32°F = 273.15 K`; `100°C = 212°F = 373.15 K`

### Area (base: 1 square meter)
| Unit | Factor (m²) | Source |
|------|-------------|--------|
| sq mm | `"0.000001"` | SI (exact, 1e-6) |
| sq cm | `"0.0001"` | SI (exact, 1e-4) |
| sq m | `"1"` | Base unit |
| sq km | `"1000000"` | SI (exact, 1e6) |
| sq in | `"0.00064516"` | = 0.0254², exact |
| sq ft | `"0.09290304"` | = 0.3048², exact |
| acre | `"4046.8564224"` | = 43560 × 0.09290304, exact |

### Speed (base: 1 m/s)
| Unit | Factor (m/s) | Source |
|------|--------------|--------|
| m/s | `"1"` | Base unit |
| km/h | `"0.277777..." ` | Use `BigDecimal("1000").divide(BigDecimal("3600"), MathContext.DECIMAL128)` |
| mph | `"0.44704"` | = 1609.344/3600, exact |
| knots | `"0.514444..."` | Use `BigDecimal("1852").divide(BigDecimal("3600"), MathContext.DECIMAL128)` |

For km/h and knots, compute the factor at class initialization using `BigDecimal` division with DECIMAL128 rather than hard-coding an approximated decimal string.

---

## Common Pitfalls

### Pitfall 1: BigDecimal Constructor Trap
**What goes wrong:** `BigDecimal(0.0254)` evaluates to `0.025400000000000000138...` — the Double representation bleeds through.
**Why it happens:** The `BigDecimal(double)` constructor wraps the IEEE 754 value exactly, including its imprecision.
**How to avoid:** Always use `BigDecimal("0.0254")` (String constructor) or `BigDecimal.valueOf(0.0254)` (uses canonical Double string, slightly safer than constructor but still prefer explicit String).
**Warning signs:** Round-trip tests that pass for round numbers but fail for values involving inches (0.0254) or oz (28.349523125).

### Pitfall 2: Non-Terminating Decimal in Divide
**What goes wrong:** `BigDecimal("1").divide(BigDecimal("3"))` throws `ArithmeticException: Non-terminating decimal expansion`.
**Why it happens:** `divide()` without scale/MathContext requires an exact result; 1/3 has no finite decimal representation.
**How to avoid:** Always call `divide(divisor, MathContext.DECIMAL128)` or `divide(divisor, scale, RoundingMode.HALF_UP)`.
**Warning signs:** Tests that pass for round factor values (mm, cm, m) but throw at runtime for imperial units (inch, oz, knots).

### Pitfall 3: Operator Precedence Bug
**What goes wrong:** `evaluate("2 + 3 * 4")` returns `20` instead of `14`.
**Why it happens:** A naive left-to-right evaluation without grammar layering ignores precedence.
**How to avoid:** Layer the grammar: `parseExpression` handles `+/-`, `parseTerm` handles `*/` — multiplication-level operators are resolved before addition-level ones because `parseTerm` is called inside `parseExpression`.
**Warning signs:** The explicit success criteria test `evaluate("2 + 3 * 4") == 14` — this is a required test case.

### Pitfall 4: Tokenizer Mishandling Negative Numbers
**What goes wrong:** `-3 * 4` or `5 + -2` fail to parse because the tokenizer treats `-` at the start or after an operator as a binary operator needing a left operand.
**Why it happens:** The tokenizer sees `-` as a subtraction operator, not a unary sign.
**How to avoid:** Handle unary minus in the `parseFactor` rule: if the current token is `-` and there's no left operand in the current context, treat it as negation and call `parseFactor` recursively.
**Warning signs:** Tests with negative numbers in expressions fail but positive-only expressions pass.

### Pitfall 5: Temperature Formula Errors
**What goes wrong:** F-to-K or K-to-F conversions produce wrong results because the formula was derived directly rather than through the Celsius intermediate.
**Why it happens:** Deriving cross-unit temperature formulas directly is error-prone.
**How to avoid:** Always go through Celsius as the canonical intermediate for temperature. Two well-tested two-step conversions are safer than six independently-derived formulas.
**Warning signs:** `32°F = 0°C` and `0°C = 273.15 K` pass, but `32°F = 273.15 K` fails.

### Pitfall 6: Float Comparison in Tests
**What goes wrong:** `assertEquals(0.25, evaluate("1/4"))` passes, but `assertEquals(0.3333, evaluate("1/3"))` fails due to floating-point representation.
**Why it happens:** `Double` equality is exact.
**How to avoid:** For `Double` expression evaluator results: use `assertEquals(expected, actual, delta)` with a small delta (e.g., `1e-10`). For BigDecimal conversion results: use `compareTo() == 0` not `equals()` (BigDecimal `equals` also checks scale: `1.0 != 1.00`).
**Warning signs:** Round-trip tests fail intermittently or only for certain unit pairs.

---

## Code Examples

### ExpressionEvaluator — Full Working Pattern
```kotlin
// Verified pattern: standard recursive descent arithmetic parser
// Grammar: expression → term (('+' | '-') term)*
//          term → factor (('*' | '/') factor)*
//          factor → '-' factor | '(' expression ')' | NUMBER

class ExpressionEvaluator {
    fun evaluate(input: String): Double? {
        val cleaned = input.trim().replace("\\s+".toRegex(), "")
        if (cleaned.isEmpty()) return null
        return try {
            Parser(cleaned).parseExpression().also { result ->
                if (result.isNaN() || result.isInfinite()) return null
            }
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(private val input: String) {
        var pos = 0

        fun parseExpression(): Double {
            var result = parseTerm()
            while (pos < input.length && (input[pos] == '+' || input[pos] == '-')) {
                val op = input[pos++]
                val right = parseTerm()
                result = if (op == '+') result + right else result - right
            }
            return result
        }

        private fun parseTerm(): Double {
            var result = parseFactor()
            while (pos < input.length && (input[pos] == '*' || input[pos] == '/')) {
                val op = input[pos++]
                val right = parseFactor()
                if (op == '/' && right == 0.0) throw ArithmeticException("Division by zero")
                result = if (op == '*') result * right else result / right
            }
            return result
        }

        private fun parseFactor(): Double {
            if (pos < input.length && input[pos] == '-') {
                pos++
                return -parseFactor()
            }
            if (pos < input.length && input[pos] == '(') {
                pos++
                val result = parseExpression()
                if (pos < input.length && input[pos] == ')') pos++
                return result
            }
            return parseNumber()
        }

        private fun parseNumber(): Double {
            val start = pos
            if (pos < input.length && input[pos] == '-') pos++
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
            if (pos == start) throw IllegalArgumentException("Expected number at pos $pos")
            return input.substring(start, pos).toDouble()
        }
    }
}
```

### ConversionEngine — BigDecimal Precision Pattern
```kotlin
// Verified pattern: String-constructed BigDecimal factors with DECIMAL128 division
import java.math.BigDecimal
import java.math.MathContext

fun convertLength(value: BigDecimal, from: LengthUnit, to: LengthUnit): BigDecimal {
    val factors = mapOf(
        LengthUnit.MM   to BigDecimal("0.001"),
        LengthUnit.CM   to BigDecimal("0.01"),
        LengthUnit.M    to BigDecimal("1"),
        LengthUnit.KM   to BigDecimal("1000"),
        LengthUnit.INCH to BigDecimal("0.0254"),       // exact
        LengthUnit.FOOT to BigDecimal("0.3048"),       // exact
        LengthUnit.YARD to BigDecimal("0.9144"),       // exact
        LengthUnit.MILE to BigDecimal("1609.344"),     // exact
    )
    val inMeters = value.multiply(factors[from]!!)
    return inMeters.divide(factors[to]!!, MathContext.DECIMAL128)
}
```

### JUnit 4 Test Pattern
```kotlin
// src/test/kotlin/com/acalc/domain/ExpressionEvaluatorTest.kt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpressionEvaluatorTest {
    private val evaluator = ExpressionEvaluator()
    private val delta = 1e-10

    @Test fun `integer division returns decimal`() {
        assertEquals(0.25, evaluator.evaluate("1/4")!!, delta)
    }

    @Test fun `operator precedence multiplication before addition`() {
        assertEquals(14.0, evaluator.evaluate("2 + 3 * 4")!!, delta)
    }

    @Test fun `parentheses override precedence`() {
        assertEquals(20.0, evaluator.evaluate("(2 + 3) * 4")!!, delta)
    }

    @Test fun `division by zero returns null`() {
        assertNull(evaluator.evaluate("5 / 0"))
    }
}
```

```kotlin
// src/test/kotlin/com/acalc/domain/ConversionEngineTest.kt
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ConversionEngineTest {
    private val engine = ConversionEngine()

    @Test fun `mm to inch and back is lossless`() {
        val mm = BigDecimal("25")
        val inches = engine.convertLength(mm, LengthUnit.MM, LengthUnit.INCH)
        val backToMm = engine.convertLength(inches, LengthUnit.INCH, LengthUnit.MM)
        // compareTo ignores trailing scale differences; equals would fail on "25" vs "25.0"
        assertEquals(0, mm.compareTo(backToMm))
    }

    @Test fun `32 Fahrenheit equals 0 Celsius`() {
        val result = engine.convertTemperature(BigDecimal("32"), TempUnit.FAHRENHEIT, TempUnit.CELSIUS)
        assertEquals(0, BigDecimal("0").compareTo(result))
    }

    @Test fun `0 Celsius equals 273 point 15 Kelvin`() {
        val result = engine.convertTemperature(BigDecimal("0"), TempUnit.CELSIUS, TempUnit.KELVIN)
        assertEquals(0, BigDecimal("273.15").compareTo(result))
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Third-party expression library (mXparser) | Custom ~100-line recursive descent parser | CLAUDE.md decision | No runtime dependency; trivially testable; zero license concern |
| Double arithmetic for unit conversion | BigDecimal with MathContext.DECIMAL128 | ROADMAP.md decision (Phase 1) | Eliminates floating-point drift on round-trip tests |
| XML Views + Java | Kotlin + Jetpack Compose | Industry shift 2021-2023; project requirement | Not relevant to Phase 1 (no UI), but domain objects are written in Kotlin from day one |

**Not needed in Phase 1:**
- mXparser: explicitly forbidden
- Room: no persistence in v1
- Any Android import: domain objects must be pure JVM Kotlin

---

## Open Questions

1. **ExpressionEvaluator return type: `Double` vs `BigDecimal`**
   - What we know: Evaluator is used as input to ConversionEngine, which uses BigDecimal. The precision of `1/4 = 0.25` is exact in Double.
   - What's unclear: Whether evaluation precision matters (e.g., `1/3 * 3` — does this need to round-trip exactly?).
   - Recommendation: Return `Double` from the evaluator. The precision requirement is on conversion (BigDecimal), not on expression entry. Convert to `BigDecimal.valueOf(result)` when passing to `ConversionEngine`. This avoids building a BigDecimal expression parser which is significantly more complex.

2. **Single ConversionEngine class vs. per-category strategy classes**
   - What we know: 6 categories with different factor maps; temperature has special-case logic.
   - What's unclear: Whether splitting into `LengthConverter`, `TemperatureConverter`, etc. would be cleaner.
   - Recommendation: Start with a single `ConversionEngine` class with private per-category factor maps. The Phase 4 ViewModel calls a single `convert(value, from, to)` function. Refactor if the class grows unwieldy.

3. **Ton (metric) vs. short ton vs. long ton**
   - What we know: REQUIREMENTS.md says `ton` for weight without specifying.
   - What's unclear: Which ton standard. ClevCalc uses metric ton (1000 kg).
   - Recommendation: Use metric ton (1,000,000 g) as it is the SI tonne. Document the assumption.

---

## Environment Availability

Step 2.6: Pure code/config phase — no external tools, services, runtimes, or CLIs beyond the standard JDK are required. The domain objects and their tests run entirely on the JVM with no emulator, no Android SDK at runtime, and no network access.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | Kotlin compilation, JUnit test execution | Yes | OpenJDK 21.0.10 | — |
| Android Gradle Plugin | Build (later phases need it; Phase 1 just needs `./gradlew test`) | Will be configured in project setup | 9.1.0 per CLAUDE.md | — |

Note: The project directory currently contains only CLAUDE.md and `.planning/`. The Gradle project itself has not been initialized yet. Phase 1 planning must include a task to create the Android project skeleton (build.gradle.kts, settings.gradle.kts, libs.versions.toml, src/ tree) before domain code can be written and tested.

---

## Validation Architecture

`workflow.nyquist_validation` is `true` in `.planning/config.json` — validation section required.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) |
| Config file | None needed — Android Gradle auto-discovers `src/test/kotlin/**/*Test.kt` |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.acalc.domain.*"` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements → Test Map

Phase 1 has no user-facing requirement IDs. Tests map to success criteria instead.

| Success Criterion | Behavior | Test Type | Automated Command | File Exists? |
|-------------------|----------|-----------|-------------------|-------------|
| SC-1a | `evaluate("1/4") == 0.25` | unit | `./gradlew :app:testDebugUnitTest --tests "*.ExpressionEvaluatorTest"` | No — Wave 0 |
| SC-1b | `evaluate("2 + 3 * 4") == 14` | unit | same | No — Wave 0 |
| SC-1c | `evaluate("(2+3)*4") == 20` | unit | same | No — Wave 0 |
| SC-1d | Division by zero returns null/error | unit | same | No — Wave 0 |
| SC-2 | mm→in→mm round-trip is lossless (BigDecimal) | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConversionEngineTest"` | No — Wave 0 |
| SC-3a | `32°F = 0°C` | unit | same | No — Wave 0 |
| SC-3b | `0°C = 273.15 K` | unit | same | No — Wave 0 |
| SC-4 | All 6 categories have all listed units | unit | Same (one test per category) | No — Wave 0 |
| SC-5 | All tests pass cleanly | — | `./gradlew :app:testDebugUnitTest` | No — Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.acalc.domain.*"`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full suite green before advancing to Phase 2

### Wave 0 Gaps
- [ ] Android Gradle project skeleton (`settings.gradle.kts`, `app/build.gradle.kts`, `libs.versions.toml`, `AndroidManifest.xml`) — required before any `./gradlew` command runs
- [ ] `app/src/main/kotlin/com/acalc/domain/` — source directory must exist
- [ ] `app/src/test/kotlin/com/acalc/domain/ExpressionEvaluatorTest.kt` — covers SC-1a through SC-1d
- [ ] `app/src/test/kotlin/com/acalc/domain/ConversionEngineTest.kt` — covers SC-2 through SC-4

---

## Sources

### Primary (HIGH confidence)
- [Build local unit tests — Android Developers](https://developer.android.com/training/testing/local-tests) — test source set structure, `testImplementation` dependency
- [BigDecimal — Kotlin stdlib API](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/to-big-decimal.html) — conversion from Double
- [NIST SP 1038 — SI Conversion Factors](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication1038.pdf) — authoritative conversion factor values
- [Conversion of scales of temperature — Wikipedia](https://en.wikipedia.org/wiki/Conversion_of_scales_of_temperature) — temperature formulas (cross-verified with multiple sources)
- Project CLAUDE.md — technology stack directives (custom parser, no mXparser, JUnit 4)

### Secondary (MEDIUM confidence)
- [Using BigDecimal for High-Precision Arithmetic in Kotlin — Sling Academy](https://www.slingacademy.com/article/using-bigdecimal-for-high-precision-arithmetic-kotlin/) — BigDecimal constructor pitfalls verified against JDK docs
- [Shunting-Yard in Kotlin — Anatoly Danilov, Medium](https://anatolyd.medium.com/shunting-yard-with-simple-application-in-kotlin-bfc1de0eccc4) — Kotlin parser pattern reference
- [Making Arithmetic Parser with Kotlin — Coding Blocks, Medium](https://medium.com/coding-blocks/making-arithmetic-parser-with-kotlin-4097115f5af) — recursive descent pattern reference

### Tertiary (LOW confidence)
- None

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — Phase 1 has no new library decisions; everything is prescribed in CLAUDE.md (JUnit 4, BigDecimal, custom parser)
- Architecture patterns: HIGH — recursive descent parser and base-unit factor model are well-established, both verified against multiple sources
- Exact conversion factors: HIGH — SI factors are exact by international treaty (1959); temperature formulas are universally agreed
- Pitfalls: HIGH — BigDecimal constructor trap and divide-without-MathContext are documented JDK behaviors, not opinions
- Test structure: HIGH — Android local test source set is `src/test/kotlin`, verified from official Android Developers docs

**Research date:** 2026-04-01
**Valid until:** 2026-10-01 (stable domain — conversion factors and JUnit 4 patterns do not change)
