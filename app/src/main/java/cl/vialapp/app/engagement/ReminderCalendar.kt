package cl.vialapp.app.engagement

// GUÍA DEL ARCHIVO:
// Fechas de los recordatorios, para no depender de cómo se muestran en pantalla.

import cl.vialapp.app.data.settings.ReminderFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

internal fun nextStudyReminder(now:ZonedDateTime,frequency:ReminderFrequency,hour:Int,minute:Int,anchor:LocalDate):ZonedDateTime {
    val time=java.time.LocalTime.of(hour.coerceIn(0,23),minute.coerceIn(0,59))
    for(offset in 0L..7L) {
        val day=now.toLocalDate().plusDays(offset)
        val eligible=when(frequency){
            ReminderFrequency.DAILY->true
            ReminderFrequency.WEEKDAYS->day.dayOfWeek!=DayOfWeek.SATURDAY && day.dayOfWeek!=DayOfWeek.SUNDAY
            ReminderFrequency.EVERY_TWO_DAYS->Math.floorMod(ChronoUnit.DAYS.between(anchor,day),2L)==0L
        }
        val candidate=day.atTime(time).atZone(now.zone)
        if(eligible&&candidate.isAfter(now))return candidate
    }
    error("No valid reminder date")
}
