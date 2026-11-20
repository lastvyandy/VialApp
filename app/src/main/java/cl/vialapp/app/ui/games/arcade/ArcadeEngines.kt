package cl.vialapp.app.ui.games.arcade

// GUÍA DEL ARCHIVO:
// Lógica compartida de juegos de arcade. Las reglas van acá y la interfaz las dibuja aparte.

import kotlin.math.abs
import kotlin.random.Random

internal interface ArcadeFrame {
    val elapsed: Float
    val score: Int
    val finished: Boolean
    val won: Boolean
    val health: String
    val detail: String
    val message: String
    val lesson: String
}

internal interface ArcadeEngine<S : ArcadeFrame> {
    fun snapshot(): S
    fun step(dt: Float): S
    fun act(action: Int): S
}

internal enum class RoadItemKind { TRAFFIC, CONE, COIN }
internal data class RoadItem(val id: Int, val lane: Int, val y: Float, val kind: RoadItemKind, val paint: Int = 0)
internal data class DriveFrame(
    override val elapsed: Float = 0f,
    override val score: Int = 0,
    val lives: Int = 3,
    val lane: Int = 1,
    val carX: Float = 1f,
    val slow: Boolean = false,
    val schoolZone: Boolean = false,
    val distance: Float = 0f,
    val items: List<RoadItem> = emptyList(),
    val coins: Int = 0,
    val dodged: Int = 0,
    val invulnerable: Float = 0f,
    override val message: String = "Mantén tu carril libre. Practica conducción segura.",
    val stopped: Boolean = false,
    val stopChallenge: Boolean = false,
    val stopCompleted: Boolean = false,
    val safeZones: Int = 0,
    val safeStops: Int = 0,
    val infractions: Int = 0
) : ArcadeFrame {
    override val finished get() = lives <= 0 || elapsed >= DriveEngine.TIME
    override val won get() = lives > 0 && elapsed >= DriveEngine.TIME
    override val health get() = "♥".repeat(lives) + "♡".repeat(3 - lives)
    override val detail get() = "$coins monedas · $safeZones zonas seguras · $safeStops PARE respetados · $infractions infracciones"
    override val lesson get() = "Zonas escolares: máximo 30 km/h en horas de entrada y salida. Ante un PARE, detente completamente y continúa solo si es seguro."
}

/** Three lanes, smooth steering, collisions and a timed school-zone challenge. */
internal class DriveEngine(seed: Int = Random.nextInt(), private val spawnTraffic: Boolean = true) : ArcadeEngine<DriveFrame> {
    private val random = Random(seed)
    private var state = DriveFrame()
    private var spawn = .4f
    private var id = 0
    private var speeding = 0f
    private var zoneBonus = false
    private var stopTime = 0f
    override fun snapshot() = state
    override fun act(action: Int): DriveFrame {
        if (state.finished) return state
        state = when (action) {
            0 -> if (!state.stopped) state.copy(lane = (state.lane - 1).coerceAtLeast(0)) else state
            1 -> if (!state.stopped) state.copy(lane = (state.lane + 1).coerceAtMost(2)) else state
            2 -> if (!state.stopped) state.copy(slow = !state.slow) else state
            3 -> when {
                state.stopped && state.stopCompleted -> state.copy(stopped = false)
                state.stopChallenge && !state.stopCompleted && !state.stopped -> state.copy(stopped = true)
                else -> state
            }
            else -> state
        }
        return state
    }

