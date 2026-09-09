package br.com.rb8digital.rbcinecam.ui

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.provider.MediaStore
import android.util.Size
import android.view.ViewGroup
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import br.com.rb8digital.rbcinecam.audio.AudioLevelMonitor
import br.com.rb8digital.rbcinecam.audio.AudioMeterMath
import br.com.rb8digital.rbcinecam.camera.RBCameraController
import br.com.rb8digital.rbcinecam.monitoring.ScopeFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

private val P09 = Color(0xF2090B0E)
private val PS09 = Color(0xE614171B)
private val A09 = Color(0xFFFFC400)
private val R09 = Color(0xFFFF2020)

enum class ScopeMode09(val label: String) { RGB("RGB"), WAVE("WAVE"), OFF("OFF") }
data class Media09(
    val uri: Uri,
    val name: String,
    val video: Boolean,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long
)

private val unavailable09 = AudioLevelMonitor.Level(-60f, -60f, -60f, false, false)

@Composable
fun CameraScreen09(cameraPermissionGranted: Boolean, audioPermissionGranted: Boolean) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { RBCameraController(context) }
    val audioMonitor = remember { AudioLevelMonitor(context.applicationContext) }
    var preview by remember { mutableStateOf<PreviewView?>(null) }
    var mode by remember { mutableStateOf(CameraMode.VIDEO) }
    var guide by remember { mutableStateOf(GuideMode.OFF) }
    var scopeMode by remember { mutableStateOf(ScopeMode09.RGB) }
    var scopeFrame by remember { mutableStateOf(ScopeFrame.empty()) }
    var recording by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("PRONTO") }
    var elapsed by remember { mutableLongStateOf(0L) }
    var free by remember { mutableStateOf(storageLabel09(context.filesDir.absolutePath)) }
    var battery by remember { mutableIntStateOf(batteryPct09(context)) }
    var audio by remember { mutableStateOf(unavailable09) }
    var recordingDb by remember { mutableFloatStateOf(-60f) }
    var gallery by remember { mutableStateOf(false) }
    var playerUri by remember { mutableStateOf<Uri?>(null) }
    var iso by remember { mutableStateOf("AUTO") }
    var shutter by remember { mutableStateOf("AUTO") }
    var wb by remember { mutableStateOf("AUTO") }
    var focus by remember { mutableStateOf("AF-C") }
    var ev by remember { mutableStateOf("0") }
    var focusPanel by remember { mutableStateOf(false) }
    var manualFocus by remember { mutableFloatStateOf(.5f) }
    var manualFocusAvailable by remember { mutableStateOf(false) }
    var showFocus by remember { mutableStateOf(false) }

    LaunchedEffect(recording) {
        if (recording) {
            elapsed = 0
            while (true) { delay(1000); elapsed++; free = storageLabel09(context.filesDir.absolutePath); battery = batteryPct09(context) }
        }
    }
    LaunchedEffect(showFocus) { if (showFocus) { delay(800); showFocus = false } }

    LaunchedEffect(audioPermissionGranted, recording) {
        audioMonitor.stop()
        if (audioPermissionGranted && !recording) {
            audioMonitor.start { level -> ContextCompat.getMainExecutor(context).execute { audio = level } }
        }
    }
    DisposableEffect(Unit) { onDispose { audioMonitor.stop() } }

    MaterialTheme(colorScheme = darkColorScheme()) {
        when {
            playerUri != null -> Player09(
                uri = playerUri!!,
                onGallery = { playerUri = null; gallery = true },
                onCamera = { playerUri = null; gallery = false }
            )
            gallery -> Gallery09(onCamera = { gallery = false }, onVideo = { playerUri = it })
            !cameraPermissionGranted -> Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { Text("Autorize a câmera para iniciar o RB CineCam.", color = Color.White) }
            else -> Row(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
                LeftRail09(mode, !recording,
                    onVideo = { mode = CameraMode.VIDEO; status = "MODO VÍDEO" },
                    onPhoto = { mode = CameraMode.PHOTO; status = "MODO FOTO" },
                    onGallery = { gallery = true })

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Hud09(mode, recording, status, elapsed, free, battery, audioPermissionGranted, if (recording) recordingDb else audio.peakDb)
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            AndroidView(
                                factory = { ctx -> PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(-1, -1)
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    preview = this
                                    controller.bind(owner, this, onScopeFrame = { f ->
                                        ContextCompat.getMainExecutor(context).execute { scopeFrame = f }
                                    })
                                } },
                                modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                    detectTapGestures { p -> preview?.let { controller.focusAt(it, p.x, p.y) }; showFocus = true; status = "FOCO" }
                                }
                            )
                            GuideOverlay09(guide, Modifier.fillMaxSize())
                            if (showFocus) FocusReticle09(Modifier.align(Alignment.Center))
                            Row(Modifier.align(Alignment.TopEnd).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Tool09("SCOPES ${scopeMode.label}") { scopeMode = ScopeMode09.entries[(scopeMode.ordinal + 1) % ScopeMode09.entries.size] }
                                Tool09("GRID ${guide.label}") { guide = GuideMode.entries[(guide.ordinal + 1) % GuideMode.entries.size] }
                            }
                            if (scopeMode != ScopeMode09.OFF) ScopePanel09(scopeFrame, scopeMode, Modifier.align(Alignment.BottomStart).padding(10.dp))
                            if (focusPanel) FocusPanel09(
                                current = focus,
                                manualSupported = manualFocusAvailable,
                                value = manualFocus,
                                onAfC = { focus = controller.focusContinuous(); focusPanel = false; status = "FOCO $focus" },
                                onAfS = { preview?.let { focus = controller.focusSingle(it) }; focusPanel = false; status = "FOCO $focus" },
                                onLock = { preview?.let { focus = controller.focusLock(it) }; focusPanel = false; status = "FOCO $focus" },
                                onManual = { manualFocus = it; focus = controller.setManualFocus(it); status = "FOCO $focus" },
                                onInfinity = { manualFocus = 0f; focus = controller.setManualFocus(0f) },
                                onMacro = { manualFocus = 1f; focus = controller.setManualFocus(1f) },
                                onClose = { focusPanel = false },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                            )
                        }
                        AudioStrip09(audio, recording, recordingDb, audioPermissionGranted)
                    }
                    Manual09(iso, shutter, wb, focus, ev, !recording,
                        { iso = controller.cycleIso(); status = "ISO $iso" },
                        { shutter = controller.cycleShutter(); status = "SHUTTER $shutter" },
                        { wb = controller.cycleWhiteBalance(); status = "WB $wb" },
                        { manualFocusAvailable = controller.manualFocusSupported(); focusPanel = true },
                        { ev = controller.cycleEv(); status = "EV $ev" })
                }

                RightRail09(mode, recording, elapsed,
                    onSwitch = { preview?.let { controller.switchLens(owner, it); iso="AUTO"; shutter="AUTO"; wb="AUTO"; focus="AF-C"; ev="0"; status="CÂMERA TROCADA" } },
                    onPrimary = {
                        if (mode == CameraMode.VIDEO) {
                            if (recording) { controller.stopRecording(); status = "SALVANDO" }
                            else controller.startRecording(audioPermissionGranted) { e ->
                                when (e) {
                                    is VideoRecordEvent.Start -> { recording = true; status = "REC" }
                                    is VideoRecordEvent.Status -> {
                                        val amp = e.recordingStats.audioStats.audioAmplitude
                                        recordingDb = AudioMeterMath.amplitudeDbfs(amp)
                                    }
                                    is VideoRecordEvent.Finalize -> { recording = false; status = if (e.hasError()) "ERRO ${e.error}" else "VÍDEO SALVO" }
                                }
                            }
                        } else controller.takePhoto { ok, msg -> status = msg; if (ok) free = storageLabel09(context.filesDir.absolutePath) }
                    })
            }
        }
    }
}

