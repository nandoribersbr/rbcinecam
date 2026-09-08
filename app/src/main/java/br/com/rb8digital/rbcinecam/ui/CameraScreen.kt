package br.com.rb8digital.rbcinecam.ui

import android.content.Intent
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.rb8digital.rbcinecam.camera.RBCameraController

private val Panel = Color(0xDD090B0E)
private val PanelSoft = Color(0xC414171B)
private val Accent = Color(0xFFFFC400)
private val Red = Color(0xFFFF2020)

@Composable
fun CameraScreen(cameraPermissionGranted: Boolean, audioPermissionGranted: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { RBCameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("PRONTO") }
    var iso by remember { mutableStateOf("AUTO") }
    var shutter by remember { mutableStateOf("AUTO") }
    var wb by remember { mutableStateOf("AUTO") }
    var focus by remember { mutableStateOf("AF") }
    var ev by remember { mutableStateOf("0") }

    MaterialTheme(colorScheme = darkColorScheme()) {
        if (!cameraPermissionGranted) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("Autorize a câmera para iniciar o RB CineCam.", color = Color.White)
            }
            return@MaterialTheme
        }

        Row(Modifier.fillMaxSize().background(Color.Black)) {
            LeftRail(
                onPhoto = {
                    controller.takePhoto { _, message -> status = message }
                },
                onGallery = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                        )
                    }.onFailure { status = "GALERIA INDISPONÍVEL" }
                },
                enabled = !recording
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
                    modifier = Modifier.fillMaxSize()
                )

                TopHud(
                    recording = recording,
                    status = status,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                FocusReticle(Modifier.align(Alignment.Center))

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
                recording = recording,
                onSwitch = {
                    previewView?.let {
                        controller.switchLens(owner, it)
                        iso = "AUTO"; shutter = "AUTO"; wb = "AUTO"; focus = "AF"; ev = "0"
                        status = "CÂMERA TROCADA"
                    }
                },
                onRecord = {
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
            )
        }
    }
}

@Composable
private fun LeftRail(onPhoto: () -> Unit, onGallery: () -> Unit, enabled: Boolean) {
    Column(
        Modifier.width(82.dp).fillMaxHeight().background(Panel).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("RB", color = Red, fontWeight = FontWeight.Black, fontSize = 24.sp)
        RailStatus("●", "VÍDEO", active = true)
        RailAction("▣", "FOTO", enabled, onPhoto)
        RailAction("▶", "GALERIA", enabled, onGallery)
    }
}

@Composable
private fun RightRail(recording: Boolean, onSwitch: () -> Unit, onRecord: () -> Unit) {
    Column(
        Modifier.width(96.dp).fillMaxHeight().background(Panel).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        RailAction("↻", "TROCAR", !recording, onSwitch)
        Box(
            Modifier
                .size(78.dp)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .padding(7.dp)
                .clip(CircleShape)
                .background(if (recording) Color(0xFF7A2020) else Red)
                .clickable { onRecord() },
            contentAlignment = Alignment.Center
        ) {
            Text(if (recording) "■" else "", color = Color.White, fontSize = 24.sp)
        }
        Text(if (recording) "STOP" else "REC", color = if (recording) Red else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TopHud(recording: Boolean, status: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(Panel).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("RB ", color = Red, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text("CineCam", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.width(18.dp))
        Text("1080p  •  30 FPS  •  MP4", color = Color.White, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(
            if (recording) "● REC" else status,
            color = if (recording) Red else Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
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
        modifier.fillMaxWidth().background(Panel).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ManualControl("ISO", iso, enabled, onIso, Modifier.weight(1f))
        ManualControl("SHUTTER", shutter, enabled, onShutter, Modifier.weight(1f))
        ManualControl("WB", wb, enabled, onWb, Modifier.weight(1f))
        ManualControl("FOCUS", focus, enabled, onFocus, Modifier.weight(1f))
        ManualControl("EV", ev, enabled, onEv, Modifier.weight(1f))
    }
}

@Composable
private fun ManualControl(label: String, value: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PanelSoft)
            .border(1.dp, Color(0xFF34383E), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = Color.LightGray, fontSize = 11.sp)
        Text(value, color = if (enabled) Accent else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
    }
}

@Composable
private fun RailAction(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(symbol, color = if (enabled) Color.White else Color.Gray, fontSize = 26.sp)
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (enabled) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RailStatus(symbol: String, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(symbol, color = if (active) Accent else Color.Gray, fontSize = 24.sp)
        Text(label, color = if (active) Accent else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FocusReticle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(72.dp)
            .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("+", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
    }
}
