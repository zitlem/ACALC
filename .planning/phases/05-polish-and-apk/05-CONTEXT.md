# Phase 5: Polish and APK - Context

**Gathered:** 2026-04-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver three concrete requirements: swap units button (CONV-11), copy result to clipboard (CONV-12), and a verified installable APK (APP-03). Also handle display edge cases in the converter output (very large/small numbers). User has not yet run the app on a device — UAT after this phase will validate all UX choices.

</domain>

<decisions>
## Implementation Decisions

### APK Build
- **D-01:** Build a **debug APK** via `./gradlew assembleDebug` — no keystore setup needed, installs directly via adb for sideloading. Release signing is explicitly out of scope for this phase.
- **D-02:** No network permissions — manifest must remain clean (already verified: no INTERNET permission declared)

### Swap Units (CONV-11)
- **D-03:** Add a swap button that reverses the order of all `rows` in `ConverterState` — this preserves multi-row support and is the natural extension of "flip from/to"
- **D-04:** The active row index should be adjusted or cleared after swap to avoid stale conversion source tracking
- **D-05:** Value preservation: after swapping, recompute all row values using the new row[0] as source (or active row if one exists)
- **D-06:** Placement: one global swap button per converter card (not per-row) — position between the two conversion rows or as a trailing icon in the category header area

### Copy to Clipboard (CONV-12)
- **D-07:** Copy icon (or tap-to-copy) per row — tapping copies that row's displayed value string to the system clipboard
- **D-08:** Visual confirmation via a brief Snackbar ("Copied") — no toast, use Material 3 `SnackbarHost` already available in the scaffold
- **D-09:** Only copy when the row has a non-empty value; the icon should be visually dimmed (disabled) when value is empty

### Number Display Edge Cases
- **D-10:** Cap displayed decimal places at **8 significant digits** after the decimal point — avoids ugly 10+ digit strings from `toPlainString()` for extreme conversions (e.g., mm → miles)
- **D-11:** Use scientific notation (e.g., `1.23e-8`) only when the value is less than `0.000001` or greater than `999,999,999` — otherwise use plain decimal with the 8-digit cap
- **D-12:** The `formatConverted()` helper in `ConverterViewModel` is the single place to update — all rows use it

### Claude's Discretion
- Exact swap button placement and icon (SwapVert vs SwapHoriz vs custom)
- Whether copy icon appears inline in the row or as a trailing action on the TextField
- Snackbar duration (short)
- Whether to add thousands separators to large displayed numbers

</decisions>

<specifics>
## Specific Ideas

- User has not tested the app on a device yet — UX choices above are based on requirements and codebase structure, not live testing feedback. UAT is important for this phase.
- The converter uses a multi-row model (`List<ConverterRow>`) with add/remove row support — swap must work correctly with 2+ rows.
- Inspired by ClevCalc's UX — the original "swap" in ClevCalc is a simple 2-row flip button.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Core Implementation Files
- `app/src/main/kotlin/com/acalc/ui/viewmodel/ConverterViewModel.kt` — `ConverterState`, `ConverterRow`, `formatConverted()`, `onAddRow()`, `onRemoveRow()`
- `app/src/main/kotlin/com/acalc/ui/screens/ConverterScreen.kt` — current UI with multi-row `LazyColumn`, swap button placeholder to add
- `app/src/main/AndroidManifest.xml` — must remain without INTERNET permission
- `app/build.gradle.kts` — build config for assembleDebug

### Domain Layer
- `app/src/main/kotlin/com/acalc/domain/ConversionEngine.kt` — used in `convertBetween()`
- `app/src/main/kotlin/com/acalc/domain/ExpressionEvaluator.kt` — used in `onValueChanged()`

### Requirements
- `REQUIREMENTS.md` — CONV-11 (swap), CONV-12 (copy), APP-03 (APK)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ConverterViewModel.onAddRow()` / `onRemoveRow()` — row management patterns to follow for `onSwapRows()`
- `formatConverted()` in `ConverterViewModel` — single location for display formatting fix
- Material 3 `SnackbarHost` — available in Scaffold from Phase 2 `AppShell.kt`; check if already wired

### Established Patterns
- Stateless inner composable (`ConverterContent`) — swap button and copy icons go in `ConverterContent` params, not the outer `ConverterScreen`
- `collectAsStateWithLifecycle()` for state collection
- `IconButton` with `Icons.Default.*` for UI actions (material-icons-core already a dependency)

### Integration Points
- `AppShell.kt` wraps screens in `Scaffold` — verify `SnackbarHostState` is accessible or needs to be passed down
- `ConverterState.activeRowIndex` — must be updated correctly after swap to avoid stale source-row tracking

</code_context>
