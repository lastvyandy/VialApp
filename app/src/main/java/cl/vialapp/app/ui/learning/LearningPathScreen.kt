package cl.vialapp.app.ui.learning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.LearningChapterSummary
import cl.vialapp.app.data.games.GameProgressStore
import cl.vialapp.app.data.session.SessionManager
import cl.vialapp.app.data.session.AuthenticatedRead
import cl.vialapp.app.data.session.readError
import cl.vialapp.app.ui.simulator.SimulatorEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LearningPathEntry(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF243E69), contentColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = Color(0xFF416296), shape = RoundedCornerShape(16.dp)) {
                Icon(painterResource(R.drawable.ic_award_road), contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(32.dp), tint = Color(0xFFC7DBFF))
            }
            Column(Modifier.weight(1f)) {
                Text("Tu camino a la licencia", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Lecciones, desafíos y progreso por etapas", color = Color(0xFFDBEAFE), fontSize = 13.sp)
            }
            Text("›", color = Color(0xFFBFDBFE), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LearningPathScreen(
    userKey: String, mascotId: String?, licenseClass: String?,
    onBack: () -> Unit, onChapter: (String) -> Unit, onExam: () -> Unit,
    onSimulator: () -> Unit = {}, onLibrary: () -> Unit = {}
) {
    val c=MaterialTheme.colorScheme
    val context=LocalContext.current
    val session=remember(context){SessionManager(context)}
    val store=remember(context,userKey){GameProgressStore(context,userKey)}
    val vehicle=rememberJourneyVehicle(userKey)
    var chapters by remember(userKey,licenseClass){mutableStateOf<List<LearningChapterSummary>>(emptyList())}
    var streak by remember(userKey){mutableIntStateOf(0)}
    var loading by remember(userKey){mutableStateOf(true)}
    var error by remember(userKey){mutableStateOf("")}
    var refresh by remember(userKey){mutableIntStateOf(0)}
    val listState=rememberLazyListState()
    // La ruta se vuelve a pedir si cambia la licencia, pero el progreso sigue guardado en su cuenta.
    LaunchedEffect(userKey,licenseClass,refresh){
        loading=true;error=""
        try{
            val token=session.getAccessToken()
            if(token.isNullOrBlank())error="Inicia sesión para continuar tu ruta."
            else{
                val response=AuthenticatedRead.get(session,"learning"){ApiClient.api.getLearningPath(it)}
                val body=response.body()
                if(!response.isSuccessful||body==null){error=if(response.isSuccessful)"No se pudo consultar la ruta: respuesta vacía." else readError(response.code());response.errorBody()?.close()}
                else{
                    chapters=body.chapters.sortedBy{it.order}
                    streak=body.gamification.streakDays?:0
                    store.applyRemoteProgress(body.gamification)
                }
            }
        }catch(cancelled:CancellationException){throw cancelled}
        catch(e:Exception){error=readError(e)}
        finally{loading=false}
    }
    // Clase C tiene su propio recorrido. No queremos que parezca un apéndice del de autos.
    val motorcycle=isMotorcycleLicense(licenseClass)
    val motorcycleStages=chapters.count{it.id.startsWith("moto-")}
    val next=nextChapterIndex(chapters)
    val current=chapters.getOrNull(next)
    val completed=chapters.count{it.progress.completed}
    val total=chapters.sumOf{it.totalSteps}
    val answered=chapters.sumOf{it.answeredSteps}
    val fraction=if(chapters.isEmpty())0f else completed.toFloat()/chapters.size
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(fraction,label="routeProgress")
    val scope=rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().background(c.background),state=listState,
        contentPadding=PaddingValues(horizontal=16.dp,vertical=16.dp)){
        item(key="route_header"){
            LearningHeader("Tu camino a la licencia",onBack,trailing={
                TextButton(onClick={refresh++},enabled=!loading){Text("Actualizar")}
            })
            Column(Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF19365F),Color(0xFF285DA2))),RoundedCornerShape(24.dp))
                .padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text(if(motorcycle)"ACADEMIA VIAL · CLASE C" else "ACADEMIA VIAL · CLASE B",
                            color=Color(0xFFBED6FF),fontWeight=FontWeight.Bold,fontSize=11.sp)
                        Text("Cada paso te acerca a la meta",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.ExtraBold)
                        if(chapters.isNotEmpty())Text(completed.toString()+" de "+chapters.size+" etapas completadas",color=Color(0xFFDBEAFE),fontSize=13.sp)
                    }
                    Image(painterResource(if(mascotId=="mister_pelao")R.drawable.mascot_mister_pelao else R.drawable.mascot_carvier),
                        null,modifier=Modifier.size(77.dp,96.dp),contentScale=ContentScale.Fit)
                }
                if(chapters.isNotEmpty()){
                    LinearProgressIndicator(progress={animatedProgress.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(8.dp),color=Color(0xFF91E4BE),trackColor=Color(0xFF5575A0))
                    Text(answered.toString()+" / "+total+" actividades respondidas · Racha: "+streak+" días",color=Color(0xFFDBEAFE),fontSize=12.sp)
                    HorizontalDivider(color=Color(0xFF5575A0))
                    if(current!=null){
                        Text("TU PRÓXIMA ETAPA",color=Color(0xFFBED6FF),fontSize=11.sp,fontWeight=FontWeight.Bold)
                        Text(current.title,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Bold)
                        Button(onClick={onChapter(current.id)},modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(15.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF19365F))){
                            Text("Continuar etapa "+current.order,fontWeight=FontWeight.Bold)
                        }
                        TextButton(onClick={
                            scope.launch{
                                // Header, optional context, status and road start precede stages.
                                val index=listState.layoutInfo.visibleItemsInfo.firstOrNull{it.key==current.id}?.index
                                if(index!=null)listState.animateScrollToItem(index)
                                else{
                                    val before=1+(if(motorcycle)1 else 0)+(if(error.isNotBlank())1 else 0)+1
                                    listState.animateScrollToItem(before+next)
                                }
                            }
                        },modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColors(contentColor=Color(0xFFDBEAFE))){
                            Text("Ver mi posición en la carretera")
                        }
                    }else Text("¡Ruta completada! Puedes repasar y practicar exámenes.",color=Color.White,fontWeight=FontWeight.Bold)
                }else if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(),color=Color(0xFF91E4BE))
            }
            Spacer(Modifier.height(14.dp))
        }
        // Si el backend todavía no tiene las etapas de moto, lo mostramos claramente.
        if(motorcycle)item(key="class_c_notice"){
            Surface(color=c.primaryContainer,shape=RoundedCornerShape(16.dp)){
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text("TU CAMINO COMO MOTOCICLISTA",color=c.onPrimaryContainer,
                        fontWeight=FontWeight.ExtraBold,fontSize=13.sp)
                    Text(
                        if(motorcycleStages>0)
                            "$motorcycleStages etapas específicas de motocicleta, integradas con circulación, prevención y convivencia vial."
                        else "Estamos mostrando las etapas disponibles. Las etapas de motocicleta aparecerán al actualizar el servicio de aprendizaje.",
                        color=c.onPrimaryContainer,fontSize=13.sp,lineHeight=19.sp
                    )
                    TextButton(onClick=onLibrary,modifier=Modifier.fillMaxWidth()){
                        Text("Consultar biblioteca de motocicletas")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if(error.isNotBlank())item(key="route_error"){LearningError(error){refresh++};Spacer(Modifier.height(12.dp))}
        if(loading&&chapters.isEmpty())item{
            Box(Modifier.fillMaxWidth().height(150.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}
        }
        if(chapters.isNotEmpty()){
            item(key="road_start"){
                Text("Tu recorrido",fontSize=23.sp,fontWeight=FontWeight.ExtraBold,color=c.onSurface)
                Text("Avanza por etapas. Puedes repasar todas las que hayas completado.",fontSize=13.sp,color=c.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
                    Surface(color=c.secondaryContainer,shape=RoundedCornerShape(12.dp)){
                        Text("PUNTO DE PARTIDA",Modifier.padding(horizontal=12.dp,vertical=8.dp),color=c.onSecondaryContainer,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
            itemsIndexed(chapters,key={_,item->item.id}){index,chapter->
                RoadStageNode(chapter,index,chapterAvailable(chapters,index),index==next,vehicle,motorcycle){onChapter(chapter.id)}
            }
            item(key="route_goal"){
                val finished=completed==chapters.size
                Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()){
                    Surface(color=if(finished)Color(0xFF166534) else c.primaryContainer,shape=RoundedCornerShape(18.dp)){
                        Row(Modifier.padding(horizontal=15.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                            Icon(painterResource(R.drawable.ic_collection_medal),null,tint=if(finished)Color.White else c.onPrimaryContainer,modifier=Modifier.size(24.dp))
                            Text(if(finished)"META DE ESTUDIO COMPLETADA" else "META · COMPLETAR EL RECORRIDO",fontSize=12.sp,fontWeight=FontWeight.Bold,color=if(finished)Color.White else c.onPrimaryContainer)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        item(key="practice_tools"){
            Card(onClick=onExam,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,c.outlineVariant),colors=CardDefaults.cardColors(containerColor=c.surface)){
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Icon(painterResource(R.drawable.ic_vial_exam),null,tint=c.primary,modifier=Modifier.size(30.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text("Exámenes de práctica",fontSize=17.sp,fontWeight=FontWeight.Bold,color=c.onSurface)
                        Text("Disponibles en cualquier etapa de tu camino.",fontSize=13.sp,color=c.onSurfaceVariant)
                    }
                    Text("›",fontSize=24.sp,color=c.primary)
                }
            }
            Spacer(Modifier.height(12.dp))
            SimulatorEntry(onClick=onSimulator)
            Spacer(Modifier.height(12.dp))
            Text("Completar el recorrido no equivale a aprobar el examen ni a obtener la licencia.",fontSize=12.sp,color=c.onSurfaceVariant)
        }
    }
}

@Composable
private fun RoadStageNode(chapter:LearningChapterSummary,index:Int,available:Boolean,current:Boolean,vehicle:JourneyVehicle,motorcycle:Boolean,onClick:()->Unit){
    val c=MaterialTheme.colorScheme
    val done=chapter.progress.completed
    val left=index%2==0
    val accent=when{done->Color(0xFF15803D);current->Color(0xFF2563EB);else->c.surfaceVariant}
    val fraction=chapter.answeredSteps.toFloat()/chapter.totalSteps.coerceAtLeast(1)
    Box(Modifier.fillMaxWidth()){
        Canvas(Modifier.matchParentSize()){
            val middle=size.width/2f
            val nodeX=if(left)38.dp.toPx() else size.width-38.dp.toPx()
            val y=size.height/2f
            val road=androidx.compose.ui.graphics.Path().apply{
                moveTo(middle,0f)
                cubicTo(middle,y*.22f,nodeX,y*.20f,nodeX,y)
                cubicTo(nodeX,y*1.80f,middle,y*1.78f,middle,size.height)
            }
            drawPath(road,Color(0xFF73899A),style=androidx.compose.ui.graphics.drawscope.Stroke(width=42.dp.toPx()))
            drawPath(road,Color(0xFF354A60),style=androidx.compose.ui.graphics.drawscope.Stroke(width=35.dp.toPx()))
            drawPath(road,Color(0xFFE3D7AE),style=androidx.compose.ui.graphics.drawscope.Stroke(width=1.6.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(),7.dp.toPx()))))
        }
        Row(Modifier.fillMaxWidth().heightIn(min=150.dp),verticalAlignment=Alignment.CenterVertically){
            if(left)Spacer(Modifier.width(76.dp))
            Card(onClick=onClick,enabled=available,modifier=Modifier.weight(1f).padding(vertical=9.dp),shape=RoundedCornerShape(20.dp),
                border=BorderStroke(if(current)2.dp else 1.dp,if(current)c.primary else c.outlineVariant),
                colors=CardDefaults.cardColors(containerColor=if(current)c.primaryContainer else c.surface,disabledContainerColor=c.surface)){
                val fg=if(current)c.onPrimaryContainer else c.onSurface
                val secondary=if(current)c.onPrimaryContainer else c.onSurfaceVariant
                Column(Modifier.fillMaxWidth().padding(13.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text("ETAPA "+chapter.order+" · "+when{done->"COMPLETADA";current->"ESTÁS AQUÍ";available->"DISPONIBLE";else->"BLOQUEADA"},
                        fontSize=10.sp,fontWeight=FontWeight.Bold,color=secondary)
                    Text(chapter.title,color=fg,fontSize=16.sp,fontWeight=FontWeight.Bold)
                    Text(chapter.progress.completedLessons.size.toString()+"/"+chapter.lessonCount+" lecciones · "+chapter.quizCount+" preguntas finales",color=secondary,fontSize=12.sp)
                    LinearProgressIndicator(progress={fraction.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(5.dp),
                        color=if(done)Color(0xFF15803D)else c.primary,trackColor=if(current)c.surface else c.surfaceVariant)
                    Text(when{done->"Repasar ›";available->"Continuar ›";else->"Completa la etapa anterior"},color=secondary,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                }
            }
            if(!left)Spacer(Modifier.width(76.dp))
        }
        Box(Modifier.align(if(left)Alignment.CenterStart else Alignment.CenterEnd).width(76.dp),contentAlignment=Alignment.Center){
            if(current){
                Canvas(Modifier.size(64.dp)){
                    drawCircle(Color(0xFF8EBBFA).copy(alpha=.38f),size.minDimension*.48f,center)
                    drawCircle(Color(0xFF2563EB),size.minDimension*.40f,center)
                    if(motorcycle)drawJourneyMotorcycle(center,32.dp.toPx(),vehicle.paint)
                    else drawJourneyVehicle(vehicle,center,32.dp.toPx())
                }
            }else{
                Surface(modifier=Modifier.size(50.dp),shape=RoundedCornerShape(25.dp),color=accent,
                    border=BorderStroke(2.dp,if(done)Color(0xFF9BE7B9) else c.outlineVariant)){
                    Box(contentAlignment=Alignment.Center){
                        if(done)Icon(painterResource(R.drawable.ic_collection_medal),null,tint=Color.White,modifier=Modifier.size(25.dp))
                        else if(available)Text(chapter.order.toString(),fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color.White)
                        else Icon(painterResource(R.drawable.ic_award_lock),null,tint=c.onSurfaceVariant,modifier=Modifier.size(23.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun LearningHeader(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹ Volver") }
        Text(title, color = c.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 2)
        trailing()
    }
}

@Composable
internal fun LearningError(message: String, retry: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.errorContainer, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(message, color = c.onErrorContainer, fontSize = 14.sp)
            TextButton(onClick = retry) { Text("Reintentar") }
        }
    }
}
