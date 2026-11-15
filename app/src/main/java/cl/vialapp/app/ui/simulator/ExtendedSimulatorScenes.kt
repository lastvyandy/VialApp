package cl.vialapp.app.ui.simulator

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class ExtendedPose(
    val driverX: Float, val driverY: Float, val heading: Float,
    val otherX: Float, val otherY: Float, val otherHeading: Float
)

private fun interpolate(a: Float,b: Float,t: Float) = a+(b-a)*t.coerceIn(0f,1f)
private fun route(p: Float, a: Float,b: Float,c: Float,d: Float): Float = when {
    p < 1f -> interpolate(a,b,p)
    p < 2f -> interpolate(b,c,p-1f)
    else -> interpolate(c,d,p-2f)
}
private fun circlePoint(angle: Float): Offset {
    val theta = angle * PI / 180.0
    return Offset(180f+82f*cos(theta).toFloat(),180f+82f*sin(theta).toFloat())
}

internal fun extendedPose(scene: String, progress: Float): ExtendedPose {
    val p=progress.coerceIn(0f,3f)
    return when(scene) {
        "rain" -> ExtendedPose(211f,route(p,320f,294f,273f,224f),0f,
            211f,route(p,185f,140f,82f,22f),0f)
        "cyclist" -> ExtendedPose(route(p,211f,211f,150f,211f),
            route(p,326f,292f,105f,25f),route(p,0f,0f,0f,0f),
            150f,route(p,72f,395f,435f,475f),180f)
        else -> {
            val otherAngle=route(p,145f,15f,-85f,-175f)
            val other=circlePoint(otherAngle)
            val entry=circlePoint(65f)
            val at=when {
                p<1f -> Offset(215f,interpolate(330f,323f,p))
                p<1.45f -> Offset(interpolate(215f,entry.x,(p-1f)/.45f),
                    interpolate(323f,entry.y,(p-1f)/.45f))
                p<2f -> circlePoint(interpolate(65f,23f,(p-1.45f)/.55f))
                else -> {
                    val exit=circlePoint(23f)
                    Offset(interpolate(exit.x,345f,p-2f),interpolate(exit.y,215f,p-2f))
                }
            }
            val heading=when {
                p<1f -> 0f
                p<1.45f -> interpolate(0f,65f,(p-1f)/.45f)
                p<2f -> interpolate(65f,23f,(p-1.45f)/.55f)
                else -> interpolate(23f,90f,p-2f)
            }
            ExtendedPose(at.x,at.y,heading,other.x,other.y,otherAngle)
        }
    }
}

