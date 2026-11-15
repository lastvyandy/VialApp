package cl.vialapp.app.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Standalone free study content: no official rewards or results. */
@Composable
internal fun OfflineStudyScreen(onBack:()->Unit,userKey:String="offline_guest",licenseClass:String?=null){
    val c=MaterialTheme.colorScheme
    var mode by rememberSaveable { mutableStateOf("menu") }
    var license by rememberSaveable { mutableStateOf(if(licenseClass?.trim()?.endsWith("C",ignoreCase=true)==true)"C" else "B") }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var score by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var seed by rememberSaveable{mutableIntStateOf(0)}
    val questions=remember(license,seed){OfflineQuiz.forClass(license).shuffled(kotlin.random.Random(seed))}
    fun start(){index=0;score=0;selected=-1;seed=kotlin.random.Random.nextInt();mode="quiz"}
    fun back(){if(mode=="menu")onBack() else mode="menu"}
    BackHandler(enabled=mode!="library"){back()}
    if(mode=="library"){
        VialLibraryScreen(userKey,license,onBack={mode="menu"})
        return
    }
    Surface(modifier=Modifier.fillMaxSize(),color=c.background){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)){
            TextButton(onClick={back()}){Text("‹ Volver")}
            Text("Estudio sin conexión",fontSize=28.sp,fontWeight=FontWeight.ExtraBold,color=c.onSurface)
            if(mode=="menu"){
                Text("Consulta y practica con el material integrado. No necesitas una cuenta.",color=c.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    FilterChip(selected=license=="B",onClick={license="B"},label={Text("Clase B")})
                    FilterChip(selected=license=="C",onClick={license="C"},label={Text("Clase C + base B")})
                }
                LibraryEntry(onClick={mode="library"})
                Card(colors=CardDefaults.cardColors(containerColor=c.surface),border=BorderStroke(1.dp,c.outlineVariant)){
                    Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                        Text("Repaso de conocimientos",fontWeight=FontWeight.Bold,fontSize=20.sp)
                        Text(questions.size.toString()+" preguntas con explicación y referencia al manual.",color=c.onSurfaceVariant)
                        Button(onClick={start()},modifier=Modifier.fillMaxWidth()){Text("Comenzar repaso")}
                    }
                }
                Text("No es un examen oficial: no genera XP, monedas, racha ni resultados sincronizados.",color=c.onSurfaceVariant,fontSize=13.sp)
            }else if(index>=questions.size){
                Text("Repaso terminado",fontSize=25.sp,fontWeight=FontWeight.Bold)
                Text(score.toString()+" / "+questions.size,fontSize=48.sp,color=c.primary,fontWeight=FontWeight.ExtraBold)
                Text("Resultado local, no una estimación de aprobación del examen.",color=c.onSurfaceVariant)
                Button(onClick={start()},modifier=Modifier.fillMaxWidth()){Text("Volver a practicar")}
                OutlinedButton(onClick={mode="menu"},modifier=Modifier.fillMaxWidth()){Text("Volver al menú")}
            }else{
                val q=questions[index]
                val source=OfflineQuiz.source(q)
                Text("Pregunta "+(index+1)+" de "+questions.size,color=c.primary,fontWeight=FontWeight.Bold)
                LinearProgressIndicator(progress={index.toFloat()/questions.size},modifier=Modifier.fillMaxWidth())
                Text(q.prompt,fontSize=21.sp,fontWeight=FontWeight.Bold)
                q.options.forEachIndexed{i,option->
                    val feedback=selected>=0
                    val color=when{
                        feedback && i==q.correct->c.primaryContainer
                        feedback && i==selected->c.errorContainer
                        else->c.surface
                    }
                    val fg=when{
                        feedback && i==q.correct->c.onPrimaryContainer
                        feedback && i==selected->c.onErrorContainer
                        else->c.onSurface
                    }
                    Surface(onClick={if(selected==-1){selected=i;if(i==q.correct)score++}},enabled=!feedback,
                        color=color,shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,c.outlineVariant),modifier=Modifier.fillMaxWidth()){
                        Text(option,Modifier.padding(16.dp),color=fg,fontSize=15.sp,lineHeight=22.sp)
                    }
                }
                if(selected>=0){
                    Surface(color=c.surfaceVariant,shape=RoundedCornerShape(16.dp)){
                        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                            Text(if(selected==q.correct)"Respuesta correcta" else "Revisa la respuesta",fontWeight=FontWeight.Bold,color=c.onSurface)
                            Text(q.explanation,color=c.onSurfaceVariant)
                            Text("CONASET · "+source.manual.title+" · "+source.reference,fontSize=12.sp,color=c.onSurfaceVariant)
                        }
                    }
                    Button(onClick={if(selected>=0){index++;selected=-1}},modifier=Modifier.fillMaxWidth()){Text(if(index==questions.lastIndex)"Ver resultado" else "Siguiente")}
                }
            }
        }
    }
}
