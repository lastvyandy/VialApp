package cl.vialapp.app.data.api

import com.google.gson.annotations.SerializedName

data class SimulatorCatalog(
    val scenarios: List<DrivingScenario>,
    @SerializedName("completed_ids") val completedIds: List<String>
)
data class DrivingScenario(
    val id: String, val scene: String, val title: String, val category: String,
    val description: String, val steps: List<DrivingStep>
)
data class DrivingStep(
    val prompt: String, val options: List<String>, val correct: Int, val explanation: String
)
data class SimulatorCompletionRequest(val attempts: List<List<Int>>)
data class SimulatorCompletionResponse(
    @SerializedName("xp_awarded") val xpAwarded: Int,
    val duplicate: Boolean, val errors: Int, val gamification: GamificationProgressResponse
)
