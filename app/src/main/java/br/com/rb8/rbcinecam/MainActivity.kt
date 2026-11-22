package br.com.rb8.rbcinecam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import android.view.WindowManager
import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.camera.video.VideoRecordEvent.Status
import androidx.camera.video.VideoRecordEvent.Start
import androidx.camera.video.VideoRecordEvent.Pause
import androidx.camera.video.VideoRecordEvent.Resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.provider.MediaStore
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.FocusMeteringAction
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.MediaStoreOutputOptions
import androidx.compose.foundation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.view.PreviewView
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import java.util.Locale
import android.content.ContentValues
import android.provider.MediaStore.Video
import android.provider.MediaStore.Images
import androidx.camera.video.FallbackStrategy
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

private val Ink = Color(0xFF080B10)
private val Panel = Color(0xFF1A2029)
private val Blue = Color(0xFF3B9DFF)
private val Red = Color(0xFFF52C3E)
private val Gold = Color(0xFFFFCF47)

private object TouchProbe {
    var count by mutableIntStateOf(0)
    var last by mutableStateOf("Aguardando toque")
}

class MainActivity: ComponentActivity() {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            TouchProbe.count++
            TouchProbe.last="Toque físico #${TouchProbe.count} (${ev.x.toInt()}, ${ev.y.toInt()})"
            Log.d("RBCineCam","DISPATCH: ${TouchProbe.last}")
        }
        return super.dispatchTouchEvent(ev)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowInsetsControllerCompat(window,window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { CineCamScreen() }
    }
}

@Composable
private fun Icon(name: String, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = remember(name) { ImageRequest.Builder(context)
            .data("file:///android_asset/icons/$name.svg")
            .decoderFactory(SvgDecoder.Factory()).build() },
        contentDescription = label, modifier = modifier.size(21.dp)
    )
}

