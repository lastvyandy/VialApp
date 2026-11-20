package cl.vialapp.app.ui.auth

// QUÉ PROTEGE ESTA PRUEBA:
// Evita que errores de autenticación terminen en mensajes confusos.

import org.junit.Assert.*
import org.junit.Test

class AuthMessagesTest{
    @Test fun distinguishesRejectedCredentialsFromServerAndRateLimit(){
        assertEquals("Correo o contraseña incorrectos.",authHttpMessage(401,AuthAction.LOGIN))
        assertTrue(authHttpMessage(503,AuthAction.LOGIN).contains("servidor"))
        assertTrue(authHttpMessage(429,AuthAction.LOGIN).contains("intentos"))
    }
    @Test fun duplicateEmailMatchesActualBackend400(){
        assertTrue(authHttpMessage(400,AuthAction.REGISTER,"Email ya está en uso").contains("ya está registrado"))
        assertFalse(authHttpMessage(400,AuthAction.REGISTER,"Debes confirmar que tienes 18 años o más").contains("registrado"))
    }
    @Test fun validatesPasswordWithoutTrimmingIt(){
        assertNotNull(registrationPasswordError("short"))
        assertNotNull(registrationPasswordError("á".repeat(37)))
        assertNull(registrationPasswordError("password123"))
    }
}
