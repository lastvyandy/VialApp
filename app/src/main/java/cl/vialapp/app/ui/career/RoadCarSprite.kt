package cl.vialapp.app.ui.career

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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
    val all=mesh.faces.flatMap{it.points.map(camera::project)}
    val minX=all.minOf{it.x};val maxX=all.maxOf{it.x}
    val minY=all.minOf{it.y};val maxY=all.maxOf{it.y}
    val width=224;val height=304
    val raster=CarDepthRaster(width,height)
    val sx=196f/(maxX-minX).coerceAtLeast(.01f)
    val sy=280f/(maxY-minY).coerceAtLeast(.01f)
    raster.render(mesh,camera,14f-minX*sx,12f-minY*sy,sx,sy){surfaceColor(it,color).toArgb()}
    val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
    bitmap.setPixels(raster.pixels,0,width,0,0,width,height)
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
