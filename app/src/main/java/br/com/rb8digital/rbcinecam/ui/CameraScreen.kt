package br.com.rb8digital.rbcinecam.ui

import android.content.Intent
import android.os.StatFs
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.rb8digital.rbcinecam.camera.RBCameraController
import kotlinx.coroutines.delay

private val Panel = Color(0xDD090B0E)
private val PanelSoft = Color(0xC414171B)
private val Accent = Color(0xFFFFC400)
private val Red = Color(0xFFFF2020)

enum class CameraMode { VIDEO, PHOTO }

@Composable
fun CameraScreen(cameraPermissionGranted: Boolean, audioPermissionGranted: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { RBCameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var mode by remember { mutableStateOf(CameraMode.VIDEO) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("PRONTO") }
    var iso by remember { mutableStateOf("AUTO") }
    var shutter by remember { mutableStateOf("AUTO") }
    var wb by remember { mutableStateOf("AUTO") }
    var focus by remember { mutableStateOf("AF") }
    var ev by remember { mutableStateOf("0") }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var showFocus by remember { mutableStateOf(false) }
    var freeSpace by remember { mutableStateOf(storageLabel(context.filesDir.absolutePath)) }

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

    MaterialTheme(colorScheme = darkColorScheme()) {
        if (!cameraPermissionGranted) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Text("Autorize a câmera para iniciar o RB CineCam.", color = Color.White)
            }
            return@MaterialTheme
        }

        Row(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            LeftRail(
                mode = mode,
                enabled = !recording,
                onVideo = {
                    mode = CameraMode.VIDEO
                    status = "MODO VÍDEO"
                },
                onPhoto = {
                    mode = CameraMode.PHOTO
                    status = "MODO FOTO"
                },
                onGallery = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_PICK).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                        }
                        context.startActivity(intent)
                    }.onFailure { status = "GALERIA INDISPONÍVEL" }
                }
            )

            Box(Modifier.weight(1f).fillMaxHeight()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                            controller.bind(owner, this)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                previewView?.let { controller.focusAt(it, offset.x, offset.y) }
                                showFocus = true
                                status = "FOCO"
                            }
                        }
                )

                TopHud(
                    mode = mode,
                    recording = recording,
                    status = status,
                    elapsedSeconds = elapsedSeconds,
                    freeSpace = freeSpace,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                if (showFocus) FocusReticle(Modifier.align(Alignment.Center))

                ManualBar(
                    iso = iso,
                    shutter = shutter,
                    wb = wb,
                    focus = focus,
                    ev = ev,
                    enabled = !recording,
                    onIso = { iso = controller.cycleIso(); status = "ISO $iso" },
                    onShutter = { shutter = controller.cycleShutter(); status = "SHUTTER $shutter" },
                    onWb = { wb = controller.cycleWhiteBalance(); status = "WB $wb" },
                    onFocus = { focus = controller.cycleFocus(); status = "FOCO $focus" },
                    onEv = { ev = controller.cycleEv(); status = "EV $ev" },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            RightRail(
                mode = mode,
                recording = recording,
                elapsedSeconds = elapsedSeconds,
                onSwitch = {
                    previewView?.let {
                        controller.switchLens(owner, it)
                        iso = "AUTO"
                        shutter = "AUTO"
                        wb = "AUTO"
                        focus = "AF"
                        ev = "0"
                        status = "CÂMERA TROCADA"
                    }
                },
                onPrimary = {
                    when (mode) {
                        CameraMode.VIDEO -> {
                            if (recording) {
                                controller.stopRecording()
                                status = "SALVANDO"
                            } else {
                                controller.startRecording(audioPermissionGranted) { event ->
                                    when (event) {
                                        is VideoRecordEvent.Start -> {
                                            recording = true
                                            status = "REC"
                                        }
                                        is VideoRecordEvent.Finalize -> {
                                            recording = false
                                            status = if (event.hasError()) "ERRO AO SALVAR" else "VÍDEO SALVO"
                                        }
                                    }
                                }
                            }
                        }
                        CameraMode.PHOTO -> {
                            controller.takePhoto { success, message ->
                                status = message
                                if (success) freeSpace = storageLabel(context.filesDir.absolutePath)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun LeftRail(
    mode: CameraMode,
    enabled: Boolean,
    onVideo: () -> Unit,
    onPhoto: () -> Unit,
    onGallery: () -> Unit
) {
    Column(
        Modifier
            .width(78.dp)
            .fillMaxHeight()
            .background(Panel)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        RailModeAction(Icons.Filled.Videocam, "VÍDEO", mode == CameraMode.VIDEO, enabled, onVideo)
        RailModeAction(Icons.Filled.PhotoCamera, "FOTO", mode == CameraMode.PHOTO, enabled, onPhoto)
        RailIconAction(Icons.Filled.PhotoLibrary, "GALERIA", enabled, onGallery)
    }
}

@Composable
private fun RightRail(
    mode: CameraMode,
    recording: Boolean,
    elapsedSeconds: Long,
    onSwitch: () -> Unit,
    onPrimary: () -> Unit
) {
    Column(
        Modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(Panel)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        RailIconAction(Icons.Filled.Cameraswitch, "TROCAR", !recording, onSwitch)

        Box(
            Modifier
                .size(74.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .padding(7.dp)
                .clip(CircleShape)
                .background(
                    when {
                        recording -> Color(0xFF7A2020)
                        mode == CameraMode.VIDEO -> Red
                        else -> Color.White
                    }
                )
                .clickable { onPrimary() },
            contentAlignment = Alignment.Center
        ) {
            when {
                recording -> Icon(Icons.Filled.Stop, contentDescription = "Parar gravação", tint = Color.White, modifier = Modifier.size(28.dp))
                mode == CameraMode.VIDEO -> Icon(Icons.Filled.FiberManualRecord, contentDescription = "Gravar vídeo", tint = Color.White, modifier = Modifier.size(28.dp))
                else -> Icon(Icons.Filled.PhotoCamera, contentDescription = "Capturar foto", tint = Color.Black, modifier = Modifier.size(28.dp))
            }
        }

        Text(
            when {
                recording -> formatDuration(elapsedSeconds)
                mode == CameraMode.VIDEO -> "REC"
                else -> "FOTO"
            },
            color = if (recording || mode == CameraMode.VIDEO) Red else Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun TopHud(
    mode: CameraMode,
    recording: Boolean,
    status: String,
    elapsedSeconds: Long,
    freeSpace: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Panel)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("RB ", color = Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("CineCam", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.width(16.dp))
        Text(
            if (mode == CameraMode.VIDEO) "VÍDEO  •  FHD  •  MP4" else "FOTO  •  JPEG",
            color = Color.White,
            fontSize = 13.sp
        )
        Spacer(Modifier.width(14.dp))
        Text("Livre $freeSpace", color = Color.LightGray, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text(
            if (recording) "● ${formatDuration(elapsedSeconds)}" else status,
            color = if (recording) Red else Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ManualBar(
    iso: String,
    shutter: String,
    wb: String,
    focus: String,
    ev: String,
    enabled: Boolean,
    onIso: () -> Unit,
    onShutter: () -> Unit,
    onWb: () -> Unit,
    onFocus: () -> Unit,
    onEv: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth().background(Panel).padding(7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        ManualControl("ISO", iso, enabled, onIso, Modifier.weight(1f))
        ManualControl("SHUTTER", shutter, enabled, onShutter, Modifier.weight(1f))
        ManualControl("WB", wb, enabled, onWb, Modifier.weight(1f))
        ManualControl("FOCUS", focus, enabled, onFocus, Modifier.weight(1f))
        ManualControl("EV", ev, enabled, onEv, Modifier.weight(1f))
    }
}

@Composable
private fun ManualControl(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PanelSoft)
            .border(1.dp, Color(0xFF34383E), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = Color.LightGray, fontSize = 10.sp)
        Text(
            value,
            color = if (enabled) Accent else Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun RailModeAction(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tint = when {
        !enabled -> Color.Gray
        active -> Accent
        else -> Color.White
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color(0x33222222) else Color.Transparent)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RailIconAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) Color.White else Color.Gray
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FocusReticle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(46.dp)
            .border(2.dp, Accent, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("+", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Light)
    }
}

private fun formatDuration(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private fun storageLabel(path: String): String {
    return runCatching {
        val stat = StatFs(path)
        val gb = stat.availableBytes / 1_073_741_824.0
        if (gb >= 10) "%.0f GB".format(gb) else "%.1f GB".format(gb)
    }.getOrDefault("--")
}
