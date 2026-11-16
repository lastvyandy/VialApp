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
        circuit("atacama","Desierto de Atacama",1,0,.96f, 0,0, 310,0, 490,160, 400,350, 80,420, -150,235),
        circuit("valparaiso","Valparaíso",3,1,1f, 0,0, 220,-30, 410,115, 290,270, 440,450, 40,550, -180,260),
        circuit("bosque","Bosques del sur",5,2,.82f, 0,0, 240,-70, 470,80, 400,260, 170,300, 240,520, -140,430, -220,190),
        circuit("costa","Costa del Pacífico",7,3,.94f, 0,0, 380,0, 550,200, 460,470, 150,570, -100,390,-180,130),
        circuit("santiago","Santiago nocturno",10,4,1f, 0,0, 320,0, 500,150, 500,460, 200,460, 200,240, -120,250,-160,90),
        circuit("andes","Cordillera de los Andes",12,5,.69f, 0,0, 270,-100, 540,10, 400,200, 600,380, 340,550, 40,470,-140,210),
        circuit("volcan","Ruta volcánica",15,6,.78f, 0,0, 350,-80, 530,100, 450,280, 600,520, 270,640, -90,510,-190,230),
        circuit("patagonia","Patagonia",20,7,.87f, 0,0, 400,-90, 630,110, 610,400, 270,370, 130,640,-200,460,-260,140)
    )
}
internal enum class RallyMode(val title:String,val laps:Int){ CLASSIC("Clásica",2), CHAOS("Caótica",2), TIME_TRIAL("Contrarreloj",1) }
internal enum class RallyItem(val label:String){ NITRO("Nitro"),SHIELD("Escudo"),OIL("Aceite"),MAGNET("Imán"),REPAIR("Reparación"),EMP("Pulso EMP") }
internal data class Racer(val progress:Float,val lane:Float,val speed:Float,val slowed:Float=0f,val finish:Float=0f)
internal data class Oil(val pos:V2,val life:Float)
internal data class RallyFrame(val pos:V2,val angle:Float,val speed:Float,val progress:Float,val elapsed:Float,
    val countdown:Float,val rivals:List<Racer>,val boxes:List<Float>,val coins:List<Float>,val oils:List<Oil>,
    val item:RallyItem?=null,val nitro:Float=0f,val shield:Float=0f,val magnet:Float=0f,
    val integrity:Int=100,val collected:Int=0,val drift:Float=0f,val hit:Float=0f,val pulse:Float=0f,
    val finished:Boolean=false,val place:Int=1,val lap:Int=1,val offroad:Boolean=false,val message:String="",
    val steering:Float=0f,val wrongWay:Boolean=false)
