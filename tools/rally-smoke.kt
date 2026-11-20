package cl.vialapp.app.ui.games.rally

// QUÉ PROTEGE ESTA PRUEBA:
// Carrera simulada sin OpenGL. Probamos físicas y progreso antes de confiar en una partida real.


import kotlin.math.*

/** Headless smoke test: execute the same engine as Android without the Android SDK. */
fun main(){
    val dt=1f/60f
    val reverse=RallyEngine(RallyTracks.all.first(),RallyMode.TIME_TRIAL)
    repeat(190){reverse.step(dt,0f,false,false)}
    repeat(60){reverse.step(dt,0f,false,true)}
    check(reverse.snapshot().speed<0f&&reverse.snapshot().progress<0f){"Reverse did not engage"}
    for(track in RallyTracks.all){
        val ramps=rallyRamps(track);check(ramps.size in 2..3){"Missing ramps: "+track.id}
        val ramp=ramps.first();var air=rallyAirStep(track,ramps,RallyAir(ramp.height),ramp.progress-1f,ramp.progress+1f,ramp.lane,140f,dt)
        check(air.airborne){"Ramp did not launch: "+track.id}
        var distance=ramp.progress+1f
        repeat(160){air=rallyAirStep(track,ramps,air,distance,distance+2f,ramp.lane,140f,dt);distance+=2f}
        check(!air.airborne){"Jump did not land: "+track.id}
        check(rallyBoxes(track).map{it.item}.toSet().size==6)
    }
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
                    val lookAhead=maxOf(25f,now.speed*.40f)
                    val upcoming=if(mode==RallyMode.CHAOS&&now.item==null)now.boxes.filter{b->
                        val gap=((b.progress-now.progress)%track.length+track.length)%track.length
                        gap in 12f..95f
                    }.minByOrNull{b->((b.progress-now.progress)%track.length+track.length)%track.length+abs(b.lane)*.25f}else null
                    val aim=track.at(now.progress+lookAhead,upcoming?.lane?:0f).first-now.pos
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
