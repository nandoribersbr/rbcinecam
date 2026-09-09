package br.com.rb8digital.rbcinecam.monitoring

data class ScopeFrame(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val waveform: IntArray,
    val waveformWidth: Int = 64,
    val waveformHeight: Int = 64
) {
    companion object {
        fun empty() = ScopeFrame(
            red = IntArray(64),
            green = IntArray(64),
            blue = IntArray(64),
            waveform = IntArray(64 * 64)
        )
    }
}
