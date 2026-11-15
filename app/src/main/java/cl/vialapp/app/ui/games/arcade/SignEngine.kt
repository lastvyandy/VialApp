package cl.vialapp.app.ui.games.arcade

import kotlin.random.Random

// Educational renderings checked against the CONASET Libro para la conducción en Chile,
// Clase B, February 2026, annex 1 pages 150–151. Not manufacturing specifications.
internal enum class RoadSign(val title: String, val meaning: String, val advice: String) {
    STOP("PARE", "Detenerse completamente", "Detente y reanuda la marcha solo cuando sea seguro."),
    YIELD("CEDA EL PASO", "Ceder la preferencia", "Reduce la velocidad y detente si hace falta para ceder el paso."),
    NO_ENTRY("NO ENTRAR", "No ingresar a esa vía", "No ingreses por una vía con esta prohibición."),
    NO_LEFT("NO VIRAR IZQUIERDA", "No girar a la izquierda", "Continúa por una dirección permitida."),
    MAX_50("VELOCIDAD MÁXIMA", "No superar 50 km/h", "50 es el máximo indicado; reduce aún más si las condiciones lo requieren."),
    NO_PARKING("PROHIBIDO ESTACIONAR", "No estacionar", "Busca un lugar donde esté permitido estacionar.")
}

internal data class SignQuestion(val sign: RoadSign, val choices: List<RoadSign>)

internal data class SignFrame(
    val questions: List<SignQuestion>,
    override val elapsed: Float = 0f,
    override val score: Int = 0,
    val lives: Int = 3,
    val index: Int = 0,
    val answered: Int = -1,
    val feedback: Float = 0f,
    val correct: Int = 0,
    val combo: Int = 0,
    val missed: List<RoadSign> = emptyList(),
    val flight: Float = 0f,
    val timedOut: Boolean = false,
    override val message: String = "Observa la señal y elige qué debes hacer."
) : ArcadeFrame {
    val current get() = questions.getOrNull(index) ?: questions.last()
    val window get() = (7f-index*.40f).coerceAtLeast(4.5f)
    val approach get() = (flight/window).coerceIn(0f,1f)
    override val finished get() = elapsed >= SignEngine.TIME || lives <= 0 || index >= questions.size
    override val won get() = index >= questions.size && lives > 0
    override val health get() = "♥".repeat(lives) + "♡".repeat(3 - lives)
    override val detail get() = "$correct/${questions.size} señales · combo ×$combo"
    override val lesson get() = if (missed.isEmpty()) "Reconoce forma, color y símbolo. Las señales reglamentarias establecen prioridades, límites y prohibiciones."
        else missed.distinct().joinToString("\n") { it.title + ": " + it.advice }
}

internal class SignEngine(seed: Int = Random.nextInt()) : ArcadeEngine<SignFrame> {
    private val random = Random(seed)
    private var state = SignFrame(RoadSign.entries.shuffled(random).map { sign ->
        SignQuestion(sign, (RoadSign.entries.filter { it != sign }.shuffled(random).take(2) + sign).shuffled(random))
    })
    override fun snapshot() = state
    override fun act(action: Int): SignFrame {
        val s = state
        if (s.finished || s.feedback > 0f || action !in s.current.choices.indices) return s
        val right = s.current.choices[action] == s.current.sign
        state = if (right) s.copy(answered = action, feedback = 1.45f, correct = s.correct + 1,
            combo = s.combo + 1, score = s.score + 180 + s.combo * 40 + ((1f-s.approach)*60f).toInt(), message = "¡Correcto! " + s.current.sign.advice)
        else s.copy(answered = action, feedback = 2.25f, lives = (s.lives - 1).coerceAtLeast(0),
            combo = 0, missed = s.missed + s.current.sign, message = s.current.sign.advice)
        return state
    }
    override fun step(dt: Float): SignFrame {
        if (state.finished || dt <= 0f) return state
        val s = state
        val d = dt.coerceAtMost(.05f)
        val remaining = (s.feedback - d).coerceAtLeast(0f)
        state = if (s.feedback > 0f && remaining == 0f)
            s.copy(index = s.index + 1, answered = -1, feedback = 0f, flight=0f, timedOut=false, message = "Atrapa la próxima señal.")
        else if(s.feedback > 0f) s.copy(feedback=remaining)
        else {
            val next=s.copy(elapsed=(s.elapsed+d).coerceAtMost(TIME),flight=s.flight+d)
            if(next.flight>=next.window && !next.finished) next.copy(
                lives=(s.lives-1).coerceAtLeast(0), combo=0, timedOut=true, feedback=2.25f,
                missed=s.missed+s.current.sign, message="Se escapó la señal. "+s.current.sign.advice)
            else next
        }
        return state
    }
    companion object { const val TIME = 65f }
}

