package br.com.rb8digital.rbcinecam.camera

import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RBCameraController(private val context: Context) {
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var recording: Recording? = null
    private lateinit var videoCapture: VideoCapture<Recorder>

    fun bind(owner: LifecycleOwner, previewView: PreviewView, quality: Quality = Quality.FHD) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
            videoCapture = VideoCapture.withOutput(recorder)
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            provider.unbindAll()
            provider.bindToLifecycle(owner, selector, preview, videoCapture)
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchLens(owner: LifecycleOwner, previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        bind(owner, previewView)
    }

    fun startRecording(withAudio: Boolean, onEvent: (VideoRecordEvent) -> Unit) {
        if (!::videoCapture.isInitialized || recording != null) return
        val name = "RBCineCam_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
        val values = android.content.ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/RB CineCam")
        }
        val output = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values)
            .build()
        var pending: PendingRecording = videoCapture.output.prepareRecording(context, output)
        if (withAudio) pending = pending.withAudioEnabled()
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            onEvent(event)
            if (event is VideoRecordEvent.Finalize) recording = null
        }
    }

    fun stopRecording() {
        recording?.stop()
    }

    fun isRecording(): Boolean = recording != null
}
