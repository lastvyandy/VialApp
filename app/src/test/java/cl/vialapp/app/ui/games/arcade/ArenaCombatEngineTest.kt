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
                f.phase==ArenaPhase.KNOWLEDGE->if(f.selected<0)e.act(20+f.question!!.correct)else e.act(23)
                f.phase==ArenaPhase.ROUND_CLEAR->Unit
                f.phase==ArenaPhase.WINDUP&&f.phaseTime<.27f&&f.evadeLock<=0f->e.act(5)
                f.lock<=0f&&f.distance>.27f->e.act(1)
                f.lock<=0f&&f.phase!=ArenaPhase.WINDUP->e.act(if(f.power>=100)6 else 3)
                f.phase==ArenaPhase.WINDUP&&f.phaseTime<.50f&&f.attack!=RivalAttack.CHARGE&&f.lock<=0f->e.act(4)
            }
            e.step(1f/30f)
        }
        assertTrue(e.snapshot().detail,e.snapshot().won)
        assertEquals(6,e.snapshot().knowledgeCorrect)
        assertEquals(136,e.snapshot().shieldDamage)
    }
    private fun reachShield():ArenaCombatEngine{
        val e=ArenaCombatEngine(2)
        repeat(250){
            val f=e.snapshot()
            if(f.phase==ArenaPhase.KNOWLEDGE)return e
            if(f.lock<=0f){
                if(f.distance>.25f)e.act(1) else e.act(3)
            }
            e.step(1f/30f)
        }
        error("Shield not reached")
    }
    @Test fun shieldPausesClockAndRequiresKnowledge(){
        val e=reachShield()
        val before=e.snapshot()
        assertEquals(68,before.rivalHp)
        repeat(120){e.step(1f/30f)}
        e.act(3)
        assertEquals(before,e.snapshot())
        val q=before.question!!
        e.act(20+(q.correct+1)%3)
        assertEquals(before.hp-8,e.snapshot().hp)
        assertEquals(0,e.snapshot().shieldsBroken)
        e.act(20+q.correct)
        assertEquals(0,e.snapshot().shieldsBroken)
        e.act(23)
        val retry=e.snapshot()
        e.act(20+retry.question!!.correct)
        assertEquals(51,e.snapshot().rivalHp)
        assertEquals(1,e.snapshot().shieldsBroken)
        assertEquals(17,e.snapshot().shieldDamage)
        e.act(20+retry.question!!.correct)
        assertEquals(51,e.snapshot().rivalHp)
        e.act(23)
        assertEquals(ArenaPhase.RECOVER,e.snapshot().phase)
    }
}
