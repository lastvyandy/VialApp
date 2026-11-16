package cl.vialapp.app.ui.games.rally

import kotlin.math.*
import kotlin.random.Random

internal data class V2(val x:Float,val y:Float){
    operator fun plus(v:V2)=V2(x+v.x,y+v.y)
    operator fun minus(v:V2)=V2(x-v.x,y-v.y)
    operator fun times(s:Float)=V2(x*s,y*s)
    fun length()=sqrt(x*x+y*y)
    fun dot(v:V2)=x*v.x+y*v.y
}
internal data class RallyTrack(val id:String,val name:String,val level:Int,val terrain:Int,val grip:Float,
    val anchors:List<V2>,val halfWidth:Float=43f){
    // A periodic Catmull-Rom spline; arc-length sampling makes rival speed independent of the curves.
    val nodes:List<V2> = anchors.indices.flatMap{i->
        val a=anchors[(i+anchors.size-1)%anchors.size];val b=anchors[i]
        val c=anchors[(i+1)%anchors.size];val d=anchors[(i+2)%anchors.size]
        (0 until 24).map{j->val t=j/24f;val t2=t*t;val t3=t2*t
            (b*2f+(c-a)*t+(a*2f-b*5f+c*4f-d)*t2+(b*3f-a-c*3f+d)*t3)*.5f}
    }
    val lengths:FloatArray=FloatArray(nodes.size+1).also{arr->for(i in nodes.indices)arr[i+1]=arr[i]+(nodes[(i+1)%nodes.size]-nodes[i]).length()}
    val length=lengths.last()
    fun at(distance:Float,lateral:Float=0f):Pair<V2,Float>{
        val s=((distance%length)+length)%length
        var lo=0;var hi=nodes.lastIndex
        while(lo<hi){val m=(lo+hi+1)/2;if(lengths[m]<=s)lo=m else hi=m-1}
        val a=nodes[lo];val b=nodes[(lo+1)%nodes.size];val v=b-a
        val angle=atan2(v.y,v.x);val t=(s-lengths[lo])/(lengths[lo+1]-lengths[lo]).coerceAtLeast(.01f)
        return (a+v*t+V2(-sin(angle),cos(angle))*lateral) to angle
    }
    fun nearest(pos:V2,prior:Float):RoadProjection{
        val wrapped=((prior%length)+length)%length
        var best=Float.MAX_VALUE;var progress=wrapped;var near=V2(0f,0f)
        for(i in nodes.indices){
            val a=nodes[i];val b=nodes[(i+1)%nodes.size];val ab=b-a
            val t=((pos-a).dot(ab)/ab.dot(ab).coerceAtLeast(.001f)).coerceIn(0f,1f)
            val q=a+ab*t;val sq=(pos-q).dot(pos-q)
            val s=lengths[i]+(lengths[i+1]-lengths[i])*t
            val delta=wrappedDelta(s-wrapped,length)
            // Don't teleport across adjacent pieces of a technical circuit.
            if(abs(delta)<170f&&sq<best){best=sq;progress=prior+delta;near=q}
        }
        return RoadProjection(progress,sqrt(best),near)
    }
}
internal data class RoadProjection(val progress:Float,val distance:Float,val center:V2)
internal fun wrappedDelta(delta:Float,l:Float):Float=((delta+l*.5f)%l+l)%l-l*.5f
internal object RallyTracks{
    private fun circuit(id:String,name:String,level:Int,terrain:Int,grip:Float,vararg points:Int):RallyTrack=
        RallyTrack(id,name,level,terrain,grip,points.toList().chunked(2).map{V2(it[0].toFloat(),it[1].toFloat())})
    val all=listOf(
        circuit("atacama","Desierto de Atacama",1,0,.96f, 0,0, 340,0, 480,110, 360,240, 520,370, 470,560, 190,565, 0,410, -230,440, -330,240, -200,100),
        circuit("valparaiso","Valparaíso",3,1,1f, 0,0, 325,-45, 460,95, 650,90, 700,300, 515,395, 410,235, 180,320, 310,520, 120,690, -190,600, -270,300, -180,100),
        circuit("bosque","Bosques del sur",5,2,.82f, 0,0, 280,-65, 480,60, 550,240, 400,330, 590,490, 410,660, 120,560, -80,670, -270,470, -140,230, -250,90),
        circuit("costa","Costa del Pacífico",7,3,.94f, 0,0, 390,-70, 620,35, 690,230, 560,380, 300,340, 450,570, 210,690, -70,590, -240,360, -150,150),
        circuit("santiago","Santiago nocturno",10,4,1f, 0,0, 340,0, 510,-130, 720,10, 720,280, 490,280, 490,490, 190,560, 190,310, -90,330, -260,200, -240,0),
        circuit("andes","Cordillera de los Andes",12,5,.69f, 0,0, 300,-100, 550,-70, 670,100, 530,230, 300,165, 180,320, 480,420, 665,560, 490,740, 140,710, -160,530, -300,270, -180,90),
        circuit("volcan","Ruta volcánica",15,6,.78f, 0,0, 300,-70, 580,0, 680,210, 470,285, 625,465, 420,655, 240,535, 60,700, -220,570, -315,330, -170,180, -270,50),
        circuit("patagonia","Patagonia",20,7,.87f, 0,0, 410,-80, 710,70, 775,320, 550,400, 365,260, 195,400, 410,640, 180,800, -180,670, -350,420, -225,200, -340,90)
    )
}
internal enum class RallyMode(val title:String,val laps:Int){ CLASSIC("Clásica",2), CHAOS("Caótica",2), TIME_TRIAL("Contrarreloj",1) }
internal enum class RallyItem(val label:String){ NITRO("Nitro"),SHIELD("Escudo"),OIL("Aceite"),MAGNET("Imán"),REPAIR("Reparación"),EMP("Pulso EMP") }
internal data class Racer(val progress:Float,val lane:Float,val speed:Float,val slowed:Float=0f,val finish:Float=0f,
    val nitro:Float=0f,val shield:Float=0f,val impact:Float=0f,val item:RallyItem?=null,
    val useIn:Float=0f,val integrity:Int=100,val targetLane:Float=lane,val decision:Float=0f)