@Composable private fun AudioStrip09(level: AudioLevelMonitor.Level, recording: Boolean, recDb: Float, permitted: Boolean) {
    Column(Modifier.width(76.dp).fillMaxHeight().background(P09).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ÁUDIO", color = A09, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        if (recording) {
            Text("REC MIX", color = Color.White, fontSize = 8.sp)
            AudioBar09("M", recDb, AudioMeterMath.isClipping(recDb), Modifier.weight(1f))
        } else if (permitted && level.available) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                AudioBar09("L", level.leftDb, level.clipping, Modifier.weight(1f))
                AudioBar09("R", level.rightDb, level.clipping, Modifier.weight(1f))
            }
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("AUDIO\nN/D", color = Color.Gray, fontSize = 9.sp) }
        }
        Text("0", color=Color.Gray,fontSize=7.sp); Text("-12",color=Color.Gray,fontSize=7.sp); Text("-24",color=Color.Gray,fontSize=7.sp); Text("-60 dBFS",color=Color.Gray,fontSize=7.sp)
    }
}

@Composable private fun AudioBar09(label: String, db: Float, clipping: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.width(14.dp).weight(1f).background(Color(0xFF202329))) {
            val f = (db.coerceIn(-60f, 0f) + 60f) / 60f
            drawRect(if (clipping) R09 else A09, Offset(0f, size.height * (1f-f)), androidx.compose.ui.geometry.Size(size.width, size.height*f))
        }
        Text(label, color=Color.White,fontSize=8.sp,fontWeight=FontWeight.Bold)
        Text("%.0f".format(db), color=if(clipping)R09 else Color.LightGray,fontSize=7.sp)
    }
}

