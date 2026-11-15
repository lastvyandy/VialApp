package cl.vialapp.app.ui.learning

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.*
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException

internal data class StudyStep(val id: String, val title: String, val blocks: List<com.google.gson.JsonElement>, val question: LearningQuestion)
internal data class StudyActivity(val title: String, val steps: List<StudyStep>, val review: Boolean)
private data class ChapterGame(val route: String, val title: String, val detail: String)

private fun chapterGames(id: String): List<ChapterGame> = when (id) {
    "siniestros-transito", "persona-transito", "conduccion-circunstancias-especiales" ->
        listOf(ChapterGame("games/arena", "Don Pelao Arena", "Reacciona ante situaciones de riesgo."))
    "principios-conduccion" -> listOf(ChapterGame("games/workshop", "Taller del Auto", "Repasa el mantenimiento y la inspección del vehículo."))
    "convivencia-vial", "usuarios-vulnerables" ->
        listOf(ChapterGame("games/crossroads", "Cruce Caótico", "Coordina el tráfico y el paso peatonal."))
    "normas-circulacion" -> listOf(
        ChapterGame("games/signs", "Caza la Señal", "Reconoce señales reglamentarias."),
        ChapterGame("games/carvier", "Carvier al volante", "Respeta PARE y zonas escolares."))
    else -> emptyList()
}

