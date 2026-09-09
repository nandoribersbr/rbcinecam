package br.com.rb8digital.rbcinecam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

private val HudPanel10 = Color(0xFF090B0E)
private val HudAccent10 = Color(0xFFFFC400)
private val HudRed10 = Color(0xFFFF2020)

@Composable
fun TopHud10(
    mode: CameraMode,
    recording: Boolean,
    status: String,
    elapsed: Long,
    free: String,
    battery: Int,
    audioEnabled: Boolean,
    db: Float,
    shutter: String
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .zIndex(20f)
            .clipToBounds()
            .background(HudPanel10)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val spec = TopHudPolicy10.forWidthDp(maxWidth.value, recording)
        val compact = spec.density != TopHudDensity10.FULL
        val critical = spec.density == TopHudDensity10.CRITICAL

        Row(
            Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (critical) 6.dp else 9.dp)
        ) {
            if (spec.showFullBrand) {
                Text("RB", color = HudRed10, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1)
                Text("CineCam", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            } else {
                Text("RB", color = HudRed10, fontWeight = FontWeight.Black, fontSize = 13.sp, maxLines = 1)
            }

            if (mode == CameraMode.VIDEO) {
                Text(if (critical) "FHD30" else "FHD", color = Color.White, fontSize = 9.sp, maxLines = 1)
                if (!critical) Text(if (compact) "30" else "30 FPS", color = Color.White, fontSize = 9.sp, maxLines = 1)
                Text(if (shutter == "AUTO") "AUTO" else shutter, color = Color.White, fontSize = 9.sp, maxLines = 1)
                if (spec.showCodec) Text(if (compact) "MP4" else "MP4/H.264", color = Color.LightGray, fontSize = 8.sp, maxLines = 1)
            } else {
                Text("FOTO", color = Color.White, fontSize = 9.sp, maxLines = 1)
                if (spec.showCodec) Text("JPEG", color = Color.LightGray, fontSize = 8.sp, maxLines = 1)
            }

            Text(
                if (audioEnabled) "MIC ${"%.0f".format(db)}" else "MIC OFF",
                color = if (audioEnabled) Color.LightGray else HudRed10,
                fontSize = 8.sp,
                maxLines = 1
            )

            Text(if (compact) free.replace(" GB", "G") else "Livre $free", color = Color.LightGray, fontSize = 8.sp, maxLines = 1)

            if (spec.showBattery && battery >= 0) {
                Text(if (compact) "$battery%" else "BAT $battery%", color = Color.LightGray, fontSize = 8.sp, maxLines = 1)
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = if (recording) "● REC ${formatHudDuration10(elapsed)}" else if (critical) "● STBY" else status,
                color = if (recording) HudRed10 else HudAccent10,
                fontWeight = FontWeight.Bold,
                fontSize = if (critical) 9.sp else 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

private fun formatHudDuration10(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
