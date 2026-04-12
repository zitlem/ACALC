# ACALC

An Android calculator app combining a scientific calculator with a comprehensive unit converter. Inspired by ClevCalc.

## Download

Grab the latest APK from [Releases](https://github.com/zitlem/ACALC/releases).

## Features

### Calculator
- Basic arithmetic: `+` `−` `×` `÷`
- Smart parentheses toggle `( )`
- Percent key
- Scientific functions via `•••` menu:
  - Constants: π, e, φ
  - Power: `^`
  - Roots: `√`, `³√`
  - Absolute value: `|x|`
  - Logarithms: `log`, `ln`, `log₂`
  - Trig: `sin`, `cos`, `tan` (degrees)
  - Inverse trig: `sin⁻¹`, `cos⁻¹`, `tan⁻¹`
  - Hyperbolic: `sinh`, `cosh`, `tanh`, `sinh⁻¹`, `cosh⁻¹`, `tanh⁻¹`
- Calculation history

### Unit Converter
13 categories with live conversion across all rows:

| Category | Units |
|----------|-------|
| **Triangle** | Right triangle & any triangle solver with visual diagram |
| **Length** | mm, cm, m, km, in, ft, yd, mi |
| **Weight** | mg, g, kg, oz, lb, metric ton |
| **Volume** | ml, L, tsp, tbsp, cup, fl oz, gallon |
| **Temperature** | °C, °F, K |
| **Area** | mm², cm², m², km², in², ft², acre, hectare |
| **Speed** | m/s, km/h, mph, knot, Mach |
| **Time** | ms, s, min, hr, day, week, month, year |
| **Force** | N, kN, dyne, gf, kgf, lbf, poundal |
| **Pressure** | Pa, hPa, kPa, MPa, bar, mbar, atm, psi, mmHg, inHg, torr |
| **Energy** | J, kJ, MJ, cal, kcal, Wh, kWh, BTU |
| **Power** | mW, W, kW, MW, HP, PS |
| **Angle** | degree, radian, gradian |
| **Data** | bit, byte, KB, MB, GB, TB, PB |

### Triangle Calculator
- **Right Triangle** — solve from any 2 of: sides a/b/c or angles A°/B°
- **Any Triangle** — solve SSS, SAS, ASA, AAS via law of cosines + law of sines
- Visual Canvas diagram with labeled sides, angles, angle arcs, and right-angle marker

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** ViewModel + StateFlow
- **Navigation:** Navigation 3
- **Build:** AGP 9.1 / Gradle 9.3 / Kotlin 2.3

## Requirements

- Android 8.0+ (API 26)
- Sideload via APK (no Play Store)