@Composable
fun LearningChapterScreen(userKey: String, chapterId: String, mascotId: String?, onBack: () -> Unit, onGame: (String) -> Unit) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val session = remember(context) { SessionManager(context) }
    val store = remember(context, userKey) { GameProgressStore(context, userKey) }
    var chapter by remember(userKey, chapterId) { mutableStateOf<LearningChapter?>(null) }
    var progress by remember(userKey, chapterId) { mutableStateOf<LearningProgress?>(null) }
    var loading by remember(userKey, chapterId) { mutableStateOf(true) }
    var error by remember(userKey, chapterId) { mutableStateOf("") }
    var refresh by remember(userKey, chapterId) { mutableIntStateOf(0) }
    var activity by remember(userKey, chapterId) { mutableStateOf<StudyActivity?>(null) }
    var flashcards by remember(userKey, chapterId) { mutableStateOf(false) }

    LaunchedEffect(userKey, chapterId, refresh) {
        loading = true
        error = ""
        try {
            val token = session.getAccessToken()
            if (token.isNullOrBlank()) error = "Inicia sesión para estudiar."
            else {
                val response = ApiClient.api.getLearningChapter("Bearer $token", chapterId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    chapter = body.chapter
                    progress = body.progress
                    store.applyRemoteProgress(body.gamification)
                } else error = "No se pudo abrir esta etapa."
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "No se pudo conectar con VialApp." }
        finally { loading = false }
    }

    val content = chapter
    val saved = progress
    val active = activity
    if (active != null && saved != null) {
        key(active) {
            LearningActivityScreen(userKey, chapterId, active, saved, mascotId,
                onUpdate = { progress = it },
                onBack = { activity = null })
        }
        return
    }
    if (flashcards && content != null) {
        LearningFlashcards(content.flashcards, onBack = { flashcards = false })
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(c.background),
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LearningHeader("Etapa ${content?.order ?: ""}", onBack) {
                TextButton(onClick = { refresh++ }, enabled = !loading) { Text("Actualizar") }
            }
        }
        if (loading && content == null) item {
            Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        if (error.isNotBlank()) item { LearningError(error) { refresh++ } }
        if (content != null && saved != null) {
            val allLessons = content.lessons.all { it.id in saved.completedLessons }
            item {
                Surface(color = c.primaryContainer, shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ETAPA ${content.order}", color = c.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(content.title, color = c.onPrimaryContainer, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${saved.completedLessons.size} de ${content.lessons.size} lecciones", color = c.onPrimaryContainer)
                        LinearProgressIndicator(progress = { saved.completedLessons.size.toFloat() / content.lessons.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth().height(7.dp))
                        Text(if (saved.completed) "Etapa completada · puedes repasar cuando quieras."
                            else "Lee, responde y completa el desafío final.", color = c.onPrimaryContainer, fontSize = 13.sp)
                    }
                }
            }
            item { Text("Lecciones", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c.onSurface) }
            itemsIndexed(content.lessons, key = { _, lesson -> lesson.id }) { i, lesson ->
                val done = lesson.id in saved.completedLessons
                val available = done || content.lessons.take(i).all { it.id in saved.completedLessons }
                val steps = lesson.concepts.map { StudyStep(it.id, it.title, it.blocks, it.question) }
                val answered = steps.count { it.id in saved.answers }
                LessonTile(i + 1, lesson.title, lesson.subtitle, answered, steps.size, done, available) {
                    activity = StudyActivity(lesson.title, steps, review = done)
                }
            }
            item {
                Card(onClick = {
                    val steps = content.quiz.mapIndexedNotNull { i, question ->
                        question.id?.let { StudyStep(it, "Pregunta ${i + 1}", emptyList(), question) }
                    }
                    if (steps.isNotEmpty()) activity = StudyActivity("Desafío final", steps, review = saved.quizCompleted)
                }, enabled = allLessons, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, c.outlineVariant), colors = CardDefaults.cardColors(containerColor = c.surface)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(painterResource(if (allLessons) R.drawable.ic_collection_medal else R.drawable.ic_award_lock),
                            null, tint = c.primary, modifier = Modifier.size(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Desafío final", color = c.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(if (saved.quizCompleted) "Primer intento: ${saved.score}/${content.quiz.size} correctas · Repasar"
                                else if (allLessons) "${content.quiz.size} preguntas · Continuar"
                                else "Completa todas las lecciones para abrirlo.", color = c.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (content.flashcards.isNotEmpty()) item {
                OutlinedButton(onClick = { flashcards = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp)) { Text("Repasar ${content.flashcards.size} tarjetas") }
            }
            val games = chapterGames(chapterId)
            if (games.isNotEmpty()) {
                item {
                    Text("Practica jugando", color = c.onSurface, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Opcional · no bloquea tu avance", color = c.onSurfaceVariant, fontSize = 12.sp)
                }
                itemsIndexed(games) { _, game ->
                    Card(onClick = { onGame(game.route) }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = c.surface),
                        border = BorderStroke(1.dp, c.outlineVariant)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(painterResource(R.drawable.ic_home_gamepad), null, tint = c.primary, modifier = Modifier.size(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(game.title, color = c.onSurface, fontWeight = FontWeight.Bold)
                                Text(game.detail, color = c.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Text("Jugar ›", color = c.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (content.source.isNotEmpty()) item {
                Surface(color = c.surface, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) { LearningBlocks(content.source) }
                }
            }
        }
    }
}

@Composable
private fun LessonTile(number: Int, title: String, subtitle: String, answered: Int, total: Int, done: Boolean, available: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Card(onClick = onClick, enabled = available, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = c.surface, disabledContainerColor = c.surface),
        border = BorderStroke(1.dp, if (available && !done) c.primary else c.outlineVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = if (done) Color(0xFF15803D) else if (available) c.primaryContainer else c.surfaceVariant,
                shape = RoundedCornerShape(13.dp), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (available) Text(if (done) "✓" else number.toString(),
                        color = if (done) Color.White else c.onPrimaryContainer, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    else Icon(painterResource(R.drawable.ic_award_lock), null, tint = c.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = c.onSurface, fontSize = 15.sp)
                Text(subtitle, color = c.onSurfaceVariant, fontSize = 12.sp)
                Text(if (done) "Repasar · $total/$total" else if (available) "$answered/$total actividades · Continuar ›" else "Bloqueada",
                    color = if (available) c.primary else c.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LearningFlashcards(cards: List<List<String>>, onBack: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val valid = remember(cards) { cards.filter { it.size >= 2 } }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().background(c.background),
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            LearningHeader("Tarjetas", onBack)
            Text("Repaso libre", color = c.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
            Text("Intenta recordar la respuesta antes de girar la tarjeta.", color = c.onSurfaceVariant, fontSize = 14.sp)
        }
        if (valid.isNotEmpty()) {
            item {
                Text("${index + 1}/${valid.size}", fontWeight = FontWeight.Bold, color = c.primary)
                Card(onClick = { revealed = !revealed }, modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
                    shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (revealed) c.primaryContainer else c.surface),
                    border = BorderStroke(1.dp, c.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(if (revealed) "RESPUESTA" else "PREGUNTA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.primary)
                        Text(valid[index][if (revealed) 1 else 0], fontSize = 20.sp, lineHeight = 29.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = if (revealed) c.onPrimaryContainer else c.onSurface)
                        Text("Toca para girar", color = c.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { index--; revealed = false }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("Anterior") }
                    Button(onClick = { index++; revealed = false }, enabled = index < valid.lastIndex, modifier = Modifier.weight(1f)) { Text("Siguiente") }
                }
            }
        } else item { Text("No hay tarjetas disponibles.", color = c.onSurfaceVariant) }
    }
}
