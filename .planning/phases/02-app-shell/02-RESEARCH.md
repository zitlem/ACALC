# Phase 02: App Shell - Research

**Researched:** 2026-04-02
**Domain:** Android Jetpack Compose — Navigation3, Material3 theming, edge-to-edge, Activity setup
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Bottom navigation bar uses Material 3 `NavigationBar` with icon + label for each tab (Calculator, Converter)
- **D-02:** Active tab is visually indicated via Material 3's built-in `NavigationBarItem` selected state
- **D-03:** Navigation uses Navigation3 1.0.1 with developer-owned `List<Any>` back stack — no NavController ceremony
- **D-04:** Material You dynamic color enabled — app theme responds to device wallpaper color
- **D-05:** Fallback seed color is purple (for devices that don't support dynamic color)
- **D-06:** Edge-to-edge display enabled via `enableEdgeToEdge()` in `ComponentActivity`
- **D-07:** Calculator placeholder shows centered text "Calculator" with a calculator icon
- **D-08:** Converter placeholder shows centered text "Unit Converter" with a conversion icon

### Claude's Discretion
- Icon choices for bottom navigation tabs (Calculator, Converter)
- Exact fallback color hex values
- Whether to use `Scaffold` with `bottomBar` or compose navigation bar manually

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| APP-01 | Material 3 / Material You dynamic theming | Dynamic color via `dynamicLightColorScheme`/`dynamicDarkColorScheme`, fallback `ColorScheme` from seed, `MaterialTheme` composable setup; res/values/themes.xml stub required |
| APP-02 | Bottom navigation between Calculator and Converter | Navigation3 `NavDisplay` + `entryProvider`, `NavigationBar` + `NavigationBarItem`, `Scaffold` with `bottomBar`, `rememberNavBackStack` |
</phase_requirements>

---

## Summary

Phase 02 constructs the visible skeleton of the ACALC app: a `MainActivity` that launches, renders a Material 3 themed shell with two placeholder screens, and lets the user switch tabs via a bottom navigation bar. The three technical domains are (1) Navigation3 1.0.1 tab navigation, (2) Material You dynamic theming with fallback, and (3) edge-to-edge display setup.

Navigation3 replaces the old NavController/NavHost model. The back stack is a developer-owned `SnapshotStateList<Any>`. For a two-tab app with no deep navigation within tabs, the simplest pattern is **a single back stack whose first entry is the active tab key** — switching tabs replaces the bottom of the stack. For this phase, no nested back stacks are required because neither placeholder screen has sub-navigation.

The most critical non-obvious requirement is the `res/values/themes.xml` stub. Phase 1 deliberately omitted it because Material3 DynamicColors theme reference in the manifest causes an AAPT resource-linking failure unless the XML theme resource exists. Phase 2 MUST create it. The manifest also needs a `MainActivity` declaration with `android:exported="true"` — currently missing entirely.

**Primary recommendation:** Use `Scaffold(bottomBar = { NavigationBar { … } })` with a single `rememberNavBackStack` and `NavDisplay`. This is the minimal, correct approach for a two-tab app with placeholder-only content.

---

## Standard Stack

### Core (all already declared in build.gradle.kts — no new dependencies needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `compose-material3` | 1.4.0 (via BOM 2026.03.01) | `NavigationBar`, `NavigationBarItem`, `Scaffold`, `MaterialTheme` | Official M3 component library; provides built-in selected-state styling |
| `navigation3-runtime` | 1.0.1 | `rememberNavBackStack`, back stack state | Developer-owned back stack; Compose-first |
| `navigation3-ui` | 1.0.1 | `NavDisplay`, `entryProvider` | Renders back stack entries as composables with lifecycle management |
| `activity-compose` | 1.12.3 | `ComponentActivity`, `setContent {}`, `enableEdgeToEdge()` | Required entry point; provides `enableEdgeToEdge()` extension function |
| `lifecycle-viewmodel-compose` | 2.10.0 | `viewModel()` (future use; already declared) | Included for Phase 3 readiness; not actively used in Phase 2 |
| `lifecycle-runtime-compose` | 2.10.0 | `collectAsStateWithLifecycle()` (future use) | Included for Phase 3 readiness |

**No new dependencies are needed for Phase 2.** All required libraries are already in `app/build.gradle.kts`.

### New Resource Files Required

| File | Purpose |
|------|---------|
| `app/src/main/res/values/themes.xml` | Provides `Theme.Acalc` (parent `Theme.Material3.DayNight.NoActionBar`) for manifest `android:theme` reference — required to pass AAPT resource-linking |
| `app/src/main/res/values-night/themes.xml` | Optional but conventional dark-mode variant (can inherit the same parent for now) |

---

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/
├── kotlin/com/acalc/
│   ├── domain/                    # Phase 1 — do not touch
│   │   ├── ConversionEngine.kt
│   │   ├── ExpressionEvaluator.kt
│   │   ├── UnitCategory.kt
│   │   └── Units.kt
│   └── ui/
│       ├── MainActivity.kt        # NEW — Activity entry point
│       ├── AppTheme.kt            # NEW — MaterialTheme composable
│       ├── AppShell.kt            # NEW — Scaffold + NavigationBar + NavDisplay
│       └── screens/
│           ├── CalculatorScreen.kt  # NEW — placeholder
│           └── ConverterScreen.kt   # NEW — placeholder
├── res/
│   └── values/
│       └── themes.xml             # NEW — required for AAPT
│   └── values-night/
│       └── themes.xml             # NEW — optional dark variant
└── AndroidManifest.xml            # MODIFY — add MainActivity declaration
```

### Pattern 1: Navigation3 Single Back Stack Tab Switch

For two top-level tabs with no sub-navigation, one `rememberNavBackStack` is sufficient. Replace the single entry to switch tabs rather than adding entries.

```kotlin
// Source: Navigation3 official docs + nav3-recipes patterns
// In AppShell.kt

sealed interface TabRoute
object CalculatorRoute : TabRoute
object ConverterRoute : TabRoute

@Composable
fun AppShell() {
    val backStack = rememberNavBackStack(CalculatorRoute)
    val currentRoute = backStack.last()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute is CalculatorRoute,
                    onClick = {
                        if (currentRoute !is CalculatorRoute) {
                            backStack.clear()
                            backStack.add(CalculatorRoute)
                        }
                    },
                    icon = { Icon(Icons.Outlined.Calculate, contentDescription = "Calculator") },
                    label = { Text("Calculator") }
                )
                NavigationBarItem(
                    selected = currentRoute is ConverterRoute,
                    onClick = {
                        if (currentRoute !is ConverterRoute) {
                            backStack.clear()
                            backStack.add(ConverterRoute)
                        }
                    },
                    icon = { Icon(Icons.Outlined.Sync, contentDescription = "Converter") },
                    label = { Text("Converter") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {},   // No back action needed for top-level tabs
            entryProvider = entryProvider {
                entry<CalculatorRoute> { CalculatorScreen(Modifier.padding(innerPadding)) }
                entry<ConverterRoute> { ConverterScreen(Modifier.padding(innerPadding)) }
            }
        )
    }
}
```

**entryDecorators note:** For a simple two-tab app with placeholder screens and no ViewModels per tab, the default decorators are sufficient. When Phases 3 and 4 add ViewModels, add `rememberSavedStateNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()`.

### Pattern 2: Material You Dynamic Theming with Fallback

```kotlin
// Source: Android Developers — Material Design 3 in Compose
// In AppTheme.kt

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650A4),        // M3 baseline purple
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
)

