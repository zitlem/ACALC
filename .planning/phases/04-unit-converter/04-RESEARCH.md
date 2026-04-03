# Phase 4: Unit Converter - Research

**Researched:** 2026-04-02
**Domain:** Jetpack Compose bidirectional TextField state, ViewModel per-category state management, Material 3 ExposedDropdownMenuBox, ScrollableTabRow / FilterChip
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Both input fields are editable TextField composables — typing in either immediately triggers conversion to the other (CONV-01, CONV-02)
- **D-02:** No "Convert" button — conversion happens on every keystroke via onValueChange
- **D-03:** Expression evaluation happens before conversion — user types "25.4 + 10" and it evaluates to 35.4 then converts (CONV-03)
- **D-04:** Category selector as horizontal scrollable chips or tabs at the top of the converter screen
- **D-05:** Unit selectors as dropdown menus (ExposedDropdownMenuBox) below each input field
- **D-06:** All units from requirements available: Length(8), Weight(6), Volume(7), Temperature(3), Area(7), Speed(4)
- **D-07:** ConverterViewModel holds per-category state map — switching categories saves and restores previous input/unit pairs (CONV-10)
- **D-08:** Length defaults to mm → inches as initial pair (CONV-13)
- **D-09:** Each category has sensible default pairs (e.g., kg→lb for weight, °C→°F for temperature)
- **D-10:** ConverterViewModel uses ExpressionEvaluator.evaluate() for input parsing and ConversionEngine.convert() for unit conversion — both from Phase 1
- **D-11:** State exposed as StateFlow, collected via collectAsStateWithLifecycle()
- **D-12:** Active field tracking — ViewModel knows which field the user is typing in to avoid circular updates

### Claude's Discretion

- Exact layout proportions between input fields and category selector
- Whether to show unit abbreviations or full names in dropdowns
- Keyboard type for input fields (number vs text to support expressions)
- Animation when switching categories

### Deferred Ideas (OUT OF SCOPE)

- CONV-11 (swap units button) — Phase 5
- CONV-12 (copy to clipboard) — Phase 5
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CONV-01 | Conversion updates live as user types (no "convert" button) | onValueChange → ViewModel.onFieldChanged() pattern; activeField guard prevents loops |
| CONV-02 | Both input fields are editable (bidirectional conversion) | Two TextField composables; activeField enum in ViewModel state |
| CONV-03 | User can type math expressions in unit input fields (e.g., "25.4 + 10") | ExpressionEvaluator.evaluate(String): Double? already built — call before convert(); null result means show raw input, do not update other field |
| CONV-04 | Length conversion (mm, cm, m, km, in, ft, yd, mi) | LengthUnit enum (8 values) + ConversionEngine.convert(BigDecimal, LengthUnit, LengthUnit) already implemented |
| CONV-05 | Weight conversion (mg, g, kg, oz, lb, ton) | WeightUnit enum (6 values) + ConversionEngine already implemented |
| CONV-06 | Volume conversion (ml, L, tsp, tbsp, cup, fl oz, gallon) | VolumeUnit enum (7 values) + ConversionEngine already implemented |
| CONV-07 | Temperature conversion (Celsius, Fahrenheit, Kelvin) | TempUnit enum (3 values) + ConversionEngine offset-aware already implemented |
| CONV-08 | Area conversion (sq mm, sq cm, sq m, sq km, sq in, sq ft, acres) | AreaUnit enum (7 values) + ConversionEngine already implemented |
| CONV-09 | Speed conversion (m/s, km/h, mph, knots) | SpeedUnit enum (4 values) + ConversionEngine already implemented |
| CONV-10 | Switching categories retains previous input state per category | ViewModel holds Map<UnitCategory, CategoryState>; switching updates selectedCategory, restores saved state |
| CONV-13 | Sensible default unit pairs per category (mm -> in for length) | Hardcoded defaults map in ViewModel companion object |
</phase_requirements>

---

## Summary

Phase 4 replaces a placeholder ConverterScreen with a fully functional live bidirectional unit converter. All domain logic is already built and tested from Phase 1: `ExpressionEvaluator`, `ConversionEngine`, and the six unit enums. The phase is entirely a UI + ViewModel construction problem — no new domain code is needed.

