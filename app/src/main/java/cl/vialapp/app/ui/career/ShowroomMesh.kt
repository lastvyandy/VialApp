package cl.vialapp.app.ui.career

import kotlin.math.*

/** Small, deterministic 3D mesh; x=width, y=height, z=length (nose +z). */
internal data class V3(val x:Float,val y:Float,val z:Float) {
    operator fun plus(b:V3)=V3(x+b.x,y+b.y,z+b.z)
    operator fun minus(b:V3)=V3(x-b.x,y-b.y,z-b.z)
    operator fun times(v:Float)=V3(x*v,y*v,z*v)
    fun dot(b:V3)=x*b.x+y*b.y+z*b.z
    fun cross(b:V3)=V3(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x)
    fun unit():V3 { val n=sqrt(dot(this)); return if(n<.0001f)this else this*(1f/n) }
}

internal enum class Finish { PAINT, GLASS, RUBBER, RIM, GRILLE, HEADLAMP, TAILLAMP, TRIM, CHROME, DECAL_LIGHT, DECAL_DARK, RIM_BLACK, RIM_GOLD }
internal data class CarFace(val points:List<V3>,val finish:Finish,val normal:V3,val center:V3)
internal data class CarMesh(val faces:List<CarFace>)
internal data class CarSpec(val length:Float,val width:Float,val belt:Float,val roof:Float,
    val cabinFront:Float,val cabinRear:Float,val roofFront:Float,val roofRear:Float,
    val wheelZ:Float,val tire:Float,val hood:Float,val tail:Float)

internal fun carSpec(style:String):CarSpec=when(style) {
    "hatch" -> CarSpec(1.84f,.89f,.91f,1.44f,.75f,-1.50f,.29f,-1.18f,1.16f,.35f,.83f,.91f)
    "sedan" -> CarSpec(2.23f,.95f,.94f,1.43f,.82f,-1.22f,.28f,-.68f,1.36f,.36f,.85f,.90f)
    "suv" -> CarSpec(2.15f,1.02f,1.08f,1.74f,.98f,-1.85f,.48f,-1.62f,1.34f,.42f,1.00f,1.04f)
    "sport" -> CarSpec(2.26f,1.04f,.85f,1.17f,.82f,-1.28f,.24f,-.56f,1.40f,.32f,.75f,.85f)
    else -> CarSpec(1.90f,.90f,.91f,1.39f,.69f,-1.39f,.18f,-1.00f,1.15f,.35f,.83f,.90f)
}

private fun mix(a:Float,b:Float,t:Float)=a+(b-a)*t
private fun face(points:List<V3>,finish:Finish,inside:V3):CarFace? {
    if(points.size<3)return null
    val center=points.reduce{a,b->a+b}*(1f/points.size)
    var sum=V3(0f,0f,0f)
    // Newell-style area-weighted normal, robust for narrow or non-planar panels.
    for(i in points.indices) sum=sum+(points[i]-center).cross(points[(i+1)%points.size]-center)
    val normal=sum.unit()
    if(normal.dot(normal)<.001f)return null
    return CarFace(points,finish,if(normal.dot(center-inside)<0f)normal*(-1f) else normal,center)
}
private class MeshBuilder {
    val faces=mutableListOf<CarFace>()
    fun panel(f:Finish,inside:V3,vararg p:V3) { face(p.toList(),f,inside)?.let{faces.add(it)} }
    fun ringBody(rings:List<List<V3>>,inside:V3,f:Finish) {
        for(i in 0 until rings.lastIndex) {
            val localInside=(rings[i]+rings[i+1]).reduce{a,b->a+b}*(1f/(rings[i].size+rings[i+1].size))
            for(j in rings[i].indices) {
                val k=(j+1)%rings[i].size
                panel(f,localInside,rings[i][j],rings[i][k],rings[i+1][k],rings[i+1][j])
            }
        }
        face(rings.first(),f,inside)?.let{faces.add(it)}
        face(rings.last(),f,inside)?.let{faces.add(it)}
    }
    fun box(x:Float,y:Float,z:Float,dx:Float,dy:Float,dz:Float,f:Finish) {
        val center=V3(x,y,z)
        val a=listOf(V3(x-dx,y-dy,z-dz),V3(x+dx,y-dy,z-dz),V3(x+dx,y+dy,z-dz),V3(x-dx,y+dy,z-dz))
        val b=a.map{it.copy(z=z+dz)}
        ringBody(listOf(a,b),center,f)
    }
}

