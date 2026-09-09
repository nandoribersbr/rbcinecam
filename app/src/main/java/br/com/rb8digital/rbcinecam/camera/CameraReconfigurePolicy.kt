package br.com.rb8digital.rbcinecam.camera

object CameraReconfigurePolicy {
    fun safeFpsAfterQualityChange(currentFps: Int, supported: List<Int>): Int {
        val valid = supported.distinct().sorted()
        if (valid.isEmpty()) return 30
        if (30 in valid) return 30
        return valid.minByOrNull { kotlin.math.abs(it - 30) } ?: currentFps
    }

    fun fallbackQuality(requested: String, supported: List<String>): String {
        val valid = supported.distinct()
        if (requested != "FHD" && "FHD" in valid) return "FHD"
        if (requested != "HD" && "HD" in valid) return "HD"
        return valid.firstOrNull { it != requested } ?: requested
    }
}