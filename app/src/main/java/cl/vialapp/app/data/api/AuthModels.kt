package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class RegistrationRequest(
    val email:String,val password:String,
    @SerializedName("accept_terms") val acceptTerms:Boolean,
    @SerializedName("confirm_age_18") val confirmAge18:Boolean
)
data class PasswordResetRequest(val email:String)
data class PasswordResetMessage(val message:String)
