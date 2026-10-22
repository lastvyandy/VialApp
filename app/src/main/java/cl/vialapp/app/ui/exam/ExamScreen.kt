package cl.vialapp.app.ui.exam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.ExamAnswer
import cl.vialapp.app.data.api.ExamAttemptResponse
import cl.vialapp.app.data.api.ExamDetailResponse
import cl.vialapp.app.data.api.ExamSubmitResponse
import cl.vialapp.app.data.api.SaveAnswersRequest
import cl.vialapp.app.data.api.SubmitExamRequest
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.data.games.GameProgressStore
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import cl.vialapp.app.data.api.ExamCorrectionResponse


private val VialBlue = Color(0xFF2563EB)
private val VialDark = Color(0xFF172033)
private val VialGray = Color(0xFF64748B)
private val VialBackground = Color(0xFFF4F7FB)


@Composable
fun ExamScreen(
    testId: Int,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val sessionManager =
        remember {
            SessionManager(context)
        }

    val scope =
        rememberCoroutineScope()

    val progressStore =
        remember {
            GameProgressStore(
                context
            )
        }

    /*
     * XP obtenida al finalizar este intento.
     * Se muestra sobre la pantalla de resultado.
     */
    var examXpEarned by remember {
        mutableIntStateOf(0)
    }


    var exam by remember {
        mutableStateOf<ExamDetailResponse?>(
            null
        )
    }

    var attempt by remember {
        mutableStateOf<ExamAttemptResponse?>(
            null
        )
    }

    var result by remember {
        mutableStateOf<ExamSubmitResponse?>(
            null
        )
    }

    var currentQuestionIndex by remember {
        mutableIntStateOf(0)
    }

    var answers by remember {
        mutableStateOf<Map<Int, Set<String>>>(
            emptyMap()
        )
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var submitting by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf("")
    }

    var actionError by remember {
        mutableStateOf("")
    }

    var conflictMessage by remember {
        mutableStateOf("")
    }

    var remainingSeconds by remember {
        mutableLongStateOf(0L)
    }

    var showExitDialog by remember {
        mutableStateOf(false)
    }


    /*
     * CORRECCIÓN DEL EXAMEN
     *
     * Este flujo ya existía antes de agregar XP.
     * Lo restauramos aquí sin mezclarlo con la
     * lógica de gamificación.
     */
    var correction by remember {
        mutableStateOf<ExamCorrectionResponse?>(
            null
        )
    }

    var showCorrection by remember {
        mutableStateOf(false)
    }

    var correctionLoading by remember {
        mutableStateOf(false)
    }

    var correctionError by remember {
        mutableStateOf("")
    }


    /*
     * Mantener acceso a las respuestas
     * más recientes para el envío automático.
     */
    val latestAnswers by
    rememberUpdatedState(
        answers
    )


    /*
     * ENVÍO FINAL DEL EXAMEN.
     */
    suspend fun submitCurrentExam() {

        if (submitting) {
            return
        }

        val currentAttempt =
            attempt ?: return

        submitting = true
        actionError = ""

        try {

            val token =
                sessionManager
                    .getAccessToken()

            if (token == null) {

                actionError =
                    "No se encontró una sesión válida"

                return
            }


            val request =
                SubmitExamRequest(

                    answers =
                        latestAnswers
                            .map { entry ->

                                ExamAnswer(
                                    questionId =
                                        entry.key,

                                    respuestas =
                                        entry.value
                                            .toList()
                                )
                            },

                    attemptId =
                        currentAttempt.attemptId,

                    mode =
                        "exam"
                )


            val response =
                ApiClient.api.submitExam(
                    authorization =
                        "Bearer $token",

                    testId =
                        currentAttempt.testId,

                    request =
                        request
                )


            if (response.isSuccessful) {

                val body =
                    response.body()

                if (body != null) {

                    /*
                     * XP GLOBAL DE VIALAPP
                     *
                     * Solo premiamos cuando el backend confirmó
                     * correctamente la finalización.
                     *
                     * GameProgressStore evita:
                     * - premiar dos veces el mismo attempt_id;
                     * - farmear el mismo test infinitamente.
                     */
                    examXpEarned =
                        progressStore
                            .recordExamCompletion(
                                testId =
                                    currentAttempt.testId,

                                attemptId =
                                    currentAttempt.attemptId
                            )

                    result =
                        body

                    /*
                     * Detiene el cronómetro.
                     */
                    attempt =
                        null

                } else {

                    actionError =
                        "El servidor no devolvió el resultado."
                }

            } else {

                actionError =
                    "No se pudo finalizar el examen (${response.code()})"
            }

        } catch (e: Exception) {

            actionError =
                "No se pudo enviar el examen."

            e.printStackTrace()

        } finally {

            submitting =
                false
        }
    }


    /*
     * CARGAR CORRECCIÓN.
     *
     * Si ya se descargó una vez, reutilizamos
     * el objeto local y abrimos la pantalla
     * inmediatamente.
     */
    suspend fun openCorrection() {

        val currentResult =
            result ?: return

        if (
            correction != null
        ) {
            showCorrection =
                true

            return
        }


        correctionLoading =
            true

        correctionError =
            ""


        try {

            val token =
                sessionManager
                    .getAccessToken()

            if (
                token == null
            ) {

                correctionError =
                    "No se encontró una sesión válida."

                return
            }


            val response =
                ApiClient.api
                    .getExamCorrection(
                        authorization =
                            "Bearer $token",

                        resultId =
                            currentResult.testResultId
                    )


            if (
                response.isSuccessful
            ) {

                val body =
                    response.body()

                if (
                    body != null
                ) {

                    correction =
                        body

                    showCorrection =
                        true

                } else {

                    correctionError =
                        "El servidor no devolvió la corrección."
                }

            } else {

                correctionError =
                    when (
                        response.code()
                    ) {

                        403 ->
                            "La corrección de este examen no está disponible."

                        else ->
                            "No se pudo cargar la corrección (${response.code()})."
                    }
            }

        } catch (
            e: Exception
        ) {

            correctionError =
                "No se pudo cargar la corrección."

            e.printStackTrace()

        } finally {

            correctionLoading =
                false
        }
    }


    /*
     * Si la corrección está abierta,
     * reemplaza temporalmente a la pantalla
     * de resultado.
     */
    val loadedCorrection =
        correction

    if (
        showCorrection &&
        loadedCorrection != null
    ) {

        CorrectionScreen(
            correction =
                loadedCorrection,

            onBack = {

                showCorrection =
                    false
            }
        )

        return
    }


    /*
     * Si ya tenemos resultado,
     * dejamos de mostrar el examen.
     */
    val completedResult =
        result

    if (
        completedResult != null
    ) {

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            ExamResultScreen(
                result =
                    completedResult,

                correctionLoading =
                    correctionLoading,

                correctionError =
                    correctionError,

                onViewCorrection = {

                    scope.launch {

                        openCorrection()
                    }
                },

                onBack =
                    onBack
            )


            if (
                examXpEarned > 0
            ) {

                ExamXpRewardBanner(
                    xp =
                        examXpEarned,

                    modifier =
                        Modifier
                            .align(
                                Alignment.TopCenter
                            )
                            .statusBarsPadding()
                            .padding(
                                top = 10.dp,
                                start = 18.dp,
                                end = 18.dp
                            )
                )
            }
        }

        return
    }


    /*
     * INICIAR / REANUDAR.
     */
    LaunchedEffect(testId) {

        loading =
            true

        error =
            ""

        try {

            val token =
                sessionManager
                    .getAccessToken()

            if (token == null) {

                error =
                    "No se encontró una sesión válida"

                return@LaunchedEffect
            }


            val response =
                ApiClient.api.startExam(
                    authorization =
                        "Bearer $token",

                    testId =
                        testId
                )


            if (response.isSuccessful) {

                val body =
                    response.body()

                if (body == null) {

                    error =
                        "El servidor devolvió una respuesta vacía."

                } else {

                    exam =
                        body.test


                    answers =
                        body.attempt.answers
                            .associate { answer ->

                                answer.questionId to
                                        answer.respuestas
                                            .toSet()
                            }


                    val serverTime =
                        parseServerDate(
                            response
                                .headers()["Date"]
                        )


                    remainingSeconds =
                        calculateInitialRemainingSeconds(
                            attempt =
                                body.attempt,

                            serverNow =
                                serverTime
                        )


                    /*
                     * Lo asignamos después del tiempo
                     * para evitar iniciar en 00:00.
                     */
                    attempt =
                        body.attempt


                    if (body.conflict) {

                        conflictMessage =
                            "Ya tenías un examen activo. Lo hemos reanudado."
                    }
                }

            } else {

                error =
                    "No se pudo iniciar el examen (${response.code()})"
            }

        } catch (e: Exception) {

            error =
                "No se pudo conectar con VialApp"

            e.printStackTrace()

        } finally {

            loading =
                false
        }
    }


    /*
     * CRONÓMETRO.
     */
    LaunchedEffect(
        attempt?.attemptId
    ) {

        if (attempt == null) {
            return@LaunchedEffect
        }


        while (
            remainingSeconds > 0
        ) {

            delay(
                1000
            )

            remainingSeconds =
                (remainingSeconds - 1)
                    .coerceAtLeast(0)
        }


        /*
         * Tiempo terminado:
         * enviamos automáticamente.
         */
        if (
            attempt != null &&
            result == null
        ) {

            submitCurrentExam()
        }
    }


    /*
     * BOTÓN ATRÁS DE ANDROID.
     */
    BackHandler {

        if (!submitting) {

            showExitDialog =
                true
        }
    }


    /*
     * DIÁLOGO DE SALIDA.
     */
    if (
        showExitDialog
    ) {

        ExitExamDialog(

            answered =
                answers.values
                    .count {
                        it.isNotEmpty()
                    },

            total =
                exam
                    ?.preguntas
                    ?.size
                    ?: 0,

            submitting =
                submitting,

            onContinue = {

                showExitDialog =
                    false
            },

            onLeaveAndResume = {

                showExitDialog =
                    false

                onBack()
            },

            onFinish = {

                showExitDialog =
                    false

                scope.launch {

                    submitCurrentExam()
                }
            }
        )
    }


    when {

        loading -> {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator(
                    color =
                        VialBlue
                )
            }
        }


        error.isNotEmpty() -> {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            24.dp
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            error,

                        color =
                            Color(
                                0xFFDC2626
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )


                    Button(
                        onClick =
                            onBack
                    ) {

                        Text(
                            "Volver"
                        )
                    }
                }
            }
        }


        exam != null -> {

            val currentExam =
                exam!!

            val questions =
                currentExam.preguntas


            if (
                questions.isEmpty()
            ) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "Este examen no contiene preguntas."
                    )
                }

            } else {

                val question =
                    questions[
                        currentQuestionIndex
                    ]


                val selectedAnswers =
                    answers[
                        question.id
                    ]
                        ?: emptySet()


                val progress =
                    (
                            currentQuestionIndex +
                                    1
                            ).toFloat() /
                            questions.size
                                .toFloat()


                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                VialBackground
                            )
                            .statusBarsPadding()
                ) {


                    /*
                     * HEADER
                     */
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        18.dp,

                                    vertical =
                                        10.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {


                        TextButton(
                            enabled =
                                !submitting,

                            onClick = {

                                showExitDialog =
                                    true
                            }
                        ) {

                            Text(
                                text =
                                    "← Salir",

                                color =
                                    VialGray
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )


                        TimerText(
                            seconds =
                                remainingSeconds
                        )
                    }


                    /*
                     * PROGRESO.
                     */
                    Box(
                        modifier =
                            Modifier
                                .padding(
                                    horizontal =
                                        20.dp
                                )
                                .fillMaxWidth()
                                .height(
                                    7.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .background(
                                    Color(
                                        0xFFD8E1EF
                                    )
                                )
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(
                                        progress
                                    )
                                    .background(
                                        VialBlue
                                    )
                        )
                    }


                    /*
                     * CONTENIDO.
                     */
                    Column(
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .verticalScroll(
                                    rememberScrollState()
                                )
                                .padding(
                                    horizontal =
                                        20.dp,

                                    vertical =
                                        18.dp
                                )
                    ) {


                        if (
                            conflictMessage
                                .isNotEmpty()
                        ) {

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                shape =
                                    RoundedCornerShape(
                                        16.dp
                                    ),

                                colors =
                                    CardDefaults
                                        .cardColors(
                                            containerColor =
                                                Color(
                                                    0xFFFFF7E6
                                                )
                                        )
                            ) {

                                Text(
                                    text =
                                        conflictMessage,

                                    modifier =
                                        Modifier.padding(
                                            16.dp
                                        ),

                                    color =
                                        Color(
                                            0xFF92400E
                                        )
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        16.dp
                                    )
                            )
                        }


                        if (
                            actionError
                                .isNotEmpty()
                        ) {

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                colors =
                                    CardDefaults
                                        .cardColors(
                                            containerColor =
                                                Color(
                                                    0xFFFFF1F0
                                                )
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
                                            actionError,

                                        color =
                                            Color(
                                                0xFFB42318
                                            )
                                    )


                                    if (
                                        remainingSeconds ==
                                        0L
                                    ) {

                                        TextButton(
                                            onClick = {

                                                scope.launch {

                                                    submitCurrentExam()
                                                }
                                            }
                                        ) {

                                            Text(
                                                "Reintentar finalizar"
                                            )
                                        }
                                    }
                                }
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        14.dp
                                    )
                            )
                        }


                        Text(
                            text =
                                cleanTestTitle(
                                    currentExam.nombre
                                ),

                            color =
                                VialGray,

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.Medium
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        Text(
                            text =
                                "Pregunta ${currentQuestionIndex + 1} de ${questions.size}",

                            color =
                                VialBlue,

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )


                        QuestionImage(
                            imageUrl =
                                question.imageUrl
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    20.dp
                                )
                        )


                        Text(
                            text =
                                question.texto,

                            color =
                                VialDark,

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold,

                            lineHeight =
                                30.sp
                        )


                        if (
                            question.isMultiple
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        10.dp
                                    )
                            )


                            Text(
                                text =
                                    "Selecciona todas las alternativas correctas.",

                                color =
                                    Color(
                                        0xFF9A5B00
                                    ),

                                fontSize =
                                    14.sp
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(
                                    24.dp
                                )
                        )


                        question
                            .alternativas
                            .toList()
                            .sortedBy {
                                it.first
                            }
                            .forEach {
                                    alternative ->

                                val key =
                                    alternative.first

                                val value =
                                    alternative.second


                                AnswerOption(
                                    letter =
                                        key,

                                    text =
                                        value,

                                    selected =
                                        selectedAnswers
                                            .contains(
                                                key
                                            ),

                                    enabled =
                                        remainingSeconds >
                                                0 &&
                                                !submitting,

                                    onClick = {

                                        val newSelection =

                                            if (
                                                question
                                                    .isMultiple
                                            ) {

                                                if (
                                                    selectedAnswers
                                                        .contains(
                                                            key
                                                        )
                                                ) {

                                                    selectedAnswers -
                                                            key

                                                } else {

                                                    selectedAnswers +
                                                            key
                                                }

                                            } else {

                                                setOf(
                                                    key
                                                )
                                            }


                                        answers =
                                            answers +
                                                    (
                                                            question.id to
                                                                    newSelection
                                                            )
                                    }
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            12.dp
                                        )
                                )
                            }
                    }


                    /*
                     * BOTONES INFERIORES.
                     */
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White
                                )
                                .padding(
                                    18.dp
                                ),

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )
                    ) {


                        if (
                            currentQuestionIndex >
                            0
                        ) {

                            OutlinedButton(
                                enabled =
                                    !submitting,

                                onClick = {

                                    currentQuestionIndex--
                                },

                                modifier =
                                    Modifier
                                        .weight(
                                            1f
                                        )
                                        .height(
                                            54.dp
                                        ),

                                shape =
                                    RoundedCornerShape(
                                        16.dp
                                    )
                            ) {

                                Text(
                                    "Anterior"
                                )
                            }
                        }


                        Button(
                            enabled =
                                selectedAnswers
                                    .isNotEmpty() &&
                                        !saving &&
                                        !submitting &&
                                        remainingSeconds >
                                        0,

                            onClick = {

                                scope.launch {

                                    /*
                                     * ÚLTIMA PREGUNTA:
                                     * enviamos directamente.
                                     */
                                    if (
                                        currentQuestionIndex ==
                                        questions.lastIndex
                                    ) {

                                        submitCurrentExam()

                                        return@launch
                                    }


                                    saving =
                                        true

                                    actionError =
                                        ""

                                    try {

                                        saveAnswers(
                                            sessionManager =
                                                sessionManager,

                                            attemptId =
                                                attempt
                                                    ?.attemptId,

                                            answers =
                                                answers
                                        )


                                        currentQuestionIndex++

                                    } catch (
                                        e: Exception
                                    ) {

                                        actionError =
                                            "No se pudo guardar tu respuesta."

                                        e.printStackTrace()

                                    } finally {

                                        saving =
                                            false
                                    }
                                }
                            },

                            modifier =
                                Modifier
                                    .weight(
                                        1f
                                    )
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
                                            VialBlue
                                    )
                        ) {

                            Text(
                                text =
                                    when {

                                        submitting ->
                                            "Finalizando..."

                                        saving ->
                                            "Guardando..."

                                        currentQuestionIndex ==
                                                questions.lastIndex ->
                                            "Finalizar"

                                        else ->
                                            "Siguiente"
                                    },

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ExamXpRewardBanner(
    xp: Int,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                18.dp
            ),
        color =
            Color(
                0xFFEAF8F1
            ),
        shadowElevation =
            5.dp
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 11.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "✨",
                fontSize =
                    20.sp
            )


            Spacer(
                modifier =
                    Modifier.width(
                        9.dp
                    )
            )


            Column {

                Text(
                    text =
                        "+$xp XP",
                    color =
                        Color(
                            0xFF15803D
                        ),
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )


                Text(
                    text =
                        if (
                            xp >= 30
                        ) {
                            "Examen completado"
                        } else {
                            "Bonus de repaso"
                        },
                    color =
                        Color(
                            0xFF166534
                        ),
                    fontSize =
                        11.sp
                )
            }
        }
    }
}


