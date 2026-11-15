package cl.vialapp.app.ui.career

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.abs

private val studioLight=V3(-.42f,.87f,.54f).unit()
private val coolLight=V3(.75f,.55f,-.55f).unit()
private val studioBlue=Color(0xFF93C5FD)

@Composable
internal fun ShowroomView(style:String,paint:Color,angle:Float,onRotate:(Float)->Unit,modifier:Modifier=Modifier,kit:GarageKit=GarageKit()) {
    val mesh=remember(style,kit){buildCarMesh(style,kit)}
    val renderer=remember{ShowroomRaster(512,358)}
    val bodyColor by animateColorAsState(paint,animationSpec=tween(260),label="paint")
    val currentRotate by rememberUpdatedState(onRotate)
    Canvas(modifier.aspectRatio(1.43f)
        .semantics { contentDescription="Vista tridimensional del vehículo. Desliza horizontalmente para girarlo." }
        .pointerInput(Unit) {
            detectHorizontalDragGestures { change,delta ->
                change.consume()
                currentRotate(delta / size.width.coerceAtLeast(1)*280f)
            }
        }) {
        val u=minOf(size.width/640f,size.height/448f)
        scale(u,u,pivot=Offset.Zero){
            studio()
            neonUnderCar(kit,Offset(320f,321f),230f,82f)
            renderer.draw(this,mesh,bodyColor,angle,640,448,320f,218f,112f)
        }
    }
}

@Composable
internal fun ShowroomThumbnail(style:String,paint:Color,modifier:Modifier=Modifier,kit:GarageKit=GarageKit()) {
    val mesh=remember(style,kit){buildCarMesh(style,kit)}
    val renderer=remember{ShowroomRaster(200,138)}
    Canvas(modifier.aspectRatio(1.45f)) {
        val u=minOf(size.width/200f,size.height/138f)
        scale(u,u,pivot=Offset.Zero){
            drawOval(Color(0x5510192A),Offset(20f,87f),Size(160f,32f))
            neonUnderCar(kit,Offset(100f,101f),76f,29f)
            renderer.draw(this,mesh,paint,38f,200,138,100f,64f,36f)
        }
    }
}