@Composable
fun AcalcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

### Pattern 3: MainActivity with Edge-to-Edge

```kotlin
// Source: Android Developers — enableEdgeToEdge()
// In MainActivity.kt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()   // Call BEFORE setContent — sets transparent system bars
        setContent {
            AcalcTheme {
                AppShell()
            }
        }
    }
}
```

### Pattern 4: AndroidManifest.xml — Required Changes

```xml
<!-- MODIFY: app/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="ACALC"
        android:theme="@style/Theme.Acalc"
        android:supportsRtl="true">
        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### Pattern 5: res/values/themes.xml Stub

```xml
<!-- NEW: app/src/main/res/values/themes.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Acalc" parent="Theme.Material3.DayNight.NoActionBar" />
</resources>
```

`Theme.Material3.DayNight.NoActionBar` is provided by `com.google.android.material` which is transitively available via `compose-material3`. This prevents the AAPT resource-linking failure that Phase 1 deferred.

### Anti-Patterns to Avoid

- **Using `rememberNavController()` from Navigation2:** Phase 2 uses Navigation3. The `NavDisplay` API is completely different — no `NavHost` or `composable {}` DSL from Nav2.
- **Calling `enableEdgeToEdge()` after `setContent {}`:** Must be called first or system bar colors will be wrong on first frame.
- **Applying `Modifier.padding(innerPadding)` to `NavDisplay` directly:** Pass `innerPadding` as a parameter into the screen composables. `NavDisplay` itself should fill the full `Scaffold` content area.
- **Omitting `android:exported="true"` on MainActivity:** Required since Android 12 (API 31); build will warn and app will fail to launch on Android 12+ without it.
- **Omitting `res/values/themes.xml`:** The manifest `android:theme` attribute references a style resource. Without the XML file, AAPT fails at link time even for a Compose-only app.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Tab selected visual state | Custom indicator drawing | `NavigationBarItem(selected = …)` | M3 handles active indicator color, ripple, label weight automatically |
| Navigation back stack state | Custom `mutableStateListOf` | `rememberNavBackStack()` | Handles process death, configuration changes, serialization |
| Dark/light system bar icon color | Manual `WindowInsetsController` calls | `enableEdgeToEdge()` | Handles light/dark icon inversion automatically per system theme |
| Inset padding on screen content | Manual `WindowInsets` calculation | `Scaffold` `innerPadding` via `bottomBar` | `Scaffold` applies correct bottom padding matching nav bar height |
| Dynamic color detection | Manual `Build.VERSION.SDK_INT >= 31` check | Keep it — this is the correct idiom; no abstraction needed | Straightforward conditional, no library wrapper needed |

**Key insight:** The Scaffold + NavigationBar + NavDisplay composition handles all layout concerns. The only custom code is tab key definitions and screen content.

---

## Common Pitfalls

### Pitfall 1: AAPT Resource-Linking Failure (Deferred from Phase 1)
**What goes wrong:** Build fails with `AAPT: error: resource style/Theme.Acalc not found` (or similar) even though the app uses Compose exclusively.
**Why it happens:** The `android:theme` attribute in `AndroidManifest.xml` is resolved at build time by the resource linker. It requires a `<style>` element in `res/values/themes.xml`, regardless of whether any View code actually reads the theme.
**How to avoid:** Create `res/values/themes.xml` with `Theme.Acalc` (parent: `Theme.Material3.DayNight.NoActionBar`) before adding `android:theme` to the manifest. This was explicitly noted in STATE.md as deferred to Phase 2.
**Warning signs:** `AAPT2 Error: check logs for details` during the `:app:processDebugResources` Gradle task.

### Pitfall 2: Navigation3 API Mismatch with Documentation Examples
**What goes wrong:** Code copied from older tutorials uses `NavController`, `NavHost`, `composable {}` blocks, or `NavBackStackEntry` — all from Navigation2. These APIs do not exist in Navigation3.
**Why it happens:** Navigation3 is less than a year old (stable November 2025). Most Stack Overflow answers and tutorials still reference Navigation2.
**How to avoid:** Use only `rememberNavBackStack`, `NavDisplay`, `entryProvider { entry<K> { … } }`. Verify against the [nav3-recipes repository](https://github.com/android/nav3-recipes) which is the canonical reference.
**Warning signs:** Import statements referencing `androidx.navigation.compose` (Nav2) instead of `androidx.navigation3`.

### Pitfall 3: Dynamic Color Only on Android 12+
**What goes wrong:** Dynamic color called unconditionally crashes on Android 11 (API 30) and below.
**Why it happens:** `dynamicLightColorScheme()` and `dynamicDarkColorScheme()` require API 31 (Build.VERSION_CODES.S).
**How to avoid:** Gate the call behind `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`. `minSdk = 26` means devices on API 26–30 will hit the fallback path.
**Warning signs:** `java.lang.IllegalStateException` or missing colors at runtime on older API level emulators.

### Pitfall 4: Scaffold innerPadding Not Applied to Screen Content
**What goes wrong:** Screen content is rendered behind the `NavigationBar` at the bottom.
**Why it happens:** `Scaffold` reports the content area via `innerPadding` (a `PaddingValues` parameter to the content lambda). If the screen composable does not consume it, content extends behind the nav bar.
**How to avoid:** Pass `innerPadding` into each screen composable and apply `Modifier.padding(innerPadding)` to the root composable of each screen. For placeholder screens, apply it to the `Box` or `Column` root.
**Warning signs:** Centered content visually appears to be offset upward (the nav bar eats the bottom of the content area).

### Pitfall 5: Navigation3 1.1.0 vs 1.0.1 API Differences
**What goes wrong:** Tutorials published after March 2026 may reference `sceneStrategies` (list parameter) from Nav3 1.1.0-rc01, which differs from the 1.0.1 API (`sceneStrategy` singular).
**Why it happens:** Navigation3 1.1.0 introduced a breaking change to `NavDisplay` parameters.
**How to avoid:** The project pins `navigation3 = "1.0.1"` in `libs.versions.toml`. Use the 1.0.1 API surface. Do not upgrade to 1.1.0 during this phase.
**Warning signs:** Compilation error referencing unknown parameter `sceneStrategies` on `NavDisplay`.

---

## Code Examples

### Complete AppShell with Scaffold

```kotlin
// Pattern-verified against nav3-recipes multiplestacks recipe
// Simplified for two top-level tabs with no sub-navigation

