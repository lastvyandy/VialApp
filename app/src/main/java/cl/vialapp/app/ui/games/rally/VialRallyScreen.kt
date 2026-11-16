package cl.vialapp.app.ui.games.rally

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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    var player by remember{mutableStateOf<ImageBitmap?>(null)}
    var bots by remember{mutableStateOf<List<ImageBitmap>>(emptyList())}
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
        val selectedStyle=style;val selectedColor=color;val selectedKit=kit
        val sprites=withContext(Dispatchers.Default){
            rasterizeRoadCar(selectedStyle,selectedColor,selectedKit) to listOf(
                rasterizeRoadCar("sport",Color(0xFFE46D61),GarageKit()),
                rasterizeRoadCar("hatch",Color(0xFFECC45E),GarageKit()),
                rasterizeRoadCar("sedan",Color(0xFF79CD9B),GarageKit()),
                rasterizeRoadCar("suv",Color(0xFFAF9CE8),GarageKit()),
                rasterizeRoadCar("compact",Color(0xFFF0F2F5),GarageKit()))
        }
        player=sprites.first;bots=sprites.second;loading=false
    }
    val current=engine
    val sprite=player
    if(current!=null&&sprite!=null){
        RallyRace(current,sprite,bots,kit.neonColor(),store,session,prefs,
            onMenu={engine=null;level=store.snapshot().level;refresh++})
        return
    }
    BackHandler{onBack()}
    Column(Modifier.fillMaxSize().background(Dark).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        TextButton(onClick=onBack){Text("‹ Minijuegos",color=Color.White)}
        Text("VIAL RALLY",fontSize=34.sp,fontWeight=FontWeight.Black,color=Gold)
        Text("Circuitos cerrados · competición arcade",color=Color(0xFFB9CBDD),fontSize=13.sp)
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
                            val best=prefs.getInt(t.id+"_"+mode.name,0)
                            Text(if(ready)if(best>0)"Récord: "+formatTime(best/1000f) else "Disponible" else "Bloqueado · nivel "+t.level,color=if(ready)Green else Gold,fontSize=11.sp)
                        }
                        if(track.id==t.id)Text("✓",color=Green,fontWeight=FontWeight.Bold)
                    }
                }
            }
            Button(onClick={engine=RallyEngine(track,mode,System.nanoTime().toInt())},enabled=!loading&&sprite!=null&&level>=track.level,modifier=Modifier.fillMaxWidth().height(52.dp)){
                Text(if(loading)"Preparando…" else "¡A CORRER!",fontWeight=FontWeight.Black)
            }
            Text("Los objetos aparecen en el modo caótico. Las monedas de pista suman puntos; el saldo real se actualiza con las recompensas confirmadas por VialApp.",color=Color(0xFFB7C7D9),fontSize=12.sp)
        }
    }
}
@Composable private fun RallyRace(engine:RallyEngine,player:ImageBitmap,bots:List<ImageBitmap>,neon:Color?,store:GameProgressStore,session:SessionManager,prefs:android.content.SharedPreferences,onMenu:()->Unit){
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
    fun clearInputs(){left=false;right=false;gas=false;brake=false}
    fun leave(){if(f.finished&&!saving)onMenu()else if(!f.finished){clearInputs();paused=true;exit=true}}
    BackHandler {leave()}
    DisposableEffect(context,engine){
        val lifecycle=(context as? LifecycleOwner)?.lifecycle
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE){clearInputs();paused=true}}
        lifecycle?.addObserver(observer)
        onDispose{lifecycle?.removeObserver(observer)}
    }
    LaunchedEffect(engine,paused,f.finished){
        if(paused||f.finished)return@LaunchedEffect
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
            saving=true
            val key=engine.track.id+"_"+engine.mode.name
            val ms=(f.elapsed*1000).roundToInt().coerceAtLeast(1)
            val old=prefs.getInt(key,0)
            if(old==0||ms<old)prefs.edit().putInt(key,ms).apply()
            try{
                val token=session.getAccessToken()
                reward=if(token!=null)store.recordRunRemote(ApiClient.api,token,GameProgressStore.VIAL_RALLY,engine.score(),ceil(f.elapsed.toDouble()).toInt(),eventId=runId)
                else GameRunResult(store.recordLocalHighScore(GameProgressStore.VIAL_RALLY,engine.score()),0,0,false)
                reward?.takeIf{it.syncedWithBackend}?.let{rewards.show("Carrera completada",it.coinsAwarded,it.xpAwarded)}
            }finally{saving=false}
        }
    }
    if(exit)AlertDialog(onDismissRequest={exit=false;paused=false},title={Text("¿Abandonar carrera?")},text={Text("No se otorgarán recompensas.")},
        confirmButton={TextButton(onClick=onMenu){Text("Salir")}},dismissButton={TextButton(onClick={exit=false;paused=false}){Text("Continuar")}})
    Column(Modifier.fillMaxSize().background(Dark).padding(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick=::leave){Text("‹ Salir",color=Color.White)}
            Text(engine.track.name,modifier=Modifier.weight(1f),color=Gold,fontWeight=FontWeight.Bold,fontSize=14.sp)
            if(!f.finished)TextButton(onClick={clearInputs();paused=!paused}){Text(if(paused)"Seguir" else "Pausa",color=Color.White)}
        }
        Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()){
            Text(if(engine.mode==RallyMode.TIME_TRIAL)"CRONO" else "${f.place}/6 POS",color=Gold,fontWeight=FontWeight.Black,fontSize=18.sp)
            Text("${f.lap}/${engine.mode.laps} VUELTA",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold)
            Text(formatTime(f.elapsed),color=Color.White,fontWeight=FontWeight.Bold)
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(15.dp))){
            RallyScene(engine.track,f,player,bots,neon,Modifier.fillMaxSize())
            if(f.countdown>0&&!f.finished)Text(ceil(f.countdown).toInt().toString(),Modifier.align(Alignment.Center),fontSize=76.sp,color=Color.White,fontWeight=FontWeight.Black)
            if(paused&&!f.finished)Surface(Modifier.align(Alignment.Center),color=Dark,shape=RoundedCornerShape(14.dp)){
                Button(onClick={paused=false},modifier=Modifier.padding(12.dp)){Text("Continuar")}
            }
            if(f.finished)Surface(Modifier.align(Alignment.Center).fillMaxWidth(.88f),color=Dark,shape=RoundedCornerShape(18.dp)){
                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Text("¡META!",fontSize=35.sp,color=Gold,fontWeight=FontWeight.Black)
                    Text(if(engine.mode==RallyMode.TIME_TRIAL)formatTime(f.elapsed) else "Posición ${f.place} de 6",color=Color.White)
                    Text("${engine.score()} puntos",color=Green,fontWeight=FontWeight.Bold)
                    if(saving)CircularProgressIndicator(Modifier.size(22.dp))
                    else Text(reward?.let{if(it.syncedWithBackend)"+${it.xpAwarded} XP · +${it.coinsAwarded} monedas" else "Récord local · recompensa no confirmada"}?:"Resultado local",color=Color.White,fontSize=12.sp)
                    Button(onClick=onMenu,enabled=!saving){Text("Volver a circuitos")}
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text((f.speed*1.55f).roundToInt().toString()+" km/h",color=Gold,fontSize=21.sp,fontWeight=FontWeight.Black)
            Text("Fichas: "+f.collected,color=Color.White,fontSize=12.sp)
            Text("Estado: "+f.integrity+"%",color=Green,fontSize=12.sp)
        }
        LinearProgressIndicator(progress={(f.progress/(engine.track.length*engine.mode.laps)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth(),color=Green,trackColor=Panel)
        if(!f.finished){
            Text(if(f.offroad)"FUERA DE PISTA · reduce velocidad" else if(f.nitro>0)"¡NITRO!" else f.message.ifBlank{"Mantén el acelerador y gira en las curvas"},color=Color(0xFFC3D5E4),fontSize=11.sp,maxLines=1)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
                Pedal("◀",left,!paused,Modifier.weight(1f)){left=it}
                Pedal("▶",right,!paused,Modifier.weight(1f)){right=it}
                Button(onClick={engine.useItem();f=engine.snapshot()},enabled=!paused&&f.item!=null,modifier=Modifier.weight(1.35f).height(58.dp),contentPadding=PaddingValues(3.dp)){Text(f.item?.label?:"Objeto",fontSize=10.sp)}
                Pedal("FRENO",brake,!paused,Modifier.weight(1f),true){brake=it}
                Pedal("GAS",gas,!paused,Modifier.weight(1f),false){gas=it}
            }
        }
    }
}
@Composable private fun Pedal(label:String,pressed:Boolean,enabled:Boolean,modifier:Modifier,red:Boolean=false,change:(Boolean)->Unit){
    val latest by rememberUpdatedState(change)
    Box(modifier.height(64.dp).clip(RoundedCornerShape(13.dp)).background(if(pressed)if(red)Color(0xFFC05C59)else Color(0xFF287F69)else Panel)
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
        },contentAlignment=Alignment.Center){Text(label,color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Black)}
}
private fun formatTime(seconds:Float):String{
    val ms=(seconds.coerceAtLeast(0f)*1000f).toInt()
    return "%02d:%02d.%02d".format(java.util.Locale.ROOT,ms/60000,(ms/1000)%60,(ms%1000)/10)
}
