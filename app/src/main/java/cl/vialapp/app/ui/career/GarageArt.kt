package cl.vialapp.app.ui.career

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale

internal fun garageColor(hex: String): Color =
    try { Color(android.graphics.Color.parseColor(hex)) } catch (_: IllegalArgumentException) { Color(0xFF38A9F0) }

internal fun zoneName(zone: String): String = when(zone) {
    "city" -> "Centro urbano"
    "coast" -> "Ruta costera"
    "mountain" -> "Ruta de montaña"
    "metro" -> "Gran ciudad"
    else -> "Barrio de aprendizaje"
}

internal fun zoneColors(zone:String): List<Color> = when(zone) {
    "city" -> listOf(Color(0xFF72849A),Color(0xFF3C526A))
    "coast" -> listOf(Color(0xFF65B5BD),Color(0xFF387A96))
    "mountain" -> listOf(Color(0xFF94A7BC),Color(0xFF516C81))
    "metro" -> listOf(Color(0xFF505678),Color(0xFF242B45))
    else -> listOf(Color(0xFF78A889),Color(0xFF43765E))
}

@Composable
internal fun VehiclePreview(style:String, color:Color, modifier:Modifier=Modifier) {
    Canvas(modifier.aspectRatio(1.28f)) {
        val unit=minOf(size.width/220f,size.height/172f)
        scale(unit,unit,pivot=Offset.Zero) {
            val center=Offset(size.width/unit/2f,size.height/unit/2f)
            drawOval(Color(0x42101827),center-Offset(76f,70f),Size(152f,148f))
            drawGarageVehicle(center,color,style)
        }
    }
}

/** The decorative silhouette varies within the unchanged arcade collision envelope. */
internal fun DrawScope.drawGarageVehicle(center:Offset,color:Color,style:String,opacity:Float=1f) {
    val w=when(style) {"hatch"->42f;"sedan"->43f;"suv"->47f;"sport"->46f;else->44f}
    val h=when(style) {"hatch"->61f;"sedan"->70f;"suv"->71f;"sport"->65f;else->67f}
    val roof=when(style) {"suv"->32f;"hatch"->31f;"sport"->24f;else->28f}
    val x=center.x; val y=center.y
    fun c(value:Color)=value.copy(alpha=value.alpha*opacity)
    drawRoundRect(c(Color(0x55323B45)),Offset(x-w+4,y-h+7),Size(w*2,h*2),CornerRadius(19f))
    for(dx in listOf(-w-5,w-7)) for(dy in listOf(-h+16,h-40))
        drawRoundRect(c(Color(0xFF101827)),Offset(x+dx,y+dy),Size(12f,24f),CornerRadius(4f))
    drawRoundRect(c(color),Offset(x-w,y-h),Size(w*2,h*2),CornerRadius(if(style=="suv") 13f else 22f))
    drawRoundRect(c(Color(0xFF193348)),Offset(x-w+9,y-h+25),Size((w-9)*2,h*2-49),CornerRadius(11f))
    drawRoundRect(c(color),Offset(x-roof,y-17f),Size(roof*2,45f),CornerRadius(7f))
    drawRoundRect(c(Color.White.copy(alpha=.22f)),Offset(x-w+9,y-h+7),Size((w-9)*2,12f),CornerRadius(5f))
    if(style=="sport" || style=="compact") {
        drawRoundRect(c(Color(0xFFDCE7EF)),Offset(x-5f,y-h+5f),Size(10f,h*2-10f),CornerRadius(3f))
    }
    if(style=="suv") for(dx in listOf(-26f,23f)) drawLine(c(Color(0xFF243447)),Offset(x+dx,y-20),Offset(x+dx,y+29),4f)
    if(style=="sport") drawRoundRect(c(Color(0xFF192636)),Offset(x-w+3,y+h-14),Size(w*2-6,8f),CornerRadius(3f))
    for(dx in listOf(-w+8,w-20)) {
        drawRoundRect(c(Color(0xFFFFE7B0)),Offset(x+dx,y-h+6),Size(12f,8f),CornerRadius(3f))
        drawRoundRect(c(Color(0xFFB4233C)),Offset(x+dx,y+h-11),Size(12f,6f),CornerRadius(2f))
    }
}
