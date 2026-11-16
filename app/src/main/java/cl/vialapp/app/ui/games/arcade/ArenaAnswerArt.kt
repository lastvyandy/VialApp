package cl.vialapp.app.ui.games.arcade

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/** Answer cinematic: wind-up, impact at 45%, expanding fragments, recovery.
 * Visuals consume only normalized timeline state; they never mutate gameplay. */
internal fun DrawScope.drawArenaAnswerEffect(correct:Boolean,progress:Float,player:Offset,rival:Offset,u:Float){
    if(u<=0f)return
    val p=progress.coerceIn(0f,1f)
    val target=if(correct)rival else player
    val origin=if(correct)player else rival
    val ink=if(correct)Color(0xFF83F9D3)else Color(0xFFFF7368)
    if(p<.45f){
        val t=p/.45f
        val orb=origin+(target-origin)*t
        drawCircle(ink.copy(alpha=.16f), (19f+13f*t)*u,orb)
        drawCircle(ink, (7f+6f*t)*u,orb)
        drawCircle(Color.White, 4f*u,orb)
        drawLine(ink.copy(alpha=.57f),origin,orb,6f*u,StrokeCap.Round)
        if(correct){
            drawCircle(Color(0xFFA8F5FF).copy(alpha=.16f),49f*u,rival)
            drawCircle(Color(0xFFA8F5FF),49f*u,rival,style=Stroke(3f*u))
        }
    }else{
        val t=((p-.45f)/.55f).coerceIn(0f,1f)
        val fade=1f-t
        if(t<.18f)drawRect(ink.copy(alpha=.15f*(1f-t/.18f)))
        drawCircle(ink.copy(alpha=.18f*fade), (31f+64f*t)*u,target)
        drawCircle(ink.copy(alpha=fade), (16f+80f*t)*u,target,style=Stroke((4f-2f*t)*u))
        for(i in 0..13){
            val angle=i*6.2831853f/14f
            val direction=Offset(cos(angle),sin(angle))
            val x=target+direction*((21f+(50f+(i%3)*15f)*t)*u)+Offset(0f,14f*t*t*u)
            val normal=Offset(-direction.y,direction.x)
            val fragment=Path().apply{
                val a=x+direction*(11f*u);val b=x-normal*(5f*u);val c=x-direction*(7f*u)+normal*(3f*u)
                moveTo(a.x,a.y);lineTo(b.x,b.y);lineTo(c.x,c.y);close()
            }
            drawPath(fragment,ink.copy(alpha=fade))
            drawLine(Color.White.copy(alpha=.75f*fade),x,x-direction*(10f*u),2f*u,StrokeCap.Round)
        }
        if(!correct&&t<.65f)drawCircle(Color(0xFF8BDCF9).copy(alpha=.70f*(1f-t/.65f)),49f*u,rival,style=Stroke(3f*u))
    }
}
