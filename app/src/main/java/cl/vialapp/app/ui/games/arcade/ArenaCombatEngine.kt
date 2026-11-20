package cl.vialapp.app.ui.games.arcade

// GUÍA DEL ARCHIVO:
// Combate de la arena, enemigos y daños. Las reglas no deberían depender de cómo se ven los sprites.

import kotlin.math.abs
import kotlin.random.Random

// Shared by combat, the health bar and the question result.
internal object ArenaShieldRules{
    const val FIRST_PERCENT=65
    const val SECOND_PERCENT=25
    const val ANSWER_PERCENT=10
    fun threshold(maxHp:Int,broken:Int)=when(broken){
        0->maxHp*FIRST_PERCENT/100
        1->maxHp*SECOND_PERCENT/100
        else->0
    }
    fun answerDamage(maxHp:Int)=maxHp*ANSWER_PERCENT/100
}
internal enum class ArenaPhase { APPROACH, WINDUP, STRIKE, RECOVER, ROUND_CLEAR, SHIELD_BREAK, KNOWLEDGE, ANSWER_IMPACT }
internal enum class ArenaMove(val windup:Float,val lock:Float,val range:Float,val damage:Int){
    JAB(.055f,.26f,.235f,12), HEAVY(.14f,.43f,.30f,20), SPECIAL(.24f,.72f,.72f,42)
}
internal enum class RivalAttack(val damage:Int,val tell:Float,val range:Float){
    QUICK(11,.65f,.27f), SWEEP(17,.86f,.33f), CHARGE(24,.98f,.43f)
}
internal data class ArenaCombatFrame(
    override val elapsed:Float=0f,override val score:Int=0,
    val hp:Int=100,val rivalHp:Int=320,val rival:Int=1,
    val playerX:Float=.28f,val rivalX:Float=.72f,
    val phase:ArenaPhase=ArenaPhase.APPROACH,val phaseTime:Float=.40f,
    val attack:RivalAttack=RivalAttack.QUICK,val attackNumber:Int=0,
    val move:ArenaMove?=null,val hitIn:Float=0f,val lock:Float=0f,
    val guard:Float=0f,val evade:Float=0f,val evadeLock:Float=0f,
    val power:Int=0,val combo:Int=0,val bestCombo:Int=0,val dodges:Int=0,
    val impact:Float=0f,val playerImpact:Float=0f,val freeze:Float=0f,val damage:Int=0,
    val shieldsBroken:Int=0,val knowledgeCorrect:Int=0,val knowledgeMissed:Int=0,
    val question:ArenaQuestion?=null,val selected:Int=-1,
    val shieldDamage:Int=0,
    val jump:Float=0f,val walk:Float=0f,val walkDir:Int=0,val walkCycle:Float=0f,
    val queued:Int=-1,val chainTime:Float=0f,val lastHit:ArenaMove?=null,
    val comboFlash:Float=0f,val intro:Float=.95f,
    val counterFlash:Float=0f,
    val moveIntent:Int=0,val inputTtl:Float=0f,val velocity:Float=0f,
    val rivalStun:Float=0f,val defendFlash:Float=0f,val dashDir:Int=-1,
    val lastImpact:ArenaMove?=null,
    val answerApplied:Boolean=false,
    val answerFeedback:String?=null,val answerFeedbackTime:Float=0f,
    override val message:String="Acércate y golpea."
):ArcadeFrame{
    val rivalMax:Int get()=when(rival){1->320;2->400;else->480}
    val answerCorrect:Boolean get()=selected>=0&&question?.correct==selected
    val answerProgress:Float get()=if(phase==ArenaPhase.ANSWER_IMPACT)(1f-phaseTime/ArenaCombatEngine.ANSWER_TIME).coerceIn(0f,1f)else 0f
    val isCinematic:Boolean get()=phase==ArenaPhase.SHIELD_BREAK||phase==ArenaPhase.ANSWER_IMPACT||phase==ArenaPhase.ROUND_CLEAR
    val distance:Float get()=abs(rivalX-playerX)
    val jumpHeight:Float get()=if(jump>0f)kotlin.math.sin(jump/ArenaCombatEngine.JUMP_TIME*3.1415927f).coerceAtLeast(0f) else 0f
    override val finished get()=phase!=ArenaPhase.ANSWER_IMPACT&&(hp<=0||rival>3||elapsed>=ArenaCombatEngine.TIME)
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
        if(questionIndex>=questions.size){
            val previousPrompt=s.question?.prompt
            questions=ArenaQuestions.forRival(s.rival,random).let{bank->
                if(bank.size>1&&bank.first().prompt==previousPrompt)bank.drop(1)+bank.first() else bank
            }
            questionIndex=0
        }
        return questions[questionIndex++]
    }
    override fun snapshot()=s
    override fun act(action:Int):ArenaCombatFrame{
        if(s.finished||s.isCinematic)return s
        if(s.phase==ArenaPhase.KNOWLEDGE){
            val q=s.question ?: return s
            if(action in 20..22 && s.selected<0){
                val index=action-20
                // Select exactly once, then show the consequence on the stage.
                // Damage is committed by the cinematic's impact frame, not this input.
                s=s.copy(selected=index,phase=ArenaPhase.ANSWER_IMPACT,phaseTime=ANSWER_TIME,
                    answerApplied=false,answerFeedback=null,answerFeedbackTime=0f,move=null,hitIn=0f,lock=0f,guard=0f,evade=0f,jump=0f,
                    queued=-1,moveIntent=0,inputTtl=0f,velocity=0f,walk=0f,rivalStun=0f,
                    impact=0f,playerImpact=0f,comboFlash=0f,counterFlash=0f,defendFlash=0f,intro=0f,
                    message=if(index==q.correct)"¡Respuesta correcta!" else "Respuesta incorrecta")
            }
            return s
        }
        when(action){
            8,9->s=s.copy(moveIntent=if(action==8)-1 else 1,inputTtl=.085f)
            10->s=s.copy(moveIntent=0,inputTtl=0f)
            0,1->if(s.lock<=0f&&s.evade<=0f&&s.playerImpact<=0f){
                val dir=if(action==0)-1 else 1
                val distance=.065f
                val next=(s.playerX+dir*distance).coerceIn(.12f,(s.rivalX-.19f).coerceAtLeast(.12f))
                if(next!=s.playerX)s=s.copy(playerX=next,guard=0f,walk=.15f,walkDir=dir,walkCycle=s.walkCycle+.53f)
            }
            7->if(s.jump<=0f&&s.lock<=0f&&s.evade<=0f&&s.playerImpact<=0f)
                s=s.copy(jump=JUMP_TIME,guard=0f,walk=0f,message="¡Salto!")
            2,3,6->if(s.lock>0f&&s.move!=null&&s.lock<=.34f&&s.playerImpact<=0f&&s.evade<=0f){
                // One buffered action; no multi-hit from repeated tapping.
                if(action!=6||s.power>=100)s=s.copy(queued=action)
            }else if(s.lock<=0f&&s.evade<=0f&&s.playerImpact<=0f){
                val move=when(action){2->ArenaMove.JAB;3->ArenaMove.HEAVY;else->ArenaMove.SPECIAL}
                if(move!=ArenaMove.SPECIAL||s.power>=100){
                    s=s.copy(move=move,hitIn=move.windup,lock=move.lock,guard=0f,queued=-1,walk=0f,velocity=0f,
                        power=if(move==ArenaMove.SPECIAL)0 else s.power,message=when(move){
                            ArenaMove.JAB->"Puñetazo";ArenaMove.HEAVY->if(s.jump>0f)"¡Patada aérea!" else "Patada";else->"¡ONDA PELAO!"
                        })
                }
            }
            4->if((s.lock<=0f||s.move!=null&&s.hitIn<=0f)&&s.evade<=0f&&s.playerImpact<=0f)
                s=s.copy(guard=.52f,lock=.52f,move=null,hitIn=0f,queued=-1,walk=0f,velocity=0f,message="Guardia")
            5->if(s.evadeLock<=0f&&s.playerImpact<=0f){
                s=s.copy(evade=.24f,evadeLock=.68f,dashDir=if(s.moveIntent>0&&s.inputTtl>0f)1 else -1,velocity=0f,
                    guard=0f,move=null,hitIn=0f,lock=.22f,queued=-1,walk=0f,message="¡Dash!")
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
        if(s.phase==ArenaPhase.ANSWER_IMPACT){
            val left=(s.phaseTime-d).coerceAtLeast(0f)
            var f=s.copy(phaseTime=left)
            if(!f.answerApplied&&left<=ANSWER_TIME*.55f){
                f=if(f.answerCorrect){
                    val damage=ArenaShieldRules.answerDamage(f.rivalMax)
                    f.copy(answerApplied=true,shieldsBroken=f.shieldsBroken+1,knowledgeCorrect=f.knowledgeCorrect+1,
                        shieldDamage=f.shieldDamage+damage,rivalHp=(f.rivalHp-damage).coerceAtLeast(1),
                        power=(f.power+25).coerceAtMost(100),score=f.score+damage*3+100,
                        damage=damage,lastImpact=ArenaMove.SPECIAL,message="¡Escudo destruido!")
                }else f.copy(answerApplied=true,knowledgeMissed=f.knowledgeMissed+1,
                    hp=(f.hp-8).coerceAtLeast(0),combo=0,chainTime=0f,lastHit=null,
                    message="El escudo resiste. −8 HP")
            }
            if(left<=0f){
                val explanation=f.question?.explanation
                f=when{
                    f.answerCorrect->f.copy(phase=ArenaPhase.RECOVER,phaseTime=1.35f,
                        question=null,selected=-1,answerApplied=false,answerFeedback=explanation,answerFeedbackTime=4f,
                        lock=0f,move=null,hitIn=0f,queued=-1,chainTime=0f,combo=0,jump=0f,rivalStun=.42f,
                        message="¡Guardia abierta! Ataca")
                    f.hp<=0->f.copy(phase=ArenaPhase.RECOVER,phaseTime=0f,question=null,selected=-1,answerApplied=false,
                        answerFeedback=null,answerFeedbackTime=0f,message="Fin de la pelea")
                    else->f.copy(phase=ArenaPhase.KNOWLEDGE,phaseTime=0f,question=nextQuestion(),selected=-1,
                        answerApplied=false,answerFeedback=explanation,answerFeedbackTime=0f,message="El escudo resiste. Nuevo desafío")
                }
            }
            s=f
            return s
        }
        if(s.phase==ArenaPhase.SHIELD_BREAK){
            val left=(s.phaseTime-d).coerceAtLeast(0f)
            s=s.copy(phaseTime=left,impact=(s.impact-d).coerceAtLeast(0f),phase=if(left==0f)ArenaPhase.KNOWLEDGE else ArenaPhase.SHIELD_BREAK)
            return s
        }
        val old=s
        var f=old.copy(elapsed=old.elapsed+d,phaseTime=(old.phaseTime-d).coerceAtLeast(0f),
            answerFeedbackTime=(old.answerFeedbackTime-d).coerceAtLeast(0f),
            answerFeedback=if(old.answerFeedbackTime>d)old.answerFeedback else null,
            lock=(old.lock-d).coerceAtLeast(0f),hitIn=(old.hitIn-d).coerceAtLeast(0f),
            guard=(old.guard-d).coerceAtLeast(0f),evade=(old.evade-d).coerceAtLeast(0f),
            evadeLock=(old.evadeLock-d).coerceAtLeast(0f),impact=(old.impact-d).coerceAtLeast(0f),
            playerImpact=(old.playerImpact-d).coerceAtLeast(0f),jump=(old.jump-d).coerceAtLeast(0f),walk=(old.walk-d).coerceAtLeast(0f),
            chainTime=(old.chainTime-d).coerceAtLeast(0f),combo=if(old.chainTime-d<=0f)0 else old.combo,
            comboFlash=(old.comboFlash-d).coerceAtLeast(0f),intro=(old.intro-d).coerceAtLeast(0f),counterFlash=(old.counterFlash-d).coerceAtLeast(0f),
            inputTtl=(old.inputTtl-d).coerceAtLeast(0f),rivalStun=(old.rivalStun-d).coerceAtLeast(0f),defendFlash=(old.defendFlash-d).coerceAtLeast(0f))
        val desiredVelocity=when {
            f.evade>0f->f.dashDir*0.85f
            f.lock<=0f&&f.playerImpact<=0f&&f.inputTtl>0f&&f.guard<=0f->f.moveIntent*.62f
            else->0f
        }
        val v=if(f.evade>0f)desiredVelocity else old.velocity+(desiredVelocity-old.velocity)*(d*26f).coerceAtMost(1f)
        val nx=(f.playerX+v*d).coerceIn(.12f,(f.rivalX-.19f).coerceAtLeast(.12f))
        val moved=abs(nx-f.playerX)
        f=f.copy(playerX=nx,velocity=if(f.lock>0f&&f.evade<=0f)0f else v,
            walk=if(moved>.001f&&f.evade<=0f&&f.move==null&&f.guard<=0f).11f else f.walk,
            walkDir=if(v>=0f)1 else -1,walkCycle=f.walkCycle+moved*43f)
        if(old.move!=null&&old.hitIn>0f&&f.hitIn<=0f&&old.phase!=ArenaPhase.ROUND_CLEAR){
            val m=old.move
            // A short step gives the punch reach but never crosses the enemy.
            val step=if(m==ArenaMove.SPECIAL)0f else if(m==ArenaMove.HEAVY).06f else .035f
            f=f.copy(playerX=(f.playerX+step).coerceAtMost((f.rivalX-.19f).coerceAtLeast(.12f)))
            if(f.distance<=m.range){
                val armored=f.rival>=2&&f.phase==ArenaPhase.APPROACH&&m==ArenaMove.JAB
                val chained=f.combo>=2&&f.lastHit!=m&&m!=ArenaMove.SPECIAL
                val counter=f.phase==ArenaPhase.RECOVER||f.phase==ArenaPhase.WINDUP&&f.phaseTime>.14f
                val damage=if(armored)4 else m.damage+(if(chained)7 else 0)+(if(counter)4 else 0)+(if(f.jumpHeight>.25f&&m==ArenaMove.HEAVY)5 else 0)
                // Give players a longer uninterrupted combat segment before each mandatory challenge.
                val threshold=ArenaShieldRules.threshold(f.rivalMax,f.shieldsBroken)
                val dealt=minOf(damage,(f.rivalHp-threshold).coerceAtLeast(0))
                val combo=f.combo+1
                f=f.copy(rivalHp=f.rivalHp-dealt,score=f.score+dealt*3+combo.coerceAtMost(8)*8,
                    combo=combo,bestCombo=maxOf(f.bestCombo,combo),chainTime=1.15f,lastHit=m,comboFlash=if(combo>=2).65f else 0f,counterFlash=if(counter).55f else 0f,power=(f.power+if(m==ArenaMove.SPECIAL)0 else 10).coerceAtMost(100),
                    impact=.30f,freeze=if(m==ArenaMove.SPECIAL).12f else if(m==ArenaMove.HEAVY).08f else .045f,damage=dealt,lastImpact=m,rivalStun=if(m==ArenaMove.SPECIAL).36f else if(m==ArenaMove.HEAVY).20f else .12f,message=when{armored->"¡Guardia del rival!";chained->"¡COMBO!";counter->"¡COUNTER!";else->"¡Impacto!"})
                if(f.shieldsBroken<2&&f.rivalHp==threshold){
                    s=f.copy(phase=ArenaPhase.SHIELD_BREAK,phaseTime=.62f,question=nextQuestion(),selected=-1,answerApplied=false,answerFeedback=null,answerFeedbackTime=0f,move=null,hitIn=0f,lock=0f,
                        guard=0f,evade=0f,freeze=0f,queued=-1,jump=0f,walk=0f,moveIntent=0,inputTtl=0f,velocity=0f,message="¡Escudo de conocimiento!")
                    return s
                }
                if(f.rivalHp<=0){
                    if(f.rival==3){s=f.copy(rival=4,score=f.score+700,message="¡Campeón!");return s}
                    s=f.copy(phase=ArenaPhase.ROUND_CLEAR,phaseTime=1.15f,queued=-1,jump=0f,walk=0f,score=f.score+350,message="¡KO! Recuperas hasta 20 HP");return s
                }
                if(f.phase==ArenaPhase.WINDUP && f.phaseTime>.14f && (m==ArenaMove.SPECIAL||m==ArenaMove.HEAVY&&(old.lastHit!=ArenaMove.HEAVY||old.chainTime<=0f)))f=f.copy(phase=ArenaPhase.RECOVER,phaseTime=.28f,attackNumber=f.attackNumber+1)
                else if(f.phase==ArenaPhase.RECOVER)f=f.copy(phaseTime=maxOf(f.phaseTime,.16f))
                if(m!=ArenaMove.JAB)f=f.copy(rivalX=(f.rivalX+if(m==ArenaMove.SPECIAL).055f else .025f).coerceAtMost(.87f))
            }else f=f.copy(combo=0,chainTime=0f,lastHit=null,message="Fuera de alcance")
        }
        if(f.lock<=0f)f=f.copy(move=null)
        when(f.phase){
            ArenaPhase.APPROACH->{
                val desired=f.playerX+.225f
                if(f.rivalX>desired&&f.rivalStun<=0f)f=f.copy(rivalX=(f.rivalX-d*(.35f+f.rival*.055f)).coerceAtLeast(desired))
                if(f.phaseTime<=0f&&f.distance<=.27f&&f.rivalStun<=0f){
                    val attack=nextAttack(f)
                    f=f.copy(phase=ArenaPhase.WINDUP,phaseTime=attack.tell,attack=attack,
                        message=if(attack==RivalAttack.CHARGE)"¡CARGA! Esquiva" else if(attack==RivalAttack.SWEEP)"¡BARRIDO!" else "¡Golpe entrante!")
                }
            }
            ArenaPhase.WINDUP->if(f.phaseTime<=0f){
                val attack=f.attack
                val inRange=f.distance<=attack.range
                val avoided=f.evade>0f||!inRange||(attack==RivalAttack.SWEEP&&f.jumpHeight>.24f)
                val blocked=!avoided&&f.guard>0f&&attack!=RivalAttack.CHARGE
                if(avoided)f=f.copy(dodges=f.dodges+1,power=(f.power+18).coerceAtMost(100),
                    score=f.score+75,defendFlash=.48f,message="¡Esquiva perfecta!")
                else if(blocked)f=f.copy(power=(f.power+14).coerceAtMost(100),score=f.score+45,defendFlash=.48f,rivalStun=.26f,guard=0f,lock=0f,message="¡Bloqueo! Contraataca")
                else f=f.copy(hp=(f.hp-attack.damage).coerceAtLeast(0),playerImpact=.34f,combo=0,
                    playerX=(f.playerX-.055f).coerceAtLeast(.12f),freeze=.09f,move=null,hitIn=0f,lock=.26f,queued=-1,jump=0f,walk=0f,chainTime=0f,lastHit=null,
                    message="−"+attack.damage+" HP")
                f=f.copy(phase=ArenaPhase.STRIKE,phaseTime=.17f,attackNumber=f.attackNumber+1,
                    rivalX=if(attack==RivalAttack.CHARGE&&inRange)(f.rivalX-.065f).coerceAtLeast(f.playerX+.19f) else f.rivalX)
            }
            ArenaPhase.STRIKE->if(f.phaseTime<=0f)f=f.copy(phase=ArenaPhase.RECOVER,
                phaseTime=if(f.attack==RivalAttack.CHARGE).88f else .51f,message=if(f.playerImpact>0f)f.message else "¡Contraataca!")
            ArenaPhase.RECOVER->if(f.phaseTime<=0f)f=f.copy(phase=ArenaPhase.APPROACH,phaseTime=.24f)
            ArenaPhase.SHIELD_BREAK,ArenaPhase.KNOWLEDGE,ArenaPhase.ANSWER_IMPACT->Unit
            ArenaPhase.ROUND_CLEAR->if(f.phaseTime<=0f){
                val rival=f.rival+1
                questions=ArenaQuestions.forRival(rival,random);questionIndex=0
                f=f.copy(shieldsBroken=0,question=null,selected=-1,answerApplied=false,answerFeedback=null,answerFeedbackTime=0f,rival=rival,rivalHp=if(rival==2)400 else 480,hp=(f.hp+20).coerceAtMost(100),
                    playerX=.24f,rivalX=.76f,phase=ArenaPhase.APPROACH,phaseTime=.8f,attackNumber=0,
                    move=null,lock=0f,hitIn=0f,guard=0f,evade=0f,evadeLock=0f,combo=0,queued=-1,chainTime=0f,lastHit=null,jump=0f,walk=0f,intro=.95f,moveIntent=0,inputTtl=0f,velocity=0f,rivalStun=0f,defendFlash=0f,lastImpact=null,message="¡Nuevo rival!")
            }
        }
        s=f
        if(s.queued>=0&&s.lock<=0f&&s.phase!=ArenaPhase.ROUND_CLEAR&&!s.finished){
            val action=s.queued;s=s.copy(queued=-1);act(action)
        }
        return s
    }
    companion object{const val TIME=210f;const val JUMP_TIME=.72f;const val ANSWER_TIME=1.35f}
}
