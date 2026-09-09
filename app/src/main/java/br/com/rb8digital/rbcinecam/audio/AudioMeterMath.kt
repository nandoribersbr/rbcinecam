package br.com.rb8digital.rbcinecam.audio

import kotlin.math.log10
import kotlin.math.sqrt

object AudioMeterMath {
    private const val FLOOR_DB = -60f

    fun dbfs(samples: ShortArray, length: Int = samples.size): Float {
        if (length <= 0) return FLOOR_DB
        var sumSquares = 0.0
        for (i in 0 until length.coerceAtMost(samples.size)) {
            val normalized = samples[i].toDouble() / 32768.0
            sumSquares += normalized * normalized
        }
        val rms = sqrt(sumSquares / length.coerceAtMost(samples.size))
        if (rms <= 0.000001) return FLOOR_DB
        return (20.0 * log10(rms)).toFloat().coerceIn(FLOOR_DB, 0f)
    }

    fun peakDbfs(samples: ShortArray, length: Int = samples.size): Float {
        if (length <= 0) return FLOOR_DB
        var peak = 0
        for (i in 0 until length.coerceAtMost(samples.size)) {
            val v = kotlin.math.abs(samples[i].toInt())
            if (v > peak) peak = v
        }
        if (peak <= 0) return FLOOR_DB
        return (20.0 * log10(peak / 32768.0)).toFloat().coerceIn(FLOOR_DB, 0f)
    }

    fun amplitudeDbfs(amplitude: Double): Float {
        if (amplitude <= 0.000001) return FLOOR_DB
        return (20.0 * log10(amplitude.coerceIn(0.0, 1.0))).toFloat().coerceIn(FLOOR_DB, 0f)
    }

    fun isClipping(peakDbfs: Float): Boolean = peakDbfs >= -1f
}