@Composable private fun ScopePanel09(frame: ScopeFrame, mode: ScopeMode09, modifier: Modifier = Modifier) {
    Surface(modifier.width(250.dp).height(120.dp), color = Color(0xD9080A0D), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(7.dp)) {
            Text(if(mode==ScopeMode09.RGB) "HISTOGRAMA RGB" else "WAVEFORM LUMA", color=A09,fontSize=9.sp,fontWeight=FontWeight.Bold)
            Canvas(Modifier.fillMaxSize().padding(top=4.dp)) {
                if (mode == ScopeMode09.RGB) {
                    fun drawHist(values:IntArray,color:Color){ val m=max(1,values.maxOrNull()?:1); val dx=size.width/values.size; var prev=Offset(0f,size.height); values.forEachIndexed{i,v-> val p=Offset(i*dx,size.height-(v.toFloat()/m)*size.height); if(i>0)drawLine(color,prev,p,1.5.dp.toPx()); prev=p } }
                    drawHist(frame.red,Color.Red); drawHist(frame.green,Color.Green); drawHist(frame.blue,Color.Blue)
                } else {
                    val maxD=max(1,frame.waveform.maxOrNull()?:1); val cw=size.width/frame.waveformWidth; val ch=size.height/frame.waveformHeight
                    for(y in 0 until frame.waveformHeight) for(x in 0 until frame.waveformWidth){ val d=frame.waveform[y*frame.waveformWidth+x]; if(d>0){ val a=(d.toFloat()/maxD).coerceIn(.12f,1f); drawRect(Color.White.copy(alpha=a),Offset(x*cw,y*ch),androidx.compose.ui.geometry.Size(max(1f,cw),max(1f,ch))) } }
                }
            }
        }
    }
}

@Composable private fun Gallery09(onCamera:()->Unit,onVideo:(Uri)->Unit) {
    val context=LocalContext.current
    val media=remember{queryMedia09(context)}
    Column(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().background(P09).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Button(onClick=onCamera,colors=ButtonDefaults.buttonColors(containerColor=A09,contentColor=Color.Black)){Icon(Icons.Filled.Videocam,null);Spacer(Modifier.width(6.dp));Text("VOLTAR À CÂMERA",fontWeight=FontWeight.Bold)}
            Spacer(Modifier.width(14.dp)); Text("GALERIA RB CineCam",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold)
        }
        if(media.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Nenhuma mídia encontrada",color=Color.White)} else LazyColumn {
            items(media){m-> Row(Modifier.fillMaxWidth().clickable(enabled=m.video){if(m.video)onVideo(m.uri)}.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                Thumbnail09(m); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){Text(m.name,color=Color.White,fontWeight=FontWeight.SemiBold); Text(mediaInfo09(m),color=Color.LightGray,fontSize=10.sp)}
                if(m.video) Icon(Icons.Filled.PlayCircle,"Assistir",tint=A09,modifier=Modifier.size(40.dp))
            }; HorizontalDivider(color=Color(0xFF282B30)) }
        }
    }
}

