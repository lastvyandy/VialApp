package cl.vialapp.app.ui.simulator

import org.junit.Assert.*
import org.junit.Test

class ScenarioDecisionStateTest {
    @Test fun allThreeQuestionsCanBeAnsweredAfterEachTransition() {
        for (correctAnswers in listOf(listOf(0, 1, 2), listOf(1, 0, 1), listOf(2, 0, 0))) {
            var state = ScenarioDecisionState.start(correctAnswers.size)
            correctAnswers.forEachIndexed { index, correct ->
                assertEquals(index, state.stage)
                assertFalse(state.transitioning)
                state = state.choose(index, correct, correct)
                assertTrue(state.transitioning)
                state = state.advance(index)
                assertFalse(state.transitioning)
            }
            assertTrue(state.finished)
            assertEquals(correctAnswers.map { listOf(it) }, state.attempts)
        }
    }

    @Test fun wrongChoiceDoesNotLockAndOldClicksCannotAffectNextQuestion() {
        var state = ScenarioDecisionState.start(3).choose(0, 2, 0)
        assertFalse(state.transitioning)
        assertEquals(state, state.choose(0, 2, 0))
        state = state.choose(0, 0, 0)
        assertEquals(state, state.choose(0, 1, 0))
        state = state.advance(0)
        assertEquals(state, state.choose(0, 1, 1))
        state = state.choose(1, 1, 1).advance(1)
        assertEquals(listOf(listOf(2, 0), listOf(1), emptyList<Int>()), state.attempts)
        assertEquals(2, state.stage)
        assertFalse(state.transitioning)
    }
}
