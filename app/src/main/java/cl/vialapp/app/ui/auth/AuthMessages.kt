package cl.vialapp.app.ui.auth

// GUÍA DEL ARCHIVO:
// Convertimos errores del backend en mensajes que el alumno pueda entender.

import com.google.gson.JsonParser
import java.util.Locale
import retrofit2.Response

internal enum class AuthAction { LOGIN, REGISTER, RESET, PROFILE }

internal fun registrationPasswordError(password:String):String?=when{
    password.isBlank()||password.length<8->"La contraseña debe tener al menos 8 caracteres."
    password.length>128||password.toByteArray(Charsets.UTF_8).size>72->"La contraseña es demasiado larga (máximo 72 bytes UTF-8)."
    else->null
}

internal fun authHttpMessage(code:Int,action:AuthAction,detail:String=""):String{
    val d=detail.lowercase(Locale.ROOT)
    return when{
        code==429->"Demasiados intentos. Espera unos minutos y vuelve a intentar."
        code in 500..599->"El servidor no está disponible en este momento. Intenta nuevamente."
        code==428->"Debes cambiar tu contraseña antes de continuar."
        code==401&&action==AuthAction.LOGIN->"Correo o contraseña incorrectos."
        code==401->"Tu sesión venció. Vuelve a iniciar sesión."
        code==403->"No pudimos autorizar esta solicitud."
        action==AuthAction.REGISTER&&code in listOf(400,409)&&(d.contains("ya está en uso")||d.contains("ya existe")||d.contains("ya está registrado"))->"Este correo ya está registrado. Inicia sesión o recupera tu contraseña."
        action==AuthAction.REGISTER&&code==409->"No se pudo crear la cuenta por un conflicto de datos. Revisa tu correo."
        action==AuthAction.REGISTER&&code==400&&d.contains("18 años")->"Debes confirmar que tienes 18 años o más."
        action==AuthAction.REGISTER&&code==400&&d.contains("términos")->"Debes aceptar los términos y declarar que leíste la política de privacidad."
        code==422&&d.contains("password")->"Revisa los requisitos de la contraseña."
        code==422&&d.contains("email")->"Ingresa un correo electrónico válido."
        code==400&&action==AuthAction.PROFILE&&d.contains("nombre")->"Ingresa tu nombre."
        code==400&&action==AuthAction.PROFILE&&d.contains("apellidos")->"Ingresa tus apellidos."
        code==400&&action==AuthAction.PROFILE&&d.contains("licencia")->"Selecciona una clase de licencia."
        code==400||code==422->"Revisa los datos ingresados e intenta nuevamente."
        else->"No se pudo completar la solicitud (HTTP "+code+")."
    }
}
internal fun authResponseError(response:Response<*>,action:AuthAction):String{
    val detail=runCatching{
        val raw=response.errorBody()?.use{it.string()}?:return@runCatching ""
        val node=JsonParser.parseString(raw).asJsonObject.get("detail")?:return@runCatching ""
        if(node.isJsonPrimitive)node.asString
        else if(node.isJsonArray)node.asJsonArray.joinToString(" "){v->
            val obj=v.asJsonObject
            obj.get("loc")?.toString().orEmpty()+" "+obj.get("msg")?.asString.orEmpty()
        }else ""
    }.getOrDefault("")
    return authHttpMessage(response.code(),action,detail)
}
internal fun authNetworkError(error:Exception):String=when(error){
    is java.net.SocketTimeoutException->"El servidor tardó demasiado. Intenta nuevamente."
    is java.io.IOException->"No se pudo conectar a VialApp. Revisa tu conexión a internet."
    else->"No se pudo procesar la respuesta. Intenta nuevamente."
}
