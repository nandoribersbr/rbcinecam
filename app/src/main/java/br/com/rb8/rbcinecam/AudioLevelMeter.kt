package br.com.rb8.rbcinecam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10

/** True peak envelope of CameraX recording audio, derived from VideoRecordEvent.Status. */
@Composable
fun AudioLevelMeter(level:Float,peak:Float,active:Boolean,status:String) {
    val db=if(active && level>0f) (20.0*log10(level.toDouble())).coerceAtLeast(-60.0) else -60.0
    Row(Modifier.fillMaxWidth().height(29.dp).padding(horizontal=9.dp),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("AUDIO",color=Color(0xFFAFC0D5),fontSize=10.sp)
        Canvas(Modifier.weight(1f).fillMaxHeight().padding(vertical=7.dp)) {
            val n=48
            val step=size.width/n
            val fraction=if(active) ((db+60)/60).toFloat().coerceIn(0f,1f) else 0f
            for(i in 0 until n) {
                val lit=i.toFloat()/n <= fraction && active
                val base=when {
                    i>=44 -> Color(0xFFEF3D4E)
                    i>=36 -> Color(0xFFFCC547)
                    else -> Color(0xFF45D799)
                }
                drawLine(if(lit) base else Color(0xFF35414E),
                    Offset(i*step,size.height),Offset(i*step,0f),strokeWidth=(step*.64f).coerceAtLeast(1f))
            }
        }
        Text(if(active) String.format(java.util.Locale.US,"%.0f dBFS",db) else status,
            color=if(active && db>-6) Color(0xFFEF3D4E) else Color(0xFFAFC0D5),fontSize=10.sp)
    }
}
