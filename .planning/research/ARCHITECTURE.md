# Architecture Patterns

**Domain:** Android calculator + unit converter (Kotlin + Jetpack Compose + Material 3)
**Researched:** 2026-04-01
**Confidence:** HIGH (backed by official Android docs + multiple verified sources)

---

## Recommended Architecture

**MVVM with Unidirectional Data Flow (UDF).** This is the architecture Android's own documentation recommends for Jetpack Compose. It aligns perfectly with Compose's declarative model: state flows down into composables, events flow up into ViewModels.

Do NOT use Clean Architecture's three-layer separation (domain/data/presentation) for this project. That overhead is warranted for multi-feature apps with remote data, repositories, and use cases. ACALC is a single-user, offline, single-binary app with no persistence and no network. One ViewModel per screen is the right scope.

```
UI Layer (Composables)
       |   ^
       |   |  state (StateFlow -> collectAsStateWithLifecycle)
events |   |
(lambdas)  |
       v   |
ViewModel (state holder + logic host)
       |
       |  delegates to
       v
Domain Objects (pure Kotlin, no Android deps)
  - ExpressionEvaluator
  - ConversionEngine
  - UnitCategory definitions
```

---

## Component Boundaries

### 1. App Shell

**Responsibility:** Entry point. Hosts the NavHost and bottom navigation bar. Applies Material 3 theming.

**Communicates with:** Navigation graph only. No business logic.

**Key composables:** `MainActivity`, `AppTheme`, `BottomNavBar`, `AppNavGraph`.

The bottom navigation bar has two destinations: Calculator and Converter. Each destination is a top-level route with its own ViewModel. There is no shared ViewModel between Calculator and Converter — they are independent features.

---

### 2. Calculator Feature

**Responsibility:** Standard arithmetic calculator — input display, expression building, evaluation.

**Communicates with:** `ExpressionEvaluator` (pure Kotlin object), nothing else.

**Key composables:**
- `CalculatorScreen` — root composable, observes `CalculatorViewModel` state
- `CalculatorDisplay` — shows current expression and result
- `CalculatorKeypad` — grid of buttons, fires events upward via lambdas

**Key ViewModel:** `CalculatorViewModel`
- Holds `MutableStateFlow<CalculatorUiState>`
- `CalculatorUiState` contains: `expression: String`, `result: String`, `isError: Boolean`
- Handles button press events: digit input, operator input, equals, clear, backspace
- Delegates evaluation to `ExpressionEvaluator`

**Key domain object:** `ExpressionEvaluator`
- Pure Kotlin object, no Android dependencies
- Takes a `String` expression, returns a `Double` or throws
- Handles operator precedence, decimal inputs
- No library dependency needed for this scope: implement a simple recursive descent parser or use `exp4j` (Java, no Android dependency, well-tested)

---

### 3. Converter Feature

**Responsibility:** Category selection, dual-field live input, bidirectional conversion.

**Communicates with:** `ConversionEngine`, `ExpressionEvaluator`.

**Key composables:**
- `ConverterScreen` — root composable, observes `ConverterViewModel` state
- `CategorySelector` — horizontal scrollable chip row or tab row for unit categories (Length, Weight, Volume, etc.)
- `UnitInputRow` — one row = one TextField + unit label + unit selector (dropdown)
- The screen shows exactly two `UnitInputRow` instances: "from" and "to"

**Key ViewModel:** `ConverterViewModel`
- Holds `MutableStateFlow<ConverterUiState>`
- `ConverterUiState` contains:
  - `category: UnitCategory`
  - `fromUnit: UnitType`
  - `toUnit: UnitType`
  - `fromRawInput: String` — what the user is typing in the top field
  - `toRawInput: String` — what the user is typing in the bottom field
  - `activeField: Field` (FROM or TO) — which field the user last typed into
- On every keystroke in the active field, ViewModel:
  1. Evaluates the raw expression string (delegates to `ExpressionEvaluator`)
  2. Converts the resulting Double using `ConversionEngine`
  3. Writes the converted value into the other field's `rawInput` as a formatted string

