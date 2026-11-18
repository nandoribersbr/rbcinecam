package br.com.rb8.rbcinecam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.MediaStoreOutputOptions
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.view.PreviewView
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.os.Environment
import android.content.ContentValues
import android.util.Size
import android.provider.MediaStore.Video
import android.provider.MediaStore.Images
import androidx.camera.core.AspectRatio
import androidx.camera.video.FallbackStrategy
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

private val Ink = Color(0xFF080B10)
private val Panel = Color(0xFF1A2029)
private val Blue = Color(0xFF3B9DFF)
private val Red = Color(0xFFF52C3E)

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
        contentDescription = label, modifier = modifier.size(27.dp)
    )
}

@Composable
private fun CineButton(icon: String, label: String, enabled: Boolean = true, highlighted: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min=58.dp)) {
        Surface(onClick=onClick, enabled=enabled, shape=RoundedCornerShape(12.dp),
            color=if(highlighted) Blue.copy(alpha=.22f) else Panel) {
            Box(Modifier.size(49.dp), contentAlignment=Alignment.Center) { Icon(icon, label) }
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
    LaunchedEffect(isRecording) {
        if (isRecording) { elapsed=0; while(isRecording) { delay(1000); elapsed++ } }
    }
    LaunchedEffect(cameraGranted, lens, quality, previewView) {
        if (!cameraGranted || previewView == null) return@LaunchedEffect
        val view = previewView ?: return@LaunchedEffect
        val provider = try { providerFuture.get() } catch (ex: Exception) {
            error = "Erro ao iniciar câmera: ${ex.message}"; return@LaunchedEffect
        }
        try {
            availableFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            val selector = CameraSelector.Builder().requireLensFacing(lens).build()
            val info = provider.availableCameraInfos.firstOrNull {
                selector.filter(listOf(it)).isNotEmpty()
            }
            supportedQualities = if (info != null) QualitySelector.getSupportedQualities(info) else listOf(Quality.FHD)
            val chosen = if (supportedQualities.contains(quality)) quality else supportedQualities.firstOrNull() ?: Quality.SD
            val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
            val photo = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            val recorder = Recorder.Builder().setQualitySelector(
                QualitySelector.from(chosen, FallbackStrategy.lowerQualityOrHigherThan(chosen))
            ).build()
            val video = VideoCapture.withOutput(recorder)
            provider.unbindAll()
            val bound = provider.bindToLifecycle(lifecycleOwner, selector, preview, photo, video)
            camera=bound
            imageCapture=photo
            videoCapture=video
            zoom=1f
            view.setOnTouchListener { _, event ->
                if(event.action == MotionEvent.ACTION_UP) {
                    val point=view.meteringPointFactory.createPoint(event.x,event.y)
                    bound.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                    true
                } else true
            }
            error=null
        } catch (ex: Exception) {
            error="Configuração de captura incompatível: ${ex.message}"
            camera=null; imageCapture=null; videoCapture=null
        }
    }

    fun startOrStop() {
        if (isRecording) { recording?.stop(); return }
        val video = videoCapture ?: return
        val values = ContentValues().apply {
            put(Video.Media.DISPLAY_NAME, "RBCineCam_${System.currentTimeMillis()}.mp4")
            put(Video.Media.MIME_TYPE, "video/mp4")
            put(Video.Media.RELATIVE_PATH, "Movies/RBCineCam")
        }
        val options = MediaStoreOutputOptions.Builder(context.contentResolver, Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values).build()
        var pending = video.output.prepareRecording(context, options)
        if (mic && audioGranted) pending = pending.withAudioEnabled()
        try {
            recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                when(event) {
                    is VideoRecordEvent.Start -> { isRecording=true; error=null }
                    is VideoRecordEvent.Finalize -> {
                        isRecording=false
                        recording=null
                        if(event.hasError()) error="Gravação falhou: ${event.error}"
                    }
                }
            }
        } catch (ex: Exception) { error="Não foi possível gravar: ${ex.message}" }
    }

    fun photograph() {
        val capture=imageCapture ?: return
        val values=ContentValues().apply {
            put(Images.Media.DISPLAY_NAME,"RBCineCam_${System.currentTimeMillis()}.jpg")
            put(Images.Media.MIME_TYPE,"image/jpeg")
            put(Images.Media.RELATIVE_PATH,"Pictures/RBCineCam")
        }
        val options=ImageCapture.OutputFileOptions.Builder(
            context.contentResolver, Images.Media.EXTERNAL_CONTENT_URI, values
        ).build()
        capture.takePicture(options, ContextCompat.getMainExecutor(context), object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) { error=null }
            override fun onError(exception: ImageCaptureException) { error="Foto falhou: ${exception.message}" }
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
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(14.dp),modifier=Modifier.fillMaxWidth()) {
                    Text("RB CineCam",color=Color.White,fontWeight=FontWeight.Bold)
                    Text(if(quality==Quality.UHD) "4K" else "FHD",color=Color.White)
                    Text(if(mic && audioGranted) "MIC ON" else "MIC OFF",color=Color.LightGray)
                    Text(if(isRecording) "REC  ${elapsed}s" else "PRONTO",color=if(isRecording) Red else Color.Green)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={settings=true},enabled=!isRecording) { Icon("configuracoes","Configurações") }
                }
                Row(modifier=Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black,RoundedCornerShape(12.dp))) {
                        AndroidView(factory={ ctx -> PreviewView(ctx).apply {
                            scaleType=PreviewView.ScaleType.FILL_CENTER
                            previewView=this
                        } }, modifier=Modifier.fillMaxSize())
                        if(grid) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                                val w=size.width; val h=size.height
                                for(i in 1..2) {
                                    drawLine(Color.White.copy(alpha=.38f),
                                        androidx.compose.ui.geometry.Offset(w*i/3f,0f),
                                        androidx.compose.ui.geometry.Offset(w*i/3f,h),strokeWidth=1f)
                                    drawLine(Color.White.copy(alpha=.38f),
                                        androidx.compose.ui.geometry.Offset(0f,h*i/3f),
                                        androidx.compose.ui.geometry.Offset(w,h*i/3f),strokeWidth=1f)
                                }
                            }
                        }
                    }
                    Column(Modifier.width(88.dp),horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.SpaceEvenly) {
                        CineButton("fotografar","Foto",enabled=!isRecording && imageCapture!=null){photograph()}
                        CineButton("trocar_camera","Câmera",enabled=!isRecording && availableFront) {
                            lens=if(lens==CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        }
                        Surface(onClick={::startOrStop},enabled=videoCapture!=null,shape=RoundedCornerShape(60.dp),
                            color=if(isRecording) Red else Panel) {
                            Box(Modifier.size(72.dp),contentAlignment=Alignment.Center) {
                                Icon(if(isRecording) "parar" else "gravar",if(isRecording) "Parar" else "Gravar",Modifier.size(44.dp))
                            }
                        }
                        CineButton("galeria","Galeria",enabled=!isRecording) {
                            try {context.startActivity(Intent(Intent.ACTION_VIEW).apply {type="video/*";flags=Intent.FLAG_ACTIVITY_NEW_TASK})}
                            catch(ex:Exception) {error="Nenhuma galeria de vídeos disponível"}
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.SpaceEvenly) {
                    CineButton("zoom_menos","Zoom -",enabled=camera!=null) {
                        camera?.cameraControl?.setZoomRatio((zoom-.25f).coerceAtLeast(1f))
                        zoom=(zoom-.25f).coerceAtLeast(1f)
                    }
                    Text(String.format(Locale.US,"%.1fx",zoom),color=Color.White)
                    CineButton("zoom_mais","Zoom +",enabled=camera!=null) {
                        val max=camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
                        zoom=(zoom+.25f).coerceAtMost(max)
                        camera?.cameraControl?.setZoomRatio(zoom)
                    }
                    CineButton("grade_tercos","Grade",highlighted=grid){ grid=!grid }
                    CineButton(if(mic) "microfone" else "microfone_mudo","Áudio",enabled=!isRecording){
                        if(!audioGranted && !mic) requestPermissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        mic=!mic
                    }
                    CineButton("informacoes","Info"){settings=true}
                }
                if(error!=null) {
                    Text(error ?: "",color=Red,style=MaterialTheme.typography.labelSmall)
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
                        Text("ISO, obturador manual, LOG, zebra, histograma e peaking exigem implementação e verificação do hardware; não são exibidos como controles ativos.")
                    }
                },
                confirmButton={TextButton(onClick={settings=false}){Text("Fechar")}})
        }
    }
}
