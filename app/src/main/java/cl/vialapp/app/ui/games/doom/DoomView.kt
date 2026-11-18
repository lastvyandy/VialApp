package cl.vialapp.app.ui.games.doom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/** Original pixel sprites, perspective-correct textured walls, floor casting and column depth. */
internal class DoomView(context:Context):View(context){
    var engine:DoomEngine?=null
    private val w=400
    private var h=224
    private var pixels=IntArray(w*h)
    private val depth=FloatArray(w)
    private var bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
    private var lowCanvas=Canvas(bitmap)
    private val artFilter=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    override fun onSizeChanged(width:Int,height:Int,oldw:Int,oldh:Int){
        super.onSizeChanged(width,height,oldw,oldh)
        if(width<=0||height<=0)return
        val next=(w*height.toFloat()/width).roundToInt().coerceIn(112,300)
        if(next==h)return
        h=next;pixels=IntArray(w*h)
        bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);lowCanvas=Canvas(bitmap)
    }
    private val brush=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    private val target=Rect()
    private val art=DoomArt()
    private var previousX=0f
    private var previousY=0f
    private data class Sprite(val x:Float,val y:Float,val size:Float,val art:IntArray,val hit:Boolean=false,val lift:Float=0f,val widthScale:Float=1f)
    private val sprites=ArrayList<Sprite>()
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val e=engine?:return
        val ca=cos(e.angle);val sa=sin(e.angle)
        val planeX=-sa*.66f;val planeY=ca*.66f
        val focal=w/(2f*.66f)
        val moving=hypot(e.x-previousX,e.y-previousY)>.001f
        previousX=e.x;previousY=e.y
        val horizon=h*.5f+e.lookPitch*h*.30f+e.cameraElevation*17f+(if(moving)sin(e.walk*1.65f)*1.1f else 0f)+e.blastFlash*sin(e.elapsed*70f)*5f+e.recoil*1.6f
        val muzzle=(if(e.shotFlash>0f).21f else 0f)+e.blastFlash*1.1f
        for(y in 0 until h){
            val rowDistance=(focal*.5f)/abs(y+.5f-horizon).coerceAtLeast(.5f)
            val light=((if(e.level==4).70f else .90f)/(1f+rowDistance*.12f)+muzzle/(1f+rowDistance*.4f)).coerceIn(.09f,if(e.level==4).77f else .89f)
            var fx=e.x+rowDistance*(ca-planeX);var fy=e.y+rowDistance*(sa-planeY)
            val sx=rowDistance*planeX*2/w;val sy=rowDistance*planeY*2/w
            val isFloor=y>=horizon
            val tex=if(e.level==4&&isFloor)art.roadFloor else if(isFloor)art.floor else art.ceiling
            for(x in 0 until w){
                val tx=(fx*64).toInt() and 63;val ty=(fy*64).toInt() and 63
                val cx=floor(fx).toInt();val cy=floor(fy).toInt()
                val stripe=isFloor&&((cx in 1..7&&cy==3)||(cx in 9..14&&cy==11))&&ty in 29..32
                val fortressStripe=e.level==3&&isFloor&&(cx==14||cy==19)&&ty in 28..33
                val avenueLine=e.level==4&&isFloor&&(cy==5||cy==14||cy==22)&&ty in 29..33
                val zebra=e.level==4&&isFloor&&(cy==8||cy==12||cy==20)&&cx%3!=0&&tx in 5..58&&ty in 5..55
                val roadside=e.level==4&&isFloor&&(cx==1||cx==32||cy==1||cy==25)&&tx in 3..60
                val stair=e.level==4&&isFloor&&e.stairTier(cx,cy)>0
                val riser=stair&&ty in 0..8
                val tread=stair&&ty in 9..13
                val worn=stair&&((tx+y/5)and 15)<3
                val color=if(e.level==4&&!isFloor){
                    val yy=(y*64/horizon.toInt().coerceAtLeast(1)).coerceIn(0,63)
                    val xx=((x*2+(e.angle*150f).toInt()) and 511)
                    art.sky[yy*512+xx]
                }else if(riser)0xff171c21.toInt() else if(tread)0xffa36d35.toInt() else if(worn)0xff775b45.toInt() else if(zebra)0xffa7a7a0.toInt() else if(avenueLine)0xffbc793a.toInt()
                else if(roadside)0xffa9ada0.toInt() else if(fortressStripe)0xffbc8234.toInt() else if(e.level==3&&isFloor&&cx>=21&&cy>=16&&((tx in 1..4)||(ty in 1..4)))0xff8d3e24.toInt() else if(stripe)0xffad8d38.toInt() else if(isFloor&&cx>=16&&cy>=8&&((tx in 5..8)||(ty in 5..8)))0xff553630.toInt() else if(isFloor&&cx>=9&&cy<7&&cx<=14&&ty in 1..3)0xff627064.toInt() else tex[ty*64+tx]
                val edge=.83f+.17f*(1f-abs(x-w*.5f)/(w*.5f))
                pixels[y*w+x]=if(e.level==4&&!isFloor)color else shade(color,light*edge)
                fx+=sx;fy+=sy
            }
        }
        for(x in 0 until w){
            val p=2f*x/w-1f
            val ray=e.ray(e.x,e.y,ca+planeX*p,sa+planeY*p)
            val d=ray.distance;depth[x]=d
            val lineHeight=(focal/d).coerceAtMost(5000f)
            val top=horizon-lineHeight*.5f
            val tx=(ray.u*64).toInt().coerceIn(0,63)
            val door=if(ray.cell==5)e.doors.firstOrNull{it.x==ray.mapX&&it.y==ray.mapY} else null
            val tex=if(e.level==4){if(door?.kind==4)art.roadWalls[4] else if(door!=null)art.roadGates[door.kind.coerceIn(0,3)] else art.roadWalls[(ray.cell-1).coerceIn(0,5)]}
                else if(door!=null)art.doors[door.kind.coerceIn(0,3)] else art.walls[(ray.cell-1).coerceIn(0,5)]
            val light=(((if(e.level==4).83f else 1.08f)+muzzle)/(1f+d*.17f)*(if(ray.side==1).76f else 1f)).coerceIn(.15f,1.15f)
            for(y in top.toInt().coerceIn(0,h-1)..(top+lineHeight).toInt().coerceIn(0,h-1)){
                val ty=((y-top)*64/lineHeight).toInt().coerceIn(0,63)
                val color=tex[ty*64+tx]
                val edge=.86f+.14f*(1f-abs(x-w*.5f)/(w*.5f))
                val lit=if(ray.cell==5&&ty in 8..24&&tx in 17..47)max(light,.66f)else light
                pixels[y*w+x]=shade(color,lit*edge)
            }
        }
        sprites.clear()
        for(p in e.decor)if(hypot(p.x-e.x,p.y-e.y)>.31f){
            // World props are scenery, not upright enemies. Preserve their silhouette in perspective.
            val size=if(e.level==4)when(p.kind){
                DoomDecorKind.CAR_BLUE,DoomDecorKind.CAR_RED,DoomDecorKind.CAR_YELLOW->p.scale.coerceAtMost(.43f)
                DoomDecorKind.BUS_STOP->.55f
                DoomDecorKind.CAPILLARY_AD->.54f
                DoomDecorKind.ROAD_BARRIER->.36f
                DoomDecorKind.CONE->.24f
                DoomDecorKind.SIGN_STOP,DoomDecorKind.SIGN_YIELD,DoomDecorKind.SIGN_PEDESTRIAN->.65f
                DoomDecorKind.DEMON_TOTEM->.63f
                else->p.scale.coerceAtMost(.83f)
            }else p.scale
            val aspect=when(p.kind){
                DoomDecorKind.CAR_BLUE,DoomDecorKind.CAR_RED,DoomDecorKind.CAR_YELLOW->1.85f
                DoomDecorKind.BUS_STOP->1.75f
                DoomDecorKind.CAPILLARY_AD->1.45f
                DoomDecorKind.ROAD_BARRIER->1.55f
                DoomDecorKind.CONE->.72f
                DoomDecorKind.SIGN_STOP,DoomDecorKind.SIGN_YIELD,DoomDecorKind.SIGN_PEDESTRIAN->.71f
                else->1f
            }
            sprites+=Sprite(p.x,p.y,size,art.decor[p.kind.ordinal],false,p.lift,aspect)
        }
        for(b in e.explosives)if(!b.exploded)sprites+=Sprite(b.x,b.y,if(b.crate).70f else .77f,art.explosive[if(b.crate)1 else 0])
        for(b in e.explosions){val frame=((.52f-b.time)/.52f*6).toInt().coerceIn(0,5);sprites+=Sprite(b.x,b.y,1.05f+frame*.13f,art.explosion[frame],true,.12f)}
        for(l in e.loot)if(!l.taken)sprites+=Sprite(l.x,l.y,if(l.kind==DoomLootKind.BERSERK).68f else .46f,art.loot[l.kind.ordinal])
        for(a in e.enemies){
            if(a.hp>0){
                val phase=if(a.alert)((e.elapsed*6+a.id*.4f).toInt() and 3)else 0
                val size=when(a.kind){DoomKind.BOSS->if(e.level==3)1.55f else 1.33f;DoomKind.MINOXIDIL->1.83f;DoomKind.FLESH_STALKER->1.02f;DoomKind.DRONE->.73f;DoomKind.HEAVY->1.12f;DoomKind.RUNNER->.66f;else->.91f}
                val pose=if(a.attackFlash>0f)art.enemyAttack[a.kind.ordinal] else art.enemyFrames[a.kind.ordinal][phase]
                sprites+=Sprite(a.x,a.y,size,pose,a.flash>0f,if(a.kind==DoomKind.DRONE).25f+sin(e.elapsed*4+a.id)*.07f else 0f)
            }else{
                val frame=(a.deathAge/.12f).toInt().coerceIn(0,5)
                val size=when(a.kind){DoomKind.BOSS->if(e.level==3)1.55f else 1.33f;DoomKind.MINOXIDIL->1.83f;DoomKind.FLESH_STALKER->1.02f;DoomKind.DRONE->.73f;DoomKind.HEAVY->1.12f;DoomKind.RUNNER->.66f;else->.91f}
                sprites+=Sprite(a.x,a.y,size,art.enemyDeath[a.kind.ordinal][frame],frame==0,if(a.kind==DoomKind.DRONE)(.25f*(1f-a.deathAge/.55f)).coerceAtLeast(0f)else 0f)
            }
        }
        for(b in e.bolts)sprites+=Sprite(b.x,b.y,if(b.chrome).34f else if(b.toxic).27f else .23f,when{b.chrome->art.chromeBolt;b.toxic->art.toxicBolt;else->art.bolt},false,.43f)
        for(b in e.rockets)sprites+=Sprite(b.x,b.y,.32f,art.rocket,false,.38f)
        sprites.sortByDescending{(it.x-e.x)*(it.x-e.x)+(it.y-e.y)*(it.y-e.y)}
        for(s in sprites){
            val dx=s.x-e.x;val dy=s.y-e.y;val z=dx*ca+dy*sa
            if(z<.11f)continue
            val px=w*.5f*(1f+(-dx*sa+dy*ca)/(z*.66f))
            val size=(focal*s.size/z).coerceAtMost(2200f)
            val bottom=horizon+focal*.5f/z-focal*s.lift/z;val top=bottom-size
            val spriteWidth=size*s.widthScale;val left=px-spriteWidth*.5f
            val light=((if(e.level==4).98f else 1.25f)/(1f+z*.14f)).coerceIn(.25f,1f)
            for(xx in left.toInt().coerceAtLeast(0)..(left+spriteWidth).toInt().coerceAtMost(w-1)){
                if(z>=depth[xx])continue
                val tx=((xx-left)*64/spriteWidth).toInt().coerceIn(0,63)
                for(yy in top.toInt().coerceAtLeast(0)..bottom.toInt().coerceAtMost(h-1)){
                    val ty=((yy-top)*64/size).toInt().coerceIn(0,63)
                    val color=s.art[ty*64+tx]
                    if(color ushr 24!=0)pixels[yy*w+xx]=shade(color,if(s.hit)1.45f else light)
                }
            }
        }
        bitmap.setPixels(pixels,0,w,0,0,w,h)
        lowCanvas.save()
        val gunScale=(h/180f).coerceIn(.68f,1.32f)
        val bob=if(moving)sin(e.walk)*3.5f else 0f
        lowCanvas.translate(w/2f+bob,h-1f+gunScale*(e.weaponLowering*91f+e.recoil*(if(e.weapon==DoomWeapon.SHOTGUN)17f else 8f))+(if(moving)abs(cos(e.walk))*2.3f else 0f))
        lowCanvas.scale(gunScale,gunScale)
        if(e.berserkActive)drawBerserkFists(lowCanvas,e) else drawGun(lowCanvas,e.weapon,e.shotFlash>0f,e.weaponPhase,e.weaponProgress)
        lowCanvas.restore()
        val cx=w/2f;val cy=horizon
        brush.color=if(e.targetHint()!=null)Color.rgb(255,203,88) else Color.argb(160,240,237,213)
        lowCanvas.drawRect(cx-7,cy,cx-3,cy+1,brush);lowCanvas.drawRect(cx+4,cy,cx+8,cy+1,brush)
        lowCanvas.drawRect(cx,cy-7,cx+1,cy-3,brush);lowCanvas.drawRect(cx,cy+4,cx+1,cy+8,brush)
        if(e.targetHint()!=null){lowCanvas.drawRect(cx-1,cy-1,cx+2,cy+2,brush)}
        if(e.berserkActive){
            val pulse=(sin(e.elapsed*7f)*13f).toInt()
            brush.color=Color.argb((45+pulse).coerceIn(29,67),181,12,11)
            lowCanvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),brush)
            // Deep red vignette that pulses throughout the power-up.
            for(k in 0..3){
                val edge=(4-k)*3f
                brush.color=Color.argb((25+k*17+pulse).coerceIn(18,95),117,2,3)
                lowCanvas.drawRect(0f,0f,w.toFloat(),edge,brush)
                lowCanvas.drawRect(0f,h-edge,w.toFloat(),h.toFloat(),brush)
                lowCanvas.drawRect(0f,0f,edge,h.toFloat(),brush)
                lowCanvas.drawRect(w-edge,0f,w.toFloat(),h.toFloat(),brush)
            }
        }
        if(e.hurtFlash>0f){brush.color=Color.argb((e.hurtFlash*480).toInt().coerceIn(0,100),180,12,8);lowCanvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),brush)}
        if(e.blastFlash>0f){brush.color=Color.argb((e.blastFlash*240).toInt().coerceIn(0,64),255,151,59);lowCanvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),brush)}
        if(e.pickupFlash>0f){brush.color=Color.argb((e.pickupFlash*125).toInt().coerceIn(0,22),220,200,85);lowCanvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),brush)}
        target.set(0,0,width,height);brush.color=Color.WHITE
        canvas.drawBitmap(bitmap,null,target,brush)
    }
    /** Cached original pixel-art fists, scaled with sharp edges like a classic FPS. */
    private fun drawBerserkFists(c:Canvas,e:DoomEngine){
        val progress=if(e.punchSwing>0f)
            sin(((.27f-e.punchSwing)/.27f).coerceIn(0f,1f)*PI.toFloat()).coerceAtLeast(0f)
        else 0f
        fun fist(right:Boolean,thrust:Float){
            c.save()
            if(right)c.scale(-1f,1f)
            val size=1f+thrust*.30f
            val left=-139f+thrust*51f
            val top=-132f-thrust*44f
            c.drawBitmap(DoomFistSprite.bitmap,null,
                RectF(left,top,left+126f*size,top+141f*size),artFilter)
            c.restore()
        }
        // Draw the idle fist behind the striking fist so the hit has depth and weight.
        fist(!e.punchFromRight,0f)
        fist(e.punchFromRight,progress)
    }
    private fun drawGun(c:Canvas,weapon:DoomWeapon,fire:Boolean,phase:DoomWeaponPhase,progress:Float){
        fun rect(l:Float,t:Float,r:Float,b:Float,color:Int){brush.color=color or 0xff000000.toInt();c.drawRect(l,t,r,b,brush)}
        fun poly(color:Int,vararg p:Float){val path=Path();path.moveTo(p[0],p[1]);for(i in 2 until p.size step 2)path.lineTo(p[i],p[i+1]);path.close();brush.color=color or 0xff000000.toInt();c.drawPath(path,brush)}
        val a=when(weapon){DoomWeapon.PISTOL->13f;DoomWeapon.SHOTGUN->26f;DoomWeapon.PLASMA->32f;DoomWeapon.MINIGUN->38f;DoomWeapon.ROCKET->30f}
        val n=if(weapon==DoomWeapon.PISTOL)-51f else -64f
        poly(0x442e25,-59f,0f,-45f,-29f,-24f,-36f,-8f,-10f,8f,-10f,24f,-36f,45f,-29f,59f,0f)
        poly(0xc18a64,-54f,0f,-42f,-24f,-24f,-29f,-14f,-6f,14f,-6f,24f,-29f,42f,-24f,54f,0f)
        poly(0x573a31,-42f,0f,-34f,-18f,-22f,-25f,-13f,0f)
        poly(0x1b1c20,-a-8,0f,-a-5,-25f,-a,n+8,-a+4,n,a-4,n,a,n+8,a+5,-25f,a+8,0f)
        poly(0x63646a,-a-2,0f,-a,-24f,-a+3,n+5,a-3,n+5,a,-24f,a+2,0f)
        poly(0xb8b6af,-a+3,n+5,-a+6,n+1,a-6,n+1,a-3,n+5,a-6,-12f,-a+6,-12f)
        rect(-a+7,n+9,a-7,-12f,if(weapon==DoomWeapon.PLASMA)0x225e65 else 0x44454a)
        for(i in 0..4)rect(-a+3,-10f-i*5f,a-3,-8f-i*5f,0x303136)
        rect(-a+4,n-1,a-4,n+5,0x161619)
        when(weapon){
            DoomWeapon.PISTOL->{
                rect(-a-2,-31f,-a+1,-11f,0x9c9d96);rect(a-2,-35f,a+1,-12f,0x23272a)
                rect(-a+3,n-3,a-3,n+2,0x242529);rect(-4f,n-5,4f,n-2,0x9fa2a5)
                for(k in 0..4)rect(-a+3,-19f+k*3,-a+6,-17f+k*3,0x878882)
            }
            DoomWeapon.SHOTGUN->{
                rect(-a+6,n+4,-3f,-18f,0x303136);rect(3f,n+4,a-6,-18f,0x303136)
                rect(-a+7,n+5,-a+10,-22f,0x92928f);rect(4f,n+5,7f,-22f,0x92928f)
                rect(-2f,n-1,2f,-14f,0x18191a);rect(-a+5,-20f,a-5,-6f,0x714429)
                val pump=if(phase==DoomWeaponPhase.PUMPING||phase==DoomWeaponPhase.RELOADING)sin(progress*PI.toFloat())*13f else 0f
                rect(-a+5,-26f+pump,a-5,-12f+pump,0x52331f)
                for(k in 0..5)rect(-a+7,-25f+k*2+pump,a-7,-24f+k*2+pump,0xa46839)
            }
            DoomWeapon.PLASMA->{
                rect(-a+4,-43f,-a+8,-17f,0x65fff2);rect(a-8,-43f,a-4,-17f,0x65fff2)
                for(k in 0..5){rect(-a+9,-48f+k*6,-a+13,-45f+k*6,0x67a9a0);rect(a-13,-48f+k*6,a-9,-45f+k*6,0x67a9a0)}
                rect(-7f,n-4,7f,n+3,0x367b76);rect(-4f,n-5,4f,n+1,0xa3fff0)
            }
            DoomWeapon.MINIGUN->{
                rect(-33f,-28f,33f,-4f,0x494b42)
                for(k in 0..4){val x=-24f+k*12f;val active=fire&&(k+(progress*5).toInt())%2==0
                    rect(x-4f,n-7f,x+4f,-26f,0x1b1d1d)
                    rect(x-3f,n-5f,x,-29f,if(active)0xd1aa65 else 0x939894)
                    rect(x+1f,n-5f,x+4f,-29f,0x494d4c)
                    rect(x-4f,n-7f,x+4f,n-2f,0x171919)
                }
                rect(-34f,-37f,34f,-30f,0x71756b);rect(-27f,-19f,27f,-12f,0x292b29)
                for(k in 0..5)rect(23f,-25f+k*4,35f,-22f+k*4,0xb08a43)
            }
            DoomWeapon.ROCKET->{
                poly(0x292b21,-29f,-5f,-27f,-36f,-21f,n-8f,21f,n-8f,27f,-36f,29f,-5f)
                poly(0x71764a,-23f,-10f,-20f,n-5f,20f,n-5f,23f,-10f)
                rect(-16f,n-10f,16f,n+6f,0x1c211a);rect(-11f,n-7f,11f,n+2f,0x35392d)
                rect(-25f,-39f,25f,-32f,0xa39b66);rect(-26f,-24f,26f,-19f,0x353729)
                rect(17f,-53f,26f,-44f,0x9d6431);rect(-4f,n-13f,4f,n-8f,0xadb281)
            }
        }
        if(phase==DoomWeaponPhase.RELOADING&&progress in .20f.. .82f){
            val slide=sin(progress*PI.toFloat())*21f
            // Left hand reaches in with a fresh magazine/shell/cell.
            poly(0x37241c,-69f,0f,-55f,-12f-slide,-39f,-25f-slide,-25f,-20f-slide,-19f,-6f,-28f,0f)
            poly(0xb67b4f,-63f,0f,-51f,-12f-slide,-39f,-19f-slide,-28f,-16f-slide,-26f,-8f,-35f,0f)
            when(weapon){
                DoomWeapon.PISTOL->{rect(-36f,-29f-slide,-25f,-8f-slide,0x31343b);rect(-34f,-30f-slide,-28f,-27f-slide,0xb7a77d)}
                DoomWeapon.SHOTGUN->{rect(-36f,-28f-slide,-29f,-13f-slide,0x983e26);rect(-36f,-16f-slide,-29f,-13f-slide,0xd6aa60)}
                DoomWeapon.PLASMA->{rect(-41f,-27f-slide,-27f,-10f-slide,0x254e58);rect(-38f,-25f-slide,-30f,-12f-slide,0x6bd4be)}
                DoomWeapon.MINIGUN->{rect(-42f,-28f-slide,-24f,-8f-slide,0x5a503a);for(k in 0..3)rect(-41f+k*4,-30f-slide,-38f+k*4,-14f-slide,0xc5a253)}
                DoomWeapon.ROCKET->{rect(-39f,-30f-slide,-27f,-8f-slide,0x797c56);poly(0xc3a96e,-39f,-30f-slide,-33f,-38f-slide,-27f,-30f-slide)}
            }
        }
        if(fire){poly(if(weapon==DoomWeapon.PLASMA)0x299ad1 else 0xf26b21,-30f,n-1,-13f,n-18,-24f,n-27,-5f,n-24,4f,n-44,13f,n-24,30f,n-27,20f,n-10,32f,n-1);poly(if(weapon==DoomWeapon.PLASMA)0x82f7ed else 0xffd65a,-19f,n,-7f,n-15,3f,n-32,10f,n-13,20f,n);poly(0xfff8c4,-8f,n,1f,n-18,9f,n)}
    }
    private fun shade(c:Int,f:Float):Int{
        val r=(((c shr 16)and 255)*f).toInt().coerceIn(0,255)
        val g=(((c shr 8)and 255)*f).toInt().coerceIn(0,255)
        val b=((c and 255)*f).toInt().coerceIn(0,255)
        return 0xff000000.toInt() or (r shl 16) or (g shl 8) or b
    }
}

