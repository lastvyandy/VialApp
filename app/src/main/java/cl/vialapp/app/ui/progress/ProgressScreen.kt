package cl.vialapp.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cl.vialapp.app.data.api.AdvancedStatsResponse
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.CategoryStatResponse
import cl.vialapp.app.data.api.RecentTestResponse
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.gamification.ProgressGamificationSection

private val ProgressBlue = Color(0xFF2563EB)
private val ProgressDark = Color(0xFF172033)
private val ProgressGray = Color(0xFF64748B)
private val ProgressGreen = Color(0xFF14966F)
private val ProgressRed = Color(0xFFD92D20)
private val ProgressOrange = Color(0xFFF59E0B)
private val ProgressBackground = Color(0xFFF4F7FB)


@Composable
fun ProgressScreen() {

    val context =
        LocalContext.current


    val sessionManager =
        remember {
            SessionManager(context)
        }


    var stats by remember {
        mutableStateOf<AdvancedStatsResponse?>(
            null
        )
    }


    var loading by remember {
        mutableStateOf(true)
    }


    var error by remember {
        mutableStateOf("")
    }


    LaunchedEffect(Unit) {

        loading = true
        error = ""


        try {

            val token =
                sessionManager.getAccessToken()


            if (token == null) {

                error =
                    "No se encontró una sesión válida."

                return@LaunchedEffect
            }


            val response =
                ApiClient.api.getAdvancedStats(
                    authorization =
                        "Bearer $token"
                )


            if (response.isSuccessful) {

                stats =
                    response.body()

            } else {

                error =
                    "No se pudo cargar tu progreso (${response.code()})."
            }


        } catch (e: Exception) {

            error =
                "No se pudo conectar con VialApp."

            e.printStackTrace()


        } finally {

            loading = false
        }
    }


    when {

        loading -> {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            ProgressBackground
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator(
                    color =
                        ProgressBlue
                )
            }
        }


        error.isNotEmpty() -> {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            ProgressBackground
                        )
                        .padding(
                            24.dp
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        error,

                    color =
                        ProgressRed
                )
            }
        }


        stats == null ||
                stats?.summary?.hasData != true -> {

            EmptyProgressState()
        }


        else -> {

            ProgressContent(
                stats =
                    stats!!
            )
        }
    }
}


@Composable
private fun ProgressContent(
    stats: AdvancedStatsResponse
) {

    val summary =
        stats.summary


    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    ProgressBackground
                ),

        contentPadding =
            PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 110.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {


        item {

            ProgressGamificationSection()
        }

        /*
         * CABECERA
         */
        item {

            Text(
                text =
                    "Tu progreso",

                color =
                    ProgressDark,

                fontSize =
                    30.sp,

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
                    "Mira cómo has avanzado y qué temas puedes seguir reforzando.",

                color =
                    ProgressGray,

                fontSize =
                    15.sp,

                lineHeight =
                    21.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )
        }


        /*
         * PROMEDIO PRINCIPAL
         */
        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        26.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(
                                0xFFEAF1FF
                            )
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            22.dp
                        )
                ) {


                    Text(
                        text =
                            "Promedio general",

                        color =
                            ProgressBlue,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            14.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Row(
                        verticalAlignment =
                            Alignment.Bottom
                    ) {


                        Text(
                            text =
                                "${summary.averageScore}%",

                            color =
                                scoreColor(
                                    summary.averageScore
                                ),

                            fontSize =
                                42.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    10.dp
                                )
                        )


                        Text(
                            text =
                                "en ${summary.totalTests} exámenes",

                            color =
                                ProgressGray,

                            fontSize =
                                14.sp,

                            modifier =
                                Modifier.padding(
                                    bottom = 8.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                16.dp
                            )
                    )


                    LinearProgressIndicator(
                        progress = {
                            (
                                    summary.averageScore /
                                            100f
                                    )
                                .coerceIn(
                                    0f,
                                    1f
                                )
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    9.dp
                                ),

                        color =
                            scoreColor(
                                summary.averageScore
                            ),

                        trackColor =
                            Color(
                                0xFFD6E2F6
                            )
                    )
                }
            }
        }


        /*
         * RESUMEN EN 4 TARJETAS
         */
        item {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {


                    ProgressStatCard(
                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        emoji =
                            "✅",

                        value =
                            "${summary.passedTests}",

                        label =
                            "Aprobados",

                        secondary =
                            "${summary.passPercentage}% del total"
                    )


                    ProgressStatCard(
                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        emoji =
                            "🏆",

                        value =
                            "${summary.bestScore}%",

                        label =
                            "Mejor puntaje",

                        secondary =
                            "Tu mejor resultado"
                    )
                }


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {


                    ProgressStatCard(
                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        emoji =
                            "📝",

                        value =
                            "${summary.totalAnsweredQuestions}",

                        label =
                            "Preguntas",

                        secondary =
                            "${summary.totalCorrectAnswers} correctas"
                    )


                    ProgressStatCard(
                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        emoji =
                            "📅",

                        value =
                            "${summary.testsLast7Days}",

                        label =
                            "Últimos 7 días",

                        secondary =
                            "${summary.testsLast30Days} en 30 días"
                    )
                }
            }
        }


        /*
         * MEJOR CATEGORÍA
         */
        summary.bestCategory
            ?.let { bestCategory ->

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(
                                22.dp
                            ),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Color(
                                        0xFFEAF8F1
                                    )
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    20.dp
                                )
                        ) {


                            Text(
                                text =
                                    "🌟 Tu mejor categoría",

                                color =
                                    ProgressGreen,

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize =
                                    14.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )


                            Text(
                                text =
                                    bestCategory.nombre,

                                color =
                                    ProgressDark,

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize =
                                    19.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        5.dp
                                    )
                            )


                            Text(
                                text =
                                    "${bestCategory.successPercentage}% de acierto",

                                color =
                                    ProgressGreen,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }


        /*
         * RENDIMIENTO POR CATEGORÍA
         */
        if (
            stats.categories.isNotEmpty()
        ) {

            item {

                Text(
                    text =
                        "Rendimiento por categoría",

                    color =
                        ProgressDark,

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.padding(
                            top = 8.dp
                        )
                )
            }


            items(
                items =
                    stats.categories,

                key = {
                    it.categoria
                }
            ) { category ->

                ProgressCategoryCard(
                    category =
                        category
                )
            }
        }


        /*
         * RESULTADOS RECIENTES
         */
        if (
            stats.recentTests.isNotEmpty()
        ) {

            item {

                Text(
                    text =
                        "Últimos resultados",

                    color =
                        ProgressDark,

                    fontSize =
                        21.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier =
                        Modifier.padding(
                            top = 10.dp
                        )
                )
            }


            items(
                items =
                    stats.recentTests.reversed(),

                key = {
                    it.id
                }
            ) { test ->

                RecentTestCard(
                    test =
                        test
                )
            }
        }


        /*
         * CRITERIO
         */
        item {

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                color =
                    Color.White,

                shape =
                    RoundedCornerShape(
                        18.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            18.dp
                        )
                ) {


                    Text(
                        text =
                            "ℹ️ Criterio de aprobación",

                        color =
                            ProgressDark,

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
                            summary.passCriterion,

                        color =
                            ProgressGray,

                        fontSize =
                            14.sp
                    )
                }
            }
        }
    }
}


