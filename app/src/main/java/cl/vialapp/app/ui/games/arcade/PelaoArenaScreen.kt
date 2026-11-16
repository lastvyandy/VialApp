package cl.vialapp.app.ui.games.arcade

import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.games.GameProgressStore
import kotlin.math.sin

@Composable
fun PelaoArenaScreen(userKey:String,onBack:()->Unit){
    val audio=LocalGameAudio.current
    val sound=audio?.enabled==true
    var previous by remember{mutableStateOf<ArenaCombatFrame?>(null)}
    @Composable fun Feedback(f:ArenaCombatFrame,enabled:Boolean){
        LaunchedEffect(f.score,f.hp,f.phase,f.rival,f.move,enabled,sound){
            val old=previous
            if(enabled&&sound&&old!=null&&f.elapsed>=old.elapsed){
                when{
                    f.phase==ArenaPhase.SHIELD_BREAK&&old.phase!=f.phase->audio?.play("shield")
                    f.phase==ArenaPhase.ANSWER_IMPACT&&old.phase!=f.phase&&f.answerCorrect->audio?.play("power")
                    f.phase==ArenaPhase.ANSWER_IMPACT&&f.answerApplied&&!old.answerApplied->audio?.play(if(f.answerCorrect)"correct" else "heavy")
                    f.move==ArenaMove.SPECIAL&&old.move!=f.move->audio?.play("power")
                    f.defendFlash>0f&&f.score>old.score->audio?.play("block")
                    f.hp<old.hp->audio?.play("heavy")
                    f.rivalHp<old.rivalHp&&f.rival==old.rival&&f.phase!=ArenaPhase.KNOWLEDGE->audio?.play(if(f.lastImpact==ArenaMove.JAB)"hit" else "heavy")
                }
            }
            previous=f
        }
    }
    ArcadeSession(userKey=userKey,gameId=GameProgressStore.PELAO_ARENA,title="Don Pelao Arena",
        subtitle="Arcade de combate · tres rondas · seis escudos",
        rules="Mantén el mando para moverte. Combina puños y patadas, salta para evitar barridos y usa el dash para esquivar. Encadena ataques en menos de un segundo; puedes preparar el siguiente golpe antes de terminar el actual. El especial lanza una onda al 100% de energía. Dos escudos educativos por rival: responde para romperlos. Al responder verás el resultado en la arena. El cronómetro se detiene durante los desafíos y las animaciones de respuesta.",
        duration=ArenaCombatEngine.TIME,create={ArenaCombatEngine()},onBack=onBack,showPlayMessages=false,compactCombatHud=true,
        scene={f,m,enabled,act->
            Feedback(f,enabled)
            if(f.phase==ArenaPhase.KNOWLEDGE && f.question!=null) ArenaKnowledgePanel(f,m,enabled,act)
            else CombatScene(f,m)
        },resultArt={f,m->Feedback(f,true);Canvas(m){val u=minOf(size.width/240f,size.height/240f);translate(size.width/2,size.height*.93f){scale(u,u,pivot=Offset.Zero){drawPelao(f)}}}},
        controls={f,enabled,act->if(f.phase!=ArenaPhase.KNOWLEDGE) ArenaControls(f,enabled,act,sound){audio?.setEffectsEnabled(!sound)}})
}

