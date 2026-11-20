package cl.vialapp.app.ui.practice

// GUÍA DEL ARCHIVO:
// Estudio adaptativo: alumno nuevo ve diagnóstico, y quien ya respondió ve temas para reforzar.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.CategoryStatResponse
import cl.vialapp.app.ui.learning.LearningPathEntry
import cl.vialapp.app.ui.theme.vialColor

/**
 * Primero mostramos lo que el alumno necesita para avanzar.
 * Si es nuevo, parte con la ruta y un examen de diagnóstico.
 * Si ya tenemos suficientes respuestas, le mostramos qué tema debería reforzar.
 * No hacemos que una función Premium sea obligatoria para comenzar.
 */
@Composable
internal fun PracticeDashboard(
    stats: List<CategoryStatResponse>,
    loading: Boolean,
    error: String,
    onRetry: () -> Unit,
    onPracticeCategory: (String) -> Unit,
    onTakeExam: () -> Unit,
    onLearningPath: () -> Unit,
    onAdaptivePractice: () -> Unit,
    onSimulator: () -> Unit,
    onGames: () -> Unit,
    onLibrary: () -> Unit,
    onOfflineStudy: () -> Unit
) {
    val c=MaterialTheme.colorScheme
    val primary=vialColor(Color(0xFF2563EB))
    // No sirve recomendar una materia solo porque tuvo una pregunta mala. Usamos los datos que ya alcanzan.
    val priority=remember(stats){recommendedPriority(stats)}
    val categories=remember(stats){rankedPracticeCategories(stats)}
    var expanded by rememberSaveable{mutableStateOf(false)}

    LazyColumn(
        modifier=Modifier.fillMaxSize().background(c.background),
        contentPadding=PaddingValues(start=18.dp,end=18.dp,top=18.dp,bottom=110.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)
    ){
        item {
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text("Tu entrenamiento vial",color=c.onBackground,
                    fontSize=26.sp,fontWeight=FontWeight.ExtraBold)
                Text(
                    if(priority!=null)"Hoy te recomendamos reforzar un tema antes de continuar."
                    else "Avanza paso a paso hacia tu licencia de conducir.",
                    color=c.onSurfaceVariant,fontSize=14.sp,lineHeight=20.sp
                )
            }
        }

        if(priority!=null) {
            item {
                PracticePriorityTile(
                    stat=priority,
                    onPractice={onPracticeCategory(priority.categoria)}
                )
            }
        }

        item {
            Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                SectionLabel("TU RECORRIDO DE APRENDIZAJE")
                LearningPathEntry(onClick=onLearningPath)
            }
        }

        // Si todavía no hay una prioridad confiable, mejor ofrecer una evaluación inicial.
        if(priority==null){
            item {
                Card(
                    shape=RoundedCornerShape(20.dp),
                    colors=CardDefaults.cardColors(containerColor=c.surface),
                    border=BorderStroke(1.dp,c.outlineVariant),
                    modifier=Modifier.fillMaxWidth()
                ){
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        Text(
                            when {
                                loading->"Buscando recomendaciones para ti"
                                stats.isEmpty()->"Descubramos qué necesitas practicar"
                                else->"Aún necesitamos más respuestas"
                            },
                            color=c.onSurface,fontSize=17.sp,fontWeight=FontWeight.Bold
                        )
                        Text(
                            when {
                                loading->"Mientras se actualiza tu historial, puedes empezar tu camino o hacer un examen."
                                stats.isEmpty()->"Haz una primera evaluación para conocer tus fortalezas y los temas que necesitas reforzar."
                                else->"Tus resultados todavía no permiten recomendar una categoría con suficiente confianza. Sigue practicando para personalizar tu plan."
                            },
                            color=c.onSurfaceVariant,fontSize=13.sp,lineHeight=19.sp
                        )
                    }
                }
            }
        }

        if(error.isNotBlank()){
            item {
                Row(
                    Modifier.fillMaxWidth()
                        .background(c.errorContainer,RoundedCornerShape(12.dp))
                        .padding(horizontal=12.dp,vertical=8.dp),
                    horizontalArrangement=Arrangement.spacedBy(8.dp),
                    verticalAlignment=Alignment.CenterVertically
                ){
                    Text("No se pudo actualizar tu historial. Tu ruta y las demás herramientas siguen disponibles.",
                        modifier=Modifier.weight(1f),color=c.onErrorContainer,
                        fontSize=12.sp,lineHeight=16.sp)
                    TextButton(onClick=onRetry){
                        Text("Reintentar",fontSize=12.sp,color=c.onErrorContainer)
                    }
                }
            }
        }

        item {
            Card(
                modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(20.dp),
                colors=CardDefaults.cardColors(containerColor=c.primaryContainer)
            ){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(if(priority==null)"MIDE TU PUNTO DE PARTIDA" else "COMPRUEBA TU AVANCE",
                        color=c.onPrimaryContainer,fontWeight=FontWeight.Bold,fontSize=11.sp)
                    Text(if(priority==null&&stats.isEmpty()&&!loading)"Realiza tu primera evaluación"
                         else if(priority==null)"Construyamos tu diagnóstico"
                         else "¿Listo para poner a prueba lo aprendido?",
                        color=c.onPrimaryContainer,fontWeight=FontWeight.Bold,fontSize=17.sp)
                    Text(if(priority==null&&stats.isEmpty())"Un primer examen nos ayuda a detectar qué contenidos reforzar."
                         else if(priority==null)"Aún necesitamos más respuestas para crear una recomendación fiable."
                         else "Un examen completo permite actualizar tus resultados por categoría.",
                        color=c.onPrimaryContainer,fontSize=13.sp)
                    Button(onClick=onTakeExam,modifier=Modifier.fillMaxWidth()){
                        Text(if(priority==null&&stats.isEmpty()&&!loading)"Hacer mi primer examen" else "Realizar un examen")
                    }
                }
            }
        }

        item { SectionLabel("REFUERZA Y APLICA LO APRENDIDO") }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                PracticeQuickTile(
                    title="Entrenamiento personalizado",
                    subtitle="Refuerza errores con sesiones cortas · Premium",
                    icon=R.drawable.ic_award_target,
                    onClick=onAdaptivePractice,
                    modifier=Modifier.weight(1f)
                )
                PracticeQuickTile(
                    title="Simulador Vial",
                    subtitle="Resuelve situaciones reales de tránsito",
                    icon=R.drawable.ic_award_wheel,
                    onClick=onSimulator,
                    modifier=Modifier.weight(1f)
                )
            }
        }

        item { SectionLabel("EXPLORA Y REPASA") }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                PracticeQuickTile(
                    title="Biblioteca Vial",
                    subtitle="Consulta teoría y material de estudio",
                    icon=R.drawable.ic_vial_library,
                    onClick=onLibrary,
                    modifier=Modifier.weight(1f)
                )
                PracticeQuickTile(
                    title="Minijuegos",
                    subtitle="Practica con desafíos y recompensas",
                    icon=R.drawable.ic_home_gamepad,
                    onClick=onGames,
                    modifier=Modifier.weight(1f)
                )
            }
        }
        item {
            OutlinedButton(onClick=onOfflineStudy,modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(14.dp)){
                Icon(painterResource(R.drawable.ic_vial_bookmark),
                    contentDescription=null,modifier=Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Repasar sin conexión",fontWeight=FontWeight.Medium)
            }
        }

        if(categories.size>1){
            item {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.SpaceBetween){
                    Column {
                        Text("Tu rendimiento por temas",fontSize=18.sp,
                            color=c.onSurface,fontWeight=FontWeight.Bold)
                        Text("${categories.size} categorías registradas",
                            color=c.onSurfaceVariant,fontSize=12.sp)
                    }
                    TextButton(onClick={expanded=!expanded}){
                        Text(if(expanded)"Ocultar" else "Ver todas")
                    }
                }
            }
            if(expanded){
                item {
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                        categories.forEach { category->
                            Card(onClick={onPracticeCategory(category.categoria)},
                                modifier=Modifier.fillMaxWidth(),
                                shape=RoundedCornerShape(13.dp),
                                colors=CardDefaults.cardColors(containerColor=c.surface),
                                border=BorderStroke(1.dp,c.outlineVariant)){
                                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                                    Column(Modifier.weight(1f),
                                        verticalArrangement=Arrangement.spacedBy(3.dp)){
                                        Text(category.categoria,color=c.onSurface,
                                            fontWeight=FontWeight.SemiBold,fontSize=14.sp)
                                        Text("${category.incorrectas} errores de ${category.totalAnswered} respuestas",
                                            color=c.onSurfaceVariant,fontSize=11.sp)
                                    }
                                    Text("${category.successPercentage}%",
                                        color=if(category.successPercentage>=80)
                                            vialColor(Color(0xFF14966F))
                                            else if(category.successPercentage>=60)
                                                vialColor(Color(0xFFBE830A))
                                            else c.error,
                                        fontWeight=FontWeight.Bold,fontSize=15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(value:String){
    Text(value,fontSize=11.sp,fontWeight=FontWeight.ExtraBold,
        color=MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PracticePriorityTile(stat:CategoryStatResponse,onPractice:()->Unit){
    val c=MaterialTheme.colorScheme
    val blue=vialColor(Color(0xFF2563EB))
    Card(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=c.secondaryContainer)
    ){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("TU PRIORIDAD DE HOY",color=c.onSecondaryContainer,
                fontWeight=FontWeight.ExtraBold,fontSize=11.sp)
            Text(stat.categoria,color=c.onSecondaryContainer,fontSize=20.sp,
                fontWeight=FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("${stat.successPercentage}% de aciertos",color=c.onSecondaryContainer,
                    fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                Text("${stat.incorrectas} errores / ${stat.totalAnswered} respuestas",
                    color=c.onSecondaryContainer,fontSize=12.sp)
            }
            LinearProgressIndicator(
                progress={ (stat.successPercentage/100f).coerceIn(0f,1f) },
                modifier=Modifier.fillMaxWidth().height(7.dp),
                color=blue,trackColor=c.onSecondaryContainer.copy(alpha=.17f)
            )
            Button(onClick=onPractice,modifier=Modifier.fillMaxWidth(),
                shape=RoundedCornerShape(13.dp)){
                Text("Practicar esta categoría",fontWeight=FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PracticeQuickTile(
    title:String,subtitle:String,icon:Int,
    onClick:()->Unit,modifier:Modifier=Modifier
){
    val c=MaterialTheme.colorScheme
    Card(
        onClick=onClick,modifier=modifier.heightIn(min=154.dp),
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=c.surface),
        border=BorderStroke(1.dp,c.outlineVariant)
    ){
        Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Box(
                Modifier.size(39.dp).background(c.primaryContainer,RoundedCornerShape(12.dp)),
                contentAlignment=Alignment.Center
            ){
                Icon(painter=painterResource(icon),contentDescription=null,
                    tint=c.onPrimaryContainer,modifier=Modifier.size(22.dp))
            }
            Text(title,color=c.onSurface,fontWeight=FontWeight.Bold,
                fontSize=14.sp,lineHeight=18.sp)
            Text(subtitle,color=c.onSurfaceVariant,fontSize=12.sp,lineHeight=16.sp)
        }
    }
}
