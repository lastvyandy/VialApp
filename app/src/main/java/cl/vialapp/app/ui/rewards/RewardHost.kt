package cl.vialapp.app.ui.rewards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

internal data class RewardNotice(val title:String,val coins:Int,val xp:Int,val serial:Long)
internal class RewardController {
    var notice by mutableStateOf<RewardNotice?>(null)
        private set
    var serial by mutableLongStateOf(0L)
        private set
    fun xp(title:String,total:Int,amount:Int){
        if(amount>0)show(title,coinsFromAward(total,amount),amount)
    }
    fun show(title:String,coins:Int,xp:Int=0){
        if(coins<=0&&xp<=0)return
        serial++
        notice=RewardNotice(title,coins.coerceAtLeast(0),xp.coerceAtLeast(0),serial)
    }
    fun dismiss(id:Long){if(notice?.serial==id)notice=null}
}
internal val LocalRewards=compositionLocalOf<RewardController>{RewardController()}

@Composable
internal fun RewardHost(content:@Composable ()->Unit){
    val controller=remember{RewardController()}
    CompositionLocalProvider(LocalRewards provides controller){
        Box(Modifier.fillMaxSize()){
            content()
            val active=controller.notice
            if(active!=null)key(active.serial){
                LaunchedEffect(active.serial){delay(2300);controller.dismiss(active.serial)}
                RewardToast(active,Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal=16.dp,vertical=10.dp))
            }
        }
    }
}
@Composable
private fun RewardToast(notice:RewardNotice,modifier:Modifier=Modifier){
    val progress=remember{Animatable(0f)}
    LaunchedEffect(Unit){progress.animateTo(1f,tween(1050))}
    Surface(modifier=modifier.fillMaxWidth().graphicsLayer{
        val p=progress.value
        alpha=(p*7f).coerceIn(0f,1f)
        translationY=-(1f-(p*5f).coerceIn(0f,1f))*28.dp.toPx()
    }.semantics{liveRegion=LiveRegionMode.Polite},shape=RoundedCornerShape(20.dp),
        color=Color(0xFF203B63),contentColor=Color.White,shadowElevation=7.dp){
        Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            if(notice.coins>0)CoinBurst(Modifier.size(72.dp),progress.value)
            else Surface(color=Color(0xFF375D91),shape=RoundedCornerShape(14.dp)){
                Text("XP",Modifier.padding(14.dp),fontWeight=FontWeight.ExtraBold,color=Color.White)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                Text(notice.title,fontSize=13.sp,color=Color(0xFFDCEAFF),fontWeight=FontWeight.SemiBold)
                if(notice.coins>0)Text("+"+notice.coins+" monedas",fontSize=23.sp,color=Color(0xFFFFD77C),fontWeight=FontWeight.ExtraBold)
                if(notice.xp>0)Text("+"+notice.xp+" XP",fontSize=if(notice.coins>0)14.sp else 23.sp,color=Color.White,fontWeight=FontWeight.Bold)
            }
        }
    }
}
@Composable
private fun CoinBurst(modifier:Modifier,progress:Float){
    Canvas(modifier){
        val radius=size.minDimension*.20f
        val p=progress.coerceIn(0f,1f)
        val gold=Color(0xFFFFD26C)
        val edge=Color(0xFFB27620)
        for(i in 0..4){
            val t=((p-i*.055f)*1.4f).coerceIn(0f,1f)
            val angle=-2.8f+i*.48f
            val distance=size.minDimension*.39f*sin(t*Math.PI.toFloat())
            val place=center+Offset(kotlin.math.cos(angle)*distance,kotlin.math.sin(angle)*distance)
            val a=sin(t*Math.PI.toFloat()).coerceIn(0f,1f)
            drawCircle(gold.copy(alpha=a),radius*.58f,place)
            drawCircle(edge.copy(alpha=a),radius*.58f,place,style=Stroke(1.dp.toPx()))
        }
        val r=radius*1.45f*(1f+.20f*sin(p*Math.PI.toFloat()))
        drawCircle(edge,r+1.dp.toPx(),center+Offset(0f,2.dp.toPx()))
        drawCircle(gold,r,center)
        drawCircle(Color(0xFFFFE8A4),r*.77f,center,style=Stroke(2.dp.toPx()))
        drawCircle(edge,r*.47f,center,style=Stroke(1.4.dp.toPx()))
        drawLine(edge,center+Offset(0f,-r*.37f),center+Offset(0f,r*.37f),2.dp.toPx())
    }
}
@Composable
internal fun RewardCoinArt(modifier:Modifier=Modifier){
    val p=remember{Animatable(0f)}
    LaunchedEffect(Unit){p.animateTo(1f,tween(1050))}
    CoinBurst(modifier,p.value)
}
