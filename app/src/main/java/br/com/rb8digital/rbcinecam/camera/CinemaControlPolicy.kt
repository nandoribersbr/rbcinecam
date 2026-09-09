package br.com.rb8digital.rbcinecam.camera

import kotlin.math.roundToInt

object CinemaControlPolicy {
    private val preferredFrameRates = listOf(24, 25, 30, 50, 60, 120)

    fun shutterDenominator(frameRate: Int, angle: Int): Int {
        require(frameRate > 0)
        require(angle in 1..360)
        return (frameRate * 360.0 / angle).roundToInt()
    }

    fun availableFrameRates(candidateRates: List<Int>, maxFps: Int): List<Int> =
        preferredFrameRates.filter { it <= maxFps && candidateRates.contains(it) }

    fun lensLabel(zoom: Float): String {
        val rounded = if (kotlin.math.abs(zoom - zoom.roundToInt()) < 0.05f) {
            zoom.roundToInt().toString()
        } else {
            "%.1f".format(java.util.Locale.US, zoom)
        }
        return "$rounded×"
    }

    fun exposureTimeNs(frameRate: Int, angleDegrees: Double): Long {
        require(frameRate > 0)
        require(angleDegrees > 0.0 && angleDegrees <= 360.0)
        val frameDurationNs = 1_000_000_000.0 / frameRate
        return (frameDurationNs * angleDegrees / 360.0).toLong()
    }
}