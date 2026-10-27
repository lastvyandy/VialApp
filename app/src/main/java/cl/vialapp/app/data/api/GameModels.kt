package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class GamificationProgressResponse(
    @SerializedName("total_xp") val totalXp: Int = 0,
    @SerializedName("total_points") val totalPoints: Int = 0,
    @SerializedName("games_played") val gamesPlayed: Int = 0,
    @SerializedName("level") val level: Int = 1,
    @SerializedName("xp_in_level") val xpInLevel: Int = 0,
    @SerializedName("xp_to_next_level") val xpToNextLevel: Int = 500
)

data class GamificationGameEventRequest(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("game_id") val gameId: String,
    @SerializedName("score") val score: Int,
    @SerializedName("duration_seconds") val durationSeconds: Int
)

data class GamificationEventResponse(
    @SerializedName("total_xp") val totalXp: Int = 0,
    @SerializedName("total_points") val totalPoints: Int = 0,
    @SerializedName("games_played") val gamesPlayed: Int = 0,
    @SerializedName("level") val level: Int = 1,
    @SerializedName("xp_in_level") val xpInLevel: Int = 0,
    @SerializedName("xp_to_next_level") val xpToNextLevel: Int = 500,
    @SerializedName("event_id") val eventId: String = "",
    @SerializedName("event_type") val eventType: String = "",
    @SerializedName("xp_awarded") val xpAwarded: Int = 0,
    @SerializedName("points_awarded") val pointsAwarded: Int = 0,
    @SerializedName("duplicate") val duplicate: Boolean = false
)