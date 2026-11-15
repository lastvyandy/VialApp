package cl.vialapp.app.ui.simulator

/** One immutable state for the current decision and its animation transition. */
internal data class ScenarioDecisionState(
    val attempts: List<List<Int>>,
    val stage: Int = 0,
    val transitioning: Boolean = false
) {
    val finished: Boolean get() = stage >= attempts.size

    fun choose(expectedStage: Int, option: Int, correct: Int): ScenarioDecisionState {
        if (finished || transitioning || expectedStage != stage || option in attempts[stage]) return this
        val updated = attempts.mapIndexed { i, values -> if (i == stage) values + option else values }
        return copy(attempts = updated, transitioning = option == correct)
    }

    fun advance(expectedStage: Int): ScenarioDecisionState =
        if (!finished && transitioning && expectedStage == stage)
            copy(stage = stage + 1, transitioning = false)
        else this

    companion object {
        fun start(count: Int) = ScenarioDecisionState(List(count) { emptyList() })
    }
}
