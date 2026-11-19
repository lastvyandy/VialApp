package cl.vialapp.app.ui.games.doom

import kotlin.math.*
import kotlin.random.Random

internal enum class DoomWeapon(val title:String,val delay:Float,val damage:Int,val reach:Float,val spread:Float){
    PISTOL("PISTOLA",.27f,24,12f,.055f), SHOTGUN("ESCOPETA",.72f,74,7f,.17f), PLASMA("PELAOTRÓN",.15f,21,11f,.09f),
    MINIGUN("MINIGUN",.105f,19,13f,.085f), ROCKET("LANZACOHETES",.82f,150,18f,.045f)
}
internal enum class DoomKind(val hp:Int,val speed:Float,val attack:Float,val damage:Int){
    GUARD(55,.85f,1.65f,9), DRONE(38,1.3f,1.9f,7), BOSS(310,.65f,1.22f,13),
    SHOTGUNNER(72,.93f,1.6f,7), HEAVY(135,.55f,2.15f,11), RUNNER(36,2.0f,1.0f,26),
    FLESH_STALKER(74,1.4f,1.25f,11), MINOXIDIL(820,1.05f,1.17f,15),
    SABOTEUR(62,1.0f,1.65f,8), HACKER(44,1.85f,1.30f,7), RIVAL_GUARD(105,.85f,1.65f,12),
    CORRUPT_TECH(90,1.1f,1.38f,11), ADMINISTRATOR(630,.93f,1.21f,14)
}
internal enum class DoomWeaponPhase { READY, LOWERING, RAISING, FIRING, PUMPING, RELOADING }
internal data class DoomExplosive(val x:Float,val y:Float,val crate:Boolean,var hp:Int=24,var exploded:Boolean=false)
internal data class DoomExplosion(val x:Float,val y:Float,var time:Float=.52f)
internal enum class DoomLootKind { COIN, AMMO, SHELLS, CELL, MEDKIT, ARMOR, SHOTGUN, PLASMA, KEY, MINIGUN, ROCKET, BELT, ROCKET_AMMO, BERSERK, RED_KEY }
internal data class DoomEnemy(val id:Int,val kind:DoomKind,var x:Float,var y:Float,var hp:Int=kind.hp,var cooldown:Float=.8f,var flash:Float=0f,var alert:Boolean=false,var attackFlash:Float=0f,var pattern:Int=0,var deathAge:Float=0f,var moving:Boolean=false,var walkPhase:Float=0f)
internal data class DoomLoot(val kind:DoomLootKind,val x:Float,val y:Float,var taken:Boolean=false)
internal enum class DoomDecorKind { BARREL, CRATE, TERMINAL, LAMP, WRECK,
    CAR_BLUE, CAR_RED, CAR_YELLOW, SIGN_STOP, SIGN_YIELD, SIGN_PEDESTRIAN, CONE,
    BUS_STOP, TREE, CAPILLARY_AD, DEMON_TOTEM, ROAD_BARRIER,
    SERVER_RACK, BROKEN_RACK, CRT_MONITOR, FIBER_CABLE, CHIP_PANEL, SUPAPLEX_DISK,
    VIALAPP_SERVER, TRAFFIC_TERMINAL, COOLANT_TANK, WARNING_PANEL, SERVER_FAN, RAILING, DOCUMENT }
