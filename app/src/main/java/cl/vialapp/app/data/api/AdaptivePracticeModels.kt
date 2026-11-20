package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Datos de práctica personalizada separados de la interfaz.

import com.google.gson.annotations.SerializedName

data class AdaptivePracticeSession(
    @SerializedName("attempt_token") val attemptToken: String,
    val questions: List<ExamQuestion>
)

data class AdaptivePracticeSubmission(
    @SerializedName("attempt_token") val attemptToken: String,
    val answers: List<ExamAnswer>
)

data class AdaptivePracticeResult(
    val correctas: Int,
    @SerializedName("total_preguntas") val totalQuestions: Int,
    val resultados: List<ExamQuestionResult>,
    @SerializedName("xp_awarded") val xpAwarded: Int,
    val duplicate: Boolean,
    val gamification: GamificationProgressResponse
)
