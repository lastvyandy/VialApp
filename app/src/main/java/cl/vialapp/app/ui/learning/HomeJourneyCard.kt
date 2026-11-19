package cl.vialapp.app.ui.learning

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.LearningChapterSummary
import cl.vialapp.app.ui.career.drawGarageVehicle
import cl.vialapp.app.ui.career.garageColor

internal data class JourneyVehicle(val style:String,val paint:Color)
@Composable
internal fun rememberJourneyVehicle(userKey:String):JourneyVehicle {
    val c=LocalContext.current
    return remember(c,userKey) {
        val p=c.getSharedPreferences("career_appearance_"+userKey,Context.MODE_PRIVATE)
        JourneyVehicle(p.getString("style","compact")?:"compact",garageColor(p.getString("paint","#38A9F0")?:"#38A9F0"))
    }
}

internal fun isMotorcycleLicense(license:String?):Boolean =
    license?.trim()?.uppercase(java.util.Locale.ROOT)?.removePrefix("CLASE ")?.trim()=="C"

@Composable
internal fun HomeJourneyCard(
    userKey:String, licenseText:String, licenseClass:String?,
    chapters:List<LearningChapterSummary>?, loading:Boolean,
    onPath:()->Unit, onContinue:(String)->Unit, onExam:()->Unit,
    onPractice:()->Unit,error:String?=null,onRetry:()->Unit={}
) {
    val vehicle=rememberJourneyVehicle(userKey)
    val next=chapters?.let{it.getOrNull(nextChapterIndex(it))}
    val done=chapters?.count{it.progress.completed}?:0
    val count=chapters?.size?:0
    val full=count>0 && done==count
    val fg=Color(0xFFDCEAFF)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF172F58),Color(0xFF2255A1))))
        .padding(18.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){
        Text("TU CAMINO A LA LICENCIA",color=Color(0xFFBED6FF),fontSize=11.sp,fontWeight=FontWeight.Bold)
        Text(if(licenseText.startsWith("Licencia",ignoreCase=true))licenseText else "Licencia "+licenseText,color=Color.White,fontSize=27.sp,fontWeight=FontWeight.ExtraBold)
        Text(
            when {
                isMotorcycleLicense(licenseClass) &&
                    chapters?.any{it.id.startsWith("moto-")} == true ->
                    "Tu ruta de motociclista: conducción, protección y normas viales en un solo recorrido."
                isMotorcycleLicense(licenseClass) && loading ->
                    "Preparando tu camino de motociclista…"
                isMotorcycleLicense(licenseClass) ->
                    "Las etapas de motocicleta estarán disponibles cuando se actualice el servicio de aprendizaje."
                else -> "Tu ruta de conductor: aprende las normas y practica a tu ritmo."
            },
            color=fg,fontSize=12.sp
        )
        when {
            count>0 && chapters!=null -> {
                if(count<=15) JourneyPreview(chapters,vehicle,isMotorcycleLicense(licenseClass),Modifier.fillMaxWidth().height(114.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(done.toString()+" de "+count+" etapas completadas",color=fg,fontSize=13.sp,modifier=Modifier.weight(1f))
                    Text((done*100/count).toString()+"%",color=Color.White,fontWeight=FontWeight.Bold)
                }
                LinearProgressIndicator(progress={done.toFloat()/count},modifier=Modifier.fillMaxWidth().height(7.dp),
                    color=Color(0xFF8FE4BE),trackColor=Color(0xFF526E9B))
                HorizontalDivider(color=Color(0xFF5473A0))
                Text(if(full)"META DE ESTUDIO ALCANZADA" else "TU PRÓXIMA ETAPA",color=Color(0xFFBED6FF),fontSize=11.sp,fontWeight=FontWeight.Bold)
                Text(next?.title?:"¡Completaste el recorrido!",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold)
                if(next!=null){
                    val pending=next.lessons.firstOrNull{it.id !in next.progress.completedLessons}
                    Text(pending?.title ?: "Desafío final de la etapa",color=fg,fontSize=13.sp)
                } else Text("Repasa o comprueba tus conocimientos. Completar la ruta no garantiza aprobar.",color=fg,fontSize=12.sp)
            }
            loading -> {
                LinearProgressIndicator(Modifier.fillMaxWidth(),color=Color(0xFFBED6FF),trackColor=Color(0xFF526E9B))
                Text("Consultando tu avance…",color=fg,fontSize=13.sp)
            }
            else -> Text("Abre el recorrido para consultar tus etapas. El avance se sincroniza con tu cuenta.",color=fg,fontSize=13.sp)
        }
        if(error!=null){
            Text(if(count>0) "Último avance consultado · "+error else error,color=fg,fontSize=12.sp)
            TextButton(onClick=onRetry,colors=ButtonDefaults.textButtonColors(contentColor=Color.White)){Text("Reintentar conexión")}
        }
        Button(onClick={if(next!=null)onContinue(next.id) else onPath()},
            modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(15.dp),
            colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF193E79))){
            Text(if(full)"Repasar mi camino" else if(next!=null && done==0 && next.answeredSteps==0)"Comenzar mi camino" else "Continuar mi camino",fontWeight=FontWeight.Bold)
        }
        if(next!=null) TextButton(onClick=onPath,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
            colors=ButtonDefaults.textButtonColors(contentColor=fg)){Text("Ver recorrido completo")}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(onClick=onPractice,modifier=Modifier.weight(1f).heightIn(min=48.dp),
                border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF89A8D4)),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)){Text("Practicar",fontSize=13.sp)}
            OutlinedButton(onClick=onExam,modifier=Modifier.weight(1f).heightIn(min=48.dp),
                border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF89A8D4)),
                colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)){Text("Exámenes",fontSize=13.sp)}
        }
    }
}

