package br.com.rb8.rbcinecam

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

private data class Sample(val r:IntArray,val g:IntArray,val b:IntArray,
    val clipped:List<Offset>,val edges:List<Offset>)

private fun sample(bitmap:Bitmap):Sample {
    val w=96; val h=54
    val scaled=Bitmap.createScaledBitmap(bitmap,w,h,false)
    val pixels=IntArray(w*h)
    scaled.getPixels(pixels,0,w,0,0,w,h)
    if(scaled !== bitmap) scaled.recycle()
    val r=IntArray(32);val g=IntArray(32);val b=IntArray(32)
    val luma=IntArray(w*h)
    val highlights=mutableListOf<Offset>(); val edges=mutableListOf<Offset>()
    for(y in 0 until h) for(x in 0 until w) {
        val p=pixels[y*w+x]
        val rr=(p shr 16) and 255
        val gg=(p shr 8) and 255
        val bb=p and 255
        r[rr/8]++;g[gg/8]++;b[bb/8]++
        luma[y*w+x]=(rr*77+gg*150+bb*29)/256
        if(rr>245 && gg>245 && bb>245 && x%3==0 && y%3==0)
            highlights.add(Offset(x.toFloat()/w,y.toFloat()/h))
    }
    for(y in 2 until h-2 step 2) for(x in 2 until w-2 step 2){
        val i=y*w+x
        if(abs(luma[i+1]-luma[i-1])+abs(luma[i+w]-luma[i-w])>110)
            edges.add(Offset(x.toFloat()/w,y.toFloat()/h))
    }
    return Sample(r,g,b,highlights,edges)
}

@Composable
fun MonitorOverlay(preview:PreviewView,hist:Boolean,zebra:Boolean,peaking:Boolean){
    var frame by remember { mutableStateOf<Sample?>(null) }
    LaunchedEffect(preview,hist,zebra,peaking) {
        while(hist || zebra || peaking){
            val bitmap=try{preview.bitmap}catch(_:Exception){null}
            frame=if(bitmap!=null)withContext(Dispatchers.Default){sample(bitmap)}else null
            delay(800)
        }
    }
    val data=frame?:return
    if(zebra) Canvas(Modifier.fillMaxSize()) {
        for(p in data.clipped){
            val x=p.x*size.width;val y=p.y*size.height
            drawLine(Color.Red.copy(alpha=.75f),Offset(x-4f,y+4f),
                Offset(x+4f,y-4f),strokeWidth=2f)
        }
    }
    if(peaking) Canvas(Modifier.fillMaxSize()) {
        for(p in data.edges) drawCircle(Color.Yellow.copy(alpha=.8f),radius=1.3f,
            center=Offset(p.x*size.width,p.y*size.height))
    }
    if(hist) Canvas(Modifier.size(120.dp,54.dp).background(Color.Black.copy(alpha=.72f))) {
        val width=size.width/32f
        val max=(data.r+data.g+data.b).maxOrNull()?.coerceAtLeast(1)?:1
        listOf(data.r to Color.Red,data.g to Color.Green,data.b to Color.Blue).forEach{ (values,color) ->
            for(i in 0..31){
                val yy=size.height*(1-values[i].toFloat()/max)
                drawLine(color.copy(alpha=.7f),Offset(i*width,size.height),
                    Offset(i*width,yy),strokeWidth=width*.75f)
            }
        }
    }
}