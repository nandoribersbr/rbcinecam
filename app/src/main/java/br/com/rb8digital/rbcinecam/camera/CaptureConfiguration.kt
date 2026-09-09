package br.com.rb8digital.rbcinecam.camera

import kotlin.math.abs

data class CaptureConfiguration(
    val quality: String = "FHD",
    val fps: Int = 30
) {
    fun request(quality: String = this.quality, fps: Int = this.fps) =
        CaptureConfiguration(quality, fps)

    companion object {
        fun safeFps(supported: List<Int>): Int {
            val valid = supported.distinct().sorted()
            if (valid.isEmpty()) return 30
            if (30 in valid) return 30
            return valid.minByOrNull { abs(it - 30) } ?: 30
        }
    }
}
