package cl.vialapp.app.data.settings

import android.content.Context


enum class ReminderFrequency {
    DAILY,
    WEEKDAYS,
    EVERY_TWO_DAYS
}


data class StudyPreferences(
    val configured: Boolean,
    val dailyStudyMinutes: Int,
    val remindersEnabled: Boolean,
    val reminderFrequency: ReminderFrequency,
    val reminderHour: Int,
    val reminderMinute: Int
)


class StudyPreferencesStore(
    context: Context,
    userKey: String = "local"
) {
    private val safeUserKey =
        userKey
            .replace(
                Regex("[^A-Za-z0-9_-]"),
                "_"
            )
            .take(
                80
            )

    private val preferences =
        context.getSharedPreferences(
            "${PREFERENCES_NAME}_$safeUserKey",
            Context.MODE_PRIVATE
        )


    fun get(): StudyPreferences {
        val frequency =
            runCatching {
                ReminderFrequency.valueOf(
                    preferences.getString(
                        KEY_REMINDER_FREQUENCY,
                        ReminderFrequency.DAILY.name
                    ) ?: ReminderFrequency.DAILY.name
                )
            }.getOrDefault(
                ReminderFrequency.DAILY
            )

        return StudyPreferences(
            configured =
                preferences.getBoolean(
                    KEY_CONFIGURED,
                    false
                ),

            dailyStudyMinutes =
                preferences.getInt(
                    KEY_DAILY_STUDY_MINUTES,
                    DEFAULT_DAILY_MINUTES
                ),

            remindersEnabled =
                preferences.getBoolean(
                    KEY_REMINDERS_ENABLED,
                    true
                ),

            reminderFrequency =
                frequency,

            reminderHour =
                preferences.getInt(
                    KEY_REMINDER_HOUR,
                    DEFAULT_REMINDER_HOUR
                ),

            reminderMinute =
                preferences.getInt(
                    KEY_REMINDER_MINUTE,
                    DEFAULT_REMINDER_MINUTE
                )
        )
    }


    fun save(
        dailyStudyMinutes: Int,
        remindersEnabled: Boolean,
        reminderFrequency: ReminderFrequency,
        reminderHour: Int,
        reminderMinute: Int,
        markConfigured: Boolean = true
    ) {
        preferences
            .edit()
            .putInt(
                KEY_DAILY_STUDY_MINUTES,
                dailyStudyMinutes
                    .coerceIn(
                        5,
                        180
                    )
            )
            .putBoolean(
                KEY_REMINDERS_ENABLED,
                remindersEnabled
            )
            .putString(
                KEY_REMINDER_FREQUENCY,
                reminderFrequency.name
            )
            .putInt(
                KEY_REMINDER_HOUR,
                reminderHour.coerceIn(
                    0,
                    23
                )
            )
            .putInt(
                KEY_REMINDER_MINUTE,
                reminderMinute.coerceIn(
                    0,
                    59
                )
            )
            .putBoolean(
                KEY_CONFIGURED,
                if (
                    markConfigured
                ) {
                    true
                } else {
                    preferences.getBoolean(
                        KEY_CONFIGURED,
                        false
                    )
                }
            )
            .apply()
    }


    fun isConfigured(): Boolean {
        return preferences.getBoolean(
            KEY_CONFIGURED,
            false
        )
    }


    companion object {
        private const val PREFERENCES_NAME =
            "vialapp_study_preferences"

        private const val KEY_CONFIGURED =
            "study_setup_configured"

        private const val KEY_DAILY_STUDY_MINUTES =
            "daily_study_minutes"

        private const val KEY_REMINDERS_ENABLED =
            "reminders_enabled"

        private const val KEY_REMINDER_FREQUENCY =
            "reminder_frequency"

        private const val KEY_REMINDER_HOUR =
            "reminder_hour"

        private const val KEY_REMINDER_MINUTE =
            "reminder_minute"

        private const val DEFAULT_DAILY_MINUTES =
            15

        private const val DEFAULT_REMINDER_HOUR =
            19

        private const val DEFAULT_REMINDER_MINUTE =
            0
    }
}
