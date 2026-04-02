# Feature Landscape

**Domain:** Android calculator + unit converter app (personal use, sideloaded APK)
**Researched:** 2026-04-01
**Comparable apps:** ClevCalc, Converter NOW, All-In-One Calculator, HiPER Scientific Calculator, RealCalc

---

## Table Stakes

Features users expect from any calculator or unit converter app. Missing means the product feels broken or incomplete before any differentiating work matters.

### Calculator

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Basic arithmetic: +, -, ×, ÷ | Every phone calculator has this | Low | Must handle left-to-right vs operator precedence correctly; users expect standard math order (× before +) |
| Decimal input | Any real-world measurement involves decimals | Low | Prevent double-decimal entry (e.g., "1..5") |
| Clear (C) and backspace | Correction without full reset is essential | Low | All production apps offer both; "C" clears everything, backspace removes last character |
| Percentage operator | Extremely common in everyday calculations | Low | "10% of 200" → 20; behavior must match platform convention |
| Display of full expression | Users need to see what they typed, not just result | Low | Show the running expression in the display area, not just the last number |
| Error handling | Division by zero, invalid expressions should not crash | Low | Show "Error" or "undefined" inline rather than crashing or showing NaN silently |
| Readable number formatting | Large numbers like "1000000" are hard to read | Low | Thousands separator (1,000,000) in the result; omit from input field to keep editing clean |

### Unit Converter

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Instant live conversion | Users expect the result to update as they type, not on button press | Medium | This is now baseline; any delay or button-press requirement feels archaic |
| Bidirectional input | Either field should be editable; editing field B updates field A | Medium | Standard in ClevCalc, Converter NOW, and all well-rated apps; one-directional converters get poor reviews |
| At least 5 unit categories | Length, weight, temperature, volume, area are universal daily needs | Low | Per PROJECT.md, 6 categories are required: length, weight, volume, temperature, area, speed |
| All common units in each category | Users expect mm, cm, m, km, inches, feet, yards, miles in Length | Low | See PROJECT.md for the full required unit list per category |
| Unit selector (dropdown/picker) | How the user chooses which unit to convert from/to | Low | Must be discoverable; hidden selectors cause confusion |
| Decimal precision | Conversion results like 25.4mm = 1 inch need appropriate decimal places | Low | 4–6 significant figures is the norm; not 15 decimal places, not rounded to integers |

---

## Differentiators

Features that separate good apps from commodity ones. Not expected by default, but valued once discovered — these drive retention and word-of-mouth for personal-use tools.

### Core Differentiator (This App's Defining Feature)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Math expressions in unit input fields** | Type "25.4 + 10" in the mm field; the app evaluates the expression then converts the result | Medium | This is the key feature per PROJECT.md. No mainstream Android converter does this. Requires expression parsing before the conversion step. The input field shows the expression; the output field shows the converted result of the evaluated expression. On "=" or focus-out, expression is evaluated. |
| **Live dual-display with expression awareness** | Both fields update in real time; expressions in either field evaluate live as you type | High | Harder to implement than standard dual-display because partial expressions (e.g., "25.4 +") must be handled gracefully without showing errors mid-type. Implementation strategy: parse on change, silently ignore parse errors during active input, only show error state when user is done typing (focus lost or "=" pressed). |

### Strong Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Clean, ad-free UI | Most unit converter apps in the Play Store are ad-heavy; a clean interface is genuinely rare | Low | This is a sideloaded personal app — zero ads is trivially achieved. Material 3 dynamic color (Material You) is a free differentiation bonus. |
| Material You dynamic theming | Matches the system color palette from the wallpaper; feels native | Low | Available for free with `compose-material3`. minSdk 26 and Android 12+ enables full dynamic color; earlier versions get a static fallback. This requires no extra work if the stack is already Material 3. |
| Category navigation with instant context switch | Switching between Length, Weight, etc. should not reset previous inputs | Medium | Remembering state per category (the last two units chosen + last value) is a quality-of-life feature most apps handle poorly. Requires per-category ViewModel state, not a single shared converter state. |
| Sensible default unit pairs | Opening "Length" should default to mm → inches (or the most recently used pair), not some arbitrary pair | Low | ClevCalc does this well. Reduces the number of taps to the most common conversion. For this app, mm → in is the stated primary use case. |

### Moderate Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Copy result to clipboard | One-tap copy of the converted value is a workflow accelerator | Low | Common in ClevCalc and Converter NOW. Useful when passing a measurement to another app. |
| Swap units button | Quickly flip the from/to unit assignment | Low | Single-icon button; saves two dropdown interactions. Expected in good converters, absent in basic ones. |
| Long-press to paste into input | Allows pasting a value from clipboard into a unit field | Low | Converter NOW added this as a notable 2024 update — confirms user demand. |

---

## Anti-Features

