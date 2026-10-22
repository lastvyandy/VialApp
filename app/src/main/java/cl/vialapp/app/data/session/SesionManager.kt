package cl.vialapp.app.data.session

import android.content.Context

class SessionManager(context: Context) {

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