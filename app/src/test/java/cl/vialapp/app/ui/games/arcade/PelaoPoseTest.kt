package cl.vialapp.app.ui.games.arcade

import org.junit.Assert.*
import org.junit.Test

class PelaoPoseTest {
    @Test fun defenseAttackAndIdleHaveDistinctPoses() {
        val engine = FightEngine(2)
        assertEquals(PelaoPose.IDLE, engine.snapshot().pose)
        engine.act(engine.snapshot().hazard.response)
        assertEquals(PelaoPose.BLOCK, engine.snapshot().pose)
        engine.act(3)
        assertEquals(PelaoPose.ATTACK_ONE, engine.snapshot().pose)
        repeat(14) { engine.step(1f / 30f) }
        assertEquals(PelaoPose.IDLE, engine.snapshot().pose)
        engine.act(3)
        assertEquals(PelaoPose.ATTACK_TWO, engine.snapshot().pose)
    }

    @Test fun incorrectResponseAndTimeoutShowDamageNotAttack() {
        val engine = FightEngine(7)
        engine.act((engine.snapshot().hazard.response + 1) % 3)
        assertEquals(PelaoPose.HIT, engine.snapshot().pose)
        assertEquals(85, engine.snapshot().hp)
        val timeout = FightEngine(8)
        repeat(185) { timeout.step(1f / 30f) }
        assertEquals(PelaoPose.HIT, timeout.snapshot().pose)
        assertEquals(80, timeout.snapshot().hp)
    }

    @Test fun terminalResultAlwaysOverridesTemporaryPose() {
        assertEquals(PelaoPose.VICTORY, FightFrame(boss = 4, actionPose = PelaoPose.ATTACK_ONE, poseTime = .3f).pose)
        assertEquals(PelaoPose.DEFEAT, FightFrame(hp = 0, actionPose = PelaoPose.HIT, poseTime = .8f).pose)
        assertEquals(PelaoPose.DEFEAT, FightFrame(elapsed = FightEngine.TIME).pose)
    }
}
