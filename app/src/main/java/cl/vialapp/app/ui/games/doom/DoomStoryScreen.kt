package cl.vialapp.app.ui.games.doom

// GUÍA DEL ARCHIVO:
// Cinemáticas livianas antes de jugar, con imágenes retro y texto progresivo.

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val sceneBlack=Color(0xFF0D1115)
private val scenePaper=Color(0xFFE8D8B4)
private val sceneAmber=Color(0xFFE8AF5D)
private val sceneRed=Color(0xFF9E372D)
private val sceneFrame=Color(0xFF6C6353)

/** Three short, skippable storyboards per stage; no video files or downloads. */
@Composable
internal fun DoomStoryScreen(level:Int,onStart:()->Unit,onBack:()->Unit){
    val cards=remember(level){DoomStory.intro(level)}
    var page by rememberSaveable(level){mutableIntStateOf(0)}
    var revealed by remember(page){mutableIntStateOf(0)}
    val current=cards[page]
    BackHandler(onBack=onBack)
    LaunchedEffect(level,page){
        for(index in 1..current.text.length){
            delay(18)
            if(revealed>=current.text.length)break
            revealed=index
        }
    }
    fun advance(){
        if(revealed<current.text.length){revealed=current.text.length;return}
        if(page<cards.lastIndex)page++ else onStart()
    }
    Column(
        Modifier.fillMaxSize().background(sceneBlack).padding(horizontal=18.dp,vertical=8.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp)
    ){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween){
            Text("DON PELAO  /  EPISODIO 01",color=sceneAmber,fontSize=13.sp,
                fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold)
            Text("E1M"+level+"    "+(page+1)+" / "+cards.size,color=scenePaper,fontSize=12.sp,
                fontFamily=FontFamily.Monospace)
        }
        Box(Modifier.fillMaxWidth().weight(1f)
            .border(3.dp,sceneFrame).background(sceneBlack)){
            DoomStoryboardArt(level,page,Modifier.fillMaxSize())
            Text("●  VIALAPP / ARCHIVO DE EMERGENCIA",
                modifier=Modifier.align(Alignment.TopStart)
                    .background(sceneBlack.copy(alpha=.87f)).padding(horizontal=7.dp,vertical=4.dp),
                color=sceneAmber,fontSize=10.sp,fontFamily=FontFamily.Monospace)
        }
        Column(Modifier.fillMaxWidth().heightIn(min=96.dp,max=134.dp)
            .border(2.dp,sceneFrame).background(Color(0xFF20231F)).padding(10.dp),
            verticalArrangement=Arrangement.spacedBy(4.dp)){
            Text(current.heading,color=sceneAmber,fontWeight=FontWeight.Black,
                fontFamily=FontFamily.Monospace,fontSize=13.sp)
            Text(current.text.take(revealed)+(if(revealed<current.text.length)"▌" else " █"),
                color=scenePaper,fontFamily=FontFamily.Monospace,fontSize=12.sp,lineHeight=16.sp)
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween){
            TextButton(onClick=onBack){
                Text("VOLVER",color=scenePaper,fontFamily=FontFamily.Monospace)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick=onStart){
                Text("OMITIR INTRO",color=scenePaper,fontFamily=FontFamily.Monospace)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick=::advance,
                colors=ButtonDefaults.buttonColors(containerColor=sceneRed)){
                Text(if(page==cards.lastIndex&&revealed==current.text.length)"ENTRAR AL NIVEL"
                    else if(revealed<current.text.length)"MOSTRAR TODO" else "SIGUIENTE",
                    color=Color.White,fontFamily=FontFamily.Monospace,fontSize=12.sp,
                    fontWeight=FontWeight.Bold)
            }
        }
    }
}

