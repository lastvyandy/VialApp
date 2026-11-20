package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Petición para cambiar Clase B o C sin borrar lo que ya avanzó el alumno.

import com.google.gson.annotations.SerializedName

data class LicenseClassUpdateRequest(
    @SerializedName("license_class") val licenseClass: String
)
