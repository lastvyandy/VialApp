package cl.vialapp.app.ui.simulator

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.*
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.ui.mascot.MascotReactionCard
import cl.vialapp.app.ui.mascot.MascotReaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SimulatorEntry(onClick: () -> Unit) {
    Card(onClick=onClick, modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.cardColors(containerColor=Color(0xFF244D45),contentColor=Color.White)) {
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_award_road),contentDescription=null,
                modifier=Modifier.size(35.dp),tint=Color(0xFFB7E8CC))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("Simulador vial",fontSize=19.sp,fontWeight=FontWeight.Bold)
                Text("Toma decisiones al volante",fontSize=13.sp,color=Color(0xFFD4F2E3))
            }
            Text("›",fontSize=27.sp,color=Color(0xFFB7E8CC))
        }
    }
}

@Composable
fun SimulatorScreen(userKey: String, mascotId: String?, onBack: () -> Unit) {
    val context=LocalContext.current
    val manager=remember(context) { SessionManager(context) }
    val store=remember(context,userKey) { GameProgressStore(context,userKey) }
    var catalog by remember(userKey) { mutableStateOf<SimulatorCatalog?>(null) }
    var scenario by remember(userKey) { mutableStateOf<DrivingScenario?>(null) }
    var loading by remember(userKey) { mutableStateOf(true) }
    var refresh by remember(userKey) { mutableIntStateOf(0) }
    val c=MaterialTheme.colorScheme
    LaunchedEffect(userKey,refresh) {
        loading=true
        try {
            val token=manager.getAccessToken()
            val response=token?.let { ApiClient.api.getSimulator("Bearer " + it) }
            if (response?.isSuccessful==true) catalog=response.body()
        } catch (e:CancellationException) { throw e }
        catch (_:Exception) { }
        finally { loading=false }
    }
    val selected=scenario
    if (selected!=null) {
        key(selected.id) {
            ScenarioPlay(selected,mascotId,manager,store,onExit={ scenario=null },
                onSaved={ catalog=catalog?.let { it.copy(completedIds=(it.completedIds+selected.id).distinct()) } })
        }
    } else {
        BackHandler(onBack=onBack)
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item {
                TextButton(onClick=onBack) { Text("‹ Volver") }
                Text("ACADEMIA VIAL",color=c.primary,fontSize=12.sp,fontWeight=FontWeight.Bold)
                Text("Simulador vial",color=c.onSurface,fontSize=30.sp,fontWeight=FontWeight.ExtraBold)
                Text("Elige una situación, observa el entorno y decide cómo continuar.",color=c.onSurfaceVariant)
            }
            val data=catalog
            if (data==null) item {
                if (loading) Box(Modifier.fillMaxWidth().height(100.dp),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
                else {
                    Text("No pudimos cargar los escenarios. Verifica tu conexión.",color=c.onSurfaceVariant)
                    OutlinedButton(onClick={refresh++}) { Text("Reintentar") }
                }
            } else {
                item { Text(data.completedIds.size.toString()+" de "+data.scenarios.size+" escenarios completados",color=c.primary,fontWeight=FontWeight.Bold) }
                items(data.scenarios,key={it.id}) { item ->
                    Card(onClick={scenario=item},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),
                        border=BorderStroke(1.dp,c.outlineVariant)) {
                        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            SimulatorBoard(item.scene,0f)
                            Text(item.category.uppercase(),color=c.primary,fontSize=11.sp,fontWeight=FontWeight.Bold)
                            Text(item.title,color=c.onSurface,fontSize=20.sp,fontWeight=FontWeight.Bold)
                            Text(item.description,color=c.onSurfaceVariant,fontSize=14.sp)
                            Text(if (item.id in data.completedIds) "Completado · Volver a practicar" else "Comenzar · Hasta 20 XP",
                                color=c.primary,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
                        }
                    }
                }
            }
            item { Text("Escenarios educativos simplificados. No sustituyen las clases prácticas de conducción.",color=c.onSurfaceVariant,fontSize=12.sp) }
        }
    }
}