@Composable
private fun ExitExamDialog(
    answered: Int,
    total: Int,
    submitting: Boolean,
    onContinue: () -> Unit,
    onLeaveAndResume: () -> Unit,
    onFinish: () -> Unit
) {

    AlertDialog(

        onDismissRequest = {

            if (!submitting) {
                onContinue()
            }
        },

        title = {

            Text(
                text =
                    "¿Qué quieres hacer?"
            )
        },

        text = {

            Column {

                Text(
                    text =
                        "Has respondido $answered de $total preguntas."
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                Text(
                    text =
                        "Si finalizas ahora, las preguntas sin responder se considerarán incorrectas."
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )


                OutlinedButton(
                    enabled =
                        !submitting,

                    onClick =
                        onLeaveAndResume,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Salir y continuar después"
                    )
                }


                Text(
                    text =
                        "El cronómetro seguirá avanzando aunque salgas.",

                    color =
                        VialGray,

                    fontSize =
                        12.sp
                )
            }
        },

        confirmButton = {

            Button(
                enabled =
                    !submitting,

                onClick =
                    onFinish
            ) {

                Text(
                    if (submitting) {
                        "Finalizando..."
                    } else {
                        "Finalizar examen"
                    }
                )
            }
        },

        dismissButton = {

            TextButton(
                enabled =
                    !submitting,

                onClick =
                    onContinue
            ) {

                Text(
                    "Seguir respondiendo"
                )
            }
        }
    )
}