@Composable
fun AppShell() {
    val backStack = rememberNavBackStack(CalculatorRoute)
    val currentRoute = backStack.last()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute is CalculatorRoute,
                    onClick = {
                        if (currentRoute !is CalculatorRoute) {
                            backStack.clear()
                            backStack.add(CalculatorRoute)
                        }
                    },
                    icon = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
                    label = { Text("Calculator") }
                )
                NavigationBarItem(
                    selected = currentRoute is ConverterRoute,
                    onClick = {
                        if (currentRoute !is ConverterRoute) {
                            backStack.clear()
                            backStack.add(ConverterRoute)
                        }
                    },
                    icon = { Icon(Icons.Outlined.SyncAlt, contentDescription = null) },
                    label = { Text("Converter") }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {},
            entryProvider = entryProvider {
                entry<CalculatorRoute> {
                    CalculatorScreen(modifier = Modifier.padding(innerPadding))
                }
                entry<ConverterRoute> {
                    ConverterScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        )
    }
}
```

### Placeholder Screen Pattern

```kotlin
// CalculatorScreen.kt — placeholder only; replaced in Phase 3
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Calculate,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Calculator", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
```

### Icon Candidates (Claude's Discretion)

| Tab | Icon (Material Symbols / Icons.Outlined) | Notes |
|-----|----------------------------------------|-------|
| Calculator | `Icons.Outlined.Calculate` | Available in `material-icons-extended` or as a vector asset |
| Converter | `Icons.Outlined.SyncAlt` or `Icons.Outlined.Autorenew` | `SyncAlt` conveys bidirectionality; matches converter concept |

**Note:** `Icons.Outlined.Calculate` and `Icons.Outlined.SyncAlt` are in the extended icon set. The standard `compose-material3` BOM does NOT include the extended icons. Options:
1. Add `androidx.compose.material:material-icons-extended` dependency (large ~5MB increase)
2. Use basic icons from the core set: `Icons.Default.Home` / `Icons.Default.SwapHoriz`
3. Add only the specific icons as local vector drawable XML assets in `res/drawable/`

**Recommendation:** Use option 2 (basic icons) for Phase 2 placeholder. The exact icon choice is Claude's discretion and can be refined later. Avoid adding the full extended icon set for a two-icon use case.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Navigation2 `NavController` + `NavHost` | Navigation3 `rememberNavBackStack` + `NavDisplay` | Stable Nov 2025 | No NavController; back stack is a plain `List<Any>` |
| `WindowCompat.enableEdgeToEdge(window)` | `enableEdgeToEdge()` extension on `ComponentActivity` | activity-compose 1.9+ | Simpler call; same behavior |
| Manual `DynamicColors.applyToActivitiesIfAvailable` (Views) | `dynamicLightColorScheme(context)` in Compose | M3 Compose stable | No Application class needed for dynamic color in Compose |
| `android:theme="@style/Theme.AppCompat.NoActionBar"` | `android:theme="@style/Theme.Material3.DayNight.NoActionBar"` | Material3 stable | Use M3 parent to avoid AppCompat conflicts |

**Deprecated/outdated:**
- `rememberNavController()`: Navigation2 API — do not use in this project
- `LiveData`: superseded by StateFlow (CLAUDE.md directive)
- `AppCompat` theme as manifest parent: use `Theme.Material3.DayNight.NoActionBar` instead

---

## Open Questions

1. **Icon set for bottom navigation tabs**
   - What we know: `Icons.Outlined.Calculate` and `Icons.Outlined.SyncAlt` are semantically correct but require `material-icons-extended`
   - What's unclear: Whether the extended icon set binary size increase is acceptable for a personal APK
   - Recommendation: Use basic bundled icons (`Icons.Default.Home`, `Icons.Default.Loop`, or similar) for Phase 2. Can upgrade icons in Phase 5 when APK size tuning is evaluated.

2. **Navigation3 `onBack` parameter behavior for top-level tabs**
   - What we know: `NavDisplay` takes `onBack: () -> Unit`. For a single-entry back stack, pressing back would pop the last item.
   - What's unclear: Whether an empty `onBack = {}` is safe or whether `LocalOnBackPressedDispatcherOwner` intercepts back before `onBack` is called.
   - Recommendation: Pass `onBack = {}` and verify back press behavior on device. For top-level tabs, back should exit the app — standard Android behavior if the Activity handles it.

---

## Environment Availability

Step 2.6: SKIPPED (no new external dependencies identified — all libraries already declared in build.gradle.kts, and Android SDK/Gradle toolchain verified working by Phase 1 successful build).

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 (unit tests), Compose UI Test (instrumented) |
| Config file | No separate config — test runner configured via AGP |
| Quick run command | `./gradlew :app:testDebugUnitTest` |
| Full suite command | `./gradlew :app:connectedDebugAndroidTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| APP-01 | Material You dynamic color applied at runtime | Manual smoke test (device) | — | N/A — visual validation |
| APP-01 | Fallback color scheme used on API < 31 | Unit test on `AcalcTheme` logic | `./gradlew :app:testDebugUnitTest` | ❌ Wave 0 |
| APP-02 | Clicking Converter tab shows ConverterScreen | Compose UI instrumented test | `./gradlew :app:connectedDebugAndroidTest` | ❌ Wave 0 |
| APP-02 | Clicking Calculator tab shows CalculatorScreen | Compose UI instrumented test | `./gradlew :app:connectedDebugAndroidTest` | ❌ Wave 0 |
| APP-02 | Active tab item visually selected | Compose UI instrumented test — `assert(isSelected)` | `./gradlew :app:connectedDebugAndroidTest` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest` (unit tests, ~5s)
- **Per wave merge:** `./gradlew :app:connectedDebugAndroidTest` (requires connected device/emulator)
- **Phase gate:** Manual APK install + smoke test on target device before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/kotlin/com/acalc/ui/AcalcThemeTest.kt` — covers APP-01 fallback branch unit test
- [ ] `app/src/androidTest/kotlin/com/acalc/ui/AppShellTest.kt` — covers APP-02 tab switching instrumented tests
- [ ] `app/src/androidTest/kotlin/com/acalc/ui/MainActivityTest.kt` — covers launch-without-crash validation

