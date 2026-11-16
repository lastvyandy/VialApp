package cl.vialapp.app.ui.games.arcade

import org.junit.Assert.*
import org.junit.Test

class ArenaCombatEngineTest {
    private fun tick(e:ArenaCombatEngine,frames:Int){repeat(frames){e.step(1f/60f)}}
    private fun close(e:ArenaCombatEngine){repeat(6){e.act(1)}}
    @Test fun distantPunchMissesAndClosePunchHitsOnlyOnce(){
        val e=ArenaCombatEngine(1)
        e.act(2);tick(e,25)
        assertEquals(200,e.snapshot().rivalHp)
        close(e);e.act(2);tick(e,20)
        assertTrue(e.snapshot().rivalHp<200)
        val hp=e.snapshot().rivalHp
        tick(e,8);assertEquals(hp,e.snapshot().rivalHp)
    }
    @Test fun holdingDirectionMovesInSimulationAndExpires(){
        val e=ArenaCombatEngine(1)
        val before=e.snapshot().playerX
        e.act(9);assertEquals(before,e.snapshot().playerX,.001f)
        tick(e,3);assertTrue(e.snapshot().playerX>before)
        e.act(10);tick(e,30)
        val stopped=e.snapshot().playerX
        tick(e,10);assertEquals(stopped,e.snapshot().playerX,.01f)
    }
    @Test fun dashAndGuardAreTimed(){
        val e=ArenaCombatEngine(2)
        e.act(5);val timer=e.snapshot().evadeLock
        e.act(5);assertEquals(timer,e.snapshot().evadeLock,.001f)
        tick(e,44);assertEquals(0f,e.snapshot().evade,.001f)
        e.act(4);assertTrue(e.snapshot().guard>0f)
        tick(e,40);assertEquals(0f,e.snapshot().guard,.001f)
    }
    @Test fun jumpArcsAndCannotBeRearmedInAir(){
        val e=ArenaCombatEngine(7)
        e.act(7);tick(e,18);assertTrue(e.snapshot().jumpHeight>.6f)
        val remaining=e.snapshot().jump;e.act(7);assertEquals(remaining,e.snapshot().jump,.001f)
        tick(e,32);assertEquals(0f,e.snapshot().jumpHeight,.001f)
    }
    @Test fun bufferedKickExecutesAfterJab(){
        val e=ArenaCombatEngine(9);close(e);e.act(2)
        // Accept a press even before the jab's damage frame.
        e.act(3);assertEquals(3,e.snapshot().queued)
        tick(e,55)
        assertTrue(e.snapshot().bestCombo>=2)
        assertTrue(e.snapshot().rivalHp<188)
        assertEquals(-1,e.snapshot().queued)
    }
    private fun reachShield():ArenaCombatEngine{
        val e=ArenaCombatEngine(2)
        repeat(900){
            val f=e.snapshot()
            if(f.phase==ArenaPhase.KNOWLEDGE)return e
            if(f.phase!=ArenaPhase.SHIELD_BREAK&&f.lock<=0f)
                if(f.distance>.25f)e.act(1)else e.act(3)
            e.step(1f/60f)
        }
        error("Shield not reached")
    }
    @Test fun mandatoryShieldStopsSimulationAndRequiresCorrectAnswer(){
        val e=reachShield();val before=e.snapshot()
        assertEquals(160,before.rivalHp)
        tick(e,120);e.act(3);assertEquals(before,e.snapshot())
        val q=before.question!!
        e.act(20+(q.correct+1)%3)
        assertEquals(before.hp-8,e.snapshot().hp);assertEquals(0,e.snapshot().shieldsBroken)
        e.act(20+q.correct);assertEquals(0,e.snapshot().shieldsBroken)
        e.act(23);val retry=e.snapshot();e.act(20+retry.question!!.correct)
        assertEquals(120,e.snapshot().rivalHp);assertEquals(40,e.snapshot().shieldDamage)
        e.act(20+retry.question!!.correct);assertEquals(120,e.snapshot().rivalHp)
        e.act(23);assertEquals(ArenaPhase.RECOVER,e.snapshot().phase)
    }
    @Test fun allRivalsCanBeBeatenWithSixShieldsAndFortyPercentKnowledge(){
        val e=ArenaCombatEngine(4)
        repeat(12000){
            val f=e.snapshot();if(f.finished)return@repeat
            when{
                f.phase==ArenaPhase.KNOWLEDGE->if(f.selected<0)e.act(20+f.question!!.correct)else e.act(23)
                f.phase==ArenaPhase.SHIELD_BREAK||f.phase==ArenaPhase.ROUND_CLEAR->Unit
                f.phase==ArenaPhase.WINDUP&&f.phaseTime<.18f&&f.evadeLock<=0f->e.act(5)
                f.lock<=0f&&f.distance>.27f->e.act(1)
                f.lock<=0f&&f.phase!=ArenaPhase.WINDUP->e.act(if(f.power>=100)6 else 3)
                f.phase==ArenaPhase.WINDUP&&f.phaseTime<.42f&&f.attack!=RivalAttack.CHARGE&&f.lock<=0f->e.act(4)
            }
            e.step(1f/60f)
        }
        assertTrue(e.snapshot().detail,e.snapshot().won)
        assertEquals(6,e.snapshot().knowledgeCorrect)
        assertEquals(300,e.snapshot().shieldDamage)
    }
}
