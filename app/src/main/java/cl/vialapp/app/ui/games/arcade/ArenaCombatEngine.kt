package cl.vialapp.app.ui.games.arcade

import kotlin.math.abs
import kotlin.random.Random

internal enum class ArenaPhase { APPROACH, WINDUP, STRIKE, RECOVER, ROUND_CLEAR, KNOWLEDGE }
internal enum class ArenaMove(val windup:Float,val lock:Float,val range:Float,val damage:Int){
    JAB(.09f,.34f,.28f,13), HEAVY(.22f,.65f,.32f,26), SPECIAL(.17f,.90f,.46f,50)
}
internal enum class RivalAttack(val damage:Int,val tell:Float,val range:Float){
    QUICK(12,.80f,.29f), SWEEP(20,1.10f,.36f), CHARGE(27,1.35f,.48f)
}
internal data class ArenaCombatFrame(
    override val elapsed:Float=0f,override val score:Int=0,
    val hp:Int=100,val rivalHp:Int=85,val rival:Int=1,
    val playerX:Float=.24f,val rivalX:Float=.72f,
    val phase:ArenaPhase=ArenaPhase.APPROACH,val phaseTime:Float=.40f,
    val attack:RivalAttack=RivalAttack.QUICK,val attackNumber:Int=0,
    val move:ArenaMove?=null,val hitIn:Float=0f,val lock:Float=0f,
    val guard:Float=0f,val evade:Float=0f,val evadeLock:Float=0f,
    val power:Int=0,val combo:Int=0,val bestCombo:Int=0,val dodges:Int=0,
    val impact:Float=0f,val playerImpact:Float=0f,val freeze:Float=0f,val damage:Int=0,
    val shieldsBroken:Int=0,val knowledgeCorrect:Int=0,val knowledgeMissed:Int=0,
    val question:ArenaQuestion?=null,val selected:Int=-1,
    val shieldDamage:Int=0,
    override val message:String="Acércate y golpea."
):ArcadeFrame{
    val rivalMax:Int get()=when(rival){1->85;2->110;else->145}
    val distance:Float get()=abs(rivalX-playerX)
    override val finished get()=hp<=0||rival>3||elapsed>=ArenaCombatEngine.TIME
    override val won get()=rival>3&&hp>0
    override val health get()="$hp HP"
    override val detail get()="${(rival-1).coerceIn(0,3)}/3 rivales · $knowledgeCorrect/6 escudos · $knowledgeMissed errores · combo ×$bestCombo"
    override val lesson get()=if(question!=null&&selected>=0)question.explanation else "La seguridad vial requiere observar, respetar la prioridad y anticipar riesgos. Rompe los escudos aplicando estos conocimientos; el combate es ficticio."
    val pose:PelaoPose get()=when{
        won->PelaoPose.VICTORY;finished->PelaoPose.DEFEAT;playerImpact>0f->PelaoPose.HIT
        evade>0f||guard>0f->PelaoPose.BLOCK
        move==ArenaMove.HEAVY||move==ArenaMove.SPECIAL->PelaoPose.ATTACK_TWO
        move==ArenaMove.JAB->PelaoPose.ATTACK_ONE;else->PelaoPose.IDLE
    }
}

/** Fixed-step action combat. Input is independent of questions, collision uses distance,
 * and damage is applied exactly once at a move's active frame. */
