package cl.vialapp.app.ui.games.baldchallenge

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
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
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay


private val GameBlue = Color(0xFF2563EB)
private val GameDark = Color(0xFF172033)
private val GameGray = Color(0xFF64748B)
private val GameGreen = Color(0xFF16A34A)
private val GameRed = Color(0xFFDC2626)
private val GameOrange = Color(0xFFF59E0B)
private val GamePurple = Color(0xFF7C3AED)
private val GameBackground = Color(0xFFF4F7FB)


@Composable
fun BaldChallengeScreen(
    testId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sessionManager = remember {
        SessionManager(context)
    }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    var sourceQuestions by remember {
        mutableStateOf<List<StudyQuestion>>(emptyList())
    }

    var questions by remember {
        mutableStateOf<List<StudyQuestion>>(emptyList())
    }

    var started by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    var currentIndex by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }

    var secondsLeft by remember { mutableIntStateOf(10) }
    var speedBonusExpired by remember { mutableStateOf(false) }

    var roundReady by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }

    var answered by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var answerWasCorrect by remember { mutableStateOf(false) }
    var earnedPoints by remember { mutableIntStateOf(0) }

    var pelaoPower by remember { mutableIntStateOf(0) }
    var powerModeArmed by remember { mutableStateOf(false) }
    var powerModeActive by remember { mutableStateOf(false) }

    /*
     * FEEDBACK ARCADE
     */
    var showPointsBurst by remember { mutableStateOf(false) }
    var showPowerBurst by remember { mutableStateOf(false) }
    var showStreakPunch by remember { mutableStateOf(false) }
    var wrongFeedbackNonce by remember { mutableIntStateOf(0) }

    val shakeOffset = remember {
        Animatable(0f)
    }

    val questionEntrance = remember {
        Animatable(1f)
    }

    /*
     * Progreso local compartido entre minijuegos.
     *
     * Más adelante podremos sincronizar estos datos
     * con backend sin cambiar la lógica del juego.
     */
    val progressStore = remember {
        GameProgressStore(context)
    }

    var highScore by remember {
        mutableIntStateOf(
            progressStore.getHighScore(
                GameProgressStore.BALD_CHALLENGE
            )
        )
    }

    var isNewRecord by remember {
        mutableStateOf(false)
    }

    var xpEarned by remember {
        mutableIntStateOf(0)
    }


    BackHandler {
        onBack()
    }


    /*
     * CARGAR PREGUNTAS REALES
     *
     * Usamos el modo estudio solamente como fuente.
     * Este minijuego NO crea ExamAttempt,
     * NO envía submit y NO modifica TestResult.
     */
    LaunchedEffect(testId) {
        loading = true
        error = ""

        try {
            val token = sessionManager.getAccessToken()

            if (token == null) {
                error = "No se encontró una sesión válida."
                return@LaunchedEffect
            }

            val response = ApiClient.api.getStudyTest(
                authorization = "Bearer $token",
                testId = testId
            )

            if (response.isSuccessful) {
                val body = response.body()

                val usable = body
                    ?.preguntas
                    ?.filter { question ->
                        !question.isMultiple &&
                                question.correctAnswers.size == 1 &&
                                question.alternativas.size >= 2
                    }
                    ?: emptyList()

                if (usable.isEmpty()) {
                    error =
                        "Este examen no tiene preguntas compatibles con este minijuego."
                } else {
                    sourceQuestions = usable
                    questions = usable.shuffled().take(5)
                }

            } else {
                error =
                    "No se pudieron cargar las preguntas del reto (${response.code()})."
            }

        } catch (e: Exception) {
            error = "No se pudo conectar con VialApp."
            e.printStackTrace()

        } finally {
            loading = false
        }
    }


    /*
     * CUENTA REGRESIVA
     *
     * Solo:
     * 1) antes de la primera pregunta
     * 2) antes de la última pregunta (BOSS)
     *
     * Las preguntas del medio aparecen inmediatamente.
     */
    LaunchedEffect(
        started,
        currentIndex,
        finished,
        questions
    ) {
        if (
            !started ||
            finished ||
            questions.isEmpty()
        ) {
            return@LaunchedEffect
        }

        val needsCountdown =
            currentIndex == 0 ||
                    currentIndex == questions.lastIndex

        if (!needsCountdown) {
            roundReady = true
            return@LaunchedEffect
        }

        roundReady = false
        countdownValue = 3
        secondsLeft = 10
        speedBonusExpired = false

        for (number in 3 downTo 1) {
            countdownValue = number
            delay(650)
        }

        countdownValue = 0
        delay(450)

        roundReady = true
    }


    /*
     * CRONÓMETRO DE VELOCIDAD
     *
     * Llegar a 0 NO quita una vida y NO responde
     * automáticamente la pregunta.
     * Solo se pierde el bonus por velocidad.
     */
    LaunchedEffect(
        started,
        currentIndex,
        answered,
        finished,
        roundReady
    ) {
        if (
            !started ||
            finished ||
            answered ||
            questions.isEmpty() ||
            !roundReady
        ) {
            return@LaunchedEffect
        }

        secondsLeft = 10
        speedBonusExpired = false

        while (
            secondsLeft > 0 &&
            !answered &&
            !finished &&
            roundReady
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
            speedBonusExpired = true

            haptic.performHapticFeedback(
                HapticFeedbackType.TextHandleMove
            )
        }
    }


    /*
     * Entrada suave de cada nueva pregunta.
     */
    LaunchedEffect(
        currentIndex,
        roundReady
    ) {
        if (
            started &&
            roundReady &&
            !finished
        ) {
            questionEntrance.snapTo(0f)

            questionEntrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 260
                )
            )
        }
    }


    /*
     * Sacudida breve al equivocarse.
     */
    LaunchedEffect(
        wrongFeedbackNonce
    ) {
        if (wrongFeedbackNonce == 0) {
            return@LaunchedEffect
        }

        shakeOffset.snapTo(0f)

        listOf(
            -14f,
            14f,
            -10f,
            10f,
            -6f,
            6f,
            0f
        ).forEach { target ->

            shakeOffset.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = 42
                )
            )
        }
    }


    /*
     * Los popups arcade desaparecen solos.
     */
    LaunchedEffect(
        showPointsBurst
    ) {
        if (showPointsBurst) {
            delay(850)
            showPointsBurst = false
        }
    }


    LaunchedEffect(
        showPowerBurst
    ) {
        if (showPowerBurst) {
            delay(1100)
            showPowerBurst = false
        }
    }


    LaunchedEffect(
        showStreakPunch
    ) {
        if (showStreakPunch) {
            delay(180)
            showStreakPunch = false
        }
    }


    val streakScale by animateFloatAsState(
        targetValue =
            if (showStreakPunch) {
                1.35f
            } else {
                1f
            },
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 500f
        ),
        label = "streakPunch"
    )


    fun finishGame() {
        val earnedXp =
            20 +
                    (correctCount * 10) +
                    (bestStreak * 3) +
                    if (
                        correctCount ==
                        questions.size
                    ) {
                        25
                    } else {
                        0
                    }

        xpEarned =
            earnedXp

        val newRecord =
            progressStore.recordRun(
                gameId =
                    GameProgressStore.BALD_CHALLENGE,
                score =
                    score,
                xp =
                    earnedXp
            )

        highScore =
            progressStore.getHighScore(
                GameProgressStore.BALD_CHALLENGE
            )

        isNewRecord =
            newRecord

        finished =
            true
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

        secondsLeft = 10
        speedBonusExpired = false

        roundReady = false
        countdownValue = 3

        answered = false
        selectedAnswer = null
        answerWasCorrect = false
        earnedPoints = 0

        pelaoPower = 0
        powerModeArmed = false
        powerModeActive = false

        showPointsBurst = false
        showPowerBurst = false
        showStreakPunch = false
        wrongFeedbackNonce = 0
        isNewRecord = false
        xpEarned = 0

        finished = false
        started = true
    }


    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GameBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = GameBlue
            )
        }

        return
    }


    if (error.isNotBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GameBackground)
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "👨‍🦲",
                fontSize = 56.sp
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "El Pelao no pudo preparar el entrenamiento",
                color = GameDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = error,
                color = GameGray,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Button(
                onClick = onBack
            ) {
                Text("Volver")
            }
        }

        return
    }


    if (!started) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GameBackground)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier.height(34.dp)
            )

            Text(
                text = "👨‍🦲💪",
                fontSize = 76.sp
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "Reto del Pelao",
                color = GameDark,
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "\"${questions.size} preguntas. Tres vidas. Demuéstrame qué sabes.\"",
                color = GameGray,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(26.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    GameRule(
                        emoji = "⚡",
                        text = "10 segundos para conseguir el bonus de velocidad"
                    )

                    GameRule(
                        emoji = "❤️",
                        text = "Solo una respuesta incorrecta te quita una vida"
                    )

                    GameRule(
                        emoji = "🔥",
                        text = "Encadena aciertos para aumentar tus puntos"
                    )

                    GameRule(
                        emoji = "💪",
                        text = "Llena Pelao Power para activar una ronda x2"
                    )

                    GameRule(
                        emoji = "🏁",
                        text = "La última pregunta es una ronda BOSS"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Button(
                onClick = {
                    restartGame()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameBlue
                )
            ) {
                Text(
                    text = "ACEPTO EL RETO 💪",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TextButton(
                onClick = onBack
            ) {
                Text("Ahora no")
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }

        return
    }


    if (finished) {
        BaldChallengeResult(
            correct = correctCount,
            total = questions.size,
            score = score,
            bestStreak = bestStreak,
            highScore = highScore,
            isNewRecord = isNewRecord,
            xpEarned = xpEarned,
            onRestart = {
                restartGame()
            },
            onBack = onBack
        )

        return
    }


    val isBossRound =
        questions.isNotEmpty() &&
                currentIndex == questions.lastIndex


    if (
        !roundReady &&
        (
                currentIndex == 0 ||
                        isBossRound
                )
    ) {
        CountdownScreen(
            value = countdownValue,
            round = currentIndex + 1,
            total = questions.size,
            boss = isBossRound
        )

        return
    }


    val question =
        questions.getOrNull(currentIndex)
            ?: return


    val progress by animateFloatAsState(
        targetValue =
            (
                    (currentIndex + 1).toFloat() /
                            questions.size.toFloat()
                    )
                .coerceIn(0f, 1f),
        label = "gameProgress"
    )


    val timerColor by animateColorAsState(
        targetValue =
            when {
                speedBonusExpired -> GameGray
                secondsLeft <= 3 -> GameRed
                secondsLeft <= 6 -> GameOrange
                else -> GameGreen
            },
        label = "timerColor"
    )


    val coachTitle =
        when {
            answered && answerWasCorrect && powerModeArmed ->
                "¡PELAO POWER LISTO!"

            answered && answerWasCorrect ->
                "¡ESO! SIGUE ASÍ."

            answered ->
                "ESA DOLIÓ."

            powerModeActive ->
                "⚡ MODO PELAO"

            speedBonusExpired ->
                "SIN BONUS, PERO SIGUE."

            isBossRound ->
                "RONDA FINAL."

            else ->
                "¡RÁPIDO!"
        }


    val coachSubtitle =
        when {
            answered && answerWasCorrect && powerModeArmed ->
                "La siguiente pregunta vale x2."

            answered && answerWasCorrect ->
                "El Pelao aprueba."

            answered ->
                "Apréndela y seguimos."

            powerModeActive ->
                "Esta respuesta vale el doble."

            speedBonusExpired ->
                "Todavía puedes responder."

            isBossRound ->
                "Una última. Concéntrate."

            else ->
                "El bonus de velocidad está corriendo."
        }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
    ) {

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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "✕",
                        color = GameGray,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.width(9.dp)
                )

                LinearProgressIndicator(
                    progress = {
                        progress
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp),
                    color =
                        if (isBossRound) {
                            GameOrange
                        } else {
                            GameBlue
                        },
                    trackColor = Color(0xFFE5EAF2)
                )

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Text(
                    text = buildString {
                        repeat(lives) {
                            append("❤️")
                        }

                        repeat(3 - lives) {
                            append("🖤")
                        }
                    },
                    fontSize = 15.sp
                )
            }

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text =
                        if (isBossRound) {
                            "⚠️ BOSS"
                        } else {
                            "RONDA ${currentIndex + 1}/${questions.size}"
                        },
                    color =
                        if (isBossRound) {
                            GameOrange
                        } else {
                            GameGray
                        },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                if (streak >= 2) {
                    Text(
                        text = "🔥 x$streak",
                        modifier = Modifier.graphicsLayer {
                            scaleX = streakScale
                            scaleY = streakScale
                        },
                        color = GameOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier = Modifier.width(13.dp)
                    )
                }

                if (powerModeActive) {
                    Text(
                        text = "⚡ x2",
                        color = GamePurple,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier = Modifier.width(13.dp)
                    )
                }

                AnimatedVisibility(
                    visible =
                        showPointsBurst &&
                                earnedPoints > 0,
                    enter =
                        fadeIn(
                            animationSpec = tween(100)
                        ) +
                                scaleIn(
                                    initialScale = 0.55f
                                ),
                    exit =
                        fadeOut(
                            animationSpec = tween(220)
                        ) +
                                scaleOut(
                                    targetScale = 1.25f
                                )
                ) {
                    Text(
                        text = "+$earnedPoints",
                        color = GameGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                if (
                    showPointsBurst &&
                    earnedPoints > 0
                ) {
                    Spacer(
                        modifier = Modifier.width(9.dp)
                    )
                }

                Text(
                    text = "🏆 $score",
                    color = GameBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            PelaoPowerBar(
                power = pelaoPower,
                active = powerModeActive,
                armed = powerModeArmed
            )

            AnimatedVisibility(
                visible = showPowerBurst,
                enter =
                    fadeIn(
                        animationSpec = tween(120)
                    ) +
                            scaleIn(
                                initialScale = 0.82f
                            ),
                exit =
                    fadeOut(
                        animationSpec = tween(240)
                    ) +
                            scaleOut(
                                targetScale = 1.08f
                            )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 10.dp
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF2EAFE)
                ) {
                    Text(
                        text =
                            "⚡ PELAO POWER CARGADO · PRÓXIMA RONDA x2",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 9.dp
                        ),
                        color = GamePurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }


        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    alpha =
                        questionEntrance.value

                    val scale =
                        0.985f +
                                (
                                        0.015f *
                                                questionEntrance.value
                                        )

                    scaleX = scale
                    scaleY = scale
                    translationX =
                        shakeOffset.value
                }
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color =
                        if (powerModeActive) {
                            Color(0xFFF2EAFE)
                        } else if (isBossRound) {
                            Color(0xFFFFF3E0)
                        } else {
                            Color(0xFFEAF1FF)
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                when {
                                    powerModeActive -> "👨‍🦲⚡💪"
                                    isBossRound -> "👨‍🦲🔥"
                                    else -> "👨‍🦲💪"
                                },
                            fontSize = 27.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = coachTitle,
                        color =
                            if (powerModeActive) {
                                GamePurple
                            } else {
                                GameDark
                            },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Text(
                        text = coachSubtitle,
                        color = GameGray,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    color =
                        timerColor.copy(
                            alpha = 0.12f
                        )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text =
                                if (speedBonusExpired) {
                                    "×0"
                                } else {
                                    "$secondsLeft"
                                },
                            color = timerColor,
                            fontSize =
                                if (speedBonusExpired) {
                                    16.sp
                                } else {
                                    22.sp
                                },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (
                speedBonusExpired &&
                !answered
            ) {
                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text =
                            "⏱ Perdiste el bonus de velocidad, pero todavía puedes responder.",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        color = GameGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(23.dp)
            )

            Text(
                text = question.texto,
                color = GameDark,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )


            if (!question.imageUrl.isNullOrBlank()) {
                Spacer(
                    modifier = Modifier.height(17.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = 160.dp,
                            max = 250.dp
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    AsyncImage(
                        model = question.imageUrl,
                        contentDescription =
                            "Imagen de la pregunta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = 160.dp,
                                max = 250.dp
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )


            question.alternativas
                .toSortedMap()
                .forEach { (letter, text) ->

                    GameAnswerOption(
                        letter = letter,
                        text = text,
                        selected =
                            selectedAnswer == letter,
                        correct =
                            answered &&
                                    question.correctAnswers
                                        .contains(letter),
                        wrong =
                            answered &&
                                    selectedAnswer == letter &&
                                    !question.correctAnswers
                                        .contains(letter),
                        enabled = !answered,
                        onClick = {
                            if (answered) {
                                return@GameAnswerOption
                            }

                            selectedAnswer = letter

                            val correct =
                                question.correctAnswers.toSet() ==
                                        setOf(letter)

                            answerWasCorrect = correct
                            answered = true

                            if (correct) {
                                val newStreak =
                                    streak + 1

                                streak = newStreak

                                bestStreak =
                                    maxOf(
                                        bestStreak,
                                        newStreak
                                    )

                                correctCount += 1

                                val basePoints = 100

                                val timeBonus =
                                    if (speedBonusExpired) {
                                        0
                                    } else {
                                        secondsLeft * 10
                                    }

                                val streakBonus =
                                    newStreak * 20

                                val multiplier =
                                    if (powerModeActive) {
                                        2
                                    } else {
                                        1
                                    }

                                val points =
                                    (
                                            basePoints +
                                                    timeBonus +
                                                    streakBonus
                                            ) * multiplier

                                earnedPoints = points
                                score += points

                                showPointsBurst = true

                                if (newStreak >= 2) {
                                    showStreakPunch = true
                                }

                                if (powerModeActive) {
                                    powerModeActive = false
                                    pelaoPower = 25

                                } else {
                                    val newPower =
                                        (pelaoPower + 25)
                                            .coerceAtMost(100)

                                    pelaoPower = newPower

                                    if (
                                        newPower >= 100 &&
                                        !powerModeArmed
                                    ) {
                                        powerModeArmed = true
                                        showPowerBurst = true
                                    }
                                }

                                haptic.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove
                                )

                            } else {
                                earnedPoints = 0
                                streak = 0
                                wrongFeedbackNonce += 1

                                lives =
                                    (lives - 1)
                                        .coerceAtLeast(0)

                                if (powerModeActive) {
                                    powerModeActive = false
                                    pelaoPower = 0
                                }

                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                            }
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }


            if (answered) {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (answerWasCorrect) {
                                Color(0xFFEAF8F1)
                            } else {
                                Color(0xFFFFEEEE)
                            }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(17.dp)
                    ) {
                        Text(
                            text =
                                if (answerWasCorrect) {
                                    "✅ ¡Correcto!"
                                } else {
                                    "❌ No era esa"
                                },
                            color =
                                if (answerWasCorrect) {
                                    GameGreen
                                } else {
                                    GameRed
                                },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        if (
                            answerWasCorrect &&
                            earnedPoints > 0
                        ) {
                            Spacer(
                                modifier = Modifier.height(5.dp)
                            )

                            Text(
                                text = "+$earnedPoints puntos",
                                color = GameGreen,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (
                            answerWasCorrect &&
                            powerModeArmed
                        ) {
                            Spacer(
                                modifier = Modifier.height(9.dp)
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF2EAFE)
                            ) {
                                Text(
                                    text =
                                        "⚡ ¡PELAO POWER! La siguiente pregunta vale x2.",
                                    modifier = Modifier.padding(
                                        horizontal = 11.dp,
                                        vertical = 8.dp
                                    ),
                                    color = GamePurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!answerWasCorrect) {
                            Spacer(
                                modifier = Modifier.height(7.dp)
                            )

                            val correctLetter =
                                question.correctAnswers
                                    .firstOrNull()

                            val correctText =
                                correctLetter
                                    ?.let {
                                        question.alternativas[it]
                                    }

                            Text(
                                text =
                                    "Respuesta correcta: " +
                                            "$correctLetter. " +
                                            (correctText ?: ""),
                                color = GameDark,
                                fontWeight =
                                    FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        if (
                            question.explicacion
                                .isNotBlank()
                        ) {
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = question.explicacion,
                                color = GameGray,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(17.dp)
                )

                Button(
                    onClick = {
                        if (
                            lives <= 0 ||
                            currentIndex >=
                            questions.lastIndex
                        ) {
                            finishGame()

                        } else {
                            val nextIndex =
                                currentIndex + 1

                            if (powerModeArmed) {
                                powerModeActive = true
                                powerModeArmed = false
                            }

                            earnedPoints = 0
                            selectedAnswer = null
                            answerWasCorrect = false
                            answered = false

                            secondsLeft = 10
                            speedBonusExpired = false

                            roundReady =
                                nextIndex !=
                                        questions.lastIndex

                            countdownValue = 3
                            currentIndex = nextIndex
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (isBossRound) {
                                GameOrange
                            } else {
                                GameBlue
                            }
                    )
                ) {
                    Text(
                        text =
                            if (
                                lives <= 0 ||
                                currentIndex >=
                                questions.lastIndex
                            ) {
                                "VER RESULTADO"
                            } else if (
                                currentIndex + 1 ==
                                questions.lastIndex
                            ) {
                                "IR AL BOSS 🔥"
                            } else {
                                "SIGUIENTE →"
                            },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }
    }
}


@Composable
private fun CountdownScreen(
    value: Int,
    round: Int,
    total: Int,
    boss: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (boss) {
                    Color(0xFFFFF8EB)
                } else {
                    GameBackground
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    if (boss) {
                        "⚠️ RONDA FINAL"
                    } else {
                        "RONDA $round/$total"
                    },
                color =
                    if (boss) {
                        GameOrange
                    } else {
                        GameGray
                    },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text =
                    if (boss) {
                        "👨‍🦲🔥💪"
                    } else {
                        "👨‍🦲💪"
                    },
                fontSize = 74.sp
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text =
                    if (value > 0) {
                        value.toString()
                    } else if (boss) {
                        "¡BOSS!"
                    } else {
                        "¡YA!"
                    },
                color =
                    if (boss) {
                        GameOrange
                    } else if (value > 0) {
                        GameBlue
                    } else {
                        GameGreen
                    },
                fontSize =
                    if (value > 0) {
                        76.sp
                    } else {
                        52.sp
                    },
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text =
                    when {
                        boss && value == 0 ->
                            "UNA ÚLTIMA. NO LA ARRUINES."

                        value == 3 ->
                            "Prepárate..."

                        value == 2 ->
                            "Concéntrate..."

                        value == 1 ->
                            "No pestañees."

                        else ->
                            "¡DEMUESTRA LO QUE SABES!"
                    },
                color = GameGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun PelaoPowerBar(
    power: Int,
    active: Boolean,
    armed: Boolean
) {
    val shownPower =
        if (
            active ||
            armed
        ) {
            100
        } else {
            power
        }

    val animatedPower by animateFloatAsState(
        targetValue =
            (shownPower / 100f)
                .coerceIn(0f, 1f),
        label = "pelaoPower"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text =
                    if (active) {
                        "⚡ PELAO POWER x2"
                    } else {
                        "💪 PELAO POWER"
                    },
                color =
                    if (
                        active ||
                        armed
                    ) {
                        GamePurple
                    } else {
                        GameGray
                    },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text =
                    if (active) {
                        "ACTIVO"
                    } else if (armed) {
                        "LISTO"
                    } else {
                        "$shownPower%"
                    },
                color =
                    if (
                        active ||
                        armed
                    ) {
                        GamePurple
                    } else {
                        GameGray
                    },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        LinearProgressIndicator(
            progress = {
                animatedPower
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color =
                if (
                    active ||
                    armed
                ) {
                    GamePurple
                } else {
                    GameBlue
                },
            trackColor = Color(0xFFE5EAF2)
        )
    }
}


@Composable
private fun GameAnswerOption(
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
            correct -> Color(0xFFEAF8F1)
            wrong -> Color(0xFFFFEEEE)
            selected -> Color(0xFFEAF1FF)
            else -> Color.White
        }

    val borderColor =
        when {
            correct -> GameGreen
            wrong -> GameRed
            selected -> GameBlue
            else -> Color(0xFFDCE3EE)
        }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = background,
        border = BorderStroke(
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
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp),
                color =
                    borderColor.copy(
                        alpha = 0.12f
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        color = borderColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(13.dp)
            )

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                color = GameDark,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}


@Composable
private fun GameRule(
    emoji: String,
    text: String
) {
    Row(
        modifier = Modifier.padding(
            vertical = 7.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = text,
            color = GameDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
private fun BaldChallengeResult(
    correct: Int,
    total: Int,
    score: Int,
    bestStreak: Int,
    highScore: Int,
    isNewRecord: Boolean,
    xpEarned: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val percentage =
        if (total > 0) {
            correct * 100 / total
        } else {
            0
        }

    val stars =
        when {
            percentage >= 100 -> 5
            percentage >= 80 -> 4
            percentage >= 60 -> 3
            percentage >= 40 -> 2
            else -> 1
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Spacer(
            modifier = Modifier.height(36.dp)
        )

        Text(
            text =
                if (percentage >= 80) {
                    "👨‍🦲🔥💪"
                } else {
                    "👨‍🦲💪"
                },
            fontSize = 70.sp
        )

        Spacer(
            modifier = Modifier.height(17.dp)
        )

        Text(
            text =
                when {
                    percentage >= 100 ->
                        "¡IMPECABLE!"

                    percentage >= 80 ->
                        "¡TE LA BANCASTE!"

                    percentage >= 60 ->
                        "Nada mal"

                    else ->
                        "El Pelao exige revancha"
                },
            color = GameDark,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = buildString {
                repeat(stars) {
                    append("★")
                }

                repeat(5 - stars) {
                    append("☆")
                }
            },
            color = GameOrange,
            fontSize = 30.sp
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement =
                    Arrangement.SpaceAround
            ) {
                ResultMetric(
                    value = "$correct/$total",
                    label = "Correctas"
                )

                ResultMetric(
                    value = "$score",
                    label = "Puntos"
                )

                ResultMetric(
                    value = "x$bestStreak",
                    label = "Mejor racha"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color =
                if (isNewRecord) {
                    Color(0xFFFFF4D9)
                } else {
                    Color(0xFFEAF1FF)
                }
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 13.dp
                ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        if (isNewRecord) {
                            "🏆"
                        } else {
                            "🎯"
                        },
                    fontSize = 24.sp
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column {
                    Text(
                        text =
                            if (isNewRecord) {
                                "¡NUEVO RÉCORD!"
                            } else {
                                "Récord personal"
                            },
                        color =
                            if (isNewRecord) {
                                GameOrange
                            } else {
                                GameBlue
                            },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "$highScore puntos",
                        color = GameDark,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFEAF1FF)
        ) {
            Text(
                text = "+$xpEarned XP",
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 11.dp
                ),
                color = GameBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(17.dp)
        ) {
            Text(
                text = "REVANCHA 💪",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(53.dp),
            shape = RoundedCornerShape(17.dp)
        ) {
            Text(
                text = "Volver a los juegos"
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}


@Composable
private fun ResultMetric(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = GameDark,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            color = GameGray,
            fontSize = 11.sp
        )
    }
}
