package cl.vialapp.app.ui.career

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.*
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CareerShortcuts(onCareer:()->Unit,onGarage:()->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        Shortcut("Modo carrera","Avanza por etapas",R.drawable.ic_award_road,Modifier.weight(1f),onCareer)
        Shortcut("Mi garaje","Vehículos y colores",R.drawable.ic_award_wheel,Modifier.weight(1f),onGarage)
    }
}

@Composable
private fun Shortcut(title:String,subtitle:String,drawable:Int,modifier:Modifier,onClick:()->Unit) {
    val c=MaterialTheme.colorScheme
    Card(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(19.dp),
        colors=CardDefaults.cardColors(containerColor=c.surface),border=BorderStroke(1.dp,c.outlineVariant)) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Icon(painterResource(drawable),null,tint=c.primary,modifier=Modifier.size(27.dp))
            Text(title,color=c.onSurface,fontWeight=FontWeight.Bold,fontSize=15.sp)
            Text(subtitle,color=c.onSurfaceVariant,fontSize=12.sp)
        }
    }
}

private data class PendingPurchase(val action:String,val id:String,val name:String,val price:Int)

@Composable
fun CareerScreen(userKey:String, initiallyGarage:Boolean,onBack:()->Unit,onActivity:(String)->Unit) {
    val context=LocalContext.current
    val session=remember(context){SessionManager(context)}
    val scope=rememberCoroutineScope()
    val c=MaterialTheme.colorScheme
    var garage by remember(userKey,initiallyGarage){mutableStateOf(initiallyGarage)}
    var state by remember(userKey){mutableStateOf<CareerState?>(null)}
    var loading by remember(userKey){mutableStateOf(true)}
    var busy by remember(userKey){mutableStateOf(false)}
    var error by remember(userKey){mutableStateOf("")}
    var refresh by remember(userKey){mutableIntStateOf(0)}
    var pending by remember(userKey){mutableStateOf<PendingPurchase?>(null)}
    LaunchedEffect(userKey,refresh) {
        loading=true; error=""
        try {
            val token=session.getAccessToken()
            val response=token?.let { ApiClient.api.getCareer("Bearer "+it) }
            val body=response?.body()
            if(response?.isSuccessful==true && body!=null) state=body
            else error="No pudimos cargar tu carrera. Inténtalo nuevamente."
        } catch(e:CancellationException){throw e}
        catch(_:Exception){error="No pudimos conectar con VialApp."}
        finally{loading=false}
    }
    fun act(action:String,id:String) {
        if(busy||loading)return
        busy=true; error=""
        scope.launch {
            try {
                val token=session.getAccessToken()
                val response=token?.let{ApiClient.api.changeCareer("Bearer "+it,CareerAction(action,id))}
                val body=response?.body()
                if(response?.isSuccessful==true && body!=null) state=body
                else error=if(response?.code()==409) "Revisa tus objetivos o saldo de monedas y actualiza." else "No se pudo guardar el cambio. Reintenta."
            } catch(e:CancellationException){throw e}
            catch(_:Exception){error="Se perdió la conexión. Actualiza antes de continuar."}
            finally{busy=false}
        }
    }
    BackHandler { if(!busy)onBack() }
    pending?.let { purchase ->
        AlertDialog(onDismissRequest={pending=null},title={Text("Comprar "+purchase.name)},
            text={Text("Usarás "+purchase.price+" monedas virtuales. No se realizará ningún pago real.")},
            confirmButton={TextButton(onClick={pending=null;act(purchase.action,purchase.id)}){Text("Comprar")}},
            dismissButton={TextButton(onClick={pending=null}){Text("Cancelar")}})
    }
    LazyColumn(Modifier.fillMaxSize().background(c.background),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick=onBack,enabled=!busy){Text("‹ Volver")}
                Spacer(Modifier.weight(1f))
                TextButton(onClick={refresh++},enabled=!busy&&!loading){Text("Actualizar")}
            }
            Text(if(garage)"Mi garaje" else "Modo carrera",fontSize=30.sp,fontWeight=FontWeight.ExtraBold,color=c.onSurface)
            Text(if(garage)"Personaliza tu vehículo y llévalo a la pista." else "Construye tu carrera mientras aprendes.",color=c.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                if(!garage) Button(onClick={garage=false},modifier=Modifier.weight(1f)){Text("Carrera")}
                else OutlinedButton(onClick={garage=false},modifier=Modifier.weight(1f)){Text("Carrera")}
                if(garage) Button(onClick={garage=true},modifier=Modifier.weight(1f)){Text("Garaje")}
                else OutlinedButton(onClick={garage=true},modifier=Modifier.weight(1f)){Text("Garaje")}
            }
        }
        if(loading || busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if(error.isNotBlank()) item {
            Surface(color=c.errorContainer,shape=RoundedCornerShape(14.dp)) {
                Text(error,Modifier.padding(12.dp),color=c.onErrorContainer)
            }
        }
        val data=state
        if(data!=null) {
            item { Wallet(data) }
            if(!garage) {
                item {
                    Text("Tu recorrido",fontSize=22.sp,fontWeight=FontWeight.Bold,color=c.onSurface)
                    Text("${data.stages.count { it.claimed }} de ${data.stages.size} etapas completadas",color=c.onSurfaceVariant)
                    LinearProgressIndicator(progress={data.stages.count{it.claimed}.toFloat()/data.stages.size.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().height(7.dp))
                }
                items(data.stages,key={it.id}) { stage ->
                    StageCard(stage,!busy&&!loading,onClaim={act("claim",stage.id)},onActivity=onActivity)
                }
                item {
                    Button(onClick={garage=true},modifier=Modifier.fillMaxWidth()){Text("Ir a mi garaje")}
                }
            } else {
                val active=data.vehicles.firstOrNull{it.id==data.equippedVehicle}
                val paint=data.paints.firstOrNull{it.id==data.equippedPaint}
                val color=garageColor(paint?.hex?:"#38A9F0")
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF314967),Color(0xFF192536))))
                        .padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("VEHÍCULO ACTIVO",fontSize=11.sp,color=Color(0xFFB8CFEA),fontWeight=FontWeight.Bold)
                        VehiclePreview(active?.style?:"compact",color,Modifier.fillMaxWidth(.75f))
                        Text(active?.name?:"Compacto",fontSize=22.sp,fontWeight=FontWeight.ExtraBold,color=Color.White)
                        Text((paint?.name?:"Azul Vial")+" · "+zoneName(data.zone),color=Color(0xFFCCDAEC),fontSize=13.sp)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick={onActivity("games/carvier")},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Salir a la pista")}
                    }
                }
                item { Text("Vehículos",fontSize=22.sp,fontWeight=FontWeight.Bold,color=c.onSurface) }
                items(data.vehicles,key={it.id}) { vehicle ->
                    Card(shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=c.surface),border=BorderStroke(1.dp,c.outlineVariant)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            VehiclePreview(vehicle.style,color,Modifier.width(94.dp))
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                Text(vehicle.name,fontWeight=FontWeight.Bold,color=c.onSurface)
                                Text(if(vehicle.owned)"En tu garaje" else if(!vehicle.available)"Completa ${vehicle.requiredStages} etapas" else "${vehicle.price} monedas",color=c.onSurfaceVariant,fontSize=12.sp)
                                val equipped=vehicle.id==data.equippedVehicle
                                OutlinedButton(enabled=!busy&&!loading&&!equipped&&(vehicle.owned||(vehicle.available&&data.coins>=vehicle.price)),
                                    onClick={if(vehicle.owned)act("equip_vehicle",vehicle.id) else pending=PendingPurchase("buy_vehicle",vehicle.id,vehicle.name,vehicle.price)}) {
                                    Text(if(equipped)"Equipado" else if(vehicle.owned)"Equipar" else if(!vehicle.available)"Bloqueado" else "Comprar")
                                }
                            }
                        }
                    }
                }
                item { Text("Pinturas",fontSize=22.sp,fontWeight=FontWeight.Bold,color=c.onSurface) }
                items(data.paints,key={it.id}) { option ->
                    Surface(color=c.surface,shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,c.outlineVariant)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(garageColor(option.hex)))
                            Column(Modifier.weight(1f)) {
                                Text(option.name,color=c.onSurface,fontWeight=FontWeight.Bold)
                                Text(if(option.owned)"Disponible" else "${option.price} monedas",color=c.onSurfaceVariant,fontSize=12.sp)
                            }
                            val equipped=option.id==data.equippedPaint
                            OutlinedButton(enabled=!busy&&!loading&&!equipped&&(option.owned||data.coins>=option.price),
                                onClick={if(option.owned)act("equip_paint",option.id) else pending=PendingPurchase("buy_paint",option.id,option.name,option.price)}) {
                                Text(if(equipped)"Activo" else if(option.owned)"Pintar" else "Comprar")
                            }
                        }
                    }
                }
            }
            item { Text("Monedas virtuales sin valor monetario. Los vehículos y pinturas son cosméticos y no otorgan ventajas.",color=c.onSurfaceVariant,fontSize=12.sp) }
        }
    }
}

