package cl.vialapp.app.ui.games.arcade

import org.junit.Assert.*
import org.junit.Test

class SignEngineTest {
    @Test fun everyQuestionHasExactlyOneCorrectOptionAndNoRepeat() {
        val engine = SignEngine(40)
        val questions = engine.snapshot().questions
        assertEquals(RoadSign.entries.size, questions.map { it.sign }.distinct().size)
        questions.forEach {
            assertEquals(3, it.choices.distinct().size)
            assertEquals(1, it.choices.count { answer -> answer == it.sign })
        }
    }
    @Test fun correctChoicesAdvanceWithoutDoubleScoring() {
        val engine = SignEngine(21)
        repeat(RoadSign.entries.size) {
            val frame = engine.snapshot()
            val choice = frame.current.choices.indexOf(frame.current.sign)
            engine.act(choice)
            val score = engine.snapshot().score
            engine.act(choice)
            assertEquals(score, engine.snapshot().score)
            repeat(80) { engine.step(1f / 30f) }
        }
        assertTrue(engine.snapshot().won)
    }
    @Test fun wrongChoiceCostsLifeAndStoresLesson() {
        val engine = SignEngine(1)
        val sign = engine.snapshot().current.sign
        engine.act(engine.snapshot().current.choices.indexOfFirst { it != sign })
        assertEquals(2, engine.snapshot().lives)
        assertTrue(sign in engine.snapshot().missed)
    }
}