/** Lightweight original pixel-art stage paintings; shapes scale to landscape phones. */
@Composable
internal fun DoomStoryboardArt(level:Int,page:Int,modifier:Modifier=Modifier){
    Canvas(modifier){
        val w=size.width;val h=size.height
        fun block(x:Float,y:Float,bw:Float,bh:Float,color:Color){
            drawRect(color,Offset(x*w,y*h),Size(bw*w,bh*h))
        }
        val palette=when(level){
            1->Color(0xFF27352F)
            2->Color(0xFF292D36)
            3->Color(0xFF392B2E)
            4->Color(0xFF2A2E24)
            else->Color(0xFF202D31)
        }
        block(0f,0f,1f,1f,palette)
        block(0f,.78f,1f,.22f,Color(0xFF161A1C))
        for(k in 0..14){
            val x=k/14f
            block(x,.77f,.003f,.23f,Color(0xFF4B4740))
        }
        for(k in 0..7){
            val y=.80f+k*.027f
            block(0f,y,1f,.004f,Color(0xFF4A4038))
        }
        val blink=if(page==1)Color(0xFFA74430) else Color(0xFF68422E)
        when(level){
            1->{
                for(i in 0..5){
                    val x=.07f+i*.16f
                    block(x,.18f,.12f,.56f,Color(0xFF141B20))
                    block(x+.02f,.23f,.08f,.17f,Color(0xFF4E5E5D))
                    block(x+.03f,.28f,.02f,.05f,blink)
                }
                // Armed security robot
                block(.59f,.29f,.12f,.15f,Color(0xFF657274))
                block(.56f,.44f,.18f,.24f,Color(0xFF35464B))
                block(.575f,.67f,.045f,.12f,Color(0xFF262C2D))
                block(.68f,.67f,.045f,.12f,Color(0xFF262C2D))
                block(.61f,.35f,.028f,.025f,sceneRed)
                block(.665f,.35f,.028f,.025f,sceneRed)
                block(.74f,.49f,.10f,.028f,Color(0xFF6A7274))
            }
            2->{
                for(i in 0..7){
                    val x=i*.145f
                    block(x,.09f,.047f,.65f,Color(0xFF171A20))
                    block(x+.018f,.09f,.012f,.47f,Color(0xFF51535D))
                    block(x+.045f,.11f,.075f,.025f,Color(0xFFC27D45))
                }
                block(.24f,.60f,.48f,.075f,Color(0xFF373B40))
                block(.28f,.56f,.09f,.05f,blink)
                block(.58f,.56f,.09f,.05f,blink)
                block(.47f,.23f,.075f,.095f,Color(0xFF738180))
                block(.492f,.245f,.03f,.04f,sceneRed)
            }
            3->{
                for(i in 0..5){
                    val x=.08f+i*.18f
                    block(x,.14f,.13f,.60f,Color(0xFF171B1E))
                    block(x+.023f,.20f,.08f,.18f,Color(0xFF4A4C4D))
                    block(x+.035f,.25f,.054f,.035f,blink)
                    block(x+.035f,.42f,.054f,.10f,Color(0xFF272E30))
                }
                block(.43f,.37f,.16f,.30f,Color(0xFF4F5A60))
                block(.47f,.22f,.08f,.15f,Color(0xFF6D777A))
                block(.488f,.27f,.043f,.037f,sceneRed)
                block(.395f,.53f,.23f,.045f,Color(0xFF827E68))
            }
            4->{
                block(.13f,.15f,.018f,.62f,Color(0xFF6B6D68))
                block(.086f,.18f,.12f,.22f,Color(0xFFAD3F33))
                block(.125f,.25f,.04f,.038f,Color.White)
                block(.59f,.31f,.16f,.34f,Color(0xFF454D2D))
                block(.61f,.19f,.12f,.20f,Color(0xFF6B7450))
                for(i in 0..5)block(.59f+i*.022f,.16f-(i%2)*.05f,.018f,.09f,Color(0xFF899456))
                block(.635f,.31f,.027f,.025f,sceneRed)
                block(.698f,.31f,.027f,.025f,sceneRed)
                block(.55f,.43f,.055f,.19f,Color(0xFF717E48))
                block(.74f,.43f,.055f,.19f,Color(0xFF717E48))
                block(.25f,.65f,.16f,.09f,Color(0xFF993E30))
                block(.29f,.63f,.08f,.045f,Color(0xFF32393E))
            }
            5->{
                for(i in 0..7){
                    val x=.04f+i*.12f
                    block(x,.10f,.095f,.65f,Color(0xFF131A20))
                    for(j in 0..4){
                        block(x+.017f,.17f+j*.105f,.06f,.017f,Color(0xFF46565B))
                        block(x+.065f,.205f+j*.105f,.008f,.013f,
                            if((i+j+page)%3==0)sceneRed else Color(0xFF638E66))
                    }
                }
                block(.34f,.41f,.30f,.18f,Color(0xFF202D2F))
                block(.37f,.44f,.24f,.12f,Color(0xFF3D6158))
                block(.40f,.46f,.17f,.03f,sceneAmber)
                block(.45f,.52f,.07f,.015f,sceneRed)
            }
        }
        // The three storyboards are distinct: alert, enemy revelation, and
        // Don Pelao's mission. These overlays are drawn once per static scene.
        when(page){
            0->{
                block(.035f,.57f,.18f,.09f,Color(0xFF262B2C))
                block(.047f,.59f,.15f,.012f,sceneAmber)
                block(.047f,.615f,.10f,.01f,sceneRed)
                block(.047f,.634f,.13f,.008f,Color(0xFF728079))
            }
            1->{
                block(.17f,.28f,.14f,.20f,Color(0xFF232B30))
                block(.185f,.31f,.115f,.125f,Color(0xFF526164))
                block(.20f,.345f,.026f,.023f,sceneRed)
                block(.26f,.345f,.026f,.023f,sceneRed)
                block(.18f,.49f,.12f,.13f,Color(0xFF262A2D))
                block(.16f,.51f,.025f,.12f,Color(0xFF42474A))
            }
            else->{
                // Don Pelao seen from behind at the threshold of each mission.
                block(.14f,.50f,.15f,.28f,Color(0xFF242A2D))
                block(.16f,.39f,.11f,.13f,Color(0xFFAB806A))
                block(.15f,.375f,.13f,.042f,Color(0xFF202226))
                block(.12f,.60f,.05f,.15f,Color(0xFF2D3436))
                block(.27f,.60f,.05f,.15f,Color(0xFF2D3436))
                block(.16f,.75f,.045f,.05f,Color(0xFF17191B))
                block(.245f,.75f,.045f,.05f,Color(0xFF17191B))
            }
        }
        for(i in 0..11)block(0f,i/12f,.99f,.002f,Color(0x15101010))
        block(.015f,.05f,.015f,.032f,if(page==2)sceneAmber else sceneRed)
        block(.95f,.05f,.025f,.035f,blink)
    }
}
