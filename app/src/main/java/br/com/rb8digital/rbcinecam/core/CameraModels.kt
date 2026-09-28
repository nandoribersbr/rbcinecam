package br.com.rb8digital.rbcinecam.core

enum class Resolution { HD, FHD, UHD4K }

data class VideoMode(val resolution: Resolution, val fps: Int)

data class CameraSettings(
    val cameraId: String,
    val mode: VideoMode,
    val aspectRatio: String = "16:9",
    val grid: Boolean = true
)

data class CameraCapabilities(
    val cameraId: String,
    val supportedModes: Set<VideoMode>,
    val minZoom: Float = 1f,
    val maxZoom: Float = 1f,
    val isoRange: IntRange? = null,
    val exposureTimeNs: LongRange? = null,
    val evRange: IntRange? = null,
    val supportsManualFocus: Boolean = false
)

data class ResolutionResult(
    val applied: CameraSettings,
    val didFallback: Boolean,
    val reason: String? = null
)
