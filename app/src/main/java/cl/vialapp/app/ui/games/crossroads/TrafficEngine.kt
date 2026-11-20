package cl.vialapp.app.ui.games.crossroads

// GUÍA DEL ARCHIVO:
// Reglas del tránsito y colisiones del juego. Si un auto hace algo raro, partamos por acá.

import kotlin.random.Random

internal enum class TrafficAxis { VERTICAL, HORIZONTAL }
internal enum class TrafficLane(val axis: TrafficAxis) {
    NORTH(TrafficAxis.VERTICAL), SOUTH(TrafficAxis.VERTICAL),
    WEST(TrafficAxis.HORIZONTAL), EAST(TrafficAxis.HORIZONTAL)
}

internal data class TrafficCar(
    val id: Int,
    val lane: TrafficLane,
    val progress: Float,
    val paint: Int,
    val emergency: Boolean,
    val waiting: Float
)

internal data class TrafficFrame(
    val cars: List<TrafficCar> = emptyList(),
    val green: TrafficAxis? = TrafficAxis.VERTICAL,
    val pending: TrafficAxis? = null,
    val elapsed: Float = 0f,
    val lives: Int = 3,
    val score: Int = 0,
    val passed: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val message: String = "¡Mantén el tráfico en movimiento!",
    val messageUntil: Float = 3f,
    val pedestrianWaiting: Boolean = false,
    val pedestrianCrossing: Float = 0f,
    val pedestrianServed: Int = 0,
    val pedestrianWait: Float = 0f,
    val pedestrianRequested: Boolean = false
) {
    val stage: Int get() = 1 + (elapsed / 25f).toInt().coerceIn(0, 2)
    val finished: Boolean get() = elapsed >= TrafficEngine.DURATION || lives <= 0
}

/** Pure, deterministic simulation. No Android dependency, network or per-car coroutine. */
internal class TrafficEngine(seed: Int = Random.nextInt()) {
    private val random = Random(seed)
    private var frame = TrafficFrame()
    private var spawnIn = .4f
    private var nextId = 0
    private var clearance = 0f
    private var lastAxisChange = -2f
    private var pedestrianDue = 12f
    private var pedestrianReturn = TrafficAxis.VERTICAL

    fun snapshot(): TrafficFrame = frame

    fun requestGreen(axis: TrafficAxis) {
        if (frame.finished || frame.pedestrianRequested || frame.pedestrianCrossing > 0f || frame.green == axis || frame.pending == axis || frame.elapsed - lastAxisChange < .65f) return
        frame = frame.copy(green = null, pending = axis)
        clearance = .85f
        lastAxisChange = frame.elapsed
    }

    // Pedestrians receive an exclusive phase, never a conflicting green light.
    fun requestPedestrian() {
        if (frame.finished || !frame.pedestrianWaiting || frame.pedestrianRequested || frame.pedestrianCrossing > 0f) return
        pedestrianReturn = frame.pending ?: frame.green ?: TrafficAxis.VERTICAL
        clearance = .85f
        frame = frame.copy(green = null, pending = null, pedestrianRequested = true,
            message = "Ambas vías en rojo: espera que el cruce quede libre.", messageUntil = frame.elapsed + 4f)
    }

