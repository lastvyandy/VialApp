package cl.vialapp.app.ui.games.crossroads

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.min

private val asphalt = Color(0xFF3D4858)
private val sidewalk = Color(0xFFBFC9CE)
private val dash = Color(0xFFE7EBED)
private val paints = listOf(Color(0xFF3B82F6), Color(0xFFF97316), Color(0xFF14B8A6), Color(0xFFFACC15), Color(0xFFA78BFA))

@Composable
internal fun TrafficBoard(frame: TrafficFrame, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp))) {
        val unit = min(size.width, size.height) / 1000f
        scale(unit, unit, pivot = Offset.Zero) {
            drawRect(Color(0xFF789B78), size = Size(1000f, 1000f))
            // Raised sidewalks and four blocks; asphalt has no emoji or static car tokens.
            drawRect(sidewalk, Offset(294f, 0f), Size(412f, 1000f))
            drawRect(sidewalk, Offset(0f, 294f), Size(1000f, 412f))
            drawRect(asphalt, Offset(330f, 0f), Size(340f, 1000f))
            drawRect(asphalt, Offset(0f, 330f), Size(1000f, 340f))
            district(25f, 25f, Color(0xFFB7A18B), Color(0xFFE4D7B9))
            district(727f, 25f, Color(0xFF74929C), Color(0xFFA7CCD0))
            district(25f, 727f, Color(0xFF9D898F), Color(0xFFD8B7A9))
            district(727f, 727f, Color(0xFF749F86), Color(0xFFB9D5B3))

            for (i in 0..4) {
                val d = 12f + i * 62f
                drawLine(dash.copy(alpha = .70f), Offset(500f, d), Offset(500f, d + 33f), 5f)
                drawLine(dash.copy(alpha = .70f), Offset(500f, 995f - d), Offset(500f, 962f - d), 5f)
                drawLine(dash.copy(alpha = .70f), Offset(d, 500f), Offset(d + 33f, 500f), 5f)
                drawLine(dash.copy(alpha = .70f), Offset(995f - d, 500f), Offset(962f - d, 500f), 5f)
            }
            repeat(7) { i ->
                val d = 343f + i * 47f
                drawRect(dash, Offset(d, 334f), Size(26f, 37f))
                drawRect(dash, Offset(d, 629f), Size(26f, 37f))
                drawRect(dash, Offset(334f, d), Size(37f, 26f))
                drawRect(dash, Offset(629f, d), Size(37f, 26f))
            }
            drawLine(dash, Offset(334f, 326f), Offset(500f, 326f), 6f)
            drawLine(dash, Offset(500f, 674f), Offset(666f, 674f), 6f)
            drawLine(dash, Offset(326f, 500f), Offset(326f, 666f), 6f)
            drawLine(dash, Offset(674f, 334f), Offset(674f, 500f), 6f)

            light(311f, 281f, frame.green == TrafficAxis.VERTICAL, frame.pending != null)
            light(689f, 719f, frame.green == TrafficAxis.VERTICAL, frame.pending != null)
            light(281f, 689f, frame.green == TrafficAxis.HORIZONTAL, frame.pending != null)
            light(719f, 311f, frame.green == TrafficAxis.HORIZONTAL, frame.pending != null)
            for (car in frame.cars) drawCar(car, frame.elapsed)
        }
    }
}

private fun DrawScope.district(x: Float, y: Float, wall: Color, roof: Color) {
    drawRoundRect(Color(0xFF9AB593), Offset(x, y), Size(248f, 248f), CornerRadius(19f))
    drawRoundRect(Color(0xFF526B58).copy(alpha = .36f), Offset(x + 37f, y + 41f), Size(173f, 127f), CornerRadius(8f))
    drawRoundRect(wall, Offset(x + 27f, y + 26f), Size(174f, 127f), CornerRadius(8f))
    drawRoundRect(roof, Offset(x + 37f, y + 35f), Size(155f, 105f), CornerRadius(7f))
    repeat(4) { i ->
        drawRect(Color(0xFF526879), Offset(x + 48f + i * 34f, y + 78f), Size(20f, 37f))
    }
    for (i in 0..2) {
        val center = Offset(x + 48f + i * 72f, y + 205f)
        drawCircle(Color(0xFF527A55), 24f, center + Offset(4f, 4f))
        drawCircle(Color(0xFF3F7655), 21f, center)
        drawCircle(Color(0xFF689A66), 13f, center - Offset(5f, 6f))
    }
}

