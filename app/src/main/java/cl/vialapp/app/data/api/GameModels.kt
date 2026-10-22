package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName


data class StudyTestResponse(

    val id: Int,

    val nombre: String,

    val categoria: String?,

    @SerializedName("clase_licencia")
    val licenseClass: String,

    @SerializedName("created_at")
    val createdAt: String,

    val preguntas: List<StudyQuestion>
)


data class StudyQuestion(

    val id: Int,

    @SerializedName("nro_pregunta")
    val questionNumber: Int,

    val texto: String,

    val alternativas: Map<String, String>,

    @SerializedName("clase_licencia")
    val licenseClass: String,

    val categoria: String,

    @SerializedName("imagen_url")
    val imageUrl: String?,

    @SerializedName("is_multiple")
    val isMultiple: Boolean,

    @SerializedName("correct_answers")
    val correctAnswers: List<String>,

    val explicacion: String
)