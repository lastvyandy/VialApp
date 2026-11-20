package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Datos para completar el perfil sin crear otra cuenta por accidente.

import com.google.gson.annotations.SerializedName

data class CompleteProfileRequest(

    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String,

    @SerializedName("license_class")
    val licenseClass: String
)