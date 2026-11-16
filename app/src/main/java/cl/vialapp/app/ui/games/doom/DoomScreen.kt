package cl.vialapp.app.ui.games.doom

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
    fun clear(){drive=DoomInput()}
    fun drain(){
        for(event in e.drainEvents()){
            audio?.play(when(event){
                DoomEvent.SHOT->"doom_shot";DoomEvent.BLAST->"doom_blast";DoomEvent.PLASMA->"doom_plasma"
                DoomEvent.HIT->"hit";DoomEvent.HURT->"heavy";DoomEvent.COIN->"coin";DoomEvent.LOOT->"reward"
                DoomEvent.DOOR->"doom_door";DoomEvent.CORRECT->"correct";DoomEvent.WRONG->"wrong"
                DoomEvent.VICTORY->"victory";DoomEvent.DEFEAT->"wrong";DoomEvent.SECRET->"xp"
            })
        }
    }
    fun change(action:()->Unit){action();drain();revision++}
    val tick=revision
    val challenge=e.pending?.question
    val finished=e.finished
    val configuration=LocalConfiguration.current
    val ready=configuration.screenWidthDp>=configuration.screenHeightDp
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
    Column(Modifier.fillMaxSize().background(Dark).padding(horizontal=5.dp,vertical=3.dp)){
        Row(Modifier.fillMaxWidth().height(34.dp),verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick={if(finished&&!saving)onMenu()else if(!finished){clear();paused=true;exit=true}},enabled=!saving,contentPadding=PaddingValues(horizontal=6.dp)){Text("SALIR",color=White,fontSize=11.sp)}
            Text("SECTOR 01  /  "+e.kills+"/"+e.enemies.size+" ROBOTS",color=Gold,fontWeight=FontWeight.Bold,fontSize=11.sp,modifier=Modifier.weight(1f))
            TextButton(onClick={audio?.let{it.setEffectsEnabled(!it.enabled)}},contentPadding=PaddingValues(horizontal=5.dp)){
                Text(if(audio?.enabled==true)"SFX ON" else "SFX OFF",color=White,fontSize=10.sp)
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
                    onTap={horizontal->if(!paused&&!e.finished&&e.pending==null&&e.touchInteract(horizontal)){clear();drain();revision++}},
                    onUse={clear();change{e.interact()}},
                    onWeapon={change{e.cycleWeapon()}}
                )
                e.targetHint()?.let{hint->
                    Text(hint,Modifier.align(Alignment.Center).offset(y=19.dp).background(Dark.copy(alpha=.83f),RoundedCornerShape(3.dp)).padding(horizontal=7.dp,vertical=3.dp),color=Gold,fontSize=10.sp,fontWeight=FontWeight.Black)
                }
                DoomMap(e,tick,Modifier.align(Alignment.TopEnd).padding(7.dp).size(87.dp,67.dp))
            }
            if(paused&&!exit&&!finished&&challenge==null){
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.75f)),contentAlignment=Alignment.Center){
                    Button(onClick={paused=false}){Text("CONTINUAR")}
                }
            }
            if(finished){
                Box(Modifier.fillMaxSize().background(Dark.copy(alpha=.93f)),contentAlignment=Alignment.Center){
                    Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){
                        Text(if(e.won)"SECTOR RECUPERADO" else "MISIÓN FALLIDA",fontSize=25.sp,fontWeight=FontWeight.Black,color=if(e.won)Gold else Red)
                        Text("Puntos "+e.score+"  ·  Robots "+e.kills+"  ·  Secretos "+e.secrets+"/1",color=White)
                        if(saving)Text("Guardando resultado…",color=White,fontSize=12.sp)
                        else Text(reward?.takeIf{it.syncedWithBackend}?.let{"+${it.xpAwarded} XP · +${it.coinsAwarded} monedas"}?:"Récord local",color=Gold)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            Button(onClick=onRetry,enabled=!saving){Text("REINTENTAR")}
                            OutlinedButton(onClick=onMenu,enabled=!saving){Text("MENÚ",color=White)}
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(52.dp).background(Metal).border(2.dp,Color(0xFF716B61)).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Meter("VIDA",e.hp.toString()+"%",Red)
            Meter("ARMADURA",e.armor.toString()+"%",Color(0xFF83C8C9))
            DoomFace(e.hp,e.hurtFlash>0f,e.shotFlash>0f,Modifier.size(64.dp,49.dp))
            TextButton(onClick={change{e.cycleWeapon()}},enabled=!finished&&!paused&&challenge==null,contentPadding=PaddingValues(0.dp)){Meter(e.weapon.title,e.ammunition.toString(),Gold)}
            Meter("FICHAS",e.coins.toString(),Gold)
            Meter("ACCESO",if(e.hasKey)"SI" else "—",Gold)
        }
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
        p.x<w*.42f&&p.y>h*.43f->TouchRole.MOVE
        else->TouchRole.LOOK
    }
}
private fun touchLayout(w:Float,h:Float):TouchLayout{
    val r=min(h*.30f,w*.116f)
    return TouchLayout(Offset(r*1.35f,h-r*1.20f),Offset(w-r*1.04f,h-r*1.15f),
        Offset(w-r*1.05f,h-r*2.62f),Offset(w-r*2.45f,h-r*.67f),r,w,h)
}
@Composable private fun DoomTouchControls(modifier:Modifier,onInput:(DoomInput)->Unit,onLook:(Float)->Unit,onTap:(Float)->Unit,onUse:()->Unit,onWeapon:()->Unit){
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
                                TouchRole.LOOK->tap((c.position.x/l.w*2f-1f).coerceIn(-1f,1f))
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
        drawCircle(Color.Black.copy(alpha=.33f),r*.87f,origin)
        drawCircle(White.copy(alpha=.35f),r*.87f,origin,style=Stroke(1.5.dp.toPx()))
        drawCircle(White.copy(alpha=.48f),r*.29f,origin+stick*r*.60f)
        fun pad(p:Offset,radius:Float,label:String,active:Boolean=false){
            drawCircle(if(active)Red.copy(alpha=.90f) else Color.Black.copy(alpha=.42f),radius,p)
            drawCircle((if(active)Gold else White).copy(alpha=.53f),radius,p,style=Stroke(1.5.dp.toPx()))
            labelPaint.textSize=radius*.39f
            drawIntoCanvas{it.nativeCanvas.drawText(label,p.x,p.y-(labelPaint.ascent()+labelPaint.descent())*.5f,labelPaint)}
        }
        pad(l.gun,r*.86f,"FIRE",firing);pad(l.use,r*.49f,"USAR");pad(l.weapon,r*.46f,"ARMA")
    }
}
@Composable private fun Meter(label:String,value:String,color:Color){
    Column(horizontalAlignment=Alignment.CenterHorizontally){
        Text(value,color=color,fontWeight=FontWeight.Black,fontSize=20.sp,lineHeight=22.sp)
        Text(label,color=White,fontWeight=FontWeight.Bold,fontSize=8.sp,maxLines=1)
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
@Composable private fun DoomFace(hp:Int,hurt:Boolean,fire:Boolean,modifier:Modifier){
    Box(modifier.clip(RoundedCornerShape(5.dp)).background(if(hurt||hp<35)Color(0xFF502C26)else if(fire)Color(0xFF72532C)else Color(0xFF232224)).border(1.dp,Color(0xFF807367),RoundedCornerShape(5.dp))){
        Image(painterResource(R.drawable.mascot_mister_pelao),"Don Pelao",Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alignment=Alignment.TopCenter)
        if(hurt)Box(Modifier.fillMaxSize().background(Color(0x37C01812)))
        if(hp<35)Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(Red))
    }
}
