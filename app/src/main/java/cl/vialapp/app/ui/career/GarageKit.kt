package cl.vialapp.app.ui.career

// GUÍA DEL ARCHIVO:
// Piezas y modificaciones que se pueden mostrar o equipar en vehículos.

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

internal data class GarageKit(val wheels:String="wheels_stock",val decal:String="decal_stock",
    val wing:String="wing_stock",val neon:String="neon_stock"){
    fun neonColor():Color?=when(neon){
        "neon_blue"->Color(0xFF36DFFF)
        "neon_purple"->Color(0xFFB46BFF)
        else->null
    }
}

internal fun garageKit(items:Map<String,String>?)=GarageKit(
    items.orEmpty()["llantas"] ?: "wheels_stock",
    items.orEmpty()["vinilos"] ?: "decal_stock",
    items.orEmpty()["aleron"] ?: "wing_stock",
    items.orEmpty()["neon"] ?: "neon_stock")

internal fun cachedKit(prefs:SharedPreferences)=GarageKit(
    prefs.getString("wheels","wheels_stock") ?: "wheels_stock",
    prefs.getString("decal","decal_stock") ?: "decal_stock",
    prefs.getString("wing","wing_stock") ?: "wing_stock",
    prefs.getString("neon","neon_stock") ?: "neon_stock")
