package cl.vialapp.app.ui.learning

// GUÍA DEL ARCHIVO:
// Elige siguiente capítulo y dónde retomar. El orden de la interfaz no reemplaza los permisos del servidor.

import cl.vialapp.app.data.api.LearningChapterSummary
import cl.vialapp.app.data.api.LearningProgress

/** Guided order is a UI recommendation, not a new authorization rule for the shared API. */
internal fun nextChapterIndex(chapters: List<LearningChapterSummary>): Int =
    chapters.indexOfFirst { !it.progress.completed }.let { if (it < 0) chapters.size else it }

internal fun chapterAvailable(chapters: List<LearningChapterSummary>, index: Int): Boolean =
    index in chapters.indices && (chapters[index].progress.completed || index <= nextChapterIndex(chapters))

internal fun resumeIndex(ids: List<String>, progress: LearningProgress, review: Boolean): Int =
    if (review) 0 else ids.indexOfFirst { it !in progress.answers }.let { if (it == -1) 0 else it }
