package cl.vialapp.app.ui.career

import kotlin.math.ceil
import kotlin.math.floor

/**
 * A small software depth buffer shared by the garage and driving sprites.
 * Camera-space inverse distance is affine after perspective projection.
 * Resolve every pixel against the nearest surface, never average face depth.
 */
internal class CarDepthRaster(val width:Int,val height:Int) {
    val pixels=IntArray(width*height)
    private val depth=FloatArray(width*height)
    private data class P(val x:Float,val y:Float,val invZ:Float)
    fun render(mesh:CarMesh,camera:CarCamera,originX:Float,originY:Float,scaleX:Float,scaleY:Float=scaleX,
               color:(CarFace)->Int){
        pixels.fill(0)
        depth.fill(Float.NEGATIVE_INFINITY)
        for(face in mesh.faces){
            // Do not discard back faces: non-planar wheel arch panels share edges;
            // the z buffer chooses the nearest surface even at grazing angles.
            val ps=face.points.map{
                val p=camera.project(it)
                P(originX+p.x*scaleX,originY+p.y*scaleY,1f/(9f-p.depth).coerceAtLeast(2f))
            }
            val argb=color(face)
            for(i in 1 until ps.lastIndex) triangle(ps[0],ps[i],ps[i+1],argb)
        }
    }
    private fun triangle(a:P,b:P,c:P,argb:Int){
        val x0=floor(minOf(a.x,b.x,c.x)).toInt().coerceAtLeast(0)
        val x1=ceil(maxOf(a.x,b.x,c.x)).toInt().coerceAtMost(width-1)
        val y0=floor(minOf(a.y,b.y,c.y)).toInt().coerceAtLeast(0)
        val y1=ceil(maxOf(a.y,b.y,c.y)).toInt().coerceAtMost(height-1)
        if(x1<x0||y1<y0)return
        val area=(b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x)
        if(kotlin.math.abs(area)<.00001f)return
        val uDx=(b.y-c.y)/area
        val uDy=(c.x-b.x)/area
        val vDx=(c.y-a.y)/area
        val vDy=(a.x-c.x)/area
        val wDx=-uDx-vDx
        val zDx=a.invZ*uDx+b.invZ*vDx+c.invZ*wDx
        val px=x0+.5f-c.x
        val py=y0+.5f-c.y
        var rowU=px*uDx+py*uDy
        var rowV=px*vDx+py*vDy
        for(y in y0..y1){
            var u=rowU
            var v=rowV
            var z=a.invZ*u+b.invZ*v+c.invZ*(1f-u-v)
            var index=y*width+x0
            for(x in x0..x1){
                if(u>=-.00002f&&v>=-.00002f&&u+v<=1.00002f&&z>depth[index]){
                    depth[index]=z
                    pixels[index]=argb
                }
                u+=uDx;v+=vDx;z+=zDx;index++
            }
            rowU+=uDy;rowV+=vDy
        }
    }
}
