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
    println("DOOM smoke OK: combat, three weapons, coins, touch-ray doors, classroom gates, secret, credential, exit, all 12 face states, enemy deaths, reload and chain explosions.")
}
