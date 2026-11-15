package cl.vialapp.app.ui.rewards
internal fun coinsFromAward(totalXp:Int,xpAwarded:Int):Int{
    val total=totalXp.coerceAtLeast(0)
    val award=xpAwarded.coerceIn(0,total)
    return total/5-(total-award)/5
}
