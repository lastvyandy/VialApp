package cl.vialapp.app.ui.games.crossroads

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.games.GameProgressStore
import kotlinx.coroutines.delay


private val ChaosBlue = Color(0xFF2563EB)
private val ChaosDark = Color(0xFF172033)
private val ChaosGray = Color(0xFF64748B)
private val ChaosGreen = Color(0xFF16A34A)
private val ChaosRed = Color(0xFFDC2626)
private val ChaosOrange = Color(0xFFF59E0B)
private val ChaosPurple = Color(0xFF7C3AED)
private val ChaosBackground = Color(0xFFF4F7FB)
private val RoadColor = Color(0xFF4B5563)
private val GrassColor = Color(0xFFDFF3E6)


private enum class ActorPosition {
    WEST,
    EAST,
    NORTH,
    SOUTH,
    CENTER
}


private enum class CrossroadKind {
    NORMAL,
    ROUNDABOUT,
    OBSTRUCTION,
    PEDESTRIAN,
    POLICE
}


private data class CrossroadActor(
    val id: String,
    val name: String,
    val emoji: String,
    val direction: String,
    val position: ActorPosition,
    val control: String? = null
)


private data class CrossroadScenario(
    val id: String,
    val title: String,
    val prompt: String,
    val kind: CrossroadKind,
    val actors: List<CrossroadActor>,
    val correctActorId: String,
    val explanation: String,
    val difficulty: Int
)


