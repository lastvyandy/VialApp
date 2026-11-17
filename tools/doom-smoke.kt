package cl.vialapp.app.ui.games.doom

import kotlin.math.*

private fun walk(e:DoomEngine,x:Float,y:Float){
    repeat(500){
        val dx=x-e.x;val dy=y-e.y;val d=hypot(dx,dy)
        if(d<.10f)return
        val f=(dx*cos(e.angle)+dy*sin(e.angle))/d
        val s=(-dx*sin(e.angle)+dy*cos(e.angle))/d
        e.step(.025f,DoomInput(f,s))
    }
    error("Blocked walking to "+x+","+y+" at "+e.x+","+e.y)
}
fun main(){
    val combat=DoomEngine(19)
    check(combat.enemies.size==13&&combat.enemies.map{it.kind}.toSet().size==6)
    check(combat.solid(.2f,3.5f))
    check(combat.ray(combat.x,combat.y,1f,0f).cell==5)
    val guard=combat.enemies.first()
    check(combat.visible(combat.x,combat.y,guard.x,guard.y))
    repeat(100){combat.step(.025f,DoomInput(fire=true))}
    check(guard.hp==0){"Pistol failed to defeat initial guard"}
    check(combat.kills>=1&&combat.ammunition<44)
    val combatEvents=combat.drainEvents()
    check(combatEvents.contains(DoomEvent.SHOT)&&combatEvents.contains(DoomEvent.ENEMY_DEATH))
    check(guard.deathAge in 0f.. .72f)
    combat.advanceDeathAnimations(.8f);check(guard.deathAge==.72f)
    check(doomFaceState(100,0,false,false)==DoomFaceState.NORMAL)
    check(doomFaceState(100,-1,false,false)==DoomFaceState.LEFT)
    check(doomFaceState(100,1,false,false)==DoomFaceState.RIGHT)
    check(doomFaceState(100,0,true,false)==DoomFaceState.ANGRY)
    check(doomFaceState(50,-1,false,false)==DoomFaceState.HURT_LEFT)
    check(doomFaceState(50,1,false,false)==DoomFaceState.HURT_RIGHT)
    check(doomFaceState(50,0,true,false)==DoomFaceState.HURT)
    check(doomFaceState(15,-1,false,false)==DoomFaceState.CRITICAL_LEFT)
    check(doomFaceState(15,1,false,false)==DoomFaceState.CRITICAL_RIGHT)
    check(doomFaceState(0,1,false,true)==DoomFaceState.CRITICAL)
    check(doomFaceState(100,0,false,true)==DoomFaceState.BERSERK_HEALTHY)
    check(doomFaceState(40,0,false,true)==DoomFaceState.BERSERK_HURT)
    val face=DoomEngine()
    face.look(.1f);check(face.faceState==DoomFaceState.RIGHT)
    face.look(-.1f);check(face.faceState==DoomFaceState.LEFT)
    face.step(.025f,DoomInput(fire=true));check(face.faceState==DoomFaceState.BERSERK_HEALTHY)
    // Independent route fixture: isolate navigation and educational gates from combat.
    val e=DoomEngine()
    e.enemies.forEach{it.hp=0}
    walk(e,4.4f,3.5f);check(e.coins==1)
    check(!e.touchInteract(0f)){"Distant doors must not open"}
    walk(e,7.4f,3.5f)
    check(e.targetHint()=="TOCA PARA ABRIR")
    e.look(1.0f);check(!e.touchInteract(0f)){"Looking away must not open the nearby door"}
    e.look(-1.0f)
    check(!e.touchInteract(0f,1.2f)){"A tap outside the projected door must not open it"}
    check(e.touchInteract(0f));check(e.doors[0].open)
    walk(e,9.6f,3.5f);check(e.owns(DoomWeapon.SHOTGUN));repeat(24){e.step(.025f)};check(e.weapon==DoomWeapon.SHOTGUN)
    walk(e,14.4f,4.5f);e.interact();check(e.pending?.kind==3)
    e.answer(0);check(e.secrets==1&&e.doors[4].open)
    walk(e,16.5f,4.5f);walk(e,19.4f,4.5f);repeat(24){e.step(.025f)};check(e.weapon==DoomWeapon.PLASMA)
    walk(e,15.5f,4.5f);walk(e,11.5f,4.5f);walk(e,11.5f,6.4f)
    e.look(1.2f);e.look((PI/2).toFloat()-1.2f)
    check(e.targetHint()=="TOCA EL TERMINAL")
    check(e.touchInteract(0f));check(e.pending?.kind==1)
    val hp=e.hp;e.answer(0);check(e.hp<hp&&!e.doors[1].open)
    check(e.touchInteract(0f));val elapsed=e.elapsed;e.step(.05f,DoomInput(1f));check(e.elapsed==elapsed)
    e.answer(1);check(e.doors[1].open&&e.pending==null)
    check(e.questionsAnswered==3&&e.questionsCorrect==2)
    walk(e,11.5f,8.5f);walk(e,11.5f,11.5f);walk(e,14.5f,11.5f)
    e.interact();check(!e.doors[3].open&&!e.hasKey)
    walk(e,9.5f,11.5f);e.interact();check(e.doors[2].open)
    walk(e,7.5f,11.5f);walk(e,2.5f,11.5f);walk(e,2.5f,14.5f);check(e.hasKey)
    walk(e,2.5f,11.5f);walk(e,9.5f,11.5f);walk(e,14.5f,11.5f)
    e.interact();check(e.doors[3].open)
    walk(e,16.5f,11.5f);walk(e,18.5f,11.5f);walk(e,19.5f,13.5f)
    e.interact();check(e.won&&e.finished)
    val blast=DoomEngine()
    blast.enemies.forEach{it.hp=0}
    blast.explosives.clear()
    blast.explosives+=DoomExplosive(4.25f,3.5f,false)
    blast.explosives+=DoomExplosive(5.25f,3.5f,true)
    val ammo=blast.ammunition
    blast.step(.025f,DoomInput(fire=true))
    check(blast.explosives.all{it.exploded}&&blast.explosions.size==2){"Shot must cause a chain reaction"}
    check(blast.hp<100&&blast.ammunition==ammo-1)
    check(blast.drainEvents().count{it==DoomEvent.EXPLOSION}==2)
    repeat(30){blast.step(.025f)}
    check(blast.explosions.isEmpty())
    repeat(12){blast.step(.025f,DoomInput(fire=true))}
    repeat(25){blast.step(.025f)}
    val reserve=blast.ammunition
    blast.reload();check(blast.weaponPhase==DoomWeaponPhase.RELOADING)
    repeat(30){blast.step(.025f)}
    check(blast.ammunition==reserve&&blast.magazineAmmo==min(reserve,10))
    val second=DoomEngine(level=2)
    check(second.enemies.size==17&&second.secretTotal==1&&second.doors.count{it.kind==1}==2)
    second.enemies.forEach{it.hp=0}
    check(second.cell(second.exitX,second.exitY)==6)
    walk(second,5.5f,3.5f);second.interact();check(second.pending!=null);second.answer(0)
    check(second.doors.first().open)
    walk(second,7.5f,3.5f);walk(second,11.5f,3.5f);walk(second,11.5f,5.5f)
    second.interact();check(second.pending!=null);second.answer(2)
    walk(second,11.5f,7.5f);walk(second,11.5f,11.5f);walk(second,15.5f,11.5f)
    second.interact();walk(second,17.5f,11.5f);walk(second,19.5f,11.5f);walk(second,19.5f,14.5f);check(second.hasKey)
    walk(second,19.5f,11.5f);walk(second,17.5f,11.5f);walk(second,15.5f,11.5f);walk(second,7.5f,11.5f)
    second.interact();walk(second,5.5f,11.5f);walk(second,2.5f,11.5f);walk(second,2.5f,13.5f);second.interact();check(second.won)
    val third=DoomEngine(level=3)
    check(third.width==30&&third.height==23&&third.enemies.size==33)
    check(third.bossHealth==560&&third.secretTotal==2&&third.doors.count{it.kind==1}==3)
    check(third.loot.all{!third.solid(it.x,it.y,.12f)})
    check(third.enemies.all{!third.solid(it.x,it.y,.19f)})
    third.enemies.forEach{it.hp=0}
    walk(third,3.5f,3.5f);check(third.owns(DoomWeapon.SHOTGUN))
    walk(third,6.5f,3.5f);third.interact();check(third.pending?.kind==1)
    third.answer(1);walk(third,8.5f,3.5f);check(third.owns(DoomWeapon.MINIGUN))
    repeat(36){third.step(.025f)};check(third.weapon==DoomWeapon.MINIGUN)
    val belt=third.ammunition;third.drainEvents()
    repeat(12){third.step(.025f,DoomInput(fire=true))}
    check(third.ammunition<belt&&third.drainEvents().contains(DoomEvent.MINIGUN_SHOT))
    walk(third,19.5f,3.5f);third.interact();check(third.pending?.kind==3);third.answer(2)
    walk(third,23.5f,3.5f);check(third.owns(DoomWeapon.PLASMA)&&third.secrets==1)
    walk(third,19.5f,3.5f);walk(third,14.5f,3.5f);walk(third,14.5f,6.5f);third.interact();third.answer(0)
    walk(third,14.5f,8.5f);check(third.owns(DoomWeapon.ROCKET))
    repeat(44){third.step(.025f)};check(third.weapon==DoomWeapon.ROCKET)
    third.explosives.clear()
    third.drainEvents()
    val rocketsBefore=third.ammunition
    third.step(.025f,DoomInput(fire=true))
    check(third.rockets.size==1&&third.ammunition==rocketsBefore-1)
    check(third.drainEvents().contains(DoomEvent.ROCKET_SHOT))
    repeat(100){third.step(.025f)}
    check(third.rockets.isEmpty()&&third.drainEvents().contains(DoomEvent.EXPLOSION))
    walk(third,14.5f,11.5f);walk(third,19.5f,11.5f);third.interact()
    walk(third,27.5f,11.5f);walk(third,27.5f,13.5f);check(third.hasKey)
    walk(third,27.5f,11.5f);walk(third,14.5f,11.5f);walk(third,14.5f,14.5f);third.interact();check(third.pending?.kind==1);third.answer(1)
    walk(third,14.5f,19.5f);walk(third,8.5f,19.5f);third.interact();check(third.pending?.kind==3);third.answer(0)
    walk(third,6.5f,19.5f);walk(third,3.5f,19.5f);check(third.secrets==2)
    walk(third,8.5f,19.5f);walk(third,19.5f,19.5f);third.interact()
    walk(third,27.5f,19.5f);third.interact()
    check(third.won&&third.questionsAnswered==5&&third.questionsCorrect==5)
    // Isolate the new boss's attack pattern and test the seven-shot phase.
    val bossFight=DoomEngine(level=3)
    val boss=bossFight.enemies.last()
    bossFight.enemies.filter{it!==boss}.forEach{it.hp=0}
    boss.x=5.5f;boss.y=3.5f;boss.hp=200;boss.cooldown=0f
    bossFight.step(.025f)
    check(bossFight.bolts.size==7&&bossFight.bossAlive)
    // E1M4 must be distinctly outdoors and navigable without skipping mandatory lessons.
    val fourth=DoomEngine(level=4)
    check(fourth.levelTitle=="ZONA CERO CAPILAR"&&fourth.width==34&&fourth.height==27)
    check(fourth.bossName=="CAPITAN MINOXIDIL"&&fourth.bossHealth==820)
    check(fourth.enemies.size==33&&fourth.enemies.count{it.kind==DoomKind.FLESH_STALKER}>=8)
    check(fourth.enemies.last().kind==DoomKind.MINOXIDIL)
    check(fourth.secretTotal==4&&fourth.doors.count{it.kind==1}==4&&fourth.doors.count{it.kind==4}==2)
    check(fourth.loot.count{it.kind==DoomLootKind.BERSERK}==2)
    check(fourth.stairTier(14,8)>fourth.stairTier(14,6))
    check(fourth.decor.count{it.kind in listOf(DoomDecorKind.CAR_BLUE,DoomDecorKind.CAR_RED,DoomDecorKind.CAR_YELLOW)}>=8)
    check(fourth.decor.count{it.kind in listOf(DoomDecorKind.SIGN_STOP,DoomDecorKind.SIGN_YIELD,DoomDecorKind.SIGN_PEDESTRIAN)}>=5)
    check(fourth.loot.all{!fourth.solid(it.x,it.y,.10f)}){"E1M4 pickup blocked by scenery"}
    check(fourth.enemies.all{!fourth.solid(it.x,it.y,.18f)}){"E1M4 enemy spawned inside a car or wall"}
    fourth.enemies.forEach{it.hp=0}
    walk(fourth,8.4f,3.5f);walk(fourth,8.4f,5.5f);fourth.interact()
    check(fourth.pending?.question?.correct==1);fourth.answer(1)
    walk(fourth,11.5f,5.5f);check(fourth.owns(DoomWeapon.MINIGUN))
    walk(fourth,20.4f,5.5f);fourth.interact();check(fourth.doors[1].open)
    walk(fourth,23.5f,5.5f);walk(fourth,25.5f,5.5f)
    walk(fourth,25.5f,9.5f);walk(fourth,27.5f,9.5f)
    fourth.interact();check(fourth.pending?.question?.correct==2);fourth.answer(2)
    walk(fourth,27.5f,12.5f);walk(fourth,22.5f,14.5f);fourth.interact()
    check(fourth.pending?.question?.correct==0);fourth.answer(0)
    walk(fourth,18.5f,14.5f);walk(fourth,18.5f,15.6f);walk(fourth,10.5f,15.6f);walk(fourth,10.5f,14.5f);fourth.interact()
    check(fourth.pending?.kind==3);fourth.answer(1);check(fourth.secrets==1)
    walk(fourth,7.5f,14.5f);walk(fourth,5.5f,17.4f);fourth.interact()
    check(fourth.pending?.kind==3);fourth.answer(0);check(fourth.secrets==2)
    walk(fourth,5.5f,19.5f);walk(fourth,5.5f,17.4f)
    walk(fourth,7.5f,14.5f);walk(fourth,10.5f,14.5f)
    walk(fourth,15.5f,14.5f);walk(fourth,15.5f,17.4f);fourth.interact()
    check(fourth.pending?.question?.correct==1);fourth.answer(1)
    walk(fourth,15.5f,19.5f);walk(fourth,13.5f,19.5f);walk(fourth,13.5f,22.5f);walk(fourth,11.5f,22.5f);check(fourth.hasKey)
    walk(fourth,20.4f,22.5f);fourth.interact();check(fourth.doors.first{it.x==21&&it.y==22}.open)
    walk(fourth,22.5f,22.5f);walk(fourth,31.5f,23.5f)
    fourth.interact();check(fourth.won&&fourth.questionsCorrect==6)
    // Multi-phase serum volleys and reinforcements belong only to Captain Minoxidil.
    val captainTest=DoomEngine(level=4)
    val captain=captainTest.enemies.last()
    captainTest.enemies.filter{it!==captain}.forEach{it.hp=0}
    captain.x=5.1f;captain.y=3.5f;captain.hp=200;captain.cooldown=0f
    captainTest.step(.025f)
    check(captainTest.bolts.size==11&&captainTest.bolts.all{it.chrome})
    repeat(3){captain.cooldown=0f;captainTest.step(.025f)}
    check(captainTest.enemies.size>33){"Captain Minoxidil must summon mutated reinforcements"}
    check(captainTest.enemies.count{it.kind==DoomKind.FLESH_STALKER}<=19)
    // Hidden cache is an actual breakaway wall with a timed melee power-up.
    val hidden=DoomEngine(level=4)
    hidden.enemies.forEach{it.hp=0}
    walk(hidden,4.5f,3.5f);walk(hidden,4.5f,7.5f)
    hidden.interact()
    check(hidden.doors.first{it.x==3&&it.y==7}.open&&hidden.secrets==1)
    walk(hidden,2.5f,7.5f);walk(hidden,2.5f,8.5f)
    check(hidden.berserkActive&&hidden.berserkTime>20f)
    val mutant=hidden.enemies.first{it.kind==DoomKind.FLESH_STALKER}
    mutant.x=3.45f;mutant.y=8.5f;mutant.hp=74
    hidden.step(.03f,DoomInput(punch=true))
    check(mutant.hp==0&&hidden.drainEvents().contains(DoomEvent.PUNCH))
    repeat(510){hidden.step(.05f)}
    check(!hidden.berserkActive)
    // Raised stairs are traversable rather than decorative walls.
    val steps=DoomEngine(level=4)
    steps.enemies.forEach{it.hp=0}
    walk(steps,13.5f,3.5f);walk(steps,13.5f,7.5f)
    check(steps.stairTier(13,7)>0&&steps.cameraElevation>.15f)
    println("DOOM smoke OK: combat, four missions, five weapons, coins, touch-ray doors, classroom gates, secret, credential, exit, all 12 face states, enemy deaths, reload and chain explosions.")
}
