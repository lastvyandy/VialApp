package cl.vialapp.app.ui.premium

// GUÍA DEL ARCHIVO:
// Muestra opciones Premium, pero la confirmación de beneficios la hace el backend.

import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cl.vialapp.app.data.api.*
import cl.vialapp.app.data.session.AuthenticatedRead
import cl.vialapp.app.data.session.SessionManager
import com.android.billingclient.api.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.security.MessageDigest

private val periods=listOf("daily" to "1 día","biweekly" to "15 días","monthly" to "30 días")
private val productIds=periods.flatMap{(period,_)->listOf("b","c","all").map{"premium_"+period+"_"+it}}.toSet()
private fun accountHash(id:String)=MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}

@Composable internal fun PremiumScreen(user:UserResponse,onUserUpdated:(UserResponse)->Unit,onBack:()->Unit){
    val context=LocalContext.current
    val session=remember(context){SessionManager(context)}
    val scope=rememberCoroutineScope()
    val currentUpdate by rememberUpdatedState(onUserUpdated)
    val account=remember(user.id){accountHash(user.id)}
    var enabled by remember{mutableStateOf<Boolean?>(null)}
    var products by remember{mutableStateOf<Map<String,ProductDetails>>(emptyMap())}
    var connected by remember{mutableStateOf(false)}
    var loading by remember{mutableStateOf(true)}
    var purchasing by remember{mutableStateOf(false)}
    var verifying by remember{mutableIntStateOf(0)}
    var message by remember{mutableStateOf("")}
    var license by remember{mutableStateOf(if(user.licenseClass=="C")"c" else "b")}
    var refresh by remember{mutableIntStateOf(0)}
    val inflight=remember(user.id){mutableSetOf<String>()}
    fun process(purchases:List<Purchase>){
        scope.launch{
            for(purchase in purchases){
                if(purchase.accountIdentifiers?.obfuscatedAccountId!=account)continue
                if(purchase.purchaseState==Purchase.PurchaseState.PENDING){message="Pago pendiente de Google Play. Se activará tras la confirmación.";continue}
                if(purchase.purchaseState!=Purchase.PurchaseState.PURCHASED)continue
                val id=purchase.products.singleOrNull()?.takeIf{it in productIds}?:continue
                if(!inflight.add(purchase.purchaseToken))continue
                verifying++
                try{
                    // The existing backend verifies the exact account, token and product and consumes only after granting access.
                    val token=session.getAccessToken()?.takeIf{it.isNotBlank()}?:error("Sesión vencida")
                    val result=ApiClient.api.verifyPlayPurchase("Bearer "+token,PlayPurchaseVerify(id,purchase.purchaseToken))
                    if(result.isSuccessful&&result.body()?.premiumActivated==true){
                        message=if(result.body()?.consumptionPending==true)"Premium acreditado. Sincroniza para habilitar otra compra." else "¡Premium activado!"
                        val profile=AuthenticatedRead.get(session,"premium-profile"){ApiClient.api.getMe(it)}
                        profile.body()?.takeIf{profile.isSuccessful&&it.id==user.id}?.let{currentUpdate(it)}
                    }else{
                        message=if(result.code()==401)"Tu sesión venció. Inicia sesión de nuevo y sincroniza tus compras." else "La compra sigue pendiente de verificación del servidor. Usa Sincronizar compras; no pagues de nuevo."
                    }
                }catch(c:CancellationException){throw c}
                catch(_:Exception){message="No se pudo verificar la compra. Sincroniza cuando tengas conexión."}
                finally{inflight.remove(purchase.purchaseToken);verifying--}
            }
        }
    }
    val updatePurchases by rememberUpdatedState<(List<Purchase>)->Unit>({process(it)})
    val client=remember(user.id){
        BillingClient.newBuilder(context.applicationContext)
            .setListener{result,purchases->
                scope.launch{
                    purchasing=false
                    when(result.responseCode){
                        BillingClient.BillingResponseCode.OK->updatePurchases(purchases.orEmpty())
                        BillingClient.BillingResponseCode.USER_CANCELED->message="Compra cancelada."
                        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED->{message="Hay una compra sin sincronizar. Usa Sincronizar compras."}
                        else->message="Google Play no pudo iniciar la compra."
                    }
                }
            }.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection().build()
    }
    fun sync(){
        if(!client.isReady)return
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()){result,purchases->
            scope.launch{if(result.responseCode==BillingClient.BillingResponseCode.OK)updatePurchases(purchases)else message="No se pudieron consultar tus compras."}
        }
    }
    fun query(){
        if(!client.isReady)return
        loading=true
        val params=QueryProductDetailsParams.newBuilder().setProductList(productIds.map{QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(BillingClient.ProductType.INAPP).build()}).build()
        client.queryProductDetailsAsync(params){result,details->
            scope.launch{
                products=if(result.responseCode==BillingClient.BillingResponseCode.OK)details.productDetailsList.associateBy{it.productId} else emptyMap()
                loading=false
                if(products.isEmpty())message="No hay planes disponibles en Google Play. Comprueba la tienda o la pista de pruebas."
                sync()
            }
        }
    }
    DisposableEffect(client){onDispose{client.endConnection()}}
    LaunchedEffect(refresh){
        loading=true;message=""
        try{
            val flag=ApiClient.api.getPublicFeatures()
            enabled=flag.body()?.let{it.paymentsEnabled&&it.googlePlayEnabled}?.takeIf{flag.isSuccessful}
            if(enabled!=true){products=emptyMap();loading=false;if(enabled==null)message="No se pudo consultar la disponibilidad de los pagos.";return@LaunchedEffect}
            if(client.isReady){connected=true;query()}
            else client.startConnection(object:BillingClientStateListener{
                override fun onBillingSetupFinished(result:BillingResult){
                    scope.launch{connected=result.responseCode==BillingClient.BillingResponseCode.OK;if(connected)query()else{loading=false;message="No se pudo conectar a Google Play."}}
                }
                override fun onBillingServiceDisconnected(){scope.launch{connected=false;loading=false;message="Google Play se desconectó. Reintenta."}}
            })
        }catch(c:CancellationException){throw c}
        catch(_:Exception){loading=false;message="No se pudo consultar la disponibilidad de los pagos."}
    }
    var ctx=context
    while(ctx is ContextWrapper&&ctx !is Activity)ctx=ctx.baseContext
    val activity=ctx as? Activity
    DisposableEffect(activity,client){
        val lifecycle=(activity as? LifecycleOwner)?.lifecycle
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME&&client.isReady)sync()}
        lifecycle?.addObserver(observer)
        onDispose{lifecycle?.removeObserver(observer)}
    }
    fun buy(product:ProductDetails){
        val act=activity?:return
        if(enabled!=true||!connected||!client.isReady||purchasing||verifying>0)return
        val offer=product.oneTimePurchaseOfferDetails?:return
        val details=BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
        offer.offerToken?.takeIf{it.isNotBlank()}?.let{details.setOfferToken(it)}
        purchasing=true
        val result=client.launchBillingFlow(act,BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(details.build())).setObfuscatedAccountId(account).build())
        if(result.responseCode!=BillingClient.BillingResponseCode.OK){purchasing=false;message="No se pudo abrir el pago de Google Play."}
    }
    BackHandler{onBack()}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        TextButton(onClick=onBack){Text("‹ Perfil")}
        Text("VialApp Premium",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold)
        Text("Acceso a exámenes, correcciones y práctica Premium. Pago único por el período que elijas, sin renovación automática.")
        if(user.premiumLicenseClasses.isNotEmpty())Text("Acceso activo: "+user.premiumLicenseClasses.joinToString(", "){ "Clase "+it })
        if(enabled==false)Text("Los pagos desde Google Play todavía no están habilitados.")
        if(loading||verifying>0){LinearProgressIndicator(Modifier.fillMaxWidth());if(verifying>0)Text("Verificando compra…")}
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("b" to "Clase B","c" to "Clase C","all" to "Ambas").forEach{(id,label)->FilterChip(selected=license==id,onClick={license=id},label={Text(label)})}
        }
        if(enabled==true)for((id,label) in periods){
            val product=products["premium_"+id+"_"+license]
            val price=product?.oneTimePurchaseOfferDetails?.formattedPrice
            Card(Modifier.fillMaxWidth()){
                Row(Modifier.fillMaxWidth().padding(15.dp),verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){Text(label,fontWeight=FontWeight.Bold);Text(price?:"No disponible")}
                    Button(onClick={product?.let{buy(it)}},enabled=price!=null&&connected&&!purchasing&&verifying==0){Text("Comprar")}
                }
            }
        }
        if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick={if(client.isReady)sync()else refresh++},enabled=enabled!=false&&!purchasing&&verifying==0,modifier=Modifier.fillMaxWidth()){Text("Sincronizar compras")}
        TextButton(onClick={refresh++},enabled=!purchasing&&verifying==0){Text("Actualizar disponibilidad")}
    }
}
