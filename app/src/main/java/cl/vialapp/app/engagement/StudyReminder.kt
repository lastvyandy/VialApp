package cl.vialapp.app.engagement

// GUÍA DEL ARCHIVO:
// Notificaciones para practicar. Respetamos horarios y permisos; no queremos un despertador cargante.

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.vialapp.app.MainActivity
import cl.vialapp.app.R
import cl.vialapp.app.data.settings.StudyPreferencesStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId

internal object StudyReminder {
    const val OPEN_PRACTICE="cl.vialapp.app.OPEN_PRACTICE"
    private const val ALARM="cl.vialapp.app.STUDY_REMINDER"
    private const val CHANNEL="study_reminders"
    private const val NOTIFICATION_ID=410
    private fun prefs(c:Context)=c.getSharedPreferences("vialapp_reminder_schedule",Context.MODE_PRIVATE)
    fun channel(c:Context){
        val manager=c.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(NotificationChannel(CHANNEL,"Recordatorios de estudio",NotificationManager.IMPORTANCE_DEFAULT).apply{
            description="Avisos según tu rutina de práctica de VialApp"
        })
    }
    fun openPractice(c:Context,code:Int)=PendingIntent.getActivity(c,code,
        Intent(c,MainActivity::class.java).setAction(OPEN_PRACTICE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun alarmIntent(c:Context)=PendingIntent.getBroadcast(c,411,
        Intent(c,StudyReminderReceiver::class.java).setAction(ALARM),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun cancel(c:Context){
        c.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent(c))
        prefs(c).edit().remove("scheduled_user").remove("scheduled_at").apply()
        c.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }
    fun configure(c:Context,user:String){
        if(EngagementState.activeUser(c)!=user)return
        val p=prefs(c)
        val frequency=StudyPreferencesStore(c,user).get().reminderFrequency.name
        if(p.getString("frequency_"+user,null)!=frequency){
            p.edit().putString("frequency_"+user,frequency).putString("anchor_"+user,LocalDate.now().toString()).apply()
        }
        schedule(c)
    }
    fun schedule(c:Context){
        val user=EngagementState.activeUser(c)
        if(user==null){cancel(c);return}
        val settings=StudyPreferencesStore(c,user).get()
        if(!settings.configured || !settings.remindersEnabled){cancel(c);return}
        channel(c)
        val p=prefs(c)
        val now=ZonedDateTime.now()
        val anchor=runCatching{LocalDate.parse(p.getString("anchor_"+user,""))}.getOrNull()?:now.toLocalDate().also{
            p.edit().putString("anchor_"+user,it.toString()).putString("frequency_"+user,settings.reminderFrequency.name).apply()
        }
        val at=nextStudyReminder(now,settings.reminderFrequency,settings.reminderHour,settings.reminderMinute,anchor).toInstant().toEpochMilli()
        p.edit().putString("scheduled_user",user).putLong("scheduled_at",at).apply()
        c.getSystemService(AlarmManager::class.java)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,alarmIntent(c))
    }
    fun deliver(c:Context){
        val p=prefs(c)
        val user=EngagementState.activeUser(c)
        if(user==null){cancel(c);return}
        val settings=StudyPreferencesStore(c,user).get()
        val scheduledAt=p.getLong("scheduled_at",0L)
        val now=System.currentTimeMillis()
        val today=LocalDate.now()
        val valid=p.getString("scheduled_user",null)==user && scheduledAt>0 && now>=scheduledAt &&
            Instant.ofEpochMilli(scheduledAt).atZone(ZoneId.systemDefault()).toLocalDate()==today
        if(valid&&settings.configured&&settings.remindersEnabled&&
            p.getString("notified_"+user,null)!=today.toString() && !EngagementState.studiedToday(c,user) &&
            canNotify(c)){
            val message="Dedica unos "+settings.dailyStudyMinutes+" minutos a practicar las normas de tránsito."
            channel(c)
            val notification=NotificationCompat.Builder(c,CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_road)
                .setContentTitle("Un paso más hacia tu licencia")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(openPractice(c,412))
                .setAutoCancel(true).setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setColor(0xFF2563EB.toInt()).build()
            try{
                NotificationManagerCompat.from(c).notify(NOTIFICATION_ID,notification)
                p.edit().putString("notified_"+user,today.toString()).apply()
            }catch(_:SecurityException){ /* permission changed after check */ }
        }
        schedule(c)
        StudyWidget.updateAll(c)
    }
    fun canNotify(c:Context):Boolean{
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false
        return NotificationManagerCompat.from(c).areNotificationsEnabled() &&
            c.getSystemService(NotificationManager::class.java)?.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE
    }
}

class StudyReminderReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        when(intent.action){
            "cl.vialapp.app.STUDY_REMINDER"->StudyReminder.deliver(context)
            Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED->{StudyReminder.schedule(context);StudyWidget.updateAll(context)}
        }
    }
}
