package cl.vialapp.app.ui.library

// GUÍA DEL ARCHIVO:
// Navegación visual por la biblioteca, temas y licencias.

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R

@Composable
internal fun LibraryEntry(onClick:()->Unit){
    val c=MaterialTheme.colorScheme
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),
        colors=CardDefaults.cardColors(containerColor=c.primaryContainer)){
        Row(Modifier.padding(17.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){
            Surface(shape=RoundedCornerShape(15.dp),color=c.surface){
                Icon(painterResource(R.drawable.ic_vial_library),contentDescription=null,
                    tint=c.primary,modifier=Modifier.padding(12.dp).size(25.dp))
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text("Biblioteca vial",fontSize=19.sp,fontWeight=FontWeight.ExtraBold,color=c.onPrimaryContainer)
                Text("Consulta los manuales de CONASET, incluso sin internet.",fontSize=12.sp,color=c.onPrimaryContainer)
            }
            Text("›",fontSize=28.sp,color=c.onPrimaryContainer)
        }
    }
}

@Composable
internal fun VialLibraryScreen(userKey:String,licenseClass:String?,onBack:()->Unit){
    val context=LocalContext.current
    val colors=MaterialTheme.colorScheme
    val prefs=remember(context,userKey){context.getSharedPreferences("vial_library_"+userKey,Context.MODE_PRIVATE)}
    var favorites by remember(prefs){mutableStateOf(prefs.getStringSet("favorites",emptySet())?.toSet().orEmpty())}
    var query by rememberSaveable{mutableStateOf("")}
    var sourceFilter by rememberSaveable(licenseClass){mutableStateOf(if(licenseClass?.trim()?.endsWith("C",ignoreCase=true)==true)"all" else "B")}
    var section by rememberSaveable{mutableStateOf<String?>(null)}
    var onlyFavorites by rememberSaveable{mutableStateOf(false)}
    var opened by rememberSaveable{mutableStateOf<String?>(null)}
    val article=VialLibrary.articles.firstOrNull{it.id==opened}
    fun toggle(id:String){
        favorites=if(id in favorites) favorites-id else favorites+id
        prefs.edit().putStringSet("favorites",favorites.toSet()).apply()
    }
    BackHandler{if(article!=null)opened=null else onBack()}
    if(article!=null){
        ArticleView(article,article.id in favorites,onBack={opened=null},onFavorite={toggle(article.id)})
        return
    }
    val manual=Manual.entries.firstOrNull{it.name==sourceFilter}
    val sections=remember(manual){VialLibrary.articles.filter{manual==null||it.manual==manual}.map{it.section}.distinct()}
    val results=remember(query,manual,section,onlyFavorites,favorites){
        VialLibrary.search(query,manual,section,if(onlyFavorites)favorites else null)
    }
    LazyColumn(
        modifier=Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=20.dp,vertical=16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)
    ){
        item{
            TextButton(onClick=onBack,contentPadding=PaddingValues(0.dp)){Text("‹ Volver")}
            Text("Biblioteca vial",fontSize=30.sp,fontWeight=FontWeight.ExtraBold,color=colors.onSurface)
            Text("Tu guía de consulta · disponible sin conexión",fontSize=14.sp,color=colors.onSurfaceVariant)
        }
        item{
            Surface(shape=RoundedCornerShape(18.dp),color=colors.primaryContainer){
                Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    Text("Basada en los libros de CONASET",fontWeight=FontWeight.Bold,color=colors.onPrimaryContainer)
                    Text("Clase B: edición 2024. Motociclistas: edición 2016. El manual de Clase C es complementario al de Clase B.",fontSize=12.sp,color=colors.onPrimaryContainer)
                    Text("Fichas de estudio de las ediciones indicadas. CONASET dispone de una edición Clase B de febrero de 2026; la revisión integral frente a esa edición está pendiente.",fontSize=12.sp,color=colors.onPrimaryContainer)
                    TextButton(onClick={openManualPortal(context)}){Text("Consultar versiones oficiales")}
                    Text("Examen teórico B y C: 35 preguntas, 38 puntos posibles (3 preguntas dobles), mínimo 33 puntos. Tiempo máximo 45 minutos. Las preguntas reales no son públicas.",fontSize=12.sp,color=colors.onPrimaryContainer)
                }
            }
        }
        item{
            OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,
                label={Text("Buscar un tema")},placeholder={Text("Ej. ABS, casco, distancia…")},
                leadingIcon={Icon(painterResource(R.drawable.ic_vial_search),null,modifier=Modifier.size(22.dp))},
                trailingIcon={if(query.isNotEmpty())TextButton(onClick={query=""}){Text("Borrar")}},
                keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),shape=RoundedCornerShape(16.dp))
        }
        item{
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                items(listOf("all" to "Ambas clases","B" to "Clase B","C" to "Clase C"),key={it.first}){(key,label)->
                    FilterChip(selected=sourceFilter==key,onClick={sourceFilter=key;section=null},label={Text(label)})
                }
            }
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                item{FilterChip(selected=section==null,onClick={section=null},label={Text("Todos los temas")})}
                items(sections,key={it}){name->FilterChip(selected=section==name,onClick={section=if(section==name)null else name},label={Text(name)})}
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text(results.size.toString()+" fichas",color=colors.onSurfaceVariant,modifier=Modifier.weight(1f),fontSize=13.sp)
                FilterChip(selected=onlyFavorites,onClick={onlyFavorites=!onlyFavorites},label={Text("Guardadas ("+favorites.size+")")})
            }
        }
        if(results.isEmpty())item{
            Surface(shape=RoundedCornerShape(18.dp),color=colors.surfaceVariant){
                Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("No encontramos fichas",fontWeight=FontWeight.Bold,color=colors.onSurface)
                    Text("Prueba otra palabra o cambia los filtros.",color=colors.onSurfaceVariant)
                    TextButton(onClick={query="";section=null;sourceFilter="all";onlyFavorites=false}){Text("Restablecer filtros")}
                }
            }
        }
        items(results,key={it.id}){entry->
            Surface(onClick={opened=entry.id},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),
                color=colors.surface,border=BorderStroke(1.dp,colors.outlineVariant)){
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Surface(shape=RoundedCornerShape(12.dp),color=colors.secondaryContainer){
                        Icon(painterResource(if(entry.section=="Señales de tránsito verticales")R.drawable.ic_award_stop
                            else if(entry.section=="Funcionamiento de la motocicleta"||entry.section=="Los principios de la conducción")R.drawable.ic_award_wrench
                            else R.drawable.ic_vial_library),null,
                            tint=colors.onSecondaryContainer,modifier=Modifier.padding(10.dp).size(24.dp))
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Text(entry.title,fontSize=16.sp,fontWeight=FontWeight.Bold,color=colors.onSurface)
                        Text(entry.section,fontSize=12.sp,color=colors.onSurfaceVariant)
                        Text("Clase "+entry.manual.name+" · "+entry.reference,fontSize=11.sp,color=colors.primary)
                    }
                    IconButton(onClick={toggle(entry.id)}){Icon(painterResource(if(entry.id in favorites)R.drawable.ic_vial_bookmark_filled else R.drawable.ic_vial_bookmark),
                        contentDescription=if(entry.id in favorites)"Quitar de guardadas" else "Guardar ficha",
                        tint=colors.primary,modifier=Modifier.size(22.dp))}
                }
            }
        }
        item{
            Text("Fuente: Comisión Nacional de Seguridad de Tránsito (CONASET). Resúmenes de VialApp.",fontSize=12.sp,color=colors.onSurfaceVariant)
            TextButton(onClick={openManualPortal(context)}){Text("Consultar manuales oficiales")}
        }
    }
}

