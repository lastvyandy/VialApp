package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Medallas que informa el servidor; la pantalla las muestra, pero no decide cuándo se ganan.

import com.google.gson.annotations.SerializedName

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val kind: String,
    val rarity: String,
    val icon: String,
    val goal: Int,
    val progress: Int,
    val unlocked: Boolean,
    @SerializedName("unlocked_at") val unlockedAt: String? = null
)

data class AchievementCollectionResponse(
    val items: List<AchievementItem>,
    @SerializedName("newly_unlocked") val newlyUnlocked: List<String> = emptyList()
)