@Composable
private fun CombatScene(f:ArenaCombatFrame,modifier:Modifier){
    val name=when(f.rival){1->"IMPRUDENTE";2->"DISTRAÍDO";else->"TEMERARIO"}
    val ink=when(f.rival){1->Color(0xFFF7AA44);2->Color(0xFF73C8F5);else->Color(0xFFED647C)}
    val tell=f.phase==ArenaPhase.WINDUP
    val heavy=tell&&f.attack==RivalAttack.CHARGE
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF101C2E)).padding(9.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
            CombatHealth("DON PELAO",f.hp,100,Color(0xFF73E0AC),Modifier.weight(1f))
            key(f.rival){CombatHealth(name,f.rivalHp,f.rivalMax,ink,Modifier.weight(1f),2-f.shieldsBroken)}
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
            Text("RIVAL "+f.rival.coerceAtMost(3)+"/3 · ESCUDOS "+(2-f.shieldsBroken),color=Color(0xFFBCD2EF),fontSize=11.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={f.power/100f},modifier=Modifier.weight(1f).height(7.dp),color=Color(0xFFF9C950),trackColor=Color(0xFF334860))
            Text("POWER "+f.power+"%",color=Color(0xFFFFDF83),fontSize=10.sp,fontWeight=FontWeight.Bold)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(13.dp))){
            Canvas(Modifier.fillMaxSize()){
                val w=size.width;val h=size.height
                val floor=h*.81f
                drawArenaStage(f,floor)
                for(x in listOf(f.playerX,f.rivalX))drawOval(Color.Black.copy(alpha=.42f),Offset(w*x-w*.092f,floor-7f),Size(w*.184f,14f))
                if(tell){
                    val range=f.attack.range*w
                    drawRoundRect((if(heavy)Color(0xFFEF4444) else Color(0xFFFBBF24)).copy(alpha=.24f),
                        Offset((f.rivalX*w-range).coerceAtLeast(0f),floor-9f),Size(range,18f),CornerRadius(6f))
                }
                val unit=minOf(w/330f,h/260f)
                val shake=if(f.impact>0f)sin(f.impact*56f)*(f.impact/.30f)*unit*4f else 0f
                val answer=f.phase==ArenaPhase.ANSWER_IMPACT
                val p=f.answerProgress
                val correct=f.answerCorrect
                val rush=if(answer&&!correct)sin((p/.45f).coerceIn(0f,1f)*1.5707963f)*(1f-((p-.45f)/.55f).coerceIn(0f,1f)) else 0f
                val recoilBeat=if(answer&&p>.45f)sin(((p-.45f)/.55f).coerceIn(0f,1f)*3.1415927f) else 0f
                val cx=w*f.rivalX+shake+(if(answer&&correct)recoilBeat*unit*17f else -rush*minOf(w*.10f,f.distance*w*.35f))
                val cy=floor
                val lift=f.jumpHeight*minOf(h*.16f,w*.15f)
                val px=w*f.playerX
                val recoil=if(answer&&!correct)-recoilBeat*unit*16f else if(f.playerImpact>0f)sin(f.playerImpact*40f)*4f*unit else 0f
                val enemyPose=if(answer){
                    if(correct)f.copy(rivalStun=if(p>.45f)recoilBeat*.36f else 0f)
                    else f.copy(phase=if(p<.32f)ArenaPhase.WINDUP else if(p<.66f)ArenaPhase.STRIKE else ArenaPhase.RECOVER,rivalStun=0f,attack=RivalAttack.CHARGE)
                }else f
                translate(cx,cy){scale(unit,unit,pivot=Offset.Zero){drawArenaRival(enemyPose,ink)}}
                if(f.phase==ArenaPhase.SHIELD_BREAK){
                    val p=(1f-f.phaseTime/.62f).coerceIn(0f,1f)
                    val radius=(45f+65f*p)*unit
                    drawCircle(Color(0xFF75DBFF).copy(alpha=(1f-p)*.20f),radius,Offset(cx,cy-102f*unit))
                    drawCircle(Color(0xFFBBF3FF).copy(alpha=(1f-p)),radius,Offset(cx,cy-102f*unit),style=Stroke(3f*unit))
                    repeat(6){i->val angle=i*1.0472f;val v=Offset(kotlin.math.cos(angle),kotlin.math.sin(angle));val center=Offset(cx,cy-102f*unit)+v*radius;drawLine(Color(0xFFDAF9FF),center,center+v*12f*unit,3f*unit)}
                }
                if(f.evade>0f){
                    val trail=Color(0xFF8EDBFF).copy(alpha=.28f*(f.evade/.44f).coerceIn(0f,1f))
                    repeat(3){i->drawLine(trail,Offset(px+(i+1)*14f*unit,cy-(68f+i*24f)*unit),Offset(px+(i+4)*25f*unit,cy-(68f+i*24f)*unit),4f*unit,StrokeCap.Round)}
                }
                val playerPose=if(answer){
                    if(correct)f.copy(move=ArenaMove.SPECIAL,lock=(ArenaMove.SPECIAL.lock-p*.90f).coerceAtLeast(0f))
                    else f.copy(playerImpact=recoilBeat*.34f)
                }else f
                translate(px+recoil,cy-lift){scale(unit,unit,pivot=Offset.Zero){drawPelao(playerPose)}}
                if(answer)drawArenaAnswerEffect(correct,p,Offset(px,cy-108f*unit),Offset(cx,cy-106f*unit),unit)
                if(f.move==ArenaMove.SPECIAL&&f.hitIn>0f){
                    val fraction=(1f-f.hitIn/ArenaMove.SPECIAL.windup).coerceIn(0f,1f)
                    val travel=minOf(f.distance,ArenaMove.SPECIAL.range)*w*fraction
                    val orb=Offset((px+travel).coerceAtMost(w-12f*unit),cy-lift-105f*unit)
                    drawCircle(Color(0xFF83DAFF).copy(alpha=.20f),35f*unit,orb)
                    drawCircle(Color(0xFF4DA8ED),20f*unit,orb)
                    drawCircle(Color(0xFFDBFAFF),11f*unit,orb)
                    drawLine(Color(0xFF90EBFF).copy(alpha=.65f),orb-Offset(32f*unit,0f),orb,10f*unit,StrokeCap.Round)
                }
                if(f.impact>0f){
                    val x=w*(f.playerX*.35f+f.rivalX*.65f);val yy=floor-110f*unit
                    val p=(1f-f.impact/.30f).coerceIn(0f,1f)
                    val spark=Color(0xFFFDE68A).copy(alpha=(f.impact/.30f).coerceIn(0f,1f))
                    drawCircle(Color.White.copy(alpha=(1f-p)*.75f),10f*unit,Offset(x,yy))
                    drawCircle(spark, (19f+31f*p)*unit,Offset(x,yy),style=Stroke(2.5f*unit))
                    repeat(12){i->
                        val angle=i*6.283185f/12f
                        val d=Offset(kotlin.math.cos(angle),kotlin.math.sin(angle))
                        drawLine(spark,Offset(x,yy)+d*((8f+22f*p)*unit),Offset(x,yy)+d*((29f+41f*p)*unit),3f*unit,StrokeCap.Round)
                    }
                }
            }
            if(f.phase==ArenaPhase.SHIELD_BREAK)Text("¡ROMPE EL ESCUDO!",Modifier.align(Alignment.TopCenter).padding(top=12.dp),fontSize=24.sp,color=Color(0xFFBEF4FF),fontWeight=FontWeight.ExtraBold)
            else if(f.phase==ArenaPhase.ANSWER_IMPACT){
                val success=f.answerCorrect
                val tint=if(success)Color(0xFF73F0BB)else Color(0xFFFF9395)
                Column(Modifier.align(Alignment.TopCenter).padding(top=12.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text(if(success)"¡CORRECTO!" else "¡CONTRAATAQUE!",fontSize=25.sp,color=tint,fontWeight=FontWeight.ExtraBold)
                    Text(if(!f.answerApplied)if(success)"Rompiendo el escudo..." else "El escudo resiste..." else if(success)"−"+f.damage+" HP · +25 ENERGÍA" else "−8 HP",fontSize=14.sp,color=Color.White,fontWeight=FontWeight.Bold)
                }
            }
            else if(tell)Surface(Modifier.align(Alignment.TopCenter).padding(5.dp),color=if(heavy)Color(0xFF9B2730)else Color(0xFF775522),shape=RoundedCornerShape(10.dp)){
                Column(Modifier.widthIn(max=230.dp).padding(horizontal=12.dp,vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text(if(heavy)"CARGA · ESQUIVA" else if(f.attack==RivalAttack.SWEEP)"BARRIDO · SALTA O BLOQUEA" else "GOLPE · BLOQUEA",color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=12.sp)
                    LinearProgressIndicator(progress={1f-(f.phaseTime/f.attack.tell).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(4.dp),color=Color.White,trackColor=Color.White.copy(alpha=.2f))
                }
            }else if(f.phase==ArenaPhase.ROUND_CLEAR)Text("¡KO!",Modifier.align(Alignment.Center),fontSize=44.sp,color=Color(0xFFFDE68A),fontWeight=FontWeight.ExtraBold)
            if(f.intro>0f&&!tell&&!f.isCinematic)Text(if(f.intro>.45f)"RONDA "+f.rival else "¡PELEA!",Modifier.align(Alignment.TopCenter).padding(top=12.dp),fontSize=28.sp,color=Color(0xFFFFD477),fontWeight=FontWeight.ExtraBold)
            if(!f.isCinematic&&f.comboFlash>0f&&f.combo>=2)Text(f.combo.toString()+" GOLPES",Modifier.align(Alignment.CenterStart).padding(8.dp),fontSize=25.sp,color=Color(0xFFFFD477),fontWeight=FontWeight.ExtraBold)
            if(!f.isCinematic&&f.defendFlash>0f)Text("¡DEFENSA!",Modifier.align(Alignment.BottomCenter).padding(bottom=15.dp),fontSize=18.sp,color=Color(0xFFBAFCE1),fontWeight=FontWeight.ExtraBold)
            else if(!f.isCinematic&&f.counterFlash>0f)Text("COUNTER",Modifier.align(Alignment.BottomCenter).padding(bottom=15.dp),fontSize=18.sp,color=Color(0xFFDAF9FF),fontWeight=FontWeight.ExtraBold)
            if(f.impact>0f&&!f.isCinematic)Text("−"+f.damage,Modifier.align(Alignment.CenterEnd).padding(12.dp),color=Color(0xFFFFDA75),fontSize=25.sp,fontWeight=FontWeight.ExtraBold)
            if(f.answerFeedbackTime>0f&&!f.isCinematic)f.answerFeedback?.let{explanation->
                Surface(Modifier.align(Alignment.BottomCenter).padding(horizontal=8.dp,vertical=4.dp),
                    color=Color(0xE6193441),shape=RoundedCornerShape(8.dp)){
                    Text(explanation,Modifier.padding(horizontal=8.dp,vertical=5.dp),fontSize=11.sp,lineHeight=14.sp,color=Color(0xFFD9FCEF))
                }
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Text(f.message,modifier=Modifier.weight(1f),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)
            if(f.combo>1)Text("×"+f.combo+" COMBO",color=Color(0xFFFDE68A),fontSize=12.sp,fontWeight=FontWeight.ExtraBold)
        }
    }
}
@Composable
private fun CombatHealth(name:String,hp:Int,max:Int,color:Color,modifier:Modifier,shields:Int=0){
    val ratio=(hp.toFloat()/max).coerceIn(0f,1f)
    val current by animateFloatAsState(ratio,tween(180),label="health")
    val trail by animateFloatAsState(ratio,tween(620),label="damageTrail")
    Column(modifier,verticalArrangement=Arrangement.spacedBy(4.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text(name,modifier=Modifier.weight(1f),color=Color(0xFFE5ECF8),fontSize=10.sp,fontWeight=FontWeight.ExtraBold,maxLines=1)
            Text(hp.toString(),color=Color.White,fontSize=11.sp,fontWeight=FontWeight.Bold)
        }
        Canvas(Modifier.fillMaxWidth().height(12.dp)){
            val corner=CornerRadius(4.dp.toPx())
            drawRoundRect(Color(0xFF323D50),cornerRadius=corner)
            if(trail>0f)drawRoundRect(Color(0xFFF3D18C),size=Size(size.width*maxOf(current,trail),size.height),cornerRadius=corner)
            if(current>0f)drawRoundRect(Brush.verticalGradient(listOf(color,color.copy(alpha=.64f))),size=Size(size.width*current,size.height),cornerRadius=corner)
            if(shields>0){
                // Mark only thresholds that are still ahead of the player.
                val marks=if(shields==2)listOf(ArenaShieldRules.FIRST_PERCENT/100f,ArenaShieldRules.SECOND_PERCENT/100f)else listOf(ArenaShieldRules.SECOND_PERCENT/100f)
                for(mark in marks){
                    val x=size.width*mark
                    drawLine(Color.White,Offset(x,0f),Offset(x,size.height),2.dp.toPx())
                }
            }
        }
    }
}

/** Articulated original robot: limbs, wind-up and hit arcs are tied to the simulation. */
private fun DrawScope.drawArenaRival(f:ArenaCombatFrame,armor:Color){
    val sizeFactor=when(f.rival){1->.92f;2->1.0f;else->1.12f}
    val bob=sin(f.elapsed*6f)*2.5f
    val tell=f.phase==ArenaPhase.WINDUP
    val strike=f.phase==ArenaPhase.STRIKE
    val lean=if(f.rivalStun>0f)18f*(f.rivalStun/.36f).coerceIn(0f,1f) else if(strike)-20f else if(tell)12f else 0f
    val step=if(f.phase==ArenaPhase.APPROACH&&f.rivalStun<=0f)sin(f.elapsed*17f)*12f else 0f
    scale(sizeFactor,sizeFactor,pivot=Offset.Zero){
        val body=Color(0xFF39495F)
        val dark=Color(0xFF182536)
        val edge=Color(0xFF9BB1C9)
        fun limb(a:Offset,b:Offset,c:Offset){
            drawLine(dark,a,b,19f,StrokeCap.Round);drawLine(armor,a,b,13f,StrokeCap.Round)
            drawLine(dark,b,c,18f,StrokeCap.Round);drawLine(body,b,c,12f,StrokeCap.Round)
            drawCircle(edge,6f,b);drawCircle(dark,12f,c);drawCircle(armor,8f,c)
        }
        if(strike&&f.attack==RivalAttack.SWEEP)limb(Offset(-17f,-65f),Offset(-54f,-32f),Offset(-91f,-13f))
        else limb(Offset(-17f,-65f),Offset(-24f-step*.55f,-34f),Offset(-32f-step,-9f))
        limb(Offset(17f,-65f),Offset(24f+step*.55f,-32f),Offset(32f+step,-9f))
        drawRoundRect(dark,Offset(if(strike&&f.attack==RivalAttack.SWEEP)-107f else -46f-step,-10f),Size(31f,10f),CornerRadius(4f))
        drawRoundRect(dark,Offset(15f+step,-10f),Size(31f,10f),CornerRadius(4f))
        val x=lean;val yy=-100f+bob
        val chest=Path().apply{moveTo(x-35f,yy-34f);lineTo(x+35f,yy-34f);lineTo(x+26f,yy+25f);lineTo(x-26f,yy+25f);close()}
        drawPath(chest,dark);drawPath(chest,armor.copy(alpha=.90f),style=Stroke(5f))
        drawRoundRect(body,Offset(x-23f,yy-21f),Size(46f,31f),CornerRadius(7f))
        drawCircle(if(tell)Color(0xFFFDE68A) else armor,if(f.rival==3)11f else 8f,Offset(x,yy-5f))
        val frontHand=when{strike&&f.attack==RivalAttack.CHARGE->Offset(x-98f,yy-18f);strike&&f.attack==RivalAttack.SWEEP->Offset(x-49f,yy-30f);strike->Offset(x-84f,yy-8f);tell->Offset(x-17f,yy-58f);else->Offset(x-47f,yy-15f)}
        limb(Offset(x-37f,yy-22f),Offset(x-49f,yy+5f),frontHand)
        limb(Offset(x+35f,yy-22f),Offset(x+46f,yy+4f),Offset(x+39f,yy-16f))
        drawRoundRect(armor,Offset(x-27f,yy-74f),Size(54f,39f),CornerRadius(if(f.rival==3)5f else 11f))
        drawRoundRect(dark,Offset(x-21f,yy-66f),Size(42f,15f),CornerRadius(5f))
        drawRoundRect(if(tell)Color(0xFFFFD166)else Color(0xFFDBF7FF),Offset(x-15f,yy-62f),Size(29f,5f),CornerRadius(2f))
        if(f.rival==2)drawLine(edge,Offset(x+18f,yy-75f),Offset(x+26f,yy-92f),3f,StrokeCap.Round)
        if(f.rival==3)for(dx in listOf(-39f,39f))drawRoundRect(armor,Offset(x+dx-10f,yy-42f),Size(20f,19f),CornerRadius(3f))
    }
}
