package cl.vialapp.app.ui.games.crossroads

// QUÉ PROTEGE ESTA PRUEBA:
// Prueba los cruces y colisiones del tránsito, no solo si los autos se dibujan.

import org.junit.Assert.*
import org.junit.Test

class TrafficEngineTest {
    @Test fun carsNeverEnterCrossingAgainstOtherAxis() {
        val engine = TrafficEngine(17)
        repeat(2250) { tick ->
            if (tick % 150 == 0) engine.requestGreen(if (tick % 300 == 0) TrafficAxis.HORIZONTAL else TrafficAxis.VERTICAL)
            if (engine.snapshot().pedestrianWaiting) engine.requestPedestrian()
            val frame = engine.step(1f / 30f)
            val crossing = frame.cars.filter { it.progress > TrafficEngine.STOP + .0001f && it.progress < TrafficEngine.CLEAR }
            assertTrue(crossing.map { it.lane.axis }.distinct().size <= 1)
            if (frame.green != null) assertTrue(crossing.all { it.lane.axis == frame.green })
        }
    }
    @Test fun alternatingSignalsMovesTrafficAndScores() {
        val engine = TrafficEngine(42)
        repeat(2252) { tick ->
            if (tick % 210 == 0) engine.requestGreen(if ((tick / 210) % 2 == 0) TrafficAxis.VERTICAL else TrafficAxis.HORIZONTAL)
            if (engine.snapshot().pedestrianWaiting) engine.requestPedestrian()
            engine.step(1f / 30f)
        }
        val result = engine.snapshot()
        assertTrue(result.passed > 10)
        assertTrue(result.score >= result.passed * 100)
        assertTrue(result.finished)
    }
    @Test fun neglectingOneDirectionLosesLives() {
        val engine = TrafficEngine(7)
        repeat(2252) { engine.step(1f / 30f) }
        assertTrue(engine.snapshot().lives < 3)
    }
    @Test fun pedestrianPhaseNeverAllowsVehicleGreen() {
        val engine = TrafficEngine(3)
        repeat(380) { engine.step(1f / 30f) }
        assertTrue(engine.snapshot().pedestrianWaiting)
        engine.requestPedestrian()
        var observed = false
        repeat(200) {
            engine.requestGreen(TrafficAxis.HORIZONTAL)
            val frame = engine.step(1f / 30f)
            if (frame.pedestrianCrossing > 0f) {
                observed = true
                assertNull(frame.green)
            }
        }
        assertTrue(observed)
        assertEquals(1, engine.snapshot().pedestrianServed)
    }
}
