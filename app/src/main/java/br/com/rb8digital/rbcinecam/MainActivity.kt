package br.com.rb8digital.rbcinecam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import br.com.rb8digital.rbcinecam.ui.CameraScreen

class MainActivity : ComponentActivity() {
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { renderCamera() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        renderCamera()
        if (!hasCameraPermission()) permissionLauncher.launch(requiredPermissions)
    }

    private fun renderCamera() {
        val cameraGranted = hasCameraPermission()
        val audioGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            CameraScreen(
                cameraPermissionGranted = cameraGranted,
                audioPermissionGranted = audioGranted
            )
        }
    }

    private fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}
