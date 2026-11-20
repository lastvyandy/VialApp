package cl.vialapp.app.ui.rewards

// GUÍA DEL ARCHIVO:
// Representa monedas ganadas sin mezclar cantidad y animación.
internal fun coinsFromAward(totalXp:Int,xpAwarded:Int):Int{
    val total=totalXp.coerceAtLeast(0)
    val award=xpAwarded.coerceIn(0,total)
    return total/5-(total-award)/5
}