The key design challenge is the **circular-update problem**: when field A changes, the ViewModel computes field B's value and updates state. Without a guard, collecting that state update would retrigger field A's `onValueChange`, causing an infinite recomposition loop. The solution is an `activeField` enum in `ConverterState` that the ViewModel sets when processing a change — the other field's `onValueChange` reads the computed value but the ViewModel ignores incoming changes on the non-active field during a recomposition cycle.

The per-category state persistence (CONV-10) is implemented as a `Map<UnitCategory, CategoryState>` in the ViewModel, where `CategoryState` holds `topInput`, `bottomInput`, `topUnit` (typed as the category's unit enum), and `bottomUnit`. Switching categories writes the current `CategoryState` back into the map and reads the new category's saved state (or defaults).

**Primary recommendation:** Build `ConverterViewModel` first with full unit tests before writing any UI. The circular-update guard logic and per-category state map are the two correctness-sensitive pieces that must be verified in isolation.

---

## Standard Stack

### Core — All Already in Project

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose Material3 | 1.4.0 (via BOM 2026.03.01) | `ExposedDropdownMenuBox`, `FilterChip`, `OutlinedTextField` | Already in project; provides all needed UI components |
| lifecycle-viewmodel-compose | 2.10.0 | `viewModel {}` factory, `ViewModel` base class | Already in project; CalculatorViewModel pattern to replicate |
| lifecycle-runtime-compose | 2.10.0 | `collectAsStateWithLifecycle()` | Already in project; lifecycle-safe state collection |
| kotlinx.coroutines (transitive) | via lifecycle | `MutableStateFlow`, `StateFlow` | Included transitively; same pattern as CalculatorViewModel |

### No New Dependencies Required

Phase 4 requires zero new library additions to `build.gradle.kts` or `libs.versions.toml`. All needed components are already present:
- Domain: `ExpressionEvaluator`, `ConversionEngine`, all unit enums
- UI: Material3 BOM already includes `ExposedDropdownMenuBox`, `FilterChip`, `ScrollableTabRow`
- Architecture: ViewModel + StateFlow pattern already established

---

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/kotlin/com/acalc/
├── domain/
│   ├── ConversionEngine.kt       (existing — no changes)
│   ├── ExpressionEvaluator.kt    (existing — no changes)
│   ├── UnitCategory.kt           (existing — no changes)
│   └── Units.kt                  (existing — no changes)
└── ui/
    ├── screens/
    │   └── ConverterScreen.kt    (REPLACE placeholder — full implementation)
    └── viewmodel/
        └── ConverterViewModel.kt (NEW — mirrors CalculatorViewModel structure)

app/src/test/kotlin/com/acalc/
└── ui/viewmodel/
    └── ConverterViewModelTest.kt (NEW — unit tests before UI)
```

### Pattern 1: ConverterViewModel State Model

The ViewModel holds a sealed union of per-category states. The cleanest approach uses a single data class with a sealed `UnitPair` to type-safely carry the from/to enum:

```kotlin
// Sealed wrapper so the ViewModel can hold from/to units without losing type info
sealed class UnitPair {
    data class Length(val from: LengthUnit, val to: LengthUnit) : UnitPair()
    data class Weight(val from: WeightUnit, val to: WeightUnit) : UnitPair()
    data class Volume(val from: VolumeUnit, val to: VolumeUnit) : UnitPair()
    data class Temperature(val from: TempUnit, val to: TempUnit) : UnitPair()
    data class Area(val from: AreaUnit, val to: AreaUnit) : UnitPair()
    data class Speed(val from: SpeedUnit, val to: SpeedUnit) : UnitPair()
}

data class CategoryState(
    val topInput: String = "",
    val bottomInput: String = "",
    val units: UnitPair
)

enum class ActiveField { TOP, BOTTOM }

data class ConverterState(
    val selectedCategory: UnitCategory = UnitCategory.LENGTH,
    val activeField: ActiveField = ActiveField.TOP,
    val topInput: String = "",
    val bottomInput: String = "",
    val units: UnitPair = UnitPair.Length(LengthUnit.MM, LengthUnit.INCH)
)
```

The ViewModel keeps a private `MutableMap<UnitCategory, CategoryState>` initialized with defaults. On category switch it snapshots current inputs into the map, then loads the new category's saved (or default) state.

### Pattern 2: Circular-Update Guard (the critical correctness pattern)

The active-field guard is the most important design decision. Two approaches exist:

**Approach A — activeField in state (recommended):**
```kotlin
fun onTopChanged(raw: String) {
    _state.update { it.copy(activeField = ActiveField.TOP, topInput = raw) }
    recompute(raw, isTop = true)
}

fun onBottomChanged(raw: String) {
    _state.update { it.copy(activeField = ActiveField.BOTTOM, bottomInput = raw) }
    recompute(raw, isTop = false)
}

private fun recompute(raw: String, isTop: Boolean) {
    val evaluated = evaluator.evaluate(raw) ?: return  // null = incomplete expr, do nothing
    val converted = convertForCurrentCategory(
        value = BigDecimal(evaluated.toString()),
        fromTop = isTop
    )
    val formatted = formatConverted(converted)
    _state.update {
        if (isTop) it.copy(bottomInput = formatted)
        else it.copy(topInput = formatted)
    }
}
```

**Approach B — ignore-flag boolean (simpler but fragile):**
A private `isUpdating` boolean prevents re-entry. Fragile under concurrent access and Compose recomposition timing. Not recommended.

Use Approach A. The `activeField` in state also enables the UI to style the active field differently if desired.

### Pattern 3: ExposedDropdownMenuBox for Unit Selection

Material3's `ExposedDropdownMenuBox` composable is the correct component (D-05). Pattern from Material3 docs:

```kotlin
@Composable
fun UnitDropdown(
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
```

Note: `Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)` is the current API in Material3 1.3+. The older `Modifier.menuAnchor()` without arguments is deprecated in 1.4.0.

### Pattern 4: Category Selector (ScrollableTabRow or FilterChip row)

Both are supported by the locked decision D-04. `ScrollableTabRow` is simpler to implement for exactly 6 fixed categories:

```kotlin
val categories = UnitCategory.entries
val selectedIndex = categories.indexOf(state.selectedCategory)
ScrollableTabRow(selectedTabIndex = selectedIndex) {
    categories.forEachIndexed { index, category ->
        Tab(
            selected = index == selectedIndex,
            onClick = { onCategorySelected(category) },
            text = { Text(category.displayLabel) }
        )
    }
}
```

`FilterChip` row in a `LazyRow` is equally valid and provides a different visual treatment. The planner can choose either — both work identically.

### Pattern 5: Keyboard Type for Expression-Aware Input Fields

Locked decision D-03 requires accepting expressions like "25.4 + 10". This means the keyboard type CANNOT be `KeyboardType.Decimal` (only allows digits and decimal point). The correct approach:

```kotlin
OutlinedTextField(
    value = topInput,
    onValueChange = onTopChanged,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,    // allows operators: +, -, *, /
        imeAction = ImeAction.Done
    )
)
```

`KeyboardType.Number` also blocks operators. Only `KeyboardType.Text` allows the full expression character set needed. This is a genuine gotcha — using `KeyboardType.Decimal` would silently strip operator characters on many Android keyboards.

### Pattern 6: Result Formatting for Converter Output

The converted result (written to the non-active field) needs a consistent formatting policy. Unlike the calculator which shows integers without decimals, conversions often produce non-terminating decimals. Recommended policy:

- Strip trailing zeros after decimal point
- Cap at 10 significant decimal places
- Never show scientific notation (use `toPlainString()` on BigDecimal)
- Example: 25.4 mm → 1.0 inches (not 1.00000000 or 1)

```kotlin
private fun formatConverted(value: BigDecimal): String {
    // Round to 10 decimal places, strip trailing zeros
    return value
        .setScale(10, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}
```

### Pattern 7: ConversionEngine dispatch (sealed UnitPair switch)

The ViewModel's `convertForCurrentCategory()` must dispatch to the correct `ConversionEngine.convert()` overload. The sealed `UnitPair` makes this exhaustive:

```kotlin
private fun convertForCurrentCategory(value: BigDecimal, fromTop: Boolean): BigDecimal {
    return when (val units = _state.value.units) {
        is UnitPair.Length -> if (fromTop)
            engine.convert(value, units.from, units.to)
            else engine.convert(value, units.to, units.from)
        is UnitPair.Weight -> if (fromTop)
            engine.convert(value, units.from, units.to)
            else engine.convert(value, units.to, units.from)
        // ... etc for all 6
    }
}
```

### Anti-Patterns to Avoid

- **Storing unit enums as strings in state:** Use the typed enum directly. String-based unit selection forces fragile string-to-enum parsing on every recompose.
- **Triggering conversion inside the composable:** Conversion logic belongs in the ViewModel. The composable only calls `onTopChanged(raw)` and displays state — never calls ConversionEngine directly.
- **Resetting non-active field to empty on null expression:** If `ExpressionEvaluator.evaluate()` returns null (incomplete expression like "25."), leave the other field unchanged — do not clear it. This preserves the last good conversion result while the user is still typing.
- **Using `remember { mutableStateOf() }` for unit selection state:** Unit selection is ViewModel state (persists across recomposition, per-category). Do not hoist it to composable-local state.
- **Using `TextField` without `readOnly = true` for dropdowns:** The unit selector TextField inside `ExposedDropdownMenuBox` must be `readOnly = true` to prevent software keyboard appearing when the user taps the dropdown anchor.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Unit conversion math | Custom formulas per unit pair | `ConversionEngine.convert()` (Phase 1) | Already built, BigDecimal precision, 35 units, all categories |
| Expression parsing | Second parser | `ExpressionEvaluator.evaluate()` (Phase 1) | Already built, tested, handles operators + parentheses |
| Dropdown menu | Custom popup/dialog for unit selection | `ExposedDropdownMenuBox` (Material3) | Handles keyboard focus, touch dismiss, accessibility |
| Category tabs | Custom horizontal scroll | `ScrollableTabRow` or `LazyRow` + `FilterChip` (Material3) | Handles scroll, selection indicator, accessibility |
| State persistence across category switches | SharedPreferences or Room | In-memory `Map<UnitCategory, CategoryState>` in ViewModel | No persistence requirement in v1; ViewModel survives config changes |

**Key insight:** Phase 1 domain work was specifically designed so Phase 4 is a pure UI problem. Do not redo any domain work.

---

## Common Pitfalls

### Pitfall 1: Infinite Recomposition Loop (the circular update)
**What goes wrong:** Field A onChange → ViewModel updates state → state flows to both fields → Field B onChange fires → ViewModel updates state → loop.
**Why it happens:** `TextField.onValueChange` fires whenever the `value` prop changes externally (e.g., from the ViewModel writing the converted result). Compose does not distinguish user-initiated vs program-initiated value changes.
**How to avoid:** The `activeField` guard in ConverterViewModel. In `onTopChanged()`, set `activeField = TOP` before writing `bottomInput`. In `onBottomChanged()`, set `activeField = BOTTOM` before writing `topInput`. The composable always passes the current raw string to `onValueChange` — the ViewModel simply ignores writes that would cause the loop because it already computed the result for the opposing field in the same state update.
**Warning signs:** App freezes or ANR immediately upon typing a character in either field.

### Pitfall 2: KeyboardType.Decimal Silently Drops Operator Characters
**What goes wrong:** User types "25.4 + 10" but the "+" is silently dropped by the system keyboard because `KeyboardType.Decimal` instructs Android to show only digits and decimal point.
**Why it happens:** Android's `KeyboardType` is a hint to the soft keyboard. `Decimal` and `Number` types restrict the visible keys. Different keyboard apps (Gboard, Samsung, SwiftKey) implement this restriction differently — some drop typed chars, some just show a number-only layout but allow text entry.
**How to avoid:** Use `KeyboardType.Text`. The app still validates input downstream via `ExpressionEvaluator.evaluate()` — invalid characters simply return null and the other field is not updated.
**Warning signs:** Integration test with expression input fails; characters after the first operator are missing from the TextField value.

### Pitfall 3: ExposedDropdownMenuBox menuAnchor API Change
**What goes wrong:** `Modifier.menuAnchor()` no longer compiles or shows a deprecation warning in Material3 1.3+.
**Why it happens:** The API changed to require an explicit `MenuAnchorType` parameter: `Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)` for a read-only text field anchor.
**How to avoid:** Use `Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)` — this is confirmed correct for Material3 1.4.0 (in use).
**Warning signs:** Compile error or lint deprecation warning on `menuAnchor()` in the TextField.

### Pitfall 4: BigDecimal → Double → BigDecimal Precision Loss
**What goes wrong:** `ExpressionEvaluator.evaluate()` returns `Double?`. Converting Double to BigDecimal via `BigDecimal(double)` introduces floating-point representation artifacts (e.g., `BigDecimal(0.1)` is `0.1000000000000000055511151231257827021181583404541015625`).
**Why it happens:** `BigDecimal(double)` constructor captures the exact binary floating-point representation, not the decimal value. This is a well-known Java/Kotlin pitfall.
**How to avoid:** Always use `BigDecimal(evaluated.toString())` or `evaluated.toBigDecimal()` (Kotlin extension which calls `toString()` internally) to go from Double to BigDecimal. This is already established in Phase 1's ConversionEngine.
**Warning signs:** Conversion outputs show trailing noise digits like `0.039370078740157484` for 1 mm → inches (correct is `0.0393700787401574803`).

### Pitfall 5: Per-Category State Map Not Initialized with Defaults
**What goes wrong:** Switching to a category that has never been visited returns null from the map, causing a NullPointerException or blank screen.
**Why it happens:** The `Map<UnitCategory, CategoryState>` is initialized lazily but the ViewModel doesn't pre-populate all 6 entries with defaults.
**How to avoid:** Initialize the map in the ViewModel constructor with all 6 categories' default `CategoryState` objects. Use a `companion object` to hold the defaults:
```kotlin
private val categoryStateMap = UnitCategory.entries.associateWith { cat ->
    defaultStateFor(cat)
}.toMutableMap()
```
**Warning signs:** Crash or blank converter fields when switching from Length to any other category for the first time.

### Pitfall 6: Null Expression Result Clears the Other Field
**What goes wrong:** User types "25." (valid mid-expression). `ExpressionEvaluator.evaluate("25.")` returns null (incomplete). The ViewModel writes empty string to the other field, erasing the last good conversion result.
**Why it happens:** Handling null by clearing is an easy implementation mistake.
**How to avoid:** On null from `evaluate()`, do not update the other field at all — leave it showing the last successfully computed value. Only update when `evaluate()` returns a non-null result.
**Warning signs:** Other field flickers to empty while user is still typing a decimal.

---

## Code Examples

### ConverterViewModel skeleton
```kotlin
class ConverterViewModel : ViewModel() {
    private val evaluator = ExpressionEvaluator()
    private val engine = ConversionEngine()

