package cl.vialapp.app.ui.games.doom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val boardBlack=Color(0xFF0A0C0B)
private val boardText=Color(0xFFD8CCAB)
private val boardGold=Color(0xFFCEAC6B)
private val boardBorder=Color(0xFF82745C)

/** A recoverable document styled as an in-game 1990s terminal, not a Material dialog. */
@Composable
internal fun DoomDossier(note:DoomNote,number:Int,total:Int,controller:Boolean,onClose:()->Unit){
    Box(Modifier.fillMaxSize().background(Color(0xF50B0F0F)),
        contentAlignment=Alignment.Center){
        // Subtle CRT scanlines are only drawn while a document is open.
        Canvas(Modifier.fillMaxSize()){
            val spacing=6.dp.toPx()
            var y=0f
            while(y<size.height){
                drawLine(Color(0x101DA084),Offset(0f,y),Offset(size.width,y),1.dp.toPx())
                y+=spacing
            }
        }
        Column(Modifier.fillMaxWidth(.90f).widthIn(max=720.dp)
            .heightIn(max=340.dp)
            .border(3.dp,boardBorder)
            .background(Color(0xFF202521))
            .padding(12.dp),
            verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("VIALAPP  /  ARCHIVO RECUPERADO",color=boardGold,fontSize=12.sp,
                    fontWeight=FontWeight.Black,fontFamily=FontFamily.Monospace,
                    modifier=Modifier.weight(1f))
                Text("["+number.toString().padStart(2,'0')+"/"+total.toString().padStart(2,'0')+"]",
                    color=boardText,fontFamily=FontFamily.Monospace,fontSize=11.sp)
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF963B34)))
            Text(note.title.uppercase(),color=Color(0xFFF2D9A7),fontSize=19.sp,
                fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Black,lineHeight=23.sp)
            Box(Modifier.fillMaxWidth().weight(1f)
                .border(1.dp,Color(0xFF4C5A50))
                .background(boardBlack).verticalScroll(rememberScrollState())
                .padding(horizontal=13.dp,vertical=10.dp)){
                Text("> "+note.body+"\n\n_",
                    fontFamily=FontFamily.Monospace,fontSize=14.sp,lineHeight=21.sp,
                    color=boardText)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween){
                Text(if(controller)"A / B · CERRAR" else "DOCUMENTO ALMACENADO",
                    color=Color(0xFF91B49F),fontFamily=FontFamily.Monospace,fontSize=10.sp)
                Button(onClick=onClose,
                    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF813D30))){
                    Text("CERRAR [X]",color=Color.White,
                        fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}
