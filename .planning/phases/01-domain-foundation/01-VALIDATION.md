---
phase: 1
slug: domain-foundation
status: final
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-01
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 |
| **Config file** | app/build.gradle.kts (testImplementation) |
| **Quick run command** | `./gradlew testDebugUnitTest --tests "com.acalc.*"` |
| **Full suite command** | `./gradlew testDebugUnitTest` |
| **Estimated runtime** | ~5 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest --tests "com.acalc.*"`
- **After every plan wave:** Run `./gradlew testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 10 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 1 | Foundation | build | `./gradlew :app:testDebugUnitTest` | N/A (build check) | pending |
| 1-01-02 | 01 | 1 | Foundation | build | `./gradlew :app:compileDebugKotlin` | N/A (build check) | pending |
| 1-02-01 | 02 | 2 | ExpressionEvaluator | unit | `./gradlew testDebugUnitTest --tests "*.ExpressionEvaluatorTest"` | W0 | pending |
| 1-02-02 | 02 | 2 | ExpressionEvaluator | unit | `./gradlew testDebugUnitTest --tests "*.ExpressionEvaluatorTest"` | W0 | pending |
| 1-03-01 | 03 | 2 | ConversionEngine | unit | `./gradlew testDebugUnitTest --tests "*.ConversionEngineTest"` | W0 | pending |
| 1-03-02 | 03 | 2 | ConversionEngine | unit | `./gradlew testDebugUnitTest --tests "*.ConversionEngineTest"` | W0 | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/kotlin/com/acalc/domain/ExpressionEvaluatorTest.kt` — created by Plan 02 Task 1 (RED phase)
- [ ] `app/src/test/kotlin/com/acalc/domain/ConversionEngineTest.kt` — created by Plan 03 Task 1 (RED phase)
- [ ] JUnit 4.13.2 dependency in build.gradle.kts — created by Plan 01 Task 1

*Wave 0 creates the Android project skeleton including test infrastructure.*

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 10s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved
