from pathlib import Path

# Camera engine: selectable quality + target FPS.
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/camera/RBCameraController.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('import android.provider.MediaStore\n', 'import android.provider.MediaStore\nimport android.util.Range\n')
s = s.replace('import androidx.camera.core.CameraSelector\n', 'import androidx.camera.core.CameraSelector\nimport androidx.camera.core.DynamicRange\n')
s = s.replace('    private val activeFrameRate = 30\n', '    private var activeFrameRate = 30\n    private var activeQuality: Quality = Quality.FHD\n')
s = s.replace('        quality: Quality = Quality.FHD,\n', '        quality: Quality = activeQuality,\n')
s = s.replace('            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()\n            videoCapture = VideoCapture.withOutput(recorder)\n', '            activeQuality = quality\n            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(activeQuality)).build()\n            videoCapture = VideoCapture.Builder(recorder).setTargetFrameRate(Range(activeFrameRate, activeFrameRate)).build()\n')
s = s.replace('    fun activeFps(): Int = activeFrameRate\n', '''    fun activeFps(): Int = activeFrameRate

    fun activeQualityLabel(): String = when (activeQuality) {
        Quality.UHD -> "4K"
        Quality.FHD -> "FHD"
        Quality.HD -> "HD"
        else -> "FHD"
    }

    fun supportedFps(): List<Int> {
        val activeCamera = camera ?: return listOf(30)
        val info = Camera2CameraInfo.from(activeCamera.cameraInfo)
        val ranges = info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES).orEmpty()
        val candidates = listOf(24, 25, 30, 50, 60, 120)
        val supported = candidates.filter { fps -> ranges.any { fps in it.lower..it.upper } }
        return if (supported.isEmpty()) listOf(30) else supported
    }

    fun supportedQualityLabels(): List<String> {
        val activeCamera = camera ?: return listOf("FHD")
        val qualities = Recorder.getVideoCapabilities(activeCamera.cameraInfo).getSupportedQualities(DynamicRange.SDR)
        val labels = buildList {
            if (Quality.HD in qualities) add("HD")
            if (Quality.FHD in qualities) add("FHD")
            if (Quality.UHD in qualities) add("4K")
        }
        return if (labels.isEmpty()) listOf("FHD") else labels
    }

    fun cycleFps(owner: LifecycleOwner, previewView: PreviewView): String {
        if (recording != null) return "${activeFrameRate} FPS"
        activeFrameRate = CaptureSettingsPolicy.nextFps(activeFrameRate, supportedFps())
        bind(owner, previewView, activeQuality, scopeCallback)
        return "${activeFrameRate} FPS"
    }

    fun cycleQuality(owner: LifecycleOwner, previewView: PreviewView): String {
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
''')
p.write_text(s, encoding='utf-8')

# Active screen: capture settings buttons + aspect framing overlay.
p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/ui/CameraScreen09.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('import br.com.rb8digital.rbcinecam.camera.RBCameraController\n', 'import br.com.rb8digital.rbcinecam.camera.RBCameraController\nimport br.com.rb8digital.rbcinecam.camera.CaptureSettingsPolicy\n')
s = s.replace('    var showFocus by remember { mutableStateOf(false) }\n', '    var showFocus by remember { mutableStateOf(false) }\n    var qualityLabel by remember { mutableStateOf("FHD") }\n    var fpsLabel by remember { mutableStateOf("30") }\n    var aspectLabel by remember { mutableStateOf("16:9") }\n')
s = s.replace('''                            GuideOverlay09(guide, Modifier.fillMaxSize())
''', '''                            AspectMask09(aspectLabel, Modifier.fillMaxSize())
                            GuideOverlay09(guide, Modifier.fillMaxSize())
''')
s = s.replace('''                            Row(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Tool09("SCOPES ${scopeMode.label}") { scopeMode = ScopeMode09.entries[(scopeMode.ordinal + 1) % ScopeMode09.entries.size] }
                                Tool09("GRID ${guide.label}") { guide = GuideMode.entries[(guide.ordinal + 1) % GuideMode.entries.size] }
                            }
''', '''                            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Tool09("${qualityLabel}") { preview?.let { qualityLabel = controller.cycleQuality(owner, it); status = "QUADRO $qualityLabel" } }
                                Tool09("${fpsLabel} FPS") { preview?.let { fpsLabel = controller.cycleFps(owner, it).substringBefore(" "); status = "FPS $fpsLabel" } }
                                Tool09("${aspectLabel}") { aspectLabel = CaptureSettingsPolicy.nextAspect(aspectLabel); status = "ASPECT $aspectLabel" }
                                Tool09("SCOPES ${scopeMode.label}") { scopeMode = ScopeMode09.entries[(scopeMode.ordinal + 1) % ScopeMode09.entries.size] }
                                Tool09("GRID ${guide.label}") { guide = GuideMode.entries[(guide.ordinal + 1) % GuideMode.entries.size] }
                            }
''')
insert = '''
@Composable private fun AspectMask09(aspect: String, modifier: Modifier = Modifier) {
    if (aspect == "16:9") return
    Canvas(modifier) {
        val ratio = when (aspect) {
            "17:9" -> 17f / 9f
            "2.00:1" -> 2f
            "2.35:1" -> 2.35f
            "2.39:1" -> 2.39f
            "9:16" -> 9f / 16f
            "4:3" -> 4f / 3f
            "1:1" -> 1f
            else -> 16f / 9f
        }
        val current = size.width / size.height
        val shade = Color.Black.copy(alpha = 0.55f)
        if (current > ratio) {
            val targetW = size.height * ratio
            val side = (size.width - targetW) / 2f
            drawRect(shade, size = androidx.compose.ui.geometry.Size(side, size.height))
            drawRect(shade, topLeft = Offset(size.width - side, 0f), size = androidx.compose.ui.geometry.Size(side, size.height))
        } else if (current < ratio) {
            val targetH = size.width / ratio
            val bar = (size.height - targetH) / 2f
            drawRect(shade, size = androidx.compose.ui.geometry.Size(size.width, bar))
            drawRect(shade, topLeft = Offset(0f, size.height - bar), size = androidx.compose.ui.geometry.Size(size.width, bar))
        }
    }
}

'''
anchor = '@Composable private fun AudioStrip09('
if anchor not in s:
    raise SystemExit('AudioStrip09 anchor not found')
s = s.replace(anchor, insert + anchor, 1)
p.write_text(s, encoding='utf-8')

# Build identity.
b = Path('app/build.gradle.kts')
g = b.read_text(encoding='utf-8')
g = g.replace('versionCode = 11', 'versionCode = 13')
g = g.replace('versionName = "0.10.1-alpha"', 'versionName = "0.12.0-alpha"')
b.write_text(g, encoding='utf-8')

print('RB CineCam 0.12 capture controls patch applied')
