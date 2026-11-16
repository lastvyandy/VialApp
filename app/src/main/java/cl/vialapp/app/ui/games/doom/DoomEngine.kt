package cl.vialapp.app.ui.games.doom

import kotlin.math.*
import kotlin.random.Random

internal enum class DoomWeapon(val title:String,val delay:Float,val damage:Int,val reach:Float,val spread:Float){
    PISTOL("PISTOLA",.27f,24,12f,.055f), SHOTGUN("ESCOPETA",.72f,74,7f,.17f), PLASMA("PELAOTRÓN",.15f,21,11f,.09f)
}
internal enum class DoomKind(val hp:Int,val speed:Float,val attack:Float,val damage:Int){
    GUARD(55,.85f,1.65f,9), DRONE(38,1.3f,1.9f,7), BOSS(310,.65f,1.22f,13),
    SHOTGUNNER(72,.93f,1.6f,7), HEAVY(135,.55f,2.15f,11), RUNNER(36,2.0f,1.0f,26)
}
internal enum class DoomWeaponPhase { READY, LOWERING, RAISING, FIRING, PUMPING, RELOADING }
internal data class DoomExplosive(val x:Float,val y:Float,val crate:Boolean,var hp:Int=24,var exploded:Boolean=false)
internal data class DoomExplosion(val x:Float,val y:Float,var time:Float=.52f)
internal enum class DoomLootKind { COIN, AMMO, SHELLS, CELL, MEDKIT, ARMOR, SHOTGUN, PLASMA, KEY }
internal data class DoomEnemy(val id:Int,val kind:DoomKind,var x:Float,var y:Float,var hp:Int=kind.hp,var cooldown:Float=.8f,var flash:Float=0f,var alert:Boolean=false,var attackFlash:Float=0f,var pattern:Int=0)
internal data class DoomLoot(val kind:DoomLootKind,val x:Float,val y:Float,var taken:Boolean=false)
internal enum class DoomDecorKind { BARREL, CRATE, TERMINAL, LAMP, WRECK }
internal data class DoomDecor(val kind:DoomDecorKind,val x:Float,val y:Float,val scale:Float,val lift:Float=0f)
internal data class DoomBolt(var x:Float,var y:Float,val vx:Float,val vy:Float,val damage:Int,var life:Float=3f)
internal data class DoomChallenge(val prompt:String,val answers:List<String>,val correct:Int,val explanation:String)
internal data class DoomDoor(val x:Int,val y:Int,val kind:Int,val question:DoomChallenge?=null,var open:Boolean=false)
internal data class DoomRay(val distance:Float,val cell:Int,val u:Float,val side:Int,val mapX:Int=-1,val mapY:Int=-1)
internal data class DoomInput(val forward:Float=0f,val strafe:Float=0f,val turn:Float=0f,val fire:Boolean=false)
internal enum class DoomEvent { SHOT, BLAST, PLASMA, HIT, HURT, COIN, LOOT, SWITCH, DOOR, CORRECT, WRONG, VICTORY, DEFEAT, SECRET, EXPLOSION, RELOAD, ENEMY_SHOT }
internal class DoomEngine(seed:Int=19){
    val width=22
    val height=17
    private val grid=IntArray(width*height){1}
    private val random=Random(seed)
    val doors=mutableListOf<DoomDoor>()
    val enemies=mutableListOf<DoomEnemy>()
    val loot=mutableListOf<DoomLoot>()
    val bolts=mutableListOf<DoomBolt>()
    val decor=mutableListOf<DoomDecor>()
    val explosives=mutableListOf<DoomExplosive>()
    val explosions=mutableListOf<DoomExplosion>()
    private val events=ArrayList<DoomEvent>()
    var x=2.5f;private set
    var y=3.5f;private set
    var angle=0f;private set
    var hp=100;private set
    var armor=0;private set
    var coins=0;private set
    var kills=0;private set
    var elapsed=0f;private set
    var weapon=DoomWeapon.PISTOL;private set
    private val ammo= intArrayOf(44,0,0)
    private val owned=booleanArrayOf(true,false,false)
    private val magazineSize=intArrayOf(10,2,18)
    private val magazine=intArrayOf(10,0,0)
    var weaponPhase=DoomWeaponPhase.READY;private set
    var weaponProgress=0f;private set
    private var queuedWeapon:DoomWeapon?=null
    private var phaseDuration=1f
    private var reloadAfterShot=false
    val magazineAmmo get()=min(ammunition,magazine[weapon.ordinal])
    fun ammoFor(w:DoomWeapon)=ammo[w.ordinal]
    fun owns(w:DoomWeapon)=owned[w.ordinal]
    val weaponLowering get()=when(weaponPhase){
        DoomWeaponPhase.LOWERING->weaponProgress
        DoomWeaponPhase.RAISING->1f-weaponProgress
        DoomWeaponPhase.RELOADING->sin(weaponProgress*PI.toFloat())*.50f
        DoomWeaponPhase.PUMPING->sin(weaponProgress*PI.toFloat())*.19f
        else->0f
    }
    val recoil get()=if(weaponPhase==DoomWeaponPhase.FIRING)sin(weaponProgress*PI.toFloat()).coerceAtLeast(0f)else 0f
    var blastFlash=0f;private set
    var pickupFlash=0f;private set
    var hasKey=false;private set
    var secrets=0;private set
    var pending:DoomDoor?=null;private set
    var message="Carvier: Encuentra el acceso al núcleo.";private set
    var messageTime=4f;private set
    var shotFlash=0f;private set
    var hurtFlash=0f;private set
    var walk=0f;private set
    private var fireCooldown=0f
    var won=false;private set
    val finished get()=won||hp<=0
    val ammunition get()=ammo[weapon.ordinal]
    val score get()=kills*100+coins*35+secrets*250+(if(won)1000 else 0)
    val bossAlive get()=enemies.any{it.kind==DoomKind.BOSS&&it.hp>0}
    val weapons get()=DoomWeapon.entries.filter{owned[it.ordinal]}
    init{
        fun room(x1:Int,y1:Int,x2:Int,y2:Int,wall:Int){for(yy in y1-1..y2+1)for(xx in x1-1..x2+1)if(xx in 0 until width&&yy in 0 until height&&grid[yy*width+xx]!=0)grid[yy*width+xx]=wall
            for(yy in y1..y2)for(xx in x1..x2)grid[yy*width+xx]=0}
        room(1,1,7,6,1);room(9,1,14,6,2);room(16,1,20,6,3)
        room(1,8,7,15,2);room(9,8,14,15,1);room(16,8,20,15,4)
        fun door(x:Int,y:Int,kind:Int,q:DoomChallenge?=null){doors+=DoomDoor(x,y,kind,q);grid[y*width+x]=5}
        door(8,3,0)
        door(11,7,1,DoomChallenge("¿Qué señal exige detenerse por completo?",listOf("CEDA EL PASO","PARE","VELOCIDAD MÁXIMA"),1,"PARE: detente completamente y cede el paso."))
        door(8,11,0)
        door(15,11,2)
        door(15,4,3,DoomChallenge("El semáforo está rojo. ¿Qué haces?",listOf("Me detengo","Acelero","Paso si está vacío"),0,"La luz roja obliga a detenerse."))
        // Concrete pillars make rooms less boxy and provide cover.
        for((xx,yy) in listOf(3 to 1,6 to 5,10 to 2,13 to 5,3 to 12,6 to 14,10 to 13,13 to 9,17 to 9,19 to 14))grid[yy*width+xx]=2
        fun enemy(k:DoomKind,xx:Float,yy:Float){enemies+=DoomEnemy(enemies.size,k,xx,yy,cooldown=.8f+random.nextFloat()*.8f)}
        enemy(DoomKind.GUARD,6.2f,3.5f);enemy(DoomKind.DRONE,5.1f,5.8f)
        enemy(DoomKind.SHOTGUNNER,12.9f,3.3f);enemy(DoomKind.RUNNER,10.3f,5.5f)
        enemy(DoomKind.HEAVY,13.3f,11.1f);enemy(DoomKind.SHOTGUNNER,4.5f,9.6f)
        enemy(DoomKind.DRONE,5.5f,14.5f);enemy(DoomKind.DRONE,18.5f,4.6f)
        enemy(DoomKind.HEAVY,17.2f,12.5f);enemy(DoomKind.BOSS,19.2f,12.7f)
        enemy(DoomKind.RUNNER,6.8f,13.5f);enemy(DoomKind.SHOTGUNNER,19.3f,9.5f);enemy(DoomKind.DRONE,12.4f,14.8f)
        fun item(k:DoomLootKind,xx:Float,yy:Float){loot+=DoomLoot(k,xx,yy)}
        item(DoomLootKind.COIN,4.4f,3.5f);item(DoomLootKind.AMMO,3.5f,5.5f)
        item(DoomLootKind.SHOTGUN,9.6f,3.5f);item(DoomLootKind.SHELLS,12.5f,2.5f)
        item(DoomLootKind.MEDKIT,13.5f,4.5f);item(DoomLootKind.COIN,10.5f,6.4f)
        item(DoomLootKind.ARMOR,18.5f,2.5f);item(DoomLootKind.PLASMA,19.4f,4.5f);item(DoomLootKind.CELL,17.5f,5.5f);item(DoomLootKind.COIN,19.5f,5.5f)
        item(DoomLootKind.MEDKIT,12.5f,10.2f);item(DoomLootKind.SHELLS,10.5f,14.5f)
        item(DoomLootKind.KEY,2.5f,14.5f);item(DoomLootKind.COIN,2.5f,11.5f);item(DoomLootKind.AMMO,5.5f,10.5f)
        item(DoomLootKind.MEDKIT,14.5f,12.5f);item(DoomLootKind.SHELLS,16.4f,11.5f)
        item(DoomLootKind.COIN,18.3f,10.5f)
        item(DoomLootKind.AMMO,12.5f,6.5f);item(DoomLootKind.SHELLS,6.5f,13.5f);item(DoomLootKind.MEDKIT,17.5f,14.5f);item(DoomLootKind.ARMOR,6.5f,8.5f)
        explosives+=listOf(DoomExplosive(5.7f,1.7f,false),DoomExplosive(6.8f,2.1f,true),DoomExplosive(12.2f,2.5f,true),DoomExplosive(13.8f,3.0f,false),DoomExplosive(12.5f,12.5f,false),DoomExplosive(5.8f,9.0f,true),DoomExplosive(18.1f,11.8f,true),DoomExplosive(19.6f,11.3f,false))
        grid[13*width+20]=6
        // Edge-of-room silhouettes give every sector its own identity without blocking the route.
        decor+=listOf(
            DoomDecor(DoomDecorKind.CRATE,1.42f,1.45f,.74f),
            DoomDecor(DoomDecorKind.BARREL,7.48f,1.45f,.63f),
            DoomDecor(DoomDecorKind.BARREL,7.50f,2.05f,.58f),
            DoomDecor(DoomDecorKind.WRECK,1.48f,5.65f,.76f),
            DoomDecor(DoomDecorKind.LAMP,4.4f,1.14f,.39f,.84f),
            DoomDecor(DoomDecorKind.TERMINAL,9.45f,1.47f,.91f),
            DoomDecor(DoomDecorKind.CRATE,14.50f,1.52f,.72f),
            DoomDecor(DoomDecorKind.LAMP,13.8f,1.16f,.4f,.83f),
            DoomDecor(DoomDecorKind.TERMINAL,16.47f,1.52f,.94f),
            DoomDecor(DoomDecorKind.CRATE,20.5f,1.5f,.88f),
            DoomDecor(DoomDecorKind.BARREL,20.5f,5.5f,.68f),
            DoomDecor(DoomDecorKind.BARREL,1.48f,8.52f,.72f),
            DoomDecor(DoomDecorKind.CRATE,7.52f,14.52f,.84f),
            DoomDecor(DoomDecorKind.LAMP,4.2f,8.15f,.43f,.86f),
            DoomDecor(DoomDecorKind.WRECK,10.4f,8.48f,.75f),
            DoomDecor(DoomDecorKind.TERMINAL,14.52f,8.52f,.86f),
            DoomDecor(DoomDecorKind.BARREL,14.48f,15.48f,.62f),
            DoomDecor(DoomDecorKind.LAMP,17.61f,8.18f,.47f,.84f),
            DoomDecor(DoomDecorKind.CRATE,20.5f,8.5f,.72f),
            DoomDecor(DoomDecorKind.BARREL,16.5f,15.5f,.74f)
        )
    }
    fun cell(cx:Int,cy:Int):Int{
        if(cx !in 0 until width||cy !in 0 until height)return 1
        val t=grid[cy*width+cx]
        return if(t==5&&doors.any{it.x==cx&&it.y==cy&&it.open})0 else t
    }
    fun solid(px:Float,py:Float,r:Float=.19f):Boolean =
        cell(floor(px-r).toInt(),floor(py-r).toInt())!=0||
        cell(floor(px+r).toInt(),floor(py-r).toInt())!=0||
        cell(floor(px-r).toInt(),floor(py+r).toInt())!=0||
        cell(floor(px+r).toInt(),floor(py+r).toInt())!=0
    fun ray(px:Float,py:Float,dx:Float,dy:Float):DoomRay{
        var mx=floor(px).toInt();var my=floor(py).toInt()
        val ddx=if(abs(dx)<.00001f)1e6f else abs(1f/dx);val ddy=if(abs(dy)<.00001f)1e6f else abs(1f/dy)
        val sx=if(dx<0)-1 else 1;val sy=if(dy<0)-1 else 1
        var ax=(if(dx<0)px-mx else mx+1-px)*ddx;var ay=(if(dy<0)py-my else my+1-py)*ddy
        repeat(80){
            val side:Int;val d:Float
            if(ax<ay){d=ax;ax+=ddx;mx+=sx;side=0}else{d=ay;ay+=ddy;my+=sy;side=1}
            val c=cell(mx,my)
            if(c!=0){val u=if(side==0)py+d*dy else px+d*dx
                return DoomRay(d.coerceAtLeast(.025f),c,u-floor(u),side,mx,my)}
        }
        return DoomRay(80f,1,0f,0)
    }
    fun visible(ax:Float,ay:Float,bx:Float,by:Float):Boolean{
        val dx=bx-ax;val dy=by-ay;val d=hypot(dx,dy)
        return d<.01f||ray(ax,ay,dx/d,dy/d).distance>=d-.06f
    }
    fun drainEvents():List<DoomEvent> = events.toList().also{events.clear()}
    private fun tell(s:String){message=s;messageTime=3.5f}
    private fun phase(value:DoomWeaponPhase,duration:Float){
        weaponPhase=value;phaseDuration=duration;weaponProgress=0f
    }
    fun selectWeapon(next:DoomWeapon){
        if(finished||pending!=null||!owns(next)||next==weapon&&queuedWeapon==null)return
        if(next==weapon&&weaponPhase!=DoomWeaponPhase.LOWERING){queuedWeapon=null;return}
        queuedWeapon=next
        if(weaponPhase==DoomWeaponPhase.READY||weaponPhase==DoomWeaponPhase.RELOADING){
            reloadAfterShot=false;phase(DoomWeaponPhase.LOWERING,.19f)
        }
    }
    fun cycleWeapon(){
        val available=weapons;val current=queuedWeapon?:weapon
        selectWeapon(available[(available.indexOf(current)+1)%available.size])
    }
    fun reload(){
        if(finished||pending!=null||weaponPhase!=DoomWeaponPhase.READY||magazineAmmo>=min(ammunition,magazineSize[weapon.ordinal])||ammunition<=0)return
        phase(DoomWeaponPhase.RELOADING,when(weapon){DoomWeapon.PISTOL->.54f;DoomWeapon.SHOTGUN->.65f;DoomWeapon.PLASMA->.46f})
        events+=DoomEvent.RELOAD
    }
    private fun advanceWeapon(dt:Float){
        if(weaponPhase==DoomWeaponPhase.READY)return
        weaponProgress=(weaponProgress+dt/phaseDuration).coerceAtMost(1f)
        if(weaponProgress<1f)return
        when(weaponPhase){
            DoomWeaponPhase.LOWERING->{weapon=queuedWeapon?:weapon;queuedWeapon=null;events+=DoomEvent.SWITCH;phase(DoomWeaponPhase.RAISING,.22f)}
            DoomWeaponPhase.RAISING->{phase(DoomWeaponPhase.READY,1f);if(queuedWeapon!=null&&queuedWeapon!=weapon)phase(DoomWeaponPhase.LOWERING,.19f)}
            DoomWeaponPhase.FIRING->{phase(DoomWeaponPhase.READY,1f);if(queuedWeapon!=null&&queuedWeapon!=weapon){reloadAfterShot=false;phase(DoomWeaponPhase.LOWERING,.19f)}else if(reloadAfterShot){reloadAfterShot=false;reload()}else if(weapon==DoomWeapon.SHOTGUN){phase(DoomWeaponPhase.PUMPING,.30f);events+=DoomEvent.RELOAD}}
            DoomWeaponPhase.PUMPING->{phase(DoomWeaponPhase.READY,1f);if(queuedWeapon!=null&&queuedWeapon!=weapon)phase(DoomWeaponPhase.LOWERING,.19f)}
            DoomWeaponPhase.RELOADING->{magazine[weapon.ordinal]=min(ammunition,magazineSize[weapon.ordinal]);phase(DoomWeaponPhase.READY,1f);if(queuedWeapon!=null&&queuedWeapon!=weapon)phase(DoomWeaponPhase.LOWERING,.19f)}
            DoomWeaponPhase.READY->Unit
        }
    }
    fun look(delta:Float){
        if(!finished&&pending==null&&delta.isFinite())angle=(angle+delta.coerceIn(-1.2f,1.2f))%(2*PI.toFloat())
    }
    // The screen and interaction use the same camera ray, not the nearest door behind the player.
    fun screenRay(horizontal:Float):DoomRay {
        val c=cos(angle);val s=sin(angle);val p=horizontal.coerceIn(-1f,1f)*.66f
        return ray(x,y,c-s*p,s+c*p)
    }
    fun targetHint(horizontal:Float=0f):String?{
        val hit=screenRay(horizontal)
        if(hit.distance>1.7f)return null
        if(hit.cell==6)return if(bossAlive)"NÚCLEO ACTIVO" else "TOCA PARA SALIR"
        if(hit.cell!=5)return null
        val door=doors.firstOrNull{!it.open&&it.x==hit.mapX&&it.y==hit.mapY}?:return null
        return when{door.kind==2&&!hasKey->"REQUIERE CREDENCIAL";door.question!=null->"TOCA EL TERMINAL";else->"TOCA PARA ABRIR"}
    }
    fun touchInteract(horizontal:Float,vertical:Float=0f):Boolean{
        if(finished||pending!=null)return false
        val hit=screenRay(horizontal)
        if(hit.distance>1.7f||abs(vertical)>.5f/(1.32f*hit.distance)+.03f)return false
        if(hit.cell==6){interactExit();return true}
        val door=doors.firstOrNull{!it.open&&it.x==hit.mapX&&it.y==hit.mapY&&hit.cell==5}?:return false
        openDoor(door);return true
    }
    private fun interactExit(){
            if(bossAlive)tell("Carvier: Destruye el núcleo antes de salir.")
            else{won=true;events+=DoomEvent.VICTORY;tell("¡Sector recuperado!")}
    }
    // Accessible fallback, also used in the deterministic progression test.
    fun interact(){
        if(finished||pending!=null)return
        if(hypot(20.5f-x,13.5f-y)<1.55f){interactExit();return}
        val door=doors.filter{!it.open}.minByOrNull{hypot(it.x+.5f-x,it.y+.5f-y)}
        if(door==null||hypot(door.x+.5f-x,door.y+.5f-y)>1.6f){tell("Acércate a una puerta o terminal.");return}
        openDoor(door)
    }
    private fun openDoor(door:DoomDoor){
        if(door.kind==2&&!hasKey){tell("Se necesita la credencial de acceso.");return}
        if(door.question!=null){pending=door;return}
        door.open=true;events+=DoomEvent.DOOR;tell("Acceso abierto.")
    }
    fun answer(index:Int){
        val d=pending?:return;val q=d.question?:return
        pending=null
        if(index==q.correct){d.open=true;events+=DoomEvent.CORRECT;events+=DoomEvent.DOOR
            if(d.kind==3){secrets++;events+=DoomEvent.SECRET};tell(q.explanation)}
        else{events+=DoomEvent.WRONG;hurt(8);tell(q.explanation)}
    }
    fun dismissChallenge(){pending=null}
    private fun hurt(n:Int){if(finished||n<=0)return;val absorbed=min(armor,n/2);armor-=absorbed;hp=(hp-n+absorbed).coerceAtLeast(0);hurtFlash=.2f;events+=DoomEvent.HURT;if(hp<=0)events+=DoomEvent.DEFEAT}
    private fun damageEnemy(e:DoomEnemy,amount:Int){
        if(e.hp<=0||amount<=0)return
        e.hp=(e.hp-amount).coerceAtLeast(0);e.flash=.17f;e.alert=true;events+=DoomEvent.HIT
        if(e.hp==0){kills++;if(e.kind==DoomKind.BOSS)tell("¡Núcleo destruido! Busca la salida.")}
    }
    private fun explosion(cx:Float,cy:Float,radius:Float=2.35f,power:Int=110){
        explosions+=DoomExplosion(cx,cy)
        events+=DoomEvent.EXPLOSION
        if(hypot(cx-x,cy-y)<5f)blastFlash=.25f
        fun damage(px:Float,py:Float):Int{
            val d=hypot(px-cx,py-cy)
            return if(d<radius&&visible(cx,cy,px,py))(power*(1f-d/radius)).toInt()else 0
        }
        for(a in enemies)damageEnemy(a,damage(a.x,a.y))
        hurt((damage(x,y)*.55f).toInt())
        for(b in explosives)if(!b.exploded&&damage(b.x,b.y)>0)damageExplosive(b,damage(b.x,b.y))
    }
    private fun damageExplosive(b:DoomExplosive,amount:Int){
        if(b.exploded||amount<=0)return
        b.hp-=amount
        if(b.hp<=0){b.exploded=true;explosion(b.x,b.y)}
    }
    private fun shoot(){
        if(fireCooldown>0f||finished||weaponPhase!=DoomWeaponPhase.READY)return
        val w=weapon
        if(ammo[w.ordinal]<=0){fireCooldown=.3f;tell("Sin munición. Cambia de arma.");return}
        if(magazineAmmo<=0){reload();return}
        ammo[w.ordinal]--;magazine[w.ordinal]--;fireCooldown=w.delay;shotFlash=.10f
        reloadAfterShot=magazineAmmo<=0&&ammunition>0
        phase(DoomWeaponPhase.FIRING,when(w){DoomWeapon.PISTOL->.16f;DoomWeapon.SHOTGUN->.33f;DoomWeapon.PLASMA->.12f})
        events+=when(w){DoomWeapon.PISTOL->DoomEvent.SHOT;DoomWeapon.SHOTGUN->DoomEvent.BLAST;DoomWeapon.PLASMA->DoomEvent.PLASMA}
        val candidates=enemies.filter{e->e.hp>0&&hypot(e.x-x,e.y-y)<=w.reach&&visible(x,y,e.x,e.y)}
            .map{e->e to abs(atan2(sin(atan2(e.y-y,e.x-x)-angle),cos(atan2(e.y-y,e.x-x)-angle)))}
            .filter{(e,a)->a<=w.spread+.23f/hypot(e.x-x,e.y-y).coerceAtLeast(.4f)}
            .sortedBy{(e,a)->hypot(e.x-x,e.y-y)+a*5}
        val box=explosives.filter{!it.exploded&&hypot(it.x-x,it.y-y)<=w.reach&&visible(x,y,it.x,it.y)}
            .map{it to abs(atan2(sin(atan2(it.y-y,it.x-x)-angle),cos(atan2(it.y-y,it.x-x)-angle)))}
            .filter{(b,a)->a<=w.spread+.25f/hypot(b.x-x,b.y-y).coerceAtLeast(.4f)}
            .minByOrNull{(b,a)->hypot(b.x-x,b.y-y)+a*5}
        if(box!=null&&(candidates.isEmpty()||hypot(box.first.x-x,box.first.y-y)+box.second*5<hypot(candidates[0].first.x-x,candidates[0].first.y-y)+candidates[0].second*5)){
            damageExplosive(box.first,w.damage);return
        }
        val targets=if(w==DoomWeapon.SHOTGUN)candidates.take(2)else candidates.take(1)
        for((i,pair) in targets.withIndex()){
            val e=pair.first;val d=hypot(e.x-x,e.y-y)
            val damage=if(w==DoomWeapon.SHOTGUN)(w.damage*(1f-d/12f)/(i+1)).toInt() else w.damage
            damageEnemy(e,damage)
        }
    }
    private fun collect(){
        for(l in loot){if(l.taken||hypot(l.x-x,l.y-y)>.49f)continue
            if(l.kind==DoomLootKind.MEDKIT&&hp==100)continue
            l.taken=true;pickupFlash=.16f
            when(l.kind){
                DoomLootKind.COIN->{coins++;events+=DoomEvent.COIN}
                DoomLootKind.AMMO->ammo[0]+=24
                DoomLootKind.SHELLS->ammo[1]+=12
                DoomLootKind.CELL->ammo[2]+=45
                DoomLootKind.MEDKIT->hp=(hp+35).coerceAtMost(100)
                DoomLootKind.ARMOR->armor=(armor+65).coerceAtMost(100)
                DoomLootKind.SHOTGUN->{owned[1]=true;ammo[1]+=12;magazine[1]=min(ammo[1],magazineSize[1]);selectWeapon(DoomWeapon.SHOTGUN);tell("Escopeta adquirida.")}
                DoomLootKind.PLASMA->{owned[2]=true;ammo[2]+=45;magazine[2]=min(ammo[2],magazineSize[2]);selectWeapon(DoomWeapon.PLASMA);tell("¡Pelaotrón adquirido!")}
                DoomLootKind.KEY->{hasKey=true;tell("Credencial del centro de control adquirida.")}
            }
            if(l.kind!=DoomLootKind.COIN)events+=DoomEvent.LOOT
        }
    }
    fun step(dt:Float,input:DoomInput=DoomInput()){
        if(finished||pending!=null||dt<=0f)return
        val d=dt.coerceAtMost(.05f);elapsed+=d
        fireCooldown=(fireCooldown-d).coerceAtLeast(0f);shotFlash=(shotFlash-d).coerceAtLeast(0f);hurtFlash=(hurtFlash-d).coerceAtLeast(0f);messageTime=(messageTime-d).coerceAtLeast(0f)
        advanceWeapon(d)
        blastFlash=(blastFlash-d).coerceAtLeast(0f);pickupFlash=(pickupFlash-d).coerceAtLeast(0f)
        explosions.forEach{it.time-=d};explosions.removeAll{it.time<=0f}
        angle=(angle+input.turn.coerceIn(-1f,1f)*2.65f*d)%(2*PI.toFloat())
        val f=input.forward.coerceIn(-1f,1f);val s=input.strafe.coerceIn(-1f,1f);val norm=hypot(f,s).coerceAtLeast(1f)
        val dx=(cos(angle)*f-sin(angle)*s)*d*3.3f/norm;val dy=(sin(angle)*f+cos(angle)*s)*d*3.3f/norm
        if(!solid(x+dx,y)&&enemies.none{it.hp>0&&hypot(it.x-x-dx,it.y-y)<.36f})x+=dx
        if(!solid(x,y+dy)&&enemies.none{it.hp>0&&hypot(it.x-x,it.y-y-dy)<.36f})y+=dy
        if(abs(dx)+abs(dy)>.001f)walk+=d*9f
        collect();if(input.fire)shoot()
        for(e in enemies){
            if(e.hp<=0)continue
            e.flash=(e.flash-d).coerceAtLeast(0f);e.attackFlash=(e.attackFlash-d).coerceAtLeast(0f);e.cooldown-=d
            val distance=hypot(x-e.x,y-e.y)
            val sees=distance<8f&&visible(e.x,e.y,x,y)
            if(sees)e.alert=true
            if(!sees||!e.alert)continue
            if(e.kind==DoomKind.RUNNER&&distance<.85f){
                e.hp=0;kills++;explosion(e.x,e.y,1.65f,64);continue
            }
            val desired=when(e.kind){DoomKind.DRONE->2.7f;DoomKind.SHOTGUNNER->2.1f;DoomKind.HEAVY->3f;DoomKind.RUNNER->.55f;else->1.35f}
            val lateral=if(e.kind==DoomKind.DRONE)sin(elapsed*2.7f+e.id)*.82f else 0f
            if(distance>.05f&&(distance>desired||lateral!=0f)){
                val approach=if(distance>desired)1f else 0f
                val vx=((x-e.x)*approach-(y-e.y)*lateral)/distance*e.kind.speed*d
                val vy=((y-e.y)*approach+(x-e.x)*lateral)/distance*e.kind.speed*d
                if(!solid(e.x+vx,e.y,.20f)&&enemies.none{it!==e&&it.hp>0&&hypot(it.x-e.x-vx,it.y-e.y)<.42f})e.x+=vx
                if(!solid(e.x,e.y+vy,.20f)&&enemies.none{it!==e&&it.hp>0&&hypot(it.x-e.x,it.y-e.y-vy)<.42f})e.y+=vy
            }
            if(e.kind!=DoomKind.RUNNER&&distance<6f&&e.cooldown<=0f){
                e.cooldown=e.kind.attack+random.nextFloat()*.3f;e.attackFlash=.20f;e.pattern++;events+=DoomEvent.ENEMY_SHOT
                val a=atan2(y-e.y,x-e.x)
                val offsets=when(e.kind){
                    DoomKind.SHOTGUNNER->floatArrayOf(-.17f,0f,.17f)
                    DoomKind.HEAVY->floatArrayOf(-.10f,.10f)
                    DoomKind.BOSS->if(e.pattern%2==0||e.hp<e.kind.hp/2)floatArrayOf(-.27f,-.12f,0f,.12f,.27f)else floatArrayOf(0f)
                    else->floatArrayOf(0f)
                }
                val speed=when(e.kind){DoomKind.DRONE->4.4f;DoomKind.HEAVY->2.6f;DoomKind.BOSS->3.7f;else->3.3f}
                for(o in offsets){val dir=a+o;bolts+=DoomBolt(e.x+cos(dir)*.25f,e.y+sin(dir)*.25f,cos(dir)*speed,sin(dir)*speed,e.kind.damage)}
            }
        }
        val it=bolts.iterator()
        while(it.hasNext()){
            val b=it.next();b.x+=b.vx*d;b.y+=b.vy*d;b.life-=d
            if(b.life<=0f||solid(b.x,b.y,.03f)){it.remove();continue}
            if(hypot(b.x-x,b.y-y)<.29f){hurt(b.damage);it.remove()}
        }
    }
}
