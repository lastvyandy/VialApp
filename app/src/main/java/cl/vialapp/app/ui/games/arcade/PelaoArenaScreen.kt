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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.vialapp.app.data.games.GameProgressStore
import androidx.compose.ui.unit.sp
import kotlin.math.sin

@Composable
fun PelaoArenaScreen(userKey: String, onBack: () -> Unit) {
    ArcadeSession(
        userKey = userKey, gameId = GameProgressStore.PELAO_ARENA, title = "Don Pelao Arena",
        subtitle = "Tres rivales con patrones propios, esquivas y un ataque especial.",
        rules = "Resuelve el peligro de tránsito para romper la guardia. Golpea para cargar Pelao Power y usa el especial cuando llegue a 100%. Los rivales avanzados contraatacan; esquiva en cuanto se encienda la alerta roja. Cada victoria recupera hasta 15 HP. Combate ficticio.",
        duration = FightEngine.TIME, create = { FightEngine() }, onBack = onBack, showPlayMessages=false,
        scene = { frame, modifier, _, _ -> FightScene(frame, modifier) },
        resultArt = { frame, modifier ->
            PelaoSprite(frame.pose, modifier)
        },
        controls = { frame, enabled, act ->
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    val ready = enabled && frame.phase == FightPhase.TELEGRAPH
                    ArcadeControl("Detenerse", ready, Modifier.weight(1f)) { act(0) }
                    ArcadeControl("Reducir", ready, Modifier.weight(1f)) { act(1) }
                    ArcadeControl("Distancia", ready, Modifier.weight(1f)) { act(2) }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    ArcadeControl("Golpear",enabled&&frame.phase==FightPhase.OPENING&&frame.attackLock<=0f,Modifier.weight(1f),frame.phase==FightPhase.OPENING){act(3)}
                    ArcadeControl("Especial "+frame.power+"%",enabled&&frame.phase==FightPhase.OPENING&&frame.attackLock<=0f&&frame.power>=100,Modifier.weight(1f),frame.power>=100){act(4)}
                }
                ArcadeControl("ESQUIVAR",enabled&&frame.phase==FightPhase.COUNTER,Modifier.fillMaxWidth(),frame.phase==FightPhase.COUNTER){act(5)}
            }
        }
    )
}

