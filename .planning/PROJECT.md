# ACALC

## What This Is

An Android calculator app inspired by ClevCalc. It combines a standard calculator with a comprehensive unit converter featuring live dual-display conversion. The app is for personal use — a functional APK that can be installed directly on an Android phone.

## Core Value

The unit converter with live dual display — type a value in one unit and see the converted result update in real-time, especially mm/cm to inches conversion.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Standard calculator with basic arithmetic operations (+, -, x, /)
- [ ] Unit converter with live dual display (type in one, see the other update instantly)
- [ ] Math expressions in unit input fields (e.g., type "25.4 + 10" in mm field, evaluates then converts)
- [ ] Length conversion (mm, cm, m, km, inches, feet, yards, miles)
- [ ] Weight/mass conversion (mg, g, kg, oz, lb, ton)
- [ ] Volume conversion (ml, L, tsp, tbsp, cup, fl oz, gallon)
- [ ] Temperature conversion (Celsius, Fahrenheit, Kelvin)
- [ ] Area conversion (sq mm, sq cm, sq m, sq km, sq in, sq ft, acres)
- [ ] Speed conversion (m/s, km/h, mph, knots)
- [ ] Clean Material 3 design following Android conventions
- [ ] Buildable as installable APK

### Out of Scope

- Tip calculator — not needed for v1
- Discount calculator — not needed for v1
- Currency converter — requires live API, adds complexity
- GPA/BMI/Fuel/Loan calculators — not needed for v1
- Play Store listing/publishing — just needs to work as APK
- iOS support — Android only
- History/memory storage — keep it simple for v1

## Context

- Inspired by ClevCalc's UX, particularly the live dual-display conversion approach
- The mm/cm to inches conversion is the most frequently used feature
- This is a personal-use tool — no accounts, no cloud, no analytics needed
- Kotlin + Jetpack Compose is the modern Android-native approach with Material 3

## Constraints

- **Platform**: Android only (Kotlin + Jetpack Compose)
- **Distribution**: Sideloaded APK (no Play Store requirements)
- **Connectivity**: Fully offline — no network calls needed
- **Design**: Material 3 / Material You theming

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Kotlin + Jetpack Compose | Modern Android-native, best performance, Material 3 built-in | — Pending |
| No currency converter in v1 | Requires live API and adds connectivity dependency | — Pending |
| Offline-only | Calculator and unit conversion don't need network | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? -> Move to Out of Scope with reason
2. Requirements validated? -> Move to Validated with phase reference
3. New requirements emerged? -> Add to Active
4. Decisions to log? -> Add to Key Decisions
5. "What This Is" still accurate? -> Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-01 after initialization*
