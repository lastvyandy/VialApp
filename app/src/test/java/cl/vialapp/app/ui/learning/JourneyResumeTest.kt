package cl.vialapp.app.ui.learning

import cl.vialapp.app.data.api.*
import org.junit.Assert.*
import org.junit.Test

class JourneyResumeTest {
    private val q=LearningQuestion("q","Pregunta",listOf("A","B"))
    private val chapter=LearningChapter("test","Etapa",1,listOf(
        LearningLesson("first","Primera","",listOf(LearningConcept("a","A",emptyList(),q))),
        LearningLesson("second","Segunda","",listOf(LearningConcept("b","B",emptyList(),q)))
    ),listOf(q),emptyList(),emptyList())
    private fun saved(lessons:List<String>,complete:Boolean=false)=LearningProgress(emptyMap(),lessons,complete,0,complete)
    @Test fun startsFirstPendingLesson(){
        assertEquals("Primera",pendingLearningActivity(chapter,saved(emptyList()))?.title)
        assertEquals("Segunda",pendingLearningActivity(chapter,saved(listOf("first")))?.title)
    }
    @Test fun resumesFinalChallengeOnlyAfterLessons(){
        assertEquals("Desafío final",pendingLearningActivity(chapter,saved(listOf("first","second")))?.title)
        assertNull(pendingLearningActivity(chapter,saved(listOf("first","second"),true)))
    }
    @Test fun recognizesClassCWithOrWithoutPrefix(){
        assertTrue(isMotorcycleLicense("C"))
        assertTrue(isMotorcycleLicense("Clase C"))
        assertFalse(isMotorcycleLicense("B"))
    }
}
