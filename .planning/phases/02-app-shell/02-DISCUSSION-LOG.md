# Phase 2: App Shell - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-02
**Phase:** 02-app-shell
**Areas discussed:** Tab visual style, Placeholder content, Theme color, Navigation pattern
**Mode:** Auto (all recommended defaults selected)

---

## Tab Visual Style

| Option | Description | Selected |
|--------|-------------|----------|
| Icon + label (Material 3 default) | Standard Material 3 NavigationBar with both icon and text label | ✓ |
| Icon only | Compact bottom bar with icons, no labels | |

**User's choice:** Icon + label (auto-selected recommended default)
**Notes:** Material 3 guidelines recommend icon + label for bottom navigation with 2-5 destinations.

---

## Placeholder Content

| Option | Description | Selected |
|--------|-------------|----------|
| Centered label with icon | Simple centered text and icon indicating the tab's purpose | ✓ |
| Empty screen | Completely blank placeholder | |
| Coming soon message | Placeholder with "Coming in Phase N" messaging | |

**User's choice:** Centered label with icon (auto-selected recommended default)
**Notes:** Keeps it simple while making tab switching visually verifiable.

---

## Theme Color

| Option | Description | Selected |
|--------|-------------|----------|
| Dynamic color with purple fallback | Material You wallpaper-based colors, falls back to purple seed on older devices | ✓ |
| Fixed color scheme | Static purple/blue theme, no dynamic color | |
| Dynamic color with blue fallback | Same as above but blue seed | |

**User's choice:** Dynamic color with purple fallback (auto-selected recommended default)
**Notes:** APP-01 requires Material You dynamic theming. Purple aligns with calculator app conventions.

---

## Navigation Pattern

| Option | Description | Selected |
|--------|-------------|----------|
| Navigation3 with owned back stack | Compose-native Navigation3 1.0.1 with List<Any> back stack | ✓ |
| Navigation2 (NavController) | Traditional Jetpack Navigation with NavController | |

**User's choice:** Navigation3 with owned back stack (auto-selected recommended default)
**Notes:** CLAUDE.md specifies Navigation3 1.0.1 as the recommended navigation library.

---

## Claude's Discretion

- Tab icon selection (Material Icons)
- Exact fallback seed color hex
- Scaffold vs manual composition

## Deferred Ideas

None
