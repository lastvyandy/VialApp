package cl.vialapp.app.ui.games.crossroads

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.games.GameRunResult
import cl.vialapp.app.data.session.SessionManager
import java.util.UUID
import kotlin.math.ceil

private const val ArcadeRecordId = GameProgressStore.CROSSROADS_ARCADE
private enum class ArcadePhase { INTRO, PLAY, RESULT }

@Composable
fun CrossroadsChaosScreen(userKey: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context, userKey) { GameProgressStore(context, userKey) }
    val session = remember(context) { SessionManager(context) }
    var phase by remember(userKey) { mutableStateOf(ArcadePhase.INTRO) }
    var engine by remember(userKey) { mutableStateOf(TrafficEngine()) }
    var frame by remember(userKey) { mutableStateOf(engine.snapshot()) }
    var runId by remember(userKey) { mutableStateOf(UUID.randomUUID().toString()) }
    var paused by remember { mutableStateOf(false) }
    var exitDialog by remember { mutableStateOf(false) }
    var result by remember(userKey) { mutableStateOf<GameRunResult?>(null) }
    var saving by remember(userKey) { mutableStateOf(false) }
    var record by remember(userKey) { mutableIntStateOf(store.getHighScore(ArcadeRecordId)) }

    fun start() {
        engine = TrafficEngine()
        frame = engine.snapshot()
        runId = UUID.randomUUID().toString()
        result = null
        saving = false
        paused = false
        phase = ArcadePhase.PLAY
    }
    fun requestExit() {
        if (phase == ArcadePhase.PLAY) {
            paused = true
            exitDialog = true
        } else if (!saving) onBack()
    }

    BackHandler { requestExit() }
    val latestPhase by rememberUpdatedState(phase)
    DisposableEffect(context) {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && latestPhase == ArcadePhase.PLAY) paused = true
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    LaunchedEffect(phase, paused, runId) {
        if (phase != ArcadePhase.PLAY || paused) return@LaunchedEffect
        var previous = withFrameNanos { it }
        var accumulator = 0f
        while (phase == ArcadePhase.PLAY && !paused) {
            val now = withFrameNanos { it }
            accumulator += ((now - previous) / 1_000_000_000f).coerceIn(0f, .10f)
            previous = now
            if (accumulator >= 1f / 30f) {
                frame = engine.step(accumulator)
                accumulator = 0f
                if (frame.finished) {
                    saving = frame.passed > 0
                    phase = ArcadePhase.RESULT
                }
            }
        }
    }

    LaunchedEffect(phase, runId) {
        if (phase != ArcadePhase.RESULT || !saving || result != null) return@LaunchedEffect
        val token = session.getAccessToken()
        // Scores use a new local record category because the previous quiz used a different scale.
        // The existing backend game-event endpoint accepts this ID and applies its established caps.
        result = if (!token.isNullOrBlank()) {
            store.recordRunRemote(
                api = ApiClient.api, token = token, gameId = ArcadeRecordId,
                score = frame.score, durationSeconds = ceil(frame.elapsed.toDouble()).toInt().coerceAtLeast(1),
                fallbackXp = 0, eventId = runId
            )
        } else {
            val newRecord = store.recordRun(ArcadeRecordId, frame.score, 0)
            GameRunResult(newRecord, 0, frame.score, false)
        }
        record = store.getHighScore(ArcadeRecordId)
        saving = false
    }

    if (exitDialog) {
        AlertDialog(
            onDismissRequest = { exitDialog = false; paused = false },
            title = { Text("¿Salir de la partida?") },
            text = { Text("El progreso de esta partida no se guardará.") },
            confirmButton = { TextButton(onClick = onBack) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { exitDialog = false; paused = false }) { Text("Continuar") } }
        )
    }

    when (phase) {
        ArcadePhase.INTRO -> ArcadeIntro(record, onBack, ::start)
        ArcadePhase.PLAY -> ArcadePlay(
            frame, paused, { paused = !paused }, ::requestExit,
            onPedestrian = {
                if (!paused) {
                    engine.requestPedestrian()
                    frame = engine.snapshot()
                }
            },
            onSignal = {
                if (!paused) {
                    engine.requestGreen(it)
                    frame = engine.snapshot()
                }
            }
        )
        ArcadePhase.RESULT -> ArcadeResult(frame, record, result, saving, ::start, onBack)
    }
}