    override fun step(dt: Float): DriveFrame {
        if (state.finished || dt <= 0f) return state
        val d = dt.coerceAtMost(.05f)
        val elapsed = (state.elapsed + d).coerceAtMost(TIME)
        val zone = (elapsed in 18f..27f || elapsed in 43f..52f)
        // Short, explicitly signposted scripted junctions in the arcade.
        val stopChallenge = elapsed in 9f..15f || elapsed in 34f..40f
        var stopped = state.stopped
        var stopCompleted = state.stopCompleted
        var safeZones = state.safeZones
        var safeStops = state.safeStops
        var infractions = state.infractions
        var lives = state.lives
        var score = state.score
        var message = state.message
        var invulnerable = (state.invulnerable - d).coerceAtLeast(0f)
        var coins = state.coins
        var dodged = state.dodged
        val x = state.carX + (state.lane - state.carX).coerceIn(-d * 7f, d * 7f)
        val speed = if (stopped) 0f else if (state.slow) .27f else .43f + elapsed / 240f

        if (zone && !state.schoolZone) {
            speeding = 0f; zoneBonus = true
            message = "Zona escolar (entrada/salida): reduce a 30 km/h."
        }
        if (zone && !state.slow && !stopped) {
            speeding += d
            if (speeding >= 2f && zoneBonus) {
                lives = (lives - 1).coerceAtLeast(0)
                zoneBonus = false
                infractions++
                message = "Excediste el límite de 30 km/h. −1 vida."
            }
        }
        if (!zone && state.schoolZone && zoneBonus) {
            score += 300
            safeZones++
            message = "¡Respetaste la zona escolar! +300"
        }

        if (stopChallenge && !state.stopChallenge) {
            stopTime = 0f
            stopCompleted = false
            message = "Señal PARE: detente completamente y espera el aviso."
        }
        if (stopChallenge && !stopCompleted) {
            stopTime = if (stopped) stopTime + d else 0f
            if (stopTime >= 1.3f) {
                stopCompleted = true
                safeStops++
                score += 350
                message = "¡Detención completa! Cruce libre: puedes avanzar."
            }
        }
        if (!stopChallenge && state.stopChallenge) {
            if (!stopCompleted) {
                lives = (lives - 1).coerceAtLeast(0)
                infractions++
                message = "No respetaste el PARE. −1 vida."
            }
            stopped = false
        }

        val next = mutableListOf<RoadItem>()
        for (item in state.items) {
            val y = item.y + speed * d
            val touching = !stopped && abs(x - item.lane) < .48f && y >= .75f && y <= .91f
            when {
                touching && item.kind == RoadItemKind.COIN -> {
                    if ((!zone || state.slow) && (!stopChallenge || stopCompleted) && !stopped) {
                        coins++; score += 100
                        if (!stopChallenge && !zone) message = "¡Moneda! +100"
                    }
                }
                touching && invulnerable <= 0f -> {
                    lives = (lives - 1).coerceAtLeast(0)
                    invulnerable = 1.4f
                    message = "Colisión: anticipa los obstáculos. −1 vida."
                }
                touching -> Unit
                y > 1.11f -> if (item.kind != RoadItemKind.COIN) { dodged++; score += 60 }
                else -> next += item.copy(y = y)
            }
        }
        if (!stopChallenge && !stopped) spawn -= d
        if (spawnTraffic && spawn <= 0f && !stopChallenge && !stopped) {
            spawn += (1.05f - elapsed / 180f).coerceAtLeast(.65f)
            val lane = random.nextInt(3)
            if (next.none { it.lane == lane && it.y < .20f }) {
                val kind = when (random.nextInt(5)) {
                    0, 1 -> RoadItemKind.COIN
                    2 -> RoadItemKind.CONE
                    else -> RoadItemKind.TRAFFIC
                }
                next += RoadItem(++id, lane, -.13f, kind, random.nextInt(4))
            }
        }
        state = DriveFrame(elapsed, score, lives, state.lane, x, state.slow, zone,
            state.distance + speed * d * 100f, next, coins, dodged, invulnerable, message,
            stopped, stopChallenge, stopCompleted, safeZones, safeStops, infractions)
        return state
    }
    companion object { const val TIME = 65f }
}

internal enum class FightPhase { TELEGRAPH, OPENING, RECOVERY, COUNTER }

