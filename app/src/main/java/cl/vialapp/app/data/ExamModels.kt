package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName


data class StartExamResponse(
    val attempt: ExamAttemptResponse,

    // Este es el examen COMPLETO con sus preguntas
    val test: ExamDetailResponse,

    val conflict: Boolean
)


data class ExamAttemptResponse(
    val id: Int,

    @SerializedName("attempt_id")
    val attemptId: Int,

    @SerializedName("test_id")
    val testId: Int,

    @SerializedName("test_result_id")
    val testResultId: Int?,

    val status: String,

    val answers: List<ExamAnswer>,

    @SerializedName("started_at")
    val startedAt: String?,

    @SerializedName("expires_at")
    val expiresAt: String?,

    @SerializedName("last_saved_at")
    val lastSavedAt: String?,

    @SerializedName("finished_at")
    val finishedAt: String?,

    @SerializedName("duration_seconds")
    val durationSeconds: Int,

    @SerializedName("autosave_interval_seconds")
    val autosaveIntervalSeconds: Int,

    // Este aparece en /tests/active-attempt
    // y es solamente un resumen
    val test: ExamTestSummary? = null
)


data class ExamTestSummary(
    val id: Int,

    val nombre: String,

    val categoria: String?,

    @SerializedName("clase_licencia")
    val licenseClass: String
)


data class ExamDetailResponse(
    val id: Int,

    val nombre: String,

    val categoria: String?,

    @SerializedName("clase_licencia")
    val licenseClass: String,

    @SerializedName("created_at")
    val createdAt: String,

    val preguntas: List<ExamQuestion>
)


data class ExamQuestion(
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
    val isMultiple: Boolean
)


data class ExamAnswer(

    @SerializedName("question_id")
    val questionId: Int,

    val respuestas: List<String>
)


data class SaveAnswersRequest(
    val answers: List<ExamAnswer>
)


data class SubmitExamRequest(

    val answers: List<ExamAnswer>,

    @SerializedName("attempt_id")
    val attemptId: Int?,

    val mode: String = "exam"
)


data class ExamSubmitResponse(

    @SerializedName("test_result_id")
    val testResultId: Int,

    @SerializedName("test_id")
    val testId: Int,

    val correctas: Int,

    @SerializedName("total_preguntas")
    val totalQuestions: Int,

    val puntaje: Int,

    @SerializedName("can_view_correction")
    val canViewCorrection: Boolean? = null,

    @SerializedName("is_practice_result")
    val isPracticeResult: Boolean? = null,

    val resultados: List<ExamQuestionResult> = emptyList()
)


data class ExamQuestionResult(

    @SerializedName("question_id")
    val questionId: Int,

    val categoria: String,

    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @SerializedName("selected_answers")
    val selectedAnswers: List<String>,

    @SerializedName("correct_answers")
    val correctAnswers: List<String>,

    val explicacion: String
)

data class ExamCorrectionResponse(
    val id: Int,

    @SerializedName("test_id")
    val testId: Int,

    val puntaje: Int,
    val correctas: Int,

    @SerializedName("total_preguntas")
    val totalQuestions: Int,

    val fecha: String?,

    @SerializedName("started_at")
    val startedAt: String?,

    @SerializedName("finished_at")
    val finishedAt: String?,

    @SerializedName("duration_seconds")
    val durationSeconds: Int?,

    val detalles: List<ExamCorrectionQuestion>
)


data class ExamCorrectionQuestion(

    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("texto_pregunta")
    val questionText: String,

    val alternativas: Map<String, String>,

    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @SerializedName("selected_answers")
    val selectedAnswers: List<String>,

    @SerializedName("correct_answers")
    val correctAnswers: List<String>,

    val explicacion: String,

    @SerializedName("imagen_url")
    val imageUrl: String?
)