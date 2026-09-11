package br.com.rb8digital.rbcinecam.camera

object LargeFormatPolicy {
    val modes = listOf("LF 1.90", "LF 1.43")

    fun preferredFps(supported: List<Int>): Int {
        val valid = supported.distinct().sorted()
        if (24 in valid) return 24
        if (30 in valid) return 30
        return CaptureConfiguration.safeFps(valid)
    }

    fun preferredQuality(supported: List<String>): String {
        val rank = mapOf("SD" to 0, "HD" to 1, "FHD" to 2, "QHD" to 3, "UHD" to 4, "4K" to 4, "8K" to 5)
        return supported.distinct().maxByOrNull { rank[it.uppercase()] ?: -1 } ?: "FHD"
    }

    fun aspectRatio(mode: String): Float = when (mode) {
        "LF 1.43" -> 1.43f
        else -> 1.90f
    }
}
