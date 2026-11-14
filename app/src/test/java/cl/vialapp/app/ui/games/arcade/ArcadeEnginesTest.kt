package cl.vialapp.app.ui.games.arcade

import org.junit.Assert.*
import org.junit.Test

class ArcadeEnginesTest {
    @Test fun drivingClampsLaneAndMovesSmoothly() {
        val engine = DriveEngine(1)
        repeat(5) { engine.act(0) }
        assertEquals(0, engine.snapshot().lane)
        repeat(10) { engine.step(1f / 30f) }
        assertEquals(0f, engine.snapshot().carX, .01f)
        repeat(5) { engine.act(1) }
        assertEquals(2, engine.snapshot().lane)
    }

    @Test fun combatCorrectDefenseOpensAttackAndWrongDefenseCostsHealth() {
        val engine = FightEngine(2)
        val start = engine.snapshot()
        engine.act(start.hazard.response)
        assertEquals(FightPhase.OPENING, engine.snapshot().phase)
        engine.act(3)
        assertEquals(80, engine.snapshot().bossHp)
        engine.act(3)
        assertEquals(80, engine.snapshot().bossHp)
        repeat(110) { engine.step(1f / 30f) }
        val next = engine.snapshot()
        assertEquals(FightPhase.TELEGRAPH, next.phase)
        engine.act((next.hazard.response + 1) % 3)
        assertEquals(85, engine.snapshot().hp)
    }

    @Test fun workshopRequiresCorrectPartAndToolAndCanFinish() {
        val engine = WorkshopEngine(3)
        val first = engine.snapshot().fault!!
        engine.act(10 + first.ordinal)
        assertEquals(0, engine.snapshot().completed)
        engine.act((first.ordinal + 1) % 4)
        engine.act(10 + first.ordinal)
        assertEquals(2, engine.snapshot().lives)
        repeat(4) {
            val part = engine.snapshot().fault!!
            engine.act(part.ordinal)
            engine.act(10 + part.ordinal)
            assertTrue(engine.snapshot().checking)
            engine.act(20 + part.correct)
            repeat(40) { engine.step(1f / 30f) }
        }
        assertTrue(engine.snapshot().won)
        assertEquals(1600, engine.snapshot().score)
    }
}
