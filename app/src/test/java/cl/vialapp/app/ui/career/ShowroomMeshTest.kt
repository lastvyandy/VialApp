package cl.vialapp.app.ui.career

import org.junit.Assert.*
import org.junit.Test

class ShowroomMeshTest {
    @Test fun modelsHaveShadedBodyWindowsLampsAndRimsAtEveryAngle() {
        val styles=listOf("compact","hatch","sedan","suv","sport")
        val specs=styles.map(::carSpec)
        assertEquals(5,specs.distinct().size)
        for(style in styles){
            val mesh=buildCarMesh(style)
            assertTrue(mesh.faces.size>200)
            assertTrue(mesh.faces.any{it.finish==Finish.GLASS})
            assertTrue(mesh.faces.any{it.finish==Finish.HEADLAMP})
            assertTrue(mesh.faces.any{it.finish==Finish.TAILLAMP})
            assertTrue(mesh.faces.any{it.finish==Finish.RIM})
            for(angle in 0 until 360 step 15){
                val camera=CarCamera(angle.toFloat())
                val visible=mesh.faces.filter(camera::visible)
                assertTrue(visible.any{it.finish==Finish.PAINT})
                assertTrue(visible.any{it.finish==Finish.GLASS})
                for(f in visible) for(p in f.points.map(camera::project))
                    assertTrue(p.x.isFinite() && p.y.isFinite() && p.depth.isFinite())
            }
        }
    }

    @Test fun accessoryPartsChangeGeometryAndMaterialsWithoutChangingTheBaseModel() {
        for(style in listOf("compact","hatch","sedan","suv","sport")){
            val original=buildCarMesh(style)
            val custom=buildCarMesh(style,GarageKit(wheels="wheels_gold",decal="decal_rally",wing="wing_gt"))
            assertTrue(custom.faces.size>original.faces.size)
            assertTrue(custom.faces.any{it.finish==Finish.RIM_GOLD})
            assertTrue(custom.faces.any{it.finish==Finish.DECAL_LIGHT})
            assertFalse(original.faces.any{it.finish==Finish.DECAL_LIGHT})
            val camera=CarCamera(180f,78f)
            assertTrue(custom.faces.filter(camera::visible).any{it.finish==Finish.DECAL_LIGHT})
            for(face in custom.faces) for(point in face.points.map(camera::project))
                assertTrue(point.x.isFinite() && point.y.isFinite() && point.depth.isFinite())
        }
    }
}
