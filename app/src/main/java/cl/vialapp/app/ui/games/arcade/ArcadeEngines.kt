package cl.vialapp.app.ui.games.arcade

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
    override val message: String = "¡Mantén tu carril libre!"
) : ArcadeFrame {
    override val finished get() = lives <= 0 || elapsed >= DriveEngine.TIME
    override val won get() = lives > 0 && elapsed >= DriveEngine.TIME
    override val health get() = "♥".repeat(lives) + "♡".repeat(3 - lives)
    override val detail get() = "$coins monedas · $dodged obstáculos superados"
}

/** Three lanes, smooth steering, collisions and a timed school-zone challenge. */
internal class DriveEngine(seed: Int = Random.nextInt()) : ArcadeEngine<DriveFrame> {
    private val random = Random(seed)
    private var state = DriveFrame()
    private var spawn = .4f
    private var id = 0
    private var speeding = 0f
    private var zoneBonus = false
    override fun snapshot() = state
    override fun act(action: Int): DriveFrame {
        if (state.finished) return state
        state = when (action) {
            0 -> state.copy(lane = (state.lane - 1).coerceAtLeast(0))
            1 -> state.copy(lane = (state.lane + 1).coerceAtMost(2))
            2 -> state.copy(slow = !state.slow)
            else -> state
        }
        return state
    }

    override fun step(dt: Float): DriveFrame {
        if (state.finished || dt <= 0f) return state
        val d = dt.coerceAtMost(.05f)
        val elapsed = (state.elapsed + d).coerceAtMost(TIME)
        val zone = (elapsed in 18f..27f || elapsed in 43f..52f)
        var lives = state.lives
        var score = state.score
        var message = state.message
        var invulnerable = (state.invulnerable - d).coerceAtLeast(0f)
        var coins = state.coins
        var dodged = state.dodged
        val x = state.carX + (state.lane - state.carX).coerceIn(-d * 7f, d * 7f)
        val speed = if (state.slow) .27f else .43f + elapsed / 240f

        if (zone && !state.schoolZone) {
            speeding = 0f; zoneBonus = true
            message = "Zona escolar: activa velocidad reducida."
        }
        if (zone && !state.slow) {
            speeding += d
            if (speeding >= 2f && zoneBonus) {
                lives = (lives - 1).coerceAtLeast(0)
                zoneBonus = false
                message = "Exceso de velocidad en zona escolar."
            }
        } else if (zone) speeding = 0f
        if (!zone && state.schoolZone && zoneBonus) {
            score += 300
            message = "¡Zona escolar respetada! +300"
        }

        val next = mutableListOf<RoadItem>()
        for (item in state.items) {
            val y = item.y + speed * d
            val touching = abs(x - item.lane) < .48f && y >= .75f && y <= .91f
            when {
                touching && item.kind == RoadItemKind.COIN -> {
                    coins++; score += 100; message = "¡Moneda! +100"
                }
                touching && invulnerable <= 0f -> {
                    lives = (lives - 1).coerceAtLeast(0)
                    invulnerable = 1.4f
                    message = "¡Cuidado! Perdiste una vida."
                }
                touching -> Unit
                y > 1.11f -> if (item.kind != RoadItemKind.COIN) { dodged++; score += 60 }
                else -> next += item.copy(y = y)
            }
        }
        spawn -= d
        if (spawn <= 0f) {
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
            state.distance + speed * d * 100f, next, coins, dodged, invulnerable, message)
        return state
    }
    companion object { const val TIME = 65f }
}

internal enum class FightPhase { TELEGRAPH, OPENING, RECOVERY }
internal enum class Hazard(val label: String, val response: Int, val advice: String) {
    PEDESTRIAN("Peatón en la calzada", 0, "Frena ante un peatón."),
    CLEAR_LANE("Obstáculo · carril contiguo libre", 1, "Solo esquiva si es seguro."),
    TAILGATE("Vehículo demasiado cerca", 2, "Aumenta la distancia de seguridad.")
}
internal data class FightFrame(
    override val elapsed: Float = 0f,
    override val score: Int = 0,
    val hp: Int = 100,
    val bossHp: Int = 100,
    val boss: Int = 1,
    val hazard: Hazard = Hazard.PEDESTRIAN,
    val phase: FightPhase = FightPhase.TELEGRAPH,
    val remaining: Float = 2.8f,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val strikes: Int = 0,
    val impact: Float = 0f,
    val attackLock: Float = 0f,
    override val message: String = "Observa el peligro y responde a tiempo."
) : ArcadeFrame {
    override val finished get() = hp <= 0 || boss > 3 || elapsed >= FightEngine.TIME
    override val won get() = boss > 3 && hp > 0
    override val health get() = "$hp HP"
    override val detail get() = "${(boss - 1).coerceAtMost(3)}/3 rivales · combo x$bestCombo"
}

