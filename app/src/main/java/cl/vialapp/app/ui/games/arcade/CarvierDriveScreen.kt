package cl.vialapp.app.ui.games.arcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.games.GameProgressStore
import kotlin.math.abs

@Composable
fun CarvierDriveScreen(userKey: String, onBack: () -> Unit) {
    ArcadeSession(
        userKey = userKey, gameId = GameProgressStore.CARVIER_DRIVE, title = "Carvier al volante",
        subtitle = "Una carretera, tres carriles y 65 segundos para llegar a la meta.",
        rules = "Desliza o usa las flechas para cambiar de carril. Recoge monedas solo cuando sea seguro. En zona escolar activa 30 km/h. Cuando aparezca PARE, pulsa Detener, espera el aviso de cruce libre y pulsa Avanzar. Tienes tres vidas.",
        duration = DriveEngine.TIME, create = { DriveEngine() }, onBack = onBack,
        scene = { frame, modifier, enabled, act ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val scheme = MaterialTheme.colorScheme
                Surface(color = if (frame.stopChallenge) Color(0xFFB42318) else if (frame.schoolZone) Color(0xFF85520E) else scheme.surface, shape = RoundedCornerShape(12.dp)) {
                    val fg = if (frame.stopChallenge || frame.schoolZone) Color.White else scheme.onSurface
                    Text(when {
                        frame.stopChallenge && frame.stopCompleted && frame.stopped -> "PARE · cruce libre, pulsa Avanzar"
                        frame.stopChallenge && frame.stopCompleted -> "✓ PARE respetado"
                        !frame.stopChallenge && (frame.elapsed in 15f..18f || frame.elapsed in 40f..43f) -> "PRÓXIMA ZONA ESCOLAR · reduce a 30 km/h"
                        frame.stopChallenge -> "PARE · detención completa"
                        frame.schoolZone -> "ZONA ESCOLAR · máximo 30 km/h"
                        else -> "VELOCIDAD: ${if (frame.stopped) 0 else if (frame.slow) 30 else 50} km/h (simulada)"
                    }, Modifier.fillMaxWidth().padding(9.dp), color = fg, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                }
                DriveRoad(frame, Modifier.weight(1f).fillMaxWidth(), enabled, act)
            }
        },
        controls = { frame, enabled, act ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArcadeControl("◀ Izq.", enabled && !frame.stopped && frame.lane > 0, Modifier.weight(1f)) { act(0) }
                ArcadeControl(if (frame.slow) "30 km/h ✓" else "Reducir a 30", enabled && !frame.stopped, Modifier.weight(1.35f), frame.slow) { act(2) }
                ArcadeControl("Der. ▶", enabled && !frame.stopped && frame.lane < 2, Modifier.weight(1f)) { act(1) }
            }
            if ((frame.stopChallenge && !frame.stopCompleted) || frame.stopped) {
                ArcadeControl(if (frame.stopped && frame.stopCompleted) "AVANZAR" else if (frame.stopped) "DETENIDO · ESPERA" else "DETENER (PARE)", enabled && (!frame.stopped || frame.stopCompleted), Modifier.fillMaxWidth(), frame.stopCompleted) { act(3) }
            }
            }
        }
    )
}

private val roadPaints = listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFF14B8A6), Color(0xFFA78BFA))
private fun laneX(lane: Float) = 205f + lane * 145f

