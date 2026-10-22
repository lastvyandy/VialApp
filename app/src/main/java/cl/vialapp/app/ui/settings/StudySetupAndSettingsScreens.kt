package cl.vialapp.app.ui.settings

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.settings.ReminderFrequency
import cl.vialapp.app.data.settings.StudyPreferences
import cl.vialapp.app.data.settings.StudyPreferencesStore


private val SettingsBlue =
    Color(0xFF2563EB)

private val SettingsDark =
    Color(0xFF172033)

private val SettingsGray =
    Color(0xFF64748B)

private val SettingsBackground =
    Color(0xFFF4F7FB)

private val SettingsGreen =
    Color(0xFF16A34A)


private data class ReminderTimeOption(
    val label: String,
    val hour: Int,
    val minute: Int
)


private val studyMinuteOptions =
    listOf(
        10,
        15,
        20,
        30,
        45
    )


private val reminderTimeOptions =
    listOf(
        ReminderTimeOption(
            label =
                "09:00",
            hour =
                9,
            minute =
                0
        ),

        ReminderTimeOption(
            label =
                "15:00",
            hour =
                15,
            minute =
                0
        ),

        ReminderTimeOption(
            label =
                "19:00",
            hour =
                19,
            minute =
                0
        ),

        ReminderTimeOption(
            label =
                "21:00",
            hour =
                21,
            minute =
                0
        )
    )


@Composable
fun StudySetupScreen(
    userKey: String,
    onFinished: () -> Unit
) {
    val context =
        LocalContext.current

    val store =
        remember {
            StudyPreferencesStore(
                context =
                    context,
                userKey =
                    userKey
            )
        }

    val initial =
        store.get()

    var dailyMinutes by remember {
        mutableIntStateOf(
            initial.dailyStudyMinutes
        )
    }

    var remindersEnabled by remember {
        mutableStateOf(
            initial.remindersEnabled
        )
    }

    var reminderFrequency by remember {
        mutableStateOf(
            initial.reminderFrequency
        )
    }

    var reminderHour by remember {
        mutableIntStateOf(
            initial.reminderHour
        )
    }

    var reminderMinute by remember {
        mutableIntStateOf(
            initial.reminderMinute
        )
    }


    StudyPreferencesContent(
        title =
            "Antes de empezar",

        subtitle =
            "Configura una rutina simple. Puedes cambiarla después desde Configuración.",

        dailyMinutes =
            dailyMinutes,

        onDailyMinutesChange = {
            dailyMinutes =
                it
        },

        remindersEnabled =
            remindersEnabled,

        onRemindersEnabledChange = {
            remindersEnabled =
                it
        },

        reminderFrequency =
            reminderFrequency,

        onReminderFrequencyChange = {
            reminderFrequency =
                it
        },

        reminderHour =
            reminderHour,

        reminderMinute =
            reminderMinute,

        onReminderTimeChange = { hour, minute ->
            reminderHour =
                hour

            reminderMinute =
                minute
        },

        primaryButtonText =
            "Guardar y empezar",

        onPrimaryAction = {

            store.save(
                dailyStudyMinutes =
                    dailyMinutes,

                remindersEnabled =
                    remindersEnabled,

                reminderFrequency =
                    reminderFrequency,

                reminderHour =
                    reminderHour,

                reminderMinute =
                    reminderMinute,

                markConfigured =
                    true
            )

            onFinished()
        },

        onBack =
            null
    )
}


@Composable
fun StudySettingsScreen(
    userKey: String,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context =
        LocalContext.current

    val store =
        remember {
            StudyPreferencesStore(
                context =
                    context,
                userKey =
                    userKey
            )
        }

    val initial =
        store.get()

    var dailyMinutes by remember {
        mutableIntStateOf(
            initial.dailyStudyMinutes
        )
    }

    var remindersEnabled by remember {
        mutableStateOf(
            initial.remindersEnabled
        )
    }

    var reminderFrequency by remember {
        mutableStateOf(
            initial.reminderFrequency
        )
    }

    var reminderHour by remember {
        mutableIntStateOf(
            initial.reminderHour
        )
    }

    var reminderMinute by remember {
        mutableIntStateOf(
            initial.reminderMinute
        )
    }

    var saved by remember {
        mutableStateOf(
            false
        )
    }


    StudyPreferencesContent(
        title =
            "Configuración de estudio",

        subtitle =
            "Ajusta cuánto quieres estudiar y cada cuánto quieres que VialApp te lo recuerde.",

        dailyMinutes =
            dailyMinutes,

        onDailyMinutesChange = {
            dailyMinutes =
                it

            saved =
                false
        },

        remindersEnabled =
            remindersEnabled,

        onRemindersEnabledChange = {
            remindersEnabled =
                it

            saved =
                false
        },

        reminderFrequency =
            reminderFrequency,

        onReminderFrequencyChange = {
            reminderFrequency =
                it

            saved =
                false
        },

        reminderHour =
            reminderHour,

        reminderMinute =
            reminderMinute,

        onReminderTimeChange = { hour, minute ->
            reminderHour =
                hour

            reminderMinute =
                minute

            saved =
                false
        },

        primaryButtonText =
            if (
                saved
            ) {
                "Guardado ✓"
            } else {
                "Guardar cambios"
            },

        onPrimaryAction = {

            store.save(
                dailyStudyMinutes =
                    dailyMinutes,

                remindersEnabled =
                    remindersEnabled,

                reminderFrequency =
                    reminderFrequency,

                reminderHour =
                    reminderHour,

                reminderMinute =
                    reminderMinute,

                markConfigured =
                    true
            )

            saved =
                true
        },

        onBack =
            onBack
    )
}


