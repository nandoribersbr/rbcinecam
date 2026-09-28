package br.com.rb8digital.rbcinecam.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.provider.MediaStore
import android.util.Range
import android.view.Surface
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPoint
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
import br.com.rb8digital.rbcinecam.core.CameraCapabilities
import br.com.rb8digital.rbcinecam.core.Resolution
import br.com.rb8digital.rbcinecam.core.VideoMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraEngine(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    suspend fun ensureProvider(): ProcessCameraProvider {
        provider?.let { return it }
        val future = ProcessCameraProvider.getInstance(context)
        return suspendCancellableCoroutine { cont ->
            future.addListener({
                runCatching { future.get() }
                    .onSuccess { provider = it; cont.resume(it) }
                    .onFailure { cont.resumeWithException(it) }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    suspend fun discoverCapabilities(): List<CameraCapabilities> {
        val p = ensureProvider()
        return p.availableCameraInfos.mapNotNull { info -> info.toCapabilities() }
    }

    suspend fun bind(
        owner: LifecycleOwner,
        previewView: PreviewView,
        cameraId: String,
        mode: VideoMode
    ): Camera {
        val p = ensureProvider()
        p.unbindAll()

        val selector = CameraSelector.Builder()
            .addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == cameraId } }
            .build()

        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setTargetFrameRate(Range(mode.fps, mode.fps))
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val quality = when (mode.resolution) {
            Resolution.UHD4K -> Quality.UHD
            Resolution.FHD -> Quality.FHD
            Resolution.HD -> Quality.HD
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality))
            .build()
        val video = VideoCapture.Builder(recorder)
            .setTargetFrameRate(Range(mode.fps, mode.fps))
            .build()

        val bound = p.bindToLifecycle(owner, selector, preview, video)
        camera = bound
        videoCapture = video
        return bound
    }

    fun setZoom(ratio: Float) {
        val c = camera ?: return
        val state = c.cameraInfo.zoomState.value ?: return
        c.cameraControl.setZoomRatio(ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio))
    }

    fun tapToFocus(point: MeteringPoint) {
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun applyManual(
        iso: Int?,
        shutterNs: Long?,
        evIndex: Int?,
        awbMode: Int?,
        focusDistance: Float?
    ) {
        val c = camera ?: return
        val builder = CaptureRequestOptions.Builder()
        if (iso != null || shutterNs != null) {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            iso?.let { builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it) }
            shutterNs?.let { builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it) }
        }
        evIndex?.let { builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, it) }
        awbMode?.let { builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, it) }
        focusDistance?.let {
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, it)
        }
        Camera2CameraControl.from(c.cameraControl).setCaptureRequestOptions(builder.build())
    }

    fun resetAuto() {
        val c = camera ?: return
        Camera2CameraControl.from(c.cameraControl).clearCaptureRequestOptions()
    }

    fun startRecording(onEvent: (VideoRecordEvent) -> Unit): Recording? {
        if (recording != null) return recording
        val vc = videoCapture ?: return null
        val name = "RB_CineCam_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/RB CineCam")
        }
        val output = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

        var pending: PendingRecording = vc.output.prepareRecording(context, output)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pending = pending.withAudioEnabled()
        }
        val rec = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            onEvent(event)
            if (event is VideoRecordEvent.Finalize) recording = null
        }
        recording = rec
        return rec
    }

    fun stopRecording() {
        recording?.stop()
    }

    fun release() {
        recording?.stop()
        recording = null
        provider?.unbindAll()
        camera = null
        videoCapture = null
    }

    private fun CameraInfo.toCapabilities(): CameraCapabilities? {
        val c2 = Camera2CameraInfo.from(this)
        val id = c2.cameraId
        val qualities = QualitySelector.getSupportedQualities(this)

        val fpsRanges = c2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.flatMap { r -> listOf(r.lower, r.upper) }
            ?.filter { it in setOf(24, 25, 30, 50, 60) }
            ?.toSet()
            .orEmpty().ifEmpty { setOf(30) }

        val modes = buildSet {
            if (Quality.UHD in qualities) fpsRanges.forEach { add(VideoMode(Resolution.UHD4K, it)) }
            if (Quality.FHD in qualities) fpsRanges.forEach { add(VideoMode(Resolution.FHD, it)) }
            if (Quality.HD in qualities) fpsRanges.forEach { add(VideoMode(Resolution.HD, it)) }
        }
        if (modes.isEmpty()) return null

        val iso = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposure = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val ev = c2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val minFocus = c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
        val zoom = zoomState.value

        return CameraCapabilities(
            cameraId = id,
            supportedModes = modes,
            minZoom = zoom?.minZoomRatio ?: 1f,
            maxZoom = zoom?.maxZoomRatio ?: 1f,
            isoRange = iso?.let { it.lower..it.upper },
            exposureTimeNs = exposure?.let { it.lower..it.upper },
            evRange = ev?.let { it.lower..it.upper },
            supportsManualFocus = minFocus > 0f
        )
    }
}
