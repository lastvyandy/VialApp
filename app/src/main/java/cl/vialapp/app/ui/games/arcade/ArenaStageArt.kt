package cl.vialapp.app.ui.games.arcade

// GUÍA DEL ARCHIVO:
// Escenarios y elementos visuales de Don Pelao Arena.

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.sin

/** Vector environments: the same collision plane in three distinct locations. */
internal fun DrawScope.drawArenaStage(f:ArenaCombatFrame,floor:Float){
    val w=size.width;val h=size.height
    if(w<=0f||h<=0f)return
    val level=f.rival.coerceIn(1,3)
    val sky=when(level){
        1->listOf(Color(0xFF252944),Color(0xFFD47C54),Color(0xFFF6BC76))
        2->listOf(Color(0xFF11182D),Color(0xFF233D6A),Color(0xFF537C9D))
        else->listOf(Color(0xFF241C2C),Color(0xFF643344),Color(0xFFB66A4E))
    }
    val accent=when(level){1->Color(0xFFFFC47B);2->Color(0xFF64D9F8);else->Color(0xFFFB9361)}
    drawRect(Brush.verticalGradient(sky,endY=floor))
    val horizon=floor*.66f
    // Distant mountains, roofs and skyline; two contrast layers create depth.
    if(level==1){
        val ridge=Path().apply{
            moveTo(0f,horizon)
            for(i in 0..10)lineTo(w*i/10f,horizon-h*(.10f+.07f*sin(i*1.8f)))
            lineTo(w,floor);lineTo(0f,floor);close()
        }
        drawPath(ridge,Color(0xFF74617C))
        drawCircle(Color(0xFFFFD79D),w*.085f,Offset(w*.78f,h*.22f))
    }
    val far=if(level==2)Color(0xFF203451)else Color(0xFF42384B)
    val near=if(level==3)Color(0xFF312534)else Color(0xFF243244)
    for(i in -1..9){
        val bw=w*.115f
        val x=(i-.4f)*bw-(f.playerX-.5f)*w*.022f
        val high=h*(.13f+(i+4)%4*.045f)
        drawRect(far,Offset(x,horizon-high),Size(bw*.88f,floor-horizon+high))
        for(j in 0..2)for(k in 0..2){
            val xx=x+bw*(.17f+j*.24f);val yy=horizon-high+h*(.023f+k*.037f)
            drawRect(accent.copy(alpha=if((i+j+k)%3==0).42f else .09f),Offset(xx,yy),Size(bw*.10f,h*.012f))
        }
    }
    // Store fronts / underpass pillars / factory gantry.
    val baseline=floor-h*.085f
    for(i in 0..3){
        val x=i*w*.275f-w*.035f
        val bw=w*.235f
        val top=baseline-h*(.24f+.035f*(i%2))
        drawRect(near,Offset(x,top),Size(bw,baseline-top))
        drawRect(Color(0xFF152233),Offset(x+w*.012f,top+h*.06f),Size(bw-w*.024f,baseline-top-h*.06f))
        drawRect(accent.copy(alpha=.55f),Offset(x+w*.015f,top+h*.022f),Size(bw-w*.03f,h*.017f))
        if(level!=3){
            val wet=if(level==2)Color(0xFF406585)else Color(0xFF86644C)
            drawRect(wet,Offset(x+w*.028f,top+h*.10f),Size(bw*.74f,h*.095f))
            drawLine(near,Offset(x+bw*.5f,top+h*.10f),Offset(x+bw*.5f,top+h*.195f),3f)
            for(k in 0..3)drawLine(near,Offset(x+w*.027f,top+h*(.22f+k*.014f)),Offset(x+bw*.88f,top+h*(.22f+k*.014f)),2f)
        }else{
            for(k in 0..2)drawRect(Color(0xFF5D4148),Offset(x+w*.029f,top+h*(.09f+k*.047f)),Size(bw*.68f,h*.025f))
        }
    }
    // Railings and traffic barriers at the edge of the arena.
    drawRect(Color(0xFF1B2737),Offset(0f,baseline-h*.018f),Size(w,h*.015f))
    for(i in 0..11){
        val x=i*w/11f
        drawLine(Color(0xFF1B2737),Offset(x,baseline-h*.045f),Offset(x,baseline+h*.023f),w*.012f)
    }
    drawRect(Color(0xFF79818A),Offset(0f,baseline+h*.017f),Size(w,floor-baseline-h*.017f))
    for(i in 0..9)drawRect(if(i%2==0)Color(0xFFE1C0A0)else Color(0xFF4C5764),Offset(i*w/10f,floor-h*.023f),Size(w/10f,h*.020f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF485461),Color(0xFF1B2531)),startY=floor,endY=h),Offset(0f,floor),Size(w,h-floor))
    val base=floor+h*.10f
    for(i in -1..4){
        val x=i*w*.25f
        drawLine(Color(0xFFF5CB76).copy(alpha=.75f),Offset(x,base),Offset(x+w*.14f,base),h*.008f,StrokeCap.Round)
    }
    if(level==2){
        for(i in 0..3)drawOval(accent.copy(alpha=.09f),Offset(i*w*.29f,floor+h*.03f),Size(w*.21f,h*.09f))
    }
}