@Composable
private fun Wallet(data:CareerState) {
    val c=MaterialTheme.colorScheme
    Surface(color=c.primaryContainer,shape=RoundedCornerShape(19.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Icon(painterResource(R.drawable.ic_collection_medal),null,tint=c.onPrimaryContainer,modifier=Modifier.size(34.dp))
            Column(Modifier.weight(1f)) {
                Text("${data.coins} monedas",fontSize=24.sp,fontWeight=FontWeight.ExtraBold,color=c.onPrimaryContainer)
                Text("100 iniciales · 1 por cada 5 XP · premios de carrera",fontSize=12.sp,color=c.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun StageCard(stage:CareerStage,enabled:Boolean,onClaim:()->Unit,onActivity:(String)->Unit) {
    val c=MaterialTheme.colorScheme
    Card(shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,c.outlineVariant),colors=CardDefaults.cardColors(containerColor=c.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Brush.horizontalGradient(zoneColors(stage.zone))).padding(14.dp),
                verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(if(stage.available)R.drawable.ic_award_road else R.drawable.ic_award_lock),null,
                    tint=Color.White,modifier=Modifier.size(31.dp))
                Column(Modifier.weight(1f)) {
                    Text("ETAPA ${stage.index}",fontSize=11.sp,color=Color.White,fontWeight=FontWeight.Bold)
                    Text(stage.title,color=Color.White,fontSize=19.sp,fontWeight=FontWeight.ExtraBold)
                }
            }
            Text(if(stage.claimed)"Completada" else if(stage.available)"+${stage.rewardCoins} monedas al completar" else "Completa la etapa anterior",color=c.primary,fontWeight=FontWeight.Bold,fontSize=13.sp)
            stage.requirements.forEach { goal ->
                Text(goal.label+" · "+goal.progress+"/"+goal.goal,color=c.onSurfaceVariant,fontSize=13.sp)
                LinearProgressIndicator(progress={if(stage.claimed)1f else goal.progress.toFloat()/goal.goal.coerceAtLeast(1)},modifier=Modifier.fillMaxWidth().height(5.dp))
                if(stage.available&&!stage.claimed&&goal.progress<goal.goal) {
                    TextButton(onClick={onActivity(when(goal.metric){"exams"->"exams";"scenarios"->"simulator";else->"games"})},enabled=enabled) {
                        Text(when(goal.metric){"exams"->"Ir a exámenes";"scenarios"->"Ir al simulador";else->"Ir a minijuegos"})
                    }
                }
            }
            if(stage.claimable) Button(onClick=onClaim,enabled=enabled,modifier=Modifier.fillMaxWidth()){Text("Reclamar recompensa")}
        }
    }
}
