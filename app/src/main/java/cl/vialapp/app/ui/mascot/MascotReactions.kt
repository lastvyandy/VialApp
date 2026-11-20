package cl.vialapp.app.ui.mascot

// GUÍA DEL ARCHIVO:
// Reglas para decidir cuándo la mascota reacciona, separadas de las tarjetas.

import cl.vialapp.app.data.api.GamificationEventResponse

enum class MascotReaction(val expression: String) {
    PERFECT("celebrating"),
    GREAT("happy"),
    KEEP_GOING("serious"),
    REVIEW("disappointed"),
    MISSION("happy"),
    LEVEL_UP("celebrating"),
    PODIUM("celebrating")
}

object MascotReactions {
    fun isSupported(mascotId: String?): Boolean =
        mascotId == "carvier" || mascotId == "mister_pelao"

    // Se respetan las bandas del resultado existente; no certifica aprobación oficial.
    fun exam(score: Int): MascotReaction = when {
        score >= 100 -> MascotReaction.PERFECT
        score >= 87 -> MascotReaction.GREAT
        score >= 70 -> MascotReaction.KEEP_GOING
        else -> MascotReaction.REVIEW
    }

    // El backend informa nivel, XP dentro del nivel y XP otorgada.
    // Cruzar un umbral implica que la nueva XP supera el progreso dentro del nivel.
    fun levelReached(reward: GamificationEventResponse?): Int? =
        reward?.takeIf {
            !it.duplicate && it.level > 1 && it.xpAwarded > 0 &&
                it.xpInLevel >= 0 && it.xpAwarded > it.xpInLevel
        }?.level

    fun name(mascotId: String): String =
        if (mascotId == "mister_pelao") "Don Pelao" else "Carvier"

    fun resourceName(mascotId: String, reaction: MascotReaction): String =
        (if (mascotId == "mister_pelao") "don_pelao" else "carvier") + "_" + reaction.expression

    fun message(mascotId: String, reaction: MascotReaction): String =
        if (mascotId == "mister_pelao") {
            when (reaction) {
                MascotReaction.PERFECT -> "¡Una bestia, un animal! Ni una sola mala."
                MascotReaction.GREAT -> "¡Bien, máquina! Así se hace."
                MascotReaction.KEEP_GOING -> "Vas bien, pero todavía hay que entrenar. ¡Dale!"
                MascotReaction.REVIEW -> "Así no vamos a sacar la licencia, po. ¡A repasar y darle de nuevo!"
                MascotReaction.MISSION -> "¡Misión cumplida! Esa es la disciplina que me gusta."
                MascotReaction.LEVEL_UP -> "¡Puro músculo cerebral! Sigue así, máquina."
                MascotReaction.PODIUM -> "¡En el podio, máquina! Ahora a defender ese puesto."
            }
        } else {
            when (reaction) {
                MascotReaction.PERFECT -> "¡Puntaje perfecto! ¡Eres una máquina!"
                MascotReaction.GREAT -> "¡Excelente trabajo! Cada vez estás más cerca."
                MascotReaction.KEEP_GOING -> "¡Vas por buen camino! Repasemos un poquito más."
                MascotReaction.REVIEW -> "No te rindas. Vamos a aprender de los errores juntos."
                MascotReaction.MISSION -> "¡Una misión más! Sigamos avanzando juntos."
                MascotReaction.LEVEL_UP -> "¡Subimos de marcha! Qué orgullo llegar tan lejos."
                MascotReaction.PODIUM -> "¡Llegamos al podio! Sigamos avanzando juntos."
            }
        }
}