@Composable
fun CrossroadsChaosScreen(
    onBack: () -> Unit
) {
    val context =
        LocalContext.current

    val haptic =
        LocalHapticFeedback.current

    val progressStore =
        remember {
            GameProgressStore(context)
        }

    var sourceScenarios by remember {
        mutableStateOf(
            buildCrossroadsRun()
        )
    }

    var started by remember {
        mutableStateOf(false)
    }

    var introReady by remember {
        mutableStateOf(false)
    }

    var finished by remember {
        mutableStateOf(false)
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

    var selectedActorId by remember {
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

    var countdownValue by remember {
        mutableIntStateOf(3)
    }

    var runNonce by remember {
        mutableIntStateOf(0)
    }

    var resultSaved by remember {
        mutableStateOf(false)
    }

    var xpEarned by remember {
        mutableIntStateOf(0)
    }

    var highScore by remember {
        mutableIntStateOf(
            progressStore.getHighScore(
                GameProgressStore.CROSSROADS_CHAOS
            )
        )
    }

    var isNewRecord by remember {
        mutableStateOf(false)
    }


    BackHandler {
        onBack()
    }


    val currentScenario =
        sourceScenarios
            .getOrNull(
                currentIndex
            )


    fun restartGame() {
        sourceScenarios =
            buildCrossroadsRun()

        currentIndex = 0
        lives = 3
        score = 0
        correctCount = 0
        streak = 0
        bestStreak = 0
        secondsLeft = 8

        answered = false
        selectedActorId = null
        answerWasCorrect = false
        timedOut = false
        earnedPoints = 0

        countdownValue = 3
        introReady = false

        xpEarned = 0
        isNewRecord = false
        resultSaved = false

        finished = false
        started = true

        runNonce += 1
    }


    fun nextRound() {
        selectedActorId = null
        answerWasCorrect = false
        timedOut = false
        earnedPoints = 0
        answered = false

        currentIndex += 1
    }


    /*
     * Cuenta regresiva solamente al comenzar.
     */
    LaunchedEffect(
        started,
        runNonce
    ) {
        if (
            !started ||
            finished ||
            sourceScenarios.isEmpty()
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
        delay(450)

        introReady = true
    }


    /*
     * Cronómetro por cruce.
     *
     * La dificultad reduce gradualmente el tiempo:
     * nivel 1 = 8 s
     * nivel 2 = 7 s
     * nivel 3 = 6 s
     */
    LaunchedEffect(
        currentIndex,
        introReady,
        answered,
        runNonce
    ) {
        val scenario =
            sourceScenarios
                .getOrNull(
                    currentIndex
                )
                ?: return@LaunchedEffect

        if (
            !started ||
            finished ||
            !introReady ||
            answered
        ) {
            return@LaunchedEffect
        }

        secondsLeft =
            secondsForDifficulty(
                scenario.difficulty
            )

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
            selectedActorId = null
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
     * Después del feedback pasamos automáticamente
     * al siguiente cruce.
     */
    LaunchedEffect(
        answered,
        currentIndex,
        runNonce
    ) {
        if (
            !answered ||
            finished
        ) {
            return@LaunchedEffect
        }

        delay(
            if (answerWasCorrect) {
                1100
            } else {
                1750
            }
        )

        val lastRound =
            currentIndex >=
                    sourceScenarios.lastIndex

        if (
            lives <= 0 ||
            lastRound
        ) {
            finished = true
        } else {
            nextRound()
        }
    }


    /*
     * Una sola escritura por partida.
     *
     * Desde aquí ya empezamos a construir
     * el progreso global de minijuegos:
     * puntos + XP + partidas.
     */
    LaunchedEffect(
        finished,
        runNonce
    ) {
        if (
            !finished ||
            resultSaved
        ) {
            return@LaunchedEffect
        }

        val earnedXp =
            20 +
                    (correctCount * 10) +
                    (bestStreak * 3) +
                    if (
                        correctCount ==
                        sourceScenarios.size
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
                    GameProgressStore.CROSSROADS_CHAOS,
                score =
                    score,
                xp =
                    earnedXp
            )

        highScore =
            progressStore.getHighScore(
                GameProgressStore.CROSSROADS_CHAOS
            )

        isNewRecord =
            newRecord

        resultSaved =
            true
    }


    if (!started) {
        CrossroadsIntro(
            highScore =
                highScore,
            totalRounds =
                sourceScenarios.size,
            onStart = {
                restartGame()
            },
            onBack =
                onBack
        )

        return
    }


    if (finished) {
        CrossroadsResult(
            correct =
                correctCount,
            total =
                sourceScenarios.size,
            score =
                score,
            bestStreak =
                bestStreak,
            xpEarned =
                xpEarned,
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
        CrossroadsCountdown(
            value =
                countdownValue
        )

        return
    }


    if (currentScenario == null) {
        return
    }


    val isBossRound =
        currentIndex ==
                sourceScenarios.lastIndex


    val progress by
    animateFloatAsState(
        targetValue =
            (
                    (currentIndex + 1)
                        .toFloat() /
                            sourceScenarios.size
                                .toFloat()
                    )
                .coerceIn(
                    0f,
                    1f
                ),
        label =
            "chaosProgress"
    )


    val maxSeconds =
        secondsForDifficulty(
            currentScenario.difficulty
        )


    val timerProgress by
    animateFloatAsState(
        targetValue =
            (
                    secondsLeft
                        .toFloat() /
                            maxSeconds
                                .toFloat()
                    )
                .coerceIn(
                    0f,
                    1f
                ),
        label =
            "chaosTimer"
    )


    val timerColor =
        when {
            secondsLeft <= 2 ->
                ChaosRed

            secondsLeft <= 4 ->
                ChaosOrange

            else ->
                ChaosGreen
        }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ChaosBackground
            )
    ) {

        /*
         * HUD
         */
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White
                )
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
                        color =
                            ChaosGray,
                        fontSize =
                            21.sp,
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
                        progress
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp),
                    color =
                        if (isBossRound) {
                            ChaosOrange
                        } else {
                            ChaosBlue
                        },
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
                        if (isBossRound) {
                            "🔥 CRUCE BOSS"
                        } else {
                            "CRUCE ${currentIndex + 1}/${sourceScenarios.size}"
                        },
                    color =
                        if (isBossRound) {
                            ChaosOrange
                        } else {
                            ChaosGray
                        },
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
                            ChaosOrange,
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
                        ChaosBlue,
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


        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
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
                            currentScenario.title,
                        color =
                            ChaosDark,
                        fontSize =
                            19.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            currentScenario.prompt,
                        color =
                            ChaosGray,
                        fontSize =
                            13.sp,
                        lineHeight =
                            18.sp
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
                    Modifier.height(14.dp)
            )

            Surface(
                shape =
                    RoundedCornerShape(30.dp),
                color =
                    when (
                        currentScenario.difficulty
                    ) {
                        1 ->
                            Color(0xFFEAF8F1)

                        2 ->
                            Color(0xFFFFF7E6)

                        else ->
                            Color(0xFFF2EAFE)
                    }
            ) {
                Text(
                    text =
                        when (
                            currentScenario.difficulty
                        ) {
                            1 ->
                                "CAOS I"

                            2 ->
                                "CAOS II"

                            else ->
                                "CAOS III"
                        },
                    modifier =
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                    color =
                        when (
                            currentScenario.difficulty
                        ) {
                            1 ->
                                ChaosGreen

                            2 ->
                                ChaosOrange

                            else ->
                                ChaosPurple
                        },
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            CrossroadScene(
                scenario =
                    currentScenario,
                answered =
                    answered,
                selectedActorId =
                    selectedActorId,
                timedOut =
                    timedOut,
                onActorClick = { actorId ->
                    if (!answered) {
                        selectedActorId =
                            actorId

                        val correct =
                            actorId ==
                                    currentScenario
                                        .correctActorId

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

                            val bossMultiplier =
                                if (isBossRound) {
                                    2
                                } else {
                                    1
                                }

                            val points =
                                (
                                        100 *
                                                currentScenario
                                                    .difficulty +
                                                secondsLeft *
                                                12 +
                                                newStreak *
                                                25
                                        ) *
                                        bossMultiplier

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
                    Modifier.height(15.dp)
            )

            Text(
                text =
                    if (answered) {
                        if (answerWasCorrect) {
                            "✅ ¡PASA!"
                        } else if (timedOut) {
                            "⏱ ¡CHOQUE DE DECISIONES!"
                        } else {
                            "❌ ¡FRENA!"
                        }
                    } else {
                        "Toca directamente a quien tiene prioridad"
                    },
                color =
                    when {
                        answerWasCorrect ->
                            ChaosGreen

                        answered ->
                            ChaosRed

                        else ->
                            ChaosGray
                    },
                fontSize =
                    if (answered) {
                        18.sp
                    } else {
                        13.sp
                    },
                fontWeight =
                    if (answered) {
                        FontWeight.ExtraBold
                    } else {
                        FontWeight.Medium
                    },
                textAlign =
                    TextAlign.Center
            )

            if (
                answered &&
                answerWasCorrect &&
                earnedPoints > 0
            ) {
                Spacer(
                    modifier =
                        Modifier.height(7.dp)
                )

                Text(
                    text =
                        "+$earnedPoints",
                    color =
                        ChaosGreen,
                    fontSize =
                        28.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                if (isBossRound) {
                    Text(
                        text =
                            "BOSS x2",
                        color =
                            ChaosOrange,
                        fontSize =
                            12.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            if (answered) {
                Spacer(
                    modifier =
                        Modifier.height(13.dp)
                )

                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(18.dp),
                    color =
                        if (answerWasCorrect) {
                            Color(0xFFEAF8F1)
                        } else {
                            Color(0xFFFFEEEE)
                        }
                ) {
                    Text(
                        text =
                            currentScenario.explanation,
                        modifier =
                            Modifier.padding(15.dp),
                        color =
                            ChaosDark,
                        fontSize =
                            13.sp,
                        lineHeight =
                            19.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )
        }
    }
}


@Composable
private fun CrossroadsIntro(
    highScore: Int,
    totalRounds: Int,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ChaosBackground
            )
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
            text =
                "🚦💥🚗",
            fontSize =
                72.sp
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(
            text =
                "Cruce Caótico",
            color =
                ChaosDark,
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
                "Prioridades, virajes, PARE, rotondas, peatones y emergencias. Decide quién pasa antes de que el cruce se vaya al demonio.",
            color =
                ChaosGray,
            fontSize =
                15.sp,
            lineHeight =
                22.sp,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
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
                ChaosRule(
                    emoji = "🚗",
                    text =
                        "$totalRounds cruces por partida"
                )

                ChaosRule(
                    emoji = "❤️",
                    text =
                        "3 vidas"
                )

                ChaosRule(
                    emoji = "🔥",
                    text =
                        "Combos por decisiones correctas"
                )

                ChaosRule(
                    emoji = "⚡",
                    text =
                        "Cada nivel te da menos tiempo"
                )

                ChaosRule(
                    emoji = "👑",
                    text =
                        "La última ronda es BOSS x2"
                )
            }
        }

        if (highScore > 0) {
            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )

            Surface(
                shape =
                    RoundedCornerShape(16.dp),
                color =
                    Color(0xFFFFF7E6)
            ) {
                Text(
                    text =
                        "🏆 Récord: $highScore",
                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                    color =
                        ChaosOrange,
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
            onClick =
                onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape =
                RoundedCornerShape(17.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        ChaosBlue
                )
        ) {
            Text(
                text =
                    "ENTRAR AL CAOS 🚦",
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(9.dp)
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
                Modifier.height(24.dp)
        )
    }
}


@Composable
private fun CrossroadsCountdown(
    value: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                ChaosBackground
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
                    "🚗🚦🚙",
                fontSize =
                    74.sp
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
                        "¡CAOS!"
                    },
                color =
                    if (value > 0) {
                        ChaosBlue
                    } else {
                        ChaosOrange
                    },
                fontSize =
                    if (value > 0) {
                        78.sp
                    } else {
                        49.sp
                    },
                fontWeight =
                    FontWeight.ExtraBold
            )

            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )

            Text(
                text =
                    when (value) {
                        3 ->
                            "Mira todo el cruce."

                        2 ->
                            "Busca quién tiene prioridad."

                        1 ->
                            "Decide rápido."

                        else ->
                            "¡QUE NO CHOQUEN!"
                    },
                color =
                    ChaosGray,
                fontSize =
                    15.sp,
                fontWeight =
                    FontWeight.SemiBold,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}


@Composable
private fun CrossroadScene(
    scenario: CrossroadScenario,
    answered: Boolean,
    selectedActorId: String?,
    timedOut: Boolean,
    onActorClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .background(
                GrassColor,
                RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFD4E5DA),
                shape = RoundedCornerShape(28.dp)
            )
    ) {

        /*
         * Calles.
         */
        Box(
            modifier = Modifier
                .align(
                    Alignment.Center
                )
                .fillMaxHeight()
                .width(112.dp)
                .background(
                    RoadColor
                )
        )

        Box(
            modifier = Modifier
                .align(
                    Alignment.Center
                )
                .fillMaxWidth()
                .height(112.dp)
                .background(
                    RoadColor
                )
        )

        /*
         * Líneas de referencia simples.
         */
        Box(
            modifier = Modifier
                .align(
                    Alignment.Center
                )
                .fillMaxHeight()
                .width(2.dp)
                .background(
                    Color.White.copy(
                        alpha = 0.45f
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(
                    Alignment.Center
                )
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Color.White.copy(
                        alpha = 0.45f
                    )
                )
        )


        if (
            scenario.kind ==
            CrossroadKind.ROUNDABOUT
        ) {
            Surface(
                modifier = Modifier
                    .align(
                        Alignment.Center
                    )
                    .size(92.dp),
                shape =
                    CircleShape,
                color =
                    GrassColor,
                border =
                    BorderStroke(
                        14.dp,
                        RoadColor
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
                            "↻",
                        color =
                            ChaosGray,
                        fontSize =
                            23.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }


        if (
            scenario.kind ==
            CrossroadKind.OBSTRUCTION
        ) {
            Text(
                text =
                    "🚧",
                modifier = Modifier
                    .align(
                        Alignment.Center
                    )
                    .offset(
                        x = 42.dp,
                        y = (-32).dp
                    ),
                fontSize =
                    28.sp
            )
        }


        if (
            scenario.kind ==
            CrossroadKind.PEDESTRIAN
        ) {
            Text(
                text =
                    "▰ ▰ ▰ ▰",
                modifier = Modifier
                    .align(
                        Alignment.Center
                    )
                    .offset(
                        y = 31.dp
                    ),
                color =
                    Color.White,
                fontSize =
                    17.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }


        if (
            scenario.kind ==
            CrossroadKind.POLICE
        ) {
            Surface(
                modifier = Modifier
                    .align(
                        Alignment.Center
                    )
                    .size(64.dp),
                shape =
                    CircleShape,
                color =
                    Color.White
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            "👮✋",
                        fontSize =
                            28.sp
                    )
                }
            }
        }


        scenario
            .actors
            .forEach { actor ->

                val isCorrectActor =
                    actor.id ==
                            scenario
                                .correctActorId

                val isSelected =
                    actor.id ==
                            selectedActorId

                val actorModifier =
                    when (actor.position) {
                        ActorPosition.WEST ->
                            Modifier
                                .align(
                                    Alignment.CenterStart
                                )
                                .offset(
                                    x = 14.dp
                                )

                        ActorPosition.EAST ->
                            Modifier
                                .align(
                                    Alignment.CenterEnd
                                )
                                .offset(
                                    x = (-14).dp
                                )

                        ActorPosition.NORTH ->
                            Modifier
                                .align(
                                    Alignment.TopCenter
                                )
                                .offset(
                                    y = 54.dp
                                )

                        ActorPosition.SOUTH ->
                            Modifier
                                .align(
                                    Alignment.BottomCenter
                                )
                                .offset(
                                    y = (-16).dp
                                )

                        ActorPosition.CENTER ->
                            Modifier
                                .align(
                                    Alignment.Center
                                )
                    }

                CrossroadActorToken(
                    actor =
                        actor,
                    answered =
                        answered,
                    isCorrectActor =
                        isCorrectActor,
                    isSelected =
                        isSelected,
                    timedOut =
                        timedOut,
                    modifier =
                        actorModifier,
                    onClick = {
                        onActorClick(
                            actor.id
                        )
                    }
                )
            }


        Surface(
            modifier = Modifier
                .align(
                    Alignment.TopCenter
                )
                .padding(
                    top = 10.dp
                ),
            shape =
                RoundedCornerShape(30.dp),
            color =
                Color.White.copy(
                    alpha = 0.94f
                )
        ) {
            Text(
                text =
                    when (
                        scenario.kind
                    ) {
                        CrossroadKind.NORMAL ->
                            "CRUCE"

                        CrossroadKind.ROUNDABOUT ->
                            "ROTONDA"

                        CrossroadKind.OBSTRUCTION ->
                            "PISTA BLOQUEADA"

                        CrossroadKind.PEDESTRIAN ->
                            "PASO PEATONAL"

                        CrossroadKind.POLICE ->
                            "CONTROL DE TRÁNSITO"
                    },
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                color =
                    ChaosDark,
                fontSize =
                    10.sp,
                fontWeight =
                    FontWeight.ExtraBold
            )
        }
    }
}


@Composable
private fun CrossroadActorToken(
    actor: CrossroadActor,
    answered: Boolean,
    isCorrectActor: Boolean,
    isSelected: Boolean,
    timedOut: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val showCorrect =
        answered &&
                isCorrectActor

    val showWrong =
        answered &&
                isSelected &&
                !isCorrectActor

    val scale by
    animateFloatAsState(
        targetValue =
            when {
                showCorrect ->
                    1.14f

                showWrong ->
                    0.92f

                else ->
                    1f
            },
        label =
            "actorScale"
    )


    Surface(
        modifier =
            modifier
                .size(
                    width = 88.dp,
                    height = 78.dp
                )
                .scale(
                    scale
                )
                .clickable(
                    enabled =
                        !answered,
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(20.dp),
        color =
            when {
                showCorrect ->
                    Color(0xFFEAF8F1)

                showWrong ->
                    Color(0xFFFFEEEE)

                timedOut &&
                        isCorrectActor ->
                    Color(0xFFEAF8F1)

                else ->
                    Color.White
            },
        border =
            BorderStroke(
                width =
                    if (
                        showCorrect ||
                        showWrong
                    ) {
                        3.dp
                    } else {
                        1.dp
                    },
                color =
                    when {
                        showCorrect ->
                            ChaosGreen

                        showWrong ->
                            ChaosRed

                        else ->
                            Color(0xFFD9E1EC)
                    }
            )
    ) {
        Column(
            modifier =
                Modifier.padding(
                    6.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        actor.emoji,
                    fontSize =
                        27.sp
                )

                Spacer(
                    modifier =
                        Modifier.width(3.dp)
                )

                Text(
                    text =
                        actor.direction,
                    color =
                        ChaosDark,
                    fontSize =
                        15.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (
                !actor.control
                    .isNullOrBlank()
            ) {
                Text(
                    text =
                        actor.control,
                    fontSize =
                        15.sp
                )
            }

            Text(
                text =
                    actor.name,
                color =
                    ChaosDark,
                fontSize =
                    9.sp,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center,
                maxLines =
                    1
            )
        }
    }
}


@Composable
private fun ChaosRule(
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
                ChaosDark,
            fontSize =
                14.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}


@Composable
private fun CrossroadsResult(
    correct: Int,
    total: Int,
    score: Int,
    bestStreak: Int,
    xpEarned: Int,
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
            .background(
                ChaosBackground
            )
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
                        "🚦👑🚗"

                    percentage >= 75 ->
                        "🚦🔥"

                    else ->
                        "🚦😵"
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
                        "¡REY DEL CRUCE!"

                    percentage >= 75 ->
                        "¡DOMINASTE EL CAOS!"

                    percentage >= 50 ->
                        "Sobreviviste"

                    else ->
                        "Hubo taco..."
                },
            color =
                ChaosDark,
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
                "$correct de $total cruces resueltos",
            color =
                ChaosGray,
            fontSize =
                15.sp
        )

        Spacer(
            modifier =
                Modifier.height(22.dp)
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
                ChaosMetric(
                    value =
                        "$score",
                    label =
                        "Puntos"
                )

                ChaosMetric(
                    value =
                        "x$bestStreak",
                    label =
                        "Racha"
                )

                ChaosMetric(
                    value =
                        "$highScore",
                    label =
                        "Récord"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(14.dp)
        )

        Surface(
            shape =
                RoundedCornerShape(18.dp),
            color =
                Color(0xFFEAF1FF)
        ) {
            Text(
                text =
                    "+$xpEarned XP",
                modifier =
                    Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 11.dp
                    ),
                color =
                    ChaosBlue,
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.ExtraBold
            )
        }

        if (isNewRecord) {
            Spacer(
                modifier =
                    Modifier.height(12.dp)
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
                        ChaosOrange,
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
                        ChaosBlue
                )
        ) {
            Text(
                text =
                    "OTRO CRUCE 🔥",
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
private fun ChaosMetric(
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
                ChaosDark,
            fontSize =
                20.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                label,
            color =
                ChaosGray,
            fontSize =
                11.sp
        )
    }
}


private fun secondsForDifficulty(
    difficulty: Int
): Int {
    return when (difficulty) {
        1 ->
            8

        2 ->
            7

        else ->
            6
    }
}


private fun buildCrossroadsRun():
        List<CrossroadScenario> {
    val easy =
        CROSSROAD_SCENARIOS
            .filter {
                it.difficulty == 1
            }
            .shuffled()
            .take(2)

    val medium =
        CROSSROAD_SCENARIOS
            .filter {
                it.difficulty == 2
            }
            .shuffled()
            .take(3)

    val hard =
        CROSSROAD_SCENARIOS
            .filter {
                it.difficulty == 3
            }
            .shuffled()
            .take(3)

    return easy +
            medium +
            hard
}


private val CROSSROAD_SCENARIOS =
    listOf(

        /*
         * Regla del material:
         * en un cruce sin regulación se cede
         * al vehículo que llega desde la derecha.
         */
        CrossroadScenario(
            id =
                "right_priority",
            title =
                "Cruce sin regulación",
            prompt =
                "No hay semáforos, PARE ni Ceda el Paso. ¿Quién pasa primero?",
            kind =
                CrossroadKind.NORMAL,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "Auto azul",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Auto rojo",
                        emoji = "🚗",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH
                    )
                ),
            correctActorId =
                "red",
            explanation =
                "En un cruce sin regulación debes dar preferencia al vehículo que llega desde tu derecha. El auto azul tiene al rojo a su derecha.",
            difficulty =
                1
        ),

        CrossroadScenario(
            id =
                "stop_sign",
            title =
                "Uno tiene PARE",
            prompt =
                "El auto rojo llega con señal PARE. ¿Quién tiene prioridad?",
            kind =
                CrossroadKind.NORMAL,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "Auto azul",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Auto rojo",
                        emoji = "🚗",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH,
                        control = "🛑"
                    )
                ),
            correctActorId =
                "blue",
            explanation =
                "PARE obliga a detenerse y reanudar la marcha sólo cuando sea seguro. El auto rojo debe ceder.",
            difficulty =
                1
        ),

        CrossroadScenario(
            id =
                "yield_sign",
            title =
                "Ceda el Paso",
            prompt =
                "El auto azul circula por la vía y el rojo enfrenta Ceda el Paso.",
            kind =
                CrossroadKind.NORMAL,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "Auto azul",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Auto rojo",
                        emoji = "🚗",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH,
                        control = "🔻"
                    )
                ),
            correctActorId =
                "blue",
            explanation =
                "Ceda el Paso exige reducir la velocidad y detenerse si es necesario. El rojo debe dejar pasar al azul.",
            difficulty =
                1
        ),

        CrossroadScenario(
            id =
                "traffic_light",
            title =
                "Semáforo básico",
            prompt =
                "Azul tiene verde y rojo tiene rojo. ¿Quién puede avanzar?",
            kind =
                CrossroadKind.NORMAL,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "Auto azul",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST,
                        control = "🟢"
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Auto rojo",
                        emoji = "🚗",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH,
                        control = "🔴"
                    )
                ),
            correctActorId =
                "blue",
            explanation =
                "La luz verde permite continuar o virar, salvo prohibición específica. La luz roja obliga a detenerse.",
            difficulty =
                1
        ),

        CrossroadScenario(
            id =
                "turning_yields",
            title =
                "Uno va a virar",
            prompt =
                "El azul sigue recto y el rojo va a virar. ¿Quién conserva la prioridad?",
            kind =
                CrossroadKind.NORMAL,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "Sigue recto",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Va a virar",
                        emoji = "🚗",
                        direction = "↰",
                        position =
                            ActorPosition.EAST
                    )
                ),
            correctActorId =
                "blue",
            explanation =
                "Al virar pierdes la preferencia y debes respetar a otros vehículos y peatones que tengan derecho de paso.",
            difficulty =
                2
        ),

        CrossroadScenario(
            id =
                "roundabout",
            title =
                "Entrada a rotonda",
            prompt =
                "El azul ya circula en la rotonda y el rojo quiere ingresar.",
            kind =
                CrossroadKind.ROUNDABOUT,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "En rotonda",
                        emoji = "🚙",
                        direction = "↻",
                        position =
                            ActorPosition.EAST
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Quiere entrar",
                        emoji = "🚗",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH
                    )
                ),
            correctActorId =
                "blue",
            explanation =
                "Al ingresar a una rotonda debes ceder a quienes ya circulan dentro.",
            difficulty =
                2
        ),

        CrossroadScenario(
            id =
                "driveway",
            title =
                "Salida desde propiedad",
            prompt =
                "El rojo sale desde un estacionamiento hacia la calle.",
            kind =
                CrossroadKind.NORMAL,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "Ya circula",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Sale del lugar",
                        emoji = "🚗",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH,
                        control = "🏠"
                    )
                ),
            correctActorId =
                "blue",
            explanation =
                "Al salir desde una propiedad o estacionamiento debes ceder a vehículos, ciclistas y peatones que ya están en tránsito.",
            difficulty =
                2
        ),

        CrossroadScenario(
            id =
                "starting_again",
            title =
                "Puesta en marcha",
            prompt =
                "El rojo estaba detenido y quiere incorporarse. El azul ya circula.",
            kind =
                CrossroadKind.NORMAL,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "En movimiento",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Se incorpora",
                        emoji = "🚗",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH,
                        control = "P"
                    )
                ),
            correctActorId =
                "blue",
            explanation =
                "Después de estar detenido y ponerse en marcha debes ceder a quienes ya circulan.",
            difficulty =
                2
        ),

        CrossroadScenario(
            id =
                "blocked_lane",
            title =
                "Pista bloqueada",
            prompt =
                "El rojo encuentra un obstáculo y necesita cambiarse a la pista del azul.",
            kind =
                CrossroadKind.OBSTRUCTION,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "Pista libre",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "red",
                        name = "Pista bloqueada",
                        emoji = "🚗",
                        direction = "→",
                        position =
                            ActorPosition.EAST,
                        control = "🚧"
                    )
                ),
            correctActorId =
                "blue",
            explanation =
                "Si necesitas cambiarte porque tu pista está obstaculizada, no tienes prioridad sobre la pista vecina.",
            difficulty =
                3
        ),

        CrossroadScenario(
            id =
                "emergency",
            title =
                "Emergencia",
            prompt =
                "La ambulancia se aproxima usando señales de emergencia.",
            kind =
                CrossroadKind.NORMAL,
            actors =
                listOf(
                    CrossroadActor(
                        id = "blue",
                        name = "Auto azul",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "ambulance",
                        name = "Ambulancia",
                        emoji = "🚑",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH,
                        control = "🚨"
                    )
                ),
            correctActorId =
                "ambulance",
            explanation =
                "Si se aproxima un vehículo de emergencia usando señales luminosas o acústicas debes cederle el derecho a vía.",
            difficulty =
                3
        ),

        CrossroadScenario(
            id =
                "pedestrian",
            title =
                "Viraje con peatón",
            prompt =
                "El auto va a virar y una persona está cruzando por el paso peatonal.",
            kind =
                CrossroadKind.PEDESTRIAN,
            actors =
                listOf(
                    CrossroadActor(
                        id = "car",
                        name = "Auto virando",
                        emoji = "🚗",
                        direction = "↱",
                        position =
                            ActorPosition.WEST
                    ),
                    CrossroadActor(
                        id = "pedestrian",
                        name = "Peatón",
                        emoji = "🚶",
                        direction = "↑",
                        position =
                            ActorPosition.SOUTH
                    )
                ),
            correctActorId =
                "pedestrian",
            explanation =
                "Al virar debes respetar a peatones y otros usuarios que tengan derecho de paso. En un paso de cebra debes detenerte cuando alguien se disponga a cruzar.",
            difficulty =
                3
        ),

        CrossroadScenario(
            id =
                "police_over_light",
            title =
                "Semáforo vs Carabinero",
            prompt =
                "El semáforo está verde, pero Carabineros ordena detenerse. Toca a quién debes obedecer.",
            kind =
                CrossroadKind.POLICE,
            actors =
                listOf(
                    CrossroadActor(
                        id = "car",
                        name = "Semáforo verde",
                        emoji = "🚙",
                        direction = "→",
                        position =
                            ActorPosition.WEST,
                        control = "🟢"
                    ),
                    CrossroadActor(
                        id = "police",
                        name = "Carabineros",
                        emoji = "👮",
                        direction = "✋",
                        position =
                            ActorPosition.SOUTH
                    )
                ),
            correctActorId =
                "police",
            explanation =
                "Si una indicación de Carabineros contradice al semáforo, debes obedecer la instrucción de Carabineros.",
            difficulty =
                3
        )
    )
