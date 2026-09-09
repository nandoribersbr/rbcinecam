package br.com.rb8digital.rbcinecam.ui

enum class TopHudDensity10 { FULL, COMPACT, CRITICAL }

data class TopHudSpec10(
    val density: TopHudDensity10,
    val showCodec: Boolean,
    val showBattery: Boolean,
    val showFullBrand: Boolean,
    val showRecordingStatus: Boolean
)

object TopHudPolicy10 {
    fun forWidthDp(widthDp: Float, recording: Boolean): TopHudSpec10 = when {
        widthDp >= 780f -> TopHudSpec10(
            density = TopHudDensity10.FULL,
            showCodec = true,
            showBattery = true,
            showFullBrand = true,
            showRecordingStatus = true
        )
        widthDp >= 600f -> TopHudSpec10(
            density = TopHudDensity10.COMPACT,
            showCodec = true,
            showBattery = true,
            showFullBrand = false,
            showRecordingStatus = true
        )
        else -> TopHudSpec10(
            density = TopHudDensity10.CRITICAL,
            showCodec = false,
            showBattery = false,
            showFullBrand = false,
            showRecordingStatus = true
        )
    }
}