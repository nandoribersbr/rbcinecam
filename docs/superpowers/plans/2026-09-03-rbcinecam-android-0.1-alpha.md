# RB CineCam Android 0.1 Alpha Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar a primeira versão funcional do RB CineCam para Android com preview real, gravação de vídeo, áudio, seleção de qualidade e controles manuais condicionados às capacidades do aparelho.

**Architecture:** Aplicativo nativo em Kotlin e Jetpack Compose. CameraX gerencia preview, lifecycle e gravação; Camera2 Interop fornece controles de baixo nível quando suportados. Capabilities são isoladas para impedir que a interface ofereça funções inexistentes no hardware.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, Android SDK, Jetpack Compose, CameraX, Camera2 Interop, Media3, JUnit e AndroidX Test.

**Spec:** `docs/superpowers/specs/2026-09-03-rbcinecam-android-design.md`

## Global Constraints

- Plataforma inicial: Android apenas.
- Nome do produto: RB CineCam.
- Versão inicial: 0.1 Alpha.
- Recursos avançados só aparecem quando o dispositivo realmente os suporta.
- RAW Video permanece experimental e condicionado ao hardware.
- LOG nativo e HDR dependem das APIs e capacidades reais do dispositivo.
- O núcleo de captura tem prioridade sobre efeitos visuais e recursos cosméticos.

---

### Task 1: Bootstrap do aplicativo e pipeline de build

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/MainActivity.kt`
- Create: `.github/workflows/build-android.yml`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/AppIdentityTest.kt`

**Interfaces:**
- Produces: app Android instalável, pacote `br.com.rb8.rbcinecam`, workflow `assembleDebug`.

- [ ] Criar teste unitário de identidade da aplicação.
- [ ] Executar teste e confirmar falha antes da implementação.
- [ ] Criar projeto Gradle mínimo e MainActivity Compose.
- [ ] Executar `./gradlew testDebugUnitTest assembleDebug`.
- [ ] Commitar como `feat: bootstrap RB CineCam Android app`.

### Task 2: Modelo de capacidades da câmera

**Files:**
- Create: `app/src/main/java/br/com/rb8/rbcinecam/camera/CameraCapabilities.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/camera/CameraCapabilityRepository.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/camera/CameraCapabilitiesTest.kt`

**Interfaces:**
- Produces: `CameraCapabilities`, `VideoQualityOption`, `FrameRateOption`, `DynamicRangeOption`.

- [ ] Escrever testes para filtragem de 4K, 60 fps, HEVC, HDR, LOG e RAW.
- [ ] Confirmar RED.
- [ ] Implementar modelos imutáveis e regras de disponibilidade.
- [ ] Confirmar GREEN.
- [ ] Commitar como `feat: add device capability model`.

### Task 3: Preview e permissões

**Files:**
- Create: `app/src/main/java/br/com/rb8/rbcinecam/camera/CameraController.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/ui/CameraScreen.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/ui/PermissionGate.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/camera/CameraUiStateTest.kt`

**Interfaces:**
- Produces: `CameraUiState`, `CameraController.bindPreview()`.

- [ ] Escrever teste para estados permission-required, initializing, ready e error.
- [ ] Confirmar RED.
- [ ] Implementar PermissionGate e PreviewView dentro do Compose.
- [ ] Integrar CameraX ao lifecycle.
- [ ] Confirmar testes e build.
- [ ] Commitar como `feat: add live camera preview`.

### Task 4: Gravação de vídeo com áudio

**Files:**
- Create: `app/src/main/java/br/com/rb8/rbcinecam/capture/RecordingController.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/capture/RecordingState.kt`
- Modify: `app/src/main/java/br/com/rb8/rbcinecam/ui/CameraScreen.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/capture/RecordingStateTest.kt`

**Interfaces:**
- Produces: `RecordingState`, `startRecording()`, `stopRecording()`.

- [ ] Escrever testes do ciclo Idle → Preparing → Recording → Finalizing → Idle/Error.
- [ ] Confirmar RED.
- [ ] Implementar VideoCapture/Recorder com MediaStore.
- [ ] Solicitar permissão de áudio e habilitar áudio quando autorizada.
- [ ] Mostrar duração da gravação.
- [ ] Confirmar testes e build.
- [ ] Commitar como `feat: add video recording`.

