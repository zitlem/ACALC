# Phase 4: Unit Converter - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver the core value feature: a live bidirectional unit converter replacing the Converter placeholder screen. Users type in either field and see the other update in real-time. Supports math expressions in input fields, 6 unit categories, and persistent per-category state.

</domain>

<decisions>
## Implementation Decisions

### Live Dual Display
- **D-01:** Both input fields are editable TextField composables — typing in either immediately triggers conversion to the other (CONV-01, CONV-02)
- **D-02:** No "Convert" button — conversion happens on every keystroke via onValueChange
- **D-03:** Expression evaluation happens before conversion — user types "25.4 + 10" and it evaluates to 35.4 then converts (CONV-03)

### Category & Unit Selection
- **D-04:** Category selector as horizontal scrollable chips or tabs at the top of the converter screen
- **D-05:** Unit selectors as dropdown menus (ExposedDropdownMenuBox) below each input field
- **D-06:** All units from requirements available: Length(8), Weight(6), Volume(7), Temperature(3), Area(7), Speed(4)

### State Management
- **D-07:** ConverterViewModel holds per-category state map — switching categories saves and restores previous input/unit pairs (CONV-10)
- **D-08:** Length defaults to mm → inches as initial pair (CONV-13)
- **D-09:** Each category has sensible default pairs (e.g., kg→lb for weight, °C→°F for temperature)

### Architecture
- **D-10:** ConverterViewModel uses ExpressionEvaluator.evaluate() for input parsing and ConversionEngine.convert() for unit conversion — both from Phase 1
- **D-11:** State exposed as StateFlow, collected via collectAsStateWithLifecycle()
- **D-12:** Active field tracking — ViewModel knows which field the user is typing in to avoid circular updates

### Claude's Discretion
- Exact layout proportions between input fields and category selector
- Whether to show unit abbreviations or full names in dropdowns
- Keyboard type for input fields (number vs text to support expressions)
- Animation when switching categories

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Domain Layer (Phase 1)
- `app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt` — evaluate(String): Double?
- `app/src/main/kotlin/com/acalc/domain/ConversionEngine.kt` — convert(value, from, to): BigDecimal
- `app/src/main/kotlin/com/acalc/domain/UnitCategory.kt` — 6 category enum
- `app/src/main/kotlin/com/acalc/domain/Units.kt` — All unit enums (35 units across 6 categories)

### UI Layer (Phase 2-3)
- `app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt` — Current placeholder to replace
- `app/src/main/kotlin/com/acalc/ui/AppShell.kt` — Navigation wiring
- `app/src/main/kotlin/com/acalc/ui/viewmodel/CalculatorViewModel.kt` — ViewModel pattern to follow

### Technology Stack
- `CLAUDE.md` — ViewModel + StateFlow pattern, collectAsStateWithLifecycle(), no LiveData

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ExpressionEvaluator.evaluate(String): Double?` — parse math expressions in unit fields
- `ConversionEngine.convert(value: Double, from: Enum, to: Enum): BigDecimal` — all 6 categories
- `UnitCategory` enum with `Length, Weight, Temperature, Volume, Area, Speed`
- Per-category unit enums: `LengthUnit`, `WeightUnit`, `VolumeUnit`, `TemperatureUnit`, `AreaUnit`, `SpeedUnit`
- `CalculatorViewModel` pattern — StateFlow + viewModel {} factory

### Established Patterns
- `com.acalc.ui.screens` for screen composables
- `com.acalc.ui.viewmodel` for ViewModels
- Material 3 theming via AcalcTheme

### Integration Points
- `ConverterScreen.kt` receives `modifier: Modifier` from AppShell's innerPadding
- Navigation already wired via ConverterRoute in AppShell

</code_context>

<specifics>
## Specific Ideas

- Core use case: mm/cm to inches conversion — this is the most frequently used feature
- Inspired by ClevCalc's live dual-display approach
- Expression support in input fields is a differentiator (e.g., type "25.4 + 10" to add measurements)

</specifics>

<deferred>
## Deferred Ideas

- CONV-11 (swap units button) — Phase 5
- CONV-12 (copy to clipboard) — Phase 5

</deferred>

---

*Phase: 04-unit-converter*
*Context gathered: 2026-04-02 via auto mode*