internal enum class PelaoPose(val resourceName: String) {
    IDLE("don_pelao_idle"),
    ATTACK_ONE("don_pelao_attack_1"),
    ATTACK_TWO("don_pelao_attack_2"),
    HIT("don_pelao_hit"),
    BLOCK("don_pelao_block"),
    VICTORY("don_pelao_victory"),
    DEFEAT("don_pelao_defeat")
}
internal enum class Hazard(val label: String, val response: Int, val advice: String) {
    PEDESTRIAN("Peatón cruzando delante de ti", 0, "Detente antes del paso y deja cruzar."),
    STOP_SIGN("Señal PARE en tu vía", 0, "El PARE exige detenerse completamente."),
    RED_LIGHT("Semáforo rojo delante de ti", 0, "Detente antes de la línea de detención."),
    SCHOOL("Zona escolar en horario de salida", 1, "No superes los 30 km/h en ese horario."),
    FOG("Niebla: visibilidad reducida", 1, "Reduce la velocidad y adapta la conducción a la visibilidad."),
    WORKS("Trabajos en la vía, carril estrecho", 1, "Reduce la velocidad y respeta la señalización."),
    TAILGATE("Vas muy cerca del vehículo precedente", 2, "Aumenta la distancia: usa la referencia de tres segundos en buenas condiciones.")
}
internal data class FightFrame(
    override val elapsed: Float = 0f,
    override val score: Int = 0,
    val hp: Int = 100,
    val bossHp: Int = 100,
    val boss: Int = 1,
    val hazard: Hazard = Hazard.PEDESTRIAN,
    val phase: FightPhase = FightPhase.TELEGRAPH,
    val remaining: Float = 6.0f,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val strikes: Int = 0,
    val impact: Float = 0f,
    val attackLock: Float = 0f,
    val power: Int = 0,
    val openingHits: Int = 0,
    val dodges: Int = 0,
    val lastDamage: Int = 0,
    val correctDecisions: Int = 0,
    val missedDecisions: Int = 0,
    val actionPose: PelaoPose = PelaoPose.IDLE,
    val poseTime: Float = 0f,
    override val message: String = "Observa el peligro y responde a tiempo."
) : ArcadeFrame {
    override val finished get() = hp <= 0 || boss > 3 || elapsed >= FightEngine.TIME
    override val won get() = boss > 3 && hp > 0
    val pose: PelaoPose get() = when {
        won -> PelaoPose.VICTORY
        finished -> PelaoPose.DEFEAT
        poseTime > 0f -> actionPose
        else -> PelaoPose.IDLE
    }
    override val health get() = "$hp HP"
    override val detail get() = "${(boss - 1).coerceAtMost(3)}/3 rivales · $correctDecisions decisiones · $dodges esquivas"
    override val lesson get() = "Detente ante PARE, luz roja y peatones que cruzan delante de ti. Reduce la velocidad si hay poca visibilidad y conserva la distancia de seguridad."
}