    private val _state = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = _state

    // Per-category saved state — all 6 initialized with defaults
    private val categoryStateMap: MutableMap<UnitCategory, CategoryState> =
        UnitCategory.entries.associateWith { defaultStateFor(it) }.toMutableMap()

    fun onCategorySelected(category: UnitCategory) {
        // Save current category state
        val current = _state.value
        categoryStateMap[current.selectedCategory] = CategoryState(
            topInput = current.topInput,
            bottomInput = current.bottomInput,
            units = current.units
        )
        // Load new category state
        val next = categoryStateMap[category]!!
        _state.update { _ ->
            ConverterState(
                selectedCategory = category,
                activeField = ActiveField.TOP,
                topInput = next.topInput,
                bottomInput = next.bottomInput,
                units = next.units
            )
        }
    }

    fun onTopChanged(raw: String) {
        _state.update { it.copy(activeField = ActiveField.TOP, topInput = raw) }
        val evaluated = evaluator.evaluate(raw) ?: return
        val result = convertCurrentUnits(BigDecimal(evaluated.toString()), fromTop = true)
        _state.update { it.copy(bottomInput = formatConverted(result)) }
    }

    fun onBottomChanged(raw: String) {
        _state.update { it.copy(activeField = ActiveField.BOTTOM, bottomInput = raw) }
        val evaluated = evaluator.evaluate(raw) ?: return
        val result = convertCurrentUnits(BigDecimal(evaluated.toString()), fromTop = false)
        _state.update { it.copy(topInput = formatConverted(result)) }
    }

