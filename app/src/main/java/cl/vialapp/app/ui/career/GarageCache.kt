package cl.vialapp.app.ui.career

import android.content.Context
import cl.vialapp.app.data.api.CareerState

/** Save only confirmed loadouts. Offline play never equips an unpurchased preview. */
internal fun cacheEquippedGarage(context:Context,userKey:String,state:CareerState) {
    val model=state.vehicles.firstOrNull{it.id==state.equippedVehicle} ?: return
    val paint=state.paints.firstOrNull{it.id==state.equippedPaint} ?: return
    context.getSharedPreferences("career_appearance_"+userKey,Context.MODE_PRIVATE)
        .edit().putString("style",model.style).putString("paint",paint.hex).putString("zone",state.zone).apply()
}