private fun cleanTestTitle(
    name: String
): String {

    return name
        .replace(
            Regex(
                "\\s\\d{8}-\\d{4}$"
            ),
            ""
        )
        .trim()
}


private fun parseServerDate(
    value: String?
): Instant? {

    if (
        value.isNullOrBlank()
    ) {
        return null
    }


    return runCatching {

        ZonedDateTime
            .parse(
                value,
                DateTimeFormatter
                    .RFC_1123_DATE_TIME
            )
            .toInstant()

    }.getOrNull()
}


private fun parseExamTimestamp(
    value: String?
): Instant? {

    if (
        value.isNullOrBlank()
    ) {
        return null
    }


    val raw =
        value.trim()


    val hasTimezone =
        raw.endsWith(
            "Z",
            ignoreCase = true
        ) ||
                Regex(
                    "[+-]\\d{2}:?\\d{2}$"
                ).containsMatchIn(
                    raw
                )


    val normalized =
        if (hasTimezone) {
            raw
        } else {
            "${raw}Z"
        }


    runCatching {

        return Instant.parse(
            normalized
        )
    }


    runCatching {

        return OffsetDateTime
            .parse(raw)
            .toInstant()
    }


    runCatching {

        return LocalDateTime
            .parse(raw)
            .toInstant(
                ZoneOffset.UTC
            )
    }


    return null
}


