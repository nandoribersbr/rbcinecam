package br.com.rb8digital.rbcinecam.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Reads real PCM samples from the device microphone and reports dBFS levels.
 * If Android cannot open/share the microphone, the monitor reports unavailable
 * instead of producing synthetic meter animation.
 */
class AudioLevelMonitor(private val context: Context) {
    data class Level(
        val leftDb: Float,
        val rightDb: Float,
        val peakDb: Float,
        val clipping: Boolean,
        val available: Boolean
    )

    private val running = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null

    fun start(onLevel: (Level) -> Unit) {
        if (running.getAndSet(true)) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            running.set(false)
            onLevel(unavailable())
            return
        }

        val sampleRate = 48_000
        val stereoMask = AudioFormat.CHANNEL_IN_STEREO
        val monoMask = AudioFormat.CHANNEL_IN_MONO
        val stereoMin = AudioRecord.getMinBufferSize(sampleRate, stereoMask, AudioFormat.ENCODING_PCM_16BIT)
        val monoMin = AudioRecord.getMinBufferSize(sampleRate, monoMask, AudioFormat.ENCODING_PCM_16BIT)
        val useStereo = stereoMin > 0
        val channelMask = if (useStereo) stereoMask else monoMask
        val minBuffer = if (useStereo) stereoMin else monoMin

        if (minBuffer <= 0) {
            running.set(false)
            onLevel(unavailable())
            return
        }

        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelMask,
                AudioFormat.ENCODING_PCM_16BIT,
                (minBuffer * 2).coerceAtLeast(4096)
            )
        }.getOrNull()

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            running.set(false)
            onLevel(unavailable())
            return
        }

        audioRecord = record
        runCatching { record.startRecording() }.onFailure {
            stop()
            onLevel(unavailable())
            return
        }

        worker = thread(name = "RBCineCam-AudioMeter", isDaemon = true) {
            val buffer = ShortArray(2048)
            val left = ShortArray(1024)
            val right = ShortArray(1024)
            while (running.get()) {
                val read = runCatching { record.read(buffer, 0, buffer.size) }.getOrDefault(AudioRecord.ERROR_INVALID_OPERATION)
                if (read <= 0) {
                    onLevel(unavailable())
                    break
                }

                if (useStereo) {
                    var frames = 0
                    var i = 0
                    while (i + 1 < read && frames < left.size) {
                        left[frames] = buffer[i]
                        right[frames] = buffer[i + 1]
                        frames++
                        i += 2
                    }
                    val l = AudioMeterMath.dbfs(left, frames)
                    val r = AudioMeterMath.dbfs(right, frames)
                    val peak = maxOf(AudioMeterMath.peakDbfs(left, frames), AudioMeterMath.peakDbfs(right, frames))
                    onLevel(Level(l, r, peak, AudioMeterMath.isClipping(peak), true))
                } else {
                    val db = AudioMeterMath.dbfs(buffer, read)
                    val peak = AudioMeterMath.peakDbfs(buffer, read)
                    onLevel(Level(db, db, peak, AudioMeterMath.isClipping(peak), true))
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        val record = audioRecord
        audioRecord = null
        runCatching { record?.stop() }
        runCatching { record?.release() }
        worker = null
    }

    private fun unavailable() = Level(-60f, -60f, -60f, false, false)
}