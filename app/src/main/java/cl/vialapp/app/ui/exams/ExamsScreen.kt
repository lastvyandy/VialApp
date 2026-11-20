package cl.vialapp.app.ui.exams

// GUÍA DEL ARCHIVO:
// Catálogo por temas e historial. Que aparezca un examen no significa que esté desbloqueado.

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cl.vialapp.app.data.api.ApiClient
import cl.vialapp.app.data.api.AssignedTest
import cl.vialapp.app.data.api.ExamAttemptResponse
import cl.vialapp.app.data.api.ExamCategoryCount
import cl.vialapp.app.data.api.ExamCatalogEntry
import cl.vialapp.app.data.api.ExamHistoryResult
import cl.vialapp.app.data.api.ExtraExamRequest
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

private const val PAGE_SIZE=50

private fun classCode(value:String?):String =
    (value?:"B").trim().uppercase(Locale.ROOT).removePrefix("CLASE ").trim()

private fun groupName(value:String?):String =
    value?.trim()?.takeIf{it.isNotBlank()}?:"Mixto"

private fun displayExamName(name:String):String =
    name.replace(Regex("\\s\\d{8}-\\d{4}$"),"").trim()

/**
 * Acá se pueden revisar los exámenes por categoría y volver a los que ya hizo.
 * Ojo: ver un examen en el catálogo no significa que esté desbloqueado.
 * Los permisos y límites los sigue comprobando el servidor.
 */
