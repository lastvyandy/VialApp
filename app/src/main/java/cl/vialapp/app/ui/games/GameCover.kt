package cl.vialapp.app.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.ui.games.arcade.*
import cl.vialapp.app.ui.games.crossroads.*

@Composable
internal fun GameCover(gameId: String, modifier: Modifier = Modifier) {
    if(gameId==GameProgressStore.PELAO_DOOM){
        cl.vialapp.app.ui.games.doom.DoomPreview(modifier)
        return
    }
    val palette = gameCoverPalette(gameId)
    val category = when (gameId) {
        GameProgressStore.CARVIER_DRIVE -> "CONDUCCIÓN"
        GameProgressStore.PELAO_ARENA -> "REFLEJOS"
        GameProgressStore.BALD_CHALLENGE -> "CONOCIMIENTOS"
        GameProgressStore.VIAL_RALLY -> "COMPETICIÓN"
        GameProgressStore.AUTO_WORKSHOP -> "MECÁNICA"
        GameProgressStore.CROSSROADS_ARCADE -> "PRIORIDADES"
        else -> "SEÑALIZACIÓN"
    }
    Box(modifier.background(Brush.horizontalGradient(palette))) {
        Canvas(Modifier.fillMaxSize()) {
            repeat(7) { i ->
                drawCircle(Color.White.copy(alpha = .045f), size.height * .19f,
                    Offset(size.width * (.08f + i * .14f), size.height * (if (i % 2 == 0) .24f else .8f)))
            }
        }
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.43f).padding(16.dp)) {
            Text(category, color = Color(0xFFDBEAFE), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(when (gameId) {
                GameProgressStore.CARVIER_DRIVE -> "En ruta"
                GameProgressStore.PELAO_ARENA -> "Arena"
                GameProgressStore.BALD_CHALLENGE -> "El reto"
                GameProgressStore.VIAL_RALLY -> "Rally"
                GameProgressStore.AUTO_WORKSHOP -> "Box 01"
                GameProgressStore.CROSSROADS_ARCADE -> "Hora punta"
                else -> "Caza la señal"
            }, color = Color.White, fontSize = 23.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.55f).fillMaxHeight().padding(8.dp), contentAlignment = Alignment.Center) {
            GameCoverIllustration(gameId)
        }
    }
}

private fun gameCoverPalette(gameId:String):List<Color> = when (gameId) {
        GameProgressStore.PELAO_DOOM -> listOf(Color(0xFF342626),Color(0xFF784532))
        GameProgressStore.CARVIER_DRIVE -> listOf(Color(0xFF104B72), Color(0xFF214F64))
        GameProgressStore.PELAO_ARENA, GameProgressStore.BALD_CHALLENGE -> listOf(Color(0xFF24304B), Color(0xFF513552))
        GameProgressStore.VIAL_RALLY -> listOf(Color(0xFF203C51),Color(0xFFB47842))
        GameProgressStore.AUTO_WORKSHOP -> listOf(Color(0xFF273E51), Color(0xFF75552F))
        GameProgressStore.CROSSROADS_ARCADE -> listOf(Color(0xFF204C42), Color(0xFF315D72))
        GameProgressStore.SIGN_HUNT_ARCADE -> listOf(Color(0xFF172C47),Color(0xFF286579))
        else -> listOf(Color(0xFF263D62), Color(0xFF356981))
    }

@Composable
internal fun GameRecordThumbnail(gameId:String,modifier:Modifier=Modifier){
    Box(modifier.clip(RoundedCornerShape(14.dp))
        .background(Brush.horizontalGradient(gameCoverPalette(gameId)))
        .padding(4.dp),contentAlignment=Alignment.Center){
        GameCoverIllustration(gameId)
    }
}