---

## Project Constraints (from CLAUDE.md)

Directives that constrain this phase:

| Directive | Applies to Phase 2 |
|-----------|--------------------|
| Kotlin + Jetpack Compose only — no XML Views | All UI must be Compose composables |
| Material 3 / Material You theming | `MaterialTheme` with dynamic color; `Theme.Material3` parent in themes.xml |
| Fully offline — no network calls | No network-related APIs anywhere in this phase |
| Navigation3 1.0.1 | Pin to 1.0.1; do not use Nav2 `NavController` |
| ViewModel + StateFlow, not LiveData | Phase 2 has no ViewModel yet; applies from Phase 3 onward |
| No Hilt/Dagger | Manual ViewModel construction when needed |
| No Room/SQLite | No persistence in this phase |
| No mXparser | Not relevant to this phase |
| Custom parser | Not relevant to this phase |
| Kotlin DSL build scripts | Already in place; do not add Groovy DSL |
| AGP 9.1.0 — `kotlinOptions` removed | Do not add `kotlinOptions` block to build.gradle.kts |
| Compose plugin must be explicit (`kotlin.compose`) | Already applied in both build files |

---

## Sources

### Primary (HIGH confidence)
- Android Developers — Material Design 3 in Compose: `dynamicLightColorScheme`, `dynamicDarkColorScheme`, `MaterialTheme` setup
- Android Developers — Navigation 3 release page: 1.0.1 stable, 1.1.0-rc01 breaking change to `sceneStrategies`
- Android Developers — Window insets in Compose: `Scaffold` + `innerPadding` + `WindowInsets` patterns
- Android Developers — Navigation Bar component: `NavigationBar`, `NavigationBarItem`, `selected` parameter
- STATE.md (project): "android:theme omitted from Phase 1 manifest — Material3 DynamicColors theme causes AAPT resource linking failure without res/values stub; to be added in Phase 2"

