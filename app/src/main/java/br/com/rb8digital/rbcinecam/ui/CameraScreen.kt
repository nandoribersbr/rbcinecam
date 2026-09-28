package br.com.rb8digital.rbcinecam.ui

import android.hardware.camera2.CaptureRequest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rb8digital.rbcinecam.camera.CameraUiState
import br.com.rb8digital.rbcinecam.camera.CameraViewModel
import br.com.rb8digital.rbcinecam.core.VideoMode

@Composable
fun CameraScreen(onGallery: () -> Unit, vm: CameraViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PreviewView(context).also { view ->
                    view.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = view
                    vm.initialize(owner, view)
                }
            },
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { p -> previewView?.let { vm.focusAt(it, p.x, p.y) } }
            }
        )

        AspectMask(state.aspectRatio)
        if (state.grid) ThirdsGrid()

        Column(Modifier.fillMaxSize()) {
            TechnicalHud(
                state = state,
                onMode = { mode -> previewView?.let { vm.requestMode(owner, it, mode) } },
                onCamera = { previewView?.let { vm.switchCamera(owner, it) } },
                onGrid = { vm.setGrid(!state.grid) },
                onAspect = vm::setAspectRatio
            )
            Spacer(Modifier.weight(1f))
            state.warning?.let {
                Surface(color = Color.Black.copy(alpha = .7f), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(it, color = Color.White, modifier = Modifier.padding(8.dp))
                }
            }
            BottomControls(
                recording = state.recording,
                elapsedMs = state.elapsedMs,
                audioLevel = state.audioLevel,
                onRecord = vm::toggleRecording,
                onGallery = onGallery,
                onAuto = vm::resetAuto,
                onWb = { vm.setWb(CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT) }
            )
        }
    }
}

@Composable
private fun TechnicalHud(
    state: CameraUiState,
    onMode: (VideoMode) -> Unit,
    onCamera: () -> Unit,
    onGrid: () -> Unit,
    onAspect: (String) -> Unit
) {
    Surface(color = Color.Black.copy(alpha = .88f)) {
        LazyRow(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { Text("RB CINECAM", color = Color.White) }
            items(state.activeCapabilities?.supportedModes?.sortedWith(compareBy<VideoMode> { it.resolution.ordinal }.thenBy { it.fps }) ?: emptyList()) { mode ->
                AssistChip(onClick = { onMode(mode) }, label = { Text(mode.label()) })
            }
            item { AssistChip(onClick = onCamera, label = { Icon(Icons.Default.Cameraswitch, null) }) }
            item { AssistChip(onClick = onGrid, label = { Icon(Icons.Default.GridOn, null) }) }
            items(listOf("16:9", "1.85:1", "2.00:1", "2.39:1", "Large Format")) { ratio ->
                AssistChip(onClick = { onAspect(ratio) }, label = { Text(ratio) })
            }
            item { Text(if (state.manualFocus) "MF" else "AF", color = Color.White) }
            item { Text(state.iso?.let { "ISO $it" } ?: "ISO AUTO", color = Color.White) }
            item { Text(if (state.shutterNs != null) "SHUTTER M" else "SHUTTER AUTO", color = Color.White) }
        }
    }
}

@Composable
private fun BottomControls(
    recording: Boolean,
    elapsedMs: Long,
    audioLevel: Float,
    onRecord: () -> Unit,
    onGallery: () -> Unit,
    onAuto: () -> Unit,
    onWb: () -> Unit
) {
    Surface(color = Color.Black.copy(alpha = .9f)) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onGallery) { Icon(Icons.Default.PhotoLibrary, null, tint = Color.White) }
            Text(if (recording) "REC ${formatElapsed(elapsedMs)}" else "STBY", color = if (recording) Color.Red else Color.White)
            LinearProgressIndicator(progress = { audioLevel }, modifier = Modifier.width(110.dp))
            TextButton(onClick = onAuto) { Text("AUTO") }
            TextButton(onClick = onWb) { Text("WB") }
            Button(onClick = onRecord, colors = ButtonDefaults.buttonColors(containerColor = if (recording) Color.DarkGray else Color.Red)) {
                Text(if (recording) "STOP" else "REC")
            }
        }
    }
}

@Composable
private fun ThirdsGrid() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color.White.copy(alpha = .35f)
        drawLine(c, Offset(size.width / 3, 0f), Offset(size.width / 3, size.height), 1f)
        drawLine(c, Offset(size.width * 2 / 3, 0f), Offset(size.width * 2 / 3, size.height), 1f)
        drawLine(c, Offset(0f, size.height / 3), Offset(size.width, size.height / 3), 1f)
        drawLine(c, Offset(0f, size.height * 2 / 3), Offset(size.width, size.height * 2 / 3), 1f)
    }
}

@Composable
private fun AspectMask(ratio: String) {
    if (ratio == "16:9") return
    val fraction = when (ratio) {
        "1.85:1" -> .035f
        "2.00:1" -> .075f
        "2.39:1" -> .13f
        "Large Format" -> .02f
        else -> 0f
    }
    if (fraction <= 0f) return
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(fraction).background(Color.Black.copy(alpha = .55f)))
        Spacer(Modifier.weight(1f - fraction * 2f))
        Box(Modifier.fillMaxWidth().weight(fraction).background(Color.Black.copy(alpha = .55f)))
    }
}

private fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}

private fun VideoMode.label() = "${resolution.name.replace("UHD4K", "4K")} ${fps}"