@Composable
private fun ScenarioPlay(scenario:DrivingScenario,mascotId:String?,manager:SessionManager,store:GameProgressStore,
                         onExit:()->Unit,onSaved:()->Unit) {
    val c=MaterialTheme.colorScheme
    var play by remember { mutableStateOf(ScenarioDecisionState.start(scenario.steps.size)) }
    val stage=play.stage
    val attempts=play.attempts
    val moving=play.transitioning
    val finished=play.finished
    val step=scenario.steps.getOrNull(stage)
    val rejected=attempts.getOrNull(stage).orEmpty()
    val warning=step!=null && rejected.isNotEmpty() && !moving
    var exitDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<SimulatorCompletionResponse?>(null) }
    var saveError by remember { mutableStateOf(false) }
    val motion=remember { Animatable(0f) }
    val errors=attempts.mapIndexed { i, a -> a.count { it!=scenario.steps[i].correct } }.sum()

    // The state owns the transition, not a reused answer button. Even if the
    // animation is interrupted, the next decision cannot inherit its lock.
    LaunchedEffect(stage,moving) {
        if (!moving) return@LaunchedEffect
        try {
            withTimeoutOrNull(2500L) {
                motion.animateTo((stage+1).toFloat(),tween(durationMillis=if(stage==1) 1450 else 850))
            }
            motion.snapTo((stage+1).toFloat())
        } finally {
            play=play.advance(stage)
        }
    }
    fun leave() {
        if (saving) return
        if (result!=null || attempts.all { it.isEmpty() }) onExit() else exitDialog=true
    }
    BackHandler { leave() }
    if (exitDialog) AlertDialog(onDismissRequest={exitDialog=false},
        title={Text("¿Salir del escenario?")},text={Text("Las decisiones todavía no guardadas se perderán.")},
        confirmButton={TextButton(onClick=onExit){Text("Salir")}},
        dismissButton={TextButton(onClick={exitDialog=false}){Text("Continuar")}})
    LaunchedEffect(finished,retry) {
        if (!finished || result!=null) return@LaunchedEffect
        saving=true
        saveError=false
        try {
            val token=manager.getAccessToken()
            val response=token?.let { ApiClient.api.completeSimulator("Bearer " + it,scenario.id,SimulatorCompletionRequest(attempts)) }
            val body=response?.body()
            if (response?.isSuccessful==true && body!=null) {
                result=body
                store.applyRemoteProgress(body.gamification)
                onSaved()
            } else saveError=true
        } catch (e:CancellationException) { throw e }
        catch (_:Exception) { saveError=true }
        finally { saving=false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=::leave,enabled=!saving){Text("‹ Escenarios")}
            Spacer(Modifier.weight(1f))
            Text(stage.coerceAtMost(scenario.steps.size).toString()+"/"+scenario.steps.size,color=c.primary,fontWeight=FontWeight.Bold)
        }
        Text(scenario.title,fontSize=24.sp,fontWeight=FontWeight.ExtraBold,color=c.onSurface)
        LinearProgressIndicator(progress={stage.toFloat()/scenario.steps.size.coerceAtLeast(1)},
            modifier=Modifier.fillMaxWidth().height(6.dp))
        Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) {
            SimulatorBoard(scenario.scene,motion.value,modifier=Modifier.widthIn(max=270.dp),warning=warning)
        }
        Text("Vehículo azul · Tu auto",color=c.onSurfaceVariant,fontSize=12.sp)
        if (finished) {
            Text("¡Escenario completado!",color=c.primary,fontSize=23.sp,fontWeight=FontWeight.Bold)
            Text("Decisiones sin error: "+attempts.count { it.size==1 }+" de "+scenario.steps.size+" · Errores: "+errors,color=c.onSurface)
            if (mascotId!=null) MascotReactionCard(mascotId,
                if (errors==0) MascotReaction.PERFECT else MascotReaction.KEEP_GOING)
            when {
                saving -> CircularProgressIndicator(Modifier.size(24.dp))
                result!=null -> Text(if (result?.duplicate==true) "Ya recibiste la recompensa de este escenario." else "+"+(result?.xpAwarded ?: 0)+" XP confirmados",
                    fontWeight=FontWeight.Bold,color=c.primary)
                saveError -> {
                    Text("No se guardó todavía. Puedes volver a intentar.",color=c.error)
                    Button(onClick={retry++},modifier=Modifier.fillMaxWidth()){Text("Reintentar guardado")}
                }
            }
            OutlinedButton(onClick=::leave,enabled=!saving,modifier=Modifier.fillMaxWidth()){Text("Ver más escenarios")}
        } else if(step!=null) {
            // Give every question its own composition identity and immutable
            // stage number. Do not reuse a LazyColumn item's old callback.
            key(stage) {
                Text(step.prompt,color=c.onSurface,fontSize=20.sp,fontWeight=FontWeight.Bold)
                Text("Errores: "+errors,color=c.onSurfaceVariant,fontSize=12.sp)
                if (warning || moving) {
                    Surface(color=if(warning)c.errorContainer else c.primaryContainer,shape=RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(if(warning)"Decisión insegura: prueba otra opción" else "¡Correcto! Preparando la siguiente decisión…",
                                fontWeight=FontWeight.Bold,color=if(warning)c.onErrorContainer else c.onPrimaryContainer)
                            Text(step.explanation,color=if(warning)c.onErrorContainer else c.onPrimaryContainer,fontSize=14.sp)
                        }
                    }
                }
                step.options.forEachIndexed { index, option ->
                    OutlinedButton(onClick={ play=play.choose(stage,index,step.correct) },
                        enabled=!moving && index !in rejected,modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(14.dp)) {
                        Text(option,modifier=Modifier.fillMaxWidth(),fontSize=15.sp)
                    }
                }
            }
        }
    }
}
