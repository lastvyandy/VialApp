package cl.vialapp.app.ui.games.arcade

import androidx.activity.compose.BackHandler
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

private enum class Phase { INTRO, PLAY, RESULT }

@Composable
internal fun <S : ArcadeFrame> ArcadeSession(
    userKey: String,
    gameId: String,
    title: String,
    subtitle: String,
    rules: String,
    duration: Float,
    create: () -> ArcadeEngine<S>,
    onBack: () -> Unit,
    scene: @Composable (S, Modifier, Boolean, (Int) -> Unit) -> Unit,
    controls: @Composable (S, Boolean, (Int) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val store = remember(context, userKey) { GameProgressStore(context, userKey) }
    val session = remember(context) { SessionManager(context) }
    var engine by remember(userKey, gameId) { mutableStateOf(create()) }
    var frame by remember(userKey, gameId) { mutableStateOf(engine.snapshot()) }
    var phase by remember(userKey, gameId) { mutableStateOf(Phase.INTRO) }
    var paused by remember { mutableStateOf(false) }
    var exitDialog by remember { mutableStateOf(false) }
    var runId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var reward by remember { mutableStateOf<GameRunResult?>(null) }
    var saving by remember { mutableStateOf(false) }
    var record by remember(userKey, gameId) { mutableIntStateOf(store.getHighScore(gameId)) }
    val c = MaterialTheme.colorScheme

    fun start() {
        engine = create()
        frame = engine.snapshot()
        paused = false
        runId = UUID.randomUUID().toString()
        reward = null
        saving = false
        phase = Phase.PLAY
    }
    fun leave() {
        if (phase == Phase.PLAY) { paused = true; exitDialog = true }
        else if (!saving) onBack()
    }
    fun finishIfOver() {
        if (frame.finished && phase == Phase.PLAY) {
            saving = frame.score > 0
            phase = Phase.RESULT
        }
    }
    fun act(action: Int) {
        if (phase == Phase.PLAY && !paused) {
            frame = engine.act(action)
            finishIfOver()
        }
    }

    BackHandler { leave() }
    val latestPhase by rememberUpdatedState(phase)
    DisposableEffect(context) {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && latestPhase == Phase.PLAY) paused = true
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    LaunchedEffect(phase, paused, runId) {
        if (phase != Phase.PLAY || paused) return@LaunchedEffect
        var last = withFrameNanos { it }
        var carry = 0f
        while (phase == Phase.PLAY && !paused) {
            val now = withFrameNanos { it }
            carry += ((now - last) / 1_000_000_000f).coerceIn(0f, .12f)
            last = now
            var updated = false
            while (carry >= 1f / 30f && !frame.finished) {
                frame = engine.step(1f / 30f)
                carry -= 1f / 30f
                updated = true
            }
            if (updated) finishIfOver()
        }
    }

    LaunchedEffect(phase, runId) {
        if (phase != Phase.RESULT || !saving || reward != null) return@LaunchedEffect
        val token = session.getAccessToken()
        try {
            reward = if (!token.isNullOrBlank()) {
                store.recordRunRemote(ApiClient.api, token, gameId, frame.score,
                    ceil(frame.elapsed.toDouble()).toInt().coerceAtLeast(1), fallbackXp = 0, eventId = runId)
            } else {
                GameRunResult(store.recordRun(gameId, frame.score, 0), 0, frame.score, false)
            }
            record = store.getHighScore(gameId)
        } finally { saving = false }
    }

    if (exitDialog) AlertDialog(
        onDismissRequest = { exitDialog = false },
        title = { Text("¿Abandonar partida?") },
        text = { Text("Esta partida no se guardará.") },
        confirmButton = { TextButton(onClick = onBack) { Text("Salir") } },
        dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Seguir jugando") } }
    )

    when (phase) {
        Phase.INTRO -> Column(
            Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TextButton(onClick = onBack) { Text("‹ Minijuegos") }
            Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = c.onSurface)
            Text(subtitle, color = c.onSurfaceVariant)
            scene(frame, Modifier.fillMaxWidth().height(270.dp), false) {}
            Text("Cómo jugar", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.onSurface)
            Text(rules, color = c.onSurfaceVariant, fontSize = 14.sp, lineHeight = 21.sp)
            Text("Récord · $record puntos", color = c.primary, fontWeight = FontWeight.Bold)
            Button(onClick = ::start, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Text("¡A jugar!", fontWeight = FontWeight.Bold)
            }
        }
        Phase.PLAY -> Column(
            Modifier.fillMaxSize().background(c.background).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = ::leave, contentPadding = PaddingValues(0.dp)) { Text("‹ Salir") }
                Spacer(Modifier.weight(1f))
                Text(title, color = c.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { paused = !paused }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (paused) "▶ Seguir" else "Ⅱ Pausa")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ArcadeMetric("PUNTOS", frame.score.toString(), Modifier.weight(1f))
                ArcadeMetric("TIEMPO", "${ceil((duration - frame.elapsed).coerceAtLeast(0f).toDouble()).toInt()}s", Modifier.weight(1f))
                ArcadeMetric("ESTADO", frame.health, Modifier.weight(1f))
            }
            LinearProgressIndicator(progress = { (frame.elapsed / duration).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                scene(frame, Modifier.fillMaxSize(), !paused, ::act)
                if (paused) Surface(color = Color(0xEE172033), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EN PAUSA", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { paused = false }) { Text("Continuar", color = Color(0xFFBFDBFE)) }
                    }
                }
            }
            Surface(color = c.surface, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(frame.detail, fontSize = 12.sp, color = c.primary, fontWeight = FontWeight.Bold)
                    Text(frame.message, fontSize = 12.sp, color = c.onSurface, minLines = 2)
                }
            }
            controls(frame, !paused, ::act)
        }
        Phase.RESULT -> Column(
            Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Text(if (frame.won) "🏆" else "🏁", fontSize = 54.sp)
            Text(if (frame.won) "¡Desafío superado!" else "Fin de partida", color = c.onSurface,
                fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("${frame.score}", color = c.primary, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
            Text("PUNTOS · ${frame.detail}", color = c.onSurfaceVariant, textAlign = TextAlign.Center)
            Text("Récord: $record", color = c.primary, fontWeight = FontWeight.Bold)
            if (reward?.isNewHighScore == true) Text("¡Nuevo récord!", color = c.primary, fontWeight = FontWeight.Bold)
            if (saving) CircularProgressIndicator(Modifier.size(24.dp))
            else reward?.let {
                Text(if (it.syncedWithBackend) "+${it.xpAwarded} XP" else "Récord local · XP no confirmado", color = c.primary)
            }
            Button(onClick = ::start, enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Jugar otra vez") }
            OutlinedButton(onClick = onBack, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Volver a minijuegos") }
        }
    }
}

@Composable
private fun ArcadeMetric(label: String, value: String, modifier: Modifier) {
    val c = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = c.surface) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = c.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 18.sp, color = c.onSurface, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}

@Composable
internal fun ArcadeControl(label: String, enabled: Boolean, modifier: Modifier, highlight: Boolean = false, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Button(onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (highlight) Color(0xFF166534) else c.primaryContainer,
            contentColor = if (highlight) Color.White else c.onPrimaryContainer),
        contentPadding = PaddingValues(5.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
