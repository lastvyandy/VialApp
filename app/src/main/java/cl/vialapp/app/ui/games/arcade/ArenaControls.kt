package cl.vialapp.app.ui.games.arcade

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun ArenaControls(f:ArenaCombatFrame,enabled:Boolean,act:(Int)->Unit){
    val ready=enabled&&f.phase!=ArenaPhase.ROUND_CLEAR
    val attackReady=ready&&f.lock<=0f&&f.evade<=0f&&f.playerImpact<=0f
    Row(Modifier.fillMaxWidth().background(Color(0xFF142135),RoundedCornerShape(18.dp))
        .padding(horizontal=10.dp,vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            ArenaJoystick(ready,act)
            Text("MOVIMIENTO",color=Color(0xFFB6C9E1),fontSize=9.sp,fontWeight=FontWeight.Bold)
        }
        Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.Bottom){
                ActionCircle("Fuerte",3,attackReady,Color(0xFFA3542C),46,act)
                ActionCircle("Golpe",2,attackReady,Color(0xFFD77C36),62,act)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                ActionCircle("Guardia",4,attackReady,Color(0xFF376BA9),46,act)
                ActionCircle("Esquiva",5,ready&&f.evadeLock<=0f&&f.playerImpact<=0f,Color(0xFF317E91),46,act)
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
        if(enabled&&direction!=0)while(true){callback(if(direction<0)0 else 1);delay(95)}
    }
    Box(Modifier.size(98.dp).clip(CircleShape).background(Color(0xFF243950))
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
    Column(horizontalAlignment=Alignment.CenterHorizontally){
        Box(Modifier.size(diameter.dp).clip(CircleShape)
            .background(if(enabled)color else color.copy(alpha=.30f))
            .border(1.5.dp,if(enabled)Color.White.copy(alpha=.62f)else Color.White.copy(alpha=.16f),CircleShape)
            .clickable(enabled=enabled){act(action)}.semantics{contentDescription=label},contentAlignment=Alignment.Center){
            Canvas(Modifier.fillMaxSize().padding(11.dp)){
                val w=size.width;val h=size.height
                val ink=Color.White.copy(alpha=if(enabled)1f else .35f)
                val stroke=w*.10f
                when(action){
                    2,3->{
                        val fist=Path().apply{moveTo(w*.24f,h*.43f);lineTo(w*.24f,h*.19f);quadraticBezierTo(w*.35f,h*.1f,w*.43f,h*.22f);quadraticBezierTo(w*.55f,h*.05f,w*.64f,h*.24f);quadraticBezierTo(w*.8f,h*.09f,w*.84f,h*.32f);lineTo(w*.84f,h*.53f);lineTo(w*.69f,h*.82f);lineTo(w*.30f,h*.82f);lineTo(w*.10f,h*.55f);quadraticBezierTo(w*.1f,h*.38f,w*.24f,h*.43f);close()}
                        drawPath(fist,ink)
                        if(action==3)drawLine(color,Offset(w*.27f,h*.56f),Offset(w*.73f,h*.56f),stroke)
                    }
                    4->{val p=Path().apply{moveTo(w*.5f,h*.08f);lineTo(w*.88f,h*.24f);lineTo(w*.82f,h*.61f);quadraticBezierTo(w*.73f,h*.84f,w*.5f,h*.95f);quadraticBezierTo(w*.25f,h*.84f,w*.18f,h*.61f);lineTo(w*.12f,h*.24f);close()};drawPath(p,ink,style=Stroke(stroke))}
                    5->{drawLine(ink,Offset(w*.18f,h*.5f),Offset(w*.84f,h*.5f),stroke,StrokeCap.Round);drawLine(ink,Offset(w*.18f,h*.5f),Offset(w*.43f,h*.23f),stroke,StrokeCap.Round);drawLine(ink,Offset(w*.18f,h*.5f),Offset(w*.43f,h*.77f),stroke,StrokeCap.Round)}
                    6->{val p=Path().apply{moveTo(w*.60f,h*.03f);lineTo(w*.14f,h*.56f);lineTo(w*.46f,h*.56f);lineTo(w*.36f,h*.98f);lineTo(w*.87f,h*.38f);lineTo(w*.55f,h*.38f);close()};drawPath(p,ink)}
                }
            }
        }
        Text(label,color=Color(0xFFCFDDF0),fontSize=9.sp)
    }
}
