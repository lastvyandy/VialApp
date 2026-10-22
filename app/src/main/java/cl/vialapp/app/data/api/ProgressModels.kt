package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName


data class AdvancedStatsResponse(

    val summary: AdvancedStatsSummary,

    val categories: List<CategoryStatResponse> = emptyList(),

    @SerializedName("recent_tests")
    val recentTests: List<RecentTestResponse> = emptyList()
)


data class AdvancedStatsSummary(

    @SerializedName("has_data")
    val hasData: Boolean,

    @SerializedName("total_tests")
    val totalTests: Int,

    @SerializedName("tests_aprobados")
    val passedTests: Int,

    @SerializedName("porcentaje_aprobacion")
    val passPercentage: Int,

    @SerializedName("puntaje_promedio")
    val averageScore: Int,

    @SerializedName("mejor_puntaje")
    val bestScore: Int,

    @SerializedName("peor_puntaje")
    val worstScore: Int,

    @SerializedName("total_preguntas_respondidas")
    val totalAnsweredQuestions: Int,

    @SerializedName("total_respuestas_correctas")
    val totalCorrectAnswers: Int,

    @SerializedName("tests_ultimos_7_dias")
    val testsLast7Days: Int,

    @SerializedName("tests_ultimos_30_dias")
    val testsLast30Days: Int,

    @SerializedName("ultimo_test_en")
    val lastTestAt: String?,

    @SerializedName("max_errores_permitidos")
    val maxAllowedErrors: Int,

    @SerializedName("criterio_aprobacion")
    val passCriterion: String,

    @SerializedName("mejor_categoria")
    val bestCategory: BestCategoryResponse? = null
)


data class BestCategoryResponse(

    val nombre: String,

    @SerializedName("porcentaje_acierto")
    val successPercentage: Int
)


data class RecentTestResponse(

    val id: Int,

    @SerializedName("test_id")
    val testId: Int,

    val puntaje: Int?,

    val correctas: Int?,

    @SerializedName("total_preguntas")
    val totalQuestions: Int?,

    val fecha: String?,

    @SerializedName("duration_seconds")
    val durationSeconds: Int?
)