internal data class DoomDecor(val kind:DoomDecorKind,val x:Float,val y:Float,val scale:Float,val lift:Float=0f)
internal data class DoomNote(val x:Float,val y:Float,val title:String,val body:String,var read:Boolean=false)
internal data class DoomRocket(var x:Float,var y:Float,val vx:Float,val vy:Float,var life:Float=2.2f)
internal data class DoomBolt(var x:Float,var y:Float,val vx:Float,val vy:Float,val damage:Int,var life:Float=3f,val chrome:Boolean=false,val toxic:Boolean=false,val bullet:Boolean=false,val altitude:Float=.43f)
internal data class DoomChallenge(val prompt:String,val answers:List<String>,val correct:Int,val explanation:String)
internal data class DoomDoor(val x:Int,val y:Int,val kind:Int,val question:DoomChallenge?=null,var open:Boolean=false)
internal data class DoomRay(val distance:Float,val cell:Int,val u:Float,val side:Int,val mapX:Int=-1,val mapY:Int=-1)
internal data class DoomInput(val forward:Float=0f,val strafe:Float=0f,val turn:Float=0f,val fire:Boolean=false,val punch:Boolean=false,val lookVertical:Float=0f)
internal enum class DoomEvent { SHOT, BLAST, PLASMA, HIT, HURT, COIN, LOOT, SWITCH, DOOR, CORRECT, WRONG, VICTORY, DEFEAT, SECRET, EXPLOSION, RELOAD, ENEMY_SHOT, ENEMY_DEATH, MACHINE_DEATH, MINIGUN_SHOT, ROCKET_SHOT, BERSERK, PUNCH }
internal enum class DoomFaceState(val asset:String){
    NORMAL("02_normal_frente"),RIGHT("03_normal_mirando_derecha_v1"),LEFT("10_normal_mirando_izquierda_v2"),
    ANGRY("11_enojado_frente"),HURT("12_herido_frente"),HURT_RIGHT("13_herido_mirando_derecha"),
    HURT_LEFT("14_herido_mirando_izquierda"),CRITICAL("15_muy_herido_frente"),
    CRITICAL_RIGHT("16_muy_herido_mirando_derecha"),CRITICAL_LEFT("17_muy_herido_mirando_izquierda"),
    BERSERK_HURT("18_berserk_herido_frente"),BERSERK_HEALTHY("19_berserk_sano_frente")
}
internal fun doomFaceState(hp:Int,look:Int,angry:Boolean,berserk:Boolean):DoomFaceState{
    if(hp<=0)return DoomFaceState.CRITICAL
    if(berserk&&!angry)return if(hp<=65)DoomFaceState.BERSERK_HURT else DoomFaceState.BERSERK_HEALTHY
    if(angry)return if(hp<=30)DoomFaceState.CRITICAL else if(hp<=65)DoomFaceState.HURT else DoomFaceState.ANGRY
    return when{
        hp<=30->when{look<0->DoomFaceState.CRITICAL_LEFT;look>0->DoomFaceState.CRITICAL_RIGHT;else->DoomFaceState.CRITICAL}
        hp<=65->when{look<0->DoomFaceState.HURT_LEFT;look>0->DoomFaceState.HURT_RIGHT;else->DoomFaceState.HURT}
        else->when{look<0->DoomFaceState.LEFT;look>0->DoomFaceState.RIGHT;else->DoomFaceState.NORMAL}
    }
}
internal class DoomEngine(seed:Int=19,val level:Int=1){
    val levelTitle get()=when(level){2->"TUNEL BAJO CONTROL";3->"LA FORTALEZA VIAL";4->"ZONA CERO CAPILAR";5->"ASALTO AL SERVIDOR";else->"ESTACIONAMIENTO INFERNAL"}
    val exitX get()=when(level){2->1;3->28;4->32;5->43;else->20}
    val exitY get()=when(level){3->19;4->23;5->33;else->13}
    val bossMaxHp get()=when(level){3->560;4->DoomKind.MINOXIDIL.hp;5->DoomKind.ADMINISTRATOR.hp;else->DoomKind.BOSS.hp}
    val bossHealth get()=enemies.firstOrNull{it.kind==DoomKind.BOSS||it.kind==DoomKind.MINOXIDIL||it.kind==DoomKind.ADMINISTRATOR}?.hp?:0
    val secretTotal get()=doors.count{it.kind==3||it.kind==4}
    val width=when(level){3->30;4->34;5->46;else->22}
    val height=when(level){3->23;4->27;5->39;else->17}
    private val grid=IntArray(width*height){1}
    // 2.5D data is immutable after map generation. Cache tiers once, rather than
    // re-evaluating the stair layout thousands of times in the renderer.
    private val serverTiers=ByteArray(width*height)
    private val random=Random(seed)
    val doors=mutableListOf<DoomDoor>()
    val enemies=mutableListOf<DoomEnemy>()
    val loot=mutableListOf<DoomLoot>()
    val bolts=mutableListOf<DoomBolt>()
    val rockets=mutableListOf<DoomRocket>()
    val decor=mutableListOf<DoomDecor>()
    val notes=mutableListOf<DoomNote>()
    var activeNote:DoomNote?=null
        private set
    val notesRead get()=notes.count{it.read}
    private var serverAmbushMask=0
    val explosives=mutableListOf<DoomExplosive>()
    val explosions=mutableListOf<DoomExplosion>()
    private val events=ArrayList<DoomEvent>()
    var x=2.5f;private set
    var y=3.5f;private set
    var angle=0f;private set
    var hp=100;private set
    var armor=0;private set
    var berserkTime=0f;private set
    var punchSwing=0f;private set
    var punchFromRight=false;private set
    val berserkActive get()=berserkTime>0f
    var cameraElevation=0f;private set
    var lookPitch=0f;private set
    var coins=0;private set
    var kills=0;private set
    var elapsed=0f;private set
    var weapon=DoomWeapon.PISTOL;private set
    private val ammo= intArrayOf(44,0,0,0,0)
    private val owned=booleanArrayOf(true,false,false,false,false)
    private val magazineSize=intArrayOf(10,2,18,36,1)
    private val magazine=intArrayOf(10,0,0,0,0)
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
    private var faceTurn=0
    private var faceTurnTime=0f
    private var facePain=0f
    private var faceGrin=0f
    private var faceFire=0f
    var questionsAnswered=0;private set
    var questionsCorrect=0;private set
    val faceState get()=doomFaceState(hp,if(faceTurnTime>0f)faceTurn else when((elapsed/1.25f).toInt()%6){1->-1;4->1;else->0},
        facePain>0f,berserkActive||faceGrin>0f||faceFire>0f)
    val deathAnimationActive get()=enemies.any{it.hp<=0&&it.deathAge<.72f}
    fun advanceDeathAnimations(dt:Float){
        if(dt<=0f)return
        for(a in enemies)if(a.hp<=0)a.deathAge=(a.deathAge+dt).coerceAtMost(.72f)
    }
    var hasKey=false;private set
    var hasRedKey=false;private set
    var powerRestored=false;private set
    private var coolantDamageCooldown=0f
    var secrets=0;private set
    var pending:DoomDoor?=null;private set
    var message="Carvier: Encuentra el acceso al núcleo.";private set
    var messageTime=4f;private set
    var shotFlash=0f;private set
    var hurtFlash=0f;private set
    var walk=0f;private set
    private var fireCooldown=0f
    private var punchCooldown=0f
    private var reinforcementsSpawned=0
    val bossName get()=when(level){4->"CAPITAN MINOXIDIL";5->"ADMINISTRADOR RIVAL";else->"NUCLEO"}
    val missionObjective get()=when {
        level!=5->""
        !hasKey->"BUSCA LA CREDENCIAL AZUL"
        !powerRestored->"REACTIVA LOS RESPALDOS"
        !hasRedKey->"RECUPERA LA CREDENCIAL ROJA"
        bossAlive->"DEFIENDE EL NUCLEO VIALAPP"
        else->"SERVIDORES A SALVO · BUSCA LA SALIDA"
    }
    var won=false;private set
    val finished get()=won||hp<=0
    val ammunition get()=ammo[weapon.ordinal]
    val score get()=kills*100+coins*35+secrets*250+(if(won)1000 else 0)
    val bossAlive get()=enemies.any{(it.kind==DoomKind.BOSS||it.kind==DoomKind.MINOXIDIL||it.kind==DoomKind.ADMINISTRATOR)&&it.hp>0}
    val weapons get()=DoomWeapon.entries.filter{owned[it.ordinal]}
    init{
        fun room(x1:Int,y1:Int,x2:Int,y2:Int,wall:Int){for(yy in y1-1..y2+1)for(xx in x1-1..x2+1)if(xx in 0 until width&&yy in 0 until height&&grid[yy*width+xx]!=0)grid[yy*width+xx]=wall
            for(yy in y1..y2)for(xx in x1..x2)grid[yy*width+xx]=0}
        if(level==1){
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
        }else if(level==2){
            // A winding tunnel with two mandatory checkpoints and an optional supply bunker.
            room(1,1,5,5,3);room(7,1,15,5,2);room(17,1,20,5,4)
            room(17,7,20,15,1);room(7,7,15,15,3);room(1,7,5,15,4)
            fun gate(xx:Int,yy:Int,kind:Int,q:DoomChallenge?=null){doors+=DoomDoor(xx,yy,kind,q);grid[yy*width+xx]=5}
            gate(6,3,1,DoomChallenge("En un túnel con poca visibilidad, ¿qué debes hacer?",listOf("Encender luces bajas y adaptar la velocidad","Apagar las luces","Adelantar sin visibilidad"),0,"Usa luces bajas y adapta tu velocidad a la visibilidad."))
            gate(16,3,3,DoomChallenge("Si el vehículo de adelante frena, ¿qué te protege?",listOf("Circular pegado","Mantener distancia de seguridad","Acelerar"),1,"La distancia de seguridad permite reaccionar y frenar."))
            gate(11,6,1,DoomChallenge("Ante un peatón en un paso habilitado, ¿qué haces?",listOf("Tocar la bocina","Acelerar","Ceder el paso"),2,"Cede el paso y detente si es necesario."))
            gate(16,11,0);gate(6,11,2)
            for((xx,yy) in listOf(9 to 2,13 to 4,9 to 9,12 to 12,14 to 14,18 to 9,3 to 9,4 to 14))grid[yy*width+xx]=2
            fun foe(k:DoomKind,xx:Float,yy:Float){enemies+=DoomEnemy(enemies.size,k,xx,yy,cooldown=1f+random.nextFloat())}
            foe(DoomKind.GUARD,4.4f,3.5f);foe(DoomKind.RUNNER,4.3f,4.5f)
            foe(DoomKind.SHOTGUNNER,10.5f,3.5f);foe(DoomKind.DRONE,14.5f,2.5f);foe(DoomKind.HEAVY,14.5f,4.5f)
            foe(DoomKind.DRONE,19.5f,3.5f);foe(DoomKind.RUNNER,18.5f,4.5f)
            foe(DoomKind.GUARD,19.2f,8.5f);foe(DoomKind.HEAVY,19.5f,13.5f);foe(DoomKind.DRONE,17.5f,14.5f)
            foe(DoomKind.SHOTGUNNER,13.5f,8.5f);foe(DoomKind.RUNNER,8.5f,10.5f);foe(DoomKind.HEAVY,13.5f,13.5f);foe(DoomKind.DRONE,8.5f,14.5f)
            foe(DoomKind.SHOTGUNNER,4.5f,8.5f);foe(DoomKind.RUNNER,4.5f,12.5f);foe(DoomKind.BOSS,2.5f,12.5f)
            fun supply(k:DoomLootKind,xx:Float,yy:Float){loot+=DoomLoot(k,xx,yy)}
            supply(DoomLootKind.SHOTGUN,3.5f,2.5f);supply(DoomLootKind.AMMO,3.5f,4.5f)
            supply(DoomLootKind.ARMOR,7.5f,3.5f);supply(DoomLootKind.SHELLS,8.5f,4.5f);supply(DoomLootKind.MEDKIT,12.5f,2.5f)
            supply(DoomLootKind.PLASMA,19.5f,2.5f);supply(DoomLootKind.CELL,19.5f,4.5f);supply(DoomLootKind.COIN,18.5f,2.5f)
            supply(DoomLootKind.KEY,19.5f,14.5f);supply(DoomLootKind.MEDKIT,17.5f,12.5f);supply(DoomLootKind.SHELLS,18.5f,8.5f)
            supply(DoomLootKind.SHELLS,10.5f,8.5f);supply(DoomLootKind.PLASMA,14.5f,10.5f);supply(DoomLootKind.CELL,10.5f,14.5f)
            supply(DoomLootKind.MEDKIT,8.5f,12.5f);supply(DoomLootKind.ARMOR,11.5f,14.5f);supply(DoomLootKind.COIN,13.5f,11.5f)
            supply(DoomLootKind.SHELLS,4.5f,11.5f);supply(DoomLootKind.MEDKIT,2.5f,10.5f);supply(DoomLootKind.CELL,4.5f,15.5f);supply(DoomLootKind.COIN,2.5f,15.5f)
            explosives+=listOf(DoomExplosive(12.5f,3.5f,true),DoomExplosive(14.5f,3.5f,false),DoomExplosive(17.5f,9.5f,false),DoomExplosive(19.5f,11.5f,true),DoomExplosive(12.5f,10.5f,true),DoomExplosive(8.5f,13.5f,false),DoomExplosive(3.5f,13.5f,true))
            for((xx,yy) in listOf(2.5f to 1.5f,8.5f to 1.5f,14.5f to 1.5f,19.5f to 1.5f,17.5f to 7.5f,12.5f to 7.5f,2.5f to 7.5f))decor+=DoomDecor(DoomDecorKind.LAMP,xx,yy,.45f,.84f)
            grid[exitY*width+exitX]=6
            message="Carvier: Reactiva los controles del túnel y consigue la credencial."
        }else if(level==3){
            buildFortress()
        }else if(level==4){
            buildCapillaryZone()
        }else{
            require(level==5)
            buildServerAssault()
        }
    }
    private fun buildFortress(){
        // Three mandatory checkpoints along a dogleg route, two independent supply secrets.
        fun room(x1:Int,y1:Int,x2:Int,y2:Int,wall:Int){
            for(yy in y1-1..y2+1)for(xx in x1-1..x2+1)
                if(xx in 0 until width&&yy in 0 until height&&grid[yy*width+xx]!=0)grid[yy*width+xx]=wall
            for(yy in y1..y2)for(xx in x1..x2)grid[yy*width+xx]=0
        }
        room(1,1,6,6,4);room(8,1,19,6,2);room(21,1,28,6,3)
        room(8,8,19,14,4);room(21,8,28,14,1)
        room(1,16,6,21,3);room(8,16,19,21,2);room(21,16,28,21,4)
        fun gate(xx:Int,yy:Int,kind:Int,q:DoomChallenge?=null){doors+=DoomDoor(xx,yy,kind,q);grid[yy*width+xx]=5}
        gate(7,3,1,DoomChallenge("Al conducir con lluvia intensa, ¿qué conviene hacer?",listOf("Acercarse al de adelante","Reducir velocidad y aumentar distancia","Frenar bruscamente"),1,"La lluvia reduce la adherencia: baja la velocidad y deja más distancia."))
        gate(20,3,3,DoomChallenge("¿Para qué sirve el cinturón de seguridad?",listOf("Solo evita multas","Permite conducir más rápido","Reduce el riesgo de lesiones en un choque"),2,"El cinturón reduce el riesgo de lesiones y debe usarse en cada viaje."))
        gate(14,7,1,DoomChallenge("Si notas que tienes sueño al conducir, ¿qué haces?",listOf("Detenerme en un lugar seguro y descansar","Subir la música y seguir","Aumentar la velocidad"),0,"Detente en un lugar seguro y descansa. La fatiga reduce la capacidad de reacción."))
        gate(20,11,0)
        gate(14,15,1,DoomChallenge("Si escuchas una ambulancia de emergencia acercándose, ¿qué haces?",listOf("Competir con ella","Facilitar su paso sin maniobras peligrosas","Detenerme en cualquier lugar"),1,"Facilita el paso de vehículos de emergencia de forma segura."))
        gate(7,19,3,DoomChallenge("¿Qué debes hacer antes de cambiar de pista?",listOf("Señalizar y comprobar espejos y punto ciego","Solo tocar la bocina","Girar inmediatamente"),0,"Comprueba espejos y punto ciego y señaliza antes de cambiar de pista."))
        gate(20,19,2)
        // Reinforced cover islands separate the warehouse and final arena into firing lanes.
        for((xx,yy) in listOf(11 to 2,11 to 1,16 to 5,16 to 6,12 to 8,12 to 9,12 to 10,17 to 14,17 to 13,24 to 9,24 to 8,24 to 16,24 to 17,24 to 20))grid[yy*width+xx]=4
        for((xx,yy) in listOf(4 to 2,9 to 2,12 to 5,17 to 4,22 to 2,26 to 5,9 to 9,11 to 13,16 to 9,18 to 13,23 to 9,26 to 12,3 to 17,5 to 21,9 to 17,12 to 20,17 to 17,22 to 17,26 to 17,23 to 21,27 to 21))grid[yy*width+xx]=2
        fun foe(k:DoomKind,xx:Float,yy:Float){
            val life=if(k==DoomKind.BOSS)bossMaxHp else (k.hp*1.13f).roundToInt()
            enemies+=DoomEnemy(enemies.size,k,xx,yy,hp=life,cooldown=1.1f+random.nextFloat()*.7f)
        }
        foe(DoomKind.GUARD,5.2f,3.5f);foe(DoomKind.RUNNER,5.5f,5.5f);foe(DoomKind.DRONE,2.5f,5.5f)
        foe(DoomKind.GUARD,10.5f,3.5f);foe(DoomKind.SHOTGUNNER,12.5f,2.5f);foe(DoomKind.DRONE,16.5f,2.5f);foe(DoomKind.HEAVY,18.5f,4.5f);foe(DoomKind.RUNNER,17.5f,5.5f)
        foe(DoomKind.HEAVY,24.5f,3.5f);foe(DoomKind.DRONE,27.5f,4.5f);foe(DoomKind.RUNNER,25.5f,5.5f)
        foe(DoomKind.GUARD,14.5f,9.5f);foe(DoomKind.SHOTGUNNER,10.5f,10.5f);foe(DoomKind.DRONE,9.5f,12.5f);foe(DoomKind.RUNNER,13.5f,13.5f);foe(DoomKind.HEAVY,17.5f,11.5f);foe(DoomKind.SHOTGUNNER,18.5f,10.5f)
        foe(DoomKind.HEAVY,24.5f,10.5f);foe(DoomKind.GUARD,22.5f,13.5f);foe(DoomKind.DRONE,27.5f,10.5f);foe(DoomKind.RUNNER,26.5f,14.5f);foe(DoomKind.SHOTGUNNER,28.5f,13.5f)
        foe(DoomKind.HEAVY,10.5f,19.5f);foe(DoomKind.RUNNER,13.5f,17.5f);foe(DoomKind.DRONE,17.5f,20.5f);foe(DoomKind.SHOTGUNNER,18.5f,18.5f)
        foe(DoomKind.HEAVY,4.5f,18.5f);foe(DoomKind.DRONE,2.5f,20.5f)
        foe(DoomKind.HEAVY,24.5f,18.5f);foe(DoomKind.SHOTGUNNER,22.5f,20.5f);foe(DoomKind.RUNNER,26.5f,20.5f);foe(DoomKind.DRONE,27.5f,16.5f);foe(DoomKind.BOSS,26.5f,19.5f)
        fun supply(k:DoomLootKind,xx:Float,yy:Float){loot+=DoomLoot(k,xx,yy)}
        supply(DoomLootKind.SHOTGUN,3.5f,3.5f);supply(DoomLootKind.SHELLS,3.5f,5.5f);supply(DoomLootKind.ARMOR,2.5f,2.5f)
        supply(DoomLootKind.MINIGUN,8.5f,3.5f);supply(DoomLootKind.BELT,10.5f,6.5f);supply(DoomLootKind.MEDKIT,14.5f,2.5f);supply(DoomLootKind.BELT,18.5f,2.5f);supply(DoomLootKind.SHELLS,14.5f,6.5f)
        supply(DoomLootKind.PLASMA,23.5f,3.5f);supply(DoomLootKind.CELL,27.5f,2.5f);supply(DoomLootKind.ARMOR,25.5f,5.5f);supply(DoomLootKind.COIN,26.5f,3.5f)
        supply(DoomLootKind.ROCKET,14.5f,8.5f);supply(DoomLootKind.ROCKET_AMMO,12.5f,11.5f);supply(DoomLootKind.MEDKIT,10.5f,14.5f);supply(DoomLootKind.BELT,15.5f,13.5f);supply(DoomLootKind.ROCKET_AMMO,19.5f,11.5f)
        supply(DoomLootKind.KEY,27.5f,13.5f);supply(DoomLootKind.COIN,24.5f,13.5f);supply(DoomLootKind.ARMOR,23.5f,14.5f);supply(DoomLootKind.BELT,25.5f,11.5f);supply(DoomLootKind.MEDKIT,27.5f,8.5f)
        supply(DoomLootKind.MEDKIT,14.5f,16.5f);supply(DoomLootKind.ROCKET_AMMO,18.5f,19.5f);supply(DoomLootKind.BELT,15.5f,20.5f);supply(DoomLootKind.SHELLS,10.5f,18.5f)
        supply(DoomLootKind.PLASMA,3.5f,19.5f);supply(DoomLootKind.CELL,2.5f,17.5f);supply(DoomLootKind.ARMOR,2.5f,21.5f);supply(DoomLootKind.COIN,5.5f,18.5f)
        supply(DoomLootKind.MEDKIT,21.5f,19.5f);supply(DoomLootKind.ROCKET_AMMO,25.5f,16.5f);supply(DoomLootKind.BELT,24.5f,21.5f);supply(DoomLootKind.COIN,27.5f,20.5f)
        for((xx,yy) in listOf(6.2f to 5.5f,11.5f to 4.5f,18.5f to 5.5f,24.5f to 5.5f,10.5f to 12.5f,17.5f to 10.5f,22.5f to 10.5f,28.5f to 11.5f,11.5f to 18.5f,17.5f to 21.5f,23.5f to 18.5f,27.5f to 18.5f))explosives+=DoomExplosive(xx,yy,explosives.size%2==0)
        for((xx,yy) in listOf(2.5f to 1.5f,10.5f to 1.5f,16.5f to 1.5f,22.5f to 1.5f,10.5f to 8.5f,18.5f to 8.5f,22.5f to 8.5f,2.5f to 16.5f,9.5f to 16.5f,18.5f to 16.5f,21.5f to 16.5f,28.5f to 16.5f))decor+=DoomDecor(DoomDecorKind.LAMP,xx,yy,.42f,.84f)
        grid[exitY*width+exitX]=6
        message="Carvier: La fortaleza está infestada. Recupera la credencial y destruye el núcleo."
    }
    private fun buildCapillaryZone() {
        // Road-level grid: nine open-air districts, with four mandatory traffic controls.
        fun plaza(x1:Int,y1:Int,x2:Int,y2:Int,wall:Int) {
            for(yy in y1-1..y2+1)for(xx in x1-1..x2+1)
                if(xx in 0 until width&&yy in 0 until height&&grid[yy*width+xx]!=0)
                    grid[yy*width+xx]=wall
            for(yy in y1..y2)for(xx in x1..x2)grid[yy*width+xx]=0
        }
        plaza(1,1,8,9,1);plaza(10,1,20,9,2);plaza(22,1,32,9,4)
        plaza(22,11,32,17,3);plaza(10,11,20,17,1);plaza(1,11,8,17,2)
        plaza(1,19,8,25,3);plaza(10,19,20,25,2);plaza(22,19,32,25,4)
        fun barrier(xx:Int,yy:Int,kind:Int,q:DoomChallenge?=null) {
            doors+=DoomDoor(xx,yy,kind,q);grid[yy*width+xx]=5
        }
        barrier(9,5,1,DoomChallenge("Te acercas a una zona escolar. ¿Qué corresponde?",
            listOf("Acelerar para despejar la vía","Reducir la velocidad y observar el entorno","Tocar la bocina a todos"),
            1,"Cerca de colegios, reduce la velocidad y presta especial atención a los peatones."))
        barrier(21,5,0)
        barrier(27,10,1,DoomChallenge("Un auto se detiene antes de un paso de cebra. ¿Qué haces?",
            listOf("Adelantarlo por el costado","Mantener velocidad","Reducir y prepararte para detenerte"),
            2,"Podría estar dejando cruzar a una persona. No adelantes sin visibilidad."))
        barrier(21,14,1,DoomChallenge("En una esquina sin semáforo, hay peatones cruzando. ¿Qué haces?",
            listOf("Respetar su prioridad y cederles el paso","Apurarlos con la bocina","Cruzar entre ellos"),
            0,"Cede el paso a los peatones que cruzan por un lugar habilitado."))
        barrier(9,14,3,DoomChallenge("Antes de abrir la puerta del auto, ¿qué debes revisar?",
            listOf("Solo el celular","Espejos y presencia de ciclistas u otros usuarios","El nivel de combustible"),
            1,"Comprueba que no se acerquen ciclistas, peatones u otros vehículos."))
        barrier(5,18,3,DoomChallenge("¿Qué protege mejor a una persona que circula en bicicleta?",
            listOf("La distancia lateral al adelantar","Pasar lo más cerca posible","Acelerar en cada curva"),
            0,"Mantén distancia lateral suficiente y adelanta solo cuando sea seguro."))
        barrier(15,18,1,DoomChallenge("Una ambulancia viene con señales de emergencia. ¿Qué haces?",
            listOf("Seguirla para avanzar rápido","Facilitar su paso sin maniobras peligrosas","Detenerte de golpe"),
            1,"Facilita su paso con calma y de manera segura."))
        barrier(21,22,2)
        // Unmarked wall passages with actual item caches.
        for(yy in 6..9)grid[yy*width+3]=3
        barrier(3,7,4)
        for(yy in 12..16)grid[yy*width+30]=3
        barrier(30,15,4)
        // Staggered corners, damaged storefronts, angular alleys; marked roads stay clear.
        for((bx,by) in listOf(
            1 to 1,1 to 2,7 to 9,6 to 1,6 to 2,
            10 to 1,11 to 1,12 to 1,10 to 9,11 to 9,17 to 1,
            31 to 1,32 to 1,32 to 2,22 to 8,23 to 8,
            22 to 11,22 to 12,32 to 11,32 to 12,
            10 to 11,10 to 12,20 to 16,20 to 17,
            1 to 11,1 to 12,8 to 16,8 to 17,
            1 to 19,1 to 20,8 to 24,8 to 25,
            10 to 19,10 to 20,19 to 20,20 to 20,
            22 to 19,22 to 20,31 to 25,32 to 25
        ))grid[by*width+bx]=when((bx+by)%3){0->2;1->3;else->4}
        // Actual traffic island corners; the roads remain wide enough to circle enemies.
        for((xx,yy) in listOf(7 to 2,18 to 2,23 to 2,31 to 2,13 to 8,24 to 8,
            30 to 8,23 to 12,30 to 16,11 to 12,19 to 16,2 to 12,7 to 16,
            2 to 20,7 to 24,11 to 20,19 to 24,23 to 20,31 to 20))grid[yy*width+xx]=2
        fun foe(kind:DoomKind,xx:Float,yy:Float) {
            enemies+=DoomEnemy(enemies.size,kind,xx,yy,
                hp=if(kind==DoomKind.MINOXIDIL)bossMaxHp else (kind.hp*1.1f).roundToInt(),
                cooldown=.7f+random.nextFloat()*.95f)
        }
        // A moderate first wave, crossfire on the avenue, then a tough outdoor boss.
        foe(DoomKind.FLESH_STALKER,5.5f,4.5f);foe(DoomKind.GUARD,5.5f,7.5f);foe(DoomKind.DRONE,5.5f,8.5f)
        foe(DoomKind.FLESH_STALKER,12.5f,4.5f);foe(DoomKind.SHOTGUNNER,16.5f,3.5f)
        foe(DoomKind.FLESH_STALKER,19.5f,6.5f);foe(DoomKind.DRONE,14.5f,7.5f);foe(DoomKind.HEAVY,18.5f,8.5f)
        foe(DoomKind.GUARD,24.5f,3.5f);foe(DoomKind.FLESH_STALKER,27.5f,5.5f)
        foe(DoomKind.DRONE,31.5f,6.5f);foe(DoomKind.RUNNER,25.5f,8.5f)
        foe(DoomKind.SHOTGUNNER,24.5f,12.5f);foe(DoomKind.FLESH_STALKER,29.5f,14.5f)
        foe(DoomKind.HEAVY,27.5f,16.5f);foe(DoomKind.DRONE,23.5f,16.5f)
        foe(DoomKind.GUARD,18.5f,12.5f);foe(DoomKind.FLESH_STALKER,14.5f,14.5f)
        foe(DoomKind.SHOTGUNNER,11.5f,16.5f);foe(DoomKind.RUNNER,19.5f,15.5f)
        foe(DoomKind.FLESH_STALKER,4.5f,13.5f);foe(DoomKind.DRONE,6.5f,15.5f)
        foe(DoomKind.HEAVY,3.5f,22.5f);foe(DoomKind.FLESH_STALKER,6.5f,23.5f)
        foe(DoomKind.FLESH_STALKER,12.5f,22.5f);foe(DoomKind.GUARD,17.5f,20.5f)
        foe(DoomKind.SHOTGUNNER,19.5f,23.5f);foe(DoomKind.DRONE,13.5f,24.5f)
        foe(DoomKind.FLESH_STALKER,24.5f,23.5f);foe(DoomKind.HEAVY,25.5f,20.5f)
        foe(DoomKind.FLESH_STALKER,30.5f,21.5f);foe(DoomKind.DRONE,23.5f,24.5f)
        foe(DoomKind.MINOXIDIL,29.0f,23.5f)
        fun supply(k:DoomLootKind,xx:Float,yy:Float){loot+=DoomLoot(k,xx,yy)}
        supply(DoomLootKind.SHOTGUN,3.5f,4.5f);supply(DoomLootKind.SHELLS,2.5f,6.5f)
        supply(DoomLootKind.ARMOR,6.5f,8.5f);supply(DoomLootKind.MEDKIT,8.5f,7.5f)
        supply(DoomLootKind.MINIGUN,11.5f,5.5f);supply(DoomLootKind.BELT,17.5f,5.5f)
        supply(DoomLootKind.PLASMA,22.5f,4.5f);supply(DoomLootKind.CELL,29.5f,8.5f)
        supply(DoomLootKind.COIN,26.5f,2.5f);supply(DoomLootKind.MEDKIT,31.5f,4.5f)
        supply(DoomLootKind.ROCKET,24.5f,13.5f);supply(DoomLootKind.ROCKET_AMMO,31.5f,15.5f)
        supply(DoomLootKind.ARMOR,27.5f,11.5f);supply(DoomLootKind.BELT,18.5f,13.5f)
        supply(DoomLootKind.MEDKIT,14.5f,16.5f);supply(DoomLootKind.SHELLS,12.5f,12.5f)
        supply(DoomLootKind.KEY,11.5f,22.5f);supply(DoomLootKind.ROCKET_AMMO,17.5f,22.5f)
        supply(DoomLootKind.MEDKIT,18.5f,25.5f);supply(DoomLootKind.CELL,12.5f,25.5f)
        supply(DoomLootKind.COIN,5.5f,14.5f);supply(DoomLootKind.BELT,2.5f,15.5f)
        supply(DoomLootKind.ROCKET_AMMO,3.5f,23.5f);supply(DoomLootKind.ARMOR,5.5f,21.5f)
        supply(DoomLootKind.COIN,5.5f,24.5f);supply(DoomLootKind.COIN,16.5f,24.5f)
        supply(DoomLootKind.ARMOR,23.5f,21.5f);supply(DoomLootKind.ROCKET_AMMO,31.5f,24.5f)
        supply(DoomLootKind.MEDKIT,27.5f,25.5f);supply(DoomLootKind.COIN,29.5f,19.5f)
        supply(DoomLootKind.BERSERK,2.5f,8.5f)
        supply(DoomLootKind.BERSERK,25.5f,15.5f)
        supply(DoomLootKind.ROCKET_AMMO,31.5f,15.5f)
        supply(DoomLootKind.ARMOR,31.5f,14.5f)
        // Three-dimensional billboard sprites make these outdoor sectors recognizably VialApp.
        fun prop(k:DoomDecorKind,xx:Float,yy:Float,scale:Float=.82f,lift:Float=0f){
            decor+=DoomDecor(k,xx,yy,scale,lift)
        }
        prop(DoomDecorKind.CAR_BLUE,7.1f,8.1f,.54f)
        prop(DoomDecorKind.CAR_RED,15.2f,3.5f,.55f)
        prop(DoomDecorKind.CAR_YELLOW,27.2f,7.8f,.56f)
        prop(DoomDecorKind.CAR_BLUE,29.5f,12.5f,.53f)
        prop(DoomDecorKind.CAR_RED,17.5f,14.5f,.54f)
        prop(DoomDecorKind.CAR_YELLOW,3.5f,16.3f,.54f)
        prop(DoomDecorKind.CAR_BLUE,15.3f,21.4f,.55f)
        prop(DoomDecorKind.CAR_RED,25.5f,23.5f,.54f)
        prop(DoomDecorKind.SIGN_STOP,7.5f,4.6f,1.1f)
        prop(DoomDecorKind.SIGN_YIELD,26.0f,9.5f,1.1f)
        prop(DoomDecorKind.SIGN_PEDESTRIAN,11.2f,8.6f,.79f)
        prop(DoomDecorKind.SIGN_STOP,20.5f,15.2f,1f)
        prop(DoomDecorKind.SIGN_PEDESTRIAN,24.5f,17.2f,.81f)
        prop(DoomDecorKind.BUS_STOP,31.0f,4.0f,.79f)
        prop(DoomDecorKind.CAPILLARY_AD,18.0f,2.0f,.94f)
        prop(DoomDecorKind.CAPILLARY_AD,5.5f,20.0f,.90f)
        prop(DoomDecorKind.DEMON_TOTEM,5.5f,14.0f,.82f)
        prop(DoomDecorKind.DEMON_TOTEM,30.5f,19.5f,.85f)
        for((xx,yy) in listOf(2.5f to 2.0f,20.0f to 8.5f,22.5f to 2.0f,
            31.5f to 16.5f,2.0f to 11.5f,8.0f to 24.5f))
            prop(DoomDecorKind.TREE,xx,yy,1.25f)
        for((xx,yy) in listOf(8.2f to 8.5f,21.6f to 4.0f,22.6f to 11.5f,
            19.5f to 17.0f,11.0f to 20.0f,22.5f to 25.0f)){
            prop(DoomDecorKind.CONE,xx,yy,.50f)
        }
        for((xx,yy) in listOf(6.0f to 8.6f,19.5f to 8.6f,30.0f to 17.0f,
            19.0f to 24.8f))prop(DoomDecorKind.ROAD_BARRIER,xx,yy,.93f)
        for((xx,yy) in listOf(18.5f to 4.5f,31.0f to 14.5f,
            6.5f to 22.5f,27.5f to 22.5f))
            explosives+=DoomExplosive(xx,yy,explosives.size%2==0)
        grid[exitY*width+exitX]=6
        message="Carvier: Bienvenido a Zona Cero Capilar. Deten al Capitán Minoxidil."
    }
    /**
     * E1M5 · ASALTO AL SERVIDOR
     * Nonlinear VialApp data-center raid. Keys, return loops and a real backup switch
     * are more important than early access to every weapon.
     */
    private fun buildServerAssault(){
        fun room(x1:Int,y1:Int,x2:Int,y2:Int,wall:Int){
            for(yy in y1-1..y2+1)for(xx in x1-1..x2+1)
                if(xx in 0 until width&&yy in 0 until height&&grid[yy*width+xx]!=0)
                    grid[yy*width+xx]=wall
            for(yy in y1..y2)for(xx in x1..x2)grid[yy*width+xx]=0
        }
        fun hall(x1:Int,y1:Int,x2:Int,y2:Int){
            for(yy in y1..y2)for(xx in x1..x2)
                if(xx in 1 until width-1&&yy in 1 until height-1)
                    grid[yy*width+xx]=0
        }
        fun gate(xx:Int,yy:Int,kind:Int,q:DoomChallenge?=null){
            doors+=DoomDoor(xx,yy,kind,q);grid[yy*width+xx]=5
        }

        room(1,14,9,22,2)        // silent maintenance entrance
        room(11,12,23,24,1)      // hub to return to
        room(11,2,27,10,3)       // dark server gallery
        room(2,2,9,10,4)         // green coolant / secret passage
        room(30,2,42,12,2)      // exam services: red key
        room(30,14,42,24,3)     // backup archive: minigun
        room(2,26,24,36,4)      // old server storage: hidden launcher
        room(29,26,44,36,1)     // central core: final administrator

        // Two routes in/out of several areas create genuine return loops.
        fun connections(){
            hall(9,17,11,19)  // entrance -> hub
            hall(16,10,18,12) // hub -> gallery
            hall(9,5,11,7)   // gallery -> coolant
            hall(27,5,30,7)  // gallery -> exams
            hall(23,17,30,19) // hub -> archives
            hall(35,12,37,14) // exams -> archives
            hall(16,24,18,26) // hub -> warehouse
            hall(24,30,29,32) // warehouse -> core
            hall(36,24,38,26) // archive -> core
            hall(5,10,7,14) // coolant -> maintenance
            hall(5,22,7,26) // maintenance -> storage
        }
        connections()

        // Damaged support columns, rack islands and occluded sight lines.
        for((xx,yy) in listOf(
            3 to 2,8 to 3,8 to 9,12 to 4,12 to 5,15 to 4,15 to 5,
            18 to 7,18 to 8,21 to 7,21 to 8,24 to 7,26 to 9,
            13 to 13,12 to 22,22 to 23,21 to 13,
            31 to 3,31 to 4,41 to 3,41 to 4,32 to 10,
            31 to 16,31 to 17,34 to 22,40 to 22,41 to 17,
            4 to 28,5 to 28,9 to 32,10 to 32,12 to 28,16 to 34,18 to 30,23 to 34,
            30 to 28,31 to 28,40 to 28,41 to 28,42 to 29,32 to 34,36 to 34
        ))grid[yy*width+xx]=when((xx+yy)%3){0->2;1->3;else->4}
        connections()

        // Open the actual stairs and upper landings instead of hiding them inside rack pillars.
        for((xx,yy) in listOf(15 to 4,15 to 5,18 to 30,26 to 9))
            if(grid[yy*width+xx]!=5)grid[yy*width+xx]=0

        // Additional damaged partitions create cover and stop the open rooms looking like empty boxes.
        // Every partition is optional cover; the inter-room gates remain the only mandatory locks.
        for((xx,yy) in listOf(
            5 to 15,5 to 21,17 to 16,18 to 16,18 to 21,
            36 to 7,37 to 7,36 to 8,36 to 21,37 to 21,
            8 to 30,8 to 31,19 to 31,34 to 30,34 to 31
        ))if(grid[yy*width+xx]==0)grid[yy*width+xx]=4

        gate(10,18,0,DoomChallenge(
            "CONTROL DE ACCESO · ¿Qué debes hacer antes de cambiar de pista?",
            listOf("Señalizar y revisar espejos y punto ciego","Girar sin mirar","Acelerar para obligar a otros a frenar"),
            0,"Antes de cambiar de pista, señaliza y comprueba espejos y punto ciego."))
        gate(17,11,0,DoomChallenge(
            "SALA DE SERVIDORES · Al aproximarte a un paso peatonal ocupado, ¿qué haces?",
            listOf("Tocas la bocina y sigues","Reduces velocidad y cedes el paso","Adelantas al vehículo detenido"),
            1,"Reduce la velocidad y deja cruzar a los peatones con seguridad."))
        gate(10,6,8,DoomChallenge(
            "BACKUP VIALAPP · Los rivales desconectaron el servicio. ¿Qué es más seguro?",
            listOf("Aislar el equipo comprometido y recuperar un respaldo verificado",
                "Borrar todos los respaldos","Desactivar las alarmas y continuar"),
            0,"BACKUP RESTAURADO. Los servidores auxiliares responden otra vez."))
        gate(29,6,2,DoomChallenge(
            "NODO DE EXÁMENES · ¿Cómo debes conducir con lluvia intensa?",
            listOf("Frenando de golpe","Aumentando velocidad","Reduciendo velocidad y aumentando la distancia"),
            2,"Con lluvia, adapta la velocidad y aumenta la distancia de seguridad."))
        gate(29,18,7,DoomChallenge(
            "ARCHIVO DE RESPALDOS · Si viene una ambulancia en servicio de urgencia, ¿qué haces?",
            listOf("Facilitar su paso sin maniobras peligrosas","Seguirla de cerca","Competir con ella"),
            0,"Facilita el paso de los vehículos de emergencia sin generar otro riesgo."))
        gate(36,13,2,DoomChallenge(
            "NODO DE EXÁMENES · Semáforo rojo: ¿qué debes hacer?",
            listOf("Cruzar si está vacío","Detenerte antes de la línea","Acelerar"),
            1,"Luz roja: debes detenerte antes de la línea de detención."))
        gate(17,25,0,DoomChallenge(
            "ALMACÉN · Si conduces con sueño, ¿qué debes hacer?",
            listOf("Abrir la ventana y acelerar","Detenerte en un lugar seguro para descansar","Subir el volumen de la música"),
            1,"Con fatiga, busca un sitio seguro y descansa antes de continuar."))
        gate(28,31,6,DoomChallenge(
            "ACCESO AL NÚCLEO · En una intersección con luz roja, ¿qué corresponde?",
            listOf("Detenerse antes de la línea","Cruzar si parece despejada","Pasar rápidamente"),
            0,"La luz roja exige detenerte antes de la línea de detención."))
        gate(37,25,6,DoomChallenge(
            "PUERTA DE SERVICIO · Si hay ciclistas al costado, ¿qué debes considerar?",
            listOf("Adelantarlos sin distancia","Tocarles la bocina constantemente","Dejar espacio lateral y adelantarlos solo cuando sea seguro"),
            2,"Respeta el espacio lateral de los ciclistas y no adelantes sin visibilidad."))
        // Every checkpoint spans its WHOLE passage. Otherwise a player can
        // simply walk around the locked tile in a three-cell-wide hallway.
        for((xx,yy) in listOf(
            10 to 17,10 to 19,16 to 11,18 to 11,
            10 to 5,10 to 7,29 to 5,29 to 7,
            29 to 17,29 to 19,35 to 13,37 to 13,
            16 to 25,18 to 25,28 to 30,28 to 32,
            36 to 25,38 to 25
        ))grid[yy*width+xx]=4

        // Five small walled alcoves, each reachable ONLY via an unmarked wall.
        fun secretPocket(cx:Int,cy:Int,entranceDy:Int=1){
            for(yy in cy-1..cy+1)for(xx in cx-1..cx+1)grid[yy*width+xx]=4
            grid[cy*width+cx]=0
            gate(cx,cy+entranceDy,4)
        }
        secretPocket(4,4)   // coolant: Berserk
        secretPocket(20,4,-1)  // upper gallery secret; reached from balcony
        secretPocket(38,5)  // exams: chip/disc Supaplex easter egg
        secretPocket(39,19) // archives: armor
        secretPocket(21,29,-1) // launcher above the warehouse stairs
        secretPocket(5,34)  // forgotten lower storage / medical supplies
        secretPocket(39,10) // rival instructor's hidden exam orders

        fun foe(kind:DoomKind,xx:Float,yy:Float){
            enemies+=DoomEnemy(enemies.size,kind,xx,yy,cooldown=1.05f+random.nextFloat()*.95f)
        }
        foe(DoomKind.SABOTEUR,6.5f,16.5f);foe(DoomKind.SABOTEUR,8.5f,20.5f)
        foe(DoomKind.SABOTEUR,12.5f,18.5f);foe(DoomKind.HACKER,18.5f,14.5f)
        foe(DoomKind.RIVAL_GUARD,21.5f,19.5f);foe(DoomKind.SABOTEUR,14.5f,22.5f)
        foe(DoomKind.SABOTEUR,14.5f,6.5f);foe(DoomKind.HACKER,19.5f,9.5f)
        foe(DoomKind.RIVAL_GUARD,24.5f,5.5f);foe(DoomKind.CORRUPT_TECH,25.5f,8.5f)
        foe(DoomKind.SABOTEUR,4.5f,7.5f);foe(DoomKind.HACKER,7.5f,9.5f)
        foe(DoomKind.CORRUPT_TECH,4.5f,9.5f)
        foe(DoomKind.SABOTEUR,33.5f,6.5f);foe(DoomKind.HACKER,40.5f,5.5f)
        foe(DoomKind.RIVAL_GUARD,41.5f,10.5f);foe(DoomKind.CORRUPT_TECH,34.5f,10.5f)
        foe(DoomKind.RIVAL_GUARD,32.5f,17.5f);foe(DoomKind.HACKER,35.5f,21.5f)
        foe(DoomKind.CORRUPT_TECH,41.5f,22.5f);foe(DoomKind.SABOTEUR,41.5f,16.5f)
        foe(DoomKind.SABOTEUR,6.5f,30.5f);foe(DoomKind.HACKER,13.5f,32.5f)
        foe(DoomKind.RIVAL_GUARD,19.5f,33.5f);foe(DoomKind.CORRUPT_TECH,23.5f,35.5f)
        foe(DoomKind.SABOTEUR,37.5f,27.5f);foe(DoomKind.RIVAL_GUARD,33.5f,32.5f)
        foe(DoomKind.CORRUPT_TECH,41.5f,35.5f)
        // Deliberate pockets of resistance: quiet corridors alternate with multi-enemy crossfire.
        foe(DoomKind.SABOTEUR,16.5f,22.5f)
        foe(DoomKind.RIVAL_GUARD,20.5f,16.5f)
        foe(DoomKind.SABOTEUR,17.5f,8.5f)
        foe(DoomKind.HACKER,22.5f,9.5f)
        foe(DoomKind.RIVAL_GUARD,37.5f,8.5f)
        foe(DoomKind.CORRUPT_TECH,36.5f,16.5f)
        foe(DoomKind.SABOTEUR,14.5f,29.5f)
        foe(DoomKind.RIVAL_GUARD,35.5f,35.5f)
        foe(DoomKind.ADMINISTRATOR,40.0f,30.5f)

        fun item(kind:DoomLootKind,xx:Float,yy:Float){loot+=DoomLoot(kind,xx,yy)}
        item(DoomLootKind.AMMO,2.5f,16.5f)
        item(DoomLootKind.SHOTGUN,8.0f,18.5f) // early, not everything at once
        item(DoomLootKind.SHELLS,8.5f,21.5f)
        item(DoomLootKind.MEDKIT,2.5f,20.5f)
        item(DoomLootKind.COIN,4.5f,17.5f)
        item(DoomLootKind.SHELLS,14.5f,20.5f)
        item(DoomLootKind.MEDKIT,20.5f,17.5f)
        item(DoomLootKind.AMMO,14.5f,13.5f)
        item(DoomLootKind.SHELLS,16.5f,4.5f)
        item(DoomLootKind.KEY,25.5f,3.5f) // blue: exam processing
        item(DoomLootKind.MEDKIT,25.5f,9.5f)
        item(DoomLootKind.AMMO,11.5f,7.5f)
        item(DoomLootKind.COIN,14.5f,9.5f)
        item(DoomLootKind.BERSERK,4.5f,4.5f)
        item(DoomLootKind.ARMOR,7.5f,3.5f)
        item(DoomLootKind.SHELLS,20.5f,4.5f)
        item(DoomLootKind.RED_KEY,40.5f,8.5f) // red: core
        item(DoomLootKind.ARMOR,33.5f,4.5f)
        item(DoomLootKind.CELL,38.5f,5.5f)
        item(DoomLootKind.COIN,31.5f,8.5f)
        item(DoomLootKind.MINIGUN,35.5f,18.5f) // after backup
        item(DoomLootKind.BELT,41.5f,18.5f)
        item(DoomLootKind.MEDKIT,34.5f,23.5f)
        item(DoomLootKind.ARMOR,39.5f,19.5f)
        item(DoomLootKind.CELL,33.5f,21.5f)
        item(DoomLootKind.AMMO,7.5f,33.5f)
        item(DoomLootKind.SHELLS,13.5f,27.5f)
        item(DoomLootKind.ROCKET,21.5f,29.5f) // secret only
        item(DoomLootKind.ROCKET_AMMO,21.6f,29.4f)
        item(DoomLootKind.MEDKIT,18.5f,35.5f)
        item(DoomLootKind.COIN,11.5f,33.5f)
        item(DoomLootKind.PLASMA,30.5f,30.5f) // final stretch
        item(DoomLootKind.CELL,30.5f,33.5f)
        item(DoomLootKind.ARMOR,42.5f,33.5f)
        item(DoomLootKind.MEDKIT,39.5f,35.5f)
        item(DoomLootKind.ROCKET_AMMO,36.5f,35.5f)
        item(DoomLootKind.COIN,41.5f,27.5f)
        item(DoomLootKind.MEDKIT,5.5f,34.5f)
        item(DoomLootKind.ROCKET_AMMO,5.5f,34.6f)
        item(DoomLootKind.COIN,39.5f,10.5f)
        item(DoomLootKind.SHELLS,17.5f,21.5f)
        item(DoomLootKind.SHELLS,22.5f,9.5f)
        item(DoomLootKind.BELT,32.5f,23.5f)
        item(DoomLootKind.MEDKIT,36.5f,31.5f)

        // Physical reports, read with USE. The player reconstructs the sabotage in fragments.
        fun report(xx:Float,yy:Float,title:String,body:String){
            notes+=DoomNote(xx,yy,title,body)
        }
        report(4.5f,19.5f,"01 · REGISTRO DE SEGURIDAD",
            "23:14. Las autoescuelas rivales llegaron juntas y forzaron la entrada. Dijeron que, si VialApp dejaba de funcionar, nadie podría practicar ni rendir sus exámenes. El guardia de recepción no volvió a contestar.")
        report(12.5f,15.5f,"02 · ALERTA DE CARVIER",
            "Las solicitudes de licencias están congeladas. Afuera hay choques en los cruces y las radios de tránsito no paran de sonar. Don Pelao, entra por mantenimiento. La escopeta quedó en el acceso, pero las armas pesadas siguen encerradas.")
        report(16.5f,9.5f,"03 · TURNO DEL DATA CENTER",
            "No dañaron los servidores por accidente. Primero cortaron la replicación, después alteraron las credenciales del nodo de exámenes. Subí por la escalera de servicio para revisar los racks superiores. Hay sangre en la pasarela.")
        report(6.5f,6.5f,"04 · SISTEMA DE REFRIGERACIÓN",
            "El refrigerante se está filtrando por las rejillas. Las puertas falsas de este sector se instalaron para guardar suministros de emergencia. Si todo falla, busca el interruptor que permite recuperar una copia verificada.")
        report(34.5f,11.5f,"05 · CORREO INTERCEPTADO",
            "De: Coordinación de Escuelas Rivales. Orden: mantener VialApp desconectada hasta que nuestras sedes recuperen a sus alumnos. No destruir el núcleo antes de copiar los registros; el Administrador quiere quedarse con ellos.")
        report(32.5f,22.5f,"06 · BITÁCORA DE RESPALDOS",
            "La planta secundaria puede reactivar el servicio, pero primero hay que aislar el equipo comprometido. Encendí la alimentación del archivo. Escuché a varios instructores subir por la pasarela metálica.")
        report(11.5f,29.5f,"07 · ALMACÉN OLVIDADO",
            "En este depósito hay un muro hueco. Alguien escondió allí un lanzacohetes por si la sala central caía. No aparece en los planos. También se guardaron botiquines en el rincón occidental.")
        report(39.5f,10.5f,"08 · ÓRDENES SECRETAS",
            "Una última orden del consorcio: culpar a VialApp del caos en las calles. Los choques no son un fallo del sistema; ellos desconectaron los semáforos de práctica y falsificaron los registros.")
        report(41.5f,34.5f,"09 · MENSAJE SIN ENVIAR",
            "Si estás leyendo esto, llegaste al núcleo. Recupera el respaldo y derriba al Administrador. Ya nadie sabe conducir allá afuera. VialApp tiene que volver a estar en línea.")

        fun prop(kind:DoomDecorKind,xx:Float,yy:Float,scale:Float=.73f,lift:Float=0f){
            decor+=DoomDecor(kind,xx,yy,scale,lift)
        }
        // Interior architecture: racks at perimeters, distinguishable from enemies.
        for((xx,yy) in listOf(12.1f to 3.2f,16.1f to 3.2f,22.1f to 3.2f,
            12.2f to 9.7f,20.2f to 9.7f,26.2f to 9.7f,27.1f to 4.2f,
            31.1f to 3.2f,35.1f to 3.2f,41.1f to 3.2f,
            31.1f to 11.7f,41.1f to 11.7f,31.1f to 15.1f,
            42.1f to 15.1f,31.2f to 23.4f,42.1f to 23.4f,
            30.2f to 27.1f,43.1f to 27.1f,30.2f to 35.9f,43.1f to 35.8f))
            prop(DoomDecorKind.SERVER_RACK,xx,yy,.82f)
        for((xx,yy) in listOf(13.1f to 4.2f,24.2f to 3.2f,
            34.1f to 15.2f,39.1f to 22.8f,37.1f to 29.2f))
            prop(DoomDecorKind.BROKEN_RACK,xx,yy,.68f)
        prop(DoomDecorKind.VIALAPP_SERVER,18.5f,3.5f,1.07f)
        prop(DoomDecorKind.VIALAPP_SERVER,39.3f,30.2f,1.12f)
        prop(DoomDecorKind.TRAFFIC_TERMINAL,32.5f,8.5f,.67f)
        prop(DoomDecorKind.TRAFFIC_TERMINAL,13.5f,17.5f,.60f)
        prop(DoomDecorKind.WARNING_PANEL,9.1f,18.1f,.60f)
        prop(DoomDecorKind.WARNING_PANEL,29.2f,31.4f,.55f)
        prop(DoomDecorKind.CRT_MONITOR,6.5f,19.5f,.50f)
        prop(DoomDecorKind.CRT_MONITOR,40.3f,20.5f,.55f)
        prop(DoomDecorKind.CHIP_PANEL,38.5f,4.5f,.66f)
        prop(DoomDecorKind.SUPAPLEX_DISK,38.5f,5.5f,.48f)
        prop(DoomDecorKind.COOLANT_TANK,3.5f,3.5f,.65f)
        prop(DoomDecorKind.COOLANT_TANK,8.5f,3.5f,.65f)
        prop(DoomDecorKind.SERVER_FAN,4.5f,9.5f,.48f)
        prop(DoomDecorKind.FIBER_CABLE,19.5f,21.5f,.59f)
        prop(DoomDecorKind.FIBER_CABLE,17.5f,32.5f,.59f)
        prop(DoomDecorKind.FIBER_CABLE,35.5f,33.5f,.59f)
        // Broken consoles, shelves and cables at readable scale; keep central walking lanes clear.
        for((xx,yy) in listOf(
            2.0f to 15.1f,2.1f to 22.0f,7.9f to 15.1f,8.0f to 22.0f,
            11.3f to 13.1f,22.6f to 13.1f,11.2f to 23.2f,22.6f to 23.3f,
            12.1f to 2.3f,19.1f to 2.3f,26.2f to 2.3f,
            30.4f to 3.0f,42.4f to 4.1f,30.3f to 11.0f,
            30.4f to 15.0f,42.0f to 23.0f,3.0f to 27.1f,
            10.9f to 35.4f,22.4f to 34.8f,29.9f to 27.2f,43.2f to 35.4f
        ))prop(DoomDecorKind.BROKEN_RACK,xx,yy,.57f)
        for((xx,yy) in listOf(
            3.0f to 16.0f,7.6f to 17.0f,12.1f to 20.0f,21.2f to 18.0f,
            14.0f to 8.7f,23.2f to 8.9f,32.4f to 6.3f,41.5f to 9.6f,
            32.3f to 20.2f,40.2f to 16.2f,8.0f to 29.0f,
            20.2f to 33.0f,33.0f to 31.3f
        ))prop(DoomDecorKind.CRT_MONITOR,xx,yy,.43f)
        for((xx,yy) in listOf(
            6.4f to 22.0f,16.8f to 23.0f,21.2f to 14.0f,
            25.5f to 9.2f,39.5f to 3.2f,30.8f to 22.0f,
            11.2f to 32.0f,18.5f to 28.0f,42.8f to 28.4f
        ))prop(DoomDecorKind.FIBER_CABLE,xx,yy,.40f)
        // Railings outline two overhead service platforms. The ramps remain walkable.
        // Handrails stand ON the raised platforms. Their geometry is not levitating.
        for(xx in 17..25)if(xx!=20)prop(DoomDecorKind.RAILING,xx+.5f,5.8f,.34f)
        for(xx in 35..39)prop(DoomDecorKind.RAILING,xx+.5f,18.3f,.34f)
        for(xx in 18..19)prop(DoomDecorKind.RAILING,xx+.5f,29.4f,.34f)
        // Ceiling panels already contain the warning lights. Do not place floating
        // lamp billboards in mid-air; they looked like disconnected items.
        for((xx,yy) in listOf(16.5f to 8.5f,25.5f to 7.5f,
            37.5f to 17.5f,8.5f to 34.5f,34.5f to 29.5f))
            explosives+=DoomExplosive(xx,yy,explosives.size%2==0)

        x=3.5f;y=18.5f;angle=0f
        grid[exitY*width+exitX]=6
        message="Carvier: Las escuelas rivales tomaron el centro de datos. Busca la escopeta y lee los informes."
        for(yy in 0 until height)for(xx in 0 until width)
            serverTiers[yy*width+xx]=serverRawTier(xx,yy).toByte()
    }
    /** Immutable stage layout. Only its values, not this function, are used per frame. */
    private fun serverRawTier(cx:Int,cy:Int):Int = when {
        // Gallery: two flights reach the upper rack deck and the blue-key gate.
        cx in 12..15&&cy in 6..10 -> (10-cy).coerceIn(0,4)
        cx in 24..27&&cy==6 -> (28-cx).coerceIn(0,4)
        cx in 14..26&&cy in 2..5 -> 4
        // Archives: raised maintenance bay, with supplies and sightlines above.
        cx in 31..34&&cy in 18..23 -> (23-cy).coerceIn(0,5)
        cx in 34..39&&cy in 15..18 -> 5
        // Warehouse: upper storage balcony leads to the secret launcher.
        cx in 15..17&&cy in 30..35 -> (35-cy).coerceIn(0,5)
        cx in 17..23&&cy in 27..30 -> 5
        else->0
    }
    /** Cached stair tiers, shared by navigation, floor projection and collectibles. */
    fun stairTier(cx:Int,cy:Int):Int {
        if(level==5)return if(cx in 0 until width&&cy in 0 until height)
            serverTiers[cy*width+cx].toInt() else 0
        if(level!=4)return 0
        return when {
            cx in 12..16&&cy in 6..9 -> (cy-5).coerceIn(0,4)
            cx in 24..28&&cy in 12..16 -> (cy-11).coerceIn(0,4)
            cx in 25..30&&cy in 20..24 -> (cy-19).coerceIn(0,4)
            else->0
        }
    }
    /** E1M5: query a cached tile; no map searches or dynamic allocations. */
    fun floorHeight(px:Float,py:Float):Float {
        if(level!=5)return 0f
        val cx=floor(px).toInt();val cy=floor(py).toInt()
        return if(cx in 0 until width&&cy in 0 until height)
            serverTiers[cy*width+cx].toInt()*.10f else 0f
    }

