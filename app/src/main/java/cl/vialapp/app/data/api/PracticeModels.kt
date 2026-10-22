package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName


data class CategoryStatResponse(

    val categoria: String,

    @SerializedName("total_preguntas_respondidas")
    val totalAnswered: Int,

    val correctas: Int,

    val incorrectas: Int,

    @SerializedName("porcentaje_error")
    val errorPercentage: Int,

    @SerializedName("porcentaje_acierto")
    val successPercentage: Int
)