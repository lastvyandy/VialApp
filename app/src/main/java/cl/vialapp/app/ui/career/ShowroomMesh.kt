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

internal enum class Finish { PAINT, GLASS, RUBBER, RIM, GRILLE, HEADLAMP, TAILLAMP, TRIM, CHROME }
internal data class CarFace(val points:List<V3>,val finish:Finish,val normal:V3,val center:V3)
internal data class CarMesh(val faces:List<CarFace>)
internal data class CarSpec(val length:Float,val width:Float,val belt:Float,val roof:Float,
    val cabinFront:Float,val cabinRear:Float,val roofFront:Float,val roofRear:Float,
    val wheelZ:Float,val tire:Float,val hood:Float,val tail:Float)

internal fun carSpec(style:String):CarSpec=when(style) {
    "hatch" -> CarSpec(1.84f,.89f,.80f,1.42f,.75f,-1.50f,.29f,-1.18f,1.16f,.38f,.72f,.82f)
    "sedan" -> CarSpec(2.23f,.95f,.82f,1.39f,.82f,-1.22f,.28f,-.68f,1.36f,.39f,.76f,.78f)
    "suv" -> CarSpec(2.15f,1.02f,1.02f,1.72f,.98f,-1.85f,.48f,-1.62f,1.34f,.46f,.94f,1.03f)
    "sport" -> CarSpec(2.26f,1.04f,.66f,1.13f,.82f,-1.28f,.24f,-.56f,1.40f,.40f,.59f,.83f)
    else -> CarSpec(1.90f,.90f,.78f,1.34f,.69f,-1.39f,.18f,-1.00f,1.15f,.37f,.71f,.79f)
}

