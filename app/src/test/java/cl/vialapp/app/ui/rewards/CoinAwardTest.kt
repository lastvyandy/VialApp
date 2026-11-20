package cl.vialapp.app.ui.rewards

// QUÉ PROTEGE ESTA PRUEBA:
// Evita cálculos incorrectos de monedas; el Pelao no es un banco, jajaj.
import org.junit.Assert.assertEquals
import org.junit.Test
class CoinAwardTest{
    @Test fun matchesServerFloorArithmetic(){
        assertEquals(6,coinsFromAward(130,30))
        assertEquals(1,coinsFromAward(10,1))
        assertEquals(0,coinsFromAward(9,1))
        assertEquals(0,coinsFromAward(100,0))
        assertEquals(0,coinsFromAward(0,10))
    }
}
