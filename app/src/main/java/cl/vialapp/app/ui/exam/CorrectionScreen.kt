package cl.vialapp.app.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ExamCorrectionQuestion
import cl.vialapp.app.data.api.ExamCorrectionResponse
import coil3.compose.AsyncImage


private val CorrectionBlue = Color(0xFF2563EB)
private val CorrectionDark = Color(0xFF172033)
private val CorrectionGray = Color(0xFF64748B)
private val CorrectionGreen = Color(0xFF14966F)
private val CorrectionRed = Color(0xFFD92D20)
private val CorrectionOrange = Color(0xFFB54708)


@Composable
fun CorrectionScreen(
    correction: ExamCorrectionResponse,
    onBack: () -> Unit
) {

    /*
     * Errores que realmente tuvo el usuario
     * en el examen original.
     *
     * Esto NO cambia porque es información
     * histórica del examen.
     */
    val originalFailedQuestions =
        remember(correction) {

            correction.detalles.filter {
                !it.isCorrect
            }
        }


    /*
     * Errores que todavía quedan por
     * resolver durante esta sesión
     * de práctica.
     */
    var remainingErrorIds by remember(
        correction.id
    ) {

        mutableStateOf(
            originalFailedQuestions
                .map {
                    it.questionId
                }
                .toSet()
        )
    }


    var practiceMode by remember {
        mutableStateOf(false)
    }


    val remainingQuestions =
        originalFailedQuestions.filter {

            remainingErrorIds.contains(
                it.questionId
            )
        }


    /*
     * ------------------------------------------------
     * MODO PRÁCTICA DE ERRORES
     * ------------------------------------------------
     */
    if (
        practiceMode &&
        remainingQuestions.isNotEmpty()
    ) {

        ErrorPracticeScreen(
            questions =
                remainingQuestions,

            onExit = { resolvedIds ->

                /*
                 * Quitamos solamente las preguntas
                 * que fueron contestadas COMPLETAMENTE
                 * bien durante esta práctica.
                 */
                remainingErrorIds =
                    remainingErrorIds -
                            resolvedIds

                practiceMode =
                    false
            }
        )

        return
    }


    BackHandler {
        onBack()
    }


    Scaffold(

        containerColor =
            Color(0xFFF4F7FB),

        bottomBar = {

            Surface(
                color =
                    Color.White,

                shadowElevation =
                    5.dp
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                16.dp
                            )
                ) {


                    /*
                     * Todavía quedan errores.
                     */
                    if (
                        remainingQuestions.isNotEmpty()
                    ) {

                        Button(
                            onClick = {

                                practiceMode =
                                    true
                            },

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
                                            CorrectionGreen
                                    )
                        ) {

                            Text(
                                text =
                                    if (
                                        remainingQuestions.size ==
                                        originalFailedQuestions.size
                                    ) {

                                        "Practicar ${remainingQuestions.size} errores"

                                    } else {

                                        "Practicar ${remainingQuestions.size} pendientes"
                                    },

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                    } else if (
                        originalFailedQuestions.isNotEmpty()
                    ) {

                        /*
                         * El usuario ya resolvió todos
                         * los errores durante la práctica.
                         */
                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),

                            color =
                                Color(0xFFEAF8F1),

                            shape =
                                RoundedCornerShape(
                                    16.dp
                                ),

                            border =
                                BorderStroke(
                                    1.dp,
                                    Color(0xFFA9DEC9)
                                )
                        ) {

                            Text(
                                text =
                                    "✓ Repasaste todos tus errores",

                                modifier =
                                    Modifier.padding(
                                        16.dp
                                    ),

                                color =
                                    CorrectionGreen,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    TextButton(
                        onClick =
                            onBack,

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Volver al resultado"
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
                    .statusBarsPadding()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        20.dp
                    )
        ) {


            Text(
                text =
                    "Corrección",

                color =
                    CorrectionDark,

                fontSize =
                    30.sp,

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
                    "${correction.correctas} de ${correction.totalQuestions} respuestas correctas",

                color =
                    CorrectionGray,

                fontSize =
                    15.sp
            )


            /*
             * Mostrar progreso de práctica
             * separado del resultado histórico.
             */
            if (
                originalFailedQuestions.isNotEmpty()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),

                    color =
                        Color.White,

                    shape =
                        RoundedCornerShape(
                            16.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                16.dp
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
                                    "Errores del examen",

                                color =
                                    CorrectionGray,

                                fontSize =
                                    12.sp
                            )


                            Text(
                                text =
                                    originalFailedQuestions
                                        .size
                                        .toString(),

                                color =
                                    CorrectionRed,

                                fontSize =
                                    22.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }


                        Column(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {

                            Text(
                                text =
                                    "Pendientes de repaso",

                                color =
                                    CorrectionGray,

                                fontSize =
                                    12.sp
                            )


                            Text(
                                text =
                                    remainingQuestions
                                        .size
                                        .toString(),

                                color =
                                    if (
                                        remainingQuestions.isEmpty()
                                    ) {

                                        CorrectionGreen

                                    } else {

                                        CorrectionBlue
                                    },

                                fontSize =
                                    22.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )


            correction.detalles
                .forEachIndexed {
                        index,
                        question ->


                    CorrectionQuestionCard(
                        number =
                            index + 1,

                        question =
                            question
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                16.dp
                            )
                    )
                }
        }
    }
}


/*
 * ------------------------------------------------
 * TARJETA DE CORRECCIÓN
 * ------------------------------------------------
 */

@Composable
private fun CorrectionQuestionCard(
    number: Int,
    question: ExamCorrectionQuestion
) {

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
                        Color.White
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {


            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Text(
                    text =
                        "Pregunta $number",

                    color =
                        CorrectionBlue,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        14.sp
                )


                Spacer(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                Text(
                    text =
                        if (question.isCorrect) {

                            "✓ Correcta"

                        } else {

                            "✕ Incorrecta"
                        },

                    color =
                        if (question.isCorrect) {

                            CorrectionGreen

                        } else {

                            CorrectionRed
                        },

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        13.sp
                )
            }


            if (
                !question.imageUrl.isNullOrBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )


                AsyncImage(
                    model =
                        question.imageUrl,

                    contentDescription =
                        "Imagen de la pregunta",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                160.dp
                            ),

                    contentScale =
                        ContentScale.Fit
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            Text(
                text =
                    question.questionText,

                color =
                    CorrectionDark,

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    18.sp,

                lineHeight =
                    25.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            question.alternativas
                .toList()
                .sortedBy {
                    it.first
                }
                .forEach { alternative ->

                    val letter =
                        alternative.first

                    val text =
                        alternative.second


                    val correct =
                        question.correctAnswers
                            .contains(
                                letter
                            )


                    val selected =
                        question.selectedAnswers
                            .contains(
                                letter
                            )


                    CorrectionOption(
                        letter =
                            letter,

                        text =
                            text,

                        correct =
                            correct,

                        selected =
                            selected
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )
                }


            if (
                question.explicacion
                    .isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                Surface(
                    color =
                        Color(
                            0xFFFFF8E8
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                ) {

                    Text(
                        text =
                            "💡 ${question.explicacion}",

                        modifier =
                            Modifier.padding(
                                14.dp
                            ),

                        color =
                            CorrectionDark,

                        fontSize =
                            14.sp,

                        lineHeight =
                            21.sp
                    )
                }
            }
        }
    }
}


/*
 * ------------------------------------------------
 * ALTERNATIVA EN CORRECCIÓN
 * ------------------------------------------------
 */

@Composable
private fun CorrectionOption(
    letter: String,
    text: String,
    correct: Boolean,
    selected: Boolean
) {

    val background =
        when {

            correct ->
                Color(
                    0xFFEAF8F1
                )

            selected ->
                Color(
                    0xFFFFF1F0
                )

            else ->
                Color(
                    0xFFF8FAFD
                )
        }


    val border =
        when {

            correct ->
                Color(
                    0xFFA9DEC9
                )

            selected ->
                Color(
                    0xFFF2B8B5
                )

            else ->
                Color(
                    0xFFE2E8F0
                )
        }


    val markerColor =
        when {

            correct ->
                CorrectionGreen

            selected ->
                CorrectionRed

            else ->
                CorrectionGray
        }


    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        color =
            background,

        border =
            BorderStroke(
                1.dp,
                border
            ),

        shape =
            RoundedCornerShape(
                14.dp
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    13.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            Text(
                text =
                    letter.uppercase(),

                color =
                    markerColor,

                fontWeight =
                    FontWeight.Bold,

                modifier =
                    Modifier.width(
                        28.dp
                    )
            )


            Text(
                text =
                    text,

                modifier =
                    Modifier.weight(
                        1f
                    ),

                color =
                    CorrectionDark,

                fontSize =
                    14.sp
            )


            when {

                correct -> {

                    Text(
                        text =
                            "✓",

                        color =
                            CorrectionGreen,

                        fontWeight =
                            FontWeight.Bold
                    )
                }


                selected -> {

                    Text(
                        text =
                            "✕",

                        color =
                            CorrectionRed,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


/*
 * ------------------------------------------------
 * PRÁCTICA DE ERRORES
 * ------------------------------------------------
 */

@Composable
private fun ErrorPracticeScreen(
    questions: List<ExamCorrectionQuestion>,

    /*
     * Devuelve los IDs de preguntas que
     * fueron resueltas correctamente.
     */
    onExit: (Set<Int>) -> Unit
) {

    var index by remember {
        mutableIntStateOf(0)
    }


    var selectedAnswers by remember {
        mutableStateOf<Set<String>>(
            emptySet()
        )
    }


    var checked by remember {
        mutableStateOf(false)
    }


    /*
     * Preguntas resueltas correctamente
     * durante ESTA práctica.
     */
    var resolvedQuestionIds by remember {

        mutableStateOf<Set<Int>>(
            emptySet()
        )
    }


    val question =
        questions[index]


    /*
     * Una pregunta es múltiple si tiene
     * más de una respuesta correcta.
     *
     * El endpoint de corrección actualmente
     * no devuelve is_multiple.
     */
    val multiple =
        question.correctAnswers
            .toSet()
            .size > 1


    /*
     * CORRECCIÓN EXACTA.
     *
     * Ejemplo:
     *
     * correctas = A + C
     *
     * A       -> incorrecta
     * A + B   -> incorrecta
     * A + C   -> correcta
     * A+B+C   -> incorrecta
     *
     * ÚNICAMENTE A+C es válida.
     */
    val correctAnswersSet =
        question.correctAnswers
            .toSet()


    val answerIsCorrect =
        checked &&
                selectedAnswers ==
                correctAnswersSet


    BackHandler {

        onExit(
            resolvedQuestionIds
        )
    }


    Scaffold(

        containerColor =
            Color(
                0xFFF4F7FB
            ),

        bottomBar = {

            Surface(
                color =
                    Color.White,

                shadowElevation =
                    5.dp
            ) {

                Button(
                    enabled =
                        selectedAnswers
                            .isNotEmpty(),

                    onClick = {


                        /*
                         * PRIMER CLICK:
                         * comprobar.
                         */
                        if (!checked) {

                            /*
                             * ESTA es la comprobación
                             * importante.
                             *
                             * Igualdad EXACTA entre
                             * los dos conjuntos.
                             */
                            val exactAnswer =
                                selectedAnswers ==
                                        correctAnswersSet


                            checked =
                                true


                            if (exactAnswer) {

                                resolvedQuestionIds =
                                    resolvedQuestionIds +
                                            question.questionId
                            }


                        } else if (
                            index <
                            questions.lastIndex
                        ) {

                            /*
                             * Siguiente pregunta.
                             */
                            index++


                            selectedAnswers =
                                emptySet()


                            checked =
                                false


                        } else {

                            /*
                             * Fin de práctica.
                             */
                            onExit(
                                resolvedQuestionIds
                            )
                        }
                    },

                    modifier =
                        Modifier
                            .navigationBarsPadding()
                            .padding(
                                16.dp
                            )
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
                                    CorrectionGreen
                            )
                ) {

                    Text(
                        text =
                            when {

                                !checked ->
                                    "Comprobar respuesta"

                                index <
                                        questions.lastIndex ->
                                    "Siguiente error"

                                else ->
                                    "Terminar práctica"
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
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
                    .statusBarsPadding()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        20.dp
                    )
        ) {


            Text(
                text =
                    "Práctica de errores",

                color =
                    CorrectionDark,

                fontSize =
                    28.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                Text(
                    text =
                        "Error ${index + 1} de ${questions.size}",

                    color =
                        CorrectionGreen,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        14.sp
                )


                Spacer(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )


                if (
                    resolvedQuestionIds.isNotEmpty()
                ) {

                    Text(
                        text =
                            "${resolvedQuestionIds.size} resueltos",

                        color =
                            CorrectionGray,

                        fontSize =
                            12.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )


            /*
             * IMAGEN
             */
            if (
                !question.imageUrl
                    .isNullOrBlank()
            ) {

                AsyncImage(
                    model =
                        question.imageUrl,

                    contentDescription =
                        "Imagen de la pregunta",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                170.dp
                            ),

                    contentScale =
                        ContentScale.Fit
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )
            }


            /*
             * PREGUNTA
             */
            Text(
                text =
                    question.questionText,

                color =
                    CorrectionDark,

                fontSize =
                    21.sp,

                fontWeight =
                    FontWeight.Bold,

                lineHeight =
                    29.sp
            )


            /*
             * AVISO DE SELECCIÓN MÚLTIPLE
             */
            if (multiple) {

                Surface(
                    modifier =
                        Modifier
                            .padding(
                                top = 12.dp
                            )
                            .fillMaxWidth(),

                    color =
                        Color(
                            0xFFFFF8E8
                        ),

                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                ) {

                    Text(
                        text =
                            "⚠ Selecciona todas las alternativas correctas. Si marcas una alternativa incorrecta, la respuesta completa se considera incorrecta.",

                        modifier =
                            Modifier.padding(
                                12.dp
                            ),

                        color =
                            CorrectionOrange,

                        fontSize =
                            13.sp,

                        lineHeight =
                            19.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )


            /*
             * ALTERNATIVAS
             */
            question.alternativas
                .toList()
                .sortedBy {
                    it.first
                }
                .forEach { alternative ->

                    val letter =
                        alternative.first

                    val optionText =
                        alternative.second


                    val selected =
                        selectedAnswers.contains(
                            letter
                        )


                    val isCorrectOption =
                        correctAnswersSet.contains(
                            letter
                        )


                    PracticeOption(
                        letter =
                            letter,

                        text =
                            optionText,

                        selected =
                            selected,

                        checked =
                            checked,

                        correct =
                            isCorrectOption,

                        onClick = {

                            if (!checked) {

                                selectedAnswers =

                                    if (multiple) {

                                        if (selected) {

                                            selectedAnswers -
                                                    letter

                                        } else {

                                            selectedAnswers +
                                                    letter
                                        }

                                    } else {

                                        setOf(
                                            letter
                                        )
                                    }
                            }
                        }
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )
                }


            /*
             * RESULTADO DE ESTA PREGUNTA
             */
            if (checked) {

                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),

                    color =
                        if (answerIsCorrect) {

                            Color(
                                0xFFEAF8F1
                            )

                        } else {

                            Color(
                                0xFFFFF1F0
                            )
                        },

                    border =
                        BorderStroke(
                            1.dp,

                            if (answerIsCorrect) {

                                Color(
                                    0xFFA9DEC9
                                )

                            } else {

                                Color(
                                    0xFFF2B8B5
                                )
                            }
                        ),

                    shape =
                        RoundedCornerShape(
                            16.dp
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
                                if (answerIsCorrect) {

                                    "✓ ¡Respuesta correcta!"

                                } else if (multiple) {

                                    "✕ Respuesta múltiple incorrecta"

                                } else {

                                    "✕ Respuesta incorrecta"
                                },

                            color =
                                if (answerIsCorrect) {

                                    CorrectionGreen

                                } else {

                                    CorrectionRed
                                },

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                16.sp
                        )


                        if (
                            !answerIsCorrect &&
                            multiple
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        6.dp
                                    )
                            )


                            Text(
                                text =
                                    "Debes seleccionar exactamente todas las alternativas correctas y ninguna incorrecta.",

                                color =
                                    CorrectionDark,

                                fontSize =
                                    13.sp,

                                lineHeight =
                                    19.sp
                            )
                        }


                        if (
                            question.explicacion
                                .isNotBlank()
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )


                            Text(
                                text =
                                    question.explicacion,

                                color =
                                    CorrectionDark,

                                fontSize =
                                    14.sp,

                                lineHeight =
                                    21.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
 * ------------------------------------------------
 * ALTERNATIVA DE PRÁCTICA
 * ------------------------------------------------
 */

@Composable
private fun PracticeOption(
    letter: String,
    text: String,
    selected: Boolean,
    checked: Boolean,
    correct: Boolean,
    onClick: () -> Unit
) {

    val background =
        when {

            /*
             * Después de comprobar:
             * mostramos cuáles eran correctas.
             */
            checked &&
                    correct ->

                Color(
                    0xFFEAF8F1
                )


            /*
             * El usuario marcó una opción
             * equivocada.
             */
            checked &&
                    selected &&
                    !correct ->

                Color(
                    0xFFFFF1F0
                )


            /*
             * Selección todavía sin comprobar.
             */
            selected ->

                Color(
                    0xFFEAF1FF
                )


            else ->

                Color.White
        }


    val borderColor =
        when {

            checked &&
                    correct ->

                CorrectionGreen


            checked &&
                    selected &&
                    !correct ->

                CorrectionRed


            selected ->

                CorrectionBlue


            else ->

                Color(
                    0xFFD8E1EF
                )
        }


    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled =
                        !checked
                ) {

                    onClick()
                },

        color =
            background,

        border =
            BorderStroke(
                width =
                    if (selected) {
                        2.dp
                    } else {
                        1.dp
                    },

                color =
                    borderColor
            ),

        shape =
            RoundedCornerShape(
                16.dp
            )
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
                    letter.uppercase(),

                modifier =
                    Modifier.width(
                        32.dp
                    ),

                color =
                    borderColor,

                fontWeight =
                    FontWeight.Bold
            )


            Text(
                text =
                    text,

                modifier =
                    Modifier.weight(
                        1f
                    ),

                color =
                    CorrectionDark,

                fontSize =
                    15.sp
            )


            /*
             * Iconos después de comprobar.
             */
            if (
                checked &&
                correct
            ) {

                Text(
                    text =
                        "✓",

                    color =
                        CorrectionGreen,

                    fontWeight =
                        FontWeight.Bold
                )


            } else if (
                checked &&
                selected &&
                !correct
            ) {

                Text(
                    text =
                        "✕",

                    color =
                        CorrectionRed,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}