package br.com.rb8digital.rbcinecam.ui

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import br.com.rb8digital.rbcinecam.audio.AudioLevelMonitor
import br.com.rb8digital.rbcinecam.camera.RBCameraController
import kotlinx.coroutines.delay

private val Panel = Color(0xF2090B0E)
private val PanelSoft = Color(0xE614171B)
private val Accent = Color(0xFFFFC400)
private val Red = Color(0xFFFF2020)

enum class CameraMode { VIDEO, PHOTO }
enum class GuideMode(val label: String) { OFF("OFF"), THIRDS("1/3"), CENTER("CENTRO"), SAFE("SAFE"), R185("1.85"), R235("2.35"), R239("2.39") }
data class MediaEntry(val uri: Uri, val name: String, val video: Boolean)

private val unavailableAudio = AudioLevelMonitor.Level(-60f, -60f, -60f, false, false)

@Composable
fun CameraScreen(cameraPermissionGranted: Boolean, audioPermissionGranted: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { RBCameraController(context) }
    val audioMonitor = remember { AudioLevelMonitor(context.applicationContext) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var mode by remember { mutableStateOf(CameraMode.VIDEO) }
    var guide by remember { mutableStateOf(GuideMode.OFF) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("PRONTO") }
    var iso by remember { mutableStateOf("AUTO") }
    var shutter by remember { mutableStateOf("AUTO") }
    var wb by remember { mutableStateOf("AUTO") }
    var focus by remember { mutableStateOf("AF-C") }
    var ev by remember { mutableStateOf("0") }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var showFocus by remember { mutableStateOf(false) }
    var freeSpace by remember { mutableStateOf(storageLabel(context.filesDir.absolutePath)) }
    var galleryOpen by remember { mutableStateOf(false) }
    var playingUri by remember { mutableStateOf<Uri?>(null) }
    var focusPanelOpen by remember { mutableStateOf(false) }
    var manualFocus by remember { mutableFloatStateOf(0.5f) }
    var manualFocusAvailable by remember { mutableStateOf(false) }
    var audioLevel by remember { mutableStateOf(unavailableAudio) }

    LaunchedEffect(recording) {
        if (recording) {
            elapsedSeconds = 0
            while (true) {
                delay(1000)
                elapsedSeconds++
                freeSpace = storageLabel(context.filesDir.absolutePath)
            }
        }
    }

    LaunchedEffect(showFocus) {
        if (showFocus) {
            delay(900)
            showFocus = false
        }
    }

    DisposableEffect(audioPermissionGranted) {
        if (audioPermissionGranted) {
            audioMonitor.start { measured ->
                ContextCompat.getMainExecutor(context).execute {
                    audioLevel = measured
                }
            }
        } else {
            audioLevel = unavailableAudio
        }
        onDispose { audioMonitor.stop() }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        when {
            playingUri != null -> VideoPlayerScreen(playingUri!!, onBack = { playingUri = null })
            galleryOpen -> MediaGallery(onBack = { galleryOpen = false }, onVideo = { playingUri = it })
            !cameraPermissionGranted -> Box(
                Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) { Text("Autorize a câmera para iniciar o RB CineCam.") }
            else -> Row(
                Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                LeftRail(
                    mode,
                    !recording,
                    { mode = CameraMode.VIDEO; status = "MODO VÍDEO" },
                    { mode = CameraMode.PHOTO; status = "MODO FOTO" },
                    { galleryOpen = true }
                )

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    TopHud(mode, recording, status, elapsedSeconds, freeSpace, audioLevel)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(-1, -1)
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    previewView = this
                                    controller.bind(owner, this)
                                }
                            },
                            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                detectTapGestures { o ->
                                    previewView?.let { controller.focusAt(it, o.x, o.y) }
                                    showFocus = true
                                    status = "FOCO"
                                }
                            }
                        )

                        GuideOverlay(guide, Modifier.fillMaxSize())
                        if (showFocus) FocusReticle(Modifier.align(Alignment.Center))
                        GuideButton(
                            guide,
                            { guide = GuideMode.entries[(guide.ordinal + 1) % GuideMode.entries.size] },
                            Modifier.align(Alignment.TopEnd).padding(10.dp)
                        )
                        AudioMeterPanel(
                            level = audioLevel,
                            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                        )

                        if (focusPanelOpen) {
                            FocusControlPanel(
                                currentLabel = focus,
                                manualSupported = manualFocusAvailable,
                                manualValue = manualFocus,
                                onAfC = {
                                    focus = controller.focusContinuous()
                                    focusPanelOpen = false
                                    status = "FOCO $focus"
                                },
                                onAfS = {
                                    previewView?.let { focus = controller.focusSingle(it) }
                                    focusPanelOpen = false
                                    status = "FOCO $focus"
                                },
                                onLock = {
                                    previewView?.let { focus = controller.focusLock(it) }
                                    focusPanelOpen = false
                                    status = "FOCO $focus"
                                },
                                onManualChange = { value ->
                                    manualFocus = value
                                    focus = controller.setManualFocus(value)
                                    status = "FOCO $focus"
                                },
                                onInfinity = {
                                    manualFocus = 0f
                                    focus = controller.setManualFocus(0f)
                                    status = "FOCO $focus"
                                },
                                onMacro = {
                                    manualFocus = 1f
                                    focus = controller.setManualFocus(1f)
                                    status = "FOCO $focus"
                                },
                                onClose = { focusPanelOpen = false },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                            )
                        }
                    }

                    ManualBar(
                        iso, shutter, wb, focus, ev, !recording,
                        { iso = controller.cycleIso(); status = "ISO $iso" },
                        { shutter = controller.cycleShutter(); status = "SHUTTER $shutter" },
                        { wb = controller.cycleWhiteBalance(); status = "WB $wb" },
                        {
                            manualFocusAvailable = controller.manualFocusSupported()
                            focusPanelOpen = true
                        },
                        { ev = controller.cycleEv(); status = "EV $ev" }
                    )
                }

                RightRail(
                    mode,
                    recording,
                    elapsedSeconds,
                    {
                        previewView?.let {
                            controller.switchLens(owner, it)
                            iso = "AUTO"; shutter = "AUTO"; wb = "AUTO"; focus = "AF-C"; ev = "0"
                            focusPanelOpen = false
                            status = "CÂMERA TROCADA"
                        }
                    }
                ) {
                    if (mode == CameraMode.VIDEO) {
                        if (recording) {
                            controller.stopRecording()
                            status = "SALVANDO"
                        } else {
                            controller.startRecording(audioPermissionGranted) { e ->
                                when (e) {
                                    is VideoRecordEvent.Start -> { recording = true; status = "REC" }
                                    is VideoRecordEvent.Finalize -> {
                                        recording = false
                                        status = if (e.hasError()) "ERRO AO SALVAR" else "VÍDEO SALVO"
                                    }
                                }
                            }
                        }
                    } else {
                        controller.takePhoto { ok, msg ->
                            status = msg
                            if (ok) freeSpace = storageLabel(context.filesDir.absolutePath)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioMeterPanel(level: AudioLevelMonitor.Level, modifier: Modifier = Modifier) {
    Surface(modifier, color = PanelSoft, shape = RoundedCornerShape(8.dp)) {
        if (!level.available) {
            Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.MicOff, contentDescription = "Áudio indisponível", tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text("AUDIO N/D", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AudioBar("L", level.leftDb, level.clipping)
                AudioBar("R", level.rightDb, level.clipping)
                Column(horizontalAlignment = Alignment.End) {
                    Text("dBFS", color = Color.Gray, fontSize = 7.sp)
                    Text("0", color = Color.LightGray, fontSize = 7.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("-12", color = Color.LightGray, fontSize = 7.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("-36", color = Color.LightGray, fontSize = 7.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("-60", color = Color.LightGray, fontSize = 7.sp)
                }
            }
        }
    }
}

@Composable
private fun AudioBar(label: String, db: Float, clipping: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.width(10.dp).height(68.dp).background(Color(0xFF25282D))) {
            val fraction = ((db.coerceIn(-60f, 0f) + 60f) / 60f)
            val h = size.height * fraction
            drawRect(
                color = if (clipping) Red else Accent,
                topLeft = Offset(0f, size.height - h),
                size = androidx.compose.ui.geometry.Size(size.width, h)
            )
        }
        Text(label, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text("%.0f".format(db), color = if (clipping) Red else Color.LightGray, fontSize = 7.sp)
    }
}

@Composable
private fun FocusControlPanel(
    currentLabel: String,
    manualSupported: Boolean,
    manualValue: Float,
    onAfC: () -> Unit,
    onAfS: () -> Unit,
    onLock: () -> Unit,
    onManualChange: (Float) -> Unit,
    onInfinity: () -> Unit,
    onMacro: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.widthIn(min = 480.dp, max = 680.dp), color = Color(0xF20E1115), shape = RoundedCornerShape(12.dp), shadowElevation = 8.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FOCO", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text(currentLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) { Icon(Icons.Filled.Close, "Fechar", tint = Color.White) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFocusButton("AF-C", onAfC)
                SmallFocusButton("AF-S", onAfS)
                SmallFocusButton("LOCK", onLock)
                if (manualSupported) {
                    SmallFocusButton("∞", onInfinity)
                    SmallFocusButton("MACRO", onMacro)
                }
            }
            if (manualSupported) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("∞", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = manualValue,
                        onValueChange = onManualChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                    )
                    Text("MACRO", color = Color.White, fontSize = 10.sp)
                }
                Text("MF ${(manualValue * 100).toInt()}%", color = Accent, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Spacer(Modifier.height(7.dp))
                Text("Foco manual não exposto por esta lente", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SmallFocusButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(34.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun GuideButton(mode: GuideMode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }, color = PanelSoft) {
        Text("GRID ${mode.label}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun GuideOverlay(mode: GuideMode, modifier: Modifier = Modifier) {
    if (mode == GuideMode.OFF) return
    Canvas(modifier) {
        val c = Color.White.copy(alpha = .65f)
        val w = 1.dp.toPx()
        when (mode) {
            GuideMode.THIRDS -> for (i in 1..2) {
                drawLine(c, Offset(size.width * i / 3, 0f), Offset(size.width * i / 3, size.height), w)
                drawLine(c, Offset(0f, size.height * i / 3), Offset(size.width, size.height * i / 3), w)
            }
            GuideMode.CENTER -> {
                drawLine(c, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), w)
                drawLine(c, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), w)
            }
            GuideMode.SAFE -> {
                val x = size.width * .05f; val y = size.height * .05f
                drawRect(c, Offset(x, y), androidx.compose.ui.geometry.Size(size.width - 2 * x, size.height - 2 * y), style = androidx.compose.ui.graphics.drawscope.Stroke(w))
            }
            GuideMode.R185, GuideMode.R235, GuideMode.R239 -> {
                val ratio = when (mode) { GuideMode.R185 -> 1.85f; GuideMode.R235 -> 2.35f; else -> 2.39f }
                val h = size.width / ratio
                if (h < size.height) {
                    val y = (size.height - h) / 2
                    drawLine(c, Offset(0f, y), Offset(size.width, y), w)
                    drawLine(c, Offset(0f, y + h), Offset(size.width, y + h), w)
                }
            }
            else -> Unit
        }
    }
}

@Composable private fun MediaGallery(onBack: () -> Unit, onVideo: (Uri) -> Unit) {
    val context = LocalContext.current
    val media = remember { queryMedia(context) }
    Column(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Voltar") }
            Text("GALERIA RB CineCam", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (media.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhuma mídia encontrada") }
        else LazyColumn {
            items(media) { m ->
                Row(Modifier.fillMaxWidth().clickable(enabled = m.video) { if (m.video) onVideo(m.uri) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (m.video) Icons.Filled.PlayCircle else Icons.Filled.Photo, null, tint = if (m.video) Accent else Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(m.name, color = Color.White)
                        Text(if (m.video) "VÍDEO • toque para assistir" else "FOTO", color = Color.Gray, fontSize = 11.sp)
                    }
                }
                HorizontalDivider(color = Color.DarkGray)
            }
        }
    }
}

@Composable private fun VideoPlayerScreen(uri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(uri) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); prepare(); playWhenReady = true } }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxSize())
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(12.dp).background(Panel, CircleShape)) {
            Icon(Icons.Filled.ArrowBack, "Voltar", tint = Color.White)
        }
    }
}

private fun queryMedia(context: Context): List<MediaEntry> {
    val out = mutableListOf<MediaEntry>()
    fun scan(collection: Uri, projection: Array<String>, video: Boolean) {
        context.contentResolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            while (c.moveToNext()) {
                val n = c.getString(name) ?: continue
                if (n.startsWith("RBCineCam_")) out += MediaEntry(ContentUris.withAppendedId(collection, c.getLong(id)), n, video)
            }
        }
    }
    scan(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATE_ADDED), true)
    scan(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_ADDED), false)
    return out.sortedByDescending { it.name }
}

