package cl.vialapp.app.ui.games.doom

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import cl.vialapp.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.games.GameRunResult
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.games.arcade.LocalGameAudio
import cl.vialapp.app.ui.games.arcade.RoadSign
import cl.vialapp.app.ui.games.arcade.RoadSignArt
import cl.vialapp.app.ui.rewards.LocalRewards
import kotlinx.coroutines.CancellationException
import java.util.UUID
import kotlin.math.*

private val Dark=Color(0xFF161719)
private val Metal=Color(0xFF333638)
private val Gold=Color(0xFFF2CA75)
private val Red=Color(0xFFC84F3C)
private val White=Color(0xFFE7E2D4)

@Composable
fun DoomScreen(userKey:String,onBack:()->Unit){
    val context=LocalContext.current
    val store=remember(context,userKey){GameProgressStore(context,userKey)}
    val session=remember(context){SessionManager(context)}
    var run by remember{mutableStateOf<DoomEngine?>(null)}
    val engine=run
    val audio=LocalGameAudio.current
    if(engine==null){
        BackHandler{onBack()}
        Column(Modifier.fillMaxSize().background(Dark).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp)){
            TextButton(onClick=onBack){Text("‹ Minijuegos",color=Gold)}
            Text("DON PELAO",fontSize=36.sp,fontWeight=FontWeight.Black,color=Red)
            Text("OPERACIÓN CAOS VIAL",fontSize=23.sp,fontWeight=FontWeight.Black,color=Gold)
            DoomPreview(Modifier.fillMaxWidth().aspectRatio(16f/9f))
            Text("EPISODIO 01 · ESTACIONAMIENTO INFERNAL",color=Gold,fontWeight=FontWeight.Bold)
            Text("Carvier: La central vial está fuera de control. Recupera la credencial, desbloquea los accesos de tránsito y destruye el núcleo.",color=White)
            Text("Pulgar izquierdo: muévete en cualquier dirección. Desliza sobre la escena para mirar; toca directamente las puertas. Mantén FIRE para disparar y toca el arma del HUD para cambiarla.",color=White,fontSize=13.sp)
            Text("Récord: "+store.getHighScore(GameProgressStore.PELAO_DOOM)+" · Prototipo jugable",color=Gold,fontSize=12.sp)
            Button(onClick={run=DoomEngine();audio?.play("start")},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Red)){
                Text("ENTRAR AL SECTOR",fontWeight=FontWeight.Black,color=Color.White)
            }
        }
    }else key(engine){DoomRun(engine,store,session,onMenu={run=null},onRetry={run=DoomEngine();audio?.play("start")})}
}

@Composable
internal fun DoomPreview(modifier:Modifier=Modifier){
    val engine=remember{DoomEngine()}
    AndroidView(factory={DoomView(it).apply{this.engine=engine}},modifier=modifier,update={it.invalidate()})
}

