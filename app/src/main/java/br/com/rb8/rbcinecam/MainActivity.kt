package br.com.rb8.rbcinecam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.camera.video.VideoRecordEvent.Status
import androidx.camera.video.VideoRecordEvent.Start
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
            camera=bound
            imageCapture=workingPhoto
            videoCapture=workingVideo
            zoom=1f
            exposureIndex=0
            view.setOnTouchListener { _, event ->
                if(event.action == MotionEvent.ACTION_UP) {
                    val point=view.meteringPointFactory.createPoint(event.x,event.y)
                    bound.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
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
            RecordingUiState.RECORDING -> {
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
                    is VideoRecordEvent.Status -> {
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
                    Text("FPS automático",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                    Text(if(mic && audioGranted) "MIC ON" else "MIC OFF",color=Color.LightGray)
                    Text(when(recordingState) {
                        RecordingUiState.PREPARING -> "● PREPARANDO"
                        RecordingUiState.RECORDING -> "● REC ${String.format(Locale.US,"%02d:%02d",elapsed/60,elapsed%60)}"
                        RecordingUiState.STOPPING -> "● SALVANDO"
                        RecordingUiState.ERROR -> "● ERRO REC"
                        else -> if(captureMode=="erro") "ERRO CÂMERA" else if(captureMode=="iniciando") "INICIANDO" else "PRONTO"
                    }, color=when(recordingState) {
                        RecordingUiState.RECORDING,RecordingUiState.ERROR -> Red
                        RecordingUiState.PREPARING,RecordingUiState.STOPPING -> Gold
                        else -> if(captureMode=="erro") Red else Color.Green
                    },fontWeight=FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={settings=true},enabled=!isRecording) { Icon("configuracoes","Configurações") }
                }
                Row(modifier=Modifier.weight(1f).fillMaxWidth().zIndex(3f),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black,RoundedCornerShape(6.dp)).zIndex(0f)) {
                        AndroidView(factory={ ctx -> PreviewView(ctx).apply {
                            implementationMode=PreviewView.ImplementationMode.COMPATIBLE
                            scaleType=PreviewView.ScaleType.FILL_CENTER
                            previewView=this
                        } }, modifier=Modifier.fillMaxSize().zIndex(0f))
                        if(hist || zebra || peaking) previewView?.let { MonitorOverlay(it, hist, zebra, peaking) }
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
                        CineButton("fotografar","Foto",enabled=!isRecording && imageCapture!=null){photograph()}
                        CineButton("trocar_camera","Câmera",enabled=!isRecording && availableFront) {
                            lens=if(lens==CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        }
                        Surface(
                            onClick={ TouchProbe.last="REC clicado #${recordTapCount+1}"; Log.i("RBCineCam","RECORD BUTTON CLICK"); startOrStop() },
                            enabled=true,
                            shape=CircleShape,
                            border=BorderStroke(2.dp,when(recordingState) {
                                RecordingUiState.RECORDING -> Color.White
                                RecordingUiState.PREPARING,RecordingUiState.STOPPING -> Gold
                                RecordingUiState.ERROR -> Red
                                else -> Color.White
                            }),
                            color=when(recordingState) {
                                RecordingUiState.RECORDING -> Red
                                RecordingUiState.PREPARING,RecordingUiState.STOPPING -> Gold.copy(alpha=.35f)
                                RecordingUiState.ERROR -> Red.copy(alpha=.25f)
                                else -> Panel
                            }
                        ) {
                            Box(Modifier.size(64.dp),contentAlignment=Alignment.Center) {
                                Icon(if(recordingState==RecordingUiState.RECORDING) "parar" else "gravar",
                                    "Gravar ou parar vídeo",Modifier.size(46.dp))
                            }
                        }
                        Text(when(recordingState) {
                            RecordingUiState.RECORDING -> "REC"
                            RecordingUiState.PREPARING -> "AGUARDE"
                            RecordingUiState.STOPPING -> "SALVANDO"
                            RecordingUiState.ERROR -> "ERRO"
                            else -> "GRAVAR"
                        },color=if(recordingState==RecordingUiState.ERROR) Red else Color.White,
                            fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1)
                        CineButton("galeria","Galeria",enabled=!isRecording) { gallery=true }
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
                            enabled=!isRecording && !recordingPhase,highlighted=mic) {
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
                            Text("EV "+exposureIndex,color=Color.White,style=MaterialTheme.typography.labelSmall)
                            CineButton("compensacao_ev","EV +",
                                enabled=camera?.cameraInfo?.exposureState?.isExposureCompensationSupported==true) {
                                val c=camera
                                if(c!=null) {
                                    val range=c.cameraInfo.exposureState.exposureCompensationRange
                                    exposureIndex=(exposureIndex+1).coerceAtMost(range.upper)
                                    c.cameraControl.setExposureCompensationIndex(exposureIndex)
                                }
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
                        Text("TOQUES ${TouchProbe.count} • ${TouchProbe.last} • ${error ?: notification}",
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
                        Text("Toque na imagem para ajustar foco e exposição automáticos.")
                        Text("ISO, obturador e balanço de branco manuais, FPS fixo e LOG exigem implementação Camera2 e testes de hardware. Histograma, zebra e peaking são estimativas sobre a pré-visualização, não instrumentos calibrados.")
                    }
                },
                confirmButton={TextButton(onClick={settings=false}){Text("Fechar")}})
        }
    }
}