**Key domain object:** `ConversionEngine`
- Pure Kotlin object, no Android dependencies
- Stateless: `convert(value: Double, from: UnitType, to: UnitType): Double`
- Internal approach: each `UnitType` has a `toBase: Double` factor (ratio to the canonical base unit for that category). Conversion = `value * from.toBase / to.toBase`. Exception: temperature uses formula-based conversion instead of a factor.
- All factors defined as compile-time constants in a sealed class or enum — no runtime loading, no files.

**Key domain object:** `UnitCategory` / `UnitType`
- Sealed class hierarchy:
  ```
  UnitCategory.Length  -> UnitType.Millimeter, Centimeter, Meter, ... Inch, Foot, ...
  UnitCategory.Weight  -> UnitType.Milligram, Gram, Kilogram, Ounce, Pound, Ton
  UnitCategory.Volume  -> UnitType.Milliliter, Liter, Teaspoon, Tablespoon, Cup, ...
  UnitCategory.Temperature -> UnitType.Celsius, Fahrenheit, Kelvin
  UnitCategory.Area    -> UnitType.SqMillimeter, ... Acre
  UnitCategory.Speed   -> UnitType.MetersPerSecond, KmPerHour, Mph, Knots
  ```
- Each UnitType carries: display label, symbol, `toBase` factor (or formula flag for Temperature)

---

## Data Flow

### Calculator: button press to display update

```
User taps "7"
  -> CalculatorKeypad fires onDigitPressed("7")
  -> CalculatorViewModel.onDigitPressed("7")
     -> appends to expression string
     -> emits new CalculatorUiState(expression = "7", result = "")
  -> CalculatorScreen recomposes
  -> CalculatorDisplay shows "7"

User taps "="
  -> CalculatorKeypad fires onEqualsPressed()
  -> CalculatorViewModel.onEqualsPressed()
     -> ExpressionEvaluator.evaluate(expression)
     -> emits CalculatorUiState(expression = "7", result = "7")
  -> CalculatorDisplay shows result
```

### Converter: live dual-display update on keystroke

```
User types "2" into the FROM field (mm -> in)
  -> UnitInputRow fires onFromInputChanged("2")
  -> ConverterViewModel.onFromInputChanged("2")
     -> activeField = FROM
     -> fromRawInput = "2"
     -> evaluatedValue = ExpressionEvaluator.evaluate("2") = 2.0
     -> convertedValue = ConversionEngine.convert(2.0, Millimeter, Inch) = 0.0787...
     -> toRawInput = "0.079" (formatted)
     -> emits new ConverterUiState
  -> ConverterScreen recomposes
  -> Both UnitInputRow instances display updated values

User types "25.4 + 10" into FROM field
  -> Same path, ExpressionEvaluator handles the expression string
  -> evaluates to 35.4 before converting
```

**Critical detail:** When the ViewModel updates the non-active field's `rawInput`, the TextField for that field must NOT fire its own `onValueChange` event back into the ViewModel. This is prevented by tracking `activeField` — the ViewModel only processes `onFromInputChanged` events when `activeField == FROM`, and ignores them when it is programmatically setting the value. In practice: use `onValueChange` only when `field == activeField` to avoid infinite loops.

---

## Suggested Build Order

Dependencies flow from the inside out. Build inner components first.

### Layer 0: Domain Objects (no dependencies, pure Kotlin)
1. `UnitType` sealed class with all units and toBase factors
2. `UnitCategory` sealed class grouping units
3. `ConversionEngine.convert()` — pure function, fully testable immediately
4. `ExpressionEvaluator.evaluate()` — pure function, fully testable immediately

**Why first:** All features depend on these. They have zero Android dependencies and can be developed and unit-tested in isolation before any UI exists.

### Layer 1: Navigation Shell
5. `MainActivity` with `NavHost` and two placeholder screens
6. `BottomNavBar` with Calculator and Converter routes
7. `AppTheme` with Material 3 theming

**Why second:** Establishes the skeleton. Placeholder screens confirm navigation works before feature screens are built.

### Layer 2: Calculator Feature
8. `CalculatorUiState` data class
9. `CalculatorViewModel` with expression building and equals logic
10. `CalculatorKeypad` composable (buttons grid)
11. `CalculatorDisplay` composable
12. `CalculatorScreen` composable wiring ViewModel to UI

**Why third:** Calculator is the simpler feature — one input, one direction. Proves the MVVM pattern works in the project before tackling bidirectional converter state.