internal class RallyEngine(val track:RallyTrack,val mode:RallyMode,seed:Int=42){
    private val random=Random(seed)
    private val total=track.length*mode.laps
    private val start=track.at(0f)
    private val boxPositions=List(8){track.length*(it+.65f)/8f}
    private val coinPositions=List(18){track.length*(it+.45f)/18f}
    private var availableBoxes=boxPositions.toMutableList()
    private var availableCoins=coinPositions.toMutableList()
    private var boxCooldown=FloatArray(boxPositions.size)
    private var coinCooldown=FloatArray(coinPositions.size)
    private var collisionCooldown=0f
    private var f=RallyFrame(start.first,start.second,0f,0f,0f,3f,
        if(mode==RallyMode.TIME_TRIAL)emptyList() else List(5){i->Racer(listOf(38f,38f,0f,-38f,-38f)[i],if(i%2==0)-23f else 23f,0f)},
        if(mode==RallyMode.CHAOS)availableBoxes.toList()else emptyList(),availableCoins.toList(),emptyList(),place=if(mode==RallyMode.TIME_TRIAL)1 else 3)
    fun snapshot()=f
    fun recover(){
        if(f.finished||f.countdown>0f||(!f.offroad&&!f.wrongWay))return
        val center=track.at(f.progress)
        f=f.copy(pos=center.first,angle=center.second,speed=14f,elapsed=f.elapsed+4f,steering=0f,drift=0f,
            offroad=false,wrongWay=false,nitro=0f,message="Recolocado · +4 s")
    }
    fun useItem(){
        if(f.finished||f.countdown>0f)return
        val item=f.item?:return
        f=when(item){
            RallyItem.NITRO->f.copy(nitro=2.8f,message="¡NITRO!")
            RallyItem.SHIELD->f.copy(shield=6f,message="Escudo activo")
            RallyItem.OIL->f.copy(oils=(f.oils+Oil(f.pos,7f)).takeLast(8),message="Aceite en pista")
            RallyItem.MAGNET->f.copy(magnet=7f,message="Imán activo")
            RallyItem.REPAIR->f.copy(integrity=(f.integrity+35).coerceAtMost(100),message="Reparado")
            RallyItem.EMP->f.copy(rivals=f.rivals.map{if(abs(it.progress-f.progress)<190f)it.copy(slowed=2.5f)else it},pulse=.65f,message="¡EMP!")
        }.copy(item=null)
    }
    fun step(dt:Float,steer:Float,throttle:Boolean,brake:Boolean):RallyFrame{
        if(f.finished||dt<=0f)return f
        val d=dt.coerceAtMost(.05f)
        if(f.countdown>0f){f=f.copy(countdown=(f.countdown-d).coerceAtLeast(0f));return f}
        val old=f
        val maxSpeed=(if(old.nitro>0f)166f else 118f)*(if(old.integrity<30).80f else 1f)
        val acceleration=when{brake&&throttle->-36f;brake->-115f;throttle->78f;else->-21f}
        val speed=(old.speed+acceleration*d).coerceIn(0f,maxSpeed)
        val turn=old.steering+(steer.coerceIn(-1f,1f)-old.steering)*(d*12f).coerceAtMost(1f)
        val roadHeading=track.at(old.progress+22f).second
        val difference=wrappedDelta(roadHeading-old.angle,2f*PI.toFloat())
        val assist=if(!old.offroad&&abs(difference)<.65f&&speed>15f)difference*.6f else 0f
        val angle=old.angle+(turn*(.90f+1.02f*(speed/118f).coerceIn(0f,1f))*(if(brake&&speed>35f)1.32f else 1f)*track.grip*(if(speed<2f)0f else 1f)+assist)*d
        val drift=if(brake&&abs(turn)>.21f&&speed>28f)turn*.30f else 0f
        val pos=old.pos+V2(cos(angle-drift),sin(angle-drift))*speed*d
        val road=track.nearest(pos,old.progress)
        val off=road.distance>track.halfWidth
        var p=pos
        var v=if(off)(speed-36f*d).coerceIn(0f,55f) else speed
        // Soft shoulder: avoid lost cars off the map, without snapping them along the race.
        if(road.distance>track.halfWidth+35f){val dir=p-road.center;p=road.center+dir*((track.halfWidth+35f)/dir.length().coerceAtLeast(1f));v*=.72f}
        var progress=(old.progress+(road.progress-old.progress).coerceIn(-speed*d*1.6f-1f,speed*d*1.6f+1f)).coerceAtLeast(-track.length*.15f)
        val wrongWay=abs(wrappedDelta(angle-track.at(progress).second,2f*PI.toFloat()))>1.85f
        val elapsed=old.elapsed+d
        collisionCooldown=(collisionCooldown-d).coerceAtLeast(0f)
        var integrity=old.integrity
        var hurt=(old.hit-d).coerceAtLeast(0f)
        var message=old.message
        var opponents=old.rivals.mapIndexed{i,r->
            if(r.finish>0f)r else {
                val slowed=(r.slowed-d).coerceAtLeast(0f)
                val (_,heading)=track.at(r.progress)
                val (_,ahead)=track.at(r.progress+52f)
                val curve=abs(wrappedDelta(ahead-heading,2f*PI.toFloat()))
                val target=(91f+i*3.3f+track.level*.50f)*(1f-.28f*curve.coerceIn(0f,1f))*(if(slowed>0f).42f else 1f)
                var sp=(r.speed+(target-r.speed)*d*1.8f).coerceAtLeast(0f)
                val rp=track.at(r.progress,r.lane).first
                if(old.oils.any{(it.pos-rp).length()<17f})sp*=.96f
                val next=r.progress+sp*d
                Racer(next,r.lane,sp,slowed,if(next>=total)elapsed else 0f)
            }
        }
        if(collisionCooldown<=0f&&opponents.any{it.finish==0f&&(track.at(it.progress,it.lane).first-p).length()<18f}){
            v*=.72f;collisionCooldown=.8f
            if(old.shield<=0f){integrity=(integrity-7).coerceAtLeast(5);hurt=.45f;message="¡Contacto!"}
        }
        var item=old.item;var coins=old.collected
        for(i in boxPositions.indices){
            boxCooldown[i]=(boxCooldown[i]-d).coerceAtLeast(0f)
            if(mode==RallyMode.CHAOS&&item==null&&boxCooldown[i]==0f&&(track.at(boxPositions[i],0f).first-p).length()<25f){
                item=RallyItem.entries[random.nextInt(RallyItem.entries.size)];boxCooldown[i]=18f;message=item.label+" disponible"
            }
        }
        for(i in coinPositions.indices){
            coinCooldown[i]=(coinCooldown[i]-d).coerceAtLeast(0f)
            val coinLane = if (i % 2 == 0) -21f else 21f
            val pickupRadius = if (old.magnet > 0f) 62f else 20f
            val coinDistance = (track.at(coinPositions[i], coinLane).first - p).length()
            if (coinCooldown[i] == 0f && coinDistance < pickupRadius) {
                coins++;coinCooldown[i]=30f
            }
        }
        val finish=progress>=total
        if(finish)progress=total
        val place=1+opponents.count{it.progress>progress}
        val lap=(floor(progress.coerceAtLeast(0f)/track.length).toInt()+1).coerceAtMost(mode.laps)
        f=old.copy(pos=p,angle=angle,speed=v,progress=progress,elapsed=elapsed,place=place,lap=lap,rivals=opponents,
            boxes=if(mode==RallyMode.CHAOS)boxPositions.filterIndexed{i,_->boxCooldown[i]==0f}else emptyList(),
            coins=coinPositions.filterIndexed{i,_->coinCooldown[i]==0f},oils=old.oils.map{it.copy(life=it.life-d)}.filter{it.life>0f},
            item=item,nitro=(old.nitro-d).coerceAtLeast(0f),shield=(old.shield-d).coerceAtLeast(0f),
            magnet=(old.magnet-d).coerceAtLeast(0f),integrity=integrity,collected=coins,drift=drift,hit=hurt,
            pulse=(old.pulse-d).coerceAtLeast(0f),finished=finish,offroad=off,message=message,steering=turn,wrongWay=wrongWay)
        return f
    }
    fun score():Int=if(!f.finished)0 else (1100+(6-f.place)*220+f.collected*35+f.integrity*3+
        (1500-f.elapsed.toInt()*7).coerceAtLeast(0)).coerceAtMost(6000)
}