    fun step(seconds: Float): TrafficFrame {
        if (frame.finished || seconds <= 0f) return frame
        val dt = seconds.coerceAtMost(.05f)
        val elapsed = (frame.elapsed + dt).coerceAtMost(DURATION)
        var green = frame.green
        var pending = frame.pending
        var lives = frame.lives
        var score = frame.score
        var passed = frame.passed
        var combo = frame.combo
        var best = frame.bestCombo
        var message = frame.message
        var until = frame.messageUntil
        var walkWaiting = frame.pedestrianWaiting
        var walkRemaining = frame.pedestrianCrossing
        var walkServed = frame.pedestrianServed
        var walkWait = frame.pedestrianWait
        var walkRequested = frame.pedestrianRequested
        val moved = mutableListOf<TrafficCar>()

        for (lane in TrafficLane.entries) {
            var ahead = 2f
            for (car in frame.cars.filter { it.lane == lane }.sortedByDescending { it.progress }) {
                val rolling = car.progress > STOP + .0001f || green == lane.axis
                val speed = if (car.emergency) .38f else .30f
                val limit = minOf(ahead - GAP, if (rolling) 1.10f else STOP)
                val position = minOf(car.progress + speed * dt, limit).coerceAtLeast(car.progress)
                val waiting = if (position <= STOP + .0001f && position - car.progress < .02f * dt) {
                    car.waiting + dt
                } else if (position > STOP + .0001f) 0f else car.waiting
                val deadline = if (car.emergency) 9f else 15f

                if (position >= 1.06f) {
                    passed++
                    combo++
                    best = maxOf(best, combo)
                    val gain = (if (car.emergency) 350 else 100) + (combo - 1).coerceIn(0, 10) * 15
                    score += gain
                    if (car.emergency || combo % 5 == 0) {
                        message = if (car.emergency) "¡Ambulancia a salvo! +$gain" else "¡Combo x$combo! +$gain"
                        until = elapsed + 2.5f
                    }
                } else if (waiting >= deadline) {
                    lives = (lives - 1).coerceAtLeast(0)
                    combo = 0
                    message = if (car.emergency) "¡Ambulancia retrasada! -1 vida" else "¡Congestión! -1 vida"
                    until = elapsed + 3f
                } else {
                    moved.add(car.copy(progress = position, waiting = waiting))
                    ahead = position
                }
            }
        }

        // Both lights stay red until cars already committed have cleared the crossing.
        if (pending != null) {
            clearance -= dt
            val blocked = moved.any { it.progress > STOP + .0001f && it.progress < CLEAR }
            if (clearance <= 0f && !blocked) {
                green = pending
                pending = null
            }
        }

        if (walkRequested) {
            clearance -= dt
            val blocked = moved.any { it.progress > STOP + .0001f && it.progress < CLEAR }
            if (clearance <= 0f && !blocked) {
                walkRequested = false
                walkWaiting = false
                walkRemaining = 3.4f
                message = "Paso peatonal protegido: espera a que termine de cruzar."
                until = elapsed + 4f
            }
        } else if (walkRemaining > 0f) {
            walkRemaining = (walkRemaining - dt).coerceAtLeast(0f)
            if (walkRemaining <= 0f) {
                walkServed++
                score += 400
                green = pedestrianReturn
                message = "¡Peatones a salvo! +400"
                until = elapsed + 3f
                pedestrianDue = elapsed + 17f
            }
        } else if (!walkWaiting && elapsed >= pedestrianDue) {
            walkWaiting = true
            walkWait = 0f
            message = "¡Peatones esperando! Activa Paso peatonal."
            until = elapsed + 4f
        } else if (walkWaiting) {
            walkWait += dt
            if (walkWait >= 15f) {
                lives = (lives - 1).coerceAtLeast(0)
                combo = 0
                walkWaiting = false
                pedestrianDue = elapsed + 12f
                message = "No diste paso a los peatones. −1 vida."
                until = elapsed + 4f
            }
        }

        spawnIn -= dt
        if (spawnIn <= 0f) {
            val stage = 1 + (elapsed / 25f).toInt().coerceIn(0, 2)
            spawnIn += when (stage) { 1 -> 1.18f; 2 -> .87f; else -> .64f }
            val available = TrafficLane.entries.filter { lane ->
                moved.none { it.lane == lane && it.progress < GAP + .02f }
            }
            if (available.isNotEmpty()) {
                val lane = available.random(random)
                nextId++
                val emergency = nextId >= 8 && nextId % 11 == 0
                moved.add(TrafficCar(nextId, lane, -.035f, random.nextInt(5), emergency, 0f))
                if (emergency) {
                    message = "¡Ambulancia! Dale paso cuando sea seguro."
                    until = elapsed + 3f
                }
            }
        }
        frame = TrafficFrame(moved, green, pending, elapsed, lives, score, passed, combo, best, message, until,
            walkWaiting, walkRemaining, walkServed, walkWait, walkRequested)
        return frame
    }

    companion object {
        const val DURATION = 75f
        const val STOP = .29f
        const val CLEAR = .72f
        private const val GAP = .112f
    }
}
