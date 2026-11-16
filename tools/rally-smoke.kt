package cl.vialapp.app.ui.games.rally

import kotlin.math.*

/** Headless smoke test: execute the same engine as Android without the Android SDK. */
fun main(){
    val dt=1f/60f
    var collisions=0;var pickups=0;var changes=0;var completed=0
    for(track in RallyTracks.all){
        for(mode in listOf(RallyMode.CHAOS,RallyMode.TIME_TRIAL)){
            val engine=RallyEngine(track,mode,7)
            val first=engine.snapshot()
            check(first.boxes.isEmpty()==(mode!=RallyMode.CHAOS))
            var used=0;var aiMoves=false;var recoveries=0
            repeat(18000){
                val f=engine.snapshot()
                if(!f.finished){
                    check(f.pos.x.isFinite()&&f.pos.y.isFinite()&&f.elapsed.isFinite())
                    if(f.countdown<=0f&&(f.offroad||f.wrongWay)){engine.recover();recoveries++}
                    val now=engine.snapshot()
                    val aim=track.at(now.progress+maxOf(25f,now.speed*.40f)).first-now.pos
                    val error=wrappedDelta(atan2(aim.y,aim.x)-now.angle,2f*PI.toFloat())
                    val turn=(error*2.1f).coerceIn(-1f,1f)
                    val brake=abs(error)>.43f&&now.speed>80f
                    val next=engine.step(dt,turn,true,brake)
                    if(next.item!=null){engine.useItem();used++}
                    aiMoves=aiMoves||next.rivals.withIndex().any{(i,r)->abs(r.lane-first.rivals[i].lane)>4f}
                }
            }
            val result=engine.snapshot()
            check(result.finished){"Did not finish: "+track.id+" "+mode+" "+result.progress}
            check(engine.score()>0)
            if(mode==RallyMode.CHAOS){check(used>0){"No pickups in "+track.id};check(aiMoves)}
            completed++;pickups+=used;collisions+=result.contacts;if(aiMoves)changes++
            println(track.id+" "+mode+" time="+result.elapsed+" items="+used+" contacts="+result.contacts+" recoveries="+recoveries)
        }
    }
    check(collisions>0){"No collisions in any simulated race"}
    println("Completed races: "+completed+" pickups: "+pickups+" contacts: "+collisions+" changing-lane races: "+changes)
}
