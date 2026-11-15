package cl.vialapp.app.ui.games.arcade

import org.junit.Assert.*
import org.junit.Test

class ArenaCombatEngineTest {
    @Test fun distantPunchDoesNotHitAndClosePunchHitsOnce(){
        val e=ArenaCombatEngine()
        e.act(2)
        repeat(11){e.step(1f/30f)}
        assertEquals(85,e.snapshot().rivalHp)
        repeat(5){e.step(1f/30f)}
        repeat(3){e.act(1)}
        e.act(2)
        repeat(11){e.step(1f/30f)}
        assertTrue(e.snapshot().rivalHp<85)
        val hp=e.snapshot().rivalHp
        repeat(8){e.step(1f/30f)}
        assertEquals(hp,e.snapshot().rivalHp)
    }
    @Test fun dodgeAndGuardAreTimedAndNotPermanent(){
        val e=ArenaCombatEngine()
        e.act(5)
        val x=e.snapshot().playerX
        e.act(5)
        assertEquals(x,e.snapshot().playerX,.001f)
        repeat(40){e.step(1f/30f)}
        assertEquals(0f,e.snapshot().evade,.001f)
        e.act(4)
        assertTrue(e.snapshot().guard>0f)
        repeat(30){e.step(1f/30f)}
        assertEquals(0f,e.snapshot().guard,.001f)
    }
    @Test fun actionCombatCanFinishAllThreeRivals(){
        val e=ArenaCombatEngine()
        repeat(6000){
            val f=e.snapshot()
            if(f.finished)return@repeat
            when{
                f.phase==ArenaPhase.ROUND_CLEAR->Unit
                f.phase==ArenaPhase.WINDUP&&f.phaseTime<.27f&&f.evadeLock<=0f->e.act(5)
                f.lock<=0f&&f.distance>.27f->e.act(1)
                f.lock<=0f&&f.phase!=ArenaPhase.WINDUP->e.act(if(f.power>=100)6 else 3)
                f.phase==ArenaPhase.WINDUP&&f.phaseTime<.50f&&f.attack!=RivalAttack.CHARGE&&f.lock<=0f->e.act(4)
            }
            e.step(1f/30f)
        }
        assertTrue(e.snapshot().detail,e.snapshot().won)
    }
}
