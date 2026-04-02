# Technology Stack

**Project:** ACALC — Android Calculator + Unit Converter
**Researched:** 2026-04-01
**Domain:** Native Android app, offline-only, personal sideloaded APK

---

## Recommended Stack

### Build System

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Android Gradle Plugin (AGP) | 9.1.0 | Build orchestration | Latest stable (March 2026). Includes built-in Kotlin support — no need to apply `kotlin-android` plugin separately. |
| Gradle | 9.3.1 | Build runner | Required minimum for AGP 9.1.0. Kotlin DSL is the default since Gradle 8.2; no reason to use Groovy DSL on a new project. |
| Kotlin | 2.3.20 | Language | Latest stable (March 2026). AGP 9.1 includes Kotlin support; use 2.3.x for access to K2 compiler improvements and Compose compiler integration. |
| JDK | 17 | Runtime | AGP 9.1 requires JDK 17 minimum. Android Studio bundles it. |
| Version Catalog (`libs.versions.toml`) | — | Dependency management | Standard practice for new projects. Keeps all versions in one file; Gradle generates type-safe accessors. |

### Core Framework

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Jetpack Compose BOM | 2026.03.01 | Compose library versioning | Use the BOM to pin all Compose libraries to a tested, compatible set. BOM 2026.03.01 is the latest stable. Declare BOM once; omit versions from individual Compose dependencies. |
| `compose-ui` | 1.10.6 (via BOM) | UI rendering | Included via BOM. |
| `compose-foundation` | 1.10.6 (via BOM) | Layout primitives | Included via BOM. |
| `compose-material3` | 1.4.0 (via BOM) | Material Design 3 components | Latest stable. Provides `Button`, `Card`, `TextField`, `NavigationBar`, dynamic color (Material You). This is the right choice for a clean Material 3 UI on a personal Android app. |
| `compose-ui-tooling-preview` | 1.10.6 (via BOM) | @Preview support | Dev-time only. Essential for fast UI iteration. |
| `androidx.activity:activity-compose` | 1.12.3 | Entry point for Compose in Activity | Latest stable (January 2026). Provides `ComponentActivity`, `setContent {}`, and `enableEdgeToEdge()`. |

### Architecture

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.10.0 | ViewModel + Compose bridge | Latest stable. Provides `viewModel()` composable. Use ViewModel + `StateFlow` to hold screen state; this is the official Google-recommended pattern for Compose apps in 2026. Do not use LiveData on a new Compose project — StateFlow integrates directly with `collectAsStateWithLifecycle()`. |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.10.0 (same group) | `collectAsStateWithLifecycle()` | Lifecycle-aware flow collection in Compose; prevents UI updates when the app is in the background. |

### Navigation

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `androidx.navigation3:navigation3-runtime` | 1.0.1 | Navigation runtime | Navigation 3 is the Compose-first, stable successor to Nav2 (stable since November 2025, 1.0.1 released February 2026). The back stack is a developer-owned `List<Any>` — cleaner state model, no NavController ceremony. For a two-screen app (Calculator + Unit Converter tabs), this is an appropriate lightweight choice. |
| `androidx.navigation3:navigation3-ui` | 1.0.1 | NavDisplay composable | Handles rendering the current back stack entry. |

**Alternative considered:** Nav2 (`androidx.navigation:navigation-compose:2.9.7`). Nav2 is still maintained and familiar, but Nav3 is Google's official forward direction for Compose-first apps. Given this is a new project with no existing Nav2 investment, start with Nav3.

**Note for this app specifically:** The app has two top-level destinations (Calculator, Unit Converter) with sub-destinations (unit category selection). The navigation graph is small. If Nav3 adds friction during implementation, Nav2 is a safe fallback — the migration is documented.

### Expression Evaluation

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `org.mariuszgromada.math.mxparser:MathParser.org-mXparser` | 6.1.0 | Evaluate math strings in unit input fields | Supports evaluating expressions like `"25.4 + 10"` directly to a `Double`. Actively maintained (6.1.0 released February 2025). Explicitly supports Kotlin and Android. License is dual: free for non-commercial personal use (which applies here — this is a sideloaded personal app). No restrictions on binary redistribution for non-commercial apps; just retain the copyright notice. |

**Alternatives considered:**

- **ExprK** (Keelar/ExprK): Last commit October 2020, no releases published. Abandoned — do not use.
- **Custom shunting-yard parser**: Viable for the four arithmetic operators (+, -, *, /) plus parentheses, which is the only requirement here. A custom ~100-line parser avoids the mXparser dependency entirely. This is a legitimate trade-off: zero dependency vs. well-tested library. **Recommendation: write a small custom parser** (basic arithmetic only: +, -, *, /, parentheses, decimal points). mXparser brings 500+ math functions; none are needed. A bespoke parser removes the dual-license concern entirely and has no transitive dependency risk.

**Final recommendation on expression evaluation:** Write a custom infix-to-postfix evaluator. The expression grammar required (`number (op number)*` with optional parentheses) is small and well-understood. This is ~80–120 lines of Kotlin, removes all dependency risk, and is straightforward to test. If more math functions are needed later, add mXparser then.

### Testing

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `junit:junit` | 4.13.2 | Unit tests | Standard. JUnit 5 requires additional setup; JUnit 4 works out of the box with the Android test runner. |
| `androidx.compose.ui:ui-test-junit4` | via BOM | Compose UI tests | Official Compose testing library. Use `ComposeTestRule` to test composables. |
| `androidx.compose.ui:ui-test-manifest` | via BOM | Test manifest | Provides the test activity for Compose UI tests. Debug dependency only. |

