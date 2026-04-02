# Project Research Summary

**Project:** ACALC — Android Calculator + Unit Converter
**Domain:** Native Android app, offline-only, personal sideloaded APK
**Researched:** 2026-04-01
**Confidence:** HIGH

## Executive Summary

ACALC is a focused, offline-only Android app combining a standard arithmetic calculator with a unit converter across 6 categories (length, weight, volume, temperature, area, speed). The defining differentiator is expression support in unit input fields — users can type `25.4 + 10` directly into a measurement field and have it evaluated before conversion. No mainstream Android converter does this. The recommended build approach is Jetpack Compose + Material 3 + MVVM with a small, deliberately dependency-light stack: no DI framework, no persistence library, no network library, and a custom 80–120 line expression evaluator rather than a third-party parser. The architecture is two self-contained features (Calculator, Converter) sharing pure-Kotlin domain objects (ExpressionEvaluator, ConversionEngine), wired together by a Navigation 3 bottom-nav shell.

The highest-risk technical area is the unit converter's bidirectional live-display combined with expression awareness. This is not a UI challenge — it is a state management challenge. The ViewModel must own a single `activeField` truth and derive the passive field's display value rather than allowing both fields to write state back and forth. A second critical risk is floating-point precision: all conversion factors and arithmetic must use `BigDecimal` initialized from `String` literals from the first day the conversion engine is written. Retrofitting precision after UI is wired is painful. Both risks are well-understood and have clear, proven prevention strategies documented in research.

Overall build complexity for this project is low-to-medium. The technology choices are all stable, well-documented, and appropriate to the scope. The project has a clean critical path (domain objects → nav shell → calculator → converter → polish) with no ambiguous integration points. The only area of genuine implementation risk is the bidirectional state loop in the converter, which must be designed correctly on the first pass rather than fixed after the fact.

---

## Key Findings

### Recommended Stack

The stack is Kotlin 2.3.20 + AGP 9.1.0 + Jetpack Compose BOM 2026.03.01 + Material 3 1.4.0, with Navigation 3 1.0.1 for the two-tab shell and ViewModel + StateFlow for state management. All versions are current stable releases as of March 2026 and verified against official release notes. JDK 17 is required by AGP 9.1. The Version Catalog (`libs.versions.toml`) is the standard dependency management approach for new projects. No DI framework, no Room, no Retrofit — the app is offline-only with no persistence requirement in v1.

The expression evaluator should be a custom ~100-line Kotlin infix-to-postfix (Shunting-Yard or recursive descent) parser, not mXparser. mXparser is 500+ functions of overkill, carries a dual-license concern for non-commercial apps, and adds transitive dependency risk. ExprK is abandoned (last commit 2020). A custom parser covers the required grammar (+, -, *, /, parentheses, decimals), is trivially unit-tested, and has zero licensing or dependency overhead.

**Core technologies:**
- Kotlin 2.3.20: language — K2 compiler, Compose compiler integration included
- Jetpack Compose BOM 2026.03.01: UI toolkit — declarative, Material 3, official Android standard for new projects
- compose-material3 1.4.0: components and theming — Material You dynamic color is free with this library
- lifecycle-viewmodel-compose 2.10.0: ViewModel + StateFlow bridge — official Google-recommended state pattern for Compose 2026
- lifecycle-runtime-compose 2.10.0: `collectAsStateWithLifecycle()` — lifecycle-aware flow collection preventing background updates
- navigation3 1.0.1: navigation — Compose-native back stack as developer-owned List; correct choice for a two-tab app
- Custom expression evaluator (no library): arithmetic parser — zero dependency, fully testable, covers +, -, *, /, parentheses
- JUnit 4.13.2 + compose-ui-test-junit4: testing — standard Android test setup; no additional configuration needed

**What NOT to use:** LiveData, Groovy DSL, Hilt/Dagger, ExprK, mXparser, Room, Retrofit, KMP, XML Views.

### Expected Features

The feature scope is well-defined by PROJECT.md and confirmed as correctly scoped by competitive research (ClevCalc, Converter NOW, All-In-One Calculator, HiPER). The app's table stakes are standard; its differentiator — math expressions in unit input fields — is genuinely absent from all mainstream Android converters and justifies the project.