@Composable
private fun ProgressStatCard(
    modifier: Modifier,
    emoji: String,
    value: String,
    label: String,
    secondary: String
) {

    Card(
        modifier =
            modifier,

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
                    16.dp
                )
        ) {


            Text(
                text =
                    emoji,

                fontSize =
                    22.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Text(
                text =
                    value,

                color =
                    ProgressDark,

                fontSize =
                    23.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    label,

                color =
                    ProgressDark,

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        3.dp
                    )
            )


            Text(
                text =
                    secondary,

                color =
                    ProgressGray,

                fontSize =
                    11.sp
            )
        }
    }
}


@Composable
private fun ProgressCategoryCard(
    category: CategoryStatResponse
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
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
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Text(
                    text =
                        category.categoria,

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    color =
                        ProgressDark,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        15.sp
                )


                Text(
                    text =
                        "${category.successPercentage}%",

                    color =
                        scoreColor(
                            category.successPercentage
                        ),

                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        9.dp
                    )
            )


            LinearProgressIndicator(
                progress = {
                    (
                            category.successPercentage /
                                    100f
                            )
                        .coerceIn(
                            0f,
                            1f
                        )
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            7.dp
                        ),

                color =
                    scoreColor(
                        category.successPercentage
                    ),

                trackColor =
                    Color(
                        0xFFE8EDF5
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
                    "${category.correctas} correctas · ${category.incorrectas} incorrectas",

                color =
                    ProgressGray,

                fontSize =
                    12.sp
            )
        }
    }
}


@Composable
private fun RecentTestCard(
    test: RecentTestResponse
) {

    val score =
        test.puntaje ?: 0


    val correct =
        test.correctas ?: 0


    val total =
        test.totalQuestions ?: 0


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    17.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Surface(
                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                color =
                    scoreColor(score)
                        .copy(
                            alpha = 0.10f
                        )
            ) {

                Box(
                    modifier =
                        Modifier.size(
                            48.dp
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            "$score%",

                        color =
                            scoreColor(
                                score
                            ),

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            13.sp
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
                        "Examen #${test.testId}",

                    color =
                        ProgressDark,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    text =
                        "$correct de $total correctas",

                    color =
                        ProgressGray,

                    fontSize =
                        13.sp,

                    modifier =
                        Modifier.padding(
                            top = 3.dp
                        )
                )


                test.durationSeconds
                    ?.let { seconds ->

                        Text(
                            text =
                                "Duración: ${formatDuration(seconds)}",

                            color =
                                ProgressGray,

                            fontSize =
                                11.sp,

                            modifier =
                                Modifier.padding(
                                    top = 2.dp
                                )
                        )
                    }
            }
        }
    }
}


@Composable
private fun EmptyProgressState() {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    ProgressBackground
                )
                .padding(
                    28.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {


        Text(
            text =
                "📊",

            fontSize =
                54.sp
        )


        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )


        Text(
            text =
                "Todavía no hay progreso que mostrar",

            color =
                ProgressDark,

            fontSize =
                22.sp,

            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )


        Text(
            text =
                "Cuando completes tu primer examen, aquí podrás ver tus resultados y evolución.",

            color =
                ProgressGray,

            fontSize =
                15.sp,

            lineHeight =
                22.sp
        )
    }
}


private fun scoreColor(
    score: Int
): Color {

    return when {

        score >= 80 ->
            ProgressGreen

        score >= 60 ->
            ProgressOrange

        else ->
            ProgressRed
    }
}


private fun formatDuration(
    seconds: Int
): String {

    val minutes =
        seconds / 60

    val remainingSeconds =
        seconds % 60


    return if (
        minutes > 0
    ) {

        "${minutes}m ${remainingSeconds}s"

    } else {

        "${remainingSeconds}s"
    }
}