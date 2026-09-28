package br.com.rb8digital.rbcinecam.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

class AudioLevelMeter(private val context: Context) {
    suspend fun run(onLevel: (Float) -> Unit) = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return@withContext
        val rate = 16000
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return@withContext
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            min * 2
        )
        val buffer = ShortArray(min)
        try {
            recorder.startRecording()
            while (isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    var peak = 0
                    for (i in 0 until read) peak = max(peak, abs(buffer[i].toInt()))
                    onLevel((peak / 32767f).coerceIn(0f, 1f))
                }
                delay(35)
            }
        } catch (_: Throwable) {
            onLevel(0f)
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }
}