/** A correct defensive response exposes the rival; strike during the opening. */
internal class FightEngine(seed: Int = Random.nextInt()) : ArcadeEngine<FightFrame> {
    private val random = Random(seed)
    private var state = FightFrame(hazard = Hazard.entries.random(random))
    override fun snapshot() = state
    private fun nextHazard(s: FightFrame): FightFrame = s.copy(
        phase = FightPhase.TELEGRAPH,
        openingHits = 0,
        hazard = Hazard.entries.filter { it != s.hazard }.random(random),
        remaining = (6f - (s.boss - 1) * .65f).coerceAtLeast(4.5f),
        message = "¡Lee el peligro!"
    )
    override fun act(action: Int): FightFrame {
        if (state.finished) return state
        val s = state
        state = when (s.phase) {
            FightPhase.TELEGRAPH -> if(action !in 0..2) s else if (action == s.hazard.response) {
                s.copy(phase = FightPhase.OPENING, remaining = 3.2f, score = s.score + 80, power=(s.power+25).coerceAtMost(100), openingHits=0,
                    correctDecisions = s.correctDecisions + 1, actionPose = PelaoPose.BLOCK,
                    poseTime = .55f, impact = 0f, message = s.hazard.advice + " ¡Ataca!")
            } else {
                s.copy(hp = (s.hp - 15).coerceAtLeast(0), phase = FightPhase.RECOVERY,
                    remaining = 2.2f, combo = 0, impact = .35f, missedDecisions = s.missedDecisions + 1,
                    actionPose = PelaoPose.HIT, poseTime = .8f, message = s.hazard.advice)
            }
            FightPhase.OPENING -> if ((action == 3 || action == 4 && s.power>=100) && s.attackLock <= 0f) {
                val special=action==4
                val damage=if(special)45 else 20
                val enemy=(s.bossHp-damage).coerceAtLeast(0)
                val combo=s.combo+1
                val hit=s.copy(bossHp=enemy,score=s.score+100+combo.coerceAtMost(10)*10+(if(special)150 else 0),
                    combo=combo,bestCombo=maxOf(s.bestCombo,combo),strikes=s.strikes+1,
                    attackLock=if(special).72f else .4f,impact=.27f,
                    actionPose=if(special||s.strikes%2==0)PelaoPose.ATTACK_ONE else PelaoPose.ATTACK_TWO,
                    poseTime=.34f,power=if(special)0 else (s.power+15).coerceAtMost(100),
                    openingHits=s.openingHits+1,lastDamage=minOf(damage,s.bossHp),
                    message=if(special)"¡PELAO POWER!" else "¡Combo x$combo!")
                if(enemy==0){
                    if(s.boss==3)hit.copy(boss=4,score=hit.score+500,message="¡Todos los rivales derrotados!")
                    else hit.copy(boss=s.boss+1,bossHp=100+s.boss*20,phase=FightPhase.RECOVERY,
                        remaining=1.2f,score=hit.score+250,hp=(hit.hp+15).coerceAtMost(100),combo=0,
                        message="¡Rival derrotado! Recuperas 15 HP.")
                }else if(s.boss>=2 && hit.openingHits >= (if(s.boss==2)3 else 2)){
                    hit.copy(phase=FightPhase.COUNTER,remaining=if(s.boss==2)1.4f else 1.0f,
                        message="¡Contraataque! Esquiva ahora.")
                }else hit
            }else s
            FightPhase.COUNTER -> if(action==5 && s.remaining>0f){
                s.copy(phase=FightPhase.OPENING,remaining=1.9f,openingHits=0,attackLock=0f,
                    dodges=s.dodges+1,score=s.score+90,power=(s.power+20).coerceAtMost(100),
                    actionPose=PelaoPose.BLOCK,poseTime=.5f,message="¡Esquiva perfecta! Contraataca.")
            }else s
            FightPhase.RECOVERY -> s
        }
        return state
    }
    override fun step(dt: Float): FightFrame {
        if (state.finished || dt <= 0f) return state
        val d = dt.coerceAtMost(.05f)
        var s = state.copy(elapsed = (state.elapsed + d).coerceAtMost(TIME),
            remaining = (state.remaining - d).coerceAtLeast(0f),
            impact = (state.impact - d).coerceAtLeast(0f),
            attackLock = (state.attackLock - d).coerceAtLeast(0f),
            poseTime = (state.poseTime - d).coerceAtLeast(0f))
        if (s.remaining <= 0f && !s.finished) {
            s = when (s.phase) {
                FightPhase.TELEGRAPH -> s.copy(hp = (s.hp - 20).coerceAtLeast(0), combo = 0,
                    phase = FightPhase.RECOVERY, remaining = 2.2f, impact = .4f, missedDecisions = s.missedDecisions + 1,
                    actionPose = PelaoPose.HIT, poseTime = .8f, message = s.hazard.advice)
                FightPhase.COUNTER -> s.copy(hp=(s.hp-18).coerceAtLeast(0),combo=0,phase=FightPhase.RECOVERY,
                    remaining=1.3f,impact=.45f,actionPose=PelaoPose.HIT,poseTime=.8f,message="El rival te alcanzó. Esquiva el próximo golpe.")
                FightPhase.OPENING, FightPhase.RECOVERY -> nextHazard(s)
            }
        }
        state = s
        return s
    }
    companion object { const val TIME = 90f }
}

