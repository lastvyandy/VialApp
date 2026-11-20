package cl.vialapp.app.data.session

// GUÍA DEL ARCHIVO:
// Lecturas autenticadas. Si el token falla, lo manejamos consistente y no en cada pantalla a su manera.

import android.util.Log
import cl.vialapp.app.data.api.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

/** Retry safe GET requests once if the access token expired.
 * Do not automatically repeat reward claims, exam submissions or other writes.
 */
internal object AuthenticatedRead {
    private val refreshLock=Mutex()

    suspend fun <T> get(session:SessionManager, endpoint:String, read:suspend (String)->Response<T>):Response<T> {
        try {
            val token=session.getAccessToken()?.takeIf{it.isNotBlank()}
                ?: throw ReadFailure("Inicia sesión para continuar.")
            var result=read("Bearer "+token)
            if(result.code()==401){
                result.errorBody()?.close()
                val renewed=refreshLock.withLock {
                    val current=session.getAccessToken()?.takeIf{it.isNotBlank()}
                        ?: throw ReadFailure("Inicia sesión para continuar.")
                    if(current!=token) current
                    else {
                        val csrf=session.getCsrfToken()?.takeIf{it.isNotBlank()}
                            ?: throw ReadFailure("Tu sesión venció. Vuelve a iniciar sesión (HTTP 401).")
                        val response=ApiClient.api.refresh(csrf)
                        val body=response.body()
                        if(!response.isSuccessful || body==null){
                            response.errorBody()?.close()
                            Log.w("VialSync","Session refresh HTTP "+response.code())
                            throw ReadFailure(if(response.code() in listOf(401,403))
                                "Tu sesión venció. Vuelve a iniciar sesión (HTTP "+response.code()+")."
                                else "No pudimos renovar tu sesión (HTTP "+response.code()+").")
                        }
                        if(session.getAccessToken()!=token)
                            throw ReadFailure("La sesión cambió. Vuelve a abrir esta pantalla.")
                        session.saveAccessToken(body.accessToken)
                        body.csrfToken?.let{session.saveCsrfToken(it)}
                        body.accessToken
                    }
                }
                result=read("Bearer "+renewed)
            }
            if(!result.isSuccessful)Log.w("VialSync",endpoint+" HTTP "+result.code())
            return result
        }catch(e:CancellationException){throw e}
        catch(e:Exception){
            Log.w("VialSync",endpoint+" "+e.javaClass.simpleName)
            throw e
        }
    }
}

internal class ReadFailure(message:String):Exception(message)

internal fun readError(code:Int):String=when(code){
    401->"Tu sesión venció. Vuelve a iniciar sesión (HTTP 401)."
    403->"No tienes acceso a esta función (HTTP 403)."
    428->"Debes cambiar tu contraseña para continuar (HTTP 428)."
    429->"Demasiadas solicitudes. Inténtalo en un momento (HTTP 429)."
    in 500..599->"El servidor no pudo responder (HTTP "+code+")."
    else->"No se pudo consultar el servidor (HTTP "+code+")."
}
internal fun readError(e:Exception):String=when(e){
    is ReadFailure->e.message?:"No se pudo consultar tu sesión."
    is java.net.SocketTimeoutException->"El servidor tardó demasiado. Reintenta."
    is java.io.IOException->"No hay conexión con VialApp. Reintenta."
    else->"No se pudo procesar la respuesta ("+e.javaClass.simpleName+")."
}
