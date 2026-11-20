package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Guardamos cuál mascota eligió el usuario, sin mezclarla con el avatar del perfil.

import com.google.gson.annotations.SerializedName

data class MascotSelectionRequest(
    @SerializedName("mascot_id") val mascotId: String
)
