package br.com.rb8digital.rbcinecam.monitoring

object ScopeMath {
    fun histogramBin(value: Int, bins: Int = 64): Int {
        val v = value.coerceIn(0, 255)
        return ((v * bins) / 256).coerceIn(0, bins - 1)
    }

    fun waveformX(x: Int, width: Int, bins: Int = 64): Int {
        if (width <= 1) return 0
        return ((x.toLong() * bins) / width).toInt().coerceIn(0, bins - 1)
    }

    fun waveformY(luma: Int, bins: Int = 64): Int {
        val b = histogramBin(luma, bins)
        return (bins - 1 - b).coerceIn(0, bins - 1)
    }
}
