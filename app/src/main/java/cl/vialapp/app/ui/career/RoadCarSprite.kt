package cl.vialapp.app.ui.career

import android.graphics.Bitmap
import android.graphics.Canvas as NativeCanvas
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Render the actual showroom mesh once for each model/paint/kit. Gameplay only draws bitmaps. */
private data class RoadSpriteKey(val style:String,val argb:Int,val kit:GarageKit)
private val roadSpriteCache=android.util.LruCache<RoadSpriteKey,ImageBitmap>(16)

@Synchronized
internal fun rasterizeRoadCar(style:String,color:Color,kit:GarageKit):ImageBitmap {
    val key=RoadSpriteKey(style,color.toArgb(),kit)
    roadSpriteCache.get(key)?.let { return it }
    return renderRoadCar(style,color,kit).also { roadSpriteCache.put(key,it) }
}

private fun renderRoadCar(style:String,color:Color,kit:GarageKit):ImageBitmap {
    val mesh=buildCarMesh(style,kit)
    val camera=CarCamera(180f,78f)
    val faces=mesh.faces.filter(camera::visible).map{it to it.points.map(camera::project)}
        .sortedBy{(_,points)->points.map{it.depth}.average()}
    val all=faces.flatMap{it.second}
    val minX=all.minOf{it.x};val maxX=all.maxOf{it.x}
    val minY=all.minOf{it.y};val maxY=all.maxOf{it.y}
    val bitmap=Bitmap.createBitmap(224,304,Bitmap.Config.ARGB_8888)
    val canvas=NativeCanvas(bitmap)
    // Normalize to the road's existing footprint instead of creating unfairly big/small cars.
    val sx=196f/(maxX-minX).coerceAtLeast(.01f)
    val sy=280f/(maxY-minY).coerceAtLeast(.01f)
    val paint=NativePaint(NativePaint.ANTI_ALIAS_FLAG)
    for((face,points) in faces){
        val path=NativePath()
        val xy=points.map{Offset(14f+(it.x-minX)*sx,12f+(it.y-minY)*sy)}
        path.moveTo(xy.first().x,xy.first().y)
        for(p in xy.drop(1))path.lineTo(p.x,p.y)
        path.close()
        val shade=surfaceColor(face,color)
        val top=xy.minOf{it.y};val bottom=xy.maxOf{it.y}
        paint.shader=null
        paint.color=shade.toArgb()
        // A tiny same-color stroke closes antialias seams on adjoining body panels.
        paint.style=NativePaint.Style.FILL_AND_STROKE
        paint.strokeWidth=.65f
        if(face.finish in listOf(Finish.PAINT,Finish.GLASS,Finish.DECAL_LIGHT,Finish.DECAL_DARK) && bottom-top>2f){
            paint.shader=LinearGradient(0f,top,0f,bottom,intArrayOf(
                lerp(shade,Color.White,.10f).toArgb(),shade.toArgb(),lerp(shade,Color(0xFF102032),.13f).toArgb()),
                null,Shader.TileMode.CLAMP)
        }
        canvas.drawPath(path,paint)
    }
    return bitmap.asImageBitmap()
}

internal fun DrawScope.drawRoadSprite(sprite:ImageBitmap,at:Offset,opacity:Float,
    neon:Color?=null,braking:Boolean=false){
    // Absolute dimensions match the old collision envelope; sprite has transparent margins.
    drawOval(Color(0xFF07111D).copy(alpha=.40f*opacity),at-Offset(53f,65f)+Offset(4f,9f),Size(108f,143f))
    if(neon!=null) for(i in 4 downTo 1){
        val f=i/4f
        val w=120f+(i*7f);val h=152f+(i*8f)
        drawOval(neon.copy(alpha=(.10f+(1f-f)*.12f)*opacity),at-Offset(w/2f,h/2f),Size(w,h))
    }
    if(braking) for(dx in listOf(-32f,32f)){
        drawOval(Color(0xFFFF3947).copy(alpha=.28f*opacity),at+Offset(dx-11f,50f),Size(22f,29f))
    }
    drawImage(sprite,dstOffset=IntOffset((at.x-57f).roundToInt(),(at.y-78f).roundToInt()),dstSize=IntSize(114,156),alpha=opacity)
}
