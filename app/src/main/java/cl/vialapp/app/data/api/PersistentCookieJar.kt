package cl.vialapp.app.data.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(
    context: Context
) : CookieJar {

    private val preferences =
        context.getSharedPreferences(
            "vialapp_cookies",
            Context.MODE_PRIVATE
        )

    private val gson = Gson()

    private val listType =
        object : TypeToken<MutableList<StoredCookie>>() {}.type


    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {

        val now = System.currentTimeMillis()

        val storedCookies =
            loadAll()
                .filter { it.expiresAt > now }
                .toMutableList()

        cookies.forEach { cookie ->

            storedCookies.removeAll {
                it.name == cookie.name &&
                        it.domain == cookie.domain &&
                        it.path == cookie.path
            }

            if (cookie.expiresAt > now) {
                storedCookies.add(
                    StoredCookie.from(cookie)
                )
            }
        }

        saveAll(storedCookies)
    }


    override fun loadForRequest(
        url: HttpUrl
    ): List<Cookie> {

        val now = System.currentTimeMillis()

        val all = loadAll()

        val valid =
            all.filter {
                it.expiresAt > now
            }

        if (valid.size != all.size) {
            saveAll(valid)
        }

        return valid
            .map { it.toCookie() }
            .filter { it.matches(url) }
    }


    fun clear() {

        preferences
            .edit()
            .clear()
            .apply()
    }


    private fun loadAll(): MutableList<StoredCookie> {

        val json =
            preferences.getString(
                "cookies",
                null
            ) ?: return mutableListOf()

        return gson.fromJson(
            json,
            listType
        )
    }


    private fun saveAll(
        cookies: List<StoredCookie>
    ) {

        preferences
            .edit()
            .putString(
                "cookies",
                gson.toJson(cookies)
            )
            .apply()
    }


    private data class StoredCookie(

        val name: String,
        val value: String,
        val expiresAt: Long,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean

    ) {

        fun toCookie(): Cookie {

            val builder =
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .expiresAt(expiresAt)
                    .path(path)

            if (hostOnly) {
                builder.hostOnlyDomain(domain)
            } else {
                builder.domain(domain)
            }

            if (secure) {
                builder.secure()
            }

            if (httpOnly) {
                builder.httpOnly()
            }

            return builder.build()
        }


        companion object {

            fun from(
                cookie: Cookie
            ): StoredCookie {

                return StoredCookie(
                    name = cookie.name,
                    value = cookie.value,
                    expiresAt = cookie.expiresAt,
                    domain = cookie.domain,
                    path = cookie.path,
                    secure = cookie.secure,
                    httpOnly = cookie.httpOnly,
                    hostOnly = cookie.hostOnly
                )
            }
        }
    }
}