@Composable
private fun ArticleView(article:VialArticle,saved:Boolean,onBack:()->Unit,onFavorite:()->Unit){
    val c=MaterialTheme.colorScheme
    val context=LocalContext.current
    LazyColumn(modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{
            Row(verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick=onBack,modifier=Modifier.weight(1f),contentPadding=PaddingValues(0.dp)){Text("‹ Biblioteca")}
                IconButton(onClick=onFavorite){Icon(painterResource(if(saved)R.drawable.ic_vial_bookmark_filled else R.drawable.ic_vial_bookmark),if(saved)"Quitar de guardadas" else "Guardar ficha",tint=c.primary)}
            }
            Text("CLASE "+article.manual.name+" · "+article.section.uppercase(java.util.Locale.ROOT),fontSize=11.sp,fontWeight=FontWeight.Bold,color=c.primary)
            Spacer(Modifier.height(8.dp))
            Text(article.title,fontSize=29.sp,fontWeight=FontWeight.ExtraBold,color=c.onSurface)
        }
        item{
            Surface(shape=RoundedCornerShape(20.dp),color=c.primaryContainer){
                Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("En pocas palabras",fontWeight=FontWeight.Bold,color=c.onPrimaryContainer,fontSize=15.sp)
                    Text(article.summary,color=c.onPrimaryContainer,fontSize=16.sp,lineHeight=24.sp)
                }
            }
        }
        item{Text("Puntos clave",fontWeight=FontWeight.ExtraBold,fontSize=21.sp,color=c.onSurface)}
        items(article.points){point->
            Surface(shape=RoundedCornerShape(16.dp),color=c.surface,border=BorderStroke(1.dp,c.outlineVariant)){
                Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    Icon(painterResource(R.drawable.ic_award_target),null,tint=c.primary,modifier=Modifier.size(20.dp))
                    Text(point,color=c.onSurface,modifier=Modifier.weight(1f),fontSize=15.sp,lineHeight=22.sp)
                }
            }
        }
        item{
            Surface(shape=RoundedCornerShape(17.dp),color=c.surfaceVariant){
                Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text("Fuente de estudio",fontWeight=FontWeight.Bold,color=c.onSurface)
                    Text(article.manual.title+" · "+article.manual.edition,color=c.onSurfaceVariant,fontSize=13.sp)
                    Text("CONASET · "+article.reference,color=c.onSurfaceVariant,fontSize=13.sp)
                    Text("Resumen no oficial de la edición indicada. Consulta la versión vigente para normativa y requisitos.",fontSize=12.sp,color=c.onSurfaceVariant)
                    OutlinedButton(onClick={openManualPortal(context)},modifier=Modifier.fillMaxWidth()){Text("Portal oficial de manuales")}
                }
            }
        }
    }
}

private fun openManualPortal(context:Context){
    runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://mejoresconductores.conaset.cl/FileManager/MaterialDeEstudio")))}
        .onFailure{android.widget.Toast.makeText(context,"No se pudo abrir el navegador",android.widget.Toast.LENGTH_SHORT).show()}
}