@Composable
private fun DoomRun(e:DoomEngine,store:GameProgressStore,session:SessionManager,onMenu:()->Unit,onRetry:()->Unit){
    val context=LocalContext.current
    val audio=LocalGameAudio.current
    val rewards=LocalRewards.current
    val runId=remember{UUID.randomUUID().toString()}
    var revision by remember{mutableIntStateOf(0)}
    var drive by remember{mutableStateOf(DoomInput())}
    var paused by remember{mutableStateOf(false)}
    var exit by remember{mutableStateOf(false)}
    var reward by remember{mutableStateOf<GameRunResult?>(null)}
    var saving by remember{mutableStateOf(false)}
    var resultVisible by remember{mutableStateOf(false)}
    fun clear(){drive=DoomInput()}
    fun drain(){
        for(event in e.drainEvents()){
            audio?.play(when(event){
                DoomEvent.SHOT->"doom_shot";DoomEvent.BLAST->"doom_blast";DoomEvent.PLASMA->"doom_plasma"
                DoomEvent.HIT->"doom_hit";DoomEvent.HURT->"doom_hurt";DoomEvent.COIN->"doom_pickup";DoomEvent.LOOT->"doom_pickup"
                DoomEvent.SWITCH->"doom_switch";DoomEvent.DOOR->"doom_door";DoomEvent.CORRECT->"doom_correct";DoomEvent.WRONG->"doom_wrong"
                DoomEvent.VICTORY->"doom_exit";DoomEvent.DEFEAT->"doom_player_death";DoomEvent.SECRET->"doom_correct"
                DoomEvent.EXPLOSION->"doom_explosion";DoomEvent.RELOAD->"doom_reload";DoomEvent.ENEMY_SHOT->"doom_enemy"
                DoomEvent.ENEMY_DEATH->"doom_death";DoomEvent.MACHINE_DEATH->"doom_machine_death"
            })
        }
    }
    fun change(action:()->Unit){action();drain();revision++}
    val tick=revision
    val challenge=e.pending?.question
    val finished=e.finished
    val configuration=LocalConfiguration.current
    val ready=configuration.screenWidthDp>=configuration.screenHeightDp
    val musicPrefs=remember(context){context.getSharedPreferences("vial_audio",android.content.Context.MODE_PRIVATE)}
    var musicEnabled by remember{mutableStateOf(musicPrefs.getBoolean("doom_music_enabled",true))}
    val music=remember(context){
        val id=context.resources.getIdentifier("doom_music","raw",context.packageName)
        if(id==0)null else runCatching{MediaPlayer.create(context,id)?.apply{isLooping=true;setVolume(.24f,.24f)}}.getOrNull()
    }
    DisposableEffect(music){onDispose{runCatching{music?.release()}}}
    LaunchedEffect(music,musicEnabled,paused,finished,ready,challenge){
        if(music!=null)runCatching{
            music.setVolume(if(challenge==null).24f else .12f,if(challenge==null).24f else .12f)
            if(musicEnabled&&!paused&&!finished&&ready){if(!music.isPlaying)music.start()}
            else if(music.isPlaying)music.pause()
        }
    }
    DisposableEffect(context){
        var ctx=context
        while(ctx is ContextWrapper&&ctx !is Activity)ctx=ctx.baseContext
        val activity=ctx as? Activity
        val orientation=activity?.requestedOrientation
        activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val lifecycle=(activity as? LifecycleOwner)?.lifecycle
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE){clear();paused=true}}
        lifecycle?.addObserver(observer)
        onDispose{lifecycle?.removeObserver(observer);if(orientation!=null)activity?.requestedOrientation=orientation}
    }
    BackHandler{
        when{
            e.finished&&!saving->onMenu()
            challenge!=null->change{e.dismissChallenge()}
            else->{clear();paused=true;exit=true}
        }
    }
    LaunchedEffect(e,ready,paused,challenge,finished){
        if(!ready||paused||challenge!=null||finished)return@LaunchedEffect
        var last=withFrameNanos{it}
        var carry=0f
        while(!paused&&e.pending==null&&!e.finished){
            val now=withFrameNanos{it}
            carry+=((now-last)/1_000_000_000f).coerceIn(0f,.1f);last=now
            var updated=false
            while(carry>=1f/30f&&!e.finished&&e.pending==null){
                e.step(1f/30f,drive);carry-=1f/30f;updated=true
            }
            if(updated){drain();revision++}
        }
    }
    LaunchedEffect(finished){
        if(!finished)return@LaunchedEffect
        clear()
        // Let a final enemy collapse before cutting to the intermission.
        repeat(24){e.advanceDeathAnimations(1f/30f);revision++;delay(33)}
        resultVisible=true
    }
    LaunchedEffect(finished){
        if(!finished)return@LaunchedEffect
        clear()
        saving=true
        try{
            val token=session.getAccessToken()
            reward=if(e.won&&!token.isNullOrBlank()&&e.elapsed>=10f)
                store.recordRunRemote(ApiClient.api,token,GameProgressStore.PELAO_DOOM,e.score,
                    ceil(e.elapsed.toDouble()).toInt(),eventId=runId)
            else GameRunResult(store.recordLocalHighScore(GameProgressStore.PELAO_DOOM,e.score),0,0,false)
            reward?.takeIf{it.syncedWithBackend}?.let{rewards.show("Sector recuperado",it.coinsAwarded,it.xpAwarded)}
        }catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){reward=GameRunResult(store.recordLocalHighScore(GameProgressStore.PELAO_DOOM,e.score),0,0,false)}
        finally{saving=false}
    }
    if(!ready){
        Box(Modifier.fillMaxSize().background(Dark),contentAlignment=Alignment.Center){Text("Gira el teléfono para jugar",color=White)}
        return
    }
    if(resultVisible){
        DoomIntermission(e,reward,saving,onMenu,onRetry)
        return
    }
    Column(Modifier.fillMaxSize().background(Dark).padding(horizontal=5.dp,vertical=3.dp)){
        Row(Modifier.fillMaxWidth().height(34.dp),verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick={if(finished&&!saving)onMenu()else if(!finished){clear();paused=true;exit=true}},enabled=!saving,contentPadding=PaddingValues(horizontal=6.dp)){Text("SALIR",color=White,fontSize=11.sp)}
            Text("SECTOR 01  /  "+e.kills+"/"+e.enemies.size+" ROBOTS",color=Gold,fontWeight=FontWeight.Bold,fontSize=11.sp,modifier=Modifier.weight(1f))
            TextButton(onClick={audio?.let{it.setEffectsEnabled(!it.enabled)}},contentPadding=PaddingValues(horizontal=5.dp)){
                Text(if(audio?.enabled==true)"SFX ON" else "SFX OFF",color=White,fontSize=10.sp)
            }
            if(music!=null)TextButton(onClick={musicEnabled=!musicEnabled;musicPrefs.edit().putBoolean("doom_music_enabled",musicEnabled).apply()},contentPadding=PaddingValues(horizontal=5.dp)){
                Text(if(musicEnabled)"MÚSICA ON" else "MÚSICA OFF",color=White,fontSize=10.sp)
            }
            TextButton(onClick={clear();paused=!paused},enabled=!finished&&challenge==null,contentPadding=PaddingValues(horizontal=6.dp)){Text(if(paused)"SEGUIR" else "PAUSA",color=Gold,fontSize=11.sp)}
        }
        Box(Modifier.weight(1f).fillMaxWidth().border(2.dp,Color(0xFF69645B))){
            AndroidView(factory={DoomView(it).apply{engine=e}},update={view->view.engine=e;if(tick>=0)view.invalidate()},modifier=Modifier.fillMaxSize())
            if(e.messageTime>0f&&challenge==null&&!finished){
                Text(e.message,Modifier.align(Alignment.TopCenter).padding(top=6.dp).background(Dark.copy(alpha=.84f),RoundedCornerShape(3.dp)).padding(horizontal=9.dp,vertical=4.dp),
                    color=Gold,fontSize=11.sp,textAlign=TextAlign.Center)
            }
            if(!paused&&!finished&&challenge==null){
                DoomTouchControls(
                    modifier=Modifier.fillMaxSize(),
                    onInput={drive=it},
                    onLook={delta->if(!paused&&!e.finished&&e.pending==null){e.look(delta);revision++}},
                    onTap={horizontal,vertical->if(!paused&&!e.finished&&e.pending==null&&e.touchInteract(horizontal,vertical)){clear();drain();revision++}},
                    onUse={clear();change{e.interact()}},
                    onWeapon={change{e.cycleWeapon()}}
                )
                e.targetHint()?.let{hint->
                    Text(hint,Modifier.align(Alignment.Center).offset(y=19.dp).background(Dark.copy(alpha=.83f),RoundedCornerShape(3.dp)).padding(horizontal=7.dp,vertical=3.dp),color=Gold,fontSize=10.sp,fontWeight=FontWeight.Black)
                }
                DoomMap(e,tick,Modifier.align(Alignment.TopEnd).padding(5.dp).size(72.dp,54.dp))
            }
            if(paused&&!exit&&!finished&&challenge==null){
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.75f)),contentAlignment=Alignment.Center){
                    Button(onClick={paused=false}){Text("CONTINUAR")}
                }
            }

        }
        DoomStatusBar(e,tick,!finished&&!paused&&challenge==null,
            onSelect={w->change{e.selectWeapon(w)}},onReload={change{e.reload()}})

    }
    if(challenge!=null&&!finished)AlertDialog(onDismissRequest={change{e.dismissChallenge()}},containerColor=Dark,title={Text("TERMINAL DE CONTROL",color=Gold,fontWeight=FontWeight.Black)},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(challenge.prompt,color=White,fontWeight=FontWeight.Bold)
            if(e.pending?.kind==1){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    listOf(RoadSign.YIELD,RoadSign.STOP,RoadSign.MAX_50).forEachIndexed{i,sign->
                        Button(onClick={change{e.answer(i)}},modifier=Modifier.weight(1f),contentPadding=PaddingValues(4.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Metal)){
                            Column(horizontalAlignment=Alignment.CenterHorizontally){RoadSignArt(sign,Modifier.size(42.dp));Text(challenge.answers[i],fontSize=9.sp,textAlign=TextAlign.Center,color=White)}
                        }
                    }
                }
            }else challenge.answers.forEachIndexed{i,label->Button(onClick={change{e.answer(i)}},modifier=Modifier.fillMaxWidth()){Text(label)}}
        }},confirmButton={},dismissButton={TextButton(onClick={change{e.dismissChallenge()}}){Text("VOLVER",color=White)}})
    if(exit)AlertDialog(onDismissRequest={exit=false;paused=false},title={Text("¿Abandonar operación?")},text={Text("Se perderá el avance de este intento.")},
        confirmButton={TextButton(onClick=onMenu){Text("SALIR")}},dismissButton={TextButton(onClick={exit=false;paused=false}){Text("SEGUIR")}})
}