@Composable private fun LeftRail(mode: CameraMode, enabled: Boolean, onVideo: () -> Unit, onPhoto: () -> Unit, onGallery: () -> Unit) {
    Column(Modifier.width(78.dp).fillMaxHeight().background(Panel).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        RailModeAction(Icons.Filled.Videocam, "VÍDEO", mode == CameraMode.VIDEO, enabled, onVideo)
        RailModeAction(Icons.Filled.PhotoCamera, "FOTO", mode == CameraMode.PHOTO, enabled, onPhoto)
        RailIconAction(Icons.Filled.PhotoLibrary, "GALERIA", enabled, onGallery)
    }
}

@Composable private fun RightRail(mode: CameraMode, recording: Boolean, elapsed: Long, onSwitch: () -> Unit, onPrimary: () -> Unit) {
    Column(Modifier.width(96.dp).fillMaxHeight().background(Panel).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        RailIconAction(Icons.Filled.Cameraswitch, "TROCAR", !recording, onSwitch)
        Box(Modifier.size(74.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).padding(7.dp).clip(CircleShape).background(if (recording) Color(0xFF7A2020) else if (mode == CameraMode.VIDEO) Red else Color.White).clickable { onPrimary() }, contentAlignment = Alignment.Center) {
            Icon(if (recording) Icons.Filled.Stop else if (mode == CameraMode.VIDEO) Icons.Filled.FiberManualRecord else Icons.Filled.PhotoCamera, null, tint = if (mode == CameraMode.PHOTO && !recording) Color.Black else Color.White, modifier = Modifier.size(28.dp))
        }
        Text(if (recording) formatDuration(elapsed) else if (mode == CameraMode.VIDEO) "REC" else "FOTO", color = if (recording || mode == CameraMode.VIDEO) Red else Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable private fun TopHud(mode: CameraMode, recording: Boolean, status: String, elapsed: Long, free: String, audio: AudioLevelMonitor.Level) {
    Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("RB ", color = Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("CineCam", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.width(16.dp))
        Text(if (mode == CameraMode.VIDEO) "VÍDEO • FHD • 30 FPS • MP4" else "FOTO • JPEG", fontSize = 13.sp)
        Spacer(Modifier.width(14.dp))
        Text("Livre $free", color = Color.LightGray, fontSize = 11.sp)
        if (audio.available) {
            Spacer(Modifier.width(12.dp))
            Text("MIC ${"%.0f".format(audio.peakDb)} dB", color = if (audio.clipping) Red else Color.LightGray, fontSize = 10.sp)
        }
        Spacer(Modifier.weight(1f))
        Text(if (recording) "● ${formatDuration(elapsed)}" else status, color = if (recording) Red else Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable private fun ManualBar(iso: String, shutter: String, wb: String, focus: String, ev: String, enabled: Boolean, onIso: () -> Unit, onShutter: () -> Unit, onWb: () -> Unit, onFocus: () -> Unit, onEv: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel).padding(7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ManualControl("ISO", iso, enabled, onIso, Modifier.weight(1f))
        ManualControl("SHUTTER", shutter, enabled, onShutter, Modifier.weight(1f))
        ManualControl("WB", wb, enabled, onWb, Modifier.weight(1f))
        ManualControl("FOCUS", focus, enabled, onFocus, Modifier.weight(1f))
        ManualControl("EV", ev, enabled, onEv, Modifier.weight(1f))
    }
}

@Composable private fun ManualControl(label: String, value: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.height(58.dp).clip(RoundedCornerShape(8.dp)).background(PanelSoft).border(1.dp, Color(0xFF34383E), RoundedCornerShape(8.dp)).clickable(enabled = enabled) { onClick() }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(label, color = Color.LightGray, fontSize = 10.sp)
        Text(value, color = if (enabled) Accent else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable private fun RailModeAction(icon: ImageVector, label: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (!enabled) Color.Gray else if (active) Accent else Color.White
    Column(Modifier.clip(RoundedCornerShape(12.dp)).background(if (active) Color(0x33222222) else Color.Transparent).clickable(enabled = enabled) { onClick() }.padding(horizontal = 8.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp)); Spacer(Modifier.height(4.dp)); Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun RailIconAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) Color.White else Color.Gray
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = enabled) { onClick() }.padding(horizontal = 8.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp)); Spacer(Modifier.height(4.dp)); Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun FocusReticle(modifier: Modifier = Modifier) {
    Box(modifier.size(46.dp).border(2.dp, Accent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("+", color = Accent, fontSize = 20.sp) }
}

private fun formatDuration(s: Long) = "%02d:%02d".format(s / 60, s % 60)
private fun storageLabel(path: String) = runCatching {
    val stat = StatFs(path); val gb = stat.availableBytes / 1_073_741_824.0
    if (gb >= 10) "%.0f GB".format(gb) else "%.1f GB".format(gb)
}.getOrDefault("--")
