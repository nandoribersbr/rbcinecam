# RB CineCam Large Format Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o modo RB Large Format Experimental com LF 1.90:1 e LF 1.43:1 usando somente capacidades reais da câmera.

**Architecture:** O modo será uma política sobre o novo estado de captura da v0.15, mantendo separação entre configuração solicitada e aplicada. O controlador tentará a melhor qualidade compatível, priorizará 24 fps/180°, poderá remover ImageAnalysis antes de reduzir qualidade e manterá rollback transacional.

**Tech Stack:** Kotlin, Android CameraX 1.6.2, Camera2 Interop, Jetpack Compose, JUnit.

**Spec:** `docs/superpowers/specs/2026-09-09-rbcinecam-large-format-design.md`

## Global Constraints
- Android somente.
- Nome do recurso: RB Large Format Experimental.
- Formatos: LF 1.90:1 e LF 1.43:1.
- Não usar IMAX como nome comercial.
- Não anunciar capacidades inexistentes.
- UI deve refletir apenas configuração aplicada.
- Priorizar estabilidade e rollback sem crash.
- Corrigir primeiro a infraestrutura qualidade/FPS da v0.15.

---

### Task 1: Política Large Format
**Files:**
- Create: `app/src/main/java/br/com/rb8digital/rbcinecam/camera/LargeFormatPolicy.kt`
- Test: `app/src/test/java/br/com/rb8digital/rbcinecam/camera/LargeFormatPolicyTest.kt`

- [ ] Escrever testes falhando para LF 1.90/LF 1.43, prioridade 24 fps e seleção de qualidade.
- [ ] Executar testes e confirmar RED.
- [ ] Implementar política mínima.
- [ ] Executar testes e confirmar GREEN.
- [ ] Commit.

### Task 2: Reconfiguração transacional
**Files:**
- Modify: `app/src/main/java/br/com/rb8digital/rbcinecam/camera/RBCameraController.kt`
- Modify: `app/src/main/java/br/com/rb8digital/rbcinecam/camera/CaptureConfiguration.kt`
- Test: `app/src/test/java/br/com/rb8digital/rbcinecam/camera/CaptureConfigurationTest.kt`

- [ ] Testar requested/applied/fallback.
- [ ] Implementar tentativa da configuração solicitada sem alterar estado aplicado antecipadamente.
- [ ] Tentar mesma qualidade sem ImageAnalysis antes de reduzir qualidade.
- [ ] Restaurar configuração anterior se todas as tentativas falharem.
- [ ] Commit.

### Task 3: Framing Large Format
**Files:**
- Modify: tela Compose ativa de câmera.
- Test: política de aspect/framing existente ou novo teste dedicado.

- [ ] Adicionar LF 1.90 e LF 1.43 à política de framing.
- [ ] Manter framing separado da resolução codificada.
- [ ] Mostrar estado LF aplicado no HUD.
- [ ] Commit.

### Task 4: UI e controles
**Files:**
- Modify: tela Compose ativa de câmera.

- [ ] Adicionar seletor LARGE FORMAT.
- [ ] Adicionar LF 1.90/LF 1.43.
- [ ] Exibir qualidade/FPS/shutter/perfil realmente aplicados.
- [ ] Manter controles compactos/translúcidos sobre preview full-screen.
- [ ] Commit.

### Task 5: Integração com auditoria 0.15
**Files:**
- Modify: controlador e workflow.
- Remove/deactivate: patches cumulativos antigos após consolidação na fonte.

- [ ] Consolidar comportamento efetivo dos patches na fonte.
- [ ] Remover mutações de build que reescrevem controlador/UI.
- [ ] Definir versão 0.15.0-alpha e versionCode 16.
- [ ] Nomear artifact `RB-CineCam-0.15-Alpha-APK`.
- [ ] Commit.

### Task 6: Verificação
- [ ] Executar testes unitários no GitHub Actions.
- [ ] Executar assembleDebug.
- [ ] Corrigir qualquer falha até CI verde.
- [ ] Confirmar artifact oficial 0.15.
- [ ] Baixar e verificar APK antes da entrega.
