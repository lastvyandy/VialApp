package cl.vialapp.app.ui.simulator

// QUÉ PROTEGE ESTA PRUEBA:
// Valida la representación del simulador en distintas situaciones.

import org.junit.Assert.*
import org.junit.Test

class SimulatorPoseTest {
    @Test fun driverStopsWhileOtherRoadUserCrosses() {
        for (scene in listOf("stop","signal","pedestrian")) {
            val stopped = roadPose(scene,1f)
            assertEquals(stopped.driverY,roadPose(scene,1.5f).driverY,0.01f)
            assertEquals(stopped.driverY,roadPose(scene,2f).driverY,0.01f)
            assertTrue(roadPose(scene,2f).actorX > stopped.actorX)
            assertTrue(roadPose(scene,3f).driverY < stopped.driverY)
        }
    }
    @Test fun signalChangesOnlyAfterFirstDecision() {
        assertFalse(roadPose("signal",0.99f).green)
        assertTrue(roadPose("signal",1f).green)
    }
}
