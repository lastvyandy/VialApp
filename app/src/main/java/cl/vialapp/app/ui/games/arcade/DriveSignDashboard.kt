package cl.vialapp.app.ui.games.arcade

// GUÍA DEL ARCHIVO:
// Señales y avisos para las situaciones del juego de manejo.

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import kotlinx.coroutines.delay

@Composable
internal fun DriveSignDashboard(frame:DriveFrame) {
    val c=MaterialTheme.colorScheme
    val cue=driveCue(frame)
    val speed=if(frame.stopped)0 else if(frame.slow)30 else 50
    val green=Color(0xFF17613D)
    val red=Color(0xFF9B2727)
    val amber=Color(0xFF79530C)
    val accent=when(cue){
        DriveCue.STOP,DriveCue.WAIT->red
        DriveCue.GO,DriveCue.STOP_DONE->green
        DriveCue.SCHOOL_AHEAD,DriveCue.SCHOOL->amber
        else->c.surface
    }
    val fg=if(cue==DriveCue.NORMAL)c.onSurface else Color.White
    var lastLives by remember{mutableIntStateOf(frame.lives)}
    var lossSerial by remember{mutableIntStateOf(0)}
    var showLoss by remember{mutableStateOf(false)}
    LaunchedEffect(frame.lives){
        if(frame.lives<lastLives){showLoss=true;lossSerial++}
        lastLives=frame.lives
    }
    LaunchedEffect(lossSerial){
        if(lossSerial>0){delay(1100);showLoss=false}
    }
    Surface(color=accent,shape=RoundedCornerShape(14.dp)){
        Row(Modifier.fillMaxWidth().heightIn(min=66.dp).padding(horizontal=10.dp,vertical=6.dp),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
            Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.widthIn(min=54.dp).semantics{contentDescription="Velocidad simulada: "+speed+" kilómetros por hora"}){
                Text(speed.toString(),fontWeight=FontWeight.ExtraBold,color=fg,fontSize=25.sp)
                Text("km/h",color=fg,fontSize=10.sp)
            }
            Box(Modifier.weight(1f),contentAlignment=Alignment.Center){
                when(cue){
                    DriveCue.STOP -> SignArt(R.drawable.ic_drive_stop,"PARE: detención completa",42)
                    DriveCue.WAIT -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        SignArt(R.drawable.ic_drive_stop,"PARE",36)
                        SignArt(R.drawable.ic_drive_wait,"Espera detenido",30)
                    }
                    DriveCue.GO -> SignArt(R.drawable.ic_drive_go,"Cruce libre: puedes avanzar",42)
                    DriveCue.STOP_DONE -> SignArt(R.drawable.ic_drive_done,"Detención respetada",40)
                    DriveCue.SCHOOL,DriveCue.SCHOOL_AHEAD -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        SignArt(R.drawable.ic_drive_school,if(cue==DriveCue.SCHOOL_AHEAD)"Próxima zona escolar" else "Zona escolar",36)
                        SignArt(R.drawable.ic_drive_limit_30,"Máximo 30 kilómetros por hora",38)
                    }
                    DriveCue.NORMAL -> if(showLoss) Text("−1 ♥",fontSize=18.sp,fontWeight=FontWeight.Bold,color=c.error)
                        else Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
                            Image(painterResource(R.drawable.ic_drive_coin),null,Modifier.size(24.dp))
                            Text(frame.coins.toString(),fontSize=20.sp,fontWeight=FontWeight.Bold,color=fg)
                        }
                }
            }
            Column(horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(4.dp)){
                DriveCount(R.drawable.ic_drive_stop,frame.safeStops,"Detenciones correctas",fg)
                DriveCount(R.drawable.ic_drive_school,frame.safeZones,"Zonas escolares respetadas",fg)
            }
        }
    }
}
@Composable
private fun SignArt(resource:Int,description:String,side:Int){
    Image(painterResource(resource),description,Modifier.size(side.dp))
}
@Composable
private fun DriveCount(resource:Int,value:Int,description:String,color:Color){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp),
        modifier=Modifier.semantics{contentDescription=description+": "+value+" de 2"}){
        Image(painterResource(resource),null,Modifier.size(18.dp))
        Text(value.toString()+"/2",fontSize=12.sp,fontWeight=FontWeight.Bold,color=color)
    }
}
