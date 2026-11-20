package cl.vialapp.app.ui.games.rally

// GUÍA DEL ARCHIVO:
// Pantalla de carrera, HUD, controles y conexión con las reglas del Rally.

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.app.Activity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.PI
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import cl.vialapp.app.ui.games.arcade.LocalGameAudio
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.games.GameRunResult
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.career.*
import cl.vialapp.app.ui.rewards.LocalRewards
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToInt

private val Dark=Color(0xFF101C2A)
private val Panel=Color(0xFF213144)
private val Gold=Color(0xFFFFCD75)
private val Green=Color(0xFF78E2A5)

@Composable
fun VialRallyScreen(userKey:String,onBack:()->Unit,onGarage:()->Unit,onExams:()->Unit){
    val context=LocalContext.current
    val store=remember(userKey){GameProgressStore(context,userKey)}
    val session=remember(context){SessionManager(context)}
    val prefs=remember(userKey){context.getSharedPreferences("rally_records_"+userKey,Context.MODE_PRIVATE)}
    val garage=remember(userKey){context.getSharedPreferences("career_appearance_"+userKey,Context.MODE_PRIVATE)}
    var unlocked by remember(userKey){mutableIntStateOf(store.rallyExamCount())}
    var level by remember(userKey){mutableIntStateOf(store.snapshot().level)}
    var loading by remember(userKey){mutableStateOf(true)}
    var checkFailed by remember{mutableStateOf(false)}
    var refresh by remember{mutableIntStateOf(0)}
    var style by remember{mutableStateOf(garage.getString("style","compact")?:"compact")}
    var color by remember{mutableStateOf(garageColor(garage.getString("paint","#38A9F0")?:"#38A9F0"))}
    var kit by remember{mutableStateOf(cachedKit(garage))}
    var track by remember{mutableStateOf(RallyTracks.all.first())}
    var mode by remember{mutableStateOf(RallyMode.CHAOS)}
    var engine by remember{mutableStateOf<RallyEngine?>(null)}
    cl.vialapp.app.ui.GameLandscape(active=engine!=null)
    var loadout by remember{mutableStateOf<RallyLoadout?>(null)}
    var coins by remember{mutableStateOf<Int?>(null)}
    LaunchedEffect(userKey,refresh){
        loading=true;checkFailed=false
        try{
            val token=session.getAccessToken()
            if(token!=null){
                val response=withTimeoutOrNull(5000L){ApiClient.api.getGamificationProgress("Bearer "+token)}
                if(response?.isSuccessful==true&&response.body()!=null){
                    store.applyRemoteProgress(response.body()!!)
                    checkFailed=response.body()?.rallyExamCount==null
                    unlocked=store.rallyExamCount();level=store.snapshot().level
                }else checkFailed=true
                val career=withTimeoutOrNull(3500L){ApiClient.api.getCareer("Bearer "+token).takeIf{it.isSuccessful}?.body()}
                if(career!=null){
                    cacheEquippedGarage(context,userKey,career)
                    style=career.vehicles.firstOrNull{it.id==career.equippedVehicle}?.style?:"compact"
                    color=garageColor(career.paints.firstOrNull{it.id==career.equippedPaint}?.hex?:"#38A9F0")
                    kit=garageKit(career.equippedAccessories);coins=career.coins
                }
            }else checkFailed=true
        }catch(e:CancellationException){throw e}catch(_:Exception){checkFailed=true}
        loadout=RallyLoadout(style,color,kit)
        loading=false
    }
    val current=engine
    val car=loadout
    if(current!=null&&car!=null){
        RallyRace(current,car,store,session,prefs,
            onMenu={engine=null;level=store.snapshot().level;refresh++})
        return
    }
    BackHandler{onBack()}
    Column(Modifier.fillMaxSize().background(Dark).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        TextButton(onClick=onBack){Text("‹ Minijuegos",color=Color.White)}
        Text("VIAL RALLY",fontSize=34.sp,fontWeight=FontWeight.Black,color=Gold)
        Text("Carreras 3D · teléfono en horizontal",color=Color(0xFFB9CBDD),fontSize=13.sp)
        RallyPreview(track,Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(18.dp)))
        if(unlocked<3){
            Text("Licencia de competición",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
            Text("Completa tres exámenes con todas las respuestas. Tus exámenes anteriores también cuentan.",color=Color(0xFFCBD8E4))
            LinearProgressIndicator(progress={unlocked/3f},modifier=Modifier.fillMaxWidth().height(9.dp),color=Green,trackColor=Panel)
            Text(unlocked.toString()+" / 3 exámenes confirmados",color=Green,fontWeight=FontWeight.Bold)
            if(loading)CircularProgressIndicator(color=Gold,modifier=Modifier.size(24.dp))
            if(checkFailed)Text("No se pudo verificar el progreso. Conéctate y vuelve a consultar.",color=Gold)
            Button(onClick=onExams,modifier=Modifier.fillMaxWidth()){Text("Ir a exámenes")}
            OutlinedButton(onClick={refresh++},enabled=!loading,modifier=Modifier.fillMaxWidth()){Text("Actualizar progreso",color=Color.White)}
        }else{
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    Text("LICENCIA ACTIVA · NIVEL "+level,color=Green,fontWeight=FontWeight.Bold,fontSize=12.sp)
                    Text("Tu vehículo equipado",color=Color.White,fontWeight=FontWeight.Bold)
                    Text(coins?.let{"Saldo: "+it+" monedas"}?:"Garaje sincronizado al conectarte",color=Color(0xFFBCC9DA),fontSize=12.sp)
                }
                TextButton(onClick=onGarage){Text("Garaje ›",color=Gold)}
            }
            Text("Modo",color=Color.White,fontWeight=FontWeight.Bold)
            Text(when(mode){RallyMode.CHAOS->"Cajas frecuentes, ataques rivales y turbo.";RallyMode.CLASSIC->"Choques, rebufos y turbo sin objetos.";RallyMode.TIME_TRIAL->"Corre contra el reloj y domina las curvas."},color=Color(0xFFB9CBDD),fontSize=12.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                for(m in RallyMode.entries)FilterChip(selected=mode==m,onClick={mode=m},label={Text(m.title,fontSize=11.sp)},modifier=Modifier.weight(1f))
            }
            Text("Circuitos",color=Color.White,fontWeight=FontWeight.Bold)
            for(t in RallyTracks.all){
                val ready=level>=t.level
                Surface(onClick={if(ready)track=t},shape=RoundedCornerShape(13.dp),color=if(track.id==t.id)Color(0xFF34536A)else Panel){
                    Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        RallyPreview(t,Modifier.size(80.dp,57.dp).clip(RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f)){
                            Text(t.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=14.sp)
                            val best=prefs.getInt("v3_"+t.id+"_"+mode.name,0)
                            Text(if(ready)if(best>0)"Récord: "+formatTime(best/1000f) else "Disponible" else "Bloqueado · nivel "+t.level,color=if(ready)Green else Gold,fontSize=11.sp)
                        }
                        if(track.id==t.id)Text("✓",color=Green,fontWeight=FontWeight.Bold)
                    }
                }
            }
            Button(onClick={engine=RallyEngine(track,mode,System.nanoTime().toInt())},enabled=!loading&&car!=null&&level>=track.level,modifier=Modifier.fillMaxWidth().height(52.dp)){
                Text(if(loading)"Preparando…" else "¡A CORRER!",fontWeight=FontWeight.Black)
            }
            Text("Los objetos aparecen en el modo caótico. Las monedas de pista suman puntos; el saldo real se actualiza con las recompensas confirmadas por VialApp.",color=Color(0xFFB7C7D9),fontSize=12.sp)
        }
    }
}
@Composable private fun RallyRace(engine:RallyEngine,loadout:RallyLoadout,store:GameProgressStore,session:SessionManager,prefs:android.content.SharedPreferences,onMenu:()->Unit){
    var f by remember(engine){mutableStateOf(engine.snapshot())}
    var left by remember(engine){mutableStateOf(false)}
    var right by remember(engine){mutableStateOf(false)}
    var gas by remember(engine){mutableStateOf(false)}
    var brake by remember(engine){mutableStateOf(false)}
    var paused by remember(engine){mutableStateOf(false)}
    var exit by remember(engine){mutableStateOf(false)}
    var reward by remember(engine){mutableStateOf<GameRunResult?>(null)}
    var saving by remember(engine){mutableStateOf(false)}
    val runId=remember(engine){UUID.randomUUID().toString()}
    val rewards=LocalRewards.current
    val context=LocalContext.current
    val haptic=LocalHapticFeedback.current
    val audio=LocalGameAudio.current
    val sounds=audio?.enabled==true
    val motorOwner=remember(engine){Any()}
    var previousCoins by remember(engine){mutableIntStateOf(0)}
    var previousAir by remember(engine){mutableStateOf(false)}
    var previousCountdown by remember(engine){mutableStateOf(f.countdown>0f)}
    var previousContacts by remember(engine){mutableIntStateOf(0)}
    DisposableEffect(audio,motorOwner){onDispose{audio?.stopDrive(motorOwner)}}
    LaunchedEffect(f.collected){if(f.collected>previousCoins)audio?.play("coin");previousCoins=f.collected}
    LaunchedEffect(f.air.airborne){
        if(f.air.airborne!=previousAir)audio?.play(if(f.air.airborne)"jump" else "land")
        previousAir=f.air.airborne
    }
    LaunchedEffect(f.countdown>0f){
        if(previousCountdown&&f.countdown<=0f)audio?.play("start")
        previousCountdown=f.countdown>0f
    }
    LaunchedEffect(kotlin.math.abs(f.drift)>.12f){if(kotlin.math.abs(f.drift)>.12f)audio?.play("drift")}
    LaunchedEffect(f.contacts){if(f.contacts>previousContacts){haptic.performHapticFeedback(HapticFeedbackType.LongPress);if(sounds)audio?.play("crash")};previousContacts=f.contacts}
    LaunchedEffect(f.item){if(f.item!=null&&sounds)audio?.play("signal")}
    LaunchedEffect(f.nitro>0f||f.boost>0f){if((f.nitro>0f||f.boost>0f)&&sounds)audio?.play("turbo")}
    fun clearInputs(){left=false;right=false;gas=false;brake=false}
    fun leave(){if(f.finished&&!saving)onMenu()else if(!f.finished){clearInputs();paused=true;exit=true}}
    BackHandler {leave()}
    DisposableEffect(context,engine){
        var ownerContext=context
        while(ownerContext is android.content.ContextWrapper&&ownerContext !is LifecycleOwner)ownerContext=ownerContext.baseContext
        val lifecycle=(ownerContext as? LifecycleOwner)?.lifecycle
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE){clearInputs();paused=true}}
        lifecycle?.addObserver(observer)
        onDispose{lifecycle?.removeObserver(observer)}
    }
    val configuration=LocalConfiguration.current
    val landscapeReady=configuration.screenWidthDp>=configuration.screenHeightDp
    val rpm=(kotlin.math.abs(f.speed)/12f).toInt()
    LaunchedEffect(rpm,gas,paused,f.finished,f.countdown>0f,landscapeReady,sounds){
        audio?.drive(motorOwner,landscapeReady&&!paused&&!f.finished&&f.countdown<=0f&&sounds,
            (.59f+rpm*.086f).coerceAtMost(1.95f),if(gas).14f else .075f)
    }
    LaunchedEffect(engine,paused,f.finished,landscapeReady){
        if(paused||f.finished||!landscapeReady)return@LaunchedEffect
        var last=withFrameNanos{it};var carry=0f
        val tick=1f/60f
        while(!paused&&!f.finished){
            val now=withFrameNanos{it}
            carry+=((now-last)/1_000_000_000f).coerceIn(0f,.10f);last=now
            while(carry>=tick&&!f.finished){
                f=engine.step(tick,(if(right)1f else 0f)-(if(left)1f else 0f),gas,brake)
                carry-=tick
            }
        }
    }
    LaunchedEffect(f.finished){
        if(f.finished){
            audio?.stopDrive(motorOwner)
            audio?.play("victory")
            saving=true
            val key="v3_"+engine.track.id+"_"+engine.mode.name
            val ms=(f.elapsed*1000).roundToInt().coerceAtLeast(1)
            val old=prefs.getInt(key,0)
            if(old==0||ms<old)prefs.edit().putInt(key,ms).apply()
            try{
                val token=session.getAccessToken()
                reward=if(token!=null)store.recordRunRemote(ApiClient.api,token,GameProgressStore.VIAL_RALLY,engine.score(),ceil(f.elapsed.toDouble()).toInt(),eventId=runId)
                else GameRunResult(store.recordLocalHighScore(GameProgressStore.VIAL_RALLY,engine.score()),0,0,false)
                reward?.takeIf{it.syncedWithBackend}?.let{rewards.show("Carrera completada",it.coinsAwarded,it.xpAwarded)}
            }catch(e:CancellationException){throw e}catch(_:Exception){
                reward=GameRunResult(store.recordLocalHighScore(GameProgressStore.VIAL_RALLY,engine.score()),0,0,false)
            }finally{saving=false}
        }
    }
    if(exit)AlertDialog(onDismissRequest={exit=false;paused=false},title={Text("¿Abandonar carrera?")},text={Text("No se otorgarán recompensas.")},
        confirmButton={TextButton(onClick=onMenu){Text("Salir")}},dismissButton={TextButton(onClick={exit=false;paused=false}){Text("Continuar")}})
    Box(Modifier.fillMaxSize().background(Dark)){
        Rally3D(engine.track,f,loadout,Modifier.fillMaxSize())
        // A racing HUD over the scene; the controls never reduce the viewport.
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),
            horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top){
            Surface(color=Dark.copy(alpha=.86f),shape=RoundedCornerShape(12.dp)){
                Row(Modifier.padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
                    Column{
                        Text(if(engine.mode==RallyMode.TIME_TRIAL)"CRONO" else "${f.place} / 6",color=Gold,fontSize=25.sp,fontWeight=FontWeight.Black)
                        Text(if(engine.mode==RallyMode.TIME_TRIAL)"CONTRARRELOJ" else "POSICIÓN",color=Color.White,fontSize=10.sp,fontWeight=FontWeight.Bold)
                    }
                    Column{
                        Text("VUELTA ${f.lap}/${engine.mode.laps}",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
                        Text(formatTime(f.elapsed),color=Green,fontSize=18.sp,fontWeight=FontWeight.Black)
                        LinearProgressIndicator(progress={(f.progress/(engine.track.length*engine.mode.laps)).coerceIn(0f,1f)},modifier=Modifier.width(94.dp).height(4.dp),color=Green,trackColor=Panel)
                    }
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Top){
                RallyMinimap(engine.track,f,Modifier.size(82.dp).clip(RoundedCornerShape(12.dp)).background(Dark.copy(alpha=.86f)))
                Column(horizontalAlignment=Alignment.End){
                    FilledTonalButton(onClick=::leave,modifier=Modifier.height(36.dp),contentPadding=PaddingValues(horizontal=13.dp)){Text("Salir",fontSize=12.sp)}
                    if(!f.finished)FilledTonalButton(onClick={clearInputs();paused=!paused},modifier=Modifier.height(36.dp),contentPadding=PaddingValues(horizontal=13.dp)){Text(if(paused)"Seguir" else "Pausa",fontSize=12.sp)}
                    TextButton(onClick={audio?.setEffectsEnabled(!sounds)},modifier=Modifier.height(30.dp),contentPadding=PaddingValues(horizontal=4.dp)){Text(if(sounds)"Sonido ON" else "Sonido OFF",color=Color.White,fontSize=10.sp)}
                }
            }
        }
        if(!f.finished){
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom=10.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Surface(color=Dark.copy(alpha=.84f),shape=RoundedCornerShape(14.dp)){
                    Column(Modifier.padding(horizontal=14.dp,vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){
                        Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                            Text((kotlin.math.abs(f.speed)*1.55f).roundToInt().toString(),fontSize=32.sp,fontWeight=FontWeight.Black,color=Gold)
                            Text(if(f.speed<-.5f)"R · km/h" else "km/h",Modifier.padding(bottom=5.dp),fontSize=11.sp,color=Color.White)
                        }
                        Text("Fichas "+f.collected+"  ·  "+f.integrity+"% estado",color=Color.White,fontSize=11.sp)
                        if(f.air.airborne||f.speed<-.5f||f.nitro>0f||f.boost>0f||f.drafted||f.driftCharge>0f)Text(if(f.air.airborne)"¡EN EL AIRE!" else if(f.speed<-.5f)"REVERSA" else if(f.driftCharge>0f)"DERRAPE "+(f.driftCharge*100f).roundToInt()+"%" else if(f.nitro>0f)"NITRO" else if(f.boost>0f)"TURBO" else "REBUFO",color=Color(0xFF70E9FF),fontSize=12.sp,fontWeight=FontWeight.Black)
                    }
                }
            }
            if(f.eventTime>0f&&!f.offroad&&!f.wrongWay&&!paused){
                Text(f.message.uppercase(),Modifier.align(Alignment.TopCenter).padding(top=84.dp),
                    color=if(f.hit>0f)Color(0xFFFF9C79)else Gold,fontSize=19.sp,fontWeight=FontWeight.Black)
            }
            val active=landscapeReady&&!paused&&!exit&&f.countdown<=0f
            Row(Modifier.align(Alignment.BottomStart).padding(13.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)){
                Pedal("◀",left,active,Modifier.size(76.dp)){left=it}
                Pedal("▶",right,active,Modifier.size(76.dp)){right=it}
            }
            Column(Modifier.align(Alignment.BottomEnd).padding(13.dp),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(7.dp)){
                Button(onClick={engine.useItem();f=engine.snapshot()},enabled=active&&f.item!=null,
                    modifier=Modifier.height(42.dp).widthIn(min=88.dp),contentPadding=PaddingValues(horizontal=10.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF8557B9))){Text(f.item?.label?.uppercase()?:"SIN OBJETO",fontWeight=FontWeight.Black,fontSize=12.sp)}
                Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.Bottom){
                    Pedal("FRENO\nR",brake,active,Modifier.size(64.dp,70.dp),true){brake=it}
                    Pedal("GAS",gas,active,Modifier.size(76.dp,88.dp)){gas=it}
                }
            }
            if(f.offroad||f.wrongWay){
                Surface(Modifier.align(Alignment.TopCenter).padding(top=92.dp),shape=RoundedCornerShape(10.dp),color=Dark.copy(alpha=.9f)){
                    Row(Modifier.padding(horizontal=9.dp,vertical=3.dp),verticalAlignment=Alignment.CenterVertically){
                        Text(if(f.wrongWay)"SENTIDO CONTRARIO" else "FUERA DE PISTA",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Black)
                        TextButton(onClick={clearInputs();engine.recover();f=engine.snapshot()},enabled=active,contentPadding=PaddingValues(horizontal=12.dp)){Text("Recolocar · +4 s",color=Color.White,fontSize=11.sp)}
                    }
                }
            }
        }
        if(!landscapeReady&&!f.finished)Surface(Modifier.align(Alignment.Center),color=Dark,shape=RoundedCornerShape(16.dp)){
            Text("Gira el teléfono para correr",Modifier.padding(18.dp),color=Color.White,fontWeight=FontWeight.Bold)
        }
        else if(f.countdown>0f&&!f.finished)Text(ceil(f.countdown).toInt().toString(),Modifier.align(Alignment.Center),fontSize=100.sp,color=Color.White,fontWeight=FontWeight.Black)
        else if(f.elapsed<.75f&&!f.finished)Text("¡YA!",Modifier.align(Alignment.Center),color=Green,fontSize=72.sp,fontWeight=FontWeight.Black)
        if(paused&&!f.finished&&!exit)Surface(Modifier.align(Alignment.Center),color=Dark,shape=RoundedCornerShape(16.dp)){
            Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("EN PAUSA",color=Color.White,fontWeight=FontWeight.Black,fontSize=24.sp)
                Button(onClick={paused=false}){Text("Continuar")}
            }
        }
        if(f.finished)Surface(Modifier.align(Alignment.Center).widthIn(max=340.dp).fillMaxWidth(.72f),color=Dark,shape=RoundedCornerShape(18.dp)){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text(if(f.place==1&&engine.mode!=RallyMode.TIME_TRIAL)"¡VICTORIA!" else "¡META!",fontSize=30.sp,color=Gold,fontWeight=FontWeight.Black)
                Text(if(engine.mode==RallyMode.TIME_TRIAL)formatTime(f.elapsed) else "Posición ${f.place} de 6 · "+formatTime(f.elapsed),color=Color.White)
                Text("${engine.score()} puntos · ${f.contacts} contactos",color=Green,fontWeight=FontWeight.Bold)
                if(saving)CircularProgressIndicator(Modifier.size(22.dp))
                else Text(reward?.let{if(it.syncedWithBackend)"+${it.xpAwarded} XP · +${it.coinsAwarded} monedas" else "Récord local · recompensa no confirmada"}?:"Resultado local",color=Color.White,fontSize=12.sp)
                Button(onClick=onMenu,enabled=!saving){Text("Volver a circuitos")}
            }
        }
    }
}

