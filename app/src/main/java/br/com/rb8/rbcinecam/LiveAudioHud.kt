package br.com.rb8.rbcinecam

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt
import java.util.Locale

/**
 * Monitor audio in the preview (AudioRecord), then yield microphone to CameraX.
 * During recording, use real CameraX AudioStats; never launch a second recorder.
 */
@Composable
fun LiveAudioHud(
    microphoneEnabled:Boolean,
    recordingState:RecordingUiState,
    cameraAmplitude:Float,
    cameraAudioActive:Boolean
) {
    val context=LocalContext.current
    var previewAmplitude by remember { mutableFloatStateOf(0f) }
    var previewAvailable by remember { mutableStateOf(false) }
    val shouldPreview=microphoneEnabled && recordingState==RecordingUiState.IDLE &&
        ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED

    LaunchedEffect(shouldPreview) {
        previewAvailable=false
        previewAmplitude=0f
        if(!shouldPreview)return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val sampleRate=16000
            val channel=AudioFormat.CHANNEL_IN_MONO
            val encoding=AudioFormat.ENCODING_PCM_16BIT
            val minBytes=AudioRecord.getMinBufferSize(sampleRate,channel,encoding)
            var record:AudioRecord?=null
            if(minBytes<=0)return@withContext
            try {
                // Use a separate mic session only while NOT recording video.
                record=AudioRecord(
                    MediaRecorder.AudioSource.MIC,sampleRate,channel,encoding,
                    maxOf(minBytes,4096)
                )
                if(record.state!=AudioRecord.STATE_INITIALIZED)return@withContext
                record.startRecording()
                previewAvailable=true
                val samples=ShortArray(1024)
                while(isActive && shouldPreview) {
                    val count=record.read(samples,0,samples.size,AudioRecord.READ_BLOCKING)
                    if(count>0) {
                        var sum=0.0
                        for(i in 0 until count) {
                            val v=samples[i].toDouble()/32768.0
                            sum+=v*v
                        }
                        previewAmplitude=sqrt(sum/count).toFloat().coerceIn(0f,1f)
                    } else if(count<0) break
                }
            } catch(ex:SecurityException) {
                android.util.Log.w("RBCineCam","Audio preview permission denied",ex)
            } catch(ex:Exception) {
                android.util.Log.w("RBCineCam","Audio preview unavailable",ex)
            } finally {
                previewAvailable=false
                previewAmplitude=0f
                try { if(record?.recordingState==AudioRecord.RECORDSTATE_RECORDING) record.stop() }
                catch(_:Exception) {}
                record?.release()
            }
        }
    }
    val recording=recordingState==RecordingUiState.RECORDING
    val available=if(recording) cameraAudioActive else previewAvailable && shouldPreview
    val amplitude=(if(recording) cameraAmplitude else previewAmplitude).coerceIn(0f,1f)
    val db=if(available && amplitude>0f) (20.0*log10(amplitude.toDouble())).coerceIn(-60.0,0.0) else -60.0
    val status=when {
        !microphoneEnabled -> "MIC OFF"
        recordingState==RecordingUiState.PAUSED -> "PAUSA"
        recordingState==RecordingUiState.PREPARING -> "INICIANDO"
        recordingState==RecordingUiState.STOPPING -> "SALVANDO"
        recording -> if(cameraAudioActive) "REC" else "SEM SINAL"
        available -> "AO VIVO"
        else -> "MIC INDISP."
    }
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            Text("NÍVEL DE ÁUDIO",color=Color.White,fontSize=10.sp)
            Text(status,color=if(available) Color(0xFF45D799) else Color(0xFFC2CAD5),fontSize=10.sp)
            Text(if(available) String.format(Locale.US,"%.0f dBFS",db) else "-- dB",
                color=Color.White,fontSize=10.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(13.dp)) {
            val count=44
            val step=size.width/count
            val lit=if(available)((db+60)/60*count).toInt().coerceIn(0,count) else 0
            for(i in 0 until count){
                val base=when {
                    i>=40 -> Color(0xFFF24958)
                    i>=33 -> Color(0xFFF7C64D)
                    else -> Color(0xFF47D49C)
                }
                drawLine(if(i<lit)base else Color(0xFF324252),
                    Offset(i*step,size.height),Offset(i*step,0f),
                    strokeWidth=(step*.68f).coerceAtLeast(1f))
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("-60",color=Color.LightGray,fontSize=8.sp)
            Text("-30",color=Color.LightGray,fontSize=8.sp)
            Text("-12",color=Color.LightGray,fontSize=8.sp)
            Text("0 dBFS",color=Color.LightGray,fontSize=8.sp)
        }
    }
}
