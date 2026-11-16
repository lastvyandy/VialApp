package cl.vialapp.app.ui.games.rally

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.PI

class RallyEngineTest {
    private val track=RallyTracks.all.first()
    private val tick=1f/60f
    private fun start(e:RallyEngine){repeat(190){e.step(tick,0f,false,false)}}
    @Test fun allTracksAreClosedAndDistinct(){
        assertEquals(8,RallyTracks.all.size)
        assertEquals(8,RallyTracks.all.map{it.id}.toSet().size)
        for(t in RallyTracks.all){
            assertTrue(t.length>1000f)
            assertTrue((t.at(0f).first-t.at(t.length).first).length()<.01f)
            val near=t.nearest(t.at(t.length-2f).first,t.length-4f)
            assertEquals(t.length-2f,near.progress,.2f)
        }
    }
    @Test fun countdownPreventsMotion(){
        val e=RallyEngine(track,RallyMode.CHAOS)
        val original=e.snapshot()
        repeat(100){e.step(tick,1f,true,false)}
        assertEquals(original.pos,e.snapshot().pos)
        assertEquals(0f,e.snapshot().elapsed,.001f)
        assertEquals(0f,e.snapshot().speed,.001f)
        assertEquals(3,e.snapshot().place)
    }
    @Test fun steeringAndBrakingAffectThePhysicalCar(){
        val e=RallyEngine(track,RallyMode.TIME_TRIAL)
        start(e)
        repeat(50){e.step(tick,0f,true,false)}
        val before=e.snapshot()
        assertTrue(before.speed>25f)
        repeat(14){e.step(tick,1f,true,true)}
        val after=e.snapshot()
        assertTrue(after.speed<before.speed)
        assertTrue(abs(wrappedDelta(after.angle-before.angle,2f*PI.toFloat()))>.06f)
        assertTrue(after.drift>0f)
        assertEquals(0,e.score())
    }
    @Test fun recoveryDoesNotGrantProgressOrRewards(){
        val e=RallyEngine(track,RallyMode.TIME_TRIAL)
        start(e)
        repeat(1400){
            val f=e.snapshot()
            if(!f.offroad&&!f.wrongWay)e.step(tick,1f,true,false)
        }
        val f=e.snapshot()
        assertTrue(f.offroad||f.wrongWay)
        e.recover()
        val r=e.snapshot()
        assertEquals(f.progress,r.progress,.001f)
        assertEquals(f.elapsed+4f,r.elapsed,.001f)
        assertEquals(track.at(r.progress).first,r.pos)
        assertFalse(r.offroad)
        assertFalse(r.wrongWay)
        assertEquals(0,e.score())
    }
}
