from pathlib import Path

# Camera: requested quality is only committed after CameraX confirms the bind.
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/camera/RBCameraController.kt')
s = p.read_text(encoding='utf-8')
s = s.replace(
'''            activeQuality = quality
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(activeQuality)).build()
''',
'''            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
''', 1)

start = s.index('    fun cycleQualitySafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {')
end = s.index('    fun setScopesEnabled(', start)
quality_fn = '''    fun cycleQualitySafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {
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
        val requestedFps = CameraReconfigurePolicy.safeFpsAfterQualityChange(previousFps, supportedFps())
        activeFrameRate = requestedFps

        fun rollback() {
            activeQuality = previousQuality
            activeFrameRate = previousFps
            scopesEnabled = previousScopes
            bind(owner, previewView, previousQuality, scopeCallback,
                onBound = { onResult(activeQualityLabel(), false) },
                onBindError = { onResult(activeQualityLabel(), false) })
        }

        fun tryRequested(scopes: Boolean, finalAttempt: Boolean) {
            scopesEnabled = scopes
            activeFrameRate = requestedFps
            bind(owner, previewView, requestedQuality, scopeCallback,
                onBound = {
                    activeQuality = requestedQuality
                    onResult(activeQualityLabel(), true)
                },
                onBindError = {
                    if (!finalAttempt && scopes) tryRequested(false, true) else rollback()
                })
        }

        tryRequested(previousScopes, false)
    }

'''
s = s[:start] + quality_fn + s[end:]

# FPS: keep the visible/applied state until bind succeeds; rollback on failure.
fps_start = s.index('    fun cycleFpsSafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {')
fps_end = s.index('\n    private fun', fps_start)
fps_fn = '''    fun cycleFpsSafe(owner: LifecycleOwner, previewView: PreviewView, onResult: (String, Boolean) -> Unit) {
        if (recording != null) { onResult("${activeFrameRate} FPS", true); return }
        val previous = activeFrameRate
        val requested = CaptureSettingsPolicy.nextFps(previous, supportedFps())
        activeFrameRate = requested
        bind(owner, previewView, activeQuality, scopeCallback,
            onBound = { onResult("${activeFrameRate} FPS", true) },
            onBindError = {
                activeFrameRate = previous
                bind(owner, previewView, activeQuality, scopeCallback,
                    onBound = { onResult("${activeFrameRate} FPS", false) },
                    onBindError = { onResult("${activeFrameRate} FPS", false) })
            })
    }
'''
s = s[:fps_start] + fps_fn + s[fps_end:]
p.write_text(s, encoding='utf-8')

# UI: compact, translucent controls and less obstruction over the image.
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/ui/CameraScreen09.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('fillMaxWidth().padding(start = 82.dp, end = 100.dp)', 'fillMaxWidth().padding(start = 72.dp, end = 82.dp)')
s = s.replace('Modifier.padding(horizontal=3.dp).clip(RoundedCornerShape(8.dp))', 'Modifier.padding(horizontal=2.dp).clip(RoundedCornerShape(7.dp))')
s = s.replace('Modifier.padding(horizontal=9.dp, vertical=6.dp)', 'Modifier.padding(horizontal=7.dp, vertical=4.dp)')
s = s.replace('fontSize=9.sp, fontWeight=FontWeight.Bold', 'fontSize=8.sp, fontWeight=FontWeight.Bold')
s = s.replace('Modifier.width(72.dp).fillMaxHeight().background(Color.Black.copy(alpha = .38f)).padding(6.dp)', 'Modifier.width(62.dp).fillMaxHeight().background(Color.Black.copy(alpha = .30f)).padding(4.dp)')
s = s.replace('padding(start = 86.dp, end = 178.dp, bottom = 6.dp)', 'padding(start = 74.dp, end = 146.dp, bottom = 4.dp)')
s = s.replace('padding(end = 76.dp)', 'padding(end = 64.dp)')
p.write_text(s, encoding='utf-8')

# Version identity.
b = Path('app/build.gradle.kts')
g = b.read_text(encoding='utf-8')
g = g.replace('versionCode = 16', 'versionCode = 17')
g = g.replace('versionName = "0.15.0-alpha"', 'versionName = "0.16.0-alpha"')
b.write_text(g, encoding='utf-8')

print('RB CineCam 0.16 transactional capture + compact pro UI applied')
