package br.com.rb8digital.rbcinecam.core

class ConfigurationPolicy {
    fun resolve(
        requested: CameraSettings,
        previous: CameraSettings?,
        capabilities: CameraCapabilities
    ): ResolutionResult {
        if (requested.cameraId == capabilities.cameraId && requested.mode in capabilities.supportedModes) {
            return ResolutionResult(requested, false)
        }

        val previousOnSameCamera = previous?.takeIf {
            it.cameraId == capabilities.cameraId && it.mode in capabilities.supportedModes
        }
        if (previousOnSameCamera != null) {
            return ResolutionResult(
                applied = previousOnSameCamera,
                didFallback = true,
                reason = "${requested.mode.label()} indisponível. Mantido ${previousOnSameCamera.mode.label()}."
            )
        }

        val fallbackMode = capabilities.supportedModes
            .sortedWith(compareByDescending<VideoMode> { it.resolution.rank() }.thenByDescending { it.fps })
            .firstOrNull()
            ?: return ResolutionResult(
                applied = requested.copy(cameraId = capabilities.cameraId),
                didFallback = true,
                reason = "Nenhum modo de vídeo compatível foi detectado nesta câmera."
            )

        val fallback = requested.copy(cameraId = capabilities.cameraId, mode = fallbackMode)
        return ResolutionResult(
            applied = fallback,
            didFallback = true,
            reason = "${requested.mode.label()} indisponível nesta câmera. Usando ${fallbackMode.label()}."
        )
    }
}

private fun Resolution.rank() = when (this) {
    Resolution.HD -> 1
    Resolution.FHD -> 2
    Resolution.UHD4K -> 3
}

fun VideoMode.label(): String = "${when (resolution) {
    Resolution.HD -> "HD"
    Resolution.FHD -> "FHD"
    Resolution.UHD4K -> "4K"
}} ${fps} fps"