private fun mix(a:Float,b:Float,t:Float)=a+(b-a)*t
private fun face(points:List<V3>,finish:Finish,inside:V3):CarFace? {
    if(points.size<3)return null
    val center=points.reduce{a,b->a+b}*(1f/points.size)
    var normal=(points[1]-points[0]).cross(points[2]-points[0]).unit()
    // Some narrow panels have nearly collinear leading vertices.
    if(normal.dot(normal)<.001f) normal=(points[1]-points[0]).cross(points.last()-points[0]).unit()
    if(normal.dot(normal)<.001f)return null
    return CarFace(points,finish,if(normal.dot(center-inside)<0f)normal*(-1f) else normal,center)
}
private class MeshBuilder {
    val faces=mutableListOf<CarFace>()
    fun panel(f:Finish,inside:V3,vararg p:V3) { face(p.toList(),f,inside)?.let{faces.add(it)} }
    fun ringBody(rings:List<List<V3>>,inside:V3,f:Finish) {
        for(i in 0 until rings.lastIndex) {
            for(j in rings[i].indices) {
                val k=(j+1)%rings[i].size
                panel(f,inside,rings[i][j],rings[i][k],rings[i+1][k],rings[i+1][j])
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

internal fun buildCarMesh(style:String):CarMesh {
    val s=carSpec(style)
    val b=MeshBuilder()
    val inside=V3(0f,s.belt*.66f,0f)
    val front=s.length
    val rear=-s.length
    // Rounded cross-section rings. Four intermediate rings outline each wheel opening,
    // rather than hiding the wheels underneath a rectangular flat body.
    val zValues=mutableListOf(rear,rear+.14f,rear+.32f,-s.wheelZ-.55f,-s.wheelZ-.32f,
        -s.wheelZ,-s.wheelZ+.32f,-s.wheelZ+.55f,s.cabinRear,0f,s.cabinFront,
        s.wheelZ-.55f,s.wheelZ-.32f,s.wheelZ,s.wheelZ+.32f,s.wheelZ+.55f,front-.32f,front-.14f,front)
        .map{it.coerceIn(rear,front)}.distinct().sorted()
    fun top(z:Float):Float=when {
        z < s.cabinRear -> mix(s.tail-.09f,s.belt,(z-rear)/(s.cabinRear-rear).coerceAtLeast(.1f))
        z > s.cabinFront -> mix(s.belt,s.hood-.10f,(z-s.cabinFront)/(front-s.cabinFront))
        else -> s.belt
    }
    fun ring(z:Float):List<V3>{
        val end=(abs(z)-(s.length-.33f)).coerceAtLeast(0f)/.33f
        val w=s.width*(1f-.13f*end)
        val arcDistance=min(abs(z-s.wheelZ),abs(z+s.wheelZ))
        val arcRadius=s.tire+.055f
        val lower=if(arcDistance<arcRadius) max(.27f,s.tire+sqrt(arcRadius*arcRadius-arcDistance*arcDistance)) else .27f
        val y=top(z)
        // Arch top never protrudes through hood; outer sculpted shoulder remains visible.
        val arch=lower.coerceAtMost(y-.065f)
        return listOf(V3(-w*.86f,arch,z),V3(-w, max(arch,y-.20f),z),
            V3(-w*.94f,y-.045f,z),V3(-w*.76f,y,z),V3(w*.76f,y,z),
            V3(w*.94f,y-.045f,z),V3(w,max(arch,y-.20f),z),V3(w*.86f,arch,z))
    }
    b.ringBody(zValues.map(::ring),inside,Finish.PAINT)

    // Glasshouse: a broad roof, raked windshield and distinct rear hatch/sedan profile.
    val bottom=s.belt-.018f
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

    // Front fascia sits directly on the nose: grille, air intakes and LED lamp lenses.
    // A recessed dark base avoids the flat toy-like all-painted bumper.
    val noseW=s.width*.82f
    val grilleY=if(style=="suv").50f else .43f
    b.box(0f,grilleY,front+.016f,noseW*.44f,.12f,.022f,Finish.GRILLE)
    for(i in 0..2) b.box(0f,grilleY-.07f+i*.07f,front+.044f,noseW*.36f,.008f,.008f,Finish.CHROME)
    for(side in listOf(-1f,1f)){
        val x=side*noseW*.69f
        val y=top(front)-.12f
        b.box(x,y,front+.022f,noseW*.23f,.062f,.026f,Finish.HEADLAMP)
        b.box(x,.35f,front+.02f,noseW*.16f,.042f,.016f,Finish.GRILLE)
        b.box(x,top(rear)-.12f,rear-.026f,noseW*.23f,.064f,.026f,Finish.TAILLAMP)
    }
    b.box(0f,.44f,rear-.018f,noseW*.34f,.08f,.018f,Finish.TRIM)
    if(style=="sport"){
        for(side in listOf(-1f,1f)) b.box(side*.61f,s.tail+.07f,rear+.23f,.045f,.12f,.065f,Finish.TRIM)
        b.box(0f,s.tail+.19f,rear+.23f,s.width*.94f,.045f,.17f,Finish.PAINT)
    }
    if(style=="suv")for(side in listOf(-1f,1f))
        b.box(side*highW*.82f,s.roof+.04f,(s.roofFront+s.roofRear)*.5f,.04f,.04f,(s.roofFront-s.roofRear)*.42f,Finish.TRIM)

    // Four tires, machined rims and radial spokes, all in the same 3D depth pass.
    for(side in listOf(-1f,1f))for(z in listOf(-s.wheelZ,s.wheelZ)){
        val center=V3(side*(s.width-.065f),s.tire,z)
        val innerX=center.x-side*.12f
        val outerX=center.x+side*.15f
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
            b.panel(Finish.RIM,center,wheelPoint(outerX+side*.011f,radius*.72f,i),wheelPoint(outerX+side*.011f,radius*.72f,j),wheelPoint(outerX+side*.011f,radius*.61f,j),wheelPoint(outerX+side*.011f,radius*.61f,i))
        }
        val spokes=if(style=="sport")6 else 5
        val x=outerX+side*.018f
        for(i in 0 until spokes){
            val angle=i*2.0*PI/spokes
            fun spoke(r:Float,a:Double)=V3(x,center.y+r*cos(a).toFloat(),z+r*sin(a).toFloat())
            b.panel(Finish.RIM,center,spoke(radius*.16f,angle-.30),spoke(radius*.63f,angle-.11),
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
