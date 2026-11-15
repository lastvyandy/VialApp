package cl.vialapp.app.ui.games.arcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.games.GameProgressStore
import kotlin.math.sin

@Composable
fun PelaoArenaScreen(userKey:String,onBack:()->Unit){
    ArcadeSession(userKey=userKey,gameId=GameProgressStore.PELAO_ARENA,title="Don Pelao Arena",
        subtitle="Combate de acción · tres rivales · una arena",
        rules="Muévete con las flechas, acércate a distancia de golpe y alterna ataques rápidos y fuertes. El rival anuncia sus golpes: bloquea los normales y esquiva las cargas rojas. Golpea o defiéndete bien para cargar el especial. No hay preguntas durante el combate. Derrota a los tres rivales antes de 120 segundos.",
        duration=ArenaCombatEngine.TIME,create={ArenaCombatEngine()},onBack=onBack,showPlayMessages=false,
        scene={f,m,_,_->CombatScene(f,m)},resultArt={f,m->PelaoSprite(f.pose,m)},
        controls={f,enabled,act->
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    ArcadeControl("◀",enabled&&f.lock<=0f,Modifier.weight(.7f)){act(0)}
                    ArcadeControl("▶",enabled&&f.lock<=0f,Modifier.weight(.7f)){act(1)}
                    ArcadeControl("Rápido",enabled&&f.lock<=0f,Modifier.weight(1.3f)){act(2)}
                    ArcadeControl("Fuerte",enabled&&f.lock<=0f,Modifier.weight(1.3f)){act(3)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    ArcadeControl("Bloquear",enabled&&f.lock<=0f,Modifier.weight(1f)){act(4)}
                    ArcadeControl("Esquivar",enabled&&f.evadeLock<=0f,Modifier.weight(1f),f.phase==ArenaPhase.WINDUP&&f.attack==RivalAttack.CHARGE){act(5)}
                    ArcadeControl("Especial",enabled&&f.power>=100&&f.lock<=0f,Modifier.weight(1f),f.power>=100){act(6)}
                }
            }
        })
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
            CombatHealth(name,f.rivalHp,f.rivalMax,ink,Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
            Text("RIVAL "+f.rival.coerceAtMost(3)+"/3",color=Color(0xFFBCD2EF),fontSize=11.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={f.power/100f},modifier=Modifier.weight(1f).height(7.dp),color=Color(0xFFF9C950),trackColor=Color(0xFF334860))
            Text("POWER "+f.power+"%",color=Color(0xFFFFDF83),fontSize=10.sp,fontWeight=FontWeight.Bold)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(13.dp))){
            val arenaW=maxWidth
            val arenaH=maxHeight
            val playerWidth=minOf(arenaW*.38f,arenaH*.64f)
            val playerHeight=arenaH*.64f
            val y=arenaH*.16f
            Canvas(Modifier.fillMaxSize()){
                val w=size.width;val h=size.height
                val floor=h*.80f
                drawRect(Brush.verticalGradient(listOf(Color(0xFF202B4D),Color(0xFF3C5370),Color(0xFF151D2B))),size=size)
                // Layered industrial arena, beams and lit spectator silhouettes.
                for(i in 0..7){
                    val x=i*w/7f
                    val bh=h*(.24f+(i%3)*.055f)
                    drawRect(Color(0xFF24344D),Offset(x,floor-bh),Size(w*.09f,bh))
                    drawRect(ink.copy(alpha=.20f),Offset(x+3f,floor-bh+8f),Size(w*.06f,4f))
                }
                drawRect(Color(0xFF25364A),Offset(0f,h*.12f),Size(w,h*.065f))
                for(i in 0..4){
                    val x=(i+.5f)*w/5f
                    drawCircle(Color(0xFFCFDDF1).copy(alpha=.10f),w*.12f,Offset(x,h*.16f))
                    drawRoundRect(Color(0xFFE7EFFF),Offset(x-9f,h*.14f),Size(18f,5f),CornerRadius(2f))
                }
                drawRect(Brush.verticalGradient(listOf(Color(0xFF4A6079),Color(0xFF1C2B3E)),startY=floor,endY=h),Offset(0f,floor),Size(w,h-floor))
                for(i in 0..6)drawLine(Color(0xFF6D8297).copy(alpha=.38f),Offset(w*.5f+(i-3)*w*.06f,floor),Offset((i-3)*w*.45f+w*.5f,h),2f)
                drawLine(Color(0xFFE7B951),Offset(0f,floor),Offset(w,floor),3f)
                for(x in listOf(f.playerX,f.rivalX))drawOval(Color.Black.copy(alpha=.42f),Offset(w*x-w*.092f,floor-7f),Size(w*.184f,14f))
                if(tell){
                    val range=f.attack.range*w
                    drawRoundRect((if(heavy)Color(0xFFEF4444) else Color(0xFFFBBF24)).copy(alpha=.24f),
                        Offset((f.rivalX*w-range).coerceAtLeast(0f),floor-9f),Size(range,18f),CornerRadius(6f))
                }
                val unit=minOf(w/360f,h/230f)
                val cx=w*f.rivalX+(if(f.impact>0f)unit*7f*sin(f.impact*32f)else 0f)
                val cy=floor
                translate(cx,cy){scale(unit,unit,pivot=Offset.Zero){drawArenaRival(f,ink)}}
                if(f.impact>0f){
                    val x=w*(f.playerX+f.rivalX)/2f;val yy=floor-h*.26f
                    repeat(8){i->
                        val angle=i*6.283185f/8f
                        val d=Offset(kotlin.math.cos(angle),kotlin.math.sin(angle))
                        drawLine(Color(0xFFFDE68A).copy(alpha=(f.impact/.24f).coerceIn(0f,1f)),Offset(x,yy)+d*(12f*unit),Offset(x,yy)+d*(38f*unit),4f*unit,StrokeCap.Round)
                    }
                }
            }
            PelaoSprite(f.pose,Modifier.offset(x=arenaW*f.playerX-playerWidth/2,y=y).width(playerWidth).height(playerHeight)
                .graphicsLayer {
                    val m=f.move
                    val punch=if(m!=null&&f.lock>0f)sin((f.lock/m.lock).coerceIn(0f,1f)*Math.PI.toFloat())else 0f
                    translationX=18.dp.toPx()*punch-if(f.evade>0f)9.dp.toPx() else 0f
                    rotationZ=-10f*punch+(if(f.playerImpact>0f)10f else 0f)
                    scaleX=1f+.08f*punch
                    scaleY=scaleX
                })
            if(f.guard>0f)Box(Modifier.offset(x=arenaW*f.playerX-playerWidth/2,y=y).width(playerWidth).height(playerHeight)){
                Canvas(Modifier.fillMaxSize()){drawOval(Color(0xFF7DD3FC).copy(alpha=.65f),style=Stroke(3.dp.toPx()))}
            }
            if(tell)Surface(Modifier.align(Alignment.TopCenter).padding(5.dp),color=if(heavy)Color(0xFF9B2730)else Color(0xFF775522),shape=RoundedCornerShape(10.dp)){
                Column(Modifier.widthIn(max=230.dp).padding(horizontal=12.dp,vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text(if(heavy)"CARGA · ESQUIVA" else if(f.attack==RivalAttack.SWEEP)"BARRIDO · BLOQUEA" else "GOLPE · BLOQUEA",color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=12.sp)
                    LinearProgressIndicator(progress={1f-(f.phaseTime/f.attack.tell).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(4.dp),color=Color.White,trackColor=Color.White.copy(alpha=.2f))
                }
            }else if(f.phase==ArenaPhase.ROUND_CLEAR)Text("¡KO!",Modifier.align(Alignment.Center),fontSize=44.sp,color=Color(0xFFFDE68A),fontWeight=FontWeight.ExtraBold)
            if(f.impact>0f)Text("−"+f.damage,Modifier.align(Alignment.CenterEnd).padding(12.dp),color=Color(0xFFFFDA75),fontSize=25.sp,fontWeight=FontWeight.ExtraBold)
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Text(f.message,modifier=Modifier.weight(1f),color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)
            if(f.combo>1)Text("×"+f.combo+" COMBO",color=Color(0xFFFDE68A),fontSize=12.sp,fontWeight=FontWeight.ExtraBold)
        }
    }
}
@Composable
private fun CombatHealth(name:String,hp:Int,max:Int,color:Color,modifier:Modifier){
    Column(modifier,verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text(name+" · "+hp,color=Color(0xFFE5ECF8),fontSize=11.sp,fontWeight=FontWeight.ExtraBold,maxLines=1)
        LinearProgressIndicator(progress={(hp.toFloat()/max).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(9.dp),color=color,trackColor=Color(0xFF3B4659))
    }
}

/** Articulated original robot: limbs, wind-up and hit arcs are tied to the simulation. */
private fun DrawScope.drawArenaRival(f:ArenaCombatFrame,armor:Color){
    val sizeFactor=when(f.rival){1->.92f;2->1.0f;else->1.12f}
    val bob=sin(f.elapsed*6f)*2.5f
    val tell=f.phase==ArenaPhase.WINDUP
    val strike=f.phase==ArenaPhase.STRIKE
    val lean=if(strike)-13f else if(tell)7f else 0f
    scale(sizeFactor,sizeFactor,pivot=Offset.Zero){
        val body=Color(0xFF39495F)
        val dark=Color(0xFF182536)
        val edge=Color(0xFF9BB1C9)
        fun limb(a:Offset,b:Offset,c:Offset){
            drawLine(dark,a,b,19f,StrokeCap.Round);drawLine(armor,a,b,13f,StrokeCap.Round)
            drawLine(dark,b,c,18f,StrokeCap.Round);drawLine(body,b,c,12f,StrokeCap.Round)
            drawCircle(edge,6f,b);drawCircle(dark,12f,c);drawCircle(armor,8f,c)
        }
        limb(Offset(-17f,-65f),Offset(-24f,-34f),Offset(-32f,-9f))
        limb(Offset(17f,-65f),Offset(24f,-32f),Offset(32f,-9f))
        drawRoundRect(dark,Offset(-46f+lean,-10f),Size(31f,10f),CornerRadius(4f))
        drawRoundRect(dark,Offset(15f+lean,-10f),Size(31f,10f),CornerRadius(4f))
        val x=lean;val yy=-100f+bob
        val chest=Path().apply{moveTo(x-35f,yy-34f);lineTo(x+35f,yy-34f);lineTo(x+26f,yy+25f);lineTo(x-26f,yy+25f);close()}
        drawPath(chest,dark);drawPath(chest,armor.copy(alpha=.90f),style=Stroke(5f))
        drawRoundRect(body,Offset(x-23f,yy-21f),Size(46f,31f),CornerRadius(7f))
        drawCircle(if(tell)Color(0xFFFDE68A) else armor,if(f.rival==3)11f else 8f,Offset(x,yy-5f))
        val frontHand=when{strike->Offset(x-84f,yy-8f);tell->Offset(x-17f,yy-58f);else->Offset(x-47f,yy-15f)}
        limb(Offset(x-37f,yy-22f),Offset(x-49f,yy+5f),frontHand)
        limb(Offset(x+35f,yy-22f),Offset(x+46f,yy+4f),Offset(x+39f,yy-16f))
        drawRoundRect(armor,Offset(x-27f,yy-74f),Size(54f,39f),CornerRadius(if(f.rival==3)5f else 11f))
        drawRoundRect(dark,Offset(x-21f,yy-66f),Size(42f,15f),CornerRadius(5f))
        drawRoundRect(if(tell)Color(0xFFFFD166)else Color(0xFFDBF7FF),Offset(x-15f,yy-62f),Size(29f,5f),CornerRadius(2f))
        if(f.rival==2)drawLine(edge,Offset(x+18f,yy-75f),Offset(x+26f,yy-92f),3f,StrokeCap.Round)
        if(f.rival==3)for(dx in listOf(-39f,39f))drawRoundRect(armor,Offset(x+dx-10f,yy-42f),Size(20f,19f),CornerRadius(3f))
    }
}
