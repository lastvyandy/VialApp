package cl.vialapp.app.ui.simulator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class RoadPose(val driverY: Float, val actorX: Float, val green: Boolean)

internal fun roadPose(scene: String, progress: Float): RoadPose {
    val p = progress.coerceIn(0f, 3f)
    val stop = if (scene == "pedestrian") 254f else 282f
    val driver = when {
        p < 1f -> 326f + (stop - 326f) * p
        p < 2f -> stop
        else -> stop + (30f - stop) * (p - 2f)
    }
    val from = if (scene == "pedestrian") 135f else 48f
    val middle = if (scene == "pedestrian") 180f else 97f
    val to = if (scene == "pedestrian") 262f else 414f
    val actor = when {
        p < 1f -> from + (middle - from) * p
        p < 2f -> middle + (to - middle) * (p - 1f)
        else -> to
    }
    return RoadPose(driver, actor, p >= 1f)
}

@Composable
internal fun SimulatorBoard(scene: String, progress: Float, modifier: Modifier = Modifier, warning: Boolean = false) {
    val pose = roadPose(scene, progress)
    val description = when(scene) {
        "stop" -> "Señal PARE"
        "signal" -> if (pose.green) "Semáforo verde" else "Semáforo rojo"
        "rain" -> "Calzada mojada y vehículo delantero"
        "cyclist" -> "Ciclista delante y tránsito en sentido contrario"
        "roundabout" -> "Rotonda con señal Ceda el paso"
        else -> "Paso peatonal"
    }
    Canvas(modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(22.dp))
        .semantics { contentDescription = "Escena vial. Vehículo azul. " + description }) {
        val factor = size.width / 360f
        scale(factor, factor, pivot = Offset.Zero) {
            if (scene == "rain" || scene == "cyclist" || scene == "roundabout") {
                drawExtendedScene(scene, progress, warning)
                return@scale
            }
            val asphalt = Color(0xFF505B67)
            val walk = Color(0xFFD9D8CB)
            drawRect(Color(0xFF9DBA86), Offset.Zero, Size(360f,360f))
            drawRect(walk, Offset(105f,0f), Size(150f,360f))
            if (scene != "pedestrian") drawRect(walk, Offset(0f,105f), Size(360f,150f))
            drawRect(asphalt, Offset(123f,0f), Size(114f,360f))
            if (scene != "pedestrian") drawRect(asphalt, Offset(0f,123f), Size(360f,114f))
            for (i in 0..16) {
                val y = i * 22f
                if ((scene == "pedestrian" && y !in 145f..220f) || (scene != "pedestrian" && (y < 103f || y > 252f)))
                    drawLine(Color(0xFFF3D790), Offset(180f,y), Offset(180f,y+13f),2.6f)
                if (scene != "pedestrian" && (y < 103f || y > 252f))
                    drawLine(Color(0xFFF3D790), Offset(y,180f), Offset(y+13f,180f),2.6f)
            }
            if (scene == "pedestrian") {
                for (i in 0..6) drawRect(Color(0xFFF4F5F3), Offset(128f+i*16f,161f), Size(10f,40f))
                drawLine(Color.White,Offset(182f,219f),Offset(233f,219f),4f)
            } else {
                drawLine(Color.White,Offset(182f,249f),Offset(233f,249f),4f)
            }
            listOf(Offset(32f,35f),Offset(82f,80f),Offset(309f,36f),
                Offset(43f,300f),Offset(308f,310f),Offset(290f,78f)).forEach { at ->
                drawCircle(Color(0x35415C44),20f,at + Offset(3f,5f))
                drawCircle(Color(0xFF52835D),18f,at)
                drawCircle(Color(0xFF72A571),11f,at - Offset(4f,4f))
            }
            if (scene == "stop") {
                val center = Offset(271f,252f)
                drawLine(Color(0xFF65727A),center,center+Offset(0f,27f),4f)
                val octagon = Path().apply {
                    repeat(8) { i ->
                        val angle = PI/8 + i*PI/4
                        val x = center.x + 21f*cos(angle).toFloat()
                        val y = center.y + 21f*sin(angle).toFloat()
                        if(i==0) moveTo(x,y) else lineTo(x,y)
                    }
                    close()
                }
                drawPath(octagon,Color(0xFFC9343B))
                drawPath(octagon,Color.White,style=Stroke(2f))
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color=android.graphics.Color.WHITE
                    textAlign=android.graphics.Paint.Align.CENTER
                    textSize=11f
                    typeface=android.graphics.Typeface.DEFAULT_BOLD
                }
                drawContext.canvas.nativeCanvas.drawText("PARE",center.x,center.y+4f,paint)
            } else if(scene=="signal") {
                drawLine(Color(0xFF687582),Offset(270f,228f),Offset(270f,259f),4f)
                drawRoundRect(Color(0xFF202B35),Offset(257f,176f),Size(26f,63f),CornerRadius(7f))
                drawCircle(if(!pose.green) Color(0xFFFF5057) else Color(0xFF66383D),7f,Offset(270f,188f))
                drawCircle(Color(0xFF6B592B),7f,Offset(270f,207f))
                drawCircle(if(pose.green) Color(0xFF67EF91) else Color(0xFF315943),7f,Offset(270f,226f))
            }
            if(scene=="pedestrian") {
                val at=Offset(pose.actorX,181f)
                drawCircle(Color(0x44232B31),9f,at+Offset(2f,5f))
                drawLine(Color(0xFF243447),at+Offset(-4f,2f),at+Offset(-5f,10f),4f,StrokeCap.Round)
                drawLine(Color(0xFF243447),at+Offset(4f,2f),at+Offset(7f,9f),4f,StrokeCap.Round)
                drawRoundRect(Color(0xFFE9A945),at-Offset(7f,7f),Size(14f,13f),CornerRadius(4f))
                drawCircle(Color(0xFFE9C3A1),5f,at-Offset(0f,10f))
            } else car(Offset(pose.actorX,210f),Color(0xFFCF5354),90f,false)
            car(Offset(210f,pose.driverY),Color(0xFF3985DF),0f,progress in 1f..2f)
            if(warning) drawRoundRect(Color(0xFFF15B52),Offset(5f,5f),Size(350f,350f),CornerRadius(22f),style=Stroke(5f))
        }
    }
}

internal fun DrawScope.car(at: Offset, body: Color, heading: Float, braking: Boolean) {
    rotate(heading,at) {
        drawRoundRect(Color(0x44202932),at-Offset(15f,27f)+Offset(3f,5f),Size(32f,58f),CornerRadius(9f))
        for(y in listOf(-19f,13f)) {
            drawRoundRect(Color(0xFF222A32),at+Offset(-16f,y),Size(5f,12f),CornerRadius(2f))
            drawRoundRect(Color(0xFF222A32),at+Offset(11f,y),Size(5f,12f),CornerRadius(2f))
        }
        drawRoundRect(body,at-Offset(14f,29f),Size(28f,58f),CornerRadius(9f))
        drawRoundRect(Color(0xFF29475C),at-Offset(10f,18f),Size(20f,36f),CornerRadius(5f))
        drawRoundRect(body,at-Offset(10f,7f),Size(20f,14f),CornerRadius(3f))
        for(x in listOf(-10f,3f)) {
            drawRoundRect(Color(0xFFFFE6A6),at+Offset(x,-26f),Size(7f,4f),CornerRadius(2f))
            drawRoundRect(if(braking) Color(0xFFFFB4AE) else Color(0xFFB83A43),at+Offset(x,23f),Size(7f,4f),CornerRadius(2f))
        }
    }
}
