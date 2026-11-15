package cl.vialapp.app.ui.career

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class CarDepthRasterTest {
    @Test fun nearerFaceWinsRegardlessOfSubmissionOrder(){
        fun square(z:Float,finish:Finish)=CarFace(listOf(V3(-1f,-.28f,z),V3(1f,-.28f,z),
            V3(1f,1.72f,z),V3(-1f,1.72f,z)),finish,V3(0f,0f,1f),V3(0f,.72f,z))
        val far=square(-1f,Finish.PAINT);val near=square(1f,Finish.HEADLAMP)
        val raster=CarDepthRaster(100,100)
        for(list in listOf(listOf(far,near),listOf(near,far))){
            raster.render(CarMesh(list),CarCamera(0f,0f),50f,50f,25f){it.finish.ordinal+1}
            assertEquals(Finish.HEADLAMP.ordinal+1,raster.pixels[50*100+50])
        }
    }
    @Test fun bothHeadlightsAndTaillightsHaveVisiblePixels(){
        for(style in listOf("compact","hatch","sedan","suv","sport")){
            val mesh=buildCarMesh(style)
            for((angle,finish) in listOf(0f to Finish.HEADLAMP,180f to Finish.TAILLAMP)){
                val raster=CarDepthRaster(320,220)
                raster.render(mesh,CarCamera(angle,10f),160f,110f,65f){it.finish.ordinal+1}
                val left=raster.pixels.indices.count{it%320<160&&raster.pixels[it]==finish.ordinal+1}
                val right=raster.pixels.indices.count{it%320>=160&&raster.pixels[it]==finish.ordinal+1}
                assertTrue("$style $finish: left=$left",left>10)
                assertTrue("$style $finish: right=$right",right>10)
            }
        }
    }
    @Test fun archClearsEveryTireAndWheelsStayInsideFenders(){
        for(style in listOf("compact","hatch","sedan","suv","sport")){
            val spec=carSpec(style)
            for(i in 0..180){
                val a=i*PI/180
                val z=spec.wheelZ+spec.tire*cos(a).toFloat()
                val upper=spec.tire+spec.tire*sin(a).toFloat()
                assertTrue("$style wheel arch",wheelArchY(spec,z)>upper+.015f)
            }
            val wheels=buildCarMesh(style).faces.filter{it.finish in listOf(Finish.RUBBER,Finish.RIM,Finish.RIM_GOLD,Finish.RIM_BLACK)}
            assertTrue(wheels.flatMap{it.points}.all{abs(it.x)<=spec.width+.001f})
        }
    }
}
