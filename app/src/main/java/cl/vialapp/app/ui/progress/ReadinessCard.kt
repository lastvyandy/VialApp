package cl.vialapp.app.ui.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.ExamReadinessResponse
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.theme.vialColor
import kotlinx.coroutines.CancellationException

@Composable
internal fun ReadinessCard(userKey: String, licenseClass: String?, onPractice: () -> Unit) {
    val context = LocalContext.current
    val manager = remember(context) { SessionManager(context) }
    var data by remember(userKey, licenseClass) { mutableStateOf<ExamReadinessResponse?>(null) }
    var loading by remember(userKey, licenseClass) { mutableStateOf(true) }
    var retry by remember(userKey, licenseClass) { mutableIntStateOf(0) }
    var expanded by remember(userKey, licenseClass) { mutableStateOf(false) }
    val ink = vialColor(Color(0xFF172033))
    val gray = vialColor(Color(0xFF64748B))
    val blue = vialColor(Color(0xFF2563EB))
    val green = vialColor(Color(0xFF14966F))
    val track = vialColor(Color(0xFFE2E8F0))

    LaunchedEffect(userKey, licenseClass, retry) {
        loading = true
        data = null
        try {
            val token = manager.getAccessToken()
            if (token != null) {
                val response = ApiClient.api.getExamReadiness("Bearer " + token, licenseClass)
                if (response.isSuccessful) data = response.body()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A server running an older API must not hide existing progress.
        } finally { loading = false }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = vialColor(Color.White))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("PREPARACIÓN PARA EL EXAMEN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = blue)
            val current = data
            when {
                loading -> Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = blue)
                }
                current == null -> {
                    Text("No pudimos calcular tu preparación.", color = ink, fontWeight = FontWeight.SemiBold)
                    Text("Tu progreso se mantiene guardado.", fontSize = 13.sp, color = gray)
                    OutlinedButton(onClick = { retry++ }) { Text("Reintentar") }
                }
                else -> {
                    val score = current.score
                    Text("Clase ${current.licenseClass}", color = gray, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (score ?: 0).coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxSize(), color = blue, trackColor = track,
                                strokeWidth = 10.dp
                            )
                            Text(score?.let { "$it%" } ?: "—", color = ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(when {
                                score == null -> "Construyendo tu perfil"
                                score >= 85 -> "Dominio avanzado"
                                score >= 60 -> "Vas avanzando"
                                else -> "Sigue reforzando"
                            }, fontWeight = FontWeight.Bold, color = ink, fontSize = 18.sp)
                            Text(when {
                                current.totalQuestions == 0 -> "Aún no hay preguntas para esta clase."
                                score == null -> "Responde ${(current.minimumQuestions - current.distinctAnswered).coerceAtLeast(0)} preguntas distintas más para ver tu estimación."
                                else -> "Tu índice combina aciertos y cobertura del banco."
                            }, fontSize = 13.sp, color = gray)
                        }
                    }
                    ReadinessMetric("Dominio de lo practicado", current.accuracy, green, track, ink, gray)
                    ReadinessMetric("Cobertura del banco", current.coverage, blue, track, ink, gray)
                    Text("${current.distinctAnswered} de ${current.totalQuestions} preguntas distintas respondidas",
                        fontSize = 12.sp, color = gray)
                    if (current.categories.isNotEmpty()) {
                        HorizontalDivider(color = track)
                        Text("Qué necesitas reforzar", color = ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val visible = if (expanded) current.categories else current.categories.take(3)
                        visible.forEach { category ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(category.category, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(if (category.distinctAnswered == 0) "Sin practicar todavía"
                                    else "Dominio: ${category.accuracy ?: 0}% · Cobertura: ${category.coverage}%",
                                    color = gray, fontSize = 12.sp)
                                LinearProgressIndicator(
                                    progress = { (category.score ?: 0).coerceIn(0, 100) / 100f },
                                    modifier = Modifier.fillMaxWidth().height(5.dp),
                                    color = blue, trackColor = track)
                            }
                        }
                        if (current.categories.size > 3) TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Mostrar menos" else "Ver todas las áreas")
                        }
                    }
                    Button(onClick = onPractice, modifier = Modifier.fillMaxWidth()) {
                        Text("Seguir practicando")
                    }
                    Text("Estimación de estudio de VialApp, no una probabilidad de aprobar el examen oficial.",
                        color = gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReadinessMetric(label: String, value: Int?, color: Color, track: Color, ink: Color, gray: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), color = ink, fontSize = 13.sp)
            Text(value?.let { "$it%" } ?: "Sin datos", color = gray, fontSize = 13.sp)
        }
        LinearProgressIndicator(
            progress = { (value ?: 0).coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth().height(7.dp), color = color, trackColor = track
        )
    }
}