### SDK Targets

| Setting | Value | Rationale |
|---------|-------|-----------|
| `compileSdk` | 36 | AGP 9.1 supports up to API 36.1. Compile against the latest to get new API access. |
| `targetSdk` | 35 | Android 15. Google Play now requires apps to target API 35. For a sideloaded APK this is not enforced, but targeting 35 ensures predictable behavior on modern devices. |
| `minSdk` | 26 | Android 8.0 (Oreo). Covers ~98%+ of active devices in 2026. API 26 provides `java.time` APIs without `desugaring`, `TextDirectionHeuristic` improvements, and is the recommended floor for apps heavily using Compose and Material 3. Compose itself requires minSdk 21, but 26 is a better pragmatic floor for a personal app that will only run on the developer's phone. |

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Dependency injection | None (manual) | Hilt | Hilt adds build-time annotation processing complexity. This app has 1–2 ViewModels and no multi-module setup. Manual construction via `viewModel { CalculatorViewModel() }` is sufficient. |
| State management | ViewModel + StateFlow | MVI library (Orbit, MVI Kotlin) | Extra indirection for a simple app. Pure ViewModel + StateFlow is the Google-recommended baseline and is sufficient here. |
| Navigation | Navigation3 1.0.1 | Navigation2 (Nav Compose 2.9.7) | Nav3 is Compose-native and the forward direction. Nav2 still works but is not recommended for new Compose projects. |
| Expression parser | Custom Kotlin parser | mXparser 6.1.0 | mXparser is overkill (500+ functions) for basic arithmetic. Custom parser is smaller, zero-dependency, trivially testable. |
| Build script language | Kotlin DSL (.kts) | Groovy DSL | Kotlin DSL is the default since Gradle 8.2 and gives compile-time safety in build scripts. |
| UI toolkit | Jetpack Compose | XML Views | XML Views are legacy on new Android projects. Compose + Material 3 is the standard for all new Android development. |

---

## Full Dependency Block (build.gradle.kts)

```kotlin
// libs.versions.toml
[versions]
agp = "9.1.0"
kotlin = "2.3.20"
composeBom = "2026.03.01"
activityCompose = "1.12.3"
lifecycle = "2.10.0"
navigation3 = "1.0.1"
junit = "4.13.2"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    // Note: with AGP 9.x, kotlin-android may be bundled; verify during setup.
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        targetSdk = 35
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
```

---

## What NOT to Use

| Library | Reason to Avoid |
|---------|-----------------|
| **LiveData** | Superseded by StateFlow for new Compose projects. StateFlow integrates directly via `collectAsStateWithLifecycle()`. |
| **Groovy DSL** (build.gradle) | Kotlin DSL is the default and gives type-safe build scripts. No reason to use Groovy on a new project. |
| **Hilt / Dagger** | Annotation processing overhead is unjustified for a single-module app with 2 ViewModels. Manual ViewModel construction is sufficient. |
| **ExprK** | Abandoned (last commit 2020, no releases). Do not add as a dependency. |
| **mXparser** | License requires tracking for non-commercial use; the library is far larger than needed. Use a custom parser instead. |
| **Room / SQLite** | No persistence requirement in v1 (no history, no memory storage). Adding Room adds a migration burden for zero current benefit. |
| **Retrofit / OkHttp** | App is fully offline. No network library needed. Currency converter is explicitly out of scope for v1. |
| **Kotlin Multiplatform (KMP)** | App is Android-only by requirement. KMP adds toolchain complexity for no benefit here. |
| **XML Views** | Legacy approach. Do not mix XML layouts with Compose in this project. |

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Compose BOM version | HIGH | Verified directly from official BOM mapping page (2026.03.01 confirmed) |
| AGP 9.1.0 + Kotlin 2.3.20 | HIGH | Verified from official AGP release notes and Kotlin blog |
| Navigation3 1.0.1 stable | HIGH | Verified from official androidx.navigation3 release page |
| lifecycle-viewmodel-compose 2.10.0 | HIGH | Verified from official Compose + libraries docs |
| activity-compose 1.12.3 | HIGH | Verified from Maven/official release notes |
| Custom parser over mXparser | MEDIUM | Custom parser recommendation is opinionated; mXparser is a valid alternative but the license and size make it suboptimal for this scope |
| Navigation3 over Navigation2 | MEDIUM | Nav3 is officially stable and recommended by Google for new Compose apps, but Nav2 remains actively maintained and more widely documented in community tutorials |
| minSdk 26 | MEDIUM | No hard requirement; minSdk 21 works with Compose, but 26 is a pragmatic floor for a personal app on a modern device |

---

## Sources

- [Compose BOM mapping — Android Developers](https://developer.android.com/jetpack/compose/bom/bom-mapping) — BOM 2026.03.01
- [Compose Material 3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3) — 1.4.0 stable
- [AGP 9.1.0 release notes](https://developer.android.com/build/releases/gradle-plugin)
- [Kotlin 2.3.20 released — Kotlin Blog](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/)
- [Navigation3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) — 1.0.1 stable
- [Jetpack Navigation 3 is stable — Android Developers Blog](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html)
- [lifecycle-viewmodel-compose — Compose + libraries docs](https://developer.android.com/develop/ui/compose/libraries)
- [activity-compose Maven Repository](https://mvnrepository.com/artifact/androidx.activity/activity-compose)
- [mXparser license](https://mathparser.org/mxparser-license/)
- [ExprK GitHub — last commit 2020, abandoned](https://github.com/Keelar/ExprK)
- [StateFlow and SharedFlow — Android Developers](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [Compose December '25 release — Android Developers Blog](https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html)
