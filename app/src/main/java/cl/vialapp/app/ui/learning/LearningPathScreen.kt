package cl.vialapp.app.ui.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.LearningChapterSummary
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException

@Composable
fun LearningPathEntry(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF243E69), contentColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = Color(0xFF416296), shape = RoundedCornerShape(16.dp)) {
                Icon(painterResource(R.drawable.ic_award_road), contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(32.dp), tint = Color(0xFFC7DBFF))
            }
            Column(Modifier.weight(1f)) {
                Text("Tu camino a la licencia", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Lecciones, desafíos y progreso por etapas", color = Color(0xFFDBEAFE), fontSize = 13.sp)
            }
            Text("›", color = Color(0xFFBFDBFE), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LearningPathScreen(
    userKey: String, mascotId: String?, licenseClass: String?,
    onBack: () -> Unit, onChapter: (String) -> Unit, onExam: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val session = remember(context) { SessionManager(context) }
    val store = remember(context, userKey) { GameProgressStore(context, userKey) }
    var chapters by remember(userKey) { mutableStateOf<List<LearningChapterSummary>>(emptyList()) }
    var loading by remember(userKey) { mutableStateOf(true) }
    var error by remember(userKey) { mutableStateOf("") }
    var refresh by remember(userKey) { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    LaunchedEffect(userKey, refresh) {
        loading = true
        error = ""
        try {
            val token = session.getAccessToken()
            if (token.isNullOrBlank()) {
                error = "Inicia sesión para continuar tu ruta."
            } else {
                val response = ApiClient.api.getLearningPath("Bearer $token")
                val body = response.body()
                if (!response.isSuccessful || body == null) error = "No se pudo consultar la ruta."
                else {
                    chapters = body.chapters.sortedBy { it.order }
                    store.applyRemoteProgress(body.gamification)
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "No se pudo conectar con VialApp." }
        finally { loading = false }
    }
    val next = nextChapterIndex(chapters)
    val total = chapters.sumOf { it.totalSteps }
    val answered = chapters.sumOf { it.answeredSteps }
    val completed = chapters.count { it.progress.completed }
    LazyColumn(Modifier.fillMaxSize().background(c.background), state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LearningHeader("Tu camino", onBack, trailing = {
                TextButton(onClick = { refresh++ }, enabled = !loading) { Text("Actualizar") }
            })
            Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF1D3972), Color(0xFF3468A6))), RoundedCornerShape(24.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("ACADEMIA VIAL · CLASE B", color = Color(0xFFBFDBFE), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Cada paso te acerca a la meta", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                        if (chapters.isNotEmpty()) Text("$completed de ${chapters.size} etapas completas", color = Color(0xFFDBEAFE), fontSize = 13.sp)
                    }
                    Image(painterResource(if (mascotId == "mister_pelao") R.drawable.mascot_mister_pelao else R.drawable.mascot_carvier),
                        contentDescription = null, modifier = Modifier.size(87.dp, 108.dp), contentScale = ContentScale.Fit)
                }
                if (total > 0) {
                    LinearProgressIndicator(progress = { answered.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().height(8.dp), color = Color(0xFF86EFAC), trackColor = Color(0xFF5575A0))
                    Text("$answered / $total actividades respondidas", color = Color(0xFFDBEAFE), fontSize = 12.sp)
                }
            }
        }
        if (!licenseClass.equals("B", ignoreCase = true)) item {
            Surface(color = c.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Text("Esta ruta se basa en el material de Clase B. Puede ayudarte a repasar temas comunes, pero no reemplaza la preparación específica de tu clase.",
                    Modifier.fillMaxWidth().padding(12.dp), color = c.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        if (loading && chapters.isEmpty()) item {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        if (error.isNotBlank()) item {
            LearningError(error) { refresh++ }
        }
        if (chapters.isNotEmpty()) {
            item {
                if (next < chapters.size) {
                    Button(onClick = { onChapter(chapters[next].id) }, modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp)) { Text("Continuar etapa ${chapters[next].order}", fontWeight = FontWeight.Bold) }
                } else {
                    Surface(color = c.primaryContainer, shape = RoundedCornerShape(14.dp)) {
                        Text("¡Ruta completa! Repasa los contenidos y pon a prueba tus conocimientos.",
                            Modifier.fillMaxWidth().padding(14.dp), color = c.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Etapas", color = c.onSurface, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 16.dp))
                Text("Completa las actividades de cada etapa para abrir la siguiente.", color = c.onSurfaceVariant, fontSize = 13.sp)
            }
            itemsIndexed(chapters, key = { _, item -> item.id }) { index, chapter ->
                PathChapterNode(chapter, index, chapterAvailable(chapters, index), index == next) { onChapter(chapter.id) }
            }
            item {
                Card(onClick = onExam, modifier = Modifier.fillMaxWidth(), enabled = completed == chapters.size,
                    shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, c.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = c.surface)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(painterResource(if (completed == chapters.size) R.drawable.ic_collection_medal else R.drawable.ic_award_lock),
                            contentDescription = null, tint = c.primary, modifier = Modifier.size(32.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Simulacro de examen", color = c.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(if (completed == chapters.size) "Pon a prueba todo lo aprendido." else "Disponible al completar la ruta. Los exámenes habituales siguen accesibles.",
                                color = c.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PathChapterNode(chapter: LearningChapterSummary, index: Int, available: Boolean, current: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val done = chapter.progress.completed
    val accent = when {
        done -> Color(0xFF15803D)
        current -> Color(0xFF2563EB)
        else -> c.surfaceVariant
    }
    val fraction = chapter.answeredSteps.toFloat() / chapter.totalSteps.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (index % 2 == 0) ChapterMarker(chapter.order, available, done, accent)
        Card(onClick = onClick, enabled = available, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
            border = BorderStroke(if (current) 2.dp else 1.dp, if (current) c.primary else c.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = c.surface, disabledContainerColor = c.surface)) {
            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(when { done -> "COMPLETADA"; current -> "SIGUIENTE ETAPA"; available -> "DISPONIBLE"; else -> "BLOQUEADA" },
                    color = if (done) c.primary else c.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text(chapter.title, color = c.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${chapter.progress.completedLessons.size}/${chapter.lessonCount} lecciones · ${chapter.quizCount} preguntas finales",
                    color = c.onSurfaceVariant, fontSize = 12.sp)
                LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = if (done) Color(0xFF15803D) else c.primary, trackColor = c.surfaceVariant)
                Text(if (done) "Repasar ›" else if (available) "Continuar ›" else "Completa la etapa anterior",
                    color = if (available) c.primary else c.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (index % 2 != 0) ChapterMarker(chapter.order, available, done, accent)
    }
}

@Composable
private fun ChapterMarker(order: Int, available: Boolean, done: Boolean, accent: Color) {
    val c = MaterialTheme.colorScheme
    Box(Modifier.width(60.dp).height(124.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawLine(c.outlineVariant, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height),
                strokeWidth = 5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
        }
        Surface(modifier = Modifier.size(56.dp), color = accent, shape = RoundedCornerShape(22.dp),
            border = BorderStroke(2.dp, if (available) accent else c.outlineVariant), shadowElevation = if (available) 4.dp else 0.dp) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    done -> Text("✓", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    available -> Text(order.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    else -> Icon(painterResource(R.drawable.ic_award_lock), null, tint = c.onSurfaceVariant, modifier = Modifier.size(25.dp))
                }
            }
        }
    }
}

@Composable
internal fun LearningHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹ Volver") }
        Text(title, color = c.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 2)
        trailing()
    }
}

@Composable
internal fun LearningError(message: String, retry: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.errorContainer, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(message, color = c.onErrorContainer, fontSize = 14.sp)
            TextButton(onClick = retry) { Text("Reintentar") }
        }
    }
}
