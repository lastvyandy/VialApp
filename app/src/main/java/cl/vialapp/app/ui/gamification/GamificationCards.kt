package cl.vialapp.app.ui.gamification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.games.DailyMissionSnapshot
import cl.vialapp.app.data.games.GameProgressSnapshot
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.games.StudyStreakSnapshot


private val GamificationBlue =
    Color(0xFF2563EB)

private val GamificationDark =
    Color(0xFF172033)

private val GamificationGray =
    Color(0xFF64748B)

private val GamificationOrange =
    Color(0xFFF59E0B)

private val GamificationGreen =
    Color(0xFF16A34A)

private val GamificationRed =
    Color(0xFFDC2626)


@Composable
fun HomeXpCard(
    onProgress: () -> Unit
) {
    val context =
        LocalContext.current

    val store =
        remember {
            GameProgressStore(
                context
            )
        }

    /*
     * Se usa para refrescar inmediatamente la Home
     * cuando el usuario reclama una misión.
     */
    var refreshKey by remember {
        mutableIntStateOf(
            0
        )
    }


    key(
        refreshKey
    ) {

        /*
         * Lectura directa:
         * al volver desde examen/juego, la Home toma
         * nuevamente los datos de SharedPreferences.
         */
        val progress =
            store.snapshot()

        val streak =
            store.streakSnapshot()

        val mission =
            store.dailyMissionSnapshot()


        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            XpOverviewCard(
                progress =
                    progress,

                onProgress =
                    onProgress
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            HomeStreakCard(
                streak =
                    streak
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            HomeDailyMissionCard(
                mission =
                    mission,

                onClaim = {

                    val earned =
                        store
                            .claimDailyMissionReward()

                    if (
                        earned > 0
                    ) {

                        refreshKey++
                    }
                }
            )
        }
    }
}


@Composable
private fun XpOverviewCard(
    progress: GameProgressSnapshot,
    onProgress: () -> Unit
) {
    val xpProgress =
        (
                progress.levelXp.toFloat() /
                        progress.xpPerLevel.toFloat()
                )
            .coerceIn(
                0f,
                1f
            )

    val xpRemaining =
        (
                progress.xpPerLevel -
                        progress.levelXp
                )
            .coerceAtLeast(
                0
            )


    Card(
        onClick =
            onProgress,
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 17.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier =
                        Modifier.size(
                            46.dp
                        ),
                    shape =
                        RoundedCornerShape(
                            15.dp
                        ),
                    color =
                        Color(
                            0xFFEAF1FF
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
                                "🎮",
                            fontSize =
                                23.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            "Nivel ${progress.level} · ${levelTitle(progress.level)}",
                        color =
                            GamificationDark,
                        fontSize =
                            17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                2.dp
                            )
                    )


                    Text(
                        text =
                            "${progress.totalXp} XP acumulados",
                        color =
                            GamificationGray,
                        fontSize =
                            12.sp
                    )
                }


                Surface(
                    shape =
                        RoundedCornerShape(
                            30.dp
                        ),
                    color =
                        Color(
                            0xFFFFF7E6
                        )
                ) {

                    Text(
                        text =
                            "VER",
                        modifier =
                            Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 6.dp
                            ),
                        color =
                            GamificationOrange,
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            LinearProgressIndicator(
                progress = {
                    xpProgress
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            8.dp
                        ),
                color =
                    GamificationBlue,
                trackColor =
                    Color(
                        0xFFE5EAF2
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        "${progress.levelXp}/${progress.xpPerLevel} XP",
                    color =
                        GamificationGray,
                    fontSize =
                        11.sp
                )


                Spacer(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                Text(
                    text =
                        "$xpRemaining XP para nivel ${progress.level + 1}",
                    color =
                        GamificationBlue,
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun HomeStreakCard(
    streak: StudyStreakSnapshot
) {
    val weeklyProgress =
        (
                streak.activeDaysThisWeek.toFloat() /
                        streak.weeklyGoal.toFloat()
                )
            .coerceIn(
                0f,
                1f
            )

    val statusText =
        when {

            streak.studiedToday ->
                "Ya estudiaste hoy"

            streak.currentStreak > 0 ->
                "Estudia hoy para mantenerla"

            else ->
                "Completa un examen para comenzar"
        }

    val statusColor =
        if (
            streak.studiedToday
        ) {
            GamificationGreen
        } else {
            GamificationOrange
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier =
                        Modifier.size(
                            43.dp
                        ),
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    color =
                        Color(
                            0xFFFFF4E8
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
                                "🔥",
                            fontSize =
                                22.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            11.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            "Racha de estudio",
                        color =
                            GamificationDark,
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(
                        text =
                            "${streak.currentStreak} ${if (streak.currentStreak == 1) "día" else "días"}",
                        color =
                            GamificationOrange,
                        fontSize =
                            20.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }


                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text =
                            "Mejor: ${streak.bestStreak}",
                        color =
                            GamificationGray,
                        fontSize =
                            11.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Text(
                        text =
                            statusText,
                        color =
                            statusColor,
                        fontSize =
                            10.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        13.dp
                    )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        "Objetivo semanal",
                    color =
                        GamificationGray,
                    fontSize =
                        11.sp
                )


                Spacer(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                Text(
                    text =
                        "${streak.activeDaysThisWeek}/${streak.weeklyGoal} días",
                    color =
                        GamificationDark,
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            LinearProgressIndicator(
                progress = {
                    weeklyProgress
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            7.dp
                        ),
                color =
                    GamificationOrange,
                trackColor =
                    Color(
                        0xFFF1F5F9
                    )
            )
        }
    }
}


@Composable
private fun HomeDailyMissionCard(
    mission: DailyMissionSnapshot,
    onClaim: () -> Unit
) {
    val progress =
        (
                mission.progress.toFloat() /
                        mission.goal.toFloat()
                )
            .coerceIn(
                0f,
                1f
            )

    val containerColor =
        when {

            mission.claimed ->
                Color(
                    0xFFEAF8F1
                )

            mission.completed ->
                Color(
                    0xFFFFF8E8
                )

            else ->
                Color.White
        }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    containerColor
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier =
                        Modifier.size(
                            43.dp
                        ),
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    color =
                        if (
                            mission.claimed
                        ) {
                            Color(
                                0xFFDDF5E8
                            )
                        } else {
                            Color(
                                0xFFEAF1FF
                            )
                        }
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                if (
                                    mission.claimed
                                ) {
                                    "✅"
                                } else {
                                    "🎯"
                                },
                            fontSize =
                                21.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            11.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            if (
                                mission.claimed
                            ) {
                                "Misión completada"
                            } else {
                                "Misión de hoy"
                            },
                        color =
                            GamificationGray,
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )


                    Text(
                        text =
                            mission.title,
                        color =
                            GamificationDark,
                        fontSize =
                            15.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Surface(
                    shape =
                        RoundedCornerShape(
                            30.dp
                        ),
                    color =
                        if (
                            mission.claimed
                        ) {
                            Color(
                                0xFFDDF5E8
                            )
                        } else {
                            Color(
                                0xFFFFF4E8
                            )
                        }
                ) {

                    Text(
                        text =
                            "+${mission.rewardXp} XP",
                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                        color =
                            if (
                                mission.claimed
                            ) {
                                GamificationGreen
                            } else {
                                GamificationOrange
                            },
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        11.dp
                    )
            )


            Text(
                text =
                    mission.description,
                color =
                    GamificationDark,
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    mission.detail,
                color =
                    GamificationGray,
                fontSize =
                    11.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            LinearProgressIndicator(
                progress = {
                    progress
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            7.dp
                        ),
                color =
                    if (
                        mission.completed
                    ) {
                        GamificationGreen
                    } else {
                        GamificationBlue
                    },
                trackColor =
                    Color(
                        0xFFE5EAF2
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            when {

                mission.claimed -> {

                    Text(
                        text =
                            "Recompensa reclamada · Se renueva mañana",
                        color =
                            GamificationGreen,
                        fontSize =
                            11.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }


                mission.completed -> {

                    Button(
                        onClick =
                            onClaim,
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    GamificationGreen
                            )
                    ) {

                        Text(
                            text =
                                "Reclamar +${mission.rewardXp} XP",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }


                else -> {

                    Text(
                        text =
                            "Se renueva mañana",
                        color =
                            GamificationGray,
                        fontSize =
                            10.sp
                    )
                }
            }
        }
    }
}


@Composable
fun ProgressGamificationSection() {
    val context =
        LocalContext.current

    val store =
        remember {
            GameProgressStore(
                context
            )
        }

    val progress =
        store.snapshot()

    val streak =
        store.streakSnapshot()

    val baldHighScore =
        store.getHighScore(
            GameProgressStore.BALD_CHALLENGE
        )

    val crossroadsHighScore =
        store.getHighScore(
            GameProgressStore.CROSSROADS_CHAOS
        )

    val xpProgress =
        (
                progress.levelXp.toFloat() /
                        progress.xpPerLevel.toFloat()
                )
            .coerceIn(
                0f,
                1f
            )


    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            text =
                "Progreso de jugador",
            color =
                GamificationDark,
            fontSize =
                21.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    5.dp
                )
        )


        Text(
            text =
                "La XP reúne tu actividad de estudio y tus minijuegos en una sola progresión.",
            color =
                GamificationGray,
            fontSize =
                13.sp,
            lineHeight =
                18.sp
        )


        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    24.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        20.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Surface(
                        modifier =
                            Modifier.size(
                                58.dp
                            ),
                        shape =
                            RoundedCornerShape(
                                18.dp
                            ),
                        color =
                            Color(
                                0xFFEAF1FF
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
                                    "🎮",
                                fontSize =
                                    29.sp
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.width(
                                14.dp
                            )
                    )


                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        Text(
                            text =
                                "Nivel ${progress.level}",
                            color =
                                GamificationDark,
                            fontSize =
                                24.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )


                        Text(
                            text =
                                levelTitle(
                                    progress.level
                                ),
                            color =
                                GamificationBlue,
                            fontSize =
                                13.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    Column(
                        horizontalAlignment =
                            Alignment.End
                    ) {

                        Text(
                            text =
                                "${progress.totalXp}",
                            color =
                                GamificationDark,
                            fontSize =
                                20.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            text =
                                "XP total",
                            color =
                                GamificationGray,
                            fontSize =
                                10.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            17.dp
                        )
                )


                LinearProgressIndicator(
                    progress = {
                        xpProgress
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                9.dp
                            ),
                    color =
                        GamificationBlue,
                    trackColor =
                        Color(
                            0xFFE5EAF2
                        )
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                Text(
                    text =
                        "${progress.levelXp}/${progress.xpPerLevel} XP hacia el nivel ${progress.level + 1}",
                    color =
                        GamificationGray,
                    fontSize =
                        11.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            19.dp
                        )
                )


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    ProgressMetric(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        value =
                            "${progress.totalPoints}",
                        label =
                            "Puntos arcade",
                        emoji =
                            "🏆"
                    )


                    ProgressMetric(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        value =
                            "${progress.totalRuns}",
                        label =
                            "Partidas",
                        emoji =
                            "🎮"
                    )


                    ProgressMetric(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        value =
                            "${progress.totalExamCompletions}",
                        label =
                            "Exámenes",
                        emoji =
                            "📝"
                    )
                }
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        Text(
            text =
                "Constancia",
            color =
                GamificationDark,
            fontSize =
                17.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    20.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            16.dp
                        ),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                ProgressMetric(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    value =
                        "${streak.currentStreak}",
                    label =
                        "Racha actual",
                    emoji =
                        "🔥"
                )


                ProgressMetric(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    value =
                        "${streak.bestStreak}",
                    label =
                        "Mejor racha",
                    emoji =
                        "🏅"
                )


                ProgressMetric(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    value =
                        "${progress.totalMissionsCompleted}",
                    label =
                        "Misiones",
                    emoji =
                        "🎯"
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        Text(
            text =
                "Récords personales",
            color =
                GamificationDark,
            fontSize =
                17.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        GameRecordRow(
            emoji =
                "👨‍🦲💪",
            title =
                "Reto del Pelao",
            score =
                baldHighScore
        )


        Spacer(
            modifier =
                Modifier.height(
                    9.dp
                )
        )


        GameRecordRow(
            emoji =
                "🚦💥",
            title =
                "Cruce Caótico",
            score =
                crossroadsHighScore
        )


        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )


        MilestoneCard(
            progress =
                progress,

            streak =
                streak
        )
    }
}


@Composable
private fun ProgressMetric(
    modifier: Modifier,
    value: String,
    label: String,
    emoji: String
) {
    Surface(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                17.dp
            ),
        color =
            Color(
                0xFFF7F9FC
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    horizontal = 6.dp,
                    vertical = 13.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    emoji,
                fontSize =
                    18.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    value,
                color =
                    GamificationDark,
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    label,
                color =
                    GamificationGray,
                fontSize =
                    9.sp
            )
        }
    }
}


@Composable
private fun GameRecordRow(
    emoji: String,
    title: String,
    score: Int
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        color =
            Color.White
    ) {

        Row(
            modifier =
                Modifier.padding(
                    15.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    emoji,
                fontSize =
                    26.sp
            )


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )


            Text(
                text =
                    title,
                modifier =
                    Modifier.weight(
                        1f
                    ),
                color =
                    GamificationDark,
                fontSize =
                    14.sp,
                fontWeight =
                    FontWeight.SemiBold
            )


            Text(
                text =
                    if (
                        score > 0
                    ) {
                        score.toString()
                    } else {
                        "—"
                    },
                color =
                    if (
                        score > 0
                    ) {
                        GamificationOrange
                    } else {
                        GamificationGray
                    },
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


@Composable
private fun MilestoneCard(
    progress: GameProgressSnapshot,
    streak: StudyStreakSnapshot
) {
    val milestone =
        when {

            streak.currentStreak >= 7 ->
                "🔥 Una semana completa de estudio. Tu constancia ya se está convirtiendo en hábito."

            progress.totalMissionsCompleted >= 5 ->
                "🎯 Ya completaste 5 misiones diarias."

            progress.totalExamCompletions >= 1 ->
                "📝 Tus exámenes ya suman XP, racha y progreso de misión."

            progress.totalRuns >= 1 ->
                "🎮 Tus minijuegos suman XP y también pueden ayudar con algunas misiones."

            else ->
                "🚗 Completa un examen para iniciar tu racha de estudio."
        }


    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        color =
            Color(
                0xFFEAF8F1
            )
    ) {

        Text(
            text =
                milestone,
            modifier =
                Modifier.padding(
                    15.dp
                ),
            color =
                GamificationGreen,
            fontSize =
                12.sp,
            fontWeight =
                FontWeight.SemiBold,
            lineHeight =
                17.sp
        )
    }
}


private fun levelTitle(
    level: Int
): String {
    return when {

        level >= 20 ->
            "Leyenda Vial"

        level >= 15 ->
            "Maestro del Cruce"

        level >= 10 ->
            "Piloto Experto"

        level >= 5 ->
            "Piloto Vial"

        level >= 3 ->
            "Aprendiz Vial"

        else ->
            "Novato"
    }
}