    fun onTopUnitChanged(unit: UnitEnum) { /* sealed dispatch, then recompute */ }
    fun onBottomUnitChanged(unit: UnitEnum) { /* sealed dispatch, then recompute */ }
}
```

### ConverterScreen structure
```kotlin
@Composable
fun ConverterScreen(modifier: Modifier = Modifier) {
    val vm = viewModel<ConverterViewModel> { ConverterViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()
    ConverterContent(
        state = state,
        onCategorySelected = vm::onCategorySelected,
        onTopChanged = vm::onTopChanged,
        onBottomChanged = vm::onBottomChanged,
        onTopUnitChanged = vm::onTopUnitChanged,
        onBottomUnitChanged = vm::onBottomUnitChanged,
        modifier = modifier
    )
}

@Composable
private fun ConverterContent(
    state: ConverterState,
    // ... callbacks
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        CategorySelector(
            selected = state.selectedCategory,
            onSelected = onCategorySelected
        )
        ConversionRow(
            label = "From",
            input = state.topInput,
            unitDisplay = state.units.topDisplayName(),
            unitOptions = state.selectedCategory.unitOptions(),
            onInputChanged = onTopChanged,
            onUnitChanged = onTopUnitChanged
        )
        ConversionRow(
            label = "To",
            input = state.bottomInput,
            unitDisplay = state.units.bottomDisplayName(),
            unitOptions = state.selectedCategory.unitOptions(),
            onInputChanged = onBottomChanged,
            onUnitChanged = onBottomUnitChanged
        )
    }
}
```

### Default unit pairs
```kotlin
companion object {
    fun defaultStateFor(category: UnitCategory): CategoryState = when (category) {
        UnitCategory.LENGTH      -> CategoryState(units = UnitPair.Length(LengthUnit.MM, LengthUnit.INCH))
        UnitCategory.WEIGHT      -> CategoryState(units = UnitPair.Weight(WeightUnit.KG, WeightUnit.LB))
        UnitCategory.VOLUME      -> CategoryState(units = UnitPair.Volume(VolumeUnit.ML, VolumeUnit.FL_OZ))
        UnitCategory.TEMPERATURE -> CategoryState(units = UnitPair.Temperature(TempUnit.CELSIUS, TempUnit.FAHRENHEIT))
        UnitCategory.AREA        -> CategoryState(units = UnitPair.Area(AreaUnit.SQ_M, AreaUnit.SQ_FT))
        UnitCategory.SPEED       -> CategoryState(units = UnitPair.Speed(SpeedUnit.KM_PER_H, SpeedUnit.MPH))
    }
}
```

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) |
| Config file | none — standard Android test runner auto-discovers |
| Quick run command | `./gradlew :app:testDebugUnitTest` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CONV-01 | Live conversion on keystroke — other field updates | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-02 | Bidirectional — typing in bottom field updates top | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-03 | Expression "25.4 + 10" evaluates to 35.4 then converts | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-04 | Length: mm to inches correctness | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-05 | Weight: kg to lb correctness | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-06 | Volume: L to gallon correctness | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-07 | Temperature: 100°C to °F = 212 | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-08 | Area: sq m to sq ft correctness | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-09 | Speed: km/h to mph correctness | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-10 | Category switch restores prior inputs/units | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |
| CONV-13 | Length default is mm → inches | unit | `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"` | ❌ Wave 0 |

