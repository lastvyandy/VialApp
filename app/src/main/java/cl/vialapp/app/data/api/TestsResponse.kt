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