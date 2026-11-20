package cl.vialapp.app.data.session

// GUÍA DEL ARCHIVO:
// Sesión de cada cuenta y tokens. Nunca copies estos datos a Logcat ni a una tarjeta.

import android.content.Context

class SessionManager(context: Context) {
    private val appContext = context.applicationContext

    private val preferences =
        context.getSharedPreferences(
            "vialapp_session",
            Context.MODE_PRIVATE
        )

    fun saveAccessToken(token: String) {
        preferences
            .edit()
            .putString("access_token", token)
            .apply()
    }

    fun getAccessToken(): String? {
        return preferences.getString(
            "access_token",
            null
        )
    }

    fun clearSession() {
        preferences
            .edit()
            .clear()
            .apply()
        cl.vialapp.app.engagement.EngagementState.deactivate(appContext)
    }


    fun saveCsrfToken(
        token: String
    ) {

        preferences
            .edit()
            .putString(
                "csrf_token",
                token
            )
            .apply()
    }


    fun getCsrfToken(): String? {

        return preferences.getString(
            "csrf_token",
            null
        )
    }
}