from pathlib import Path

p = Path('app/src/main/java/br/com/rb8digital/rbcinecam/ui/CameraScreen09.kt')
s = p.read_text(encoding='utf-8')

# ProShot-inspired translucent visual language. Preview remains full-bleed underneath.
s = s.replace('private val P09 = Color(0xF2090B0E)', 'private val P09 = Color(0x7A090B0E)')
s = s.replace('private val PS09 = Color(0xE614171B)', 'private val PS09 = Color(0x7014171B)')

start = s.index('@Composable\nfun CameraScreen09(')
end = s.index('@Composable private fun CaptureBar13', start)

screen = r'''@Composable
fun CameraScreen09(cameraPermissionGranted: Boolean, audioPermissionGranted: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { RBCameraController(context) }
    val audioMonitor = remember { AudioLevelMonitor(context.applicationContext) }
    var preview by remember { mutableStateOf<PreviewView?>(null) }
    var mode by remember { mutableStateOf(CameraMode.VIDEO) }
    var guide by remember { mutableStateOf(GuideMode.OFF) }
    var scopeMode by remember { mutableStateOf(ScopeMode09.RGB) }
    var scopeFrame by remember { mutableStateOf(ScopeFrame.empty()) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("PRONTO") }
    var elapsed by remember { mutableLongStateOf(0L) }
    var free by remember { mutableStateOf(storageLabel09(context.filesDir.absolutePath)) }
    var battery by remember { mutableIntStateOf(batteryPct09(context)) }
    var audio by remember { mutableStateOf(unavailable09) }
    var recordingDb by remember { mutableFloatStateOf(-60f) }
    var gallery by remember { mutableStateOf(false) }
    var playerUri by remember { mutableStateOf<Uri?>(null) }
    var iso by remember { mutableStateOf("AUTO") }
    var shutter by remember { mutableStateOf("AUTO") }
    var wb by remember { mutableStateOf("AUTO") }
    var focus by remember { mutableStateOf("AF-C") }
    var ev by remember { mutableStateOf("0") }
    var focusPanel by remember { mutableStateOf(false) }
    var manualFocus by remember { mutableFloatStateOf(.5f) }
    var manualFocusAvailable by remember { mutableStateOf(false) }
    var showFocus by remember { mutableStateOf(false) }
    var qualityLabel by remember { mutableStateOf("FHD") }
    var fpsLabel by remember { mutableStateOf("30") }
    var aspectLabel by remember { mutableStateOf("16:9") }

    LaunchedEffect(recording) {
        if (recording) {
            elapsed = 0
            while (true) { delay(1000); elapsed++; free = storageLabel09(context.filesDir.absolutePath); battery = batteryPct09(context) }
        }
    }
    LaunchedEffect(showFocus) { if (showFocus) { delay(800); showFocus = false } }
    LaunchedEffect(audioPermissionGranted, recording) {
        audioMonitor.stop()
        if (audioPermissionGranted && !recording) audioMonitor.start { level ->
            ContextCompat.getMainExecutor(context).execute { audio = level }
        }
    }
    DisposableEffect(Unit) { onDispose { audioMonitor.stop() } }

    MaterialTheme(colorScheme = darkColorScheme()) {
        when {
            playerUri != null -> Player09(playerUri!!, onGallery = { playerUri = null; gallery = true }, onCamera = { playerUri = null; gallery = false })
            gallery -> Gallery09(onCamera = { gallery = false }, onVideo = { playerUri = it })
            !cameraPermissionGranted -> Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { Text("Autorize a câmera para iniciar o RB CineCam.", color = Color.White) }
            else -> Box(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
                AndroidView(
                    factory = { ctx -> PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        preview = this
                        controller.bind(owner, this, onScopeFrame = { f -> ContextCompat.getMainExecutor(context).execute { scopeFrame = f } })
                    } },
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures { point -> preview?.let { controller.focusAt(it, point.x, point.y) }; showFocus = true; status = "FOCO" }
                    }
                )

                AspectMask09(aspectLabel, Modifier.fillMaxSize())
                GuideOverlay09(guide, Modifier.fillMaxSize())
                if (showFocus) FocusReticle09(Modifier.align(Alignment.Center))

                Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(start = 82.dp, end = 100.dp)) {
                    TopHud10(mode, recording, status, elapsed, free, battery, audioPermissionGranted, if (recording) recordingDb else audio.peakDb, shutter)
                    CaptureOverlay14(
                        qualityLabel, fpsLabel, aspectLabel, scopeMode, guide, !recording,
                        onQuality = { preview?.let { pv -> status = "AJUSTANDO QUADRO..."; controller.cycleQualitySafe(owner, pv) { label, exact -> qualityLabel = label; fpsLabel = controller.activeFps().toString(); status = if (exact) "QUADRO $label" else "QUADRO $label • FALLBACK" } } },
                        onFps = { preview?.let { pv -> status = "AJUSTANDO FPS..."; controller.cycleFpsSafe(owner, pv) { label, exact -> fpsLabel = label.substringBefore(" "); status = if (exact) "FPS $fpsLabel" else "FPS $fpsLabel • INCOMPATÍVEL" } } },
                        onAspect = { aspectLabel = CaptureSettingsPolicy.nextAspect(aspectLabel); status = "ASPECT $aspectLabel" },
                        onScope = { scopeMode = ScopeMode09.entries[(scopeMode.ordinal + 1) % ScopeMode09.entries.size] },
                        onGuide = { guide = GuideMode.entries[(guide.ordinal + 1) % GuideMode.entries.size] }
                    )
                }

                LeftRail09(mode, !recording,
                    onVideo = { mode = CameraMode.VIDEO; status = "MODO VÍDEO" },
                    onPhoto = { mode = CameraMode.PHOTO; status = "MODO FOTO" },
                    onGallery = { gallery = true })

                Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
                    AudioStrip09(audio, recording, recordingDb, audioPermissionGranted)
                }

                Box(Modifier.align(Alignment.CenterEnd).padding(end = 76.dp)) {
                    RightRail09(mode, recording, elapsed,
                        onSwitch = { preview?.let { controller.switchLens(owner, it); iso="AUTO"; shutter="AUTO"; wb="AUTO"; focus="AF-C"; ev="0"; status="CÂMERA TROCADA" } },
                        onPrimary = {
                            if (mode == CameraMode.VIDEO) {
                                if (recording) { controller.stopRecording(); status = "SALVANDO" }
                                else controller.startRecording(audioPermissionGranted) { e -> when(e) {
                                    is VideoRecordEvent.Start -> { recording = true; status = "REC" }
                                    is VideoRecordEvent.Status -> { recordingDb = AudioMeterMath.amplitudeDbfs(e.recordingStats.audioStats.audioAmplitude) }
                                    is VideoRecordEvent.Finalize -> { recording = false; status = if(e.hasError()) "ERRO ${e.error}" else "VÍDEO SALVO" }
                                } }
                            } else controller.takePhoto { ok, msg -> status = msg; if(ok) free = storageLabel09(context.filesDir.absolutePath) }
                        })
                }

                Manual09(iso, shutter, wb, focus, ev, !recording,
                    { iso = controller.cycleIso(); status = "ISO $iso" },
                    { shutter = controller.cycleShutter(); status = "SHUTTER $shutter" },
                    { wb = controller.cycleWhiteBalance(); status = "WB $wb" },
                    { manualFocusAvailable = controller.manualFocusSupported(); focusPanel = true },
                    { ev = controller.cycleEv(); status = "EV $ev" },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 86.dp, end = 178.dp, bottom = 6.dp)
                )

                if (scopeMode != ScopeMode09.OFF) ScopePanel09(scopeFrame, scopeMode, Modifier.align(Alignment.BottomStart).padding(start = 88.dp, bottom = 76.dp))
                if (focusPanel) FocusPanel09(
                    current=focus, manualSupported=manualFocusAvailable, value=manualFocus,
                    onAfC={focus=controller.focusContinuous();focusPanel=false;status="FOCO $focus"},
                    onAfS={preview?.let{focus=controller.focusSingle(it)};focusPanel=false;status="FOCO $focus"},
                    onLock={preview?.let{focus=controller.focusLock(it)};focusPanel=false;status="FOCO $focus"},
                    onManual={manualFocus=it;focus=controller.setManualFocus(it);status="FOCO $focus"},
                    onInfinity={manualFocus=0f;focus=controller.setManualFocus(0f)},
                    onMacro={manualFocus=1f;focus=controller.setManualFocus(1f)},
                    onClose={focusPanel=false},
                    modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=76.dp)
                )
            }
        }
    }
}

@Composable private fun CaptureOverlay14(
    quality:String, fps:String, aspect:String, scope:ScopeMode09, guide:GuideMode, enabled:Boolean,
    onQuality:()->Unit, onFps:()->Unit, onAspect:()->Unit, onScope:()->Unit, onGuide:()->Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(top=3.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CaptureChip14(quality, enabled, onQuality)
        CaptureChip14("${fps} FPS", enabled, onFps)
        CaptureChip14(aspect, enabled, onAspect)
        CaptureChip14("SCOPE ${scope.label}", true, onScope)
        CaptureChip14("GRID ${guide.label}", true, onGuide)
    }
}

@Composable private fun CaptureChip14(value:String, enabled:Boolean, onClick:()->Unit) {
    Surface(
        Modifier.padding(horizontal=3.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled=enabled){onClick()},
        color = Color.Black.copy(alpha = ProOverlayPolicy.controlAlpha),
        shape = RoundedCornerShape(8.dp)
    ) { Text(value, Modifier.padding(horizontal=9.dp, vertical=6.dp), color=if(enabled)A09 else Color.Gray, fontSize=9.sp, fontWeight=FontWeight.Bold, maxLines=1) }
}

'''

