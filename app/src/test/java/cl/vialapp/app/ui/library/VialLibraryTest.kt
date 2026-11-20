package cl.vialapp.app.ui.library

// QUÉ PROTEGE ESTA PRUEBA:
// Revisa la estructura y consistencia de los contenidos de biblioteca.

import org.junit.Assert.*
import org.junit.Test

class VialLibraryTest {
    @Test fun sourcePagesAndIdsAreValid(){
        assertEquals(VialLibrary.articles.size,VialLibrary.articles.map{it.id}.distinct().size)
        for(a in VialLibrary.articles){
            assertTrue(a.firstPage>=1 && a.lastPage>=a.firstPage && a.lastPage<=a.manual.pages)
            assertTrue(a.points.isNotEmpty())
        }
    }
    @Test fun searchIgnoresAccentsAndFiltersSources(){
        assertTrue(VialLibrary.search("conduccion curva",Manual.C,null).any{it.id=="moto-curve"})
        assertTrue(VialLibrary.search("reaccion",Manual.B,null).any{it.id=="stopping-distance"})
        assertTrue(VialLibrary.search("casco",Manual.B,null).none{it.manual==Manual.C})
        assertEquals(listOf("helmet"),VialLibrary.search("",null,null,setOf("helmet")).map{it.id})
    }
}