@Composable private fun Thumbnail09(m:Media09){ val context=LocalContext.current; val bitmap by produceState<Bitmap?>(null,m.uri){value=withContext(Dispatchers.IO){if(Build.VERSION.SDK_INT>=29)runCatching{context.contentResolver.loadThumbnail(m.uri,Size(160,90),null)}.getOrNull() else null}}; Box(Modifier.width(120.dp).height(68.dp).background(Color(0xFF17191D)),contentAlignment=Alignment.Center){if(bitmap!=null)Image(bitmap!!.asImageBitmap(),null,Modifier.fillMaxSize()) else Icon(if(m.video)Icons.Filled.Movie else Icons.Filled.Photo,null,tint=Color.Gray)} }

@Composable private fun Player09(uri:Uri,onGallery:()->Unit,onCamera:()->Unit){ val context=LocalContext.current; val player=remember(uri){ExoPlayer.Builder(context).build().apply{setMediaItem(MediaItem.fromUri(uri));prepare();playWhenReady=true}}; DisposableEffect(player){onDispose{player.release()}}; Box(Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)){AndroidView(factory={ctx->PlayerView(ctx).apply{this.player=player;useController=true}},modifier=Modifier.fillMaxSize());Row(Modifier.align(Alignment.TopStart).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onGallery){Icon(Icons.Filled.ArrowBack,null);Spacer(Modifier.width(5.dp));Text("GALERIA")};Button(onClick=onCamera,colors=ButtonDefaults.buttonColors(containerColor=A09,contentColor=Color.Black)){Icon(Icons.Filled.Videocam,null);Spacer(Modifier.width(5.dp));Text("CÂMERA")}}} }

@Composable private fun Hud09(mode:CameraMode,recording:Boolean,status:String,elapsed:Long,free:String,battery:Int,audio:Boolean,db:Float){Row(Modifier.fillMaxWidth().background(P09).padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Text("RB ",color=R09,fontWeight=FontWeight.Black,fontSize=17.sp);Text("CineCam",color=Color.White,fontWeight=FontWeight.Bold,fontSize=17.sp);Spacer(Modifier.width(14.dp));Text(if(mode==CameraMode.VIDEO)"FHD • 30 FPS • MP4" else "FOTO • JPEG",color=Color.White,fontSize=11.sp);Spacer(Modifier.width(12.dp));Text("Livre $free",color=Color.LightGray,fontSize=10.sp);Spacer(Modifier.width(10.dp));Text("BAT $battery%",color=Color.LightGray,fontSize=10.sp);Spacer(Modifier.width(10.dp));Text(if(audio)"MIC ${"%.0f".format(db)} dB" else "MIC OFF",color=if(audio)Color.LightGray else R09,fontSize=10.sp);Spacer(Modifier.weight(1f));Text(if(recording)"● REC ${formatDuration09(elapsed)}" else status,color=if(recording)R09 else A09,fontWeight=FontWeight.Bold,fontSize=11.sp)} }

