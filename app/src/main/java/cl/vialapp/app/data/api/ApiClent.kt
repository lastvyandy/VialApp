package cl.vialapp.app.data.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL =
        "https://api.vialapp.cl/api/v1/"

    private var cookieJar: PersistentCookieJar? = null

    lateinit var api: VialApi
        private set


    fun init(context: Context) {

        if (::api.isInitialized) {
            return
        }

        val persistentCookieJar =
            PersistentCookieJar(
                context.applicationContext
            )

        cookieJar =
            persistentCookieJar

        val okHttpClient =
            OkHttpClient.Builder()
                .cookieJar(
                    persistentCookieJar
                )
                .build()

        api =
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()
                .create(VialApi::class.java)
    }


    fun clearCookies() {

        cookieJar?.clear()
    }
}