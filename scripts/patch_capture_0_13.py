from pathlib import Path

# --- Camera engine: safe async rebind with fallback ---
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/camera/RBCameraController.kt')
s = p.read_text(encoding='utf-8')

s = s.replace(
'''        quality: Quality = activeQuality,
        onScopeFrame: ((ScopeFrame) -> Unit)? = null
    ) {''',
'''        quality: Quality = activeQuality,
        onScopeFrame: ((ScopeFrame) -> Unit)? = null,
        onBindError: ((Throwable) -> Unit)? = null,
        onBound: (() -> Unit)? = null
    ) {''',
1)

s = s.replace(
'''        providerFuture.addListener({
            val provider = providerFuture.get()
''',
'''        providerFuture.addListener({
            try {
            val provider = providerFuture.get()
''',
1)

s = s.replace(
'''            resetManualState()
            applyManualControls()
        }, ContextCompat.getMainExecutor(context))
''',
'''            resetManualState()
            applyManualControls()
            onBound?.invoke()
            } catch (t: Throwable) {
                onBindError?.invoke(t)
            }
        }, ContextCompat.getMainExecutor(context))
''',
1)

anchor = '''    fun cycleQuality(owner: LifecycleOwner, previewView: PreviewView): String {
        if (recording != null) return activeQualityLabel()
        val next = CaptureSettingsPolicy.nextQuality(activeQualityLabel(), supportedQualityLabels())
        activeQuality = when (next) {
            "4K" -> Quality.UHD
            "HD" -> Quality.HD
            else -> Quality.FHD
        }
        bind(owner, previewView, activeQuality, scopeCallback)
        return activeQualityLabel()
    }
'''
replacement = '''    fun cycleQuality(owner: LifecycleOwner, previewView: PreviewView): String {
        if (recording != null) return activeQualityLabel()
        val next = CaptureSettingsPolicy.nextQuality(activeQualityLabel(), supportedQualityLabels())
        activeQuality = when (next) {
            "4K" -> Quality.UHD
            "HD" -> Quality.HD
            else -> Quality.FHD
        }
        activeFrameRate = CameraReconfigurePolicy.safeFpsAfterQualityChange(activeFrameRate, supportedFps())
        bind(owner, previewView, activeQuality, scopeCallback)
        return activeQualityLabel()
    }

    fun cycleQualitySafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {
        if (recording != null) { onResult(activeQualityLabel(), true); return }
        val supported = supportedQualityLabels()
        val previousQuality = activeQuality
        val previousFps = activeFrameRate
        val requestedLabel = CaptureSettingsPolicy.nextQuality(activeQualityLabel(), supported)
        val requestedQuality = when (requestedLabel) {
            "4K" -> Quality.UHD
            "HD" -> Quality.HD
            else -> Quality.FHD
        }
        activeQuality = requestedQuality
        activeFrameRate = CameraReconfigurePolicy.safeFpsAfterQualityChange(previousFps, supportedFps())
        bind(owner, previewView, activeQuality, scopeCallback,
            onBound = { onResult(activeQualityLabel(), true) },
            onBindError = {
                val fallbackLabel = CameraReconfigurePolicy.fallbackQuality(requestedLabel, supported)
                activeQuality = when (fallbackLabel) { "4K" -> Quality.UHD; "HD" -> Quality.HD; else -> Quality.FHD }
                activeFrameRate = CameraReconfigurePolicy.safeFpsAfterQualityChange(previousFps, supportedFps())
                bind(owner, previewView, activeQuality, scopeCallback,
                    onBound = { onResult(activeQualityLabel(), false) },
                    onBindError = {
                        activeQuality = previousQuality
                        activeFrameRate = previousFps
                        bind(owner, previewView, activeQuality, scopeCallback)
                        onResult(activeQualityLabel(), false)
                    })
            })
    }

    fun cycleFpsSafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {
        if (recording != null) { onResult("${activeFrameRate} FPS", true); return }
        val previous = activeFrameRate
        activeFrameRate = CaptureSettingsPolicy.nextFps(activeFrameRate, supportedFps())
        bind(owner, previewView, activeQuality, scopeCallback,
            onBound = { onResult("${activeFrameRate} FPS", true) },
            onBindError = {
                activeFrameRate = previous
                bind(owner, previewView, activeQuality, scopeCallback)
                onResult("${activeFrameRate} FPS", false)
            })
    }
'''
if anchor not in s:
    raise SystemExit('cycleQuality marker not found')
