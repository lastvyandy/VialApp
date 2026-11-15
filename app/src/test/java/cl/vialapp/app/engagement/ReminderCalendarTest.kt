package cl.vialapp.app.engagement

import cl.vialapp.app.data.settings.ReminderFrequency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class ReminderCalendarTest {
    private val friday=ZonedDateTime.parse("2026-11-13T20:00:00+01:00[Europe/Paris]")
    @Test fun dailyMovesToTomorrowAfterTime(){
        assertEquals("2026-11-14",nextStudyReminder(friday,ReminderFrequency.DAILY,19,0,friday.toLocalDate()).toLocalDate().toString())
    }
    @Test fun skipsWeekend(){
        assertEquals("2026-11-16",nextStudyReminder(friday,ReminderFrequency.WEEKDAYS,19,0,friday.toLocalDate()).toLocalDate().toString())
    }
    @Test fun alternateDaysUseStableAnchor(){
        val anchor=LocalDate.parse("2026-11-12")
        assertEquals("2026-11-14",nextStudyReminder(friday,ReminderFrequency.EVERY_TWO_DAYS,19,0,anchor).toLocalDate().toString())
    }
    @Test fun futureReminderKeepsToday(){
        assertEquals(21,nextStudyReminder(friday,ReminderFrequency.DAILY,21,10,friday.toLocalDate()).hour)
    }
    @Test fun handlesDaylightSavingGap(){
        val before=ZonedDateTime.parse("2026-03-29T00:30:00+01:00[Europe/Paris]")
        assertEquals(3,nextStudyReminder(before,ReminderFrequency.DAILY,2,30,before.toLocalDate()).hour)
    }
}
