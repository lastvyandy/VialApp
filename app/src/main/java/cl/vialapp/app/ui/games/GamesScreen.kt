package cl.vialapp.app.ui.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import cl.vialapp.app.data.api.AssignedTest
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager


private val GamesBlue = Color(0xFF2563EB)
private val GamesDark = Color(0xFF172033)
private val GamesGray = Color(0xFF64748B)
private val GamesGreen = Color(0xFF14966F)
private val GamesOrange = Color(0xFFF59E0B)
private val GamesBackground = Color(0xFFF4F7FB)


@Composable
fun GamesScreen(
    licenseClass: String?,
    onOpenBaldChallenge: (Int) -> Unit,
    onOpenCrossroadsChaos: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context =
        LocalContext.current

    val sessionManager =
        remember {
            SessionManager(context)
        }

    val progressStore =
        remember {
            GameProgressStore(context)
        }

    /*
     * Se vuelve a leer durante la composición.
     * Al regresar desde un minijuego, Navigation
     * recompone esta pantalla y muestra el progreso
     * acumulado actualizado.
     */
    val gameProgress =
        progressStore.snapshot()

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    var sourceTest by remember {
        mutableStateOf<AssignedTest?>(
            null
        )
    }


    /*
     * Elegimos un examen accesible solamente como
     * banco de preguntas para los minijuegos.
     *
     * No iniciamos ningún ExamAttempt.
     */
    LaunchedEffect(
        licenseClass
    ) {
        loading = true
        error = ""

        try {
            val token =
                sessionManager
                    .getAccessToken()

            if (token == null) {
                error =
                    "No se encontró una sesión válida."

                return@LaunchedEffect
            }

            val response =
                ApiClient.api.getTests(
                    authorization =
                        "Bearer $token",
                    licenseClass =
                        licenseClass,
                    category =
                        null
                )

            if (response.isSuccessful) {
                val tests =
                    response
                        .body()
                        ?.tests
                        ?: emptyList()

                sourceTest =
                    tests.firstOrNull {
                        !it.isLocked
                    }

            } else {
                error =
                    "No pudimos preparar los minijuegos."
            }

        } catch (e: Exception) {
            error =
                "No se pudo conectar con VialApp."

            e.printStackTrace()

        } finally {
            loading = false
        }
    }


    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    GamesBackground
                ),
        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 22.dp,
                bottom = 110.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                16.dp
            )
    ) {

        item {
            TextButton(
                onClick =
                    onBack,
                contentPadding =
                    PaddingValues(0.dp)
            ) {
                Text(
                    text =
                        "‹ Volver",
                    color =
                        GamesBlue,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "Minijuegos",
                color =
                    GamesDark,
                fontSize =
                    30.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text =
                    "Entrena rápido, supera tus récords y aprende sin sentir que estás haciendo otro examen.",
                color =
                    GamesGray,
                fontSize =
                    15.sp,
                lineHeight =
                    21.sp
            )
        }


        item {
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(26.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFEAF1FF)
                    )
            ) {
                Row(
                    modifier =
                        Modifier.padding(20.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            "👨‍🦲💪🚗",
                        fontSize =
                            42.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(14.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                "Zona de entrenamiento",
                            color =
                                GamesDark,
                            fontSize =
                                19.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Los puntos y récords son del juego: no alteran tus resultados oficiales.",
                            color =
                                GamesGray,
                            fontSize =
                                13.sp,
                            lineHeight =
                                18.sp
                        )
                    }
                }
            }
        }


        item {
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    )
            ) {
                Column(
                    modifier =
                        Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                text =
                                    "🎮 Nivel ${gameProgress.level}",
                                color =
                                    GamesDark,
                                fontSize =
                                    18.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "${gameProgress.totalXp} XP · ${gameProgress.totalPoints} puntos acumulados",
                                color =
                                    GamesGray,
                                fontSize =
                                    12.sp
                            )
                        }

                        Text(
                            text =
                                "${gameProgress.totalRuns} partidas",
                            color =
                                GamesBlue,
                            fontSize =
                                12.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    LinearProgressIndicator(
                        progress = {
                            (
                                    gameProgress.levelXp
                                        .toFloat() /
                                            gameProgress.xpPerLevel
                                                .toFloat()
                                    )
                                .coerceIn(
                                    0f,
                                    1f
                                )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        color =
                            GamesBlue,
                        trackColor =
                            Color(0xFFE5EAF2)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "${gameProgress.levelXp}/${gameProgress.xpPerLevel} XP para el siguiente nivel",
                        color =
                            GamesGray,
                        fontSize =
                            11.sp
                    )
                }
            }
        }


        item {
            PlayableGameCard(
                emoji =
                    "👨‍🦲💪",
                title =
                    "Reto del Pelao",
                subtitle =
                    "5 rondas · 3 vidas · Pelao Power",
                description =
                    "Responde rápido, arma combos y llega al BOSS con el modo x2 cargado.",
                accent =
                    GamesGreen,
                enabled =
                    sourceTest != null &&
                            !loading,
                onClick = {
                    sourceTest
                        ?.id
                        ?.let {
                            onOpenBaldChallenge(
                                it
                            )
                        }
                }
            )
        }


        item {
            PlayableGameCard(
                emoji =
                    "🚦💥",
                title =
                    "Cruce Caótico",
                subtitle =
                    "Prioridades · combos · BOSS",
                description =
                    "Toca quién tiene prioridad antes de que el cruce se convierta en un desastre.",
                accent =
                    GamesOrange,
                enabled =
                    true,
                onClick = {
                    onOpenCrossroadsChaos()
                }
            )
        }


        item {
            PausedGameCard(
                emoji =
                    "🛑👀",
                title =
                    "Caza la Señal",
                description =
                    "En pausa mientras preparamos un catálogo visual de señaléticas realmente correcto."
            )
        }


        item {
            ComingSoonGameCard(
                emoji =
                    "🔧🚗",
                title =
                    "Taller del Auto",
                description =
                    "Diagnostica problemas y demuestra cuánto sabes de mantenimiento."
            )
        }


        if (loading) {
            item {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.Center,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(20.dp),
                        strokeWidth =
                            2.dp,
                        color =
                            GamesBlue
                    )

                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )

                    Text(
                        text =
                            "Preparando juegos...",
                        color =
                            GamesGray
                    )
                }
            }

        } else if (
            sourceTest == null
        ) {
            item {
                Text(
                    text =
                        if (error.isNotBlank()) {
                            error
                        } else {
                            "Reto del Pelao necesita un examen accesible. Cruce Caótico funciona igualmente."
                        },
                    color =
                        GamesGray,
                    fontSize =
                        13.sp
                )
            }
        }
    }
}