private fun DrawScope.light(x: Float, y: Float, green: Boolean, changing: Boolean) {
    drawRoundRect(Color(0xFF192536), Offset(x - 18f, y - 31f), Size(36f, 62f), CornerRadius(9f))
    drawCircle(Color(0xFF573B40), 10f, Offset(x, y - 17f))
    drawCircle(Color(0xFF3D5145), 10f, Offset(x, y + 17f))
    val color = if (green) Color(0xFF4ADE80) else if (changing) Color(0xFFFBBF24) else Color(0xFFFB7185)
    val center = Offset(x, y + if (green) 17f else -17f)
    drawCircle(color.copy(alpha = .30f), 19f, center)
    drawCircle(color, 10f, center)
}

private fun DrawScope.drawCar(car: TrafficCar, elapsed: Float) {
    val p = car.progress * 1000f
    val (center, angle) = when (car.lane) {
        TrafficLane.NORTH -> Offset(416f, p) to 180f
        TrafficLane.SOUTH -> Offset(584f, 1000f - p) to 0f
        TrafficLane.WEST -> Offset(p, 584f) to 90f
        TrafficLane.EAST -> Offset(1000f - p, 416f) to -90f
    }
    val color = if (car.emergency) Color(0xFFFAFAFA) else paints[car.paint % paints.size]
    rotate(angle, center) {
        val x = center.x
        val y = center.y
        drawRoundRect(Color(0xFF111827).copy(alpha = .4f), Offset(x - 28f, y - 40f), Size(62f, 94f), CornerRadius(13f))
        for (dx in listOf(-33f, 24f)) {
            drawRoundRect(Color(0xFF202833), Offset(x + dx, y - 28f), Size(9f, 23f), CornerRadius(2f))
            drawRoundRect(Color(0xFF202833), Offset(x + dx, y + 17f), Size(9f, 23f), CornerRadius(2f))
        }
        drawRoundRect(color, Offset(x - 29f, y - 46f), Size(58f, 92f), CornerRadius(12f))
        drawRoundRect(Color(0xFF253B51), Offset(x - 22f, y - 26f), Size(44f, 20f), CornerRadius(5f))
        drawRoundRect(Color(0xFF486276), Offset(x - 20f, y + 17f), Size(40f, 16f), CornerRadius(4f))
        drawRoundRect(color.copy(alpha = .92f), Offset(x - 21f, y - 4f), Size(42f, 19f), CornerRadius(5f))
        for (dx in listOf(-21f, 15f)) {
            drawRoundRect(Color(0xFFFEF3C7), Offset(x + dx, y - 41f), Size(7f, 6f), CornerRadius(2f))
            drawRoundRect(Color(0xFFBE123C), Offset(x + dx, y + 37f), Size(7f, 5f), CornerRadius(2f))
        }
        if (car.emergency) {
            drawRect(Color(0xFFE11D48), Offset(x - 12f, y + 4f), Size(24f, 6f))
            drawRect(Color(0xFFE11D48), Offset(x - 3f, y - 5f), Size(6f, 24f))
            val flash = if ((elapsed * 6).toInt() % 2 == 0) Color(0xFFF43F5E) else Color(0xFF38BDF8)
            drawRoundRect(flash, Offset(x - 17f, y - 12f), Size(34f, 7f), CornerRadius(4f))
        }
        if (car.waiting > if (car.emergency) 3f else 6f) {
            val fraction = (car.waiting / if (car.emergency) 9f else 15f).coerceIn(0f, 1f)
            drawRoundRect(Color(0xFF1F2937), Offset(x - 31f, y + 51f), Size(62f, 8f), CornerRadius(4f))
            drawRoundRect(if (fraction > .7f) Color(0xFFFB7185) else Color(0xFFFBBF24), Offset(x - 31f, y + 51f), Size(62f * fraction, 8f), CornerRadius(4f))
        }
    }
}
