package cl.vialapp.app.ui.games.rally

// QUÉ PROTEGE ESTA PRUEBA:
// Verifica físicas, rivales y resultados de Rally sin necesitar OpenGL.

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
    @Test fun chaosHasSparseVariedPickupsAndClassicHasNone(){
        for(t in RallyTracks.all){
            val boxes=rallyBoxes(t)
            assertTrue(boxes.size in 7..18)
            assertEquals(6,boxes.map{it.item}.toSet().size)
            assertTrue(boxes.groupBy{it.progress}.values.all{it.size in 1..2})
            assertTrue(boxes.groupBy{it.progress}.values.map{it.size}.toSet().size>1)
            assertTrue(boxes.all{abs(it.lane)<t.halfWidth-8f})
            assertTrue(RallyEngine(t,RallyMode.CLASSIC).snapshot().boxes.isEmpty())
            assertEquals(boxes,RallyEngine(t,RallyMode.CHAOS).snapshot().boxes)
        }
    }
    @Test fun brakeEngagesReverseAndGasReturnsToForward(){
        val e=RallyEngine(track,RallyMode.TIME_TRIAL)
        start(e)
        repeat(60){e.step(tick,0f,true,false)}
        repeat(105){e.step(tick,0f,false,true)}
        val reverse=e.snapshot()
        assertTrue(reverse.speed < -10f)
        val p=reverse.progress
        repeat(30){e.step(tick,0f,false,true)}
        assertTrue(e.snapshot().progress<p)
        assertEquals(1,e.snapshot().lap)
        assertFalse(e.snapshot().finished)
        repeat(75){e.step(tick,0f,true,false)}
        assertTrue(e.snapshot().speed>20f)
    }
    @Test fun rampsHaveSurfaceLaunchAndLand(){
        for(t in RallyTracks.all){
            val ramps=rallyRamps(t)
            assertTrue(ramps.size in 2..3)
            assertTrue(ramps.first().progress<t.length*.34f)
            val ramp=ramps.first()
            assertEquals(ramp.height*.5f,rallyRampSurface(t,ramps,ramp.progress-ramp.run*.5f,ramp.lane),.01f)
            assertEquals(0f,rallyRampSurface(t,ramps,ramp.progress-ramp.run*.5f,-ramp.lane),.01f)
            var a=rallyAirStep(t,ramps,RallyAir(ramp.height),ramp.progress-1f,ramp.progress+1f,ramp.lane,140f,tick)
            assertTrue(a.airborne)
            val initial=a.height
            a=rallyAirStep(t,ramps,a,ramp.progress+1f,ramp.progress+3f,ramp.lane,140f,tick)
            assertTrue(a.height>initial)
            var p=ramp.progress+3f
            repeat(180){a=rallyAirStep(t,ramps,a,p,p+2f,ramp.lane,140f,tick);p+=2f}
            assertFalse(a.airborne)
            assertEquals(0f,a.height,.01f)
            val backwards=rallyAirStep(t,ramps,RallyAir(),ramp.progress+1f,ramp.progress-1f,ramp.lane,-40f,tick)
            assertFalse(backwards.airborne)
        }
    }
    @Test fun rivalsChangeLaneAndPickUpItems(){
        val e=RallyEngine(track,RallyMode.CHAOS,7)
        start(e)
        val original=e.snapshot().rivals.map{it.lane}
        var moved=false
        var itemSeen=false
        repeat(600){
            val f=e.step(tick,0f,false,false)
            moved=moved||f.rivals.withIndex().any{(i,r)->abs(r.lane-original[i])>4f}
            itemSeen=itemSeen||f.rivals.any{it.item!=null||it.nitro>0f||it.shield>0f}
        }
        assertTrue(moved)
        assertTrue(itemSeen)
        assertEquals(0,e.score())
    }
    @Test fun firstCircuitHasLeftAndRightTurns(){
        for(t in RallyTracks.all){
            val turns=(0..100).map{i->
                val p=t.length*i/100f
                wrappedDelta(t.at(p+45f).second-t.at(p-45f).second,2f*PI.toFloat())
            }
            assertTrue(turns.any{it>.24f})
            assertTrue(turns.any{it<-.24f})
        }
    }
}
