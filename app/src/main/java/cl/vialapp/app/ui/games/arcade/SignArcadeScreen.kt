package cl.vialapp.app.ui.games.arcade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.games.GameProgressStore

@Composable
fun SignArcadeScreen(userKey: String, onBack: () -> Unit) {
    ArcadeSession(userKey=userKey,gameId=GameProgressStore.SIGN_HUNT_ARCADE,
        title="Caza la Señal",subtitle="Supervivencia · señales en movimiento, sin límite de oleadas.",
        rules="Lee la misión de la parte superior. Toca directamente la señal que corresponde mientras las tres bajan por la carretera. Acertar pronto da puntos extra; equivocarte o dejar escapar la señal cuesta una vida. Cada oleada es más rápida hasta una velocidad máxima jugable. La partida termina cuando pierdes las tres vidas.",
        duration=SignEngine.TIME,create={SignEngine()},onBack=onBack,showPlayMessages=false,endless=true,
        scene={frame,modifier,enabled,act->SignScene(frame,modifier,enabled,act)},
        controls={frame,_,_->
            val c=MaterialTheme.colorScheme
            Surface(color=c.surfaceVariant,shape=RoundedCornerShape(14.dp)){
                Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){
                    Text("Capturadas "+frame.correct,fontWeight=FontWeight.Bold,color=c.onSurfaceVariant)
                    Text("Combo ×"+frame.combo,color=c.primary,fontWeight=FontWeight.ExtraBold)
                }
            }
        })
}

@Composable
private fun SignScene(frame:SignFrame,modifier:Modifier,enabled:Boolean,act:(Int)->Unit){
    val revealing=frame.feedback>0f
    val right=frame.answered>=0&&frame.current.choices[frame.answered]==frame.current.sign
    val p=frame.approach
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF172C47)).padding(9.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)){
        Column(Modifier.fillMaxWidth().padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text("OLEADA "+(frame.index+1)+" · NIVEL "+frame.speedTier,
                color=Color(0xFFBBD6FF),fontSize=11.sp,fontWeight=FontWeight.Bold)
            Text(frame.current.sign.meaning,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.ExtraBold,textAlign=TextAlign.Center)
        }
        LinearProgressIndicator(progress={(1f-p).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(6.dp),
            color=if(p>.75f)Color(0xFFF87171)else Color(0xFF67E8F9),trackColor=Color(0xFF344962))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp))){
            Canvas(Modifier.fillMaxSize()){
                drawRect(Brush.verticalGradient(listOf(Color(0xFF8FB9D1),Color(0xFF3C6474))),size=size)
                val side=size.width*.055f
                drawRect(Color(0xFF344252),Offset(side,0f),Size(size.width-2*side,size.height))
                for(i in 1..2){
                    val x=side+(size.width-2*side)*i/3f
                    val period=size.height*.20f
                    for(k in -1..6){
                        val y=k*period+(p*period*2f)%period
                        drawLine(Color.White.copy(alpha=.65f),Offset(x,y),Offset(x,y+period*.48f),3f)
                    }
                }
                for(i in 0..1){
                    val x=if(i==0)side else size.width-side
                    drawLine(Color(0xFFFFD477),Offset(x,0f),Offset(x,size.height),4f)
                }
                val finish=size.height*.90f
                for(i in 0..11)for(j in 0..1)drawRect(if((i+j)%2==0)Color.White else Color(0xFF111827),
                    Offset(i*size.width/12f,finish+j*8f),Size(size.width/12f,8f))
            }
            val cell=maxWidth/3f
            val diameter=minOf(cell*.81f,maxHeight*.29f).coerceAtLeast(1.dp)
            // The visual target and hit box share the same layout coordinates.
            // Decoys get separate lane offsets so they are genuinely moving targets.
            frame.current.choices.forEachIndexed { index,sign->
                val laneProgress=(p*(.86f+index*.07f)+index*.045f).coerceIn(0f,1f)
                val y=(maxHeight-diameter)*(.06f+.80f*laneProgress)
                val selected=frame.answered==index
                val color=when{
                    revealing&&sign==frame.current.sign->Color(0xFF86EFAC)
                    revealing&&selected->Color(0xFFFCA5A5)
                    else->Color(0xFFDFE8F2)
                }
                Surface(modifier=Modifier.offset(x=cell*index+(cell-diameter)/2f,y=y).size(diameter)
                    .clickable(enabled=enabled&&!revealing){act(index)}
                    .semantics{contentDescription=sign.title},
                    color=Color(0xFF1C354B).copy(alpha=.92f),shape=RoundedCornerShape(15.dp),
                    border=BorderStroke(if(revealing&&sign==frame.current.sign)3.dp else 1.dp,color)){
                    RoadSignArt(sign,Modifier.fillMaxSize().padding(5.dp))
                }
            }
            if(revealing) Surface(Modifier.align(Alignment.BottomCenter).padding(6.dp),
                color=if(right)Color(0xFF17613D)else Color(0xFF9B2727),shape=RoundedCornerShape(10.dp)){
                Text(if(right)"¡CAPTURADA!" else if(frame.timedOut)"¡SE ESCAPÓ!" else "SEÑAL INCORRECTA",
                    Modifier.padding(horizontal=12.dp,vertical=7.dp),color=Color.White,fontWeight=FontWeight.Bold,fontSize=12.sp)
            }
        }
        Text(if(revealing)frame.current.sign.advice else "Toca la señal antes de la meta.",color=Color.White,
            fontSize=12.sp,lineHeight=16.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().heightIn(min=32.dp))
    }
}

