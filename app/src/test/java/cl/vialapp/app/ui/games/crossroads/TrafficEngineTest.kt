package cl.vialapp.app.ui.games.crossroads

import org.junit.Assert.*
import org.junit.Test

class TrafficEngineTest {
    @Test fun carsNeverEnterCrossingAgainstOtherAxis() {
        val engine = TrafficEngine(17)
        repeat(2250) { tick ->
            if (tick % 150 == 0) engine.requestGreen(if (tick % 300 == 0) TrafficAxis.HORIZONTAL else TrafficAxis.VERTICAL)
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
}
