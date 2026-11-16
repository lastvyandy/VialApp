package cl.vialapp.app.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.vialapp.app.R
import cl.vialapp.app.data.api.*
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AuthScreen(onLoginSuccess:(UserResponse)->Unit,onOfflineStudy:()->Unit){
    val context=LocalContext.current
    val session=remember(context){SessionManager(context)}
    val scope=rememberCoroutineScope()
    val c=MaterialTheme.colorScheme
    var mode by rememberSaveable{mutableStateOf("login")}
    var email by rememberSaveable{mutableStateOf("")}
    // Credentials stay in memory, not the saved-instance-state bundle.
    var password by remember{mutableStateOf("")}
    var confirmation by remember{mutableStateOf("")}
    var visible by remember{mutableStateOf(false)}
    var terms by rememberSaveable{mutableStateOf(false)}
    var age by rememberSaveable{mutableStateOf(false)}
    var loading by remember{mutableStateOf(false)}
    var attempted by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    var notice by rememberSaveable{mutableStateOf("")}
    var googleId by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(Unit){try{val result=ApiClient.api.getPublicFeatures();if(result.isSuccessful)googleId=result.body()?.googleClientId}catch(c:CancellationException){throw c}catch(_:Exception){}}
    val registering=mode=="register"
    val resetting=mode=="reset"
    val emailValid=android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passError=if(registering)registrationPasswordError(password) else if(password.isBlank())"Ingresa tu contraseña." else null
    fun switch(next:String){
        if(loading)return
        mode=next;password="";confirmation="";visible=false;attempted=false;error="";notice=""
        terms=false;age=false
    }
    fun link(path:String){
        runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://vialapp.cl/"+path)))}
            .onFailure{error="No se pudo abrir el navegador."}
    }
    fun google(){
        if(loading)return
        val clientId=googleId
        if(clientId.isNullOrBlank()){error="Google no está disponible en este momento.";return}
        if(registering&&(!age||!terms)){attempted=true;error="Confirma tu edad y acepta los términos para crear la cuenta con Google.";return}
        loading=true;error="";notice=""
        scope.launch{
            try{
                val token=GoogleIdentity.getToken(context,clientId)
                val response=ApiClient.api.googleLogin(GoogleLoginRequest(token,registering&&terms,registering&&age))
                val tokens=response.body()
                if(!response.isSuccessful||tokens==null||tokens.accessToken.isBlank()){
                    error=when(response.code()){
                        409->"Esta cuenta ya existe. Inicia sesión con contraseña y vincula Google desde tu perfil."
                        400->if(!registering)"Si es tu primera vez, elige Crear cuenta y acepta los términos antes de continuar con Google." else "No se pudo verificar Google. Revisa tu cuenta e intenta nuevamente."
                        else->authResponseError(response,AuthAction.LOGIN)
                    };return@launch
                }
                session.saveAccessToken(tokens.accessToken)
                tokens.csrfToken?.let{session.saveCsrfToken(it)}
                val profile=ApiClient.api.getMe("Bearer "+tokens.accessToken)
                val user=profile.body()
                if(profile.isSuccessful&&user!=null){password="";onLoginSuccess(user)}
                else{session.clearSession();ApiClient.clearCookies();error="No se pudo cargar tu perfil. Intenta nuevamente."}
            }catch(c:CancellationException){throw c}
            catch(c:GoogleSignInCancelled){/* user closed chooser */}
            catch(c:GoogleSignInUnavailable){error="No se pudo iniciar sesión con Google. Comprueba Google Play Services."}
            catch(c:Exception){error=authNetworkError(c)}
            finally{loading=false}
        }
    }
    fun submit(){
        if(loading)return
        attempted=true;error="";notice=""
        if(!emailValid)return
        if(!resetting&&passError!=null)return
        if(registering&&(confirmation!=password||!terms||!age))return
        loading=true
        val action=when(mode){"register"->AuthAction.REGISTER;"reset"->AuthAction.RESET;else->AuthAction.LOGIN}
        scope.launch{
            try{
                when(action){
                    AuthAction.REGISTER->{
                        val result=ApiClient.api.register(RegistrationRequest(email.trim(),password,terms,age))
                        if(result.isSuccessful){
                            mode="login";password="";confirmation="";attempted=false;visible=false
                            notice="¡Cuenta creada! Inicia sesión para completar tu perfil."
                        }else error=authResponseError(result,action)
                    }
                    AuthAction.RESET->{
                        val result=ApiClient.api.requestPasswordReset(PasswordResetRequest(email.trim()))
                        if(result.isSuccessful)notice="Si existe una cuenta asociada al correo, recibirás instrucciones para recuperar tu contraseña."
                        else error=authResponseError(result,action)
                    }
                    else->{
                        val result=ApiClient.api.login(email.trim(),password)
                        val tokens=result.body()
                        if(!result.isSuccessful){error=authResponseError(result,action);return@launch}
                        if(tokens==null||tokens.accessToken.isBlank()){error="No se recibió una sesión válida. Intenta nuevamente.";return@launch}
                        session.saveAccessToken(tokens.accessToken)
                        tokens.csrfToken?.let{session.saveCsrfToken(it)}
                        val profile=ApiClient.api.getMe("Bearer "+tokens.accessToken)
                        val user=profile.body()
                        if(profile.isSuccessful&&user!=null){password="";onLoginSuccess(user)}
                        else{
                            if(profile.code() in listOf(401,403)){session.clearSession();ApiClient.clearCookies()}
                            error=if(profile.isSuccessful)"No fue posible cargar tu perfil. Intenta nuevamente." else authResponseError(profile,AuthAction.PROFILE)
                        }
                    }
                }
            }catch(cancelled:CancellationException){throw cancelled}
            catch(e:Exception){error=authNetworkError(e)}
            finally{loading=false}
        }
    }
    BackHandler(enabled=mode!="login"||loading){if(!loading)switch("login")}
    Surface(Modifier.fillMaxSize(),color=c.background){
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=18.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(13.dp)){
            if(mode=="login")TextButton(onClick=onOfflineStudy,enabled=!loading,modifier=Modifier.align(Alignment.End)){Text("Estudiar sin conexión")}
            else TextButton(onClick={switch("login")},enabled=!loading,modifier=Modifier.align(Alignment.Start)){Text("‹ Iniciar sesión")}
            Image(painterResource(R.drawable.vialapp_brand_logo),"VialApp",modifier=Modifier.size(if(registering)100.dp else 144.dp),contentScale=ContentScale.Fit)
            Text(when(mode){"register"->"Crea tu cuenta";"reset"->"Recupera tu contraseña";else->"Bienvenido a VialApp"},fontSize=26.sp,fontWeight=FontWeight.ExtraBold,color=c.onSurface,modifier=Modifier.fillMaxWidth())
            Text(when(mode){"register"->"Comienza tu camino a la licencia.";"reset"->"Ingresa el correo de tu cuenta.";else->"Ingresa a tu cuenta para continuar."},color=c.onSurfaceVariant,modifier=Modifier.fillMaxWidth())
            if(notice.isNotEmpty())Surface(color=c.primaryContainer,shape=RoundedCornerShape(14.dp)){
                Text(notice,Modifier.fillMaxWidth().padding(14.dp),color=c.onPrimaryContainer)
            }
            OutlinedTextField(value=email,onValueChange={email=it;error=""},enabled=!loading,modifier=Modifier.fillMaxWidth(),
                label={Text("Correo electrónico")},singleLine=true,shape=RoundedCornerShape(14.dp),
                isError=attempted&&!emailValid,supportingText=if(attempted&&!emailValid){{Text("Ingresa un correo electrónico válido.")}}else null,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,imeAction=if(resetting)ImeAction.Done else ImeAction.Next))
            if(!resetting){
                OutlinedTextField(value=password,onValueChange={password=it;error=""},enabled=!loading,modifier=Modifier.fillMaxWidth(),
                    label={Text("Contraseña")},singleLine=true,shape=RoundedCornerShape(14.dp),isError=attempted&&passError!=null,
                    supportingText=if(registering||attempted&&passError!=null){{Text(if(attempted&&passError!=null)passError?:"" else "Mínimo 8 caracteres.")}}else null,
                    visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon={TextButton(onClick={visible=!visible},enabled=!loading){Text(if(visible)"Ocultar" else "Mostrar")}},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=if(registering)ImeAction.Next else ImeAction.Done))
            }
            if(registering){
                OutlinedTextField(value=confirmation,onValueChange={confirmation=it;error=""},enabled=!loading,modifier=Modifier.fillMaxWidth(),label={Text("Confirmar contraseña")},
                    singleLine=true,shape=RoundedCornerShape(14.dp),visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),
                    isError=attempted&&(confirmation!=password||confirmation.isEmpty()),
                    supportingText=if(attempted&&confirmation!=password){{Text("Las contraseñas no coinciden.")}}else null,
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done))
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=age,onCheckedChange={age=it},enabled=!loading);Text("Confirmo que tengo 18 años o más.",color=c.onSurface,modifier=Modifier.weight(1f))}
                if(attempted&&!age)Text("Confirma tu edad para continuar.",color=c.error,fontSize=12.sp)
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=terms,onCheckedChange={terms=it},enabled=!loading);Text("Acepto los términos y declaro que leí la política de privacidad.",color=c.onSurface,modifier=Modifier.weight(1f))}
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    TextButton(onClick={link("terminos-y-condiciones")},modifier=Modifier.weight(1f)){Text("Términos")}
                    TextButton(onClick={link("politica-de-privacidad")},modifier=Modifier.weight(1f)){Text("Privacidad")}
                }
                if(attempted&&!terms)Text("Debes aceptar los términos y la política.",color=c.error,fontSize=12.sp)
            }else if(!resetting)TextButton(onClick={switch("reset")},enabled=!loading,modifier=Modifier.align(Alignment.End)){Text("¿Olvidaste tu contraseña?")}
            if(error.isNotBlank())Surface(color=c.errorContainer,shape=RoundedCornerShape(14.dp)){
                Text(error,Modifier.fillMaxWidth().padding(14.dp),color=c.onErrorContainer,fontSize=14.sp)
            }
            Button(onClick=::submit,enabled=!loading,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=RoundedCornerShape(15.dp)){
                if(loading){CircularProgressIndicator(Modifier.size(19.dp),color=c.onPrimary,strokeWidth=2.dp);Spacer(Modifier.width(10.dp))}
                Text(if(loading)when(mode){"register"->"Creando cuenta…";"reset"->"Enviando…";else->"Iniciando sesión…"}else when(mode){"register"->"Crear cuenta";"reset"->"Enviar instrucciones";else->"Iniciar sesión"},fontWeight=FontWeight.Bold)
            }
            if(!resetting&&!googleId.isNullOrBlank()){
                OutlinedButton(onClick=::google,enabled=!loading,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(15.dp)){
                    Image(painterResource(R.drawable.ic_google_sign_in),null,modifier=Modifier.size(20.dp))
                    Spacer(Modifier.width(11.dp))
                    Text(if(registering)"Registrarse con Google" else "Continuar con Google",fontWeight=FontWeight.SemiBold)
                }
            }
            if(mode=="login")TextButton(onClick={switch("register")},enabled=!loading){Text("¿No tienes cuenta? Crear cuenta")}
            Text("VialApp · Educación vial",fontSize=12.sp,color=c.onSurfaceVariant)
        }
    }
}
