package cl.vialapp.app.ui.career

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.CareerState
import cl.vialapp.app.data.api.GarageVehicle
import cl.vialapp.app.data.api.GarageAccessory

private val hero=Color(0xFF101C2A)
private val panel=Color(0xFF1C2E41)
private val pale=Color(0xFFCFDEEF)
private val accent=Color(0xFF8CD5FF)
private val gold=Color(0xFFFFD88C)

@Composable
internal fun GarageShowroom(data:CareerState,enabled:Boolean,
    onAction:(String,String)->Unit,onPurchase:(String,String,String,Int)->Unit,
    onDrive:()->Unit) {
    val c=MaterialTheme.colorScheme
    var carId by rememberSaveable { mutableStateOf(data.equippedVehicle) }
    var paintId by rememberSaveable { mutableStateOf(data.equippedPaint) }
    var angle by rememberSaveable { mutableFloatStateOf(38f) }
    val car=data.vehicles.firstOrNull{it.id==carId} ?: data.vehicles.firstOrNull{it.id==data.equippedVehicle}
    val paint=data.paints.firstOrNull{it.id==paintId} ?: data.paints.firstOrNull{it.id==data.equippedPaint}
    val activeVehicle=data.vehicles.firstOrNull{it.id==data.equippedVehicle}
    val activePaint=data.paints.firstOrNull{it.id==data.equippedPaint}
    val color=garageColor(paint?.hex?:"#38A9F0")
    var slot by rememberSaveable { mutableStateOf("llantas") }
    var accessoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val accessories=data.accessories.orEmpty()
    val chosenAccessory=accessories.firstOrNull{it.id==accessoryId&&it.slot==slot}
        ?: accessories.firstOrNull{it.id==data.equippedAccessories.orEmpty()[slot]}
        ?: accessories.firstOrNull{it.slot==slot}
    val equippedKit=garageKit(data.equippedAccessories)
    val kit=garageKit(data.equippedAccessories.orEmpty() + (chosenAccessory?.let{mapOf(it.slot to it.id)} ?: emptyMap()))
    val previewIsActive=car?.id==data.equippedVehicle && paint?.id==data.equippedPaint && kit==equippedKit
    fun turn(delta:Float){angle=((angle+delta)%360f+360f)%360f}
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(16.dp)){
        if(car!=null) {
            Card(shape=RoundedCornerShape(25.dp),colors=CardDefaults.cardColors(containerColor=hero),
                border=BorderStroke(1.dp,Color(0xFF344D65))) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal=17.dp,vertical=14.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
                            Text("VIAL PERFORMANCE / SHOWROOM",fontSize=10.sp,color=accent,fontWeight=FontWeight.Bold,letterSpacing=1.1.sp)
                            Text(car.name,fontSize=26.sp,color=Color.White,fontWeight=FontWeight.ExtraBold)
                            Text(carType(car.style)+" · "+(paint?.name?:"Azul Vial"),fontSize=13.sp,color=pale)
                        }
                        Surface(shape=RoundedCornerShape(12.dp),color=panel) {
                            Row(Modifier.padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                Icon(painterResource(R.drawable.ic_vial_coin),null,tint=gold,modifier=Modifier.size(18.dp))
                                Text(data.coins.toString(),color=gold,fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                    ShowroomView(car.style,color,angle,::turn,modifier=Modifier.fillMaxWidth(),kit=kit)
                    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        TextButton(onClick={turn(-45f)},modifier=Modifier.semantics{contentDescription="Girar auto a la izquierda"},
                            colors=ButtonDefaults.textButtonColors(contentColor=accent)){Text("‹",fontSize=27.sp)}
                        Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text("VISTA 360°",color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                            Text("Arrastra para girar",color=pale,fontSize=11.sp)
                        }
                        TextButton(onClick={turn(45f)},modifier=Modifier.semantics{contentDescription="Girar auto a la derecha"},
                            colors=ButtonDefaults.textButtonColors(contentColor=accent)){Text("›",fontSize=27.sp)}
                    }
                    TextButton(onClick={angle=38f},modifier=Modifier.align(Alignment.CenterHorizontally),
                        colors=ButtonDefaults.textButtonColors(contentColor=accent)){Text("Restablecer vista",fontSize=12.sp)}
                    HorizontalDivider(color=Color(0xFF344D65))
                    Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        val active=car.id==data.equippedVehicle
                        Text(when {
                            active -> "VEHÍCULO EQUIPADO"
                            car.owned -> "EN TU COLECCIÓN"
                            !car.available -> "DESBLOQUEA "+car.requiredStages+" ETAPAS DE CARRERA"
                            else -> car.price.toString()+" MONEDAS"
                        },color=if(active)Color(0xFF86EFAC) else gold,fontWeight=FontWeight.Bold,fontSize=12.sp)
                        Button(onClick={if(car.owned)onAction("equip_vehicle",car.id) else onPurchase("buy_vehicle",car.id,car.name,car.price)},
                            enabled=enabled&&!active&&(car.owned||(car.available&&data.coins>=car.price)),
                            modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                            shape=RoundedCornerShape(13.dp),
                            colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=Color(0xFF102339),
                                disabledContainerColor=panel,disabledContentColor=pale)){
                            Text(when{
                                active -> "Equipado"
                                car.owned -> "Equipar vehículo"
                                !car.available -> "Vehículo bloqueado"
                                data.coins<car.price -> "Monedas insuficientes"
                                else -> "Comprar por "+car.price+" monedas"
                            },fontWeight=FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("Colección",color=c.onSurface,fontSize=21.sp,fontWeight=FontWeight.ExtraBold,modifier=Modifier.weight(1f))
                Text(data.vehicles.count{it.owned}.toString()+"/"+data.vehicles.size,color=c.primary,fontWeight=FontWeight.Bold)
            }
            LazyRow(horizontalArrangement=Arrangement.spacedBy(9.dp)){
                items(data.vehicles,key={it.id}){model->
                    VehicleTile(model,model.id==car?.id,model.id==data.equippedVehicle,color,
                        onClick={carId=model.id})
                }
            }
        }
        Surface(shape=RoundedCornerShape(20.dp),color=c.surface,border=BorderStroke(1.dp,c.outlineVariant)){
            Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text("Taller de pintura",fontSize=20.sp,fontWeight=FontWeight.ExtraBold,color=c.onSurface,modifier=Modifier.weight(1f))
                    Text(data.paints.count{it.owned}.toString()+"/"+data.paints.size,color=c.primary,fontWeight=FontWeight.Bold)
                }
                Text("Prueba cualquier color en el showroom.",color=c.onSurfaceVariant,fontSize=13.sp)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(11.dp),contentPadding=PaddingValues(vertical=3.dp)){
                    items(data.paints,key={it.id}){option->
                        val chosen=option.id==paint?.id
                        val swatch=garageColor(option.hex)
                        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Surface(onClick={paintId=option.id},modifier=Modifier.size(48.dp).semantics {
                                contentDescription=option.name+(if(option.owned)" disponible" else " en venta")
                                selected=chosen
                            },shape=CircleShape,color=c.surface,
                                border=BorderStroke(if(chosen)3.dp else 1.dp,if(chosen)c.primary else c.outlineVariant)){
                                Box(Modifier.padding(5.dp).clip(CircleShape).background(swatch),contentAlignment=Alignment.Center){
                                    if(option.id==data.equippedPaint)
                                        Icon(painterResource(R.drawable.ic_award_target),null,tint=Color(0xFF101827),modifier=Modifier.size(20.dp))
                                }
                            }
                            if(!option.owned) Text(option.price.toString(),fontSize=10.sp,color=c.onSurfaceVariant)
                            else Text("Tuyo",fontSize=10.sp,color=c.onSurfaceVariant)
                        }
                    }
                }
                if(paint!=null){
                    val equipped=paint.id==data.equippedPaint
                    Text(paint.name,color=c.onSurface,fontWeight=FontWeight.Bold)
                    Text(if(equipped)"Color equipado" else if(paint.owned)"Disponible para tu vehículo activo" else paint.price.toString()+" monedas",color=c.onSurfaceVariant,fontSize=12.sp)
                    OutlinedButton(onClick={if(paint.owned)onAction("equip_paint",paint.id) else onPurchase("buy_paint",paint.id,paint.name,paint.price)},
                        enabled=enabled&&!equipped&&(paint.owned||data.coins>=paint.price),modifier=Modifier.fillMaxWidth()){
                        Text(if(equipped)"Pintura activa" else if(paint.owned)"Aplicar pintura" else if(data.coins<paint.price)"Monedas insuficientes" else "Comprar pintura")
                    }
                }
            }
        }
        if(accessories.isNotEmpty()) {
            Surface(shape=RoundedCornerShape(20.dp),color=c.surface,border=BorderStroke(1.dp,c.outlineVariant)){
                Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Text("Taller de accesorios",fontSize=21.sp,fontWeight=FontWeight.ExtraBold,color=c.onSurface)
                    Text("Combina piezas y prueba su aspecto antes de comprarlas.",color=c.onSurfaceVariant,fontSize=13.sp)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        items(listOf("llantas" to "Llantas","vinilos" to "Vinilos","aleron" to "Alerones","neon" to "Neón"),key={it.first}){(key,label)->
                            FilterChip(selected=slot==key,onClick={slot=key;accessoryId=null},label={Text(label)})
                        }
                    }
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(9.dp)){
                        items(accessories.filter{it.slot==slot},key={it.id}){part->
                            val selected=part.id==chosenAccessory?.id
                            val equipped=part.id==data.equippedAccessories.orEmpty()[part.slot]
                            PartTile(part,selected,equipped,car?.style?:"compact",color,equippedKit,
                                onClick={accessoryId=part.id})
                        }
                    }
                    chosenAccessory?.let{part->
                        val equipped=part.id==data.equippedAccessories.orEmpty()[part.slot]
                        Text(part.name,fontWeight=FontWeight.Bold,color=c.onSurface)
                        Text(when{equipped->"Equipado";part.owned->"En tu colección";!part.available->"Completa "+part.requiredStages+" etapas de carrera";else->part.price.toString()+" monedas"},color=c.onSurfaceVariant,fontSize=13.sp)
                        Button(onClick={if(part.owned)onAction("equip_accessory",part.id) else onPurchase("buy_accessory",part.id,part.name,part.price)},
                            enabled=enabled&&!equipped&&(part.owned||(part.available&&data.coins>=part.price)),modifier=Modifier.fillMaxWidth()){
                            Text(when{equipped->"Equipado";part.owned->"Equipar accesorio";!part.available->"Bloqueado";data.coins<part.price->"Monedas insuficientes";else->"Comprar accesorio"})
                        }
                        if(kit!=equippedKit) TextButton(onClick={accessoryId=null},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Volver a la configuración equipada")}
                    }
                    Text("Accesorios cosméticos compartidos entre tus vehículos. El neón solo se simula en el juego.",fontSize=11.sp,color=c.onSurfaceVariant)
                }
            }
        }
        Surface(shape=RoundedCornerShape(19.dp),color=c.primaryContainer){
            Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
                    Icon(painterResource(R.drawable.ic_award_road),null,tint=c.onPrimaryContainer,modifier=Modifier.size(27.dp))
                    Column(Modifier.weight(1f)){
                        Text("Listo para la pista",fontWeight=FontWeight.Bold,color=c.onPrimaryContainer,fontSize=17.sp)
                        Text(zoneName(data.zone),fontSize=12.sp,color=c.onPrimaryContainer)
                    }
                }
                Text((activeVehicle?.name?:"Compacto")+" · "+(activePaint?.name?:"Azul Vial"),color=c.onPrimaryContainer,fontWeight=FontWeight.SemiBold)
                if(!previewIsActive) Text("Estás viendo una vista previa. En pista usarás el vehículo, la pintura y los accesorios equipados.",color=c.onPrimaryContainer,fontSize=12.sp)
                Button(onClick=onDrive,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),
                    shape=RoundedCornerShape(14.dp)){Text("Conducir en Carvier al volante",fontWeight=FontWeight.Bold)}
            }
        }
    }
}