internal enum class WorkshopPart(val label: String, val tool: String, val symptom: String, val fact: String, val check: String, val choices: List<String>, val correct: Int) {
    TIRE("Neumático", "Manómetro", "Neumático con presión baja", "Comprueba la presión indicada por el fabricante, preferentemente en frío.", "El manómetro indica presión inferior a la recomendada.", listOf("Circular igual", "Ajustar presión", "Desinflar más"), 1),
    LIGHT("Faro", "Revisión de luces", "Faro izquierdo apagado", "Las luces permiten ver y ser visto.", "La prueba confirma que el faro no enciende.", listOf("Resolver la falla", "Circular de noche", "Tapar el faro"), 0),
    BATTERY("Batería", "Multímetro", "Aviso eléctrico de batería", "Una advertencia del sistema de carga exige revisión.", "Se detecta una anomalía eléctrica.", listOf("Ignorar aviso", "Desconectar al azar", "Revisión técnica"), 2),
    GLASS("Parabrisas", "Limpiavidrios", "Parabrisas sucio", "Mantén la visibilidad antes de iniciar la marcha.", "La suciedad impide ver correctamente.", listOf("Limpiar y revisar", "Acelerar", "Conducir igual"), 0)
}

internal data class WorkshopFrame(
    override val elapsed: Float = 0f,
    override val score: Int = 0,
    val lives: Int = 3,
    val jobs: List<WorkshopPart> = WorkshopPart.entries,
    val completed: Int = 0,
    val selectedPart: Int = -1,
    val repairTime: Float = 0f,
    val flash: Float = 0f,
    val checking: Boolean = false,
    override val message: String = "1. Toca la pieza. 2. Elige herramienta. 3. Decide qué hacer."
) : ArcadeFrame {
    val fault get() = jobs.getOrNull(completed)
    override val finished get() = lives <= 0 || completed >= jobs.size || elapsed >= WorkshopEngine.TIME
    override val won get() = completed == jobs.size && lives > 0
    override val health get() = "♥".repeat(lives) + "♡".repeat(3 - lives)
    override val detail get() = "$completed/${jobs.size} diagnósticos"
    override val lesson get() = "Antes de circular, revisa presión de neumáticos, luces, visibilidad y avisos eléctricos. Ante una falla que comprometa la seguridad, no continúes hasta resolverla."
}

internal class WorkshopEngine(seed: Int = Random.nextInt()) : ArcadeEngine<WorkshopFrame> {
    private var state = WorkshopFrame(jobs = WorkshopPart.entries.shuffled(Random(seed)))
    override fun snapshot() = state
    override fun act(action: Int): WorkshopFrame {
        val s = state
        val issue = s.fault ?: return s
        if (s.finished || s.repairTime > 0f) return s
        if (s.checking) {
            if (action in 20..22) {
                state = if (action - 20 == issue.correct) {
                    s.copy(checking = false, repairTime = 1.15f, message = issue.fact)
                } else {
                    s.copy(lives = (s.lives - 1).coerceAtLeast(0), flash = .5f, message = issue.fact)
                }
            }
            return state
        }
        state = when (action) {
            in 0..3 -> s.copy(selectedPart = action, message = "Zona: ${WorkshopPart.entries[action].label}. Elige herramienta.")
            4 -> s.copy(selectedPart = -1, message = "Selecciona la pieza afectada.")
            in 10..13 -> if (s.selectedPart == -1) {
                s.copy(message = "Primero toca una parte del vehículo.")
            } else if (s.selectedPart == issue.ordinal && action - 10 == issue.ordinal) {
                s.copy(checking = true, message = issue.check)
            } else {
                s.copy(lives = (s.lives - 1).coerceAtLeast(0), flash = .5f,
                    selectedPart = -1, message = "No corresponde a esta falla. ${issue.fact}")
            }
            else -> s
        }
        return state
    }
    override fun step(dt: Float): WorkshopFrame {
        if (state.finished || dt <= 0f) return state
        val d = dt.coerceAtMost(.05f)
        val old = state
        var s = old.copy(elapsed = (old.elapsed + d).coerceAtMost(TIME),
            repairTime = (old.repairTime - d).coerceAtLeast(0f), flash = (old.flash - d).coerceAtLeast(0f))
        if (old.repairTime > 0f && s.repairTime <= 0f) {
            s = s.copy(completed = old.completed + 1, selectedPart = -1, score = old.score + 400,
                message = "¡Correcto! ${old.fault?.fact.orEmpty()}")
        }
        state = s
        return s
    }
    companion object { const val TIME = 95f }
}
