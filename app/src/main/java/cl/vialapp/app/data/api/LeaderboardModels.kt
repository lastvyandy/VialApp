package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class LeaderboardEntryResponse(
    @SerializedName("position") val position: Int,
    @SerializedName("alias") val alias: String,
    @SerializedName("xp") val xp: Int,
    @SerializedName("mascot_id") val mascotId: String? = null,
    @SerializedName("is_me") val isMe: Boolean = false,
    @SerializedName("avatar_key") val avatarKey: String? = null
)

data class WeeklyLeaderboardResponse(
    @SerializedName("week_start") val weekStart: String,
    @SerializedName("week_end") val weekEnd: String,
    @SerializedName("resets_at") val resetsAt: String,
    @SerializedName("participants") val participants: Int,
    @SerializedName("entries") val entries: List<LeaderboardEntryResponse>,
    @SerializedName("my_entry") val myEntry: LeaderboardEntryResponse? = null,
    @SerializedName("previous_medal") val previousMedal: Int? = null,
    @SerializedName("week_offset") val weekOffset: Int = 0
)

data class AvatarResponse(val key: String? = null)
