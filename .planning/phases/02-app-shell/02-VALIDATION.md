---
phase: 02
slug: app-shell
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-02
---

# Phase 02 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (already configured from Phase 1) |
| **Config file** | `app/build.gradle.kts` (testImplementation already declared) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest` |
| **Estimated runtime** | ~5 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 5 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | APP-01 | manual | Device/emulator launch | N/A | ⬜ pending |
| 02-01-02 | 01 | 1 | APP-02 | manual | Device/emulator visual check | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. Phase 2 is primarily UI shell work — compilation success is the primary automated verification. Visual behavior requires device/emulator testing.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Dynamic theming responds to wallpaper | APP-01 | Requires device with wallpaper set | Change wallpaper, relaunch app, verify color scheme changes |
| Bottom nav switches tabs | APP-02 | Requires UI interaction | Tap Calculator tab, verify placeholder. Tap Converter tab, verify placeholder. |
| Edge-to-edge display | APP-01 | Visual layout check | Verify content extends behind system bars, no overlap with navigation |
| App launches without crash | APP-01 | Runtime behavior | Install APK, tap icon, verify app opens |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 5s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