@Composable
private fun ManualTimePickerButton(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context =
        LocalContext.current

    val formattedTime =
        String.format(
            "%02d:%02d",
            hour,
            minute
        )

    TextButton(
        onClick = {

            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->

                    onTimeSelected(
                        selectedHour,
                        selectedMinute
                    )
                },
                hour,
                minute,
                true
            ).show()
        },
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            text =
                "Elegir otra hora · $formattedTime",
            color =
                SettingsBlue,
            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
private fun StudyPreferencesContent(
    title: String,
    subtitle: String,
    dailyMinutes: Int,
    onDailyMinutesChange: (Int) -> Unit,
    remindersEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    reminderFrequency: ReminderFrequency,
    onReminderFrequencyChange: (ReminderFrequency) -> Unit,
    reminderHour: Int,
    reminderMinute: Int,
    onReminderTimeChange: (Int, Int) -> Unit,
    primaryButtonText: String,
    onPrimaryAction: () -> Unit,
    onBack: (() -> Unit)?
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    SettingsBackground
                )
                .statusBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                )
    ) {

        if (
            onBack != null
        ) {

            TextButton(
                onClick =
                    onBack,
                contentPadding =
                    PaddingValues(
                        0.dp
                    )
            ) {

                Text(
                    text =
                        "‹ Volver",
                    color =
                        SettingsBlue,
                    fontWeight =
                        FontWeight.Bold
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )
        }


        Text(
            text =
                title,
            color =
                SettingsDark,
            fontSize =
                29.sp,
            fontWeight =
                FontWeight.ExtraBold
        )


        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )


        Text(
            text =
                subtitle,
            color =
                SettingsGray,
            fontSize =
                14.sp,
            lineHeight =
                20.sp
        )


        Spacer(
            modifier =
                Modifier.height(
                    22.dp
                )
        )


        SectionTitle(
            emoji =
                "⏱️",
            title =
                "¿Cuánto quieres estudiar al día?",
            subtitle =
                "Esto será tu meta diaria de estudio."
        )


        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )


        OptionGrid(
            options =
                studyMinuteOptions.map {
                    "$it min"
                },

            selectedIndex =
                studyMinuteOptions
                    .indexOf(
                        dailyMinutes
                    )
                    .coerceAtLeast(
                        0
                    ),

            onSelected = { index ->

                onDailyMinutesChange(
                    studyMinuteOptions[
                        index
                    ]
                )
            }
        )


        Spacer(
            modifier =
                Modifier.height(
                    24.dp
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
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
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
                            "🔔 Recordatorios",
                        color =
                            SettingsDark,
                        fontSize =
                            16.sp,
                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    Text(
                        text =
                            "VialApp te recordará estudiar según tu rutina.",
                        color =
                            SettingsGray,
                        fontSize =
                            12.sp,
                        lineHeight =
                            17.sp
                    )
                }


                Switch(
                    checked =
                        remindersEnabled,

                    onCheckedChange =
                        onRemindersEnabledChange
                )
            }
        }


        if (
            remindersEnabled
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            SectionTitle(
                emoji =
                    "📅",
                title =
                    "¿Cada cuánto?",
                subtitle =
                    "Elige el intervalo de tus recordatorios."
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                FrequencyOption(
                    title =
                        "Todos los días",
                    subtitle =
                        "Un recordatorio diario.",
                    selected =
                        reminderFrequency ==
                                ReminderFrequency.DAILY,
                    onClick = {

                        onReminderFrequencyChange(
                            ReminderFrequency.DAILY
                        )
                    }
                )


                FrequencyOption(
                    title =
                        "Lunes a viernes",
                    subtitle =
                        "Solo durante la semana.",
                    selected =
                        reminderFrequency ==
                                ReminderFrequency.WEEKDAYS,
                    onClick = {

                        onReminderFrequencyChange(
                            ReminderFrequency.WEEKDAYS
                        )
                    }
                )


                FrequencyOption(
                    title =
                        "Cada 2 días",
                    subtitle =
                        "Una rutina más relajada.",
                    selected =
                        reminderFrequency ==
                                ReminderFrequency.EVERY_TWO_DAYS,
                    onClick = {

                        onReminderFrequencyChange(
                            ReminderFrequency.EVERY_TWO_DAYS
                        )
                    }
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            SectionTitle(
                emoji =
                    "🕒",
                title =
                    "¿A qué hora?",
                subtitle =
                    "Puedes cambiarla cuando quieras."
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            val selectedTimeIndex =
                reminderTimeOptions
                    .indexOfFirst {

                        it.hour ==
                                reminderHour &&
                                it.minute ==
                                reminderMinute
                    }
                    .let {

                        if (
                            it >= 0
                        ) {
                            it
                        } else {
                            2
                        }
                    }


            OptionGrid(
                options =
                    reminderTimeOptions.map {
                        it.label
                    },

                selectedIndex =
                    selectedTimeIndex,

                onSelected = { index ->

                    val option =
                        reminderTimeOptions[
                            index
                        ]

                    onReminderTimeChange(
                        option.hour,
                        option.minute
                    )
                }
            )


            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )


            ManualTimePickerButton(
                hour =
                    reminderHour,

                minute =
                    reminderMinute,

                onTimeSelected =
                    onReminderTimeChange
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    28.dp
                )
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    18.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(
                            0xFFEAF8F1
                        )
                )
        ) {

            Text(
                text =
                    buildSummary(
                        dailyMinutes =
                            dailyMinutes,

                        remindersEnabled =
                            remindersEnabled,

                        reminderFrequency =
                            reminderFrequency,

                        reminderHour =
                            reminderHour,

                        reminderMinute =
                            reminderMinute
                    ),
                modifier =
                    Modifier.padding(
                        15.dp
                    ),
                color =
                    SettingsGreen,
                fontSize =
                    12.sp,
                fontWeight =
                    FontWeight.SemiBold,
                lineHeight =
                    18.sp
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )


        Button(
            onClick =
                onPrimaryAction,
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
                ButtonDefaults.buttonColors(
                    containerColor =
                        SettingsBlue
                )
        ) {

            Text(
                text =
                    primaryButtonText,
                fontSize =
                    15.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )
    }
}


@Composable
private fun SectionTitle(
    emoji: String,
    title: String,
    subtitle: String
) {
    Column {

        Text(
            text =
                "$emoji  $title",
            color =
                SettingsDark,
            fontSize =
                17.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    3.dp
                )
        )


        Text(
            text =
                subtitle,
            color =
                SettingsGray,
            fontSize =
                12.sp
        )
    }
}


