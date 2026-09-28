package br.com.rb8digital.rbcinecam

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import br.com.rb8digital.rbcinecam.gallery.GalleryScreen
import br.com.rb8digital.rbcinecam.gallery.PlayerScreen
import br.com.rb8digital.rbcinecam.ui.CameraScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RBCineCamApp() }
    }
}

private sealed interface Screen {
    data object Camera : Screen
    data object Gallery : Screen
    data class Player(val uri: Uri) : Screen
}

@Composable
private fun RBCineCamApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var screen by remember { mutableStateOf<Screen>(Screen.Camera) }
    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionsGranted = result[Manifest.permission.CAMERA] == true && result[Manifest.permission.RECORD_AUDIO] == true
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            if (!permissionsGranted) {
                PermissionScreen { launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }
            } else {
                when (val s = screen) {
                    Screen.Camera -> CameraScreen(onGallery = { screen = Screen.Gallery })
                    Screen.Gallery -> GalleryScreen(onBack = { screen = Screen.Camera }, onOpen = { screen = Screen.Player(it) })
                    is Screen.Player -> PlayerScreen(s.uri, onBack = { screen = Screen.Gallery })
                }
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("RB CineCam precisa de câmera e microfone para gravar vídeo com áudio.", color = Color.White)
            Button(onClick = onRequest) { Text("Conceder permissões") }
        }
    }
}
