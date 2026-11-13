package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class LicenseClassUpdateRequest(
    @SerializedName("license_class") val licenseClass: String
)
