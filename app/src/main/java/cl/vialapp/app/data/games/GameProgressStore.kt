package cl.vialapp.app.data.games

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
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


class GameProgressStore(
    context: Context
) {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )


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


    /*
     * Registra una partida de minijuego.
     *
     * IMPORTANTE:
     * jugar entrega XP y puede avanzar una misión diaria,
     * pero NO mantiene por sí solo la racha de estudio.
     */
    fun recordRun(
        gameId: String,
        score: Int,
        xp: Int
    ): Boolean {
        val currentHighScore =
            getHighScore(
                gameId
            )

        val newHighScore =
            score > currentHighScore

        val today =
            LocalDate.now()

        val editor =
            preferences.edit()

        if (
            newHighScore
        ) {
            editor.putInt(
                "${gameId}_high_score",
                score
            )
        }

        editor.putInt(
            "${gameId}_runs",
            getRuns(
                gameId
            ) + 1
        )

        editor.putInt(
            KEY_TOTAL_POINTS,
            preferences.getInt(
                KEY_TOTAL_POINTS,
                0
            ) + score.coerceAtLeast(
                0
            )
        )

        editor.putInt(
            KEY_TOTAL_XP,
            preferences.getInt(
                KEY_TOTAL_XP,
                0
            ) + xp.coerceAtLeast(
                0
            )
        )

        editor.putInt(
            KEY_TOTAL_RUNS,
            preferences.getInt(
                KEY_TOTAL_RUNS,
                0
            ) + 1
        )

        editor.putInt(
            dailyGamesKey(
                today
            ),
            preferences.getInt(
                dailyGamesKey(
                    today
                ),
                0
            ) + 1
        )

        editor.apply()

        return newHighScore
    }


    /*
     * XP por examen completado.
     *
     * Anti-farm v1:
     *
     * - El mismo attempt_id nunca premia dos veces.
     * - Primera finalización del mismo test en el día: +30 XP.
     * - Segunda finalización del mismo test en el día: +5 XP.
     * - Desde la tercera: 0 XP.
     *
     * Aunque una repetición ya no entregue XP base,
     * sí cuenta como actividad completada del día.
     */
    fun recordExamCompletion(
        testId: Int,
        attemptId: Int?
    ): Int {
        if (
            attemptId == null
        ) {
            return 0
        }

        val attemptRewardKey =
            "rewarded_exam_attempt_$attemptId"

        if (
            preferences.getBoolean(
                attemptRewardKey,
                false
            )
        ) {
            return 0
        }

        val today =
            LocalDate.now()

        val dailyTestKey =
            "exam_daily_${today}_test_$testId"

        val completionsToday =
            preferences.getInt(
                dailyTestKey,
                0
            )

        val earnedXp =
            when (
                completionsToday
            ) {
                0 -> 30
                1 -> 5
                else -> 0
            }

        val editor =
            preferences.edit()
                .putBoolean(
                    attemptRewardKey,
                    true
                )
                .putInt(
                    dailyTestKey,
                    completionsToday + 1
                )
                .putInt(
                    dailyExamsKey(
                        today
                    ),
                    preferences.getInt(
                        dailyExamsKey(
                            today
                        ),
                        0
                    ) + 1
                )
                .putInt(
                    KEY_TOTAL_EXAM_COMPLETIONS,
                    preferences.getInt(
                        KEY_TOTAL_EXAM_COMPLETIONS,
                        0
                    ) + 1
                )

        if (
            earnedXp > 0
        ) {
            editor.putInt(
                KEY_TOTAL_XP,
                preferences.getInt(
                    KEY_TOTAL_XP,
                    0
                ) + earnedXp
            )
        }

        editor.apply()

        /*
         * Un examen completado sí cuenta como estudio real:
         * actualizamos racha y objetivo semanal.
         */
        markStudyDay(
            today
        )

        return earnedXp
    }


    /*
     * Recompensa genérica única.
     *
     * La mantenemos porque luego nos servirá
     * para logros, recompensas de nivel, etc.
     */
    fun awardXpOnce(
        eventKey: String,
        xp: Int
    ): Int {
        val safeXp =
            xp.coerceAtLeast(
                0
            )

        if (
            safeXp == 0
        ) {
            return 0
        }

        val key =
            "xp_once_$eventKey"

        if (
            preferences.getBoolean(
                key,
                false
            )
        ) {
            return 0
        }

        preferences
            .edit()
            .putBoolean(
                key,
                true
            )
            .putInt(
                KEY_TOTAL_XP,
                preferences.getInt(
                    KEY_TOTAL_XP,
                    0
                ) + safeXp
            )
            .apply()

        return safeXp
    }


    fun streakSnapshot(): StudyStreakSnapshot {
        val today =
            LocalDate.now()

        val monday =
            today.with(
                TemporalAdjusters.previousOrSame(
                    DayOfWeek.MONDAY
                )
            )

        val activeDaysThisWeek =
            (0L..6L)
                .count { offset ->

                    val date =
                        monday.plusDays(
                            offset
                        )

                    preferences.getBoolean(
                        studyDayKey(
                            date
                        ),
                        false
                    )
                }

        val storedCurrentStreak =
            preferences.getInt(
                KEY_CURRENT_STREAK,
                0
            )

        val lastStudyDate =
            preferences
                .getString(
                    KEY_LAST_STUDY_DATE,
                    null
                )
                ?.let { raw ->

                    runCatching {
                        LocalDate.parse(
                            raw
                        )
                    }.getOrNull()
                }

        /*
         * Si el usuario dejó pasar más de un día completo,
         * mostramos la racha actual como 0 incluso antes de
         * que vuelva a estudiar. El récord histórico se conserva.
         */
        val effectiveCurrentStreak =
            when {

                lastStudyDate == null ->
                    0

                lastStudyDate == today ->
                    storedCurrentStreak

                lastStudyDate.plusDays(
                    1
                ) == today ->
                    storedCurrentStreak

                else ->
                    0
            }

        return StudyStreakSnapshot(
            currentStreak =
                effectiveCurrentStreak,

            bestStreak =
                preferences.getInt(
                    KEY_BEST_STREAK,
                    0
                ),

            activeDaysThisWeek =
                activeDaysThisWeek,

            weeklyGoal =
                WEEKLY_STUDY_GOAL,

            studiedToday =
                preferences.getBoolean(
                    studyDayKey(
                        today
                    ),
                    false
                )
        )
    }


    fun dailyMissionSnapshot(): DailyMissionSnapshot {
        val today =
            LocalDate.now()

        val examsToday =
            preferences.getInt(
                dailyExamsKey(
                    today
                ),
                0
            )

        val gamesToday =
            preferences.getInt(
                dailyGamesKey(
                    today
                ),
                0
            )

        val mission =
            missionForDate(
                today
            )

        val claimed =
            preferences.getBoolean(
                missionClaimedKey(
                    today
                ),
                false
            )

        return when (
            mission
        ) {

            MissionKind.ONE_EXAM -> {

                val progress =
                    examsToday
                        .coerceAtMost(
                            1
                        )

                DailyMissionSnapshot(
                    id =
                        mission.id,

                    title =
                        "Kilómetros de estudio",

                    description =
                        "Completa 1 examen hoy.",

                    rewardXp =
                        25,

                    progress =
                        progress,

                    goal =
                        1,

                    detail =
                        "Exámenes: $progress/1",

                    completed =
                        progress >= 1,

                    claimed =
                        claimed
                )
            }


            MissionKind.EXAM_AND_GAME -> {

                val examDone =
                    if (
                        examsToday >= 1
                    ) {
                        1
                    } else {
                        0
                    }

                val gameDone =
                    if (
                        gamesToday >= 1
                    ) {
                        1
                    } else {
                        0
                    }

                val progress =
                    examDone +
                            gameDone

                DailyMissionSnapshot(
                    id =
                        mission.id,

                    title =
                        "Vuelta completa",

                    description =
                        "Completa 1 examen y juega 1 minijuego.",

                    rewardXp =
                        35,

                    progress =
                        progress,

                    goal =
                        2,

                    detail =
                        "Examen: $examDone/1 · Juego: $gameDone/1",

                    completed =
                        progress >= 2,

                    claimed =
                        claimed
                )
            }


            MissionKind.TWO_EXAMS -> {

                val progress =
                    examsToday
                        .coerceAtMost(
                            2
                        )

                DailyMissionSnapshot(
                    id =
                        mission.id,

                    title =
                        "Doble entrenamiento",

                    description =
                        "Completa 2 exámenes hoy.",

                    rewardXp =
                        40,

                    progress =
                        progress,

                    goal =
                        2,

                    detail =
                        "Exámenes: $progress/2",

                    completed =
                        progress >= 2,

                    claimed =
                        claimed
                )
            }
        }
    }


    /*
     * La recompensa se reclama de forma explícita desde Home.
     *
     * Esto hace visible el momento de la recompensa y evita
     * que el total de XP cambie "mágicamente" sin feedback.
     */
    fun claimDailyMissionReward(): Int {
        val today =
            LocalDate.now()

        val mission =
            dailyMissionSnapshot()

        if (
            !mission.completed ||
            mission.claimed
        ) {
            return 0
        }

        preferences
            .edit()
            .putBoolean(
                missionClaimedKey(
                    today
                ),
                true
            )
            .putInt(
                KEY_TOTAL_XP,
                preferences.getInt(
                    KEY_TOTAL_XP,
                    0
                ) + mission.rewardXp
            )
            .putInt(
                KEY_TOTAL_MISSIONS_COMPLETED,
                preferences.getInt(
                    KEY_TOTAL_MISSIONS_COMPLETED,
                    0
                ) + 1
            )
            .apply()

        return mission.rewardXp
    }


    fun snapshot(): GameProgressSnapshot {
        val totalXp =
            preferences.getInt(
                KEY_TOTAL_XP,
                0
            )

        return GameProgressSnapshot(
            totalPoints =
                preferences.getInt(
                    KEY_TOTAL_POINTS,
                    0
                ),

            totalXp =
                totalXp,

            totalRuns =
                preferences.getInt(
                    KEY_TOTAL_RUNS,
                    0
                ),

            totalExamCompletions =
                preferences.getInt(
                    KEY_TOTAL_EXAM_COMPLETIONS,
                    0
                ),

            totalMissionsCompleted =
                preferences.getInt(
                    KEY_TOTAL_MISSIONS_COMPLETED,
                    0
                ),

            level =
                (totalXp / XP_PER_LEVEL) + 1,

            levelXp =
                totalXp % XP_PER_LEVEL,

            xpPerLevel =
                XP_PER_LEVEL
        )
    }


    private fun markStudyDay(
        today: LocalDate
    ) {
        if (
            preferences.getBoolean(
                studyDayKey(
                    today
                ),
                false
            )
        ) {
            return
        }

        val lastStudyDate =
            preferences
                .getString(
                    KEY_LAST_STUDY_DATE,
                    null
                )
                ?.let { raw ->

                    runCatching {
                        LocalDate.parse(
                            raw
                        )
                    }.getOrNull()
                }

        val previousStreak =
            preferences.getInt(
                KEY_CURRENT_STREAK,
                0
            )

        val newStreak =
            when {

                lastStudyDate == null ->
                    1

                lastStudyDate == today ->
                    previousStreak

                lastStudyDate.plusDays(
                    1
                ) == today ->
                    previousStreak + 1

                else ->
                    1
            }

        val previousBest =
            preferences.getInt(
                KEY_BEST_STREAK,
                0
            )

        preferences
            .edit()
            .putBoolean(
                studyDayKey(
                    today
                ),
                true
            )
            .putString(
                KEY_LAST_STUDY_DATE,
                today.toString()
            )
            .putInt(
                KEY_CURRENT_STREAK,
                newStreak
            )
            .putInt(
                KEY_BEST_STREAK,
                max(
                    previousBest,
                    newStreak
                )
            )
            .apply()
    }


    private fun missionForDate(
        date: LocalDate
    ): MissionKind {
        val index =
            (
                    date.toEpochDay() %
                            3L
                    )
                .toInt()

        return when (
            index
        ) {
            0 ->
                MissionKind.ONE_EXAM

            1 ->
                MissionKind.EXAM_AND_GAME

            else ->
                MissionKind.TWO_EXAMS
        }
    }


    private fun dailyExamsKey(
        date: LocalDate
    ): String {
        return "daily_exams_$date"
    }


    private fun dailyGamesKey(
        date: LocalDate
    ): String {
        return "daily_games_$date"
    }


    private fun studyDayKey(
        date: LocalDate
    ): String {
        return "study_day_$date"
    }


    private fun missionClaimedKey(
        date: LocalDate
    ): String {
        return "daily_mission_claimed_$date"
    }


    private enum class MissionKind(
        val id: String
    ) {
        ONE_EXAM(
            "one_exam"
        ),

        EXAM_AND_GAME(
            "exam_and_game"
        ),

        TWO_EXAMS(
            "two_exams"
        )
    }


    companion object {
        const val BALD_CHALLENGE =
            "bald_challenge"

        const val SIGN_HUNT =
            "sign_hunt"

        const val CROSSROADS_CHAOS =
            "crossroads_chaos"

        private const val PREFERENCES_NAME =
            "vialapp_games"

        private const val KEY_TOTAL_POINTS =
            "games_total_points"

        private const val KEY_TOTAL_XP =
            "games_total_xp"

        private const val KEY_TOTAL_RUNS =
            "games_total_runs"

        private const val KEY_TOTAL_EXAM_COMPLETIONS =
            "total_exam_completions"

        private const val KEY_TOTAL_MISSIONS_COMPLETED =
            "total_missions_completed"

        private const val KEY_CURRENT_STREAK =
            "study_current_streak"

        private const val KEY_BEST_STREAK =
            "study_best_streak"

        private const val KEY_LAST_STUDY_DATE =
            "study_last_date"

        private const val XP_PER_LEVEL =
            500

        private const val WEEKLY_STUDY_GOAL =
            5
    }
}
