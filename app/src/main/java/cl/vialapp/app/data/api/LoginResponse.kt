package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class LoginResponse(

    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("csrf_token")
    val csrfToken: String?
)