@Composable
private fun CineButton(icon: String, label: String, enabled: Boolean = true, highlighted: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(45.dp).zIndex(5f)) {
        Surface(onClick={ TouchProbe.last="Botão: $label"; Log.i("RBCineCam","BUTTON $label"); onClick() }, enabled=enabled, shape=RoundedCornerShape(9.dp),
            color=if(highlighted) Blue.copy(alpha=.22f) else Panel) {
            Box(Modifier.size(34.dp), contentAlignment=Alignment.Center) { Icon(icon, label) }
        }
        Text(label, color=if(enabled) Color.White else Color.Gray, style=MaterialTheme.typography.labelSmall,
            maxLines=1, overflow=TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CineCamScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var audioGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val requestPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        cameraGranted = permissions[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    var lens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var grid by remember { mutableStateOf(true) }
    var mic by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var gallery by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(false) }
    var recordingPhase by remember { mutableStateOf(false) }
    var recordingState by remember { mutableStateOf(RecordingUiState.IDLE) }
    var diagnosticsOpen by remember { mutableStateOf(false) }
    var lastVideoBytes by remember { mutableLongStateOf(0L) }
    var freeSpaceMb by remember { mutableLongStateOf(0L) }
    var recordingErrorCode by remember { mutableStateOf<Int?>(null) }
    var recordTapCount by remember { mutableIntStateOf(0) }
    var captureMode by remember { mutableStateOf("iniciando") }
    var notification by remember { mutableStateOf("Inicializando a câmera…") }
    var sessionEpoch by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var hist by remember { mutableStateOf(false) }
    var zebra by remember { mutableStateOf(false) }
    var peaking by remember { mutableStateOf(false) }
    var exposureIndex by remember { mutableIntStateOf(0) }
    var aeLocked by remember { mutableStateOf(false) }
    var aeLockSupported by remember { mutableStateOf(false) }
    var focusLocked by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusStatus by remember { mutableStateOf("AF auto") }
    var exposureStep by remember { mutableFloatStateOf(0f) }
    var manualSensorSupported by remember { mutableStateOf(false) }
    var manualSensorOn by remember { mutableStateOf(false) }
    var isoRange by remember { mutableStateOf<Range<Int>?>(null) }
    var shutterRange by remember { mutableStateOf<Range<Long>?>(null) }
    var isoValue by remember { mutableIntStateOf(100) }
    var shutterNs by remember { mutableLongStateOf(10000000L) }
    var awbModes by remember { mutableStateOf(listOf(CaptureRequest.CONTROL_AWB_MODE_AUTO)) }
    var awbValue by remember { mutableIntStateOf(CaptureRequest.CONTROL_AWB_MODE_AUTO) }
    var manualPanel by remember { mutableStateOf(false) }
    var actualQuality by remember { mutableStateOf(Quality.FHD) }
    var quality by remember { mutableStateOf(Quality.FHD) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var elapsed by remember { mutableIntStateOf(0) }
    var supportedQualities by remember { mutableStateOf(listOf(Quality.FHD)) }
    var availableFront by remember { mutableStateOf(false) }

    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    LaunchedEffect(Unit) {
        while(true) {
            freeSpaceMb = try {
                StatFs(Environment.getExternalStorageDirectory().path).availableBytes / (1024*1024)
            } catch(_:Exception) { 0L }
            delay(3000)
        }
    }
    LaunchedEffect(recordingState) {
        if(recordingState == RecordingUiState.RECORDING) {
            while(recordingState == RecordingUiState.RECORDING) {
                delay(1000)
                elapsed++
            }
        }
    }
    LaunchedEffect(recordingState) {
        if(recordingState == RecordingUiState.PREPARING) {
            delay(12000)
            if(recordingState == RecordingUiState.PREPARING) {
                notification="Sem retorno da câmera. Encerrando tentativa…"
                error="Tempo limite da inicialização de vídeo excedido"
                Log.e("RBCineCam","No Start event received within 12s")
                recordingState=RecordingUiState.STOPPING
                recording?.stop()
                if(recording==null) recordingState=RecordingUiState.ERROR
            }
        }
    }
    LaunchedEffect(cameraGranted, lens, quality, previewView) {
        if (!cameraGranted || previewView == null) return@LaunchedEffect
        val view = previewView ?: return@LaunchedEffect
        captureMode = "iniciando"
        notification = "Abrindo câmera…"
        val provider = try { withContext(Dispatchers.IO) { providerFuture.get() } } catch (ex: Exception) {
            captureMode="erro"
            error = "Erro ao iniciar câmera: ${ex.message}"
            notification=error ?: "Erro"
            return@LaunchedEffect
        }
        try {
            availableFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            val selector = CameraSelector.Builder().requireLensFacing(lens).build()
            val info = provider.availableCameraInfos.firstOrNull {
                selector.filter(listOf(it)).isNotEmpty()
            }
            supportedQualities = if (info != null) QualitySelector.getSupportedQualities(info) else listOf(Quality.FHD)
            val chosen = if (supportedQualities.contains(quality)) quality else supportedQualities.firstOrNull() ?: Quality.SD
            actualQuality=chosen
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            val photo = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            fun recordingUseCase(q: Quality): VideoCapture<Recorder> {
                val recorder=Recorder.Builder().setQualitySelector(
                    QualitySelector.from(q, FallbackStrategy.lowerQualityOrHigherThan(q))
                ).build()
                return VideoCapture.withOutput(recorder)
            }
            provider.unbindAll()
            var workingVideo:VideoCapture<Recorder>? = null
            var workingCamera:Camera? = null
            var workingPhoto:ImageCapture? = null
            // Some devices cannot combine preview+photo+video at the selected resolution.
            // Recording takes priority over still photography.
            val qualities = listOf(chosen,Quality.FHD,Quality.HD,Quality.SD).distinct()
            val diagnostics = mutableListOf<String>()
            for(q in qualities) {
                if(q != chosen && !supportedQualities.contains(q)) continue
                try {
                    val v=recordingUseCase(q)
                    provider.unbindAll()
                    val c=provider.bindToLifecycle(lifecycleOwner,selector,preview,photo,v)
                    workingCamera=c;workingVideo=v;workingPhoto=photo;actualQuality=q
                    captureMode="foto_video"
                    break
                } catch(ex:Exception) {
                    diagnostics.add("foto+vídeo ${q}: ${ex.javaClass.simpleName}")
                }
                try {
                    val v=recordingUseCase(q)
                    provider.unbindAll()
                    val c=provider.bindToLifecycle(lifecycleOwner,selector,preview,v)
                    workingCamera=c;workingVideo=v;workingPhoto=null;actualQuality=q
                    captureMode="video"
                    notification="Modo vídeo: foto indisponível nesta configuração"
                    break
                } catch(ex:Exception) {
                    diagnostics.add("vídeo ${q}: ${ex.javaClass.simpleName}")
                }
            }
            if(workingCamera==null || workingVideo==null) {
                captureMode="erro"
                notification="Câmera não conseguiu habilitar gravação"
                throw IllegalStateException(diagnostics.joinToString("; "))
            }
            val bound=workingCamera
            aeLocked=false
            manualSensorOn=false
            manualPanel=false
            val characteristics=Camera2CameraInfo.from(bound.cameraInfo)
            val capabilities=characteristics.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
            isoRange=characteristics.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            shutterRange=characteristics.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            manualSensorSupported=capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) &&
                isoRange!=null && shutterRange!=null
            isoValue=isoRange?.let { 100.coerceIn(it.lower,it.upper) } ?: 100
            shutterNs=shutterRange?.let { 10000000L.coerceIn(it.lower,it.upper) } ?: 10000000L
            awbModes=characteristics.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
                ?.toList() ?: listOf(CaptureRequest.CONTROL_AWB_MODE_AUTO)
            awbValue=CaptureRequest.CONTROL_AWB_MODE_AUTO
            focusLocked=false
            focusPoint=null
            focusStatus="AF auto"
            aeLockSupported=try {
                Camera2CameraInfo.from(bound.cameraInfo)
                    .getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE)==true
            } catch(ex:Exception) { false }
            camera=bound
            imageCapture=workingPhoto
            videoCapture=workingVideo
            zoom=1f
            exposureIndex=0
            exposureStep=bound.cameraInfo.exposureState.exposureCompensationStep.toFloat()
            view.setOnTouchListener { _, event ->
                if(event.action == MotionEvent.ACTION_UP) {
                    val point=view.meteringPointFactory.createPoint(event.x,event.y)
                    val action=FocusMeteringAction.Builder(point).apply {
                        if(focusLocked) disableAutoCancel()
                    }.build()
                    val future=bound.cameraControl.startFocusAndMetering(action)
                    focusPoint=Offset(event.x,event.y)
                    focusStatus=if(focusLocked) "AF fixado" else "Focalizando…"
                    future.addListener({
                        try {
                            val result=future.get()
                            focusStatus=if(result.isFocusSuccessful) {
                                if(focusLocked) "AF fixado" else "AF confirmado"
                            } else "Foco não confirmado"
                        } catch(ex:Exception) {
                            focusStatus="Falha de foco"
                            Log.w("RBCineCam","Focus failure",ex)
                        }
                    },ContextCompat.getMainExecutor(context))
                    true
                } else true
            }
            error=null
            if(captureMode=="foto_video") notification="Câmera pronta para gravar"
        } catch (ex: Exception) {
            captureMode="erro"
            notification="Falha ao abrir sessão de vídeo"
            error="Configuração de captura incompatível: ${ex.message}"
            if (quality == Quality.UHD) quality = Quality.FHD
            camera=null; imageCapture=null; videoCapture=null
        }
    }

    fun startOrStop() {
        recordTapCount++
        Log.i("RBCineCam","REC tapped #$recordTapCount state=$recordingState capture=$captureMode")
        when(recordingState) {
            RecordingUiState.RECORDING, RecordingUiState.PAUSED -> {
                recordingState=RecordingUiState.STOPPING
                notification="Finalizando gravação…"
                Log.i("RBCineCam","REC stop requested")
                recording?.stop()
                return
            }
            RecordingUiState.PREPARING, RecordingUiState.STOPPING -> {
                notification="Aguarde a operação atual…"
                return
            }
            else -> Unit
        }
        val video=videoCapture
        if(video==null) {
            recordingState=RecordingUiState.ERROR
            error="Gravação indisponível: $captureMode. Reabra o app ou selecione HD."
            notification="Sessão de gravação não preparada"
            Log.e("RBCineCam","REC videoCapture null")
            return
        }
        val values=ContentValues().apply {
            put(Video.Media.DISPLAY_NAME, "RBCineCam_${System.currentTimeMillis()}.mp4")
            put(Video.Media.MIME_TYPE,"video/mp4")
            put(Video.Media.RELATIVE_PATH,"Movies/RBCineCam")
        }
        recordingState=RecordingUiState.PREPARING
        recordingPhase=true
        elapsed=0
        error=null
        recordingErrorCode=null
        notification="Preparando gravação…"
        Log.i("RBCineCam","Preparing recording, mic=${mic && audioGranted} quality=$actualQuality")
        try {
            val options=MediaStoreOutputOptions.Builder(
                context.contentResolver,Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(values).build()
            var pending=video.output.prepareRecording(context,options)
            if(mic && audioGranted) pending=pending.withAudioEnabled()
            recording=pending.start(ContextCompat.getMainExecutor(context)) { event ->
                Log.i("RBCineCam","Recording event ${event.javaClass.simpleName}")
                when(event) {
                    is VideoRecordEvent.Start -> {
                        recordingState=RecordingUiState.RECORDING
                        recordingPhase=false
                        isRecording=true
                        notification="Gravando • toque no botão para parar"
                        error=null
                    }
                    is VideoRecordEvent.Pause -> {
                        recordingState=RecordingUiState.PAUSED
                        notification="Gravação pausada"
                    }
                    is VideoRecordEvent.Resume -> {
                        recordingState=RecordingUiState.RECORDING
                        notification="Gravação retomada"
                    }
                    is VideoRecordEvent.Status -> {
                        lastVideoBytes=event.recordingStats.numBytesRecorded
                        // The callback confirms that a recorder is delivering updates.
                        if(recordingState==RecordingUiState.PREPARING) {
                            recordingState=RecordingUiState.RECORDING
                            recordingPhase=false
                            isRecording=true
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        recording=null
                        recordingPhase=false
                        isRecording=false
                        if(event.hasError()) {
                            recordingState=RecordingUiState.ERROR
                            recordingErrorCode=event.error
                            error="Falha REC (${event.error}): ${event.cause?.message ?: "sem detalhes do Android"}"
                            notification="Falha na gravação"
                            Log.e("RBCineCam",error,event.cause)
                            Toast.makeText(context,error,Toast.LENGTH_LONG).show()
                        } else {
                            recordingState=RecordingUiState.IDLE
                            notification="Vídeo salvo com sucesso"
                            error=null
                            Toast.makeText(context,"Vídeo salvo em Movies/RBCineCam",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch(ex:Exception) {
            recording=null
            isRecording=false
            recordingPhase=false
            recordingState=RecordingUiState.ERROR
            error="Falha ao iniciar gravação: ${ex.message ?: ex.javaClass.simpleName}"
            notification="Falha ao iniciar vídeo"
            Log.e("RBCineCam","REC start exception",ex)
            Toast.makeText(context,error,Toast.LENGTH_LONG).show()
        }
    }

    fun applyManualControls(targetManual:Boolean, targetIso:Int, targetShutter:Long, targetAwb:Int, targetAeLock:Boolean) {
        val current=camera ?: run { notification="Câmera indisponível"; return }
        if(recordingState!=RecordingUiState.IDLE) {
            notification="Finalize a gravação para alterar controles manuais"
            return
        }
        if(targetManual && !manualSensorSupported) {
            notification="ISO e obturador manuais não suportados nesta lente"
            return
        }
        if(!awbModes.contains(targetAwb)) {
            notification="Preset WB não suportado nesta lente"
            return
        }
        val newIso=isoRange?.let { targetIso.coerceIn(it.lower,it.upper) } ?: targetIso
        val newShutter=shutterRange?.let { targetShutter.coerceIn(it.lower,it.upper) } ?: targetShutter
        try {
            val builder=CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE,targetAwb)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE,
                    if(targetManual) CaptureRequest.CONTROL_AE_MODE_OFF else CaptureRequest.CONTROL_AE_MODE_ON)
            if(targetManual) {
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY,newIso)
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME,newShutter)
            } else if(aeLockSupported) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK,targetAeLock)
            }
            notification="Aplicando controles de câmera…"
            // Replaces previous Camera2 options, preventing stale ISO and shutter in auto mode.
            val future=Camera2CameraControl.from(current.cameraControl)
                .setCaptureRequestOptions(builder.build())
            future.addListener({
                try {
                    future.get()
                    manualSensorOn=targetManual
                    isoValue=newIso
                    shutterNs=newShutter
                    awbValue=targetAwb
                    aeLocked=if(targetManual) false else targetAeLock
                    notification=if(targetManual) "Exposição manual aplicada" else "Exposição automática aplicada"
                    error=null
                } catch(ex:Exception) {
                    error="Ajustes não aplicados: ${ex.message}"
                    Log.e("RBCineCam","Manual camera options failed",ex)
                }
            },ContextCompat.getMainExecutor(context))
        } catch(ex:Exception) {
            error="Falha ao solicitar controles manuais: ${ex.message}"
            Log.e("RBCineCam","Manual controls error",ex)
        }
    }

    fun setAeLock(locked:Boolean) {
        if(!aeLockSupported) {
            notification="Bloqueio AE indisponível"
            return
        }
        if(manualSensorOn) {
            notification="Desative exposição manual para usar o bloqueio AE"
            return
        }
        applyManualControls(false,isoValue,shutterNs,awbValue,locked)
    }

    fun cycleAwb() {
        if(awbModes.isEmpty()) return
        val supportedModes=listOf(
            CaptureRequest.CONTROL_AWB_MODE_AUTO,
            CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
            CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
            CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
            CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
        ).filter { awbModes.contains(it) }
        if(supportedModes.isEmpty()) {
            notification="Sem presets WB suportados"
            return
        }
        val next=supportedModes[(supportedModes.indexOf(awbValue)+1).mod(supportedModes.size)]
        applyManualControls(manualSensorOn,isoValue,shutterNs,next,aeLocked)
    }

    fun setAfLock(locked:Boolean) {
        val c=camera ?: return
        if(locked) {
            val v=previewView ?: return
            val point=v.meteringPointFactory.createPoint(v.width/2f,v.height/2f)
            val future=c.cameraControl.startFocusAndMetering(
                FocusMeteringAction.Builder(point).disableAutoCancel().build()
            )
            focusStatus="Fixando foco…"
            future.addListener({
                try {
                    focusLocked=future.get().isFocusSuccessful
                    focusStatus=if(focusLocked) "AF fixado" else "AF não confirmado"
                } catch(ex:Exception) {
                    focusLocked=false
                    focusStatus="Falha ao fixar AF"
                }
            },ContextCompat.getMainExecutor(context))
        } else {
            c.cameraControl.cancelFocusAndMetering()
            focusLocked=false
            focusStatus="AF auto"
        }
    }

    fun pauseOrResume() {
        val active=recording ?: return
        try {
            if(recordingState==RecordingUiState.RECORDING) {
                active.pause()
                notification="Solicitando pausa…"
            } else if(recordingState==RecordingUiState.PAUSED) {
                active.resume()
                notification="Retomando gravação…"
            }
        } catch(ex:Exception) {
            error="Não foi possível pausar/retomar: ${ex.message}"
            Log.e("RBCineCam","pause/resume error",ex)
        }
    }

    fun photograph() {
        val capture=imageCapture ?: run {
            notification="Foto indisponível. O modo vídeo está priorizado."
            return
        }
        val values=ContentValues().apply {
            put(Images.Media.DISPLAY_NAME,"RBCineCam_${System.currentTimeMillis()}.jpg")
            put(Images.Media.MIME_TYPE,"image/jpeg")
            put(Images.Media.RELATIVE_PATH,"Pictures/RBCineCam")
        }
        val options=ImageCapture.OutputFileOptions.Builder(
            context.contentResolver, Images.Media.EXTERNAL_CONTENT_URI, values
        ).build()
        capture.takePicture(options, ContextCompat.getMainExecutor(context), object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                error=null; notification="Foto salva na galeria do Android"
                Toast.makeText(context,"Foto salva",Toast.LENGTH_SHORT).show()
            }
            override fun onError(exception: ImageCaptureException) {
                error="Foto falhou: ${exception.message}"
                Toast.makeText(context,error,Toast.LENGTH_LONG).show()
            }
        })
    }

    if(!cameraGranted) {
        Surface(color=Ink, modifier=Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp), verticalArrangement=Arrangement.Center,
                horizontalAlignment=Alignment.CenterHorizontally) {
                Text("RB CineCam precisa acessar a câmera para capturar imagens.",color=Color.White)
                Spacer(Modifier.height(12.dp))
                Button(onClick={ requestPermissions.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO)) }) {
                    Text("Permitir câmera e microfone")
                }
            }
        }
        return
    }

    MaterialTheme(colorScheme=darkColorScheme(primary=Blue, background=Ink)) {
        Surface(color=Ink, modifier=Modifier.fillMaxSize()) {
            if(gallery) {
                InternalGallery(onClose={gallery=false})
            } else Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(4.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(9.dp),modifier=Modifier.fillMaxWidth().height(32.dp).zIndex(4f)) {
                    Text("RB",color=Color.White,fontWeight=FontWeight.Black)
                    Text("CineCam",color=Red,fontWeight=FontWeight.Bold)
                    Text(if(actualQuality==Quality.UHD) "4K" else if(actualQuality==Quality.FHD) "FHD" else "HD",color=Color.White)
                    Text("FPS auto",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                    if(freeSpaceMb>0) Text("${freeSpaceMb/1024} GB livres",color=Color.LightGray,
                        style=MaterialTheme.typography.labelSmall)
                    Text(if(mic && audioGranted) "MIC ON" else "MIC OFF",color=Color.LightGray)
                    Text(when(recordingState) {
                        RecordingUiState.PREPARING -> "● PREPARANDO"
                        RecordingUiState.RECORDING -> "● REC ${String.format(Locale.US,"%02d:%02d",elapsed/60,elapsed%60)}"
                        RecordingUiState.PAUSED -> "Ⅱ PAUSADO ${String.format(Locale.US,"%02d:%02d",elapsed/60,elapsed%60)}"
                        RecordingUiState.STOPPING -> "● SALVANDO"
                        RecordingUiState.ERROR -> "● ERRO REC"
                        else -> if(captureMode=="erro") "ERRO CÂMERA" else if(captureMode=="iniciando") "INICIANDO" else "PRONTO"
                    }, color=when(recordingState) {
                        RecordingUiState.RECORDING,RecordingUiState.ERROR -> Red
                        RecordingUiState.PAUSED -> Gold
                        RecordingUiState.PREPARING,RecordingUiState.STOPPING -> Gold
                        else -> if(captureMode=="erro") Red else Color.Green
                    },fontWeight=FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={settings=true},enabled=recordingState==RecordingUiState.IDLE) { Icon("configuracoes","Configurações") }
                }
                Row(modifier=Modifier.weight(1f).fillMaxWidth().zIndex(3f),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black,RoundedCornerShape(6.dp)).zIndex(0f)) {
                        AndroidView(factory={ ctx -> PreviewView(ctx).apply {
                            implementationMode=PreviewView.ImplementationMode.COMPATIBLE
                            scaleType=PreviewView.ScaleType.FILL_CENTER
                            previewView=this
                        } }, modifier=Modifier.fillMaxSize().zIndex(0f))
                        if(hist || zebra || peaking) previewView?.let { MonitorOverlay(it, hist, zebra, peaking) }
                        if(focusPoint!=null) {
                            Canvas(Modifier.fillMaxSize()) {
                                val p=focusPoint!!
                                drawCircle(if(focusLocked) Gold else Blue,25f,p,style=Stroke(width=2.5f))
                                drawLine(Gold,Offset(p.x-9f,p.y),Offset(p.x+9f,p.y),strokeWidth=1.5f)
                                drawLine(Gold,Offset(p.x,p.y-9f),Offset(p.x,p.y+9f),strokeWidth=1.5f)
                            }
                        }
                        if(grid) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                val w=size.width; val h=size.height
                                for(i in 1..2) {
                                    drawLine(Color.White.copy(alpha=.23f),
                                        androidx.compose.ui.geometry.Offset(w*i/3f,0f),
                                        androidx.compose.ui.geometry.Offset(w*i/3f,h),strokeWidth=1f)
                                    drawLine(Color.White.copy(alpha=.38f),
                                        androidx.compose.ui.geometry.Offset(0f,h*i/3f),
                                        androidx.compose.ui.geometry.Offset(w,h*i/3f),strokeWidth=1f)
                                }
                            }
                        }
                    }
                    Column(Modifier.width(66.dp).zIndex(15f),horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.SpaceEvenly) {
                        CineButton("fotografar","Foto",enabled=recordingState==RecordingUiState.IDLE && imageCapture!=null){photograph()}
                        CineButton("trocar_camera","Câmera",enabled=recordingState==RecordingUiState.IDLE && availableFront) {
                            lens=if(lens==CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        }
                        Surface(
                            onClick={ TouchProbe.last="REC clicado #${recordTapCount+1}"; Log.i("RBCineCam","RECORD BUTTON CLICK"); startOrStop() },
                            enabled=true,
                            shape=CircleShape,
                            border=BorderStroke(2.dp,when(recordingState) {
                                RecordingUiState.RECORDING -> Color.White
                                RecordingUiState.PAUSED -> Gold
                                RecordingUiState.PREPARING,RecordingUiState.STOPPING -> Gold
                                RecordingUiState.ERROR -> Red
                                else -> Color.White
                            }),
                            color=when(recordingState) {
                                RecordingUiState.RECORDING -> Red
                                RecordingUiState.PAUSED -> Gold.copy(alpha=.32f)
                                RecordingUiState.PREPARING,RecordingUiState.STOPPING -> Gold.copy(alpha=.35f)
                                RecordingUiState.ERROR -> Red.copy(alpha=.25f)
                                else -> Panel
                            }
                        ) {
                            Box(Modifier.size(64.dp),contentAlignment=Alignment.Center) {
                                Icon(if(recordingState==RecordingUiState.RECORDING || recordingState==RecordingUiState.PAUSED) "parar" else "gravar",
                                    "Gravar ou parar vídeo",Modifier.size(46.dp))
                            }
                        }
                        Text(when(recordingState) {
                            RecordingUiState.RECORDING -> "REC"
                            RecordingUiState.PAUSED -> "PAUSADO"
                            RecordingUiState.PREPARING -> "AGUARDE"
                            RecordingUiState.STOPPING -> "SALVANDO"
                            RecordingUiState.ERROR -> "ERRO"
                            else -> "GRAVAR"
                        },color=if(recordingState==RecordingUiState.ERROR) Red else Color.White,
                            fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1)
                        if(recordingState==RecordingUiState.RECORDING || recordingState==RecordingUiState.PAUSED) {
                            CineButton(if(recordingState==RecordingUiState.PAUSED) "reproduzir" else "pausar",
                                if(recordingState==RecordingUiState.PAUSED) "Retomar" else "Pausar") { pauseOrResume() }
                        } else {
                            CineButton("galeria","Galeria",enabled=recordingState==RecordingUiState.IDLE) { gallery=true }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(if(toolsOpen) 62.dp else 43.dp).zIndex(15f),
                    horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(9.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        CineButton("zoom_menos","Zoom -",enabled=camera!=null) {
                            val c=camera
                            if(c!=null) {
                                val minimum=c.cameraInfo.zoomState.value?.minZoomRatio ?: 1f
                                zoom=(zoom-.25f).coerceAtLeast(minimum)
                                c.cameraControl.setZoomRatio(zoom)
                            }
                        }
                        Text(String.format(Locale.US,"%.1fx",zoom),color=Color.White)
                        CineButton("zoom_mais","Zoom +",enabled=camera!=null) {
                            val c=camera
                            if(c!=null) {
                                val maximum=c.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                                zoom=(zoom+.25f).coerceAtMost(maximum)
                                c.cameraControl.setZoomRatio(zoom)
                            }
                        }
                        CineButton("grade_tercos","Grade",highlighted=grid) { grid=!grid }
                        CineButton(if(mic) "microfone" else "microfone_mudo","Áudio",
                            enabled=recordingState==RecordingUiState.IDLE,highlighted=mic) {
                            if(!audioGranted && !mic) requestPermissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            else mic=!mic
                        }
                        if(toolsOpen) {
                            CineButton("compensacao_ev","EV -",
                                enabled=camera?.cameraInfo?.exposureState?.isExposureCompensationSupported==true) {
                                val c=camera
                                if(c!=null) {
                                    val range=c.cameraInfo.exposureState.exposureCompensationRange
                                    exposureIndex=(exposureIndex-1).coerceAtLeast(range.lower)
                                    c.cameraControl.setExposureCompensationIndex(exposureIndex)
                                }
                            }
                            Text("EV "+String.format(Locale.US,"%+.1f",exposureIndex*exposureStep),
                                color=Color.White,style=MaterialTheme.typography.labelSmall)
                            CineButton("iso",if(manualPanel) "Manuais -" else "Manuais +",
                                enabled=recordingState==RecordingUiState.IDLE,
                                highlighted=manualPanel) { manualPanel=!manualPanel }
                            CineButton("bloquear_foco",if(focusLocked) "AF fixo" else "AF auto",
                                enabled=camera!=null,highlighted=focusLocked) {setAfLock(!focusLocked)}
                            CineButton("bloquear_foco",if(aeLocked) "AE fixo" else "AE auto",
                                enabled=aeLockSupported,highlighted=aeLocked) {setAeLock(!aeLocked)}
                            CineButton("compensacao_ev","EV +",
                                enabled=camera?.cameraInfo?.exposureState?.isExposureCompensationSupported==true) {
                                val c=camera
                                if(c!=null) {
                                    val range=c.cameraInfo.exposureState.exposureCompensationRange
                                    exposureIndex=(exposureIndex+1).coerceAtMost(range.upper)
                                    c.cameraControl.setExposureCompensationIndex(exposureIndex)
                                }
                            }
                            if(manualPanel) {
                                CineButton("iso",if(manualSensorOn) "ISO M" else "ISO A",
                                    enabled=manualSensorSupported && recordingState==RecordingUiState.IDLE,
                                    highlighted=manualSensorOn) {
                                    applyManualControls(!manualSensorOn,isoValue,shutterNs,awbValue,false)
                                }
                                CineButton("zoom_menos","ISO -",enabled=manualSensorSupported &&
                                    manualSensorOn && recordingState==RecordingUiState.IDLE) {
                                    applyManualControls(true,(isoValue-100).coerceAtLeast(isoRange?.lower ?: 100),
                                        shutterNs,awbValue,false)
                                }
                                Text("$isoValue",color=Color.White)
                                CineButton("zoom_mais","ISO +",enabled=manualSensorSupported &&
                                    manualSensorOn && recordingState==RecordingUiState.IDLE) {
                                    applyManualControls(true,(isoValue+100).coerceAtMost(isoRange?.upper ?: 100),
                                        shutterNs,awbValue,false)
                                }
                                CineButton("obturador","1/t -",enabled=manualSensorSupported &&
                                    manualSensorOn && recordingState==RecordingUiState.IDLE) {
                                    applyManualControls(true,isoValue,(shutterNs*2).coerceAtMost(shutterRange?.upper ?: shutterNs),
                                        awbValue,false)
                                }
                                Text("1/"+(1000000000L/shutterNs.coerceAtLeast(1L)),
                                    color=Color.White,style=MaterialTheme.typography.labelSmall)
                                CineButton("obturador","1/t +",enabled=manualSensorSupported &&
                                    manualSensorOn && recordingState==RecordingUiState.IDLE) {
                                    applyManualControls(true,isoValue,(shutterNs/2).coerceAtLeast(shutterRange?.lower ?: 1L),
                                        awbValue,false)
                                }
                                CineButton("balanco_branco","WB",enabled=recordingState==RecordingUiState.IDLE) { cycleAwb() }
                                Text(when(awbValue) {
                                    CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT -> "Sol"
                                    CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "Nublado"
                                    CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT -> "Tungst."
                                    CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT -> "Fluor."
                                    else -> "WB auto"
                                },color=Color.White,style=MaterialTheme.typography.labelSmall)
                            }
                            CineButton("histograma","Hist.",highlighted=hist) { hist=!hist }
                            CineButton("zebra","Zebra",highlighted=zebra) { zebra=!zebra }
                            CineButton("peaking","Peaking",highlighted=peaking) { peaking=!peaking }
                            CineButton("informacoes","Info") { settings=true }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(onClick={ TouchProbe.last="Ferramentas clicado"; toolsOpen=!toolsOpen },shape=RoundedCornerShape(10.dp),
                        color=if(toolsOpen) Blue.copy(alpha=.3f) else Panel) {
                        Text(if(toolsOpen) "Fechar" else "Ferramentas",color=Color.White,
                            style=MaterialTheme.typography.labelSmall,
                            modifier=Modifier.padding(horizontal=8.dp, vertical=12.dp))
                    }
                }
                Surface(color=if(error!=null) Red.copy(alpha=.20f) else Ink.copy(alpha=.90f),
                    modifier=Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement=Arrangement.SpaceBetween,
                        verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=2.dp)) {
                        Text((if(diagnosticsOpen) "TOQUES ${TouchProbe.count} • ${TouchProbe.last} • " else "") +
                                (error ?: notification) +
                                (if(lastVideoBytes>0 && (recordingState==RecordingUiState.RECORDING || recordingState==RecordingUiState.PAUSED))
                                    " • ${lastVideoBytes/(1024*1024)} MB" else ""),
                            color=if(error!=null) Color(0xFFFFACAC) else Color.LightGray,
                            style=MaterialTheme.typography.labelSmall,
                            maxLines=1,overflow=TextOverflow.Ellipsis,
                            modifier=Modifier.weight(1f))
                        if(captureMode=="erro" || recordingState==RecordingUiState.ERROR) TextButton(onClick={quality=Quality.HD}) {
                            Text("Tentar HD",style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if(settings) AlertDialog(onDismissRequest={settings=false},
                title={Text("Configurações de captura")},
                text={
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        Text("Resolução disponível no dispositivo")
                        Row {
                            listOf(Quality.FHD,Quality.UHD).forEach { q ->
                                val ok=supportedQualities.contains(q)
                                TextButton(onClick={quality=q;settings=false},enabled=ok && !isRecording) {
                                    Text(if(q==Quality.UHD) "4K" else "1080p")
                                }
                            }
                        }
                        Text("Toque na imagem para focalizar. Use AF fixo para manter foco e AE fixo para bloquear a exposição quando o hardware permitir.")
                        Text("Estado do foco: "+focusStatus)
                        Text(if(manualSensorSupported) "Sensor manual: ISO ${isoRange?.lower} a ${isoRange?.upper}" else "Sensor manual indisponível nesta lente")
                        Text("Modo de exposição: "+if(manualSensorOn) "manual" else "automático")
                        Text("ISO: $isoValue • obturador: ${shutterNs/1000000.0} ms")
                        Text("WB: "+awbValue+" (preset do Android)")
                        Text("Compensação de exposição: "+String.format(Locale.US,"%+.2f EV",exposureIndex*exposureStep))
                        Text(if(aeLockSupported) "Bloqueio AE disponível" else "Bloqueio AE não informado pelo dispositivo")
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("Diagnóstico de toques")
                            Switch(checked=diagnosticsOpen,onCheckedChange={diagnosticsOpen=it})
                        }
                        Text("ISO/obturador exigem suporte MANUAL_SENSOR. WB usa apenas presets reportados. FPS fixo, LOG e ajuste Kelvin ainda não implementados. Os monitores são aproximados.")
                    }
                },
                confirmButton={TextButton(onClick={settings=false}){Text("Fechar")}})
        }
    }
}
