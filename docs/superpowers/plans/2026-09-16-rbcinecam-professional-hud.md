# RB CineCam Professional Functional HUD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the approved professional full-screen HUD to RB CineCam without introducing any dead controls.

**Architecture:** Preserve the existing CameraX capture engine and transactional configuration work from 0.16. Recompose the HUD around a safe-area top technical bar and compact translucent edge controls; capability-driven state determines which options are enabled and applied-state confirmation drives labels.

**Tech Stack:** Android, Kotlin, CameraX, existing RB CineCam patch/build pipeline, JUnit, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-16-rbcinecam-professional-hud-design.md`

## Global Constraints
- No visible control without a real implementation.
- Preview must remain full-bleed.
- Top technical bar must respect safe area and avoid clipping on narrow screens.
- FHD/4K/FPS labels must represent applied capture state, not requested state.
- Failed camera reconfiguration must roll back without crash.
- Preserve LF 1.90/LF 1.43, audio meters, gallery playback, grid, scopes and manual controls already implemented.

---

### Task 1: Capability-driven top technical bar
**Files:**
- Modify: existing HUD/layout patch source used by the workflow
- Test: add/update HUD contract tests

- [ ] Write failing tests asserting resolution, FPS, aspect, scope and grid controls live in the safe top bar and no duplicate floating row remains.
- [ ] Run tests and confirm failure.
- [ ] Implement compact top bar with responsive spacing and safe-area padding.
- [ ] Run tests and confirm pass.
- [ ] Commit.

### Task 2: Applied-state capture controls
**Files:**
- Modify: capture configuration/rebind implementation introduced in 0.16
- Test: transactional capture tests

- [ ] Add failing tests for supported-option filtering and applied-state-only HUD updates.
- [ ] Run tests and confirm failure.
- [ ] Connect resolution/FPS/aspect controls to active-camera capabilities and transactional rebind.
- [ ] Ensure rollback preserves previous HUD state.
- [ ] Run tests and confirm pass.
- [ ] Commit.

### Task 3: Compact edge and bottom controls
**Files:**
- Modify: HUD/layout patch source
- Test: visual contract/static layout tests

- [ ] Add failing assertions for compact translucent ISO/shutter/WB/focus/EV controls and reduced right-side footprint.
- [ ] Implement compact controls without covering central framing area.
- [ ] Make audio monitor collapsible and scopes hidden by default.
- [ ] Run tests and confirm pass.
- [ ] Commit.

### Task 4: Dead-control release gate
**Files:**
- Modify: CI verification script/workflow
- Test: verification contract

- [ ] Add checks preventing mock-only LUT/peaking/manual-focus controls from appearing without handlers.
- [ ] Verify every visible top/bottom/edge control maps to an implemented action.
- [ ] Run verification and unit tests.
- [ ] Commit.

### Task 5: Build and package candidate
**Files:**
- Modify: version/build metadata as required
- Test: complete CI

- [ ] Run unit tests.
- [ ] Build APK.
- [ ] Verify artifact upload succeeds.
- [ ] Inspect final workflow status and artifact metadata.
- [ ] Package the candidate APK for physical S24 FE validation.
