package cl.vialapp.app.ui.games.arcade

import org.junit.Assert.*
import org.junit.Test

class ArenaCombatEngineTest {
    private fun tick(e:ArenaCombatEngine,frames:Int){repeat(frames){e.step(1f/60f)}}
    private fun close(e:ArenaCombatEngine){repeat(6){e.act(1)}}
    @Test fun distantPunchMissesAndClosePunchHitsOnlyOnce(){
        val e=ArenaCombatEngine(1)
        e.act(2);tick(e,25)
        assertEquals(320,e.snapshot().rivalHp)
        close(e);e.act(2);tick(e,20)
        assertTrue(e.snapshot().rivalHp<320)
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
        assertTrue(e.snapshot().rivalHp<308)
        assertEquals(-1,e.snapshot().queued)
    }
    private fun reachShield():ArenaCombatEngine{
        val e=ArenaCombatEngine(2)
        repeat(1800){
            val f=e.snapshot()
            if(f.phase==ArenaPhase.KNOWLEDGE)return e
            if(!f.isCinematic&&f.lock<=0f)
                if(f.distance>.25f)e.act(1)else e.act(if(f.lastHit==ArenaMove.HEAVY)2 else 3)
            e.step(1f/60f)
        }
        error("Shield not reached")
    }

    @Test fun shieldsLeaveLongerCombatSegmentsForEveryRival(){
        for(maxHp in listOf(320,400,480)){
            val first=ArenaShieldRules.threshold(maxHp,0)
            val second=ArenaShieldRules.threshold(maxHp,1)
            val answerDamage=ArenaShieldRules.answerDamage(maxHp)
            assertEquals(maxHp*35/100,maxHp-first)
            assertEquals(maxHp*30/100,first-answerDamage-second)
            assertEquals(maxHp*15/100,second-answerDamage)
        }
        val e=reachShield()
        assertEquals(208,e.snapshot().rivalHp)
        e.act(20+e.snapshot().question!!.correct);tick(e,82)
        val initial=e.snapshot().rivalHp
        var hits=0
        repeat(2400){
            val f=e.snapshot()
            if(f.phase!=ArenaPhase.KNOWLEDGE){
                if(!f.isCinematic&&f.lock<=0f){
                    if(f.distance>.25f)e.act(1)else e.act(if(f.lastHit==ArenaMove.HEAVY)2 else 3)
                }
                val next=e.step(1f/60f)
                if(next.rivalHp<f.rivalHp)hits++
            }
        }
        assertEquals(ArenaPhase.KNOWLEDGE,e.snapshot().phase)
        assertEquals(80,e.snapshot().rivalHp)
        assertEquals(96,initial-e.snapshot().rivalHp)
        assertTrue(hits>=4)
    }

    @Test fun answerCinematicAppliesDamageAtImpactExactlyOnce(){
        val e=reachShield();val before=e.snapshot()
        assertEquals(208,before.rivalHp)
        tick(e,120);e.act(3);assertEquals(before,e.snapshot())
        val q=before.question!!
        e.act(20+q.correct)
        assertEquals(ArenaPhase.ANSWER_IMPACT,e.snapshot().phase)
        assertEquals(before.rivalHp,e.snapshot().rivalHp)
        assertFalse(e.snapshot().answerApplied)
        tick(e,20)
        assertEquals(before.rivalHp,e.snapshot().rivalHp)
        // Early continue and repeated inputs cannot award again or skip it.
        e.act(23);e.act(20+q.correct);e.act(3)
        tick(e,25)
        assertTrue(e.snapshot().answerApplied)
        assertEquals(176,e.snapshot().rivalHp)
        assertEquals(32,e.snapshot().shieldDamage)
        assertEquals(before.elapsed,e.snapshot().elapsed,.001f)
        tick(e,37)
        assertEquals(ArenaPhase.RECOVER,e.snapshot().phase)
        assertEquals(1,e.snapshot().knowledgeCorrect)
        assertEquals(176,e.snapshot().rivalHp)
        assertEquals(q.explanation,e.snapshot().answerFeedback)
        assertNull(e.snapshot().question)
        e.act(20+q.correct);e.act(23)
        assertEquals(1,e.snapshot().knowledgeCorrect)
        tick(e,10)
        assertTrue(e.snapshot().elapsed>before.elapsed)
        assertNull(e.snapshot().question)
        e.act(3);tick(e,20)
        assertTrue(e.snapshot().rivalHp<176)
        assertEquals(1,e.snapshot().shieldsBroken)
        assertNull(e.snapshot().question)
    }

    @Test fun wrongAnswerShowsCounterattackBeforeAllowingRetry(){
        val e=reachShield();val before=e.snapshot();val q=before.question!!
        e.act(20+(q.correct+1)%3)
        assertEquals(ArenaPhase.ANSWER_IMPACT,e.snapshot().phase)
        assertEquals(before.hp,e.snapshot().hp)
        tick(e,45)
        assertEquals(before.hp-8,e.snapshot().hp)
        assertEquals(1,e.snapshot().knowledgeMissed)
        assertEquals(0,e.snapshot().shieldsBroken)
        assertEquals(before.rivalHp,e.snapshot().rivalHp)
        e.act(20+q.correct);tick(e,60)
        assertEquals(ArenaPhase.KNOWLEDGE,e.snapshot().phase)
        assertEquals(before.hp-8,e.snapshot().hp)
        assertEquals(before.elapsed,e.snapshot().elapsed,.001f)
        val retry=e.snapshot()
        assertEquals(-1,retry.selected)
        assertNotEquals(q.prompt,retry.question!!.prompt)
        assertEquals(q.explanation,retry.answerFeedback)
        e.act(20+retry.question.correct);tick(e,82)
        assertEquals(1,e.snapshot().shieldsBroken)
        assertEquals(176,e.snapshot().rivalHp)
        assertEquals(ArenaPhase.RECOVER,e.snapshot().phase)
        assertNull(e.snapshot().question)
    }
    @Test fun allRivalsCanBeBeatenWithSixShieldsAndTwentyPercentKnowledge(){
        val e=ArenaCombatEngine(4)
        repeat(20000){
            val f=e.snapshot();if(f.finished)return@repeat
            when{
                f.phase==ArenaPhase.KNOWLEDGE->e.act(20+f.question!!.correct)
                f.isCinematic->Unit
                f.phase==ArenaPhase.WINDUP&&f.phaseTime<.18f&&f.evadeLock<=0f->e.act(5)
                f.lock<=0f&&f.distance>.27f->e.act(1)
                f.lock<=0f&&f.phase!=ArenaPhase.WINDUP->e.act(if(f.power>=100)6 else if(f.lastHit==ArenaMove.HEAVY)2 else 3)
                f.phase==ArenaPhase.WINDUP&&f.phaseTime<.42f&&f.attack!=RivalAttack.CHARGE&&f.lock<=0f->e.act(4)
            }
            e.step(1f/60f)
        }
        assertTrue(e.snapshot().detail,e.snapshot().won)
        assertEquals(6,e.snapshot().knowledgeCorrect)
        assertEquals(240,e.snapshot().shieldDamage)
    }
}
