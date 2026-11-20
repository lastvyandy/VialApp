package cl.vialapp.app.ui.mascot

// QUÉ PROTEGE ESTA PRUEBA:
// Comprueba que la mascota reaccione al estado correcto, no al azar.

import cl.vialapp.app.data.api.GamificationEventResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MascotReactionsTest {
    @Test
    fun resultBandsMatchExistingScreen() {
        assertEquals(MascotReaction.PERFECT, MascotReactions.exam(100))
        assertEquals(MascotReaction.GREAT, MascotReactions.exam(87))
        assertEquals(MascotReaction.KEEP_GOING, MascotReactions.exam(86))
        assertEquals(MascotReaction.KEEP_GOING, MascotReactions.exam(70))
        assertEquals(MascotReaction.REVIEW, MascotReactions.exam(69))
    }

    @Test
    fun onlyNewRewardsCrossingLevelBoundaryCelebrate() {
        val reward = GamificationEventResponse(level = 2, xpInLevel = 10, xpAwarded = 30)
        assertEquals(2, MascotReactions.levelReached(reward))
        assertNull(MascotReactions.levelReached(reward.copy(duplicate = true)))
        assertNull(MascotReactions.levelReached(reward.copy(xpAwarded = 0)))
        assertNull(MascotReactions.levelReached(reward.copy(xpInLevel = 30)))
        assertNull(MascotReactions.levelReached(null))
    }

    @Test
    fun resourceNamesPreserveDatabaseIdentifiers() {
        assertEquals("don_pelao_disappointed", MascotReactions.resourceName("mister_pelao", MascotReaction.REVIEW))
        assertEquals("carvier_celebrating", MascotReactions.resourceName("carvier", MascotReaction.PERFECT))
    }
}
