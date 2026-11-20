package cl.vialapp.app.ui.simulator

// QUÉ PROTEGE ESTA PRUEBA:
// Valida poses y posiciones de los escenarios adicionales.

import org.junit.Assert.*
import org.junit.Test

class ExtendedScenarioPoseTest {
    @Test fun rainMaintainsIncreasingFollowingDistance() {
        val start = extendedPose("rain",0f)
        val end = extendedPose("rain",2f)
        assertTrue(end.driverY-end.otherY > start.driverY-start.otherY)
        assertEquals(start.driverX,end.driverX,0.01f)
    }
    @Test fun cyclistWaitsForOncomingCarThenPassesAndReturns() {
        val waiting=extendedPose("cyclist",1f)
        val passing=extendedPose("cyclist",2f)
        val end=extendedPose("cyclist",3f)
        assertTrue(waiting.otherY>360f)
        assertTrue(passing.driverX<waiting.driverX)
        assertEquals(waiting.driverX,end.driverX,0.01f)
    }
    @Test fun roundaboutEntersAfterYieldAndUsesRightExit() {
        val waiting=extendedPose("roundabout",1f)
        val inside=extendedPose("roundabout",2f)
        val end=extendedPose("roundabout",3f)
        assertTrue(waiting.driverY-29f>291f)
        assertTrue(inside.driverY<waiting.driverY)
        assertTrue(end.driverX>inside.driverX)
    }
}
