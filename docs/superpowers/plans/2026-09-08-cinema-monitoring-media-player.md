# Cinema Monitoring and Internal Media Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evolve RB CineCam 0.5 into a production-oriented 0.6 Alpha with top-anchored HUD, non-recorded framing grids and an internal gallery/video player.

**Architecture:** Keep CameraX/Camera2 capture intact. Add monitoring overlays as Compose-only preview layers so guides are never burned into recorded media. Replace external ACTION_PICK gallery flow with an in-app MediaStore browser and Android Media3 ExoPlayer playback.

**Tech Stack:** Kotlin, Jetpack Compose, CameraX 1.6.2, Camera2 Interop, Android MediaStore, AndroidX Media3 ExoPlayer.

**Spec:** Approved in conversation on 2026-09-08.

## Global Constraints
- No decorative controls: every visible action must work.
- Monitoring overlays must never be recorded into the video file.
- Preserve current working REC, PHOTO, manual controls, focus tap and lens switch.
- Preserve stable alpha signing workflow.
- Landscape-first UI.

---

### Task 1: Anchor HUD and preview layout
- [ ] Move TopHud out of preview overlay into the center-column layout above PreviewView.
- [ ] Keep system safe insets while eliminating camera image above/behind HUD.
- [ ] Verify manual controls remain below preview.

### Task 2: Framing guides
- [ ] Add GuideMode state: OFF, THIRDS, CENTER, SAFE, 1.85, 2.35, 2.39.
- [ ] Add a functional GRID action cycling guide modes.
- [ ] Draw guides with Compose Canvas over PreviewView only.
- [ ] Verify captured video/photo remains clean.

### Task 3: Internal media gallery
- [ ] Query MediaStore for RB CineCam photos/videos.
- [ ] Build in-app thumbnail/list browser.
- [ ] Open selected photos inside app.
- [ ] Open selected videos inside app.

### Task 4: Internal video playback
- [ ] Add Media3 ExoPlayer dependencies.
- [ ] Add VideoPlayerScreen with Play/Pause, seek position/duration and back action.
- [ ] Release player lifecycle resources correctly.
- [ ] Verify recorded MP4 plays without leaving RB CineCam.

### Task 5: Version and verification
- [ ] Bump versionCode to 6 and versionName to 0.6.0-alpha.
- [ ] Run unit tests.
- [ ] Build debug APK with stable alpha signing.
- [ ] Verify GitHub Actions build and artifact upload are successful before delivery.