private const val WHEEL_CLEARANCE=.085f
internal fun wheelArchY(s:CarSpec,z:Float):Float {
    val d=min(abs(z-s.wheelZ),abs(z+s.wheelZ))
    val r=s.tire+WHEEL_CLEARANCE
    return when{
        d<=r -> max(.27f,s.tire+sqrt((r*r-d*d).coerceAtLeast(0f)))
        d<r+.06f -> mix(s.tire,.27f,(d-r)/.06f)
        else -> .27f
    }
}
internal fun buildCarMesh(style:String, kit: GarageKit = GarageKit()):CarMesh {
    val s=carSpec(style)
    val b=MeshBuilder()
    val inside=V3(0f,s.belt*.66f,0f)
    val front=s.length
    val rear=-s.length
    // The outer shell has actual open wheel arches. The central underbody is
    // narrower than the inner tire face, so no wheel intersects a solid body slab.
    val zValues=buildList<Float>{
        addAll(listOf(rear,rear+.12f,rear+.30f,s.cabinRear,0f,s.cabinFront,front-.30f,front-.12f,front))
        for(axle in listOf(-s.wheelZ,s.wheelZ)){
            val radius=s.tire+WHEEL_CLEARANCE
            add(axle-radius-.06f);add(axle+radius+.06f)
            for(i in 0..18) add(axle+radius*cos(i*PI/18).toFloat())
        }
    }.map{it.coerceIn(rear,front)}.distinct().sorted()
    val rearShoulder=min(s.cabinRear,-s.wheelZ)
    val frontShoulder=max(s.cabinFront,s.wheelZ)
    fun smooth(t:Float)=t.coerceIn(0f,1f).let{it*it*(3f-2f*it)}
    fun baseTop(z:Float):Float=when {
        z < rearShoulder -> mix(s.tail-.06f,s.belt,smooth((z-rear)/(rearShoulder-rear).coerceAtLeast(.1f)))
        z > frontShoulder -> mix(s.belt,s.hood-.06f,smooth((z-frontShoulder)/(front-frontShoulder)))
        else -> s.belt
    }
    fun top(z:Float)=max(baseTop(z),wheelArchY(s,z)+.105f)
    fun ring(z:Float):List<V3>{
        val end=((abs(z)-(s.length-.33f)).coerceAtLeast(0f)/.33f).coerceAtMost(1f)
        val w=s.width*(1f-.13f*end)
        val arch=wheelArchY(s,z)
        val y=top(z)
        // Fender crowns rise above the tire; do not clamp the hole into the tire.
        // A flatter, broad hood and sculpted shoulders give a cleaner silhouette.
        return listOf(V3(-w*.86f,arch,z),V3(-w,max(arch+.027f,y-.20f),z),
            V3(-w*.965f,y-.038f,z),V3(-w*.74f,y,z),V3(w*.74f,y,z),
            V3(w*.965f,y-.038f,z),V3(w,max(arch+.027f,y-.20f),z),V3(w*.86f,arch,z))
    }
    b.box(0f,.30f,0f,s.width*.57f,.145f,s.length-.16f,Finish.TRIM)
    b.ringBody(zValues.map(::ring),inside,Finish.PAINT)

    // Glasshouse: a broad roof, raked windshield and distinct rear hatch/sedan profile.
    val bottom=min(top(s.cabinFront),top(s.cabinRear))-.018f
    val lowW=s.width*.83f
    val highW=s.width*(if(style=="suv").75f else .62f)
    val lf=V3(-lowW,bottom,s.cabinFront)
    val rf=V3(lowW,bottom,s.cabinFront)
    val lr=V3(-lowW,bottom,s.cabinRear)
    val rr=V3(lowW,bottom,s.cabinRear)
    val tlf=V3(-highW,s.roof,s.roofFront)
    val trf=V3(highW,s.roof,s.roofFront)
    val tlr=V3(-highW,s.roof-.015f,s.roofRear)
    val trr=V3(highW,s.roof-.015f,s.roofRear)
    val cabin=V3(0f,(bottom+s.roof)/2f,(s.cabinFront+s.cabinRear)/2f)
    b.panel(Finish.PAINT,cabin,tlf,trf,trr,tlr)
    fun glass(a:V3,c:V3,d:V3,e:V3){
        val mid=(a+c+d+e)*.25f
        // Narrow painted surround; four strips are actual geometry, not a flat decal.
        val inset=listOf(a,c,d,e).map{it+(mid-it)*.12f}
        val outer=listOf(a,c,d,e)
        for(i in 0..3){val j=(i+1)%4;b.panel(Finish.PAINT,cabin,outer[i],outer[j],inset[j],inset[i])}
        b.panel(Finish.GLASS,cabin,*inset.toTypedArray())
    }
    glass(lf,rf,trf,tlf)
    glass(rr,lr,tlr,trr)
    for(side in listOf(-1f,1f)) {
        val a=if(side<0)lf else rf; val c=if(side<0)lr else rr
        val d=if(side<0)tlr else trr; val e=if(side<0)tlf else trf
        val t=if(style=="sport").53f else .47f
        val m1=a+(c-a)*t; val m2=e+(d-e)*t
        glass(a,m1,m2,e);glass(m1,c,d,m2)
        // Mirrors and a bright sill/door-handle accent.
        b.box(side*s.width*.99f,bottom+.12f,s.cabinFront-.09f,.15f,.065f,.13f,Finish.PAINT)
        b.box(side*(s.width+.006f),bottom-.12f,-.12f,.014f,.025f,.15f,Finish.CHROME)
        if(style=="sedan"||style=="suv")
            b.box(side*(s.width+.006f),bottom-.12f,-.86f,.014f,.025f,.13f,Finish.CHROME)
    }

    // Separate recessed fascia and two matching lens assemblies. Each visible
    // lens is a single bevelled polygon with positive distance from the body.
    // Never place coplanar light polygons on the bumper or across each other.
    val noseW=s.width*.82f
    val grilleY=if(style=="suv").50f else .43f
    b.box(0f,grilleY,front+.020f,noseW*.42f,.105f,.020f,Finish.GRILLE)
    for(i in 0..2) b.box(0f,grilleY-.06f+i*.06f,front+.048f,noseW*.35f,.006f,.005f,Finish.CHROME)
    fun lens(x:Float,y:Float,end:Float,finish:Finish){
        val direction=if(end>0f)1f else -1f
        fun outline(depth:Float,hw:Float,hh:Float):List<V3>{
            val zz=end+direction*depth
            val d=hw*.22f
            return listOf(V3(x-hw+d,y+hh,zz),V3(x+hw-d,y+hh,zz),
                V3(x+hw,y+hh*.36f,zz),V3(x+hw-d,y-hh,zz),
                V3(x-hw+d,y-hh,zz),V3(x-hw,y-hh*.36f,zz))
        }
        val housing=V3(x,y,end+direction*.012f)
        b.ringBody(listOf(outline(-.009f,noseW*.247f,.080f),outline(.032f,noseW*.238f,.074f)),housing,Finish.GRILLE)
        b.panel(finish,inside,*outline(.044f,noseW*.213f,.057f).toTypedArray())
    }
    for(side in listOf(-1f,1f)){
        val x=side*noseW*.69f
        lens(x,top(front)-.12f,front,Finish.HEADLAMP)
        lens(x,top(rear)-.12f,rear,Finish.TAILLAMP)
        b.box(x,.35f,front+.016f,noseW*.145f,.032f,.017f,Finish.GRILLE)
    }
    b.box(0f,.44f,rear-.018f,noseW*.31f,.065f,.018f,Finish.TRIM)
    if(style=="sport" && kit.wing=="wing_stock"){
        for(side in listOf(-1f,1f)) b.box(side*.61f,s.tail+.07f,rear+.23f,.045f,.12f,.065f,Finish.TRIM)
        b.box(0f,s.tail+.19f,rear+.23f,s.width*.94f,.045f,.17f,Finish.PAINT)
    }
    if(kit.wing!="wing_stock"){
        val gt=kit.wing=="wing_gt"
        val y=s.tail+(if(gt).34f else .13f)
        for(side in listOf(-1f,1f))
            b.box(side*s.width*.61f,(s.tail+y)*.5f,rear+.24f,.045f,(y-s.tail)*.5f,.060f,Finish.TRIM)
        b.box(0f,y,rear+.22f,s.width*.95f,.045f,if(gt).22f else .12f,Finish.TRIM)
        if(gt)for(side in listOf(-1f,1f))
            b.box(side*s.width*.94f,y+.025f,rear+.22f,.035f,.095f,.22f,Finish.CHROME)
    }
    if(kit.decal!="decal_stock"){
        val finish=if(kit.decal=="decal_rally")Finish.DECAL_LIGHT else Finish.DECAL_DARK
        val stripes=if(kit.decal=="decal_rally")listOf(-.16f to .11f,.16f to .11f) else listOf(0f to .22f)
        for((x,w) in stripes){
            val roofZ1=s.roofRear+.035f; val roofZ2=s.roofFront-.035f
            fun roofY(z:Float)=mix(s.roof-.015f,s.roof,(z-s.roofRear)/(s.roofFront-s.roofRear))+.012f
            b.panel(finish,cabin,V3(x-w,roofY(roofZ1),roofZ1),V3(x+w,roofY(roofZ1),roofZ1),
                V3(x+w,roofY(roofZ2),roofZ2),V3(x-w,roofY(roofZ2),roofZ2))
            for(i in 0 until zValues.lastIndex){
                val a=zValues[i];val z=zValues[i+1]
                if(a>=s.cabinFront || z<=s.cabinRear){
                    b.panel(finish,inside,V3(x-w,top(a)+.008f,a),V3(x+w,top(a)+.008f,a),
                        V3(x+w,top(z)+.008f,z),V3(x-w,top(z)+.008f,z))
                }
            }
        }
    }
    if(style=="suv")for(side in listOf(-1f,1f))
        b.box(side*highW*.82f,s.roof+.04f,(s.roofFront+s.roofRear)*.5f,.04f,.04f,(s.roofFront-s.roofRear)*.42f,Finish.TRIM)

    val rim=when(kit.wheels){"wheels_black"->Finish.RIM_BLACK;"wheels_gold"->Finish.RIM_GOLD;else->Finish.RIM}
    // Four tires, machined rims and radial spokes, all in the same 3D depth pass.
    for(side in listOf(-1f,1f))for(z in listOf(-s.wheelZ,s.wheelZ)){
        val innerX=side*s.width*.65f
        val outerX=side*s.width*.965f
        val center=V3((innerX+outerX)*.5f,s.tire,z)
        val radius=s.tire
        fun wheelPoint(x:Float,r:Float,i:Int):V3 {
            val a=i*2.0*PI/24.0
            return V3(x,center.y+r*cos(a).toFloat(),z+r*sin(a).toFloat())
        }
        for(i in 0 until 24){
            val j=(i+1)%24
            b.panel(Finish.RUBBER,center,wheelPoint(innerX,radius,i),wheelPoint(innerX,radius,j),wheelPoint(outerX,radius,j),wheelPoint(outerX,radius,i))
            b.panel(Finish.RUBBER,center,wheelPoint(outerX,radius,i),wheelPoint(outerX,radius,j),wheelPoint(outerX,radius*.72f,j),wheelPoint(outerX,radius*.72f,i))
            b.panel(Finish.RUBBER,center,wheelPoint(innerX,radius,i),wheelPoint(innerX,radius,j),V3(innerX,center.y,z))
            b.panel(Finish.GRILLE,center,wheelPoint(outerX+side*.004f,radius*.72f,i),wheelPoint(outerX+side*.004f,radius*.72f,j),V3(outerX+side*.004f,center.y,z))
            b.panel(rim,center,wheelPoint(outerX+side*.011f,radius*.72f,i),wheelPoint(outerX+side*.011f,radius*.72f,j),wheelPoint(outerX+side*.011f,radius*.61f,j),wheelPoint(outerX+side*.011f,radius*.61f,i))
        }
        val spokes=if(style=="sport")6 else 5
        val x=outerX+side*.018f
        for(i in 0 until spokes){
            val angle=i*2.0*PI/spokes
            fun spoke(r:Float,a:Double)=V3(x,center.y+r*cos(a).toFloat(),z+r*sin(a).toFloat())
            b.panel(rim,center,spoke(radius*.16f,angle-.30),spoke(radius*.63f,angle-.11),
                spoke(radius*.63f,angle+.11),spoke(radius*.16f,angle+.30))
        }
        for(i in 0 until 12){
            fun hub(j:Int):V3 {val a=j*2.0*PI/12;return V3(x+side*.006f,center.y+radius*.21f*cos(a).toFloat(),z+radius*.21f*sin(a).toFloat())}
            b.panel(Finish.CHROME,center,V3(x+side*.006f,center.y,z),hub(i),hub(i+1))
        }
    }
    return CarMesh(b.faces.toList())
}

internal data class ScreenPoint(val x:Float,val y:Float,val depth:Float)
internal class CarCamera(azimuth:Float,elevation:Float=18f){
    private val a=azimuth*PI/180.0
    private val e=elevation*PI/180.0
    private val right=V3(cos(a).toFloat(),0f,-sin(a).toFloat())
    private val forward=V3((sin(a)*cos(e)).toFloat(),sin(e).toFloat(),(cos(a)*cos(e)).toFloat())
    private val up=forward.cross(right).unit()
    private val target=V3(0f,.72f,0f)
    fun project(p:V3):ScreenPoint {
        val v=p-target
        val depth=v.dot(forward)
        val perspective=9f/(9f-depth).coerceAtLeast(2f)
        return ScreenPoint(v.dot(right)*perspective,-v.dot(up)*perspective,depth)
    }
    fun visible(face:CarFace)=face.normal.dot(forward*9f+target-face.center)>0f
}
