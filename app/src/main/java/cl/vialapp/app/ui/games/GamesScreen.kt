package cl.vialapp.app.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import cl.vialapp.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.AssignedTest
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.theme.vialColor
import cl.vialapp.app.ui.career.CareerShortcuts

private val GamesBlue: Color
    @Composable get() = vialColor(Color(0xFF2563EB))
private val GamesDark: Color
    @Composable get() = vialColor(Color(0xFF172033))
private val GamesGray: Color
    @Composable get() = vialColor(Color(0xFF64748B))
private val GamesGreen: Color
    @Composable get() = vialColor(Color(0xFF14966F))
private val GamesOrange: Color
    @Composable get() = vialColor(Color(0xFFF59E0B))
private val GamesBackground: Color
    @Composable get() = vialColor(Color(0xFFF4F7FB))

@Composable
fun GamesScreen(
    licenseClass: String?,
    userKey: String,
    onOpenBaldChallenge: (Int) -> Unit,
    onOpenCrossroadsChaos: () -> Unit,
    onOpenCarvier: () -> Unit,
    onOpenArena: () -> Unit,
    onOpenWorkshop: () -> Unit,
    onOpenSigns: () -> Unit,
    onBack: () -> Unit,
    onCareer: () -> Unit = {},
    onGarage: () -> Unit = {}
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val progressStore = remember(context, userKey) {
        GameProgressStore(
            context = context,
            userKey = userKey
        )
    }

    val gameProgress = progressStore.snapshot()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var sourceTest by remember { mutableStateOf<AssignedTest?>(null) }

    LaunchedEffect(licenseClass) {
        loading = true
        error = ""

        try {
            val token = sessionManager.getAccessToken()
            if (token == null) {
                error = "No se encontró una sesión válida."
                return@LaunchedEffect
            }

            // Sincronizar XP y progreso con el servidor al entrar a juegos
            try {
                progressStore.syncFromBackend(ApiClient.api, token)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val response = ApiClient.api.getTests(
                authorization = "Bearer $token",
                licenseClass = licenseClass,
                category = null
            )

            if (response.isSuccessful) {
                val tests = response.body()?.tests ?: emptyList()
                sourceTest = tests.firstOrNull { !it.isLocked }
            } else {
                error = "No pudimos preparar los minijuegos."
            }

        } catch (e: Exception) {
            error = "No se pudo conectar con VialApp."
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GamesBackground),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 22.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "‹ Volver",
                    color = GamesBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Minijuegos",
                color = GamesDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Entrena rápido, supera tus récords y aprende sin sentir que estás haciendo otro examen.",
                color = GamesGray,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }

        item { CareerShortcuts(onCareer, onGarage) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = vialColor(Color(0xFFEAF1FF)))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrainingIcon(R.drawable.ic_home_gamepad)
                        TrainingIcon(R.drawable.ic_award_target)
                        TrainingIcon(R.drawable.ic_award_wheel)
                    }
                    Column {
                        Text(
                            text = "Zona de entrenamiento",
                            color = GamesDark,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Los XP se sincronizan con tu cuenta y los récords personales se guardan en este dispositivo.",
                            color = GamesGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = vialColor(Color.White))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(painterResource(R.drawable.ic_home_gamepad), contentDescription = null,
                                    tint = GamesBlue, modifier = Modifier.size(21.dp))
                                Text("Nivel ${gameProgress.level}", color = GamesDark,
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "${gameProgress.totalXp} XP · ${gameProgress.totalPoints} puntos acumulados",
                                color = GamesGray,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "${gameProgress.totalRuns} partidas",
                            color = GamesBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = {
                            (gameProgress.levelXp.toFloat() / gameProgress.xpPerLevel.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = GamesBlue,
                        trackColor = vialColor(Color(0xFFE5EAF2))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${gameProgress.levelXp}/${gameProgress.xpPerLevel} XP para el siguiente nivel",
                        color = GamesGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            PlayableGameCard(
                gameId = GameProgressStore.CARVIER_DRIVE,
                record = progressStore.getHighScore(GameProgressStore.CARVIER_DRIVE),
                title = "Carvier al volante",
                subtitle = "Conducción arcade · 3 carriles",
                description = "Esquiva el tráfico, recoge monedas y reduce la velocidad en zonas escolares.",
                accent = GamesBlue,
                enabled = true,
                onClick = onOpenCarvier
            )
        }

        item {
            PlayableGameCard(
                gameId = GameProgressStore.PELAO_ARENA,
                record = progressStore.getHighScore(GameProgressStore.PELAO_ARENA),
                title = "Don Pelao Arena",
                subtitle = "Combate · esquivas · especial · 3 rivales",
                description = "Rompe la defensa con decisiones seguras, esquiva contraataques y carga Pelao Power.",
                accent = GamesGreen,
                enabled = true,
                onClick = onOpenArena
            )
        }

        item {
            PlayableGameCard(
                gameId = GameProgressStore.AUTO_WORKSHOP,
                record = progressStore.getHighScore(GameProgressStore.AUTO_WORKSHOP),
                title = "Taller del Auto",
                subtitle = "Diagnóstico interactivo · 4 fallas",
                description = "Cuatro órdenes: identifica la pieza, utiliza la herramienta y toma una decisión segura.",
                accent = GamesOrange,
                enabled = true,
                onClick = onOpenWorkshop
            )
        }

        item {
            PlayableGameCard(
                gameId = GameProgressStore.BALD_CHALLENGE,
                record = progressStore.getHighScore(GameProgressStore.BALD_CHALLENGE),
                title = "Reto del Pelao · clásico",
                subtitle = "5 rondas · 3 vidas · Pelao Power",
                description = "Responde rápido, arma combos y llega al BOSS con el modo x2 cargado.",
                accent = GamesGreen,
                enabled = sourceTest != null && !loading,
                onClick = {
                    sourceTest?.id?.let { onOpenBaldChallenge(it) }
                }
            )
        }

        item {
            PlayableGameCard(
                gameId = GameProgressStore.CROSSROADS_ARCADE,
                record = progressStore.getHighScore(GameProgressStore.CROSSROADS_ARCADE),
                title = "Cruce Caótico",
                subtitle = "Arcade en tiempo real · 75 s",
                description = "Controla los semáforos de una ciudad en movimiento, evita la congestión y da prioridad a las ambulancias.",
                accent = GamesOrange,
                enabled = true,
                onClick = {
                    onOpenCrossroadsChaos()
                }
            )
        }

        item {
            PlayableGameCard(
                gameId = GameProgressStore.SIGN_HUNT_ARCADE,
                record = progressStore.getHighScore(GameProgressStore.SIGN_HUNT_ARCADE),
                title = "Caza la Señal",
                subtitle = "Supervivencia · dificultad progresiva",
                description = "Atrapa señales chilenas en oleadas infinitas. Mantén tus vidas y supera tu récord.",
                accent = GamesBlue,
                enabled = true,
                onClick = onOpenSigns
            )
        }

        if (loading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = GamesBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Preparando juegos...", color = GamesGray)
                }
            }
        } else if (sourceTest == null) {
            item {
                Text(
                    text = if (error.isNotBlank()) error else "El reto clásico requiere un examen accesible. Los demás juegos están disponibles.",
                    color = GamesGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TrainingIcon(drawable: Int) {
    val c = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(13.dp), color = c.surface) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(drawable), contentDescription = null,
                tint = c.primary, modifier = Modifier.size(25.dp))
        }
    }
}

@Composable
private fun PlayableGameCard(
    gameId: String,
    record: Int,
    title: String,
    subtitle: String,
    description: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val c = MaterialTheme.colorScheme
    Card(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, c.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = c.surface, disabledContainerColor = c.surface)
    ) {
        GameCover(gameId, Modifier.fillMaxWidth().height(140.dp))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(subtitle.uppercase(), color = accent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(title, color = c.onSurface, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(description, color = c.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp)
            HorizontalDivider(color = c.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (record > 0) "Récord: $record" else "Sin récord aún", color = c.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(12.dp), color = if (enabled) c.primaryContainer else c.surfaceVariant) {
                    Text(if (enabled) "JUGAR  ›" else "NO DISPONIBLE", Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        color = if (enabled) c.onPrimaryContainer else c.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
