package cl.vialapp.app.ui.games.rally

import androidx.compose.ui.graphics.Color
import cl.vialapp.app.ui.career.GarageKit
import cl.vialapp.app.ui.career.V3
import cl.vialapp.app.ui.career.buildCarMesh
import cl.vialapp.app.ui.career.surfaceColor
import kotlin.math.*
import kotlin.random.Random

/** All coordinates are real world vertices (x, up, z); no sprite billboards. */
internal data class RallyPalette(val sky:Color,val fog:Color,val ground:Color,val shoulder:Color,val road:Color,
    val curb:Color,val accent:Color,val rock:Color)
internal fun rallyPalette(t:RallyTrack)=when(t.terrain){
    0->RallyPalette(Color(0xFFFFC890),Color(0xFFE8AE79),Color(0xFFBB8A59),Color(0xFF98714D),Color(0xFF57565A),Color(0xFFFFDBA4),Color(0xFFC55C40),Color(0xFF9D674A))
    1->RallyPalette(Color(0xFF91C9E1),Color(0xFFACC3C8),Color(0xFF839A8B),Color(0xFFC0B2A3),Color(0xFF454D57),Color(0xFFF2E5CA),Color(0xFFD87962),Color(0xFF716A69))
    2->RallyPalette(Color(0xFFA2CAD5),Color(0xFF9AB3A3),Color(0xFF50774B),Color(0xFF957D5F),Color(0xFF6D5B47),Color(0xFFF3E4BC),Color(0xFF2A6344),Color(0xFF6B8063))
    3->RallyPalette(Color(0xFF92D3EB),Color(0xFFABCED4),Color(0xFFDBC49C),Color(0xFFB4A186),Color(0xFF495764),Color(0xFFFFE4BF),Color(0xFF428F83),Color(0xFFA28A72))
    4->RallyPalette(Color(0xFF121A37),Color(0xFF283555),Color(0xFF27334A),Color(0xFF39485F),Color(0xFF303846),Color(0xFF80DCFA),Color(0xFFAE86E6),Color(0xFF495B86))
    5->RallyPalette(Color(0xFFA9CDE9),Color(0xFFC7D9E4),Color(0xFFDCE7EF),Color(0xFFA4B7C9),Color(0xFF6E8294),Color(0xFFF8F9FF),Color(0xFF5C8EBD),Color(0xFF899CB1))
    6->RallyPalette(Color(0xFFC97F73),Color(0xFF986B6B),Color(0xFF514650),Color(0xFF805E50),Color(0xFF46464B),Color(0xFFFFBE82),Color(0xFFE58547),Color(0xFF72616B))
    else->RallyPalette(Color(0xFFA7CEDC),Color(0xFFB0C7C9),Color(0xFF73947B),Color(0xFFA4B4A0),Color(0xFF55635E),Color(0xFFE9E9D9),Color(0xFF477F69),Color(0xFF758A85))
}

