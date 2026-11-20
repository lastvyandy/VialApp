package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Configuramos Retrofit y cookies. Sí, el nombre tiene una errata antigua: no lo renombres sin revisar las referencias.

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Este es el servidor que usa el APK. Cambiar el repositorio no despliega el backend automáticamente.
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
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .callTimeout(35, TimeUnit.SECONDS)
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