package br.com.rb8digital.rbcinecam.camera

object CaptureSettingsPolicy {
    val aspectRatios = listOf("16:9", "17:9", "2.00:1", "2.35:1", "2.39:1", "9:16", "4:3", "1:1")

    fun nextFps(current: Int, supported: List<Int>): Int {
        if (supported.isEmpty()) return current
        val sorted = supported.distinct().sorted()
        val i = sorted.indexOf(current)
        return if (i < 0 || i == sorted.lastIndex) sorted.first() else sorted[i + 1]
    }

    fun nextQuality(current: String, supported: List<String>): String {
        if (supported.isEmpty()) return current
        val i = supported.indexOf(current)
        return if (i < 0 || i == supported.lastIndex) supported.first() else supported[i + 1]
    }

    fun nextAspect(current: String): String {
        val i = aspectRatios.indexOf(current)
        return if (i < 0 || i == aspectRatios.lastIndex) aspectRatios.first() else aspectRatios[i + 1]
    }
}