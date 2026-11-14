package cl.vialapp.app.ui.games.arcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.vialapp.app.data.games.GameProgressStore
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

@Composable
fun AutoWorkshopScreen(userKey: String, onBack: () -> Unit) {
    ArcadeSession(
        userKey = userKey, gameId = GameProgressStore.AUTO_WORKSHOP, title = "Taller del Auto",
        subtitle = "Inspecciona cuatro fallas del vehículo antes de que se acabe el tiempo.",
        rules = "La zona defectuosa se señala en rojo. Toca esa parte del auto y después elige la herramienta adecuada. Una selección incorrecta te cuesta una vida. Es una simulación de diagnóstico: no realiza reparaciones reales.",
        duration = WorkshopEngine.TIME, create = { WorkshopEngine() }, onBack = onBack,
        scene = { frame, modifier, enabled, act -> WorkshopScene(frame, modifier, enabled, act) },
        controls = { frame, enabled, act ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                for (row in 0..1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (column in 0..1) {
                            val index = row * 2 + column
                            ArcadeControl(WorkshopPart.entries[index].tool, enabled && frame.repairTime <= 0f,
                                Modifier.weight(1f)) { act(10 + index) }
                        }
                    }
                }
            }
        }
    )
}

// Wheel, headlight, battery, windshield. Coordinates are in normalized garage units.
private val hotspots = listOf(Offset(175f, 520f), Offset(235f, 155f), Offset(395f, 252f), Offset(350f, 375f))

@Composable
private fun WorkshopScene(frame: WorkshopFrame, modifier: Modifier, enabled: Boolean, act: (Int) -> Unit) {
    val currentAct by rememberUpdatedState(act)
    val c = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(c.surface).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(frame.fault?.symptom ?: "¡Inspección terminada!", color = c.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Canvas(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).pointerInput(enabled) {
            if (enabled) detectTapGestures { point ->
                val x = point.x * 700f / size.width
                val y = point.y * 750f / size.height
                val index = hotspots.indices.minByOrNull { hypot(hotspots[it].x - x, hotspots[it].y - y) }
                if (index != null && hypot(hotspots[index].x - x, hotspots[index].y - y) < 99f) currentAct(index)
            }
        }) {
            scale(size.width / 700f, size.height / 750f, pivot = Offset.Zero) {
                drawRect(Color(0xFF536274), size = Size(700f, 750f))
                for (i in 0..7) drawLine(Color(0xFF637388), Offset(i * 100f, 0f), Offset(i * 100f, 750f), 2f)
                for (i in 0..8) drawLine(Color(0xFF637388), Offset(0f, i * 100f), Offset(700f, i * 100f), 2f)
                drawRoundRect(Color(0xFF263445), Offset(114f, 40f), Size(472f, 670f), CornerRadius(24f))
                drawRoundRect(Color(0xFF8B9CAA), Offset(134f, 60f), Size(432f, 630f), CornerRadius(22f))
                drawLine(Color(0xFFFBBF24), Offset(148f, 65f), Offset(148f, 680f), 10f)
                drawLine(Color(0xFFFBBF24), Offset(552f, 65f), Offset(552f, 680f), 10f)
                for (x in listOf(177f, 470f)) for (y in listOf(205f, 482f)) {
                    val flat = frame.fault == WorkshopPart.TIRE && x < 200f && y > 300f
                    drawRoundRect(Color(0xFF111827), Offset(x - 24f, y), Size(if (flat) 38f else 52f, 86f), CornerRadius(12f))
                }
                drawRoundRect(Color(0xFF1C2F42).copy(alpha = .6f), Offset(209f, 102f), Size(294f, 548f), CornerRadius(78f))
                drawRoundRect(Color(0xFF3B82F6), Offset(202f, 95f), Size(296f, 548f), CornerRadius(73f))
                drawRoundRect(Color(0xFF1D3449), Offset(224f, 173f), Size(252f, 145f), CornerRadius(18f))
                drawRoundRect(Color(0xFF62788C), Offset(241f, 192f), Size(92f, 105f), CornerRadius(12f))
                for (i in 0..3) drawLine(Color(0xFF192D40), Offset(254f, 204f + i * 22f), Offset(318f, 204f + i * 22f), 6f)
                drawRoundRect(if (frame.fault == WorkshopPart.BATTERY) Color(0xFFF87171) else Color(0xFF4ADE80),
                    Offset(352f, 218f), Size(86f, 65f), CornerRadius(9f))
                drawLine(Color(0xFF111827), Offset(366f, 238f), Offset(386f, 238f), 5f)
                drawLine(Color(0xFF111827), Offset(402f, 238f), Offset(422f, 238f), 5f)
                drawLine(Color(0xFF111827), Offset(412f, 228f), Offset(412f, 248f), 5f)
                drawRoundRect(Color(0xFF486B85), Offset(231f, 325f), Size(238f, 98f), CornerRadius(15f))
                if (frame.fault == WorkshopPart.GLASS) {
                    repeat(6) { i -> drawLine(Color(0xFFCCB697).copy(alpha = .8f), Offset(248f + i * 31f, 338f), Offset(269f + i * 31f, 407f), 13f) }
                }
                drawRoundRect(Color(0xFF2563EB), Offset(248f, 437f), Size(204f, 83f), CornerRadius(16f))
                drawRoundRect(Color(0xFF39576F), Offset(238f, 533f), Size(224f, 62f), CornerRadius(13f))
                for (x in listOf(235f, 430f)) drawRoundRect(
                    if (x < 300f && frame.fault == WorkshopPart.LIGHT) Color(0xFF39495A) else Color(0xFFFFE7A0),
                    Offset(x - 13f, 135f), Size(48f, 26f), CornerRadius(8f))
                hotspots.forEachIndexed { index, point ->
                    val selected = frame.selectedPart == index
                    val faulty = frame.fault?.ordinal == index
                    val accent = if (selected) Color(0xFF4ADE80) else if (faulty) Color(0xFFF87171) else Color(0xFFDCE7F6)
                    drawCircle(accent.copy(alpha = .2f), 42f, point)
                    drawCircle(accent, 42f, point, style = Stroke(width = if (selected) 7f else 4f))
                    drawCircle(accent, 7f, point)
                }
                if (frame.flash > 0f) drawRect(Color(0xFFEF4444).copy(alpha = .12f), size = Size(700f, 750f))
            }
        }
        if (frame.repairTime > 0f) {
            LinearProgressIndicator(progress = { (1f - frame.repairTime / 1.15f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
        Text(if (frame.selectedPart >= 0) "Seleccionado: ${WorkshopPart.entries[frame.selectedPart].label}" else "Toca una zona del auto.",
            color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
