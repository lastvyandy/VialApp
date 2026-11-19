package cl.vialapp.app.ui.games.doom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val background=Color(0xFF161719)
private val panel=Color(0xFF25292D)
private val gold=Color(0xFFF2CA75)
private val pale=Color(0xFFE7E2D4)
private val red=Color(0xFFC84F3C)

@Composable
// La idea es poder leer de qué trata cada nivel antes de entrar a jugar.
internal fun DoomMissionMenu(
    unlocked:Int,
    record:Int,
    onBack:()->Unit,
    onStart:(Int)->Unit
){
    // Cambiar la misión solo actualiza esta vista, no inicia el nivel al tiro.
    var selected by rememberSaveable{mutableIntStateOf(1)}
    var controlsOpen by rememberSaveable{mutableStateOf(false)}
    val brief=remember(selected){DoomStory.brief(selected)}
    // Los niveles bloqueados igual se pueden mirar; jugar es otra cosa.
    val available=selected<=unlocked
    Column(
        modifier=Modifier.fillMaxSize().background(background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal=18.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        TextButton(onClick=onBack){Text("‹ Minijuegos",color=gold)}
        Text("DON PELAO",color=red,fontWeight=FontWeight.Black,fontSize=28.sp)
        Text("OPERACIÓN CAOS VIAL",color=gold,fontWeight=FontWeight.Black,fontSize=21.sp)
        Text("EPISODIO 01 · SELECCIÓN DE MISIÓN",color=pale,fontSize=11.sp,
            fontFamily=FontFamily.Monospace)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(7.dp)
        ){
            for(level in 1..5){
                Button(
                    onClick={selected=level},
                    shape=RoundedCornerShape(8.dp),
                    colors=ButtonDefaults.buttonColors(
                        containerColor=if(selected==level)red else Color(0xFF363B3E)
                    ),
                    contentPadding=PaddingValues(horizontal=14.dp,vertical=7.dp)
                ){
                    Text("E1M$level",color=Color.White,fontSize=12.sp,
                        fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Black)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(155.dp)
            .border(2.dp,Color(0xFF716754),RoundedCornerShape(5.dp))
            .background(Color(0xFF111A1D))){
            DoomStoryboardArt(selected,0,Modifier.fillMaxSize())
            Text(
                if(available)"MISIÓN DISPONIBLE" else "MISIÓN BLOQUEADA",
                Modifier.align(Alignment.TopEnd)
                    .background(Color(0xD1161719))
                    .padding(horizontal=9.dp,vertical=5.dp),
                color=if(available)gold else Color(0xFFC6BAB0),
                fontSize=10.sp,fontFamily=FontFamily.Monospace
            )
        }
        Column(
            Modifier.fillMaxWidth()
                .background(panel,RoundedCornerShape(8.dp))
                .border(1.dp,Color(0xFF4B504F),RoundedCornerShape(8.dp))
                .padding(13.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)
        ){
            Text("E1M$selected · "+DoomStory.title(selected),
                color=gold,fontSize=19.sp,fontWeight=FontWeight.Black)
            Text(brief.subtitle,color=red,fontSize=11.sp,
                fontWeight=FontWeight.Bold,fontFamily=FontFamily.Monospace)
            Text(brief.teaser,color=pale,fontSize=13.sp,lineHeight=19.sp)
            HorizontalDivider(color=Color(0xFF4B504F))
            Text("OBJETIVO · "+brief.objective,color=pale,fontSize=12.sp,lineHeight=18.sp)
            if(!available){
                Text("Para desbloquear esta misión, completa E1M"+(selected-1)+".",
                    color=Color(0xFFD7B49C),fontSize=12.sp)
            }
            Button(
                onClick={onStart(selected)},
                enabled=available,
                modifier=Modifier.fillMaxWidth(),
                colors=ButtonDefaults.buttonColors(
                    containerColor=red,disabledContainerColor=Color(0xFF464446)
                )
            ){
                Text(if(available)"VER INTRODUCCIÓN Y JUGAR" else "MISIÓN BLOQUEADA",
                    color=Color.White,fontWeight=FontWeight.Black,fontSize=12.sp)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically){
            Text("RÉCORD: $record",color=gold,fontSize=11.sp,
                fontFamily=FontFamily.Monospace)
            TextButton(onClick={controlsOpen=!controlsOpen}){
                Text(if(controlsOpen)"OCULTAR CONTROLES" else "VER CONTROLES",color=pale,
                    fontSize=11.sp,fontFamily=FontFamily.Monospace)
            }
        }
        if(controlsOpen){
            Text("Pantalla: desplázate con el joystick, desliza para mirar y toca las puertas para interactuar.",
                color=pale,fontSize=12.sp,lineHeight=17.sp)
            Text("Xbox: stick izquierdo mover · derecho mirar · RT disparar · LT golpear · A usar · X recargar · Y/RB arma siguiente · LB arma anterior · START pausa.",
                color=gold,fontSize=12.sp,lineHeight=17.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}
