package br.com.rb8digital.rbcinecam.camera

import br.com.rb8digital.rbcinecam.core.CameraCapabilities
import br.com.rb8digital.rbcinecam.core.CameraSettings
import br.com.rb8digital.rbcinecam.core.Resolution
import br.com.rb8digital.rbcinecam.core.VideoMode

data class CameraUiState(
    val ready: Boolean = false,
    val recording: Boolean = false,
    val elapsedMs: Long = 0,
    val settings: CameraSettings = CameraSettings("", VideoMode(Resolution.FHD, 30)),
    val cameras: List<CameraCapabilities> = emptyList(),
    val activeCapabilities: CameraCapabilities? = null,
    val zoomRatio: Float = 1f,
    val iso: Int? = null,
    val shutterNs: Long? = null,
    val ev: Int = 0,
    val wbMode: Int? = null,
    val manualFocus: Boolean = false,
    val focusDistance: Float? = null,
    val grid: Boolean = true,
    val aspectRatio: String = "16:9",
    val audioLevel: Float = 0f,
    val warning: String? = null,
    val lastVideoUri: String? = null
)
