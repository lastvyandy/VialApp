package cl.vialapp.app.ui.practice

import cl.vialapp.app.data.api.CategoryStatResponse
import org.junit.Assert.*
import org.junit.Test

class PracticeOverviewLogicTest {
    private fun category(name:String,incorrect:Int,total:Int):CategoryStatResponse {
        val correct=(total-incorrect).coerceAtLeast(0)
        val accuracy=if(total>0)correct*100/total else 0
        return CategoryStatResponse(
            categoria=name,totalAnswered=total,correctas=correct,
            incorrectas=incorrect,
            errorPercentage=100-accuracy,
            successPercentage=accuracy
        )
    }

    @Test fun newcomerHasNoUnfoundedPersonalPriority(){
        assertNull(recommendedPriority(emptyList()))
        assertNull(recommendedPriority(listOf(category("Señales",1,1))))
        assertNull(recommendedPriority(listOf(category("Normas",0,24))))
    }

    @Test fun oneWrongAnswerDoesNotOutrankRepeatedDifficulty(){
        val stats=listOf(
            category("Muy poca evidencia",1,1),
            category("Señales de prioridad",9,20),
            category("Estacionamiento",3,22)
        )
        assertEquals("Señales de prioridad",recommendedPriority(stats)?.categoria)
    }

    @Test fun algorithmDoesNotDependOnServerOrdering(){
        val a=category("Distancias",6,18)
        val b=category("Velocidad",2,20)
        assertEquals("Distancias",recommendedPriority(listOf(b,a))?.categoria)
        assertEquals("Distancias",recommendedPriority(listOf(a,b))?.categoria)
    }

    @Test fun categoriesRemainAccessibleEvenWithLittleEvidence(){
        val stats=listOf(category("Señales",1,1),category("Intersecciones",6,20))
        val ordered=rankedPracticeCategories(stats)
        assertEquals(stats.size,ordered.size)
        assertEquals("Intersecciones",ordered.first().categoria)
        assertEquals(setOf("Señales","Intersecciones"),ordered.map{it.categoria}.toSet())
    }

    @Test fun priorityRequiresMeaningfulSampleAndAnActualError(){
        assertNull(recommendedPriority(listOf(category("Normas",0,30),category("Poco historial",4,4))))
        assertEquals("Normas",recommendedPriority(listOf(category("Normas",1,8)))?.categoria)
    }
}
