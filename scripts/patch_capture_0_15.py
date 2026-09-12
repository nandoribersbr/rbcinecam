from pathlib import Path

# Controller hardening after 0.14 effective patches.
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/camera/RBCameraController.kt')
s = p.read_text(encoding='utf-8')

s = s.replace('    private var scopeCallback: ((ScopeFrame) -> Unit)? = null\n', '    private var scopeCallback: ((ScopeFrame) -> Unit)? = null\n    private var scopesEnabled = false\n')
s = s.replace('''            val analysis = scopeCallback?.let { callback ->
''', '''            val analysis = if (scopesEnabled) scopeCallback?.let { callback ->
''')
s = s.replace('''                    .also { it.setAnalyzer(analysisExecutor, ScopeAnalyzer(callback)) }
            }
''', '''                    .also { it.setAnalyzer(analysisExecutor, ScopeAnalyzer(callback)) }
            } else null
''', 1)

# Do not wipe manual settings every time quality/FPS is rebound.
s = s.replace('''            resetManualState()
            applyManualControls()
''', '''            applyManualControls()
''')

old_quality = '''    fun cycleQualitySafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {
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
'''
new_quality = '''    fun cycleQualitySafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {
        if (recording != null) { onResult(activeQualityLabel(), true); return }
        val supported = supportedQualityLabels()
        val previousQuality = activeQuality
        val previousFps = activeFrameRate
        val previousScopes = scopesEnabled
        val requestedLabel = CaptureSettingsPolicy.nextQuality(activeQualityLabel(), supported)
        val requestedQuality = when (requestedLabel) {
            "4K" -> Quality.UHD
            "HD" -> Quality.HD
            else -> Quality.FHD
        }
        activeQuality = requestedQuality
        activeFrameRate = CameraReconfigurePolicy.safeFpsAfterQualityChange(previousFps, supportedFps())

        fun rollback() {
            activeQuality = previousQuality
            activeFrameRate = previousFps
            scopesEnabled = previousScopes
            bind(owner, previewView, activeQuality, scopeCallback)
            onResult(activeQualityLabel(), false)
        }

        fun fallbackQuality() {
            val fallbackLabel = CameraReconfigurePolicy.fallbackQuality(requestedLabel, supported)
            activeQuality = when (fallbackLabel) { "4K" -> Quality.UHD; "HD" -> Quality.HD; else -> Quality.FHD }
            activeFrameRate = CameraReconfigurePolicy.safeFpsAfterQualityChange(previousFps, supportedFps())
            bind(owner, previewView, activeQuality, scopeCallback,
                onBound = { onResult(activeQualityLabel(), false) },
                onBindError = { rollback() })
        }

        bind(owner, previewView, activeQuality, scopeCallback,
            onBound = { onResult(activeQualityLabel(), true) },
            onBindError = {
                if (scopesEnabled) {
                    scopesEnabled = false
                    activeQuality = requestedQuality
                    bind(owner, previewView, activeQuality, scopeCallback,
                        onBound = { onResult(activeQualityLabel(), true) },
                        onBindError = { fallbackQuality() })
                } else fallbackQuality()
            })
    }
'''
if old_quality not in s:
    raise SystemExit('0.13 quality function marker not found')
s = s.replace(old_quality, new_quality, 1)

insert_anchor = '    fun cycleFpsSafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {'
scopes_fn = '''    fun setScopesEnabled(owner: LifecycleOwner, previewView: PreviewView, enabled: Boolean, onResult: (Boolean) -> Unit) {
        if (recording != null) { onResult(scopesEnabled); return }
        val previous = scopesEnabled
        scopesEnabled = enabled
        bind(owner, previewView, activeQuality, scopeCallback,
            onBound = { onResult(scopesEnabled) },
            onBindError = {
                scopesEnabled = previous
                bind(owner, previewView, activeQuality, scopeCallback)
                onResult(scopesEnabled)
            })
    }

    fun scopesEnabled(): Boolean = scopesEnabled

'''
if insert_anchor not in s:
    raise SystemExit('fps function anchor not found')
s = s.replace(insert_anchor, scopes_fn + insert_anchor, 1)
p.write_text(s, encoding='utf-8')

# UI: scopes start OFF, toggle real ImageAnalysis, Large Format ratios, version identity.
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/ui/CameraScreen09.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('var scopeMode by remember { mutableStateOf(ScopeMode09.RGB) }', 'var scopeMode by remember { mutableStateOf(ScopeMode09.OFF) }')
s = s.replace('''                        onScope = { scopeMode = ScopeMode09.entries[(scopeMode.ordinal + 1) % ScopeMode09.entries.size] },
''', '''                        onScope = {
                            val next = ScopeMode09.entries[(scopeMode.ordinal + 1) % ScopeMode09.entries.size]
                            preview?.let { pv ->
                                controller.setScopesEnabled(owner, pv, next != ScopeMode09.OFF) { enabled ->
                                    scopeMode = if (enabled) next else ScopeMode09.OFF
                                    status = if (enabled) "SCOPES ${scopeMode.label}" else "SCOPES OFF"
                                }
                            } ?: run { scopeMode = next }
                        },
''')
s = s.replace('''            "1:1" -> 1f
            else -> 16f / 9f
''', '''            "1:1" -> 1f
            "LF 1.90" -> 1.90f
            "LF 1.43" -> 1.43f
            else -> 16f / 9f
''')
p.write_text(s, encoding='utf-8')

# Capture aspect policy includes Large Format monitor framings.
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/camera/CaptureSettingsPolicy.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('val aspectRatios = listOf("16:9", "17:9", "2.00:1", "2.35:1", "2.39:1", "9:16", "4:3", "1:1")', 'val aspectRatios = listOf("16:9", "17:9", "2.00:1", "2.35:1", "2.39:1", "9:16", "4:3", "1:1", "LF 1.90", "LF 1.43")')
p.write_text(s, encoding='utf-8')

# Build identity after 0.14 patch.
b = Path('app/build.gradle.kts')
g = b.read_text(encoding='utf-8')
g = g.replace('versionCode = 15', 'versionCode = 16')
g = g.replace('versionName = "0.14.0-alpha"', 'versionName = "0.15.0-alpha"')
b.write_text(g, encoding='utf-8')

print('RB CineCam 0.15 transactional capture + Large Format patch applied')