/** Interleaved XYZ/RGB. Allocate only when building the scene or a vehicle. */
internal class RallyMeshBuilder{
    private var data=FloatArray(4096)
    private var used=0
    fun vertex(v:V3,c:Color){
        if(used+6>data.size)data=data.copyOf(data.size*2)
        data[used++]=v.x;data[used++]=v.y;data[used++]=v.z
        data[used++]=c.red;data[used++]=c.green;data[used++]=c.blue
    }
    fun tri(a:V3,b:V3,c:V3,color:Color){vertex(a,color);vertex(b,color);vertex(c,color)}
    fun quad(a:V3,b:V3,c:V3,d:V3,color:Color){tri(a,b,c,color);tri(a,c,d,color)}
    fun finish()=data.copyOf(used)
    fun shade(c:Color,f:Float)=Color((c.red*f).coerceIn(0f,1f),(c.green*f).coerceIn(0f,1f),(c.blue*f).coerceIn(0f,1f))
    fun box(x:Float,y:Float,z:Float,hx:Float,h:Float,hz:Float,c:Color){
        val a=V3(x-hx,y,z-hz);val b=V3(x+hx,y,z-hz);val d=V3(x-hx,y,z+hz);val e=V3(x+hx,y,z+hz)
        val up=V3(0f,h,0f)
        quad(a,b,b+up,a+up,shade(c,.73f));quad(b,e,e+up,b+up,shade(c,.88f))
        quad(e,d,d+up,e+up,shade(c,.60f));quad(d,a,a+up,d+up,shade(c,.94f))
        quad(a+up,b+up,e+up,d+up,c)
    }
    fun cone(x:Float,y:Float,z:Float,r:Float,h:Float,c:Color,sides:Int=6){
        val peak=V3(x,y+h,z)
        for(i in 0 until sides){
            val a=i*(2f*PI.toFloat()/sides);val b=(i+1)*(2f*PI.toFloat()/sides)
            val p=V3(x+cos(a)*r,y,z+sin(a)*r);val q=V3(x+cos(b)*r,y,z+sin(b)*r)
            tri(p,q,peak,shade(c,.74f+.24f*(cos(a-.7f)+1f)/2f))
        }
    }
    fun disc(x:Float,y:Float,z:Float,r:Float,c:Color,sides:Int=16){
        for(i in 0 until sides){val a=i*2f*PI.toFloat()/sides;val b=(i+1)*2f*PI.toFloat()/sides
            tri(V3(x,y,z),V3(x+r*cos(a),y,z+r*sin(a)),V3(x+r*cos(b),y,z+r*sin(b)),c)}
    }
}
private fun V2.v(h:Float)=V3(x,h,y)
internal fun makeRallyWorld(track:RallyTrack,p:RallyPalette):FloatArray{
    val m=RallyMeshBuilder()
    val minX=track.nodes.minOf{it.x};val maxX=track.nodes.maxOf{it.x}
    val minZ=track.nodes.minOf{it.y};val maxZ=track.nodes.maxOf{it.y}
    val midX=(minX+maxX)/2;val midZ=(minZ+maxZ)/2
    m.quad(V3(minX-1900f,-.8f,minZ-1900f),V3(maxX+1900f,-.8f,minZ-1900f),V3(maxX+1900f,-.8f,maxZ+1900f),V3(minX-1900f,-.8f,maxZ+1900f),p.ground)
    val slices=ceil(track.length/7f).toInt()
    fun strip(s:Float,e:Float,l:Float,r:Float,y:Float,color:Color){
        m.quad(track.at(s,l).first.v(y),track.at(s,r).first.v(y),track.at(e,r).first.v(y),track.at(e,l).first.v(y),color)
    }
    for(i in 0 until slices){
        val s=i*track.length/slices;val e=(i+1)*track.length/slices
        val w=track.halfWidth
        strip(s,e,-w-9,w+9,-.28f,p.shoulder)
        strip(s,e,-w,w,0f,if(i%2==0)p.road else m.shade(p.road,.985f))
        for(side in listOf(-1f,1f)){
            val l=side*(w-2.6f);val r=side*(w+1.0f)
            strip(s,e,l,r,.17f,if(i/3%2==0)p.curb else p.accent)
        }
        if(i%6<3)strip(s,e,-.65f,.65f,.09f,m.shade(p.curb,.81f))
        // Guardrail posts in technical sections provide speed reference without invisible obstacles.
        if(i%5==0){
            val curve=wrappedDelta(track.at(s+40f).second-track.at(s-20f).second,2f*PI.toFloat())
            if(abs(curve)>.30f){
                val side=if(curve>0f)-1f else 1f
                val q=track.at(s,side*(w+10f)).first
                m.box(q.x,0f,q.y,1.5f,6f,1.5f,p.curb)
                val q2=track.at(e+24f,side*(w+10f)).first
                m.quad(q.v(3.7f),q2.v(3.7f),q2.v(6.2f),q.v(6.2f),p.rock)
            }
        }
    }
    // Turbo strips on straights; the same world coordinates drive the physics.
    for(pad in rallyPads(track)){
        val left=pad.lane-14f;val right=pad.lane+14f
        strip(pad.progress-12f,pad.progress+12f,left,right,.22f,Color(0xFF197899))
        for(k in 0..2){
            val z=pad.progress-8f+k*7f
            val a=track.at(z,left+2f).first.v(.29f)
            val b=track.at(z+4f,pad.lane).first.v(.29f)
            val c=track.at(z,right-2f).first.v(.29f)
            val d=track.at(z+4f,right-2f).first.v(.29f)
            val e=track.at(z+8f,pad.lane).first.v(.29f)
            val f=track.at(z+4f,left+2f).first.v(.29f)
            m.quad(a,b,e,f,Color(0xFF8DF9F4));m.quad(b,c,d,e,Color(0xFF8DF9F4))
        }
    }
    // Wedge ramps with solid side faces, contrasting landing arrows and a real collision profile.
    for(ramp in rallyRamps(track)){
        val s=ramp.progress-ramp.run
        val e=ramp.progress
        val l=ramp.lane-ramp.halfWidth;val r=ramp.lane+ramp.halfWidth
        val a=track.at(s,l).first;val b=track.at(s,r).first
        val c=track.at(e,r).first;val d=track.at(e,l).first
        val h=ramp.height
        m.quad(a.v(.25f),b.v(.25f),c.v(h),d.v(h),Color(0xFFE9A63C))
        m.tri(a.v(.18f),d.v(.18f),d.v(h),Color(0xFF744528))
        m.tri(b.v(.18f),c.v(h),c.v(.18f),Color(0xFF9D5A2B))
        m.quad(d.v(.18f),c.v(.18f),c.v(h),d.v(h),Color(0xFF78482B))
        fun top(at:Float,lane:Float)=track.at(at,lane).first.v(h*((at-s)/ramp.run)+.32f)
        for(k in 0..2){
            val z=s+5f+k*8f
            m.quad(top(z,l+2f),top(z,r-2f),top(z+3f,r-2f),top(z+3f,l+2f),Color(0xFF34313A))
        }
        for(side in listOf(-1f,1f)){
            val q=track.at(s, ramp.lane+side*(ramp.halfWidth+2f)).first
            m.cone(q.x,0f,q.y,2.4f,5f,Color(0xFFFF7951),6)
        }
    }
    // Checkerboard starting line, spanning the actual road.
    for(i in 0 until 12)for(j in 0..1){
        val l=-track.halfWidth+2*track.halfWidth*i/12f
        val r=-track.halfWidth+2*track.halfWidth*(i+1)/12f
        strip(-5f+j*4,-1f+j*4,l,r,.24f,if((i+j)%2==0)Color.White else Color(0xFF202B35))
    }
    // Start gantry: side posts outside the road, an overhead beam above the cars.
    val start=track.at(0f)
    for(side in listOf(-1f,1f)){
        val q=track.at(0f,side*(track.halfWidth+13)).first
        m.box(q.x,0f,q.y,3.5f,33f,3.5f,p.accent)
    }
    val left=track.at(0f,-track.halfWidth-13f).first
    val right=track.at(0f,track.halfWidth+13f).first
    val forward=V2(cos(start.second),sin(start.second))*4f
    m.quad((left-forward).v(29f),(right-forward).v(29f),(right-forward).v(35f),(left-forward).v(35f),p.accent)
    m.quad((left+forward).v(29f),(right+forward).v(29f),(right+forward).v(35f),(left+forward).v(35f),p.accent)
    m.quad((left-forward).v(35f),(right-forward).v(35f),(right+forward).v(35f),(left+forward).v(35f),p.curb)
    val rnd=Random(track.id.hashCode())
    // Distinct scenery per circuit, placed behind the shoulder and outside every road segment.
    for(i in 0 until 180){
        val s=track.length*i/180f
        val side=if(i%2==0)1f else -1f
        val q=track.at(s,side*(track.halfWidth+36f+rnd.nextFloat()*115f)).first
        val radius=7f+rnd.nextFloat()*12f
        if(track.nodes.any{(it-q).length()<track.halfWidth+radius+13f})continue
        when(track.terrain){
            1,4->{
                val h=14f+rnd.nextFloat()*(if(track.terrain==4)65f else 23f)
                val c=if(i%3==0)p.accent else if(i%3==1)p.rock else m.shade(p.curb,.78f)
                m.box(q.x,0f,q.y,radius,h,radius*.8f,c)
                for(k in 1..(h/9).toInt()){
                    for(j in -1..1){
                        m.box(q.x+j*radius*.48f,k*8f-3f,q.y-radius*.8f-.2f,radius*.12f,3.6f,.35f,if(track.terrain==4)Color(0xFFFFD48C)else Color(0xFF344759))
                    }
                }
                if(track.terrain==1)m.cone(q.x,h,q.y,radius*1.20f,8f,p.rock,4)
            }
            2,7->{
                m.box(q.x,0f,q.y,1.6f,12f,1.6f,Color(0xFF69503A))
                val h=19f+rnd.nextFloat()*19f
                m.cone(q.x,7f,q.y,radius,h,p.accent)
                m.cone(q.x,15f,q.y,radius*.70f,h*.75f,m.shade(p.accent,1.18f))
            }
            3->{
                m.box(q.x,0f,q.y,1.4f,24f,1.4f,Color(0xFF88674D))
                m.cone(q.x,21f,q.y,radius*.85f,9f,p.accent,6)
            }
            else->{
                m.cone(q.x,0f,q.y,radius,9f+rnd.nextFloat()*24f,p.rock,5)
                if(track.terrain==5)m.cone(q.x,8f,q.y,radius*.62f,15f,p.curb,5)
            }
        }
    }
    // Large silhouettes at the horizon, with snow caps and a coastal color plane.
    for(i in 0 until 26){
        val a=i*2f*PI.toFloat()/26f
        val r=660f+rnd.nextFloat()*180f
        val x=midX+cos(a)*r;val z=midZ+sin(a)*r
        val h=90f+rnd.nextFloat()*180f
        if(track.terrain==3&&sin(a)>.25f)continue
        m.cone(x,-.5f,z,130f+rnd.nextFloat()*95f,h,m.shade(p.rock,.89f),5)
        if(track.terrain==5||track.terrain==7||track.terrain==6&&i%4==0)m.cone(x,h*.68f,z,55f,h*.38f,if(track.terrain==6)Color(0xFFC48367)else Color(0xFFE4ECF1),5)
    }
    if(track.terrain==3)m.quad(V3(minX-1800f,-.6f,maxZ+100f),V3(maxX+1800f,-.6f,maxZ+100f),V3(maxX+1800f,-.6f,maxZ+1800f),V3(minX-1800f,-.6f,maxZ+1800f),Color(0xFF4799B0))
    return m.finish()
}
internal fun makeRallyCar(style:String,paint:Color,kit:GarageKit):FloatArray{
    val m=RallyMeshBuilder()
    val mesh=buildCarMesh(style,kit)
    for(face in mesh.faces){
        val points=face.points
        val ink=surfaceColor(face,paint)
        for(i in 1 until points.lastIndex)m.tri(points[0],points[i],points[i+1],ink)
    }
    return m.finish()
}
internal fun makePickup(isBox:Boolean,item:RallyItem=RallyItem.NITRO):FloatArray{
    val m=RallyMeshBuilder()
    if(isBox){
        val c=when(item){
            RallyItem.NITRO->Color(0xFF168BCF)
            RallyItem.SHIELD->Color(0xFF35B996)
            RallyItem.OIL->Color(0xFF695846)
            RallyItem.MAGNET->Color(0xFFE29B31)
            RallyItem.REPAIR->Color(0xFFDB4D5A)
            RallyItem.EMP->Color(0xFF9459D1)
        }
        val hx=if(item==RallyItem.OIL)4.3f else 5f
        m.box(0f,-4.5f,0f,hx,10f,5f,c)
        m.box(0f,4.8f,0f,hx+.4f,1f,5.4f,m.shade(c,1.3f))
        val white=Color(0xFFFFF5DC)
        for(side in listOf(-1f,1f)){
            val z=side*5.18f
            fun bar(x:Float,y:Float,width:Float,height:Float)=m.box(x,y,z,width,height,.24f,white)
            when(item){
                RallyItem.REPAIR->{bar(0f,-.85f,3.4f,1.7f);bar(0f,-3.3f,.9f,6.6f)}
                RallyItem.NITRO->{for(k in 0..2)bar(-2.1f+k*2.1f,-3f+k*.7f,.65f,5f-k*.8f)}
                RallyItem.SHIELD->{bar(0f,2.1f,3.2f,1f);bar(-2.6f,-1.7f,.65f,4.5f);bar(2.6f,-1.7f,.65f,4.5f);bar(0f,-2.8f,2.1f,1f)}
                RallyItem.OIL->{bar(0f,-2.9f,2.5f,4.3f);bar(0f,1.1f,1.3f,2.2f)}
                RallyItem.MAGNET->{bar(-2.2f,-2.5f,.85f,5.8f);bar(2.2f,-2.5f,.85f,5.8f);bar(0f,-3f,3f,1.5f)}
                RallyItem.EMP->{bar(0f,-.6f,3.4f,1.2f);bar(0f,-3.2f,.7f,6.4f);bar(-2.3f,1.5f,.55f,1.1f);bar(2.3f,-2.6f,.55f,1.1f)}
            }
        }
    }else{
        m.box(0f,-4f,0f,3.5f,8f,1.0f,Color(0xFFD99B27))
        m.box(0f,-2.8f,0f,2.6f,5.6f,1.2f,Color(0xFFFFD75B))
    }
    return m.finish()
}