private class DoomArt{
    private val rng=Random(1993)
    private fun rgb(r:Int,g:Int,b:Int)=0xff000000.toInt() or (r.coerceIn(0,255) shl 16) or (g.coerceIn(0,255) shl 8) or b.coerceIn(0,255)
    private fun grain(x:Int,y:Int):Int{
        val block=((x/4*17+y/4*31+x/9*7) xor (y/8*11+x/8*13))%19-9
        return block+rng.nextInt(-5,6)
    }
    val walls=Array(6){kind->IntArray(4096){i->
        val x=i%64;val y=i/64;val n=grain(x,y)
        when(kind){
            0->{
                val xx=(x+if(y/16%2==0)0 else 16)%32;val yy=y%16
                val wear=((x*19+y*13)%59<4)||(x%11==0&&y%19>11)
                when{yy==0||xx==0->rgb(35+n,32+n,29+n);yy==1||xx==1->rgb(141+n,125+n,104+n);yy==15||xx==31->rgb(60+n,54+n,47+n);wear->rgb(79+n,69+n,57+n);else->rgb(109+n,99+n,84+n)}
            }
            1->{when{x<2||x>61||y<2||y>61->rgb(37+n,39+n,40+n);y in 25..35->if((x+y)/8%2==0)rgb(180+n,135+n,35+n)else rgb(29+n,27+n,23+n);x%16 in 0..2->rgb(49+n,50+n,50+n);(x*13+y*7)%41<7->rgb(121+n,74+n,43+n);else->rgb(83+n,85+n,83+n)}}
            2->{when{
                x in 7..56&&y in 7..46->when{y in 7..10||x in 7..10->rgb(23+n,27+n,26+n);y in 43..46||x in 53..56->rgb(111+n,121+n,103+n);y in 15..19&&x in 15..46->rgb(82+n,151+n,106+n);y in 25..39&&x in 15..48&&((x*3+y*7)%17<4)->rgb(61+n,115+n,80+n);else->rgb(17+n,33+n,27+n)}
                x%16==0||y%16==0->rgb(30,34,35);y in 52..57->rgb(123+n,94+n,40+n);else->rgb(67+n,76+n,70+n)
            }}
            3->{when{y in 4..10||y in 53..59->rgb(139+n,30+n,24+n);x%16 in 0..2->rgb(33+n,23+n,24+n);x%16==3->rgb(115+n,68+n,54+n);else->rgb(77+n,54+n,49+n)}}
            4->{when{x in 3..7||x in 56..60->rgb(32+n,33+n,35+n);y in 4..8||y in 55..59->rgb(40+n,41+n,43+n);y in 28..35->if((x+y)/7%2==0)rgb(190+n,146+n,36+n)else rgb(31+n,31+n,29+n);x in 47..52&&y in 19..25->rgb(218,44+n,27);x%12==0->rgb(51+n,52+n,51+n);else->rgb(91+n,96+n,94+n)}}
            else->{when{x in 4..9||x in 54..59->rgb(48+n,59+n,60+n);y in 8..17&&x in 15..48->rgb(30,184+n,117+n);y%13==0->rgb(36+n,51+n,43+n);else->rgb(65+n,83+n,72+n)}}
        }
    }}
    val floor=IntArray(4096){i->
        val x=i%64;val y=i/64;val n=grain(x,y)
        when{x<2||y<2->rgb(25+n,25+n,23+n);x==2||y==2->rgb(90+n,83+n,69+n);(x/7+y/11)%9==1->rgb(48+n,43+n,36+n);else->rgb(70+n,65+n,55+n)}
    }
    val ceiling=IntArray(4096){i->val x=i%64;val y=i/64;val n=rng.nextInt(-8,9);if(x in 27..36&&y in 8..55)rgb(110+n,105+n,81+n)else if(x%16==0||y%16==0)rgb(23+n,24+n,25+n)else rgb(49+n,45+n,43+n)}
    // Fixed-size textures: no new bitmaps are created in the draw loop.
    val sky=IntArray(512*64){i->
        val x=i%512;val y=i/512
        val fracture=((x/5*17+y/3*29+x/33*23) xor (x*7+y*11)) and 63
        val cloud=((x/31+y/9*17) xor (x/9+y/5*5) xor (x/74*13)) and 31
        val skyline=45+(x/19%5)-((x*17/49)%7)
        val redGlow=((x+112)%512) in 135..325&&y in 32..48
        val lightning=(
            x in 368..370&&y in 8..13||
            x in 366..368&&y in 14..20||
            x in 371..373&&y in 21..28||
            x in 369..371&&y in 29..33
        )
        when{
            y>=skyline->rgb(20+fracture/6,22+fracture/7,27+fracture/6)
            y>skyline-3->rgb(82+fracture/2,37+fracture/3,34+fracture/4)
            lightning->rgb(153,125,126)
            redGlow->rgb(68+fracture/3,28+fracture/6,30+fracture/5)
            cloud in 0..10->rgb(26+fracture/4,24+fracture/6,34+fracture/4)
            cloud in 11..19->rgb(44+fracture/5,36+fracture/6,43+fracture/5)
            else->rgb(20+y/5+fracture/8,24+y/6+fracture/8,34+y/6+fracture/7)
        }
    }
    val roadFloor=IntArray(4096){i->
        val x=i%64;val y=i/64
        val speck=((x*11+y*37+x*y) xor (x+y*7)) and 15
        when{
            x in 0..2||y in 0..2->rgb(43+speck,55+speck,59+speck)
            (x*7+y*5)%41<3->rgb(95+speck,77+speck,65+speck)
            else->rgb(43+speck,45+speck,46+speck)
        }
    }
    val roadWalls=Array(6){kind->IntArray(4096){i->
        val x=i%64;val y=i/64;val n=grain(x,y)
        when(kind){
            0->when{y%16<2||x%32<2->rgb(22+n,28+n,32+n);y%16 in 2..3->rgb(115+n,78+n,65+n);else->rgb(82+n,65+n,62+n)}
            1->when{y<5->rgb(48+n,31+n,26+n);y>55->rgb(27+n,24+n,27+n);x%13 in 0..2->rgb(22+n,23+n,25+n);else->rgb(84+n,70+n,59+n)}
            2->when{y in 5..13->rgb(80+n,28+n,23+n);y in 16..44&&x in 10..54->rgb(23+n,47+n,45+n);y in 16..44->rgb(134+n,101+n,65+n);else->rgb(74+n,77+n,65+n)}
            3->when{y<12->rgb(68+n,28+n,26+n);x%16<3->rgb(29+n,32+n,33+n);y%16<2->rgb(109+n,48+n,36+n);else->rgb(71+n,58+n,56+n)}
            4->when{y in 27..35->if(x%16<8)rgb(163,109,43)else rgb(36,35,34);x%8<2->rgb(31+n,33+n,34+n);else->rgb(78+n,74+n,70+n)}
            else->when{y in 11..30&&x in 10..55->rgb(85+n,27+n,28+n);x%12<2->rgb(27+n,38+n,40+n);else->rgb(58+n,66+n,59+n)}
        }
    }}
    val roadGates=Array(4){kind->
        val bg=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888)
        bg.setPixels(roadWalls[4],0,64,0,0,64,64)
        val c=Canvas(bg);val p=Paint().apply{isAntiAlias=false}
        fun box(l:Float,t:Float,r:Float,b:Float,color:Int){p.color=color or 0xff000000.toInt();c.drawRect(l,t,r,b,p)}
        box(5f,5f,59f,59f,0x263F46)
        box(8f,9f,56f,55f,0x436B71)
        box(8f,30f,56f,44f,0xD8DFD9)
        for(k in 0..4)box(9f+k*10,31f,15f+k*10,43f,0xE05C36)
        box(15f,9f,49f,28f,if(kind==2)0x89511E else 0x216D6A)
        p.textSize=7f;p.textAlign=Paint.Align.CENTER;p.typeface=android.graphics.Typeface.DEFAULT_BOLD;p.color=android.graphics.Color.WHITE
        c.drawText(when(kind){0->"DESVIO";1->"CONTROL";2->"CERRADO";else->"BONUS"},32f,22f,p)
        IntArray(4096).also{bg.getPixels(it,0,64,0,0,64,64);bg.recycle()}
    }
    private class Ink(val c:Canvas){
        private val p=Paint().apply{isAntiAlias=false}
        fun box(l:Int,t:Int,r:Int,b:Int,v:Int){p.color=v or 0xff000000.toInt();c.drawRect(l.toFloat(),t.toFloat(),r.toFloat(),b.toFloat(),p)}
        fun poly(v:Int,vararg xy:Int){val a=Path();a.moveTo(xy[0].toFloat(),xy[1].toFloat());for(i in 2 until xy.size step 2)a.lineTo(xy[i].toFloat(),xy[i+1].toFloat());a.close();p.color=v or 0xff000000.toInt();c.drawPath(a,p)}
        fun oval(l:Int,t:Int,r:Int,b:Int,v:Int){p.color=v or 0xff000000.toInt();c.drawOval(l.toFloat(),t.toFloat(),r.toFloat(),b.toFloat(),p)}
    }
    private fun sprite(f:Ink.()->Unit):IntArray{
        val b=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);Ink(Canvas(b)).f()
        return IntArray(4096).also{b.getPixels(it,0,64,0,0,64,64);b.recycle()}
    }
    val enemies=arrayOf(
        sprite{
            oval(9,57,55,64,0x252024);box(14,42,29,60,0x26282c);box(35,42,50,60,0x26282c)
            box(15,47,24,56,0x64676a);box(38,47,46,56,0x64676a);box(10,57,28,63,0x4a4641);box(36,57,54,63,0x4a4641)
            poly(0x222327,12,16,52,16,57,35,46,47,18,47,7,35);poly(0x767875,17,18,47,18,49,37,41,43,23,43,15,37)
            box(20,27,44,34,0xa99b64);box(24,36,40,40,0x3b3935)
            box(22,5,42,8,0x33353a);box(18,8,46,21,0x9b9c8e);box(20,12,44,19,0x272528)
            box(22,13,30,17,0xf84532);box(34,13,42,17,0xf84532);box(25,13,28,15,0xffb069);box(37,13,40,15,0xffb069)
            box(7,23,15,43,0x4e5053);box(49,23,57,41,0x4e5053);box(3,33,11,48,0x2a2626);box(2,31,14,35,0xa2a09d)
        },
        sprite{
            oval(8,50,56,55,0x252024);poly(0x262d35,3,24,12,15,21,18,43,18,52,15,61,24,55,39,44,45,20,45,9,39)
            poly(0x657c7b,8,25,17,20,25,24,39,24,47,20,56,25,51,36,43,40,21,40,13,36)
            oval(20,18,44,43,0x333639);oval(24,21,40,39,0xab3a25);oval(28,25,37,35,0xff7040);oval(30,27,35,32,0xffdca0)
            box(8,36,16,43,0x4cbbdf);box(48,36,56,43,0x4cbbdf);box(11,43,14,49,0x85ecfb);box(50,43,53,49,0x85ecfb)
        },
        sprite{
            oval(0,56,64,64,0x242024);poly(0x222025,5,38,14,19,50,19,59,38,55,60,9,60)
            poly(0x61514a,9,36,18,22,46,22,55,36,49,55,15,55)
            box(0,28,12,47,0x393436);box(52,28,64,47,0x393436)
            box(2,31,9,42,0xb9a8a1);box(55,31,62,42,0xb9a8a1)
            poly(0x947566,16,8,48,8,52,25,43,42,21,42,12,25)
            box(20,12,44,33,0x292027);oval(23,14,41,33,0xa32624);oval(26,17,38,30,0xff6f35);oval(30,20,35,26,0xffedac)
            box(17,38,47,45,0x202328);for(i in 0..4)box(20+i*6,39,23+i*6,43,0xffb33c)
            box(12,50,26,61,0x383337);box(38,50,52,61,0x383337);box(7,59,28,64,0x6b605a);box(36,59,57,64,0x6b605a)
        },
        sprite{
            oval(9,57,55,64,0x242022);box(12,42,27,61,0x303535);box(36,42,52,61,0x303535)
            box(10,57,28,63,0x7b6e52);box(36,57,54,63,0x7b6e52)
            poly(0x292628,8,21,16,16,48,16,56,23,50,46,14,46)
            poly(0x987c4e,15,21,48,21,46,39,20,42,13,33);box(19,25,45,30,0xb39c65)
            box(21,6,43,19,0x6d6861);box(23,10,41,15,0x201c20);box(24,11,30,14,0xf5c05d);box(34,11,40,14,0xf5c05d)
            box(7,28,15,44,0x746749);box(49,26,57,41,0x746749)
            box(5,33,58,40,0x1f2427);box(8,32,55,35,0x909390);box(21,40,47,44,0x594132);box(50,31,61,38,0x34363a)
        },
        sprite{
            oval(1,57,63,64,0x211e22);box(9,43,28,61,0x2c2b31);box(36,43,56,61,0x2c2b31)
            box(5,54,29,63,0x6b7770);box(35,54,59,63,0x6b7770)
            poly(0x24282d,2,20,15,10,49,10,62,22,54,46,10,46)
            poly(0x697d77,7,22,17,14,47,14,57,23,50,40,14,40)
            box(18,22,46,35,0x33393a);box(21,26,43,32,0x9e4328)
            box(22,4,42,19,0x7e8881);box(24,10,40,15,0x242527);box(26,11,38,14,0xff5d31)
            box(2,27,17,44,0x32363a);box(47,27,62,44,0x32363a)
            box(4,33,16,48,0x8b8b81);box(48,33,60,48,0x8b8b81);box(6,37,14,44,0x2d252a);box(50,37,58,44,0x2d252a)
        },
        sprite{
            oval(6,51,58,63,0x251d1b);poly(0x322826,4,37,12,16,46,11,60,36,54,53,13,53)
            poly(0xb4472a,10,32,17,19,44,15,54,32,50,49,15,49)
            box(19,22,43,39,0x242628);box(22,25,40,35,0xfab543);box(26,27,37,32,0xffe5a7)
            box(6,44,18,59,0x464446);box(46,44,58,59,0x464446)
            for(i in 0..3){box(10,46+i*3,17,47+i*3,0x888075);box(47,46+i*3,54,47+i*3,0x888075)}
            box(28,6,35,14,0xe45c39);box(30,7,33,11,0xffeba0)
        },

        // Flesh stalker: crooked claws, horned skull, dripping surgical flesh.
        sprite{
            oval(4,56,60,64,0x1c1014)
            box(7,46,23,62,0x241b1d);box(39,45,56,62,0x241b1d)
            poly(0x201a1f,7,26,14,16,24,20,43,14,54,28,51,50,11,50)
            poly(0x7e3c3a,11,29,17,19,44,21,50,33,43,48,14,46)
            for(k in 0..3){val x=16+k*8;box(x,32,x+3,42,0xb35b4a)}
            // Fangs and broken bone protrusions; absolutely no human scalp.
            poly(0xb3a38b,14,20,6,2,23,16)
            poly(0xb3a38b,40,15,56,0,50,24)
            poly(0x37262b,17,13,47,13,51,32,40,39,18,32)
            box(20,23,29,28,0xef3a1f);box(35,23,45,28,0xef3a1f)
            box(22,24,28,26,0xff9b45);box(37,24,44,26,0xff9b45)
            poly(0x111116,19,33,43,33,38,43,25,44)
            for(k in 0..4)poly(0xe0c7a6,22+k*4,35,25+k*4,35,23+k*4,42)
            poly(0x4e2327,10,35,4,40,3,53,13,50)
            poly(0x4e2327,51,35,59,41,62,54,52,50)
            for(k in 0..2){box(1+k*3,47-k*4,5+k*3,55-k*4,0x97866e);box(52+k*3,44+k*4,56+k*3,53+k*4,0x97866e)}
            box(20,50,46,55,0x862b2a)
        },
        // Captain Minoxidil: grotesque armored bio-mech, red cyclops,
        // syringes, steel tusks, no exposed human face or bald scalp.
        sprite{
            oval(1,55,63,64,0x181013)
            box(7,43,24,63,0x1c2026);box(40,43,57,63,0x1c2026)
            box(8,57,27,63,0x70725d);box(38,57,57,63,0x70725d)
            poly(0x191b1f,5,34,13,21,23,17,43,17,55,25,60,38,51,54,12,54)
            poly(0x5c322f,11,29,19,21,45,22,53,34,46,50,15,48)
            poly(0x9d5139,14,31,22,24,43,24,49,34,44,47,18,45)
            // Massive serrated armored crown.
            poly(0x242c31,11,22,5,3,21,13,29,2,37,11,50,1,55,25,46,31,16,31)
            poly(0x6f756e,16,19,17,9,26,16,31,7,37,17,45,9,46,22)
            poly(0x191217,16,22,47,22,51,33,42,40,20,40,12,32)
            oval(20,22,45,36,0x751d22);oval(25,23,41,33,0xed3d23)
            oval(29,24,36,30,0xffdb74)
            for(k in 0..4)poly(0xe6ccb1,20+k*5,34,24+k*5,34,22+k*5,44)
            // Shoulder launchers loaded with toxic serum vials.
            box(2,18,15,45,0x18272b);box(49,18,62,45,0x18272b)
            box(5,10,12,35,0x3da989);box(52,10,59,35,0x3da989)
            box(6,7,11,14,0xb5d59a);box(53,7,58,14,0xb5d59a)
            box(4,35,13,42,0xa33d2e);box(51,35,60,42,0xa33d2e)
            box(12,47,52,52,0x4a2224);box(16,48,48,50,0xb45234)
        },
        // SABOTEUR: human raider with dark side-parted hair, dirty orange work vest.
        sprite{
            oval(7,58,56,64,0x111219)
            box(12,44,27,61,0x222831);box(36,44,51,61,0x212630)
            box(9,58,27,63,0x454243);box(36,58,55,63,0x454243)
            poly(0x24272e,8,29,15,19,48,19,56,30,49,50,15,50)
            poly(0x9b4730,14,27,22,22,44,22,50,28,45,45,18,45)
            box(27,26,36,45,0xd4a44c);box(20,30,24,40,0xddc06d)
            box(6,29,14,45,0x7d503f);box(49,29,57,44,0x7d503f)
            box(13,7,51,25,0x18151a);box(15,12,49,35,0x9e684e)
            // Side-parted hair, sideburns and unshaven jaw.
            poly(0x181217,12,13,21,4,43,5,54,14,49,20,39,15,32,13,22,19,15,29)
            poly(0x4c3024,17,10,38,7,49,12,39,13,25,12)
            box(20,21,28,24,0x26222a);box(35,21,43,24,0x26222a)
            box(24,31,40,34,0x40302c);box(21,29,24,31,0x4d3025)
            // Steel pistol held against torso.
            box(25,37,51,43,0x1a252b);box(41,34,59,38,0x68777b)
        },
        // HACKER: long, spiky hair; exposed pale face and burgundy hoodie.
        sprite{
            oval(6,57,58,64,0x15121a)
            box(15,43,28,62,0x1d2534);box(37,42,52,62,0x202837)
            poly(0x341a35,7,29,17,17,46,17,58,30,50,51,13,51)
            poly(0x77304c,13,28,23,24,42,24,52,34,45,46,18,46)
            box(30,24,34,44,0xc77970);box(10,35,18,48,0x54243a)
            box(50,34,58,49,0x54243a)
            poly(0x1b182b,12,22,8,7,19,13,22,2,31,9,36,0,44,9,53,4,49,22,43,19,30,16,20,28)
            poly(0x30243b,18,18,23,12,38,10,45,17,40,25,27,30,18,26)
            oval(21,20,45,39,0xbe8c73);box(22,26,30,28,0x15191d)
            box(35,26,43,28,0x15191d);box(27,34,39,36,0x6b393b)
            box(14,40,21,46,0xcaa6a1)
            box(39,40,58,44,0x1a222e);box(42,36,55,40,0x465459)
        },
        // RIVAL_GUARD: messy visible brown hair, broad armored silhouette, shotgun.
        sprite{
            oval(3,56,61,64,0x111718)
            box(9,42,26,61,0x263033);box(37,42,55,61,0x263033)
            box(9,59,27,64,0x7d746b);box(38,59,56,64,0x7d746b)
            poly(0x171c22,4,25,12,13,53,13,61,28,56,52,8,52)
            poly(0x536052,10,26,19,20,43,20,54,29,50,49,13,48)
            box(19,30,46,41,0x202c30);box(22,33,44,37,0x7a847a)
            box(4,30,15,48,0x585848);box(49,30,61,48,0x585848)
            oval(17,10,48,34,0xc48f68)
            poly(0x281b16,14,17,17,5,25,11,30,2,40,8,47,4,51,17,43,14,32,13,24,18,19,29)
            box(20,22,29,25,0x242225);box(35,22,44,25,0x242225)
            box(23,30,41,33,0x48352e)
            box(21,38,56,45,0x161d25);box(34,35,61,39,0x949b93)
            box(15,42,21,47,0x9e6b53)
        },
        // CORRUPT_TECH: long dark hair escaping a cracked visor, grim white lab coat.
        sprite{
            oval(6,57,59,64,0x131417)
            box(13,45,28,62,0x253036);box(37,45,52,62,0x293038)
            poly(0x202b30,5,30,17,17,47,17,60,32,52,52,13,52)
            poly(0x9faaa0,12,30,22,23,41,23,53,31,49,47,16,48)
            poly(0x3e4b4d,20,27,29,29,31,44,20,45)
            poly(0x334348,34,28,44,25,43,45,34,45)
            box(29,27,36,44,0x9b282b)
            oval(19,10,47,36,0x8f6352)
            poly(0x101a20,12,19,15,4,24,8,36,2,48,9,52,22,47,33,42,18,25,18,21,34)
            poly(0x273039,15,11,22,6,42,5,45,13,34,13,21,18)
            box(18,19,48,26,0x1d2a34);box(21,20,44,24,0x70bdaf)
            box(26,27,39,30,0x4d292d)
            box(2,33,17,45,0x6f7770);box(49,32,62,46,0x6f7770)
            box(47,38,58,44,0x193b41);box(50,35,54,51,0x8dc2a5)
        },
        // ADMINISTRATOR: tall human rival, huge slicked-back hair and armored red coat.
        sprite{
            oval(0,56,63,64,0x150f13)
            box(8,43,25,64,0x1e202b);box(39,42,57,64,0x1c2027)
            box(7,58,28,64,0x74716b);box(39,58,59,64,0x74716b)
            poly(0x24232c,2,30,10,14,20,10,47,11,57,21,63,34,55,56,8,56)
            poly(0x85282c,7,29,15,19,48,17,57,31,49,53,13,51)
            poly(0xc14631,15,27,22,22,44,22,50,31,46,47,18,46)
            box(29,31,36,51,0xd0a45c);box(22,36,43,39,0x27252b)
            oval(16,7,49,36,0xb77758)
            // A very explicit HAIR silhouette: prominent black pompadour and sideburns.
            poly(0x0e1119,12,17,10,4,19,7,23,0,33,4,43,0,51,6,55,19,48,16,44,14,32,15,21,23,17,33)
            poly(0x34303b,15,12,24,3,42,4,49,11,40,12,29,11,21,17)
            box(19,21,30,25,0x192127);box(35,21,47,25,0x192127)
            box(21,22,29,24,0xb6bdad);box(37,22,45,24,0xb6bdad)
            poly(0x352123,23,30,38,30,42,34,30,35)
            box(3,25,15,48,0x812b2d);box(50,25,62,48,0x812b2d)
            box(3,30,13,37,0x394048);box(52,30,61,38,0x394048)
            box(6,42,17,48,0x4e5558);box(49,42,60,48,0x4e5558)
            box(16,49,52,53,0x1f252c)
        })
    val loot=Array(DoomLootKind.entries.size){i->sprite{
        when(DoomLootKind.entries[i]){
            DoomLootKind.COIN->{oval(15,12,49,59,0x87531a);oval(12,9,46,56,0xe0a82c);oval(17,14,41,51,0xffd759);box(28,23,32,43,0x9a6b21)}
            DoomLootKind.MEDKIT->{box(7,28,57,60,0x303333);box(9,25,55,55,0xbbc3b7);box(25,29,39,51,0xbd372b);box(19,35,45,45,0xbd372b)}
            DoomLootKind.ARMOR->{poly(0x293c3b,9,13,22,6,42,6,55,13,48,53,32,61,16,53);poly(0x67a5a4,12,15,23,10,41,10,52,15,45,50,32,56,19,50);box(26,18,38,46,0x355757)}
            DoomLootKind.KEY->{box(7,18,57,49,0x97721e);box(10,16,55,43,0xf5c955);box(14,20,26,32,0x554626);box(32,21,50,26,0xfbf0b4);box(32,32,48,36,0x776027)}
            DoomLootKind.RED_KEY->{box(7,18,57,49,0x6f2024);box(10,16,55,43,0xe45c4d);box(14,20,26,32,0x4c2526);box(32,21,50,26,0xffb8a0);box(32,32,48,36,0x77302e)}
            DoomLootKind.MINIGUN->{box(5,30,57,45,0x303333);for(k in 0..3)box(24,21+k*4,61,24+k*4,0x9b9b86);box(9,42,26,56,0x735b35);for(k in 0..4)box(13+k*7,42,17+k*7,52,0xc6a158)}
            DoomLootKind.ROCKET->{poly(0x4c5140,4,36,20,24,59,12,63,26,29,39,13,49);box(24,24,53,30,0x9b9a69);box(6,35,22,45,0x3a3a31)}
            DoomLootKind.ROCKET_AMMO->{for(k in 0..2){val x=11+k*16;box(x,27,x+10,57,0x747a49);poly(0xb5ad79,x,27,x+5,13,x+10,27);box(x,46,x+10,51,0x97502f)}}
            DoomLootKind.BELT->{box(7,34,58,59,0x50462e);for(k in 0..5){box(9+k*8,21,15+k*8,48,0xb99a4c);box(9+k*8,20,15+k*8,24,0xe4cc85)}}
            DoomLootKind.BERSERK->{box(7,8,57,62,0x261a1e);box(11,12,53,58,0x841c20);poly(0xc4a386,23,12,43,12,47,30,39,43,24,43,16,29);poly(0xe5c9a5,18,31,28,27,35,30,49,22,52,37,37,55,17,48);box(21,40,48,53,0xdd9b75);box(14,53,54,59,0xc93823);box(16,10,20,16,0xffdb62);box(45,9,49,15,0xffdb62)}
            DoomLootKind.SHOTGUN,DoomLootKind.PLASMA->{poly(0x27252a,5,35,26,20,61,14,61,26,27,36,15,49,7,48);box(26,18,59,25,0xb3b5b0);box(6,36,26,45,0x804a2d);if(i==DoomLootKind.PLASMA.ordinal)box(29,25,52,31,0x64e9d7)}
            else->{val color=when(i){DoomLootKind.SHELLS.ordinal->0xb34131;DoomLootKind.CELL.ordinal->0x4bb9ae;else->0xa69146};box(8,32,56,60,0x292a26);box(10,29,54,55,color);for(k in 0..4){box(13+k*8,25,18+k*8,47,color);box(13+k*8,23,18+k*8,28,0xe9cd86)}}
        }
    }}
    // Build distinct, readable door panels, including terminal labels, once.
    val doors=Array(4){kind->
        val b=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888)
        b.setPixels(walls[4],0,64,0,0,64,64)
        val c=Canvas(b);val p=Paint().apply{isAntiAlias=false}
        fun box(l:Float,t:Float,r:Float,bb:Float,col:Int){p.color=col or 0xff000000.toInt();c.drawRect(l,t,r,bb,p)}
        box(10f,3f,54f,61f,0x272a2a);box(12f,5f,52f,59f,0x4d514d)
        for(i in 0..5){box(14f,26f+i*5,50f,28f+i*5,0x272929);box(14f,28f+i*5,50f,29f+i*5,0x6f7066)}
        box(15f,7f,49f,24f,0x171c1a);box(17f,9f,47f,21f,if(kind==2)0x8b5a20 else if(kind==0)0x30573b else 0x285363)
        p.color=0xffd3e7cc.toInt();p.textSize=7f;p.typeface=android.graphics.Typeface.MONOSPACE;p.textAlign=Paint.Align.CENTER
        c.drawText(when(kind){0->"ACCESO";1->"VIAL";2->"LOCK";else->"SEC."},32f,17f,p)
        box(47f,33f,53f,44f,0x212b2a);box(49f,35f,51f,38f,if(kind==2)0xf0b345 else 0x8cce91)
        IntArray(4096).also{b.getPixels(it,0,64,0,0,64,64);b.recycle()}
    }
    // Sculpted highlights on original sprites, with a four-frame mechanical walk cycle.
    private fun detail(src:IntArray,frame:Int,kind:Int):IntArray{
        val out=IntArray(4096)
        for(y in 0..63)for(x in 0..63){
            val shift=if(kind!=1&&y>=44&&y<61&&frame%2==1){if(x<32)if(frame==1)2 else -2 else if(frame==1)-2 else 2}else 0
            val sx=x+shift;if(sx !in 0..63)continue
            val col=src[y*64+sx];if(col ushr 24==0)continue
            val above=if(y>0)src[(y-1)*64+sx] else 0
            val left=if(sx>0)src[y*64+sx-1]else 0
            val right=if(sx<63)src[y*64+sx+1]else 0
            val below=if(y<63)src[(y+1)*64+sx]else 0
            val f=when{above!=col||left!=col->1.15f;right!=col||below!=col->.77f;else->1f}*(1f-(sx-20)*.003f)
            val n=((x*17+y*31)%11)-5
            out[y*64+x]=rgb((((col shr 16)and 255)*f).toInt()+n,(((col shr 8)and 255)*f).toInt()+n,((col and 255)*f).toInt()+n)
        }
        return out
    }
    val enemyFrames=Array(enemies.size){kind->Array(4){frame->detail(enemies[kind],frame,kind)}}
    val enemyAttack=Array(enemies.size){kind->detail(enemies[kind],0,kind).also{a->
        val cx=when(kind){0->7;1->32;2->32;3->53;4->50;else->32}
        val cy=when(kind){0->35;1->30;2->28;3->35;4->42;else->27}
        for(y in (cy-7).coerceAtLeast(0)..(cy+7).coerceAtMost(63))for(x in (cx-7).coerceAtLeast(0)..(cx+7).coerceAtMost(63)){
            val d=abs(x-cx)+abs(y-cy)
            if(d<9)a[y*64+x]=if(d<4)0xffffe6a0.toInt()else 0xffef7035.toInt()
        }
    }}
    // Six distinct collapse frames preserve each robot's silhouette.
    val enemyDeath=Array(enemies.size){kind->Array(6){frame->
        val out=IntArray(4096)
        val source=enemies[kind]
        val compression=floatArrayOf(.98f,.82f,.61f,.42f,.24f,.18f)[frame]
        val tilt=floatArrayOf(0f,.12f,.24f,.38f,.50f,.55f)[frame]
        for(y in 0..63)for(x in 0..63){
            val c=source[y*64+x]
            if(c ushr 24==0)continue
            val xx=(x+(63-y)*tilt).roundToInt().coerceIn(0,63)
            val yy=(63-(63-y)*compression).roundToInt().coerceIn(0,63)
            val brightness=1f-frame*.075f
            out[yy*64+xx]=rgb((((c shr 16)and 255)*brightness).toInt(),(((c shr 8)and 255)*brightness).toInt(),((c and 255)*brightness).toInt())
        }
        if(frame in 0..3){
            for(i in 0..(18-frame*3)){
                val xx=(i*23+kind*13+frame*7)%58+3
                val yy=(i*11+frame*9+kind*3)%38+16
                out[yy*64+xx]=if(i%3==0)0xffffcf61.toInt()else 0xffd35d2b.toInt()
            }
        }
        if(frame>=3)for(y in 60..63)for(x in 13..53)if((x+y*7+kind)%5!=0&&out[y*64+x]==0)out[y*64+x]=0xff382923.toInt()
        out
    }}
    val explosive=arrayOf(
        sprite{
            oval(10,53,54,64,0x282021);box(13,13,51,58,0x382624);box(16,15,48,55,0x8c3526)
            box(17,16,23,53,0xcc6541);box(44,16,49,55,0x5a2723);oval(13,5,51,20,0x4a302b);oval(17,7,47,17,0xa36a50)
            box(14,20,50,24,0x38312b);box(14,49,50,53,0x38312b);box(19,29,45,45,0xe7b54a)
            poly(0x322924,32,30,42,42,22,42);box(31,34,33,39,0xffd15b)
        },
        sprite{
            box(4,16,60,62,0x2b2322);box(7,19,57,59,0x843e2c);box(9,21,55,25,0xca7250)
            box(9,54,55,58,0x472923);box(10,26,54,51,0x47312a)
            for(i in 0..5)poly(if(i%2==0)0xcda448 else 0x2e2825,10+i*7,27,16+i*7,27,22+i*7,49,16+i*7,49)
            box(20,31,45,46,0xb1462b);box(29,33,34,40,0xffe2a0);box(29,42,34,44,0xffe2a0)
        })
    val explosion=Array(6){frame->IntArray(4096){i->
        val x=i%64-32;val y=i/64-32
        val d=hypot(x.toFloat(),y.toFloat());val edge=14+frame*3+((x*17+y*11) and 7)
        val n=((x*19+y*31+frame*53) and 15)
        when{
            d>edge->0
            frame>=4->if(n<4+frame)0 else rgb(94+n*4,78+n*3,65+n*2)
            d<edge*.35f->rgb(255,235-n*3,158-n*4)
            d<edge*.67f->rgb(255,164-n*4,35+n)
            else->rgb(174+n*5,48+n*2,12+n)
        }
    }}
    val decor=arrayOf(
        sprite{
            oval(11,46,53,63,0x1e2120);box(13,13,51,55,0x292e27)
            box(16,13,47,55,0x536343);box(18,16,23,51,0x788060);box(43,15,49,54,0x343d2b)
            oval(13,5,51,20,0x242922);oval(16,7,48,18,0x838b67);oval(19,9,45,16,0x535d45)
            box(16,27,48,35,0x967e38);box(16,35,48,37,0x312f1c)
            box(14,20,50,23,0x292c24);box(14,47,50,51,0x333629);box(18,23,21,27,0x829070)
        },
        sprite{
            box(3,14,61,61,0x252420);box(6,17,58,57,0x5d5643);box(8,19,55,23,0x958363);box(8,52,55,56,0x342f28)
            for(i in 0..5){box(10+i*7,23,15+i*7,51,0x77654d);box(10+i*7,24,11+i*7,50,0x9c835b)}
            poly(0x423e34,8,23,14,21,55,47,55,54,49,53,8,29)
            box(8,22,15,28,0x333631);box(49,48,56,54,0x333631)
        },
        sprite{
            box(10,6,54,63,0x222828);box(13,8,50,62,0x54615b);box(15,10,48,33,0x191e1d)
            box(18,13,45,30,0x204b39);for(i in 0..3){box(21,15+i*3,40-i*2,17+i*3,0x61a67e)}
            box(14,36,49,47,0x333d36);for(i in 0..4)box(18+i*6,39,21+i*6,43,0x92968b)
            box(18,51,45,59,0x2b3230)
        },
        sprite{
            box(14,9,50,48,0x222527);box(18,12,46,45,0x534137);box(21,15,43,42,0x9c5536);box(24,17,40,40,0xf3aa53);box(27,20,37,36,0xffdda0)
            box(9,8,55,13,0x363733);box(9,44,55,49,0x333630)
        },
        sprite{
            oval(2,51,62,62,0x212223);poly(0x30353a,5,41,18,25,43,24,57,38,62,54,4,54);poly(0x4e585b,10,39,20,28,40,27,51,39,49,48,12,48);box(21,29,39,37,0x202b30);box(8,43,15,49,0x8a7554);box(49,43,56,49,0x803a2c);box(10,53,22,61,0x252526);box(43,53,55,61,0x252526)
        },
        // Small abandoned hatchback: low wheelbase, proper wheels and dusty slate-blue paint.
        sprite{
            oval(2,54,62,62,0x191A1C)
            box(5,39,59,54,0x1E242B)
            poly(0x314C58,4,39,9,34,20,30,24,22,44,22,49,31,58,34,60,40,59,51,5,51)
            poly(0x536F76,23,25,43,25,47,34,19,34)
            poly(0x1B303A,24,27,31,27,30,33,21,33)
            poly(0x253B45,34,27,42,27,45,33,33,33)
            box(6,39,58,47,0x425E68)
            box(8,43,56,46,0x62787A)
            box(6,48,58,52,0x242C34)
            oval(9,46,25,62,0x0F1114);oval(40,46,56,62,0x0F1114)
            oval(13,50,21,58,0x7B8582);oval(44,50,52,58,0x7B8582)
            oval(15,52,19,56,0x292D30);oval(46,52,50,56,0x292D30)
            box(5,38,10,42,0xC4B98C);box(53,38,59,42,0x9C4032)
            box(31,32,33,42,0x2B3437)
        },
        sprite{
            oval(1,53,63,63,0x232526);box(5,39,59,55,0x4d2028)
            poly(0xa93640,5,34,16,29,20,18,42,18,51,29,59,34,59,50,5,50)
            poly(0x81c2cc,21,20,42,20,48,32,16,32)
            box(9,35,55,48,0xd45a4d);box(10,39,54,42,0xffa69b)
            oval(10,47,25,63,0x151b22);oval(41,47,56,63,0x151b22)
            oval(15,52,22,60,0x8a9395);oval(44,52,51,60,0x8a9395)
            box(6,37,12,43,0xffd987);box(52,37,58,43,0xffd987)
        },
        sprite{
            oval(1,53,63,63,0x252b2d);box(5,39,59,55,0x493824)
            poly(0xc68a2c,5,34,16,29,20,18,42,18,51,29,59,34,59,50,5,50)
            poly(0xc6e1db,21,20,42,20,48,32,16,32)
            box(9,35,55,48,0xe6ba4f);box(10,39,54,42,0xffe4a0)
            oval(10,47,25,63,0x151b22);oval(41,47,56,63,0x151b22)
            oval(15,52,22,60,0x8a9395);oval(44,52,51,60,0x8a9395)
            box(6,37,12,43,0xffe3a0);box(52,37,58,43,0xffe3a0)
        },
        sprite{
            box(30,31,35,63,0x929995);box(10,6,54,36,0xf2ece4);box(12,8,52,34,0xcf302b)
            poly(0xf8f0df,16,15,21,8,43,8,49,15,49,27,43,33,21,33,16,27)
            box(14,16,51,24,0xf0eee6)
            box(20,18,44,21,0xd13630)
        },
        sprite{
            box(30,32,35,64,0x929995);poly(0xe4f1ee,32,3,4,37,60,37)
            poly(0xc93034,32,9,10,33,54,33)
            box(15,27,49,31,0xfcf2e8)
        },
        sprite{
            box(30,32,35,64,0x656E6C);box(7,4,57,37,0xD6DCD2);box(11,8,53,33,0x375C6A)
            box(17,12,23,25,0xe9eeee);box(22,12,42,16,0xe9eeee)
            poly(0xe9eeee,21,27,38,27,42,23,42,29,21,29)
            box(22,31,44,33,0xf9f9e9)
        },
        sprite{
            poly(0xb8b7ab,30,7,35,7,52,53,15,53)
            poly(0xef9028,28,13,38,13,53,55,12,55)
            box(21,52,44,60,0xe6e6d7);box(21,56,44,60,0xcf4d31)
        },
        // Clearly readable low shelter, rather than an enormous blue monolith.
        sprite{
            box(4,11,60,18,0x181D20)
            poly(0x565C5A,2,11,9,4,56,4,62,11,59,15,4,15)
            box(8,7,56,11,0x7D806F)
            box(6,17,11,59,0x30383B);box(52,17,57,59,0x30383B)
            box(9,17,52,21,0x89918B)
            box(12,20,17,51,0x414C4E);box(46,20,51,51,0x414C4E)
            box(17,22,46,48,0x34474B)
            box(20,24,43,38,0x536568)
            box(22,27,41,29,0x7B8784)
            box(16,46,47,50,0x282F31)
            box(15,48,49,53,0x766B54)
            box(18,53,22,61,0x272A2B);box(42,53,46,61,0x272A2B)
            box(4,58,60,62,0x373B3B)
        },
        sprite{
            box(29,38,35,63,0x5c4731)
            oval(8,4,56,42,0x244f34);oval(12,0,52,28,0x3e7842)
            oval(4,20,42,52,0x34623a);oval(28,22,61,50,0x3c743b)
            oval(17,8,41,25,0x6bb36d)
        },
        // Flat roadside billboard, with no humanoid or enemy-like silhouette.
        sprite{
            box(3,7,61,55,0x1c2026);box(6,10,58,52,0x572420)
            box(10,14,54,48,0x281a1c);box(13,16,51,20,0xaf4732)
            box(14,26,50,30,0xc8b08b);box(14,33,50,36,0x8d764e)
            for(k in 0..6)box(13+k*6,43,16+k*6,49,if(k%2==0)0xaa6b29 else 0x27252a)
            box(12,50,52,55,0x9a372a);box(9,56,17,64,0x4a4040);box(47,56,55,64,0x4a4040)
        },
        // Concrete devil effigy, fixed to a square roadside plinth.
        sprite{
            box(4,51,60,64,0x33353a);box(8,46,56,54,0x777970)
            box(11,18,53,49,0x303238)
            poly(0x7c827a,13,21,5,2,24,15,31,18,42,13,57,1,50,23,45,43,20,43)
            poly(0x26272d,18,25,45,25,42,43,23,43)
            box(20,30,28,34,0xe55127);box(37,30,45,34,0xe55127)
            poly(0xe0ccb0,23,39,28,39,25,45)
            poly(0xe0ccb0,38,39,43,39,40,45)
            box(15,48,50,52,0x9a6b36)
        },
        sprite{
            box(5,25,59,35,0xe9ece0);box(7,26,57,33,0xd35635)
            for(k in 0..3)box(11+k*13,26,17+k*13,33,0xf7e9da)
            box(5,35,12,54,0x889b96);box(52,35,59,54,0x889b96)
            box(3,52,15,61,0x2f3237);box(49,52,61,61,0x2f3237)
        })
    val rocket=sprite{oval(10,10,54,54,0x963624);oval(16,16,48,48,0xff9440);oval(25,25,39,39,0xffedb0)}
    val bolt=sprite{oval(7,7,57,57,0x952b23);oval(15,15,49,49,0xff6728);oval(23,23,41,41,0xffeeac)}
    val chromeBolt=sprite{oval(3,3,61,61,0x1c5b69);oval(11,11,53,53,0x62d7d0);oval(23,23,41,41,0xf8ffe7);box(26,0,38,14,0xffe1a4);box(26,50,38,64,0xffe1a4)}
    val toxicBolt=sprite{oval(5,4,59,61,0x3b1f24);poly(0x6a7b25,6,35,20,9,45,10,58,37,44,57,17,58);oval(19,18,46,46,0x9eb83d);oval(26,23,39,37,0xf2f7aa)}
    val wreck=sprite{poly(0x333133,0,52,16,40,31,47,43,37,63,51,58,61,5,61);box(18,47,33,55,0x7a7770);box(39,45,44,49,0x9a3929)}
}