internal fun DrawScope.drawExtendedScene(scene:String, progress:Float, warning:Boolean) {
    val p=progress.coerceIn(0f,3f)
    val pose=extendedPose(scene,p)
    val wet=scene=="rain"
    val grass=if(wet) Color(0xFF718F83) else Color(0xFF93B480)
    val road=if(wet) Color(0xFF40596C) else Color(0xFF515A64)
    val curb=Color(0xFFD1D6CA)
    val blue=Color(0xFF3985DF)
    val red=Color(0xFFD35D4F)
    drawRect(grass,Offset.Zero,Size(360f,360f))
    if(scene=="roundabout") {
        // Single lane roundabout, left-hand island, right-hand approach and exit.
        drawRect(curb,Offset(114f,0f),Size(132f,360f))
        drawRect(curb,Offset(0f,114f),Size(360f,132f))
        drawCircle(curb,120f,Offset(180f,180f))
        drawRect(road,Offset(126f,0f),Size(108f,360f))
        drawRect(road,Offset(0f,126f),Size(360f,108f))
        drawCircle(road,108f,Offset(180f,180f))
        drawCircle(curb,57f,Offset(180f,180f))
        drawCircle(grass,51f,Offset(180f,180f))
        tree(Offset(168f,170f),14f)
        tree(Offset(199f,194f),13f)
        for(i in 0..4) {
            val t=i*22f
            drawLine(Color(0xFFEDE4BA),Offset(180f,t),Offset(180f,t+12f),2.5f)
            drawLine(Color(0xFFEDE4BA),Offset(t,180f),Offset(t+12f,180f),2.5f)
            drawLine(Color(0xFFEDE4BA),Offset(180f,360f-t),Offset(180f,348f-t),2.5f)
            drawLine(Color(0xFFEDE4BA),Offset(360f-t,180f),Offset(348f-t,180f),2.5f)
        }
        for(i in 0..4) drawRect(Color.White,Offset(182f+i*10f,291f),Size(6f,4f))
        val center=Offset(270f,291f)
        val triangle=Path().apply {moveTo(center.x-19f,center.y-16f);lineTo(center.x+19f,center.y-16f);lineTo(center.x,center.y+17f);close()}
        drawLine(Color(0xFF53636C),center,center+Offset(0f,29f),4f)
        drawPath(triangle,Color.White)
        drawPath(triangle,Color(0xFFD23B43),style=Stroke(5f))
        car(Offset(pose.otherX,pose.otherY),red,pose.otherHeading,false)
    } else {
        drawRect(curb,Offset(105f,0f),Size(150f,360f))
        drawRect(road,Offset(121f,0f),Size(118f,360f))
        drawLine(Color(0xFFDFE5E9),Offset(125f,0f),Offset(125f,360f),2f)
        drawLine(Color(0xFFDFE5E9),Offset(235f,0f),Offset(235f,360f),2f)
        for(i in 0..15) drawLine(Color(0xFFE8D6A0),Offset(180f,i*24f),Offset(180f,i*24f+13f),2.5f)
        if(wet) {
            for(i in 0..7) {
                val x=if(i%2==0) 133f else 214f
                drawOval(Color(0x335CC7E9),Offset(x,21f+i*43f),Size(18f,13f))
            }
            drawLine(Color(0x995EEAD4),Offset(pose.driverX,pose.driverY-33f),
                Offset(pose.otherX,pose.otherY+33f),2.5f,StrokeCap.Round)
        }
        if(scene=="cyclist") {
            val bikeY=route(p,218f,203f,170f,138f)
            val bike=Offset(226f,bikeY)
            for(y in listOf(-9f,9f)) drawOval(Color(0xFF1E293B),bike+Offset(-3f,y-5f),Size(6f,10f))
            drawLine(Color(0xFFF0BE4A),bike-Offset(0f,9f),bike+Offset(0f,9f),3f)
            drawCircle(Color(0xFFEAC39A),4f,bike-Offset(0f,3f))
            drawRoundRect(Color(0xFF32A895),bike-Offset(5f,0f),Size(10f,9f),CornerRadius(3f))
            if(p in 1.55f..2.2f) {
                drawLine(Color(0xFF95F4CB),Offset(pose.driverX+15f,bikeY),Offset(220f,bikeY),2f)
            }
        }
        car(Offset(pose.otherX,pose.otherY),red,pose.otherHeading,wet)
    }
    listOf(Offset(28f,41f),Offset(77f,94f),Offset(313f,43f),Offset(284f,95f),
        Offset(36f,307f),Offset(310f,312f),Offset(74f,276f)).forEach { at ->
        if(scene!="roundabout" || ((at.x<100f || at.x>260f)&&(at.y<100f || at.y>260f))) tree(at,16f)
    }
    car(Offset(pose.driverX,pose.driverY),blue,pose.heading,p<1.1f)
    if(wet) {
        drawRect(Color(0x224B7D9E),Offset.Zero,Size(360f,360f))
        for(i in 0..47) {
            val x=(i*67f+p*33f)%370f
            val y=(i*103f+p*83f)%370f
            drawLine(Color(0xA0BBDDEA),Offset(x,y),Offset(x-6f,y+15f),1.5f,StrokeCap.Round)
        }
    }
    if(warning) drawRoundRect(Color(0xFFF15B52),Offset(5f,5f),Size(350f,350f),CornerRadius(22f),style=Stroke(5f))
}

private fun DrawScope.tree(at:Offset,r:Float) {
    drawCircle(Color(0x35415C44),r+3f,at+Offset(3f,5f))
    drawCircle(Color(0xFF4C7D56),r,at)
    drawCircle(Color(0xFF74A56F),r*.62f,at-Offset(4f,4f))
}
