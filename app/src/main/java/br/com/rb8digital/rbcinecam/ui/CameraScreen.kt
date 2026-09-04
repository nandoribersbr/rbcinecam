package br.com.rb8digital.rbcinecam.ui

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.rb8digital.rbcinecam.camera.RBCameraController

@Composable
fun CameraScreen(cameraPermissionGranted: Boolean, audioPermissionGranted: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { RBCameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("PRONTO") }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
            if (cameraPermissionGranted) {
                AndroidView(Modifier.fillMaxSize(), factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = this
                        controller.bind(owner, this)
                    }
                })
            } else {
                Text("Autorize a câmera para iniciar o RB CineCam.", Modifier.align(Alignment.Center), color = Color.White)
            }

            if (cameraPermissionGranted) {
                Row(Modifier.fillMaxWidth().background(Color(0xB3000000)).padding(12.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (recording) "● REC" else "RB CINECAM", color = if (recording) Color.Red else Color.White, fontWeight = FontWeight.Bold)
                    Text("1080p  |  30 FPS  |  180°  |  H.264", color = Color.White)
                    Text(status, color = if (recording) Color.Red else Color.White)
                }

                Column(Modifier.fillMaxWidth().background(Color(0xB3000000)).padding(12.dp).align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Control("ISO", "AUTO")
                        Control("SHUTTER", "180°")
                        Control("WB", "AUTO")
                        Control("FOCUS", "AF")
                        Control("EV", "0.0")
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { previewView?.let { controller.switchLens(owner, it) } }, enabled = !recording) { Text("LENTE") }
                        Button(onClick = {
                            if (recording) {
                                controller.stopRecording(); recording = false; status = "SALVANDO"
                            } else {
                                controller.startRecording(audioPermissionGranted) { event ->
                                    status = when (event) {
                                        is androidx.camera.video.VideoRecordEvent.Start -> "REC"
                                        is androidx.camera.video.VideoRecordEvent.Finalize -> { recording = false; if (event.hasError()) "ERRO" else "SALVO" }
                                        else -> status
                                    }
                                }
                                recording = true
                            }
                        }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = if (recording) Color.DarkGray else Color.Red), modifier = Modifier.size(72.dp)) { Text(if (recording) "STOP" else "REC") }
                        Text("ZEBRA  •  PEAK", color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun Control(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
