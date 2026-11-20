package cl.vialapp.app.ui.career

// GUÍA DEL ARCHIVO:
// Estilos visuales reutilizables del garaje y sus autos.

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap

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

/** Layered, lighting-aware top view for the arcade; no changes to the collision envelope. */
internal fun DrawScope.drawGarageVehicle(center:Offset,color:Color,style:String,opacity:Float=1f) {
    val w=when(style) {"hatch"->42f;"sedan"->43f;"suv"->47f;"sport"->46f;else->44f}
    val h=when(style) {"hatch"->61f;"sedan"->70f;"suv"->71f;"sport"->65f;else->67f}
    val x=center.x; val y=center.y
    fun c(value:Color)=value.copy(alpha=value.alpha*opacity)
    val body=Path().apply {
        moveTo(x-w*.63f,y-h)
        quadraticBezierTo(x-w*.97f,y-h+4f,x-w,y-h+22f)
        lineTo(x-w,y+h-24f)
        quadraticBezierTo(x-w*.95f,y+h-3f,x-w*.64f,y+h)
        lineTo(x+w*.64f,y+h)
        quadraticBezierTo(x+w*.95f,y+h-3f,x+w,y+h-24f)
        lineTo(x+w,y-h+22f)
        quadraticBezierTo(x+w*.97f,y-h+4f,x+w*.63f,y-h)
        close()
    }
    drawOval(c(Color(0x65101A27)),Offset(x-w-7f,y-h+9f),Size(w*2+17f,h*2+8f))
    for(dx in listOf(-w-5,w-7))for(dy in listOf(-h+17,h-41)){
        drawRoundRect(c(Color(0xFF101827)),Offset(x+dx,y+dy),Size(12f,26f),CornerRadius(4f))
        drawRoundRect(c(Color(0xFF536273)),Offset(x+dx+3f,y+dy+5f),Size(5f,16f),CornerRadius(2f))
    }
    val dark=lerp(color,Color(0xFF102032),.43f)
    val shine=lerp(color,Color.White,.32f)
    drawPath(body,Brush.horizontalGradient(listOf(c(dark),c(color),c(shine),c(color),c(dark)),
        startX=x-w,endX=x+w))
    drawPath(body,c(Color(0xB8172635)),style=Stroke(2.2f))
    // Sculpted hood, front fenders and painted roof.
    val hood=Path().apply{
        moveTo(x-w*.71f,y-h+12f);lineTo(x+w*.71f,y-h+12f)
        lineTo(x+w*.58f,y-25f);lineTo(x-w*.58f,y-25f);close()
    }
    drawPath(hood,Brush.verticalGradient(listOf(c(shine),c(color)),startY=y-h+10f,endY=y-25f))
    val top=if(style=="suv")-23f else -17f
    val bottom=if(style=="hatch")32f else 24f
    val canopy=Path().apply {
        moveTo(x-w*.62f,y-32f);lineTo(x+w*.62f,y-32f)
        lineTo(x+w*.74f,y+bottom);lineTo(x-w*.74f,y+bottom);close()
    }
    drawPath(canopy,Brush.verticalGradient(listOf(c(Color(0xFF7896B1)),c(Color(0xFF152A40)),c(Color(0xFF233D54))),
        startY=y-32f,endY=y+bottom))
    drawRoundRect(c(color),Offset(x-w*.50f,y+top),Size(w,bottom-top-10f),CornerRadius(7f))
    drawRoundRect(c(shine.copy(alpha=.44f)),Offset(x-w*.44f,y+top+2f),Size(w*.88f,5f),CornerRadius(3f))
    for(side in listOf(-1f,1f)){
        drawLine(c(shine),Offset(x+side*w*.84f,y-h+20f),Offset(x+side*w*.84f,y+h-22f),2.5f,StrokeCap.Round)
        drawRoundRect(c(dark),Offset(x+side*w-6f,y-23f),Size(12f,12f),CornerRadius(4f))
        drawLine(c(Color(0xFF9AAEBF)),Offset(x+side*w*.88f,y+4f),Offset(x+side*w*.88f,y+14f),2.2f,StrokeCap.Round)
    }
    if(style=="sport"||style=="compact"){
        drawRoundRect(c(Color(0xFFDCE7EF)),Offset(x-4f,y-h+8f),Size(8f,27f),CornerRadius(2f))
        drawRoundRect(c(Color(0xFFDCE7EF)),Offset(x-4f,y+top+2f),Size(8f,bottom-top-14f),CornerRadius(2f))
    }
    if(style=="suv")for(dx in listOf(-w*.41f,w*.41f))drawLine(c(Color(0xFF1C2936)),Offset(x+dx,y+top),Offset(x+dx,y+bottom),4f,StrokeCap.Round)
    if(style=="sport")drawRoundRect(c(Color(0xFF192636)),Offset(x-w+3,y+h-13),Size(w*2-6,8f),CornerRadius(3f))
    drawRoundRect(c(Color(0xFF14202E)),Offset(x-w*.33f,y-h+2f),Size(w*.66f,8f),CornerRadius(3f))
    for(dx in listOf(-w+7,w-19)){
        drawRoundRect(c(Color(0xFF7CA7D0)),Offset(x+dx,y-h+5f),Size(12f,11f),CornerRadius(3f))
        drawRoundRect(c(Color(0xFFF1FAFF)),Offset(x+dx+1f,y-h+6f),Size(10f,6f),CornerRadius(3f))
        drawRoundRect(c(Color(0xFF8D2338)),Offset(x+dx,y+h-13f),Size(12f,10f),CornerRadius(3f))
        drawRoundRect(c(Color(0xFFFF6D72)),Offset(x+dx+1f,y+h-10f),Size(10f,4f),CornerRadius(2f))
    }
}
