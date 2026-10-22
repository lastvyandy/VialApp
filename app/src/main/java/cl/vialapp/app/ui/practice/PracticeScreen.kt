package cl.vialapp.app.ui.practice

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
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.CategoryStatResponse
import cl.vialapp.app.data.session.SessionManager


private val PracticeBlue = Color(0xFF2563EB)
private val PracticeDark = Color(0xFF172033)
private val PracticeGray = Color(0xFF64748B)
private val PracticeGreen = Color(0xFF14966F)
private val PracticeOrange = Color(0xFFF59E0B)
private val PracticeBackground = Color(0xFFF4F7FB)


@Composable
fun PracticeScreen(
    onPracticeCategory: (String) -> Unit,
    onTakeExam: () -> Unit
) {

    val context =
        LocalContext.current


    val sessionManager =
        remember {
            SessionManager(context)
        }


    var stats by remember {
        mutableStateOf<List<CategoryStatResponse>>(
            emptyList()
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
                ApiClient.api.getCategoryStats(
                    authorization =
                        "Bearer $token"
                )


            if (response.isSuccessful) {

                stats =
                    response.body()
                        ?: emptyList()

            } else {

                error =
                    "No pudimos cargar tus estadísticas (${response.code()})."
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
                            PracticeBackground
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator(
                    color =
                        PracticeBlue
                )
            }
        }


        error.isNotEmpty() -> {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            PracticeBackground
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
                        Color(0xFFD92D20)
                )
            }
        }


        stats.isEmpty() -> {

            EmptyPracticeState(
                onTakeExam =
                    onTakeExam
            )
        }


        else -> {

            val priority =
                stats.first()


            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            PracticeBackground
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

                    Text(
                        text =
                            "Practicar",

                        color =
                            PracticeDark,

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
                            "Refuerza los temas donde más te estás equivocando.",

                        color =
                            PracticeGray,

                        fontSize =
                            15.sp,

                        lineHeight =
                            21.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )
                }


                item {

                    PriorityPracticeCard(
                        stat =
                            priority,

                        onPractice = {

                            onPracticeCategory(
                                priority.categoria
                            )
                        }
                    )
                }


                if (
                    stats.size > 1
                ) {

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        Text(
                            text =
                                "Tu rendimiento",

                            color =
                                PracticeDark,

                            fontSize =
                                21.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    items(
                        items =
                            stats.drop(1),

                        key = {
                            it.categoria
                        }
                    ) { stat ->

                        CategoryPracticeCard(
                            stat =
                                stat,

                            onClick = {

                                onPracticeCategory(
                                    stat.categoria
                                )
                            }
                        )
                    }
                }


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
                                    Color.White
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
                                    "🚘 ¿Quieres medir tu progreso?",

                                color =
                                    PracticeDark,

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize =
                                    17.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        6.dp
                                    )
                            )


                            Text(
                                text =
                                    "Realiza un examen completo para actualizar tus áreas fuertes y débiles.",

                                color =
                                    PracticeGray,

                                fontSize =
                                    14.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        16.dp
                                    )
                            )


                            OutlinedButton(
                                onClick =
                                    onTakeExam,

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    "Ir a mis exámenes"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun PriorityPracticeCard(
    stat: CategoryStatResponse,
    onPractice: () -> Unit
) {

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
                    Color(0xFFEAF1FF)
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
                    "🎯 Tu prioridad de hoy",

                color =
                    PracticeBlue,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    stat.categoria,

                color =
                    PracticeDark,

                fontSize =
                    23.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Text(
                text =
                    "${stat.successPercentage}% de acierto",

                color =
                    scoreColor(
                        stat.successPercentage
                    ),

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    "${stat.incorrectas} errores de ${stat.totalAnswered} respuestas",

                color =
                    PracticeGray,

                fontSize =
                    14.sp,

                modifier =
                    Modifier.padding(
                        top = 4.dp
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            LinearProgressIndicator(
                progress = {
                    (
                            stat.successPercentage /
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
                            8.dp
                        ),

                color =
                    scoreColor(
                        stat.successPercentage
                    ),

                trackColor =
                    Color(0xFFD6E2F6)
            )


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            Button(
                onClick =
                    onPractice,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            PracticeBlue
                    )
            ) {

                Text(
                    text =
                        "Practicar esta categoría",

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun CategoryPracticeCard(
    stat: CategoryStatResponse,
    onClick: () -> Unit
) {

    Card(
        onClick =
            onClick,

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    18.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        stat.categoria,

                    color =
                        PracticeDark,

                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    text =
                        "${stat.incorrectas} errores",

                    color =
                        PracticeGray,

                    fontSize =
                        12.sp
                )
            }


            Text(
                text =
                    "${stat.successPercentage}%",

                color =
                    scoreColor(
                        stat.successPercentage
                    ),

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


@Composable
private fun EmptyPracticeState(
    onTakeExam: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    PracticeBackground
                )
                .padding(
                    24.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {


        Text(
            text =
                "🚗",

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
                "Todavía no conocemos tus puntos débiles",

            color =
                PracticeDark,

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
                "Completa un examen para que VialApp pueda recomendarte qué practicar.",

            color =
                PracticeGray,

            fontSize =
                15.sp
        )


        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )


        Button(
            onClick =
                onTakeExam,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Hacer mi primer examen"
            )
        }
    }
}


private fun scoreColor(
    score: Int
): Color {

    return when {

        score >= 80 ->
            PracticeGreen

        score >= 60 ->
            PracticeOrange

        else ->
            Color(0xFFD92D20)
    }
}