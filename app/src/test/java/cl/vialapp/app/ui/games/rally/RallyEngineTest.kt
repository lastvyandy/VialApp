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
    @Test fun chaosHasEarlyFullWidthPickupRowsAndClassicHasNone(){
        for(t in RallyTracks.all){
            val rows=rallyBoxRows(t)
            assertTrue(rows.size>=14)
            assertEquals(86f,rows.first(),.001f)
            assertTrue(rows.zipWithNext().all{(a,b)->b-a<=175.1f})
            val lanes=rallyBoxLanes(t)
            assertEquals(4,lanes.size)
            assertTrue(lanes.zipWithNext().all{(a,b)->b-a<38f})
            assertTrue(RallyEngine(t,RallyMode.CLASSIC).snapshot().boxes.isEmpty())
            assertEquals(rows,RallyEngine(t,RallyMode.CHAOS).snapshot().boxes)
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