/** Shared artwork for both the game catalog and personal records. */
@Composable
private fun GameCoverIllustration(gameId:String){
    when (gameId) {
        GameProgressStore.PELAO_DOOM -> cl.vialapp.app.ui.games.doom.DoomPreview(Modifier.fillMaxSize())
        GameProgressStore.PELAO_ARENA -> MascotGameThumbnail {
            PelaoSprite(PelaoPose.ATTACK_ONE, Modifier.fillMaxSize())
        }
        GameProgressStore.BALD_CHALLENGE -> MascotGameThumbnail {
            PelaoSprite(PelaoPose.VICTORY, Modifier.fillMaxSize())
        }
        GameProgressStore.CARVIER_DRIVE -> MascotGameThumbnail {
            Image(painterResource(R.drawable.mascot_carvier), "Carvier",
                Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        GameProgressStore.CROSSROADS_ARCADE -> {
            val preview = remember { TrafficFrame(cars = listOf(
                TrafficCar(1, TrafficLane.NORTH, .20f, 0, false, 0f),
                TrafficCar(2, TrafficLane.WEST, .20f, 1, false, 0f),
                TrafficCar(3, TrafficLane.SOUTH, .82f, 2, false, 0f)
            )) }
            TrafficBoard(preview, Modifier.size(122.dp))
        }
        GameProgressStore.VIAL_RALLY -> cl.vialapp.app.ui.games.rally.RallyPreview(cl.vialapp.app.ui.games.rally.RallyTracks.all.first(),Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)))
        GameProgressStore.AUTO_WORKSHOP -> Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * .55f, size.height * .5f)
            val u = minOf(size.width, size.height) / 140f
            drawRoundRect(Color(0xFF182836), center - Offset(48f*u, 59f*u), Size(96f*u, 118f*u), CornerRadius(22f*u))
            for (x in listOf(-48f, 36f)) for (y in listOf(-37f, 23f))
                drawRoundRect(Color(0xFF111827), center + Offset(x*u,y*u), Size(14f*u, 26f*u), CornerRadius(4f*u))
            drawRoundRect(Color(0xFF3B82F6), center - Offset(40f*u, 57f*u), Size(80f*u, 114f*u), CornerRadius(20f*u))
            drawRoundRect(Color(0xFF172E49), center - Offset(30f*u, 32f*u), Size(60f*u, 27f*u), CornerRadius(8f*u))
            drawRoundRect(Color(0xFF6B98BA), center + Offset(-28f*u, 20f*u), Size(56f*u, 18f*u), CornerRadius(6f*u))
            drawCircle(Color(0xFFFFD166), 21f*u, center + Offset(-39f*u, 38f*u), style = Stroke(5f*u))
        }
        else -> SignHuntCover()
    }
}

/** Use the same rounded square silhouette as the traffic-board cover without cropping a mascot. */
@Composable
private fun MascotGameThumbnail(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.size(122.dp).clip(shape)
            .background(Color.White.copy(alpha = .08f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = .12f)), shape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** A compact preview of the actual three-lane sign-catching game, not loose road-sign icons. */
@Composable
private fun SignHuntCover() {
    BoxWithConstraints(
        Modifier.size(122.dp).aspectRatio(1f).clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF203B51))
            .border(1.dp,Color.White.copy(alpha=.16f),RoundedCornerShape(24.dp)),
        contentAlignment=Alignment.Center
    ) {
        val tile=minOf(maxWidth,maxHeight)
        Canvas(Modifier.fillMaxSize()){
            val w=size.width;val h=size.height
            fun point(x:Float,y:Float)=Offset(w*x,h*y)
            drawRect(Brush.verticalGradient(listOf(Color(0xFF52828A),Color(0xFF172C47))))
            val road=Path().apply{
                moveTo(w*.30f,0f);lineTo(w*.70f,0f);lineTo(w*.96f,h);lineTo(w*.04f,h);close()
            }
            drawPath(road,Color(0xFF2D3A4B))
            for(side in listOf(-1f,1f)){
                drawLine(Color(0xFFD6E5E8),point(.5f+side*.19f,0f),point(.5f+side*.45f,1f),strokeWidth=w*.025f)
                for(k in 0..3){
                    val y=k*.25f
                    fun x(t:Float)=.5f+side*(.07f+.075f*t)
                    drawLine(Color(0xFFB5CBD4),point(x(y),y),point(x(y+.13f),y+.13f),strokeWidth=w*.016f,cap=StrokeCap.Round)
                }
            }
            drawLine(Color(0xFF65DDA7),point(.09f,.84f),point(.91f,.84f),strokeWidth=h*.035f)
        }
        SignCoverTile(RoadSign.YIELD,Modifier.align(Alignment.TopEnd).offset(x=-tile*.15f,y=tile*.12f).size(tile*.32f),false)
        SignCoverTile(RoadSign.STOP,Modifier.align(Alignment.BottomStart).offset(x=tile*.13f,y=-tile*.22f).size(tile*.46f),true)
        Canvas(Modifier.align(Alignment.BottomEnd).offset(x=-tile*.11f,y=-tile*.09f).size(tile*.22f)){
            drawCircle(Color(0xFF15865F))
            drawLine(Color.White,Offset(size.width*.25f,size.height*.50f),Offset(size.width*.43f,size.height*.68f),size.width*.10f,cap=StrokeCap.Round)
            drawLine(Color.White,Offset(size.width*.43f,size.height*.68f),Offset(size.width*.77f,size.height*.32f),size.width*.10f,cap=StrokeCap.Round)
        }
    }
}

@Composable
private fun SignCoverTile(sign:RoadSign,modifier:Modifier,selected:Boolean){
    val shape=RoundedCornerShape(10.dp)
    Box(modifier.clip(shape).background(Color(0xFF162B42))
        .border(if(selected)2.dp else 1.dp,if(selected)Color(0xFF61E8AB)else Color(0xFFBBCEDA),shape).padding(3.dp),
        contentAlignment=Alignment.Center){
        RoadSignArt(sign,Modifier.fillMaxSize())
    }
}
