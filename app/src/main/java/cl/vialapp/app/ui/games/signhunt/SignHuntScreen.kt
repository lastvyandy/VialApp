package cl.vialapp.app.ui.games.signhunt

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.StudyQuestion
import cl.vialapp.app.data.session.SessionManager
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay


private val HuntBlue = Color(0xFF2563EB)
private val HuntDark = Color(0xFF172033)
private val HuntGray = Color(0xFF64748B)
private val HuntGreen = Color(0xFF16A34A)
private val HuntRed = Color(0xFFDC2626)
private val HuntOrange = Color(0xFFF59E0B)
private val HuntBackground = Color(0xFFF4F7FB)


@Composable
fun SignHuntScreen(
    licenseClass: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sessionManager = remember {
        SessionManager(context)
    }

    val preferences = remember {
        context.getSharedPreferences(
            "vialapp_games",
            android.content.Context.MODE_PRIVATE
        )
    }

    var highScore by remember {
        mutableIntStateOf(
            preferences.getInt(
                "sign_hunt_high_score",
                0
            )
        )
    }

    var isNewRecord by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    var sourceQuestions by remember {
        mutableStateOf<List<StudyQuestion>>(
            emptyList()
        )
    }

    var questions by remember {
        mutableStateOf<List<StudyQuestion>>(
            emptyList()
        )
    }

    var started by remember {
        mutableStateOf(false)
    }

    var finished by remember {
        mutableStateOf(false)
    }

    var introReady by remember {
        mutableStateOf(false)
    }

    var countdownValue by remember {
        mutableIntStateOf(3)
    }

    var gameNonce by remember {
        mutableIntStateOf(0)
    }

    var currentIndex by remember {
        mutableIntStateOf(0)
    }

    var lives by remember {
        mutableIntStateOf(3)
    }

    var score by remember {
        mutableIntStateOf(0)
    }

    var correctCount by remember {
        mutableIntStateOf(0)
    }

    var streak by remember {
        mutableIntStateOf(0)
    }

    var bestStreak by remember {
        mutableIntStateOf(0)
    }

    var secondsLeft by remember {
        mutableIntStateOf(8)
    }

    var answered by remember {
        mutableStateOf(false)
    }

    var selectedAnswer by remember {
        mutableStateOf<String?>(null)
    }

    var answerWasCorrect by remember {
        mutableStateOf(false)
    }

    var timedOut by remember {
        mutableStateOf(false)
    }

    var earnedPoints by remember {
        mutableIntStateOf(0)
    }


    BackHandler {
        onBack()
    }


    /*
     * Buscamos preguntas visuales de señalización
     * entre varios exámenes accesibles del usuario.
     *
     * Así Caza la Señal no depende de que justo el
     * primer examen tenga suficientes señales.
     *
     * Seguimos sin iniciar ExamAttempt ni guardar
     * resultados oficiales.
     */
    LaunchedEffect(licenseClass) {
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

            val testsResponse =
                ApiClient.api.getTests(
                    authorization =
                        "Bearer $token",
                    licenseClass =
                        licenseClass,
                    category =
                        null
                )

            if (!testsResponse.isSuccessful) {
                error =
                    "No pudimos buscar preguntas para Caza la Señal."
                return@LaunchedEffect
            }

            val candidateTests =
                testsResponse
                    .body()
                    ?.tests
                    ?.filter {
                        !it.isLocked
                    }
                    ?.take(6)
                    ?: emptyList()

            val collected =
                mutableListOf<StudyQuestion>()

            for (test in candidateTests) {
                if (collected.size >= 8) {
                    break
                }

                val studyResponse =
                    ApiClient.api.getStudyTest(
                        authorization =
                            "Bearer $token",
                        testId =
                            test.id
                    )

                if (!studyResponse.isSuccessful) {
                    continue
                }

                val compatible =
                    studyResponse
                        .body()
                        ?.preguntas
                        ?.filter { question ->
                            isSignQuestion(question) &&
                                    !question.isMultiple &&
                                    question.correctAnswers.size == 1 &&
                                    question.alternativas.size >= 2 &&
                                    !question.imageUrl.isNullOrBlank()
                        }
                        ?: emptyList()

                collected.addAll(
                    compatible
                )
            }

            val unique =
                collected
                    .distinctBy {
                        it.id
                    }

            if (unique.isEmpty()) {
                error =
                    "No encontramos preguntas visuales de señalización entre tus exámenes accesibles."
            } else {
                sourceQuestions =
                    unique

                questions =
                    unique
                        .shuffled()
                        .take(5)
            }

        } catch (e: Exception) {
            error =
                "No se pudo conectar con VialApp."

            e.printStackTrace()

        } finally {
            loading = false
        }
    }

    fun restartGame() {
        questions =
            sourceQuestions
                .shuffled()
                .take(5)

        currentIndex = 0
        lives = 3
        score = 0
        correctCount = 0
        streak = 0
        bestStreak = 0
        secondsLeft = 8

        answered = false
        selectedAnswer = null
        answerWasCorrect = false
        timedOut = false
        earnedPoints = 0

        countdownValue = 3
        introReady = false
        isNewRecord = false

        finished = false
        started = true
        gameNonce += 1
    }


    fun prepareNextRound() {
        selectedAnswer = null
        answerWasCorrect = false
        timedOut = false
        earnedPoints = 0
        answered = false
        secondsLeft = 8

        currentIndex += 1
    }


    /*
     * Cuenta regresiva SOLO al comenzar la partida.
     */
    LaunchedEffect(
        started,
        gameNonce
    ) {
        if (
            !started ||
            finished ||
            questions.isEmpty()
        ) {
            return@LaunchedEffect
        }

        introReady = false
        countdownValue = 3

        for (number in 3 downTo 1) {
            countdownValue = number
            delay(600)
        }

        countdownValue = 0
        delay(400)

        introReady = true
    }


    /*
     * En este minijuego la velocidad sí forma parte
     * de la mecánica: 8 segundos para "cazar" la señal.
     */
    LaunchedEffect(
        currentIndex,
        introReady,
        answered,
        gameNonce
    ) {
        if (
            !started ||
            finished ||
            !introReady ||
            answered ||
            questions.isEmpty()
        ) {
            return@LaunchedEffect
        }

        secondsLeft = 8

        while (
            secondsLeft > 0 &&
            !answered &&
            !finished
        ) {
            delay(1000)

            secondsLeft =
                (secondsLeft - 1)
                    .coerceAtLeast(0)
        }

        if (
            secondsLeft == 0 &&
            !answered &&
            !finished
        ) {
            timedOut = true
            answerWasCorrect = false
            earnedPoints = 0
            streak = 0

            lives =
                (lives - 1)
                    .coerceAtLeast(0)

            answered = true

            haptic.performHapticFeedback(
                HapticFeedbackType.LongPress
            )
        }
    }


    /*
     * Feedback muy corto y paso automático.
     * Queremos que Caza la Señal tenga ritmo arcade.
     */
    LaunchedEffect(
        answered,
        currentIndex,
        gameNonce
    ) {
        if (
            !answered ||
            finished
        ) {
            return@LaunchedEffect
        }

        delay(
            if (answerWasCorrect) {
                850
            } else {
                1450
            }
        )

        val lastRound =
            currentIndex >=
                    questions.lastIndex

        if (
            lives <= 0 ||
            lastRound
        ) {
            finished = true
        } else {
            prepareNextRound()
        }
    }


    /*
     * Guardamos solamente el récord LOCAL.
     */
    LaunchedEffect(
        finished,
        score
    ) {
        if (
            finished &&
            score > highScore
        ) {
            highScore = score
            isNewRecord = true

            preferences
                .edit()
                .putInt(
                    "sign_hunt_high_score",
                    score
                )
                .apply()
        }
    }


    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HuntBackground),
            contentAlignment =
                Alignment.Center
        ) {
            CircularProgressIndicator(
                color = HuntBlue
            )
        }

        return
    }


    if (error.isNotBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HuntBackground)
                .padding(26.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text = "🛑",
                fontSize = 62.sp
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    "No pudimos montar la pista",
                color =
                    HuntDark,
                fontSize =
                    21.sp,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    error,
                color =
                    HuntGray,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(22.dp)
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

        return
    }


    if (!started) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HuntBackground)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(24.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier =
                    Modifier.height(32.dp)
            )

            Text(
                text = "🛑👀",
                fontSize = 74.sp
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    "Caza la Señal",
                color =
                    HuntDark,
                fontSize =
                    31.sp,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "Mira la imagen, reconoce la señal y responde antes de que se escape.",
                color =
                    HuntGray,
                fontSize =
                    15.sp,
                lineHeight =
                    22.sp,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(26.dp)
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(22.dp),
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
                    HuntRule(
                        emoji = "👀",
                        text =
                            "${questions.size} señales en esta partida"
                    )

                    HuntRule(
                        emoji = "⏱",
                        text =
                            "8 segundos por señal"
                    )

                    HuntRule(
                        emoji = "❤️",
                        text =
                            "Tienes 3 oportunidades"
                    )

                    HuntRule(
                        emoji = "🔥",
                        text =
                            "Las respuestas seguidas aumentan el combo"
                    )

                    HuntRule(
                        emoji = "🏆",
                        text =
                            "Supera tu propio récord"
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            if (highScore > 0) {
                Surface(
                    shape =
                        RoundedCornerShape(16.dp),
                    color =
                        Color(0xFFFFF7E6)
                ) {
                    Text(
                        text =
                            "🏆 Récord: $highScore puntos",
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            ),
                        color =
                            HuntOrange,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Button(
                onClick = {
                    restartGame()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape =
                    RoundedCornerShape(17.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            HuntBlue
                    )
            ) {
                Text(
                    text =
                        "SALIR DE CAZA 🛑",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            TextButton(
                onClick =
                    onBack
            ) {
                Text(
                    "Ahora no"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )
        }

        return
    }


    if (finished) {
        SignHuntResult(
            correct =
                correctCount,
            total =
                questions.size,
            score =
                score,
            bestStreak =
                bestStreak,
            highScore =
                highScore,
            isNewRecord =
                isNewRecord,
            onRestart = {
                restartGame()
            },
            onBack =
                onBack
        )

        return
    }


    if (!introReady) {
        SignHuntCountdown(
            value =
                countdownValue
        )

        return
    }


    val question =
        questions
            .getOrNull(
                currentIndex
            )
            ?: return


    val roundProgress by
    animateFloatAsState(
        targetValue =
            (
                    (currentIndex + 1)
                        .toFloat() /
                            questions.size
                                .toFloat()
                    )
                .coerceIn(
                    0f,
                    1f
                ),
        label =
            "huntRoundProgress"
    )


    val timerProgress by
    animateFloatAsState(
        targetValue =
            (secondsLeft / 8f)
                .coerceIn(
                    0f,
                    1f
                ),
        label =
            "huntTimerProgress"
    )


    val timerColor =
        when {
            secondsLeft <= 2 ->
                HuntRed

            secondsLeft <= 4 ->
                HuntOrange

            else ->
                HuntGreen
        }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HuntBackground)
    ) {

        /*
         * HUD
         */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                TextButton(
                    onClick =
                        onBack,
                    contentPadding =
                        PaddingValues(0.dp)
                ) {
                    Text(
                        text = "✕",
                        color = HuntGray,
                        fontSize = 21.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                LinearProgressIndicator(
                    progress = {
                        roundProgress
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp),
                    color =
                        HuntBlue,
                    trackColor =
                        Color(0xFFE5EAF2)
                )

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Text(
                    text =
                        buildString {
                            repeat(lives) {
                                append("❤️")
                            }

                            repeat(3 - lives) {
                                append("🖤")
                            }
                        },
                    fontSize =
                        15.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        "SEÑAL ${currentIndex + 1}/${questions.size}",
                    color =
                        HuntGray,
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                if (streak >= 2) {
                    Text(
                        text =
                            "🔥 x$streak",
                        color =
                            HuntOrange,
                        fontSize =
                            13.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )
                }

                Text(
                    text =
                        "🏆 $score",
                    color =
                        HuntBlue,
                    fontSize =
                        13.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            LinearProgressIndicator(
                progress = {
                    timerProgress
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                color =
                    timerColor,
                trackColor =
                    Color(0xFFE5EAF2)
            )
        }


        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp)
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
                                if (answered) {
                                    if (answerWasCorrect) {
                                        "¡CAZADA! 🎯"
                                    } else if (timedOut) {
                                        "¡SE ESCAPÓ! 💨"
                                    } else {
                                        "CASI... 👀"
                                    }
                                } else {
                                    "¿QUÉ INDICA?"
                                },
                            color =
                                when {
                                    answerWasCorrect ->
                                        HuntGreen

                                    answered ->
                                        HuntRed

                                    else ->
                                        HuntDark
                                },
                            fontSize =
                                17.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )

                        Text(
                            text =
                                if (answered) {
                                    if (answerWasCorrect) {
                                        "Buena vista."
                                    } else {
                                        "Memorízala para la próxima."
                                    }
                                } else {
                                    "Identifica la señal antes de que llegue a cero."
                                },
                            color =
                                HuntGray,
                            fontSize =
                                12.sp
                        )
                    }

                    Surface(
                        modifier =
                            Modifier.size(58.dp),
                        shape =
                            RoundedCornerShape(18.dp),
                        color =
                            timerColor.copy(
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
                                    "$secondsLeft",
                                color =
                                    timerColor,
                                fontSize =
                                    22.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                /*
                 * La imagen ocupa el protagonismo.
                 */
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape =
                        RoundedCornerShape(26.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            Color(0xFFE1E7F0)
                        )
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        AsyncImage(
                            model =
                                question.imageUrl,
                            contentDescription =
                                "Señal de tránsito",
                            modifier =
                                Modifier.fillMaxSize(),
                            contentScale =
                                ContentScale.Fit
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                /*
                 * Mantenemos el enunciado como apoyo.
                 * Muchas preguntas ya vienen redactadas
                 * específicamente alrededor de la imagen.
                 */
                Text(
                    text =
                        question.texto,
                    color =
                        HuntDark,
                    fontSize =
                        17.sp,
                    fontWeight =
                        FontWeight.Bold,
                    lineHeight =
                        23.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                question
                    .alternativas
                    .toSortedMap()
                    .forEach { (letter, text) ->

                        HuntAnswerOption(
                            letter =
                                letter,
                            text =
                                text,
                            selected =
                                selectedAnswer
                                    ?.equals(
                                        letter,
                                        ignoreCase = true
                                    )
                                        == true,
                            correct =
                                answered &&
                                        isCorrectAnswer(
                                            question,
                                            letter
                                        ),
                            wrong =
                                answered &&
                                        selectedAnswer
                                            ?.equals(
                                                letter,
                                                ignoreCase = true
                                            )
                                        == true &&
                                        !isCorrectAnswer(
                                            question,
                                            letter
                                        ),
                            enabled =
                                !answered,
                            onClick = {
                                if (!answered) {
                                    selectedAnswer =
                                        letter

                                    val correct =
                                        isCorrectAnswer(
                                            question,
                                            letter
                                        )

                                    answerWasCorrect =
                                        correct

                                    timedOut =
                                        false

                                    answered =
                                        true

                                    if (correct) {
                                        val newStreak =
                                            streak + 1

                                        streak =
                                            newStreak

                                        bestStreak =
                                            maxOf(
                                                bestStreak,
                                                newStreak
                                            )

                                        correctCount +=
                                            1

                                        val points =
                                            100 +
                                                    (secondsLeft * 15) +
                                                    (newStreak * 25)

                                        earnedPoints =
                                            points

                                        score +=
                                            points

                                        haptic
                                            .performHapticFeedback(
                                                HapticFeedbackType.TextHandleMove
                                            )

                                    } else {
                                        earnedPoints =
                                            0

                                        streak =
                                            0

                                        lives =
                                            (lives - 1)
                                                .coerceAtLeast(0)

                                        haptic
                                            .performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                    }
                                }
                            }
                        )

                        Spacer(
                            modifier =
                                Modifier.height(9.dp)
                        )
                    }

                if (
                    answered &&
                    !answerWasCorrect
                ) {
                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    val correctLetter =
                        question
                            .correctAnswers
                            .firstOrNull()

                    val correctText =
                        correctLetter
                            ?.let { key ->
                                question
                                    .alternativas
                                    .entries
                                    .firstOrNull {
                                        it.key.equals(
                                            key,
                                            ignoreCase = true
                                        )
                                    }
                                    ?.value
                            }

                    Surface(
                        shape =
                            RoundedCornerShape(16.dp),
                        color =
                            Color(0xFFFFEEEE)
                    ) {
                        Text(
                            text =
                                "Correcta: " +
                                        (correctLetter ?: "") +
                                        ". " +
                                        (correctText ?: ""),
                            modifier =
                                Modifier.padding(13.dp),
                            color =
                                HuntRed,
                            fontSize =
                                13.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )
            }


            /*
             * Popup arcade de puntos.
             *
             * Lo dejamos simple por ahora para no depender
             * del módulo completo compose-animation.
             */
            if (
                answered &&
                answerWasCorrect &&
                earnedPoints > 0
            ) {
                Surface(
                    modifier =
                        Modifier.align(
                            Alignment.Center
                        ),
                    shape =
                        RoundedCornerShape(22.dp),
                    color =
                        HuntGreen
                ) {
                    Text(
                        text =
                            "+$earnedPoints",
                        modifier =
                            Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 14.dp
                            ),
                        color =
                            Color.White,
                        fontSize =
                            28.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}


@Composable
private fun SignHuntCountdown(
    value: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HuntBackground),
        contentAlignment =
            Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    "🛑👀",
                fontSize =
                    76.sp
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text =
                    if (value > 0) {
                        value.toString()
                    } else {
                        "¡CAZA!"
                    },
                color =
                    if (value > 0) {
                        HuntBlue
                    } else {
                        HuntGreen
                    },
                fontSize =
                    if (value > 0) {
                        78.sp
                    } else {
                        48.sp
                    },
                fontWeight =
                    FontWeight.ExtraBold
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    when (value) {
                        3 ->
                            "Abre bien los ojos..."

                        2 ->
                            "Busca los detalles..."

                        1 ->
                            "Que no se te escape."

                        else ->
                            "¡VAMOS!"
                    },
                color =
                    HuntGray,
                fontSize =
                    15.sp,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}


@Composable
private fun HuntAnswerOption(
    letter: String,
    text: String,
    selected: Boolean,
    correct: Boolean,
    wrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background =
        when {
            correct ->
                Color(0xFFEAF8F1)

            wrong ->
                Color(0xFFFFEEEE)

            selected ->
                Color(0xFFEAF1FF)

            else ->
                Color.White
        }

    val borderColor =
        when {
            correct ->
                HuntGreen

            wrong ->
                HuntRed

            selected ->
                HuntBlue

            else ->
                Color(0xFFDCE3EE)
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled =
                    enabled,
                onClick =
                    onClick
            ),
        shape =
            RoundedCornerShape(17.dp),
        color =
            background,
        border =
            BorderStroke(
                width =
                    if (
                        correct ||
                        wrong ||
                        selected
                    ) {
                        2.dp
                    } else {
                        1.dp
                    },
                color =
                    borderColor
            )
    ) {
        Row(
            modifier =
                Modifier.padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Surface(
                modifier =
                    Modifier.size(36.dp),
                shape =
                    RoundedCornerShape(11.dp),
                color =
                    borderColor.copy(
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
                            letter,
                        color =
                            borderColor,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Text(
                text =
                    text,
                modifier =
                    Modifier.weight(1f),
                color =
                    HuntDark,
                fontSize =
                    14.sp,
                lineHeight =
                    19.sp
            )
        }
    }
}


@Composable
private fun HuntRule(
    emoji: String,
    text: String
) {
    Row(
        modifier =
            Modifier.padding(
                vertical = 7.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text =
                emoji,
            fontSize =
                22.sp
        )

        Spacer(
            modifier =
                Modifier.width(12.dp)
        )

        Text(
            text =
                text,
            color =
                HuntDark,
            fontSize =
                14.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}


@Composable
private fun SignHuntResult(
    correct: Int,
    total: Int,
    score: Int,
    bestStreak: Int,
    highScore: Int,
    isNewRecord: Boolean,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val percentage =
        if (total > 0) {
            correct * 100 / total
        } else {
            0
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HuntBackground)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier =
                Modifier.height(34.dp)
        )

        Text(
            text =
                when {
                    percentage >= 100 ->
                        "🛑👁️🔥"

                    percentage >= 80 ->
                        "🛑🎯"

                    else ->
                        "🛑👀"
                },
            fontSize =
                70.sp
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text =
                when {
                    percentage >= 100 ->
                        "¡RADAR HUMANO!"

                    percentage >= 80 ->
                        "¡OJO DE HALCÓN!"

                    percentage >= 60 ->
                        "Buena vista"

                    else ->
                        "Se te escaparon algunas"
                },
            color =
                HuntDark,
            fontSize =
                27.sp,
            fontWeight =
                FontWeight.Bold,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "$correct de $total señales cazadas",
            color =
                HuntGray,
            fontSize =
                15.sp
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

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
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                horizontalArrangement =
                    Arrangement.SpaceAround
            ) {
                HuntResultMetric(
                    value =
                        "$score",
                    label =
                        "Puntos"
                )

                HuntResultMetric(
                    value =
                        "x$bestStreak",
                    label =
                        "Racha"
                )

                HuntResultMetric(
                    value =
                        "$highScore",
                    label =
                        "Récord"
                )
            }
        }

        if (isNewRecord) {
            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Surface(
                shape =
                    RoundedCornerShape(18.dp),
                color =
                    Color(0xFFFFF7E6)
            ) {
                Text(
                    text =
                        "🏆 ¡NUEVO RÉCORD!",
                    modifier =
                        Modifier.padding(
                            horizontal = 18.dp,
                            vertical = 11.dp
                        ),
                    color =
                        HuntOrange,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        Button(
            onClick =
                onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape =
                RoundedCornerShape(17.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        HuntBlue
                )
        ) {
            Text(
                text =
                    "VOLVER A CAZAR 🛑",
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(9.dp)
        )

        OutlinedButton(
            onClick =
                onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
            shape =
                RoundedCornerShape(17.dp)
        ) {
            Text(
                text =
                    "Volver a los juegos"
            )
        }

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )
    }
}


@Composable
private fun HuntResultMetric(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Text(
            text =
                value,
            color =
                HuntDark,
            fontSize =
                20.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                label,
            color =
                HuntGray,
            fontSize =
                11.sp
        )
    }
}


private fun isCorrectAnswer(
    question: StudyQuestion,
    letter: String
): Boolean {
    return question
        .correctAnswers
        .any {
            it.equals(
                letter,
                ignoreCase = true
            )
        }
}


private fun isSignQuestion(
    question: StudyQuestion
): Boolean {
    val text =
        "${question.categoria} ${question.texto}"
            .lowercase()

    return text.contains("señal") ||
            text.contains("senal") ||
            text.contains("señalización") ||
            text.contains("semaforo") ||
            text.contains("semáforo") ||
            text.contains("demarcacion") ||
            text.contains("demarcación") ||
            text.contains("ceda el paso") ||
            text.contains("pare")
}