@Composable private fun RallyMinimap(track:RallyTrack,frame:RallyFrame,modifier:Modifier){
    val path=remember(track){Path().apply{moveTo(track.nodes.first().x,track.nodes.first().y);for(p in track.nodes.drop(1))lineTo(p.x,p.y);close()}}
    val bounds=remember(track){floatArrayOf(track.nodes.minOf{it.x},track.nodes.maxOf{it.x},track.nodes.minOf{it.y},track.nodes.maxOf{it.y})}
    Canvas(modifier.padding(8.dp)){
        val u=minOf(size.width/(bounds[1]-bounds[0]+60f),size.height/(bounds[3]-bounds[2]+60f))
        translate(size.width/2,size.height/2){scale(u,u,pivot=Offset.Zero){
            translate(-(bounds[0]+bounds[1])/2,-(bounds[2]+bounds[3])/2){
                drawPath(path,Color(0xFFB6CBDD),style=Stroke(14f,cap=StrokeCap.Round,join=androidx.compose.ui.graphics.StrokeJoin.Round))
                for(r in frame.rivals){val p=track.at(r.progress,r.lane).first;drawCircle(Color(0xFFFFA072),14f,Offset(p.x,p.y))}
                drawCircle(Green,20f,Offset(frame.pos.x,frame.pos.y));drawCircle(Color.White,20f,Offset(frame.pos.x,frame.pos.y),style=Stroke(4f))
            }
        }}
    }
}
@Composable private fun Pedal(label:String,pressed:Boolean,enabled:Boolean,modifier:Modifier,red:Boolean=false,change:(Boolean)->Unit){
    val latest by rememberUpdatedState(change)
    Box(modifier.clip(RoundedCornerShape(25.dp)).background(if(pressed)if(red)Color(0xFFC05C59)else Color(0xFF287F69)else Panel.copy(alpha=.79f)).border(1.5.dp,Color.White.copy(alpha=.42f),RoundedCornerShape(25.dp))
        .pointerInput(enabled){
            if(enabled)awaitEachGesture{
                val first=awaitFirstDown(requireUnconsumed=false)
                first.consume();latest(true)
                try{do{
                    val event=awaitPointerEvent()
                    val p=event.changes.firstOrNull{it.id==first.id}?:break
                    if(!p.pressed)break
                    p.consume()
                }while(true)}finally{latest(false)}
            }
        },contentAlignment=Alignment.Center){Text(label,textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=Color.White,fontSize=if(label=="◀"||label=="▶")25.sp else 13.sp,fontWeight=FontWeight.Black)}
}
private fun formatTime(seconds:Float):String{
    val ms=(seconds.coerceAtLeast(0f)*1000f).toInt()
    return "%02d:%02d.%02d".format(java.util.Locale.ROOT,ms/60000,(ms/1000)%60,(ms%1000)/10)
}
