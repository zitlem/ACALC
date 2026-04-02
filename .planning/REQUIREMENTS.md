# Requirements: ACALC

**Defined:** 2026-04-01
**Core Value:** Unit converter with live dual display — type a value in one unit and see the converted result update in real-time, especially mm/cm to inches

## v1 Requirements

### Calculator

- [ ] **CALC-01**: User can perform basic arithmetic (+, -, x, /)
- [ ] **CALC-02**: User can input decimal numbers
- [ ] **CALC-03**: User can clear all input (C) and backspace last character
- [ ] **CALC-04**: User can calculate percentages
- [ ] **CALC-05**: User can see the full expression while typing (not just last number)
- [ ] **CALC-06**: User sees readable error messages for invalid operations (division by zero)
- [ ] **CALC-07**: Large numbers display with thousands separators

### Unit Converter

- [ ] **CONV-01**: Conversion updates live as user types (no "convert" button)
- [ ] **CONV-02**: Both input fields are editable (bidirectional conversion)
- [ ] **CONV-03**: User can type math expressions in unit input fields (e.g., "25.4 + 10")
- [ ] **CONV-04**: Length conversion (mm, cm, m, km, inches, feet, yards, miles)
- [ ] **CONV-05**: Weight conversion (mg, g, kg, oz, lb, ton)
- [ ] **CONV-06**: Volume conversion (ml, L, tsp, tbsp, cup, fl oz, gallon)
- [ ] **CONV-07**: Temperature conversion (Celsius, Fahrenheit, Kelvin)
- [ ] **CONV-08**: Area conversion (sq mm, sq cm, sq m, sq km, sq in, sq ft, acres)
- [ ] **CONV-09**: Speed conversion (m/s, km/h, mph, knots)
- [ ] **CONV-10**: Switching categories retains previous input state per category
- [ ] **CONV-11**: Swap units button to flip from/to units
- [ ] **CONV-12**: Copy converted result to clipboard
- [ ] **CONV-13**: Sensible default unit pairs per category (mm -> in for length)

### App Shell

- [x] **APP-01**: Material 3 / Material You dynamic theming
- [x] **APP-02**: Bottom navigation between Calculator and Converter
- [ ] **APP-03**: Builds as installable APK (offline, no network permissions)

## v2 Requirements

### Additional Calculators

- **TIP-01**: Tip calculator with bill splitting
- **DISC-01**: Discount calculator with tax
- **CURR-01**: Currency converter with offline rate database

### Enhanced Features

- **HIST-01**: Calculation and conversion history
- **SCI-01**: Scientific calculator mode (trig, log, sqrt)
- **WIDG-01**: Home screen widget for quick conversions

## Out of Scope

| Feature | Reason |
|---------|--------|
| Currency converter | Requires live API, adds network dependency and failure modes |
| Calculation history / memory | Adds persistence layer (Room/SQLite), migration risk, not core use case |
| Scientific calculator mode | Adds keypad complexity, stretches custom parser beyond arithmetic |
| Financial calculators (loan, BMI, tip) | No connection to core converter feature |
| Home screen widget | Complex to implement in Compose, primary use case doesn't benefit |
| Custom units | Adds settings UI, persistence, edge-case handling for known fixed set |
| Online sync / cloud | Offline-only is a constraint, not a limitation |
| Play Store listing | Sideloaded APK only |
| iOS support | Android only |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CALC-01 | Phase 3 | Pending |
| CALC-02 | Phase 3 | Pending |
| CALC-03 | Phase 3 | Pending |
| CALC-04 | Phase 3 | Pending |
| CALC-05 | Phase 3 | Pending |
| CALC-06 | Phase 3 | Pending |
| CALC-07 | Phase 3 | Pending |
| CONV-01 | Phase 4 | Pending |
| CONV-02 | Phase 4 | Pending |
| CONV-03 | Phase 4 | Pending |
| CONV-04 | Phase 4 | Pending |
| CONV-05 | Phase 4 | Pending |
| CONV-06 | Phase 4 | Pending |
| CONV-07 | Phase 4 | Pending |
| CONV-08 | Phase 4 | Pending |
| CONV-09 | Phase 4 | Pending |
| CONV-10 | Phase 4 | Pending |
| CONV-11 | Phase 5 | Pending |
| CONV-12 | Phase 5 | Pending |
| CONV-13 | Phase 4 | Pending |
| APP-01 | Phase 2 | Complete |
| APP-02 | Phase 2 | Complete |
| APP-03 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 23 total
- Mapped to phases: 23
- Unmapped: 0

---
*Requirements defined: 2026-04-01*
*Last updated: 2026-04-01 after roadmap creation*