s = s[:start] + screen + s[end:]

# Manual bar now accepts an external modifier so it can float over the full-bleed image.
s = s.replace(
'@Composable private fun Manual09(iso:String,shutter:String,wb:String,focus:String,ev:String,enabled:Boolean,onIso:()->Unit,onShutter:()->Unit,onWb:()->Unit,onFocus:()->Unit,onEv:()->Unit){Row(Modifier.fillMaxWidth().background(P09).padding(7.dp)',
'@Composable private fun Manual09(iso:String,shutter:String,wb:String,focus:String,ev:String,enabled:Boolean,onIso:()->Unit,onShutter:()->Unit,onWb:()->Unit,onFocus:()->Unit,onEv:()->Unit,modifier:Modifier=Modifier){Row(modifier.fillMaxWidth().background(Color.Transparent).padding(7.dp)'
)

# Slim translucent rails and audio meter. They remain controls over the picture, not black side panels.
s = s.replace('Modifier.width(78.dp).fillMaxHeight().background(P09)', 'Modifier.width(78.dp).fillMaxHeight().background(Color.Transparent)')
s = s.replace('Modifier.width(96.dp).fillMaxHeight().background(P09)', 'Modifier.width(96.dp).fillMaxHeight().background(Color.Transparent)')
s = s.replace('Modifier.width(76.dp).fillMaxHeight().background(P09).padding(6.dp)', 'Modifier.width(72.dp).fillMaxHeight().background(Color.Black.copy(alpha = .38f)).padding(6.dp)')
s = s.replace('color = Color(0xD9080A0D)', 'color = Color.Black.copy(alpha = .52f)')

p.write_text(s, encoding='utf-8')

b = Path('app/build.gradle.kts')
g = b.read_text(encoding='utf-8')
g = g.replace('versionCode = 14', 'versionCode = 15')
g = g.replace('versionName = "0.13.0-alpha"', 'versionName = "0.14.0-alpha"')
b.write_text(g, encoding='utf-8')

print('RB CineCam 0.14 full-bleed translucent overlay UI applied')
