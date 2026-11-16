package cl.vialapp.app.ui.games.doom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.vialapp.app.data.games.GameRunResult
import cl.vialapp.app.ui.games.arcade.LocalGameAudio
import kotlinx.coroutines.delay
import kotlin.math.*

private val red=Color(0xFFE94A29)
private val ochre=Color(0xFFE9B875)
private val ivory=Color(0xFFE9D3AA)
private val glyphs=mapOf(
    'A' to "01110/11011/11011/11111/11011/11011/11011",
    'B' to "11110/11011/11011/11110/11011/11011/11110",
    'C' to "01111/11000/11000/11000/11000/11000/01111",
    'D' to "11110/11011/11011/11011/11011/11011/11110",
    'E' to "11111/11000/11000/11110/11000/11000/11111",
    'F' to "11111/11000/11000/11110/11000/11000/11000",
    'G' to "01111/11000/11000/11011/11011/11011/01111",
    'H' to "11011/11011/11011/11111/11011/11011/11011",
    'I' to "11111/00100/00100/00100/00100/00100/11111",
    'J' to "00111/00011/00011/00011/11011/11011/01110",
    'K' to "11011/11011/11110/11100/11110/11011/11011",
    'L' to "11000/11000/11000/11000/11000/11000/11111",
    'M' to "11011/11111/11111/11011/11011/11011/11011",
    'N' to "11011/11111/11111/11011/11011/11011/11011",
    'O' to "01110/11011/11011/11011/11011/11011/01110",
    'P' to "11110/11011/11011/11110/11000/11000/11000",
    'Q' to "01110/11011/11011/11011/11111/01110/00011",
    'R' to "11110/11011/11011/11110/11100/11010/11011",
    'S' to "01111/11000/11000/01110/00011/00011/11110",
    'T' to "11111/01110/00100/00100/00100/00100/00100",
    'U' to "11011/11011/11011/11011/11011/11011/01110",
    'V' to "11011/11011/11011/11011/11011/01010/00100",
    'W' to "11011/11011/11011/11011/11111/11111/01010",
    'X' to "11011/11011/01010/00100/01010/11011/11011",
    'Y' to "11011/11011/01010/00100/00100/00100/00100",
    'Z' to "11111/00011/00110/01100/11000/11000/11111",
    '0' to "01110/11011/11011/11011/11011/11011/01110",
    '1' to "00100/01100/00100/00100/00100/00100/01110",
    '2' to "01110/11011/00011/00110/01100/11000/11111",
    '3' to "11110/00011/00011/01110/00011/00011/11110",
    '4' to "11011/11011/11011/11111/00011/00011/00011",
    '5' to "11111/11000/11000/11110/00011/00011/11110",
    '6' to "01110/11000/11000/11110/11011/11011/01110",
    '7' to "11111/00011/00010/00110/01100/01000/01000",
    '8' to "01110/11011/11011/01110/11011/11011/01110",
    '9' to "01110/11011/11011/01111/00011/00011/01110",
    '%' to "11001/11011/00010/00100/01000/11011/10011",
    ':' to "00000/00100/00100/00000/00100/00100/00000",
    '>' to "10000/01000/00100/00010/00100/01000/10000",
    '+' to "00000/00100/00100/11111/00100/00100/00000",
    '-' to "00000/00000/00000/11111/00000/00000/00000",
    '/' to "00001/00011/00010/00100/01000/11000/10000",
    '.' to "00000/00000/00000/00000/00000/00100/00100"
).mapValues{it.value.split('/')}
private fun DrawScope.pixelText(text:String,x:Float,y:Float,unit:Float,color:Color,centered:Boolean=false){
    val start=if(centered)x-(text.length*6-1)*unit*.5f else x
    for((i,ch) in text.withIndex()){
        val rows=glyphs[ch]?:continue
        for((yy,row) in rows.withIndex())for((xx,c) in row.withIndex())if(c=='1'){
            val pos=Offset(start+(i*6+xx)*unit,y+yy*unit)
            drawRect(Color(0xFF21120A),pos+Offset(unit,unit),Size(unit,unit))
            val shade=if(yy<2)color else if(yy<5)color.copy(red=color.red*.90f,green=color.green*.88f,blue=color.blue*.85f)else color.copy(red=color.red*.72f,green=color.green*.72f,blue=color.blue*.72f)
            drawRect(shade,pos,Size(unit+.025f,unit+.025f))
        }
    }
}
@Composable internal fun DoomIntermission(e:DoomEngine,reward:GameRunResult?,saving:Boolean,onMenu:()->Unit,onRetry:()->Unit){
    val audio=LocalGameAudio.current
    var stage by remember{mutableIntStateOf(0)}
    var tally by remember{mutableFloatStateOf(0f)}
    var skip by remember{mutableStateOf(false)}
    val metrics=listOf("KILLS" to e.kills*100/e.enemies.size.coerceAtLeast(1),
        "ITEMS" to e.loot.count{it.taken}*100/e.loot.size.coerceAtLeast(1),
        "SECRETS" to e.secrets*100,
        "TRANSITO" to if(e.questionsAnswered>0)e.questionsCorrect*100/e.questionsAnswered else 0)
    LaunchedEffect(skip){
        if(skip){stage=metrics.size;tally=1f;return@LaunchedEffect}
        delay(200)
        for(i in metrics.indices){
            stage=i;tally=0f
            repeat(13){step->tally=(step+1)/13f;delay(31)}
            audio?.play("doom_reload");delay(120)
        }
        stage=metrics.size;tally=1f
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF241A15))){
        // A low-res 320x200 style composition mapped uniformly to the actual screen.
        Canvas(Modifier.fillMaxSize().clickable(enabled=stage<metrics.size,onClickLabel="Mostrar resultados"){skip=true}
            .semantics{contentDescription="Resultados del sector. "+metrics.joinToString{it.first+" "+it.second+"%"}}){
            val tile=size.width/160f
            // Original corroded masonry: opaque pixels and strong bevels, not a modern gradient.
            for(y in 0..(size.height/tile).toInt())for(x in 0..159){
                val sx=(x+if(y/12%2==0)0 else 13)%26
                val sy=y%12
                val n=((x*31+y*71+x*y*3) xor (x*13+y*9)) and 15
                val c=when{
                    sx==0||sy==0->Color(0xFF1C1511)
                    sx==1||sy==1->Color(0xFF534032)
                    sx==25||sy==11->Color(0xFF251A14)
                    else->Color(red=(53+n)/255f,green=(33+n)/255f,blue=(23+n)/255f)
                }
                drawRect(c,Offset(x*tile,y*tile),Size(tile+.2f,tile+.2f))
            }
            val u=min(size.width/320f,size.height/200f)
            val ox=(size.width-320*u)/2
            val oy=(size.height-200*u)/2
            fun t(text:String,x:Float,y:Float,scale:Float,c:Color,centered:Boolean=false){
                pixelText(text,ox+x*u,oy+y*u,u*scale,c,centered)
            }
            t("E1M1",160f,6f,1.35f,ivory,true)
            t("ESTACIONAMIENTO INFERNAL",160f,21f,1.8f,red,true)
            t(if(e.won)"COMPLETADO" else "MISION FALLIDA",160f,43f,2.05f,red,true)
            for((i,m) in metrics.withIndex()){
                val y=69f+i*20f
                t(m.first,24f,y,1.85f,ochre)
                val v=when{stage>i->m.second;stage==i->(m.second*tally).toInt();else->0}
                t(v.toString()+"%",226f,y,2.05f,red)
            }
            val seconds=e.elapsed.toInt()
            t("TIME",23f,151f,1.45f,ochre)
            t("%02d:%02d".format(seconds/60,seconds%60),74f,151f,1.45f,ivory)
            t("SCORE",173f,151f,1.45f,ochre)
            t(e.score.toString(),234f,151f,1.45f,ivory)
            val rewardText=if(saving)"GUARDANDO..." else reward?.takeIf{it.syncedWithBackend}?.let{"+${it.xpAwarded} XP / +${it.coinsAwarded} MONEDAS"}?:"RECORD LOCAL"
            t(rewardText,160f,166f,1f,ochre,true)
        }
        // Classic text menu: genuine touch actions, no Material cards or buttons.
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom=4.dp).height(30.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            RetroMenuItem("REINTENTAR",!saving,onRetry)
            RetroMenuItem("MENU",!saving,onMenu)
        }
    }
}
@Composable private fun RetroMenuItem(label:String,enabled:Boolean,action:()->Unit){
    Canvas(Modifier.width(if(label=="MENU")94.dp else 150.dp).fillMaxHeight().clickable(enabled=enabled,onClickLabel=label,onClick=action)
        .semantics{contentDescription=label}){
        val text="> "+label
        val u=min(size.width/(text.length*6+2),size.height/10f)
        val p=Offset(size.width*.5f,(size.height-u*7)/2f)
        drawRect(Color(0xFF241A15).copy(alpha=.86f))
        pixelText(text,p.x,p.y,u,if(enabled)ochre else Color(0xFF77736C),true)
    }
}
