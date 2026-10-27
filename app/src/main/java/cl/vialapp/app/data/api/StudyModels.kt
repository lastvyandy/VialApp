package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class StudyTestResponse(
    val id: Int? = null,
    val nombre: String? = null,
    val categoria: String? = null,
    @SerializedName("clase_licencia")
    val licenseClass: String? = null,
    val preguntas: List<StudyQuestion> = emptyList()
)

data class StudyQuestion(
    val id: Int = 0,

    @SerializedName("nro_pregunta")
    val questionNumber: Int = 0,

    val texto: String = "",

    val alternativas: Map<String, String> = emptyMap(),

    @SerializedName("clase_licencia")
    val licenseClass: String = "",

    val categoria: String = "",

    @SerializedName("imagen_url")
    val imageUrl: String? = null,

    @SerializedName("is_multiple")
    val isMultiple: Boolean = false,

    @SerializedName("correct_answers")
    val correctAnswers: List<String> = emptyList(),

    val explicacion: String = ""
)