    private fun canMove(fromX:Float,fromY:Float,toX:Float,toY:Float,r:Float):Boolean {
        if(solid(toX,toY,r))return false
        // A vertical drop/ledge is not a staircase. The player and rivals must
        // approach high balconies via the actual risers, never teleport through a wall.
        return level!=5||abs(floorHeight(toX,toY)-floorHeight(fromX,fromY))<=.115f
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
        cell(floor(px+r).toInt(),floor(py+r).toInt())!=0||
        (level==4&&decor.any{it.kind in setOf(DoomDecorKind.CAR_BLUE,DoomDecorKind.CAR_RED,DoomDecorKind.CAR_YELLOW)&&hypot(it.x-px,it.y-py)<.21f+r})
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
    private fun nearbyNote(distance:Float=1.25f):DoomNote? =
        notes.filter{!it.read&&hypot(it.x-x,it.y-y)<=distance&&visible(x,y,it.x,it.y)}
            .minByOrNull{hypot(it.x-x,it.y-y)}
    private fun facingNote(horizontal:Float):DoomNote? {
        val target=nearbyNote(1.6f)?:return null
        val direction=atan2(target.y-y,target.x-x)
        val look=angle+atan(horizontal*.66f)
        return if(abs(atan2(sin(direction-look),cos(direction-look)))<.36f)target else null
    }
    private fun readNote(note:DoomNote){
        note.read=true;activeNote=note;events+=DoomEvent.LOOT
    }
    fun closeNote(){activeNote=null}
    private fun serverWave(flag:Int,trigger:Boolean,vararg foes:Triple<DoomKind,Float,Float>){
        if(!trigger||(serverAmbushMask and flag)!=0)return
        serverAmbushMask=serverAmbushMask or flag
        var spawned=0
        for((kind,xx,yy) in foes){
            if(solid(xx,yy,.24f)||hypot(xx-x,yy-y)<2.3f)continue
            enemies+=DoomEnemy(enemies.size,kind,xx,yy,cooldown=1.1f+random.nextFloat()*.8f)
            spawned++
        }
        if(spawned>0)tell("¡Alerta! Refuerzos de las autoescuelas rivales.")
    }
    private fun phase(value:DoomWeaponPhase,duration:Float){
        weaponPhase=value;phaseDuration=duration;weaponProgress=0f
    }
    fun selectWeapon(next:DoomWeapon){
        if(finished||pending!=null||berserkActive||!owns(next)||next==weapon&&queuedWeapon==null)return
        if(next==weapon&&weaponPhase!=DoomWeaponPhase.LOWERING){queuedWeapon=null;return}
        queuedWeapon=next
        if(weaponPhase==DoomWeaponPhase.READY||weaponPhase==DoomWeaponPhase.RELOADING){
            reloadAfterShot=false;phase(DoomWeaponPhase.LOWERING,.19f)
        }
    }
    fun cycleWeapon(){
        if(berserkActive)return
        val available=weapons;val current=queuedWeapon?:weapon
        selectWeapon(available[(available.indexOf(current)+1)%available.size])
    }
    fun previousWeapon(){
        if(berserkActive)return
        val available=weapons;val current=queuedWeapon?:weapon
        selectWeapon(available[(available.indexOf(current)-1+available.size)%available.size])
    }
    fun reload(){
        if(berserkActive)return
        if(finished||pending!=null||weaponPhase!=DoomWeaponPhase.READY||magazineAmmo>=min(ammunition,magazineSize[weapon.ordinal])||ammunition<=0)return
        phase(DoomWeaponPhase.RELOADING,when(weapon){DoomWeapon.PISTOL->.54f;DoomWeapon.SHOTGUN->.65f;DoomWeapon.PLASMA->.46f;DoomWeapon.MINIGUN->.92f;DoomWeapon.ROCKET->.66f})
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
        if(!finished&&pending==null&&delta.isFinite()){
            angle=(angle+delta.coerceIn(-1.2f,1.2f))%(2*PI.toFloat())
            if(abs(delta)>.007f){faceTurn=if(delta>0)1 else -1;faceTurnTime=.55f}
        }
    }
    // The screen and interaction use the same camera ray, not the nearest door behind the player.
    fun screenRay(horizontal:Float):DoomRay {
        val c=cos(angle);val s=sin(angle);val p=horizontal.coerceIn(-1f,1f)*.66f
        return ray(x,y,c-s*p,s+c*p)
    }
    fun targetHint(horizontal:Float=0f):String?{
        if(activeNote==null&&facingNote(horizontal)!=null)return "LEER DOCUMENTO"
        val hit=screenRay(horizontal)
        if(hit.distance>1.7f)return null
        if(hit.cell==6)return if(bossAlive)(when(level){4->"CAPITÁN EN PIE";5->"SABOTAJE EN CURSO";else->"NÚCLEO ACTIVO"}) else "TOCA PARA SALIR"
        if(hit.cell!=5)return null
        val door=doors.firstOrNull{!it.open&&it.x==hit.mapX&&it.y==hit.mapY}?:return null
        return when {door.kind==2&&!hasKey->"REQUIERE LLAVE AZUL";door.kind==6&&!hasRedKey->"REQUIERE LLAVE ROJA";door.kind==7&&!powerRestored->"BACKUP SIN ENERGIA";door.kind==4->"MURO SOSPECHOSO";door.question!=null->"TOCA EL TERMINAL";else->"TOCA PARA ABRIR"}
    }
    fun touchInteract(horizontal:Float,vertical:Float=0f):Boolean{
        if(finished||pending!=null||activeNote!=null)return false
        if(abs(vertical)<.5f){
            facingNote(horizontal)?.let{readNote(it);return true}
        }
        val hit=screenRay(horizontal)
        if(hit.distance>1.7f||abs(vertical)>.5f/(1.32f*hit.distance)+.03f)return false
        if(hit.cell==6){interactExit();return true}
        val door=doors.firstOrNull{!it.open&&it.x==hit.mapX&&it.y==hit.mapY&&hit.cell==5}?:return false
        openDoor(door);return true
    }
    private fun interactExit(){
            if(bossAlive)tell(when(level){4->"Carvier: ¡Derrota al Capitán Minoxidil!";5->"Carvier: El Administrador Rival sigue atacando VialApp.";else->"Carvier: Destruye el núcleo antes de salir."})
            else if(level==5&&!powerRestored)tell("Carvier: Debes restablecer el backup antes de salir.")
            else{won=true;events+=DoomEvent.VICTORY;tell(if(level==5)"¡VialApp vuelve a estar en línea! Servidor asegurado." else "¡Sector recuperado!")}
    }
    // Accessible fallback, also used in the deterministic progression test.
    fun interact(){
        if(finished||pending!=null||activeNote!=null)return
        nearbyNote()?.let{readNote(it);return}
        if(hypot(exitX+.5f-x,exitY+.5f-y)<1.55f){interactExit();return}
        val door=doors.filter{!it.open}.minByOrNull{hypot(it.x+.5f-x,it.y+.5f-y)}
        if(door==null||hypot(door.x+.5f-x,door.y+.5f-y)>1.6f){tell("Acércate a una puerta o terminal.");return}
        openDoor(door)
    }
    private fun openDoor(door:DoomDoor){
        if(door.kind==2&&!hasKey){tell("Se necesita la credencial azul.");return}
        if(door.kind==6&&!hasRedKey){tell("El núcleo requiere una credencial roja.");return}
        if(level==5&&door.kind==6&&!powerRestored){tell("El núcleo necesita el backup activo.");return}
        if(door.kind==7&&!powerRestored){tell("Sin energía: activa el sistema de respaldo.");return}
        if(door.question!=null){pending=door;return}
        door.open=true;events+=DoomEvent.DOOR
        if(door.kind==4){secrets++;events+=DoomEvent.SECRET;tell("¡MURO SECRETO! "+secrets+"/"+secretTotal)}
        else tell("Acceso abierto.")
    }
    fun answer(index:Int){
        val d=pending?:return;val q=d.question?:return
        pending=null;questionsAnswered++
        if(index==q.correct){questionsCorrect++;faceGrin=.85f;d.open=true;events+=DoomEvent.CORRECT;events+=DoomEvent.DOOR
            if(d.kind==3){secrets++;events+=DoomEvent.SECRET}
            if(d.kind==8){powerRestored=true;events+=DoomEvent.SWITCH}
            tell(q.explanation)}
        else{events+=DoomEvent.WRONG;hurt(if(level==5)3 else 8);tell(q.explanation)}
    }
    fun dismissChallenge(){pending=null}
    private fun hurt(n:Int){if(finished||n<=0)return;val absorbed=min(armor,n/2);armor-=absorbed;hp=(hp-n+absorbed).coerceAtLeast(0);hurtFlash=.2f;facePain=.65f;events+=DoomEvent.HURT;if(hp<=0)events+=DoomEvent.DEFEAT}
    private fun damageEnemy(e:DoomEnemy,amount:Int){
        if(e.hp<=0||amount<=0)return
        e.hp=(e.hp-amount).coerceAtLeast(0);e.flash=.17f;e.alert=true;events+=DoomEvent.HIT
        if(e.hp==0)enemyDied(e)
    }
    private fun enemyDied(e:DoomEnemy){
        e.hp=0;e.deathAge=0f;e.attackFlash=0f;kills++
        events+=if(e.kind==DoomKind.DRONE||e.kind==DoomKind.HEAVY||e.kind==DoomKind.BOSS||e.kind==DoomKind.MINOXIDIL)DoomEvent.MACHINE_DEATH else DoomEvent.ENEMY_DEATH
        if(e.kind==DoomKind.BOSS||e.kind==DoomKind.MINOXIDIL||e.kind==DoomKind.ADMINISTRATOR){faceGrin=1.5f;tell(when(level){4->"¡Capitán Minoxidil derrotado! Escapa por la avenida.";5->"¡Administrador Rival neutralizado! El núcleo de VialApp está seguro.";else->"¡Núcleo destruido! Busca la salida."})}
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
    private fun punch(){
        if(!berserkActive||punchCooldown>0f||finished||pending!=null)return
        punchCooldown=.29f;punchSwing=.27f;punchFromRight=!punchFromRight
        faceFire=.3f;events+=DoomEvent.PUNCH
        val target=enemies.filter{it.hp>0&&hypot(it.x-x,it.y-y)<1.5f&&visible(x,y,it.x,it.y)}
            .filter{abs(atan2(sin(atan2(it.y-y,it.x-x)-angle),cos(atan2(it.y-y,it.x-x)-angle)))<.85f}
            .minByOrNull{hypot(it.x-x,it.y-y)}
        if(target!=null){
            damageEnemy(target,164)
            val nx=target.x+cos(angle)*.33f;val ny=target.y+sin(angle)*.33f
            if(target.hp>0&&!solid(nx,ny,.22f)){target.x=nx;target.y=ny}
        }
    }
    private fun shoot(){
        if(berserkActive){punch();return}
        if(fireCooldown>0f||finished||weaponPhase!=DoomWeaponPhase.READY)return
        val w=weapon
        if(ammo[w.ordinal]<=0){fireCooldown=.3f;tell("Sin munición. Cambia de arma.");return}
        if(magazineAmmo<=0){reload();return}
        ammo[w.ordinal]--;magazine[w.ordinal]--;fireCooldown=w.delay;shotFlash=.10f;faceFire=.36f
        reloadAfterShot=magazineAmmo<=0&&ammunition>0
        phase(DoomWeaponPhase.FIRING,when(w){DoomWeapon.PISTOL->.16f;DoomWeapon.SHOTGUN->.33f;DoomWeapon.PLASMA->.12f;DoomWeapon.MINIGUN->.082f;DoomWeapon.ROCKET->.26f})
        events+=when(w){DoomWeapon.PISTOL->DoomEvent.SHOT;DoomWeapon.SHOTGUN->DoomEvent.BLAST;DoomWeapon.PLASMA->DoomEvent.PLASMA;DoomWeapon.MINIGUN->DoomEvent.MINIGUN_SHOT;DoomWeapon.ROCKET->DoomEvent.ROCKET_SHOT}
        if(w==DoomWeapon.ROCKET){rockets+=DoomRocket(x,y,cos(angle)*9f,sin(angle)*9f);return}
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
                DoomLootKind.SHOTGUN->{owned[1]=true;ammo[1]+=12;magazine[1]=min(ammo[1],magazineSize[1]);selectWeapon(DoomWeapon.SHOTGUN);faceGrin=1.25f;tell("Escopeta adquirida.")}
                DoomLootKind.PLASMA->{owned[2]=true;ammo[2]+=45;magazine[2]=min(ammo[2],magazineSize[2]);selectWeapon(DoomWeapon.PLASMA);faceGrin=1.25f;tell("¡Pelaotrón adquirido!")}
                DoomLootKind.KEY->{hasKey=true;tell(if(level==5)"LLAVE AZUL: acceso al nodo de exámenes." else "Credencial del centro de control adquirida.")}
                DoomLootKind.RED_KEY->{hasRedKey=true;tell("LLAVE ROJA: acceso al núcleo principal.")}
                DoomLootKind.MINIGUN->{owned[3]=true;ammo[3]+=108;magazine[3]=min(ammo[3],magazineSize[3]);selectWeapon(DoomWeapon.MINIGUN);faceGrin=1.25f;tell("¡Minigun adquirida!")}
                DoomLootKind.ROCKET->{owned[4]=true;ammo[4]+=6;magazine[4]=1;selectWeapon(DoomWeapon.ROCKET);faceGrin=1.25f;tell("¡Lanzacohetes adquirido!")}
                DoomLootKind.BELT->ammo[3]+=72
                DoomLootKind.ROCKET_AMMO->ammo[4]+=4
                DoomLootKind.BERSERK->{berserkTime=24f;punchSwing=0f;hp=max(hp,75);faceGrin=2f;events+=DoomEvent.BERSERK;tell("¡BERSERK! Puños devastadores durante 24 segundos.")}
            }
            if(l.kind!=DoomLootKind.COIN)events+=DoomEvent.LOOT
        }
    }
    fun step(dt:Float,input:DoomInput=DoomInput()){
        if(finished||pending!=null||activeNote!=null||dt<=0f)return
        val d=dt.coerceAtMost(.05f);elapsed+=d
        fireCooldown=(fireCooldown-d).coerceAtLeast(0f);punchCooldown=(punchCooldown-d).coerceAtLeast(0f);punchSwing=(punchSwing-d).coerceAtLeast(0f);berserkTime=(berserkTime-d).coerceAtLeast(0f);shotFlash=(shotFlash-d).coerceAtLeast(0f);hurtFlash=(hurtFlash-d).coerceAtLeast(0f);messageTime=(messageTime-d).coerceAtLeast(0f)
        advanceWeapon(d)
        advanceDeathAnimations(d)
        coolantDamageCooldown=(coolantDamageCooldown-d).coerceAtLeast(0f)
        faceTurnTime=(faceTurnTime-d).coerceAtLeast(0f);facePain=(facePain-d).coerceAtLeast(0f);faceGrin=(faceGrin-d).coerceAtLeast(0f);faceFire=(faceFire-d).coerceAtLeast(0f)
        blastFlash=(blastFlash-d).coerceAtLeast(0f);pickupFlash=(pickupFlash-d).coerceAtLeast(0f)
        explosions.forEach{it.time-=d};explosions.removeAll{it.time<=0f}
        angle=(angle+input.turn.coerceIn(-1f,1f)*2.65f*d)%(2*PI.toFloat())
        lookPitch=(lookPitch-input.lookVertical.coerceIn(-1f,1f)*1.7f*d).coerceIn(-.3f,.3f)
        val f=input.forward.coerceIn(-1f,1f);val s=input.strafe.coerceIn(-1f,1f);val norm=hypot(f,s).coerceAtLeast(1f)
        val speed=if(berserkActive)4.45f else 3.3f
        val dx=(cos(angle)*f-sin(angle)*s)*d*speed/norm;val dy=(sin(angle)*f+cos(angle)*s)*d*speed/norm
        if(canMove(x,y,x+dx,y,.19f)&&enemies.none{it.hp>0&&hypot(it.x-x-dx,it.y-y)<.36f})x+=dx
        if(canMove(x,y,x,y+dy,.19f)&&enemies.none{it.hp>0&&hypot(it.x-x,it.y-y-dy)<.36f})y+=dy
        if(abs(dx)+abs(dy)>.001f)walk+=d*9f
        val rise=if(level==5)floorHeight(x,y) else stairTier(floor(x).toInt(),floor(y).toInt())*.22f
        cameraElevation+=(rise-cameraElevation)*(d*9f).coerceIn(0f,1f)
        collect()
        if(level==5){
            serverWave(1,hasKey&&x in 15f..27f&&y in 2f..10f,
                Triple(DoomKind.SABOTEUR,18.5f,8.5f),Triple(DoomKind.HACKER,24.5f,9.5f))
            serverWave(2,powerRestored&&x in 30f..42f&&y in 14f..24f,
                Triple(DoomKind.RIVAL_GUARD,39.5f,23.5f),Triple(DoomKind.SABOTEUR,33.5f,22.5f))
            serverWave(4,hasRedKey&&powerRestored&&x in 29f..44f&&y in 26f..36f,
                Triple(DoomKind.CORRUPT_TECH,32.5f,29.5f),Triple(DoomKind.SABOTEUR,41.5f,29.5f))
        }
        if(level==5&&(x in 2.3f..4.9f||x in 7.1f..9.2f)&&y in 7.7f..10.7f&&coolantDamageCooldown<=0f){
            hurt(5);coolantDamageCooldown=.85f
        }
        if(input.fire)shoot();if(input.punch)punch()
        val rocketIterator=rockets.iterator()
        while(rocketIterator.hasNext()){
            val rocket=rocketIterator.next();rocket.life-=d
            // Smaller substeps keep fast rockets from tunnelling through walls or targets.
            var impact=false
            repeat(3){
                if(!impact){
                    val nx=rocket.x+rocket.vx*d/3;val ny=rocket.y+rocket.vy*d/3
                    if(solid(nx,ny,.035f)){impact=true}
                    else{
                        rocket.x=nx;rocket.y=ny
                        if(enemies.any{it.hp>0&&hypot(it.x-nx,it.y-ny)<.38f}||explosives.any{!it.exploded&&hypot(it.x-nx,it.y-ny)<.32f})impact=true
                    }
                }
            }
            if(impact||rocket.life<=0f){rocketIterator.remove();explosion(rocket.x,rocket.y,2.65f,DoomWeapon.ROCKET.damage)}
        }
        val newMinions=ArrayList<DoomEnemy>()
        for(e in enemies){
            if(e.hp<=0)continue
            e.moving=false
            e.flash=(e.flash-d).coerceAtLeast(0f);e.attackFlash=(e.attackFlash-d).coerceAtLeast(0f);e.cooldown-=d
            val distance=hypot(x-e.x,y-e.y)
            val sees=distance<8f&&visible(e.x,e.y,x,y)
            if(sees)e.alert=true
            if(!sees||!e.alert)continue
            if(e.kind==DoomKind.RUNNER&&distance<.85f){
                enemyDied(e);explosion(e.x,e.y,1.65f,64);continue
            }
            val desired=when(e.kind){DoomKind.DRONE->2.7f;DoomKind.SHOTGUNNER,DoomKind.RIVAL_GUARD->2.1f;DoomKind.HEAVY->3f;DoomKind.RUNNER,DoomKind.HACKER->.80f;DoomKind.MINOXIDIL,DoomKind.ADMINISTRATOR->3.2f;DoomKind.FLESH_STALKER,DoomKind.CORRUPT_TECH->2.0f;else->1.35f}
            val lateral=when{e.kind==DoomKind.FLESH_STALKER||e.kind==DoomKind.HACKER->sin(elapsed*4.2f+e.id)*.72f;e.kind==DoomKind.DRONE||e.kind==DoomKind.MINOXIDIL||level==3&&e.kind==DoomKind.BOSS->sin(elapsed*2.7f+e.id)*.82f;else->0f}
            val difficulty=when(level){3->1.17f;4->1.13f;5->1.06f;else->1f}
            if(distance>.05f&&(distance>desired||lateral!=0f)){
                val oldX=e.x;val oldY=e.y
                val approach=if(distance>desired)1f else 0f
                val vx=((x-e.x)*approach-(y-e.y)*lateral)/distance*e.kind.speed*d*difficulty
                val vy=((y-e.y)*approach+(x-e.x)*lateral)/distance*e.kind.speed*d*difficulty
                if(canMove(e.x,e.y,e.x+vx,e.y,.20f)&&enemies.none{it!==e&&it.hp>0&&hypot(it.x-e.x-vx,it.y-e.y)<.42f})e.x+=vx
                if(canMove(e.x,e.y,e.x,e.y+vy,.20f)&&enemies.none{it!==e&&it.hp>0&&hypot(it.x-e.x,it.y-e.y-vy)<.42f})e.y+=vy
                val traveled=hypot(e.x-oldX,e.y-oldY)
                e.moving=traveled>.0015f
                if(e.moving)e.walkPhase+=traveled*12f
            }
            if(e.kind!=DoomKind.RUNNER&&distance<6f&&e.cooldown<=0f){
                e.cooldown=(e.kind.attack+random.nextFloat()*.3f)/(if(e.kind==DoomKind.MINOXIDIL&&e.hp<bossMaxHp/3)1.9f else if((level==3&&e.kind==DoomKind.BOSS||level==5&&e.kind==DoomKind.ADMINISTRATOR)&&e.hp<bossMaxHp/2)1.75f else difficulty);e.attackFlash=.20f;e.pattern++;events+=DoomEvent.ENEMY_SHOT
                val a=atan2(y-e.y,x-e.x)
                val offsets=when(e.kind){
                    DoomKind.SHOTGUNNER,DoomKind.RIVAL_GUARD->floatArrayOf(-.17f,0f,.17f)
                    DoomKind.CORRUPT_TECH->floatArrayOf(-.11f,.11f)
                    DoomKind.ADMINISTRATOR->if(e.hp<bossMaxHp/2)floatArrayOf(-.43f,-.26f,-.10f,0f,.10f,.26f,.43f)else floatArrayOf(-.18f,0f,.18f)
                    DoomKind.FLESH_STALKER->if(e.pattern%2==0)floatArrayOf(-.19f,0f,.19f)else floatArrayOf(0f)
                    DoomKind.HEAVY->floatArrayOf(-.10f,.10f)
                    DoomKind.BOSS->if(level==3&&e.hp<bossMaxHp/2)floatArrayOf(-.45f,-.30f,-.15f,0f,.15f,.30f,.45f)else if(e.pattern%2==0||e.hp<bossMaxHp/2)floatArrayOf(-.27f,-.12f,0f,.12f,.27f)else floatArrayOf(0f)
                    DoomKind.MINOXIDIL->if(e.hp<bossMaxHp/3)floatArrayOf(-.70f,-.55f,-.4f,-.25f,-.1f,0f,.1f,.25f,.4f,.55f,.7f) else if(e.hp<bossMaxHp*2/3)floatArrayOf(-.48f,-.32f,-.16f,0f,.16f,.32f,.48f) else floatArrayOf(-.20f,0f,.20f)
                    else->floatArrayOf(0f)
                }
                val speed=when(e.kind){DoomKind.DRONE->4.4f;DoomKind.HEAVY->2.6f;DoomKind.BOSS->3.7f;DoomKind.MINOXIDIL->3.8f;DoomKind.ADMINISTRATOR->3.7f;else->3.3f}
                for(o in offsets){
                    val dir=a+o
                    val ballistic=level==5
                    val velocity=if(ballistic)10f else speed*difficulty
                    val impact=if(ballistic)(e.kind.damage*difficulty*.62f).roundToInt().coerceAtLeast(3)else(e.kind.damage*difficulty).roundToInt()
                    bolts+=DoomBolt(e.x+cos(dir)*.25f,e.y+sin(dir)*.25f,
                        cos(dir)*velocity,sin(dir)*velocity,impact,
                        life=if(ballistic)1.35f else 3f,
                        chrome=e.kind==DoomKind.MINOXIDIL,
                        toxic=e.kind==DoomKind.FLESH_STALKER,
                        bullet=ballistic,
                        altitude=if(ballistic)floorHeight(e.x,e.y)+.50f else .43f)
                }
                if(e.kind==DoomKind.MINOXIDIL&&e.hp<bossMaxHp*2/3&&e.pattern%4==0&&reinforcementsSpawned<8){
                    for((xx,yy) in listOf(e.x-1.6f to e.y,e.x+1.6f to e.y)){
                        if(reinforcementsSpawned>=8)break
                        if(cell(floor(xx).toInt(),floor(yy).toInt())==0&&!solid(xx,yy,.25f)&&hypot(x-xx,y-yy)>1.2f){
                            newMinions+=DoomEnemy(enemies.size+newMinions.size,DoomKind.FLESH_STALKER,xx,yy,cooldown=.75f,alert=true)
                            reinforcementsSpawned++
                        }
                    }
                }
            }
        }
        if(newMinions.isNotEmpty())enemies+=newMinions
        val it=bolts.iterator()
        while(it.hasNext()){
            val b=it.next();b.x+=b.vx*d;b.y+=b.vy*d;b.life-=d
            if(b.life<=0f||solid(b.x,b.y,.03f)){it.remove();continue}
            if(hypot(b.x-x,b.y-y)<.29f){hurt(b.damage);it.remove()}
        }
    }
}
