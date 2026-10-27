package cl.vialapp.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.AdvancedStatsResponse
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.ExamAttemptResponse
import cl.vialapp.app.data.api.UserResponse
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.gamification.HomeXpCard

private val HomeBlue = Color(0xFF2563EB)
private val HomeDark = Color(0xFF172033)
private val HomeGray = Color(0xFF64748B)
private val HomeGreen = Color(0xFF14966F)
private val HomeOrange = Color(0xFFF59E0B)
private val HomeRed = Color(0xFFD92D20)
private val HomeBackground = Color(0xFFF4F7FB)

@Composable
fun HomeScreen(
    user: UserResponse,
    onPractice: () -> Unit,
    onExam: () -> Unit,
    onProgress: () -> Unit,
    onGames: () -> Unit,
    onResumeExam: (Int) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val progressStore = remember { GameProgressStore(context) }

    var stats by remember { mutableStateOf<AdvancedStatsResponse?>(null) }
    var activeAttempt by remember { mutableStateOf<ExamAttemptResponse?>(null) }
    var dashboardLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        dashboardLoading = true
        val token = sessionManager.getAccessToken()

        if (token == null) {
            dashboardLoading = false
            return@LaunchedEffect
        }

        // Sincronizar XP y gamificación con el backend al cargar el Home
        try {
            progressStore.syncFromBackend(ApiClient.api, token)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Estadísticas avanzadas
        try {
            val statsResponse = ApiClient.api.getAdvancedStats(
                authorization = "Bearer $token"
            )
            if (statsResponse.isSuccessful) {
                stats = statsResponse.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Examen activo en curso
        try {
            val activeResponse = ApiClient.api.getActiveAttempt(
                authorization = "Bearer $token"
            )
            if (activeResponse.isSuccessful) {
                val body = activeResponse.body()
                activeAttempt = if (body?.status == "active") body else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dashboardLoading = false
    }

    val name = user.firstName?.takeIf { it.isNotBlank() } ?: "Conductor"
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "V"
    val licenseText = user.licenseClass?.let {
        if (it.startsWith("Clase", ignoreCase = true)) it else "Clase $it"
    } ?: "Licencia"

    val summary = stats?.summary
    val priorityCategory = stats?.categories?.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 18.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Cabecera
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hola, $name 👋",
                        color = HomeDark,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$licenseText · ${if (user.isPremium) "Premium" else "Cuenta gratuita"}",
                        color = HomeGray,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F0FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = HomeBlue,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tarjeta principal
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2563EB), Color(0xFF4F7DF3))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "TU PREPARACIÓN",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = licenseText,
                        color = Color.White,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = if (summary?.hasData == true) {
                            "Llevas ${summary.totalTests} exámenes completados."
                        } else {
                            "Empieza hoy y construye tu progreso paso a paso."
                        },
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onPractice,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = HomeBlue
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Continuar practicando",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Examen en curso
        activeAttempt?.let { attempt ->
            item {
                Card(
                    onClick = { onResumeExam(attempt.testId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF1FF))
                ) {
                    Row(
                        modifier = Modifier.padding(19.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            shape = RoundedCornerShape(15.dp),
                            color = Color.White
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "⏱", fontSize = 25.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Examen en curso",
                                color = HomeBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = attempt.test?.nombre ?: "Tienes un examen pendiente",
                                color = HomeDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Toca para continuar",
                                color = HomeGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }

                        Text(
                            text = "›",
                            color = HomeBlue,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            HomeXpCard(onProgress = onProgress)
        }

        // Tu ruta de hoy
        item {
            Text(
                text = "Tu ruta de hoy",
                color = HomeDark,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (dashboardLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = HomeBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Preparando tu recomendación...", color = HomeGray)
                        }
                    } else if (priorityCategory != null) {
                        Text(
                            text = "🎯 Prioridad recomendada",
                            color = HomeBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = priorityCategory.categoria,
                            color = HomeDark,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "${priorityCategory.successPercentage}% de acierto · " +
                                    "${priorityCategory.incorrectas} errores registrados",
                            color = HomeGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(13.dp))
                        LinearProgressIndicator(
                            progress = {
                                (priorityCategory.successPercentage / 100f).coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = scoreColor(priorityCategory.successPercentage),
                            trackColor = Color(0xFFE8EDF5)
                        )
                        Spacer(modifier = Modifier.height(17.dp))
                        Button(
                            onClick = onPractice,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HomeBlue)
                        ) {
                            Text(text = "Practicar ahora", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "🚦 Descubre tus puntos débiles",
                            color = HomeDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Completa un examen y VialApp podrá recomendarte qué temas reforzar.",
                            color = HomeGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onExam,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text(text = "Hacer un examen", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Resumen de progreso
        if (summary?.hasData == true) {
            item {
                Text(
                    text = "Tu progreso",
                    color = HomeDark,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    onClick = onProgress,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HomeMetric(
                                modifier = Modifier.weight(1f),
                                value = "${summary.averageScore}%",
                                label = "Promedio"
                            )
                            HomeMetric(
                                modifier = Modifier.weight(1f),
                                value = "${summary.totalTests}",
                                label = "Exámenes"
                            )
                            HomeMetric(
                                modifier = Modifier.weight(1f),
                                value = "${summary.totalCorrectAnswers}",
                                label = "Correctas"
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ver progreso completo",
                                modifier = Modifier.weight(1f),
                                color = HomeBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "›",
                                color = HomeBlue,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Acciones principales
        item {
            Text(
                text = "¿Qué quieres hacer?",
                color = HomeDark,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🎯",
                    title = "Practicar",
                    description = "Refuerza tus temas",
                    onClick = onPractice
                )
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🚗",
                    title = "Exámenes",
                    description = "Pon a prueba lo aprendido",
                    onClick = onExam
                )
            }
        }

        item {
            Card(
                onClick = onGames,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF172033))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "👨‍🦲💪", fontSize = 38.sp)
                    Spacer(modifier = Modifier.width(15.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Minijuegos",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Entrena, compite y supera tus récords.",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "JUGAR ›",
                        color = Color(0xFF93C5FD),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E6))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🚗💪", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(15.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Un poco cada día",
                            color = HomeDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Practicar con constancia te ayuda a detectar y reforzar tus errores antes del examen.",
                            color = HomeGray,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetric(modifier: Modifier, value: String, label: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF4F7FB)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = HomeDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = HomeGray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HomeActionCard(
    modifier: Modifier,
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = emoji, fontSize = 29.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = title, color = HomeDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = description, color = HomeGray, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

private fun scoreColor(score: Int): Color {
    return when {
        score >= 80 -> HomeGreen
        score >= 60 -> HomeOrange
        else -> HomeRed
    }
}