private fun carType(style:String)=when(style){
    "hatch"->"Urban"
    "sedan"->"Touring"
    "suv"->"Adventure"
    "sport"->"Performance"
    else->"Street"
}

@Composable
private fun VehicleTile(model:GarageVehicle,chosen:Boolean,equipped:Boolean,color:Color,onClick:()->Unit){
    val c=MaterialTheme.colorScheme
    Surface(onClick=onClick,modifier=Modifier.width(143.dp).semantics{selected=chosen},
        shape=RoundedCornerShape(17.dp),color=if(chosen)c.primaryContainer else c.surface,
        border=BorderStroke(if(chosen)2.dp else 1.dp,if(chosen)c.primary else c.outlineVariant)){
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            ShowroomThumbnail(model.style,color,Modifier.fillMaxWidth())
            val fg=if(chosen)c.onPrimaryContainer else c.onSurface
            Text(model.name,color=fg,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1)
            Text(when{equipped->"Equipado";model.owned->"En colección";!model.available->"Etapa "+model.requiredStages;else->model.price.toString()+" monedas"},
                color=if(chosen)c.onPrimaryContainer else c.onSurfaceVariant,fontSize=11.sp,maxLines=1)
        }
    }
}

@Composable
private fun PartTile(part:GarageAccessory,chosen:Boolean,equipped:Boolean,style:String,color:Color,base:GarageKit,onClick:()->Unit){
    val c=MaterialTheme.colorScheme
    val preview=when(part.slot){
        "llantas"->base.copy(wheels=part.id)
        "vinilos"->base.copy(decal=part.id)
        "aleron"->base.copy(wing=part.id)
        else->base.copy(neon=part.id)
    }
    Surface(onClick=onClick,modifier=Modifier.width(148.dp).semantics{selected=chosen},
        shape=RoundedCornerShape(16.dp),color=if(chosen)c.primaryContainer else c.surface,
        border=BorderStroke(if(chosen)2.dp else 1.dp,if(chosen)c.primary else c.outlineVariant)){
        Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color(0xFF203246))){
                ShowroomThumbnail(style,color,Modifier.fillMaxWidth(),kit=preview)
            }
            Text(part.name,fontSize=13.sp,fontWeight=FontWeight.Bold,color=if(chosen)c.onPrimaryContainer else c.onSurface)
            Text(when{equipped->"Equipado";part.owned->"Tuyo";!part.available->"Etapa "+part.requiredStages;else->part.price.toString()+" monedas"},
                fontSize=11.sp,color=if(chosen)c.onPrimaryContainer else c.onSurfaceVariant)
        }
    }
}
