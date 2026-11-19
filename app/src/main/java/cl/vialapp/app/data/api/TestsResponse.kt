package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class TestsResponse(

    val tests: List<AssignedTest>,

    @SerializedName("next_unlock_at")
    val nextUnlockAt: String?,

    @SerializedName("daily_exam_limit")
    val dailyExamLimit: Int,

    @SerializedName("unlock_minutes")
    val unlockMinutes: Int,

    @SerializedName("history_has_more")
    val historyHasMore: Boolean
)


data class AssignedTest(

    val id: Int,

    val nombre: String,

    val categoria: String?,

    @SerializedName("clase_licencia")
    val licenseClass: String,

    @SerializedName("assigned_date")
    val assignedDate: String?,

    @SerializedName("is_today")
    val isToday: Boolean,

    @SerializedName("has_result")
    val hasResult: Boolean,

    @SerializedName("result_id")
    val resultId: Int?,

    @SerializedName("is_locked")
    val isLocked: Boolean,

    @SerializedName("requires_premium")
    val requiresPremium: Boolean,

    @SerializedName("can_start")
    val canStart: Boolean,

    @SerializedName("locked_reason")
    val lockedReason: String?
)

/** Catalog counts contain metadata only; they do not grant access to exam questions. */
data class ExamCategoryCount(
    @SerializedName("clase_licencia") val licenseClass: String,
    val categoria: String,
    val cantidad: Int
)

/** One completed attempt, independent of whether its exam is still assigned. */
data class ExamHistoryResult(
    val id: Int,
    @SerializedName("test_id") val testId: Int,
    val categoria: String?,
    @SerializedName("clase_licencia") val licenseClass: String?,
    val puntaje: Int?,
    val correctas: Int?,
    @SerializedName("total_preguntas") val totalQuestions: Int?,
    val fecha: String?
)

data class ExtraExamRequest(
    @SerializedName("clase_licencia") val licenseClass: String,
    val categoria: String
)


/** Read-only metadata; availability is checked against separately assigned exams. */
data class ExamCatalogEntry(
    val id: Int,
    val nombre: String,
    val categoria: String?,
    @SerializedName("clase_licencia") val licenseClass: String
)

data class ExamCatalogPage(
    val tests: List<ExamCatalogEntry> = emptyList(),
    @SerializedName("has_more") val hasMore: Boolean = false
)