/** A correct defensive response exposes the rival; strike during the opening. */
internal class FightEngine(seed: Int = Random.nextInt()) : ArcadeEngine<FightFrame> {
    private val random = Random(seed)
    private var state = FightFrame(hazard = Hazard.entries.random(random))
    override fun snapshot() = state
    private fun nextHazard(s: FightFrame): FightFrame = s.copy(
        phase = FightPhase.TELEGRAPH,
        hazard = Hazard.entries.random(random),
        remaining = (3.2f - (s.boss - 1) * .4f).coerceAtLeast(2f),
        message = "¡Lee el peligro!"
    )
    override fun act(action: Int): FightFrame {
        if (state.finished) return state
        val s = state
        state = when (s.phase) {
            FightPhase.TELEGRAPH -> if (action == s.hazard.response) {
                s.copy(phase = FightPhase.OPENING, remaining = 2.1f, score = s.score + 80,
                    message = "¡Bien! Ahora ataca al rival.")
            } else {
                s.copy(hp = (s.hp - 15).coerceAtLeast(0), phase = FightPhase.RECOVERY,
                    remaining = .65f, combo = 0, impact = .35f, message = s.hazard.advice)
            }
            FightPhase.OPENING -> if (action == 3 && s.attackLock <= 0f) {
                val enemy = (s.bossHp - 20).coerceAtLeast(0)
                val combo = s.combo + 1
                val hit = s.copy(bossHp = enemy, score = s.score + 100 + combo.coerceAtMost(10) * 10,
                    combo = combo, bestCombo = maxOf(s.bestCombo, combo), strikes = s.strikes + 1,
                    attackLock = .33f, impact = .25f, message = "¡Combo x$combo!")
                if (enemy == 0) {
                    if (s.boss == 3) hit.copy(boss = 4, score = hit.score + 500, message = "¡Todos los rivales derrotados!")
                    else hit.copy(boss = s.boss + 1, bossHp = 100 + s.boss * 20, phase = FightPhase.RECOVERY,
                        remaining = 1.2f, score = hit.score + 250, message = "¡Rival derrotado!")
                } else hit
            } else s
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
            attackLock = (state.attackLock - d).coerceAtLeast(0f))
        if (s.remaining <= 0f && !s.finished) {
            s = when (s.phase) {
                FightPhase.TELEGRAPH -> s.copy(hp = (s.hp - 20).coerceAtLeast(0), combo = 0,
                    phase = FightPhase.RECOVERY, remaining = .65f, impact = .4f, message = s.hazard.advice)
                FightPhase.OPENING, FightPhase.RECOVERY -> nextHazard(s)
            }
        }
        state = s
        return s
    }
    companion object { const val TIME = 75f }
}

internal enum class WorkshopPart(val label: String, val tool: String, val symptom: String, val fact: String) {
    TIRE("Neumático", "Manómetro", "Neumático con presión baja", "Comprueba la presión indicada por el fabricante."),
    LIGHT("Faro", "Revisión de luces", "Faro izquierdo apagado", "Las luces deben funcionar antes de circular."),
    BATTERY("Batería", "Multímetro", "Aviso eléctrico de batería", "La revisión eléctrica permite detectar fallas."),
    GLASS("Parabrisas", "Limpiavidrios", "Parabrisas sucio", "Mantén la visibilidad antes de iniciar la marcha.")
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
    override val message: String = "Inspecciona el vehículo y elige una herramienta."
) : ArcadeFrame {
    val fault get() = jobs.getOrNull(completed)
    override val finished get() = lives <= 0 || completed >= jobs.size || elapsed >= WorkshopEngine.TIME
    override val won get() = completed == jobs.size && lives > 0
    override val health get() = "♥".repeat(lives) + "♡".repeat(3 - lives)
    override val detail get() = "$completed/${jobs.size} diagnósticos"
}

internal class WorkshopEngine(seed: Int = Random.nextInt()) : ArcadeEngine<WorkshopFrame> {
    private var state = WorkshopFrame(jobs = WorkshopPart.entries.shuffled(Random(seed)))
    override fun snapshot() = state
    override fun act(action: Int): WorkshopFrame {
        val s = state
        val issue = s.fault ?: return s
        if (s.finished || s.repairTime > 0f) return s
        state = when (action) {
            in 0..3 -> s.copy(selectedPart = action, message = "Zona: ${WorkshopPart.entries[action].label}. Elige herramienta.")
            in 10..13 -> if (s.selectedPart == -1) {
                s.copy(message = "Primero toca una parte del vehículo.")
            } else if (s.selectedPart == issue.ordinal && action - 10 == issue.ordinal) {
                s.copy(repairTime = 1.15f, message = "Comprobación en curso…")
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
    companion object { const val TIME = 65f }
}
