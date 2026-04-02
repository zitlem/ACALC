---
phase: 03
slug: calculator
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-02
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (configured in Phase 1) |
| **Config file** | `app/build.gradle.kts` (testImplementation already declared) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.acalc.ui.viewmodel.CalculatorViewModelTest"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~5 seconds |

---

## Sampling Rate

- **After every task commit:** Run quick command
- **After every plan wave:** Run full suite command
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 5 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | CALC-01,02,03,04,05,06,07 | unit | `./gradlew :app:testDebugUnitTest --tests "*.CalculatorViewModelTest"` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 2 | CALC-01,02,03,05 | build | `./gradlew :app:assembleDebug` | ✅ | ⬜ pending |
| 03-02-02 | 02 | 2 | All CALC | manual | Visual verification on emulator | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/kotlin/com/acalc/ui/viewmodel/CalculatorViewModelTest.kt` — test file created as part of TDD task 03-01

*Wave 0 is satisfied by Task 03-01-01 (TDD: tests written first).*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Calculator button grid renders correctly | CALC-01 | Visual layout | Launch app, verify all buttons visible and tappable |
| Expression display updates while typing | CALC-05 | Visual feedback | Tap digits/operators, verify expression shown in display |
| Thousands separators in result | CALC-07 | Visual formatting | Evaluate 1000*1000, verify "1,000,000" displayed |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 5s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
