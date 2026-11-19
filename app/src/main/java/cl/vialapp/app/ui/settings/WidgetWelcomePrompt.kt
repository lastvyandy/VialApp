package cl.vialapp.app.ui.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.engagement.StudyWidget

/** The launcher widget is a device-level choice, not an account-level setting. */
@Composable
internal fun WidgetWelcomePrompt(mascotId:String?) {
    val context=LocalContext.current
    val preferences=remember(context) {
        context.getSharedPreferences("vialapp_widget_onboarding",Context.MODE_PRIVATE)
    }
    val manager=remember(context){AppWidgetManager.getInstance(context)}
    val provider=remember(context){ComponentName(context,StudyWidget::class.java)}
    var visible by remember { mutableStateOf(!preferences.getBoolean("offer_answered",false)) }
    var manualHelp by rememberSaveable { mutableStateOf(false) }

    fun finish() {
        preferences.edit().putBoolean("offer_answered",true).apply()
        visible=false
    }

    LaunchedEffect(Unit) {
        if(visible && runCatching{manager.getAppWidgetIds(provider).isNotEmpty()}.getOrDefault(false)){
            finish()
        }
    }

    if(!visible)return

    if(manualHelp){
        AlertDialog(
            onDismissRequest={finish()},
            title={Text("Añade VialApp a tu inicio")},
            text={Text("Mantén pulsada una zona vacía de la pantalla de inicio de Android, selecciona Widgets y busca VialApp. Arrastra Tu progreso al lugar que prefieras.")},
            confirmButton={TextButton(onClick={finish()}){Text("Entendido")}}
        )
        return
    }

    AlertDialog(
        onDismissRequest={finish()},
        title={Text("Tu progreso, siempre a mano",fontWeight=FontWeight.ExtraBold)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
                Text("¿Quieres añadir el widget de VialApp a tu pantalla de inicio? Consulta tu nivel y tu racha, y practica con un toque.",
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF183549)).padding(14.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text("VIALAPP / TU RUTA",fontSize=10.sp,fontWeight=FontWeight.Bold,color=Color(0xFF87CDC4))
                            Text("Tu progreso",fontSize=21.sp,fontWeight=FontWeight.ExtraBold,color=Color.White)
                            Text("Racha y progreso diario",fontSize=12.sp,color=Color(0xFFF0C888))
                        }
                        Image(painter=painterResource(if(mascotId=="mister_pelao") R.drawable.mascot_mister_pelao else R.drawable.mascot_carvier),
                            contentDescription="Tu mascota",
                            modifier=Modifier.size(58.dp).clip(CircleShape)
                                .border(2.dp,Color(0xFF79CFBE),CircleShape),
                            contentScale=ContentScale.Crop)
                    }
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF406270)))
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                        Text("XP · Racha · Misión de hoy",fontSize=11.sp,color=Color(0xFFD3E4F8),modifier=Modifier.weight(1f))
                        Surface(shape=RoundedCornerShape(10.dp),color=Color(0xFFA5D5FF)){
                            Text("Practicar  ›",Modifier.padding(horizontal=12.dp,vertical=8.dp),fontSize=12.sp,
                                fontWeight=FontWeight.Bold,color=Color(0xFF102846))
                        }
                    }
                }
                Text("Vista previa · Puedes añadirlo más tarde desde Configuración de estudio.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton={
            Button(onClick={
                val alreadyAdded=runCatching{manager.getAppWidgetIds(provider).isNotEmpty()}.getOrDefault(false)
                if(alreadyAdded) finish()
                else {
                    val requested=runCatching{
                        manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(provider,null,null)
                    }.getOrDefault(false)
                    if(requested)finish() else manualHelp=true
                }
            }){Text("Añadir widget")}
        },
        dismissButton={TextButton(onClick={finish()}){Text("Ahora no")}}
    )
}
