package cl.vialapp.app.ui.games.arcade

// GUÍA DEL ARCHIVO:
// Dibujo del Pelao dentro de la arena. El personaje merece algo más que un emoji gigante, jajaj.

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.sin

/** Original articulated vector fighter, with all joints in one shared coordinate system. */
internal fun DrawScope.drawPelao(f:ArenaCombatFrame){
    val outline=Color(0xFF262936)
    val skin=Color(0xFFD99B71)
    val light=Color(0xFFF4C59A)
    val shade=Color(0xFFAA634B)
    val pants=Color(0xFF33465A)
    val beard=Color(0xFF412B27)
    val m=f.move
    val swing=if(m!=null&&f.lock>0f){
        val t=m.lock-f.lock
        if(t<=m.windup)(t/m.windup).coerceIn(0f,1f)
        else (1f-(t-m.windup)/(m.lock-m.windup)).coerceIn(0f,1f)
    }else 0f
    val kick=if(m==ArenaMove.HEAVY)swing else 0f
    val punch=if(m==ArenaMove.HEAVY)0f else swing
    val gait=if(f.walk>0f)sin(f.walkCycle)*f.walkDir else 0f
    val air=f.jumpHeight
    val crouch=if(f.evade>0f)15f else if(f.finished&&!f.won)17f else 0f
    val lean=15f*punch-12f*kick+7f*gait-(if(f.playerImpact>0f)20f*(f.playerImpact/.34f) else 0f)
    val chest=Offset(lean,-112f+crouch+sin(f.elapsed*7f)*2.8f+3f*kotlin.math.abs(gait))
    val waist=Offset(lean,-65f+crouch*.55f)
    fun limb(a:Offset,b:Offset,width:Float,ink:Color){
        drawLine(outline,a,b,width+4f,StrokeCap.Round)
        drawLine(ink,a,b,width,StrokeCap.Round)
    }
    fun leg(side:Float){
        val activeKick=if(side>0f)kick else 0f
        val stride=side*gait
        val knee=Offset(side*23f+38f*activeKick+stride*13f,-31f-39f*activeKick-8f*air)
        val ankle=Offset(side*30f+79f*activeKick+stride*22f,-10f-66f*activeKick-18f*air-kotlin.math.abs(stride)*5f)
        limb(waist+Offset(side*12f,0f),knee,24f,pants)
        limb(knee,ankle,19f,pants)
        drawRoundRect(outline,ankle-Offset(11f,0f),Size(31f,10f),CornerRadius(4f))
        drawRoundRect(Color(0xFF657588),ankle-Offset(8f,-5f),Size(25f,3f),CornerRadius(1.5f))
    }
    leg(-1f);leg(1f)
    val guard=f.guard>0f
    val back=chest+Offset(-27f,-4f)
    val front=chest+Offset(28f,-3f)
    fun arm(a:Offset,b:Offset,c:Offset){
        limb(a,b,26f,skin);limb(b,c,19f,skin)
        drawCircle(shade,12f,a+Offset(1f,2f))
        drawOval(skin,a-Offset(15f,12f),Size(29f,28f))
        drawOval(light,a-Offset(11f,10f),Size(16f,10f))
        drawRoundRect(outline,c-Offset(12f,12f),Size(25f,24f),CornerRadius(9f))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF46566C),Color(0xFF1A2436)),startY=c.y-10f,endY=c.y+10f),c-Offset(9f,10f),Size(20f,16f),CornerRadius(7f))
        drawLine(Color(0xFF7F98AD),c+Offset(-5f,-5f),c+Offset(5f,-6f),2f,StrokeCap.Round)
    }
    arm(back,back+Offset(-19f,25f),if(f.won)back+Offset(-20f,-48f)else if(guard)chest+Offset(7f,-31f)else chest+Offset(-6f,9f))
    val torso=Path().apply{
        moveTo(chest.x-32f,chest.y-17f);quadraticBezierTo(chest.x,chest.y-26f,chest.x+32f,chest.y-17f)
        quadraticBezierTo(chest.x+30f,chest.y+13f,waist.x+18f,waist.y)
        lineTo(waist.x-19f,waist.y);quadraticBezierTo(chest.x-30f,chest.y+15f,chest.x-32f,chest.y-17f);close()
    }
    drawPath(torso,skin);drawPath(torso,outline,style=Stroke(3f))
    val vest=Path().apply{
        moveTo(chest.x-18f,chest.y-21f);lineTo(chest.x-10f,chest.y-20f);lineTo(chest.x-5f,chest.y+2f)
        lineTo(chest.x+12f,chest.y+2f);lineTo(chest.x+18f,chest.y-20f);lineTo(chest.x+24f,chest.y-18f)
        lineTo(waist.x+18f,waist.y);lineTo(waist.x-19f,waist.y);close()
    }
    drawPath(vest,Brush.verticalGradient(listOf(Color(0xFF5C9BC2),Color(0xFF39709A)),startY=chest.y-24f,endY=waist.y))
    drawLine(Color(0xFF28577D),chest+Offset(-18f,14f),waist+Offset(-10f,-5f),3f)
    drawLine(Color(0xFF80C0DB),chest+Offset(-14f,9f),chest+Offset(15f,9f),3f,StrokeCap.Round)
    drawLine(Color(0xFF224665),chest+Offset(-11f,23f),chest+Offset(12f,23f),3f,StrokeCap.Round)
    drawLine(outline,waist-Offset(20f,0f),waist+Offset(20f,0f),6f,StrokeCap.Round)
    drawRoundRect(Color(0xFFF2C477),waist-Offset(5f,4f),Size(10f,8f),CornerRadius(2f))
    val face=chest+Offset(7f,-45f)
    limb(chest+Offset(3f,-23f),chest+Offset(3f,-35f),19f,skin)
    drawOval(outline,face-Offset(27f,31f),Size(53f,60f))
    drawOval(Brush.linearGradient(listOf(light,skin,shade),start=face-Offset(20f,26f),end=face+Offset(28f,25f)),face-Offset(24f,29f),Size(48f,56f))
    drawOval(skin,face-Offset(28f,4f),Size(11f,16f))
    drawOval(light,face-Offset(15f,24f),Size(23f,10f))
    val hair=Path().apply{
        moveTo(face.x-23f,face.y+1f);quadraticBezierTo(face.x-14f,face.y+10f,face.x-2f,face.y+4f)
        quadraticBezierTo(face.x+8f,face.y+9f,face.x+23f,face.y-1f)
        quadraticBezierTo(face.x+25f,face.y+29f,face.x+3f,face.y+32f)
        quadraticBezierTo(face.x-24f,face.y+27f,face.x-23f,face.y+1f);close()
    }
    drawPath(hair,beard);drawPath(hair,outline,style=Stroke(2f))
    drawLine(Color(0xFF724A3B),face+Offset(-14f,16f),face+Offset(-7f,24f),2f,StrokeCap.Round)
    drawLine(beard,face+Offset(-15f,-7f),face+Offset(-4f,-4f),5f,StrokeCap.Round)
    drawLine(beard,face+Offset(7f,-4f),face+Offset(19f,-8f),5f,StrokeCap.Round)
    drawOval(Color.White,face+Offset(-13f,-4f),Size(10f,6f));drawOval(Color.White,face+Offset(8f,-4f),Size(10f,6f))
    drawCircle(outline,2.7f,face+Offset(-6f,-1f));drawCircle(outline,2.7f,face+Offset(15f,-1f))
    drawLine(shade,face+Offset(3f,0f),face+Offset(7f,6f),3f,StrokeCap.Round)
    drawLine(light,face+Offset(-4f,16f),face+Offset(10f,15f),2f,StrokeCap.Round)
    val hand=when{f.won->front+Offset(14f,-50f);guard->face+Offset(15f,16f);else->chest+Offset(37f+52f*punch-12f*kick,-20f-5f*punch)}
    arm(front,front+Offset(14f+15f*punch,if(f.won)-20f else 21f-23f*punch),hand)
    if(guard)drawOval(Color(0xFF7DD3FC).copy(alpha=.75f),chest-Offset(42f,51f),Size(96f,104f),style=Stroke(3f))
    if(kick>.4f){
        val trail=Path().apply{moveTo(38f,-32f);quadraticBezierTo(96f,-82f,128f,-65f);quadraticBezierTo(90f,-60f,53f,-22f);close()}
        drawPath(trail,Color(0xFFFFDC9C).copy(alpha=.22f*kick))
    }
    if(m==ArenaMove.SPECIAL&&punch>0f){
        drawCircle(Color(0xFFF9C950).copy(alpha=.22f),24f+10f*punch,hand)
        drawCircle(Color(0xFFFDE68A),18f,hand,style=Stroke(3f))
    }
}
