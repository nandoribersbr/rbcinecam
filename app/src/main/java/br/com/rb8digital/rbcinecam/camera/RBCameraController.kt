package br.com.rb8digital.rbcinecam.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import kotlin.math.roundToInt

@OptIn(ExperimentalCamera2Interop::class)
class RBCameraController(private val context: Context) {
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var recording: Recording? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var imageCapture: ImageCapture
    private var camera: Camera? = null
    private var camera2Control: Camera2CameraControl? = null

    private val activeFrameRate = 30
    private val isoOptions = listOf<Int?>(null, 100, 200, 400, 800, 1600)
    private val shutterAngles = listOf<Double?>(null, 90.0, 144.0, 172.8, 180.0, 270.0, 360.0)
    private val shutterLabels = listOf("AUTO", "90°", "144°", "172.8°", "180°", "270°", "360°")
    private val wbModes = listOf(
        CaptureRequest.CONTROL_AWB_MODE_AUTO,
        CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
        CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
        CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
        CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
    )
    private val wbLabels = listOf("AUTO", "DIA", "NUBLADO", "TUNGSTÊNIO", "FLUOR")
    private val focusFractions = listOf<Float?>(null, 0f, 0.25f, 0.5f, 0.75f, 1f)
    private val focusLabels = listOf("AF", "∞", "25%", "50%", "75%", "MACRO")

    private var isoIndex = 0
    private var shutterIndex = 0
    private var wbIndex = 0
    private var focusIndex = 0
    private var evStops = 0

    fun bind(owner: LifecycleOwner, previewView: PreviewView, quality: Quality = Quality.FHD) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(quality))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            provider.unbindAll()
            camera = provider.bindToLifecycle(owner, selector, preview, videoCapture, imageCapture)
            camera2Control = camera?.cameraControl?.let { Camera2CameraControl.from(it) }
            resetManualState()
            applyManualControls()
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchLens(owner: LifecycleOwner, previewView: PreviewView) {
        if (recording != null) return
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bind(owner, previewView)
    }

    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        val activeCamera = camera ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).build()
        activeCamera.cameraControl.startFocusAndMetering(action)
    }

    fun startRecording(withAudio: Boolean, onEvent: (VideoRecordEvent) -> Unit) {
        if (!::videoCapture.isInitialized || recording != null) return

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "RBCineCam_${timestamp()}")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/RB CineCam")
        }
        val output = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

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

    fun takePhoto(onResult: (Boolean, String) -> Unit) {
        if (!::imageCapture.isInitialized || recording != null) {
            onResult(false, "CÂMERA OCUPADA")
            return
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "RBCineCam_${timestamp()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RB CineCam")
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onResult(true, "FOTO SALVA")
                }

                override fun onError(exception: ImageCaptureException) {
                    onResult(false, "ERRO NA FOTO")
                }
            }
        )
    }

    fun cycleIso(): String {
        isoIndex = (isoIndex + 1) % isoOptions.size
        applyManualControls()
        return isoOptions[isoIndex]?.toString() ?: "AUTO"
    }

    fun cycleShutter(): String {
        shutterIndex = (shutterIndex + 1) % shutterAngles.size
        applyManualControls()
        return shutterLabels[shutterIndex]
    }

    fun cycleWhiteBalance(): String {
        wbIndex = (wbIndex + 1) % wbModes.size
        applyManualControls()
        return wbLabels[wbIndex]
    }

    fun cycleFocus(): String {
        focusIndex = (focusIndex + 1) % focusFractions.size
        applyManualControls()
        return focusLabels[focusIndex]
    }

    fun cycleEv(): String {
        evStops++
        if (evStops > 2) evStops = -2

        val activeCamera = camera ?: return formatEv()
        val exposureState = activeCamera.cameraInfo.exposureState
        if (!exposureState.isExposureCompensationSupported) return "N/D"

        val step = exposureState.exposureCompensationStep.toFloat()
        val desiredIndex = if (step > 0f) (evStops / step).roundToInt() else 0
        val range = exposureState.exposureCompensationRange
        activeCamera.cameraControl.setExposureCompensationIndex(
            desiredIndex.coerceIn(range.lower, range.upper)
        )
        return formatEv()
    }

    fun activeFps(): Int = activeFrameRate

    private fun resetManualState() {
        isoIndex = 0
        shutterIndex = 0
        wbIndex = 0
        focusIndex = 0
        evStops = 0
    }

    private fun applyManualControls() {
        val activeCamera = camera ?: return
        val control = camera2Control ?: return
        val cameraInfo = Camera2CameraInfo.from(activeCamera.cameraInfo)
        val builder = CaptureRequestOptions.Builder()

        val iso = isoOptions[isoIndex]
        val angle = shutterAngles[shutterIndex]
        val shutterNs = angle?.let { CinemaControlPolicy.exposureTimeNs(activeFrameRate, it) }

        if (iso == null && shutterNs == null) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )
        } else {
            val sensitivityRange = cameraInfo.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
            )
            val exposureRange = cameraInfo.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
            )
            val safeIso = (iso ?: 100).let { value ->
                sensitivityRange?.let { value.coerceIn(it.lower, it.upper) } ?: value
            }
            val safeShutter = (shutterNs ?: 16_666_667L).let { value ->
                exposureRange?.let { value.coerceIn(it.lower, it.upper) } ?: value
            }

            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF
            )
            builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, safeIso)
            builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, safeShutter)
        }

        builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, wbModes[wbIndex])

        val focusFraction = focusFractions[focusIndex]
        if (focusFraction == null) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
        } else {
            val minFocus = cameraInfo.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
            ) ?: 0f
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            builder.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                minFocus * focusFraction
            )
        }

        control.setCaptureRequestOptions(builder.build())
    }

    private fun formatEv(): String = if (evStops > 0) "+$evStops" else evStops.toString()

    private fun timestamp(): String = SimpleDateFormat(
        "yyyyMMdd_HHmmss",
        Locale.US
    ).format(Date())
}