// A single gesture surface owns all pointers. Each thumb has an independent role.
private enum class TouchRole{ MOVE,LOOK,FIRE,USE,WEAPON }
private data class TouchContact(val role:TouchRole,val origin:Offset,var last:Offset,var travel:Float=0f)
private data class TouchLayout(val move:Offset,val gun:Offset,val use:Offset,val weapon:Offset,val radius:Float,val w:Float,val h:Float){
    fun role(p:Offset):TouchRole=when{
        (p-gun).getDistance()<radius*.91f->TouchRole.FIRE
        (p-use).getDistance()<radius*.55f->TouchRole.USE
        (p-weapon).getDistance()<radius*.54f->TouchRole.WEAPON
        p.x<w*.31f&&p.y>h*.54f->TouchRole.MOVE
        else->TouchRole.LOOK
    }
}
private fun touchLayout(w:Float,h:Float):TouchLayout{
    val r=min(h*.19f,w*.079f)
    return TouchLayout(Offset(r*1.35f,h-r*1.20f),Offset(w-r*1.04f,h-r*1.15f),
        Offset(w-r*1.05f,h-r*2.62f),Offset(w-r*2.45f,h-r*.67f),r,w,h)
}
@Composable private fun DoomTouchControls(modifier:Modifier,onInput:(DoomInput)->Unit,onLook:(Float)->Unit,onTap:(Float,Float)->Unit,onUse:()->Unit,onWeapon:()->Unit){
    val input by rememberUpdatedState(onInput);val look by rememberUpdatedState(onLook);val tap by rememberUpdatedState(onTap)
    val use by rememberUpdatedState(onUse);val weapon by rememberUpdatedState(onWeapon)
    var stick by remember{mutableStateOf(Offset.Zero)}
    var anchor by remember{mutableStateOf<Offset?>(null)}
    var firing by remember{mutableStateOf(false)}
    val labelPaint=remember{android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{
        color=android.graphics.Color.WHITE;textAlign=android.graphics.Paint.Align.CENTER;typeface=android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.BOLD)
    }}
    Canvas(modifier.pointerInput(Unit){
        val contacts=mutableMapOf<PointerId,TouchContact>()
        fun publish(l:TouchLayout){
            val movement=contacts.values.firstOrNull{it.role==TouchRole.MOVE}
            val v=movement?.let{(it.last-it.origin)/(l.radius*.66f)}?:Offset.Zero
            val d=v.getDistance()
            stick=if(d<.055f)Offset.Zero else v/d.coerceAtLeast(1f)
            anchor=movement?.origin
            firing=contacts.values.any{it.role==TouchRole.FIRE}
            input(DoomInput(-stick.y,stick.x,0f,firing))
        }
        try{
            awaitEachGesture{
                val first=awaitFirstDown(requireUnconsumed=false)
                val l=touchLayout(size.width.toFloat(),size.height.toFloat())
                fun down(id:PointerId,p:Offset){
                    var role=l.role(p)
                    if(role==TouchRole.MOVE&&contacts.values.any{it.role==TouchRole.MOVE})role=TouchRole.LOOK
                    contacts[id]=TouchContact(role,p,p)
                }
                down(first.id,first.position);first.consume();publish(l)
                do{
                    val event=awaitPointerEvent()
                    for(c in event.changes){
                        if(c.pressed&&!c.previousPressed)down(c.id,c.position)
                        val t=contacts[c.id]?:continue
                        val delta=c.position-t.last
                        t.travel+=delta.getDistance();t.last=c.position
                        if(c.pressed&&t.role in setOf(TouchRole.LOOK,TouchRole.FIRE)&&delta.x!=0f)look(delta.x/l.w*3.55f)
                        if(!c.pressed){
                            contacts.remove(c.id)
                            if(t.travel<l.radius*.16f)when(t.role){
                                TouchRole.LOOK->tap((c.position.x/l.w*2f-1f).coerceIn(-1f,1f),(c.position.y-l.h*.5f)/l.w)
                                TouchRole.MOVE->if((t.origin-l.move).getDistance()>l.radius*.95f)tap((c.position.x/l.w*2f-1f).coerceIn(-1f,1f),(c.position.y-l.h*.5f)/l.w)
                                TouchRole.USE->use()
                                TouchRole.WEAPON->weapon()
                                else->Unit
                            }
                        }
                        c.consume()
                    }
                    publish(l)
                }while(contacts.isNotEmpty())
            }
        }finally{contacts.clear();stick=Offset.Zero;anchor=null;firing=false;input(DoomInput())}
    }){
        val l=touchLayout(size.width,size.height)
        val r=l.radius
        val origin=anchor?:l.move
        drawCircle(Color.Black.copy(alpha=.20f),r*.87f,origin)
        drawCircle(White.copy(alpha=.24f),r*.87f,origin,style=Stroke(1.5.dp.toPx()))
        drawCircle(White.copy(alpha=.36f),r*.29f,origin+stick*r*.60f)
        fun pad(p:Offset,radius:Float,label:String,active:Boolean=false){
            val corner=androidx.compose.ui.geometry.CornerRadius(radius*.23f)
            val pos=p-Offset(radius*.83f,radius*.72f)
            val box=Size(radius*1.66f,radius*1.44f)
            drawRoundRect(if(active)Red.copy(alpha=.74f) else Color.Black.copy(alpha=.28f),pos,box,corner)
            drawRoundRect((if(active)Gold else White).copy(alpha=.38f),pos,box,corner,style=Stroke(1.dp.toPx()))
            labelPaint.textSize=radius*.42f
            drawIntoCanvas{it.nativeCanvas.drawText(label,p.x,p.y-(labelPaint.ascent()+labelPaint.descent())*.5f,labelPaint)}
        }
        pad(l.gun,r*.86f,"FIRE",firing);pad(l.use,r*.49f,"USAR");pad(l.weapon,r*.46f,"ARMA")
    }
}
// Pixel display glyphs, drawn rather than relying on a device font.
private val hudDigits=arrayOf(
    arrayOf("111","101","101","101","111"),arrayOf("010","110","010","010","111"),
    arrayOf("111","001","111","100","111"),arrayOf("111","001","111","001","111"),
    arrayOf("101","101","111","001","001"),arrayOf("111","100","111","001","111"),
    arrayOf("111","100","111","101","111"),arrayOf("111","001","010","010","010"),
    arrayOf("111","101","111","101","111"),arrayOf("111","101","111","001","111"))