s = s.replace(anchor, replacement, 1)
p.write_text(s, encoding='utf-8')

# --- UI: nothing interactive overlays the camera preview ---
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/ui/CameraScreen09.kt')
s = p.read_text(encoding='utf-8')

# Place capture/tool controls in their own strip, above preview.
marker = '''                    Row(Modifier.weight(1f).fillMaxWidth()) {
'''
insert = '''                    CaptureBar13(
                        quality = qualityLabel,
                        fps = fpsLabel,
                        aspect = aspectLabel,
                        scope = scopeMode,
                        guide = guide,
                        enabled = !recording,
                        onQuality = {
                            preview?.let { pv ->
                                status = "AJUSTANDO QUADRO..."
                                controller.cycleQualitySafe(owner, pv) { label, exact ->
                                    qualityLabel = label
                                    fpsLabel = controller.activeFps().toString()
                                    status = if (exact) "QUADRO $label" else "QUADRO $label • FALLBACK SEGURO"
                                }
                            }
                        },
                        onFps = {
                            preview?.let { pv ->
                                status = "AJUSTANDO FPS..."
                                controller.cycleFpsSafe(owner, pv) { label, exact ->
                                    fpsLabel = label.substringBefore(" ")
                                    status = if (exact) "FPS $fpsLabel" else "FPS $fpsLabel • COMBINAÇÃO INCOMPATÍVEL"
                                }
                            }
                        },
                        onAspect = { aspectLabel = CaptureSettingsPolicy.nextAspect(aspectLabel); status = "ASPECT $aspectLabel" },
                        onScope = { scopeMode = ScopeMode09.entries[(scopeMode.ordinal + 1) % ScopeMode09.entries.size] },
                        onGuide = { guide = GuideMode.entries[(guide.ordinal + 1) % GuideMode.entries.size] }
                    )
                    Row(Modifier.weight(1f).fillMaxWidth()) {
'''
if marker not in s:
    raise SystemExit('camera row marker not found')
s = s.replace(marker, insert, 1)

# Remove top-right controls added by 0.12 from the preview.
old_tools = '''                            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tool09("${qualityLabel}") { preview?.let { qualityLabel = controller.cycleQuality(owner, it); status = "QUADRO $qualityLabel" } }
                                Tool09("${fpsLabel} FPS") { preview?.let { fpsLabel = controller.cycleFps(owner, it).substringBefore(" "); status = "FPS $fpsLabel" } }
                                Tool09("${aspectLabel}") { aspectLabel = CaptureSettingsPolicy.nextAspect(aspectLabel); status = "ASPECT $aspectLabel" }
                                Tool09("SCOPES ${scopeMode.label}") { scopeMode = ScopeMode09.entries[(scopeMode.ordinal + 1) % ScopeMode09.entries.size] }
                                Tool09("GRID ${guide.label}") { guide = GuideMode.entries[(guide.ordinal + 1) % GuideMode.entries.size] }
                            }
'''
if old_tools not in s:
    raise SystemExit('0.12 overlay tools marker not found')
s = s.replace(old_tools, '', 1)

# Remove scopes and focus controls from over the image.
s = s.replace('''                            if (scopeMode != ScopeMode09.OFF) ScopePanel09(scopeFrame, scopeMode, Modifier.align(Alignment.BottomStart).padding(10.dp))
''', '', 1)
focus_block = '''                            if (focusPanel) FocusPanel09(
                                current = focus,
                                manualSupported = manualFocusAvailable,
                                value = manualFocus,
                                onAfC = { focus = controller.focusContinuous(); focusPanel = false; status = "FOCO $focus" },
                                onAfS = { preview?.let { focus = controller.focusSingle(it) }; focusPanel = false; status = "FOCO $focus" },
                                onLock = { preview?.let { focus = controller.focusLock(it) }; focusPanel = false; status = "FOCO $focus" },
                                onManual = { manualFocus = it; focus = controller.setManualFocus(it); status = "FOCO $focus" },
                                onInfinity = { manualFocus = 0f; focus = controller.setManualFocus(0f) },
                                onMacro = { manualFocus = 1f; focus = controller.setManualFocus(1f) },
                                onClose = { focusPanel = false },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                            )
'''
if focus_block not in s:
    raise SystemExit('focus overlay marker not found')