@Composable
private fun FightScene(frame: FightFrame, modifier: Modifier) {
    val opening = frame.phase == FightPhase.OPENING
    val counter=frame.phase==FightPhase.COUNTER
    val pose = frame.pose
    val attacking = pose == PelaoPose.ATTACK_ONE || pose == PelaoPose.ATTACK_TWO
    val takingDamage = pose == PelaoPose.HIT
    val enemyMax = 100 + (frame.boss - 1).coerceIn(0, 2) * 20
    val enemyColor = when (frame.boss) { 1 -> Color(0xFFF97316); 2 -> Color(0xFFA78BFA); else -> Color(0xFFFB7185) }
    val enemyName = when (frame.boss) { 1 -> "IMPRUDENTE"; 2 -> "DISTRAÍDO"; else -> "TEMERARIO" }
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF15253A)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text("DON PELAO · ${frame.hp} HP", color = Color(0xFFDBEAFE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { frame.hp / 100f }, Modifier.fillMaxWidth().height(7.dp), color = Color(0xFF4ADE80), trackColor = Color(0xFF35445A))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("$enemyName · ${frame.bossHp} HP", color = enemyColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { (frame.bossHp.toFloat() / enemyMax).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(7.dp), color = enemyColor, trackColor = Color(0xFF35445A))
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
            Text("RIVAL "+frame.boss.coerceAtMost(3)+"/3",color=Color(0xFFDBEAFE),fontSize=11.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(progress={frame.power/100f},modifier=Modifier.weight(1f).height(7.dp),color=Color(0xFFFBBF24),trackColor=Color(0xFF35445A))
            Text("POWER "+frame.power+"%",color=Color(0xFFFDE68A),fontSize=10.sp,fontWeight=FontWeight.Bold)
        }
        Surface(color = if(counter)Color(0xFFB42318) else if (opening) Color(0xFF166534) else Color(0xFF31455F), shape = RoundedCornerShape(11.dp)) {
            Column(Modifier.fillMaxWidth().padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (frame.phase) { FightPhase.TELEGRAPH -> frame.hazard.label; FightPhase.OPENING -> "¡GUARDIA ROTA!"; FightPhase.COUNTER -> "¡ESQUIVA AHORA!"; FightPhase.RECOVERY -> frame.message },
                    fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
                Text(if (opening) "VENTANA DE ATAQUE" else if(counter) "CONTRAATAQUE" else if (frame.phase == FightPhase.TELEGRAPH) "ELIGE UNA DECISIÓN SEGURA" else "PREPARACIÓN", color = Color(0xFFDBEAFE), fontSize = 10.sp)
                LinearProgressIndicator(progress = { (frame.remaining / (if(counter) 1.4f else if (opening) 3.2f else 6f)).coerceIn(0f, 1f) },
                    Modifier.fillMaxWidth().height(4.dp), color = if (opening) Color(0xFF86EFAC) else Color(0xFFFBBF24), trackColor = Color(0xFF172033))
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF20344C), Color(0xFF4C6D83))), size = size)
                val floor = size.height * .82f
                repeat(7) { i ->
                    val w = size.width / 7
                    val h = size.height * (.19f + (i % 3) * .09f)
                    drawRect(Color(0xFF263B50), Offset(i * w, floor - h), Size(w * .8f, h))
                }
                drawRect(Color(0xFF455366), Offset(0f, floor), Size(size.width, size.height - floor))
                drawLine(Color(0xFFFCD34D), Offset(0f, floor + 9f), Offset(size.width, floor + 9f), 3f)
                drawOval(Color(0xFF111827).copy(alpha = .38f), Offset(size.width * .10f, floor - 4f), Size(size.width * .33f, 15f))
                if (opening) drawCircle(Color(0xFF86EFAC).copy(alpha = .11f), size.width * .26f, Offset(size.width * .76f, size.height * .51f))
                val cx = size.width * (if(counter) .76f-.17f*(1f-(frame.remaining/1.4f).coerceIn(0f,1f)) else if(attacking&&frame.impact>0f) .80f else .76f)
                val cy = size.height * .53f + sin(frame.elapsed * 5f) * size.height * .018f
                val unit = minOf(size.width / 330f, size.height / 220f)
                if(counter){
                    drawCircle(Color(0xFFEF4444).copy(alpha=.17f),size.width*.35f,Offset(cx,cy))
                    drawLine(Color(0xFFFCA5A5),Offset(cx,cy),Offset(size.width*.28f,cy),5f)
                }
                scale(unit, unit, pivot = Offset(cx, cy)) {
                    drawOval(Color(0xFF111827).copy(alpha = .5f), Offset(cx - 51f, floor - 7f), Size(103f, 15f))
                    // Stylized road-hazard robot, not a static emoji token.
                    drawRoundRect(enemyColor, Offset(cx - 43f, cy - 22f), Size(86f, 77f), CornerRadius(if(frame.boss==3)6f else 20f))
                    drawRoundRect(Color(0xFF23334A),Offset(cx-25f,cy+1f),Size(50f,34f),CornerRadius(8f))
                    if(frame.boss==2)drawCircle(Color(0xFF67E8F9),12f,Offset(cx,cy+18f))
                    else if(frame.boss==3){for(i in -1..1)drawLine(Color(0xFFFBBF24),Offset(cx+i*13f,cy+8f),Offset(cx+i*13f,cy+27f),5f)}
                    else drawLine(Color(0xFFFDE68A),Offset(cx-12f,cy+18f),Offset(cx+12f,cy+18f),6f)
                    drawRoundRect(Color(0xFF192638), Offset(cx - 32f, cy - 49f), Size(64f, 39f), CornerRadius(12f))
                    drawRoundRect(if (opening) Color(0xFF86EFAC) else Color(0xFFFCA5A5), Offset(cx - 23f, cy - 35f), Size(46f, 12f), CornerRadius(5f))
                    for (dx in listOf(-54f, 39f)) {
                        drawRoundRect(enemyColor, Offset(cx + dx, cy + 1f), Size(18f, 53f), CornerRadius(8f))
                        drawCircle(Color(0xFF111827), 12f, Offset(cx + dx + 9f, cy + 48f))
                    }
                    drawRoundRect(Color(0xFF182537), Offset(cx - 33f, cy + 48f), Size(27f, 23f), CornerRadius(5f))
                    drawRoundRect(Color(0xFF182537), Offset(cx + 6f, cy + 48f), Size(27f, 23f), CornerRadius(5f))
                    if (attacking && frame.impact > 0f) {
                        val strike = Path().apply { moveTo(cx - 46f, cy - 38f); lineTo(cx - 10f, cy - 5f); lineTo(cx - 28f, cy + 4f); lineTo(cx + 2f, cy + 29f); lineTo(cx - 64f, cy + 2f); close() }
                        drawPath(strike, Color(0xFFFDE68A))
                    }
                }
            }
            // Give each sprite the same full-body canvas, anchored just above the arena floor.
            // Slight horizontal movement is inside the allocated bounds, so no head or feet are cropped.
            val lunge = if (attacking) (maxWidth * .045f) * (frame.poseTime / .34f).coerceIn(0f, 1f) else 0.dp
            val recoil = if (takingDamage) maxWidth * .025f else 0.dp
            Box(
                Modifier.align(Alignment.BottomStart)
                    .padding(start = maxWidth * .04f + lunge - recoil, bottom = maxHeight * .12f)
                    .width(maxWidth * .53f)
                    .height(maxHeight * .85f)
            ) {
                PelaoSprite(pose, Modifier.fillMaxSize())
            }
            if (takingDamage && frame.impact > 0f) {
                Box(Modifier.matchParentSize().background(Color(0xFFEF4444).copy(alpha = .16f)))
            }
            if (attacking) Text("−${frame.lastDamage} HP · ×${frame.combo}", color = Color(0xFFFDE68A), fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp, modifier = Modifier.align(Alignment.TopCenter).padding(8.dp))
        }
    }
}