@Composable
private fun OptionGrid(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        options
            .chunked(
                3
            )
            .forEachIndexed {
                    rowIndex,
                    row ->

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    row.forEachIndexed {
                            itemIndex,
                            text ->

                        val index =
                            rowIndex * 3 +
                                    itemIndex

                        SelectableOption(
                            text =
                                text,

                            selected =
                                index ==
                                        selectedIndex,

                            modifier =
                                Modifier.weight(
                                    1f
                                ),

                            onClick = {

                                onSelected(
                                    index
                                )
                            }
                        )
                    }


                    repeat(
                        3 -
                                row.size
                    ) {

                        Spacer(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )
                    }
                }
            }
    }
}


@Composable
private fun SelectableOption(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier =
            modifier
                .clickable(
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (
                        selected
                    ) {
                        Color(
                            0xFFEAF1FF
                        )
                    } else {
                        Color.White
                    }
            )
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 15.dp,
                        horizontal = 8.dp
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    text,
                color =
                    if (
                        selected
                    ) {
                        SettingsBlue
                    } else {
                        SettingsDark
                    },
                fontSize =
                    13.sp,
                fontWeight =
                    if (
                        selected
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    }
            )
        }
    }
}


@Composable
private fun FrequencyOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(
                17.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (
                        selected
                    ) {
                        Color(
                            0xFFEAF1FF
                        )
                    } else {
                        Color.White
                    }
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
                    if (
                        selected
                    ) {
                        "●"
                    } else {
                        "○"
                    },
                color =
                    if (
                        selected
                    ) {
                        SettingsBlue
                    } else {
                        SettingsGray
                    },
                fontSize =
                    18.sp
            )


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )


            Column {

                Text(
                    text =
                        title,
                    color =
                        SettingsDark,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.Bold
                )


                Text(
                    text =
                        subtitle,
                    color =
                        SettingsGray,
                    fontSize =
                        11.sp
                )
            }
        }
    }
}


private fun buildSummary(
    dailyMinutes: Int,
    remindersEnabled: Boolean,
    reminderFrequency: ReminderFrequency,
    reminderHour: Int,
    reminderMinute: Int
): String {
    if (
        !remindersEnabled
    ) {
        return "📚 Meta: $dailyMinutes minutos de estudio al día · Recordatorios desactivados."
    }

    val frequencyText =
        when (
            reminderFrequency
        ) {

            ReminderFrequency.DAILY ->
                "todos los días"

            ReminderFrequency.WEEKDAYS ->
                "de lunes a viernes"

            ReminderFrequency.EVERY_TWO_DAYS ->
                "cada 2 días"
        }

    val time =
        String.format(
            "%02d:%02d",
            reminderHour,
            reminderMinute
        )

    return "📚 Meta: $dailyMinutes min/día · 🔔 Recordatorio $frequencyText a las $time."
}