s = s.replace(focus_block, '', 1)

# Render optional panels in dedicated strips below the camera row, never over preview.
marker = '''                        AudioStrip09(audio, recording, recordingDb, audioPermissionGranted)
                    }
                    Manual09(iso, shutter, wb, focus, ev, !recording,
'''
replacement = '''                        AudioStrip09(audio, recording, recordingDb, audioPermissionGranted)
                    }
                    if (scopeMode != ScopeMode09.OFF) {
                        Row(Modifier.fillMaxWidth().height(132.dp).background(P09).padding(horizontal = 8.dp, vertical = 6.dp)) {
                            ScopePanel09(scopeFrame, scopeMode)
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (focusPanel) FocusPanel09(
                        current = focus,
                        manualSupported = manualFocusAvailable,
                        value = manualFocus,
                        onAfC = { focus = controller.focusContinuous(); focusPanel = false; status = "FOCO $focus" },
                        onAfS = { preview?.let { focus = controller.focusSingle(it) }; focusPanel = false; status = "FOCO $focus" },
                        onLock = { preview?.let { focus = controller.focusLock(it) }; focusPanel = false; status = "FOCO $focus" },
                        onManual = { manualFocus = it; focus = controller.setManualFocus(it); status = "FOCO $focus" },
                        onInfinity = { manualFocus = 0f; focus = controller.setManualFocus(0f) },
                        onMacro = { manualFocus = 1f; focus = controller.setManualFocus(1f) },
                        onClose = { focusPanel = false },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Manual09(iso, shutter, wb, focus, ev, !recording,
'''
if marker not in s:
    raise SystemExit('post-preview marker not found')
s = s.replace(marker, replacement, 1)

# Add dedicated capture bar composable.
anchor = '@Composable private fun AudioStrip09('
bar = '''@Composable private fun CaptureBar13(
    quality:String,
    fps:String,
    aspect:String,
    scope:ScopeMode09,
    guide:GuideMode,
    enabled:Boolean,
    onQuality:()->Unit,
    onFps:()->Unit,
    onAspect:()->Unit,
    onScope:()->Unit,
    onGuide:()->Unit
) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).background(P09).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CaptureChip13("QUADRO", quality, enabled, onQuality, Modifier.weight(1f))
        CaptureChip13("FPS", fps, enabled, onFps, Modifier.weight(1f))
        CaptureChip13("ASPECT", aspect, enabled, onAspect, Modifier.weight(1f))
        CaptureChip13("SCOPES", scope.label, true, onScope, Modifier.weight(1f))
        CaptureChip13("GRID", guide.label, true, onGuide, Modifier.weight(1f))
    }
}

@Composable private fun CaptureChip13(label:String,value:String,enabled:Boolean,onClick:()->Unit,modifier:Modifier=Modifier) {
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(7.dp)).background(PS09)
            .border(1.dp, Color(0xFF34383E), RoundedCornerShape(7.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = Color.Gray, fontSize = 7.sp, maxLines = 1)
        Text(value, color = if (enabled) A09 else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

'''
if anchor not in s:
    raise SystemExit('AudioStrip anchor not found')
s = s.replace(anchor, bar + anchor, 1)
p.write_text(s, encoding='utf-8')

# --- Build identity ---
b = Path('app/build.gradle.kts')
g = b.read_text(encoding='utf-8')
g = g.replace('versionCode = 13', 'versionCode = 14')
g = g.replace('versionName = "0.12.0-alpha"', 'versionName = "0.13.0-alpha"')
b.write_text(g, encoding='utf-8')

print('RB CineCam 0.13 clean preview and safe reconfiguration patch applied')