private fun calculateInitialRemainingSeconds(
    attempt: ExamAttemptResponse,
    serverNow: Instant?
): Long {

    val expiration =
        parseExamTimestamp(
            attempt.expiresAt
        )


    if (
        expiration != null
    ) {

        val reference =
            serverNow
                ?: Instant.now()


        val milliseconds =
            Duration
                .between(
                    reference,
                    expiration
                )
                .toMillis()


        if (
            milliseconds > 0
        ) {

            return (
                    (milliseconds + 999) /
                            1000
                    )
                .coerceAtMost(
                    attempt
                        .durationSeconds
                        .toLong()
                )
        }
    }


    return attempt
        .durationSeconds
        .toLong()
}


private suspend fun saveAnswers(
    sessionManager: SessionManager,
    attemptId: Int?,
    answers: Map<Int, Set<String>>
) {

    if (
        attemptId == null
    ) {
        return
    }


    val token =
        sessionManager
            .getAccessToken()
            ?: return


    val request =
        SaveAnswersRequest(

            answers =
                answers.map {
                        entry ->

                    ExamAnswer(
                        questionId =
                            entry.key,

                        respuestas =
                            entry.value
                                .toList()
                    )
                }
        )


    val response =
        ApiClient.api
            .saveExamAnswers(
                authorization =
                    "Bearer $token",

                attemptId =
                    attemptId,

                request =
                    request
            )


    if (
        !response.isSuccessful
    ) {

        throw IllegalStateException(
            "No se pudieron guardar las respuestas (${response.code()})"
        )
    }
}