### Secondary (MEDIUM confidence)
- nav3-recipes GitHub repository (android/nav3-recipes): `multiplestacks` recipe pattern for `NavigationBar` + `NavDisplay` wiring
- Medium / Muhammad Irfan Ali: Bottom navigation + Navigation3 code patterns, `entryDecorators` list usage
- Android Developers — enableEdgeToEdge guide: `WindowCompat.enableEdgeToEdge(window)` call placement before `setContent {}`

### Tertiary (LOW confidence — needs validation at implementation time)
- Navigation3 `onBack = {}` behavior for top-level tab back-stack — not explicitly documented for the single-entry case; verify at implementation

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already in build.gradle.kts, versions verified in Phase 1
- Architecture: HIGH — Navigation3 patterns verified against official nav3-recipes and release docs
- Material You theming: HIGH — official Android Developers documentation, standard pattern
- Edge-to-edge: HIGH — `enableEdgeToEdge()` official API, well-documented
- themes.xml requirement: HIGH — explicitly documented in STATE.md as a known Phase 1 deferral
- `onBack` behavior for top-level tabs: LOW — implementation detail not explicitly covered in Nav3 docs for single-entry stacks

**Research date:** 2026-04-02
**Valid until:** 2026-05-02 (Navigation3 is stable; unlikely to change in 30 days; 1.1.0-rc01 is pre-release)