### Layer 3: Converter Feature
13. `ConverterUiState` data class
14. `ConverterViewModel` with bidirectional live-update logic
15. `UnitInputRow` composable (TextField + unit selector)
16. `CategorySelector` composable (chip row for category switching)
17. `ConverterScreen` composable wiring ViewModel to UI

**Why last:** Most complex state management (bidirectional, active field tracking). Benefits from ExpressionEvaluator being proven in Layer 0 and MVVM pattern proven in Layer 2.

### Layer 4: Polish
18. Material 3 theming, spacing, typography
19. Unit selectors (DropdownMenu or ModalBottomSheet per field)
20. Edge cases: empty input, divide-by-zero, overflow display

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Shared ViewModel Between Calculator and Converter

**What:** One `AppViewModel` holding state for both screens.
**Why bad:** Couples unrelated features. State for one screen stays alive when the other is visible. Makes testing harder.
**Instead:** Separate `CalculatorViewModel` and `ConverterViewModel` scoped to their respective NavGraph destinations.

---

### Anti-Pattern 2: Bidirectional TextField Binding Without Active Field Guard

**What:** Both TextFields update the ViewModel on every `onValueChange`, and the ViewModel updates both fields in response.
**Why bad:** Causes infinite recomposition loops. User types "1", ViewModel converts to "0.039", which fires another `onValueChange("0.039")`, which reconverts, forever.
**Instead:** Track `activeField` in ViewModel state. Only the active field's `onValueChange` triggers a conversion. The other field receives its value as read-only state from the ViewModel.

---

### Anti-Pattern 3: Hardcoding Conversion Logic in Composables

**What:** `val result = inputValue * 0.0393701` inside a `@Composable` function.
**Why bad:** Untestable, violates separation of concerns, breaks previews.
**Instead:** All conversion math lives in `ConversionEngine`, called from ViewModel only.

---

### Anti-Pattern 4: String-Based Navigation Routes With Arguments for Simple 2-Tab App

**What:** Using `navController.navigate("converter/length/mm/in")` to encode converter state in the route.
**Why bad:** The converter's "which units are selected" is UI state, not navigation state. The app has only two top-level destinations. Encoding unit selection in routes is overengineering that adds serialization complexity.
**Instead:** Unit selection lives in `ConverterViewModel`. Bottom navigation only switches between Calculator and Converter routes.

---

## Scalability Considerations

This is a personal-use app. The architecture above is intentionally right-sized — not over-engineered. If scope grows:

| Concern | At Current Scope | If Adding More Categories |
|---------|-----------------|--------------------------|
| UnitType definitions | Sealed class in one file | Still fine; sealed class scales to 50+ types |
| Adding a new category | Add sealed subclass + toBase factors | One file change, no ViewModel changes |
| Currency converter (future) | Not in v1 | Add `CurrencyConverterViewModel` as separate screen with its own Repository layer |
| History feature (future) | Not in v1 | Add Room DB + Repository; ViewModels gain a Repository dependency |

Multi-module architecture is not warranted for this project. It adds Gradle complexity with no benefit for a single-developer, single-APK app of this size.

---

## Sources

- [Compose UI Architecture — Android Developers](https://developer.android.com/develop/ui/compose/architecture) (HIGH confidence — official)
- [State and Jetpack Compose — Android Developers](https://developer.android.com/develop/ui/compose/state) (HIGH confidence — official)
- [Modern Android App Architecture in 2025: MVVM, MVI, Clean Architecture](https://medium.com/@androidlab/modern-android-app-architecture-in-2025-mvvm-mvi-and-clean-architecture-with-jetpack-compose-c0df3c727334) (MEDIUM confidence — community article)
- [Two-way data binding in Jetpack Compose — ProAndroidDev](https://proandroiddev.com/two-way-data-binding-in-jetpack-compose-1be55c402ec6) (MEDIUM confidence — community article)
- [Navigation with Compose — Android Developers](https://developer.android.com/develop/ui/compose/navigation) (HIGH confidence — official)
- [ExprK — simple math expression evaluator for Kotlin](https://github.com/Keelar/ExprK) (MEDIUM confidence — GitHub)
- [Unit Converter Jetpack Compose — Medium](https://medium.com/@abhineshchandra1234/unit-converter-jetpack-compose-c3413d232239) (MEDIUM confidence — community article)