@Composable
private fun PlayableGameCard(
    emoji: String,
    title: String,
    subtitle: String,
    description: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick =
            onClick,
        enabled =
            enabled,
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(26.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White,
                disabledContainerColor =
                    Color.White
            )
    ) {
        Column(
            modifier =
                Modifier.padding(21.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Surface(
                    modifier =
                        Modifier.size(66.dp),
                    shape =
                        RoundedCornerShape(20.dp),
                    color =
                        accent.copy(
                            alpha = 0.12f
                        )
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                emoji,
                            fontSize =
                                31.sp
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(15.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            title,
                        color =
                            GamesDark,
                        fontSize =
                            20.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            subtitle,
                        color =
                            GamesGray,
                        fontSize =
                            12.sp
                    )
                }

                Surface(
                    shape =
                        RoundedCornerShape(30.dp),
                    color =
                        accent.copy(
                            alpha = 0.12f
                        )
                ) {
                    Text(
                        text =
                            if (enabled) {
                                "JUGAR"
                            } else {
                                "CARGANDO"
                            },
                        modifier =
                            Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 6.dp
                            ),
                        color =
                            accent,
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    description,
                color =
                    GamesGray,
                fontSize =
                    14.sp,
                lineHeight =
                    20.sp
            )
        }
    }
}


@Composable
private fun PausedGameCard(
    emoji: String,
    title: String,
    description: String
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFFFFBEB)
            )
    ) {
        Row(
            modifier =
                Modifier.padding(19.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    emoji,
                fontSize =
                    31.sp
            )

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        title,
                    color =
                        GamesDark,
                    fontSize =
                        17.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        description,
                    color =
                        GamesGray,
                    fontSize =
                        12.sp,
                    lineHeight =
                        17.sp
                )
            }

            Text(
                text =
                    "EN PAUSA",
                color =
                    GamesOrange,
                fontSize =
                    10.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


@Composable
private fun ComingSoonGameCard(
    emoji: String,
    title: String,
    description: String
) {
    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(0xFFF8FAFC)
            )
    ) {
        Row(
            modifier =
                Modifier.padding(19.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    emoji,
                fontSize =
                    31.sp
            )

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        title,
                    color =
                        GamesDark,
                    fontSize =
                        17.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        description,
                    color =
                        GamesGray,
                    fontSize =
                        12.sp,
                    lineHeight =
                        17.sp
                )
            }

            Text(
                text =
                    "PRONTO",
                color =
                    GamesGray,
                fontSize =
                    10.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