internal data class Oil(val pos:V2,val life:Float,val owner:Int=-1)
internal data class RallyImpact(val pos:V2,val life:Float)
internal data class RallyPulse(val pos:V2,val life:Float)
internal data class RallyPad(val progress:Float,val lane:Float)
internal fun rallyBoxRows(track:RallyTrack):List<Float>{
    val count=ceil(track.length/175f).toInt()
    return List(count){track.length*it/count+86f}
}
internal fun rallyBoxLanes(track:RallyTrack)=listOf(-track.halfWidth*.72f,-track.halfWidth*.24f,track.halfWidth*.24f,track.halfWidth*.72f)
internal fun rallyPads(track:RallyTrack):List<RallyPad> {
    val candidates=(1..12).map{track.length*it/13f}
    val straight=candidates.filter{abs(wrappedDelta(track.at(it+35f).second-track.at(it-20f).second,2f*PI.toFloat()))<.36f}
    return straight.take(6).mapIndexed{i,p->RallyPad(p,if(i%2==0)-track.halfWidth*.4f else track.halfWidth*.4f)}
}
internal fun RallyTrack.lateral(pos:V2,progress:Float):Float {
    val at=at(progress)
    return (pos-at.first).dot(V2(-sin(at.second),cos(at.second)))
}
internal data class RallyFrame(val pos:V2,val angle:Float,val speed:Float,val progress:Float,val elapsed:Float,
    val countdown:Float,val rivals:List<Racer>,val boxes:List<Float>,val coins:List<Float>,val oils:List<Oil>,
    val item:RallyItem?=null,val nitro:Float=0f,val shield:Float=0f,val magnet:Float=0f,
    val integrity:Int=100,val collected:Int=0,val drift:Float=0f,val hit:Float=0f,val pulse:Float=0f,
    val finished:Boolean=false,val place:Int=1,val lap:Int=1,val offroad:Boolean=false,val message:String="",
    val steering:Float=0f,val wrongWay:Boolean=false,
    val impacts:List<RallyImpact> = emptyList(),val enemyPulses:List<RallyPulse> = emptyList(),
    val boost:Float=0f,val drafted:Boolean=false,val eventTime:Float=0f,val contacts:Int=0)