### Task 5: Qualidade, fps e codec

**Files:**
- Create: `app/src/main/java/br/com/rb8/rbcinecam/capture/CaptureProfile.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/capture/CaptureProfileSelector.kt`
- Modify: `app/src/main/java/br/com/rb8/rbcinecam/ui/CameraScreen.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/capture/CaptureProfileSelectorTest.kt`

**Interfaces:**
- Produces: `CaptureProfile(resolution, fps, codec, dynamicRange)`.

- [ ] Escrever testes para impedir combinações não suportadas.
- [ ] Confirmar RED.
- [ ] Implementar perfis 1080p/4K, 24/30/60 fps e H.264/HEVC conforme capabilities.
- [ ] Confirmar GREEN e build.
- [ ] Commitar como `feat: add capture profile selection`.

### Task 6: Controles manuais via Camera2 Interop

**Files:**
- Create: `app/src/main/java/br/com/rb8/rbcinecam/manual/ManualControlState.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/manual/ManualCameraController.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/manual/ManualControlStateTest.kt`

**Interfaces:**
- Produces: controle de ISO, exposição/shutter, foco, white balance lock e zoom.

- [ ] Escrever testes de clamp para ISO, shutter e foco.
- [ ] Confirmar RED.
- [ ] Implementar validação contra ranges do hardware.
- [ ] Integrar Camera2 Interop somente quando necessário.
- [ ] Confirmar GREEN e build.
- [ ] Commitar como `feat: add manual camera controls`.

### Task 7: Detecção de HDR, LOG e RAW

**Files:**
- Create: `app/src/main/java/br/com/rb8/rbcinecam/color/ColorCapabilityDetector.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/raw/RawCapabilityDetector.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/color/ColorCapabilityDetectorTest.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/raw/RawCapabilityDetectorTest.kt`

**Interfaces:**
- Produces: `ColorCapabilities`, `RawCapabilities`.

- [ ] Escrever testes para HLG10/HDR10/LOG/RAW disponível e indisponível.
- [ ] Confirmar RED.
- [ ] Implementar detecção baseada nas capacidades expostas pelo sistema.
- [ ] Bloquear RAW contínuo quando throughput/capability não for confirmado.
- [ ] Confirmar GREEN e build.
- [ ] Commitar como `feat: detect HDR LOG and RAW capabilities`.

### Task 8: Interface cinematográfica 0.1 Alpha

**Files:**
- Modify: `app/src/main/java/br/com/rb8/rbcinecam/ui/CameraScreen.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/ui/components/StatusBar.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/ui/components/ManualControls.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/ui/components/RecordControls.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/ui/CameraDisplayModelTest.kt`

**Interfaces:**
- Produces: UI principal com resolução, fps, codec, ISO, shutter, WB, foco, REC e status.

- [ ] Escrever teste do display model.
- [ ] Confirmar RED.
- [ ] Implementar interface escura orientada a operação.
- [ ] Bloquear alterações críticas durante gravação.
- [ ] Confirmar GREEN e build.
- [ ] Commitar como `feat: add cinematic capture interface`.

### Task 9: Biblioteca de mídia e validação final

**Files:**
- Create: `app/src/main/java/br/com/rb8/rbcinecam/media/MediaRepository.kt`
- Create: `app/src/main/java/br/com/rb8/rbcinecam/ui/MediaScreen.kt`
- Test: `app/src/test/java/br/com/rb8/rbcinecam/media/MediaItemTest.kt`

**Interfaces:**
- Produces: listagem das gravações criadas pelo RB CineCam.

- [ ] Escrever teste de metadata do item de mídia.
- [ ] Confirmar RED.
- [ ] Implementar consulta ao MediaStore e tela básica de mídia.
- [ ] Executar `./gradlew testDebugUnitTest assembleDebug`.
- [ ] Verificar APK gerado pelo workflow.
- [ ] Commitar como `feat: complete RB CineCam 0.1 Alpha foundation`.
