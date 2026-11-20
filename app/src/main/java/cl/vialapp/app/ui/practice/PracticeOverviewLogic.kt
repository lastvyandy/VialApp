package cl.vialapp.app.ui.practice

// GUÍA DEL ARCHIVO:
// Recomendaciones con suficientes respuestas. Una respuesta mala no significa que el alumno no sepa manejar, po.

import cl.vialapp.app.data.api.CategoryStatResponse

/** Necesitamos varias respuestas antes de decirle al alumno que este tema le cuesta. */
internal const val MIN_PRIORITY_ANSWERS = 8

/**
 * Acá ordenamos los temas por dificultad real, no por el orden en que llegaron del servidor.
 * Con muy pocas respuestas podríamos recomendar cualquier cosa, así que las filtramos.
 * Esto sirve para estudiar mejor, no para asegurar que el alumno aprobará el examen.
 */
internal fun recommendedPriority(stats: List<CategoryStatResponse>): CategoryStatResponse? =
    stats.asSequence()
        .filter { it.totalAnswered >= MIN_PRIORITY_ANSWERS && it.incorrectas > 0 }
        .maxWithOrNull(
            compareBy<CategoryStatResponse> {
                // Le bajamos el peso a los porcentajes extremos cuando hay pocas respuestas.
                // No es lo mismo equivocarse una vez que llevar varios intentos fallando.
                (it.incorrectas.coerceIn(0, it.totalAnswered) + 2.0) /
                    (it.totalAnswered + 10.0)
            }.thenBy { it.incorrectas.coerceAtLeast(0) }
                .thenBy { it.totalAnswered.coerceAtLeast(0) }
        )

/** Dejamos todos los temas visibles, pero los que más necesitan repaso van primero. */
internal fun rankedPracticeCategories(stats: List<CategoryStatResponse>): List<CategoryStatResponse> =
    stats.sortedWith(
        compareByDescending<CategoryStatResponse> {
            if (it.totalAnswered >= MIN_PRIORITY_ANSWERS)
                (it.incorrectas.coerceIn(0, it.totalAnswered) + 2.0) /
                    (it.totalAnswered + 10.0)
            else -1.0
        }.thenByDescending { it.totalAnswered }
    )
