package cl.vialapp.app.ui.library

import org.junit.Assert.*
import org.junit.Test

class OfflineQuizTest {
    @Test fun everyQuestionHasValidOptionsAndSource(){
        assertEquals(18,OfflineQuiz.questions.size)
        for(q in OfflineQuiz.questions){
            assertEquals(3,q.options.distinct().size)
            assertTrue(q.correct in q.options.indices)
            assertTrue(q.explanation.isNotBlank())
            assertTrue(OfflineQuiz.source(q).reference.isNotBlank())
        }
    }
    @Test fun classCIncludesGeneralKnowledgeAndMotorcycleTopics(){
        assertEquals(12,OfflineQuiz.forClass("B").size)
        assertEquals(18,OfflineQuiz.forClass("C").size)
        assertTrue(OfflineQuiz.forClass("C").containsAll(OfflineQuiz.forClass("B")))
    }
}