@Composable
fun ExamsScreen(
    licenseClass:String?,
    category:String?=null,
    onExamClick:(Int)->Unit,
    onBack:()->Unit
){
    val c=MaterialTheme.colorScheme
    val context=LocalContext.current
    val session=remember(context){SessionManager(context)}
    val scope=rememberCoroutineScope()
    val license=remember(licenseClass){classCode(licenseClass)}
    var selected by rememberSaveable(category){mutableStateOf(category)}
    var tab by rememberSaveable{mutableStateOf("categories")}
    var reload by remember{mutableIntStateOf(0)}
    var page by remember(selected){mutableIntStateOf(0)}
    var tests by remember(selected,license){mutableStateOf<List<AssignedTest>>(emptyList())}
    var more by remember(selected){mutableStateOf(false)}
    var testsLoading by remember{mutableStateOf(true)}
    var nextLoading by remember{mutableStateOf(false)}
    var catalogue by remember{mutableStateOf<List<ExamCategoryCount>>(emptyList())}
    var catalogPage by remember(selected){mutableIntStateOf(0)}
    var catalogEntries by remember(selected){mutableStateOf<List<ExamCatalogEntry>>(emptyList())}
    var catalogMore by remember(selected){mutableStateOf(false)}
    var catalogLoading by remember(selected){mutableStateOf(false)}
    var catalogSupported by remember(selected){mutableStateOf(true)}
    var history by remember{mutableStateOf<List<ExamHistoryResult>>(emptyList())}
    var historyLoaded by remember{mutableStateOf(false)}
    var historyLoading by remember{mutableStateOf(false)}
    var activeAttempt by remember{mutableStateOf<ExamAttemptResponse?>(null)}
    var warning by remember{mutableStateOf("")}
    var unlockBusy by remember{mutableStateOf(false)}
    var pendingTest by remember{mutableStateOf<Int?>(null)}

    fun back(){
        when{
            category==null&&selected!=null->{selected=null;warning=""}
            category==null&&tab=="history"->tab="categories"
            else->onBack()
        }
    }
    BackHandler(onBack=::back)
    val owner=LocalLifecycleOwner.current
    DisposableEffect(owner){
        val observer=LifecycleEventObserver{_,event->
            if(event==Lifecycle.Event.ON_RESUME)reload++
        }
        owner.lifecycle.addObserver(observer)
        onDispose{owner.lifecycle.removeObserver(observer)}
    }

    LaunchedEffect(license,reload){
        try{
            val counts=ApiClient.api.getExamCatalogCounts()
            if(counts.isSuccessful)catalogue=counts.body().orEmpty()
            val token=session.getAccessToken()
            if(token!=null){
                val active=ApiClient.api.getActiveAttempt("Bearer $token")
                if(active.isSuccessful)activeAttempt=active.body()?.takeIf{it.status=="active"}
            }
        }catch(e:CancellationException){throw e}
        catch(_:Exception){ /* The assigned list still works without catalog counts. */ }
    }
    // Traemos los exámenes asignados por páginas. No hace falta descargar todo al entrar.
    LaunchedEffect(license,selected,reload,page){
        val initial=page==0
        if(initial){testsLoading=true;tests=emptyList()}else nextLoading=true
        try{
            val token=session.getAccessToken()
            if(token==null){
                warning="Inicia sesión para consultar tus exámenes."
            }else{
                val result=ApiClient.api.getTests(
                    authorization="Bearer $token",
                    licenseClass=licenseClass,
                    category=selected,
                    historyLimit=PAGE_SIZE,
                    historyOffset=page*PAGE_SIZE
                )
                if(result.isSuccessful){
                    val batch=result.body()
                    val all=if(initial)batch?.tests.orEmpty()else tests+batch?.tests.orEmpty()
                    tests=all.distinctBy{it.id to it.assignedDate}
                    more=batch?.historyHasMore==true
                    warning=""
                }else if(initial){
                    warning="No pudimos cargar los exámenes. Pulsa Reintentar."
                }else{
                    warning="No se pudo cargar la página siguiente. Intenta nuevamente."
                }
            }
        }catch(e:CancellationException){throw e}
        catch(_:Exception){warning="No se pudo conectar con VialApp. Pulsa Reintentar."}
        finally{testsLoading=false;nextLoading=false}
    }

    // El catálogo completo trae nombres y categorías, no las preguntas protegidas.
    // Si el servidor todavía no tiene esta función, igual mostramos los exámenes asignados.
    LaunchedEffect(license,selected,catalogPage,reload){
        if(selected==null)return@LaunchedEffect
        catalogLoading=true
        try{
            val token=session.getAccessToken()
            if(token!=null){
                val result=ApiClient.api.getExamCatalogPage(
                    authorization="Bearer $token",
                    licenseClass=licenseClass,
                    category=selected,
                    offset=catalogPage*PAGE_SIZE,
                    limit=PAGE_SIZE
                )
                if(result.isSuccessful){
                    val batch=result.body()
                    val added=batch?.tests.orEmpty()
                    catalogEntries=(if(catalogPage==0)added else catalogEntries+added)
                        .distinctBy{it.id}
                    catalogMore=batch?.hasMore==true
                    catalogSupported=true
                }else{
                    catalogSupported=false
                    catalogMore=false
                }
            }
        }catch(e:CancellationException){throw e}
        catch(_:Exception){catalogSupported=false}
        finally{catalogLoading=false}
    }

    // El historial es otra cosa: acá queremos ver los intentos anteriores, incluso si fueron repetidos.
    LaunchedEffect(tab,reload){
        if(tab!="history")return@LaunchedEffect
        historyLoading=true
        try{
            val token=session.getAccessToken()
            if(token==null){warning="Inicia sesión para consultar el historial."}
            else{
                val result=ApiClient.api.getMyExamHistory("Bearer $token")
                if(result.isSuccessful){
                    history=result.body().orEmpty().sortedByDescending{it.fecha.orEmpty()}
                    historyLoaded=true
                    warning=""
                }else warning="No se pudo cargar el historial."
            }
        }catch(e:CancellationException){throw e}
        catch(_:Exception){warning="No se pudo conectar con VialApp."}
        finally{historyLoading=false}
    }

    // Si ya hay un examen empezado, no abrimos otro por encima sin avisar.
    fun openExam(id:Int){
        if(activeAttempt!=null&&activeAttempt?.testId!=id)pendingTest=id
        else onExamClick(id)
    }

    val selectedItems=remember(tests,selected){
        if(selected==null)tests else tests.filter{groupName(it.categoria).equals(selected,true)}
    }
    val allVisible=remember(catalogEntries,tests,selected){
        val entries=if(catalogEntries.isNotEmpty())catalogEntries else
            tests.filter{selected==null||groupName(it.categoria).equals(selected,true)}
                .map{ExamCatalogEntry(it.id,it.nombre,it.categoria,it.licenseClass)}
        val assigned=tests.filter{selected==null||groupName(it.categoria).equals(selected,true)}
            .associateBy{it.id}
        entries.sortedWith(compareByDescending<ExamCatalogEntry>{
            val exam=assigned[it.id]
            exam?.canStart==true
        }.thenBy{it.id})
    }

    val assignedCounts=remember(tests){
        tests.groupingBy{groupName(it.categoria)}.eachCount()
    }
    // Juntamos las categorías del catálogo con las que el alumno ya tiene asignadas.
    val categoryList=remember(catalogue,tests,license){
        val fromCatalog=catalogue.filter{classCode(it.licenseClass)==license}
            .groupBy{groupName(it.categoria)}
            .mapValues{(_,v)->v.sumOf{it.cantidad}}
        (fromCatalog.keys+tests.map{groupName(it.categoria)}).distinct()
            .sortedWith(compareBy<String>{it!="Mixto"}.thenBy{it})
            .map{it to (fromCatalog[it]?:0)}
    }

    Column(Modifier.fillMaxSize().background(c.background)){
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=12.dp),
            horizontalArrangement=Arrangement.spacedBy(8.dp),
            verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick=::back){Text("‹",fontSize=25.sp)}
            Column(Modifier.weight(1f)){
                Text(if(selected==null)"Tus exámenes" else selected?:"Tus exámenes",
                    fontSize=24.sp,fontWeight=FontWeight.ExtraBold,color=c.onBackground)
                Text("Licencia $license · Aprende, evalúate y repasa",
                    fontSize=12.sp,color=c.onSurfaceVariant)
            }
        }
        if(category==null){
            Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),
                horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(selected=tab=="categories",
                    onClick={tab="categories";selected=null},
                    label={Text("Por categoría")})
                FilterChip(selected=tab=="history",
                    onClick={tab="history";selected=null},
                    label={Text("Mi historial")})
            }
        }
        if(warning.isNotBlank()){
            Surface(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=7.dp),
                shape=RoundedCornerShape(12.dp),color=c.errorContainer){
                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(warning,Modifier.weight(1f),fontSize=12.sp,color=c.onErrorContainer)
                    TextButton(onClick={warning="";reload++}){Text("Reintentar")}
                }
            }
        }
        if(tab=="history"&&category==null){
            if(historyLoading&&!historyLoaded){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    CircularProgressIndicator()
                }
            }else{
                val visible=if(selected!=null)history.filter{
                    groupName(it.categoria).equals(selected,true)
                }else history
                LazyColumn(
                    modifier=Modifier.fillMaxSize(),
                    contentPadding=PaddingValues(start=18.dp,end=18.dp,top=10.dp,bottom=115.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)
                ){
                    item{
                        Text("Exámenes realizados",fontWeight=FontWeight.Bold,fontSize=18.sp)
                        Text("Cada intento conserva su resultado. Para repetir, se aplican las reglas de acceso de tu plan.",
                            color=c.onSurfaceVariant,fontSize=12.sp,lineHeight=17.sp)
                    }
                    if(visible.isEmpty())item{
                        Text("Aún no tienes exámenes realizados.",color=c.onSurfaceVariant)
                    }
                    items(visible,key={it.id}){item->
                        Card(shape=RoundedCornerShape(17.dp),
                            colors=CardDefaults.cardColors(containerColor=c.surface)){
                            Column(Modifier.fillMaxWidth().padding(15.dp),
                                verticalArrangement=Arrangement.spacedBy(7.dp)){
                                Text("Examen #"+item.testId,fontWeight=FontWeight.Bold,
                                    color=c.onSurface)
                                Text(groupName(item.categoria)+" · "+
                                    (item.correctas?:0)+"/"+(item.totalQuestions?:0)+" correctas"+
                                    (item.puntaje?.let{" · $it puntos"}?:""),
                                    color=c.onSurfaceVariant,fontSize=12.sp)
                                Text(item.fecha?.take(10)?:"Fecha no disponible",
                                    color=c.onSurfaceVariant,fontSize=11.sp)
                                OutlinedButton(onClick={openExam(item.testId)},
                                    modifier=Modifier.fillMaxWidth()){
                                    Text("Repetir examen")
                                }
                            }
                        }
                    }
                }
            }
        }else if(selected==null){
            LazyColumn(
                modifier=Modifier.fillMaxSize(),
                contentPadding=PaddingValues(start=18.dp,end=18.dp,top=10.dp,bottom=115.dp),
                verticalArrangement=Arrangement.spacedBy(11.dp)
            ){
                item{
                    Text("Elige una categoría",fontSize=18.sp,fontWeight=FontWeight.Bold)
                    Text("Consulta el catálogo completo y los exámenes que tienes asignados. Los exámenes por tema y los adicionales pueden requerir Premium.",
                        color=c.onSurfaceVariant,fontSize=12.sp,lineHeight=17.sp)
                }
                if(testsLoading&&categoryList.isEmpty())item{
                    CircularProgressIndicator()
                }
                if(categoryList.isEmpty()&&!testsLoading)item{
                    Text("No hay categorías disponibles para esta licencia.",
                        color=c.onSurfaceVariant)
                }
                items(categoryList,key={it.first}){(name,count)->
                    Card(onClick={selected=name;page=0},shape=RoundedCornerShape(18.dp),
                        colors=CardDefaults.cardColors(containerColor=c.surface),
                        border=BorderStroke(1.dp,c.outlineVariant)){
                        Row(Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                                Text(name,fontWeight=FontWeight.Bold,fontSize=17.sp,
                                    color=c.onSurface)
                                Text(
                                    (if(count>0)"$count exámenes en el catálogo"else "Exámenes de esta categoría"),
                                    color=c.onSurfaceVariant,fontSize=12.sp)
                            }
                            Text("Ver ›",color=c.primary,fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
        }else{
            LazyColumn(
                modifier=Modifier.fillMaxSize(),
                contentPadding=PaddingValues(start=18.dp,end=18.dp,top=8.dp,bottom=115.dp),
                verticalArrangement=Arrangement.spacedBy(10.dp)
            ){
                item{
                    Text("Exámenes de "+selected,fontSize=18.sp,fontWeight=FontWeight.Bold)
                    Text("Explora los exámenes del catálogo. Los que aún no tienes asignados aparecen bloqueados; la disponibilidad se gestiona desde tu cuenta.",
                        fontSize=12.sp,color=c.onSurfaceVariant,lineHeight=18.sp)
                }
                activeAttempt?.let{attempt->
                    item{
                        Card(colors=CardDefaults.cardColors(containerColor=c.primaryContainer)){
                            Column(Modifier.fillMaxWidth().padding(14.dp)){
                                Text("Tienes un examen en curso",fontWeight=FontWeight.Bold,
                                    color=c.onPrimaryContainer)
                                Button(onClick={onExamClick(attempt.testId)},
                                    modifier=Modifier.fillMaxWidth()){
                                    Text("Continuar examen")
                                }
                            }
                        }
                    }
                }
                if(!catalogSupported)item{
                    Surface(color=c.surfaceVariant,shape=RoundedCornerShape(12.dp)){
                        Text("No se pudo consultar el catálogo completo. Por ahora puedes ver y realizar los exámenes que ya tienes asignados.",
                            modifier=Modifier.fillMaxWidth().padding(12.dp),
                            fontSize=12.sp,color=c.onSurfaceVariant)
                    }
                }
                if(testsLoading)item{
                    Box(Modifier.fillMaxWidth().height(65.dp),contentAlignment=Alignment.Center){
                        CircularProgressIndicator()
                    }
                }
                if(allVisible.isEmpty()&&!testsLoading&&!catalogLoading)item{
                    Text("No hay exámenes asignados todavía en esta categoría. Consulta otras categorías o solicita otro examen si tu plan lo permite.",
                        color=c.onSurfaceVariant,fontSize=13.sp)
                }
                items(allVisible,key={it.id}){entry->
                    val exam=selectedItems.firstOrNull{it.id==entry.id}
                    val active=activeAttempt?.testId==entry.id
                    val allowed=active||exam?.canStart==true
                    Card(shape=RoundedCornerShape(18.dp),
                        border=BorderStroke(1.dp,c.outlineVariant),
                        colors=CardDefaults.cardColors(containerColor=c.surface)){
                        Column(Modifier.fillMaxWidth().padding(15.dp),
                            verticalArrangement=Arrangement.spacedBy(7.dp)){
                            Text(displayExamName(entry.nombre),fontSize=16.sp,
                                fontWeight=FontWeight.Bold,color=c.onSurface)
                            Text(when{
                                active -> "En curso"
                                exam==null -> "No asignado todavía"
                                exam.isLocked -> exam.lockedReason?:"Requiere Premium"
                                exam.hasResult -> "Realizado anteriormente"
                                allowed -> "Disponible"
                                else -> "No disponible por ahora"
                            },color=if(allowed)c.primary else c.onSurfaceVariant,fontSize=12.sp)
                            if(exam?.assignedDate!=null)Text(
                                "Asignado: "+exam.assignedDate.take(10),
                                color=c.onSurfaceVariant,fontSize=11.sp)
                            Button(
                                onClick={openExam(entry.id)},
                                enabled=allowed,
                                modifier=Modifier.fillMaxWidth(),
                                colors=ButtonDefaults.buttonColors(containerColor=c.primary)){
                                Text(when{
                                    active->"Continuar examen"
                                    !allowed->"Pendiente de asignación"
                                    exam?.hasResult==true->"Volver a realizar"
                                    else->"Comenzar examen"
                                })
                            }
                        }
                    }
                }
                if(catalogLoading&&catalogEntries.isEmpty())item{
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){
                        CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)
                    }
                }
                if(catalogMore)item{
                    OutlinedButton(onClick={catalogPage++},enabled=!catalogLoading,
                        modifier=Modifier.fillMaxWidth()){
                        Text(if(catalogLoading)"Cargando catálogo..." else "Mostrar más del catálogo")
                    }
                }
                if(more)item{
                    OutlinedButton(onClick={page++},enabled=!nextLoading,
                        modifier=Modifier.fillMaxWidth()){
                        if(nextLoading)CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp)
                        else Text("Cargar más exámenes")
                    }
                }
                item{
                    OutlinedButton(
                        onClick={
                            if(!unlockBusy){
                                scope.launch{
                                    unlockBusy=true
                                    try{
                                        val token=session.getAccessToken()
                                        if(token==null){
                                            warning="Debes iniciar sesión para solicitar otro examen."
                                        }else if(license !in setOf("B","C")){
                                            warning="Este desbloqueo está disponible para clases B y C."
                                        }else{
                                            val result=ApiClient.api.unlockExtraExam(
                                                "Bearer $token",
                                                ExtraExamRequest(license,selected?:"Mixto")
                                            )
                                            when{
                                                result.isSuccessful->{
                                                    warning="¡Nuevo examen asignado! Ya puedes comenzar."
                                                    page=0
                                                    reload++
                                                }
                                                result.code()==403->
                                                    warning="Los exámenes adicionales requieren Premium de esta clase de licencia."
                                                result.code()==409->
                                                    warning="Completa primero los exámenes disponibles o comprueba si ya agotaste el catálogo de esta categoría."
                                                else->warning="No se pudo asignar un examen adicional. Reintenta."
                                            }
                                        }
                                    }catch(e:CancellationException){throw e}
                                    catch(_:Exception){
                                        warning="No se pudo conectar. Reintenta al recuperar la conexión."
                                    }finally{unlockBusy=false}
                                }
                            }
                        },
                        enabled=!unlockBusy,
                        modifier=Modifier.fillMaxWidth()
                    ){
                        if(unlockBusy)CircularProgressIndicator(Modifier.size(16.dp),strokeWidth=2.dp)
                        else Text("Solicitar otro examen · Premium")
                    }
                }
                item{
                    Text("Puedes repetir exámenes asignados cuando tu plan lo permita. Nunca se muestran preguntas de exámenes bloqueados.",
                        color=c.onSurfaceVariant,fontSize=11.sp)
                }
            }
        }
    }

    pendingTest?.let{
        AlertDialog(
            onDismissRequest={pendingTest=null},
            title={Text("Ya tienes un examen en curso")},
            text={Text("Termina o cancela tu examen actual antes de empezar otro. Puedes continuar el examen pendiente ahora.")},
            confirmButton={
                TextButton(onClick={
                    pendingTest=null
                    activeAttempt?.testId?.let(onExamClick)
                }){Text("Continuar examen")}
            },
            dismissButton={TextButton(onClick={pendingTest=null}){Text("Volver")}}
        )
    }
}
