package cl.vialapp.app.ui.games.arcade

import org.junit.Assert.*
import org.junit.Test

class ArcadeUpgradeTest {
    @Test fun signMissesCostLifeAndAdvanceWithoutAward(){
        val engine=SignEngine(8)
        repeat(220){engine.step(1f/30f)}
        assertTrue(engine.snapshot().timedOut)
        assertEquals(2,engine.snapshot().lives)
        assertEquals(0,engine.snapshot().score)
        repeat(80){engine.step(1f/30f)}
        assertEquals(1,engine.snapshot().index)
        assertFalse(engine.snapshot().timedOut)
    }
    @Test fun earlierCaptureAwardsMoreThanLateCapture(){
        val early=SignEngine(8)
        val late=SignEngine(8)
        repeat(100){late.step(1f/30f)}
        for(engine in listOf(early,late)){
            val s=engine.snapshot()
            engine.act(s.current.choices.indexOf(s.current.sign))
        }
        assertTrue(early.snapshot().score>late.snapshot().score)
    }
    @Test fun arenaCanReachAllRivalsUsingPowerAndDodge(){
        val engine=FightEngine(4)
        repeat(2500){
            val s=engine.snapshot()
            if(s.finished)return@repeat
            when(s.phase){
                FightPhase.TELEGRAPH->engine.act(s.hazard.response)
                FightPhase.OPENING->if(s.attackLock<=0f)engine.act(if(s.power>=100)4 else 3)
                FightPhase.COUNTER->engine.act(5)
                FightPhase.RECOVERY->Unit
            }
            engine.step(1f/30f)
        }
        assertTrue(engine.snapshot().won)
        assertTrue(engine.snapshot().dodges>0)
    }
}
