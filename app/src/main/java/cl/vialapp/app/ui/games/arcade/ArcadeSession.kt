package cl.vialapp.app.ui.games.arcade

import cl.vialapp.app.ui.rewards.LocalRewards
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import kotlinx.coroutines.CancellationException

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
    controls: @Composable (S, Boolean, (Int) -> Unit) -> Unit,
    resultArt: (@Composable (S, Modifier) -> Unit)? = null,
    showPlayMessages: Boolean = true
) {
    val context = LocalContext.current
    val store = remember(context, userKey) { GameProgressStore(context, userKey) }
    val session = remember(context) { SessionManager(context) }
    var engine by remember(userKey, gameId) { mutableStateOf(create()) }
    var frame by remember(userKey, gameId) { mutableStateOf(engine.snapshot()) }
    var phase by remember(userKey, gameId) { mutableStateOf(Phase.INTRO) }
    var paused by remember { mutableStateOf(false) }
    var exitDialog by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    var runId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var reward by remember { mutableStateOf<GameRunResult?>(null) }
    var saving by remember { mutableStateOf(false) }
    var record by remember(userKey, gameId) { mutableIntStateOf(store.getHighScore(gameId)) }
    val c = MaterialTheme.colorScheme
    val rewards=LocalRewards.current
    val haptic = LocalHapticFeedback.current

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
            val previous = frame
            frame = engine.act(action)
            if (frame.health != previous.health || frame.score > previous.score) {
                haptic.performHapticFeedback(if (frame.score > previous.score) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
            }
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
                GameRunResult(store.recordLocalHighScore(gameId, frame.score), 0, 0, false)
            }
            reward?.takeIf{it.syncedWithBackend}?.let{rewards.show("Partida guardada",it.coinsAwarded,it.xpAwarded)}
            record = store.getHighScore(gameId)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            reward=GameRunResult(store.recordLocalHighScore(gameId,frame.score),0,0,false)
            record=store.getHighScore(gameId)
        } finally { saving = false }
    }

    if (help) AlertDialog(
        onDismissRequest = { help = false; paused = false },
        title = { Text("Cómo jugar") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(rules)
            Text(frame.lesson, color = c.onSurfaceVariant)
        } },
        confirmButton = { TextButton(onClick = { help = false; paused = false }) { Text("Continuar") } }
    )
    if (exitDialog) AlertDialog(
        onDismissRequest = { exitDialog = false; paused = false },
        title = { Text("¿Abandonar partida?") },
        text = { Text("Esta partida no se guardará.") },
        confirmButton = { TextButton(onClick = onBack) { Text("Salir") } },
        dismissButton = { TextButton(onClick = { exitDialog = false; paused = false }) { Text("Seguir jugando") } }
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
            Surface(color = c.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Qué vas a aprender", fontWeight = FontWeight.Bold, color = c.onPrimaryContainer)
                    Text(frame.lesson, color = c.onPrimaryContainer, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (paused) "Partida pausada" else "Toma decisiones seguras", fontSize = 12.sp, color = c.onSurfaceVariant)
                TextButton(onClick = { paused = true; help = true }, contentPadding = PaddingValues(0.dp)) { Text("? Cómo jugar") }
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
            if (showPlayMessages) Surface(color = c.surface, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(frame.detail, fontSize = 12.sp, color = c.primary, fontWeight = FontWeight.Bold)
                    Text(frame.message, fontSize = 13.sp, color = c.onSurface, lineHeight = 18.sp)
                }
            }
            controls(frame, !paused, ::act)
        }
        Phase.RESULT -> Column(
            Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            if (resultArt != null) {
                resultArt(frame, Modifier.fillMaxWidth().height(230.dp))
            } else {
                Text(if (frame.won) "🏆" else "🏁", fontSize = 54.sp)
            }
            Text(if (frame.won) "¡Desafío superado!" else "Fin de partida", color = c.onSurface,
                fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text("${frame.score}", color = c.primary, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
            Text("PUNTOS · ${frame.detail}", color = c.onSurfaceVariant, textAlign = TextAlign.Center)
            Text("Récord: $record", color = c.primary, fontWeight = FontWeight.Bold)
            Surface(color = c.primaryContainer, shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Lo que aprendiste", fontWeight = FontWeight.Bold, color = c.onPrimaryContainer)
                    Text(frame.lesson, color = c.onPrimaryContainer, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
            if (reward?.isNewHighScore == true) Text("¡Nuevo récord!", color = c.primary, fontWeight = FontWeight.Bold)
            if (saving) CircularProgressIndicator(Modifier.size(24.dp))
            else reward?.let {
                Text(if (it.syncedWithBackend) "+${it.xpAwarded} XP"+(if(it.coinsAwarded>0)" · +${it.coinsAwarded} monedas" else "") else "Récord local · XP no confirmado", color = c.primary)
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
