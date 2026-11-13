package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class MascotSelectionRequest(
    @SerializedName("mascot_id") val mascotId: String
)
