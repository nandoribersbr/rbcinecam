package br.com.rb8digital.rbcinecam.ui

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.rb8digital.rbcinecam.camera.RBCameraController

@Composable
fun CameraScreen(
    cameraPermissionGranted: Boolean,
    audioPermissionGranted: Boolean
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { RBCameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (cameraPermissionGranted) "PRONTO" else "PERMISSÃO DE CÂMERA") }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (cameraPermissionGranted) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
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
                    }
                )
            } else {
                Text(
                    "Autorize a câmera para usar o RB CineCam.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RB CineCam 0.1 Alpha", color = Color.White)
                Text(status, color = if (recording) Color.Red else Color.White)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp).align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { previewView?.let { controller.switchLens(owner, it) } },
                    enabled = cameraPermissionGranted && !recording
                ) { Text("CÂMERA") }

                Button(
                    onClick = {
                        if (recording) {
                            controller.stopRecording()
                            recording = false
                            status = "SALVANDO"
                        } else {
                            controller.startRecording(withAudio = audioPermissionGranted) { event ->
                                status = when (event) {
                                    is androidx.camera.video.VideoRecordEvent.Start -> "REC"
                                    is androidx.camera.video.VideoRecordEvent.Finalize -> {
                                        recording = false
                                        if (event.hasError()) "ERRO ${event.error}" else "SALVO"
                                    }
                                    else -> status
                                }
                            }
                            recording = true
                        }
                    },
                    enabled = cameraPermissionGranted
                ) { Text(if (recording) "STOP" else "REC") }

                Text("1080p • 30", color = Color.White)
            }
        }
    }
}
