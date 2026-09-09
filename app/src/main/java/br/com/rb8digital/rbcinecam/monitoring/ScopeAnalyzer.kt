package br.com.rb8digital.rbcinecam.monitoring

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class ScopeAnalyzer(
    private val onFrame: (ScopeFrame) -> Unit
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            if (image.planes.size < 3) return
            val width = image.width
            val height = image.height
            if (width <= 0 || height <= 0) return

            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]
            val yBuffer = yPlane.buffer.duplicate()
            val uBuffer = uPlane.buffer.duplicate()
            val vBuffer = vPlane.buffer.duplicate()

            val rHist = IntArray(64)
            val gHist = IntArray(64)
            val bHist = IntArray(64)
            val waveform = IntArray(64 * 64)

            val stepX = (width / 96).coerceAtLeast(4)
            val stepY = (height / 54).coerceAtLeast(4)

            var y = 0
            while (y < height) {
                var x = 0
                while (x < width) {
                    val yIndex = y * yPlane.rowStride + x * yPlane.pixelStride
                    val uvX = x / 2
                    val uvY = y / 2
                    val uIndex = uvY * uPlane.rowStride + uvX * uPlane.pixelStride
                    val vIndex = uvY * vPlane.rowStride + uvX * vPlane.pixelStride
                    if (yIndex < yBuffer.limit() && uIndex < uBuffer.limit() && vIndex < vBuffer.limit()) {
                        val yy = yBuffer.get(yIndex).toInt() and 0xff
                        val uu = (uBuffer.get(uIndex).toInt() and 0xff) - 128
                        val vv = (vBuffer.get(vIndex).toInt() and 0xff) - 128
                        val c = (yy - 16).coerceAtLeast(0)
                        val r = ((298 * c + 409 * vv + 128) shr 8).coerceIn(0, 255)
                        val g = ((298 * c - 100 * uu - 208 * vv + 128) shr 8).coerceIn(0, 255)
                        val b = ((298 * c + 516 * uu + 128) shr 8).coerceIn(0, 255)

                        rHist[ScopeMath.histogramBin(r)]++
                        gHist[ScopeMath.histogramBin(g)]++
                        bHist[ScopeMath.histogramBin(b)]++

                        val wx = ScopeMath.waveformX(x, width)
                        val wy = ScopeMath.waveformY(yy)
                        waveform[wy * 64 + wx]++
                    }
                    x += stepX
                }
                y += stepY
            }

            onFrame(ScopeFrame(rHist, gHist, bHist, waveform))
        } finally {
            image.close()
        }
    }
}
