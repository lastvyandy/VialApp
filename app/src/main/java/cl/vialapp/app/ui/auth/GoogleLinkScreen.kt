package cl.vialapp.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.vialapp.app.data.api.*
import cl.vialapp.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable internal fun GoogleLinkScreen(user:UserResponse,onUserUpdated:(UserResponse)->Unit,onBack:()->Unit){
    val context=LocalContext.current
    val session=remember(context){SessionManager(context)}
    val scope=rememberCoroutineScope()
    var pass by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        TextButton(onClick=onBack,enabled=!busy){Text("‹ Perfil")}
        Text("Vincular Google",style=MaterialTheme.typography.headlineMedium)
        Text("Usa una cuenta Google con el mismo correo que "+user.email+". Conserva tu progreso y tu Premium.")
        if(user.googleLinked)Text("Google ya está vinculado.")
        else{
            OutlinedTextField(pass,{pass=it},modifier=Modifier.fillMaxWidth(),enabled=!busy,singleLine=true,label={Text("Contraseña actual")},visualTransformation=PasswordVisualTransformation())
            Button(onClick={
                busy=true;message=""
                scope.launch{
                    try{
                        val id=ApiClient.api.getPublicFeatures().body()?.googleClientId
                        if(id.isNullOrBlank()){message="Google no está configurado.";return@launch}
                        val sessionToken=session.getAccessToken()
                        if(sessionToken.isNullOrBlank()){message="Inicia sesión nuevamente.";return@launch}
                        val token=GoogleIdentity.getToken(context,id)
                        val response=ApiClient.api.linkGoogle("Bearer "+sessionToken,GoogleLinkRequest(token,pass))
                        val updated=response.body()
                        if(response.isSuccessful&&updated!=null&&updated.id==user.id){pass="";onUserUpdated(updated);onBack()}
                        else message=when(response.code()){401->"Contraseña incorrecta o sesión vencida.";409->"La identidad Google ya está vinculada.";400->"Verifica que el correo Google coincida.";else->"No se pudo vincular Google."}
                    }catch(c:CancellationException){throw c}
                    catch(c:GoogleSignInCancelled){}
                    catch(_:Exception){message="No se pudo completar la vinculación."}
                    finally{busy=false}
                }
            },enabled=!busy&&pass.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text(if(busy)"Vinculando…" else "Vincular con Google")}
        }
        if(message.isNotBlank())Text(message,color=MaterialTheme.colorScheme.error)
    }
}