internal class ArenaCombatEngine(seed:Int=Random.nextInt()):ArcadeEngine<ArenaCombatFrame>{
    private val random=Random(seed)
    private var questions=ArenaQuestions.forRival(1,random)
    private var questionIndex=0
    private var s=ArenaCombatFrame()
    private fun nextQuestion():ArenaQuestion {
        if(questionIndex>=questions.size){questions=ArenaQuestions.forRival(s.rival,random);questionIndex=0}
        return questions[questionIndex++]
    }
    override fun snapshot()=s
    override fun act(action:Int):ArenaCombatFrame{
        if(s.finished||s.phase==ArenaPhase.ROUND_CLEAR)return s
        if(s.phase==ArenaPhase.KNOWLEDGE){
            val q=s.question ?: return s
            if(action in 20..22 && s.selected<0){
                val index=action-20
                s=if(index==q.correct){
                    val damage=s.rivalMax/5
                    s.copy(selected=index,shieldsBroken=s.shieldsBroken+1,knowledgeCorrect=s.knowledgeCorrect+1,
                        shieldDamage=s.shieldDamage+damage,rivalHp=(s.rivalHp-damage).coerceAtLeast(1),
                        power=(s.power+25).coerceAtMost(100),score=s.score+damage*10+100,
                        message="¡Escudo roto! +25 de energía")
                }else s.copy(selected=index,knowledgeMissed=s.knowledgeMissed+1,hp=(s.hp-8).coerceAtLeast(0),combo=0,
                    message="El escudo resiste. −8 HP")
            }else if(action==23&&s.selected>=0){
                s=if(s.selected==q.correct)s.copy(phase=ArenaPhase.RECOVER,phaseTime=1.2f,
                    question=null,selected=-1,lock=0f,move=null,hitIn=0f,message="¡Contraataca!")
                else s.copy(question=nextQuestion(),selected=-1,message="Intenta otro desafío")
            }
            return s
        }
        when(action){
            0,1->if(s.lock<=0f&&s.evade<=0f){
                val next=(s.playerX+if(action==0)-.065f else .065f).coerceIn(.085f,s.rivalX-.14f)
                s=s.copy(playerX=next,guard=0f)
            }
            2,3,6->if(s.lock<=0f&&s.evade<=0f&&s.playerImpact<=0f){
                val move=when(action){2->ArenaMove.JAB;3->ArenaMove.HEAVY;else->ArenaMove.SPECIAL}
                if(move!=ArenaMove.SPECIAL||s.power>=100){
                    s=s.copy(move=move,hitIn=move.windup,lock=move.lock,guard=0f,
                        power=if(move==ArenaMove.SPECIAL)0 else s.power,message=when(move){
                            ArenaMove.JAB->"Golpe rápido";ArenaMove.HEAVY->"Golpe fuerte";else->"¡PELAO POWER!"
                        })
                }
            }
            4->if(s.lock<=0f&&s.evade<=0f&&s.guard<=0f&&s.playerImpact<=0f)
                s=s.copy(guard=.65f,lock=.85f,message="Guardia")
            5->if(s.evadeLock<=0f&&s.playerImpact<=0f){
                s=s.copy(playerX=(s.playerX-.105f).coerceAtLeast(.085f),evade=.48f,evadeLock=.95f,
                    guard=0f,move=null,hitIn=0f,lock=.35f,message="Esquiva")
            }
        }
        return s
    }
    private fun nextAttack(f:ArenaCombatFrame):RivalAttack=when(f.rival){
        1->if(f.attackNumber%3==2)RivalAttack.SWEEP else RivalAttack.QUICK
        2->if(f.attackNumber%2==0)RivalAttack.SWEEP else RivalAttack.QUICK
        else->when(f.attackNumber%3){0->RivalAttack.CHARGE;1->RivalAttack.QUICK;else->RivalAttack.SWEEP}
    }
    override fun step(dt:Float):ArenaCombatFrame{
        if(s.finished||dt<=0f||s.phase==ArenaPhase.KNOWLEDGE)return s
        val d=dt.coerceAtMost(.05f)
        if(s.freeze>0f){s=s.copy(freeze=(s.freeze-d).coerceAtLeast(0f));return s}
        val old=s
        var f=old.copy(elapsed=old.elapsed+d,phaseTime=(old.phaseTime-d).coerceAtLeast(0f),
            lock=(old.lock-d).coerceAtLeast(0f),hitIn=(old.hitIn-d).coerceAtLeast(0f),
            guard=(old.guard-d).coerceAtLeast(0f),evade=(old.evade-d).coerceAtLeast(0f),
            evadeLock=(old.evadeLock-d).coerceAtLeast(0f),impact=(old.impact-d).coerceAtLeast(0f),
            playerImpact=(old.playerImpact-d).coerceAtLeast(0f))
        if(old.move!=null&&old.hitIn>0f&&f.hitIn<=0f&&old.phase!=ArenaPhase.ROUND_CLEAR){
            val m=old.move
            // A short step gives the punch reach but never crosses the enemy.
            val step=if(m==ArenaMove.SPECIAL).12f else .045f
            f=f.copy(playerX=(f.playerX+step).coerceAtMost(f.rivalX-.14f))
            if(f.distance<=m.range){
                val armored=f.rival>=2&&f.phase==ArenaPhase.APPROACH&&m==ArenaMove.JAB
                val damage=if(armored)4 else m.damage
                // Two mandatory shields each remove 20% of maximum HP via knowledge.
                val threshold=when(f.shieldsBroken){0->f.rivalMax*4/5;1->f.rivalMax*2/5;else->0}
                val dealt=minOf(damage,(f.rivalHp-threshold).coerceAtLeast(0))
                val combo=f.combo+1
                f=f.copy(rivalHp=f.rivalHp-dealt,score=f.score+dealt*10+combo.coerceAtMost(12)*8,
                    combo=combo,bestCombo=maxOf(f.bestCombo,combo),power=(f.power+if(m==ArenaMove.SPECIAL)0 else 10).coerceAtMost(100),
                    impact=.24f,freeze=.065f,damage=dealt,message=if(armored)"¡Guardia del rival!" else "¡Impacto!")
                if(f.shieldsBroken<2&&f.rivalHp==threshold){
                    s=f.copy(phase=ArenaPhase.KNOWLEDGE,question=nextQuestion(),selected=-1,move=null,hitIn=0f,lock=0f,
                        guard=0f,evade=0f,freeze=0f,message="¡Escudo de conocimiento!")
                    return s
                }
                if(f.rivalHp<=0){
                    if(f.rival==3){s=f.copy(rival=4,score=f.score+700,message="¡Campeón!");return s}
                    s=f.copy(phase=ArenaPhase.ROUND_CLEAR,phaseTime=1.35f,score=f.score+350,message="¡KO! +15 HP");return s
                }
                if(f.phase==ArenaPhase.RECOVER)f=f.copy(phaseTime=maxOf(f.phaseTime,.24f))
            }else f=f.copy(combo=0,message="Fuera de alcance")
        }
        if(f.lock<=0f)f=f.copy(move=null)
        when(f.phase){
            ArenaPhase.APPROACH->{
                val desired=f.playerX+.225f
                if(f.rivalX>desired)f=f.copy(rivalX=(f.rivalX-d*(.23f+f.rival*.035f)).coerceAtLeast(desired))
                if(f.phaseTime<=0f&&f.distance<=.27f){
                    val attack=nextAttack(f)
                    f=f.copy(phase=ArenaPhase.WINDUP,phaseTime=attack.tell,attack=attack,
                        message=if(attack==RivalAttack.CHARGE)"¡CARGA! Esquiva" else if(attack==RivalAttack.SWEEP)"¡BARRIDO!" else "¡Golpe entrante!")
                }
            }
            ArenaPhase.WINDUP->if(f.phaseTime<=0f){
                val attack=f.attack
                val inRange=f.distance<=attack.range
                val avoided=f.evade>0f||!inRange
                val blocked=!avoided&&f.guard>0f&&attack!=RivalAttack.CHARGE
                if(avoided)f=f.copy(dodges=f.dodges+1,power=(f.power+18).coerceAtMost(100),
                    score=f.score+75,message="¡Esquiva perfecta!")
                else if(blocked)f=f.copy(power=(f.power+14).coerceAtMost(100),score=f.score+45,message="¡Bloqueo!")
                else f=f.copy(hp=(f.hp-attack.damage).coerceAtLeast(0),playerImpact=.34f,combo=0,
                    playerX=(f.playerX-.04f).coerceAtLeast(.085f),freeze=.08f,move=null,hitIn=0f,lock=.35f,
                    message="−"+attack.damage+" HP")
                f=f.copy(phase=ArenaPhase.STRIKE,phaseTime=.15f,attackNumber=f.attackNumber+1)
            }
            ArenaPhase.STRIKE->if(f.phaseTime<=0f)f=f.copy(phase=ArenaPhase.RECOVER,
                phaseTime=if(f.attack==RivalAttack.CHARGE)1.05f else .75f,message=if(f.playerImpact>0f)f.message else "¡Contraataca!")
            ArenaPhase.RECOVER->if(f.phaseTime<=0f)f=f.copy(phase=ArenaPhase.APPROACH,phaseTime=.45f)
            ArenaPhase.KNOWLEDGE->Unit
            ArenaPhase.ROUND_CLEAR->if(f.phaseTime<=0f){
                val rival=f.rival+1
                questions=ArenaQuestions.forRival(rival,random);questionIndex=0
                f=f.copy(shieldsBroken=0,question=null,selected=-1,rival=rival,rivalHp=if(rival==2)110 else 145,hp=(f.hp+15).coerceAtMost(100),
                    playerX=.24f,rivalX=.76f,phase=ArenaPhase.APPROACH,phaseTime=.8f,attackNumber=0,
                    move=null,lock=0f,hitIn=0f,guard=0f,evade=0f,combo=0,message="¡Nuevo rival!")
            }
        }
        s=f
        return s
    }
    companion object{const val TIME=120f}
}
