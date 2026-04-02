<!-- GSD:project-start source:PROJECT.md -->
## Project

**ACALC**

An Android calculator app inspired by ClevCalc. It combines a standard calculator with a comprehensive unit converter featuring live dual-display conversion. The app is for personal use — a functional APK that can be installed directly on an Android phone.

**Core Value:** The unit converter with live dual display — type a value in one unit and see the converted result update in real-time, especially mm/cm to inches conversion.

### Constraints

- **Platform**: Android only (Kotlin + Jetpack Compose)
- **Distribution**: Sideloaded APK (no Play Store requirements)
- **Connectivity**: Fully offline — no network calls needed
- **Design**: Material 3 / Material You theming
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

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
### Expression Evaluation
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| `org.mariuszgromada.math.mxparser:MathParser.org-mXparser` | 6.1.0 | Evaluate math strings in unit input fields | Supports evaluating expressions like `"25.4 + 10"` directly to a `Double`. Actively maintained (6.1.0 released February 2025). Explicitly supports Kotlin and Android. License is dual: free for non-commercial personal use (which applies here — this is a sideloaded personal app). No restrictions on binary redistribution for non-commercial apps; just retain the copyright notice. |
- **ExprK** (Keelar/ExprK): Last commit October 2020, no releases published. Abandoned — do not use.
- **Custom shunting-yard parser**: Viable for the four arithmetic operators (+, -, *, /) plus parentheses, which is the only requirement here. A custom ~100-line parser avoids the mXparser dependency entirely. This is a legitimate trade-off: zero dependency vs. well-tested library. **Recommendation: write a small custom parser** (basic arithmetic only: +, -, *, /, parentheses, decimal points). mXparser brings 500+ math functions; none are needed. A bespoke parser removes the dual-license concern entirely and has no transitive dependency risk.
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
## Alternatives Considered
| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Dependency injection | None (manual) | Hilt | Hilt adds build-time annotation processing complexity. This app has 1–2 ViewModels and no multi-module setup. Manual construction via `viewModel { CalculatorViewModel() }` is sufficient. |
| State management | ViewModel + StateFlow | MVI library (Orbit, MVI Kotlin) | Extra indirection for a simple app. Pure ViewModel + StateFlow is the Google-recommended baseline and is sufficient here. |
| Navigation | Navigation3 1.0.1 | Navigation2 (Nav Compose 2.9.7) | Nav3 is Compose-native and the forward direction. Nav2 still works but is not recommended for new Compose projects. |
| Expression parser | Custom Kotlin parser | mXparser 6.1.0 | mXparser is overkill (500+ functions) for basic arithmetic. Custom parser is smaller, zero-dependency, trivially testable. |
| Build script language | Kotlin DSL (.kts) | Groovy DSL | Kotlin DSL is the default since Gradle 8.2 and gives compile-time safety in build scripts. |
| UI toolkit | Jetpack Compose | XML Views | XML Views are legacy on new Android projects. Compose + Material 3 is the standard for all new Android development. |
## Full Dependency Block (build.gradle.kts)
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
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
