package cl.vialapp.app.engagement

// GUÍA DEL ARCHIVO:
// Widget de XP y racha. RemoteViews no usa los mismos recortes que Compose, ojo ahí.

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.widget.RemoteViews
import cl.vialapp.app.R
import cl.vialapp.app.data.games.GameProgressStore
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/** Compact, personal study overview for the launcher; all data comes from local
 * account-specific snapshots, so a slow server never stalls widget updates. */
class StudyWidget:AppWidgetProvider(){
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
        for(id in ids)runCatching{render(context,manager,id)}
            .onFailure{android.util.Log.w("VialWidget","Widget update failed",it)}
    }
    override fun onAppWidgetOptionsChanged(context:Context,manager:AppWidgetManager,id:Int,newOptions:Bundle){
        runCatching{render(context,manager,id)}
            .onFailure{android.util.Log.w("VialWidget","Widget resize failed",it)}
    }
    companion object {
        private const val XP_PER_LEVEL=500
        private val mascotCache=ConcurrentHashMap<Int,Bitmap>()

        internal fun updateAll(c:Context){
            val manager=AppWidgetManager.getInstance(c)
            for(id in manager.getAppWidgetIds(ComponentName(c,StudyWidget::class.java))){
                runCatching{render(c,manager,id)}
                    .onFailure{android.util.Log.w("VialWidget","Widget update failed",it)}
            }
        }

        // Leemos el último avance local. El widget no debería quedar congelado esperando internet.
        private fun render(c:Context,manager:AppWidgetManager,id:Int){
            val user=EngagementState.activeUser(c)
            val xp=user?.let{EngagementState.xp(c,it)}
            val streak=user?.let{EngagementState.streak(c,it)}
            val studied=user?.let{EngagementState.studiedToday(c,it)}==true
            val options=manager.getAppWidgetOptions(id)
            val minWidth=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,250)
            val minHeight=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,150)
            val compact=minWidth in 1..219 || minHeight in 1..135
            val layout=if(compact)R.layout.widget_study_compact else R.layout.widget_study
            val views=RemoteViews(c.packageName,layout)
            val mascot=if(user!=null&&EngagementState.mascot(c)=="mister_pelao")
                R.drawable.mascot_mister_pelao else R.drawable.mascot_carvier
            runCatching{views.setImageViewBitmap(R.id.widget_mascot,circularMascot(c,mascot))}
                .onFailure{views.setImageViewResource(R.id.widget_mascot,mascot)}

            val mission=user?.let{
                runCatching{GameProgressStore(c,it).dailyMissionSnapshot()}.getOrNull()
            }
            val missionText=when{
                user==null->"Tu próxima misión comienza aquí"
                mission==null->"Abre VialApp para revisar tu misión diaria"
                mission.claimed->"Misión del día · Recompensa cobrada"
                mission.completed->"Misión completa · Reclama +"+mission.rewardXp+" XP"
                else->mission.title+" · "+mission.progress+"/"+mission.goal
            }
            val streakText=when{
                user==null->"Tu ruta empieza hoy"
                studied->"✓ Práctica de hoy completada"
                streak!=null&&streak>0->"Racha: "+streak+(if(streak==1)" día" else " días")
                else->"Un poco más cada día"
            }
            views.setTextViewText(R.id.widget_level,when{
                user==null->"Bienvenido"
                xp==null->"Tu progreso"
                else->"Nivel "+(xp/XP_PER_LEVEL+1)
            })
            views.setTextViewText(R.id.widget_streak,streakText)
            views.setTextViewText(R.id.widget_xp,when{
                user==null->"Inicia sesión para ver tu XP"
                xp==null->"Sincroniza tu progreso al abrir VialApp"
                else->(xp%XP_PER_LEVEL).toString()+"/"+XP_PER_LEVEL+" XP · "+xp+" total"
            })
            views.setTextViewText(R.id.widget_today,missionText)
            views.setProgressBar(R.id.widget_progress,XP_PER_LEVEL,(xp?:0)%XP_PER_LEVEL,false)
            views.setTextViewText(R.id.widget_action,if(user==null)"ABRIR ›" else "PRACTICAR ›")
            val target=StudyReminder.openPractice(c,413)
            views.setOnClickPendingIntent(R.id.widget_action,target)
            views.setOnClickPendingIntent(R.id.widget_root,target)
            manager.updateAppWidget(id,views)
        }

        /**
         * Android RemoteViews cannot apply a Compose clip modifier. Generate a
         * small real circular crop once per mascot and share it across widgets.
         * Source WebP images can be large; sample them before allocating pixels.
         */
        private fun circularMascot(c:Context,res:Int):Bitmap{
            mascotCache[res]?.let{return it}
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeResource(c.resources,res,bounds)
            var sample=1
            while(bounds.outWidth/sample>320&&bounds.outHeight/sample>320)sample*=2
            val source=BitmapFactory.decodeResource(c.resources,res,
                BitmapFactory.Options().apply{inSampleSize=sample})
                ?:error("Could not decode widget mascot")
            val width=144
            val bitmap=Bitmap.createBitmap(width,width,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(bitmap)
            val cx=width/2f
            val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color=Color.rgb(43,85,101)
            canvas.drawCircle(cx,cx,cx-2f,paint)

            val shader=BitmapShader(source,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP)
            val scale=max(width.toFloat()/source.width,width.toFloat()/source.height)
            val matrix=Matrix()
            matrix.setScale(scale,scale)
            matrix.postTranslate((width-source.width*scale)/2f,(width-source.height*scale)/2f)
            shader.setLocalMatrix(matrix)
            paint.shader=shader
            canvas.drawCircle(cx,cx,cx-7f,paint)
            paint.shader=null
            paint.color=Color.rgb(121,207,190)
            paint.style=Paint.Style.STROKE
            paint.strokeWidth=5f
            canvas.drawCircle(cx,cx,cx-4f,paint)
            source.recycle()
            mascotCache.putIfAbsent(res,bitmap)
            return mascotCache[res]?:bitmap
        }
    }
}