**Must have (table stakes):**
- Basic arithmetic (+, -, ×, ÷) with correct operator precedence — users catch left-to-right evaluation immediately
- Decimal input with double-decimal prevention
- Clear and backspace controls
- Percentage operator
- Full expression display (not just the last number)
- Error display for division by zero / invalid expressions — crash is unacceptable
- Readable number formatting (thousands separator in result)
- Instant live conversion (no button-press required) — delay feels archaic
- Bidirectional unit input — poor reviews for one-directional converters
- 6 unit categories: length, weight, volume, temperature, area, speed
- All required units per category (see PROJECT.md for full list)
- Unit selector UI (dropdown or picker)
- Appropriate decimal precision (4–6 significant figures)

**Should have (differentiators):**
- Math expressions in unit input fields — the defining feature; type `25.4 + 10` in the mm field, get conversion of 35.4
- Live dual-display with expression awareness — partial expressions silently ignored during active typing; error only on focus-out or equals
- Material You dynamic theming — free with Material 3; matches system wallpaper color
- Per-category state retention — switching Length to Weight and back remembers last unit pair and value
- Sensible default unit pairs — mm → in as primary default for Length
- Copy result to clipboard — one-tap workflow accelerator
- Swap units button — saves two dropdown interactions
- Long-press to paste into input field

**Defer to v2+:**
- Currency converter (requires network, API key, rate management — explicitly out of scope)
- Calculation history / memory storage (requires Room, migration risk, no v1 requirement)
- Scientific calculator mode (not in requirements; custom parser should not be stretched)
- Home screen widget (complex Glance library, no v1 use case)
- Custom unit definitions (persistence + edge cases; not needed for fixed unit set)

### Architecture Approach

The architecture is MVVM with Unidirectional Data Flow. Clean Architecture's three-layer separation (domain/data/presentation) is explicitly overkill here — this is a single-user, offline, no-persistence app. The right scope is one ViewModel per screen. State flows down from ViewModels via StateFlow collected with `collectAsStateWithLifecycle()`; events flow up via lambda callbacks. Pure Kotlin domain objects (ExpressionEvaluator, ConversionEngine, UnitType sealed class) have zero Android dependencies and are independently unit-testable. The two feature ViewModels are not shared — Calculator and Converter are independent.

**Major components:**
1. App Shell (`MainActivity`, `AppTheme`, `BottomNavBar`, `AppNavGraph`) — entry point; hosts navigation; applies theming; no business logic
2. Calculator Feature (`CalculatorViewModel`, `CalculatorScreen`, `CalculatorKeypad`, `CalculatorDisplay`) — expression building + evaluation; delegates to ExpressionEvaluator; one-directional data flow
3. Converter Feature (`ConverterViewModel`, `ConverterScreen`, `UnitInputRow`, `CategorySelector`) — bidirectional live-display; expression-aware; delegates to both ExpressionEvaluator and ConversionEngine; activeField guard prevents circular state updates
4. ExpressionEvaluator (pure Kotlin object) — takes a String expression, returns Double; no Android deps; handles precedence via Shunting-Yard or recursive descent
5. ConversionEngine (pure Kotlin object) — stateless `convert(value, from, to): BigDecimal`; hub-and-spoke via toBase factors; temperature handled as special-case affine formula

### Critical Pitfalls

1. **Double/float precision in conversion factors** — `25 mm → in → mm` silently returns `25.000000000000004`. Fix: use `BigDecimal(String)` constants for all factors from day one. Never `BigDecimal(25.4)` — that inherits Double's imprecision. Use explicit scale and `RoundingMode.HALF_UP` on every `.divide()` call.

2. **Bidirectional TextField infinite recomposition loop** — Field A updates ViewModel → ViewModel sets Field B → Field B fires `onValueChange` → loop. Fix: track `activeField` enum in ViewModel. Only the active field's `onValueChange` triggers conversion. The passive field displays derived state, never writes state.