@Composable private fun LeftRail09(mode:CameraMode,enabled:Boolean,onVideo:()->Unit,onPhoto:()->Unit,onGallery:()->Unit){Column(Modifier.width(78.dp).fillMaxHeight().background(P09),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){Rail09(Icons.Filled.Videocam,"VÍDEO",mode==CameraMode.VIDEO,enabled,onVideo);Rail09(Icons.Filled.PhotoCamera,"FOTO",mode==CameraMode.PHOTO,enabled,onPhoto);Rail09(Icons.Filled.PhotoLibrary,"GALERIA",false,enabled,onGallery)}}
@Composable private fun Rail09(icon:ImageVector,label:String,active:Boolean,enabled:Boolean,onClick:()->Unit){val tint=if(!enabled)Color.Gray else if(active)A09 else Color.White;Column(Modifier.clip(RoundedCornerShape(10.dp)).background(if(active)Color(0x33222222)else Color.Transparent).clickable(enabled=enabled){onClick()}.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,label,tint=tint,modifier=Modifier.size(26.dp));Text(label,color=tint,fontSize=9.sp,fontWeight=FontWeight.Bold)}}
@Composable private fun RightRail09(mode:CameraMode,recording:Boolean,elapsed:Long,onSwitch:()->Unit,onPrimary:()->Unit){Column(Modifier.width(96.dp).fillMaxHeight().background(P09),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){Rail09(Icons.Filled.Cameraswitch,"TROCAR",false,!recording,onSwitch);Box(Modifier.size(74.dp).clip(CircleShape).border(4.dp,Color.White,CircleShape).padding(7.dp).clip(CircleShape).background(if(recording)Color(0xFF7A2020) else if(mode==CameraMode.VIDEO)R09 else Color.White).clickable{onPrimary()},contentAlignment=Alignment.Center){Icon(if(recording)Icons.Filled.Stop else if(mode==CameraMode.VIDEO)Icons.Filled.FiberManualRecord else Icons.Filled.PhotoCamera,null,tint=if(mode==CameraMode.PHOTO&&!recording)Color.Black else Color.White,modifier=Modifier.size(28.dp))};Text(if(recording)formatDuration09(elapsed) else if(mode==CameraMode.VIDEO)"REC" else "FOTO",color=if(recording||mode==CameraMode.VIDEO)R09 else A09,fontWeight=FontWeight.Bold,fontSize=12.sp)}}

@Composable private fun Manual09(iso:String,shutter:String,wb:String,focus:String,ev:String,enabled:Boolean,onIso:()->Unit,onShutter:()->Unit,onWb:()->Unit,onFocus:()->Unit,onEv:()->Unit){Row(Modifier.fillMaxWidth().background(P09).padding(7.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){Ctrl09("ISO",iso,enabled,onIso,Modifier.weight(1f));Ctrl09("SHUTTER",shutter,enabled,onShutter,Modifier.weight(1f));Ctrl09("WB",wb,enabled,onWb,Modifier.weight(1f));Ctrl09("FOCUS",focus,enabled,onFocus,Modifier.weight(1f));Ctrl09("EV",ev,enabled,onEv,Modifier.weight(1f))}}
@Composable private fun Ctrl09(label:String,value:String,enabled:Boolean,onClick:()->Unit,modifier:Modifier=Modifier){Column(modifier.height(58.dp).clip(RoundedCornerShape(8.dp)).background(PS09).border(1.dp,Color(0xFF34383E),RoundedCornerShape(8.dp)).clickable(enabled=enabled){onClick()}.padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(label,color=Color.LightGray,fontSize=10.sp);Text(value,color=if(enabled)A09 else Color.Gray,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=1)}}
@Composable private fun Tool09(label:String,onClick:()->Unit){Surface(Modifier.clip(RoundedCornerShape(8.dp)).clickable{onClick()},color=PS09){Text(label,Modifier.padding(horizontal=10.dp,vertical=7.dp),color=A09,fontSize=9.sp,fontWeight=FontWeight.Bold)}}

@Composable private fun FocusPanel09(current:String,manualSupported:Boolean,value:Float,onAfC:()->Unit,onAfS:()->Unit,onLock:()->Unit,onManual:(Float)->Unit,onInfinity:()->Unit,onMacro:()->Unit,onClose:()->Unit,modifier:Modifier=Modifier){Surface(modifier.widthIn(min=480.dp,max=680.dp),color=Color(0xF20E1115),shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("FOCO $current",color=A09,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));IconButton(onClick=onClose){Icon(Icons.Filled.Close,null,tint=Color.White)}};Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){OutlinedButton(onClick=onAfC){Text("AF-C")};OutlinedButton(onClick=onAfS){Text("AF-S")};OutlinedButton(onClick=onLock){Text("LOCK")};if(manualSupported){OutlinedButton(onClick=onInfinity){Text("∞")};OutlinedButton(onClick=onMacro){Text("MACRO")}}};if(manualSupported){Slider(value,onValueChange=onManual,valueRange=0f..1f);Text("MF ${(value*100).toInt()}%",color=A09,modifier=Modifier.align(Alignment.CenterHorizontally))}else Text("Foco manual não exposto por esta lente",color=Color.Gray,fontSize=9.sp)}}}

