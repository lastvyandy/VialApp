package cl.vialapp.app.ui.games.arcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.games.GameProgressStore
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
    override val message: String = "Observa la señal y elige qué debes hacer."
) : ArcadeFrame {
    val current get() = questions.getOrNull(index) ?: questions.last()
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
        state = if (right) s.copy(answered = action, feedback = 2.5f, correct = s.correct + 1,
            combo = s.combo + 1, score = s.score + 180 + s.combo * 40, message = "¡Correcto! " + s.current.sign.advice)
        else s.copy(answered = action, feedback = 3f, lives = (s.lives - 1).coerceAtLeast(0),
            combo = 0, missed = s.missed + s.current.sign, message = s.current.sign.advice)
        return state
    }
    override fun step(dt: Float): SignFrame {
        if (state.finished || dt <= 0f) return state
        val s = state
        val d = dt.coerceAtMost(.05f)
        val remaining = (s.feedback - d).coerceAtLeast(0f)
        state = if (s.feedback > 0f && remaining == 0f)
            s.copy(index = s.index + 1, answered = -1, feedback = 0f, message = "Observa la próxima señal.")
        else s.copy(elapsed = (s.elapsed + if (s.feedback > 0f) 0f else d).coerceAtMost(TIME), feedback = remaining)
        return state
    }
    companion object { const val TIME = 65f }
}

@Composable
fun SignArcadeScreen(userKey: String, onBack: () -> Unit) {
    ArcadeSession(userKey = userKey, gameId = GameProgressStore.SIGN_HUNT_ARCADE,
        title = "Caza la Señal", subtitle = "Seis señales chilenas, decisiones rápidas y combos.",
        rules = "Observa la señal que aparece sobre la carretera. Toca la acción correcta, no el nombre de la señal. Lee la explicación antes de pasar a la siguiente. Tienes tres vidas; las explicaciones no consumen tiempo.",
        duration = SignEngine.TIME, create = { SignEngine() }, onBack = onBack,
        scene = { frame, modifier, _, _ -> SignScene(frame, modifier) },
        controls = { frame, enabled, act ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                frame.current.choices.forEachIndexed { index, choice ->
                    val reveal = frame.answered >= 0
                    val correct = choice == frame.current.sign
                    val c = MaterialTheme.colorScheme
                    Button(onClick = { act(index) }, enabled = enabled && !reveal,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.primaryContainer, contentColor = c.onPrimaryContainer,
                            disabledContainerColor = if (reveal && correct) Color(0xFF166534)
                            else if (reveal && index == frame.answered) Color(0xFFB42318) else c.surfaceVariant,
                            disabledContentColor = if (reveal && (correct || index == frame.answered)) Color.White else c.onSurfaceVariant
                        )) { Text(choice.meaning, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
            }
        }
    )
}

@Composable
private fun SignScene(frame: SignFrame, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF172C47)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("SEÑAL ${(frame.index + 1).coerceAtMost(frame.questions.size)}/${frame.questions.size}",
            fontSize = 12.sp, color = Color(0xFFBFDBFE), fontWeight = FontWeight.Bold)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF8AC9E8), Color(0xFFD9EAE9))), size = size)
                val horizon = size.height * .55f
                drawRect(Color(0xFF79A77E), Offset(0f, horizon), Size(size.width, size.height - horizon))
                val road = Path().apply {
                    moveTo(size.width * .43f, horizon); lineTo(size.width * .57f, horizon)
                    lineTo(size.width * .95f, size.height); lineTo(size.width * .05f, size.height); close()
                }
                drawPath(road, Color(0xFF3B4755))
                drawLine(Color(0xFFF1F5F9), Offset(size.width * .5f, horizon + 6f), Offset(size.width * .5f, size.height), 4f)
                drawLine(Color(0xFF536174), Offset(size.width * .50f, size.height * .45f), Offset(size.width * .50f, size.height * .88f), 10f)
            }
            RoadSignArt(frame.current.sign, Modifier.fillMaxHeight(.81f).aspectRatio(1f).padding(5.dp))
        }
        Text(if (frame.answered >= 0) frame.current.sign.title else "¿Qué debes hacer?",
            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun RoadSignArt(sign: RoadSign, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = minOf(size.width, size.height) / 200f
        val origin = Offset((size.width - 200f * u) / 2, (size.height - 200f * u) / 2)
        translate(origin.x, origin.y) {
        scale(u, u, pivot = Offset.Zero) {
            val red = Color(0xFFE11D37)
            when (sign) {
                RoadSign.STOP -> {
                    val shape = Path().apply {
                        for (i in 0..7) {
                            val a = Math.PI / 8 + i * Math.PI / 4
                            val x = 100f + (94 * kotlin.math.cos(a)).toFloat()
                            val y = 100f + (94 * kotlin.math.sin(a)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(shape, red)
                    drawPath(shape, Color.White, style = Stroke(4f))
                    signText("PARE", 51f, 117f, android.graphics.Color.WHITE)
                }
                RoadSign.YIELD -> {
                    val triangle = Path().apply { moveTo(15f, 28f); lineTo(185f, 28f); lineTo(100f, 178f); close() }
                    drawPath(triangle, Color.White)
                    drawPath(triangle, red, style = Stroke(19f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
                else -> {
                    drawCircle(Color.White, 94f, Offset(100f,100f))
                    drawCircle(red, 85f, Offset(100f,100f), style = Stroke(15f))
                    when (sign) {
                        RoadSign.MAX_50 -> signText("50", 76f, 126f, android.graphics.Color.BLACK)
                        RoadSign.NO_PARKING -> signText("E", 98f, 133f, android.graphics.Color.BLACK)
                        RoadSign.NO_ENTRY -> {
                            drawLine(Color.Black, Offset(100f, 151f), Offset(100f, 70f), 23f)
                            val arrow = Path().apply { moveTo(100f, 45f); lineTo(64f, 83f); lineTo(136f, 83f); close() }
                            drawPath(arrow, Color.Black)
                        }
                        RoadSign.NO_LEFT -> {
                            drawLine(Color.Black, Offset(123f, 150f), Offset(123f, 85f), 23f)
                            drawLine(Color.Black, Offset(123f, 85f), Offset(67f, 85f), 23f, StrokeCap.Butt)
                            val arrow = Path().apply { moveTo(37f, 85f); lineTo(79f, 51f); lineTo(79f, 119f); close() }
                            drawPath(arrow, Color.Black)
                        }
                        else -> Unit
                    }
                    if (sign != RoadSign.MAX_50) {
                        if (sign == RoadSign.NO_LEFT) drawLine(red, Offset(42f, 158f), Offset(158f, 42f), 15f)
                        else drawLine(red, Offset(42f, 42f), Offset(158f, 158f), 15f)
                    }
                }
            }
        }
        }
    }
}

private fun DrawScope.signText(value: String, sizePx: Float, baseline: Float, color: Int) {
    drawContext.canvas.nativeCanvas.drawText(value, 100f, baseline,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sizePx
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
}
