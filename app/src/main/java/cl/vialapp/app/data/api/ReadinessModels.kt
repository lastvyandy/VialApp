package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Preparación estimada; no significa que alguien tenga asegurado aprobar el examen real.

import com.google.gson.annotations.SerializedName

data class ExamReadinessResponse(
    @SerializedName("license_class") val licenseClass: String,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("distinct_answered") val distinctAnswered: Int,
    val coverage: Int,
    val accuracy: Int?,
    val score: Int?,
    @SerializedName("minimum_questions") val minimumQuestions: Int,
    @SerializedName("has_enough_data") val hasEnoughData: Boolean,
    val categories: List<ReadinessCategory>
)

data class ReadinessCategory(
    val category: String,
    @SerializedName("total_questions") val totalQuestions: Int,
    @SerializedName("distinct_answered") val distinctAnswered: Int,
    val coverage: Int,
    val accuracy: Int?,
    val score: Int?
)
