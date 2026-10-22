package cl.vialapp.app.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ExamSubmitResponse


private val ResultBlue = Color(0xFF2563EB)
private val ResultDark = Color(0xFF172033)
private val ResultGray = Color(0xFF64748B)
private val ResultBackground = Color(0xFFF4F7FB)


@Composable
fun ExamResultScreen(
    result: ExamSubmitResponse,
    correctionLoading: Boolean,
    correctionError: String,
    onViewCorrection: () -> Unit,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }


    val progressTarget =
        (result.puntaje / 100f)
            .coerceIn(
                0f,
                1f
            )


    val animatedProgress by
    animateFloatAsState(
        targetValue =
            progressTarget,

        label =
            "examResultProgress"
    )


    val incorrectas =
        result.totalQuestions -
                result.correctas


    val title =
        when {

            result.puntaje >= 87 ->
                "¡Excelente! 🎉"

            result.puntaje >= 70 ->
                "¡Buen trabajo! 💪"

            result.puntaje >= 50 ->
                "Puedes mejorar"

            else ->
                "Necesitas repasar"
        }


    val subtitle =
        when {

            result.puntaje >= 87 ->
                "Tienes un muy buen dominio de este examen."

            result.puntaje >= 70 ->
                "Estás cerca. Sigue reforzando tus puntos débiles."

            result.puntaje >= 50 ->
                "Repasa los temas donde tuviste más errores."

            else ->
                "Vamos a usar tus errores para reforzar tu aprendizaje."
        }


    Scaffold(

        containerColor =
            ResultBackground,

        bottomBar = {

            Surface(
                color =
                    Color.White,

                shadowElevation =
                    6.dp
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 12.dp
                            )
                ) {


                    OutlinedButton(
                        enabled =
                            !correctionLoading,

                        onClick =
                            onViewCorrection,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    52.dp
                                ),

                        shape =
                            RoundedCornerShape(
                                16.dp
                            )
                    ) {

                        if (
                            correctionLoading
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        20.dp
                                    ),

                                strokeWidth =
                                    2.dp
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        10.dp
                                    )
                            )
                        }


                        Text(
                            if (correctionLoading) {
                                "Cargando corrección..."
                            } else {
                                "Ver corrección"
                            }
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Button(
                        onClick =
                            onBack,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    54.dp
                                ),

                        shape =
                            RoundedCornerShape(
                                16.dp
                            ),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        ResultBlue
                                )
                    ) {

                        Text(
                            text =
                                "Volver a mis exámenes",

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }

    ) { innerPadding ->


        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .statusBarsPadding()
                    .padding(
                        24.dp
                    ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            Text(
                text =
                    "Resultado",

                color =
                    ResultDark,

                fontSize =
                    30.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        30.dp
                    )
            )


            Box(
                modifier =
                    Modifier.size(
                        190.dp
                    ),

                contentAlignment =
                    Alignment.Center
            ) {


                CircularProgressIndicator(
                    progress = {
                        animatedProgress
                    },

                    modifier =
                        Modifier.fillMaxSize(),

                    strokeWidth =
                        16.dp,

                    color =
                        ResultBlue,

                    trackColor =
                        Color(
                            0xFFDCE6F5
                        )
                )


                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {


                    Text(
                        text =
                            "${result.puntaje}%",

                        color =
                            ResultDark,

                        fontSize =
                            44.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Text(
                        text =
                            "${result.correctas}/${result.totalQuestions} correctas",

                        color =
                            ResultGray,

                        fontSize =
                            14.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )


            Text(
                text =
                    title,

                color =
                    ResultBlue,

                fontSize =
                    24.sp,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Text(
                text =
                    subtitle,

                color =
                    ResultGray,

                fontSize =
                    16.sp,

                lineHeight =
                    23.sp,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        26.dp
                    )
            )


            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        22.dp
                    ),

                colors =
                    CardDefaults
                        .cardColors(
                            containerColor =
                                Color.White
                        )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 22.dp,
                                horizontal = 12.dp
                            ),

                    horizontalArrangement =
                        Arrangement.SpaceEvenly
                ) {


                    ResultStat(
                        value =
                            result.correctas
                                .toString(),

                        label =
                            "Correctas",

                        color =
                            Color(
                                0xFF14966F
                            )
                    )


                    ResultStat(
                        value =
                            incorrectas
                                .toString(),

                        label =
                            "Incorrectas",

                        color =
                            Color(
                                0xFFD92D20
                            )
                    )


                    ResultStat(
                        value =
                            result.totalQuestions
                                .toString(),

                        label =
                            "Total",

                        color =
                            ResultBlue
                    )
                }
            }


            if (
                correctionError.isNotEmpty()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )


                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),

                    color =
                        Color(
                            0xFFFFF1F0
                        ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        )
                ) {

                    Text(
                        text =
                            correctionError,

                        modifier =
                            Modifier.padding(
                                16.dp
                            ),

                        color =
                            Color(
                                0xFFB42318
                            ),

                        fontSize =
                            14.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
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
                    CardDefaults
                        .cardColors(
                            containerColor =
                                Color(
                                    0xFFEAF1FF
                                )
                        )
            ) {

                Text(
                    text =
                        "🎯 Tus resultados ya fueron guardados. VialApp utilizará tus respuestas para identificar los temas que necesitas reforzar.",

                    modifier =
                        Modifier.padding(
                            18.dp
                        ),

                    color =
                        ResultDark,

                    fontSize =
                        14.sp,

                    lineHeight =
                        21.sp
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )
        }
    }
}


@Composable
private fun ResultStat(
    value: String,
    label: String,
    color: Color
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text =
                value,

            color =
                color,

            fontSize =
                27.sp,

            fontWeight =
                FontWeight.Bold
        )


        Text(
            text =
                label,

            color =
                ResultGray,

            fontSize =
                12.sp
        )
    }
}