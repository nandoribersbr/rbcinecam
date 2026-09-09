# Audio Meter and Focus Controls Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build RB CineCam 0.8 Alpha with a real microphone level monitor and expanded focus controls.

**Architecture:** Add an AudioRecord-based metering module that reports measured PCM RMS/peak values and degrades to unavailable when the device cannot share the microphone. Extend Camera2 focus control with AF-C, AF-S, focus lock and manual lens-distance control. Keep unsupported states out of the UI.

**Tech Stack:** Kotlin, Jetpack Compose, CameraX 1.6.2, Camera2 Interop, Android AudioRecord.

**Spec:** Approved in conversation on 2026-09-08.

## Global Constraints
- No animated/fake audio values.
- Audio meter must reflect samples read from the microphone or report unavailable.
- Focus controls must issue real Camera2/CameraControl requests.
- Preserve current recording, photo, gallery, grid, shutter angle and player behavior.
- Landscape-first UI.

---

### Task 1: Audio metering policy
- [ ] Add tests for PCM-to-dBFS conversion and clipping threshold.
- [ ] Implement pure AudioMeterMath.
- [ ] Verify tests.

### Task 2: Microphone meter
- [ ] Add AudioLevelMonitor using AudioRecord.
- [ ] Expose RMS/peak samples through callback.
- [ ] Handle permission/device capture failure as unavailable.

### Task 3: Focus engine
- [ ] Add focus mode policy tests.
- [ ] Implement AF-C, AF-S, LOCK and manual focus fraction in RBCameraController.
- [ ] Read minimum focus distance to disable unsupported manual focus.

### Task 4: UI
- [ ] Add real L/R meter panel with dBFS labels and clipping indication.
- [ ] Open focus control panel from FOCUS block.
- [ ] Provide AF-C, AF-S, LOCK, MF slider, infinity and macro actions only when supported.

### Task 5: Release
- [ ] Bump to 0.8.0-alpha/versionCode 8.
- [ ] Update Actions artifact name.
- [ ] Run tests and APK build.
- [ ] Deliver only after workflow success.
