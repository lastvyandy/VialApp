package cl.vialapp.app.ui.learning

import cl.vialapp.app.data.api.LearningChapterSummary
import cl.vialapp.app.data.api.LearningProgress
import org.junit.Assert.*
import org.junit.Test

class LearningRouteLogicTest {
    private fun progress(done: Boolean = false, vararg ids: String) =
        LearningProgress(ids.associateWith { 0 }, emptyList(), done, 0, done)
    private fun chapter(id: String, done: Boolean) =
        LearningChapterSummary(id, id, 1, 0, emptyList(), 1, 0, progress(done))

    @Test fun firstIncompleteIsAvailableAndPreviouslyCompletedCanBeReviewed() {
        val chapters = listOf(chapter("a", true), chapter("b", false), chapter("c", false), chapter("d", true))
        assertEquals(1, nextChapterIndex(chapters))
        assertTrue(chapterAvailable(chapters, 0))
        assertTrue(chapterAvailable(chapters, 1))
        assertFalse(chapterAvailable(chapters, 2))
        assertTrue(chapterAvailable(chapters, 3))
    }

    @Test fun resumeSkipsPersistedStepsButReviewStartsAtBeginning() {
        val ids = listOf("a", "b", "c")
        val progress = progress(false, "a", "b")
        assertEquals(2, resumeIndex(ids, progress, false))
        assertEquals(0, resumeIndex(ids, progress, true))
    }
}
