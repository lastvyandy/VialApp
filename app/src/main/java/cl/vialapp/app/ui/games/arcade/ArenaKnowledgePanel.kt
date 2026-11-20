package cl.vialapp.app.ui.games.arcade

// GUÍA DEL ARCHIVO:
// Muestra preguntas y explicaciones durante la arena. Jugar también debería enseñar.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ArenaKnowledgePanel(f:ArenaCombatFrame,modifier:Modifier,enabled:Boolean,act:(Int)->Unit){
    val q=f.question?:return
    Column(modifier.background(Color(0xFF17283F),RoundedCornerShape(18.dp)).padding(12.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("ROMPE EL ESCUDO",color=Color(0xFFFFD57B),fontSize=13.sp,fontWeight=FontWeight.ExtraBold)
            Text("TIEMPO EN PAUSA",color=Color(0xFFBCD9F6),fontSize=10.sp)
        }
        key(q){
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
                KnowledgeArt(q.illustration,Modifier.fillMaxWidth().height(100.dp))
                Text(q.prompt,color=Color.White,fontWeight=FontWeight.Bold,fontSize=17.sp,lineHeight=22.sp)
                f.answerFeedback?.let{explanation->
                    Text("El escudo resiste · Nuevo desafío",color=Color(0xFFFFB0B0),fontWeight=FontWeight.Bold,fontSize=13.sp)
                    Text(explanation,color=Color(0xFFDFEAFE),fontSize=13.sp,lineHeight=18.sp)
                }
                q.answers.forEachIndexed{index,label->
                    val fill=Color(0xFF344D69)
                    Button(onClick={act(20+index)},enabled=enabled&&f.selected<0,modifier=Modifier.fillMaxWidth().heightIn(min=46.dp),
                        shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=fill,contentColor=Color.White,disabledContainerColor=fill,disabledContentColor=Color.White)){
                        Text(label,fontSize=13.sp)
                    }
                }

            }
        }
        Text("Dos escudos por rival · el conocimiento importa",color=Color(0xFFB6CAE1),fontSize=11.sp)
    }
}

@Composable
private fun KnowledgeArt(art:ArenaIllustration,modifier:Modifier){
    val sign=when(art){
        ArenaIllustration.STOP->RoadSign.STOP;ArenaIllustration.YIELD->RoadSign.YIELD
        ArenaIllustration.SCHOOL->RoadSign.SCHOOL;ArenaIllustration.SPEED->RoadSign.MAX_50;else->null
    }
    Box(modifier.background(Color(0xFF263D54),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){
        if(sign!=null)RoadSignArt(sign,Modifier.fillMaxHeight(.89f).aspectRatio(1f))
        else Canvas(Modifier.fillMaxSize().padding(9.dp)){
            val w=size.width;val h=size.height
            val asphalt=Color(0xFF4D5B6F);val white=Color(0xFFE9F3FB)
            fun car(x:Float,y:Float,ink:Color){
                drawRoundRect(ink,Offset(x-w*.065f,y-h*.19f),Size(w*.13f,h*.38f),CornerRadius(6f))
                drawRoundRect(Color(0xFF243B51),Offset(x-w*.046f,y-h*.10f),Size(w*.092f,h*.12f),CornerRadius(3f))
            }
            when(art){
                ArenaIllustration.ROUNDABOUT->{
                    drawCircle(asphalt,h*.46f,center);drawCircle(Color(0xFF549176),h*.22f,center)
                    drawLine(asphalt,Offset(w*.5f,h),center+Offset(0f,h*.30f),h*.30f)
                    car(w*.5f+h*.33f,h*.5f,Color(0xFFF2AD51));car(w*.5f,h*.87f,Color(0xFF60A5FA))
                }
                ArenaIllustration.RED_LIGHT->{
                    drawRoundRect(Color(0xFF111B29),Offset(w*.43f,0f),Size(w*.14f,h),CornerRadius(8f))
                    for(i in 0..2)drawCircle(if(i==0)Color(0xFFFF4D55)else Color(0xFF36434F),h*.115f,Offset(w*.5f,h*(.18f+i*.32f)))
                }
                ArenaIllustration.PHONE->{
                    drawRoundRect(Color(0xFF111B29),Offset(w*.41f,0f),Size(w*.18f,h),CornerRadius(9f))
                    drawRoundRect(Color(0xFF81B2DC),Offset(w*.43f,h*.10f),Size(w*.14f,h*.68f),CornerRadius(4f))
                    drawCircle(Color(0xFFE75E5E),h*.43f,center,style=Stroke(5f))
                    drawLine(Color(0xFFE75E5E),center-Offset(h*.30f,h*.30f),center+Offset(h*.30f,h*.30f),5f)
                }
                ArenaIllustration.BELT->{
                    drawRoundRect(Color(0xFF4D6075),Offset(w*.36f,h*.12f),Size(w*.28f,h*.84f),CornerRadius(12f))
                    drawCircle(Color(0xFFE8AF7D),h*.16f,Offset(w*.5f,h*.20f))
                    drawRoundRect(Color(0xFF68A4DB),Offset(w*.42f,h*.36f),Size(w*.16f,h*.48f),CornerRadius(9f))
                    drawLine(white,Offset(w*.43f,h*.32f),Offset(w*.57f,h*.84f),h*.095f)
                    drawRoundRect(Color(0xFFF6BA58),Offset(w*.55f,h*.77f),Size(w*.055f,h*.13f),CornerRadius(3f))
                }
                else->{
                    drawRect(asphalt,Offset(w*.29f,0f),Size(w*.42f,h))
                    for(i in 0..4)drawLine(white,Offset(w*.5f,h*i/5f),Offset(w*.5f,h*i/5f+h*.08f),2f)
                    car(w*.40f,h*.25f,Color(0xFFF0AA53));car(w*.40f,h*.79f,Color(0xFF60A5FA))
                    if(art==ArenaIllustration.RAIN)for(i in 0..15){val x=w*(i%8)/8f;val y=h*(i/8)*.5f;drawLine(Color(0xFFAAC9EA),Offset(x,y),Offset(x-5f,y+17f),2f)}
                    else drawLine(Color(0xFFFFD57B),Offset(w*.6f,h*.33f),Offset(w*.6f,h*.69f),3f)
                }
            }
        }
    }
}
