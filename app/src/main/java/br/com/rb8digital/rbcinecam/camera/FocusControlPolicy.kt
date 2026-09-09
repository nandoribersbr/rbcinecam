package br.com.rb8digital.rbcinecam.camera

object FocusControlPolicy {
    fun manualFocusSupported(minFocusDistance: Float?): Boolean = (minFocusDistance ?: 0f) > 0f

    fun distanceForFraction(minFocusDistance: Float, fraction: Float): Float =
        minFocusDistance * fraction.coerceIn(0f, 1f)
}