package cl.vialapp.app.ui.games.doom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
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
    private data class Sprite(val x:Float,val y:Float,val size:Float,val art:IntArray,val hit:Boolean=false,val lift:Float=0f)
    private val sprites=ArrayList<Sprite>()
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val e=engine?:return
        val ca=cos(e.angle);val sa=sin(e.angle)
        val planeX=-sa*.66f;val planeY=ca*.66f
        val focal=w/(2f*.66f)
        val moving=hypot(e.x-previousX,e.y-previousY)>.001f
        previousX=e.x;previousY=e.y
        val horizon=h*.5f+(if(moving)sin(e.walk*1.65f)*1.1f else 0f)
        val muzzle=(if(e.shotFlash>0f).21f else 0f)+e.blastFlash*1.1f
        for(y in 0 until h){
            val rowDistance=(focal*.5f)/abs(y+.5f-horizon).coerceAtLeast(.5f)
            val light=(.90f/(1f+rowDistance*.12f)+muzzle/(1f+rowDistance*.4f)).coerceIn(.12f,.89f)
            var fx=e.x+rowDistance*(ca-planeX);var fy=e.y+rowDistance*(sa-planeY)
            val sx=rowDistance*planeX*2/w;val sy=rowDistance*planeY*2/w
            val isFloor=y>=horizon
            val tex=if(isFloor)art.floor else art.ceiling
            for(x in 0 until w){
                val tx=(fx*64).toInt() and 63;val ty=(fy*64).toInt() and 63
                val cx=floor(fx).toInt();val cy=floor(fy).toInt()
                val stripe=isFloor&&((cx in 1..7&&cy==3)||(cx in 9..14&&cy==11))&&ty in 29..32
                val color=if(stripe)0xffad8d38.toInt() else if(isFloor&&cx>=16&&cy>=8&&((tx in 5..8)||(ty in 5..8)))0xff553630.toInt() else if(isFloor&&cx>=9&&cy<7&&cx<=14&&ty in 1..3)0xff627064.toInt() else tex[ty*64+tx]
                val edge=.83f+.17f*(1f-abs(x-w*.5f)/(w*.5f))
                pixels[y*w+x]=shade(color,light*edge)
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
            val tex=if(door!=null)art.doors[door.kind.coerceIn(0,3)] else art.walls[(ray.cell-1).coerceIn(0,5)]
            val light=((1.08f+muzzle)/(1f+d*.17f)*(if(ray.side==1).76f else 1f)).coerceIn(.18f,1.15f)
            for(y in top.toInt().coerceIn(0,h-1)..(top+lineHeight).toInt().coerceIn(0,h-1)){
                val ty=((y-top)*64/lineHeight).toInt().coerceIn(0,63)
                val color=tex[ty*64+tx]
                val edge=.86f+.14f*(1f-abs(x-w*.5f)/(w*.5f))
                val lit=if(ray.cell==5&&ty in 8..24&&tx in 17..47)max(light,.66f)else light
                pixels[y*w+x]=shade(color,lit*edge)
            }
        }
        sprites.clear()
        for(p in e.decor)if(hypot(p.x-e.x,p.y-e.y)>.42f)sprites+=Sprite(p.x,p.y,p.scale,art.decor[p.kind.ordinal],false,p.lift)
        for(b in e.explosives)if(!b.exploded)sprites+=Sprite(b.x,b.y,if(b.crate).70f else .77f,art.explosive[if(b.crate)1 else 0])
        for(b in e.explosions){val frame=((.52f-b.time)/.52f*6).toInt().coerceIn(0,5);sprites+=Sprite(b.x,b.y,1.05f+frame*.13f,art.explosion[frame],true,.12f)}
        for(l in e.loot)if(!l.taken)sprites+=Sprite(l.x,l.y,.46f,art.loot[l.kind.ordinal])
        for(a in e.enemies){
            if(a.hp>0){
                val phase=if(a.alert)((e.elapsed*6+a.id*.4f).toInt() and 3)else 0
                val size=when(a.kind){DoomKind.BOSS->1.33f;DoomKind.DRONE->.73f;DoomKind.HEAVY->1.12f;DoomKind.RUNNER->.66f;else->.91f}
                val pose=if(a.attackFlash>0f)art.enemyAttack[a.kind.ordinal] else art.enemyFrames[a.kind.ordinal][phase]
                sprites+=Sprite(a.x,a.y,size,pose,a.flash>0f,if(a.kind==DoomKind.DRONE).25f+sin(e.elapsed*4+a.id)*.07f else 0f)
            }else{
                val frame=(a.deathAge/.12f).toInt().coerceIn(0,5)
                val size=when(a.kind){DoomKind.BOSS->1.33f;DoomKind.DRONE->.73f;DoomKind.HEAVY->1.12f;DoomKind.RUNNER->.66f;else->.91f}
                sprites+=Sprite(a.x,a.y,size,art.enemyDeath[a.kind.ordinal][frame],frame==0,if(a.kind==DoomKind.DRONE)(.25f*(1f-a.deathAge/.55f)).coerceAtLeast(0f)else 0f)
            }
        }
        for(b in e.bolts)sprites+=Sprite(b.x,b.y,.23f,art.bolt,false,.43f)
        sprites.sortByDescending{(it.x-e.x)*(it.x-e.x)+(it.y-e.y)*(it.y-e.y)}
        for(s in sprites){
            val dx=s.x-e.x;val dy=s.y-e.y;val z=dx*ca+dy*sa
            if(z<.11f)continue
            val px=w*.5f*(1f+(-dx*sa+dy*ca)/(z*.66f))
            val size=(focal*s.size/z).coerceAtMost(2200f)
            val bottom=horizon+focal*.5f/z-focal*s.lift/z;val top=bottom-size;val left=px-size*.5f
            val light=(1.25f/(1f+z*.14f)).coerceIn(.27f,1f)
            for(xx in left.toInt().coerceAtLeast(0)..(left+size).toInt().coerceAtMost(w-1)){
                if(z>=depth[xx])continue
                val tx=((xx-left)*64/size).toInt().coerceIn(0,63)
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
        drawGun(lowCanvas,e.weapon,e.shotFlash>0f,e.weaponPhase,e.weaponProgress)
        lowCanvas.restore()
        val cx=w/2f;val cy=horizon
        brush.color=if(e.targetHint()!=null)Color.rgb(255,203,88) else Color.argb(160,240,237,213)
        lowCanvas.drawRect(cx-7,cy,cx-3,cy+1,brush);lowCanvas.drawRect(cx+4,cy,cx+8,cy+1,brush)
        lowCanvas.drawRect(cx,cy-7,cx+1,cy-3,brush);lowCanvas.drawRect(cx,cy+4,cx+1,cy+8,brush)
        if(e.targetHint()!=null){lowCanvas.drawRect(cx-1,cy-1,cx+2,cy+2,brush)}
        if(e.hurtFlash>0f){brush.color=Color.argb((e.hurtFlash*480).toInt().coerceIn(0,100),180,12,8);lowCanvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),brush)}
        if(e.blastFlash>0f){brush.color=Color.argb((e.blastFlash*240).toInt().coerceIn(0,64),255,151,59);lowCanvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),brush)}
        if(e.pickupFlash>0f){brush.color=Color.argb((e.pickupFlash*125).toInt().coerceIn(0,22),220,200,85);lowCanvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),brush)}
        target.set(0,0,width,height);brush.color=Color.WHITE
        canvas.drawBitmap(bitmap,null,target,brush)
    }
    private fun drawGun(c:Canvas,weapon:DoomWeapon,fire:Boolean,phase:DoomWeaponPhase,progress:Float){
        fun rect(l:Float,t:Float,r:Float,b:Float,color:Int){brush.color=color or 0xff000000.toInt();c.drawRect(l,t,r,b,brush)}
        fun poly(color:Int,vararg p:Float){val path=Path();path.moveTo(p[0],p[1]);for(i in 2 until p.size step 2)path.lineTo(p[i],p[i+1]);path.close();brush.color=color or 0xff000000.toInt();c.drawPath(path,brush)}
        val a=when(weapon){DoomWeapon.PISTOL->13f;DoomWeapon.SHOTGUN->26f;DoomWeapon.PLASMA->32f}
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
        })
    val loot=Array(DoomLootKind.entries.size){i->sprite{
        when(DoomLootKind.entries[i]){
            DoomLootKind.COIN->{oval(15,12,49,59,0x87531a);oval(12,9,46,56,0xe0a82c);oval(17,14,41,51,0xffd759);box(28,23,32,43,0x9a6b21)}
            DoomLootKind.MEDKIT->{box(7,28,57,60,0x303333);box(9,25,55,55,0xbbc3b7);box(25,29,39,51,0xbd372b);box(19,35,45,45,0xbd372b)}
            DoomLootKind.ARMOR->{poly(0x293c3b,9,13,22,6,42,6,55,13,48,53,32,61,16,53);poly(0x67a5a4,12,15,23,10,41,10,52,15,45,50,32,56,19,50);box(26,18,38,46,0x355757)}
            DoomLootKind.KEY->{box(7,18,57,49,0x97721e);box(10,16,55,43,0xf5c955);box(14,20,26,32,0x554626);box(32,21,50,26,0xfbf0b4);box(32,32,48,36,0x776027)}
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
        })
    val bolt=sprite{oval(7,7,57,57,0x952b23);oval(15,15,49,49,0xff6728);oval(23,23,41,41,0xffeeac)}
    val wreck=sprite{poly(0x333133,0,52,16,40,31,47,43,37,63,51,58,61,5,61);box(18,47,33,55,0x7a7770);box(39,45,44,49,0x9a3929)}
}