@Composable
private fun DriveRoad(frame: DriveFrame, modifier: Modifier, enabled: Boolean, act: (Int) -> Unit) {
    val currentAct by rememberUpdatedState(act)
    Canvas(modifier.clip(RoundedCornerShape(22.dp)).pointerInput(enabled) {
        if (enabled) {
            var travel = 0f
            detectHorizontalDragGestures(
                onDragStart = { travel = 0f },
                onDragEnd = { travel = 0f },
                onDragCancel = { travel = 0f }
            ) { change, amount ->
                travel += amount
                if (abs(travel) > size.width * .14f) {
                    currentAct(if (travel < 0) 0 else 1)
                    travel = 0f
                }
                change.consume()
            }
        }
    }) {
        scale(size.width / 700f, size.height / 1000f, pivot = Offset.Zero) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF78A889), Color(0xFF43765E)), endY = 1000f), size = Size(700f, 1000f))
            drawRect(Color(0xFFA3B2AB), Offset(110f, 0f), Size(480f, 1000f))
            drawRect(Color(0xFF354251), Offset(132f, 0f), Size(436f, 1000f))
            drawLine(Color(0xFFE2E8F0), Offset(138f, 0f), Offset(138f, 1000f), 5f)
            drawLine(Color(0xFFE2E8F0), Offset(562f, 0f), Offset(562f, 1000f), 5f)
            val offset = (frame.distance * 14f) % 145f
            for (i in -1..8) {
                val y = i * 145f + offset
                for (x in listOf(278f, 422f)) drawLine(Color(0xFFE5E7EB).copy(alpha = .85f), Offset(x, y), Offset(x, y + 67f), 5f)
                for (x in listOf(54f, 646f)) {
                    drawCircle(Color(0xFF254D43).copy(alpha = .35f), 36f, Offset(x + 5f, y + 25f))
                    drawCircle(Color(0xFF28674B), 32f, Offset(x, y + 17f))
                    drawCircle(Color(0xFF6EA16B), 19f, Offset(x - 6f, y + 11f))
                }
            }
            if (frame.schoolZone) {
                drawRect(Color(0xFFFBBF24).copy(alpha = .18f), Offset(132f, 0f), Size(436f, 1000f))
                for (i in 0..4) drawRect(Color(0xFFFDE68A), Offset(143f + i * 87f, 70f), Size(62f, 20f))
                drawCircle(Color(0xFFFBBF24), 37f, Offset(64f, 130f))
                drawCircle(Color(0xFF1F2937), 28f, Offset(64f, 130f))
                drawLine(Color(0xFFFBBF24), Offset(49f, 146f), Offset(79f, 114f), 8f)
            }
            if (frame.stopChallenge && !frame.stopCompleted) {
                // Arcade checkpoint: visual stop line and octagonal marker ahead of the player.
                drawLine(Color.White, Offset(145f, 737f), Offset(555f, 737f), 9f)
                val center = Offset(626f, 710f)
                val sign = Path().apply {
                    for (i in 0..7) {
                        val angle = Math.PI / 8 + i * Math.PI / 4
                        val x = center.x + (42 * kotlin.math.cos(angle)).toFloat()
                        val y = center.y + (42 * kotlin.math.sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(sign, Color(0xFFB42318))
                drawPath(sign, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(5f))
                drawContext.canvas.nativeCanvas.drawText("PARE", center.x, center.y + 8f,
                    android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    })
            }
            frame.items.forEach { item ->
                val center = Offset(laneX(item.lane.toFloat()), item.y * 1000f)
                when (item.kind) {
                    RoadItemKind.TRAFFIC -> roadCar(center, roadPaints[item.paint % roadPaints.size], false, 1f)
                    RoadItemKind.CONE -> {
                        drawRoundRect(Color(0xFF111827).copy(alpha = .3f), center - Offset(33f, 9f), Size(70f, 32f), CornerRadius(8f))
                        val cone = Path().apply { moveTo(center.x, center.y - 40f); lineTo(center.x - 27f, center.y + 18f); lineTo(center.x + 27f, center.y + 18f); close() }
                        drawPath(cone, Color(0xFFF97316))
                        drawLine(Color.White, center - Offset(16f, 3f), center + Offset(16f, -3f), 8f)
                    }
                    RoadItemKind.COIN -> {
                        drawCircle(Color(0xFFB77912), 29f, center + Offset(0f, 5f))
                        drawCircle(Color(0xFFFBBF24), 28f, center)
                        drawCircle(Color(0xFFFFE7A0), 20f, center)
                        drawLine(Color(0xFFD68B13), center - Offset(0f, 12f), center + Offset(0f, 12f), 6f)
                    }
                }
            }
            val opacity = if (frame.invulnerable > 0f && (frame.elapsed * 10).toInt() % 2 == 0) .45f else 1f
            roadCar(Offset(laneX(frame.carX), 830f), Color(0xFF38A9F0), true, opacity)
        }
    }
}

private fun DrawScope.roadCar(center: Offset, color: Color, player: Boolean, opacity: Float) {
    val x = center.x; val y = center.y
    fun paint(c: Color) = c.copy(alpha = opacity)
    drawRoundRect(paint(Color(0xFF202833)), Offset(x - 48f, y - 65f), Size(96f, 142f), CornerRadius(19f))
    for (dx in listOf(-52f, 40f)) {
        drawRoundRect(paint(Color(0xFF111827)), Offset(x + dx, y - 47f), Size(12f, 35f), CornerRadius(3f))
        drawRoundRect(paint(Color(0xFF111827)), Offset(x + dx, y + 30f), Size(12f, 30f), CornerRadius(3f))
    }
    drawRoundRect(paint(color), Offset(x - 46f, y - 72f), Size(92f, 138f), CornerRadius(21f))
    drawRoundRect(paint(Color(0xFF1D354C)), Offset(x - 35f, y - 35f), Size(70f, 33f), CornerRadius(8f))
    drawRoundRect(paint(Color(0xFF43627A)), Offset(x - 30f, y + 22f), Size(60f, 23f), CornerRadius(7f))
    drawRoundRect(paint(Color.White.copy(alpha = .23f)), Offset(x - 34f, y - 67f), Size(68f, 15f), CornerRadius(8f))
    for (dx in listOf(-31f, 19f)) {
        drawRoundRect(paint(Color(0xFFFFF1BD)), Offset(x + dx, y - 66f), Size(13f, 9f), CornerRadius(3f))
        drawRoundRect(paint(Color(0xFFBE123C)), Offset(x + dx, y + 54f), Size(13f, 7f), CornerRadius(3f))
    }
    if (player) {
        drawRoundRect(paint(Color(0xFFDBEAFE)), Offset(x - 8f, y - 51f), Size(16f, 100f), CornerRadius(5f))
        drawRoundRect(paint(Color(0xFF145DB2)), Offset(x - 32f, y - 3f), Size(64f, 24f), CornerRadius(7f))
    }
}