3. **Expression parser integer division** — `5 / 2` evaluates to `2` not `2.5` if the lexer tokenizes literals as Int. Silent wrong answers. Fix: tokenize all numeric literals as Double or BigDecimal in the lexer. Test: `evaluate("1/4")` must return `0.25`.

4. **Temperature not using offset-aware formula** — treating `°F → °C` as a multiplicative factor produces `32°F = 17.78°C` instead of `0°C`. Fix: temperature is a special case — implement `toKelvin(BigDecimal)` and `fromKelvin(BigDecimal)` per variant; do not fold into the generic factor table.

5. **BigDecimal display showing scientific notation or trailing zeros** — `.toString()` on a BigDecimal produces `2.54E+1` or `25.4000000`. Fix: always render via `stripTrailingZeros().toPlainString()`, capped to 10–12 significant figures with `setScale(N, RoundingMode.HALF_UP)` first.

---

## Implications for Roadmap

Based on the combined research, the dependency graph clearly dictates a 4-phase structure. Phases are ordered inside-out: domain objects first (no dependencies, immediately testable), then shell, then simpler feature (Calculator), then the complex feature (Converter), then polish.

### Phase 1: Domain Foundation
**Rationale:** ExpressionEvaluator and ConversionEngine have zero Android dependencies and feed every other component. Building and fully testing them in isolation before any UI exists removes uncertainty from all downstream phases. Pitfalls 1, 2, 3, 6, and 7 must all be addressed here — they cannot be retrofitted after UI is wired.
**Delivers:** Pure-Kotlin domain objects fully covered by unit tests. A passing test matrix for all unit pairs and expression edge cases gives confidence before a single composable is written.
**Addresses:** Custom expression evaluator (arithmetic: +, -, *, /, parentheses), ConversionEngine with BigDecimal hub-and-spoke, UnitType/UnitCategory sealed class hierarchy for all 6 categories, temperature special-case formula.
**Avoids:** Pitfall 1 (Double precision), Pitfall 2 (chained conversion drift), Pitfall 3 (integer division), Pitfall 6 (temperature formula), Pitfall 7 (operator precedence), Pitfall 11 (derived area/speed factors).

### Phase 2: Navigation Shell + Theming
**Rationale:** Once domain objects are proven, establishing the app skeleton before building features prevents having to refactor navigation into existing screen code later. Navigation 3's back stack model needs to be validated early — it is newer and less community-documented than Nav2.
**Delivers:** Working two-tab app with placeholder Calculator and Converter screens, Material 3 dynamic theming applied, edge-to-edge display enabled.
**Uses:** Navigation3 1.0.1, activity-compose 1.12.3, compose-material3 1.4.0, `enableEdgeToEdge()`.
**Implements:** App Shell component.

### Phase 3: Calculator Feature
**Rationale:** Calculator is the simpler feature — one input direction, no bidirectional state, no category switching. It proves the MVVM + StateFlow pattern works in the project and validates ExpressionEvaluator integration end-to-end in a real UI context before tackling the more complex Converter state model.
**Delivers:** Fully functional calculator with expression display, operator precedence, backspace/clear, percentage, error display, and readable number formatting.
**Addresses:** Basic arithmetic table stakes, display of full expression, error handling, thousands separator in result.
**Avoids:** Pitfall 9 (TextField IME filtering — use InputTransformation API, not onValueChange filtering), Pitfall 10 (division by zero crash).

### Phase 4: Converter Feature
**Rationale:** Most complex feature. Depends on ExpressionEvaluator (Phase 1), ConversionEngine (Phase 1), and confirmed MVVM pattern (Phase 3). Bidirectional state management is the highest-risk implementation task in the project — it must be designed correctly on first pass. The activeField guard pattern must be implemented from the start, not added as a fix.
**Delivers:** Live bidirectional unit converter with expression support in input fields, 6 unit categories with per-category state retention, unit selectors, sensible default pairs (mm → in), swap button.
**Implements:** ConverterViewModel with activeField guard, UnitInputRow, CategorySelector.
**Avoids:** Pitfall 4 (circular recomposition), Pitfall 5 (locale decimal separator — normalize comma to period before parsing), Pitfall 8 (BigDecimal display), Pitfall 12 (debounce 50ms on expression evaluation flow).

