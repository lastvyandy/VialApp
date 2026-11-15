package cl.vialapp.app.engagement

import android.content.Context
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.data.api.GamificationProgressResponse
import java.time.LocalDate
import java.time.ZoneId

/** Only the verified signed-in account may be visible on the device's launcher. */
internal object EngagementState {
    private fun prefs(c:Context)=c.getSharedPreferences("vialapp_engagement",Context.MODE_PRIVATE)
    fun activeUser(c:Context):String? =
        if(SessionManager(c).getAccessToken().isNullOrBlank()) null else prefs(c).getString("active_user",null)
    fun mascot(c:Context)=prefs(c).getString("mascot","carvier")
    fun activate(c:Context,user:String,mascot:String?){
        prefs(c).edit().putString("active_user",user).putString("mascot",mascot?:"carvier").apply()
        StudyReminder.schedule(c)
        StudyWidget.updateAll(c)
    }
    fun deactivate(c:Context){
        prefs(c).edit().remove("active_user").remove("mascot").apply()
        StudyReminder.cancel(c)
        StudyWidget.updateAll(c)
    }
    private fun snapshots(c:Context,user:String)=c.getSharedPreferences("vialapp_engagement_progress_"+user,Context.MODE_PRIVATE)
    fun recordProgress(c:Context,user:String,remote:GamificationProgressResponse){
        val editor=snapshots(c,user).edit().putInt("xp",remote.totalXp.coerceAtLeast(0))
        if(remote.streakDays!=null&&remote.streakActiveToday!=null){
            editor.putInt("streak",remote.streakDays.coerceAtLeast(0))
                .putString("streak_date",LocalDate.now(ZoneId.of("America/Santiago")).toString())
                .putBoolean("studied",remote.streakActiveToday)
        }
        editor.apply()
        if(activeUser(c)==user) StudyWidget.updateAll(c)
    }
    fun markStudy(c:Context,user:String){
        snapshots(c,user).edit().putString("local_study_date",LocalDate.now().toString()).apply()
        if(activeUser(c)==user)StudyWidget.updateAll(c)
    }
    fun studiedToday(c:Context,user:String):Boolean{
        val p=snapshots(c,user)
        return p.getString("local_study_date",null)==LocalDate.now().toString() ||
            (p.getString("streak_date",null)==LocalDate.now(ZoneId.of("America/Santiago")).toString() && p.getBoolean("studied",false))
    }
    fun xp(c:Context,user:String):Int? {
        val p=snapshots(c,user)
        return if(p.contains("xp"))p.getInt("xp",0) else null
    }
    fun streak(c:Context,user:String):Int?{
        val p=snapshots(c,user)
        val date=runCatching{LocalDate.parse(p.getString("streak_date",""))}.getOrNull()?:return null
        val today=LocalDate.now(ZoneId.of("America/Santiago"))
        val days=p.getInt("streak",0)
        return when {
            date==today->days
            date==today.minusDays(1)&&p.getBoolean("studied",false)->days
            date.isBefore(today)->0
            else->null
        }
    }
}