@Composable
private fun QuestionImage(
    imageUrl: String?
) {

    var imageFailed by remember(
        imageUrl
    ) {

        mutableStateOf(false)
    }


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    170.dp
                ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        Color.White
                ),

        border =
            BorderStroke(
                1.dp,
                Color(
                    0xFFD8E1EF
                )
            )
    ) {


        if (
            imageUrl.isNullOrBlank() ||
            imageFailed
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color(
                                0xFFF1F5F9
                            )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        "🚦 Imagen no disponible",

                    color =
                        VialGray
                )
            }

        } else {

            Box(
                modifier =
                    Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {


                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            28.dp
                        ),

                    color =
                        VialBlue
                )


                AsyncImage(
                    model =
                        imageUrl,

                    contentDescription =
                        "Imagen de la pregunta",

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                6.dp
                            ),

                    contentScale =
                        ContentScale.Fit,

                    onError = {

                        imageFailed =
                            true
                    }
                )
            }
        }
    }
}


@Composable
private fun AnswerOption(
    letter: String,
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {

    val background =
        if (selected) {

            Color(
                0xFFEAF1FF
            )

        } else {

            Color.White
        }


    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled =
                        enabled
                ) {
                    onClick()
                },

        shape =
            RoundedCornerShape(
                18.dp
            ),

        color =
            background,

        border =
            BorderStroke(
                if (selected) {
                    2.dp
                } else {
                    1.dp
                },

                if (selected) {
                    VialBlue
                } else {
                    Color(
                        0xFFD8E1EF
                    )
                }
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


            Surface(
                shape =
                    RoundedCornerShape(
                        10.dp
                    ),

                color =
                    if (selected) {

                        VialBlue

                    } else {

                        Color(
                            0xFFF1F5F9
                        )
                    }
            ) {

                Box(
                    modifier =
                        Modifier.size(
                            38.dp
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            letter.uppercase(),

                        color =
                            if (selected) {

                                Color.White

                            } else {

                                VialDark
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            Text(
                text =
                    text,

                modifier =
                    Modifier
                        .padding(
                            start =
                                14.dp
                        )
                        .weight(
                            1f
                        ),

                color =
                    VialDark,

                fontSize =
                    16.sp,

                lineHeight =
                    22.sp
            )
        }
    }
}


@Composable
private fun TimerText(
    seconds: Long
) {

    val minutes =
        seconds / 60

    val remaining =
        seconds % 60


    val text =
        String.format(
            "%02d:%02d",
            minutes,
            remaining
        )


    val urgent =
        seconds <= 300


    Surface(
        color =
            if (urgent) {

                Color(
                    0xFFFFF1F0
                )

            } else {

                Color.White
            },

        shape =
            RoundedCornerShape(
                30.dp
            ),

        border =
            BorderStroke(
                1.dp,

                if (urgent) {

                    Color(
                        0xFFF2B8B5
                    )

                } else {

                    Color(
                        0xFFC8D8F8
                    )
                }
            )
    ) {

        Text(
            text =
                "⏱ $text",

            modifier =
                Modifier.padding(
                    horizontal =
                        12.dp,

                    vertical =
                        7.dp
                ),

            color =
                if (urgent) {

                    Color(
                        0xFFB42318
                    )

                } else {

                    VialBlue
                },

            fontWeight =
                FontWeight.Bold,

            fontSize =
                14.sp
        )
    }
}
