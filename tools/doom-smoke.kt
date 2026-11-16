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
    check(combat.drainEvents().contains(DoomEvent.SHOT))
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
    println("DOOM smoke OK: combat, three weapons, coins, touch-ray doors, classroom gates, secret, credential, exit.")
}