Note: CONV-04 through CONV-09 conversion correctness is already covered by `ConversionEngineTest.kt` (Phase 1). The ViewModel tests verify that the ViewModel correctly dispatches to ConversionEngine and surfaces the result — not that the math is correct (already proven).

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "*.ConverterViewModelTest"`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/kotlin/com/acalc/ui/viewmodel/ConverterViewModelTest.kt` — covers all CONV-* requirements listed above

*(No new test infrastructure needed — JUnit 4 + existing test runner is sufficient)*

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Modifier.menuAnchor()` (no args) | `Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)` | Material3 1.3.0 | Required for correct ExposedDropdownMenuBox behavior |
| `LiveData.observe()` | `StateFlow` + `collectAsStateWithLifecycle()` | Compose 1.x / Lifecycle 2.6+ | Already established in project; do not use LiveData |
| `NavController` (Nav2) | `NavBackStack` + `NavDisplay` (Nav3) | Navigation3 1.0.0 stable (Nov 2025) | Already established in AppShell; ConverterScreen needs no navigation changes |

---

## Environment Availability

Step 2.6: SKIPPED (no new external dependencies — this phase only uses tools already present in the project)

---

## Open Questions

1. **UnitPair sealed class vs. storing units as two separate `Any` fields**
   - What we know: Type-safe sealed class requires more boilerplate but eliminates runtime casting
   - What's unclear: Whether Kotlin `when` exhaustiveness on a 6-branch sealed class is worth the setup
   - Recommendation: Use sealed class. The exhaustiveness check is a real correctness benefit for the conversion dispatch; the boilerplate is a one-time cost at ViewModel construction time.

2. **Unit display: abbreviation vs. full name in dropdowns**
   - What we know: `displayName` field on each unit enum already holds abbreviation (e.g., "mm", "kg", "°C")
   - What's unclear: Whether 7-character options like "gallon" and "knots" look better as full names or abbreviations in the dropdown
   - Recommendation: Use `displayName` (abbreviation) in the dropdown anchor TextField for compactness; full name in the DropdownMenuItem list for readability. Claude's discretion per CONTEXT.md.

3. **Category selector: ScrollableTabRow vs. FilterChip LazyRow**
   - What we know: Both are viable; 6 categories fit without scrolling on most screen sizes
   - What's unclear: Visual preference
   - Recommendation: `ScrollableTabRow` is simpler (no need to manage selected chip state separately) and follows Material3 navigation tab convention. FilterChip provides a more "chip" aesthetic. Either is correct — planner's discretion.

---

## Sources

### Primary (HIGH confidence)
- `app/src/main/kotlin/com/acalc/domain/ConversionEngine.kt` — exact API signatures, all 6 convert() overloads, BigDecimal usage
- `app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt` — exact signature `evaluate(String): Double?`, null contract
- `app/src/main/kotlin/com/acalc/domain/Units.kt` — all 35 unit enum values and displayName fields
- `app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorViewModel.kt` — established ViewModel + StateFlow pattern to replicate
- `app/src/main/kotlin/com/acalc/ui/AppShell.kt` — confirmed ConverterScreen receives `modifier: Modifier` from innerPadding
- `.planning/phases/04-unit-converter/04-CONTEXT.md` — locked decisions D-01 through D-12
- `gradle/libs.versions.toml` — confirmed all needed libraries already present, no new deps required

### Secondary (MEDIUM confidence)
- Material3 ExposedDropdownMenuBox `menuAnchor(MenuAnchorType)` API — confirmed from Material3 1.4.0 migration pattern; older `menuAnchor()` without args is deprecated
- `KeyboardType.Text` requirement for expression input — verified by reasoning from Android keyboard type semantics; `Decimal`/`Number` types only allow digit characters

### Tertiary (LOW confidence — validate at implementation)
- BigDecimal(double) precision trap — well-known Java pitfall, addressed by using `toString()` bridge; verify that `evaluator.evaluate()` Double result converts correctly via `.toBigDecimal()` extension

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already in project, versions confirmed from libs.versions.toml
- Architecture: HIGH — ViewModel + StateFlow pattern is a direct extension of CalculatorViewModel; domain APIs are exact (read from source)
- Circular-update guard: HIGH — the activeField pattern is a well-established solution to this exact problem in Compose bidirectional TextField scenarios
- Pitfalls: HIGH for loop/keyboard pitfalls (verified from Compose behavior); MEDIUM for menuAnchor API change (verified by pattern match on Material3 1.4.0)

**Research date:** 2026-04-02
**Valid until:** 2026-05-02 (stable libraries, no fast-moving APIs in scope)