private fun DrawScope.studio(){
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1220),Color(0xFF20354C),Color(0xFF111C2B)),endY=448f),size=Size(640f,448f))
    drawRect(Brush.radialGradient(listOf(Color(0xFF405D7D),Color.Transparent),
        center=Offset(320f,194f),radius=300f),size=Size(640f,448f))
    // Structural wall panels, window grid, luminous steel frame.
    for(i in 0..5){
        val x=16f+i*104f
        drawRoundRect(Color(0xFF182637),Offset(x,16f),Size(100f,216f),CornerRadius(3f))
        drawLine(Color(0xFF40546A),Offset(x+1f,22f),Offset(x+1f,212f),1.4f)
        for(j in 0..2)drawLine(Color(0xFF23394D),Offset(x+7f,49f+j*56f),Offset(x+93f,49f+j*56f),1.2f)
    }
    for(x in listOf(45f,594f)){
        drawRoundRect(Brush.linearGradient(listOf(Color.Transparent,Color(0xFF93C5FD).copy(alpha=.18f),Color.Transparent),
            start=Offset(x-21f,35f),end=Offset(x+21f,35f)),Offset(x-22f,30f),Size(44f,179f),CornerRadius(16f))
        drawLine(studioBlue,Offset(x,36f),Offset(x,205f),3.6f,StrokeCap.Round)
    }
    drawLine(Color(0xFFABCFF9).copy(alpha=.75f),Offset(115f,28f),Offset(525f,28f),3.5f,StrokeCap.Round)
    drawLine(Color(0xFF6CB7E5).copy(alpha=.45f),Offset(117f,34f),Offset(523f,34f),1.4f)
    // Vanishing-point floor and a raised circular display platform.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF24374A),Color(0xFF101B29)),startY=234f,endY=448f),
        Offset(0f,234f),Size(640f,214f))
    for(i in -7..7)drawLine(Color(0xFF7596AF).copy(alpha=.13f),Offset(320f+i*27f,234f),Offset(320f+i*119f,448f),1f)
    for(y in listOf(249f,269f,297f,335f,387f,438f))drawLine(Color(0xFF7596AF).copy(alpha=.13f),Offset(0f,y),Offset(640f,y),1f)
    drawOval(Color(0xAA030812),Offset(35f,285f),Size(570f,130f))
    drawOval(Color(0xFF0B1522),Offset(44f,278f),Size(552f,124f))
    drawOval(Brush.verticalGradient(listOf(Color(0xFF526A82),Color(0xFF25374A)),startY=260f,endY=399f),
        Offset(44f,259f),Size(552f,130f))
    drawOval(Color(0xFF90C7FC).copy(alpha=.82f),Offset(47f,263f),Size(546f,126f),style=Stroke(2.6f))
    drawOval(Color(0xFF9CB8D0).copy(alpha=.31f),Offset(64f,271f),Size(512f,106f),style=Stroke(1f))
    drawOval(Brush.radialGradient(listOf(Color(0xB8040A14),Color(0x00040A14)),center=Offset(320f,322f),radius=239f),
        Offset(78f,275f),Size(484f,101f))
    // Subtle floor illumination in front of the platform.
    drawLine(studioBlue.copy(alpha=.4f),Offset(105f,404f),Offset(535f,404f),1f)
}
internal fun surfaceColor(face:CarFace,base:Color):Color{
    val n=face.normal
    val key=n.dot(studioLight).coerceAtLeast(0f)
    val fill=n.dot(coolLight).coerceAtLeast(0f)
    return when(face.finish){
        Finish.PAINT -> {
            val shade=lerp(base,Color(0xFF071323),(.48f-key*.35f-fill*.11f).coerceIn(.07f,.48f))
            lerp(shade,Color(0xFFE4F2FF),(key*key*.21f+fill*.055f).coerceIn(0f,.30f))
        }
        Finish.GLASS -> lerp(Color(0xFF101D2B),Color(0xFF93BCD6),(.16f+key*.39f+fill*.16f).coerceIn(0f,.75f))
        Finish.RUBBER -> lerp(Color(0xFF080F17),Color(0xFF414D58),key*.37f+fill*.16f)
        Finish.RIM -> lerp(Color(0xFF758492),Color(0xFFF1F5F9),.34f+key*.44f)
        Finish.GRILLE -> lerp(Color(0xFF0A1420),Color(0xFF354454),key*.30f)
        Finish.HEADLAMP -> lerp(Color(0xFFADCFE7),Color(0xFFFFFEED),.74f)
        Finish.TAILLAMP -> lerp(Color(0xFF95203A),Color(0xFFFF7C7C),.35f+key*.35f)
        Finish.TRIM -> lerp(Color(0xFF101A26),Color(0xFF526274),key*.42f)
        Finish.DECAL_LIGHT -> lerp(Color(0xFFA5B6C6),Color.White,.42f+key*.48f)
        Finish.DECAL_DARK -> lerp(Color(0xFF101820),Color(0xFF4A5663),key*.65f)
        Finish.RIM_BLACK -> lerp(Color(0xFF0B121B),Color(0xFF586574),.14f+key*.45f)
        Finish.RIM_GOLD -> lerp(Color(0xFF795020),Color(0xFFFFD887),.38f+key*.48f)
        Finish.CHROME -> lerp(Color(0xFF8FA7BA),Color(0xFFF8FAFC),.40f+key*.48f)
    }
}
/** Reuse the pixel/depth buffers and one bitmap. Redraw only on angle, paint or mesh change. */
private class ShowroomRaster(private val width:Int,private val height:Int){
    private val raster=CarDepthRaster(width,height)
    private val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
    private val image=bitmap.asImageBitmap()
    private var previousMesh:CarMesh?=null
    private var previousPaint:Color?=null
    private var previousAngle=Float.NaN
    fun draw(scope:DrawScope,mesh:CarMesh,paint:Color,angle:Float,w:Int,h:Int,x:Float,y:Float,unit:Float){
        if(previousMesh!==mesh||previousPaint!=paint||previousAngle!=angle){
            val sx=width.toFloat()/w
            val sy=height.toFloat()/h
            raster.render(mesh,CarCamera(angle),x*sx,y*sy,unit*sx,unit*sy){surfaceColor(it,paint).toArgb()}
            bitmap.setPixels(raster.pixels,0,width,0,0,width,height)
            previousMesh=mesh;previousPaint=paint;previousAngle=angle
        }
        with(scope){drawImage(image,dstSize=IntSize(w,h))}
    }
}

private fun DrawScope.neonUnderCar(kit:GarageKit,at:Offset,rx:Float,ry:Float){
    val glow=kit.neonColor() ?: return
    for(i in 4 downTo 1){
        val f=i/4f
        drawOval(glow.copy(alpha=.10f+(1f-f)*.13f),at-Offset(rx*f,ry*f),Size(rx*f*2f,ry*f*2f))
    }
}
