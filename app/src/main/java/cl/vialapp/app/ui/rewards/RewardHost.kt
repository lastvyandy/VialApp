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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.roundToInt

internal data class RewardNotice(val title:String,val coins:Int,val xp:Int,val serial:Long)
internal class RewardController {
    var notice by mutableStateOf<RewardNotice?>(null)
        private set
    var serial by mutableLongStateOf(0L)
        private set
    fun xp(title:String,total:Int,amount:Int,showToast:Boolean=true){
        if(amount<=0)return
        // The mission dialog has its own animation. Still refresh the wallet.
        if(showToast)show(title,coinsFromAward(total,amount),amount)
        else serial++
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
        Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=9.dp),
            verticalArrangement=Arrangement.spacedBy(2.dp)){
            Text(notice.title,fontSize=13.sp,color=Color(0xFFDCEAFF),fontWeight=FontWeight.SemiBold)
            if(notice.xp>0) {
                XpRewardLine(notice.xp,progress.value,Color(0xFFB8DDFF))
            }
            if(notice.coins>0) Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)){
                CoinBurst(Modifier.size(48.dp),progress.value)
                Text("+"+notice.coins+" monedas",fontSize=21.sp,color=Color(0xFFFFD77C),fontWeight=FontWeight.ExtraBold,
                    modifier=Modifier.weight(1f))
            }
        }
    }
}

/** A one-shot XP animation is independent of the coin burst, including XP-only awards. */
@Composable
internal fun RewardXpLine(amount:Int,color:Color,modifier:Modifier=Modifier){
    if(amount<=0)return
    val animation=remember(amount){Animatable(0f)}
    LaunchedEffect(amount){animation.animateTo(1f,tween(850))}
    XpRewardLine(amount,animation.value,color,modifier)
}

@Composable
private fun XpRewardLine(amount:Int,progress:Float,color:Color,modifier:Modifier=Modifier){
    val p=progress.coerceIn(0f,1f)
    val displayed=(amount*p).roundToInt().coerceIn(1,amount)
    Row(modifier.fillMaxWidth().clearAndSetSemantics {
        contentDescription="+"+amount+" puntos de experiencia"
    },verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        XpBurst(Modifier.size(48.dp),p)
        Text("+"+displayed+" XP",fontSize=23.sp,color=color,fontWeight=FontWeight.ExtraBold,
            modifier=Modifier.weight(1f).graphicsLayer{
                scaleX=1f+.10f*sin(p*Math.PI.toFloat())
                scaleY=scaleX
            })
    }
}

@Composable
private fun XpBurst(modifier:Modifier,progress:Float){
    Canvas(modifier){
        val p=progress.coerceIn(0f,1f)
        val unit=size.minDimension
        val radius=unit*.28f
        // Brief expanding halo and small sparks around the experience symbol.
        val pulse=sin(p*Math.PI.toFloat()).coerceAtLeast(0f)
        drawCircle(Color(0xFF8BCBFF).copy(alpha=.34f*pulse),radius+unit*.17f*p,
            center,style=Stroke(1.6.dp.toPx()))
        for(i in 0..5){
            val t=((p-i*.035f)*1.25f).coerceIn(0f,1f)
            val alpha=sin(t*Math.PI.toFloat()).coerceIn(0f,1f)
            val angle=(i/6f*2f*Math.PI.toFloat())-.5f
            val delta=Offset(cos(angle),sin(angle))
            val spark=center+delta*(radius+unit*.18f*t)
            drawLine(Color(0xFF9AD9FF).copy(alpha=alpha),spark-delta*unit*.035f,spark+delta*unit*.035f,
                strokeWidth=1.7.dp.toPx(),cap=StrokeCap.Round)
        }
        val r=radius*(1f+.13f*pulse)
        drawCircle(Color(0xFF1763C5),r,center)
        drawCircle(Color(0xFF6FC3FF),r*.86f,center,style=Stroke(1.4.dp.toPx()))
        val bolt=Path().apply{
            moveTo(center.x+r*.12f,center.y-r*.72f)
            lineTo(center.x-r*.50f,center.y+r*.06f)
            lineTo(center.x-r*.06f,center.y+r*.06f)
            lineTo(center.x-r*.15f,center.y+r*.71f)
            lineTo(center.x+r*.52f,center.y-r*.14f)
            lineTo(center.x+r*.08f,center.y-r*.14f)
            close()
        }
        drawPath(bolt,Color(0xFFF1F8FF))
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
