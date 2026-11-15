package cl.vialapp.app.ui.learning

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.*
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun LearningActivityScreen(
    userKey: String, chapterId: String, activity: StudyActivity, initialProgress: LearningProgress,
    mascotId: String?, onUpdate: (LearningProgress) -> Unit, onBack: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val session = remember(context) { SessionManager(context) }
    val store = remember(context, userKey) { GameProgressStore(context, userKey) }
    val scope = rememberCoroutineScope()
    val start = remember(activity) { resumeIndex(activity.steps.map { it.id }, initialProgress, activity.review) }
    var index by remember(activity) { mutableIntStateOf(start) }
    var choice by remember(activity, index) { mutableIntStateOf(-1) }
    var feedback by remember(activity, index) { mutableStateOf<LearningAnswerResponse?>(null) }
    var saving by remember(activity) { mutableStateOf(false) }
    var error by remember(activity, index) { mutableStateOf("") }
    var xp by remember(activity) { mutableIntStateOf(0) }
    var correct by remember(activity) { mutableIntStateOf(0) }
    var completed by remember(activity) { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val step = activity.steps.getOrNull(index)
    val list = rememberLazyListState()
    fun leave() {
        if (saving) return
        if (completed) onBack() else confirmExit = true
    }
    BackHandler { leave() }
    LaunchedEffect(index) { list.scrollToItem(0) }

    fun submit() {
        val current = step ?: return
        val selected = choice
        if (selected !in current.question.options.indices || saving || feedback != null) return
        saving = true
        error = ""
        scope.launch {
            try {
                val token = session.getAccessToken()
                if (token.isNullOrBlank()) error = "Inicia sesión para guardar tu respuesta."
                else {
                    val response = ApiClient.api.answerLearningStep("Bearer $token", chapterId, current.id, LearningAnswerRequest(selected))
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        store.applyRemoteProgress(body.gamification)
                        onUpdate(body.progress)
                        xp += body.xpAwarded
                        if (body.correct) correct++
                        feedback = body
                    } else error = if (response.code() == 409) "Completa primero las actividades anteriores. Vuelve a la etapa y actualiza."
                        else "No se pudo guardar. Reintenta para continuar."
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Error de conexión. Tu respuesta no se confirmó." }
            finally { saving = false }
        }
    }

    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false },
        title = { Text("¿Volver a la etapa?") },
        text = { Text("Las respuestas confirmadas quedan guardadas en tu cuenta.") },
        confirmButton = { TextButton(onClick = onBack) { Text("Salir") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Seguir") } })

    if (completed || step == null) {
        LazyColumn(Modifier.fillMaxSize().background(c.background),
            contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                LearningHeader("Actividad terminada", onBack)
                Box(Modifier.fillMaxWidth().background(c.primaryContainer, RoundedCornerShape(24.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                    Image(painterResource(if (mascotId == "mister_pelao") R.drawable.mascot_mister_pelao else R.drawable.mascot_carvier),
                        null, modifier = Modifier.height(155.dp).fillMaxWidth(), contentScale = ContentScale.Fit)
                }
                val chapterFinished = !initialProgress.completed && feedback?.progress?.completed == true && !activity.review
                if(chapterFinished) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=14.dp)){
                        Icon(painterResource(R.drawable.ic_collection_medal),null,tint=c.primary,modifier=Modifier.size(32.dp))
                        Text("¡Etapa completada!",color=c.primary,fontWeight=FontWeight.ExtraBold,fontSize=20.sp)
                    }
                }
                Text(if (activity.review) "¡Repaso completado!" else if(chapterFinished) "¡Un paso más hacia tu licencia!" else "¡Actividad completada!", color = c.onSurface,
                    fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, modifier = Modifier.padding(top = 15.dp))
                Text("$correct de ${activity.steps.size - start} respuestas correctas en esta sesión.", color = c.onSurfaceVariant, fontSize = 15.sp)
                if(chapterFinished) Text("Tu avance ya quedó guardado en el camino.",color=c.onSurfaceVariant,fontSize=13.sp)
                Text(if (xp > 0) "+$xp XP confirmados" else "Progreso sincronizado · sin XP nuevos", color = c.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Volver a la etapa", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().background(c.background), state = list,
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LearningHeader(if (activity.review) "Repaso" else "Aprender", ::leave)
            Text(activity.title, color = c.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(progress = { (index + if (feedback != null) 1 else 0).toFloat() / activity.steps.size },
                    modifier = Modifier.weight(1f).height(8.dp))
                Text("${index + 1}/${activity.steps.size}", color = c.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text(step.title, color = c.onSurface, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 6.dp))
        }
        if (step.blocks.isNotEmpty()) item {
            Surface(color = c.surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, c.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { LearningBlocks(step.blocks) }
            }
        }
        item {
            Text("Ponlo en práctica", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(step.question.prompt, color = c.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 27.sp)
        }
        items(step.question.options.size) { option ->
            val result = feedback
            val selected = choice == option
            val right = result != null && result.correctAnswer == option
            val wrong = result != null && selected && !result.correct
            val outline = when { right -> Color(0xFF16A34A); wrong -> c.error; selected -> c.primary; else -> c.outlineVariant }
            val bg = when { right -> Color(0xFF166534); wrong -> c.errorContainer; selected -> c.primaryContainer; else -> c.surface }
            val fg = when { right -> Color.White; wrong -> c.onErrorContainer; selected -> c.onPrimaryContainer; else -> c.onSurface }
            Card(onClick = { choice = option }, enabled = result == null && !saving,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(if (selected || right) 2.dp else 1.dp, outline),
                colors = CardDefaults.cardColors(containerColor = bg, disabledContainerColor = bg)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (right) "✓" else ('A' + option).toString(), color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(step.question.options[option], color = fg, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                }
            }
        }
        if (error.isNotBlank()) item { LearningError(error) { submit() } }
        item {
            val result = feedback
            if (result != null) {
                Surface(color = if (result.correct) c.primaryContainer else c.errorContainer, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val fg = if (result.correct) c.onPrimaryContainer else c.onErrorContainer
                        Text(if (result.correct) "¡Correcto!" else "Revisa la explicación", color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(result.explanation, color = fg, lineHeight = 21.sp, fontSize = 14.sp)
                        if (result.xpAwarded > 0) Text("+${result.xpAwarded} XP", color = fg, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    if (index == activity.steps.lastIndex) completed = true else index++
                }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (index == activity.steps.lastIndex) "Finalizar" else "Continuar", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = ::submit, enabled = choice >= 0 && !saving,
                    modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                    if (saving) CircularProgressIndicator(Modifier.size(22.dp), color = c.onPrimary, strokeWidth = 2.dp)
                    else Text("Comprobar respuesta", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
