# Phase 2: App Shell - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a launchable Android app with two-tab bottom navigation (Calculator and Converter), Material 3 / Material You dynamic theming, and edge-to-edge display. Both tab screens are placeholders — full functionality comes in Phases 3 and 4.

</domain>

<decisions>
## Implementation Decisions

### Tab Navigation
- **D-01:** Bottom navigation bar uses Material 3 `NavigationBar` with icon + label for each tab (Calculator, Converter)
- **D-02:** Active tab is visually indicated via Material 3's built-in `NavigationBarItem` selected state
- **D-03:** Navigation uses Navigation3 1.0.1 with developer-owned `List<Any>` back stack — no NavController ceremony

### Theming
- **D-04:** Material You dynamic color enabled — app theme responds to device wallpaper color
- **D-05:** Fallback seed color is purple (for devices that don't support dynamic color)
- **D-06:** Edge-to-edge display enabled via `enableEdgeToEdge()` in `ComponentActivity`

### Placeholder Screens
- **D-07:** Calculator placeholder shows centered text "Calculator" with a calculator icon
- **D-08:** Converter placeholder shows centered text "Unit Converter" with a conversion icon

### Claude's Discretion
- Icon choices for bottom navigation tabs (Calculator, Converter)
- Exact fallback color hex values
- Whether to use `Scaffold` with `bottomBar` or compose navigation bar manually

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Technology Stack
- `CLAUDE.md` — Full technology stack with version pinning (AGP 9.1.0, Kotlin 2.3.20, Compose BOM 2026.03.01, Navigation3 1.0.1, activity-compose 1.12.3)

### Build Configuration
- `gradle/libs.versions.toml` — Version catalog with all dependency versions
- `app/build.gradle.kts` — App module build config with Compose enabled

### Domain Layer (Phase 1 output)
- `app/src/main/kotlin/com/acalc/domain/UnitCategory.kt` — Unit category enum (6 categories)
- `app/src/main/kotlin/com/acalc/domain/Units.kt` — Unit definitions (35 units)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Domain layer is pure Kotlin with no Android dependencies — clean separation already established
- `UnitCategory` enum provides the category list that the Converter tab will eventually use

### Established Patterns
- Package structure: `com.acalc.domain` for domain logic — UI should follow `com.acalc.ui` pattern
- Kotlin-first, no XML views, no annotation processing

### Integration Points
- `AndroidManifest.xml` exists but has no Activity declared — needs `MainActivity` entry
- `app/build.gradle.kts` already has Compose and Material 3 dependencies configured
- No `Application` class exists yet — may not be needed for this phase

</code_context>

<specifics>
## Specific Ideas

- Inspired by ClevCalc's UX — clean, functional calculator app
- Material You dynamic theming is a core design requirement (APP-01)
- Bottom navigation is the standard Android pattern for 2-5 top-level destinations

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 02-app-shell*
*Context gathered: 2026-04-02 via auto mode*
