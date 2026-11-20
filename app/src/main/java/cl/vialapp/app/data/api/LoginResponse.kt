package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Respuesta de autenticación; después la sesión se encarga de los tokens.

import com.google.gson.annotations.SerializedName

data class LoginResponse(

    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("csrf_token")
    val csrfToken: String?
)