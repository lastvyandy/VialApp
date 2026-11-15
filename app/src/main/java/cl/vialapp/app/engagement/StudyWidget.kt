package cl.vialapp.app.engagement

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import cl.vialapp.app.R

class StudyWidget:AppWidgetProvider(){
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
        for(id in ids) render(context,manager,id)
    }
    override fun onAppWidgetOptionsChanged(context:Context,manager:AppWidgetManager,id:Int,newOptions:Bundle){
        render(context,manager,id)
    }
    companion object {
        internal fun updateAll(c:Context){
            val manager=AppWidgetManager.getInstance(c)
            for(id in manager.getAppWidgetIds(ComponentName(c,StudyWidget::class.java))) {
                runCatching { render(c,manager,id) }.onFailure { android.util.Log.w("VialWidget","Widget update failed",it) }
            }
        }
        private fun render(c:Context,manager:AppWidgetManager,id:Int){
            val user=EngagementState.activeUser(c)
            val xp=user?.let{EngagementState.xp(c,it)}
            val streak=user?.let{EngagementState.streak(c,it)}
            val studied=user?.let{EngagementState.studiedToday(c,it)}==true
            val compact=manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250)<220
            val views=RemoteViews(c.packageName,if(compact)R.layout.widget_study_compact else R.layout.widget_study)
            views.setImageViewResource(R.id.widget_mascot,if(user!=null&&EngagementState.mascot(c)=="mister_pelao")R.drawable.mascot_mister_pelao else R.drawable.mascot_carvier)
            views.setTextViewText(R.id.widget_level,when{user==null->"Tu ruta empieza aquí";xp==null->"Tu progreso";else->"Nivel "+(xp/500+1)})
            views.setTextViewText(R.id.widget_xp,when{user==null->"Inicia sesión en VialApp";xp==null->"Abre la app para sincronizar";else->(xp%500).toString()+"/500 XP · Guardado"})
            views.setTextViewText(R.id.widget_streak,when{user==null->"Aprende a tu ritmo";studied->"Estudio de hoy completado";streak!=null&&streak>0->"Racha: "+streak+(if(streak==1)" día" else " días");else->"Un poco más cada día"})
            views.setProgressBar(R.id.widget_progress,500,(xp?:0)%500,false)
            views.setTextViewText(R.id.widget_action,if(user==null)"Abrir VialApp" else "Practicar  ›")
            val target=StudyReminder.openPractice(c,413)
            views.setOnClickPendingIntent(R.id.widget_action,target)
            views.setOnClickPendingIntent(R.id.widget_root,target)
            manager.updateAppWidget(id,views)
        }
    }
}