@Composable private fun GuideOverlay09(mode:GuideMode,modifier:Modifier=Modifier){if(mode==GuideMode.OFF)return;Canvas(modifier){val c=Color.White.copy(.65f);val w=1.dp.toPx();when(mode){GuideMode.THIRDS->for(i in 1..2){drawLine(c,Offset(size.width*i/3,0f),Offset(size.width*i/3,size.height),w);drawLine(c,Offset(0f,size.height*i/3),Offset(size.width,size.height*i/3),w)};GuideMode.CENTER->{drawLine(c,Offset(size.width/2,0f),Offset(size.width/2,size.height),w);drawLine(c,Offset(0f,size.height/2),Offset(size.width,size.height/2),w)};GuideMode.SAFE->{val x=size.width*.05f;val y=size.height*.05f;drawRect(c,Offset(x,y),androidx.compose.ui.geometry.Size(size.width-2*x,size.height-2*y),style=Stroke(w))};GuideMode.R185,GuideMode.R235,GuideMode.R239->{val ratio=when(mode){GuideMode.R185->1.85f;GuideMode.R235->2.35f;else->2.39f};val h=size.width/ratio;if(h<size.height){val y=(size.height-h)/2;drawLine(c,Offset(0f,y),Offset(size.width,y),w);drawLine(c,Offset(0f,y+h),Offset(size.width,y+h),w)}};else->Unit}}}
@Composable private fun FocusReticle09(modifier:Modifier=Modifier){Box(modifier.size(46.dp).border(2.dp,A09,RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Text("+",color=A09,fontSize=20.sp)}}

private fun queryMedia09(context:Context):List<Media09>{val out=mutableListOf<Media09>();context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.Video.Media._ID,MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION,MediaStore.Video.Media.WIDTH,MediaStore.Video.Media.HEIGHT,MediaStore.Video.Media.DATE_ADDED),null,null,"${MediaStore.Video.Media.DATE_ADDED} DESC")?.use{c->val id=c.getColumnIndexOrThrow(MediaStore.Video.Media._ID);val name=c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);val dur=c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);val w=c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH);val h=c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT);val d=c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);while(c.moveToNext()){val n=c.getString(name)?:continue;if(n.startsWith("RBCineCam_"))out+=Media09(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,c.getLong(id)),n,true,c.getLong(dur),c.getInt(w),c.getInt(h),c.getLong(d))}};context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,arrayOf(MediaStore.Images.Media._ID,MediaStore.Images.Media.DISPLAY_NAME,MediaStore.Images.Media.WIDTH,MediaStore.Images.Media.HEIGHT,MediaStore.Images.Media.DATE_ADDED),null,null,"${MediaStore.Images.Media.DATE_ADDED} DESC")?.use{c->val id=c.getColumnIndexOrThrow(MediaStore.Images.Media._ID);val name=c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);val w=c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH);val h=c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT);val d=c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);while(c.moveToNext()){val n=c.getString(name)?:continue;if(n.startsWith("RBCineCam_"))out+=Media09(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,c.getLong(id)),n,false,0,c.getInt(w),c.getInt(h),c.getLong(d))}};return out.sortedByDescending{it.dateAdded}}
private fun mediaInfo09(m:Media09):String{val date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(m.dateAdded*1000));return if(m.video)"VÍDEO • ${m.width}×${m.height} • ${formatDuration09(m.durationMs/1000)} • $date" else "FOTO • ${m.width}×${m.height} • $date"}
private fun batteryPct09(context:Context):Int=(context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0,100)?:-1
private fun formatDuration09(s:Long)="%02d:%02d".format(s/60,s%60)
private fun storageLabel09(path:String)=runCatching{val st=StatFs(path);val gb=st.availableBytes/1_073_741_824.0;if(gb>=10)"%.0f GB".format(gb)else"%.1f GB".format(gb)}.getOrDefault("--")