@Composable
internal fun RoadSignArt(sign: RoadSign, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = minOf(size.width, size.height) / 200f
        val origin = Offset((size.width - 200f * u) / 2, (size.height - 200f * u) / 2)
        translate(origin.x, origin.y) {
        scale(u, u, pivot = Offset.Zero) {
            val red = Color(0xFFE11D37)
            when (sign) {
                RoadSign.STOP -> {
                    val shape = Path().apply {
                        for (i in 0..7) {
                            val a = Math.PI / 8 + i * Math.PI / 4
                            val x = 100f + (94 * kotlin.math.cos(a)).toFloat()
                            val y = 100f + (94 * kotlin.math.sin(a)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(shape, red)
                    drawPath(shape, Color.White, style = Stroke(4f))
                    signText("PARE", 51f, 117f, android.graphics.Color.WHITE)
                }
                RoadSign.YIELD -> {
                    val triangle = Path().apply { moveTo(15f, 28f); lineTo(185f, 28f); lineTo(100f, 178f); close() }
                    drawPath(triangle, Color.White)
                    drawPath(triangle, red, style = Stroke(19f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
                RoadSign.CURVE_RIGHT,RoadSign.CURVE_LEFT,RoadSign.BUMP,RoadSign.NARROW_BRIDGE,RoadSign.SCHOOL,RoadSign.TWO_WAY -> {
                    val yellow=Color(0xFFFFD344)
                    val diamond=Path().apply{moveTo(100f,4f);lineTo(196f,100f);lineTo(100f,196f);lineTo(4f,100f);close()}
                    drawPath(diamond,yellow);drawPath(diamond,Color.Black,style=Stroke(3f))
                    fun line(a:Offset,b:Offset,w:Float=13f){drawLine(Color.Black,a,b,w,StrokeCap.Round)}
                    when(sign){
                        RoadSign.CURVE_RIGHT,RoadSign.CURVE_LEFT->{
                            val dir=if(sign==RoadSign.CURVE_RIGHT)1f else -1f
                            val path=Path().apply{moveTo(100f-dir*25f,150f);lineTo(100f-dir*25f,105f);quadraticBezierTo(100f-dir*25f,69f,100f+dir*38f,69f)}
                            drawPath(path,Color.Black,style=Stroke(17f,cap=StrokeCap.Round))
                            val head=Path().apply{moveTo(100f+dir*54f,69f);lineTo(100f+dir*21f,46f);lineTo(100f+dir*21f,92f);close()};drawPath(head,Color.Black)
                        }
                        RoadSign.BUMP->{val hump=Path().apply{moveTo(48f,122f);quadraticBezierTo(100f,55f,152f,122f);close()};drawPath(hump,Color.Black)}
                        RoadSign.NARROW_BRIDGE->{for(dir in listOf(-1f,1f)){val p=Path().apply{moveTo(100f+dir*52f,146f);lineTo(100f+dir*52f,124f);lineTo(100f+dir*25f,105f);lineTo(100f+dir*25f,73f);lineTo(100f+dir*52f,53f)};drawPath(p,Color.Black,style=Stroke(12f))}}
                        RoadSign.SCHOOL->{for((x,y) in listOf(Offset(78f,75f),Offset(125f,89f))){drawCircle(Color.Black,9f,Offset(x,y));line(Offset(x,y+13f),Offset(x-4f,y+41f),11f);line(Offset(x-4f,y+39f),Offset(x-18f,y+61f),8f);line(Offset(x-4f,y+39f),Offset(x+12f,y+61f),8f);line(Offset(x,y+20f),Offset(x+18f,y+30f),8f)}}
                        RoadSign.TWO_WAY->{for(dir in listOf(-1f,1f)){val x=100f+dir*26f;line(Offset(x,62f),Offset(x,138f),13f);val y=100f-dir*42f;val h=Path().apply{moveTo(x,y);lineTo(x-17f,y+dir*27f);lineTo(x+17f,y+dir*27f);close()};drawPath(h,Color.Black)}}
                        else->Unit
                    }
                }
                else -> {
                    drawCircle(Color.White, 94f, Offset(100f,100f))
                    drawCircle(red, 85f, Offset(100f,100f), style = Stroke(15f))
                    when (sign) {
                        RoadSign.MAX_50,RoadSign.MAX_30,RoadSign.MAX_60 -> signText(sign.name.substringAfter("_"), 76f, 126f, android.graphics.Color.BLACK)
                        RoadSign.NO_PARKING,RoadSign.NO_STOP -> signText("E", 98f, 133f, android.graphics.Color.BLACK)
                        RoadSign.NO_ENTRY -> {
                            drawLine(Color.Black, Offset(100f, 151f), Offset(100f, 70f), 23f)
                            val arrow = Path().apply { moveTo(100f, 45f); lineTo(64f, 83f); lineTo(136f, 83f); close() }
                            drawPath(arrow, Color.Black)
                        }
                        RoadSign.NO_LEFT -> {
                            drawLine(Color.Black, Offset(123f, 150f), Offset(123f, 85f), 23f)
                            drawLine(Color.Black, Offset(123f, 85f), Offset(67f, 85f), 23f, StrokeCap.Butt)
                            val arrow = Path().apply { moveTo(37f, 85f); lineTo(79f, 51f); lineTo(79f, 119f); close() }
                            drawPath(arrow, Color.Black)
                        }
                        RoadSign.NO_RIGHT -> {
                            drawLine(Color.Black,Offset(77f,150f),Offset(77f,85f),23f)
                            drawLine(Color.Black,Offset(77f,85f),Offset(133f,85f),23f)
                            drawPath(Path().apply{moveTo(163f,85f);lineTo(121f,51f);lineTo(121f,119f);close()},Color.Black)
                        }
                        RoadSign.NO_U -> {
                            val p=Path().apply{moveTo(68f,151f);lineTo(68f,83f);cubicTo(68f,37f,134f,37f,134f,83f);lineTo(134f,119f)}
                            drawPath(p,Color.Black,style=Stroke(20f,cap=StrokeCap.Round))
                            drawPath(Path().apply{moveTo(134f,150f);lineTo(104f,112f);lineTo(164f,112f);close()},Color.Black)
                        }
                        RoadSign.NO_OVERTAKE -> {
                            for(x in listOf(62f,138f)){
                                val ink=if(x<100f)red else Color.Black
                                drawRoundRect(ink,Offset(x-23f,58f),Size(46f,84f),CornerRadius(10f))
                                drawRoundRect(Color.White,Offset(x-15f,74f),Size(30f,17f),CornerRadius(4f))
                                drawRoundRect(Color.White,Offset(x-15f,115f),Size(30f,12f),CornerRadius(4f))
                            }
                        }
                        else -> Unit
                    }
                    if (sign==RoadSign.NO_STOP)drawLine(red,Offset(42f,158f),Offset(158f,42f),15f)
                    if (sign !in listOf(RoadSign.MAX_30,RoadSign.MAX_50,RoadSign.MAX_60,RoadSign.NO_OVERTAKE)) {
                        if (sign == RoadSign.NO_LEFT) drawLine(red, Offset(42f, 158f), Offset(158f, 42f), 15f)
                        else drawLine(red, Offset(42f, 42f), Offset(158f, 158f), 15f)
                    }
                }
            }
        }
        }
    }
}

private fun DrawScope.signText(value: String, sizePx: Float, baseline: Float, color: Int) {
    drawContext.canvas.nativeCanvas.drawText(value, 100f, baseline,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sizePx
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
}