@Composable
private fun JourneyPreview(chapters:List<LearningChapterSummary>,vehicle:JourneyVehicle,motorcycle:Boolean,modifier:Modifier){
    val next=nextChapterIndex(chapters)
    Canvas(modifier){
        val count=chapters.size+1
        val columns=(count+1)/2
        val margin=16.dp.toPx().coerceAtMost(size.width/5f)
        val top=size.height*.26f
        val bottom=size.height*.76f
        val dx=(size.width-2*margin)/(columns-1).coerceAtLeast(1)
        val positions=(0 until count).map{i->
            val row=i/columns
            val col=if(row==0)i else columns-1-i%columns
            Offset(margin+col*dx,if(row==0)top else bottom)
        }
        val road=Path().apply{
            val first=positions.first()
            moveTo(first.x,first.y)
            for(i in 1 until positions.size){
                val a=positions[i-1];val b=positions[i]
                if(i==columns){
                    val turn=margin*.75f
                    cubicTo(a.x+turn,a.y,b.x+turn,b.y,b.x,b.y)
                }else lineTo(b.x,b.y)
            }
        }
        drawPath(road,Color(0xFF0D2347),style=Stroke(width=22.dp.toPx(),cap=StrokeCap.Round))
        drawPath(road,Color(0xFFB8CBE7),style=Stroke(width=1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),5.dp.toPx()))))
        val r=minOf(10.dp.toPx(),dx*.34f)
        for(i in chapters.indices){
            val p=positions[i]
            if(chapters[i].progress.completed){
                drawCircle(Color(0xFF2CA77C),r,p)
                drawLine(Color.White,p+Offset(-r*.47f,0f),p+Offset(-r*.10f,r*.38f),2.dp.toPx(),StrokeCap.Round)
                drawLine(Color.White,p+Offset(-r*.10f,r*.38f),p+Offset(r*.53f,-r*.42f),2.dp.toPx(),StrokeCap.Round)
            }else if(i==next){
                drawCircle(Color(0x557FB8FF),r*1.7f,p)
                if(motorcycle) drawJourneyMotorcycle(p,minOf(r*2.25f,23.dp.toPx()),vehicle.paint)
                else drawJourneyVehicle(vehicle,p,minOf(r*2.25f,23.dp.toPx()))
            }else{
                drawCircle(Color(0xFF536E94),r*.77f,p)
                drawCircle(Color(0xFFB1C6E4),r*.23f,p)
            }
        }
        val goal=positions.last()
        drawCircle(Color(0xFFFFD477),r*1.15f,goal)
        val x=goal.x-r*.35f;val y=goal.y-r*.55f
        drawLine(Color(0xFF6D4914),Offset(x,y),Offset(x,goal.y+r*.56f),2.dp.toPx(),StrokeCap.Round)
        val flag=Path().apply{moveTo(x,y);lineTo(x+r*1.05f,y);lineTo(x+r*.80f,y+r*.38f);lineTo(x+r*1.05f,y+r*.70f);lineTo(x,y+r*.70f);close()}
        drawPath(flag,Color(0xFF6D4914))
    }
}

/** Reuse the light-weight garage top view, rather than rebuilding 3D meshes in the scroll list. */
internal fun DrawScope.drawJourneyVehicle(vehicle:JourneyVehicle,center:Offset,width:Float){
    val factor=width/104f
    scale(factor,factor,pivot=center){
        drawGarageVehicle(center,vehicle.paint,vehicle.style)
    }
}


/** Side-view motorcycle marker for the Clase C route, drawn locally without image assets. */
internal fun DrawScope.drawJourneyMotorcycle(center:Offset,width:Float,paint:Color){
    val w=width.coerceAtLeast(4f)
    val dark=Color(0xFF13223A)
    val steel=Color(0xFFDDECF7)
    val left=Offset(center.x-w*.32f,center.y+w*.16f)
    val right=Offset(center.x+w*.32f,center.y+w*.16f)
    val wheelR=w*.16f
    drawCircle(dark,wheelR*1.20f,left)
    drawCircle(dark,wheelR*1.20f,right)
    drawCircle(steel,wheelR*.75f,left)
    drawCircle(steel,wheelR*.75f,right)
    drawCircle(dark,wheelR*.45f,left)
    drawCircle(dark,wheelR*.45f,right)
    val top=Offset(center.x-w*.07f,center.y-w*.13f)
    drawLine(paint,left,top,strokeWidth=w*.11f,cap=StrokeCap.Round)
    drawLine(paint,top,right,strokeWidth=w*.11f,cap=StrokeCap.Round)
    drawLine(paint,left,Offset(center.x+w*.03f,center.y+w*.16f),
        strokeWidth=w*.09f,cap=StrokeCap.Round)
    drawLine(steel,Offset(center.x+w*.08f,center.y-w*.21f),right,
        strokeWidth=w*.07f,cap=StrokeCap.Round)
    drawLine(steel,Offset(center.x+w*.03f,center.y-w*.23f),
        Offset(center.x+w*.22f,center.y-w*.23f),strokeWidth=w*.07f,cap=StrokeCap.Round)
    drawLine(dark,Offset(center.x-w*.28f,center.y-w*.11f),
        Offset(center.x-w*.01f,center.y-w*.11f),strokeWidth=w*.12f,cap=StrokeCap.Round)
    drawCircle(paint,w*.115f,Offset(center.x-w*.10f,center.y-w*.32f))
}