@Composable private fun PixelNumber(value:Int,color:Color,modifier:Modifier=Modifier){
    val digits=value.coerceAtLeast(0).toString()
    Canvas(modifier){
        val unit=min(size.height/5f,size.width/(digits.length*4-1))
        val origin=Offset((size.width-unit*(digits.length*4-1))/2f,(size.height-unit*5f)/2f)
        for((i,ch) in digits.withIndex())for(y in 0..4)for(x in 0..2)if(hudDigits[ch-'0'][y][x]=='1'){
            val p=origin+Offset((i*4+x)*unit,y*unit)
            drawRect(Color.Black.copy(alpha=.85f),p+Offset(unit*.25f,unit*.25f),Size(unit,unit))
            drawRect(color,p,Size(unit,unit))
        }
    }
}
@Composable private fun HudMeter(label:String,value:Int,color:Color,modifier:Modifier=Modifier,sub:String?=null,onClick:(()->Unit)?=null){
    Column(modifier.then(if(onClick!=null)Modifier.clickable(onClick=onClick)else Modifier).background(Color(0xFF242524)).border(1.dp,Color(0xFF676052)).padding(horizontal=4.dp,vertical=3.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        PixelNumber(value,color,Modifier.fillMaxWidth().height(23.dp))
        Text(label,color=Color(0xFFD2C3A3),fontWeight=FontWeight.Black,fontSize=9.sp,maxLines=1,lineHeight=10.sp)
        if(sub!=null)Text(sub,color=Gold,fontSize=8.sp,maxLines=1,lineHeight=9.sp)
    }
}
@Composable private fun DoomStatusBar(e:DoomEngine,revision:Int,enabled:Boolean,onSelect:(DoomWeapon)->Unit,onReload:()->Unit){
    Box(Modifier.fillMaxWidth().height(61.dp).background(Color(0xFF49433A)).border(2.dp,Color(0xFF9B8C72))){
        Canvas(Modifier.fillMaxSize()){
            for(y in 0..17)drawLine(if(y%2==0)Color(0xFF5D5548)else Color(0xFF3B3730),Offset(0f,size.height*y/17f),Offset(size.width,size.height*y/17f),1.dp.toPx())
            drawLine(Color(0xFFBAA27D),Offset.Zero,Offset(size.width,0f),2.dp.toPx())
        }
        Row(Modifier.fillMaxSize().padding(4.dp),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.CenterVertically){
            HudMeter("AMMO",e.ammunition,Color(0xFFE8B747),Modifier.weight(1.12f).fillMaxHeight(),
                sub=if(e.weaponPhase==DoomWeaponPhase.RELOADING)"RECARGA" else "CARG. "+e.magazineAmmo,
                onClick=if(enabled)onReload else null)
            HudMeter("HEALTH",e.hp,Color(0xFFE94D3A),Modifier.weight(1f).fillMaxHeight())
            Column(Modifier.weight(1.18f).fillMaxHeight().background(Color(0xFF292721)).border(1.dp,Color(0xFF716652)),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                Text("ARMS",color=Color(0xFFD2C3A3),fontSize=9.sp,fontWeight=FontWeight.Black)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                    DoomWeapon.entries.forEachIndexed{i,w->
                        Box(Modifier.weight(1f).height(23.dp).padding(1.dp).background(if(e.weapon==w)Color(0xFF83602D)else Color(0xFF1A1A18)).clickable(enabled=enabled&&e.owns(w)){onSelect(w)},contentAlignment=Alignment.Center){
                            PixelNumber(i+2,if(e.weapon==w)Gold else if(e.owns(w))Color(0xFFD3C2A0)else Color(0xFF54524B),Modifier.size(12.dp,17.dp))
                        }
                    }
                }
                Text(e.weapon.title,color=Gold,fontSize=8.sp,maxLines=1)
            }
            DoomFace(e.faceState,e.hp<=0,Modifier.weight(1.07f).fillMaxHeight())
            HudMeter("ARMOR",e.armor,Color(0xFF9FB7A0),Modifier.weight(1f).fillMaxHeight())
            Column(Modifier.weight(1.06f).fillMaxHeight().background(Color(0xFF242524)).border(1.dp,Color(0xFF676052)),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                PixelNumber(e.coins,Gold,Modifier.fillMaxWidth().height(17.dp))
                Text("FICHAS",color=Color(0xFFD2C3A3),fontSize=8.sp,fontWeight=FontWeight.Black)
                Text(if(e.hasKey)"◆ KEY" else "◇ KEY",color=if(e.hasKey)Gold else Color(0xFF77736A),fontSize=10.sp,fontWeight=FontWeight.Bold)
            }
            Column(Modifier.weight(.95f).fillMaxHeight().background(Color(0xFF242524)).border(1.dp,Color(0xFF676052)).padding(3.dp),verticalArrangement=Arrangement.Center){
                DoomWeapon.entries.forEachIndexed{i,w->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                        Text(listOf("BUL","SHL","CEL")[i],color=Color(0xFFAFA38A),fontSize=8.sp,lineHeight=10.sp)
                        Text(e.ammoFor(w).toString(),color=if(e.weapon==w)Gold else White,fontSize=9.sp,fontWeight=FontWeight.Bold,lineHeight=10.sp)
                    }
                }
            }
        }
        if(revision<0)Spacer(Modifier.size(0.dp))
    }
}
@Composable private fun DoomMap(e:DoomEngine,revision:Int,modifier:Modifier){
    Canvas(modifier.background(Dark.copy(alpha=.84f)).padding(3.dp)){
        if(revision<0)return@Canvas
        val sx=size.width/e.width;val sy=size.height/e.height
        for(y in 0 until e.height)for(x in 0 until e.width){
            val c=e.cell(x,y)
            if(c!=0)drawRect(if(c==5)Gold.copy(alpha=.7f)else if(c==6)Color(0xFF64D5A1)else Color(0xFF77736B),Offset(x*sx,y*sy),Size(sx,sy))
        }
        val p=Offset(e.x*sx,e.y*sy)
        drawCircle(Red,3.dp.toPx(),p);drawLine(White,p,p+Offset(cos(e.angle)*sx*1.7f,sin(e.angle)*sy*1.7f),1.5.dp.toPx())
    }
}
@Composable private fun DoomFace(state:DoomFaceState,dead:Boolean,modifier:Modifier){
    val context=LocalContext.current
    val bitmaps=remember(context){
        // PNG and WebP have the same resource ID. Missing portraits never break the build.
        val fallback=context.resources.getIdentifier("doom_face_02_normal_frente","drawable",context.packageName)
        val default=if(fallback!=0)fallback else R.drawable.mascot_mister_pelao
        val cache=mutableMapOf<Int,androidx.compose.ui.graphics.ImageBitmap>()
        DoomFaceState.entries.associateWith{face->
            val id=context.resources.getIdentifier("doom_face_"+face.asset,"drawable",context.packageName).let{if(it!=0)it else default}
            cache.getOrPut(id){
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeResource(context.resources,id,bounds)
                var sample=1
                while(max(bounds.outWidth,bounds.outHeight)/sample>256)sample*=2
                val image=BitmapFactory.decodeResource(context.resources,id,BitmapFactory.Options().apply{inSampleSize=sample;inScaled=false})
                (image?:android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888)).asImageBitmap()
            }
        }
    }
    Box(modifier.background(Color(0xFF191817)).border(2.dp,Color(0xFF8D7F64)).padding(2.dp)){
        Image(bitmap=bitmaps.getValue(state),contentDescription="Don Pelao",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
        if(dead)Box(Modifier.fillMaxSize().background(Color(0x66000000)))
    }
}

