# Roadmap: ACALC

## Overview

ACALC is built inside-out: pure-Kotlin domain objects first (no Android dependencies, immediately testable), then the app shell and navigation, then the simpler Calculator feature to validate the MVVM pattern, then the core value feature — the bidirectional live-display Unit Converter with expression support — and finally a polish pass covering quality-of-life features and APK verification. Each phase delivers a coherent, independently verifiable capability. The converter's bidirectional state management is the highest-risk implementation task and is deliberately deferred until the domain objects and MVVM pattern are proven.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Domain Foundation** - Pure-Kotlin expression evaluator and conversion engine, fully unit-tested before any UI exists
- [x] **Phase 2: App Shell** - Two-tab navigation with Material 3 theming; placeholder screens for Calculator and Converter (completed 2026-04-02)
- [x] **Phase 3: Calculator** - Fully functional arithmetic calculator with expression display, error handling, and number formatting (completed 2026-04-02)
- [ ] **Phase 4: Unit Converter** - Live bidirectional converter with expression-aware input across 6 unit categories
- [ ] **Phase 5: Polish and APK** - Copy to clipboard, swap units, display edge cases, and verified installable APK build

## Phase Details

### Phase 1: Domain Foundation
**Goal**: Pure-Kotlin domain objects are correct, precise, and fully tested before any Android UI is written
**Depends on**: Nothing (first phase)
**Requirements**: (No direct user-facing requirements — this phase is the foundation all other phases depend on)
**Success Criteria** (what must be TRUE):
  1. ExpressionEvaluator correctly evaluates all arithmetic expressions including operator precedence, parentheses, and decimal inputs (e.g., `evaluate("1/4")` returns `0.25`, `evaluate("2 + 3 * 4")` returns `14`)
  2. ConversionEngine converts between all units in all 6 categories with BigDecimal precision (e.g., `25 mm -> in -> mm` round-trips without floating-point drift)
  3. Temperature conversions use the correct offset-aware formula (e.g., `32°F = 0°C`, `0°C = 273.15 K`)
  4. All unit categories (length, weight, volume, temperature, area, speed) are represented with the full unit sets defined in requirements
  5. Unit tests cover all unit pairs and expression edge cases; the test suite passes cleanly
**Plans:** 2/3 plans executed
Plans:
- [x] 01-01-PLAN.md — Android Gradle project skeleton and unit enum types
- [x] 01-02-PLAN.md — ExpressionEvaluator (TDD: custom recursive-descent parser)
- [x] 01-03-PLAN.md — ConversionEngine (TDD: BigDecimal conversion for 6 categories)

### Phase 2: App Shell
**Goal**: Users can launch the app and navigate between two tabs with Material 3 theming applied
**Depends on**: Phase 1
**Requirements**: APP-01, APP-02
**Success Criteria** (what must be TRUE):
  1. App launches without crashing on the target Android device
  2. User can tap the bottom navigation bar to switch between Calculator and Converter tabs; the active tab is visually indicated
  3. Material 3 dynamic theming is applied and the app color scheme responds to the device wallpaper color
  4. Edge-to-edge display is enabled and system bars do not obscure content
**Plans:** 2/2 plans complete
Plans:
- [x] 02-01-PLAN.md — XML theme stubs, AndroidManifest, Material You theming, MainActivity with edge-to-edge
- [x] 02-02-PLAN.md — Navigation3 AppShell with bottom NavigationBar and placeholder screens
**UI hint**: yes

### Phase 3: Calculator
**Goal**: Users can perform arithmetic calculations with a complete, correct, and readable calculator UI
**Depends on**: Phase 2
**Requirements**: CALC-01, CALC-02, CALC-03, CALC-04, CALC-05, CALC-06, CALC-07
**Success Criteria** (what must be TRUE):
  1. User can tap digit and operator keys to build and evaluate arithmetic expressions including +, -, x, / with correct operator precedence
  2. User can input decimal numbers; double-tapping the decimal point does not insert a second decimal
  3. User can press C to clear all input and backspace to delete the last character
  4. User can use the % key and the result is calculated correctly
  5. The display shows the full expression being built (not just the last number), and division by zero shows a readable error message rather than crashing
**Plans:** 2/2 plans complete
Plans:
- [x] 03-01-PLAN.md — CalculatorViewModel with TDD (expression logic, formatting, error handling)
- [x] 03-02-PLAN.md — CalculatorScreen UI (Material 3 button grid, display area, ViewModel wiring)
**UI hint**: yes

### Phase 4: Unit Converter
**Goal**: Users can convert between units in any of 6 categories using live bidirectional input, including math expressions in either field
**Depends on**: Phase 3
**Requirements**: CONV-01, CONV-02, CONV-03, CONV-04, CONV-05, CONV-06, CONV-07, CONV-08, CONV-09, CONV-10, CONV-13
**Success Criteria** (what must be TRUE):
  1. Typing a value in either unit field immediately updates the other field — no button press required; both fields are editable and drive conversion in either direction
  2. User can type a math expression (e.g., `25.4 + 10`) directly into a unit input field and the result is evaluated before conversion is applied
  3. User can select any unit category (Length, Weight, Volume, Temperature, Area, Speed) and all units defined in requirements are available in both unit selectors
  4. Switching between categories and returning restores the previous value and unit pair for that category
  5. Length category defaults to mm and inches as the initial unit pair when first opened
**Plans:** 1/2 plans executed
Plans:
- [x] 04-01-PLAN.md — ConverterViewModel TDD (state model, bidirectional conversion, per-category state, activeField guard)
- [ ] 04-02-PLAN.md — ConverterScreen UI (category tabs, input fields, unit dropdowns, ViewModel wiring)
**UI hint**: yes

### Phase 5: Polish and APK
**Goal**: The app is complete with quality-of-life features, display edge cases handled, and verified as an installable APK
**Depends on**: Phase 4
**Requirements**: CONV-11, CONV-12, APP-03
**Success Criteria** (what must be TRUE):
  1. User can tap a swap button in the converter to exchange the from and to units, with the current value preserved
  2. User can copy the converted result to the clipboard with a single tap and receives visual confirmation
  3. The app builds as a release APK that installs and runs correctly on an Android device via sideloading; no network permissions are declared
**Plans**: TBD
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Domain Foundation | 2/3 | In Progress|  |
| 2. App Shell | 2/2 | Complete   | 2026-04-02 |
| 3. Calculator | 2/2 | Complete   | 2026-04-02 |
| 4. Unit Converter | 1/2 | In Progress|  |
| 5. Polish and APK | 0/? | Not started | - |
