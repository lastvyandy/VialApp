package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Datos del perfil recibidos desde la API; no deberían contener secretos.

import com.google.gson.annotations.SerializedName

data class UserResponse(

    val id: String,

    val username: String,

    val email: String,

    @SerializedName("first_name")
    val firstName: String?,

    @SerializedName("last_name")
    val lastName: String?,

    @SerializedName("license_class")
    val licenseClass: String?,

    @SerializedName("profile_completed")
    val profileCompleted: Boolean,


    @SerializedName("is_premium")
    val isPremium: Boolean,

    @SerializedName("premium_license_classes")
    val premiumLicenseClasses: List<String> = emptyList(),

    @SerializedName("mascot_id")
    val mascotId: String? = null,
    @SerializedName("google_linked") val googleLinked:Boolean=false
)