### Phase 5: Polish and Quality-of-Life
**Rationale:** Low-complexity, high-impact features that layer on top of the working core. None block core functionality but together make the app feel finished.
**Delivers:** Copy to clipboard, long-press paste, Material You dynamic color confirmation, display edge cases (overflow, very small/large values), final spacing and typography pass.
**Addresses:** Moderate differentiators (copy, swap, paste) and display quality (Pitfall 8 display formatting at render layer).

### Phase Ordering Rationale

- Domain objects first because all three other phases depend on them and they are independently testable — this is the lowest-risk way to gain confidence before any UI complexity is added.
- Navigation shell before features because retrofitting Nav3 into existing screen code is harder than building screens into an established shell.
- Calculator before Converter because it is a strict subset of the complexity — one-directional, no expression-in-field UX, simpler state — and validates the MVVM pattern before the harder Converter state model.
- Polish last because it has no dependencies on feature correctness and can be done incrementally without blocking testing of core behavior.
- BigDecimal and activeField guard are both non-negotiable from Phase 1/4 respectively — they cannot be added later without rewrites.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 4 (Converter):** The bidirectional TextField + expression-aware live-display UX state machine has documented community pitfalls but limited canonical examples for this exact combination. When planning Phase 4, research the `TextFieldState` + `InputTransformation` API (stable as of Compose 1.7 / August 2025) and confirm the exact activeField guard implementation pattern with a minimal prototype before committing to the full ConverterViewModel design.
- **Phase 2 (Navigation Shell):** Navigation 3 is stable but newer — less community documentation than Nav2. Confirm the exact `NavDisplay` composable integration with a bottom navigation bar before building feature screens on top of it.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Domain Foundation):** Shunting-Yard algorithm and BigDecimal arithmetic are extremely well-documented. Custom parser is ~100 lines of standard Kotlin.
- **Phase 3 (Calculator):** Standard MVVM calculator pattern with extensive documented examples. No novel integration challenges.
- **Phase 5 (Polish):** Clipboard API, Material You dynamic color, and display formatting are all straightforward Compose/Android APIs.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All versions verified from official release notes and BOM mapping pages. Custom parser recommendation is the one opinionated call (vs. mXparser), but rationale is sound for this scope. |
| Features | HIGH | Competitive analysis across 5+ apps confirms table stakes. Differentiator (expression in input fields) confirmed as absent in mainstream apps. Feature scope per PROJECT.md is well-defined and matches research recommendations. |
| Architecture | HIGH | Official Android docs + confirmed MVVM + UDF pattern. Bidirectional state guard pattern (activeField) is the one implementation-level pattern that needs a prototype to confirm, but the design principle is well-established. |
| Pitfalls | HIGH for floating-point and architecture pitfalls; MEDIUM for Compose-specific edge cases | BigDecimal precision issues are deterministic and well-documented. Circular recomposition pattern is confirmed by multiple community sources. Locale decimal separator behavior depends on IME implementation and may need device testing to confirm. |

**Overall confidence:** HIGH

### Gaps to Address

- **TextFieldState + InputTransformation API for expression input fields:** This is the recommended way to handle input filtering without IME desynchronization (Pitfall 9). It is the newer API (stable Compose 1.7 / August 2025) and has less community documentation than the legacy `value`/`onValueChange` API. Prototype this before committing to the converter TextField implementation.
- **Navigation 3 bottom nav integration:** Nav3's `NavDisplay` with a bottom bar is documented but has fewer real-world examples than Nav2. Build a throwaway prototype during Phase 2 to confirm the tab switching behavior before building feature screens on top of it.
- **BigDecimal display precision strategy for very large / very small values:** The `stripTrailingZeros().toPlainString()` approach is correct for normal conversion values. Edge cases (converting 1 nanometer to miles, or 1 light-year to millimeters) could produce very long strings. Decide on a maximum significant figure cap and overflow display format (e.g., scientific notation for values beyond a threshold) during Phase 1 testing.
- **minSdk 26 vs 21 decision:** Research recommends minSdk 26 as a pragmatic floor for a personal app. If the target device runs Android 8.0+, this is correct. Confirm the actual device's Android version before locking this setting — it has no functional impact on any feature in scope.

