package br.com.rb8.rbcinecam

import android.net.Uri
import android.provider.MediaStore
import android.content.ContentUris
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class VideoItem(val uri: Uri,val title:String,val duration:Long)

private fun userVideos(context:android.content.Context):List<VideoItem>{
    val result=mutableListOf<VideoItem>()
    val columns=arrayOf(MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,MediaStore.Video.Media.DURATION)
    context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,columns,
        MediaStore.Video.Media.RELATIVE_PATH+" LIKE ?",arrayOf("Movies/RBCineCam/%"),
        MediaStore.Video.Media.DATE_ADDED+" DESC")?.use { c ->
        val id=c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val name=c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val duration=c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        while(c.moveToNext()) result.add(VideoItem(
            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,c.getLong(id)),
            c.getString(name)?:"Vídeo",c.getLong(duration)))
    }
    return result
}

@Composable
fun InternalGallery(onClose:()->Unit){
    val context=LocalContext.current
    var list by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var selected by remember { mutableStateOf<Uri?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit){
        try{ list=withContext(Dispatchers.IO){userVideos(context)} }
        catch(ex:Exception){failure="Erro na galeria: "+ex.message}
    }
    Column(Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .background(Color(0xFF080B10))
        .padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){
            Text("Galeria do RB CineCam",color=Color.White)
            TextButton(onClick={if(selected!=null)selected=null else onClose()}) {
                Text(if(selected!=null)"Voltar à galeria" else "Voltar à câmera")
            }
        }
        if(selected != null) {
            val player=remember(selected) { ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(selected!!));prepare();playWhenReady=true
            } }
            DisposableEffect(player) { onDispose{player.release()} }
            AndroidView(factory={ PlayerView(it).apply {
                this.player=player; useController=true
            } },modifier=Modifier.fillMaxSize())
        } else if(failure!=null) Text(failure!!,color=Color.Red)
        else if(list.isEmpty()) Text("Nenhum vídeo gravado pelo RB CineCam",color=Color.LightGray)
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){
            items(list){ item ->
                Surface(onClick={selected=item.uri},color=Color(0xFF1B222D),
                    shape=RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(item.title,color=Color.White)
                        Text((item.duration/1000).toString()+" s",color=Color.LightGray)
                    }
                }
            }
        }
    }
}