internal class RallyEngine(val track:RallyTrack,val mode:RallyMode,seed:Int=42){
    private val random=Random(seed)
    private val total=track.length*mode.laps
    private val start=track.at(0f)
    private val boxPositions=rallyBoxRows(track)
    private val lanes=rallyBoxLanes(track)
    private val pads=rallyPads(track)
    private val coinPositions=List(32){track.length*(it+.45f)/32f}
    private val boxCooldown=FloatArray(boxPositions.size)
    private val playerBoxLap=IntArray(boxPositions.size){-1}
    private val coinCooldown=FloatArray(coinPositions.size)
    private val padCooldown=FloatArray(pads.size)
    private val botBoxLap=Array(5){IntArray(boxPositions.size){-1}}
    private val botPadLap=Array(5){IntArray(pads.size){-1}}
    private val contactCooldown=Array(6){FloatArray(6)}
    private var oilCooldown=0f
    private var f=RallyFrame(start.first,start.second,0f,0f,0f,3f,
        if(mode==RallyMode.TIME_TRIAL)emptyList() else List(5){i->Racer(listOf(38f,38f,0f,-38f,-38f)[i],if(i%2==0)-23f else 23f,0f)},
        if(mode==RallyMode.CHAOS)boxPositions else emptyList(),coinPositions,emptyList(),place=if(mode==RallyMode.TIME_TRIAL)1 else 3)
    fun snapshot()=f
    fun recover(){
        if(f.finished||f.countdown>0f||(!f.offroad&&!f.wrongWay))return
        val center=track.at(f.progress)
        f=f.copy(pos=center.first,angle=center.second,speed=14f,elapsed=f.elapsed+4f,steering=0f,drift=0f,
            offroad=false,wrongWay=false,nitro=0f,boost=0f,message="Recolocado · +4 s",eventTime=2f)
    }
    fun useItem(){
        if(f.finished||f.countdown>0f)return
        val item=f.item?:return
        f=when(item){
            RallyItem.NITRO->f.copy(nitro=3.4f,message="¡NITRO!")
            RallyItem.SHIELD->f.copy(shield=6f,message="Escudo activo")
            RallyItem.OIL->f.copy(oils=(f.oils+Oil(f.pos-V2(cos(f.angle),sin(f.angle))*20f,8f)).takeLast(16),message="Aceite en pista")
            RallyItem.MAGNET->f.copy(magnet=7f,message="Imán activo")
            RallyItem.REPAIR->f.copy(integrity=(f.integrity+35).coerceAtMost(100),message="Reparado")
            RallyItem.EMP->f.copy(rivals=f.rivals.map{if(it.shield<=0f&&(track.at(it.progress,it.lane).first-f.pos).length()<125f)it.copy(slowed=2.5f,nitro=0f)else it},pulse=.65f,message="¡EMP!")
        }.copy(item=null,eventTime=1.8f)
    }
    fun step(dt:Float,steer:Float,throttle:Boolean,brake:Boolean):RallyFrame{
        if(f.finished||dt<=0f)return f
        val d=dt.coerceAtMost(.05f)
        if(f.countdown>0f){f=f.copy(countdown=(f.countdown-d).coerceAtLeast(0f));return f}
        val old=f
        val elapsed=old.elapsed+d
        var integrity=old.integrity
        var hurt=(old.hit-d).coerceAtLeast(0f)
        var message=old.message
        var event=(old.eventTime-d).coerceAtLeast(0f)
        var contacts=old.contacts
        var impacts=old.impacts.map{it.copy(life=it.life-d)}.filter{it.life>0f}.toMutableList()
        val pulses=old.enemyPulses.map{it.copy(life=it.life-d)}.filter{it.life>0f}.toMutableList()
        val oils=old.oils.map{it.copy(life=it.life-d)}.filter{it.life>0f}.toMutableList()
        fun notice(text:String){message=text;event=1.6f}
        for(a in contactCooldown.indices)for(b in contactCooldown[a].indices)
            contactCooldown[a][b]=(contactCooldown[a][b]-d).coerceAtLeast(0f)
        oilCooldown=(oilCooldown-d).coerceAtLeast(0f)
        val playerLane=track.lateral(old.pos,old.progress)
        val drafted=!old.offroad&&old.rivals.any{it.finish==0f&&it.progress-old.progress in 25f..100f&&abs(it.lane-playerLane)<14f}
        var boost=(old.boost-d).coerceAtLeast(0f)
        var nitro=(old.nitro-d).coerceAtLeast(0f)
        val maxSpeed=(when{nitro>0f->202f;boost>0f->185f;drafted->164f;else->150f})*(if(old.integrity<30).86f else 1f)
        val acceleration=when{brake&&throttle->-34f;brake->-132f;throttle->if(nitro>0f||boost>0f)153f else 108f;else->-22f}
        val speed=(old.speed+acceleration*d).coerceIn(0f,maxSpeed)
        val turn=old.steering+(steer.coerceIn(-1f,1f)-old.steering)*(d*15f).coerceAtMost(1f)
        val roadHeading=track.at(old.progress+22f).second
        val difference=wrappedDelta(roadHeading-old.angle,2f*PI.toFloat())
        val assist=if(!old.offroad&&abs(difference)<.78f&&speed>15f)difference*.85f else 0f
        var angle=old.angle+(turn*(1.18f+1.44f*(speed/150f).coerceIn(0f,1f))*(if(brake&&speed>35f)1.35f else 1f)*(.70f+.30f*track.grip)*(if(speed<2f)0f else 1f)+assist)*d
        val drift=if(brake&&abs(turn)>.21f&&speed>28f)turn*.35f else 0f
        var p=old.pos+V2(cos(angle-drift),sin(angle-drift))*speed*d
        var road=track.nearest(p,old.progress)
        var off=road.distance>track.halfWidth
        var v=if(off)(speed-57f*d).coerceIn(0f,67f) else speed
        if(road.distance>track.halfWidth+23f){
            val dir=p-road.center
            p=road.center+dir*((track.halfWidth+23f)/dir.length().coerceAtLeast(1f))
            v*=.78f
        }
        var progress=(old.progress+(road.progress-old.progress).coerceIn(-speed*d*1.65f-1f,speed*d*1.65f+1f)).coerceAtLeast(-track.length*.15f)
        var opponents=old.rivals.mapIndexed{i,r->
            if(r.finish>0f)r else{
                val slowed=(r.slowed-d).coerceAtLeast(0f)
                var botNitro=(r.nitro-d).coerceAtLeast(0f)
                var shield=(r.shield-d).coerceAtLeast(0f)
                val heading=track.at(r.progress).second
                val curve=wrappedDelta(track.at(r.progress+65f).second-heading,2f*PI.toFloat())
                var decision=(r.decision-d).coerceAtLeast(0f)
                var targetLane=r.targetLane
                if(decision<=0f){
                    // Choose the least occupied racing line. Favor inside on curves, change line on traffic.
                    val inside=if(curve>.11f)19f else if(curve<-.11f)-19f else 0f
                    val options=listOf(-27f,-9f,9f,27f)
                    targetLane=options.minByOrNull{lane->
                        var cost=abs(lane-inside)*.17f+abs(lane-r.lane)*.13f
                        for((j,o) in old.rivals.withIndex())if(j!=i&&o.finish==0f){
                            val gap=o.progress-r.progress
                            if(gap in -25f..100f&&abs(o.lane-lane)<18f)
                                cost+=(100f-gap.coerceAtLeast(0f))*.42f
                        }
                        val pg=old.progress-r.progress
                        if(pg in -24f..110f&&abs(playerLane-lane)<18f)cost+=(110f-pg.coerceAtLeast(0f))*.45f
                        if(mode==RallyMode.CHAOS&&r.item==null){
                            val next=boxPositions.minOf { ((it-r.progress)%track.length+track.length)%track.length }
                            if(next<105f)cost+=lanes.minOf{abs(it-lane)}*.8f
                        }
                        for(o in oils)if(o.owner!=i&&(track.at(r.progress+38f,lane).first-o.pos).length()<28f)cost+=65f
                        cost+random.nextFloat()*5f
                    }?:0f
                    decision=.28f+random.nextFloat()*.30f
                }
                val lane=r.lane+(targetLane-r.lane)*(d*2.7f).coerceAtMost(1f)
                // Mild catch-up keeps the field close; a faster player can still pull away.
                val catchUp=((old.progress-r.progress)/420f*12f).coerceIn(-9f,13f)
                val target=(132f+i*3.2f+track.level*.26f+catchUp+(if(botNitro>0f)38f else 0f))*
                    (1f-.33f*abs(curve).coerceIn(0f,1.2f))*(if(slowed>0f).48f else 1f)*(if(r.integrity<30).86f else 1f)
                var sp=(r.speed+(target-r.speed)*d*2.65f).coerceAtLeast(0f)
                var next=r.progress+sp*d
                var botSlow=slowed
                var botHp=r.integrity
                val rp=track.at(next,lane).first
                if(shield<=0f&&slowed<=0f&&oils.any{it.owner!=i&&(it.pos-rp).length()<15f}){
                    botSlow=1.5f;sp*=.63f;botHp=(botHp-6).coerceAtLeast(5)
                }
                var held=r.item
                var use=(r.useIn-d).coerceAtLeast(0f)
                if(mode==RallyMode.CHAOS&&held==null){
                    for(row in boxPositions.indices){
                        val lap=floor(next/track.length).toInt().coerceAtLeast(0)
                        val targetProgress=lap*track.length+boxPositions[row]
                        if(next>=targetProgress&&r.progress<targetProgress&&botBoxLap[i][row]!=lap&&lanes.any{abs(it-lane)<17f}){
                            botBoxLap[i][row]=lap
                            held=listOf(RallyItem.NITRO,RallyItem.SHIELD,RallyItem.OIL,RallyItem.EMP,RallyItem.REPAIR)[random.nextInt(5)]
                            use=.8f+random.nextFloat()*2.2f
                            break
                        }
                    }
                }
                for(k in pads.indices){
                    val pad=pads[k]
                    val lap=floor(next/track.length).toInt().coerceAtLeast(0)
                    val at=lap*track.length+pad.progress
                    if(next>=at&&r.progress<at&&botPadLap[i][k]!=lap&&abs(lane-pad.lane)<17f){
                        botPadLap[i][k]=lap;botNitro=maxOf(1.0f,botNitro)
                    }
                }
                Racer(next,lane,sp,botSlow,if(next>=total)elapsed else 0f,botNitro,shield,
                    (r.impact-d).coerceAtLeast(0f),held,use,botHp,targetLane,decision)
            }
        }.toMutableList()
        // AI items actually affect opponents, not just the player's car.
        for(i in opponents.indices){
            var r=opponents[i]
            if(r.finish>0f||r.item==null||r.useIn>0f)continue
            val at=track.at(r.progress,r.lane)
            val gap=old.progress-r.progress
            val nearPlayer=(at.first-p).length()<100f
            val otherAhead=opponents.indices.any{j->j!=i&&opponents[j].progress-r.progress in 5f..100f}
            val otherBehind=opponents.indices.any{j->j!=i&&r.progress-opponents[j].progress in 8f..105f}
            when(r.item){
                RallyItem.NITRO->{if(abs(wrappedDelta(track.at(r.progress+45f).second-at.second,2f*PI.toFloat()))>.75f)continue;r=r.copy(nitro=2.3f)}
                RallyItem.SHIELD->{if(!nearPlayer&&!otherAhead&&!otherBehind)continue;r=r.copy(shield=4f)}
                RallyItem.REPAIR->{if(r.integrity>75) {r=r.copy(item=RallyItem.NITRO,useIn=.2f);opponents[i]=r;continue};r=r.copy(integrity=(r.integrity+30).coerceAtMost(100))}
                RallyItem.OIL->{if(!(gap in -105f..-8f)&&!otherBehind)continue
                    oils.add(Oil(at.first-V2(cos(at.second),sin(at.second))*23f,7f,i))}
                RallyItem.EMP->{
                    if(!nearPlayer&&!otherAhead&&!otherBehind)continue
                    pulses.add(RallyPulse(at.first,.65f))
                    for(j in opponents.indices)if(i!=j&&opponents[j].shield<=0f&&
                        (track.at(opponents[j].progress,opponents[j].lane).first-at.first).length()<105f)
                        opponents[j]=opponents[j].copy(slowed=1.65f,nitro=0f)
                    if(nearPlayer&&old.shield<=0f){v*=.66f;nitro=0f;boost=0f;hurt=.42f;notice("¡EMP enemigo!")}
                }
                RallyItem.MAGNET,null->Unit
            }
            opponents[i]=r.copy(item=null,useIn=0f)
        }
        // Local longitudinal/lateral collision volumes; resolve both vehicles every tick.
        // Use the physical distance too, so cars separated by an adjacent curve never collide.
        for(i in opponents.indices){
            var r=opponents[i]
            if(r.finish>0f)continue
            val gp=r.progress-progress
            val lp=track.lateral(p,progress)
            val gl=r.lane-lp
            val rp=track.at(r.progress,r.lane).first
            if(abs(gp)<26f&&abs(gl)<15f&&(rp-p).length()<32f){
                val heading=track.at(progress).second
                val tangent=V2(cos(heading),sin(heading))
                val side=V2(-sin(heading),cos(heading))
                val sideways=(15f-abs(gl))<(26f-abs(gp))
                val sign=if(if(sideways)gl>=0f else gp>=0f)1f else -1f
                val penetration=(if(sideways)15f-abs(gl) else 26f-abs(gp))+.45f
                if(sideways){
                    p=p-side*(sign*penetration*.53f)
                    r=r.copy(lane=(r.lane+sign*penetration*.47f).coerceIn(-track.halfWidth+7f,track.halfWidth-7f))
                }else{
                    val push=penetration*.50f
                    p=p-tangent*(sign*push)
                    progress=(progress-sign*push).coerceAtLeast(-track.length*.15f)
                    r=r.copy(progress=r.progress+sign*push)
                }
                if(contactCooldown[0][i+1]<=0f){
                    contactCooldown[0][i+1]=.48f
                    val force=(abs(v-r.speed)*.13f+if(sideways)5f else 9f).toInt().coerceIn(4,19)
                    v*=if(sideways).88f else .69f
                    r=r.copy(speed=r.speed*(if(sideways).88f else .74f),impact=.48f,
                        integrity=if(r.shield>0f)r.integrity else (r.integrity-force).coerceAtLeast(5))
                    if(old.shield<=0f){integrity=(integrity-force).coerceAtLeast(5);hurt=.48f;angle-=sign*(if(sideways).12f else .055f)}
                    impacts.add(RallyImpact((p+rp)*.5f,.48f));contacts++;notice("¡CHOQUE!")
                }
                opponents[i]=r
            }
        }
        for(i in opponents.indices)for(j in i+1 until opponents.size){
            var a=opponents[i];var b=opponents[j]
            if(a.finish>0f||b.finish>0f)continue
            val gap=b.progress-a.progress;val lateral=b.lane-a.lane
            if(abs(gap)<26f&&abs(lateral)<15f){
                val along=(26f-abs(gap))<(15f-abs(lateral))
                if(along){
                    val push=(26f-abs(gap)+.3f)*.50f*(if(gap>=0f)1f else -1f)
                    a=a.copy(progress=a.progress-push);b=b.copy(progress=b.progress+push)
                }else{
                    val push=(15f-abs(lateral)+.3f)*.50f*(if(lateral>=0f)1f else -1f)
                    a=a.copy(lane=(a.lane-push).coerceIn(-35f,35f));b=b.copy(lane=(b.lane+push).coerceIn(-35f,35f))
                }
                if(contactCooldown[i+1][j+1]<=0f){
                    contactCooldown[i+1][j+1]=.55f
                    impacts.add(RallyImpact((track.at(a.progress,a.lane).first+track.at(b.progress,b.lane).first)*.5f,.42f))
                    a=a.copy(speed=a.speed*.86f,impact=.42f,integrity=if(a.shield>0f)a.integrity else (a.integrity-4).coerceAtLeast(5))
                    b=b.copy(speed=b.speed*.83f,impact=.42f,integrity=if(b.shield>0f)b.integrity else (b.integrity-4).coerceAtLeast(5))
                }
                opponents[i]=a;opponents[j]=b
            }
        }
        road=track.nearest(p,progress);off=road.distance>track.halfWidth
        if(oilCooldown<=0f&&old.shield<=0f&&oils.any{it.owner!=-1&&(it.pos-p).length()<15f}){
            oilCooldown=1.7f;angle+=if(random.nextBoolean()).30f else -.30f
            v*=.56f;hurt=.5f;integrity=(integrity-5).coerceAtLeast(5);notice("¡Aceite!")
        }
        var item=old.item;var coins=old.collected
        for(row in boxPositions.indices){
            boxCooldown[row]=(boxCooldown[row]-d).coerceAtLeast(0f)
            val currentLap=floor(progress.coerceAtLeast(0f)/track.length).toInt()
            if(mode==RallyMode.CHAOS&&item==null&&boxCooldown[row]==0f&&playerBoxLap[row]!=currentLap&&
                abs(wrappedDelta(progress-boxPositions[row],track.length))<25f&&
                lanes.any{lane->(track.at(boxPositions[row],lane).first-p).length()<19f}){
                val pool=if(integrity<65)listOf(RallyItem.NITRO,RallyItem.REPAIR,RallyItem.EMP,RallyItem.SHIELD,RallyItem.REPAIR)
                    else if(old.place>=4)listOf(RallyItem.NITRO,RallyItem.NITRO,RallyItem.EMP,RallyItem.MAGNET,RallyItem.SHIELD)
                    else listOf(RallyItem.NITRO,RallyItem.EMP,RallyItem.OIL,RallyItem.SHIELD,RallyItem.MAGNET,RallyItem.REPAIR)
                item=pool[random.nextInt(pool.size)]
                boxCooldown[row]=5f;playerBoxLap[row]=currentLap;notice(item.label+" disponible")
            }
        }
        for(k in pads.indices){
            padCooldown[k]=(padCooldown[k]-d).coerceAtLeast(0f)
            val pad=pads[k]
            if(padCooldown[k]==0f&&(track.at(pad.progress,pad.lane).first-p).length()<22f&&v>25f){
                boost=1.8f;padCooldown[k]=7f;v=maxOf(v,154f);notice("¡TURBO!")
            }
        }
        for(i in coinPositions.indices){
            coinCooldown[i]=(coinCooldown[i]-d).coerceAtLeast(0f)
            val coinLane=if(i%2==0)-21f else 21f
            val pickupRadius=if(old.magnet>0f)62f else 20f
            if(coinCooldown[i]==0f&&(track.at(coinPositions[i],coinLane).first-p).length()<pickupRadius){coins++;coinCooldown[i]=22f}
        }
        val finish=progress>=total
        if(finish)progress=total
        val place=1+opponents.count{it.finish>0f||it.progress>progress}
        val lap=(floor(progress.coerceAtLeast(0f)/track.length).toInt()+1).coerceAtMost(mode.laps)
        val wrongWay=abs(wrappedDelta(angle-track.at(progress).second,2f*PI.toFloat()))>1.85f
        f=old.copy(pos=p,angle=angle,speed=v,progress=progress,elapsed=elapsed,place=place,lap=lap,rivals=opponents.toList(),
            boxes=if(mode==RallyMode.CHAOS)boxPositions.filterIndexed{i,_->boxCooldown[i]==0f&&playerBoxLap[i]!=floor(progress.coerceAtLeast(0f)/track.length).toInt()}else emptyList(),
            coins=coinPositions.filterIndexed{i,_->coinCooldown[i]==0f},oils=oils.takeLast(16),
            item=item,nitro=nitro,shield=(old.shield-d).coerceAtLeast(0f),magnet=(old.magnet-d).coerceAtLeast(0f),
            integrity=integrity,collected=coins,drift=drift,hit=hurt,pulse=(old.pulse-d).coerceAtLeast(0f),
            finished=finish,offroad=off,message=message,steering=turn,wrongWay=wrongWay,
            impacts=impacts.takeLast(18),enemyPulses=pulses.takeLast(5),boost=boost,drafted=drafted,eventTime=event,contacts=contacts)
        return f
    }
    fun score():Int=if(!f.finished)0 else (1100+(6-f.place)*220+f.collected*35+f.integrity*3+
        (1500-f.elapsed.toInt()*7).coerceAtLeast(0)).coerceAtMost(6000)
}