@Composable private fun DoomIntermission(e:DoomEngine,reward:GameRunResult?,saving:Boolean,onMenu:()->Unit,onRetry:()->Unit){
    val audio=LocalGameAudio.current
    var stage by remember{mutableIntStateOf(0)}
    var tally by remember{mutableFloatStateOf(0f)}
    val metrics=listOf("ENEMIGOS" to e.kills*100/e.enemies.size.coerceAtLeast(1),
        "SECRETOS" to e.secrets*100,
        "OBJETOS" to (e.loot.count{it.taken}*100/e.loot.size.coerceAtLeast(1)),
        "TRÁNSITO" to if(e.questionsAnswered>0)e.questionsCorrect*100/e.questionsAnswered else 0)
    LaunchedEffect(Unit){
        delay(180)
        for(i in metrics.indices){
            stage=i;tally=0f
            repeat(12){step->tally=(step+1)/12f;delay(24)}
            audio?.play("doom_reload");delay(100)
        }
        stage=metrics.size;tally=1f
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF171513))){
        val short=maxHeight<310.dp
        Canvas(Modifier.fillMaxSize()){
            val cell=size.width/100f
            for(y in 0..(size.height/cell).toInt())for(x in 0..99){
                val n=((x*79+y*313+x*y*17) xor (x*13+y*7)) and 15
                val c=if(y%7==0||((x+if((y/7)%2==0)0 else 7)%14==0))Color(0xFF24201D)else Color(0xFF40362D)
                drawRect(c.copy(alpha=.64f+n*.018f),Offset(x*cell,y*cell),Size(cell+.5f,cell+.5f))
            }
            drawRect(Color(0x99100D0B))
            for(y in listOf(5.dp.toPx(),size.height-6.dp.toPx())){
                drawLine(Color(0xFF6C5942),Offset(0f,y),Offset(size.width,y),2.dp.toPx())
                drawLine(Color(0xFF151210),Offset(0f,y+3),Offset(size.width,y+3),2.dp.toPx())
            }
        }
        Column(Modifier.fillMaxSize().padding(horizontal=20.dp,vertical=if(short)8.dp else 14.dp),horizontalAlignment=Alignment.CenterHorizontally){
            Text("E1M1  /  ESTACIONAMIENTO INFERNAL",color=Gold,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,fontSize=if(short)12.sp else 15.sp)
            Text(if(e.won)"¡SECTOR COMPLETADO!" else "MISIÓN FALLIDA",color=Color(0xFFFF4B30),fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Black,fontSize=if(short)23.sp else 30.sp,lineHeight=if(short)25.sp else 32.sp)
            Row(Modifier.weight(1f).fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1.25f),verticalArrangement=Arrangement.spacedBy(if(short)3.dp else 6.dp)){
                    for((i,m) in metrics.withIndex()){
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                            Text(m.first,color=Color(0xFFE7BD77),fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,fontSize=if(short)13.sp else 17.sp)
                            val v=when{stage>i->m.second;stage==i->(m.second*tally).toInt();else->0}
                            Row(verticalAlignment=Alignment.CenterVertically){
                                PixelNumber(v,Color(0xFFE95636),Modifier.width(45.dp).height(if(short)15.dp else 22.dp))
                                Text("%",color=Red,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Black,fontSize=if(short)13.sp else 17.sp)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).border(1.dp,Color(0xFF826D50)).background(Color(0xDD211B17)).padding(if(short)5.dp else 9.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)){
                    Text("TIEMPO",color=Gold,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,fontSize=10.sp)
                    val seconds=e.elapsed.toInt()
                    Text("%02d:%02d".format(seconds/60,seconds%60),color=White,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Black,fontSize=if(short)20.sp else 27.sp)
                    Text("PUNTOS",color=Gold,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,fontSize=10.sp)
                    PixelNumber(e.score,Color(0xFFEDB951),Modifier.fillMaxWidth().height(if(short)17.dp else 24.dp))
                    Text(if(saving)"GUARDANDO..." else reward?.takeIf{it.syncedWithBackend}?.let{"+${it.xpAwarded} XP  +${it.coinsAwarded} MONEDAS"}?:"RÉCORD LOCAL",color=Gold,fontFamily=FontFamily.Monospace,fontSize=9.sp,textAlign=TextAlign.Center)
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(15.dp)){
                RetroResultButton("REINTENTAR",!saving,onRetry)
                RetroResultButton("MENÚ",!saving,onMenu)
            }
        }
    }
}
@Composable private fun RetroResultButton(label:String,enabled:Boolean,action:()->Unit){
    Box(Modifier.width(132.dp).height(34.dp).background(Color(0xFF3B2F25)).border(2.dp,Color(0xFF967951)).clickable(enabled=enabled,onClick=action),contentAlignment=Alignment.Center){
        Text("› "+label,color=if(enabled)Gold else Color.Gray,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Black,fontSize=13.sp)
    }
}
