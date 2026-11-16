package cl.vialapp.app.ui.games

import cl.vialapp.app.data.games.GameProgressStore

/** The current playable lineup. Retired games retain saved scores but are not advertised. */
internal data class PlayableGame(val id:String,val title:String,val route:String)

internal object GameCatalog {
    val entries = listOf(
        PlayableGame(GameProgressStore.CARVIER_DRIVE,"Carvier al volante","games/carvier"),
        PlayableGame(GameProgressStore.PELAO_ARENA,"Don Pelao Arena","games/arena"),
        PlayableGame(GameProgressStore.VIAL_RALLY,"Vial Rally","games/rally"),
        PlayableGame(GameProgressStore.BALD_CHALLENGE,"Reto del Pelao · clásico","games"),
        PlayableGame(GameProgressStore.CROSSROADS_ARCADE,"Cruce Caótico","games/crossroads"),
        PlayableGame(GameProgressStore.SIGN_HUNT_ARCADE,"Caza la Señal","games/signs"),
        PlayableGame(GameProgressStore.PELAO_DOOM,"Don Pelao: Operación Caos Vial","games/doom")
    )
    fun title(id:String):String=entries.first{it.id==id}.title
}