Things to deliberately NOT build in v1. These have explicit rationale; revisit in future milestones only if there is clear user demand.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Currency converter | Requires live exchange rate API, adds network permission, introduces failure modes (no internet, stale rates, API key management) | Hard out-of-scope per PROJECT.md. If added later: use a dedicated offline rates database, not a live endpoint |
| Calculation history / memory storage | Adds persistence layer (Room/SQLite), migration risk, UI complexity, and scroll state management — none of which serve the core use case | Keep the display ephemeral. The expression IS the history during a session. Per PROJECT.md: explicitly out of scope for v1 |
| Scientific calculator mode | Trig, log, sqrt, exponentiation are absent from the current requirements and add keypad surface area | Add only if the expression evaluator makes it trivially free. A custom arithmetic parser should not be stretched into a scientific parser. |
| Financial calculators (loan, BMI, tip) | Niche utility with no connection to the core converter feature | Out of scope per PROJECT.md. ClevCalc bundles these because it is a general-purpose app; this app is not. |
| Home screen widget | Complex to implement in Compose (glance-appwidget library), and the primary use case (open app → convert measurement) does not benefit from a widget | Defer indefinitely. No evidence in PROJECT.md that this was considered. |
| Unit customization / custom units | Adds settings UI, persistence, and edge-case handling (circular conversions, missing factors) | Not needed for a known fixed unit set. The required units per PROJECT.md are well-defined. |
| Online sync / cloud backup | No accounts, no cloud per PROJECT.md requirements | Offline-only is a constraint, not a limitation to work around |
| Play Store listing optimizations | In-app review prompts, consent screens, target SDK compliance for store submission | Sideloaded APK only. No store requirements apply. |

---

## Feature Dependencies

```
Basic arithmetic evaluator
    → Math expression support in unit input fields
        → Live dual-display with expression awareness

Unit category state (per-category unit pair + value memory)
    → Category navigation with retained state

Unit selector UI (dropdown / bottom sheet)
    → Bidirectional conversion
        → Swap units button (requires bidirectional to be meaningful)

Clipboard API integration
    → Copy result to clipboard
    → Long-press to paste into input
```

Critical path:

```
Number input + display
    → Basic arithmetic operations
        → Error handling (division by zero, invalid input)
            → Expression evaluator (custom infix parser)
                → Unit conversion engine (conversion factors)
                    → Live dual-display
                        → Expression-aware live dual-display
```

---

## MVP Recommendation

The PROJECT.md active requirements are already a well-scoped MVP. The recommended build order within that scope:

**Build first (foundational, no dependencies):**
1. Custom expression evaluator (arithmetic only: +, -, *, /, parentheses) — testable in isolation, feeds everything else
2. Unit conversion engine — pure data layer, no UI dependencies, individually testable
3. Basic calculator UI — validates the expression evaluator end-to-end with a display

**Build second (core feature):**
4. Unit converter UI with live dual-display — uses conversion engine + bidirectional TextField state
5. Expression support in unit input fields — wires expression evaluator into unit converter inputs
6. Category navigation — tabs or navigation drawer; retain state per category

**Quality-of-life (low complexity, high impact):**
7. Copy result to clipboard
8. Swap units button
9. Sensible default unit pairs (mm → in for Length)
10. Material You dynamic theming (nearly free with Material 3)

**Defer entirely from v1:**
- History / memory
- Scientific mode
- Currency converter
- Widgets
- Custom units

---

## Complexity Notes

**What is genuinely hard about this app's core feature:**

The live dual-display with expression support requires careful UX state management:

1. User types "25.4 +" into the mm field — this is a partial expression. The app must not show an error, must not crash, must not flash a wrong intermediate result. The output (inches) field should either hold its last valid value or show nothing.
2. User types "25.4 + 10" — expression is now valid. Evaluate to 35.4mm, convert to 1.3937 inches. Update inches field.
3. User switches to the inches field and types "2" — the mm field should now show 2 × 25.4 = 50.8. The expression in the mm field is replaced by this computed value (the expression state is cleared when the non-expression field takes focus).

The stateful logic is: "which field is the active input, and does it contain an expression or a bare number?"

**Expression evaluation scope for v1:**
- Operators: +, -, *, / (or × and ÷ as display aliases)
- Parentheses: yes, to support "(25.4 + 10) * 2"
- Unary negation: optional but useful ("-5 cm")
- Scientific notation, sqrt, trig: NO — custom parser does not need these

Per STACK.md, a custom ~80–120 line Kotlin infix-to-postfix evaluator handles this grammar. mXparser is explicitly not recommended for this project.

---

## Sources

- [ClevCalc official site — features](https://clevcalc.com/)
- [ClevCalc how-to guide](https://clevcalc.com/how-to-use)
- [ClevCalc on Google Play](https://play.google.com/store/apps/details?id=com.dencreak.dlcalculator&hl=en_US)
- [Converter NOW on F-Droid](https://f-droid.org/en/packages/com.ferrarid.converterpro/)
- [Converter NOW on Google Play](https://play.google.com/store/apps/details?id=com.ferrarid.converterpro&hl=en)
- [Android Authority — best Android calculator apps](https://www.androidauthority.com/best-android-calculator-apps-577878/)
- [Android Authority — best currency and unit converters](https://www.androidauthority.com/best-currency-and-unit-converters-android-1215169/)
- [MakeUseOf — best free unit conversion apps for Android](https://www.makeuseof.com/best-free-unit-conversion-apps-android/)
- [GitHub — Mather: expression-based calculator + unit converter for Android](https://github.com/icasdri/Mather)
- [mXparser library](https://mathparser.org/)
