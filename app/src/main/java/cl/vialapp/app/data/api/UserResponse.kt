package cl.vialapp.app.data.api

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
    val isPremium: Boolean
)