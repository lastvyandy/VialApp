package cl.vialapp.app.ui.practice

import cl.vialapp.app.data.api.CategoryStatResponse

/** A category needs a usable sample before it can drive a study recommendation. */
internal const val MIN_PRIORITY_ANSWERS = 8

/**
 * Rank evidence-supported weaknesses, not the API list order. A tiny sample
 * cannot put a category at the top of the student's training plan.
 *
 * This is a recommendation, not an exam-readiness or pass prediction.
 */
internal fun recommendedPriority(stats: List<CategoryStatResponse>): CategoryStatResponse? =
    stats.asSequence()
        .filter { it.totalAnswered >= MIN_PRIORITY_ANSWERS && it.incorrectas > 0 }
        .maxWithOrNull(
            compareBy<CategoryStatResponse> {
                // Smoothed error rate: an extreme result from only a few
                // answers carries less weight than sustained difficulty.
                (it.incorrectas.coerceIn(0, it.totalAnswered) + 2.0) /
                    (it.totalAnswered + 10.0)
            }.thenBy { it.incorrectas.coerceAtLeast(0) }
                .thenBy { it.totalAnswered.coerceAtLeast(0) }
        )

/** Keep all categories visible, but put the strongest supported needs first. */
internal fun rankedPracticeCategories(stats: List<CategoryStatResponse>): List<CategoryStatResponse> =
    stats.sortedWith(
        compareByDescending<CategoryStatResponse> {
            if (it.totalAnswered >= MIN_PRIORITY_ANSWERS)
                (it.incorrectas.coerceIn(0, it.totalAnswered) + 2.0) /
                    (it.totalAnswered + 10.0)
            else -1.0
        }.thenByDescending { it.totalAnswered }
    )
