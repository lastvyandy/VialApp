package cl.vialapp.app.data.games

// GUÍA DEL ARCHIVO:
// Avance local de juegos, XP y recompensas. No perdamos una partida solo porque internet se fue.

import android.content.Context
import cl.vialapp.app.data.api.GamificationEventResponse
import cl.vialapp.app.data.api.GamificationGameEventRequest
import cl.vialapp.app.data.api.GamificationProgressResponse
import cl.vialapp.app.data.api.VialApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.math.max

data class GameProgressSnapshot(
    val totalPoints: Int,
    val totalXp: Int,
    val totalRuns: Int,
    val totalExamCompletions: Int,
    val totalMissionsCompleted: Int,
    val level: Int,
    val levelXp: Int,
    val xpPerLevel: Int
)

data class StudyStreakSnapshot(
    val currentStreak: Int,
    val bestStreak: Int,
    val activeDaysThisWeek: Int,
    val weeklyGoal: Int,
    val studiedToday: Boolean
)

data class DailyMissionSnapshot(
    val id: String,
    val title: String,
    val description: String,
    val rewardXp: Int,
    val progress: Int,
    val goal: Int,
    val detail: String,
    val completed: Boolean,
    val claimed: Boolean
)

data class GameRunResult(
    val isNewHighScore: Boolean,
    val xpAwarded: Int,
    val pointsAwarded: Int,
    val syncedWithBackend: Boolean,
    val coinsAwarded:Int=0
)


