package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class RegistrationRequest(
    val email:String,val password:String,
    @SerializedName("accept_terms") val acceptTerms:Boolean,
    @SerializedName("confirm_age_18") val confirmAge18:Boolean
)
data class PasswordResetRequest(val email:String)
data class PasswordResetMessage(val message:String)

data class PublicFeatures(
    @SerializedName("payments_enabled") val paymentsEnabled:Boolean,
    @SerializedName("google_client_id") val googleClientId:String?=null
)
data class GoogleLoginRequest(
    val token:String,
    @SerializedName("accept_terms") val acceptTerms:Boolean=false,
    @SerializedName("confirm_age_18") val confirmAge18:Boolean=false
)
data class GoogleLinkRequest(val token:String,@SerializedName("current_password") val currentPassword:String)
data class PlayPurchaseVerify(@SerializedName("product_id") val productId:String,
    @SerializedName("purchase_token") val purchaseToken:String)
data class PlayPurchaseResult(val status:String,
    @SerializedName("premium_activated") val premiumActivated:Boolean,
    @SerializedName("consumption_pending") val consumptionPending:Boolean)
