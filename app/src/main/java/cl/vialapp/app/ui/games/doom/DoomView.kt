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
        val muzzle=if(e.shotFlash>0f).21f else 0f
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
                val color=if(stripe)0xffad8d38.toInt() else tex[ty*64+tx]
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
        for(l in e.loot)if(!l.taken)sprites+=Sprite(l.x,l.y,.46f,art.loot[l.kind.ordinal])
        for(a in e.enemies){
            if(a.hp>0){
                val phase=if(a.alert)((e.elapsed*6+a.id*.4f).toInt() and 3)else 0
                val size=if(a.kind==DoomKind.BOSS)1.18f else if(a.kind==DoomKind.DRONE).73f else .91f
                val pose=art.enemyFrames[a.kind.ordinal][phase]
                sprites+=Sprite(a.x,a.y,size,pose,a.flash>0f,if(a.kind==DoomKind.DRONE).25f+sin(e.elapsed*4+a.id)*.07f else 0f)
            }else sprites+=Sprite(a.x,a.y,.42f,art.wreck)
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
        lowCanvas.translate(w/2f+bob,h-1f+(if(e.shotFlash>0f)7f else if(moving)abs(cos(e.walk))*2.3f else 0f))
        lowCanvas.scale(gunScale,gunScale)
        drawGun(lowCanvas,e.weapon,e.shotFlash>0f)
        lowCanvas.restore()
        val cx=w/2f;val cy=horizon
        brush.color=if(e.targetHint()!=null)Color.rgb(255,203,88) else Color.argb(160,240,237,213)
        lowCanvas.drawRect(cx-7,cy,cx-3,cy+1,brush);lowCanvas.drawRect(cx+4,cy,cx+8,cy+1,brush)
        lowCanvas.drawRect(cx,cy-7,cx+1,cy-3,brush);lowCanvas.drawRect(cx,cy+4,cx+1,cy+8,brush)
        if(e.targetHint()!=null){lowCanvas.drawRect(cx-1,cy-1,cx+2,cy+2,brush)}
        if(e.hurtFlash>0f){brush.color=Color.argb((e.hurtFlash*480).toInt().coerceIn(0,100),180,12,8);lowCanvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),brush)}
        target.set(0,0,width,height);brush.color=Color.WHITE
        canvas.drawBitmap(bitmap,null,target,brush)
    }
    private fun drawGun(c:Canvas,weapon:DoomWeapon,fire:Boolean){
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
                for(k in 0..4)rect(-a+7,-18f+k*2,a-7,-17f+k*2,0xa46839)
            }
            DoomWeapon.PLASMA->{
                rect(-a+4,-43f,-a+8,-17f,0x65fff2);rect(a-8,-43f,a-4,-17f,0x65fff2)
                for(k in 0..5){rect(-a+9,-48f+k*6,-a+13,-45f+k*6,0x67a9a0);rect(a-13,-48f+k*6,a-9,-45f+k*6,0x67a9a0)}
                rect(-7f,n-4,7f,n+3,0x367b76);rect(-4f,n-5,4f,n+1,0xa3fff0)
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
    val bolt=sprite{oval(7,7,57,57,0x952b23);oval(15,15,49,49,0xff6728);oval(23,23,41,41,0xffeeac)}
    val wreck=sprite{poly(0x333133,0,52,16,40,31,47,43,37,63,51,58,61,5,61);box(18,47,33,55,0x7a7770);box(39,45,44,49,0x9a3929)}
}