// Guardamos el progreso por usuario, para que las partidas y recompensas no se mezclen entre cuentas.
class GameProgressStore(
    context: Context,
    userKey: String? = null
) {
    private val appContext = context.applicationContext
    private val engagementUser = userKey
    private val preferences =
        context.getSharedPreferences(
            if (userKey.isNullOrBlank()) {
                PREFERENCES_NAME
            } else {
                "${PREFERENCES_NAME}_$userKey"
            },
            Context.MODE_PRIVATE
        )


    // Rally se desbloquea con exámenes reales, no solo por entrar varias veces al juego.
    fun rallyExamCount():Int = preferences.getInt("rally_confirmed_exams",0).coerceIn(0,3)

    // Solo aumentamos el número confirmado; no se desbloquea Rally por entrar al menú varias veces.
    private fun confirmRallyExams(count:Int?){
        if(count!=null)preferences.edit().putInt("rally_confirmed_exams",maxOf(rallyExamCount(),count.coerceIn(0,3))).apply()
    }

    fun getHighScore(
        gameId: String
    ): Int {
        return preferences.getInt(
            "${gameId}_high_score",
            0
        )
    }

    fun getRuns(
        gameId: String
    ): Int {
        return preferences.getInt(
            "${gameId}_runs",
            0
        )
    }

    /**
     * Sincroniza el progreso de XP, puntos y partidas jugadas desde el backend.
     */
    // Si hay conexión traemos lo del servidor, pero el menú puede seguir usando el progreso local.
    suspend fun syncFromBackend(
        api: VialApi,
        token: String
    ): GameProgressSnapshot = withContext(Dispatchers.IO) {
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        try {
            val response = api.getGamificationProgress(authHeader)
            if (response.isSuccessful) response.body()?.let(::applyRemoteProgress)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { /* Keep last confirmed progress. */ }
        snapshot()
    }

    /**
     * Registra una partida en el backend (POST /api/v1/users/me/gamification/games)
     * y actualiza los contadores locales de misión diaria y récord personal.
     * Si falla la red, aplica el registro local como respaldo.
     */
    suspend fun recordRunRemote(
        api: VialApi,
        token: String,
        gameId: String,
        score: Int,
        durationSeconds: Int,
        fallbackXp: Int = 0,
        eventId: String = UUID.randomUUID().toString()
    ): GameRunResult = withContext(Dispatchers.IO) {
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val request = GamificationGameEventRequest(
            eventId = eventId,
            gameId = gameId,
            score = score.coerceIn(0, 100_000),
            durationSeconds = durationSeconds.coerceIn(1, 3600)
        )

        try {
            val response = api.recordGameEvent(authHeader, request)
            if (response.isSuccessful && response.body() != null) {
                val event = response.body()!!
                val newHighScore = applyRemoteGameEvent(gameId, score, event)
                return@withContext GameRunResult(
                    isNewHighScore = newHighScore,
                    xpAwarded = if(event.duplicate)0 else event.xpAwarded,
                    pointsAwarded = if(event.duplicate)0 else event.pointsAwarded,
                    syncedWithBackend = true,
                    coinsAwarded=if(event.duplicate)0 else cl.vialapp.app.ui.rewards.coinsFromAward(event.totalXp,event.xpAwarded)
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Fallback a registro local si hay error de red
        }

        // Offline records are personal only; never mint official XP or points.
        val newHighScore = recordLocalHighScore(gameId, score)
        GameRunResult(
            isNewHighScore = newHighScore,
            xpAwarded = 0,
            pointsAwarded = 0,
            syncedWithBackend = false
        )
    }

    fun applyRemoteProgress(
        remote: GamificationProgressResponse
    ) {
        confirmRallyExams(remote.rallyExamCount)
        preferences.edit()
            .putInt(KEY_TOTAL_XP, remote.totalXp.coerceAtLeast(0))
            .putInt(KEY_TOTAL_POINTS, remote.totalPoints.coerceAtLeast(0))
            .putInt(KEY_TOTAL_RUNS, remote.gamesPlayed.coerceAtLeast(0))
            .apply()
        engagementUser?.takeIf { it.isNotBlank() }?.let { cl.vialapp.app.engagement.EngagementState.recordProgress(appContext,it,remote) }
    }

    private fun applyRemoteGameEvent(
        gameId: String,
        score: Int,
        event: GamificationEventResponse
    ): Boolean {
        confirmRallyExams(event.rallyExamCount)
        val currentHighScore = getHighScore(gameId)
        val newHighScore = score > currentHighScore
        val today = LocalDate.now()

        val editor = preferences.edit()
        if (newHighScore) {
            editor.putInt("${gameId}_high_score", score)
        }

        if (!event.duplicate) {
            editor.putInt("${gameId}_runs", getRuns(gameId) + 1)
            editor.putInt(
                dailyGamesKey(today),
                preferences.getInt(dailyGamesKey(today), 0) + 1
            )
        }

        editor.putInt(KEY_TOTAL_XP, event.totalXp.coerceAtLeast(0))
        editor.putInt(KEY_TOTAL_POINTS, event.totalPoints.coerceAtLeast(0))
        editor.putInt(KEY_TOTAL_RUNS, event.gamesPlayed.coerceAtLeast(0))
        editor.apply()
        engagementUser?.takeIf { it.isNotBlank() }?.let {
            cl.vialapp.app.engagement.EngagementState.recordProgress(appContext,it,
                GamificationProgressResponse(totalXp=event.totalXp,totalPoints=event.totalPoints,gamesPlayed=event.gamesPlayed,
                    level=event.level,xpInLevel=event.xpInLevel,xpToNextLevel=event.xpToNextLevel,
                    streakDays=event.streakDays,streakActiveToday=event.streakActiveToday))
        }

        return newHighScore
    }

    /*
     * Registra una partida de minijuego localmente (respaldo offline).
     * IMPORTANTE:
     * jugar entrega XP y puede avanzar una misión diaria,
     * pero NO mantiene por sí solo la racha de estudio.
     */
    fun recordLocalHighScore(gameId: String, score: Int): Boolean {
        val best=score>getHighScore(gameId)
        if(best)preferences.edit().putInt("${gameId}_high_score",score).apply()
        return best
    }

    fun recordRun(
        gameId: String,
        score: Int,
        xp: Int
    ): Boolean {
        val currentHighScore = getHighScore(gameId)
        val newHighScore = score > currentHighScore
        val today = LocalDate.now()
        val editor = preferences.edit()

        if (newHighScore) {
            editor.putInt("${gameId}_high_score", score)
        }

        editor.putInt(
            "${gameId}_runs",
            getRuns(gameId) + 1
        )

        editor.putInt(
            KEY_TOTAL_POINTS,
            preferences.getInt(KEY_TOTAL_POINTS, 0) + score.coerceAtLeast(0)
        )

        editor.putInt(
            KEY_TOTAL_XP,
            preferences.getInt(KEY_TOTAL_XP, 0) + xp.coerceAtLeast(0)
        )

        editor.putInt(
            KEY_TOTAL_RUNS,
            preferences.getInt(KEY_TOTAL_RUNS, 0) + 1
        )

        editor.putInt(
            dailyGamesKey(today),
            preferences.getInt(dailyGamesKey(today), 0) + 1
        )

        editor.apply()
        return newHighScore
    }

    /*
     * XP por examen completado.
     */
    fun recordExamCompletion(
        testId: Int,
        attemptId: Int?
    ): Int {
        if (attemptId == null) {
            return 0
        }

        val attemptRewardKey = "rewarded_exam_attempt_$attemptId"
        if (preferences.getBoolean(attemptRewardKey, false)) {
            return 0
        }

        val today = LocalDate.now()
        val dailyTestKey = "exam_daily_${today}_test_$testId"
        val completionsToday = preferences.getInt(dailyTestKey, 0)

        val earnedXp = when (completionsToday) {
            0 -> 30
            1 -> 5
            else -> 0
        }

        val editor = preferences.edit()
            .putBoolean(attemptRewardKey, true)
            .putInt(dailyTestKey, completionsToday + 1)
            .putInt(
                dailyExamsKey(today),
                preferences.getInt(dailyExamsKey(today), 0) + 1
            )
            .putInt(
                KEY_TOTAL_EXAM_COMPLETIONS,
                preferences.getInt(KEY_TOTAL_EXAM_COMPLETIONS, 0) + 1
            )

        if (earnedXp > 0) {
            editor.putInt(
                KEY_TOTAL_XP,
                preferences.getInt(KEY_TOTAL_XP, 0) + earnedXp
            )
        }

        editor.apply()
        markStudyDay(today)
        return earnedXp
    }


    /**
     * Registra un examen completado usando la recompensa
     * confirmada por el backend.
     *
     * La XP, los puntos y el nivel dependen de FastAPI.
     * Las rachas y misiones siguen siendo locales
     * hasta que migremos esas funcionalidades.
     */
    fun recordConfirmedExamCompletion(
        testId: Int,
        attemptId: Int?,
        reward: GamificationEventResponse
    ): Int {

        // 1. Actualizar el progreso global con los
        // valores oficiales devueltos por FastAPI.
        applyRemoteProgress(
            GamificationProgressResponse(
                totalXp = reward.totalXp,
                totalPoints = reward.totalPoints,
                gamesPlayed = reward.gamesPlayed,
                level = reward.level,
                xpInLevel = reward.xpInLevel,
                xpToNextLevel = reward.xpToNextLevel,
                streakDays = reward.streakDays,
                streakActiveToday = reward.streakActiveToday,
                rallyExamCount = reward.rallyExamCount
            )
        )

        // 2. Registrar la actividad local del examen
        // sin sumar XP otra vez.
        if (attemptId != null) {

            val attemptKey =
                "rewarded_exam_attempt_$attemptId"

            val alreadyRecorded =
                preferences.getBoolean(attemptKey, false)

            if (!alreadyRecorded) {

                val today = LocalDate.now()

                val dailyTestKey =
                    "exam_daily_${today}_test_$testId"

                val editor = preferences.edit()
                    .putBoolean(attemptKey, true)
                    .putInt(
                        dailyTestKey,
                        preferences.getInt(dailyTestKey, 0) + 1
                    )
                    .putInt(
                        dailyExamsKey(today),
                        preferences.getInt(
                            dailyExamsKey(today), 0
                        ) + 1
                    )
                    .putInt(
                        KEY_TOTAL_EXAM_COMPLETIONS,
                        preferences.getInt(
                            KEY_TOTAL_EXAM_COMPLETIONS, 0
                        ) + 1
                    )

                editor.apply()

                // La racha solo avanza por estudio real.
                markStudyDay(today)
            }
        }

        // 3. No anunciar una recompensa nuevamente
        // si el backend indica un evento duplicado.
        return if (!reward.duplicate) {
            reward.xpAwarded.coerceAtLeast(0)
        } else {
            0
        }
    }


    fun awardXpOnce(
        eventKey: String,
        xp: Int
    ): Int {
        val safeXp = xp.coerceAtLeast(0)
        if (safeXp == 0) {
            return 0
        }

        val key = "xp_once_$eventKey"
        if (preferences.getBoolean(key, false)) {
            return 0
        }

        preferences.edit()
            .putBoolean(key, true)
            .putInt(
                KEY_TOTAL_XP,
                preferences.getInt(KEY_TOTAL_XP, 0) + safeXp
            )
            .apply()

        return safeXp
    }

    fun streakSnapshot(): StudyStreakSnapshot {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val activeDaysThisWeek = (0L..6L).count { offset ->
            val date = monday.plusDays(offset)
            preferences.getBoolean(studyDayKey(date), false)
        }

        val storedCurrentStreak = preferences.getInt(KEY_CURRENT_STREAK, 0)
        val lastStudyDate = preferences.getString(KEY_LAST_STUDY_DATE, null)?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }

        val effectiveCurrentStreak = when {
            lastStudyDate == null -> 0
            lastStudyDate == today -> storedCurrentStreak
            lastStudyDate.plusDays(1) == today -> storedCurrentStreak
            else -> 0
        }

        return StudyStreakSnapshot(
            currentStreak = effectiveCurrentStreak,
            bestStreak = preferences.getInt(KEY_BEST_STREAK, 0),
            activeDaysThisWeek = activeDaysThisWeek,
            weeklyGoal = WEEKLY_STUDY_GOAL,
            studiedToday = preferences.getBoolean(studyDayKey(today), false)
        )
    }

    // La misión diaria sale del progreso que ya guardamos, no hace falta pedirla cada vez.
    fun dailyMissionSnapshot(): DailyMissionSnapshot {
        val today = LocalDate.now()
        val examsToday = preferences.getInt(dailyExamsKey(today), 0)
        val gamesToday = preferences.getInt(dailyGamesKey(today), 0)
        val mission = missionForDate(today)
        val claimed = preferences.getBoolean(missionClaimedKey(today), false)

        return when (mission) {
            MissionKind.ONE_EXAM -> {
                val progress = examsToday.coerceAtMost(1)
                DailyMissionSnapshot(
                    id = mission.id,
                    title = "Kilómetros de estudio",
                    description = "Completa 1 examen hoy.",
                    rewardXp = 25,
                    progress = progress,
                    goal = 1,
                    detail = "Exámenes: $progress/1",
                    completed = progress >= 1,
                    claimed = claimed
                )
            }
            MissionKind.EXAM_AND_GAME -> {
                val examDone = if (examsToday >= 1) 1 else 0
                val gameDone = if (gamesToday >= 1) 1 else 0
                val progress = examDone + gameDone
                DailyMissionSnapshot(
                    id = mission.id,
                    title = "Vuelta completa",
                    description = "Completa 1 examen y juega 1 minijuego.",
                    rewardXp = 35,
                    progress = progress,
                    goal = 2,
                    detail = "Examen: $examDone/1 · Juego: $gameDone/1",
                    completed = progress >= 2,
                    claimed = claimed
                )
            }
            MissionKind.TWO_EXAMS -> {
                val progress = examsToday.coerceAtMost(2)
                DailyMissionSnapshot(
                    id = mission.id,
                    title = "Doble entrenamiento",
                    description = "Completa 2 exámenes hoy.",
                    rewardXp = 40,
                    progress = progress,
                    goal = 2,
                    detail = "Exámenes: $progress/2",
                    completed = progress >= 2,
                    claimed = claimed
                )
            }
        }
    }

    fun claimDailyMissionReward(): Int {
        val today = LocalDate.now()
        val mission = dailyMissionSnapshot()
        if (!mission.completed || mission.claimed) {
            return 0
        }

        preferences.edit()
            .putBoolean(missionClaimedKey(today), true)
            .putInt(
                KEY_TOTAL_XP,
                preferences.getInt(KEY_TOTAL_XP, 0) + mission.rewardXp
            )
            .putInt(
                KEY_TOTAL_MISSIONS_COMPLETED,
                preferences.getInt(KEY_TOTAL_MISSIONS_COMPLETED, 0) + 1
            )
            .apply()

        return mission.rewardXp
    }

    fun snapshot(): GameProgressSnapshot {
        val totalXp = preferences.getInt(KEY_TOTAL_XP, 0)
        return GameProgressSnapshot(
            totalPoints = preferences.getInt(KEY_TOTAL_POINTS, 0),
            totalXp = totalXp,
            totalRuns = preferences.getInt(KEY_TOTAL_RUNS, 0),
            totalExamCompletions = preferences.getInt(KEY_TOTAL_EXAM_COMPLETIONS, 0),
            totalMissionsCompleted = preferences.getInt(KEY_TOTAL_MISSIONS_COMPLETED, 0),
            level = (totalXp / XP_PER_LEVEL) + 1,
            levelXp = totalXp % XP_PER_LEVEL,
            xpPerLevel = XP_PER_LEVEL
        )
    }

    private fun markStudyDay(today: LocalDate) {
        engagementUser?.takeIf { it.isNotBlank() }?.let { cl.vialapp.app.engagement.EngagementState.markStudy(appContext,it) }
        if (preferences.getBoolean(studyDayKey(today), false)) {
            return
        }

        val lastStudyDate = preferences.getString(KEY_LAST_STUDY_DATE, null)?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }

        val previousStreak = preferences.getInt(KEY_CURRENT_STREAK, 0)
        val newStreak = when {
            lastStudyDate == null -> 1
            lastStudyDate == today -> previousStreak
            lastStudyDate.plusDays(1) == today -> previousStreak + 1
            else -> 1
        }

        val previousBest = preferences.getInt(KEY_BEST_STREAK, 0)
        preferences.edit()
            .putBoolean(studyDayKey(today), true)
            .putString(KEY_LAST_STUDY_DATE, today.toString())
            .putInt(KEY_CURRENT_STREAK, newStreak)
            .putInt(KEY_BEST_STREAK, max(previousBest, newStreak))
            .apply()
    }

    private fun missionForDate(date: LocalDate): MissionKind {
        val index = (date.toEpochDay() % 3L).toInt()
        return when (index) {
            0 -> MissionKind.ONE_EXAM
            1 -> MissionKind.EXAM_AND_GAME
            else -> MissionKind.TWO_EXAMS
        }
    }

    private fun dailyExamsKey(date: LocalDate): String = "daily_exams_$date"
    private fun dailyGamesKey(date: LocalDate): String = "daily_games_$date"
    private fun studyDayKey(date: LocalDate): String = "study_day_$date"
    private fun missionClaimedKey(date: LocalDate): String = "daily_mission_claimed_$date"

    private enum class MissionKind(val id: String) {
        ONE_EXAM("one_exam"),
        EXAM_AND_GAME("exam_and_game"),
        TWO_EXAMS("two_exams")
    }

    companion object {
        const val BALD_CHALLENGE = "bald_challenge"
        const val SIGN_HUNT = "sign_hunt"
        const val SIGN_HUNT_ARCADE = "sign_hunt_arcade"
        const val CROSSROADS_CHAOS = "crossroads_chaos"
        const val CROSSROADS_ARCADE = "crossroads_arcade"
        const val CARVIER_DRIVE = "carvier_drive"
        const val PELAO_ARENA = "pelao_arena"
        const val AUTO_WORKSHOP = "auto_workshop"
        const val VIAL_RALLY = "vial_rally"
        const val PELAO_DOOM = "pelao_doom"

        private const val PREFERENCES_NAME = "vialapp_games"
        private const val KEY_TOTAL_POINTS = "games_total_points"
        private const val KEY_TOTAL_XP = "games_total_xp"
        private const val KEY_TOTAL_RUNS = "games_total_runs"
        private const val KEY_TOTAL_EXAM_COMPLETIONS = "total_exam_completions"
        private const val KEY_TOTAL_MISSIONS_COMPLETED = "total_missions_completed"
        private const val KEY_CURRENT_STREAK = "study_current_streak"
        private const val KEY_BEST_STREAK = "study_best_streak"
        private const val KEY_LAST_STUDY_DATE = "study_last_date"
        private const val XP_PER_LEVEL = 500
        private const val WEEKLY_STUDY_GOAL = 5
    }
}