@Composable
private fun ArcadeIntro(record: Int, onBack: () -> Unit, onStart: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val demo = remember {
        TrafficFrame(cars = listOf(
            TrafficCar(1, TrafficLane.NORTH, .23f, 0, false, 0f),
            TrafficCar(2, TrafficLane.WEST, .20f, 1, false, 0f),
            TrafficCar(3, TrafficLane.SOUTH, .78f, 2, false, 0f),
            TrafficCar(4, TrafficLane.EAST, .09f, 4, false, 0f)
        ))
    }
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("‹ Minijuegos") }
        Text("CRUCE CAÓTICO", color = c.primary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text("Domina la ciudad", color = c.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Text("75 segundos. Cuatro calles. Un solo objetivo: que el tráfico no colapse.", color = c.onSurfaceVariant)
        TrafficBoard(demo)
        Card(colors = CardDefaults.cardColors(containerColor = c.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cómo jugar", fontWeight = FontWeight.Bold, color = c.onSurface)
                Text("🚦 Alterna el paso vertical y horizontal. El cruce se despeja automáticamente antes de cambiar.", color = c.onSurfaceVariant, fontSize = 14.sp)
                Text("🚑 Prioriza ambulancias de emergencia sin abrir un cruce ocupado. Los autos que esperan demasiado te cuestan una vida.", color = c.onSurfaceVariant, fontSize = 14.sp)
                Text("🚶 Cuando aparezcan peatones, activa Paso peatonal. Los vehículos se detienen y el cruce se abre únicamente cuando está libre.", color = c.onSurfaceVariant, fontSize = 14.sp)
                Text("🏆 Récord: $record puntos", color = c.primary, fontWeight = FontWeight.Bold)
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) {
            Text("¡A jugar!", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text("Simulación arcade: no sustituye las reglas de tránsito reales.", fontSize = 11.sp, color = c.onSurfaceVariant)
    }
}

@Composable
private fun ArcadePlay(
    frame: TrafficFrame, paused: Boolean, onPause: () -> Unit,
    onExit: () -> Unit, onPedestrian: () -> Unit, onSignal: (TrafficAxis) -> Unit
) {
    val c = MaterialTheme.colorScheme
    val time = ceil((TrafficEngine.DURATION - frame.elapsed).coerceAtLeast(0f).toDouble()).toInt()
    Column(
        Modifier.fillMaxSize().background(c.background).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit, contentPadding = PaddingValues(0.dp)) { Text("‹ Salir") }
            Spacer(Modifier.weight(1f))
            Text("OLEADA ${frame.stage}/3", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = c.primary)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onPause, contentPadding = PaddingValues(0.dp)) { Text(if (paused) "▶ Seguir" else "Ⅱ Pausa") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudValue("PUNTOS", frame.score.toString(), Modifier.weight(1f))
            HudValue("TIEMPO", "${time}s", Modifier.weight(1f))
            HudValue("VIDAS", "♥".repeat(frame.lives) + "♡".repeat(3 - frame.lives), Modifier.weight(1f), Color(0xFFF87171))
        }
        LinearProgressIndicator(
            progress = { (frame.elapsed / TrafficEngine.DURATION).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = c.primary, trackColor = c.surfaceVariant
        )
        // Flexible board leaves room for controls even on smaller handsets.
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight)
                TrafficBoard(frame, Modifier.size(side))
            }
            if (paused) {
                Surface(color = Color(0xE61B2738), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EN PAUSA", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        TextButton(onClick = onPause) { Text("Continuar", color = Color(0xFFBFDBFE)) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${frame.passed} autos · ${frame.pedestrianServed} pasos", color = c.onSurfaceVariant, fontSize = 13.sp)
            Text("COMBO ×${frame.combo}", color = c.primary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Surface(color = c.surface, shape = RoundedCornerShape(12.dp)) {
            Text(
                when {
                    frame.pedestrianCrossing > 0f -> "Peatones cruzando · todas las vías en rojo"
                    frame.pedestrianRequested -> "Despejando el cruce para los peatones"
                    frame.pedestrianWaiting -> "Peatones esperando · activa Paso peatonal"
                    frame.pending != null -> "Despejando el cruce · ambas vías en rojo"
                    frame.elapsed <= frame.messageUntil -> frame.message
                    else -> "Filas: N/S ${frame.cars.count { it.lane.axis == TrafficAxis.VERTICAL && it.waiting > 0f }} · E/O ${frame.cars.count { it.lane.axis == TrafficAxis.HORIZONTAL && it.waiting > 0f }}"
                },
                Modifier.fillMaxWidth().padding(10.dp),
                color = c.onSurface, fontSize = 12.sp, textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SignalButton("↕", "Norte / Sur", frame.green == TrafficAxis.VERTICAL, !paused && !frame.pedestrianRequested && frame.pedestrianCrossing <= 0f, Modifier.weight(1f)) { onSignal(TrafficAxis.VERTICAL) }
            SignalButton("↔", "Este / Oeste", frame.green == TrafficAxis.HORIZONTAL, !paused && !frame.pedestrianRequested && frame.pedestrianCrossing <= 0f, Modifier.weight(1f)) { onSignal(TrafficAxis.HORIZONTAL) }
        }
        Button(
            onClick = onPedestrian,
            enabled = !paused && frame.pedestrianWaiting && !frame.pedestrianRequested && frame.pedestrianCrossing <= 0f,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534), contentColor = Color.White)
        ) { Text(if (frame.pedestrianWaiting && !frame.pedestrianRequested) "🚶 Dar paso a peatones" else "Paso peatonal", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun HudValue(label: String, value: String, modifier: Modifier, accent: Color = MaterialTheme.colorScheme.onSurface) {
    val c = MaterialTheme.colorScheme
    Surface(modifier = modifier, color = c.surface, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, c.outlineVariant)) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = c.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

@Composable
private fun SignalButton(arrow: String, label: String, green: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (green) Color(0xFF166534) else c.surfaceContainerHigh,
            contentColor = if (green) Color.White else c.onSurface,
            disabledContainerColor = c.surfaceVariant, disabledContentColor = c.onSurfaceVariant
        ),
        border = BorderStroke(1.dp, if (green) Color(0xFF4ADE80) else c.outlineVariant),
        contentPadding = PaddingValues(6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$arrow  ${if (green) "●" else "○"}", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = if (green && enabled) Color(0xFFBBF7D0) else c.onSurfaceVariant)
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(if (green) "VERDE" else "ROJO", fontSize = 10.sp)
        }
    }
}

@Composable
private fun ArcadeResult(frame: TrafficFrame, record: Int, result: GameRunResult?, saving: Boolean, onRestart: () -> Unit, onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(if (frame.lives > 0) "🏁" else "🚦", fontSize = 54.sp)
        Text(if (frame.lives > 0) "¡Ciudad bajo control!" else "¡Hora punta!", color = c.onSurface, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("${frame.score}", color = c.primary, fontWeight = FontWeight.ExtraBold, fontSize = 55.sp)
        Text("PUNTOS", color = c.onSurfaceVariant, fontWeight = FontWeight.Bold)
        if (result?.isNewHighScore == true) Text("🏆 ¡Nuevo récord!", color = c.primary, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudValue("AUTOS", frame.passed.toString(), Modifier.weight(1f))
            HudValue("COMBO", "×${frame.bestCombo}", Modifier.weight(1f))
            HudValue("RÉCORD", record.toString(), Modifier.weight(1f))
        }
        Surface(color = c.primaryContainer, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("Paso peatonal: ${frame.pedestrianServed}", color = c.onPrimaryContainer, fontWeight = FontWeight.Bold)
                Text("Recuerda: no ingreses a una intersección bloqueada; respeta los semáforos y permite el paso seguro de peatones y vehículos de emergencia.", color = c.onPrimaryContainer, fontSize = 13.sp)
            }
        }
        if (saving) {
            CircularProgressIndicator(Modifier.size(24.dp))
            Text("Guardando partida…", color = c.onSurfaceVariant)
        } else if (result != null) {
            if (result.syncedWithBackend) Text("+${result.xpAwarded} XP", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            else Text("Récord guardado en este dispositivo. XP no confirmado.", color = c.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp)
        }
        Button(onClick = onRestart, enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Jugar otra vez", fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = onBack, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Volver a minijuegos") }
    }
}
