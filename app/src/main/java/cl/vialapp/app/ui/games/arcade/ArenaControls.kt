package cl.vialapp.app.ui.games.arcade

// GUÍA DEL ARCHIVO:
// Controles de la arena; deben funcionar sin tapar media pantalla.

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun ArenaControls(f:ArenaCombatFrame,enabled:Boolean,act:(Int)->Unit,sound:Boolean,onToggleSound:()->Unit){
    val ready=enabled&&!f.isCinematic&&f.phase!=ArenaPhase.KNOWLEDGE
    val attackReady=ready
    Row(Modifier.fillMaxWidth().background(Color(0xFF101823),RoundedCornerShape(12.dp))
        .padding(horizontal=10.dp,vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            ArenaJoystick(ready,act)
            Text(if(sound)"Sonido: sí" else "Sonido: no",Modifier.clickable{onToggleSound()}.padding(4.dp),color=Color(0xFFB6C9E1),fontSize=10.sp,fontWeight=FontWeight.Bold)
        }
        Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.Bottom){
                ActionCircle("Salto",7,ready,Color(0xFF317E91),46,act)
                ActionCircle("Patada",3,attackReady,Color(0xFFB55738),50,act)
                ActionCircle("Puño",2,attackReady,Color(0xFFD77C36),54,act)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                ActionCircle("Guardia",4,ready,Color(0xFF376BA9),46,act)
                ActionCircle("Dash",5,ready,Color(0xFF317E91),46,act)
                ActionCircle("Especial",6,attackReady&&f.power>=100,Color(0xFF865ABB),46,act)
            }
        }
    }
}
@Composable
private fun ArenaJoystick(enabled:Boolean,act:(Int)->Unit){
    var direction by remember{mutableIntStateOf(0)}
    val callback by rememberUpdatedState(act)
    LaunchedEffect(direction,enabled){
        if(enabled&&direction!=0)while(true){callback(if(direction<0)8 else 9);delay(20)}
        else if(enabled)callback(10)
    }
    Box(Modifier.size(98.dp).clip(CircleShape).background(Color(0xFF1A293C))
        .border(2.dp,Color(0xFF647D98),CircleShape)
        .pointerInput(enabled){
            if(enabled)awaitEachGesture{
                val first=awaitFirstDown(requireUnconsumed=false)
                fun update(x:Float){direction=when{ x<size.width*.38f->-1;x>size.width*.62f->1;else->0 }}
                try{
                    update(first.position.x);first.consume()
                    do {
                        val event=awaitPointerEvent()
                        val change=event.changes.firstOrNull{it.id==first.id}?:break
                        if(!change.pressed)break
                        update(change.position.x);change.consume()
                    }while(true)
                }finally{direction=0}
            }
        }.semantics{contentDescription="Mando de movimiento"},
        contentAlignment=Alignment.Center){
        Canvas(Modifier.fillMaxSize()){
            val u=size.width
            drawCircle(Color(0xFF152437),u*.40f)
            drawCircle(Color(0xFF546C86),u*.22f,center+Offset(direction*u*.15f,0f))
            drawCircle(Color(0xFFADC0D3),u*.20f,center+Offset(direction*u*.15f,0f),style=Stroke(1.5.dp.toPx()))
            for(d in listOf(-1,1)){
                val x=center.x+d*u*.36f
                val p=Path().apply{moveTo(x+d*u*.045f,center.y);lineTo(x-d*u*.025f,center.y-u*.055f);lineTo(x-d*u*.025f,center.y+u*.055f);close()}
                drawPath(p,Color(0xFFDAE6F5))
            }
        }
    }
}
@Composable
private fun ActionCircle(label:String,action:Int,enabled:Boolean,color:Color,diameter:Int,act:(Int)->Unit){
    val callback by rememberUpdatedState(act)
    var pressed by remember{mutableStateOf(false)}
    Column(horizontalAlignment=Alignment.CenterHorizontally){
        Box(Modifier.size(diameter.dp).clip(CircleShape)
            .background(Brush.verticalGradient(if(enabled)listOf(if(pressed)Color.White.copy(alpha=.48f) else color.copy(alpha=.95f),color.copy(alpha=.65f))else listOf(color.copy(alpha=.32f),color.copy(alpha=.19f))))
            .border(1.5.dp,if(enabled)Color.White.copy(alpha=.62f)else Color.White.copy(alpha=.16f),CircleShape)
            .pointerInput(enabled,action){
                if(enabled)awaitEachGesture{
                    val first=awaitFirstDown(requireUnconsumed=false)
                    first.consume()
                    pressed=true
                    callback(action)
                    try{
                        do {
                            val event=awaitPointerEvent()
                            val change=event.changes.firstOrNull{it.id==first.id}?:break
                            if(!change.pressed)break
                            change.consume()
                        }while(true)
                    }finally{pressed=false}
                }
            }.semantics{
                contentDescription=label;role=Role.Button
                if(enabled)onClick{callback(action);true}else disabled()
            },contentAlignment=Alignment.Center){
            Canvas(Modifier.fillMaxSize().padding(11.dp)){
                val w=size.width;val h=size.height
                val ink=Color.White.copy(alpha=if(enabled)1f else .35f)
                val stroke=w*.10f
                when(action){
                    2->{
                        val fist=Path().apply{moveTo(w*.24f,h*.43f);lineTo(w*.24f,h*.19f);quadraticBezierTo(w*.35f,h*.1f,w*.43f,h*.22f);quadraticBezierTo(w*.55f,h*.05f,w*.64f,h*.24f);quadraticBezierTo(w*.8f,h*.09f,w*.84f,h*.32f);lineTo(w*.84f,h*.53f);lineTo(w*.69f,h*.82f);lineTo(w*.30f,h*.82f);lineTo(w*.10f,h*.55f);quadraticBezierTo(w*.1f,h*.38f,w*.24f,h*.43f);close()}
                        drawPath(fist,ink)
                        drawLine(color,Offset(w*.33f,h*.30f),Offset(w*.33f,h*.44f),stroke*.45f)
                        drawLine(color,Offset(w*.54f,h*.29f),Offset(w*.54f,h*.44f),stroke*.45f)
                    }
                    3->{val p=Path().apply{moveTo(w*.30f,h*.08f);lineTo(w*.58f,h*.12f);lineTo(w*.49f,h*.53f);lineTo(w*.88f,h*.66f);quadraticBezierTo(w*.96f,h*.72f,w*.84f,h*.90f);lineTo(w*.22f,h*.80f);lineTo(w*.23f,h*.49f);close()};drawPath(p,ink);drawLine(color,Offset(w*.25f,h*.68f),Offset(w*.80f,h*.79f),stroke*.6f)}
                    7->{drawLine(ink,Offset(w*.5f,h*.88f),Offset(w*.5f,h*.16f),stroke,StrokeCap.Round);drawLine(ink,Offset(w*.5f,h*.16f),Offset(w*.20f,h*.45f),stroke,StrokeCap.Round);drawLine(ink,Offset(w*.5f,h*.16f),Offset(w*.80f,h*.45f),stroke,StrokeCap.Round)}
                    4->{val p=Path().apply{moveTo(w*.5f,h*.08f);lineTo(w*.88f,h*.24f);lineTo(w*.82f,h*.61f);quadraticBezierTo(w*.73f,h*.84f,w*.5f,h*.95f);quadraticBezierTo(w*.25f,h*.84f,w*.18f,h*.61f);lineTo(w*.12f,h*.24f);close()};drawPath(p,ink,style=Stroke(stroke))}
                    5->{drawLine(ink,Offset(w*.18f,h*.5f),Offset(w*.84f,h*.5f),stroke,StrokeCap.Round);drawLine(ink,Offset(w*.18f,h*.5f),Offset(w*.43f,h*.23f),stroke,StrokeCap.Round);drawLine(ink,Offset(w*.18f,h*.5f),Offset(w*.43f,h*.77f),stroke,StrokeCap.Round)}
                    6->{val p=Path().apply{moveTo(w*.60f,h*.03f);lineTo(w*.14f,h*.56f);lineTo(w*.46f,h*.56f);lineTo(w*.36f,h*.98f);lineTo(w*.87f,h*.38f);lineTo(w*.55f,h*.38f);close()};drawPath(p,ink)}
                }
            }
        }
        Text(label,color=Color(0xFFCFDDF0),fontSize=10.sp,fontWeight=FontWeight.Bold)
    }
}
