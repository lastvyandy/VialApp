package cl.vialapp.app.ui.practice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.*
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.ui.rewards.LocalRewards
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.theme.vialColor
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AdaptivePracticeEntry(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = vialColor(Color(0xFFE8F2FF)))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ENTRENAMIENTO PERSONALIZADO", color = vialColor(Color(0xFF2563EB)),
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Tu práctica inteligente", color = vialColor(Color(0xFF172033)),
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Refuerza tus errores y descubre nuevas preguntas. Hasta 10 por sesión.",
                color = vialColor(Color(0xFF64748B)), fontSize = 14.sp)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("Comenzar práctica")
            }
            Text("Disponible con Premium de tu clase", fontSize = 12.sp,
                color = vialColor(Color(0xFF64748B)))
        }
    }
}

@Composable
fun AdaptivePracticeScreen(userKey: String, licenseClass: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val rewards = LocalRewards.current
    val store = remember(context,userKey){GameProgressStore(context,userKey)}
    val manager = remember(context) { SessionManager(context) }
    val scope = rememberCoroutineScope()
    var session by remember(userKey, licenseClass) { mutableStateOf<AdaptivePracticeSession?>(null) }
    var result by remember(userKey, licenseClass) { mutableStateOf<AdaptivePracticeResult?>(null) }
    var answers by remember(userKey, licenseClass) { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    var index by remember(userKey, licenseClass) { mutableIntStateOf(0) }
    var busy by remember(userKey, licenseClass) { mutableStateOf(true) }
    var error by remember(userKey, licenseClass) { mutableStateOf("") }
    var exitDialog by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    val blue = vialColor(Color(0xFF2563EB))
    val ink = vialColor(Color(0xFF172033))
    val gray = vialColor(Color(0xFF64748B))
    val green = vialColor(Color(0xFF14966F))
    val red = vialColor(Color(0xFFDC2626))
    val surface = vialColor(Color.White)
    val bg = vialColor(Color(0xFFF4F7FB))

    suspend fun start() {
        busy = true
        error = ""
        locked = false
        try {
            val token = manager.getAccessToken() ?: run {
                error = "No encontramos tu sesión. Vuelve a iniciar sesión."
                return
            }
            val response = ApiClient.api.startAdaptivePractice("Bearer " + token, licenseClass)
            val body = response.body()
            if (response.isSuccessful && body != null && body.questions.isNotEmpty()) {
                session = body
                result = null
                answers = emptyMap()
                index = 0
            } else {
                locked = response.code() == 403
                error = when (response.code()) {
                    403 -> "Necesitas Premium de tu clase para acceder a la práctica inteligente."
                    404 -> "No hay preguntas disponibles para esta clase o el servidor aún no está actualizado."
                    else -> "No pudimos comenzar la práctica. Inténtalo nuevamente."
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = "No pudimos conectar con VialApp."
        } finally { busy = false }
    }

    suspend fun submit(current: AdaptivePracticeSession) {
        if (busy || current.questions.any { answers[it.id].isNullOrEmpty() }) return
        busy = true
        error = ""
        try {
            val token = manager.getAccessToken() ?: run {
                error = "No encontramos tu sesión."
                return
            }
            val response = ApiClient.api.submitAdaptivePractice(
                "Bearer " + token,
                AdaptivePracticeSubmission(current.attemptToken,
                    current.questions.map { ExamAnswer(it.id, answers[it.id].orEmpty()) })
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                store.applyRemoteProgress(body.gamification)
                if(!body.duplicate)rewards.xp("Práctica completada",body.gamification.totalXp,body.xpAwarded)
                result = body
            } else {
                locked = response.code() in listOf(400, 403, 409)
                error = when (response.code()) {
                    400, 409 -> "La práctica venció o ya no está disponible. Comienza otra."
                    403 -> "Tu acceso Premium ya no está disponible."
                    else -> "No pudimos guardar tus respuestas. Puedes reintentar."
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = "Se perdió la conexión. Tus respuestas siguen aquí; reintenta el envío."
        } finally { busy = false }
    }

    LaunchedEffect(userKey, licenseClass) { start() }
    val leave: () -> Unit = {
        if (!busy) {
            if (result == null && answers.isNotEmpty()) exitDialog = true else onBack()
        }
    }
    BackHandler { leave() }
    if (exitDialog) {
        AlertDialog(onDismissRequest = { exitDialog = false },
            title = { Text("¿Salir de la práctica?") },
            text = { Text("Las respuestas que aún no has enviado se perderán.") },
            confirmButton = { TextButton(onClick = { exitDialog = false; onBack() }) { Text("Salir") } },
            dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("Continuar") } })
    }

    Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = leave, enabled = !busy) { Text("Volver") }
            Spacer(Modifier.weight(1f))
            Text("Práctica inteligente", color = ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
        }
        val completed = result
        val current = session
        when {
            completed != null -> {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = surface),
                            shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("¡Práctica completada!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ink)
                                Text("${completed.correctas} / ${completed.totalQuestions} correctas",
                                    fontSize = 29.sp, fontWeight = FontWeight.Bold, color = blue)
                                LinearProgressIndicator(
                                    progress = { completed.correctas.toFloat() / completed.totalQuestions.coerceAtLeast(1) },
                                    modifier = Modifier.fillMaxWidth(), color = blue)
                                Text(if (!completed.duplicate && completed.xpAwarded > 0) "+${completed.xpAwarded} XP ganados"
                                    else "Tus respuestas ya forman parte de tu progreso.", color = green)
                                Text("Revisa las respuestas y aprende de tus errores.", color = gray)
                            }
                        }
                    }
                    items(completed.resultados.sortedBy { it.isCorrect }, key = { it.questionId }) { item ->
                        val q = current?.questions?.firstOrNull { it.id == item.questionId }
                        Card(colors = CardDefaults.cardColors(containerColor = surface), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (item.isCorrect) "Correcta" else "Por reforzar", fontWeight = FontWeight.Bold,
                                    color = if (item.isCorrect) green else red)
                                Text(q?.texto.orEmpty(), fontWeight = FontWeight.SemiBold, color = ink)
                                Text("Tu respuesta: " + item.selectedAnswers.joinToString(" · ") { q?.alternativas?.get(it) ?: it }, color = gray)
                                if (!item.isCorrect) Text("Respuesta correcta: " + item.correctAnswers.joinToString(" · ") { q?.alternativas?.get(it) ?: it }, color = green)
                                if (item.explicacion.isNotBlank()) Text(item.explicacion, color = ink)
                            }
                        }
                    }
                    item {
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver a practicar") }
                    }
                }
            }
            current == null -> {
                Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (busy) CircularProgressIndicator(color = blue)
                    else {
                        Text(error, color = ink, fontSize = 16.sp)
                        Spacer(Modifier.height(16.dp))
                        if (!locked) Button(onClick = { scope.launch { start() } }) { Text("Reintentar") }
                        else OutlinedButton(onClick = onBack) { Text("Volver") }
                    }
                }
            }
            else -> {
                val q = current.questions[index]
                val selected = answers[q.id].orEmpty()
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pregunta ${index + 1} de ${current.questions.size}", color = gray, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(progress = { answers.size.toFloat() / current.questions.size },
                        modifier = Modifier.fillMaxWidth(), color = blue)
                }
                key(q.id) {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)) {
                        item {
                            Text(q.categoria, color = blue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(q.texto, color = ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        if (!q.imageUrl.isNullOrBlank()) item {
                            AsyncImage(model = q.imageUrl, contentDescription = "Imagen de la pregunta",
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 270.dp),
                                contentScale = ContentScale.Fit)
                        }
                        if (q.isMultiple) item { Text("Selecciona todas las alternativas correctas.", color = gray) }
                        items(q.alternativas.entries.sortedBy { it.key }, key = { it.key }) { (id, text) ->
                            val checked = id in selected
                            val shape = RoundedCornerShape(16.dp)
                            Row(Modifier.fillMaxWidth().background(if (checked) vialColor(Color(0xFFE8F2FF)) else surface, shape)
                                .border(1.dp, if (checked) blue else gray.copy(alpha = .2f), shape)
                                .clickable(enabled = !busy && !locked) {
                                    val next = if (q.isMultiple) {
                                        if (checked) selected - id else selected + id
                                    } else listOf(id)
                                    answers = if (next.isEmpty()) answers - q.id else answers + (q.id to next)
                                    error = ""
                                }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(id.uppercase(), color = blue, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(12.dp))
                                Text(text, color = ink, modifier = Modifier.weight(1f))
                                if (checked) Text("✓", color = blue, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (error.isNotBlank()) item { Text(error, color = red) }
                    }
                }
                if (locked) Button(onClick = { session = null; scope.launch { start() } },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text("Nueva práctica") }
                else Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (index > 0) OutlinedButton(onClick = { index-- }, enabled = !busy) { Text("Anterior") }
                    Button(
                        onClick = { if (index < current.questions.lastIndex) index++ else scope.launch { submit(current) } },
                        enabled = !busy && selected.isNotEmpty() &&
                            (index < current.questions.lastIndex || answers.size == current.questions.size),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(if (index == current.questions.lastIndex) "Finalizar práctica" else "Siguiente")
                    }
                }
            }
        }
    }
}