---

## Sources

### Primary (HIGH confidence)
- [Compose BOM mapping — Android Developers](https://developer.android.com/jetpack/compose/bom/bom-mapping) — BOM 2026.03.01 confirmed
- [AGP 9.1.0 release notes](https://developer.android.com/build/releases/gradle-plugin) — AGP version and JDK requirement
- [Kotlin 2.3.20 — Kotlin Blog](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/) — latest stable Kotlin
- [Navigation3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) — 1.0.1 stable confirmed
- [Jetpack Navigation 3 is stable — Android Developers Blog](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html) — Nav3 stability and forward direction
- [Compose UI Architecture — Android Developers](https://developer.android.com/develop/ui/compose/architecture) — MVVM + UDF pattern
- [State and Jetpack Compose — Android Developers](https://developer.android.com/develop/ui/compose/state) — state management patterns
- [StateFlow and SharedFlow — Android Developers](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) — StateFlow over LiveData
- [lifecycle-viewmodel-compose — Compose + libraries docs](https://developer.android.com/develop/ui/compose/libraries) — 2.10.0 confirmed
- [Android BigDecimal API Reference](https://developer.android.com/reference/kotlin/android/icu/math/BigDecimal) — precision and rounding
- [Jetpack Compose Performance — Android Developers](https://developer.android.com/develop/ui/compose/performance) — recomposition and performance
- [Migrate to State-Based Text Fields — Android Developers](https://developer.android.com/develop/ui/compose/text/migrate-state-based) — TextFieldState + InputTransformation API

### Secondary (MEDIUM confidence)
- [ClevCalc on Google Play](https://play.google.com/store/apps/details?id=com.dencreak.dlcalculator&hl=en_US) — competitive feature analysis
- [Converter NOW on F-Droid](https://f-droid.org/en/packages/com.ferrarid.converterpro/) — bidirectional converter UX patterns
- [Two-way data binding in Jetpack Compose — ProAndroidDev](https://proandroiddev.com/two-way-data-binding-in-jetpack-compose-1be55c402ec6) — bidirectional state patterns
- [Fixing floating-point arithmetics with Kotlin — Nicolas Frankel](https://blog.frankel.ch/fixing-floating-point-arithmetics-with-kotlin/) — BigDecimal precision strategy
- [BigDecimal for High-Precision Arithmetic in Kotlin — Sling Academy](https://www.slingacademy.com/article/using-bigdecimal-for-high-precision-arithmetic-kotlin/) — BigDecimal usage patterns
- [Floating-point for decimals — Roman Elizarov](https://elizarov.medium.com/floating-point-for-decimals-fc2861898455) — when to use BigDecimal
- [Gotchas in Jetpack Compose Recomposition — Stitch Fix Engineering](https://multithreaded.stitchfix.com/blog/2022/08/05/jetpack-compose-recomposition/) — recomposition loop patterns
- [Effective State Management for TextField in Compose — Medium/Android Developers](https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5) — IME desynchronization patterns
- [Making Arithmetic Parser with Kotlin — Medium/Coding Blocks](https://medium.com/coding-blocks/making-arithmetic-parser-with-kotlin-4097115f5af) — custom parser guidance
- [GitHub — Mather: expression-based calculator + unit converter for Android](https://github.com/icasdri/Mather) — prior art for expression+converter

### Tertiary (reference only)
- [mXparser license](https://mathparser.org/mxparser-license/) — license terms confirmed; reason to avoid for this project
- [ExprK GitHub](https://github.com/Keelar/ExprK) — abandoned status confirmed (last commit 2020)
- [TextField KeyboardType.Number decimal issue — Google Issue Tracker](https://issuetracker.google.com/issues/209835363) — locale IME decimal separator behavior

---
*Research completed: 2026-04-01*
*Ready for roadmap: yes*
